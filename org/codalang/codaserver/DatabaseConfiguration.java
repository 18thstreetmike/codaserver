/*
 * DatabaseConfiguration.java
 *
 * Created on May 29, 2007, 9:26 PM
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
public class DatabaseConfiguration {
    
    private DatabaseConfigurationOption[] options;
    private String hostname, username, password, schema, driverClass;
    
    /** Creates a new instance of DatabaseConfiguration */
    public DatabaseConfiguration() {
    }

    public DatabaseConfigurationOption[] getOptions() {
        return options;
    }

    public void setOptions(DatabaseConfigurationOption[] options) {
        this.options = options;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }
    
    public boolean configurationComplete() {
        return (this.schema != null && this.driverClass != null && this.username != null && this.password != null && this.hostname != null);
    }
}
