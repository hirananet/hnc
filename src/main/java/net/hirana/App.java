package net.hirana;

import net.hirana.irc.IRClient;
import net.hirana.irc.utils.MessageHandler;
import net.hirana.websocket.HncServer;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger log = Logger.getLogger(App.class);

    public static void main( String[] args )
    {
        try {
            HncServer wsServer = new HncServer(7000);
            wsServer.start();
            while(true) {}
        } catch (UnknownHostException e) {
            log.error("Error connecting to websocket", e);
        }

    }
}
