/*
 * CodaSystemTable.java
 *
 * Created on June 6, 2007, 11:02 AM
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
public class CodaSystemTable {
    
    private boolean codaServerFlag;
    private long revisionId, refTableRevisionId;
    private String applicationName;
    private String prefixString;
    private float codaFormatVersion;
    
    /**
     * Creates a new instance of CodaSystemTable
     */
    public CodaSystemTable(boolean codaServerFlag, long revisionId, long refTableRevisionId, String applicationName, String prefixString, float codaFormatVersion) {
        this.codaServerFlag = codaServerFlag;
        this.revisionId = revisionId;
        this.refTableRevisionId = refTableRevisionId;
        this.applicationName = applicationName;
        this.prefixString = prefixString;
        this.codaFormatVersion = codaFormatVersion;
    }

    public long getRevisionId() {
        return revisionId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getPrefixString() {
        return prefixString;
    }

    public float getCodaFormatVersion() {
        return codaFormatVersion;
    }

    public boolean isCodaServerFlag() {
        return codaServerFlag;
    }

    public long getRefTableRevisionId() {
        return refTableRevisionId;
    }

    public void setRefTableRevisionId(long refTableRevisionId) {
        this.refTableRevisionId = refTableRevisionId;
    }
}
