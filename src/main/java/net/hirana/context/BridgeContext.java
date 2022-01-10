package net.hirana.context;

import net.hirana.bridge.BridgeId;
import net.hirana.utils.DatabaseService;
import org.java_websocket.WebSocket;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public enum BridgeContext {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(BridgeContext.class);

    private DatabaseService db = DatabaseService.INSTANCE;
    private HashMap<String, List<WebSocket>> bridges;

    public boolean tryLogin(WebSocket websocket) {
        BridgeId bridge = websocket.getAttachment();
        try {
            String bcryptPassword = db.getUserPassword(bridge.getUser());
            if(bcryptPassword == null) {
                log.debug("Account doesn't exists.");
                return false;
            }
            String[] hashData = bcryptPassword.split(":");
            if(hashData[0] == "bcrypt") {
                log.error(String.format("Unsupported account version for user %s.", bridge.getUser()));
                return false;
            }
            if(!BCrypt.checkpw(bridge.getPass(), hashData[1])) {
                log.debug("Invalid password.");
                return false;
            }
            log.info(String.format("New bridge identified for user %s", bridge.getUser()));
            bridge.setIdentified();
            if(bridges.get(bridge.getUser()) == null) {
                bridges.put(bridge.getUser(), new ArrayList<>());
            }
            bridges.get(bridge.getUser()).add(websocket);
            try {
                List<String> context = ContextManager.INSTANCE.onUserConnectedFromNewDevice(bridge);
                for (String msg : context) {
                    websocket.send(msg);
                }
            } catch (IOException e) {
                log.error(String.format("Error trying to establish connection to hirana for user %s", bridge.getUser()), e);
                return false;
            }
            return true;
        } catch (SQLException e) {
            log.error("Error SQL on login ", e);
            return false;
        }
    }

    public void disconnect(WebSocket websocket) {
        BridgeId bridge = websocket.getAttachment();
        bridges.get(bridge.getUser()).remove(websocket);
    }

    public void bridgeIncomingMessage(BridgeId bridge, String message) {
        ContextManager.INSTANCE.newBridgeMessage(bridge, message);
    }

    public boolean deliverMessage(String user, String message) {
        if(bridges.get(user) == null) {
            return false;
        }
        if(bridges.get(user).size() == 0) {
            return false;
        }
        for (WebSocket webSocket : bridges.get(user)) {
            webSocket.send(message);
        }
        return true;
    }

}
