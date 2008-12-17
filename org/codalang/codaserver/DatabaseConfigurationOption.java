/*
 * DatabaseConfigurationOption.java
 *
 * Created on May 29, 2007, 10:27 PM
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
public class DatabaseConfigurationOption {
    
    private String setting, value;
    
    /**
     * Creates a new instance of DatabaseConfigurationOption
     */
    public DatabaseConfigurationOption() {
    }

    public String getSetting() {
        return setting;
    }

    public void setSetting(String setting) {
        this.setting = setting;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
}
