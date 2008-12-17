/*
 * SecuritySystemUser.java
 *
 * Created on June 4, 2007, 2:56 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.security;

import java.util.HashSet;
import java.util.Hashtable;

/**
 *
 * @author michaelarace
 */
public class SecuritySystemUser {
    
    private HashSet serverPermissions;
    
    private Hashtable groups;
    
    private String currentApplication = null;
    
    private String currentEnvironment = null;
    
    private String currentGroup = null;
    
    private long currentApplicationId, currentGroupId;
    
    private int currentEnvironmentId;
    
    private long userId;
    
    private String username;
    
    /**
     * Creates a new instance of SecuritySystemUser
     */
    public SecuritySystemUser(long userId, String username, HashSet serverPermissions, Hashtable groups) {
        this.setServerPermissions(serverPermissions);
        this.setUsername(username);
        this.setGroups(groups);
        this.setUserId(userId);
    }
    
    public HashSet getServerPermissions() {
        return serverPermissions;
    }
    
    public void setServerPermissions(HashSet serverPermissions) {
        this.serverPermissions = serverPermissions;
    }
    
    public Hashtable getGroups() {
        return groups;
    }
    
    public void setGroups(Hashtable groups) {
        this.groups = groups;
    }
    
    public boolean hasServerPermission(String permissionName) {
        return serverPermissions.contains(permissionName);
    }
    
    public boolean isInGroup(String groupName) {
        return groups.containsKey(groupName);
    }
    
    public String getCurrentApplication() {
        return currentApplication;
    }
    
    public void setCurrentApplication(String currentApplication) {
        this.currentApplication = currentApplication;
    }
    
    public String getCurrentEnvironment() {
        return currentEnvironment;
    }
    
    public void setCurrentEnvironment(String currentEnvironment) {
        this.currentEnvironment = currentEnvironment;
    }
    
    public String getCurrentGroup() {
        return currentGroup;
    }
    
    public void setCurrentGroup(String currentGroup) {
        this.currentGroup = currentGroup;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getCurrentApplicationId() {
        return currentApplicationId;
    }

    public void setCurrentApplicationId(long currentApplicationId) {
        this.currentApplicationId = currentApplicationId;
    }

    public long getCurrentGroupId() {
        return currentGroupId;
    }

    public void setCurrentGroupId(long currentGroupId) {
        this.currentGroupId = currentGroupId;
    }

    public int getCurrentEnvironmentId() {
        return currentEnvironmentId;
    }

    public void setCurrentEnvironmentId(int currentEnvironmentId) {
        this.currentEnvironmentId = currentEnvironmentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
}
