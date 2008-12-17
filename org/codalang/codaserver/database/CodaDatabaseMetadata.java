/*
 * CodaDatabaseMetadata.java
 *
 * Created on June 6, 2007, 10:35 AM
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
public interface CodaDatabaseMetadata {
    
    public String[] getTables();
    public ColumnDefinition[] getColumnsForTable (String tableName);
    public boolean isTable(String tableName);
    public boolean isColumn(String tableName, String columnName);
    public boolean isColumnDefinition(String tableName, ColumnDefinition definition);
    
    public boolean doTablesExist(CodaTable[] tables);
    
    public CodaSystemTable getSystemTable();
    
    // these help the server know how to handle this database
    public int getMaxTableNameLength();
    public int getMaxColumnNameLength();
    public int getMaxSchemaNameLength();
    
}
