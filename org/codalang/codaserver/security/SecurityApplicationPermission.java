/*
 * SecurityApplicationPermission.java
 *
 * Created on July 8, 2007, 12:44 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.security;

import java.util.HashSet;

/**
 *
 * @author michaelarace
 */
public class SecurityApplicationPermission {
    
    HashSet environmentGroups = new HashSet();
    private boolean allGroups = false;
    private boolean allEnvironments = false;
       
    /** Creates a new instance of SecurityApplicationPermission */
    public SecurityApplicationPermission(boolean allGroups, boolean allEnvironments, HashSet environmentGroups) {
        this.setAllEnvironments(allEnvironments);
        this.setAllGroups(allGroups);
        this.environmentGroups = environmentGroups;
    }
    
    public SecurityApplicationPermission(boolean allGroups, boolean allEnvironments) {
        this.allEnvironments = allEnvironments;
        this.allGroups = allGroups;
    }
    
    public void addEnvironmentGroup(String value) {
        this.environmentGroups.add(value);
    }
    
    public boolean hasPermission(int environment, long groupId) {
        if (isAllGroups() && isAllEnvironments()) {
            return true;
        } else if (environment < 0 && groupId < 0) {
            return isAllGroups() && isAllEnvironments();
        } else if (environment < 0) {
            return isAllEnvironments() && environmentGroups.contains(Long.toString(groupId));
        } else if (groupId < 0) {
            return isAllGroups() && environmentGroups.contains(Integer.toString(environment));
        } else if (isAllGroups()) {
            return environmentGroups.contains(Integer.toString(environment));
        } else if (isAllEnvironments()) {
            return environmentGroups.contains(Long.toString(groupId));
        } else {
            return environmentGroups.contains(Integer.toString(environment) + ":" + Long.toString(groupId));
        }
    }

    public boolean isAllGroups() {
        return allGroups;
    }

    public void setAllGroups(boolean allGroups) {
        this.allGroups = allGroups;
    }

    public boolean isAllEnvironments() {
        return allEnvironments;
    }

    public void setAllEnvironments(boolean allEnvironments) {
        this.allEnvironments = allEnvironments;
    }
    
}
