/*
 * HSQLDBDriver.java
 *
 * Created on May 28, 2007, 1:18 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.mysqldriver;

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
public class MySQLDriver implements CodaDatabase {
    
    // Various instance variables
    int mode = 0;                     
    String hostname, username, password, schema;
	int port;
	java.util.logging.Logger logger;
    
    public void setLogger (Logger logger) {
        this.logger = logger;
    }
    
    public boolean createSchema(String hostname, String username, String password, String schema, Hashtable options, String adminUsername, String adminPassword) {
        int port = 3306;
        //System.out.println("In CreateSchema");
        if (options != null && options.size() > 0 && (options.containsKey("port") || options.containsKey("PORT"))) {
            try {
				port = Integer.parseInt((String)options.get("port"));
			} catch (Exception e1) {
				try {
					port = Integer.parseInt((String)options.get("PORT"));
				} catch (Exception e2) {
					//skip
				}
			}
        }
		try {
			Class.forName("com.mysql.jdbc.Driver" );
		} catch (ClassNotFoundException e) {
			return false;
		}
		Connection tempConn = null;
		try {
			tempConn = DriverManager.getConnection("jdbc:mysql://"+hostname+":" +port+"/?user="+ adminUsername+ "&password="+ adminPassword);
			tempConn.setAutoCommit(false);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
			return false;
		}
		Statement s;
		String sql;
		try {
			s = tempConn.createStatement();
		} catch (SQLException e) {
			return false;
		}

		try {
			sql = "CREATE DATABASE " + schema;
			s.execute(sql);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
		}
		// create the global user
		try {
			sql = "CREATE USER " + username + " IDENTIFIED BY '" + escapeString(password) + "'";
			s.execute(sql);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
		}
		// create the localhost user
		try {
			sql = "CREATE USER `" + username + "`@`localhost` IDENTIFIED BY '" + escapeString(password) + "'";
			s.execute(sql);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
		}
		// grant all to the global user
		try {
			sql = "GRANT ALL ON "+schema+".* TO " + username;
			s.execute(sql);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
		}
		// grant all to the localhost user
		try {
			sql = "GRANT ALL ON "+schema+".* TO `" + username + "`@`localhost`";
			s.execute(sql);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
		}

		try {
			tempConn.commit();
		} catch (SQLException e) {
			return false;
		}


        return true;
    }
    
    public boolean connect(String hostname, String username, String password, String schema, Hashtable options) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.schema = schema;
		this.port = 3306;
        //System.out.println("In CreateSchema");
        if (options != null && options.size() > 0 && (options.containsKey("port") || options.containsKey("PORT"))) {
            try {
				this.port = Integer.parseInt((String)options.get("port"));
			} catch (Exception e1) {
				try {
					this.port = Integer.parseInt((String)options.get("PORT"));
				} catch (Exception e2) {
					//skip
				}
			}
        }

		Connection tempConn = null;
		CodaConnection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver" );
		} catch (ClassNotFoundException e) {
			return false;
		}
		try {
			tempConn = DriverManager.getConnection("jdbc:mysql://"+hostname+":" +port+"/"+schema+"?user="+ username+ "&password="+ password);
			conn = new MySQLConnection(tempConn, schema, logger);
		} catch (SQLException e) {
			//logger.log(Level.WARNING, e.getMessage());
			return false;
		}

		return true;
    }
    
    public boolean disconnect() {
        return true;
    }
    
    public CodaConnection getConnection() {
        Connection tempConn = null;
		CodaConnection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver" );
		} catch (ClassNotFoundException e) {
			conn = null;
		}
		try {
			String connectString = "jdbc:mysql://"+hostname+":" +port+"/"+schema+"?user="+ username+ "&password="+ password;
			tempConn = DriverManager.getConnection(connectString);
			conn = new MySQLConnection(tempConn, schema, logger);
		} catch (SQLException e) {
			logger.log(Level.WARNING, e.getMessage());
			conn = null;
		}
        return conn;
    }

    private String escapeString(String val) {
        return val.replaceAll("'", "\\'");
    }
}