/*
 * SecurityGroup.java
 *
 * Created on June 24, 2007, 1:11 AM
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
public class SecurityGroup {
    
    private long groupId;
    private String groupName;
    private String displayName;
    
    /** Creates a new instance of SecurityGroup */
    public SecurityGroup(long groupId, String groupName, String displayName) {
        this.setGroupId(groupId);
        this.setGroupName(groupName);
        this.setDisplayName(displayName);
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
}
