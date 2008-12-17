/*
 * CodaSearchCondition.java
 *
 * Created on August 12, 2007, 10:45 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.CodaException;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaSearchCondition {
    
    private Vector<CodaSubSearchCondition> conditions;
    private Vector<String> connectors;
    
    /** Creates a new instance of CodaSearchCondition */
    public CodaSearchCondition(Vector<CodaSubSearchCondition> conditions, Vector<String> connectors) {
        this.setConnectors(connectors);
        this.setConditions(conditions);
    }

    public Vector<CodaSubSearchCondition> getConditions() {
        return conditions;
    }

    public void setConditions(Vector<CodaSubSearchCondition> conditions) {
        this.conditions = conditions;
    }

    public Vector<String> getConnectors() {
        return connectors;
    }

    public void setConnectors(Vector<String> connectors) {
        this.connectors = connectors;
    }
    
    public String print(CodaFromClause fromClause) throws CodaException {
        String retval = "";
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                retval += connectors.get(i - 1) + " ";
                retval += conditions.get(i).print(fromClause);
            } else {
                retval += conditions.get(i).print(fromClause);    
            }
        }
        return retval;
    }
    
}
