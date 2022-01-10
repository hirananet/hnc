package net.hirana.context;

import net.hirana.bridge.BridgeId;
import net.hirana.irc.parser.Channel;
import net.hirana.irc.parser.RawMessage;
import net.hirana.irc.parser.SimplyOrigin;
import net.hirana.utils.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum UsersContext {
    INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(UsersContext.class);

    private HashMap<String, List<String>> channelsForUser = new HashMap<>();
    private HashMap<String, HashMap<String, List<String>>> usersInChannel = new HashMap<>();
    private HashMap<String, HashMap<String, String>> topicsInChannels = new HashMap<>();


    public List<String> makeWelcomeContext(BridgeId id, long usersConnected) {
        List<String> context = new ArrayList();
        log.debug("Sending welcome context to user" + id.getUser());
        context.add(String.format(":%s %s :%s", id.getHostName(), "001", "Welcome to HNC Bridge."));
        context.add(String.format(":%s %s :%s", id.getHostName(), "002", "Your host is HNCBridge, running v2-Alpha."));
        context.add(String.format(":%s %s :%s", id.getHostName(), "003", "This server was created now."));
        context.add(String.format(":%s %s :%s", id.getHostName(), "004", "N/A."));
        context.add(String.format(":%s %s :%s", id.getHostName(), "251", "There are "+usersConnected+" users in Hnc"));
        context.add(String.format(":%s %s :%s", id.getHostName(), "252", "0 :Not operators in Hnc allowed"));
        context.add(String.format(":%s %s :%s", id.getHostName(), "253", id.connectionNumber + " :total connections"));
        context.add(String.format(":%s %s :%s", id.getHostName(), "254", "0 :Channels processed"));
        context.add(String.format(":%s %s :%s", id.getHostName(), "375", "hnc.hirana.net message of the day."));
        context.add(String.format(":%s %s :%s", id.getHostName(), "375", "Welcome to Hirana Network Connection"));
        context.add(String.format(":%s %s :%s", id.getHostName(), "375", "This is a Hirana bouncered connection"));
        context.add(String.format(":%s %s :%s", id.getHostName(), "376", "end message of the day."));
        context.add(String.format(":%s %s :%s", id.getHostName(), "396", "HNC-Bridged.IP :is now your displayed host."));
        return context;
    }

    private String formatMessage(String host, String code, String content) {
        return String.format(":%s %s :%s", host, code, content);
    }

    public List<String> getChannelContext(BridgeId id) {
        List<String> channelContext = new ArrayList<>();
        Optional<List<String>> channels = Optional.ofNullable(channelsForUser.get(id.getUser()));
        log.debug("Sending channel context? " + (channels.isPresent() ? "yes" : "no"));
        if (channels.isPresent()) {
            String chanString = "";
            for (String chanHash : channels.get()) {
                chanString += chanHash + " ";
            }
            log.debug("channel context send");
            channelContext.add(formatMessage(id.getHostName(), "319", chanString));
        }
        return channelContext;
    }

    public List<String> getChannelTopicContext(BridgeId id) {
        final List<String> topicContext = new ArrayList<>();
        List<String> channels = getChannelContext(id);
        Optional userTopics = Optional.ofNullable(topicsInChannels.get(id.getUser()));
        log.debug("Sending topics context? " + (userTopics.isPresent() ? "yes" : "no"));
        if(userTopics.isPresent()) {
            channels.forEach(chan -> {
                Optional<String> topic = Optional.ofNullable(topicsInChannels.get(id.getUser()).get(chan));
                if(topic.isPresent()) {
                    log.debug("topic context send");
                    topicContext.add(formatMessage(id.getHostName(), "332 "+chan, topic.get()));
                }
            });
        }
        return topicContext;
    }

    public List<String> getChannelUsersContext(BridgeId id) {
        final List<String> usersContext = new ArrayList<>();
        List<String> channels = getChannelContext(id);
        Optional uicE = Optional.ofNullable(usersInChannel.get(id.getUser()));
        log.debug("Sending users context? " + (uicE.isPresent() ? "yes" : "no"));
        if(uicE.isPresent()) {
            channels.forEach(chan -> {
                Optional<List<String>> uic = Optional.ofNullable(usersInChannel.get(id.getUser()).get(chan));
                if(uic.isPresent()) {
                    String userStr = "";
                    for (String user : uic.get()) {
                        userStr += user + " ";
                    }
                    log.debug("user context send");
                    usersContext.add(formatMessage(id.getHostName(), "353 " + chan, userStr));
                    usersContext.add(formatMessage(id.getHostName(), "366 " + chan, "end of names."));
                }
            });
        }
        return usersContext;
    }

    public List<String> getQueuedMessages(BridgeId id) {
        return RedisService.INSTANCE.getQueue("QMSG:"+id.getUser());
    }

    public void addToQueue(String user, RawMessage raw) {
        RedisService.INSTANCE.pushValueQueue("QMSG:"+user, raw.raw);
    }

    public void processContext(String user, RawMessage raw) {
        if("TOPIC".equals(raw.code)) {
            // topic changed
            log.debug("IRC - Processing topic for user " + user);
            this.topic(user, raw);
        }
        if( "332".equals(raw.code)) {
            log.debug("IRC - Processing topic 2 for user " + user);
            this.topicChanged(user, raw);
        }
        if("NICK".equals(raw.code)) {
            log.debug("IRC - Processing nick for user " + user);
            this.nickChange(user, raw);
        }
        if("319".equals(raw.code)) {
            // channel list
            log.debug("IRC - Processing channels for user " + user);
            this.channelList(user, raw);
        }
        if("JOIN".equals(raw.code)) {
            log.debug("IRC - Processing join for user " + user);
            this.join(user, raw);
        }
        if("PART".equals(raw.code)) {
            log.debug("IRC - Processing part for user " + user);
            this.part(user, raw);
        }
        if("KICK".equals(raw.code)) {
            log.debug("IRC - Processing kick for user " + user);
            this.kick(user, raw);
        }
        if("QUIT".equals(raw.code)) {
            log.debug("IRC - Processing quit for user " + user);
            this.quit(user, raw);
        }
        if("353".equals(raw.code)) {
            log.debug("IRC - Processing names for user " + user);
            this.names(user, raw);
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
        if(itsMe(user, originalNick.nick)) {
            ContextManager.INSTANCE.setNick(user, newNick);
        } else {
            changeNickInAllChannels(user, originalNick.nick, newNick);
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
        if(itsMe(user, userJoinded.nick)) {
            joinedToChannel(user, channel.hashedName);
        } else {
            addUserToChannel(user, channel.hashedName, userJoinded.nick);
        }
    }

    private void part(String user, RawMessage message) {
        SimplyOrigin userParted = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        Channel channel = new Channel(message.partials.size() > 2 ? message.partials.get(2) : message.content.get());
        if(itsMe(user, userParted.nick)) {
            leaveToChannel(user, channel.hashedName);
        } else {
            removeUserFromChannel(user, channel.hashedName, userParted.nick);
        }
    }

    private void names(String user, RawMessage raw) {
        String[] users = raw.content.get().trim().split(" ");
        Pattern namesPattern = Pattern.compile("(=|@|\\*)([^:]+):", Pattern.CASE_INSENSITIVE);
        Matcher namesMatcher = namesPattern.matcher(raw.raw);
        if(!namesMatcher.find() || namesMatcher.groupCount() < 2) {
            log.error("Error parsing names for user " + user, raw.raw);
            return;
        }
        Channel chan = new Channel(namesMatcher.group(2).trim());
        for (String userName: users) {
            addUserToChannel(user, chan.hashedName, userName);
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
        if(itsMe(user, userKicked.nick)) {
            leaveToChannel(user, chan.hashedName);
        } else {
            removeUserFromChannel(user, chan.hashedName, userKicked.nick);
        }
    }

    private void quit(String user, RawMessage message) {
        SimplyOrigin userQuitted = SimplyOrigin.parseUser(message.getOrigin().get().simplyOrigin);
        if(itsMe(user, userQuitted.nick)) {
            // this can't be
        } else {
            removeUserFromAllChannels(user, userQuitted.nick);
        }
    }

    private boolean itsMe(String user, String nick) {
        Optional<String> lastNick = ContextManager.INSTANCE.getLastNick(user);
        return nick.equals(user) || (lastNick.isPresent() && lastNick.get().equals(nick));
    }

    public void joinedToChannel(String user, String channelHash) {
        if(channelsForUser.get(user) == null) {
            channelsForUser.put(user, new ArrayList<>());
        }
        if(!channelsForUser.get(user).contains(channelHash)) {
            channelsForUser.get(user).add(channelHash);
        } else {
            log.error(String.format("@%s You are previous joined to channel %s", user, channelHash));
        }
    }

    public void leaveToChannel(String user, String channelHash) {
        if(channelsForUser.get(user) == null) {
            return;
        }
        channelsForUser.get(user).removeIf(chan -> chan.equals(channelHash));
    }

    public void setTopicToChannel(String user, String channelHash, String topic) {
        Optional<HashMap<String, String>> channels = Optional.ofNullable(topicsInChannels.get(user));
        if(!channels.isPresent()) {
            channels = Optional.of(new HashMap<>());
            topicsInChannels.put(user, channels.get());
        }
        channels.get().put(channelHash, topic);
    }

    public void addUserToChannel(String user, String channelHash, String nickToAdd) {
        Optional uicE = Optional.ofNullable(usersInChannel.get(user));
        if(!uicE.isPresent()) {
            usersInChannel.put(user, new HashMap<>());
        }
        Optional uicC = Optional.ofNullable(usersInChannel.get(user).get(channelHash));
        if(!uicC.isPresent()) {
            usersInChannel.get(user).put(channelHash, new ArrayList<>());
        }
        if(!usersInChannel.get(user).get(channelHash).contains(nickToAdd)) {
            log.debug("Adding user to list " + nickToAdd);
            usersInChannel.get(user).get(channelHash).add(nickToAdd);
        }
    }

    public void removeUserFromChannel(String user, String channelHash, String nickToRemove) {
        Optional<HashMap<String, List<String>>> uicE = Optional.ofNullable(usersInChannel.get(user));
        if(uicE.isPresent()) {
            Optional<List<String>> uicC = Optional.ofNullable(uicE.get().get(channelHash));
            if(uicC.isPresent()) {
                log.debug("Removing user from list " + nickToRemove);
                uicC.get().removeIf(u -> u.equals(nickToRemove));
            }
        }
    }

    public void removeUserFromAllChannels(String user, String nickToRemove) {
        Optional<HashMap<String, List<String>>> uicE = Optional.ofNullable(usersInChannel.get(user));
        if(uicE.isPresent()) {
            log.debug("Removing user from all chanels");
            uicE.get().entrySet().forEach(entry -> {
                entry.getValue().removeIf(u -> u.equals(nickToRemove));
            });
        }
    }

    public void changeNickInAllChannels(String user, String originalNick, String newNick) {
        // TODO
    }

}
