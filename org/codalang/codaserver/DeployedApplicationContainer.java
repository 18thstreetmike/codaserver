/*
 * DeployedApplicationContainer.java
 *
 * Created on June 28, 2007, 2:12 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;
import org.codalang.codaserver.database.CodaResultSet;

import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michaelarace
 */
public class DeployedApplicationContainer {
    
    private Hashtable<String,DeployedApplication> deployedApplications;
    boolean useCacheFlag = false;
    CodaDatabase database;
    Logger logger;
    
    /** Creates a new instance of DeployedApplicationContainer */
    public DeployedApplicationContainer(CodaDatabase database, boolean useCacheFlag, Logger logger) {
        this.useCacheFlag = useCacheFlag;
        this.logger = logger;
        this.database = database;
    }

    public Hashtable getDeployedApplications() {
        return deployedApplications;
    }

    public void setDeployedApplications(Hashtable deployedApplications) {
        this.deployedApplications = deployedApplications;
    }
    
    public boolean canUserUseApplication(String applicationName, long userId, int environment ) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            return (((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).canUserUseApplication(userId, environment));
        }
        return false;
    }
    
    public boolean canUserUseApplication(String applicationName, long userId, int environment, long groupId) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            return (((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).canUserUseApplication(userId, environment, groupId));
        }
        return false;
    }
    
    public boolean canGroupUseApplication(String applicationName, String groupName) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            return (((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).canGroupUseApplication(groupName));
        }
        return false;
    }
    
    public boolean hasApplicationPermission(String applicationName, long userId, int environment, long groupId, String permissionName ) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            return (((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).hasApplicationPermission(userId, groupId, environment, permissionName));
        }
        return false;
    }
    
    public boolean hasPermission(String applicationName, long userId, long groupId, int environment, String permissionName) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
					datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }
            if (datasource != null) {
                return this.hasApplicationPermission(applicationName, userId, environment, groupId, "DEVELOPER") || datasource.hasPermission(userId, groupId, permissionName);
            }
        }
        return false;
    }
    
    public boolean hasTablePermission(String applicationName, long userId, long groupId, int environment, int permission, String tableName) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }
            if (datasource != null) {
                return this.hasApplicationPermission(applicationName, userId, environment, groupId, "DEVELOPER") || datasource.hasTablePermission(userId, groupId, permission, tableName);
            }
                
        }
        return false;
    }
    
    public boolean hasFormStatusPermission(String applicationName, long userId, long groupId, int environment, int permission, String tableName, String formStatusName, boolean adjFlag) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }
            if (datasource != null) {
                return this.hasApplicationPermission(applicationName, userId, environment, groupId, "DEVELOPER") || datasource.hasFormStatusPermission(userId, groupId, permission, tableName, formStatusName, adjFlag);
            }
                
        }
        return false;
    }
    
    public boolean hasFormStatusPermission(String applicationName, long userId, long groupId, int environment, int permission, String tableName, long formStatusId) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }
            if (datasource != null) {
                return this.hasApplicationPermission(applicationName, userId, environment, groupId, "DEVELOPER") || datasource.hasFormStatusPermission(userId, groupId, permission, tableName, formStatusId);
            }
                
        }
        return false;
    }
    
    public Vector<Long> getFormStatusesForPermission(String applicationName, long userId, long groupId, int environment, int permission, String tableName) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }
            if (datasource != null) {
                return datasource.getFormStatusesForPermission(userId, groupId, permission, tableName, this.hasApplicationPermission(applicationName, userId, environment, groupId, "DEVELOPER"));
            }
                
        }
        return new Vector();
    }
    
    public boolean hasProcedurePermission(String applicationName, long userId, long groupId, int environment, int permission, String procedureName) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }
            if (datasource != null) {
                return this.hasApplicationPermission(applicationName, userId, environment, groupId, "DEVELOPER") || datasource.hasProcedurePermission(userId, groupId, permission, procedureName);
            }
                
        }
        return false;
    }
    
    public Datasource getDatasource(String applicationName, int environment) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            Datasource datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTesting();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProduction();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevelopment();
                    break;
            }    
            return datasource;
        }
        return null;
    }
    
    public String getDatasourceName(String applicationName, int environment) {
        if(deployedApplications.containsKey(applicationName.toUpperCase())) {
            String datasource = null;
            switch (environment) {
                case 2: 
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getTestDatasourceName();
                    break;
                case 3:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getProdDatasourceName();
                    break;
                default:
                    datasource = ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).getDevDatasourceName();
                    break;
            }    
            return datasource;
        }
        return null;
    }
    
    public void removeApplication(String applicationName) {
        deployedApplications.remove(applicationName.toUpperCase());
    }
    
    public void deployApplication(String applicationName, boolean quiet, ClassLoader parentLoader) {
        if (deployedApplications.containsKey(applicationName.toUpperCase())) {
            ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).reload();
        } else {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select application_name from applications where active_flag = 1 and application_name = "+connection.formatStringForSQL("applications", "application_name", applicationName.toUpperCase())+" order by application_name asc", null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    DeployedApplication dp = new DeployedApplication(database, rs.getData(0).toUpperCase(), useCacheFlag, logger, parentLoader);
                    if (!quiet) System.out.println("Deploying application " + rs.getData(0).toLowerCase() + "...");
                    logger.log(Level.INFO, "Deploying application " + rs.getData(0).toLowerCase() + "...");
                    if (dp.reload()) {
                        if (!quiet) System.out.println("...Done");
                        logger.log(Level.INFO, "Deployed!");
                        deployedApplications.put(rs.getData(0).toUpperCase(), dp);
                    } else {
                        if (!quiet) System.out.println("...Failed");
                        logger.log(Level.INFO, "Not Deployed!");
                    }
                }
            }
        }
    }
    
    public boolean isGroupApplication(String applicationName) {
        if (deployedApplications.containsKey(applicationName.toUpperCase())) {
            return ((DeployedApplication)deployedApplications.get(applicationName.toUpperCase())).isGroupFlag();
        }
        return false;
    }
    
    public void initialize ( boolean quiet, ClassLoader parentLoader) {
        deployedApplications = new Hashtable();
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select application_name from applications where active_flag = 1 order by application_name asc", null);
        if (!rs.getErrorStatus()) {
            while (rs.next()) {
                DeployedApplication dp = new DeployedApplication(database, rs.getData(0).toUpperCase(), useCacheFlag, logger, parentLoader);
                if (!quiet) System.out.println("Deploying application " + rs.getData(0).toLowerCase() + "...");
                logger.log(Level.INFO, "Deploying application " + rs.getData(0).toLowerCase() + "...");
                if (dp.reload()) {
                    if (!quiet) System.out.println("...Done");
                    logger.log(Level.INFO, "Deployed!");
                    deployedApplications.put(rs.getData(0).toUpperCase(), dp);
                } else {
                    if (!quiet) System.out.println("...Failed");
                    logger.log(Level.INFO, "Not Deployed!");
                }
            }
        }
        
    }
    
    public DeployedApplication get(String applicationName) {
        if (deployedApplications.containsKey(applicationName.toUpperCase())) {
            return deployedApplications.get(applicationName.toUpperCase());
        }
        return null;
    }
}
