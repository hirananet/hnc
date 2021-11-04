package net.hirana;

import net.hirana.push.FCMInitializer;
import net.hirana.services.Database;
import net.hirana.services.Redis;
import net.hirana.websocket.HncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.SQLException;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {
        try {
            Redis.INSTANCE.init();
        } catch (Exception e) {
            log.error("Can't connection redis database", e);
            return;
        }
        try {
            Database.INSTANCE.init();
        } catch(SQLException exc) {
            log.error("Can't connection sql database", exc);
            return;
        }
        FCMInitializer.INSTANCE.init("/hirana-firebase.json");
        try {
            HncServer wsServer = new HncServer(7000);
            wsServer.start();
            while(true) {}
        } catch (UnknownHostException e) {
            log.error("Error connecting to websocket", e);
        }

    }
}
