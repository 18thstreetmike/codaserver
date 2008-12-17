/*
 * SessionContainer.java
 *
 * Created on June 30, 2007, 4:10 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;
import org.codalang.codaserver.database.CodaResultSet;
import org.codalang.codaserver.security.SecurityGroup;
import org.codalang.codaserver.security.SecuritySystemUser;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;

/**
 *
 * @author michaelarace
 */
public class SessionContainer {
    
    Hashtable<String,SecuritySystemUser> sessions = new Hashtable();
    boolean useCacheFlag = false;
    CodaDatabase database;
    
    
    /** Creates a new instance of SessionContainer */
    public SessionContainer(CodaDatabase database, boolean useCacheFlag) {
        this.useCacheFlag = useCacheFlag;
        this.database = database;
        loadSessions();
    }
    
    public boolean touchSession(String sessionKey) {
        GregorianCalendar cal = new GregorianCalendar();
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
        if (!rs.getErrorStatus() && rs.next()) {
            Hashtable values = new Hashtable();
            values.put("session_timestamp", new GregorianCalendar().getTimeInMillis());
            connection.updateRow("sessions", "id", rs.getDataLong(0), values);
            connection.commit();
            return true;
        } else {
            return false;
        }
    }
    
    public void put(String sessionKey, SecuritySystemUser user) {
        CodaConnection connection = database.getConnection();
        Hashtable values = new Hashtable();
        values.put("session_key", sessionKey);
        values.put("session_timestamp", new GregorianCalendar().getTimeInMillis());
        values.put("user_id", user.getUserId());
        values.put("user_name", user.getUsername());
        connection.insertRow("sessions", values);
        connection.commit();
        if (useCacheFlag) {
            sessions.put(sessionKey, user);
        }
    }
    
    public void remove(String sessionKey) {
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
        if (!rs.getErrorStatus() && rs.next()) {
            connection.deleteRow("sessions", "id", rs.getDataLong(0));
            connection.commit();
        }
        if (useCacheFlag) {
            sessions.remove(sessionKey);
        }
    }
    
    public long getSessionUserId(String sessionKey) {
        if (useCacheFlag) {
            return ((SecuritySystemUser)sessions.get(sessionKey)).getUserId();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getDataLong(0);
            } else {
                return -1;
            }
        }
    }
    
    public boolean setSessionApplication(String sessionKey, long applicationId, String applicationName, int environment) {
        CodaConnection connection = database.getConnection();
        String environmentString;
        switch (environment) {
            case  2:
                environmentString = "TEST";
                break;
            case 3:
                environmentString = "PROD";
                break;
            default:
                environmentString = "DEV";
        }
        CodaResultSet rs = connection.runQuery("select id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
        if (!rs.getErrorStatus() && rs.next()) {
            
            Hashtable values = new Hashtable();
            values.put("application_id", applicationId);
            values.put("application_name", applicationName.toUpperCase());
            values.put("environment_id", environment);
            values.put("environment", environmentString);
            connection.updateRow("sessions", "id", rs.getDataLong(0), values);
            connection.commit();
            if (useCacheFlag) {
                if (sessions.containsKey(sessionKey)) {
                    ((SecuritySystemUser)sessions.get(sessionKey)).setCurrentEnvironment(environmentString);
                    ((SecuritySystemUser)sessions.get(sessionKey)).setCurrentEnvironmentId(environment);
                    ((SecuritySystemUser)sessions.get(sessionKey)).setCurrentApplication(applicationName.toUpperCase());
                    ((SecuritySystemUser)sessions.get(sessionKey)).setCurrentApplicationId(applicationId);
                } else {
                    loadSession(sessionKey);
                }
            }
        } else {
            if (useCacheFlag) {
                sessions.remove(sessionKey);
            }
        }
        
        return false;
    }
    
    public String getSessionApplication(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getCurrentApplication();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select application_name from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getData(0);
            }
        }
        return null;
    }
    
    public long getSessionApplicationId(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getCurrentApplicationId();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select application_id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getDataLong(0);
            }
        }
        return -1;
    }
    
    public String getSessionEnvironment(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getCurrentEnvironment();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select environment from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getData(0);
            }
        }
        return null;
    }
    
    public int getSessionEnvironmentId(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getCurrentEnvironmentId();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select environment_id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getDataInt(0);
            }
        }
        return -1;
    }
    
    public boolean setSessionGroup(String sessionKey, long groupId, String groupName) {
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
        if (!rs.getErrorStatus() && rs.next()) {
            Hashtable values = new Hashtable();
            values.put("group_id", groupId);
            values.put("group_name", groupName.toUpperCase());
            connection.updateRow("sessions", "id", rs.getDataLong(0), values);
            connection.commit();
            if (useCacheFlag) {
                if (sessions.containsKey(sessionKey)) {
                    ((SecuritySystemUser)sessions.get(sessionKey)).setCurrentGroup(groupName.toUpperCase());
                } else {
                    loadSession(sessionKey);
                }
            }
        } else {
            if (useCacheFlag) {
                sessions.remove(sessionKey);
            }
        }
        
        return false;
    }
    
    public String getSessionGroup(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getCurrentGroup();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select group_name from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getData(0);
            }
        }
        return null;
    }
    
    public long getSessionGroupId(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getCurrentGroupId();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select group_id from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getDataLong(0);
            }
        }
        return -1;
    }
    
    public String getSessionUsername(String sessionKey) {
        if (useCacheFlag && sessions.containsKey(sessionKey)) {
            return sessions.get(sessionKey).getUsername();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select user_name from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey),null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getData(0);
            }
        }
        return "";
    }
    
    public void loadSession(String sessionKey) {
		this.useCacheFlag = false;
		CodaConnection connection = database.getConnection();
		CodaResultSet rs = connection.runQuery("select user_id, group_name, application_name, environment, group_id, application_id, environment_id, user_name from sessions where session_key = " + connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
		if (!rs.getErrorStatus() && rs.next()) {
			SecuritySystemUser user = getSecuritySystemUser(rs.getDataLong(0));
			user.setUsername(rs.getData(7));
			user.setCurrentApplication((rs.getData(2) == null ? null : rs.getData(2).toUpperCase()));
			user.setCurrentEnvironment((rs.getData(3) == null ? null : rs.getData(3).toUpperCase()));
			user.setCurrentGroup((rs.getData(1) == null ? null : rs.getData(1).toUpperCase()));
			user.setCurrentGroupId((rs.getData(4) == null ? -1 : rs.getDataLong(4)));
			user.setCurrentApplicationId((rs.getData(5) == null ? -1 : rs.getDataLong(5)));
			user.setCurrentEnvironmentId((rs.getData(6) == null ? -1 : rs.getDataInt(6)));

			sessions.put(sessionKey, user);
		} else {
			this.remove(sessionKey);
		}
		this.useCacheFlag = true;
    }
    
    private void loadSessions() {
        if (useCacheFlag) {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select user_id, group_name, application_name, environment, group_id, application_id, environment_id, user_name, session_key from sessions", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    SecuritySystemUser user = getSecuritySystemUser(rs.getDataLong(0));
                    user.setUsername(rs.getData(7));
                    user.setCurrentApplication((rs.getData(2) == null ? null : rs.getData(2).toUpperCase()));
                    user.setCurrentEnvironment((rs.getData(3) == null ? null : rs.getData(3).toUpperCase()));
                    user.setCurrentGroup((rs.getData(1) == null ? null : rs.getData(1).toUpperCase()));
                    user.setCurrentGroupId((rs.getData(4) == null ? -1 : rs.getDataLong(4)));
                    user.setCurrentApplicationId((rs.getData(5) == null ? -1 : rs.getDataLong(5)));
                    user.setCurrentEnvironmentId((rs.getData(6) == null ? -1 : rs.getDataInt(6)));

                    sessions.put(rs.getData(8), user);
                }
            }
        }
    }
    
    private SecuritySystemUser getSecuritySystemUser(long userId) {
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select u.server_permission_name from user_server_permissions u where u.user_id = " + connection.formatStringForSQL("user_server_permissions", "user_id", Long.toString(userId)), null);
        HashSet serverPermissions = new HashSet();
        while (rs.next()) {
            serverPermissions.add(rs.getData(0));
        }
        
        rs = connection.runQuery("select g.id, g.group_name, g.display_name from user_groups u inner join groups g on g.id = u.group_id where u.user_id = " + connection.formatStringForSQL("user_server_permissions", "user_id", Long.toString(userId)), null);
        Hashtable groups = new Hashtable();
        while (rs.next()) {
            groups.put(rs.getData(1), new SecurityGroup(rs.getDataLong(0), rs.getData(1), rs.getData(2)));
        }
        
        rs = connection.runQuery("select user_name from users where id = " + connection.formatStringForSQL("users", "id", Long.toString(userId)), null);
        String username = "";
        while (rs.next()) {
            username = rs.getData(0);
        }
        
        return new SecuritySystemUser(userId, username, serverPermissions, groups);
    }
    
    public String createSession (long userId) {
        CodaConnection connection = database.getConnection();
        boolean uniqueKey = false;
        String sessionKey = "";
        while (!uniqueKey) {
            sessionKey = this.getRandomString(32);
            CodaResultSet rs1 = connection.runQuery("select id from sessions where session_key = "+ connection.formatStringForSQL("sessions", "session_key", sessionKey), null);
            uniqueKey = (rs1.getRowsReturned() == 0);
        }
    
        this.put(sessionKey, getSecuritySystemUser(userId));
        
        return sessionKey;
    }
    
    public boolean isUserInGroup(String sessionKey, String groupName) {
        if (useCacheFlag) {
            if (sessions.containsKey(sessionKey)) {
                return ((SecuritySystemUser)sessions.get(sessionKey)).isInGroup(groupName);
            }
        } else {
            CodaConnection connection = database.getConnection();
            long userId = this.getSessionUserId(sessionKey);
            if (userId > 0) {
                CodaResultSet rs = connection.runQuery("select count(ug.*) from user_groups ug inner join groups g on g.id = ug.group_id where g.id = " + connection.formatStringForSQL("groups", "group_name", groupName.toUpperCase()) + " and ug.user_id = " + connection.formatStringForSQL("user_groups", "user_id", Long.toString(userId)), null);
                if (!rs.getErrorStatus() && rs.next()) {
                    if (rs.getDataLong(0) > 0) {
                        return true;
                    }
                } 
            }
        }
        return false;
    }
    
    public boolean hasServerPermission(String sessionKey, String permissionName) {
        if (useCacheFlag) {
            if (sessions.containsKey(sessionKey)) {
                return ((SecuritySystemUser)sessions.get(sessionKey)).hasServerPermission(permissionName);
            }    
        } else {
            CodaConnection connection = database.getConnection();
            long userId = this.getSessionUserId(sessionKey);
            if (userId > 0) {
                CodaResultSet rs = connection.runQuery("select count(*) from user_server_permissions where user_id = " + connection.formatStringForSQL("user_server_permissions", "user_id", Long.toString(userId)) + " and server_permission_name = " + connection.formatStringForSQL("user_server_permissions", "server_permission_name", permissionName.toUpperCase()), null);
                if (!rs.getErrorStatus() && rs.next()) {
                    return rs.getDataInt(0) == 1;
                }
            }
        }
        return false;
    }
    
    public static String getRandomString(int chars) {
        String charSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random r = new Random();
        String retval = "";
        for (int i = 0; i < chars; i++) {
            int rand = r.nextInt(62);
            retval += charSet.substring(rand, rand +1);
        }
        return retval;
    }
}
