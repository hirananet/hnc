package net.hirana.services;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public enum Database {
    INSTANCE;

    private HikariConfig config = new HikariConfig();
    private HikariDataSource ds;
    private static final String tableName = "anope_db_NickCore";
    private Connection conn;

    public void init() throws SQLException {
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl( getUrl(System.getenv("DB_HOST"), Integer.parseInt(System.getenv("DB_PORT")), System.getenv("DB_NAME")) );
        config.setUsername( System.getenv("DB_USERNAME"));
        config.setPassword( System.getenv("DB_PASSWORD") );
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        ds = new HikariDataSource( config );
        this.conn = ds.getConnection();
    }

    private String getUrl(String host, Integer port, String dbName) {
        return String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
    }

    public Connection getConnection() throws SQLException {
        if(this.conn == null) {
            this.conn = ds.getConnection();
        }
        return this.conn;
    }

    public String getUserPassword(String user) throws SQLException {
        Connection con = this.getConnection();
        PreparedStatement pst = con.prepareStatement(String.format("SELECT pass FROM %s WHERE LOWER(display) = '%s' LIMIT 1", tableName, user.toLowerCase()));
        ResultSet rs = pst.executeQuery();
        if(rs.next()) {
            return rs.getString("pass");
        } else {
            return null;
        }
    }

}
