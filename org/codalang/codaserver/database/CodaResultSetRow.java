/*
 * CodaResultSetRow.java
 *
 * Created on March 13, 2007, 4:02 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.database;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaResultSetRow {
    
    private Vector<String> data;
    
    /**
     * Creates a new instance of CodaResultSetRow
     */
    public CodaResultSetRow(Vector data) {
        this.data = data;
    }
    
    public Vector getData () {
        return data;
    }
    
    public String getDataValue(int index) {
        return (String)data.get(index);
    }
    
}
