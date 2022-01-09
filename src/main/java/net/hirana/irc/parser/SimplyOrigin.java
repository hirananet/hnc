package net.hirana.irc.parser;

public class SimplyOrigin {
    public String nick;
    public UModes mode;

    SimplyOrigin(String nick, UModes mode) {
        this.nick = nick;
        this.mode = mode;
    }

    public static SimplyOrigin parseUser(String nick) {
        UModes mode = UModes.UNDEFINED;
        String nickMod = nick.substring(0, 1);
        if ("~".equals(nickMod)) {
            mode = UModes.FOUNDER;
        } else if("&".equals(nickMod)) {
            mode = UModes.ADMIN;
        } else if("@".equals(nickMod)) {
            mode = UModes.OPER;
        } else if("%".equals(nickMod)) {
            mode = UModes.HALFOPER;
        } else if("+".equals(nickMod)) {
            mode = UModes.VOICE;
        }
        return new SimplyOrigin(mode == UModes.UNDEFINED ? nick : nick.substring(1), mode);
    }
}
