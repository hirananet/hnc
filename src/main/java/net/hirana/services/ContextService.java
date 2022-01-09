package net.hirana.services;

import net.hirana.irc.parser.Channel;
import net.hirana.irc.parser.RawMessage;
import net.hirana.irc.parser.SimplyOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ContextService {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(ContextService.class);

    private final ConnectionsService connSrv = ConnectionsService.INSTANCE;

    private Map<String, String> lastNick = new HashMap<>();
    private Map<String, List<Channel>> channels = new HashMap<>();

    public void processContext(String user, String message) {
        RawMessage raw = new RawMessage(message);
        if("TOPIC".equals(raw.code)) {
            // topic changed
            this.topic(user, raw);
        }
        if( "332".equals(raw.code)) {
            this.topicChanged(user, raw);
        }
        if("NICK".equals(raw.code)) {
            this.nickChange(user, raw);
        }
        if("319".equals(raw.code)) {
            // channel list
            this.channelList(user, raw);
        }
        if("JOIN".equals(raw.code)) {
            this.join(user, raw);
        }
        if("PART".equals(raw.code)) {
            this.part(user, raw);
        }
        if("KICK".equals(raw.code)) {
            this.kick(user, raw);
        }
        if("QUIT".equals(raw.code)) {
            this.quit(user, raw);
        }
    }

    private void topic(String user, RawMessage message) {
        Channel chan = new Channel(message.partials.get(2));
        String topic = message.content.get();
        setTopicToChannel(user, chan.hashedName, topic);
    }

    private void topicChanged(String user, RawMessage message) {
        Pattern topicPattern = Pattern.compile("#([^\\s]+)");
        Matcher topicMatcher = topicPattern.matcher(message.raw);
        if(!topicMatcher.find()) {
            log.error("Error parsing topic" + message.raw);
            return;
        }
        Channel chan = new Channel(topicMatcher.group(1));
        String topic = message.content.get();
        setTopicToChannel(user, chan.hashedName, topic);
    }

    private void nickChange(String user, RawMessage message) {
        String newNick = message.partials.size() > 2 ? message.partials.get(2) : message.content.get();
        SimplyOrigin originalNick = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        Optional<String> nick = getLastNick(user);
        if(!nick.isPresent() || nick.get().equals(originalNick)) {
            setLastNick(user, newNick);
        }
    }

    private void channelList(String user, RawMessage message) {
        for(String chRaw: message.content.get().split(" ")) {
            Channel chnl = new Channel(chRaw);
            joinedToChannel(user, chnl.hashedName);
        }
    }

    private void join(String user, RawMessage message) {
        SimplyOrigin userJoinded = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        Channel channel = new Channel(message.content.isPresent() ? message.content.get() : message.partials.get(2));
        Optional<String> nick = getLastNick(user);
        if(!nick.isPresent() || userJoinded.nick.equals(nick.get())) {
            joinedToChannel(user, channel.hashedName);
        }
    }

    private void part(String user, RawMessage message) {
        SimplyOrigin userParted = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        Optional<String> nick = getLastNick(user);
        if(!nick.isPresent() || userParted.nick.equals(nick.get())) {
            Channel channel = new Channel(message.partials.size() > 2 ? message.partials.get(2) : message.content.get());
            leaveToChannel(user, channel.hashedName);
        }
    }

    private void kick(String user, RawMessage message) {
        Channel chan = new Channel(message.partials.get(2));
        Pattern kickDataPattern = Pattern.compile("#([^\\s]+)\\s([^:]+)\\s");
        Matcher kickDataMatcher = kickDataPattern.matcher(message.raw);
        if(!kickDataMatcher.find()) {
            log.error("Error parsing kick data", message.raw);
            return;
        }
        SimplyOrigin userKicked = SimplyOrigin.parseUser(kickDataMatcher.group(2));
        Optional<String> nick = getLastNick(user);
        if(!nick.isPresent() || userKicked.nick.equals(nick.get())) {
            leaveToChannel(user, chan.hashedName);
        }
    }

    private void quit(String user, RawMessage message) {
        SimplyOrigin userQuitted = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        Optional<String> nick = getLastNick(user);
        if(userQuitted.nick.equals(nick.get())) {
            // this can't be
        }
    }

    public void setLastNick(String user, String nick) {
        lastNick.put(user, nick);
    }

    public Optional<String> getLastNick(String user) {
        return Optional.ofNullable(this.lastNick.get(user));
    }

    public void joinedToChannel(String user, String channelHash) {
        Optional<Channel> channel = channels.get(user).stream().filter(chan -> chan.hashedName.equals(channelHash)).findFirst();
        if (!channel.isPresent()) {
            channels.get(user).add(new Channel(channelHash));
        } else {
            log.error(String.format("@%s You are previous joined to channel %s", user, channelHash));
        }
    }

    public void leaveToChannel(String user, String channelHash) {
        channels.get(user).removeIf(chan -> chan.hashedName.equals(channelHash));
    }

    public void setTopicToChannel(String user, String channelHash, String topic) {
        Optional<Channel> channel = channels.get(user).stream().filter(chan -> chan.hashedName.equals(channelHash)).findFirst();
        if (channel.isPresent()) {
            channel.get().topic = topic;
        } else {
            log.error(String.format("@%s Can't set topic on non existant channel %s", user, channelHash));
        }
    }
}
