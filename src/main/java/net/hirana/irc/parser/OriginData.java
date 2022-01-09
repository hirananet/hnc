package net.hirana.irc.parser;

import java.util.Optional;

public class OriginData {
    public Optional<String> server = Optional.empty();
    public Optional<String> identity = Optional.empty();
    public Optional<String> nick = Optional.empty();
    public String simplyOrigin;
}
