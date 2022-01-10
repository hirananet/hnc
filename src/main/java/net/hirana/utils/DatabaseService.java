package net.hirana.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public enum DatabaseService {
    INSTANCE;

    private HikariConfig config = new HikariConfig();
    private HikariDataSource ds;
    private static final String tableName = "anope_db_NickCore";
    private static final long MaxLifetime = 25000 * 1000;
    private static final long IdleTimeout = 1800000;

    public void init() throws SQLException {
        config.setJdbcUrl( getUrl(System.getenv("DB_HOST"), Integer.parseInt(System.getenv("DB_PORT")), System.getenv("DB_NAME")) );
        config.setUsername( System.getenv("DB_USERNAME"));
        config.setPassword( System.getenv("DB_PASSWORD") );
        config.setMaxLifetime(MaxLifetime);
        config.setIdleTimeout(IdleTimeout);
        config.setMinimumIdle(3);
        config.setMaximumPoolSize(10);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        config.setConnectionTestQuery("SELECT version()");
        ds = new HikariDataSource( config );
    }

    private String getUrl(String host, Integer port, String dbName) {
        return String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
    }

    public String getUserPassword(String user) throws SQLException {
        Connection con = ds.getConnection();
        PreparedStatement pst = con.prepareStatement(String.format("SELECT pass FROM %s WHERE LOWER(display) = '%s' LIMIT 1", tableName, user.toLowerCase()));
        ResultSet rs = pst.executeQuery();
        if(rs.next()) {
            return rs.getString("pass");
        } else {
            return null;
        }
    }
}
