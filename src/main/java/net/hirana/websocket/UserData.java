package net.hirana.websocket;

import net.hirana.irc.IRClient;

public class UserData {
    public String user;
    public String password;
    public String nick;
    public boolean requestedCaps = false;
    public String encoding;
    public IRClient irc;
}
