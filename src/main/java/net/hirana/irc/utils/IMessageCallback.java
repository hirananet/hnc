package net.hirana.irc.utils;

import java.io.BufferedReader;

public interface IMessageCallback {
    BufferedReader getBufferedReader();
    void onReady();
    void onMessageReceived(String message);
    void onError(Exception e);
}
