/*
 * Configuration.java
 *
 * Created on May 29, 2007, 9:28 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

/**
 *
 * @author michaelarace
 */
public class Configuration {
    
    private DatabaseConfiguration[] databaseConfiguration;
    private ListenerConfiguration[] listenerConfiguration;
    private int cluster = 0, runCron = 0, sessionTimeout = 0;
    private String logDirectory = "log";
    
    /** Creates a new instance of Configuration */
    public Configuration() {
    }

    public DatabaseConfiguration[] getDatabaseConfiguration() {
        return databaseConfiguration;
    }

    public void setDatabaseConfiguration(DatabaseConfiguration databaseConfiguration[]) {
        this.databaseConfiguration = databaseConfiguration;
    }

    public int getCluster() {
        return cluster;
    }

    public void setCluster(int cluster) {
        this.cluster = cluster;
    }
    
    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public int getRunCron() {
        return runCron;
    }

    public void setRunCron(int runCron) {
        this.runCron = runCron;
    }

    public ListenerConfiguration[] getListenerConfiguration() {
        return listenerConfiguration;
    }

    public void setListenerConfiguration(ListenerConfiguration[] listenerConfiguration) {
        this.listenerConfiguration = listenerConfiguration;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }
    
}
