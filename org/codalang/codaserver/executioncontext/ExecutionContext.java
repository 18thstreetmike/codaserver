/*
 * ExecutionContext.java
 *
 * Created on July 24, 2007, 3:28 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.executioncontext;

import groovy.lang.GroovyClassLoader;
import org.codalang.codaserver.CodaException;
import org.codalang.codaserver.CodaServer;
import org.codalang.codaserver.TypeParser;
import org.codalang.codaserver.database.CodaDatabase;

import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class ExecutionContext {
    
    private CodaServer server;
    private CodaDatabase database;
    
    private String applicationName;
    private int environment;
    
    private Hashtable<Long,TypeParser> types;
    
    private GroovyClassLoader classLoader;
    
    /** Creates a new instance of ExecutionContext */
    public ExecutionContext(CodaServer server, CodaDatabase database) {
        this.server = server;
        this.database = database;
        types = server.getTypes();
        classLoader = new GroovyClassLoader(server.getClassLoader());
    }
    
    public ExecutionContext(CodaServer server, String applicationName, int environment) {
        this.server = server;
        this.database = server.getApplicationDatabase(applicationName, environment);
        this.applicationName = applicationName;
        this.environment = environment;
        types = server.getTypes();
        this.classLoader = server.getDeployedApplication(applicationName).getEnvironmentClassLoader(environment, server.getClassLoader());
    }
    
    public void setClassLoader(GroovyClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    public GroovyClassLoader getClassLoader() {
        return classLoader;
    }
    
    public boolean validate(long typeId, String value) {
        if (!types.containsKey(typeId)) {
            TypeParser temp = server.getTypeParser(typeId);
            if (temp == null) {
                return false;
            } else {
                types.put(typeId, temp);
            }
        }
        return types.get(typeId).validate(value);
    }
    
    public Object parse(long typeId, String value) throws CodaException {
        if (!types.containsKey(typeId)) {
            TypeParser temp = server.getTypeParser(typeId);
            if (temp == null) {
                return false;
            } else {
                types.put(typeId, temp);
            }
        }
        return types.get(typeId).parse(value);
    }
    
    public String getDisplayName(long typeId) {
        if (!types.containsKey(typeId)) {
            TypeParser temp = server.getTypeParser(typeId);
            if (temp == null) {
                return "";
            } else {
                types.put(typeId, temp);
            }
        }
        return TypeParser.getDisplayName(types.get(typeId).getTypeName());
    }

	public CodaServer getServer() {
        return server;
    }

    public CodaDatabase getDatabase() {
        return database;
    }
    
    public Hashtable<String,String> getDistinctColumnsForTables(Vector<String> tables) {
        return server.getApplicationDatasource(applicationName, environment).getDistinctColumnsForTables(tables);
    }
}
