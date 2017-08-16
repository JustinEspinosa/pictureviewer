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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ViewPicture {

    private static boolean VERBOSE = false;
    private static Curses CURSES ;

    private Bitmap bitmap;
    private Resolution resolution;
    private ColorTable colorTable;

    public ViewPicture(BufferedImage image, boolean useShape, double ppc){
        bitmap = new Bitmap(image);
        bitmap.setUseShape(useShape);
        colorTable = ColorTable.forDepth(ColorDepth.forNumCols(CURSES.numcolors()));
        resolution = new Resolution(ppc);
    }

    private static void debug(String s){
        if(VERBOSE){
            System.out.println(s);
        }
    }


    public void display() throws IOException{

        debug("Converting picture to ASCII...");
        ASCIIPicture picture = bitmap.ASCIIDither(resolution, colorTable);
        debug("Displaying picture...");
        CURSES.sc();
        int line = 0;
        for(Position p : picture.size()){

            if(p.getLine()>line){
                line = p.getLine();
                CURSES.rc();
                CURSES.print("\n");
                CURSES.sc();
            }

            ColorChar c = picture.get(p);
            CURSES.applyColorPair(c.getColors());
            CURSES.print(String.valueOf(c.getChr()));
        }
        CURSES.rc();
        CURSES.print("\n");

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
        options.addOption( "v", "verbose", false, "Outputs debug information" );

        CommandLine line = parser.parse( options, args );
        if(line.hasOption("v")){
            VERBOSE = true;
        }

        debug("Starting curses for "+ termType);

        CursesFactory factory = CursesFactory.getInstance();
        CURSES = factory.createCurses(factory.createTerminal(termType));

        debug("CURSES has "+CURSES.numcolors()+" colors.");

        String filename = line.getArgList().get(0);

        debug("Loading "+ filename);

        BufferedImage image = ImageIO.read(new File(filename));

        double ppc =  (double)image.getWidth()  /(double)CURSES.cols();
        if(line.hasOption("w")){
            ppc = (double)image.getWidth()  / Double.parseDouble(line.getOptionValue("w"));
        }

        debug("Resolution : "+ppc);

        boolean useShape = true;
        if(line.hasOption("n")){
            useShape = false;
        }

        debug("Use shape : "+useShape);


        ViewPicture vp = new ViewPicture(image, useShape,ppc );

        vp.display();

    }
}
