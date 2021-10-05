package com.github.justinespinosa.pictureviewer;

import com.github.justinespinosa.textmode.curses.Curses;
import com.github.justinespinosa.textmode.curses.CursesFactory;
import com.github.justinespinosa.textmode.curses.net.GeneralSocketIO;
import com.github.justinespinosa.textmode.curses.net.SocketIO;
import com.github.justinespinosa.textmode.curses.net.TelnetServer;
import org.apache.commons.cli.*;

import javax.net.ssl.SSLContext;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

public class ViewPictureTcp {
    private final ViewPicture viewPicture;
    private CursesFactory factory;

    private ViewPictureTcp() throws IOException{
        factory = CursesFactory.getInstance();
        viewPicture = new ViewPicture(ViewPicture.createImage7ob(), true, 3, 256);
    }

    private void handleOne(SocketChannel client){
        try {
            Curses curses = factory.createCurses(factory.createTerminal("xterm", new GeneralSocketIO(client)));
            synchronized (viewPicture) {
                System.out.println("Displaying image ");
                viewPicture.display(curses);
                System.out.println("Done displaying ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void acceptConnections(int port) throws IOException {
        System.out.println("Accepting connection son port "+port);
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(port));

        SocketChannel candidate;
        while((candidate = server.accept()) != null) {
            final SocketChannel client = candidate;
            System.out.println("New connection ");
            CompletableFuture.runAsync(() -> handleOne(client));
        }
    }

    public static void main(String[] args) throws IOException {
        new ViewPictureTcp().acceptConnections(Integer.parseInt(args[0]));
    }

}
