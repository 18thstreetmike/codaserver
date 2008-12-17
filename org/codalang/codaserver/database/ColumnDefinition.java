/*
 * ColumnDefinition.java
 *
 * Created on March 15, 2007, 7:20 PM
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
public class ColumnDefinition {
    
    private String name;
    private int sqlType;
    private boolean nullable;
    
    /** Creates a new instance of ColumnDefinition */
    public ColumnDefinition(String name, int sqlType, boolean nullable) {
        this.setName(name);
        this.setSqlType(sqlType);
        this.setNullable(nullable); 
    }
    
    public String getName() {
        return name;
    }
    
    public int getSqlType() {
        return sqlType;
    }
    
    public boolean getNullable () {
        return nullable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSqlType(int sqlType) {
        this.sqlType = sqlType;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
    
}
