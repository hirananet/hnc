package net.hirana.irc.parser;

import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {
    public Optional<String> id = Optional.empty();
    public Optional<String> author = Optional.empty();
    public Optional<String> date = Optional.empty();
    public String content;
    public boolean isMeCommand = false;
    public Optional<String> channel = Optional.empty();
    public HashMap<String, String> tags;
    public boolean haveMention = false;

    public static Message parseMessage(RawMessage raw, String user, Optional<String> lastNick) {
        Message output = new Message();
        output.tags = raw.tags;
        output.id = Optional.ofNullable(output.tags.get("msgid"));
        output.author = Optional.ofNullable(SimplyOrigin.parseUser(raw.getOrigin().get().nick.get()).nick);
        output.date = Optional.of("now");
        output.channel = Optional.ofNullable(new Channel(raw.partials.get(2)).name);
        Pattern mePattern = Pattern.compile("\u0001ACTION ([^\u0001]+)\u0001");
        Matcher meMatcher = mePattern.matcher(raw.raw);
        if(meMatcher.find()) {
            output.content = meMatcher.group(1);
            output.isMeCommand = true;
        } else {
            output.isMeCommand = false;
            output.content = raw.content.get();
        }
        output.haveMention = raw.content.get().indexOf(" "+user) >= 0 || lastNick.isPresent() && raw.content.get().indexOf(" "+lastNick.get()) >= 0;
        return output;
    }

}
