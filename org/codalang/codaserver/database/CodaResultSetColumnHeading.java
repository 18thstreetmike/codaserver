/*
 * CodaResultSetColumnHeading.java
 *
 * Created on March 13, 2007, 4:08 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.database;

/**
 *
 * @author michaelarace
 */
public class CodaResultSetColumnHeading {
    
    private String name;
    private String type;
    private int sqlType;
    
    
    /**
     * Creates a new instance of CodaResultSetColumnHeading
     */
    public CodaResultSetColumnHeading(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    public CodaResultSetColumnHeading(String name, int sqlType) {
        this.name = name;
        this.sqlType = sqlType;
    }
    
    public CodaResultSetColumnHeading(String name) {
        this.name = name;
        this.type = null;
    }
    
    public String getName () {
        return name;
    }
    
    public String getType () {
        return type;
    }

    public int getSqlType() {
        return sqlType;
    }

    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

}
