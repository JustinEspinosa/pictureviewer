package com.github.justinespinosa.pictureviewer;


import com.github.justinespinosa.textmode.curses.Curses;
import com.github.justinespinosa.textmode.curses.DefaultCursesFactory;
import com.github.justinespinosa.textmode.curses.lang.ColorChar;
import com.github.justinespinosa.textmode.curses.ui.ColorDepth;
import com.github.justinespinosa.textmode.curses.ui.ColorTable;
import com.github.justinespinosa.textmode.curses.ui.Position;
import com.github.justinespinosa.textmode.graphics.core.ASCIIPicture;
import com.github.justinespinosa.textmode.graphics.core.Bitmap;
import com.github.justinespinosa.textmode.graphics.core.Resolution;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ViewPicture {

    private static boolean VERBOSE = true;
    private static Curses CURSES ;

    private Bitmap bitmap;
    private Resolution resolution;
    private ColorTable colorTable;

    public ViewPicture(BufferedImage image, double ppc){
        bitmap = new Bitmap(image);
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

        int line = 0;
        for(Position p : picture.size()){

            if(p.getLine()>line){
                line = p.getLine();
                CURSES.print("\n");
            }

            ColorChar c = picture.get(p);
            CURSES.applyColorPair(c.getColors());
            CURSES.print(String.valueOf(c.getChr()));
        }
    }

    public static void main(String[] args) throws IOException {

        String termType = System.getenv("TERM");
        if(termType == null){
            System.err.println("Terminal is unknown. The TERM variable is not set.");
            return;
        }

        debug("Starting curses for "+ termType);

        CURSES = new DefaultCursesFactory().createCurses(termType);
        debug("CURSES has "+CURSES.numcolors()+" colors.");

        String filename = args[0];

        debug("Loading "+ filename);

        BufferedImage image = ImageIO.read(new File(filename));

        double ppc =  (double)image.getWidth()  /(double)CURSES.cols();

        if(args.length > 1){
            ppc = (double)image.getWidth()  /Double.parseDouble(args[1]);
        }

        debug("Resolution : "+ppc);

        ViewPicture vp = new ViewPicture(image, ppc);
        vp.display();

    }
}
