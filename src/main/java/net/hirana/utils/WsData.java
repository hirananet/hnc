package net.hirana.utils;

import org.java_websocket.WebSocket;

public class WsData {
    public long connNumber;
    public WebSocket ws;
    public WsData(WebSocket ws) {
        connNumber = ws.getAttachment();
        this.ws = ws;
    }
}
