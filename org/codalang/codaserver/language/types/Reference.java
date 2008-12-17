/*
 * Reference.java
 *
 * Created on September 15, 2007, 3:04 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

/**
 *
 * @author michaelarace
 */
public class Reference {
    
    private String tableName;
    private long id;
    
    /** Creates a new instance of Reference */
    public Reference(String tableName, long id) {
        this.setTableName(tableName);
        this.setId(id);
    }
    
    public Reference(String tableName, int id) {
        this.setTableName(tableName);
        this.setId(new Integer(id).longValue());
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    
}
