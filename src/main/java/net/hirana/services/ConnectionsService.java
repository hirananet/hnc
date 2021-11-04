package net.hirana.services;

import net.hirana.irc.IRClient;
import net.hirana.push.FCMService;
import net.hirana.push.PushNotificationRequest;
import net.hirana.websocket.WsData;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public enum ConnectionsService {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(ConnectionsService.class);

    private final String networkHost = "irc.hirana.net";
    private final Integer networkPort = 6667;
    private Map<String, IRClient> clientsOfUsers = new HashMap<>();
    private Map<String, List<WsData>> wsList = new HashMap<>();
    private Map<String, List<String>> queuedMessages = new HashMap<>();
    private Map<String, String> lastNick = new HashMap<>();
    private Map<String, Date> lastNotificationSended = new HashMap<>();

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
        Redis.INSTANCE.setValue(String.format("FCM-%s", user), token);
    }

    public String getNotificationTokenFromUser(String user) {
        return Redis.INSTANCE.exists(user) ? Redis.INSTANCE.getValue(String.format("FCM-%s", user)) : "NotDefined";
    }

    public boolean isConnected(String user) {
        return clientsOfUsers.containsKey(user);
    }

    public String getUserNick(String user) {
        if(!isConnected(user)) {
            return "N/A";
        }
        return clientsOfUsers.get(user).getNick();
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
                        log.error("Detected socket memory leak?");
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
        String initialSender = message.substring(0, message.indexOf("!"));
        if(privmsgIdx > 0 && Redis.INSTANCE.exists(String.format("FCM-%s", user))) {
            String msg = message.substring(privmsgIdx+1);
            String nickOrChannel = msg.split(" ")[1];
            String content = msg.substring(msg.indexOf(":")+1);
            boolean send = false;
            boolean isChannel = "#".equals(nickOrChannel.substring(0,1));
            log.info("SEND NOTIFICATION FROM "+nickOrChannel+" TO " + user);
            if(isChannel) {
                // is channel
                String pingNick = lastNick.get(user);
                String pingUser = user;
                String regex = String.format("(^|\\s)%s(\\s|$)",pingNick);
                boolean pingToNick = Pattern.compile(regex).matcher(content).find();
                if(pingToNick) {
                    send = true;
                } else {
                    regex = String.format("(^|\\s)%s(\\s|$)",pingUser);
                    boolean pingToUser = Pattern.compile(regex).matcher(content).find();
                    if(pingToUser) {
                        send = true;
                    }
                }
            } else {
                // is private message:
                send = true;
            }
            if(send) {
                if(!lastNotificationSended.containsKey(user)) {
                    lastNotificationSended.put(user, new Date());
                } else {
                    Date now = new Date();
                    if(lastNotificationSended.get(user).getTime() + 20000 < now.getTime()) { // pasaron 10 secs desde la ultima notificacion?
                        lastNotificationSended.put(user, now);
                    } else {
                        send = false;
                    }
                }
            }
            if(send) {
                PushNotificationRequest request = new PushNotificationRequest();
                request.setMessage(content);
                request.setTitle(isChannel ? String.format("%s - %s", nickOrChannel, initialSender) : nickOrChannel);
                request.setToken(Redis.INSTANCE.getValue(String.format("FCM-%s", user)));
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
