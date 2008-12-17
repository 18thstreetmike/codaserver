/*
 * SecurityDatasourceGroup.java
 *
 * Created on July 8, 2007, 8:29 PM
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
public class SecurityDatasourceGroup {
    
    private Hashtable users;
    private Hashtable roles;
    
    /** Creates a new instance of SecurityDatasourceGroup */
    public SecurityDatasourceGroup(Hashtable users, Hashtable roles) {
        this.setUsers(users);
        this.setRoles(roles);
    }

    public Hashtable getUsers() {
        return users;
    }

    public void setUsers(Hashtable users) {
        this.users = users;
    }

    public Hashtable getRoles() {
        return roles;
    }

    public void setRoles(Hashtable roles) {
        this.roles = roles;
    }
    
    
    
}
