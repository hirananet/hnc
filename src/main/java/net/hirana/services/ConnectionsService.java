package net.hirana.services;

import net.hirana.irc.IRClient;
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

    public boolean existsConnection(String user) {
        return clientsOfUsers.containsKey(user);
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

    public void disassocWsWithUser(final Long connNumber, String user) {
        if(wsList.containsKey(user)) {
            wsList.get(user).removeIf(l -> l.connNumber == connNumber);
        }
    }

    public void resendMessage(String user, final String message) {
        if(wsList.containsKey(user)) {
            List<WsData> ls = wsList.get(user);
            if(ls.size() > 0) {
                ls.forEach(ws -> ws.ws.send(message));
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
