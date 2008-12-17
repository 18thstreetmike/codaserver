/*
 * SecurityApplicationEnvironment.java
 *
 * Created on July 11, 2007, 2:44 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.security;

/**
 *
 * @author michaelarace
 */
public class SecurityApplicationEnvironment {
    
    private int environment;
    private long groupId;
    
    /** Creates a new instance of SecurityApplicationEnvironment */
    public SecurityApplicationEnvironment(int environment, long groupId) {
        this.environment = environment;
        this.groupId = groupId;
    }
    
    public SecurityApplicationEnvironment(int environment) {
        this.environment = environment;
        this.groupId = -1;
    }

    public int getEnvironment() {
        return environment;
    }

    public long getGroupId() {
        return groupId;
    }
    
}
