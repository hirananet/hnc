package net.hirana.irc.parser;

import java.util.List;

public class Channel {
    public String name;
    public String hashedName;
    public String topic;
    public List<String> channelModes;

    public Channel(String raw) {
        this.name = raw;
        if("#".equals(raw.substring(0, 1))) {
            this.name = raw.substring(1);
        }
        this.hashedName = "#" + this.name;
    }

}
