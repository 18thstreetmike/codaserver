/*
 * HSQLDBDriver.java
 *
 * Created on May 28, 2007, 1:18 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.hsqldbdriver;

import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michaelarace
 */
public class HSQLDBDriver implements CodaDatabase {
    
    // Various instance variables
    int mode = 0;                     
    String hostname, username, password, schema; 
    HSQLDBConnection conn;
    java.util.logging.Logger logger;
    
    public void setLogger (Logger logger) {
        this.logger = logger;
        if (conn != null)
            conn.setLogger(logger);
    }
    
    public boolean createSchema(String hostname, String username, String password, String schema, Hashtable options, String adminUsername, String adminPassword) {
        int mode;
        //System.out.println("In CreateSchema");
        if (options == null || options.size() == 0 || (options.containsKey("mode") || options.containsKey("MODE")) || (options.get("mode").equals("standalone") || options.get("MODE").equals("standalone") )) {
            mode = 0;
        } else {
            mode = 1;
        }
        switch (mode) {
            case 0:
                try {
                    Class.forName("org.hsqldb.jdbcDriver" );
                } catch (ClassNotFoundException e) {
                    return false;
                }
                Connection tempConn = null;
                try {
                    tempConn = DriverManager.getConnection("jdbc:hsqldb:file:" + schema, adminUsername, adminPassword);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, e.getMessage());
                    return false;
                }
                try {
                    Statement s = tempConn.createStatement();
                    String sql = "CREATE USER " + username + " PASSWORD '" + escapeString(password) + "'";
                    s.execute(sql);
                    sql = "GRANT DBA TO " + username;
                    s.execute(sql);
                    tempConn.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                    //logger.log(Level.WARNING, e.getMessage());
                    return true;
                }    
                
                break;
            default:
                return false;
        }
        return true;
    }
    
    public boolean connect(String hostname, String username, String password, String schema, Hashtable options) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.schema = schema;
        if (options == null || options.size() == 0 || (options.containsKey("mode") || options.containsKey("MODE")) || (options.get("mode").equals("standalone") || options.get("MODE").equals("standalone") )) {
            this.mode = 0;
        } else {
            this.mode = 1;
        }
        Connection tempConn = null;
        switch (this.mode) {
            case 0:
                try {
                    Class.forName("org.hsqldb.jdbcDriver" );
                } catch (ClassNotFoundException e) {
                    this.conn = null;
                    return false;
                }
                try {
                    tempConn = DriverManager.getConnection("jdbc:hsqldb:file:" + schema, username, password);
                    this.conn = new HSQLDBConnection(tempConn, logger);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, e.getMessage());
                    this.conn = null;
                    return false;
                }
                break;
            default:
                try {
                    Class.forName("org.hsqldb.jdbcDriver" );
                } catch (ClassNotFoundException e) {
                    this.conn = null;
                    return false;
                }
                try {
                    tempConn = DriverManager.getConnection("jdbc:hsqldb:hsql://"+hostname+"/" + schema, username, password);
                    this.conn = new HSQLDBConnection(tempConn, logger);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, e.getMessage());
                    this.conn = null;
                    return false;
                }
        }
        return true;
    }
    
    public boolean disconnect() {
        conn.closeConnection();
        return true;
    }
    
    public CodaConnection getConnection() {
        return conn;
    }

    private String escapeString(String val) {
        return val.replaceAll("'", "''");
    }
}