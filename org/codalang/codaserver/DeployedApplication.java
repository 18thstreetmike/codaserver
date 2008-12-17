/*
 * DeployedApplication.java
 *
 * Created on June 23, 2007, 4:39 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import groovy.lang.GroovyClassLoader;
import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;
import org.codalang.codaserver.database.CodaResultSet;
import org.codalang.codaserver.executioncontext.ExecutionContext;
import org.codalang.codaserver.language.types.BaseCodaTable;
import org.codalang.codaserver.language.types.BaseCodaTrigger;
import org.codalang.codaserver.language.types.Database;
import org.codalang.codaserver.security.SecurityApplicationPermission;
import org.codalang.codaserver.security.SecurityApplicationUser;
import org.quartz.Scheduler;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michaelarace
 */
public class DeployedApplication {
    
    public static final int DEV_ENVIRONMENT = 0;
    public static final int TEST_ENVIRONMENT = 1;
    public static final int PROD_ENVIRONMENT = 2;
    
    private long applicationId;
    private String applicationName;
    private String displayName;
    private String devDatasourceName;
    private String testDatasourceName;
    private String prodDatasourceName;
    private Datasource production;
    private Datasource testing;
    private Datasource development;
    private CodaDatabase database;
    private boolean groupFlag;
    private boolean useCacheFlag;
    private boolean loadedFlag = false;
    private Logger logger;
    private Hashtable<Integer,Scheduler> schedulers = new Hashtable();
    private FancyGroovyClassLoader prodClassLoader;
    private FancyGroovyClassLoader testClassLoader;
    private FancyGroovyClassLoader devClassLoader;
    private ClassLoader parentLoader;
    
    
    private HashSet groups = new HashSet();
    private Hashtable users = new Hashtable();
    
    /**
     * Creates a new instance of DeployedApplication
     */
    public DeployedApplication(CodaDatabase database, String applicationName, boolean useCacheFlag, Logger logger, ClassLoader parentLoader) {
        this.setDatabase(database);
        this.applicationName = applicationName;
        this.setUseCacheFlag(useCacheFlag);
        this.setLogger(logger);
        this.parentLoader = parentLoader;
        this.prodClassLoader = new FancyGroovyClassLoader(parentLoader);
        this.testClassLoader = new FancyGroovyClassLoader(parentLoader);
        this.devClassLoader = new FancyGroovyClassLoader(parentLoader);
    }
    
    public boolean reload () {
        useCacheFlag = false;
		displayName = null;
        devDatasourceName = null;
        testDatasourceName = null;
        prodDatasourceName = null;
        production = null;
        testing = null;
        development = null;
        loadedFlag = false;
        CodaConnection connection = getDatabase().getConnection();
        CodaResultSet rs = connection.runQuery("select a.id, a.application_name, a.display_name, a.group_flag, d1.datasource_name, d2.datasource_name, d3.datasource_name  from applications a left outer join datasources d1 on a.dev_datasource_id = d1.id left outer join datasources d2 on a.test_datasource_id = d2.id left outer join datasources d3 on a.prod_datasource_id = d3.id where a.active_flag = 1 and a.application_name = " + connection.formatStringForSQL("applications", "application_name", getApplicationName().toUpperCase()), null);
        if (!rs.getErrorStatus() && rs.next()) {
            this.applicationId = rs.getDataLong(0);
            this.displayName = rs.getData(2);
            this.setGroupFlag(rs.getDataBoolean(3));
            this.devDatasourceName = rs.getData(4);
            reloadDatasource(this.DEV_ENVIRONMENT, getDevDatasourceName());
            this.setTestDatasourceName(rs.getData(5));
			this.devClassLoader = this.getEnvironmentClassLoader(1, parentLoader);
			if (getTestDatasourceName() != null) {
				reloadDatasource(this.TEST_ENVIRONMENT, getTestDatasourceName());
				this.testClassLoader = this.getEnvironmentClassLoader(2, parentLoader);
			}
			this.setProdDatasourceName(rs.getData(6));
			if (getProdDatasourceName() != null) {
			   	reloadDatasource(this.PROD_ENVIRONMENT, getProdDatasourceName());
				this.prodClassLoader = this.getEnvironmentClassLoader(3, parentLoader);
			}
			loadUsers();
            loadGroups();
            
            this.setLoadedFlag(true);
			useCacheFlag = true;
			return true;
        } else {
			useCacheFlag = true;
			return false;
        }
    }
    
    public boolean reloadDatasource(int environment, String datasourceName) {
        CodaConnection connection = getDatabase().getConnection();
        CodaResultSet rs = connection.runQuery("select id, driver_name, host_name, schema_name, user_name, pass_word from datasources where datasource_name = " + connection.formatStringForSQL("datasources", "datasource_name", datasourceName), null);
        if (!rs.getErrorStatus() && rs.next()) {
            long datasourceId = rs.getDataLong(0);
            Hashtable options = new Hashtable();
            CodaResultSet rs2 = connection.runQuery("select option_name, option_value from datasource_options where datasource_id = " + connection.formatStringForSQL("datasource_options", "datasource_id", Long.toString(datasourceId)) ,null);
            if (!rs2.getErrorStatus()) {
                while (rs2.next()) {
                    options.put(rs2.getData(0), rs2.getData(1));
                }
            }
            CodaDatabase conn;
            try {
                Class.forName(rs.getData(1));
                conn = (CodaDatabase) Class.forName(rs.getData(1)).newInstance();
                if (conn.connect(rs.getData(2), rs.getData(4), rs.getData(5), rs.getData(3), options)) {
                    conn.setLogger(getLogger());
                    switch (environment) {
                        case DeployedApplication.PROD_ENVIRONMENT:
                            this.setProduction(new Datasource(environment, conn, groupFlag, isUseCacheFlag()));
                            break;
                        case DeployedApplication.TEST_ENVIRONMENT:
                            this.setTesting(new Datasource(environment, conn, groupFlag, isUseCacheFlag()));
                            break;
                        default:
                            this.setDevelopment(new Datasource(environment, conn, groupFlag, false));
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
            
        } else {
            return false;
        }
    }
    
    public boolean reloadDatasource(int environment) {
        String columnName = "";
        switch (environment) {
            case DeployedApplication.PROD_ENVIRONMENT:
                columnName = "prod_datasource_id";
                break;
            case DeployedApplication.TEST_ENVIRONMENT:
                columnName = "test_datasource_id";
                break;
            default:
                columnName = "dev_datasource_id";
        }
        CodaConnection connection = getDatabase().getConnection();
        CodaResultSet rs = connection.runQuery("select d.datasource_name from applications a left outer join datasources d on a." +columnName + " = d.id where a.id = " + this.getApplicationId(), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return reloadDatasource(environment, rs.getData(0));
        } else {
            return false;
        }
    }

    public void reloadTables(int environment) {
        if (this.useCacheFlag) {
            CodaConnection connection = this.getEnvironmentDatasource(environment).getDatabase().getConnection();
            CodaResultSet rs = connection.runQuery("select table_name, class_file from "+ this.getEnvironmentDatasource(environment).getPrefix() +"tables ", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    this.loadClass(environment, "org.codalang.codaserver.language.tables." + CodaServer.camelCapitalize(rs.getData(0), true), rs.getData(1));
                }
            } else {
                logger.log(Level.WARNING, "Application " + this.getApplicationName() + ": Couldn't get table class files");
            }
        }
    }
    
    public void loadClass(int environment, String className, String classFile) {
        if (this.useCacheFlag) {
            this.getEnvironmentClassLoader(environment, parentLoader).parseClass(classFile);
            try {
                Class.forName(className, true, this.getEnvironmentClassLoader(environment, parentLoader));
            } catch (ClassNotFoundException ex) {
                logger.log(Level.WARNING, "Class '" + className + "' was not found in the classloader.");
            }
        }
    }
    
    public void loadGroups () {
        setGroups(new HashSet());
        CodaConnection connection = getDatabase().getConnection();
        CodaResultSet rs = connection.runQuery("select g.group_name from group_applications a inner join groups g on a.group_id = g.id where a.application_id = " + connection.formatStringForSQL("group_applications", "application_id", Long.toString(this.getApplicationId())),null);
        if (!rs.getErrorStatus()) {
            while (rs.next()) {
                getGroups().add(rs.getData(0));
            }
        }
    }
    
    private void loadUsers() {
        setUsers(new Hashtable());

        CodaConnection connection = getDatabase().getConnection();
        CodaResultSet rs = connection.runQuery("select user_id, application_permission_name, environment, group_id from user_application_permissions where application_id = " + connection.formatStringForSQL("user_application_permissions", "application_id", Long.toString(this.getApplicationId()) + " order by user_id, environment, group_id"), null);
        if (!rs.getErrorStatus()) {
            long currentUserId = -1;
            String currentPermission = null;
            Hashtable tempPermissions = new Hashtable();
            while (rs.next()) {
                if (currentUserId < 0) {
                    currentUserId = rs.getDataLong(0);
                    currentPermission = rs.getData(1).toUpperCase();
                    tempPermissions.put(currentPermission, new SecurityApplicationPermission((rs.getData(3) == null ? true : false), (rs.getData(2) == null ? true : false)));
                } else if (currentUserId != rs.getDataLong(0)) {
                    getUsers().put(currentUserId, new SecurityApplicationUser(tempPermissions));
                    tempPermissions = new Hashtable();
                    currentUserId = rs.getDataLong(0);
                    currentPermission = rs.getData(1).toUpperCase();
                    tempPermissions.put(currentPermission, new SecurityApplicationPermission((rs.getData(3) == null ? true : false), (rs.getData(2) == null ? true : false)));
                } else if (!currentPermission.equalsIgnoreCase(rs.getData(1))) {
                    currentPermission = rs.getData(1).toUpperCase();
                    tempPermissions.put(currentPermission, new SecurityApplicationPermission((rs.getData(3) == null ? true : false), (rs.getData(2) == null ? true : false)));
                }
                if (rs.getData(3) == null && rs.getData(2) == null) {
                    //skip
                } else if(rs.getData(3) == null) {
                    ((SecurityApplicationPermission)tempPermissions.get(currentPermission)).addEnvironmentGroup(rs.getData(2));
                } else if(rs.getData(2) == null) {
                    ((SecurityApplicationPermission)tempPermissions.get(currentPermission)).addEnvironmentGroup(rs.getData(3));
                } else {
                    ((SecurityApplicationPermission)tempPermissions.get(currentPermission)).addEnvironmentGroup(rs.getData(2) + ":" + rs.getData(3));
                }
            }
            if (currentUserId >= 0) {
                getUsers().put(currentUserId, new SecurityApplicationUser(tempPermissions));
            }
        }
        
    }
    
    public void reloadUser(long userId) {
        if (this.isUseCacheFlag()) {
            getUsers().remove(userId);

            CodaConnection connection = getDatabase().getConnection();
            CodaResultSet rs = connection.runQuery("select application_permission_name, environment, group_id from user_application_permissions where application_id = " + connection.formatStringForSQL("user_application_permissions", "application_id", Long.toString(this.getApplicationId()) + " and user_id = "+ connection.formatStringForSQL("user_application_permissions", "user_id", Long.toString(userId)) +" order by user_id"), null);
            if (!rs.getErrorStatus()) {
                String currentPermission = null;
                Hashtable tempPermissions = new Hashtable();
                while (rs.next()) {
                    if (currentPermission == null) {
                        currentPermission = rs.getData(0).toUpperCase();
                        tempPermissions.put(currentPermission, new SecurityApplicationPermission((rs.getData(2) == null ? true : false), (rs.getData(1) == null ? true : false)));
                    } else if (!currentPermission.equalsIgnoreCase(rs.getData(0))) {
                        currentPermission = rs.getData(0).toUpperCase();
                        tempPermissions.put(currentPermission, new SecurityApplicationPermission((rs.getData(2) == null ? true : false), (rs.getData(1) == null ? true : false)));
                    } 
                    if (rs.getData(2) == null && rs.getData(1) == null) {
                        //skip
                    } else if(rs.getData(2) == null) {
                        ((SecurityApplicationPermission)tempPermissions.get(currentPermission)).addEnvironmentGroup(rs.getData(1));
                    } else if(rs.getData(1) == null) {
                        ((SecurityApplicationPermission)tempPermissions.get(currentPermission)).addEnvironmentGroup(rs.getData(2));
                    } else {
                        ((SecurityApplicationPermission)tempPermissions.get(currentPermission)).addEnvironmentGroup(rs.getData(1) + ":" + rs.getData(2));
                    }
                }
                getUsers().put(userId, new SecurityApplicationUser(tempPermissions));
            }
        }
    }
    
    public boolean hasApplicationPermission(long userId, long groupId, int environment, String permissionName) {
        if (this.useCacheFlag && environment != 1) {
            if (users.containsKey(userId)) {
                return ((SecurityApplicationUser)users.get(userId)).hasPermission(environment, groupId, permissionName);
            } else {
                return false;
            }
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(uap.user_id) from user_application_permissions uap where uap.user_id = " + connection.formatStringForSQL("user_application_permissions", "user_id", Long.toString(userId)) + (environment > 0 ? " and (uap.environment = "+ connection.formatStringForSQL("user_application_permissions", "environment", Integer.toString(environment)) + " or uap.environment is null )" : "")+ (groupId > 0 ? " and (uap.group_id = "+ connection.formatStringForSQL("user_application_permissions", "group_id", Long.toString(groupId)) + " or uap.group_id is null)" : "") +" and uap.application_id = " + connection.formatStringForSQL("user_application_permissions", "application_id", Long.toString(this.applicationId)) + " and uap.application_permission_name = " + connection.formatStringForSQL("user_application_permissions", "application_permission_name", permissionName.toUpperCase()), null);
            if (!rs.getErrorStatus()) {
                if(rs.next()) {
                    return (rs.getDataInt(0) == 1);
                }
            }
            return false;
        }
    }
    
    public boolean canGroupUseApplication(String groupName) {
        if (groupName == null) {
            return false;
        } else if (this.useCacheFlag) {
            return groups.contains(groupName);
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(a.user_id) from group_applications a left outer join groups g on g.id = a.group_id where a.application_id = "+connection.formatStringForSQL("group_applications", "application_id", Long.toString(this.applicationId))+" and  g.group_name = " + connection.formatStringForSQL("groups", "group_name", groupName.toUpperCase()) ,null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    return rs.getDataInt(0) == 1;
                }
                return false;
            }
            return false;
        }
    }
    
    public boolean canUserUseApplication(long userId, int environment) {
        if (this.useCacheFlag && environment != 1) {
            return users.containsKey(userId);
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(a.user_id) from user_application_permissions a  where a.application_permission_name = 'CONNECT' and (a.environment = "+ connection.formatStringForSQL("user_application_permissions", "environment", Integer.toString(environment)) +" or a.environment is null) and a.application_id = "+connection.formatStringForSQL("user_application_permissions", "application_id", Long.toString(this.applicationId))+" and a.user_id = " + connection.formatStringForSQL("user_application_permissions", "user_id", Long.toString(userId)) ,null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    return rs.getDataInt(0) == 1;
                }
                return false;
            }
            return false;
        }
    }
    
    public boolean canUserUseApplication(long userId, int environment, long groupId) {
        if (this.useCacheFlag && environment != 1) {
            return users.containsKey(userId);
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select count(a.user_id) from user_application_permissions a  where a.application_permission_name = 'CONNECT' and (a.environment = "+ connection.formatStringForSQL("user_application_permissions", "environment", Integer.toString(environment)) +" or a.environment is null) and (a.group_id = "+ connection.formatStringForSQL("user_application_permissions", "group_id", Long.toString(groupId)) +" or group_id is null) and a.application_id = "+connection.formatStringForSQL("user_application_permissions", "application_id", Long.toString(this.applicationId))+" and a.user_id = " + connection.formatStringForSQL("user_application_permissions", "user_id", Long.toString(userId)) ,null);
            if (!rs.getErrorStatus()) {
                if (rs.next()) {
                    return rs.getDataInt(0) == 1;
                }
                return false;
            }
            return false;
        }
    }

    public String getNameForOperationId(int operationId) {
        switch (operationId) {
            case 1: 
                return "update";
            case 2:
                return "insert";
            case 3:
                return "delete";
            default:
                return "";
        }
    }
    
    public long getApplicationId() {
        return applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDevDatasourceName() {
        return devDatasourceName;
    }

    public Datasource getProduction() {
        return production;
    }

    public void setProduction(Datasource production) {
        this.production = production;
    }

    public Datasource getTesting() {
        return testing;
    }

    public void setTesting(Datasource testing) {
        this.testing = testing;
    }

    public Datasource getDevelopment() {
        return development;
    }

    public void setDevelopment(Datasource development) {
        this.development = development;
    }

    public CodaDatabase getDatabase() {
        return database;
    }

    public void setDatabase(CodaDatabase database) {
        this.database = database;
    }

    public boolean isGroupFlag() {
        return groupFlag;
    }

    public void setGroupFlag(boolean groupFlag) {
        this.groupFlag = groupFlag;
    }

    public boolean isUseCacheFlag() {
        return useCacheFlag;
    }

    public void setUseCacheFlag(boolean useCacheFlag) {
        this.useCacheFlag = useCacheFlag;
    }

    public boolean isLoadedFlag() {
        return loadedFlag;
    }

    public void setLoadedFlag(boolean loadedFlag) {
        this.loadedFlag = loadedFlag;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public HashSet getGroups() {
        return groups;
    }

    public void setGroups(HashSet groups) {
        this.groups = groups;
    }

    public Hashtable getUsers() {
        return users;
    }

    public void setUsers(Hashtable users) {
        this.users = users;
    }

    public String getTestDatasourceName() {
        return testDatasourceName;
    }

    public void setTestDatasourceName(String testDatasourceName) {
        this.testDatasourceName = testDatasourceName;
    }

    public String getProdDatasourceName() {
        return prodDatasourceName;
    }

    public void setProdDatasourceName(String prodDatasourceName) {
        this.prodDatasourceName = prodDatasourceName;
    }
    
    public Datasource getEnvironmentDatasource(int environment) {
        switch (environment) {
            case 1:
                return this.getDevelopment();
            case 2:
                return this.getTesting();
            case 3:
                return this.getProduction();
        }
        return null;
    }
    
    public FancyGroovyClassLoader getEnvironmentClassLoader(int environment, ClassLoader parentClassLoader) {
        //if (this.useCacheFlag  && environment != 1) {
		if (this.useCacheFlag) {
			switch (environment) {
                case 1:
                    return this.devClassLoader;
                case 2:
                    return this.testClassLoader;
                case 3:
                    return this.prodClassLoader;
            }
        } else {
            if (this.getEnvironmentDatasource(environment) == null) {
                return null;
            }
            try {
                FancyGroovyClassLoader classLoader = new FancyGroovyClassLoader(parentClassLoader);

                CodaConnection connection = this.getEnvironmentDatasource(environment).getDatabase().getConnection();

                CodaResultSet rs = connection.runQuery("select table_name, class_file from " + this.getEnvironmentDatasource(environment).getPrefix() + "tables", null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        Class groovyClass = classLoader.parseClass( rs.getData(1));
						classLoader.linkClass(groovyClass);
					}
                }
                
                rs = connection.runQuery("select procedure_name, class_file from " + this.getEnvironmentDatasource(environment).getPrefix() + "procedures", null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        Class groovyClass = classLoader.parseClass( rs.getData(1));
						classLoader.linkClass(groovyClass);
                    }
                }
                
                rs = connection.runQuery("select t.table_name, p.before_flag, p.operation_id, fs.verb_status_name, p.class_file from " + this.getEnvironmentDatasource(environment).getPrefix() + "triggers p inner join " + this.getEnvironmentDatasource(environment).getPrefix() + "tables t on t.id = p.table_id left outer join " + this.getEnvironmentDatasource(environment).getPrefix() + "form_statuses fs on fs.id = p.form_status_id", null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        Class groovyClass = classLoader.parseClass( rs.getData(4));
						classLoader.linkClass(groovyClass);
                    }
                }
                
                return classLoader;
            } catch (Exception e) {
                this.logger.log(Level.WARNING,   this.getApplicationName() + ":  Unable to connect to environment " + environment + " when getting class loader");
            }
        }
        return null;
    }
    
    private void setEnvironmentClassLoader(int environment, FancyGroovyClassLoader classLoader) {
        if (this.useCacheFlag) {
            switch (environment) {
                case 1:
                    this.devClassLoader = classLoader;
                case 2:
                    this.testClassLoader = classLoader;
                case 3:
                    this.prodClassLoader = classLoader;
            }
        }
    }
    
    public void updateEnvironmentClassLoader(int environment, GroovyClassLoader parentClassLoader) {
        if (this.useCacheFlag) {
            if (this.getEnvironmentDatasource(environment) == null) {
                return;
            }
            try {
                FancyGroovyClassLoader classLoader = new FancyGroovyClassLoader(parentClassLoader);

                CodaConnection connection = this.getEnvironmentDatasource(environment).getDatabase().getConnection();

                CodaResultSet rs = connection.runQuery("select table_name, class_file from " + this.getEnvironmentDatasource(environment).getPrefix() + "tables", null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        Class groovyClass = classLoader.parseClass( rs.getData(1));
						classLoader.linkClass(groovyClass);
					}
                }
                
                rs = connection.runQuery("select procedure_name, class_file from " + this.getEnvironmentDatasource(environment).getPrefix() + "procedures", null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        Class groovyClass = classLoader.parseClass( rs.getData(1));
						classLoader.linkClass(groovyClass);
                    }
                }
                
                rs = connection.runQuery("select t.table_name, p.before_flag, p.operation_id, fs.verb_status_name, p.class_file from " + this.getEnvironmentDatasource(environment).getPrefix() + "triggers p inner join " + this.getEnvironmentDatasource(environment).getPrefix() + "tables t on t.id = p.table_id left outer join " + this.getEnvironmentDatasource(environment).getPrefix() + "form_statuses fs on fs.id = p.form_status_id", null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
						Class groovyClass = classLoader.parseClass( rs.getData(4));
						classLoader.linkClass(groovyClass);
						
                    }
                }
                
                this.setEnvironmentClassLoader( environment, classLoader);
            } catch (Exception e) {
                this.logger.log(Level.WARNING,   this.getApplicationName() + ":  Unable to connect to environment " + environment + " when getting class loader");
            }
        }
    }
    
    public void loadClassInEnvironment(int environment, String className, String classBytes) {
        if (this.useCacheFlag) {
            Class groovyClass = this.getEnvironmentClassLoader(environment, null).parseClass(classBytes);
			this.getEnvironmentClassLoader(environment, null).linkClass(groovyClass);
		}
    }
    
    public boolean runTrigger (ExecutionContext context, String tableName, String operationName, boolean beforeFlag, Database executingDatabase, Hashtable<String,String> nextHashtable, Hashtable<String, String> prevHashtable) throws ClassNotFoundException, IllegalAccessException, InstantiationException, CodaException {
        GroovyClassLoader executingClassLoader = context.getClassLoader();
        if (executingClassLoader == null) {
            return false;
        } else {
            BaseCodaTrigger trigger;
            try {
                Class triggerClass = executingClassLoader.loadClass("org.codalang.codaserver.language.triggers." + CodaServer.camelCapitalize(tableName, true) + CodaServer.camelCapitalize(beforeFlag ? "before" : "after", true) + CodaServer.camelCapitalize(operationName, true));
                trigger = (BaseCodaTrigger) triggerClass.newInstance();
            } catch (Exception e) {
                return false;
            }
            
            Class tableClass = executingClassLoader.loadClass("org.codalang.codaserver.language.tables." + CodaServer.camelCapitalize(tableName, true));
            BaseCodaTable next = (BaseCodaTable) tableClass.newInstance();
            next.setFields(nextHashtable);
            
            BaseCodaTable prev = (BaseCodaTable) tableClass.newInstance();
            if (prevHashtable != null)
				prev.setFields(prevHashtable);
            
            try {
				trigger.fire(executingDatabase, next, prev);
			} catch (Exception ex) {
				int i = 0;
			} catch (Error er) {
				int i = 0;
			}
		}
        return true;
    }

} 
