/*
 * CodaTable.java
 *
 * Created on June 6, 2007, 10:39 AM
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
public class CodaTable {
    
    private String tableName;
    
    private ColumnDefinition[] columnDefinitions;
    
    /**
     * Creates a new instance of CodaTable
     */
    public CodaTable(String tableName, ColumnDefinition[] columnDefinitions) {
        this.setTableName(tableName);
        this.setColumnDefinitions(columnDefinitions);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public ColumnDefinition[] getColumnDefinitions() {
        return columnDefinitions;
    }

    public void setColumnDefinitions(ColumnDefinition[] columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
    }
    
}
