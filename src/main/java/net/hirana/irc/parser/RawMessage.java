package net.hirana.irc.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RawMessage {

    private static final Logger log = LoggerFactory.getLogger(RawMessage.class);

    public Optional<String> content = Optional.empty();
    public Optional<String> info = Optional.empty();
    public String code = "00";
    public List<String> partials = new ArrayList<>();
    public HashMap<String, String> tags = new HashMap<>();
    public String raw;
    public Optional<OriginData> origin = Optional.empty();

    public RawMessage(String message) {
        this.raw = message;
        Pattern infoPattern = Pattern.compile(":([^:]+):?(.*)", Pattern.CASE_INSENSITIVE);
        Matcher infoMatcher = infoPattern.matcher(message);
        if(!infoMatcher.find()) {
            log.error("Can't parse info", message);
            return;
        }
        this.info = Optional.of(infoMatcher.group(1));
        Pattern tagPattern = Pattern.compile("@((;?)([^\\s=]+)=([^\\s;]+))+", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(info.get());
        if(tagMatcher.find()) {
            String tagStr = tagMatcher.group(0).substring(1);
            for(String kv: tagStr.split(";")) {
                String[] kvs = kv.split("=");
                tags.put(kvs[0], kvs[1]);
            }
        }
        this.content = Optional.of(infoMatcher.group(2));
        this.partials = Arrays.asList(this.info.get().split(" "));
        this.code = this.partials.get(1);
    }

    public Optional<OriginData> getOrigin() {
        if(!this.origin.isPresent()) {
            Pattern uOriginPattern = Pattern.compile("([^!]*!)?([^@]+@)?(.*)", Pattern.CASE_INSENSITIVE);
            Matcher uOriginMatcher = uOriginPattern.matcher(this.partials.get(0));
            if (!uOriginMatcher.find()){
                log.error("Error parsing user origin", this.partials.get(0));
                this.origin = Optional.empty();
                return this.origin;
            }
            OriginData od = new OriginData();
            if (uOriginMatcher.groupCount() < 3 || uOriginMatcher.group(2) != null) {
                od.server = Optional.of(uOriginMatcher.group(1));
                od.simplyOrigin = od.server.get();
            } else if (uOriginMatcher.groupCount() < 4 || uOriginMatcher.group(3) != null) {
                od.server = Optional.of(uOriginMatcher.group(2));
                od.identity = Optional.of(uOriginMatcher.group(1).substring(0, uOriginMatcher.group(1).length() - 1));
                od.simplyOrigin = od.identity.get();
            } else {
                od.server = Optional.of(uOriginMatcher.group(3));
                od.identity = Optional.of(uOriginMatcher.group(2).substring(0, uOriginMatcher.group(2).length() - 1));
                od.nick = Optional.of(uOriginMatcher.group(1).substring(0, uOriginMatcher.group(1).length() - 1));
                od.simplyOrigin = od.nick.get();
            }
            this.origin = Optional.of(od);
        }
        return this.origin;
    }

}
