package net.hirana.websocket;

import net.hirana.irc.IRClient;
import net.hirana.irc.parser.Channel;
import net.hirana.services.ConnectionsService;
import net.hirana.services.ContextService;
import net.hirana.services.Database;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HncServer extends WebSocketServer {

    private static final String hostName = "hnc.hirana.net";

    private static final Logger log = LoggerFactory.getLogger(IRClient.class);

    private final int port;
    private final int timeoutSeconds = 3;
    private long connectionNumber = 0;
    private Map<Long, UserData> udatas = new HashMap();

    private static final Database db = Database.INSTANCE;

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
        sendAsServer(ws, "001", "Welcome to HNC Bridge.");
        sendAsServer(ws, "002", "Your host is HCNBridge, running version Alpha.");
        sendAsServer(ws, "003", "This server was created now");
        sendAsServer(ws, "004", "N/A.");
        sendAsServer(ws, "251", ":There are Unknown users and 1 invisible on Unknown servers");
        sendAsServer(ws, "252", "Unknown :operator(s) online");
        sendAsServer(ws, "253", "Unknown :unknown connections");
        sendAsServer(ws, "254", "Unknown :channels formed");
        sendAsServer(ws, "375", "hnc.hirana.net message of the day.");
        sendAsServer(ws, "375", "Welcome to HCN Bridge.");
        sendAsServer(ws, "376", "end message of the day.");
        sendAsServer(ws, "396", "HND-UNKNOWN.IP :is now your displayed host.");
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
            if(udata.irc != null) {
                try {
                    udata.irc.sendMessage(String.format("NICK %s", udata.nick));
                    ContextService.INSTANCE.setLastNick(udata.user != null ? udata.user : udata.nick, udata.nick);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if(s.indexOf("PASS") == 0) {
            String userpass = parts.length > 1 ? parts[1] : "";
            String[] uparts = userpass.split(":");
            udata.user = uparts[0];
            ContextService.INSTANCE.setLastNick(udata.user, udata.nick != null ? udata.nick : udata.user);
            udata.password = uparts[1];
            log.info("Loging with: " + udata.user);
            try {
                String bcryptPassword = db.getUserPassword(udata.user);
                if(bcryptPassword == null) {
                    log.debug("Account doesn't exists.");
                    sendAsServer(webSocket, "NOTICE", "Account doesn't exists.");
                    webSocket.close();
                    return;
                }
                String[] hashData = bcryptPassword.split(":");
                if(hashData[0] == "bcrypt") {
                    log.debug("Unsupported account version.");
                    sendAsServer(webSocket, "NOTICE", "Unsupported account version.");
                    webSocket.close();
                    return;
                }
                if(!BCrypt.checkpw(udata.password, hashData[1])) {
                    log.debug("Invalid password.");
                    sendAsServer(webSocket, "NOTICE", "Invalid password.");
                    webSocket.close();
                    return;
                }
                udata.identified = true;
                boolean sendFakeMotd = ConnectionsService.INSTANCE.existsConnection(udata.user);
                log.debug(String.format("Exists connection %s %s", udata.user, sendFakeMotd ? "YES" : "NO"));
                udata.irc = ConnectionsService.INSTANCE.getConnection(udata.user, udata.nick, true);
                ConnectionsService.INSTANCE.assocWsWithUser(webSocket, udata.user);
                List<Channel> channels = ContextService.INSTANCE.getChannels(udata.user);
                if(channels != null) {
                    String channelSTR = "";
                    for (Channel chan: channels) {
                        channelSTR = chan.hashedName + " ";
                    }
                    sendAsServer(webSocket, "319", channelSTR);
                }
                ConnectionsService.INSTANCE.readOldMessagesFromRedis(webSocket, udata.user);
                if (sendFakeMotd) {
                    this.fakeStartSequence(webSocket);
                }
            } catch (IOException e) {
                log.error("Cannot create connection with IRC", e);
                sendAsServer(webSocket, "NOTICE", "Error creating bridge with irc.hirana.net");
                webSocket.close();
            } catch (SQLException sql) {
                log.error("Error querying", sql);
                sendAsServer(webSocket, "NOTICE", "Cannot validate your user account");
                webSocket.close();
            }
        } else if(s.indexOf("HQUIT") == 0) { // force irc disconnection
            if(!udata.identified) {
                webSocket.close();
                return;
            }
            try {
                ConnectionsService.INSTANCE.clearConnection(udata.user);
                webSocket.close();
            } catch (IOException e) {
                log.info(String.format("Can't clear instance of user %s", udata.user), e);
            }
        } else if(s.indexOf("HNCINFO") == 0) { // get information about connection
            if(!udata.identified) {
                webSocket.close();
                return;
            }
            sendAsServer(webSocket, "H01", ConnectionsService.INSTANCE.getNotificationTokenFromUser(udata.user));
            sendAsServer(webSocket, "H02", udata.nick);
            sendAsServer(webSocket, "H03", ConnectionsService.INSTANCE.isConnected(udata.user) ? "YES" : "NO");
            sendAsServer(webSocket, "H04", ConnectionsService.INSTANCE.getUserNick(udata.user));
        } else if(s.indexOf("PUSH") == 0) {
            String notification = s.substring(s.indexOf(" ")+1);
            ConnectionsService.INSTANCE.setNotificationTokenToUser(udata.user, notification);
        } else {
            if(!udata.identified) {
                webSocket.close();
                return;
            }
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
