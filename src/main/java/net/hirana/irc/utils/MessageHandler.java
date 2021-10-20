package net.hirana.irc.utils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class MessageHandler extends Thread{

    private static final Logger log = Logger.getLogger(MessageHandler.class);

    final IMessageCallback irc;
    private boolean disconnected = false;
    private boolean error = false;
    private Exception exception;

    public MessageHandler(IMessageCallback irc) {
        this.irc = irc;
    }

    @Override
    public void run() {
        log.debug("Starting read thread.");
        try {
            this.irc.onReady();
            while(!this.disconnected && !this.error) {
                this.irc.onMessageReceived(this.irc.getBufferedReader().readLine());
            }
        } catch (IOException e) {
            this.exception = e;
            this.error = true;
            log.error("IOException reading input stream", e);
            this.irc.onError(e);
        }
    }

    public void close() {
        this.disconnected = true;
    }

}
