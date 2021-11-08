package net.hirana.irc;

import net.hirana.irc.utils.IMessageCallback;
import net.hirana.irc.utils.MessageHandler;
import net.hirana.services.ConnectionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class IRClient implements IMessageCallback {

    private static final Logger log = LoggerFactory.getLogger(IRClient.class);

    private final String host;
    private final String ip;
    private final Integer puerto;
    private final Socket socket;
    private final DataOutputStream bufferOut;
    private final BufferedReader bufferIn;
    private final MessageHandler handler;
    private final String nick;
    private final String user;
    private boolean connected;

    public IRClient(String host, Integer puerto, String user, String nick) throws IOException {
        this.host = host;
        this.puerto = puerto;
        this.nick = nick;
        this.user = user;
        InetAddress ip = InetAddress.getByName(host);
        this.ip = ip.getHostAddress();
        this.socket = new Socket(ip, puerto);
        this.bufferOut = new DataOutputStream(this.socket.getOutputStream());
        this.bufferIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.handler = new MessageHandler(this);
        this.handler.start();
        this.connected = true;
    }

    public void sendMessage(String message) throws IOException {
        log.debug(String.format("Output > %s", message));
        this.bufferOut.write(String.format("%s%s",message,"\r\n").getBytes());
    }

    public void sendConnectionLine() throws IOException {
        this.sendMessage("ENCODING UTF-8");
        this.sendMessage(String.format("NICK %s", nick));
        this.sendMessage(String.format("USER %s * * :%s HncBouncer", nick, nick));
    }

    @Override
    public BufferedReader getBufferedReader() {
        return this.bufferIn;
    }

    @Override
    public void onReady() {
        try {
            this.sendConnectionLine();
        } catch (IOException e) {
            log.error("Error sending connection line", e);
        }
    }

    @Override
    public void onMessageReceived(String message) {
        log.debug(String.format("Message received: %s", message));
        try {
            if(message.indexOf("PING") == 0) {
                this.sendMessage("PONG " + message.substring(5));
            } else {
                ConnectionsService.INSTANCE.resendMessage(this.user, message);
            }
        } catch (Exception e) {
            log.error("Exception processing message: ", e);
        }
    }

    public void close() throws IOException {
        this.socket.close();
        this.closeAll();
    }

    @Override
    public void onError(Exception e) {
        this.connected = false;
        this.closeAll();
    }

    public boolean isConnected() {
        return this.connected;
    }

    private void closeAll() {
        this.handler.close();
        try {
            this.bufferOut.close();
        } catch (IOException ioException) {
            log.error("IOException closing output buffer", ioException);
        }
        try {
            this.bufferIn.close();
        } catch (IOException ioException) {
            log.error("IOException closing input buffer", ioException);
        }
    }

    public String getNick() {
        return this.nick;
    }

}
