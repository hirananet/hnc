package net.hirana;

import net.hirana.bridge.BridgeSocketService;
import net.hirana.context.ContextManager;
import net.hirana.context.IRContext;
import net.hirana.utils.DatabaseService;
import net.hirana.utils.push.FCMInitializer;
import net.hirana.utils.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        log.info("Starting Hirana Network Connection - Bouncer");
        try {
            log.info("Connecting to redis");
            RedisService.INSTANCE.init();
        } catch (Exception e) {
            log.error("Can't connect redis database", e);
            return;
        }
        try {
            log.info("Connecting to Mysql - Database");
            DatabaseService.INSTANCE.init();
        } catch(SQLException exc) {
            log.error("Can't connect sql database", exc);
            return;
        }
        log.info("Initializating FCM");
        FCMInitializer.INSTANCE.init("/hirana-firebase.json");
        log.info("Initializing IRContext");
        IRContext.INSTANCE.init("irc.hirana.net", 6667);
        try {
            int port = 7000;
            String hostname = "v2-bouncer.d.hirana.net";
            log.info(String.format("Starting bridge hostname [%s] sockets in port [%d]", hostname, port));
            BridgeSocketService wsServer = new BridgeSocketService(port, hostname);
            wsServer.start();
            log.info("Finish to started Hirana Network Connection | main thread in sleep.");
            while(true) {}
        } catch (UnknownHostException e) {
            log.error("Error connecting to websocket", e);
        }

    }
}
