package net.hirana.bridge;

import net.hirana.context.BridgeContext;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class BridgeSocketService extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(BridgeSocketService.class);

    private final String hostName;
    private final int port;
    private final int timeoutSeconds = 3;
    private long connectionNumber = 0;

    public BridgeSocketService(int port, String hostName) throws UnknownHostException {
        super(new InetSocketAddress(port));
        this.port = port;
        this.hostName = hostName;
    }

    private void sendAsServer(WebSocket webSocket, String code, String content) {
        BridgeId id = webSocket.getAttachment();
        webSocket.send(String.format(":%s %s :%s", id.getHostName(), code, content));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        log.info(String.format("New connection requested from %s, connection #%d", webSocket.getRemoteSocketAddress().getAddress().getHostAddress(), connectionNumber));
        BridgeId bridgeId = new BridgeId(connectionNumber);
        bridgeId.setHostName(String.format("bouncer-%d.%s", connectionNumber, hostName));
        webSocket.setAttachment(bridgeId);
        connectionNumber++;
        sendHandshake(webSocket);
    }

    private void sendHandshake(WebSocket webSocket) {
        log.info("Sending handshake");
        sendAsServer(webSocket, "NOTICE *", "*** Welcome to Hirana Network Connection");
        sendAsServer(webSocket, "NOTICE *", "*** LOGIN REQUIRED USING HIRANA.NET ACCOUNT");
        sendAsServer(webSocket, "NOTICE *", "*** Please send /PASS <user>:<password>");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        BridgeId bridge = webSocket.getAttachment();
        log.info(String.format("%s has left the HNC [%d]:%s", webSocket.getRemoteSocketAddress().getAddress().getHostAddress(), bridge.connectionNumber, bridge.getUser()));
        if(bridge.isIdentified()) {
            BridgeContext.INSTANCE.disconnect(webSocket);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        BridgeId bridge = webSocket.getAttachment();
        String[] parts = s.split(" ");
        if(!bridge.isIdentified()) {
            // identify user
            if(s.indexOf("ENCODING") == 0) {
                log.debug("client send encoding");
                bridge.setEncoding(parts[1]);
            } else if(s.indexOf("USER") == 0) {
                log.debug(String.format("Client send user: %s", s));
                log.debug("Responding with password request.");
                bridge.setUser(s);
                sendAsServer(webSocket, "464", "Hnc requires password");
            } else if(s.indexOf("CAP") == 0) {
                log.debug("Client send capabilities");
                bridge.setRequiredCap();
            } else if(s.indexOf("NICK") == 0) {
                bridge.setNick(parts[1]);
            } else if(s.indexOf("PASS") == 0) {
                String conString = parts[1];
                String[] uparts = conString.split(":");
                if(uparts.length > 1) {
                    bridge.setUser(uparts[0]);
                    bridge.setPass(uparts[1]);
                } else {
                    bridge.setPass(conString);
                }
                if(!BridgeContext.INSTANCE.tryLogin(webSocket)) {
                    sendAsServer(webSocket, "NOTICE", "Invalid hirana account.");
                    webSocket.close();
                }
            }
        } else {
            BridgeContext.INSTANCE.bridgeIncomingMessage(bridge, s);
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        BridgeId bridge = webSocket.getAttachment();
        log.error(String.format("Error on bridge #%d for user #s module", bridge.connectionNumber, bridge.getUser()), e);
    }

    @Override
    public void onStart() {
        log.info(String.format("Server started in port %d - Timeout setted in %dms", port, timeoutSeconds));
        setConnectionLostTimeout(timeoutSeconds);
    }

    // TODO: comandos pendientes
    /*
    HQUIT

    HNCINFO
            sendAsServer(webSocket, "H01", ConnectionsService.INSTANCE.getNotificationTokenFromUser(udata.user));
            sendAsServer(webSocket, "H02", udata.nick);
            sendAsServer(webSocket, "H03", ConnectionsService.INSTANCE.isConnected(udata.user) ? "YES" : "NO");
            sendAsServer(webSocket, "H04", ConnectionsService.INSTANCE.getUserNick(udata.user));
     */
}
