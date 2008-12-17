/*
 * CodaTableNameAlias.java
 *
 * Created on August 31, 2007, 12:45 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

/**
 *
 * @author michaelarace
 */
public class CodaTableNameAlias {
    
    private String tableName;
    private String alias;
    
    /** Creates a new instance of CodaTableNameAlias */
    public CodaTableNameAlias(String tableName, String alias) {
        this.setTableName(tableName);
        this.setAlias(alias);
    }

    public String getTableName() {
        return tableName.toUpperCase();
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getAlias() {
        return (alias == null ? null : alias.toUpperCase());
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    public boolean isNull() {
        return tableName == null;
    }
}
