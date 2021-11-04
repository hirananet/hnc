package net.hirana.services;

import redis.clients.jedis.Jedis;

public enum Redis {
    INSTANCE;

    private final String host = System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST") : "localhost";
    private final int port = System.getenv("REDIS_PORT") != null ? Integer.parseInt(System.getenv("REDIS_PORT")) : 6379;
    private final String password = System.getenv("REDIS_PASSWORD") != null ? System.getenv("REDIS_PASSWORD") : "local";
    private Jedis jedis;

    public void init() {
        this.jedis = new Jedis(host, port);
        this.jedis.auth(password);
    }

    public void setValue(String key, String value) {
        this.jedis.set(key, value);
    }

    public String getValue(String key) {
        return this.jedis.get(key);
    }

    public boolean exists(String key) {
        return this.jedis.exists(key);
    }
}
