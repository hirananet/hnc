package net.hirana.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;

public enum Redis {
    INSTANCE;
    private static final Logger log = LoggerFactory.getLogger(Redis.class);

    private final String host = System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST") : "localhost";
    private final int port = System.getenv("REDIS_PORT") != null ? Integer.parseInt(System.getenv("REDIS_PORT")) : 6379;
    private final String password = System.getenv("REDIS_PASSWORD") != null ? System.getenv("REDIS_PASSWORD") : "local";
    private final String user = System.getenv("REDIS_USER") != null ? System.getenv("REDIS_USER") : "";
    private Jedis jedis;

    public void init() throws URISyntaxException {
        String format = String.format("redis://%s:%s@%s:%d", user, password, host, port);
        log.info("Redis with password " + format);
        this.jedis = new Jedis(new URI(format));
    }

    public void remove(String key) {
        this.jedis.del(key);
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
