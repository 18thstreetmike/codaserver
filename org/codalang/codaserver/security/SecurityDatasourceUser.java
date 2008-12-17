/*
 * SecurityDatasourceUser.java
 *
 * Created on June 24, 2007, 12:04 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.security;

import org.codalang.codaserver.Datasource;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

/**
 *
 * @author michaelarace
 */
public class SecurityDatasourceUser {
    
    private HashSet<String> permissions;
    private Hashtable tablePermissions;
    private Hashtable formStatusPermissions;
    private Hashtable procedurePermissions;
    
    /** Creates a new instance of SecurityDatasourceUser */
    public SecurityDatasourceUser(HashSet permissions, Hashtable tablePermissions, Hashtable formStatusPermissions, Hashtable procedurePermissions) {
        this.setPermissions(permissions);
        this.setTablePermissions(tablePermissions);
        this.setFormStatusPermissions(formStatusPermissions);
        this.setProcedurePermissions(procedurePermissions);
    }

    public HashSet getPermissions() {
        return permissions;
    }

    public void setPermissions(HashSet permissions) {
        this.permissions = permissions;
    }

    public Hashtable getTablePermissions() {
        return tablePermissions;
    }

    public void setTablePermissions(Hashtable tablePermissions) {
        this.tablePermissions = tablePermissions;
    }

    public Hashtable getFormStatusPermissions() {
        return formStatusPermissions;
    }

    public void setFormStatusPermissions(Hashtable formStatusPermissions) {
        this.formStatusPermissions = formStatusPermissions;
    }

    public Hashtable getProcedurePermissions() {
        return procedurePermissions;
    }

    public void setProcedurePermissions(Hashtable procedurePermissions) {
        this.procedurePermissions = procedurePermissions;
    }
    
    public boolean hasPermission(String permissionName) {
        return permissions.contains(permissionName.toUpperCase());
    }
    
    public boolean hasTablePermission(int permission, String tableName) {
        if (!tablePermissions.containsKey(tableName.toUpperCase())) {
            return false;
        }
        switch(permission) {
            case Datasource.TABLE_INSERT:
                if (((String)tablePermissions.get(tableName.toUpperCase())).substring(1,1).equals("1")) {
                    return true;
                }
                break;
            case Datasource.TABLE_UPDATE:
                if (((String)tablePermissions.get(tableName.toUpperCase())).substring(2,1).equals("1")) {
                    return true;
                }
                break;
            case Datasource.TABLE_DELETE:
                if (((String)tablePermissions.get(tableName.toUpperCase())).substring(3,1).equals("1")) {
                    return true;
                }
                break;
            default:
                if (((String)tablePermissions.get(tableName.toUpperCase())).substring(0,1).equals("1")) {
                    return true;
                }
        }
        return false;
    }
    
    public boolean hasFormStatusPermission(int permission, String tableName, String formStatusName, boolean adjFlag) {
        Enumeration en = formStatusPermissions.keys();
        Hashtable adjectives = new Hashtable(), verbs = new Hashtable();
        while (en.hasMoreElements()) {
            String key = (String)en.nextElement();
            if (key.startsWith(tableName.toUpperCase())) {
                int firstColon = key.indexOf(":");
                int secondColon = key.indexOf(":", firstColon + 1);
                adjectives.put(key.substring(firstColon + 1, secondColon - firstColon + 1).toUpperCase(), key);
                verbs.put(key.substring(secondColon + 1).toUpperCase(), key);
            }
        }
        if (verbs.size() == 0 || (adjFlag && !adjectives.containsKey(formStatusName) ) || (!adjFlag && !verbs.containsKey(formStatusName))) {
            return false;
        }
        String key = (adjFlag ? (String)adjectives.get(formStatusName) : (String)verbs.get(formStatusName)).toUpperCase();
        switch(permission) {
            case Datasource.FORM_STATUS_CALL:
                if (((String)formStatusPermissions.get(key)).substring(2,1).equals("1")) {
                    return true;
                }
                break;
            case Datasource.FORM_STATUS_UPDATE:
                if (((String)formStatusPermissions.get(key)).substring(1,1).equals("1")) {
                    return true;
                }
                break;
            default:
                if (((String)formStatusPermissions.get(key)).substring(0,1).equals("1")) {
                    return true;
                }
        }
        return false;
    }
    
    public boolean hasProcedurePermission(int permission, String procedureName) {
        if (!tablePermissions.containsKey(procedureName.toUpperCase())) {
            return false;
        }
        switch(permission) {
            default:
                if (((String)procedurePermissions.get(procedureName.toUpperCase())).substring(0,1).equals("1")) {
                    return true;
                }
        }
        return false;
    }
}
