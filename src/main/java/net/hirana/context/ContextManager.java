package net.hirana.context;

import net.hirana.bridge.BridgeId;
import net.hirana.irc.parser.Message;
import net.hirana.irc.parser.RawMessage;

import java.io.IOException;
import java.util.*;

public enum ContextManager {
    INSTANCE;

    public List<String> usersConnected;
    private HashMap<String, String> lastUserNick = new HashMap<>();

    public List<String> onUserConnectedFromNewDevice(BridgeId id) throws IOException {
        List contextSnapshot = new ArrayList<>();
        if(!usersConnected.contains(id.getUser())) {
            IRContext.INSTANCE.newConnection(id);
            this.usersConnected.add(id.getUser());
        } else {
            contextSnapshot.addAll(UsersContext.INSTANCE.makeWelcomeContext(id, usersConnected.size()));
            contextSnapshot.addAll(UsersContext.INSTANCE.getChannelContext(id));
            contextSnapshot.addAll(UsersContext.INSTANCE.getChannelTopicContext(id));
            contextSnapshot.addAll(UsersContext.INSTANCE.getChannelUsersContext(id));
            contextSnapshot.addAll(UsersContext.INSTANCE.getQueuedMessages(id));
        }
        return contextSnapshot;
    }

    public void setNick(String user, String newNick) {
        this.lastUserNick.put(user, newNick);
    }

    public Optional<String> getLastNick(String user) {
        return Optional.ofNullable(this.lastUserNick.get(user));
    }

    public void ircMessage(String user, String message) {
        boolean deliveredMessage = BridgeContext.INSTANCE.deliverMessage(user, message);
        RawMessage raw = new RawMessage(message);
        if(deliveredMessage) {

        } else {
            UsersContext.INSTANCE.addToQueue(user, raw);
            // mensaje privado:
            if("PRIVMSG".equals(raw.code) && itsMe(user, raw.partials.get(2))) {
                FCMContext.INSTANCE.sendMessageToUser(user, String.format("@%s", raw.getOrigin().get().simplyOrigin), raw.content.get());
            } else if("PRIVMSG".equals(raw.code)) {
                Message msg = Message.parseMessage(raw, user, getLastNick(user));
                if(msg.haveMention) {
                    FCMContext.INSTANCE.sendMessageToUser(user, String.format("#%s - @%s", msg.channel, msg.author), msg.content);
                }
            }
        }
        UsersContext.INSTANCE.processContext(user, raw);
    }

    private boolean itsMe(String user, String nickCompare) {
        Optional<String> lastNick = getLastNick(user);
        return user.toLowerCase(Locale.ROOT).equals(nickCompare.toLowerCase(Locale.ROOT)) || lastNick.isPresent() && nickCompare.toLowerCase(Locale.ROOT).equals(lastNick.get().toLowerCase(Locale.ROOT));
    }

    public void newBridgeMessage(BridgeId id, String message) {
        if(message.indexOf("PUSH") == 0) {
            String token = message.substring(message.indexOf(" ")+1);
            FCMContext.INSTANCE.saveTokenForUser(id.getUser(), token);
            return;
        }
        // TODO: inteligencia en revisar que comandos son enviados
        IRContext.INSTANCE.sendFromUser(id.getUser(), message);
    }
}
