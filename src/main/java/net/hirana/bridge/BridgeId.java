package net.hirana.bridge;

public class BridgeId {

    private static final String DEFAULT_USER = "Not Identified";

    public long connectionNumber;
    private String user = DEFAULT_USER;
    private boolean identified;
    private String encoding;
    private boolean requiredCap = false;
    private String nick;
    private String pass;
    private String hostName;

    BridgeId(long connectionNumber) {
        this.connectionNumber = connectionNumber;
    }

    public void setUser(String user) {
        this.user = user;
        if(this.nick == null) {
            this.nick = this.user;
        }
    }

    public String getUser() {
        return user;
    }

    public void setIdentified() {
        this.identified = true;
    }

    public boolean isIdentified() {
        return identified;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setRequiredCap() {
        this.requiredCap = true;
    }

    public boolean isRequiredCap() {
        return this.requiredCap;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
        if(DEFAULT_USER.equals(this.user)) {
            this.user = nick;
        }
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
}
