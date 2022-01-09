package net.hirana.services;

import net.hirana.irc.parser.RawMessage;
import net.hirana.irc.parser.SimplyOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ContextService {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(ContextService.class);

    private final ConnectionsService connSrv = ConnectionsService.INSTANCE;

    public void processContext(String user, String message) {
        RawMessage raw = new RawMessage(message);
        if("TOPIC".equals(raw.code) || "332".equals(raw.code)) {
            // topic changed
        }
        if("NICK".equals(raw.code)) {
            this.nickChange(user, raw);
        }
        if("319".equals(raw.code)) {
            // channel list
        }
        if("JOIN".equals(raw.code)) {

        }
        if("PART".equals(raw.code)) {

        }
        if("KICK".equals(raw.code)) {

        }
        if("QUIT".equals(raw.code)) {

        }
    }

    public void nickChange(String user, RawMessage message) {
        String newNick = message.partials.size() > 2 ? message.partials.get(2) : message.content.get();
        SimplyOrigin originalNick = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        if(connSrv.getLastNick(user).equals(originalNick)) {
            connSrv.setLastNick(user, newNick);
        }
    }
}
