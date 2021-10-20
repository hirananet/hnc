package net.hirana;

import net.hirana.irc.IRClient;
import net.hirana.irc.utils.MessageHandler;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger log = Logger.getLogger(App.class);

    public static void main( String[] args )
    {
        String host = "irc.hirana.net";
        int port = 6667;
        log.info( String.format("Starting connection to %s:%d", host, port) );
        try {
            IRClient client = new IRClient(host, port);
            //client.sendConnectionLine("HNCTest");
            while(client.isConnected()) {

            }
            log.info("Disconnected.");
        } catch (IOException e) {
            log.error("Error connecting ", e);
        }

    }
}
