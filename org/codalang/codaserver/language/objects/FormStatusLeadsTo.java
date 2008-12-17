/*
 * FormStatusLeadsTo.java
 *
 * Created on July 20, 2007, 12:50 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class FormStatusLeadsTo {
    
    private String statusName;
    private Vector<String> leadsToList;
    
    /** Creates a new instance of FormStatusLeadsTo */
    public FormStatusLeadsTo(String statusName, Vector<String> leadsToList) {
        this.statusName = statusName;
        this.leadsToList = leadsToList;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Vector<String> getLeadsToList() {
        return leadsToList;
    }

    public void setLeadsToList(Vector<String> leadsToList) {
        this.leadsToList = leadsToList;
    }
    
}
