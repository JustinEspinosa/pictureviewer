package com.github.justinespinosa.pictureviewer;


import com.github.justinespinosa.textmode.curses.Curses;
import com.github.justinespinosa.textmode.curses.CursesFactory;
import com.github.justinespinosa.textmode.curses.lang.ColorChar;
import com.github.justinespinosa.textmode.curses.ui.ColorDepth;
import com.github.justinespinosa.textmode.curses.ui.ColorTable;
import com.github.justinespinosa.textmode.curses.ui.Position;
import com.github.justinespinosa.textmode.graphics.core.ASCIIPicture;
import com.github.justinespinosa.textmode.graphics.core.Bitmap;
import com.github.justinespinosa.textmode.graphics.core.Resolution;
import org.apache.commons.cli.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static java.awt.Font.PLAIN;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class ViewPicture {

    public static boolean VERBOSE = false;
    private static Curses CURSES;
    private static Font BASEFONT;
    static {
        try {
            BASEFONT = Font.createFont(Font.TRUETYPE_FONT, ViewPicture.class.
                    getClassLoader().getResourceAsStream("AdobeArabic-Bold.otf"));
        }catch (Exception e){
            System.out.println("Could not load font");
            throw new RuntimeException(e);
        }
    }

    private Bitmap bitmap;
    private int prefix = 0;
    private Resolution resolution;
    private ColorTable colorTable;
    private ASCIIPicture picture;

    public ViewPicture(BufferedImage image, boolean useShape, double ppc, int numColors){
        bitmap = new Bitmap(image);
        bitmap.setUseShape(useShape);
        colorTable = ColorTable.forDepth(ColorDepth.forNumCols(numColors));
        resolution = new Resolution(ppc);
        debug("Converting picture to ASCII...");
        picture = bitmap.ASCIIDither(resolution, colorTable);
    }

    public void setPrefix(int prefix) {
        this.prefix = prefix;
    }

    private static void debug(String s){
        if(VERBOSE){
            System.out.println(s);
        }
    }

    private void pad(Curses curses) throws IOException{
        StringBuilder builder = new StringBuilder();
        for(int i=0;i<prefix;++i){
            builder.append(' ');
        }
        if(builder.length() > 0) {
            curses.print(builder.toString());
        }
        debug("Padding for "+prefix);
    }


    public void display(Curses curses) throws IOException{

        debug("Displaying picture: " +picture.size()+" char");

        pad(curses);
        debug("Sending sc ");

        curses.sc();
        int line = 0;
        for(Position p : picture.size()){
            debug("At position "+p);

            if(p.getLine()>line){
                line = p.getLine();
                debug("At line "+line);
                curses.rc();
                curses.print("\n");
                pad(curses);
                curses.sc();
            }

            ColorChar c = picture.get(p);
            debug("Color "+c.getColors());
            debug("Chr <<"+c.getChr()+">>");

            curses.applyColorPair(c.getColors());
            curses.print(String.valueOf(c.getChr()));
        }
        curses.rc();
        curses.print("\n");

    }

    private static GeneralPath get2alb(){
        GeneralPath path = new GeneralPath();
        path.moveTo(100,160);
        path.lineTo(180,80);
        path.curveTo(180,20,160,20,140,20);
        path.curveTo(120,20,100,20,100,60);
        path.curveTo(100,20,80,20,60,20);
        path.curveTo(40,20,20,20,20,80);
        path.lineTo(100,160);
        return path;
    }

    private static void withShadow(Graphics2D graphics, Shape shape, Color color, Color shadow){
        graphics.translate(5,5);
        graphics.setColor(shadow);
        graphics.fill(shape);
        graphics.translate(-5,-5);
        graphics.setColor(color);
        graphics.fill(shape);
    }

    private static Shape getTextShape(Graphics2D graphics, String text){
        return graphics.getFont()
                .layoutGlyphVector(graphics.getFontRenderContext(), text.toCharArray(),0,text.length(),Font.LAYOUT_RIGHT_TO_LEFT)
                .getOutline();
    }

    public static BufferedImage createImage7ob() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                        ViewPicture.class.getClassLoader().getResourceAsStream("bo7"),
                        StandardCharsets.UTF_8));
        BufferedImage image = new BufferedImage(200,200, TYPE_INT_RGB);
        Graphics2D imageGraphics = (Graphics2D)image.getGraphics();
        try {
            imageGraphics.setStroke(new BasicStroke(3));
            imageGraphics.setColor(Color.BLUE);
            imageGraphics.fillRect(0,0,image.getWidth(),image.getHeight());
            withShadow(imageGraphics, get2alb(), Color.RED, Color.BLACK);
            imageGraphics.setFont(BASEFONT.deriveFont( Font.BOLD, 120));
            imageGraphics.translate(8,0);

            int lineHeight = imageGraphics.getFontMetrics().getHeight() - 50;
            String line;
            while( (line = reader.readLine())!=null){
                imageGraphics.translate(0,lineHeight);
                withShadow(imageGraphics,getTextShape(imageGraphics,line),Color.WHITE, Color.BLACK);
            }
        }finally {
            imageGraphics.dispose();
        }
        return image;
    }


    private static BufferedImage createImage(CommandLine line) throws IOException {
        if(line.hasOption("7") && line.hasOption("o") && line.hasOption("b")){
            return createImage7ob();
        }
        String filename = line.getArgList().get(0);

        debug("Loading "+ filename);

        return ImageIO.read(new File(filename));
    }

    public static void main(String[] args) throws IOException, ParseException {

        String termType = System.getenv("TERM");
        if(termType == null){
            System.err.println("Terminal is unknown. The TERM variable is not set.");
            return;
        }

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption( "w", "width", true, "Set image display width in characters" );
        options.addOption( "n", "no-shape", false, "Do not try to guess the shape of zones" );
        options.addOption( "c", "center", false, "Center the image in the screen" );
        options.addOption( "v", "verbose", false, "Outputs debug information" );
        options.addOption( "7",  "");
        options.addOption( "o", "");
        options.addOption( "b", "");

        CommandLine line = parser.parse( options, args );
        if(line.hasOption("v")){
            VERBOSE = true;
        }

        debug("Starting curses for "+ termType);

        CursesFactory factory = CursesFactory.getInstance();
        CURSES = factory.createCurses(factory.createTerminal(termType));

        debug("CURSES has "+CURSES.numcolors()+" colors.");

        BufferedImage image = createImage(line);

        int width = CURSES.cols();
        if(line.hasOption("w")){
            width = Integer.parseInt(line.getOptionValue("w"));
        }
        double ppc =  (double)image.getWidth()  /(double)width;

        debug("Resolution : "+ppc);

        boolean useShape = true;
        if(line.hasOption("n")){
            useShape = false;
        }

        debug("Use shape : "+useShape);

        ViewPicture vp = new ViewPicture(image, useShape,ppc , CURSES.numcolors());

        if(line.hasOption("c")){
            vp.setPrefix((int)Math.round(((double)(CURSES.cols() - width))/2.0));
        }

        vp.display(CURSES);

    }
}
