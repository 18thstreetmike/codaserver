/*
 * SecurityApplicationUser.java
 *
 * Created on June 23, 2007, 7:09 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.security;

import java.util.Hashtable;

/**
 *
 * @author michaelarace
 */
public class SecurityApplicationUser {
    
    private Hashtable<String,SecurityApplicationPermission> applicationPermissions;
    
    
    /** Creates a new instance of SecurityApplicationUser */
    public SecurityApplicationUser(Hashtable applicationPermissions) {
        this.applicationPermissions = applicationPermissions;
    }

    public Hashtable getApplicationPermissions() {
        return applicationPermissions;
    }

    public void setApplicationPermissions(Hashtable applicationPermissions) {
        this.applicationPermissions = applicationPermissions;
    }
    
    public boolean hasPermission(int environment, long groupId, String permissionName) {
        if (applicationPermissions.containsKey(permissionName.toUpperCase())) {
            return applicationPermissions.get(permissionName.toUpperCase()).hasPermission(environment, groupId);
        }
        return false;
    }

    
}
