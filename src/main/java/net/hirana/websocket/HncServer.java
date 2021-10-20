package net.hirana.websocket;

import net.hirana.irc.IRClient;
import net.hirana.services.ConnectionsService;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class HncServer extends WebSocketServer {

    private static final String hostName = "hnc.hirana.net";

    private static final Logger log = Logger.getLogger(IRClient.class);

    private final int port;
    private final int timeoutSeconds = 3;
    private long connectionNumber = 0;
    private Map<Long, UserData> udatas = new HashMap();

    public HncServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
        this.port = port;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        log.info(String.format("New connection received from %s with number %d", webSocket.getRemoteSocketAddress().getAddress().getHostAddress(), connectionNumber));
        sendAsServer(webSocket, "NOTICE *", "*** Welcome to Hirana Network Connection");
        sendAsServer(webSocket, "NOTICE *", "*** LOGIN REQUIRED USING HIRANA.NET ACCOUNT");
        sendAsServer(webSocket, "NOTICE *", "*** Please send /PASS <user>:<password>");
        webSocket.setAttachment(connectionNumber);
        udatas.put(connectionNumber, new UserData());
        connectionNumber++;
    }

    public void sendAsServer(WebSocket webSocket, String code, String content) {
        webSocket.send(String.format(":%s %s :%s", hostName, code, content));
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        log.info(String.format("%s has left the HNC [%d]", webSocket.getRemoteSocketAddress().getAddress().getHostAddress(), webSocket.getAttachment()));
        ConnectionsService.INSTANCE.disassocWsWithUser(webSocket.getAttachment(), udatas.get(webSocket.<Long>getAttachment()).user);
    }

    private void fakeStartSequence(WebSocket ws) {
        sendAsServer(ws, "001", "Welcome to HCN Bridge.");
        sendAsServer(ws, "002", "Your host is HCNBridge, running version Alpha.");
        sendAsServer(ws, "003", "This server was created now");
        sendAsServer(ws, "004", "N/A.");
        sendAsServer(ws, "251", ":There are 0 users and 1 invisible on 1 servers");
        sendAsServer(ws, "252", "0 :operator(s) online");
        sendAsServer(ws, "253", "1 :unknown connections");
        sendAsServer(ws, "254", "1 : channels formed");
        sendAsServer(ws, "375", "hnc.hirana.net message of the day.");
        sendAsServer(ws, "375", "Welcome to HCN Bridge.");
        sendAsServer(ws, "376", "end message of the day.");
        sendAsServer(ws, "396", "HCN-sui.3d1.88.99.IP :is now your displayed host.");
        ConnectionsService.INSTANCE.sendQueuedMessages(
                udatas.get(ws.<Long>getAttachment()).user,
                ws
        );
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        log.info(String.format("Message received: %s", s));
        String[] parts = s.split(" ");
        UserData udata = udatas.get(webSocket.<Long>getAttachment());
        if(s.indexOf("ENCODING") == 0) {
            log.info("setting encoding");
            udata.encoding = s.split(" ")[1];
        } else if(s.indexOf("USER") == 0) {
            log.info(webSocket + "user:: " + s);
            webSocket.send(":%s");
            sendAsServer(webSocket, "464", "Hnc requires password");
        } else if(s.indexOf("CAP") == 0) {
            log.info("Requesting capabilities");
            udata.requestedCaps = true;
        } else if(s.indexOf("NICK") == 0) {
            log.info("Setting new nick");
            udata.nick = s.split(" ")[1];
        } else if(s.indexOf("PASS") == 0) {
            String userpass = parts.length > 1 ? parts[1] : "";
            String[] uparts = userpass.split(":");
            log.info("Loging with: " + userpass);
            udata.user = uparts[0];
            udata.password = uparts[1];
            // TODO: validate user and password:


            try {
                boolean sendFakeMotd = !ConnectionsService.INSTANCE.existsConnection(udata.nick);
                udata.irc = ConnectionsService.INSTANCE.getConnection(udata.user, udata.nick);
                ConnectionsService.INSTANCE.assocWsWithUser(webSocket, udata.user);
                if(sendFakeMotd) {
                    this.fakeStartSequence(webSocket);
                }
            } catch (IOException e) {
                log.error("Cannot create connection with IRC");
                sendAsServer(webSocket, "NOTICE", "Error creating bridge with irc.hirana.net");
                webSocket.close();
            }
        } else {
            if(udata.irc != null) {
                try {
                    udata.irc.sendMessage(s);
                } catch (IOException e) {
                    log.error("Error bridging message", e);
                    try {
                        ConnectionsService.INSTANCE.clearConnection(udata.user);
                    } catch (IOException ioException) {
                        log.error("Error clearing connection to irc", ioException);
                    }
                    sendAsServer(webSocket, "NOTICE", "Error creating bridge with irc.hirana.net");
                    webSocket.close();
                }
            } else {
                log.error("!!!!!!!!!!!! Trying to send to non connected: ");
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        log.error("Error on websocket module", e);
    }

    @Override
    public void onStart() {
        log.info(String.format("Server started in port %d - Timeout setted in %dms", port, timeoutSeconds));
        setConnectionLostTimeout(timeoutSeconds);
    }
}