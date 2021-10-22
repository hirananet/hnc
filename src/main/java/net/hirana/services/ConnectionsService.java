package net.hirana.services;

import net.hirana.irc.IRClient;
import net.hirana.push.FCMService;
import net.hirana.push.PushNotificationRequest;
import net.hirana.websocket.WsData;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ConnectionsService {
    INSTANCE;

    private static final Logger log = Logger.getLogger(ConnectionsService.class);

    private final String networkHost = "irc.hirana.net";
    private final Integer networkPort = 6667;
    private Map<String, IRClient> clientsOfUsers = new HashMap<>();
    private Map<String, List<WsData>> wsList = new HashMap<>();
    private Map<String, List<String>> queuedMessages = new HashMap<>();
    private Map<String, String> tokens = new HashMap<>();
    private Map<String, String> lastNick = new HashMap<>();

    public boolean existsConnection(String user) {
        return clientsOfUsers.containsKey(user);
    }

    public void setLastNick(String user, String nick) {
        lastNick.put(user, nick);
    }

    public IRClient getConnection(String user, String nick) throws IOException {
        if(clientsOfUsers.containsKey(user)) {
            return clientsOfUsers.get(user);
        }
        log.debug( String.format("Starting connection to %s:%d", networkHost, networkPort) );
        IRClient client = new IRClient(networkHost, networkPort, user, nick);
        clientsOfUsers.put(user, client);
        queuedMessages.put(user, new ArrayList<>());
        return client;
    }

    public void assocWsWithUser(WebSocket ws, String user) {
        if(wsList.containsKey(user)) {
            wsList.get(user).add(new WsData(ws));
        } else {
            List<WsData> wss = new ArrayList<>();
            wss.add(new WsData(ws));
            wsList.put(user, wss);
        }
    }

    public void setNotificationTokenToUser(String user, String token) {
        log.debug(String.format("Setting push notification token for %s : %s",user,token));
        tokens.put(user, token);
    }

    public void disassocWsWithUser(final Long connNumber, String user) {
        if(wsList.containsKey(user)) {
            wsList.get(user).removeIf(l -> l.connNumber == connNumber);
        }
    }

    public void resendMessage(String user, final String message) {
        if(wsList.containsKey(user)) {
            List<WsData> ls = wsList.get(user);
            if(ls.size() > 0) {
                ls.forEach(ws -> {
                    if(ws.ws.isOpen()) {
                        ws.ws.send(message);
                    } else {
                        log.info("Detected socket memory leak?");
                    }
                });
            } else {
                log.info("Adding to queue for " + user + ": " + message);
                addMessageToQueue(user, message);
            }
        }
    }

    private void addMessageToQueue(String user, String message) {
        if(queuedMessages.get(user) == null) {
            queuedMessages.put(user, new ArrayList<>());
        }
        queuedMessages.get(user).add(message);
        // enviar notificacion?
        int privmsgIdx = message.indexOf(" PRIVMSG ");
        if(privmsgIdx > 0 &&
           tokens.containsKey(user)
        ) {
            String msg = message.substring(privmsgIdx+1);
            String nickOrChannel = msg.split(" ")[0];
            String content = msg.substring(msg.indexOf(":")+1);
            log.info("SEND NOTIFICATION OF "+nickOrChannel);
            if("#".equals(nickOrChannel.substring(0,1))) {
                // is channel

            } else {
                // is private message:
                PushNotificationRequest request = new PushNotificationRequest();
                request.setMessage(content);
                request.setTitle(nickOrChannel);
                request.setToken(tokens.get(user));
                log.debug(String.format("Sending notification of %s to %s", nickOrChannel, user));
                try {
                    FCMService.INSTANCE.sendMessageToToken(request);
                } catch (Exception e) {
                    log.error("Can't send notification", e);
                }
            }
        }
    }

    public void sendQueuedMessages(String user, final WebSocket ws) {
        if(this.queuedMessages.get(user) == null || queuedMessages.get(user).size() == 0) return;
        queuedMessages.get(user).forEach(m -> {
            ws.send(m);
            log.info(">>> Queue Sending " + m);
        });
        queuedMessages.get(user).clear();
    }

    public void clearConnection(String user) throws IOException {
        if(clientsOfUsers.containsKey(user)) {
            IRClient irc = clientsOfUsers.get(user);
            clientsOfUsers.remove(user);
            irc.close();
        }
    }
}
