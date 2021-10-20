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

    public void init() {
        config.setJdbcUrl( "jdbc_url" );
        config.setUsername( "database_username" );
        config.setPassword( "database_password" );
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        ds = new HikariDataSource( config );
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public String getUserPassword(String user) throws SQLException {
        Connection con = this.getConnection();
        PreparedStatement pst = con.prepareStatement(String.format("SELECT pass FROM %s WHERE display = %s LIMIT 1", tableName, user));
        ResultSet rs = pst.executeQuery();
        if(rs.next()) {
            return rs.getString("pass");
        } else {
            return null;
        }
    }

}
