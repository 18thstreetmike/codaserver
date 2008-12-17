/*
 * Database.java
 *
 * Created on September 15, 2007, 3:26 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.codalang.codaserver.CodaResponse;
import org.codalang.codaserver.CodaServer;
import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;
import org.codalang.codaserver.database.CodaResultSet;
import org.codalang.codaserver.language.CaseInsensitiveStringStream;
import org.codalang.codaserver.language.CodaLexer;
import org.codalang.codaserver.language.CodaParser;

/**
 *
 * @author michaelarace
 */
public class Database {
    
    String sessionKey;
    CodaServer server;
    CodaConnection connection = null;
    Object retval = new Boolean(false);
    
    /** Creates a new instance of Database */
    public Database(CodaServer server, String sessionKey) {
        this.sessionKey = sessionKey;
        this.server = server;
    }

    public Database(CodaServer server, String sessionKey, CodaConnection connection) {
        this.sessionKey = sessionKey;
        this.server = server;
        this.connection = connection;
    }

    public void setConnection(CodaConnection connection) {
        this.connection = connection;
    }
    
    public boolean execute(String command) {
        if (connection == null) {
            int environmentId = server.getSessionEnvironmentId(sessionKey);
            if (environmentId < 0) {
                return false;
            }
            String applicationName = server.getSessionApplication(sessionKey);
            CodaDatabase database = server.getApplicationDatabase(applicationName, environmentId);
            if (database == null) {
                return false;
            }

            this.connection = database.getConnection();
        }
        CodaParser parser = new CodaParser(new CommonTokenStream(new CodaLexer(new CaseInsensitiveStringStream(command))), server, sessionKey, null);
        CodaResponse rs;
        try {
            rs = parser.dml(connection).response;
            if (!rs.getError()) {
                retval = rs.getReturnValue();
            }
        } catch (RecognitionException ex) {
            return false;
        } catch (NullPointerException ex2) {
			return false;
		}
        return !rs.getError();
    }
    
    public ResultSet query(String query) {
        if (connection == null) {
            int environmentId = server.getSessionEnvironmentId(sessionKey);
            if (environmentId < 0) {
                return new ResultSet("No environment specified.");
            }
            String applicationName = server.getSessionApplication(sessionKey);
            CodaDatabase database = server.getApplicationDatabase(applicationName, environmentId);
            if (database == null) {
                return new ResultSet("Could not connect to application database.");
            }

            this.connection = database.getConnection();
        }

        CodaParser parser = new CodaParser(new CommonTokenStream(new CodaLexer(new CaseInsensitiveStringStream(query))), server, sessionKey, null);
        CodaResponse rs;
        try {
            rs = parser.query(connection).response;
        } catch (RecognitionException ex) {
            return new ResultSet("Invalid query.");
        }
        if (rs.getReturnValue() != null && rs.getReturnValue().getClass().getName().equals("org.codalang.codaserver.database.CodaResultSet"))
            return new ResultSet((CodaResultSet)rs.getReturnValue());
        else 
            return new ResultSet("Invalid query.");
    }

    public Object getLastProcedureReturnValue() {
        return retval;
    }

    public void commit() {
        if (connection == null) {
            int environmentId = server.getSessionEnvironmentId(sessionKey);
            if (environmentId < 0) {
                return;
            }
            String applicationName = server.getSessionApplication(sessionKey);
            CodaDatabase database = server.getApplicationDatabase(applicationName, environmentId);
            if (database == null) {
                return;
            }

            this.connection = database.getConnection();
        }
        connection.commit();
    }

    public void rollback() {
        if (connection == null) {
            int environmentId = server.getSessionEnvironmentId(sessionKey);
            if (environmentId < 0) {
                return;
            }
            String applicationName = server.getSessionApplication(sessionKey);
            CodaDatabase database = server.getApplicationDatabase(applicationName, environmentId);
            if (database == null) {
                return;
            }

            this.connection = database.getConnection();
        }
        connection.rollback();
    }
}
