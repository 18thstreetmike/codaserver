/*
 * Datasource.java
 *
 * Created on June 23, 2007, 6:58 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;
import org.codalang.codaserver.database.CodaResultSet;
import org.codalang.codaserver.security.SecurityDatasourceGroup;
import org.codalang.codaserver.security.SecurityDatasourceUser;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class Datasource {
    
    public static final int TABLE_SELECT = 1;
    public static final int TABLE_INSERT = 2;
    public static final int TABLE_UPDATE = 3;
    public static final int TABLE_DELETE = 4;
    
    public static final int FORM_STATUS_VIEW = 1;
    public static final int FORM_STATUS_CALL = 2;
    public static final int FORM_STATUS_UPDATE = 3;
    
    public static final int PROCEDURE_EXECUTE = 1;
    
    int environment;
    CodaDatabase database;
    boolean useCacheFlag;
    boolean groupFlag;
    String prefix = "CA_";
    
    Hashtable<Long,SecurityDatasourceGroup> groups;
    Hashtable<Long,SecurityDatasourceUser> users;
    Hashtable<String,Vector> roles;
    
    /** Creates a new instance of Datasource */
    public Datasource(int environment, CodaDatabase database, boolean groupFlag, boolean useCacheFlag) {
        this.environment = environment;
        this.database = database;
        this.useCacheFlag = useCacheFlag;
        this.groupFlag = groupFlag;
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select system_value from coda_system_information where system_property = 'PREFIX'", null);
        if (!rs.getErrorStatus() && rs.next()) {
            this.prefix = rs.getData(0);
        }
        if (useCacheFlag && !groupFlag) {
            loadRoles(-1);
            loadUsers(-1);
        } else {
            loadGroups();
        }
    }
    
    public Hashtable loadRoles(long groupId) {
        Hashtable retval = new Hashtable();
        if (this.useCacheFlag) {
            roles = new Hashtable();
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select r.role_name, ru.user_id from " + prefix + "user_roles ur inner join " + prefix + "roles r on r.id = ur.role_id "+(groupId > 0 ? " where ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) : " ")+" order r.id", null);
            if (!rs.getErrorStatus()) {
                String currentRoleName = "";
                Vector tempUsers = new Vector();
                while (rs.next()) {
                    if (currentRoleName.equals("")) {
                        currentRoleName = rs.getData(0);
                    } else if (!currentRoleName.equalsIgnoreCase(rs.getData(0))) {
                        retval.put(currentRoleName, tempUsers);
                        currentRoleName = rs.getData(0);
                        tempUsers = new Vector();
                    }
                    tempUsers.add(rs.getDataLong(1));
                }
                if (!currentRoleName.equals("")) {
                    retval.put(currentRoleName, tempUsers);
                }
            }
        }
        return retval;
    }
    
    public Hashtable loadUsers(long groupId) {
        Hashtable retval = new Hashtable();
        if (this.useCacheFlag) {
            HashSet allUsers = new HashSet();
            Hashtable tempPermissions = new Hashtable(), tempTablePermissions = new Hashtable(), tempFormStatusPermissions = new Hashtable(), tempProcedurePermissions = new Hashtable();           

            CodaConnection connection = database.getConnection();
            
            // do the permissions stuff
            CodaResultSet rs = connection.runQuery("select ur.user_id, p.permission_name from (" + prefix + "user_roles ur inner join " + prefix + "role_permissions rp on ur.role_id = rp.role_id) inner join " + prefix + "permissions p on p.id = rp.permission_id "+(groupId > 0 ? " where ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) : " ")+ " order by user_id", null);
            if (!rs.getErrorStatus()) {
                long currentUserId = -1;
                HashSet tempPermissions2 = new HashSet();
                while (rs.next()) {
                    if (currentUserId < 0) {
                        currentUserId = rs.getDataLong(0);
                    } else if (currentUserId != rs.getDataLong(0)) {
                        allUsers.add(currentUserId);
                        tempPermissions.put(currentUserId, tempPermissions2);
                        currentUserId = rs.getDataLong(0);
                        tempPermissions2 = new HashSet();
                    }
                    tempPermissions2.add(rs.getData(1).toUpperCase());
                }
                if (currentUserId >= 0) {
                    allUsers.add(currentUserId);
                    tempPermissions.put(currentUserId, tempPermissions2);    
                }
            }
            
            // do the table stuff
            rs = connection.runQuery("select ur.user_id, t.table_name, rt.select_flag, rt.insert_flag, rt.update_flag, rt.delete_flag from (" + prefix + "user_roles ur inner join " + prefix + "role_tables rt on ur.role_id = rt.role_id) inner join " + prefix + "tables t on t.id = rt.table_id "+(groupId > 0 ? " where ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) : " ")+" order by ru.user_id, t.id", null);
            if (!rs.getErrorStatus()) {
                long currentUserId = -1;
                Hashtable tempPermissions2 = new Hashtable();
                while (rs.next()) {
                    if (currentUserId < 0) {
                        currentUserId = rs.getDataLong(0);
                    } else if (currentUserId != rs.getDataLong(0)) {
                        allUsers.add(currentUserId);
                        tempTablePermissions.put(currentUserId, tempPermissions2);
                        currentUserId = rs.getDataLong(0);
                        tempPermissions2 = new Hashtable();
                    }
                    tempPermissions2.put(rs.getData(1).toUpperCase(), rs.getData(2) + rs.getData(3) + rs.getData(4) + rs.getData(5));
                }
                if (currentUserId >= 0) {
                    allUsers.add(currentUserId);
                    tempTablePermissions.put(currentUserId, tempPermissions2);    
                }
            }
            
            // do the form status stuff
            rs = connection.runQuery("select ur.user_id, t.table_name, fs.adj_status_name, fs.verb_status_name, rfs.view_flag, rfs.update_flag, rfs.call_flag from ((" + prefix + "user_roles ur inner join " + prefix + "role_form_statuses rfs on ur.role_id = rfs.role_id) inner join " + prefix + "form_statuses fs on fs.id = rfs.form_status_id) inner join " + prefix + "tables t on t.id = rfs.table_id "+(groupId > 0 ? " where ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) : " ")+" order by ru.user_id, t.id", null);
            if (!rs.getErrorStatus()) {
                long currentUserId = -1;
                Hashtable tempPermissions2 = new Hashtable();
                while (rs.next()) {
                    if (currentUserId < 0) {
                        currentUserId = rs.getDataLong(0);
                    } else if (currentUserId != rs.getDataLong(0)) {
                        allUsers.add(currentUserId);
                        tempFormStatusPermissions.put(currentUserId, tempPermissions2);
                        currentUserId = rs.getDataLong(0);
                        tempPermissions2 = new Hashtable();
                    }
                    tempPermissions2.put(rs.getData(1).toUpperCase() + ":" + rs.getData(2).toUpperCase() + ":" + rs.getData(3).toUpperCase(), rs.getData(4) + rs.getData(5) + rs.getData(6));
                }
                if (currentUserId >= 0) {
                    allUsers.add(currentUserId);
                    tempFormStatusPermissions.put(currentUserId, tempPermissions2);    
                }
            }
            
            // do the procedure stuff
            rs = connection.runQuery("select ur.user_id, p.procedure_name, rp.execute_flag from (" + prefix + "user_roles ur inner join " + prefix + "role_procedures rp on ur.role_id = rp.role_id) inner join " + prefix + "procedures p on p.id = rp.procedure_id "+(groupId > 0 ? " where ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) : " ")+" order by ru.user_id, p.id", null);
            if (!rs.getErrorStatus()) {
                long currentUserId = -1;
                Hashtable tempPermissions2 = new Hashtable();
                while (rs.next()) {
                    if (currentUserId < 0) {
                        currentUserId = rs.getDataLong(0);
                    } else if (currentUserId != rs.getDataLong(0)) {
                        allUsers.add(currentUserId);
                        tempProcedurePermissions.put(currentUserId, tempPermissions2);
                        currentUserId = rs.getDataLong(0);
                        tempPermissions2 = new Hashtable();
                    }
                    tempPermissions2.put(rs.getData(1).toUpperCase(), rs.getData(2));
                }
                if (currentUserId >= 0) {
                    allUsers.add(currentUserId);
                    tempProcedurePermissions.put(currentUserId, tempPermissions2);    
                }
            }
            
            // create all the user objects
            Iterator it = allUsers.iterator();
            while(it.hasNext()) {
                long tempUserId = Long.parseLong((String)it.next());
                retval.put(tempUserId, new SecurityDatasourceUser((HashSet)tempPermissions.get(tempUserId), (Hashtable)tempTablePermissions.get(tempUserId), (Hashtable)tempFormStatusPermissions.get(tempUserId), (Hashtable)tempProcedurePermissions.get(tempUserId)));
            }   
        }
        return retval;
    }
    
    public void reloadUser(long userId, long groupId) {
        if (this.useCacheFlag) {
            users.remove(userId);
            HashSet tempPermissions = new HashSet();
            Hashtable tempTablePermissions = new Hashtable(), tempFormStatusPermissions = new Hashtable(), tempProcedurePermissions = new Hashtable();

            CodaConnection connection = database.getConnection();

            CodaResultSet rs = connection.runQuery("select ur.user_id, p.permission_name from (" + prefix + "user_roles ur inner join " + prefix + "role_permissions rp on ur.role_id = rp.role_id) inner join " + prefix + "permissions p on p.id = rp.permission_id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = " +connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId))+ " order by user_id", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    tempPermissions.add(rs.getData(1).toUpperCase());
                }
            }
            
            // do the table stuff
            rs = connection.runQuery("select ur.user_id, t.table_name, rt.select_flag, rt.insert_flag, rt.update_flag, rt.delete_flag from (" + prefix + "user_roles ur inner join " + prefix + "role_tables rt on ur.role_id = rt.role_id) inner join " + prefix + "tables t on t.id = rt.table_id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = " +connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId))+ " order by ru.user_id, t.id", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    tempTablePermissions.put(rs.getData(1).toUpperCase(), rs.getData(2) + rs.getData(3) + rs.getData(4) + rs.getData(5));
                }
            }
            
            // do the form status stuff
            rs = connection.runQuery("select ur.user_id, t.table_name, fs.adj_status_name, fs.verb_status_name, rfs.view_flag, rfs.update_flag, rfs.call_flag from ((" + prefix + "user_roles ur inner join " + prefix + "role_form_statuses rfs on ur.role_id = rfs.role_id) inner join " + prefix + "form_statuses fs on fs.id = rfs.form_status_id) inner join " + prefix + "tables t on t.id = rfs.table_id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = " +connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId))+ " order by ru.user_id, t.id", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    tempFormStatusPermissions.put(rs.getData(1).toUpperCase() + ":" + rs.getData(2).toUpperCase() + ":" + rs.getData(3).toUpperCase(), rs.getData(4) + rs.getData(5) + rs.getData(6));
                }
            }
            
            // do the procedure stuff
            rs = connection.runQuery("select ur.user_id, p.procedure_name, rp.execute_flag from (" + prefix + "user_roles ur inner join " + prefix + "role_procedures rp on ur.role_id = rp.role_id) inner join " + prefix + "procedures p on p.id = rp.procedure_id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = " +connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId))+ " order by ru.user_id, p.id", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    tempProcedurePermissions.put(rs.getData(1).toUpperCase(), rs.getData(2));
                }
            }
            users.put(userId, new SecurityDatasourceUser(tempPermissions, tempTablePermissions, tempFormStatusPermissions, tempProcedurePermissions));
        }
    }
    
    public boolean hasPermission(long userId, long groupId, String permissionName) {
        if (this.useCacheFlag) {
            if (groupId < 0) {
                if (users.containsKey(userId)) {
                    return ((SecurityDatasourceUser)users.get(userId)).hasPermission(permissionName);
                }
            } else if (groups.containsKey(groupId)) {
                SecurityDatasourceGroup temp = (SecurityDatasourceGroup)groups.get(Long.toString(groupId));
                if (temp.getUsers().containsKey(userId)) {
                    return ((SecurityDatasourceUser)temp.getUsers().get(userId)).hasPermission(permissionName);
                }
            }
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(rp.*) from (" + prefix + "user_roles ur inner join " + prefix + "role_permissions rp on ur.role_id = rp.role_id) inner join " + prefix + "permissions p on p.id = rp.permission_id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = "+ connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " and p.permission_name = " + connection.formatStringForSQL(prefix+"permissions", "permission_name", permissionName).toUpperCase(), null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    if (rs.getDataInt(0) > 0) {
                        return true;
                    }
                }
            }
        } 
        return false;
    }
    
    public boolean hasTablePermission(long userId, long groupId, int permission, String tableName) {
        if (this.useCacheFlag) {
            if (groupId < 0) {
                if (users.containsKey(userId)) {
                    return ((SecurityDatasourceUser)users.get(userId)).hasTablePermission(permission, tableName);
                }
            } else if (groups.containsKey(groupId)) {
                SecurityDatasourceGroup temp = (SecurityDatasourceGroup)groups.get(Long.toString(groupId));
                if (temp.getUsers().containsKey(userId)) {
                    return ((SecurityDatasourceUser)temp.getUsers().get(userId)).hasTablePermission(permission, tableName);
                }
            }
            
        } else {
            String columnName = "";
            switch (permission) {
                case Datasource.TABLE_INSERT:
                    columnName = "insert_flag";
                    break;
                case Datasource.TABLE_UPDATE:
                    columnName = "update_flag";
                    break;
                case Datasource.TABLE_DELETE:
                    columnName = "delete_flag";
                    break;
                default:
                    columnName = "select_flag";
            }
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(ur.user_id) from (" + prefix + "user_roles ur inner join " + prefix + "role_tables rt on ur.role_id = rt.role_id) inner join " + prefix + "tables t on rt.table_id = t.id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = "+ connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " and t.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName).toUpperCase() + " and " + columnName + " = 1 ", null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    if (rs.getDataInt(0) > 0) {
                        return true;
                    }
                }
            }
        } 
        return false;
    }
    
    public boolean hasRole(long userId, long groupId, String roleName) {
        CodaConnection connection = database.getConnection();
        String prefix = this.getPrefix();
        CodaResultSet rs = connection.runQuery("select count(ur.user_id) from "+prefix+"user_roles ur inner join "+prefix+"roles r on r.id = ur.role_id where user_id = " + connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + (groupId > 0 ? " and ur.group_id = " + connection.formatStringForSQL(prefix+"user_roles", "group_id", Long.toString(groupId)) + " or ur.group_id is null ": " and ur.group_id is null ") + " and r.role_name = " + connection.formatStringForSQL(prefix+"roles", "role_name", roleName.toUpperCase()),null);
        if (!rs.getErrorStatus() && rs.next()) {
            return rs.getDataInt(0) > 1;
        }   
        return false;
    }
    
    public boolean hasFormStatusPermission(long userId, long groupId, int permission, String tableName, String formStatusName, boolean adjFlag) {
        if (this.useCacheFlag) {
            if (groupId < 0) {
                if (users.containsKey(userId)) {
                    return ((SecurityDatasourceUser)users.get(userId)).hasFormStatusPermission(permission, tableName, formStatusName, adjFlag);
                }
            } else if (groups.containsKey(groupId)) {
                SecurityDatasourceGroup temp = (SecurityDatasourceGroup)groups.get(Long.toString(groupId));
                if (temp.getUsers().containsKey(userId)) {
                    return ((SecurityDatasourceUser)temp.getUsers().get(userId)).hasFormStatusPermission(permission, tableName, formStatusName, adjFlag);
                }
            }
            
        } else {
            String columnName = "";
            switch (permission) {
                case Datasource.FORM_STATUS_CALL:
                    columnName = "call_flag";
                    break;
                case Datasource.FORM_STATUS_UPDATE:
                    columnName = "update_flag";
                    break;
                default:
                    columnName = "view_flag";
            }
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(rfs.role_id) from ((" + prefix + "user_roles ur inner join " + prefix + "role_form_statuses rfs on ur.role_id = rfs.role_id) inner join " + prefix + "form_statuses fs on rfs.form_status_id = fs.id) inner join " + prefix + "tables on t.id = fs.table_id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = "+ connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " and t.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName).toUpperCase() + " and " + columnName + " = 1 and " + (adjFlag ? "adj_status_name" : "verb_status_name") + " = " + connection.formatStringForSQL(prefix+"form_statuses", (adjFlag ? "adj_status_name" : "verb_status_name"), formStatusName).toUpperCase(), null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    if (rs.getDataInt(0) > 0) {
                        return true;
                    }
                }
            }
        } 
        return false;
    }
    
    public boolean hasFormStatusPermission(long userId, long groupId, int permission, String tableName, long formStatusId) {
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select adj_status_name from form_statuses where id = " + connection.formatStringForSQL("form_statuses", "id", Long.toString(formStatusId)), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return this.hasFormStatusPermission(userId, groupId, permission, tableName, rs.getData(0), true);
        } 
        return false;
    }
    
    public Vector<Long> getFormStatusesForPermission(long userId, long groupId, int permission, String formName, boolean allFlag) {
        CodaConnection connection = database.getConnection();
		String sql = "";
		if (!allFlag) {
			sql = "select rfs.form_status_id from ((" + prefix + "user_roles ur inner join " + prefix + "role_form_statuses rfs on ur.role_id = rfs.role_id) inner join " + prefix + "form_statuses fs on fs.id = rfs.form_status_id) inner join " + prefix + "tables t on t.id = fs.table_id and t.table_name = " + connection.formatStringForSQL(prefix + "tables", "table_name", formName)+" where " + (permission == this.FORM_STATUS_VIEW ? "rfs.view_flag = " + connection.formatStringForSQL(prefix + "role_form_statuses", "view_flag", "1") : (permission == this.FORM_STATUS_UPDATE ? "rfs.update_flag = " + connection.formatStringForSQL(prefix + "role_form_statuses", "update_flag", "1") : "rfs.call_flag = " + connection.formatStringForSQL(prefix + "role_form_statuses", "call_flag", "1"))) +(groupId > 0 ? " and ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) : " ") + " order by ur.user_id, t.id";
		} else {
			sql = "select fs.id from " + prefix + "form_statuses fs inner join " + prefix + "tables t on t.id = fs.table_id and t.table_name = " + connection.formatStringForSQL(prefix + "tables", "table_name", formName);
		}
		CodaResultSet rs = connection.runQuery(sql, null);
        if (!rs.getErrorStatus()) {
            Vector<Long> temp = new Vector();
            while (rs.next()) {
                temp.add(rs.getDataLong(0));
            }
            return temp;
        } else {
            return new Vector();
        }
    }
    
    public boolean hasProcedurePermission(long userId, long groupId, int permission, String procedureName) {
        if (this.useCacheFlag) {
            if (groupId < 0) {
                if (users.containsKey(userId)) {
                    return ((SecurityDatasourceUser)users.get(userId)).hasProcedurePermission(permission, procedureName);
                }
            } else if (groups.containsKey(groupId)) {
                SecurityDatasourceGroup temp = (SecurityDatasourceGroup)groups.get(Long.toString(groupId));
                if (temp.getUsers().containsKey(userId)) {
                    return ((SecurityDatasourceUser)temp.getUsers().get(userId)).hasProcedurePermission(permission, procedureName);
                }
            }
        } else {
            String columnName = "";
            switch (permission) {
               default:
                    columnName = "execute_flag";
            }
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(rp.role_id) from (" + prefix + "user_roles ur inner join " + prefix + "role_procedures rp on ur.role_id = rp.role_id) inner join " + prefix + "procedures p on rp.procedure_id = p.id where "+(groupId > 0 ? " ur.group_id = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) + " and " : " ")+" ur.user_id = "+ connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " and p.procedure_name = " + connection.formatStringForSQL(prefix+"procedures", "procedure_name", procedureName).toUpperCase() + " and " + columnName + " = 1 ", null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    if (rs.getDataInt(0) > 0) {
                        return true;
                    }
                }
            }
        } 
        return false;
    }
    
    public CodaDatabase getDatabase() {
        return database;
    }
    
    public String getPrefix() {
        return prefix;
    }

    private void loadGroups() {
        groups = new Hashtable();
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select distinct group_id from user_roles order by group_id", null);
        if (!rs.getErrorStatus()) {
            while (rs.next()) {
                groups.put(rs.getDataLong(0), new SecurityDatasourceGroup(loadUsers(rs.getDataLong(0)), loadRoles(rs.getDataLong(0))));
            }
        }
    }
    
    public Hashtable<String, String> getDistinctColumnsForTables (Vector<String> tables) {
        CodaConnection connection = database.getConnection();
        Hashtable<String, String> retval = new Hashtable();
        Vector<String> abandoned = new Vector();
        String tableList = "";
        boolean first = true;
        for (String item : tables) {
            if (first) {
                first = false;
            } else {
                tableList += ",";
            }
            tableList += connection.formatStringForSQL("tables", "table_name", item);
        }
        CodaResultSet rs = connection.runQuery("select f.field_name, t.table_name from tables t inner join table_fields f on t.id = f.table_id where table_name in ("+tableList+")", null);
        if (!rs.getErrorStatus()) {
            while (rs.next()) {
                if (retval.containsKey(rs.getData(0).toUpperCase())) {
                    retval.remove(rs.getData(0).toUpperCase());
                    abandoned.add(rs.getData(0).toUpperCase());
                } else {
                    if (!abandoned.contains(rs.getData(0).toUpperCase())) {
                        retval.put(rs.getData(0).toUpperCase(), rs.getData(1).toUpperCase());
                    }
                }
            }
        }
        return retval;
    }
}
