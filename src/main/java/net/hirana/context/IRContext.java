package net.hirana.context;

import net.hirana.bridge.BridgeId;
import net.hirana.irc.IRClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum IRContext {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(IRContext.class);

    private Map<String, IRClient> clientsOfUsers = new HashMap<>();
    private String networkHost;
    private Integer networkPort;

    public void init(String networkHost, Integer networkPort) {
        this.networkHost = networkHost;
        this.networkPort = networkPort;
    }

    public Optional<IRClient> getConnection(String user) {
        if(clientsOfUsers.containsKey(user)) {
            return Optional.of(clientsOfUsers.get(user));
        } else {
            return Optional.empty();
        }
    }

    public IRClient newConnection(BridgeId bridge) throws IOException {
        if(!clientsOfUsers.containsKey(bridge.getUser())) {
            log.info( String.format("Starting connection to %s:%d", networkHost, networkPort) );
            IRClient client = new IRClient(networkHost, networkPort, bridge.getUser(), bridge.getNick());
            clientsOfUsers.put(bridge.getUser(), client);
        }
        return clientsOfUsers.get(bridge.getUser());
    }

    public boolean sendFromUser(String user, String message) {
        Optional<IRClient> client = getConnection(user);
        if(client.isPresent()) {
            try {
                client.get().sendMessage(message);
                return true;
            } catch (IOException e) {
                log.error("Error sending message from user " + user, e);
                return false;
            }
        } else {
            log.error(String.format("Trying to send message from user %s to unconnected irc client", user));
            // TODO: pending reconnections
            return false;
        }
    }
}
