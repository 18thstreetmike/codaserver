/*
 * Main.java
 *
 * Created on February 22, 2007, 6:53 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import com.caucho.hessian.client.HessianProxyFactory;
import com.stevesoft.pat.Regex;
import groovy.lang.GroovyClassLoader;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.codalang.codaserver.database.*;
import org.codalang.codaserver.executioncontext.ExecutionContext;
import org.codalang.codaserver.httpServer.ICodaAPI;
import org.codalang.codaserver.httpServer.httpServer;
import org.codalang.codaserver.language.CaseInsensitiveStringStream;
import org.codalang.codaserver.language.CodaLexer;
import org.codalang.codaserver.language.CodaParser;
import org.codalang.codaserver.language.objects.*;
import org.codalang.codaserver.language.types.BaseCodaProcedure;
import org.codalang.codaserver.language.types.Database;
import org.ho.yaml.Yaml;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author michaelarace
 */
public class CodaServer {

    public static final int OBJECT_TYPE_DATASOURCE = 1;
    public static final int OBJECT_TYPE_USER = 2;
    public static final int OBJECT_TYPE_GROUP = 3;
    public static final int OBJECT_TYPE_ROLE = 4;
    public static final int OBJECT_TYPE_PERMISSION = 5;
    public static final int OBJECT_TYPE_TYPE = 6;
    public static final int OBJECT_TYPE_TABLE = 7;
    public static final int OBJECT_TYPE_APPLICATION = 8;
    public static final int OBJECT_TYPE_TABLE_COLUMN = 9;
    public static final int OBJECT_TYPE_FORM_STATUS_ADJ = 10;
    public static final int OBJECT_TYPE_FORM_STATUS_VERB = 11;
    public static final int OBJECT_TYPE_CRON = 12;
    public static final int OBJECT_TYPE_INDEX = 13;
    public static final int OBJECT_TYPE_PROCEDURE = 14;
    public static final int OBJECT_TYPE_TABLE_TRIGGER = 15;
    public static final int OBJECT_TYPE_FORM_TRIGGER = 16;

    // the variables of the server
    private Configuration configuration;
    private String configPath = "./conf" + System.getProperty("file.separator") + "coda.conf";
    private Logger logger;
    private CodaDatabase database;
    private FancyGroovyClassLoader classLoader = new FancyGroovyClassLoader(this.getClass().getClassLoader());
	private String listenerString = "CodaServer HTTP Listener 1.0";
	private boolean useTypeCache = true;

	// some variables for determining system state.  These are only used if caching is enabled.
    private SessionContainer sessions;
    private DeployedApplicationContainer deployedApplications;

    //The Cron scheduler
    private Scheduler scheduler;
    //private Hashtable<String,Scheduler> schedulers;

    //The types hashtable
    private Hashtable<Long,TypeParser> types = new Hashtable();

    // the WebServer Object
    private httpServer webServer;

    /** Creates a new instance of Main
	 * @param configPath*/
    public CodaServer(String configPath) {
		// Kill stderr
		PrintStream errTemp = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));

		this.configPath = configPath;

        // Read the configuration file
        try {
            this.configuration = Yaml.loadType(new File(configPath), Configuration.class );
        } catch (FileNotFoundException e) {
            System.out.println("Error reading configuration file at \""+ configPath +"\"");
            System.exit(0);
        }


        // Instantiate the logger
        try {
            logger = java.util.logging.Logger.getLogger("org.codalang.codaserver");
            String logDir = "log" + System.getProperty("file.separator");
            if (configuration.getLogDirectory() != null) {
                logDir = configuration.getLogDirectory()+ System.getProperty("file.separator");
            }
            logger.addHandler(new FileHandler(logDir  + "coda_%u-%g.log", 5000000, 100));
        } catch (IOException e) {
            System.out.println("Error initializing the logger.  Can I write there?");
            System.exit(0);
        }
        /*
        System.setErr(errTemp);
        */
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int newPort = -1;
        String configPath = "conf" + System.getProperty("file.separator") + "coda.conf", username = "", password = "", rootPassword = "password";

        boolean doFormat = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-p")) {
                try {
                    newPort = Integer.parseInt(args[i+1]);
                    i++;
                } catch (NumberFormatException e) {
                    //do nothing
                }
            } else if (args[i].equalsIgnoreCase("-c")) {
                File temp = new File(args[i+1]);
                if (temp.isFile() && temp.canRead()) {
                    configPath = args[i+1];
                }
                i++;
            } else if (args[i].equalsIgnoreCase("-format")) {
                doFormat = true;
                username = args[i+1];
                if (args[i+2].equals("\"\"")) {
                    password = "";
                } else {
                    password = args[i+2];
                }
                rootPassword = args[i+3];

                i = i + 3;
            }
        }

        CodaServer app = new CodaServer(configPath);

        if (newPort > 0) {
            app.setPort(newPort);
        }

        // for test
        /*
        doFormat = true;
        username = "sa";
        password = "";
        rootPassword = "password";
        /*/

        if (doFormat) {
            app.formatConfigDatabase(username, password, rootPassword, true);
        } else {
            app.start();
        }
    }

    public void start () {

        // Connect to the server database
        if (this.configuration.getDatabaseConfiguration()[0].configurationComplete()) {
            try {
                Class.forName(this.configuration.getDatabaseConfiguration()[0].getDriverClass());
            } catch (ClassNotFoundException e) {
                System.out.println("The specified driver class, \"" + this.configuration.getDatabaseConfiguration()[0].getDriverClass() + "\", is not in the classpath.");
                System.exit(0);
            }
            try {
                database = (CodaDatabase) Class.forName(this.configuration.getDatabaseConfiguration()[0].getDriverClass()).newInstance();
                database.setLogger(logger);

                Hashtable<String,String> options = new Hashtable<String,String>();
                for (int i =0; i < this.configuration.getDatabaseConfiguration()[0].getOptions().length; i++) {
                    options.put(this.configuration.getDatabaseConfiguration()[0].getOptions()[i].getSetting(), this.configuration.getDatabaseConfiguration()[0].getOptions()[i].getValue());
                }

                if (database.connect(configuration.getDatabaseConfiguration()[0].getHostname(), configuration.getDatabaseConfiguration()[0].getUsername(), configuration.getDatabaseConfiguration()[0].getPassword(), configuration.getDatabaseConfiguration()[0].getSchema(), options)) {
                    System.out.println("CodaServer Database connected!");
                } else {
                    System.out.println("Can't connect to the database specified in the configuration file.  Please make sure the information is entered correctly.");
                    System.exit(0);
                }
            } catch (ClassNotFoundException e) {
                System.out.println("The specified driver class, \"" + this.configuration.getDatabaseConfiguration()[0].getDriverClass() + "\", is not in the classpath.");
                System.exit(0);
            } catch (InstantiationException e) {
                System.out.println("We can't seem to connect to the database.  Are the username and password correct?");
                System.exit(0);
            } catch (IllegalAccessException e) {
                System.out.println("We can't seem to connect to the database.  Are the username and password correct?");
                System.exit(0);
            }
        } else {
            System.out.println("The database configuration was incomplete.  Please check it and try again.");
            System.exit(0);
        }


		// Load the applications
        deployedApplications = new DeployedApplicationContainer(database, true, logger);
        deployedApplications.initialize(false, classLoader);

        // Start the sesion container
        this.sessions = new SessionContainer(database, true);

        // Get a connection
        CodaConnection connection = database.getConnection();

        // Load the types
        try {
            Class.forName("org.codalang.codaserver.language.types.Longstring");
            Class.forName("org.codalang.codaserver.language.types.File");
            Class.forName("org.codalang.codaserver.language.types.Database");
            Class.forName("org.codalang.codaserver.language.types.Timestamp");
            Class.forName("org.codalang.codaserver.language.types.Reference");
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot load built-in type classes.");
            System.exit(0);
        }


        CodaResultSet rs3 = connection.runQuery("select id, built_in_flag, type_name, validation_mask, save_mask, class_file from types where built_in_flag = 1 or active_flag = 1 " , null);
        if (!rs3.getErrorStatus()) {
            while (rs3.next()) {
                if (rs3.getDataInt(1) == 1) {
                    types.put(rs3.getDataLong(0), new TypeParser(rs3.getDataLong(0), rs3.getData(2).toUpperCase()));
                } else {
                    types.put(rs3.getDataLong(0), new TypeParser(rs3.getDataLong(0), rs3.getData(2).toUpperCase(), rs3.getData(3), rs3.getData(4)));
                   	this.reloadType(rs3.getDataLong(0));
					//this.loadClass("org.codalang.codaserver.language.types.user." + CodaServer.camelCapitalize(rs3.getData(2), true),  rs3.getData(5));
                }
            }
        } else {
            System.out.println("Cannot load type information; is your database valid?");
            System.exit(0);
        }


		if(this.configuration.getRunCron() == 1) {
            // initialize the scheduler
            Properties schedulerProps = new Properties();
            schedulerProps.setProperty("org.quartz.scheduler.instanceName", "scheduler");
            schedulerProps.setProperty("org.quartz.scheduler.instanceId", "1");
            schedulerProps.setProperty("org.quartz.scheduler.rmi.export", "false");
            schedulerProps.setProperty("org.quartz.scheduler.rmi.proxy", "false");

            schedulerProps.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
            schedulerProps.setProperty("org.quartz.threadPool.threadCount", "20");

            schedulerProps.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

            StdSchedulerFactory schedFactory = new StdSchedulerFactory();
            try {
                schedFactory.initialize(schedulerProps);

                scheduler = schedFactory.getScheduler();
            } catch (org.quartz.SchedulerException e) {
                System.out.println("Problem with default scheduler settings.  You can fix it if you read Java.");
                System.exit(0);
            }

            // Set the session killer
            JobDetail sessionDetail = new JobDetail("SessionTimer", null, SessionTimerJob.class);

            JobDataMap sessionMap = new JobDataMap();
            sessionMap.put("server", this);

            sessionDetail.setJobDataMap(sessionMap);

            try {
                this.scheduleJob("SESSION_TIMER", "0 1/1 * * * ?", sessionDetail);
            } catch (ParseException pe) {
                System.out.println("...SessionTimerCron failed to launch.");
            } catch (SchedulerException se) {
                System.out.println("...SessionTimerCron failed to launch.");
            }

            // load the application crons
			//*
			System.out.println("Loading crons...");
            Hashtable<String,DeployedApplication> apps = deployedApplications.getDeployedApplications();
            for (String appName : apps.keySet()) {
                DeployedApplication da = apps.get(appName);
                for (int i = 1; i < 4; i++) {
					CodaDatabase db = null;
					if (da.getEnvironmentDatasource(i) != null) {
						db = da.getEnvironmentDatasource(i).getDatabase();
					}
					if (db != null) {

                        CodaConnection appConnection = db.getConnection();

                        String prefix = appConnection.getMetadata().getSystemTable().getPrefixString();
						if (prefix == null) {
							prefix ="";
						}
						CodaResultSet rs = appConnection.runQuery("select c.id, c.cron_name, c.minute_part, c.hour_part, c.day_of_month_part, c.month_part, c.day_of_week_part, p.procedure_name, c.executing_user_name from "+prefix+"crons c inner join "+prefix+"procedures p on p.id = c.procedure_id " ,null);
                        if (!rs.getErrorStatus()) {
                            if (rs.getRowsReturned() == 0) {
								System.out.println("...Done.");
							} else {
								while (rs.next()) {
									long executingUserId = this.getIdForObjectName(connection, rs.getData(8), this.OBJECT_TYPE_USER);
									if (executingUserId > 0) {
										Vector<String> parameters = new Vector();
										CodaResultSet rs1 = appConnection.runQuery("select p.parameter_value from "+prefix+"cron_parameters p where p.cron_id " + appConnection.formatStringForSQL("cron_parameters", "cron_id", rs.getData(0)) + " order by p.procedure_parameter_id asc ",null);
										if (!rs1.getErrorStatus()) {
											while (rs1.next()) {
												parameters.add(rs1.getData(0));
											}
										}
										JobDetail detail = new JobDetail(appName.toUpperCase() + "_" + i + "_" + rs.getData(1), null, ProcedureJob.class);

										JobDataMap map = new JobDataMap();
										map.put("cronName", rs.getData(1));
										map.put("server", this);
										map.put("applicationName", appName);
										map.put("environment", i);
										map.put("procedureName", rs.getData(7));
										map.put("parameters", parameters);
										map.put("userId", executingUserId);

										detail.setJobDataMap(map);

										try {
											this.scheduleJob(appName, i, rs.getData(1), "0 " + rs.getData(2) + " " + rs.getData(3) + " " + rs.getData(4) + " " + rs.getData(5) + " " + rs.getData(6), detail);
										} catch (ParseException pe) {
											System.out.println("..." + appName + ":" + (i == 1 ? "DEV" : (i == 2 ? "TEST" : "PROD")) + " Failed.  Check cron " + rs.getData(1) + " for sanity.");
										} catch (SchedulerException se) {
											System.out.println("..." + appName + ":" + (i == 1 ? "DEV" : (i == 2 ? "TEST" : "PROD")) + " Failed.  The scheduler is acting up.");
										}

										System.out.println("..." + appName + ":" + (i == 1 ? "DEV" : (i == 2 ? "TEST" : "PROD")) + " Worked!");
									}
								}
								System.out.println("...Done.");
							}

                        } else {
                            System.out.println("..." + appName + ":" + (i == 1 ? "DEV" : (i == 2 ? "TEST" : "PROD")) + " Failed.  Is its database up?");
                        }
                    }

                }
            }
			try {
				scheduler.start();
				System.out.println("Scheduler initialized!");
			} catch (SchedulerException e) {
				System.out.println("Cannot initialize scheduler!");
			}

			//*/

			if (this.getClustedFlag()) {
				connection.runStatement("delete from cluster where ip_address = " +connection.formatStringForSQL("cluster", "ip_address", this.getIpAddress()) + " amd port = " + connection.formatStringForSQL("cluster", "port", Integer.toString(this.getPort())));

				Hashtable values = new Hashtable();
				values.put("ip_address", this.getIpAddress());
				values.put("port", this.getPort());
				values.put("run_crons", this.configuration.getRunCron());
				connection.insertRow("cluster", values);

				connection.commit();
			}

		}

        System.out.println("Starting CodaServer Server...");
        webServer = new httpServer(logger, configuration.getListenerConfiguration()[0].getPort(), configuration.getListenerConfiguration()[0].getSoTimeout(), configuration.getListenerConfiguration()[0].getSocketBufferSize(), configuration.getListenerConfiguration()[0].getStaleConnectorCheck() == 1, configuration.getListenerConfiguration()[0].getTcpNoDelay() == 1, listenerString);

		System.out.println("Socket open on port " + configuration.getListenerConfiguration()[0].getPort() + ".  Enjoy!");
		webServer.start(this);

    }

    public void setPort(int port) {
        configuration.getListenerConfiguration()[0].setPort(port);
    }

	public SessionContainer getSessions() {
		return sessions;
	}

	public String getIpAddress() {
		return configuration.getListenerConfiguration()[0].getIpAddress();
	}

	public int getPort() {
		return configuration.getListenerConfiguration()[0].getPort();
	}

	// These are utility functions

	public boolean getClustedFlag () {
		return this.configuration.getCluster() == 1;
	}

	public Vector getClustedSystems() {
		Vector retval = new Vector();
		if (this.getClustedFlag()) {
			CodaResultSet rs = database.getConnection().runQuery("select ip_address, port from cluster where ip_address <> " + database.getConnection().formatStringForSQL("cluster", "ip_address", this.getIpAddress()) + " and port <> " + database.getConnection().formatStringForSQL("cluster", "port", Integer.toString(this.getPort())), null);
			if (!rs.getErrorStatus()) {
				while (rs.next()) {
					Hashtable temp = new Hashtable();
					temp.put("ip_address", rs.getData(0));
					temp.put("port", rs.getDataInt(1));
					retval.add(temp);
				}
			}
		}
		return retval;
	}

	public void sendTypeUpdateToCluster (long typeId) {
		if (this.getClustedFlag()) {
			HessianProxyFactory factory = new HessianProxyFactory();
			Vector<Hashtable> servers = this.getClustedSystems();

			for (Hashtable value : servers) {
				ICodaAPI basic;
				try {
					basic = (ICodaAPI) factory.create(ICodaAPI.class, "http://" + (String)value.get("ip_address") + ":" + (Integer)value.get("port"));
					basic.reloadType(this.getIpAddress(), this.getPort(), typeId);
					// test if errors or data

				} catch ( java.net.MalformedURLException e ) {
					this.log(Level.WARNING, "Please check the hostname and port.");
				} catch (com.caucho.hessian.client.HessianRuntimeException ex) {
					this.log(Level.WARNING, "There was a runtime exception talking to the server, probably a timeout.");
				}
			}
		}
	}

	public void sendSessionUpdateToCluster (String sessionKey) {
		if (this.getClustedFlag()) {
			HessianProxyFactory factory = new HessianProxyFactory();
			Vector<Hashtable> servers = this.getClustedSystems();

			for (Hashtable value : servers) {
				ICodaAPI basic;
				try {
					basic = (ICodaAPI) factory.create(ICodaAPI.class, "http://" + (String)value.get("ip_address") + ":" + (Integer)value.get("port"));
					basic.reloadSession(this.getIpAddress(), this.getPort(), sessionKey);
					// test if errors or data

				} catch ( java.net.MalformedURLException e ) {
					this.log(Level.WARNING, "Please check the hostname and port.");
				} catch (com.caucho.hessian.client.HessianRuntimeException ex) {
					this.log(Level.WARNING, "There was a runtime exception talking to the server, probably a timeout.");
				}
			}
		}
	}

	public void sendApplicationUpdateToCluster (String applicationName, String environment) {
		if (this.getClustedFlag()) {
			HessianProxyFactory factory = new HessianProxyFactory();
			Vector<Hashtable> servers = this.getClustedSystems();

			for (Hashtable value : servers) {
				ICodaAPI basic;
				try {
					basic = (ICodaAPI) factory.create(ICodaAPI.class, "http://" + (String)value.get("ip_address") + ":" + (Integer)value.get("port"));
					basic.reloadApplication(this.getIpAddress(), this.getPort(), applicationName, environment);
					// test if errors or data

				} catch ( java.net.MalformedURLException e ) {
					this.log(Level.WARNING, "Please check the hostname and port.");
				} catch (com.caucho.hessian.client.HessianRuntimeException ex) {
					this.log(Level.WARNING, "There was a runtime exception talking to the server, probably a timeout.");
				}
			}
		}
	}

	public boolean verifyClusterMember(String ipAddress, int port) {
		if (this.getClustedFlag()) {
			CodaConnection connection = database.getConnection();
			CodaResultSet rs = connection.runQuery("select count(*) from cluser where ip_address = " + connection.formatStringForSQL("cluster", "ip_address", ipAddress) + " and port = "+Integer.toString(port), null);
			if (!rs.getErrorStatus() && rs.next()) {
				return rs.getDataInt(0) == 1;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static String encrypt(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch(NoSuchAlgorithmException e) {
            // do nothing
        }
        try {
            md.update(password.getBytes("UTF-8"));
        } catch(UnsupportedEncodingException e) {
            // do nothing
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public void loadClass(String className, String classFile) {
        //if (this.configuration.getUseCache() == 1) {
            Class groovyClass = classLoader.parseClass(classFile);
			classLoader.linkClass(groovyClass);
			try {
                Class.forName(className, true, classLoader);
            } catch (ClassNotFoundException ex) {
                logger.log(Level.WARNING, "Class '" + className + "' was not found in the classloader.");
            }
        //}
    }

    public static void addRootPermissions(CodaConnection connection, String applicationName) {
        String userId = "1";
        CodaResultSet rs = connection.runQuery("select id from users where user_name = 'ROOT'",null);
        if (!rs.getErrorStatus() && rs.next()) {
            userId = rs.getData(0);
        }
        if (applicationName == null) {
            Hashtable<String,Object> values = new Hashtable<String,Object>();
            values.put("user_id", userId);
            values.put("server_permission_name", "CONNECT");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_USERS");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_USER_DATA");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_GROUPS");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_TYPES");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_APPLICATIONS");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_DATASOURCES");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "MANAGE_SESSIONS");
            connection.insertRow("user_server_permissions", values);
            values.put("server_permission_name", "QUERY_SYSTEM_TABLES");
            connection.insertRow("user_server_permissions", values);
       } else {
            CodaResultSet rs1 = connection.runQuery("select a.id from applications a where a.active_flag = 1 and a.application_name = " + connection.formatStringForSQL("applications", "application_name", applicationName.toUpperCase()),null);
            if (!rs1.getErrorStatus() && rs1.next()) {
                long applicationId = rs1.getDataLong(0);
                Hashtable<String,Object> values = new Hashtable<String,Object>();
                values.put("user_id", userId);
                values.put("application_id", applicationId);
                values.put("application_permission_name", "CONNECT");
                connection.insertRow("user_application_permissions", values);
                values.put("application_permission_name", "MANAGE_USERS");
                connection.insertRow("user_application_permissions", values);
                values.put("application_permission_name", "MANAGE_ROLES");
                connection.insertRow("user_application_permissions", values);
                values.put("application_permission_name", "DEVELOPER");
                connection.insertRow("user_application_permissions", values);
                values.put("application_permission_name", "MANAGE_CRONS");
                connection.insertRow("user_application_permissions", values);

            }
        }
    }

    // These methods return server data

    public long getIdForObjectName(CodaConnection connection, String objectName, int objectType) {
        CodaResultSet rs = null;
        switch (objectType) {
            case CodaServer.OBJECT_TYPE_DATASOURCE:
                rs = connection.runQuery("select d.id from datasources d where d.datasource_name = " + connection.formatStringForSQL("datasources", "datasource_name", objectName.toUpperCase()), null);
                break;
            case CodaServer.OBJECT_TYPE_APPLICATION:
                rs = connection.runQuery("select a.id from applications a where a.active_flag = 1 and a.application_name = " + connection.formatStringForSQL("applications", "application_name", objectName.toUpperCase()), null);
                break;
            case CodaServer.OBJECT_TYPE_USER:
                rs = connection.runQuery("select u.id from users u where u.active_flag = 1 and u.user_name = " +connection.formatStringForSQL("users", "user_name", objectName.toUpperCase()), null);
                break;
            case CodaServer.OBJECT_TYPE_TYPE:
                rs = connection.runQuery("select t.id from types t where (t.active_flag = 1 or t.built_in_flag = 1) and t.type_name = " + connection.formatStringForSQL("types", "type_name", objectName.toUpperCase()), null);
                break;
            case CodaServer.OBJECT_TYPE_GROUP:
                rs = connection.runQuery("select g.id from groups g where g.active_flag = 1 and g.group_name = " +connection.formatStringForSQL("groups", "group_name", objectName.toUpperCase()),null);
        }
        if (rs != null && !rs.getErrorStatus() && rs.next()) {
            return rs.getDataLong(0);
        }
        return -1;
    }

    public long getIdForObjectName(String objectName, int objectType, CodaConnection connection) {
        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        CodaResultSet rs = null;
        switch (objectType) {
            case CodaServer.OBJECT_TYPE_ROLE:
                rs = connection.runQuery("select id from "+prefix+"roles where role_name = " + connection.formatStringForSQL(prefix+"roles", "role_name", objectName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_PERMISSION:
                rs = connection.runQuery("select id from "+prefix+"permissions where permission_name = " + connection.formatStringForSQL(prefix+"permissions", "permission_name", objectName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_CRON:
                rs = connection.runQuery("select id from "+prefix+"crons where cron_name = " + connection.formatStringForSQL(prefix+"crons", "cron_name", objectName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_TABLE:
                rs = connection.runQuery("select id from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", objectName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_INDEX:
                rs = connection.runQuery("select id from "+prefix+"indexes where index_name = " + connection.formatStringForSQL(prefix+"indexes", "index_name", objectName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_PROCEDURE:
                rs = connection.runQuery("select id from "+prefix+"procedures where procedure_name = " + connection.formatStringForSQL(prefix+"procedures", "procedure_name", objectName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_TABLE_COLUMN:
                String tableId = objectName.substring(0, objectName.indexOf("."));
                String fieldName = objectName.substring(objectName.indexOf(".") + 1);
                rs = connection.runQuery("select id from "+prefix+"table_fields where table_id = "+connection.formatStringForSQL(prefix+"table_fields", "table_id", tableId) + " and field_name = " + connection.formatStringForSQL(prefix+"table_fields", "field_name", fieldName.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ:
                tableId = objectName.substring(0, objectName.indexOf("."));
                String statusNameAdj = objectName.substring(objectName.indexOf(".") + 1);
                rs = connection.runQuery("select id from "+prefix+"form_statuses where table_id = "+connection.formatStringForSQL(prefix+"form_statuses", "table_id", tableId) + " and adj_status_name = " + connection.formatStringForSQL(prefix+"form_statuses", "adj_status_name", statusNameAdj.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_FORM_STATUS_VERB:
                tableId = objectName.substring(0, objectName.indexOf("."));
                String statusNameVerb = objectName.substring(objectName.indexOf(".") + 1);
                rs = connection.runQuery("select id from "+prefix+"form_statuses where table_id = "+connection.formatStringForSQL(prefix+"form_statuses", "table_id", tableId) + " and verb_status_name = " + connection.formatStringForSQL(prefix+"form_statuses", "verb_status_name", statusNameVerb.toUpperCase()),null);
                break;
            case CodaServer.OBJECT_TYPE_TABLE_TRIGGER:
                int firstPeriod = objectName.indexOf(".");
                int secondPeriod = objectName.indexOf(".", firstPeriod + 1);

                tableId = objectName.substring(0, firstPeriod);
                String operationId = objectName.substring(firstPeriod + 1, secondPeriod);
                String beforeFlag = objectName.substring(secondPeriod + 1);
                rs = connection.runQuery("select id from "+prefix+"triggers where table_id = "+connection.formatStringForSQL(prefix+"triggers", "table_id", tableId) + " and operation_id = " + connection.formatStringForSQL(prefix+"triggers", "operation_id", operationId) + " and before_flag = " + connection.formatStringForSQL(prefix+"triggers", "before_flag", beforeFlag),null);
                break;
            case CodaServer.OBJECT_TYPE_FORM_TRIGGER:
                firstPeriod = objectName.indexOf(".");
                secondPeriod = objectName.indexOf(".", firstPeriod + 1);
				int thirdPeriod = objectName.indexOf(".", secondPeriod + 1);

				tableId = objectName.substring(0, firstPeriod);
                operationId = objectName.substring(firstPeriod + 1, secondPeriod);
				String formStatusId = objectName.substring(secondPeriod + 1, thirdPeriod);
                beforeFlag = objectName.substring(thirdPeriod + 1);
                rs = connection.runQuery("select id from "+prefix+"triggers where table_id = "+connection.formatStringForSQL(prefix+"triggers", "table_id", tableId) + (!formStatusId.equalsIgnoreCase("-1") ? " and form_status_id = " + connection.formatStringForSQL(prefix+"triggers", "form_status_id", formStatusId) : " and operation_id = " + connection.formatStringForSQL(prefix+"triggers", "operation_id", operationId)) + " and before_flag = " + connection.formatStringForSQL(prefix+"triggers", "before_flag", beforeFlag),null);
                break;
        }
        if (rs != null && !rs.getErrorStatus() && rs.next()) {
            return rs.getDataLong(0);
        }
        return -1;
    }

    public String getSessionApplication(String sessionKey) {
        return sessions.getSessionApplication(sessionKey);
    }

    public long getSessionUserId(String sessionKey) {
        return sessions.getSessionUserId(sessionKey);
    }

    public long getSessionGroupId(String sessionKey) {
        return sessions.getSessionGroupId(sessionKey);
    }

    public int getSessionEnvironmentId(String sessionKey) {
        return sessions.getSessionEnvironmentId(sessionKey);
    }

    public String getSessionUsername(String sessionKey) {
        return sessions.getSessionUsername(sessionKey);
    }

    public String getSessionGroup(String sessionKey) {
        return sessions.getSessionGroup(sessionKey);
    }

    public void scheduleJob (String applicationName, int environment, String jobName, String cronString, JobDetail detail)  throws ParseException, SchedulerException {
        CronTrigger trigger = new CronTrigger(applicationName.toUpperCase() + "_" + environment + "_" + jobName.toUpperCase() + "_TRIG", null, cronString);
        scheduler.scheduleJob(detail, trigger);
    }

    public void scheduleJob (String handle, String cronString, JobDetail detail)  throws ParseException, SchedulerException {
        CronTrigger trigger = new CronTrigger(handle, null, cronString);
        scheduler.scheduleJob(detail, trigger);
    }

    public void stopJob (String applicationName, int environment, String jobName) throws SchedulerException {
        scheduler.deleteJob(applicationName.toUpperCase() + "_" + environment + "_" + jobName.toUpperCase(), "");
    }

    public synchronized long logTransaction(CodaConnection connection, long applicationId, String codaStatement, boolean refTableFlag, long userId) {
        long retval = -1;
        CodaResultSet rs = connection.runQuery("select max(revision_id) from transactions where application_id = " + connection.formatStringForSQL("transactions", "application_id", Long.toString(applicationId)), null);
        if (!rs.getErrorStatus() && rs.next()) {
            long revisionId = 1;
            if (rs.getData(0) != null) {
                revisionId = rs.getDataLong(0) + 1;
            }
            Hashtable<String, Object> values = new Hashtable();
            values.put("application_id", applicationId);
            values.put("coda_statement", codaStatement);
            values.put("revision_id", revisionId);
            values.put("ref_table_flag", refTableFlag ? "1" : "0");
            values.put("create_user_id", userId);
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            retval = connection.insertRow("transactions", values);

        }
        return retval;
    }

    public boolean isArrayFieldNameInUse(CodaConnection connection, String prefix, String fieldName) {
        boolean retval = false;
        CodaResultSet rs = connection.runQuery("select count(id) from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", fieldName.toUpperCase()), null);
        if (!rs.getErrorStatus() && rs.next()) {
            retval = rs.getDataInt(0) > 0;
        }
        if (!retval) {
            rs = connection.runQuery("select count(id) from "+prefix+"table_fields where array_flag = 1 and field_name = " + connection.formatStringForSQL(prefix+"table_fields", "field_name", fieldName.toUpperCase()), null);
            if (!rs.getErrorStatus() && rs.next()) {
                retval = rs.getDataInt(0) > 0;
            }
        }
        return retval;
    }

	public boolean isRefTable(String applicationName, String tableName) {
        Datasource datasource = deployedApplications.getDatasource(applicationName, 1);
        if (datasource != null && tableName != null) {
            CodaConnection connection = datasource.getDatabase().getConnection();
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            CodaResultSet rs = connection.runQuery("select ref_table_flag from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName.toUpperCase()), null);
            if (!rs.getErrorStatus() && rs.next()) {
                return rs.getDataInt(0) == 1;
            }
        }
        return false;
    }

	public boolean isSoftDeleteTable(CodaConnection connection, String prefix, String tableName) {
        CodaResultSet rs = connection.runQuery("select soft_delete_flag from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName.toUpperCase()), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return rs.getDataInt(0) == 1;
        }
        return false;
    }

	public boolean isGroupTable(CodaConnection connection, String prefix, String tableName) {
        CodaResultSet rs = connection.runQuery("select group_flag from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName.toUpperCase()), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return rs.getDataInt(0) == 1;
        }
        return false;
    }

	public boolean isForm(CodaConnection connection, String prefix, String tableName) {
        CodaResultSet rs = connection.runQuery("select form_flag from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName.toUpperCase()), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return rs.getDataInt(0) == 1;
        }
        return false;
    }

	public boolean isSubTable(CodaConnection connection, String prefix, String tableName) {
        CodaResultSet rs = connection.runQuery("select parent_table_id from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName.toUpperCase()), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return (rs.getData(0) != null && !rs.getData(0).equals(""));
        }
        return false;
    }

    public boolean isInitialFormStatus(CodaConnection connection, String prefix, long formStatusId) {
        CodaResultSet rs = connection.runQuery("select fs.initial_flag from "+prefix+"form_statuses fs where fs.id = " + connection.formatStringForSQL(prefix+"form_statuses", "id", Long.toString(formStatusId)), null);
        if (!rs.getErrorStatus() && rs.next()) {
            return (rs.getData(0) != null && !rs.getData(0).equals(""));
        }
        return false;
    }

    public boolean isArrayField(CodaConnection connection, String prefix, String applicationName, long fieldId) {
        CodaResultSet rs = connection.runQuery("select array_flag from "+prefix+"table_fields where id = " + connection.formatStringForSQL(prefix+"table_fields", "id", Long.toString(fieldId)), null);
		if (!rs.getErrorStatus() && rs.next()) {
			return rs.getDataInt(0) == 1;
		}
	
        return false;
    }

	public boolean isBuiltInField(CodaConnection connection, String prefix, long fieldId) {
		CodaResultSet rs = connection.runQuery("select built_in_flag from "+prefix+"table_fields where id = " + connection.formatStringForSQL(prefix+"table_fields", "id", Long.toString(fieldId)), null);
		if (!rs.getErrorStatus() && rs.next()) {
			return rs.getDataInt(0) == 1;
	    }
        return false;
    }

	public static String camelCapitalize(String s, boolean initCaps) {
        // I know this should be a regex
        StringBuffer retval = new StringBuffer();

        char[] chars = s.toCharArray();
        if (initCaps) {
            retval.append(Character.toUpperCase(chars[0]));
        } else {
            retval.append(Character.toLowerCase(chars[0]));
        }

        for (int i = 1; i < s.length(); i++) {
            if (chars[i] == '_') {
                if (i+1 < chars.length && chars[i+1] != '_') {
                    retval.append(Character.toUpperCase(chars[i+1]));
                    i++;
                } else {
                    retval.append('_');
                }
            } else {
                retval.append(Character.toLowerCase(chars[i]));
            }
        }

        return retval.toString();
    }

	public CodaDatabase getDatabase() {
		return this.database;
	}

	public Hashtable<String, Vector<String>> getColumnsForTables(CodaConnection connection, Vector<String> tableNames) {
		Hashtable<String, Vector<String>> retval = new Hashtable();
		for (String tableName : tableNames) {
			ColumnDefinition[] columnDefinitions = connection.getMetadata().getColumnsForTable(tableName);
			for (int i = 0 ; i < columnDefinitions.length; i++) {
				if (retval.containsKey(columnDefinitions[i].getName().toUpperCase())) {
					retval.get(columnDefinitions[i].getName().toUpperCase()).add(tableName);
				} else {
					Vector<String> temp = new Vector();
					temp.add(tableName);
					retval.put(columnDefinitions[i].getName().toUpperCase(), temp);
				}
			}
		}
		return retval;
	}

	public int getIdForEnvironmentName(String environmentName) {
        if (environmentName.equalsIgnoreCase("DEV")) {
            return 1;
        } else if (environmentName.equalsIgnoreCase("TEST")) {
            return 2;
        } else {
            return 3;
        }
    }

    public int getIdForDatabaseOperation(String operationName) {
        if (operationName.equalsIgnoreCase("SELECT")) {
            return 1;
        } else if (operationName.equalsIgnoreCase("insert")) {
            return 2;
        } else if (operationName.equalsIgnoreCase("update")) {
            return 3;
        } else {
            return 4;
        }
    }

    public DeployedApplication getDeployedApplication(String applicationName) {
        return this.deployedApplications.get(applicationName);
    }

    public CodaDatabase getCodaDatabase(String datasourceName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        CodaConnection connection = database.getConnection();
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
            Class.forName(rs.getData(1));
            conn = (CodaDatabase) Class.forName(rs.getData(1)).newInstance();
			conn.setLogger(logger);
			if (conn.connect(rs.getData(2), rs.getData(4), rs.getData(5), rs.getData(3), options)) {
				return conn;
			} else {
				return null;
			}

        } else {
          return null;
        }
    }

    public void reloadType(long typeId) {
        CodaConnection connection = database.getConnection();

		this.useTypeCache = false;

		types.remove(typeId);
		CodaResultSet rs = connection.runQuery("select id, built_in_flag, type_name, validation_mask, save_mask, class_file from types where active_flag = 1 and id = " + connection.formatStringForSQL("types", "id", Long.toString(typeId)), null);
		if (!rs.getErrorStatus() && rs.next()) {
			if (rs.getDataInt(1) == 1) {
				types.put(rs.getDataLong(0), new TypeParser(rs.getDataLong(0), rs.getData(2).toUpperCase()));
			} else {
				types.put(rs.getDataLong(0), new TypeParser(rs.getDataLong(0), rs.getData(2).toUpperCase(), rs.getData(3), rs.getData(4)));
			}

			try {
				Class groovyClass = classLoader.parseClass(rs.getData(5));

				classLoader.linkClass(groovyClass);
				try {
					Class.forName("org.codalang.codaserver.language.types.user." + this.camelCapitalize(rs.getData(2), true), true, classLoader);
				} catch (ClassFormatError ex) {
					logger.log(Level.WARNING, "Class '" + "org.codalang.codaserver.language.types.user." + this.camelCapitalize(rs.getData(2), true) + "' was not found in the classloader.");
				} catch (Exception ex) {
					logger.log(Level.WARNING, "Class '" + "org.codalang.codaserver.language.types.user." + this.camelCapitalize(rs.getData(2), true) + "' was not found in the classloader.");
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Unable to deploy type " + rs.getData(2));
			}
		}

		this.useTypeCache = true;
	}

    public TypeParser getTypeParser(long typeId) {
            return types.get(typeId);
        
    }

    public Hashtable<Long,TypeParser> getTypes() {
        return (Hashtable<Long,TypeParser>)types.clone();
    }

    public CodaDatabase getApplicationDatabase (String applicationName, int environment) {
        if (applicationName != null) {
            return deployedApplications.getDatasource(applicationName, environment).getDatabase();
        }
        return null;
    }

    public Datasource getApplicationDatasource (String applicationName, int environment) {
        if (applicationName != null) {
            return deployedApplications.getDatasource(applicationName, environment);
        }
        return null;
    }

    public String getApplicationDatasourceName (String applicationName, int environment) {
        if (applicationName != null) {
            return deployedApplications.getDatasourceName(applicationName, environment);
        }
        return null;
    }

    public GroovyClassLoader getClassLoader() {
        return this.classLoader;
    }

    // Commands start here

    public boolean shutdown() {
        webServer.stop();

		if (this.getClustedFlag()) {
			CodaConnection connection = database.getConnection();

			connection.runStatement("delete from cluster where ip_address = " +connection.formatStringForSQL("cluster", "ip_address", this.getIpAddress()) + " amd port = " + connection.formatStringForSQL("cluster", "port", Integer.toString(this.getPort())));

			connection.commit();
		}

		database.disconnect();
        System.out.println("CodaServer Server is now closed.  Have a nice day!");
        System.exit(0);
        return true;
    }

    public void expireSessions () {
        CodaConnection connection = database.getConnection();
        if (this.configuration.getSessionTimeout() > 0) {
            long timeInMillis = new GregorianCalendar().getTimeInMillis();
            timeInMillis = timeInMillis - (1000 * 60 * this.configuration.getSessionTimeout());
            CodaResultSet rs = connection.runQuery("select session_key from sessions where  session_timestamp < " + connection.formatStringForSQL("sessions", "session_timestamp", Long.toString(timeInMillis)), null);
            while (rs.next()) {
                sessions.remove(rs.getData(0));
            }
            connection.runStatement("delete from sessions where session_timestamp < " + connection.formatStringForSQL("sessions", "session_timestamp", Long.toString(timeInMillis)));
        }
    }

    public String login(String username, String password) {
        CodaConnection connection = database.getConnection();
        String sql = "select id from users where active_flag = 1 and user_name = " + connection.formatStringForSQL("users", "user_name", username.toUpperCase()) + " and pass_word = " + connection.formatStringForSQL("users", "pass_word", encrypt(password)) + " ";
        CodaResultSet rs = connection.runQuery(sql, null);
        if (!rs.getErrorStatus() && rs.getRowsReturned() > 0) {
            if (rs.next()) {
				String sessionKey = sessions.createSession(rs.getDataLong(0));
				this.sendSessionUpdateToCluster(sessionKey);
				return sessionKey;
			} else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String login(long userId) {
        CodaConnection connection = database.getConnection();
        String sql = "select id from users where active_flag = 1 and id = " +connection.formatStringForSQL("users", "id", Long.toString(userId));
        CodaResultSet rs = connection.runQuery(sql, null);
        if (!rs.getErrorStatus() && rs.getRowsReturned() > 0) {
            if (rs.next()) {
				String sessionKey = sessions.createSession(rs.getDataLong(0));
				this.sendSessionUpdateToCluster(sessionKey);
				return sessionKey;
            }
        }
        return null;
    }

    public void logout(String sessionKey) {
        sessions.remove(sessionKey);
		this.sendSessionUpdateToCluster(sessionKey);
	}

    public void log(Level level, String message) {
        this.logger.log(level, message);
    }

    public boolean touchSession(String sessionKey) {
        return sessions.touchSession(sessionKey);
    }

    public boolean setSessionApplication(String sessionKey, String applicationName, int environment, String groupName) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return false;
        }
		CodaConnection connection = database.getConnection();
		if (deployedApplications.get(applicationName) != null && deployedApplications.get(applicationName).isGroupFlag()) {
            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
            long groupId = groupName == null ? -1 : this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
            if (userId > 0 && deployedApplications.canUserUseApplication(applicationName, userId, environment, groupId)) {
                sessions.setSessionApplication(sessionKey, applicationId, applicationName, environment);
                if (groupId > 0) {
                    sessions.setSessionGroup(sessionKey, groupId, groupName);

				}
				this.sendSessionUpdateToCluster(sessionKey);
				return true;
            }
        } else {
            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
            if (userId > 0 && deployedApplications.canUserUseApplication(applicationName, userId, environment)) {
                sessions.setSessionApplication(sessionKey, applicationId, applicationName, environment);
				this.sendSessionUpdateToCluster(sessionKey);
				return true;
            }
        }
        return false;
    }

    public CodaResponse createDatasource(String sessionKey, String datasourceName, String displayName, Hashtable properties, Hashtable options, String username, String password) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_DATASOURCES")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            // make sure the properties hashtable is complete
            if (!properties.containsKey("HOSTNAME")) {
                return new CodaResponse(true, null, 2001, "Missing datasource parameter HOSTNAME");
            } else if(!properties.containsKey("USERNAME")) {
                return new CodaResponse(true, null, 2002, "Missing datasource parameter USERNAME");
            } else if (!properties.containsKey("PASSWORD")) {
                return new CodaResponse(true, null, 2003, "Missing datasource parameter PASSWORD");
            } else if (!properties.containsKey("SCHEMA")) {
                return new CodaResponse(true, null, 2004, "Missing datasource parameter SCHEMA");
            } else if (!properties.containsKey("DRIVER")) {
                return new CodaResponse(true, null, 2005, "Missing datasource parameter DRIVER");
            }
            // make sure the datasource name isn't taken
            if (this.getIdForObjectName(connection, datasourceName.toUpperCase(), OBJECT_TYPE_DATASOURCE) > 0) {
                return new CodaResponse(true, null, 2007, "Datasource name already in use");
            }
            // test that the class is in the classpath
            try {
                Class.forName((String)properties.get("DRIVER"));
            } catch (ClassNotFoundException e) {
                return new CodaResponse(true, null, 2006, "Driver '" + (String)properties.get("DRIVER") + "' not found");
            }
            // attempt to create the schema if asked to
            try {
                if (username != null && password != null) {
                    CodaDatabase temp = (CodaDatabase)Class.forName((String)properties.get("DRIVER")).newInstance();
                    if (!temp.createSchema((String)properties.get("HOSTNAME"),(String)properties.get("USERNAME"),(String)properties.get("PASSWORD"),(String)properties.get("SCHEMA"),options,username,password)) {
                        return new CodaResponse(true, null, 2008, "Could not create new schema on database host");
                    }
                }
            } catch (ClassNotFoundException e) {
                return new CodaResponse(true, null, 2006, "Driver '" + (String)properties.get("DRIVER") + "' not found");
            } catch (InstantiationException e) {
                return new CodaResponse(true, null, 2006, "Driver '" + (String)properties.get("DRIVER") + "' not found");
            } catch (IllegalAccessException e) {
                return new CodaResponse(true, null, 2006, "Driver '" + (String)properties.get("DRIVER") + "' not found");
            }
            // insert the datasource
            Hashtable values = new Hashtable();
            values.put("datasource_name", datasourceName.toUpperCase());
            values.put("host_name", (String)properties.get("HOSTNAME"));
            values.put("driver_name", (String)properties.get("DRIVER"));
            values.put("user_name", (String)properties.get("USERNAME"));
            values.put("pass_word", (String)properties.get("PASSWORD"));
            values.put("schema_name", (String)properties.get("SCHEMA"));
            values.put("display_name", (displayName == null ? datasourceName : displayName));
            values.put("create_user_id", userId);
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            long datasourceId = connection.insertRow("datasources", values);

            Enumeration en = options.keys();
            while (en.hasMoreElements()) {
                String key = (String)en.nextElement();
                values = new Hashtable();
                values.put("datasource_id", datasourceId);
                values.put("option_name", key.toUpperCase());
                values.put("option_value", options.get(key));
                connection.insertRow("datasource_options", values);
            }
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9001, "Do not have permission to manage datasources");
        }
    }

    public CodaResponse alterDatasource(String sessionKey, String datasourceName, int operation, String newDatasourceName, String displayName, Hashtable properties, Hashtable options) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_DATASOURCES")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long datasourceId = this.getIdForObjectName(connection, datasourceName.toUpperCase(), OBJECT_TYPE_DATASOURCE);
            if (datasourceId < 0) {
                return new CodaResponse(true, null, 2009, "Datasource name is invalid");
            }
            Hashtable values = new Hashtable();
            switch (operation) {
                // set display
                case 1:
                    values = new Hashtable();
                    values.put("display_name", displayName);
                    values.put("mod_user_id", userId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow("datasources", "id", datasourceId, values);
                    break;
                // rename
                case 2:
                    if (this.getIdForObjectName(connection, newDatasourceName.toUpperCase(), OBJECT_TYPE_DATASOURCE) > 0) {
                        return new CodaResponse(true, null, 2007, "Datasource name already in use");
                    } else {
                        values = new Hashtable();
                        values.put("datasource_name", newDatasourceName.toUpperCase());
                        values.put("mod_user_id", userId);
                        values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                        connection.updateRow("datasources", "id", datasourceId, values);

                    }
                    break;
                // set properties
                case 3:
                    if (properties.containsKey("DRIVER")) {
                        try {
                            Class.forName((String)properties.get("DRIVER"));
                        } catch (ClassNotFoundException e) {
                            return new CodaResponse(true, null, 2006, "Driver '" + (String)properties.get("DRIVER") + "' not found");
                        }
                    }
                    values = new Hashtable();
                    if (properties.containsKey("HOSTNAME")) values.put("host_name", (String)properties.get("HOSTNAME"));
                    if (properties.containsKey("DRIVER")) values.put("driver_name", (String)properties.get("DRIVER"));
                    if (properties.containsKey("USERNAME")) values.put("user_name", (String)properties.get("USERNAME"));
                    if (properties.containsKey("PASSWORD")) values.put("pass_word", (String)properties.get("PASSWORD"));
                    if (properties.containsKey("SCHEMA")) values.put("schema_name", (String)properties.get("SCHEMA"));
                    connection.updateRow("datasources", "id", datasourceId, values);
                    break;
                // set options
                case 4:
                    Enumeration en = options.keys();
                    while (en.hasMoreElements()) {
                        String key = (String)en.nextElement();
                        connection.runStatement("delete from datasource_options where datasource_id = "+connection.formatStringForSQL("datasource_options", "datasource_id", Long.toString(datasourceId)) +" and option_name = " + connection.formatStringForSQL("datasource_options", "option_name", key.toUpperCase()));
                        values = new Hashtable();
                        values.put("datasource_id", datasourceId);
                        values.put("option_name", key.toUpperCase());
                        values.put("option_value", options.get(key));
                        connection.insertRow("datasource_options", values);
                    }
                    break;
            }
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9001, "Do not have permission to manage datasources");
        }
    }

    public CodaResponse dropDatasource(String sessionKey, String datasourceName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_DATASOURCES")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long datasourceId = this.getIdForObjectName(connection, datasourceName.toUpperCase(), OBJECT_TYPE_DATASOURCE);
            if (datasourceId < 0) {
                return new CodaResponse(true, null, 2009, "Datasource name is invalid");
            }
            CodaResultSet rs = connection.runQuery("select count(*) from applications where active_flag = 1 and dev_datasource_id = " + connection.formatStringForSQL("applications", "dev_datasource_id", Long.toString(datasourceId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                if(rs.getDataLong(0) > 0) {
                    return new CodaResponse(true, null, 2010, "Datasource in use as a development database");
                } else {
                    connection.runStatement("update applications set test_datasource_id = null where test_datasource_id = " + connection.formatStringForSQL("applications", "test_datasource_id", Long.toString(datasourceId)));
                    connection.runStatement("update applications set prod_datasource_id = null where prod_datasource_id = " + connection.formatStringForSQL("applications", "prod_datasource_id", Long.toString(datasourceId)));
                    connection.deleteRow("datasources", "id", datasourceId);
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                }
            } else {
                return new CodaResponse(true, null, 8001, "Unspecified magical database error.  Is the CodaServer database up?");
            }
        } else {
            return new CodaResponse(true, null, 9001, "Do not have permission to manage datasources");
        }
    }

    public CodaResponse createApplication(String sessionKey, String applicationName, String displayName, boolean groupFlag, String datasourceName, String prefix) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            if (this.getIdForObjectName(connection, applicationName.toUpperCase(), CodaServer.OBJECT_TYPE_APPLICATION) > 0) {
                return new CodaResponse(true, null, 2011, "Application name already in use");
            }
            long datasourceId = this.getIdForObjectName(connection, datasourceName.toUpperCase(), OBJECT_TYPE_DATASOURCE);
            if (datasourceId < 0) {
                return new CodaResponse(true, null, 2009, "Datasource name is invalid");
            }
			try {
				CodaDatabase conn = this.getCodaDatabase(datasourceName.toUpperCase());
				CodaSystemTable systemInfo = conn.getConnection().getMetadata().getSystemTable();
				if (systemInfo == null && prefix == null) {
					return new CodaResponse(true, null, 8009);
				}
			} catch (IllegalAccessException e) {
				return new CodaResponse(true, null, 8002);
			} catch (ClassNotFoundException e) {
				return new CodaResponse(true, null, 8002);
			} catch (InstantiationException e) {
				return new CodaResponse(true, null, 8002);
			}
			Hashtable<String,Object> values = new Hashtable();
            values.put("application_name", applicationName.toUpperCase());
            values.put("display_name", (displayName == null ? applicationName : displayName));
            values.put("group_flag", (groupFlag ? "1" : "0"));
            values.put("active_flag", "1");
            values.put("dev_datasource_id", datasourceId);
            values.put("create_user_id", userId);
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            long applicationId = connection.insertRow("applications", values);

			// format the datasource
			CodaResponse formatResponse = this.formatDatasourceForApplication(sessionKey, datasourceName, applicationName, -1, prefix == null ? "" : prefix,true);
			if (formatResponse.getError()) {
				this.dropApplication(sessionKey, applicationName);
				return formatResponse;
			}

			// set up the deployed application
			deployedApplications.deployApplication(applicationName, true, classLoader);
            addRootPermissions(connection, applicationName);
            deployedApplications.get(applicationName.toUpperCase()).reloadUser(userId);
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9002, "Do not have permission to manage applications");
        }
    }

    public CodaResponse alterApplication(String sessionKey, String applicationName, int operation, String newApplicationName, String displayName, String environmentString, String datasourceName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long applicationId = this.getIdForObjectName(connection, applicationName.toUpperCase(), CodaServer.OBJECT_TYPE_APPLICATION);
            if (applicationId < 0) {
                return new CodaResponse(true, null, 2012, "Application name is invalid");
            }
            Hashtable values = new Hashtable();
            switch (operation) {
                // display name
                case 1:
                    values = new Hashtable();
                    values.put("display_name", displayName);
                    values.put("mod_user_id", userId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow("applications", "id", applicationId, values);
                    break;
                case 2:
                    if (this.getIdForObjectName(connection, newApplicationName.toUpperCase(), CodaServer.OBJECT_TYPE_APPLICATION) > 0) {
                        return new CodaResponse(true, null, 2011, "Application name already in use");
                    }
                    values = new Hashtable();
                    values.put("application_name", newApplicationName.toUpperCase());
                    values.put("mod_user_id", userId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow("applications", "id", applicationId, values);
                    break;
                case 3:
                    String environmentColumn = "";
                    if (environmentString.equalsIgnoreCase("DEV")) {
                        environmentColumn = "dev_datasource_id";
                    } else if (environmentString.equalsIgnoreCase("TEST")) {
                        environmentColumn = "test_datasource_id";
                    } else if (environmentString.equalsIgnoreCase("PROD")) {
                        environmentColumn = "prod_datasource_id";
                    } else {
                        return new CodaResponse(true, null, 2013, "Invalid environment specified");
                    }
                    long datasourceId = this.getIdForObjectName(connection, datasourceName.toUpperCase(), OBJECT_TYPE_DATASOURCE);
                    if (datasourceId < 0) {
                        return new CodaResponse(true, null, 2009, "Datasource name is invalid");
                    }
					try {
						CodaDatabase tempDb = this.getCodaDatabase(datasourceName.toUpperCase());
						if (tempDb == null) {
							return new CodaResponse(true, null, 8002, "Cannot connect to datasource '"+datasourceName+"'.  Check the connection settings and database.");
						}
					} catch (ClassNotFoundException e) {
						return new CodaResponse(true, null, 8002, "Cannot connect to datasource '"+datasourceName+"'.  Check the connection settings and database.");
					} catch (InstantiationException e) {
						return new CodaResponse(true, null, 8002, "Cannot connect to datasource '"+datasourceName+"'.  Check the connection settings and database.");
					} catch (IllegalAccessException e) {
						return new CodaResponse(true, null, 8002, "Cannot connect to datasource '"+datasourceName+"'.  Check the connection settings and database.");
					}

					values = new Hashtable();
                    values.put(environmentColumn, datasourceId);
                    values.put("mod_user_id", userId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow("applications", "id", applicationId, values);

            }
            deployedApplications.deployApplication(applicationName, true, classLoader);
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9002, "Do not have permission to manage applications");
        }
    }

    public CodaResponse dropApplication(String sessionKey, String applicationName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long applicationId = this.getIdForObjectName(connection, applicationName.toUpperCase(), OBJECT_TYPE_APPLICATION);
            if (applicationId < 0) {
                return new CodaResponse(true, null, 2012, "Application name is invalid");
            }

            Hashtable values = new Hashtable();
            values.put("active_flag", "0");
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

            connection.updateRow("applications", "id", applicationId, values);
            deployedApplications.removeApplication(applicationName);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9002, "Do not have permission to manage applications");
        }
    }

    public CodaResponse createUser (String sessionKey, String username, String password, boolean robotFlag, Hashtable properties) {
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (sessions.hasServerPermission(sessionKey, "MANAGE_USERS")) {
            CodaConnection connection = database.getConnection();

            if (this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER) > 0) {
                return new CodaResponse(true, null, 2014, "Username already in use");
            }

            Hashtable values = new Hashtable();
            values.put("user_name", username.toUpperCase());
            values.put("pass_word", this.encrypt(password));
            values.put("robot_flag", (robotFlag ? "1" : "0"));
            values.put("active_flag", "1");
            values.put("create_user_id", modUserId);
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_id", modUserId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            Enumeration en = properties.keys();
            while (en.hasMoreElements()) {
                String key = (String)en.nextElement();
                values.put(key.toLowerCase(), properties.get(key));
            }

            connection.insertRow("users", values);
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
        }
    }

    public CodaResponse alterUser (String sessionKey, String username, int operation, String password, Hashtable properties) {
        CodaConnection connection = database.getConnection();
        if(username.equalsIgnoreCase("ROOT")) {
            if (operation == 2) {
                return new CodaResponse(true, null, 2018);
            }
            long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
            if (userId < 0) {
                return new CodaResponse(true, null, 2015, "Username is invalid");
            }
            long modUserId = sessions.getSessionUserId(sessionKey);
            if(modUserId == userId) {
                Hashtable values = new Hashtable();
                values.put("pass_word", this.encrypt(password));
                values.put("mod_user_id", modUserId);
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                connection.updateRow("users", "id", userId, values);

                connection.commit();
                return new CodaResponse(false, "Success!", -1, null);
            } else {
                return new CodaResponse(true, null, 9004, "Do not have permission to manage ROOT account");
            }
        } else {
            boolean userPerm = sessions.hasServerPermission(sessionKey, "MANAGE_USERS");
            boolean dataPerm = sessions.hasServerPermission(sessionKey, "MANAGE_USER_DATA");
            long modUserId = sessions.getSessionUserId(sessionKey);
            if (modUserId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
            if (userId < 0) {
                return new CodaResponse(true, null, 2015, "Username is invalid");
            }
            if (userPerm || dataPerm || modUserId == userId) {


                Hashtable values = new Hashtable();
                switch (operation) {
                    case 1:
                        if (userPerm) {
                            values.put("pass_word", this.encrypt(password));
                            values.put("mod_user_id", modUserId);
                            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                            connection.updateRow("users", "id", userId, values);
                        } else {
                            return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
                        }
                        break;
                    case 2:
                        if (dataPerm) {
                            Enumeration en = properties.keys();
                            while (en.hasMoreElements()) {
                                String key = (String)en.nextElement();
                                values.put(key.toLowerCase(), properties.get(key));
                            }
                            values.put("mod_user_id", modUserId);
                            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                            connection.updateRow("users", "id", userId, values);
                        } else {
                            return new CodaResponse(true, null, 9006);
                        }
                        break;

                }

                connection.commit();
                return new CodaResponse(false, "Success!", -1, null);

            } else {
               return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
            }
        }
    }

    public CodaResponse dropUser(String sessionKey, String username) {
        if(username.equalsIgnoreCase("ROOT")) {
            return new CodaResponse(true, null, 9004, "Do not have permission to manage ROOT account");
        } else {
            if (sessions.hasServerPermission(sessionKey, "MANAGE_USERS")) {
                CodaConnection connection = database.getConnection();
                long modUserId = sessions.getSessionUserId(sessionKey);
                if (modUserId < 0) {
                    return  new CodaResponse(true, null, 1005);
                }
                long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
                if (userId < 0) {
                    return new CodaResponse(true, null, 2015, "Username is invalid");
                }

                Hashtable values = new Hashtable();
                values.put("active_flag", "0");
                values.put("mod_user_id", modUserId);
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                connection.updateRow("users", "id", userId, values);
                connection.commit();
                return new CodaResponse(false, "Success!", -1, null);

            } else {
               return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
            }
        }
    }

    public CodaResponse createGroup (String sessionKey, String groupName, String displayName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_GROUPS")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            if (this.getIdForObjectName(connection, groupName.toUpperCase(), CodaServer.OBJECT_TYPE_GROUP) > 0) {
                return new CodaResponse(true, null, 2016, "Group name already in use");
            }

            Hashtable values = new Hashtable();
            values.put("group_name", groupName.toUpperCase());
            values.put("display_name", displayName);
            values.put("active_flag", "1");
            values.put("create_user_id", userId);
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

            connection.insertRow("groups", values);
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9005, "Do not have permission to manage groups");
        }
    }

    public CodaResponse alterGroup (String sessionKey, String groupName, int operation, String newGroupName, String displayName, String username) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_GROUPS")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long groupId = this.getIdForObjectName(connection, groupName.toUpperCase(), CodaServer.OBJECT_TYPE_GROUP);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2017, "Group name is invalid");
            }

            Hashtable values = new Hashtable();
            switch (operation) {
                // displayname
                case 1:
                    values.put("display_name", displayName);
                    values.put("mod_user_id", userId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow("groups", "id", groupId, values);
                    break;
                // case rename
                case 2:
                    if (this.getIdForObjectName(connection, newGroupName, CodaServer.OBJECT_TYPE_GROUP) > 0) {
                        return new CodaResponse(true, null, 2016, "Group name already in use");
                    }
                    values.put("group_name", newGroupName.toUpperCase());
                    values.put("mod_user_id", userId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow("groups", "id", groupId, values);
                    break;
                // add user
                case 3:
                    long newUserId = this.getIdForObjectName(connection, username, CodaServer.OBJECT_TYPE_USER);
                    if (newUserId > 0) {
                        CodaResultSet rs = connection.runQuery("select count(ug.user_id) from user_groups ug where ug.group_id = " + connection.formatStringForSQL("user_groups", "group_id", Long.toString(groupId)) + " and ug.group_id = " + connection.formatStringForSQL("user_groups", "user_id", Long.toString(groupId)), null);
                        if (!rs.getErrorStatus() && rs.next()) {
                           if(rs.getDataInt(0) < 1) {
                                connection.runStatement("insert into user_groups (user_id, group_id, create_user_id, create_date, mod_user_id, mod_date) values ("+connection.formatStringForSQL("user_groups", "user_id", Long.toString(newUserId))+", "+connection.formatStringForSQL("user_groups", "group_id", Long.toString(groupId))+","+connection.formatStringForSQL("user_groups", "create_user_id", Long.toString(userId))+", "+connection.formatStringForSQL("user_groups", "create_date", Long.toString(new GregorianCalendar().getTimeInMillis()))+", "+connection.formatStringForSQL("user_groups", "mod_user_id", Long.toString(userId))+", "+connection.formatStringForSQL("user_groups", "mod_date", Long.toString(new GregorianCalendar().getTimeInMillis()))+" )");
                           }
                        } else {
                           return new CodaResponse(true, null, 8001);
                        }
                    } else {
                        return new CodaResponse(true, null, 2015);
                    }
                    break;
                // remove user
                case 4:
                    newUserId = this.getIdForObjectName(connection, username, CodaServer.OBJECT_TYPE_USER);
                    if (newUserId > 0) {
                        connection.runStatement("delete from user_groups where user_id = "+connection.formatStringForSQL("user_groups", "user_id", Long.toString(newUserId))+" and group_id = "+connection.formatStringForSQL("user_groups", "group_id", Long.toString(groupId)));
                    } else {
                        return new CodaResponse(true, null, 2015);
                    }
                    break;
            }
            connection.commit();
            return new CodaResponse(false, "Success!", -1);

        } else {
            return new CodaResponse(true, null, 9005);
        }
    }

    public CodaResponse dropGroup (String sessionKey, String groupName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_GROUPS")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long groupId = this.getIdForObjectName(connection, groupName.toUpperCase(), CodaServer.OBJECT_TYPE_GROUP);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2017, "Group name is invalid");
            }

            Hashtable values = new Hashtable();
            values.put("active_flag", 0);
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            connection.updateRow("groups", "id", groupId, values);

            connection.commit();
            return new CodaResponse(false, "Success!", -1);

        } else {
            return new CodaResponse(true, null, 9005);
        }
    }

    public CodaResponse createType (String sessionKey, String typeName, String validationMask, String saveMask) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_TYPES")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            if (this.getIdForObjectName(connection, typeName.toUpperCase(), CodaServer.OBJECT_TYPE_TYPE) > 0) {
                return new CodaResponse(true, null, 2019);
            }
            try {
                Regex regex = new Regex();
                regex.compile(validationMask);
            } catch (com.stevesoft.pat.RegSyntax e) {
                return new CodaResponse(true, null, 2022);
            }
            try {
                Regex regex = new Regex();
                regex.compile(saveMask);
            } catch (com.stevesoft.pat.RegSyntax e) {
                return new CodaResponse(true, null, 2023);
            }

            try {
                String classFile = GroovyClassGenerator.getTypeClass(typeName, validationMask, saveMask);
                Hashtable values = new Hashtable();
                values.put("type_name", typeName.toUpperCase());
				values.put("built_in_flag", 0);
				values.put("validation_mask", validationMask);
                values.put("save_mask", saveMask);
                values.put("active_flag", "1");
                values.put("class_file", classFile);
                values.put("create_user_id", userId);
                values.put("create_date", new GregorianCalendar().getTimeInMillis());
                values.put("mod_user_id", userId);
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                long typeId = connection.insertRow("types", values);

				this.reloadType(typeId);

				this.sendTypeUpdateToCluster(typeId);


			} catch (IOException ex) {
                return new CodaResponse(true, null, 2083);
            }

            connection.commit();

            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9007);
        }
    }

	public CodaResponse alterType (String sessionKey, String typeName, String validationMask, String saveMask) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_TYPES")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long typeId = this.getIdForObjectName(connection, typeName.toUpperCase(), CodaServer.OBJECT_TYPE_TYPE);
            if (typeId < 0) {
                return new CodaResponse(true, null, 2020);
            }
            try {
                Regex regex = new Regex();
                regex.compile(validationMask);
            } catch (com.stevesoft.pat.RegSyntax e) {
                return new CodaResponse(true, null, 2022);
            }
            try {
                Regex regex = new Regex();
                regex.compile(saveMask);
            } catch (com.stevesoft.pat.RegSyntax e) {
                return new CodaResponse(true, null, 2023);
            }
            try {
                String classFile = GroovyClassGenerator.getTypeClass(typeName, validationMask, saveMask);

                Hashtable values = new Hashtable();
                values.put("validation_mask", validationMask);
                values.put("save_mask", saveMask);
                values.put("class_file", classFile);
                values.put("mod_user_id", userId);
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                connection.updateRow("types", "id", typeId, values);

                this.reloadType(typeId);

				this.sendTypeUpdateToCluster(typeId);

                connection.commit();
                return new CodaResponse(false, "Success!", -1, null);
            } catch (IOException e) {
                return new CodaResponse(true, null, 2084);
            }
        } else {
           return new CodaResponse(true, null, 9007);
        }
    }

    public CodaResponse dropType (String sessionKey, String typeName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_TYPES")) {
            CodaConnection connection = database.getConnection();
            long userId = sessions.getSessionUserId(sessionKey);
            if (userId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long typeId = this.getIdForObjectName(connection, typeName.toUpperCase(), CodaServer.OBJECT_TYPE_TYPE);
            if (typeId < 0) {
                return new CodaResponse(true, null, 2020);
            }

            Hashtable values = new Hashtable();
            values.put("active_flag", "0");
            values.put("mod_user_id", userId);
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

            connection.updateRow("types", "id", typeId, values);

			this.reloadType(typeId);

			this.sendTypeUpdateToCluster(typeId);

			connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9007);
        }
    }

    public CodaResponse grantServerPermissions(String sessionKey, Vector permissions, String username) {
        if (username.toUpperCase().equals("ROOT")) {
            return new CodaResponse(true, null, 9018);
        }
        if (sessions.hasServerPermission(sessionKey, "MANAGE_USERS")) {
            CodaConnection connection = database.getConnection();
            long modUserId = sessions.getSessionUserId(sessionKey);
            if (modUserId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
            if (userId < 0) {
                return new CodaResponse(true, null, 2015);
            }

            Enumeration en = permissions.elements();

            while (en.hasMoreElements()) {
                String permission = (String)en.nextElement();

                CodaResultSet rs = connection.runQuery("select count(*) from user_server_permissions where user_id = " + connection.formatStringForSQL("user_server_permissions", "user_id", Long.toString(userId)) + " and server_permission_name = " + connection.formatStringForSQL("user_server_permissions", "server_permission_name", permission.toUpperCase()), null);
                if (!rs.getErrorStatus() && rs.next()) {
                    if (rs.getDataInt(0) == 0){
                        Hashtable values = new Hashtable();
                        values.put("user_id", userId);
                        values.put("server_permission_name", permission.toUpperCase());

                        connection.insertRow("user_server_permissions", values);

					}
				} else {
                    return new CodaResponse(true, null, 8001);
                }
            }

			//sessions.loadSession(sessionKey);
			connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
        }
    }

    public CodaResponse revokeServerPermissions(String sessionKey, Vector permissions, String username) {
        if (username.toUpperCase().equals("ROOT")) {
            return new CodaResponse(true, null, 9018);
        }
        if (sessions.hasServerPermission(sessionKey, "MANAGE_USERS")) {
            CodaConnection connection = database.getConnection();
            long modUserId = sessions.getSessionUserId(sessionKey);
            if (modUserId < 0) {
                return  new CodaResponse(true, null, 1005);
            }
            long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
            if (userId < 0) {
                return new CodaResponse(true, null, 2015);
            }

            Enumeration en = permissions.elements();

            while (en.hasMoreElements()) {
                String permission = (String)en.nextElement();

                connection.runStatement("delete from user_server_permissions where user_id = " + connection.formatStringForSQL("user_server_permissions", "user_id", Long.toString(userId)) + " and server_permission_name = " + connection.formatStringForSQL("user_server_permissions", "server_permission_name", permission.toUpperCase()));
				//sessions.loadSession(sessionKey);
			}

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
        }
    }

    public CodaResponse grantApplicationPermissions(String sessionKey, Vector<String> permissions, String applicationName, String environmentString, String groupName, String username) {
        if (username.toUpperCase().equals("ROOT")) {
            return new CodaResponse(true, null, 9018);
        }
		CodaConnection connection = database.getConnection();
		long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
        if (userId < 0) {
            return new CodaResponse(true, null, 2015);
        }
        int environment = -1;
        if (environmentString != null) {
            if (environmentString.equalsIgnoreCase("DEV")) {
                environment = 1;
            } else if (environmentString.equalsIgnoreCase("TEST")) {
                environment = 2;
            } else if (environmentString.equalsIgnoreCase("PROD")) {
                environment = 3;
            } else  {
                return new CodaResponse(true, null, 2013, "Invalid environment specified");
            }
        }
        long groupId = -1;
        if (groupName != null) {
            groupId = this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
        }
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, environment, (deployedApplications.isGroupApplication(applicationName) && deployedApplications.canGroupUseApplication(applicationName, groupName) ? groupId : -1), "MANAGE_USERS")) {
            
            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);

            for (String permission : permissions) {
                if (!deployedApplications.hasApplicationPermission(applicationName, userId, environment, groupId, permission)) {
                    Hashtable values = new Hashtable();
                    values.put("user_id", userId);
                    values.put("application_id", applicationId);
                    if (environment > 0 && !permission.equalsIgnoreCase("developer")) {
                        values.put("environment", environment);
                    }
                    if (groupId > 0 && !permission.equalsIgnoreCase("developer") && !permission.equalsIgnoreCase("manage_crons")) {
                        values.put("group_id", groupId);
                    }
                    values.put("application_permission_name", permission.toUpperCase());

                    connection.insertRow("user_application_permissions", values);

                }

            }

            deployedApplications.get(applicationName).reloadUser(userId);

			this.sendApplicationUpdateToCluster(applicationName, environmentString);

			connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
        }
    }

    public CodaResponse revokeApplicationPermissions(String sessionKey, Vector permissions, String applicationName, String environmentString, String groupName, String username) {
        if (username.toUpperCase().equals("ROOT")) {
            return new CodaResponse(true, null, 9018);
        }
		CodaConnection connection = database.getConnection();
		long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
        if (userId < 0) {
            return new CodaResponse(true, null, 2015);
        }
        int environment = -1;
        if (environmentString != null) {
            if (environmentString.equalsIgnoreCase("DEV")) {
                environment = 1;
            } else if (environmentString.equalsIgnoreCase("TEST")) {
                environment = 2;
            } else if (environmentString.equalsIgnoreCase("PROD")) {
                environment = 3;
            } else  {
                return new CodaResponse(true, null, 2013, "Invalid environment specified");
            }
        }
        long groupId = -1;
        if (groupName != null) {
            groupId = this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
        }
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, environment, groupId, "MANAGE_USERS")) {
            Enumeration en = permissions.elements();
            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);

            while (en.hasMoreElements()) {
                String permission = (String)en.nextElement();

                connection.runStatement("delete from user_application_permissions where user_id = " + connection.formatStringForSQL("user_application_permissions", "user_id", Long.toString(userId)) + " and application_id = " + connection.formatStringForSQL("user_application_permissions", "application_id", Long.toString(applicationId)) + (environment > 0 && !permission.equalsIgnoreCase("developer") ? " and environment = " + connection.formatStringForSQL("user_application_permissions", "environment", Integer.toString(environment)) : "") + (groupId > 0 && !permission.equalsIgnoreCase("developer") && !permission.equalsIgnoreCase("manage_crons") ? " and group_id = " + connection.formatStringForSQL("user_application_permissions", "group_id", Long.toString(groupId)) : "") + " and application_permission_name = " + connection.formatStringForSQL("user_application_permissions", "application_permission_name", permission.toUpperCase()));
            }

            deployedApplications.get(applicationName).reloadUser(userId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);

        } else {
           return new CodaResponse(true, null, 9003, "Do not have permission to manage users");
        }
    }

    public CodaResponse grantApplicationToGroup(String sessionKey, String applicationName, String groupName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
            CodaConnection connection = database.getConnection();
            long groupId = -1;
            groupId = this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2017);
            }
            long modUserId = sessions.getSessionUserId(sessionKey);
            if (modUserId < 0) {
                return  new CodaResponse(true, null, 1005);
            }

            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2012);
            }
            CodaResultSet rs = connection.runQuery("select count(*) from group_applications where group_id = " + connection.formatStringForSQL("group_applications", "group_id", Long.toString(groupId)) + " and application_id = " + connection.formatStringForSQL("group_applications", "application_id", Long.toString(applicationId)), null);
            if (!rs.getErrorStatus() && rs.next()) {
                if (rs.getDataLong(0) == 0) {
                    Hashtable values = new Hashtable();
                    values.put("application_id", applicationId);
                    values.put("group_id", groupId);
                    values.put("create_user_id", modUserId);
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_id", modUserId);
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.insertRow("group_applications", values);
                }
            }
            connection.commit();

			this.sendApplicationUpdateToCluster(applicationName, "PROD");

			return new CodaResponse(false, "Success!", -1, null);
        } else {
           return new CodaResponse(true, null, 9002, "Do not have permission to manage applications");
        }
    }

    public CodaResponse revokeApplicationFromGroup(String sessionKey, String applicationName, String groupName) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
            CodaConnection connection = database.getConnection();

            long groupId = -1;
            groupId = this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2017);
            }
            long modUserId = sessions.getSessionUserId(sessionKey);
            if (modUserId < 0) {
                return  new CodaResponse(true, null, 1005);
            }

            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2012);
            }
            CodaResultSet rs = connection.runQuery("select count(*) from group_applications where group_id = " + connection.formatStringForSQL("group_applications", "group_id", Long.toString(groupId)) + " and application_id = " + connection.formatStringForSQL("group_applications", "application_id", Long.toString(applicationId)), null);
            if (!rs.getErrorStatus() && rs.next()) {
                if (rs.getDataLong(0) > 0) {
                    connection.runStatement("delete from group_applications where group_id = " + connection.formatStringForSQL("group_applications", "group_id", Long.toString(groupId)) + " and application_id = " + connection.formatStringForSQL("group_applications", "application_id", Long.toString(applicationId)) );
                    CodaResultSet rs2 = connection.runQuery("select distinct user_id from user_application_permissions where group_id = " + connection.formatStringForSQL("group_applications", "group_id", Long.toString(groupId)) + " and application_id = " + connection.formatStringForSQL("group_applications", "application_id", Long.toString(applicationId))  ,null);
                    if (!rs2.getErrorStatus()  && rs2.getRowsReturned() > 0) {
                        connection.runStatement("delete from user_application_permissions where group_id = " + connection.formatStringForSQL("group_applications", "group_id", Long.toString(groupId)) + " and application_id = " + connection.formatStringForSQL("group_applications", "application_id", Long.toString(applicationId)) );
                        DeployedApplication depApp = deployedApplications.get(applicationName);
                        while (rs2.next()) {
                            if (depApp != null) {
                                depApp.reloadUser(rs.getDataLong(0));
                            }
                        }
                    }
                }
            }
            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
           return new CodaResponse(true, null, 9002, "Do not have permission to manage applications");
        }
    }

    public CodaResponse formatDatasourceForCoda(String sessionKey, String datasourceName, String rootPassword) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_DATASOURCES")) {
            try {
                CodaDatabase connection = this.getCodaDatabase(datasourceName.toUpperCase());
                DatabaseFormatter.formatCodaDatabase(connection, rootPassword, false);
            } catch (ClassNotFoundException e) {
                return new CodaResponse(true, null, 8002);
            } catch (InstantiationException e) {
                return new CodaResponse(true, null, 8002);
            } catch (IllegalAccessException e) {
                return new CodaResponse(true, null, 8002);
            }

            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9001);
        }
    }

    public boolean formatConfigDatabase(String adminUsername, String adminPassword, String rootPassword, boolean echo) {
        boolean retval = false;
		if (echo) {
			System.out.println("Formatting Config Database...");
		}
		if (this.configuration.getDatabaseConfiguration()[0].configurationComplete()) {
            try {
                Class.forName(this.configuration.getDatabaseConfiguration()[0].getDriverClass());
            } catch (ClassNotFoundException e) {
                System.out.println("The specified driver class, \"" + this.configuration.getDatabaseConfiguration()[0].getDriverClass() + "\", is not in the classpath.");
                System.exit(0);
            }
            try {
                database = (CodaDatabase) Class.forName(this.configuration.getDatabaseConfiguration()[0].getDriverClass()).newInstance();
                Hashtable options = new Hashtable();
                for (int i =0; i < this.configuration.getDatabaseConfiguration()[0].getOptions().length; i++) {
                    options.put(this.configuration.getDatabaseConfiguration()[0].getOptions()[i].getSetting(), this.configuration.getDatabaseConfiguration()[0].getOptions()[i].getValue());
                }
                retval = database.createSchema(configuration.getDatabaseConfiguration()[0].getHostname(), configuration.getDatabaseConfiguration()[0].getUsername(), configuration.getDatabaseConfiguration()[0].getPassword(), configuration.getDatabaseConfiguration()[0].getSchema(), options, adminUsername, adminPassword);
                if (echo) {
					System.out.println("...Schema created");
				}
				database.connect(configuration.getDatabaseConfiguration()[0].getHostname(), configuration.getDatabaseConfiguration()[0].getUsername(), configuration.getDatabaseConfiguration()[0].getPassword(), configuration.getDatabaseConfiguration()[0].getSchema(), options);
				if (echo) {
					System.out.println("...Connected as new user");
				}
				if (retval) {
                    DatabaseFormatter.formatCodaDatabase(database, rootPassword, echo);
                } else {
					if (echo) {
						System.out.println("Could not connect to database with new user.");
					}
					retval = false;
                }
            } catch (ClassNotFoundException e) {
				if (echo) {
					System.out.println("The following error was reported: " + e.getMessage());
				}
				retval = false;
            } catch (InstantiationException e) {
                if (echo) {
					System.out.println("The following error was reported: " + e.getMessage());
				}
				retval = false;
            } catch (IllegalAccessException e) {
                if (echo) {
					System.out.println("The following error was reported: " + e.getMessage());
				}
				retval = false;
            }
        } else {
            retval = false;
        }
        return retval;
    }

    private void setRevisionSystemInfo(CodaDatabase database, long revisionId, long refTableRevisionId) {
        CodaConnection connection = database.getConnection();
        connection.runStatement("update coda_system_information set system_value = " + connection.formatStringForSQL("coda_system_information", "system_value", Long.toString(revisionId)) + " where system_property = " + connection.formatStringForSQL("coda_system_information", "system_property", "REVISION_ID"));
        connection.runStatement("update coda_system_information set system_value = " + connection.formatStringForSQL("coda_system_information", "system_value", Long.toString(refTableRevisionId)) + " where system_property = " + connection.formatStringForSQL("coda_system_information", "system_property", "REF_TABLE_REVISION_ID"));
        connection.commit();
    }

    public CodaResponse formatDatasourceForApplication(String sessionKey, String datasourceName, String applicationName, long revisionId, String prefix, boolean overwriteFlag) {
        if (sessions.hasServerPermission(sessionKey, "MANAGE_DATASOURCES")) {
			CodaConnection connection = database.getConnection();
            long applicationId = this.getIdForObjectName(connection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
			if (applicationId < 0) {
				return new CodaResponse(true, null, 2012, "Application name is invalid");
			}

			if (revisionId < 0) {
				CodaResultSet rs = connection.runQuery("select max(revision_id) from transactions where application_id = " +connection.formatStringForSQL("transactions", "application_id", Long.toString(applicationId)), null);
				if (!rs.getErrorStatus() && rs.next()) {
					if (rs.getData(0) != null) {
						revisionId = rs.getDataLong(0);
					} else {
						revisionId = -1;
					}
				} else {
					return new CodaResponse(true, null, 8001);
				}
			}

			try {
                CodaDatabase localConn = this.getCodaDatabase(datasourceName.toUpperCase());
                try {
					CodaConnection conn = localConn.getConnection();
					CodaSystemTable systemInfo = conn.getMetadata().getSystemTable();
					if (systemInfo != null && !systemInfo.isCodaServerFlag() && systemInfo.getApplicationName().equalsIgnoreCase(applicationName) && revisionId > systemInfo.getRevisionId()) {
						long lastRevisionId = systemInfo.getRevisionId(), lastRefTableRevisionId = systemInfo.getRefTableRevisionId();
						CodaResultSet rs = connection.runQuery("select coda_statement, revision_id, ref_table_flag from transactions where application_id = " +connection.formatStringForSQL("transactions", "application_id", Long.toString(applicationId)) + " and ref_table_flag = 0 and revision_id > " + connection.formatStringForSQL("transactions", "revision_id", Long.toString(systemInfo.getRevisionId())) + " and revision_id "+ (systemInfo.getRefTableRevisionId() > revisionId ? "< " + connection.formatStringForSQL("transactions", "revision_id", Long.toString(systemInfo.getRefTableRevisionId())) : " <= " + connection.formatStringForSQL("transactions", "revision_id", Long.toString(revisionId))), null);
						if (!rs.getErrorStatus()) {
							while (rs.next()) {
								CodaParser parser = new CodaParser(new CommonTokenStream(new CodaLexer(new CaseInsensitiveStringStream(rs.getData(0)))), this, sessionKey, localConn);
								CodaResponse resp = parser.stat().response;
								if (resp.getError()) {
									setRevisionSystemInfo(localConn, lastRevisionId, lastRefTableRevisionId);
									return new CodaResponse(true, null, 2081, "There was an error while formatting the application database.  Last update: Revision " + lastRevisionId);
								} else {
									lastRevisionId = rs.getDataLong(1);
									if (rs.getDataInt(2) == 1) {
										lastRefTableRevisionId = rs.getDataLong(1);
									}
								}
							}
						} else {
							setRevisionSystemInfo(localConn, lastRevisionId, lastRefTableRevisionId);
							return new CodaResponse(true, null, 2081, "There was an error while formatting the application database.  Last update: Revision " + lastRevisionId);
						}
						if (lastRevisionId != revisionId && revisionId != systemInfo.getRefTableRevisionId()) {
							rs = connection.runQuery("select coda_statement, revision_id, ref_table_flag from transactions where application_id = " +connection.formatStringForSQL("transactions", "application_id", Long.toString(applicationId)) + " and revision_id > " + connection.formatStringForSQL("transactions", "revision_id", Long.toString(systemInfo.getRefTableRevisionId())) + " and revision_id <= " + connection.formatStringForSQL("transactions", "revision_id", Long.toString(revisionId)), null);
							if (!rs.getErrorStatus()) {
								while (rs.next()) {
									CodaParser parser = new CodaParser(new CommonTokenStream(new CodaLexer(new CaseInsensitiveStringStream(rs.getData(0)))), this, sessionKey, localConn);
									CodaResponse resp = parser.stat().response;
									if (resp.getError()) {
										setRevisionSystemInfo(localConn, lastRevisionId, lastRefTableRevisionId);
										return new CodaResponse(true, null, 2081, "There was an error while formatting the application database.  Last update: Revision " + lastRevisionId);
									} else {
										lastRevisionId = rs.getDataLong(1);
										if (rs.getDataInt(2) == 1) {
											lastRefTableRevisionId = rs.getDataLong(1);
										}
									}
								}
							} else {
								setRevisionSystemInfo(localConn, lastRevisionId, lastRefTableRevisionId);
								return new CodaResponse(true, null, 2081, "There was an error while formatting the application database.  Last update: Revision " + lastRevisionId);
							}
						}
						if (revisionId == systemInfo.getRefTableRevisionId()) {
							lastRevisionId = systemInfo.getRefTableRevisionId();
						}
						setRevisionSystemInfo(localConn, lastRevisionId, lastRefTableRevisionId);
						return new CodaResponse(false, "Success!", -1, null);
					} else if (overwriteFlag) {
						DatabaseFormatter.formatApplicationDatabase(localConn, applicationName, prefix);
						//return formatDatasourceForApplication(sessionKey, datasourceName, applicationName, revisionId, prefix, false);
						return new CodaResponse(false, "Success!", -1, null);
					} else {
						return new CodaResponse(true, null, 2082);
					}

                } catch (RecognitionException e) {
                    return new CodaResponse(true, null, 8002);
                } catch (NullPointerException e) {
                    return new CodaResponse(true, null, 8002);
                }
            } catch (ClassNotFoundException e) {
                return new CodaResponse(true, null, 8002);
            } catch (IllegalAccessException e) {
                return new CodaResponse(true, null, 8002);
            } catch (InstantiationException e) {
                return new CodaResponse(true, null, 8002);
            } catch (NullPointerException e) {
				return new CodaResponse(true, null, 8002);
			}

        } else {
            return new CodaResponse(true, null, 9001);
        }
    }

    public CodaResponse grantRoleToUser(String sessionKey, String roleName, String applicationName, String environmentString, String groupName, String username) {
        if (username.toUpperCase().equals("ROOT")) {
            return new CodaResponse(true, null, 9004);
        }
		CodaConnection connection = database.getConnection();
		long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
        if (userId < 0) {
            return new CodaResponse(true, null, 2015);
        }
        int environment = 1;
        if (environmentString.equalsIgnoreCase("DEV")) {
            environment = 1;
        } else if (environmentString.equalsIgnoreCase("TEST")) {
            environment = 2;
        } else if (environmentString.equalsIgnoreCase("PROD")) {
            environment = 3;
        } else {
            return new CodaResponse(true, null, 2013, "Invalid environment specified");
        }
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        long groupId = -1;
        if (deployedApplications.get(applicationName).isGroupFlag() && groupName != null) {
            groupId = this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
        } else if (deployedApplications.get(applicationName).isGroupFlag() && groupName == null) {
            return new CodaResponse(true, null, 2026);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, environment, groupId, "MANAGE_USERS")) {
            Datasource datasource = deployedApplications.get(applicationName).getEnvironmentDatasource(environment);
            if (datasource != null) {
                CodaConnection connection2 = datasource.getDatabase().getConnection();
                long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection2);
                if (roleId < 0) {
                    return new CodaResponse(true, null, 2029);
                }
                if (!datasource.hasRole(userId, groupId, roleName)) {
                    Hashtable values = new Hashtable();
                    if (groupId > 0) {
                        values.put("group_id", groupId);
                    }
                    values.put("user_id", userId);
                    values.put("role_id", roleId);
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());

                    connection2.insertRow(datasource.getPrefix() + "user_roles", values);

                    deployedApplications.get(applicationName).getEnvironmentDatasource(environment).reloadUser(userId, groupId);

                    connection2.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                } else {
                    return new CodaResponse(true, null, 2024);
                }
            } else {
                return new CodaResponse(true, null, 8003);
            }
        } else {
            return new CodaResponse(true, null, 9008);
        }
    }

    public CodaResponse revokeRoleFromUser(String sessionKey, String roleName, String applicationName, String environmentString, String groupName, String username) {
        if (username.toUpperCase().equals("ROOT")) {
            return new CodaResponse(true, null, 9004);
        }
		CodaConnection connection = database.getConnection();
		long userId = this.getIdForObjectName(connection, username.toUpperCase(), CodaServer.OBJECT_TYPE_USER);
        if (userId < 0) {
            return new CodaResponse(true, null, 2015);
        }
        int environment = 1;
        if (environmentString.equalsIgnoreCase("DEV")) {
            environment = 1;
        } else if (environmentString.equalsIgnoreCase("TEST")) {
            environment = 2;
        } else if (environmentString.equalsIgnoreCase("PROD")) {
            environment = 3;
        } else {
            return new CodaResponse(true, null, 2013, "Invalid environment specified");
        }
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        long groupId = -1;
        if (deployedApplications.get(applicationName).isGroupFlag() && groupName != null) {
            groupId = this.getIdForObjectName(connection, groupName, CodaServer.OBJECT_TYPE_GROUP);
        } else if (deployedApplications.get(applicationName).isGroupFlag() && groupName == null) {
            return new CodaResponse(true, null, 2026);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, environment, groupId, "MANAGE_USERS")) {
            Datasource datasource = deployedApplications.get(applicationName).getEnvironmentDatasource(environment);
            if (datasource != null) {
                CodaConnection connection2 = datasource.getDatabase().getConnection();
                long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection2);
                if (roleId < 0) {
                    return new CodaResponse(true, null, 2029);
                }
                if (datasource.hasRole(userId, groupId, roleName)) {
                    connection2.runStatement("delete from " + datasource.getPrefix() + "user_roles where user_id = " + connection.formatStringForSQL(datasource.getPrefix() + "user_roles", "user_id", Long.toString(userId)) + " and role_id = " + connection.formatStringForSQL(datasource.getPrefix() + "user_roles", "role_id", Long.toString(roleId)) + " and group_id " + (groupId < 0 ? " is null " : " = " + connection.formatStringForSQL("user_roles", "group_id", Long.toString(groupId)) ));

                    deployedApplications.get(applicationName).getEnvironmentDatasource(environment).reloadUser(userId, groupId);

                    connection2.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                } else {
                    return new CodaResponse(true, null, 2025);
                }
            } else {
                return new CodaResponse(true, null, 8003);
            }
        } else {
            return new CodaResponse(true, null, 9008);
        }
    }

    public CodaResponse grantPermissionsToRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            // Test the input permissions
            Vector<Long> permissionIds = new Vector();
            for (String permission : permissions) {
                long permissionId = this.getIdForObjectName(permission, CodaServer.OBJECT_TYPE_PERMISSION, connection);
                if (permissionId < 0) {
                    return new CodaResponse(true, null, 2030, "Permission '" + permission.toUpperCase() + "' does not exist");
                } else {
                    permissionIds.add(permissionId);
                }
            }

            // get the current permissions
            CodaResultSet rs = connection.runQuery("select permission_id from "+ prefix +"role_permissions where role_id = " + connection.formatStringForSQL(prefix+"role_permissions", "role_id", Long.toString(roleId)),null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    if (permissionIds.contains(rs.getDataLong(0))) {
                        permissionIds.remove(rs.getDataLong(0));
                    }
                }
            }

            // insert each new permission
            Hashtable values = new Hashtable();
            values.put("role_id", roleId);
            for (Long permissionId : permissionIds) {
                values.put("permission_id", permissionId);
                connection.insertRow(prefix + "role_permissions", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse revokePermissionsFromRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            // Test the input permissions
            Vector<Long> permissionIds = new Vector();
            for (String permission : permissions) {
                long permissionId = this.getIdForObjectName(permission, CodaServer.OBJECT_TYPE_PERMISSION, connection);
                if (permissionId < 0) {
                    return new CodaResponse(true, null, 2030, "Permission '" + permission.toUpperCase() + "' does not exist");
                } else {
                    permissionIds.add(permissionId);
                }
            }

            for (Long permissionId : permissionIds) {
                connection.runStatement("delete from "+prefix+"role_permissions where role_id = " + connection.formatStringForSQL(prefix+"role_permissions", "role_id", Long.toString(roleId)) + " and permission_id = "+ connection.formatStringForSQL(prefix+"role_permissions", "permission_id", permissionId.toString()));
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse grantTablePermissionsToRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String tableName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            boolean selectPermission = false, insertPermission = false, updatePermission = false, deletePermission = false;
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase("SELECT")) {
                    selectPermission = true;
                } else if (permission.equalsIgnoreCase("INSERT")) {
                    insertPermission = true;
                } else if (permission.equalsIgnoreCase("UPDATE")) {
                    updatePermission = true;
                } else if (permission.equalsIgnoreCase("DELETE")) {
                    deletePermission = true;
                }
            }
            // get the current permissions
            CodaResultSet rs = connection.runQuery("select select_flag, insert_flag, update_flag, delete_flag from "+ prefix +"role_tables where role_id = " + connection.formatStringForSQL(prefix+"role_tables", "role_id", Long.toString(roleId)) + " and table_id = "+ connection.formatStringForSQL(prefix+"role_tables", "table_id", Long.toString(tableId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                selectPermission = selectPermission || rs.getDataInt(0) == 1;
                insertPermission = insertPermission || rs.getDataInt(1) == 1;
                updatePermission = updatePermission || rs.getDataInt(2) == 1;
                deletePermission = deletePermission || rs.getDataInt(3) == 1;

                connection.runStatement("update "+prefix+"role_tables set select_flag = "+connection.formatStringForSQL(prefix+"role_tables", "select_flag", (selectPermission ? "1" : "0"))+", insert_flag = "+connection.formatStringForSQL(prefix+"role_tables", "insert_flag", (insertPermission ? "1" : "0"))+", update_flag = "+connection.formatStringForSQL(prefix+"role_tables", "update_flag", (updatePermission ? "1" : "0"))+", delete_flag = "+connection.formatStringForSQL(prefix+"role_tables", "delete_flag", (deletePermission ? "1" : "0"))+" where role_id = " + connection.formatStringForSQL(prefix+"role_tables", "role_id", Long.toString(roleId)) + " and table_id = "+ connection.formatStringForSQL("role_tables", "table_id", Long.toString(tableId)));
            } else {
                Hashtable values = new Hashtable();
                values.put("role_id", roleId);
                values.put("table_id", tableId);
                values.put("select_flag", selectPermission ? "1" : "0");
                values.put("insert_flag", insertPermission ? "1" : "0");
                values.put("update_flag", updatePermission ? "1" : "0");
                values.put("delete_flag", deletePermission ? "1" : "0");
                connection.insertRow(prefix+"role_tables", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse revokeTablePermissionsFromRole(String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String tableName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            boolean selectPermission = false, insertPermission = false, updatePermission = false, deletePermission = false;
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase("SELECT")) {
                    selectPermission = true;
                } else if (permission.equalsIgnoreCase("INSERT")) {
                    insertPermission = true;
                } else if (permission.equalsIgnoreCase("UPDATE")) {
                    updatePermission = true;
                } else if (permission.equalsIgnoreCase("DELETE")) {
                    deletePermission = true;
                }
            }
            // get the current permissions
            CodaResultSet rs = connection.runQuery("select select_flag, insert_flag, update_flag, delete_flag from "+prefix+"role_tables where role_id = " + connection.formatStringForSQL(prefix+"role_tables", "role_id", Long.toString(roleId)) + " and table_id = "+ connection.formatStringForSQL(prefix+"role_tables", "table_id", Long.toString(tableId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                selectPermission = selectPermission ? false : rs.getDataInt(0) == 1;
                insertPermission = insertPermission ? false : rs.getDataInt(1) == 1;
                updatePermission = updatePermission ? false : rs.getDataInt(2) == 1;
                deletePermission = deletePermission ? false : rs.getDataInt(3) == 1;

                connection.runStatement("update "+prefix+"role_tables set select_flag = "+connection.formatStringForSQL(prefix+"role_tables", "select_flag", (selectPermission ? "1" : "0"))+", insert_flag = "+connection.formatStringForSQL(prefix+"role_tables", "insert_flag", (insertPermission ? "1" : "0"))+", update_flag = "+connection.formatStringForSQL(prefix+"role_tables", "update_flag", (updatePermission ? "1" : "0"))+", delete_flag = "+connection.formatStringForSQL(prefix+"role_tables", "delete_flag", (deletePermission ? "1" : "0"))+" where role_id = " + connection.formatStringForSQL(prefix+"role_tables", "role_id", Long.toString(roleId)) + " and table_id = "+ connection.formatStringForSQL(prefix+"role_tables", "table_id", Long.toString(tableId)));
            } else {
                Hashtable values = new Hashtable();
                values.put("role_id", roleId);
                values.put("table_id", tableId);
                values.put("select_flag", "0");
                values.put("insert_flag", "0");
                values.put("update_flag", "0");
                values.put("delete_flag", "0");
                connection.insertRow(prefix+"role_tables", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse grantFormStatusPermissionsToRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String formName, String formStatusVerb, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long tableId = this.getIdForObjectName(formName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2032);
            }
            long formStatusId = this.getIdForObjectName(Long.toString(tableId) + "." + formStatusVerb, CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
            if (formStatusId < 0) {
                return new CodaResponse(true, null, 2031);
            }
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            boolean viewFlag = false, callFlag = false, updateFlag = false;
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase("VIEW")) {
                    viewFlag = true;
                } else if (permission.equalsIgnoreCase("CALL")) {
                    callFlag = true;
                } else if (permission.equalsIgnoreCase("UPDATE")) {
                    updateFlag = true;
                }
            }
            // get the current permissions
            CodaResultSet rs = connection.runQuery("select view_flag, call_flag, update_flag from "+prefix+"role_form_statuses where role_id = " + connection.formatStringForSQL(prefix+"role_form_statuses", "role_id", Long.toString(roleId)) + " and form_status_id = "+ connection.formatStringForSQL(prefix+"role_form_statuses", "form_status_id", Long.toString(formStatusId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                viewFlag = viewFlag || rs.getDataInt(0) == 1;
                callFlag = callFlag || rs.getDataInt(1) == 1;
                updateFlag = updateFlag || rs.getDataInt(2) == 1;

                connection.runStatement("update "+prefix+"role_form_statuses set view_flag = "+connection.formatStringForSQL(prefix+"role_form_statuses", "view_flag", (viewFlag ? "1" : "0"))+", call_flag = "+connection.formatStringForSQL(prefix+"role_form_statuses", "call_flag", (callFlag ? "1" : "0"))+", update_flag = "+connection.formatStringForSQL(prefix+"role_form_statuses", "update_flag", (updateFlag ? "1" : "0"))+" where role_id = " + connection.formatStringForSQL(prefix+"role_form_statuses", "role_id", Long.toString(roleId)) + " and form_status_id = "+ connection.formatStringForSQL(prefix+"role_form_statuses", "form_status_id", Long.toString(formStatusId)));
            } else {
                Hashtable values = new Hashtable();
                values.put("role_id", roleId);
                values.put("form_status_id", formStatusId);
                values.put("view_flag", viewFlag ? "1" : "0");
                values.put("call_flag", callFlag ? "1" : "0");
                values.put("update_flag", updateFlag ? "1" : "0");
                connection.insertRow(prefix+"role_form_statuses", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse revokeFormStatusPermissionsFromRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String formName, String formStatusVerb, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long tableId = this.getIdForObjectName(formName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2032);
            }
            long formStatusId = this.getIdForObjectName(Long.toString(tableId) + "." + formStatusVerb, CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
            if (formStatusId < 0) {
                return new CodaResponse(true, null, 2031);
            }
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            boolean viewFlag = false, callFlag = false, updateFlag = false;
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase("VIEW")) {
                    viewFlag = true;
                } else if (permission.equalsIgnoreCase("CALL")) {
                    callFlag = true;
                } else if (permission.equalsIgnoreCase("UPDATE")) {
                    updateFlag = true;
                }
            }
            // get the current permissions
            CodaResultSet rs = connection.runQuery("select view_flag, call_flag, update_flag from "+prefix+"role_form_statuses where role_id = " + connection.formatStringForSQL(prefix+"role_form_statuses", "role_id", Long.toString(roleId)) + " and form_status_id = "+ connection.formatStringForSQL(prefix+"role_form_statuses", "form_status_id", Long.toString(formStatusId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                viewFlag = viewFlag ? false : rs.getDataInt(0) == 1;
                callFlag = callFlag ? false : rs.getDataInt(1) == 1;
                updateFlag = updateFlag ? false : rs.getDataInt(2) == 1;

                connection.runStatement("update "+prefix+"role_form_statuses set view_flag = "+connection.formatStringForSQL(prefix+"role_form_statuses", "view_flag", (viewFlag ? "1" : "0"))+", call_flag = "+connection.formatStringForSQL(prefix+"role_form_statuses", "call_flag", (callFlag ? "1" : "0"))+", update_flag = "+connection.formatStringForSQL(prefix+"role_form_statuses", "update_flag", (updateFlag ? "1" : "0"))+" where role_id = " + connection.formatStringForSQL(prefix+"role_form_statuses", "role_id", Long.toString(roleId)) + " and form_status_id = "+ connection.formatStringForSQL(prefix+"role_form_statuses", "form_status_id", Long.toString(formStatusId)));
            } else {
                Hashtable values = new Hashtable();
                values.put("role_id", roleId);
                values.put("form_status_id", formStatusId);
                values.put("view_flag", "0");
                values.put("call_flag", "0");
                values.put("update_flag", "0");
                connection.insertRow(prefix + "role_form_statuses", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse grantProcedurePermissionsToRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String procedureName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
            if (procedureId < 0) {
                return new CodaResponse(true, null, 2033);
            }
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            boolean executeFlag = false;
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase("EXECUTE")) {
                    executeFlag = true;
                }
            }
            // get the current permissions
            CodaResultSet rs = connection.runQuery("select execute_flag from "+prefix+"role_procedures where role_id = " + connection.formatStringForSQL(prefix+"role_procedures", "role_id", Long.toString(roleId)) + " and procedure_id = "+ connection.formatStringForSQL(prefix+"role_procedures", "procedure_id", Long.toString(procedureId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                executeFlag = executeFlag || rs.getDataInt(0) == 1;

                connection.runStatement("update "+prefix+"role_procedures set execute_flag = "+connection.formatStringForSQL(prefix+"role_procedures", "execute_flag", (executeFlag ? "1" : "0"))+" where role_id = " + connection.formatStringForSQL(prefix+"role_procedures", "role_id", Long.toString(roleId)) + " and procedure_id = "+ connection.formatStringForSQL(prefix+"role_procedures", "procedure_id", Long.toString(procedureId)));
            } else {
                Hashtable values = new Hashtable();
                values.put("role_id", roleId);
                values.put("procedure_id", procedureId);
                values.put("execute_flag", executeFlag ? "1" : "0");
                connection.insertRow(prefix+"role_procedures", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse revokeProcedurePermissionsFromRole (String sessionKey, CodaDatabase database, Vector<String> permissions, String applicationName, String procedureName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
            if (procedureId < 0) {
                return new CodaResponse(true, null, 2033);
            }
            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            boolean executeFlag = false;
            for (String permission : permissions) {
                if (permission.equalsIgnoreCase("EXECUTE")) {
                    executeFlag = true;
                }
            }
            // get the current permissions
            CodaResultSet rs = connection.runQuery("select execute_flag from "+prefix+"role_procedures where role_id = " + connection.formatStringForSQL(prefix+"role_procedures", "role_id", Long.toString(roleId)) + " and procedure_id = "+ connection.formatStringForSQL(prefix+"role_procedures", "procedure_id", Long.toString(procedureId)),null);
            if (!rs.getErrorStatus() && rs.next()) {
                executeFlag = executeFlag ? false : rs.getDataInt(0) == 1;

                connection.runStatement("update "+prefix+"role_procedures set execute_flag = "+connection.formatStringForSQL(prefix+"role_procedures", "execute_flag", (executeFlag ? "1" : "0"))+" where role_id = " + connection.formatStringForSQL(prefix+"role_procedures", "role_id", Long.toString(roleId)) + " and procedure_id = "+ connection.formatStringForSQL(prefix+"role_procedures", "procedure_id", Long.toString(procedureId)));
            } else {
                Hashtable<String, Object> values = new Hashtable();
                values.put("role_id", roleId);
                values.put("procedure_id", procedureId);
                values.put("execute_flag", "0");
                connection.insertRow(prefix+"role_procedures", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse createRole(String sessionKey, CodaDatabase database, String applicationName, String roleName, String displayName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            if (this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection) > 0) {
                return new CodaResponse(true, null, 2034);
            }
            if (displayName == null) {
                displayName = roleName;
            }
            Hashtable<String, Object> values = new Hashtable();
            values.put("role_name", roleName.toUpperCase());
            values.put("display_name", displayName);
            values.put("create_user_name", sessions.getSessionUsername(sessionKey));
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            connection.insertRow(prefix+"roles", values);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse alterRole(String sessionKey, CodaDatabase database, String applicationName, String roleName, int operation, String newRoleName, String displayName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            Hashtable values;
            switch (operation) {
                case 1:
                    values = new Hashtable();
                    values.put("display_name", displayName);
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"roles", "id", roleId, values);
                    break;
                case 2:
                    if (this.getIdForObjectName(newRoleName, CodaServer.OBJECT_TYPE_ROLE, connection) > 0) {
                        return new CodaResponse(true, null, 2034);
                    }
                    values = new Hashtable();
                    values.put("role_name", newRoleName.toUpperCase());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"roles", "id", roleId, values);
                    break;

            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse dropRole(String sessionKey, CodaDatabase database, String applicationName, String roleName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long roleId = this.getIdForObjectName(roleName, CodaServer.OBJECT_TYPE_ROLE, connection);
            if (roleId < 0) {
                return new CodaResponse(true, null, 2029);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            // remove all user_roles
            connection.runStatement("delete from "+prefix+"user_roles where role_id = " + connection.formatStringForSQL(prefix+"user_roles", "role_id", Long.toString(roleId)));

            // remove all role permissions, role table permissions, role form status permissions, and role procedure permissions
            connection.runStatement("delete from "+prefix+"role_permissions where role_id = " + connection.formatStringForSQL(prefix+"role_permissions", "role_id", Long.toString(roleId)));
            connection.runStatement("delete from "+prefix+"role_tables where role_id = " + connection.formatStringForSQL(prefix+"role_tables", "role_id", Long.toString(roleId)));
            connection.runStatement("delete from "+prefix+"role_form_statuses where role_id = " + connection.formatStringForSQL(prefix+"role_form_statuses", "role_id", Long.toString(roleId)));
            connection.runStatement("delete from "+prefix+"role_procedures where role_id = " + connection.formatStringForSQL(prefix+"role_procedures", "role_id", Long.toString(roleId)));

            // remove the role itself
            connection.deleteRow(prefix+"roles", "id", roleId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse createPermission(String sessionKey, CodaDatabase database, String applicationName, String permissionName, String displayName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            if (this.getIdForObjectName(permissionName, CodaServer.OBJECT_TYPE_PERMISSION, connection) > 0) {
                return new CodaResponse(true, null, 2035);
            }
            if (displayName == null) {
                displayName = permissionName;
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            Hashtable values = new Hashtable();
            values.put("permission_name", permissionName.toUpperCase());
            values.put("display_name", displayName);
            values.put("create_user_name", sessions.getSessionUsername(sessionKey));
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            connection.insertRow(prefix+"permissions", values);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse alterPermission(String sessionKey, CodaDatabase database, String applicationName, String permissionName, int operation, String newPermissionName, String displayName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            long permissionId = this.getIdForObjectName(permissionName, CodaServer.OBJECT_TYPE_PERMISSION, connection);
            if (permissionId < 0) {
                return new CodaResponse(true, null, 2030);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            Hashtable<String,Object> values;
            switch (operation) {
                case 1:
                    values = new Hashtable();
                    values.put("display_name", displayName);
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix + "permissions", "id", permissionId, values);
                    break;
                case 2:
                    if (this.getIdForObjectName(newPermissionName, CodaServer.OBJECT_TYPE_PERMISSION, connection) > 0) {
                        return new CodaResponse(true, null, 2035);
                    }
                    values = new Hashtable();
                    values.put("permission_name", newPermissionName.toUpperCase());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"permissions", "id", permissionId, values);
                    break;

            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse dropPermission(String sessionKey, CodaDatabase database, String applicationName, String permissionName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            long permissionId = this.getIdForObjectName(permissionName, CodaServer.OBJECT_TYPE_PERMISSION, connection);
            if (permissionId < 0) {
                return new CodaResponse(true, null, 2030);
            }

            // remove all role permissions
            connection.runStatement("delete from "+prefix+"role_permissions where permission_id = " + connection.formatStringForSQL(prefix+"role_permissions", "permission_id", Long.toString(permissionId)));

            // remove the role itself
            connection.deleteRow(prefix+"permissions", "id", permissionId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    private boolean isSubtableName(CodaDatabase database, String prefix, long tableId, String tableName) {
        CodaConnection connection = database.getConnection();
        CodaResultSet rs = connection.runQuery("select count(*) from "+prefix+"tables where table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", tableName.toUpperCase()) + " and parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)),null);
        if (!rs.getErrorStatus() && rs.next()) {
            return rs.getDataInt(0) == 1;
        } else {
            return false;
        }
    }

    // index 0 is the previous hashtable, index 1 is the next
    private Vector<Hashtable> prepareHashtablesForTrigger(ExecutionContext context, CodaConnection connection, Hashtable<String, TableFieldDefinition> fields, Hashtable prev, Hashtable next) {
        Vector retval = new Vector();
        Hashtable newPrev = new Hashtable();
        if (prev != null) {
            for (Object key : prev.keySet()) {
				try {
					newPrev.put(((String)key).toLowerCase(), context.parse(this.getIdForObjectName(connection, fields.get((String)key).getTypeName(), CodaServer.OBJECT_TYPE_TYPE), prev.get((String)key).toString()));
				} catch (CodaException e) {
					// do nothing
				}
			}
        }
        retval.add(prev != null ? newPrev.clone() : null);
		newPrev = new Hashtable();
		if (next != null) {
            for (Object key : next.keySet()) {
				try {
					newPrev.put(((String)key).toLowerCase(), context.parse(this.getIdForObjectName(connection, fields.get((String)key).getTypeName(), CodaServer.OBJECT_TYPE_TYPE), next.get((String)key).toString()));
				} catch (CodaException e) {
					// do nothing
				}
			}
        }
        retval.add(newPrev);
        return retval;
    }

    private Vector<String> getReservedObjectNames() {
        Vector<String> retval = new Vector();
        retval.add("GRANT");
        retval.add("REVOKE");
        retval.add("EXECUTE");
        retval.add("CONNECT");
        retval.add("CREATE");
        retval.add("SELECT");
        retval.add("UPDATE");
        retval.add("DELETE");
        retval.add("NULL");
        retval.add("PROCEDURE");
        retval.add("CURRENT_TIMESTAMP");
        retval.add("TABLE");
        retval.add("FORM");
        retval.add("TRIGGER");
        retval.add("FROM");
        retval.add("WHERE");
        retval.add("HAVING");
        retval.add("CURRENT_USER_ID");
        retval.add("CURRENT_USERNAME");
        retval.add("CURRENT_GROUP_NAME");
        retval.add("USER");
        retval.add("APPLICATION");
        retval.add("GROUP");
        retval.add("CRON");
        retval.add("ALTER");
        retval.add("DROP");
        return retval;
    }

    private Vector<String> getReservedColumnNames() {
        Vector<String> retval = this.getReservedObjectNames();
        retval.add("ID");
        retval.add("CREATE_USER_ID");
        retval.add("CREATE_USER_NAME");
        retval.add("CREATE_DATE");
        retval.add("MOD_USER_ID");
        retval.add("MOD_USER_NAME");
        retval.add("MOD_DATE");
        retval.add("PARENT_TABLE_ID");
        retval.add("GROUP_ID");
        retval.add("ACTIVE_FLAG");
        retval.add("STATUS_ID");
        return retval;
    }

    private Hashtable<String,TableFieldDefinition> getFieldsForTable(CodaConnection connection, String prefix, long tableId, CodaConnection serverConnection) {
        
        Hashtable<String,TableFieldDefinition> retval = new Hashtable();
        CodaResultSet rs = connection.runQuery("select tf.id, tf.field_name, tf.display_name, tf.type_name, tf.array_flag, tf.nullable_flag, tf.ref_table_id, tf.default_variable_id, tf.default_value, tf.create_user_name, tf.create_date, tf.mod_user_name, tf.mod_date from "+prefix+"table_fields tf where tf.table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", Long.toString(tableId)), null);
        if (!rs.getErrorStatus()) {
            while(rs.next()) {
                TableFieldDefinition temp = new TableFieldDefinition(rs.getDataLong(0), rs.getData(1), this.getIdForObjectName(serverConnection, rs.getData(3), CodaServer.OBJECT_TYPE_TYPE), rs.getData(3), rs.getData(2), (rs.getDataInt(4) == 1), (rs.getData(6) != null && !rs.getData(6).equals("")), (rs.getData(6) != null && !rs.getData(6).equals("") ? rs.getDataLong(6) : -1), rs.getData(7) != null ? rs.getDataInt(7) : -1, rs.getData(8), rs.getData(9), rs.getDataDate(10), rs.getData(11), rs.getDataDate(12));
                temp.setNullableFlag(rs.getDataInt(5) == 1);
                retval.put(rs.getData(1), temp);
            }
        }
        return retval;
    }

    private CodaResponse alterColumn(CodaConnection connection, boolean formFlag, String applicationName, long tableId, String tableName, String fieldName, TableFieldDefinition field, int operation, long modUserId, String sessionKey, boolean loadClass) {
        
        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (operation == 1 || operation == 2) {
            long columnId = this.getIdForObjectName(Long.toString(tableId) + "." + field.getFieldName(), CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection);
            if ((operation == 1 && columnId > -1) || (operation == 2 && !fieldName.equalsIgnoreCase(field.getFieldName()) && columnId > -1)) {
                return new CodaResponse(true, null, 2037);
            }
            if (this.isSubtableName(database, prefix, tableId, field.getFieldName())) {
                return new CodaResponse(true, null, (formFlag ? 2066 : 2065));
            }
            long typeId = this.getIdForObjectName(this.database.getConnection(), field.getTypeName(), CodaServer.OBJECT_TYPE_TYPE);
            if (typeId < 0) {
                return new CodaResponse(true, null, 2020, "Type '" + field.getTypeName() + "' is invalid");
            }
            if (field.isRefFlag()) {
                if (this.getIdForObjectName(field.getRefTableName(), CodaServer.OBJECT_TYPE_TABLE, connection) < 0) {
                    return new CodaResponse(true, null, 2028, "Table '" + field.getRefTableName() + "' is invalid");
                }
            }
            if (field.getDefaultValue() != null || field.getDefaultVariableId() != -1) {
                // Get an execution context to test the values of the default parameters
                ExecutionContext context = new ExecutionContext(this, database);
                if (!field.isArrayFlag()) {
                    if (field.getDefaultVariableId() != -1) {
                        if (field.getDefaultVariableId() == CodaConstant.SYSVAR_CURRENT_TIMESTAMP && !field.getTypeName().equalsIgnoreCase("TIMESTAMP")) {
                            return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                        } else if (field.getDefaultVariableId() != CodaConstant.SYSVAR_CURRENT_TIMESTAMP && !field.getTypeName().equalsIgnoreCase("STRING") && !field.getTypeName().equalsIgnoreCase("LONGSTRING")) {
                            return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                        }
                    } else if (!context.validate(typeId, field.getDefaultValue())) {
                        return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                    }
                } else if (field.isArrayFlag()) {
                    if (field.getDefaultVariableId() != -1) {
                        return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for array column '" + field.getFieldName() + "'");
                    } else if (!TypeParser.isArray(field.getDefaultValue())) {
                        return new CodaResponse(true, null, 3001, "Default value for column '" + field.getFieldName() + "' should be an array");
                    } else {
                        Vector<String> tokens = TypeParser.explodeArray(field.getDefaultValue());
                        for (String token : tokens) {
                            if(!context.validate(typeId, token)) {
                                return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                            }
                        }
                    }
                }
            }

            if (operation == 1) {
                if (field.isArrayFlag()) {
					if (this.isArrayFieldNameInUse(connection, prefix, field.getFieldName().toUpperCase())) {
						return new CodaResponse(true, null, 2038, "Column '" + field.getFieldName() + "' must have a globally unique name");
					}
				}
				if (field.isArrayFlag()) {
                    ColumnDefinition[] cols = new ColumnDefinition[2];
                    cols[0] = new ColumnDefinition("id", Types.BIGINT, false);
                    cols[1] = new ColumnDefinition("value", CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), false);
                    try {
                        connection.createTable(field.getFieldName().toUpperCase(), new Vector(Arrays.asList(cols)));
                        connection.addColumn(tableName.toUpperCase(), new ColumnDefinition(field.getFieldName().toUpperCase(), Types.CLOB, false));
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }
                } else {
                    try {
                        connection.addColumn(tableName.toUpperCase(), new ColumnDefinition(field.getFieldName(), CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), field.isNullableFlag()));
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }
                }
                Hashtable values = new Hashtable();
                values.put("table_id", tableId);
                values.put("field_name", field.getFieldName().toUpperCase());
                values.put("display_name", field.getDisplayedAs() == null ? field.getFieldName() : field.getDisplayedAs());
                values.put("type_name", field.getTypeName().toUpperCase());
                values.put("array_flag", field.isArrayFlag() ? 1 : 0);
                values.put("nullable_flag", field.isNullableFlag() ? 1 : 0);
				values.put("built_in_flag", field.isBuiltInFlag() ? 1 : 0);
				if (field.isRefFlag()) {
                    values.put("ref_table_id", this.getIdForObjectName(field.getRefTableName(), CodaServer.OBJECT_TYPE_TABLE, connection));
                }
                if ( field.getDefaultVariableId() != -1) {
                    values.put("default_variable_id", field.getDefaultVariableId());
                }
                if (field.getDefaultValue() != null) {
                    values.put("default_value", field.getDefaultValue());
                }
                values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                values.put("create_date", new GregorianCalendar().getTimeInMillis());
                values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                connection.insertRow(prefix+ "table_fields", values);
            } else {
                long fieldId = this.getIdForObjectName(Long.toString(tableId) + "." + fieldName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection);
                if (fieldId < 0) {
                    return new CodaResponse(true, null, 2040);
                }
				if (this.isBuiltInField(connection, prefix, fieldId)) {
					return new CodaResponse(true, null, 2096);
				}
				boolean arrayField = this.isArrayField(connection, prefix, applicationName, fieldId);

                try {
                    if (field.isArrayFlag()) {
                        if (arrayField) {
                            if (!fieldName.equalsIgnoreCase(field.getFieldName())) {
                                if (this.isArrayFieldNameInUse(connection, prefix, field.getFieldName().toUpperCase())) {
									return new CodaResponse(true, null, 2038, "Column '" + field.getFieldName() + "' must have a globally unique name");
								}
								connection.alterColumn(tableName.toUpperCase(), fieldName.toUpperCase(), new ColumnDefinition(field.getFieldName().toUpperCase(), Types.CLOB, false));
                                connection.alterTable(fieldName.toUpperCase(), field.getFieldName().toUpperCase());
                            }
                            connection.alterColumn(field.getFieldName().toUpperCase(), "VALUE", new ColumnDefinition("VALUE", CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), false));
                        } else {
                            // alter the old column
                            connection.alterColumn(tableName.toUpperCase(), fieldName.toUpperCase(), new ColumnDefinition(field.getFieldName(), Types.CLOB, false));

                            // create the new table
                            ColumnDefinition[] cols = new ColumnDefinition[2];
                            cols[0] = new ColumnDefinition("ID", Types.BIGINT, false);
                            cols[1] = new ColumnDefinition("VALUE", CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), false);

                            connection.createTable(field.getFieldName(), new Vector(Arrays.asList(cols)));
                        }

                    } else {
                        if (arrayField) {
                            connection.dropTable(fieldName.toUpperCase());

                            connection.alterColumn(tableName.toUpperCase(), fieldName.toUpperCase(), new ColumnDefinition(field.getFieldName(), CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), field.isNullableFlag()));

                        } else {

                            connection.alterColumn(tableName.toUpperCase(), fieldName, new ColumnDefinition(field.getFieldName(), CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), field.isNullableFlag()));
                        }

                    }

                } catch (SQLException e) {
                    return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                }

                Hashtable values = new Hashtable();
                values.put("field_name", field.getFieldName().toUpperCase());
                values.put("display_name", field.getDisplayedAs() == null ? field.getFieldName() : field.getDisplayedAs());
                values.put("type_name", field.getTypeName().toUpperCase());
                values.put("nullable_flag", field.isNullableFlag() ? 1 : 0);
				values.put("built_in_flag", field.isBuiltInFlag() ? 1 : 0);
				values.put("array_flag", field.isArrayFlag() ? 1 : 0);
                if (field.isRefFlag()) {
                    values.put("ref_table_id", this.getIdForObjectName(field.getRefTableName(), CodaServer.OBJECT_TYPE_TABLE, connection));
                }
                if ( field.getDefaultVariableId() != -1) {
                    values.put("default_variable_id", field.getDefaultVariableId());
                } else {
                    connection.runStatement("update "+prefix + "table_fields set default_variable_id = null where id = " + fieldId);
                }
                if (field.getDefaultValue() != null) {
                    values.put("default_value", field.getDefaultValue());
                } else {
                    connection.runStatement("update "+prefix + "table_fields set default_value = null where id = " + fieldId);
                }
                values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                connection.updateRow(prefix + "table_fields", "id", fieldId, values);
            }
        } else {
            long fieldId = this.getIdForObjectName(Long.toString(tableId) + "." + fieldName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection);
            if (fieldId < 0) {
                return new CodaResponse(true, null, 2040);
            }
			if (this.isBuiltInField(connection, prefix, fieldId)) {
				return new CodaResponse(true, null, 2095);
			}

			boolean arrayField = this.isArrayField(connection, prefix, applicationName, fieldId);

            Vector<Hashtable> indexes = new Vector();
            CodaResultSet rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"index_fields f on f.index_id = i.id where f.table_field_id = " + connection.formatStringForSQL(prefix+"index_fields", "table_field_id", Long.toString(fieldId)), null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    Hashtable<String,String> temp = new Hashtable();
                    temp.put("id", rs.getData(0));
                    temp.put("index_name", rs.getData(1));
                    indexes.add(temp);
                }
            }

            try {

                for (Hashtable<String,String> h : indexes) {
                    connection.dropIndex(h.get("index_name"));
                }

                if(arrayField) {
                    connection.dropTable(fieldName.toUpperCase());
                    connection.dropColumn(tableName.toUpperCase(), fieldName.toUpperCase());
                } else {
                    connection.dropColumn(tableName.toUpperCase(), fieldName.toUpperCase());
                }

            } catch (SQLException e) {
                return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
            }

            for (Hashtable<String,String> h : indexes) {
                connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", h.get("id")));
                connection.deleteRow(prefix+"indexes", "id", Long.parseLong(h.get("id")));
            }

            connection.deleteRow(prefix+"table_fields", "id", fieldId);

        }

        try {
			CodaConnection serverConnection = this.database.getConnection();
			String classFile = GroovyClassGenerator.getTableClass(tableName, new Vector(this.getFieldsForTable(connection, prefix, tableId, serverConnection).values()));
            Hashtable values = new Hashtable();
            values.put("class_file", classFile);
            connection.updateRow(prefix+"tables", "id", tableId, values);

            if (loadClass) {
                this.deployedApplications.get(applicationName).loadClass(1, "org.codalang.codaserver.language.tables." + CodaServer.camelCapitalize(tableName, true),classFile);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Cannot create class for table '" + tableName + "'");
        }

        connection.commit();
        return new CodaResponse(false, "Success!", -1, null);
    }

    public synchronized CodaResponse createTable (String sessionKey, CodaDatabase database, String applicationName, String tableName, String displayName, boolean groupFlag, boolean refTableFlag, String parentTableName, boolean softDeleteFlag, Vector<TableFieldDefinition> fields, boolean loadClass) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            CodaConnection connection = database.getConnection();

            if (this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection) > 0) {
                return new CodaResponse(true, null, 2036);
            }

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            long parentTableId = -1;
            if (parentTableName != null) {
                parentTableId = this.getIdForObjectName(parentTableName, CodaServer.OBJECT_TYPE_TABLE, connection);
                if(parentTableId < 0) {
                    return new CodaResponse(true, null, 2028, "Table '" + parentTableName + "' is invalid");
                }
                if (this.getIdForObjectName(Long.toString(parentTableId) + "." + tableName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection) > 0) {
                    return new CodaResponse(true, null, 2063);
                }
                if(this.isForm(connection, prefix, parentTableName)) {
                    return new CodaResponse(true, null, 2045);
                }
            }

            Vector<String> fieldNames = new Vector();
            Vector<String> identityFieldNames = new Vector();
            Vector<Long> identityFieldIds = new Vector();

            // Get an execution context to test the default values
            ExecutionContext context = new ExecutionContext(this, database);

            // get reserved column names
            Vector<String> reservedColumns = this.getReservedColumnNames();
            for (TableFieldDefinition field : fields) {
                if (reservedColumns.contains(field.getFieldName().toUpperCase())) {
                    return new CodaResponse(true, null, 2075, "The column name '" + field.getFieldName() + "' is reserved");
                }
                if (fieldNames.contains(field.getFieldName().toUpperCase())) {
                    return new CodaResponse(true, null, 2037, "Column '" + field.getFieldName() + "' defined multiple times");
                } else {
                    fieldNames.add(field.getFieldName().toUpperCase());
                }
                long typeId = this.getIdForObjectName(this.database.getConnection(), field.getTypeName().toUpperCase(), CodaServer.OBJECT_TYPE_TYPE);
                if (typeId < 0) {
                    return new CodaResponse(true, null, 2020, "Type '" + field.getTypeName() + "' is invalid");
                }
                if (field.isRefFlag()) {
                    if (this.getIdForObjectName(field.getRefTableName().toUpperCase(), CodaServer.OBJECT_TYPE_TABLE, connection) < 0) {
                        return new CodaResponse(true, null, 2028, "Table/form '" + field.getRefTableName() + "' is invalid");
                    }
                }
                if (field.isArrayFlag()) {
                    if (this.isArrayFieldNameInUse(connection, prefix, field.getFieldName().toUpperCase())) {
                        return new CodaResponse(true, null, 2038, "Column '" + field.getFieldName() + "' must have a globally unique name");
                    }
                }
                if (field.getDefaultValue() != null || field.getDefaultVariableId() != -1) {
                    if (!field.isArrayFlag()) {
                        if (field.getDefaultVariableId() != -1) {
                            if (field.getDefaultVariableId() == CodaConstant.SYSVAR_CURRENT_TIMESTAMP && !field.getTypeName().equalsIgnoreCase("TIMESTAMP")) {
                                return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                            } else if (field.getDefaultVariableId() != CodaConstant.SYSVAR_CURRENT_TIMESTAMP && !field.getTypeName().equalsIgnoreCase("STRING") && !field.getTypeName().equalsIgnoreCase("LONGSTRING")) {
                                return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                            }
                        } else if (!context.validate(typeId, field.getDefaultValue())) {
                            return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                        }
                    } else if (field.isArrayFlag()) {
                        if (field.getDefaultVariableId() != -1) {
                            return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for array column '" + field.getFieldName() + "'");
                        } else if (!TypeParser.isArray(field.getDefaultValue())) {
                            return new CodaResponse(true, null, 3001, "Default value for column '" + field.getFieldName() + "' should be an array");
                        } else {
                            Vector<String> tokens = TypeParser.explodeArray(field.getDefaultValue());
                            for (String token : tokens) {
                                if(!context.validate(typeId, token)) {
                                    return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                                }
                            }
                        }
                    }
                }

                if (field.isIdentityFlag()) {
                    identityFieldNames.add(field.getFieldName());
                }
            }

            if (displayName == null) {
                displayName = tableName;
            }

            // Insert table
            try {
				// add the built-in columns to the columnDefinitions and fields vectors
				if (softDeleteFlag) {
                    fields.add(new TableFieldDefinition("ACTIVE_FLAG", "INTEGER", "Active Flag", false, false, null, null, true));
				}
                if (groupFlag) {
                    fields.add(new TableFieldDefinition("GROUP_NAME", "STRING", "Group Name", false, false, null, null, true));
				}
                fields.add(new TableFieldDefinition("ID", "INTEGER", "ID", false, false, null, null, true));
				fields.add(new TableFieldDefinition("CREATE_USER_NAME", "STRING", "Creating Username", false, false, null, null, true));
				fields.add(new TableFieldDefinition("CREATE_DATE", "TIMESTAMP", "Created Date", false, false, null, null, true));
				fields.add(new TableFieldDefinition("MOD_USER_NAME", "STRING", "Last Modifying Username", false, false, null, null, true));
                fields.add(new TableFieldDefinition("MOD_DATE", "TIMESTAMP", "Modified Date", false, false, null, null, true));
	            if (parentTableId > 0) {
                    fields.add(new TableFieldDefinition("PARENT_TABLE_ID", "INTEGER", "Parent Table ID", false, false, null, null, true));
				}

				String classFile = GroovyClassGenerator.getTableClass(tableName, fields);
                Hashtable values = new Hashtable();
                values.put("table_name", tableName.toUpperCase());
                values.put("display_name", displayName);
                values.put("group_flag", groupFlag ? 1 : 0);
                values.put("form_flag", 0);
                values.put("class_file", classFile);
                values.put("ref_table_flag", refTableFlag ? 1 : 0);
                if (parentTableId >= 0) {
                    values.put("parent_table_id", parentTableId);
                }
                values.put("soft_delete_flag", softDeleteFlag ? 1 : 0);
                values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                values.put("create_date", new GregorianCalendar().getTimeInMillis());
                values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                long tableId = connection.insertRow(prefix+"tables", values);

                //load the class if needed
                if (loadClass) {
                    this.deployedApplications.get(applicationName).loadClass(1, "org.codalang.codaserver.language.tables." + CodaServer.camelCapitalize(tableName, true),classFile);
                }

                ArrayList<ColumnDefinition> columnDefinitions = new ArrayList();
                ArrayList<CodaTable> arrayTables = new ArrayList();

                for (TableFieldDefinition field : fields) {
                    values = new Hashtable();
                    values.put("table_id", tableId);
                    values.put("field_name", field.getFieldName().toUpperCase());
                    values.put("display_name", field.getDisplayedAs() == null ? field.getFieldName() : field.getDisplayedAs());
                    values.put("type_name", field.getTypeName().toUpperCase());
                    values.put("array_flag", field.isArrayFlag() ? 1 : 0);
					values.put("nullable_flag", field.isNullableFlag() ? 1 : 0);
					values.put("built_in_flag", field.isBuiltInFlag() ? 1 : 0);
					if (field.isRefFlag()) {
                        values.put("ref_table_id", this.getIdForObjectName(field.getRefTableName(), CodaServer.OBJECT_TYPE_TABLE, connection));
                    }
					if (field.getDefaultVariableId() != -1) {
						values.put("default_variable_id", field.getDefaultVariableId());
					}
					if (field.getDefaultValue() != null) {
                        values.put("default_value", field.getDefaultValue());
                    }
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    long fieldId = connection.insertRow(prefix+"table_fields", values);

                    if (identityFieldNames.contains(field.getFieldName())) {
                        identityFieldIds.add(fieldId);
                    }

                    if (field.isArrayFlag()) {
                        ColumnDefinition[] cols = new ColumnDefinition[2];
                        cols[0] = new ColumnDefinition("id", Types.BIGINT, false);
                        cols[1] = new ColumnDefinition("value", CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), false);
                        arrayTables.add(new CodaTable(field.getFieldName(), cols));

                        columnDefinitions.add(new ColumnDefinition(field.getFieldName().toUpperCase(), Types.CLOB, false));
                    } else {
                        columnDefinitions.add(new ColumnDefinition(field.getFieldName().toUpperCase(), CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), field.isNullableFlag()));
                    }
                }
                try {
                    connection.createTable(tableName, new Vector(columnDefinitions));
                    connection.setPrimaryKey(tableName, "ID");

                    for (CodaTable table : arrayTables) {
                        connection.createTable(table.getTableName(), new Vector(Arrays.asList(table.getColumnDefinitions())));
                    }
                } catch (SQLException e) {
                    connection.deleteRow("tables", "id", tableId);
                    return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                }

                if (identityFieldNames.size() > 0) {
                    try {
                        connection.setIdentityColumns("ID_" + tableName.toUpperCase(), tableName, identityFieldNames);
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }

                    values = new Hashtable();
                    values.put("index_name", "ID_" + tableName.toUpperCase());
                    values.put("table_id", tableId);
                    values.put("index_type_id", 2);
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    long indexId = connection.insertRow(prefix+"indexes", values);

                    for (Long fieldId : identityFieldIds) {
                        values = new Hashtable();
                        values.put("index_id", indexId);
                        values.put("table_field_id", fieldId);
                        connection.insertRow(prefix+"index_fields", values);
                    }

                }

                connection.commit();
                return new CodaResponse(false, "Success!", -1, null);
            } catch (IOException e) {
                return new CodaResponse(true, null, 2085);
            }
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public synchronized CodaResponse alterTable (String sessionKey, CodaDatabase database, String applicationName, String tableName, int operation, String newTableName, String displayName, TableFieldDefinition field, String fieldName, Vector<String> identityFields, boolean loadClass) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        boolean formFlag = this.isForm(connection, prefix, tableName);
        if (formFlag) {
            return new CodaResponse(true, null, 2039);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {

            long tableId = this.getIdForObjectName(tableName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }

            Hashtable values = new Hashtable();
            switch (operation) {
                case 1:
                    values = new Hashtable();
                    values.put("display_name", displayName);
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"tables", "id", tableId, values);
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 2:
                    if (this.getIdForObjectName(newTableName, CodaServer.OBJECT_TYPE_TABLE, connection) > 0) {
                        return new CodaResponse(true, null, 2036);
                    }
                    try {
                        connection.alterTable(tableName.toUpperCase(), newTableName.toUpperCase());
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }
                    values = new Hashtable();
                    values.put("table_name", newTableName.toUpperCase());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"tables", "id", tableId, values);

                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 3:
                    //add column
                    return this.alterColumn(connection, formFlag, applicationName, tableId, tableName, fieldName, field, 1 , modUserId, sessionKey, loadClass);
                case 4:
                    //alter column
                    return this.alterColumn(connection, formFlag, applicationName, tableId, tableName, fieldName, field, 2 , modUserId, sessionKey, loadClass);
                case 5:
                    //drop column
                    return this.alterColumn(connection, formFlag, applicationName, tableId, tableName, fieldName, null, 3, modUserId, sessionKey, loadClass);
                case 6:
                    //set identity
                    long indexId = this.getIdForObjectName("ID_" + tableName.toUpperCase(), CodaServer.OBJECT_TYPE_INDEX, connection);
                    if (indexId > 0 ) {
                        try {
                            connection.dropIndex("ID_" + tableName.toUpperCase());
                        } catch (SQLException e) {
                            return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                        }
                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", Long.toString(indexId)));
                        connection.deleteRow(prefix+"indexes", "id", indexId);
                    }

                    Vector<Long> identityFieldIds = new Vector();
                    for (String idFieldName : identityFields) {
                        long tempFieldId = this.getIdForObjectName(Long.toString(tableId) + "." + idFieldName, CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection);
                        if (tempFieldId < 0) {
                            return new CodaResponse(true, null, 2040, "Column name '" + idFieldName + "' not valid");
                        } else {
                            identityFieldIds.add(tempFieldId);
                        }
                    }

                    try {
                        connection.setIdentityColumns("ID_" + tableName.toUpperCase(), tableName, identityFields);
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }

                    values = new Hashtable();
                    values.put("index_name", "ID_" + tableName.toUpperCase());
                    values.put("table_id", tableId);
                    values.put("index_type_id", 2);
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    indexId = connection.insertRow(prefix+"indexes", values);

                    for (Long tempFieldId : identityFieldIds) {
                        values = new Hashtable();
                        values.put("index_id", indexId);
                        values.put("table_field_id", tempFieldId);
                        connection.insertRow(prefix+"index_fields", values);
                    }

                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
            }

            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public synchronized CodaResponse dropTable (String sessionKey, CodaDatabase database, String applicationName, String tableName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (this.isForm(connection, prefix, tableName)) {
            return new CodaResponse(true, null, 2039);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            long tableId = this.getIdForObjectName(tableName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }

            try {
                // Remove all indexes on reference columns on other tables that point to subtables of this table.  Phew.
                CodaResultSet rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"index_fields inf on inf.index_id = i.id inner join "+prefix+"table_fields tf on tf.id = inf.table_field_id inner join "+prefix+"tables t on tf.ref_table_id = t.id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                } else {
                    return new CodaResponse(true, null, 8002);
                }

                // Remove all reference columns to subtables of this table
                rs = connection.runQuery("select f.id, f.field_name, f.array_flag, t.table_name from "+prefix+"table_fields f inner join "+prefix+"tables t on t.id = f.ref_table_id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        if (rs.getDataInt(2) == 1) {
                            connection.dropTable(rs.getData(1));
                        }
                        connection.dropColumn(rs.getData(3), rs.getData(1));
                        connection.deleteRow(prefix+"table_fields", "id", rs.getDataLong(0));

                    }

                }

                // Remove all indexes on reference columns on other tables.  Phew.
                rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"index_fields inf on inf.index_id = i.id inner join "+prefix+"table_fields tf on tf.id = inf.table_field_id where tf.ref_table_id = " + connection.formatStringForSQL(prefix+"table_fields", "ref_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                } else {
                    return new CodaResponse(true, null, 8002);
                }

                // Remove all reference columns to this table
                rs = connection.runQuery("select f.id, f.field_name, f.array_flag, t.table_name from "+prefix+"table_fields f where f.ref_table_id = " + connection.formatStringForSQL(prefix+"table_fields", "ref_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        if (rs.getDataInt(2) == 1) {
                            connection.dropTable(rs.getData(1));
                        }
                        connection.dropColumn(rs.getData(3), rs.getData(1));
                        connection.deleteRow(prefix+"table_fields", "id", rs.getDataLong(0));

                    }

                }

                // Remove all indexes on subtables to this table.
                rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"tables t on t.id = i.table_id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                }

                // Remove all array columns from subtables to this query
                rs = connection.runQuery("select f.field_name from "+prefix+"table_fields f inner join "+prefix+"tables t on t.id = f.table_id where f.array_flag = 1 and  t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropTable(rs.getData(0));
                    }

                }

                // Remove all subtables to this table
                rs = connection.runQuery("select t.id, t.table_name from "+prefix+"tables t where parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropTable(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"table_fields where table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", rs.getData(0)));
                        connection.deleteRow(prefix+"tables", "id", rs.getDataLong(0));

                    }
                }

                // Remove all array columns for this table
                rs = connection.runQuery("select f.field_name from "+prefix+"table_fields f  where f.array_flag = 1 and f.table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropTable(rs.getData(0));
                    }

                }


                // Remove the indexes for this table
                rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i where i.table_id = " + connection.formatStringForSQL(prefix+"indexes", "table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                }

                // Remove all triggers for this table
                connection.runStatement("delete from "+prefix+"triggers where table_id = " + connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)));
                /**
                 TODO: reload triggers
                 */

                // Remove all permissions for this table
                connection.runStatement("delete from "+prefix+"role_tables where table_id = " + connection.formatStringForSQL(prefix+"role_tables", "table_id", Long.toString(tableId)));

                // Remove the table itself
                connection.dropTable(tableName);

                connection.runStatement("delete from "+prefix+"table_fields where table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", Long.toString(tableId)));
                connection.deleteRow(prefix+"tables", "id", tableId);

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
            }

            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }


    }

    public synchronized CodaResponse createForm (String sessionKey, CodaDatabase database, String applicationName, String tableName, String displayName, boolean groupFlag, String parentTableName, Vector<TableFieldDefinition> fields, Vector<FormStatusDefinition> statuses, boolean loadClass) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag){
            if (this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection) > 0) {
                return new CodaResponse(true, null, 2042);
            }

            long parentTableId = -1;
            if (parentTableName != null) {
                parentTableId = this.getIdForObjectName(parentTableName, CodaServer.OBJECT_TYPE_TABLE, connection);
                if(parentTableId < 0) {
                    return new CodaResponse(true, null, 2041, "Form '" + parentTableName + "' is invalid");
                }
                if (this.getIdForObjectName(Long.toString(parentTableId) + "." + tableName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection) > 0) {
                    return new CodaResponse(true, null, 2064);
                }
                if (!this.isForm(connection, prefix, parentTableName)) {
                    return new CodaResponse(true, null, 2044);
                }
            }

            Vector<String> fieldNames = new Vector();
            Vector<String> identityFieldNames = new Vector();
            Vector<Long> identityFieldIds = new Vector();

            // Get an execution context to test the default values
            ExecutionContext context = new ExecutionContext(this, database);

            Vector<String> reservedColumns = this.getReservedColumnNames();
            for (TableFieldDefinition field : fields) {
                if (reservedColumns.contains(field.getFieldName().toUpperCase())) {
                    return new CodaResponse(true, null, 2075, "The field name '" + field.getFieldName() + "' is reserved");
                }
                if (fieldNames.contains(field.getFieldName().toUpperCase())) {
                    return new CodaResponse(true, null, 2043, "Field '" + field.getFieldName() + "' defined multiple times");
                } else {
                    fieldNames.add(field.getFieldName().toUpperCase());
                }
                long typeId = this.getIdForObjectName(this.database.getConnection(), field.getTypeName(), CodaServer.OBJECT_TYPE_TYPE);
                if (typeId < 0) {
                    return new CodaResponse(true, null, 2020, "Type '" + field.getTypeName() + "' is invalid");
                }
                if (field.isRefFlag()) {
                    if (this.getIdForObjectName(field.getRefTableName(), CodaServer.OBJECT_TYPE_TABLE, connection) < 0) {
                        return new CodaResponse(true, null, 2028, "Table/form '" + field.getRefTableName() + "' is invalid");
                    }
                }
                if (field.isArrayFlag()) {
                    if (this.isArrayFieldNameInUse(connection, prefix, field.getFieldName().toUpperCase())) {
                        return new CodaResponse(true, null, 2046, "Field '" + field.getFieldName() + "' must have a globally unique name");
                    }
                }
                if (field.getDefaultValue() != null || field.getDefaultVariableId() != -1) {
                    if (!field.isArrayFlag()) {
                        if (field.getDefaultVariableId() != -1) {
                            if (field.getDefaultVariableId() == CodaConstant.SYSVAR_CURRENT_TIMESTAMP && !field.getTypeName().equalsIgnoreCase("TIMESTAMP")) {
                                return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                            } else if (field.getDefaultVariableId() != CodaConstant.SYSVAR_CURRENT_TIMESTAMP && !field.getTypeName().equalsIgnoreCase("STRING") && !field.getTypeName().equalsIgnoreCase("LONGSTRING")) {
                                return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                            }
                        } else if (!context.validate(typeId, field.getDefaultValue())) {
                            return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                        }
                    } else if (field.isArrayFlag()) {
                        if (field.getDefaultVariableId() != -1) {
                            return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for array column '" + field.getFieldName() + "'");
                        } else if (!TypeParser.isArray(field.getDefaultValue())) {
                            return new CodaResponse(true, null, 3001, "Default value for column '" + field.getFieldName() + "' should be an array");
                        } else {
                            Vector<String> tokens = TypeParser.explodeArray(field.getDefaultValue());
                            for (String token : tokens) {
                                if(!context.validate(typeId, token)) {
                                    return new CodaResponse(true, null, 3001, "Default value '" + field.getDefaultValue() + "' does not match the correct type for column '" + field.getFieldName() + "'");
                                }
                            }
                        }
                    }
                }
                if (field.isIdentityFlag()) {
                    identityFieldNames.add(field.getFieldName());
                }
            }

            Vector<String> formStatusAdjs = new Vector();
            Vector<String> formStatusVerbs = new Vector();
            for(FormStatusDefinition status : statuses) {
                if (status.getFormStatusVerb().equalsIgnoreCase("UPDATE") || status.getFormStatusVerb().equalsIgnoreCase("INSERT") || status.getFormStatusVerb().equalsIgnoreCase("DELETE")) {
                    return new CodaResponse(true, null, 2072);
                }
                if (formStatusAdjs.contains(status.getFormStatusAdj().toUpperCase())) {
                    return new CodaResponse(true, null, 2047, "Form status adjective '" + status.getFormStatusAdj() + "' defined multiple times");
                } else {
                    formStatusAdjs.add(status.getFormStatusAdj().toUpperCase());
                }
                if (formStatusVerbs.contains(status.getFormStatusVerb().toUpperCase())) {
                    return new CodaResponse(true, null, 2048, "Form status verb '" + status.getFormStatusVerb() + "' defined multiple times");
                } else {
                    formStatusVerbs.add(status.getFormStatusVerb().toUpperCase());
                }
            }

            if (displayName == null) {
                displayName = tableName;
            }

            // Insert table
            try {
				// add the built-in columns to the columnDefinitions and fields vectors
				fields.add(new TableFieldDefinition("ID", "INTEGER", "ID", false, false, null, null, true));
                fields.add(new TableFieldDefinition("CREATE_USER_NAME", "STRING", "Creating Username", false, false, null, null, true));
				fields.add(new TableFieldDefinition("CREATE_DATE", "TIMESTAMP", "Created Date", false, false, null, null, true));
				fields.add(new TableFieldDefinition("MOD_USER_NAME", "STRING", "Last Modifying Username", false, false, null, null, true));
				fields.add(new TableFieldDefinition("MOD_DATE", "TIMESTAMP", "Modified Date", false, false, null, null, true));
				fields.add(new TableFieldDefinition("STATUS_ID", "INTEGER", "Status ID", false, false, null, null, true));
				if (parentTableId > 0) {
					fields.add(new TableFieldDefinition("PARENT_TABLE_ID", "INTEGER", "Parent Table ID", false, false, null, null, true));
				}
				if (groupFlag) {
					fields.add(new TableFieldDefinition("GROUP_NAME", "STRING", "Group Name", false, false, null, null, true));
				}

				String classFile = GroovyClassGenerator.getTableClass(tableName, fields);
                Hashtable values = new Hashtable();
                values.put("table_name", tableName.toUpperCase());
                values.put("display_name", displayName);
                values.put("group_flag", groupFlag ? 1 : 0);
                values.put("form_flag", 1);
				values.put("class_file", classFile);
				values.put("soft_delete_flag", 0);
				values.put("ref_table_flag", 0);
                if (parentTableId >=0) {
                    values.put("parent_table_id", parentTableId);
                }
                values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                values.put("create_date", new GregorianCalendar().getTimeInMillis());
                values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                long tableId = connection.insertRow(prefix+"tables", values);

                //load the class if needed
                if (loadClass) {
                    this.deployedApplications.get(applicationName).loadClass(1, "org.codalang.codaserver.language.tables." + CodaServer.camelCapitalize(tableName, true),classFile);
                }

                ArrayList<ColumnDefinition> columnDefinitions = new ArrayList();
                ArrayList<CodaTable> arrayTables = new ArrayList();

				for (TableFieldDefinition field : fields) {
                    values = new Hashtable();
                    values.put("table_id", tableId);
                    values.put("field_name", field.getFieldName().toUpperCase());
                    values.put("display_name", field.getDisplayedAs() == null ? field.getFieldName() : field.getDisplayedAs());
                    values.put("type_name", field.getTypeName().toString().toUpperCase());
                    values.put("array_flag", field.isArrayFlag() ? 1 : 0);
                    values.put("nullable_flag", field.isNullableFlag() ? 1 : 0);
					values.put("built_in_flag", field.isBuiltInFlag() ? 1 : 0);
					if (field.isRefFlag()) {
                        values.put("ref_table_id", this.getIdForObjectName(field.getRefTableName(), CodaServer.OBJECT_TYPE_TABLE, connection));
                    }
                    if (field.getDefaultVariableId() != -1) {
						values.put("default_variable_id", field.getDefaultVariableId());
					}
					if (field.getDefaultValue() != null) {
                        values.put("default_value", field.getDefaultValue());
                    }
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    long fieldId = connection.insertRow(prefix+"table_fields", values);

                    if (identityFieldNames.contains(field.getFieldName())) {
                        identityFieldIds.add(fieldId);
                    }

                    if (field.isArrayFlag()) {
                        ColumnDefinition[] cols = new ColumnDefinition[2];
                        cols[0] = new ColumnDefinition("id", Types.BIGINT, false);
                        cols[1] = new ColumnDefinition("value", CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), false);
                        arrayTables.add(new CodaTable(field.getFieldName(), cols));

                        columnDefinitions.add(new ColumnDefinition(field.getFieldName().toUpperCase(), Types.CLOB, false));
                    } else {
                        columnDefinitions.add(new ColumnDefinition(field.getFieldName().toUpperCase(), CodaTypeConverter.getSQLTypeFromCodaType(field.getTypeName()), field.isNullableFlag()));
                    }
                }

                for (FormStatusDefinition status : statuses) {
                    values = new Hashtable();
                    values.put("table_id", tableId);
                    values.put("adj_status_name", status.getFormStatusAdj().toUpperCase());
                    values.put("adj_display_name", (status.getFormStatusAdjDisplayName() == null ? status.getFormStatusAdj() : status.getFormStatusAdjDisplayName()));
                    values.put("verb_status_name", status.getFormStatusVerb().toUpperCase());
                    values.put("verb_display_name", (status.getFormStatusVerbDisplayName() == null ? status.getFormStatusVerb() : status.getFormStatusVerbDisplayName()));
                    values.put("initial_flag", (status.isInitialFlag() ? "1" : "0"));
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.insertRow(prefix+"form_statuses", values);
                }

                try {
                    connection.createTable(tableName, new Vector(columnDefinitions));
                    connection.setPrimaryKey(tableName, "ID");

                    for (CodaTable table : arrayTables) {
                        connection.createTable(table.getTableName(), new Vector(Arrays.asList(table.getColumnDefinitions())));
                    }
                } catch (SQLException e) {
                    connection.deleteRow(prefix+"tables", "id", tableId);
                    return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                }

                if (identityFieldNames.size() > 0) {
                    try {
                        connection.setIdentityColumns("ID_" + tableName.toUpperCase(), tableName, identityFieldNames);
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }

                    values = new Hashtable();
                    values.put("index_name", "ID_" + tableName.toUpperCase());
                    values.put("table_id", tableId);
                    values.put("index_type_id", 2);
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    long indexId = connection.insertRow(prefix+"indexes", values);

                    for (Long fieldId : identityFieldIds) {
                        values = new Hashtable();
                        values.put("index_id", indexId);
                        values.put("table_field_id", fieldId);
                        connection.insertRow(prefix+"index_fields", values);
                    }

                }

                connection.commit();
                return new CodaResponse(false, "Success!", -1, null);
            } catch (IOException e) {
                return new CodaResponse(true, null, 2085);
            }
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public synchronized CodaResponse alterForm (String sessionKey, CodaDatabase database, String applicationName, String tableName, int operation, String newTableName, String displayName, TableFieldDefinition field, String fieldName, Vector<String> identityFields, FormStatusDefinition status, String statusName, Vector<FormStatusLeadsTo> statuses, boolean loadClass) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        boolean formFlag = this.isForm(connection, prefix, tableName);
        if (!formFlag) {
            return new CodaResponse(true, null, 2092);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }

            Hashtable values = new Hashtable();
            switch (operation) {
                case 1:
                    values = new Hashtable();
                    values.put("display_name", displayName);
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"tables", "id", tableId, values);
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 2:
                    if (this.getIdForObjectName(newTableName, CodaServer.OBJECT_TYPE_TABLE, connection) > 0) {
                        return new CodaResponse(true, null, 2036);
                    }
                    try {
                        connection.alterTable(tableName.toUpperCase(), newTableName.toUpperCase());
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }
                    values = new Hashtable();
                    values.put("table_name", newTableName.toUpperCase());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"tables", "id", tableId, values);

                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 3:
                    //add column
                    return this.alterColumn(connection, formFlag, applicationName, tableId, tableName, fieldName, field, 1 , modUserId, sessionKey, loadClass);
                case 4:
                    //alter column
                    return this.alterColumn(connection, formFlag, applicationName, tableId, tableName, fieldName, field, 2 , modUserId, sessionKey, loadClass);
                case 5:
                    //drop column
                    return this.alterColumn(connection, formFlag, applicationName, tableId, tableName, fieldName, null, 3, modUserId, sessionKey, loadClass);
                case 6:
                    //set identity
                    long indexId = this.getIdForObjectName("ID_" + tableName.toUpperCase(), CodaServer.OBJECT_TYPE_INDEX, connection);
                    if (indexId > 0 ) {
                        try {
                            connection.dropIndex("ID_" + tableName.toUpperCase());
                        } catch (SQLException e) {
                            return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                        }
                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", Long.toString(indexId)));
                        connection.deleteRow(prefix+"indexes", "id", indexId);
                    }

                    Vector<Long> identityFieldIds = new Vector();
                    for (String idFieldName : identityFields) {
                        long tempFieldId = this.getIdForObjectName(Long.toString(tableId) + "." + idFieldName, CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection);
                        if (tempFieldId < 0) {
                            return new CodaResponse(true, null, 2040, "Column name '" + idFieldName + "' not valid");
                        } else {
                            identityFieldIds.add(tempFieldId);
                        }
                    }

                    try {
                        connection.setIdentityColumns("ID_" + tableName.toUpperCase(), tableName, identityFields);
                    } catch (SQLException e) {
                        return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
                    }

                    values = new Hashtable();
                    values.put("index_name", "ID_" + tableName.toUpperCase());
                    values.put("table_id", tableId);
                    values.put("index_type_id", 2);
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    indexId = connection.insertRow(prefix+"indexes", values);

                    for (Long tempFieldId : identityFieldIds) {
                        values = new Hashtable();
                        values.put("index_id", indexId);
                        values.put("table_field_id", tempFieldId);
                        connection.insertRow(prefix+"index_fields", values);
                    }
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 7:
                    //add status
                    if (status.getFormStatusVerb().equalsIgnoreCase("UPDATE") || status.getFormStatusVerb().equalsIgnoreCase("INSERT") || status.getFormStatusVerb().equalsIgnoreCase("DELETE")) {
                        return new CodaResponse(true, null, 2072);
                    }
                    if (this.getIdForObjectName(Long.toString(tableId) + "." + status.getFormStatusAdj().toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection) > 0) {
                        return new CodaResponse(true, null, 2049);
                    }
                    if (this.getIdForObjectName(Long.toString(tableId) + "." + status.getFormStatusVerb().toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_VERB, connection) > 0) {
                        return new CodaResponse(true, null, 2050);
                    }
                    values = new Hashtable();
                    values.put("table_id", tableId);
                    values.put("adj_status_name", status.getFormStatusAdj().toUpperCase());
                    values.put("adj_display_name", (status.getFormStatusAdjDisplayName() == null ? status.getFormStatusAdj() : status.getFormStatusAdjDisplayName()));
                    values.put("verb_status_name", status.getFormStatusVerb().toUpperCase());
                    values.put("verb_display_name", (status.getFormStatusVerbDisplayName() == null ? status.getFormStatusVerb() : status.getFormStatusVerbDisplayName()));
                    values.put("initial_flag", (status.isInitialFlag() ? 1 : 0));
                    values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("create_date", new GregorianCalendar().getTimeInMillis());
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.insertRow(prefix+"form_statuses", values);
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 8:
                    // alter status
                    long formStatusId = this.getIdForObjectName(Long.toString(tableId) + "." + statusName.toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
                    if (formStatusId < 0) {
                        return new CodaResponse(true, null, 2051);
                    }
                    if (status.getFormStatusVerb().equalsIgnoreCase("UPDATE") || status.getFormStatusVerb().equalsIgnoreCase("INSERT") || status.getFormStatusVerb().equalsIgnoreCase("DELETE")) {
                        return new CodaResponse(true, null, 2072);
                    }
                    long formStatusAdj = this.getIdForObjectName(Long.toString(tableId) + "." + status.getFormStatusAdj().toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
					if (formStatusAdj > 0 && formStatusAdj != formStatusId) {
                        return new CodaResponse(true, null, 2049);
                    }
					long formStatusVerb = this.getIdForObjectName(Long.toString(tableId) + "." + status.getFormStatusVerb().toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_VERB, connection);
					if (formStatusVerb > 0 && formStatusVerb != formStatusId) {
                        return new CodaResponse(true, null, 2050);
                    }
                    values = new Hashtable();
                    values.put("adj_status_name", status.getFormStatusAdj().toUpperCase());
                    values.put("adj_display_name", (status.getFormStatusAdjDisplayName() == null ? status.getFormStatusAdj() : status.getFormStatusAdjDisplayName()));
                    values.put("verb_status_name", status.getFormStatusVerb().toUpperCase());
                    values.put("verb_display_name", (status.getFormStatusVerbDisplayName() == null ? status.getFormStatusVerb() : status.getFormStatusVerbDisplayName()));
                    values.put("initial_flag", (status.isInitialFlag() ? 1 : 0));
                    values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
                    values.put("mod_date", new GregorianCalendar().getTimeInMillis());
                    connection.updateRow(prefix+"form_statuses","id", formStatusId, values);
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
                case 9:
                    // drop status
                    formStatusId = this.getIdForObjectName(Long.toString(tableId) + "." + statusName.toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
                    if (formStatusId < 0) {
                        return new CodaResponse(true, null, 2051);
                    }
                    CodaResultSet rs = connection.runQuery("select count(*) from " + tableName + " where status_id = " + connection.formatStringForSQL(tableName, "status_id", Long.toString(formStatusId)), null);
                    if (!rs.getErrorStatus() && rs.next()) {
                        if (rs.getDataLong(0) > 0) {
                            return new CodaResponse(true, null, 2052);
                        } else {
                            connection.runStatement("delete from "+prefix+"triggers where form_status_id = " + connection.formatStringForSQL(prefix+"triggers", "form_status_id", Long.toString(formStatusId)));
                            connection.deleteRow(prefix+"form_statuses", "id", formStatusId);
                            connection.commit();
                            return new CodaResponse(false, "Success!", -1, null);
                        }
                    } else {
                        return new CodaResponse(true, null, 8002);
                    }
                case 10:
                    Hashtable<String,Long> formStatusNames = new Hashtable();
                    for (FormStatusLeadsTo statusLead : statuses) {
                        if (!formStatusNames.containsKey(statusLead.getStatusName().toUpperCase())) {
                            long tempStatusId = this.getIdForObjectName(tableId + "." + statusLead.getStatusName().toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
                            if (tempStatusId > 0) {
                                formStatusNames.put(statusLead.getStatusName().toUpperCase(), tempStatusId);
                            } else {
                                return new CodaResponse(true, null, 2051, "Form status '" + statusLead.getStatusName() + "' is invalid");
                            }
                            if (statusLead.getLeadsToList() != null) {
                                for (String leadsTo : statusLead.getLeadsToList()) {
                                    if (!formStatusNames.containsKey(leadsTo.toUpperCase())) {
                                        tempStatusId = this.getIdForObjectName(tableId + "." + leadsTo.toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
                                        if (tempStatusId > 0) {
                                            formStatusNames.put(leadsTo.toUpperCase(), tempStatusId);
                                        } else {
                                            return new CodaResponse(true, null, 2051, "Form status '" + leadsTo + "' is invalid");
                                        }
                                    }
                                }
                            }
                        } else {
							if (statusLead.getLeadsToList() != null) {
                                for (String leadsTo : statusLead.getLeadsToList()) {
                                    if (!formStatusNames.containsKey(leadsTo.toUpperCase())) {
                                        long tempStatusId = this.getIdForObjectName(tableId + "." + leadsTo.toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_ADJ, connection);
                                        if (tempStatusId > 0) {
                                            formStatusNames.put(leadsTo.toUpperCase(), tempStatusId);
                                        } else {
                                            return new CodaResponse(true, null, 2051, "Form status '" + leadsTo + "' is invalid");
                                        }
                                    }
                                }
                            }
						}
                    }
                    for (FormStatusLeadsTo statusLead : statuses) {
                        connection.runStatement("delete from "+prefix+"form_status_relationships where form_status_id = " + connection.formatStringForSQL(prefix+"form_status_relationships", "form_status_id", Long.toString(formStatusNames.get(statusLead.getStatusName().toUpperCase()))));
                        if (statusLead.getLeadsToList() != null && statusLead.getLeadsToList().size() > 0) {
                            values = new Hashtable();
                            values.put("form_status_id", formStatusNames.get(statusLead.getStatusName().toUpperCase()).longValue());
                            values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                            values.put("create_date", new GregorianCalendar().getTimeInMillis());
                            for(String leadsTo : statusLead.getLeadsToList()) {
                                values.put("next_form_status_id", formStatusNames.get(leadsTo.toUpperCase()).longValue());
                                connection.insertRow(prefix+"form_status_relationships", values);
                            }
                        }
                    }
                    connection.commit();
                    return new CodaResponse(false, "Success!", -1, null);
            }

            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public synchronized CodaResponse dropForm (String sessionKey, CodaDatabase database, String applicationName, String tableName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();
        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        if (!this.isForm(connection, prefix, tableName)) {
            return new CodaResponse(true, null, 2092);
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }

            try {
                 // Remove all indexes on reference columns on other tables that point to subtables of this table.  Phew.
                CodaResultSet rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"index_fields inf on inf.index_id = i.id inner join "+prefix+"table_fields tf on tf.id = inf.table_field_id inner join "+prefix+"tables t on tf.ref_table_id = t.id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                } else {
                    return new CodaResponse(true, null, 8002);
                }

                // Remove all reference columns to subtables of this table
                rs = connection.runQuery("select f.id, f.field_name, f.array_flag, t.table_name from "+prefix+"table_fields f inner join "+prefix+"tables t on t.id = f.ref_table_id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        if (rs.getDataInt(2) == 1) {
                            connection.dropTable(rs.getData(1));
                        }
                        connection.dropColumn(rs.getData(3), rs.getData(1));
                        connection.deleteRow(prefix+"table_fields", "id", rs.getDataLong(0));

                    }

                }

                // Remove all indexes on reference columns on other tables.  Phew.
                rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"index_fields inf on inf.index_id = i.id inner join "+prefix+"table_fields tf on tf.id = inf.table_field_id where tf.ref_table_id = " + connection.formatStringForSQL(prefix+"table_fields", "ref_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }
                } else {
                    return new CodaResponse(true, null, 8002);
                }

                // Remove all reference columns to this table
                rs = connection.runQuery("select f.id, f.field_name, f.array_flag, t.table_name from "+prefix+"table_fields f where f.ref_table_id = " + connection.formatStringForSQL(prefix+"table_fields", "ref_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        if (rs.getDataInt(2) == 1) {
                            connection.dropTable(rs.getData(1));
                        }
                        connection.dropColumn(rs.getData(3), rs.getData(1));
                        connection.deleteRow(prefix+"table_fields", "id", rs.getDataLong(0));

                    }

                }

                // Remove all indexes on subtables to this table.
                rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i inner join "+prefix+"tables t on t.id = i.table_id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                }

                // Remove all array columns from subtables to this query
                rs = connection.runQuery("select f.field_name from "+prefix+"table_fields f inner join "+prefix+"tables t on t.id = f.table_id where f.array_flag = 1 and t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropTable(rs.getData(0));
                    }

                }

                // Remove all form statuses for subforms of this table
                rs = connection.runQuery("select fs.id from "+prefix+"form_statuses fs inner join "+prefix+"tables t on fs.table_id = t.id where t.parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.runStatement("delete from "+prefix+"form_status_relationships where form_status_id = " + connection.formatStringForSQL(prefix+"form_status_relationships", "form_status_id", rs.getData(0)));
                        connection.deleteRow(prefix+"form_statuses", "id", rs.getDataLong(0));
                    }
                }

                // Remove all subtables to this table
                rs = connection.runQuery("select t.id, t.table_name from "+prefix+"tables t where parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropTable(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"table_fields where table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", rs.getData(0)));
                        connection.deleteRow(prefix+"tables", "id", rs.getDataLong(0));

                    }
                }

                // Remove all array columns for this table
                rs = connection.runQuery("select f.field_name from "+prefix+"table_fields f  where f.array_flag = 1 and f.table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropTable(rs.getData(0));
                    }

                }


                // Remove the indexes for this table
                rs = connection.runQuery("select i.id, i.index_name from "+prefix+"indexes i where i.table_id = " + connection.formatStringForSQL(prefix+"indexes", "table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.dropIndex(rs.getData(1));

                        connection.runStatement("delete from "+prefix+"index_fields where index_id = " + connection.formatStringForSQL(prefix+"index_fields", "index_id", rs.getData(0)));
                        connection.deleteRow(prefix+"indexes", "id", rs.getDataLong(0));

                    }

                }

                // Remove all triggers for this table
                connection.runStatement("delete from "+prefix+"triggers where table_id = " + connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)));
                /**
                 TODO: reload triggers
                 */

                // Remove all permissions for this table
                connection.runStatement("delete from "+prefix+"role_tables where table_id = " + connection.formatStringForSQL(prefix+"role_tables", "table_id", Long.toString(tableId)));

                // Remove the form statuses for this table
                rs = connection.runQuery("select fs.id from "+prefix+"form_statuses fs where fs.table_id = " + connection.formatStringForSQL(prefix+"form_statuses", "table_id", Long.toString(tableId)), null);
                if (!rs.getErrorStatus()) {
                    while (rs.next()) {
                        connection.runStatement("delete from "+prefix+"form_status_relationships where form_status_id = " + connection.formatStringForSQL(prefix+"form_status_relationships", "form_status_id", rs.getData(0)));
                        connection.deleteRow(prefix+"form_statuses", "id", rs.getDataLong(0));
                    }
                }

                // Remove the table itself
                connection.dropTable(tableName);

                connection.runStatement("delete from "+prefix+"table_fields where table_id = " + connection.formatStringForSQL(prefix+"table_fields", "table_id", Long.toString(tableId)));
                connection.deleteRow(prefix+"tables", "id", tableId);

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
            }

            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse createIndex(String sessionKey, CodaDatabase database, String applicationName, String indexName, String tableName, Vector<String> fields, boolean uniqueFlag) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            if (indexName.startsWith("PK_") || indexName.startsWith("ID_")) {
                return new CodaResponse(true, null, 2054);
            }
            if (this.getIdForObjectName(indexName, CodaServer.OBJECT_TYPE_INDEX, connection) > 0) {
                return new CodaResponse(true, null, 2053);
            }
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }
            Vector<Long> fieldIds = new Vector();
            for (String fieldName : fields) {
                long fieldId = this.getIdForObjectName(tableId + "." + fieldName, CodaServer.OBJECT_TYPE_TABLE_COLUMN, connection);
                if (fieldId < 0){
                    return new CodaResponse(true, null, 2040, (this.isForm(connection, prefix, tableName) ? "Field name '" + fieldName + "' is invalid" : "Column name '" + fieldName + "' is invalid"));
                } else {
                    fieldIds.add(fieldId);
                }
            }

            try {
                connection.createIndex(indexName.toUpperCase(), tableName.toUpperCase(), fields, uniqueFlag);
            } catch (SQLException e) {
                return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
            }

            Hashtable values = new Hashtable();
            values.put("index_name", indexName.toUpperCase());
            values.put("table_id", tableId);
            values.put("index_type_id", (uniqueFlag ? 2 : 3));
            values.put("create_user_name", sessions.getSessionUsername(sessionKey));
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            long indexId = connection.insertRow(prefix+"indexes", values);

            for (Long fieldId : fieldIds) {
                values = new Hashtable();
                values.put("index_id", indexId);
                values.put("table_field_id", fieldId);
                connection.insertRow(prefix+"index_fields", values);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse dropIndex(String sessionKey, CodaDatabase database, String applicationName, String indexName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            long indexId = this.getIdForObjectName(indexName, CodaServer.OBJECT_TYPE_INDEX, connection);
            if (indexId <  0) {
                return new CodaResponse(true, null, 2055);
            }

            try {
                connection.dropIndex(indexName.toUpperCase());
            } catch (SQLException e) {
                return new CodaResponse(true, null, 8004, "The following error was reported by the database:" + e.getMessage());
            }

			connection.runStatement("delete from " +prefix+"index_fields where index_id = " +connection.formatStringForSQL(prefix+"index_fields", "index_id", Long.toString(indexId)));

			connection.deleteRow(prefix+"indexes", "id", indexId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse createCron(String sessionKey, String applicationName, String environmentString, String cronName, String minutePart, String hourPart, String dayOfMonth, String month, String dayOfWeek, String procedureName, Vector<String> parameters, String executeUsername, String executePassword) {
        int environment = 1;
        if (environmentString.equalsIgnoreCase("DEV")) {
            environment = 1;
        } else if (environmentString.equalsIgnoreCase("TEST")) {
            environment = 2;
        } else if (environmentString.equalsIgnoreCase("PROD")) {
            environment = 3;
        } else {
            return new CodaResponse(true, null, 2013, "Invalid environment specified");
        }
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }

        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        long executeUserId = modUserId;
        if (executeUsername != null && executePassword != null) {
            CodaConnection connection = database.getConnection();
            CodaResultSet rs = connection.runQuery("select id from users where active_flag = 1 and user_name = " + connection.formatStringForSQL("users", "user_name", executeUsername) + " and pass_word = " + connection.formatStringForSQL("users", "pass_word", encrypt(executePassword)) + " ", null);
            if (!rs.getErrorStatus() && rs.getRowsReturned() > 0) {
                if (!rs.next()) {
                    return new CodaResponse(true, null, 1001);
                }
            } else {
                return new CodaResponse(true, null, 1001);
            }
        } else {
			executeUsername = this.getSessionUsername(sessionKey);
		}

        if (!deployedApplications.hasProcedurePermission(applicationName, executeUserId, -1, environment, Datasource.PROCEDURE_EXECUTE, procedureName.toUpperCase())) {
            return new CodaResponse(true, null, 9011);
        }

        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, environment, "MANAGE_CRONS" )) {
            CodaDatabase database = this.getApplicationDatabase(applicationName, environment);

            CodaConnection connection = database.getConnection(), serverConnection = this.database.getConnection();
            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            if (this.getIdForObjectName(cronName, CodaServer.OBJECT_TYPE_CRON, connection) > 0) {
                return new CodaResponse(true, null, 2057);
            }
            long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
            if (procedureId < 0) {
                return new CodaResponse(true, null, 2056);
            }

            ExecutionContext context = new ExecutionContext(this, database);

            Vector<Long> procedureParameterIds = new Vector();
            Vector<String> procedureTypeNames = new Vector();
            Vector<Boolean> procedureArrayFlags = new Vector();
            CodaResultSet rs = connection.runQuery("select pp.type_name, pp.array_flag, pp.id from "+prefix+"procedure_parameters pp where pp.procedure_id = " + connection.formatStringForSQL(prefix+"procedure_parameters", "procedure_id", Long.toString(procedureId)) + " order by order_number asc",null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    procedureTypeNames.add(rs.getData(0));
                    procedureArrayFlags.add(rs.getData(1).equals("1"));
                    procedureParameterIds.add(rs.getDataLong(2));
                }
            }

            if (procedureTypeNames.size() != parameters.size()) {
                return new CodaResponse(true, null, 2058);
            }

            if (procedureTypeNames.size() > 0) {

                CodaResponse resp = new CodaResponse();

                for(int i = 0; i < procedureTypeNames.size(); i++) {
                    long typeId = this.getIdForObjectName(serverConnection, procedureTypeNames.get(i), this.OBJECT_TYPE_TYPE);
                    if (procedureArrayFlags.get(i).booleanValue()) {
                        if (TypeParser.isArray(parameters.get(i))) {
                            Vector<String> arrayValues = TypeParser.explodeArray(parameters.get(i));
                            for (String arrayValue : arrayValues) {
                                if (!context.validate(typeId, arrayValue)){
                                    resp.addError(new CodaError(3001, "Parameter " + (i + 1) + " contains an invalid " + context.getDisplayName(typeId)));
                                }
                            }
                        }
                    } else {
                        if (!context.validate(typeId, parameters.get(i))){
                            resp.addError(new CodaError(3001, "Parameter " + (i + 1) + " is not a valid " + context.getDisplayName(typeId)));
                        }
                    }
                }

                if (resp.getError()) {
                    return resp;
                }
            }

            JobDetail detail = new JobDetail(applicationName.toUpperCase() + "_" + environment + "_" + cronName.toUpperCase(), null, ProcedureJob.class);

            JobDataMap map = new JobDataMap();
            map.put("cronName", cronName);
            map.put("server", this);
            map.put("applicationName", applicationName);
            map.put("environment", environment);
            map.put("procedureName", procedureName.toUpperCase());
            map.put("parameters", parameters);
            map.put("userId", executeUserId);

            detail.setJobDataMap(map);

            try {
                this.scheduleJob(applicationName, environment, cronName.toUpperCase(), "0 " + minutePart + " " + hourPart + " " + dayOfMonth + " " + month + " " + dayOfWeek, detail);
            } catch (ParseException pe) {
                return new CodaResponse(true, null, 2060);
            } catch (SchedulerException se) {
                return new CodaResponse(true, null, 2061);
            }

            Hashtable values = new Hashtable();
            values.put("cron_name", cronName.toUpperCase());
            values.put("procedure_id", procedureId);
            values.put("minute_part", minutePart);
            values.put("hour_part", hourPart);
            values.put("day_of_month_part", dayOfMonth);
            values.put("month_part", month);
            values.put("day_of_week_part", dayOfWeek);
            values.put("executing_user_name", executeUsername);
			values.put("create_user_name", sessions.getSessionUsername(sessionKey));
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            long cronId = connection.insertRow(prefix+"crons", values);

            int j = 0;
            for (String parameter : parameters) {
                values = new Hashtable();
                values.put("cron_id", cronId);
                values.put("parameter_value", parameter);
                values.put("procedure_parameter_id", procedureParameterIds.get(j));
                connection.insertRow(prefix+"cron_parameters", values);
                j++;
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9010);
        }
    }

    public CodaResponse dropCron(String sessionKey, String applicationName, String environmentString, String cronName) {
        int environment = 1;
        if (environmentString.equalsIgnoreCase("DEV")) {
            environment = 1;
        } else if (environmentString.equalsIgnoreCase("TEST")) {
            environment = 2;
        } else if (environmentString.equalsIgnoreCase("PROD")) {
            environment = 3;
        } else {
            return new CodaResponse(true, null, 2013, "Invalid environment specified");
        }
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }


        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, environment, "MANAGE_CRONS")) {
            CodaDatabase database = this.getApplicationDatabase(applicationName, environment);
            CodaConnection connection = database.getConnection();

            // get the prefix
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            long cronId = this.getIdForObjectName(cronName, CodaServer.OBJECT_TYPE_CRON, connection);
            if (cronId < 0) {
                return new CodaResponse(true, null, 2062);
            }

            try {
                this.stopJob(applicationName, environment, cronName.toUpperCase());
            } catch (SchedulerException se) {
                return new CodaResponse(true, null, 2061);
            }

            connection.runStatement("delete from "+prefix+"cron_parameters where cron_id = " + connection.formatStringForSQL(prefix+"cron_parameters", "cron_id", Long.toString(cronId)));
            connection.deleteRow(prefix+"crons", "id", cronId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
            return new CodaResponse(true, null, 9010);
        }
    }

    public CodaResponse selectObject(String sessionKey, String tableName, long objectId, boolean greedyFlag, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection;
        CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}

		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
        if (tableId < 0) {
            return new CodaResponse(true, null, 2028);
        }

        boolean formFlag = this.isForm(connection, prefix, tableName);
        boolean groupFlag = this.isGroupTable(connection, prefix, tableName);

        if (!formFlag && !deployedApplications.hasTablePermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.TABLE_SELECT, tableName)) {
            return new CodaResponse(true, null, 9012);
        }
        Vector retval = new Vector();
        CodaResultSet rs = connection.runQuery("select * from " + tableName + " where id = " + connection.formatStringForSQL(tableName, "id", Long.toString(objectId)), null);
        if (!rs.getErrorStatus() && rs.next()) {
            if (formFlag && !deployedApplications.hasFormStatusPermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.FORM_STATUS_VIEW, tableName, rs.getDataLong("status_id") )) {
                return new CodaResponse(true, null, 9013);
            }
            if (!greedyFlag) {
				return new CodaResponse(rs);
			} else {
				retval.add(rs);
			}
        } else {
            return new CodaResponse(true, null, 2067);
        }

        if (greedyFlag) {
            rs = connection.runQuery("select table_name from "+prefix+"tables where parent_table_id = " + connection.formatStringForSQL(prefix+"tables", "parent_table_id", Long.toString(tableId)), null);
            if (!rs.getErrorStatus()) {
                while (rs.next()) {
                    if ((!formFlag && deployedApplications.hasTablePermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.TABLE_SELECT, rs.getData(0))) || formFlag) {
                        Hashtable temp = new Hashtable();
                        CodaResultSet rs2 = connection.runQuery("select * from " + rs.getData(0) + " where parent_table_id = " + connection.formatStringForSQL(rs.getData(0), "parent_table_id", Long.toString(objectId)), null);
                        if (!rs2.getErrorStatus() && rs2.next()) {
                            if (!formFlag || (formFlag && deployedApplications.hasFormStatusPermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.FORM_STATUS_VIEW, rs.getData(0), rs2.getDataLong("status_id") ))) {
                                retval.add(rs2);
                            }
                        }
                    }
                }
            }
        }

        return new CodaResponse(retval);
    }

    public CodaResponse insert (String sessionKey, String tableName, Vector<String> columns, Vector<Vector> rows, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection, serverConnection;
		serverConnection = this.database.getConnection();
		CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}
		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        long tableId = this.getIdForObjectName(tableName.toUpperCase(), CodaServer.OBJECT_TYPE_TABLE, connection);
        if (tableId < 0) {
            return new CodaResponse(true, null, 2028);
        }

        boolean formFlag = this.isForm(connection, prefix, tableName);
        if (formFlag) {
            return new CodaResponse(true, null, 2071);
        }

        boolean subTableFlag = this.isSubTable(connection, prefix, tableName);

        boolean groupFlag = this.isGroupTable(connection, prefix, tableName);
        if (groupFlag && sessions.getSessionGroup(sessionKey) == null) {
            return new CodaResponse(true, null, 2074);
        }

        boolean softDeleteFlag = this.isSoftDeleteTable(connection, prefix, tableName);

        if (!deployedApplications.hasTablePermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.TABLE_INSERT, tableName)) {
            return new CodaResponse(true, null, 9014);
        }

        ExecutionContext context = new ExecutionContext(this, database);

		Hashtable<String,TableFieldDefinition> fields = this.getFieldsForTable(connection, prefix, tableId, serverConnection);

		// get rid of the stray row
		if (rows.get(rows.size() - 1).size() == 0) {
			rows.remove(rows.size() - 1);
		}

		//capitalize the columns.  This is just to make life easier
        for (int i = 0; i < columns.size(); i++) {
            columns.set(i, columns.get(i).toUpperCase());
        }

        //verify all of the columns specified are valid
        for (String columnName : columns) {
            if (!

                  fields.containsKey(columnName.toUpperCase())
                ) {
                return new CodaResponse(true, null, 2068,  "Column '" +columnName+ "' not found in table '"+ tableName +"'");
            }
        }

        // check for the parent table id if needed
        if (subTableFlag && !columns.contains("PARENT_TABLE_ID")) {
            return new CodaResponse(true, null, 2073);
        }

        //verify that every non-nullable column without a default is included in the insert clause
        for (TableFieldDefinition field : fields.values()) {
            if (!field.isNullableFlag()) {
                if (!field.isBuiltInFlag() && field.getDefaultVariableId() == -1 && field.getDefaultValue() == null && !columns.contains(field.getFieldName())) {
                    return new CodaResponse(true, null, 2070, "Must specify a value or default for NOT NULL column '" + field.getFieldName() + "'");
                }
            }
        }

        //validate all of the data before inserting
        CodaResponse resp = new CodaResponse();

        for (int j = 0; j < rows.size(); j++) {
            List<String> row = rows.get(j);
            for (int i = 0; i < row.size(); i++) {
                if (row.get(i) == null) {
                    if (fields.get(columns.get(i)).getDefaultVariableId() > 0) {
                        switch (fields.get(columns.get(i)).getDefaultVariableId()) {
                            case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
                                row.set(i, Long.toString(new GregorianCalendar().getTimeInMillis()));
                                break;
                            case CodaConstant.SYSVAR_CURRENT_USER_ID:
                                row.set(i, Long.toString(sessions.getSessionUserId(sessionKey)));
                                break;
                            case CodaConstant.SYSVAR_CURRENT_USERNAME:
                                row.set(i, sessions.getSessionUsername(sessionKey));
                                break;
                            case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
                                row.set(i, sessions.getSessionGroup(sessionKey));
                                break;
                        }
                    } else if (fields.get(columns.get(i)).getDefaultValue() != null) {
                        row.set(i, fields.get(columns.get(i)).getDefaultValue());
                    } else {
                        resp.addError(new CodaError(2070, (rows.size() > 1 ? "On row " + (j + 1) + ", p" : "P") +"lease specify a value for " + fields.get(columns.get(i)).getDisplayedAs()));
                    }
                } else if (row.get(i).equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                    row.set(i, Long.toString(new GregorianCalendar().getTimeInMillis()));
                } else if (row.get(i).equalsIgnoreCase("CURRENT_USER_ID")) {
                    row.set(i, Long.toString(sessions.getSessionUserId(sessionKey)));
                } else if (row.get(i).equalsIgnoreCase("CURRENT_USERNAME")) {
                    row.set(i, sessions.getSessionUsername(sessionKey));
                } else if (row.get(i).equalsIgnoreCase("CURRENT_GROUP_NAME")) {
                    row.set(i, sessions.getSessionGroup(sessionKey));
                } else if (fields.get(columns.get(i)).isArrayFlag()) {
                    if (TypeParser.isArray(row.get(i))) {
                        Vector<String> items = TypeParser.explodeArray(row.get(i));
                        for (String item : items) {
                            if (!context.validate(fields.get(columns.get(i)).getTypeId(), item)) {
                                resp.addError(new CodaError(2070, (rows.size() > 1 ? "On row " + (j + 1) + ", e" : "E") +"ach of the " + fields.get(columns.get(i)).getDisplayedAs() + " must be of type " + context.getDisplayName(fields.get(columns.get(i)).getTypeId())));
                                break;
                            }
                        }
                    } else {
                        resp.addError(new CodaError(2070, (rows.size() > 1 ? "On row " + (j + 1) + ", a" : "A") +"n array is expected for the " + fields.get(columns.get(i)).getDisplayedAs()));
                    }
                } else {
                    if (!context.validate(this.getIdForObjectName(serverConnection, fields.get(columns.get(i)).getTypeName(), CodaServer.OBJECT_TYPE_TYPE), row.get(i))) {
                        resp.addError(new CodaError(2070, (rows.size() > 1 ? "On row " + (j + 1) + ", " : "") + fields.get(columns.get(i)).getDisplayedAs() + " must be of type " + context.getDisplayName(fields.get(columns.get(i)).getTypeId())));
                    }
                }
            }
        }

        // return if there are errors
        if (resp.getError()) {
            return resp;
        }

		// set the classloader for the execution context
		context.setClassLoader(deployedApplications.get(applicationName.toUpperCase()).getEnvironmentClassLoader(environmentId, context.getClassLoader()));

		CodaResultSetColumnHeading crsch = new CodaResultSetColumnHeading("id", Types.BIGINT);
		Vector retvalHeadings = new Vector();
		retvalHeadings.add(crsch);
		CodaResultSet retval = new CodaResultSet(retvalHeadings);


		// Insert!
        for (int j = 0; j < rows.size(); j++) {
            List<String> row = rows.get(j);
            Hashtable values = new Hashtable();
            Hashtable<String,Vector> arrayValues = new Hashtable();
            for (int i = 0; i < row.size(); i++) {
                if (fields.get(columns.get(i)).isArrayFlag()) {
                    values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
                    arrayValues.put(fields.get(columns.get(i)).getFieldName(), TypeParser.explodeArray(row.get(i)));
                } else {
                    values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
                }
            }
            if (groupFlag && !values.containsKey("GROUP_ID")) {
                values.put("GROUP_ID", sessions.getSessionGroupId(sessionKey));
            }
            if (softDeleteFlag && !values.containsKey("ACTIVE_FLAG")) {
                values.put("ACTIVE_FLAG", 1);
            }
            if (!values.containsKey("CREATE_USER_NAME")) {
                values.put("CREATE_USER_NAME", sessions.getSessionUsername(sessionKey));
            }
            if (!values.containsKey("CREATE_DATE")) {
                values.put("CREATE_DATE", new GregorianCalendar().getTimeInMillis());
            }
            if (!values.containsKey("MOD_USER_NAME")) {
                values.put("MOD_USER_NAME", sessions.getSessionUsername(sessionKey));
            }
            if (!values.containsKey("MOD_DATE")) {
                values.put("MOD_DATE", new GregorianCalendar().getTimeInMillis());
            }

            // call before trigger
            Vector<Hashtable> triggerHashtables = this.prepareHashtablesForTrigger(context, serverConnection, fields, null, values);

            try {
                deployedApplications.get(applicationName).runTrigger(context, tableName, "insert", true, new Database(this, sessionKey, connection), triggerHashtables.get(1), triggerHashtables.get(0));
            } catch (ClassNotFoundException e) {
                return new CodaResponse(true, null, 4002, e.getMessage());
            } catch (IllegalAccessException e) {
                return new CodaResponse(true, null, 4002, e.getMessage());
            } catch (InstantiationException e) {
                return new CodaResponse(true, null, 4002, e.getMessage());
            } catch (CodaException e) {
                return new CodaResponse(true, null, 4002, e.getMessage());
            }

            long id = connection.insertRow(tableName.toUpperCase(), values);

			Vector rowVector = new Vector();
			rowVector.add(id);
			retval.addRow(rowVector);

			if (id < 0) {
				if (procConnection == null) {
					connection.rollback();
				}
				return new CodaResponse(true, null, 3005);
			} else {
				Enumeration enum1 = arrayValues.keys();
				while (enum1.hasMoreElements()) {
					String key = (String)enum1.nextElement();
					Vector<String> elts = arrayValues.get(key);
					for (String elt : elts) {
						Hashtable values2 = new Hashtable();
						values2.put("id", id);
						values2.put("value", elt);
						connection.insertRow(key, values2);
					}
				}

				// call after trigger
				try {
					triggerHashtables.get(1).put("id", id);
					deployedApplications.get(applicationName).runTrigger(context, tableName, "insert", false, new Database(this, sessionKey, connection), triggerHashtables.get(1), triggerHashtables.get(0));
				} catch (ClassNotFoundException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				} catch (IllegalAccessException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				} catch (InstantiationException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				} catch (CodaException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				}
			}
		}

        if (procConnection == null) {
            connection.commit();
        }

        return new CodaResponse(retval);
    }

    public CodaResponse update (String sessionKey, String tableName, Vector<String> columns, Vector<String> row, CodaSearchCondition whereClause, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection, serverConnection;
        serverConnection = this.database.getConnection();
		CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}
		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
        if (tableId < 0) {
            return new CodaResponse(true, null, 2028);
        }

        boolean formFlag = this.isForm(connection, prefix, tableName);
        boolean subTableFlag = this.isSubTable(connection, prefix, tableName);

        boolean groupFlag = this.isGroupTable(connection, prefix, tableName);
        if (groupFlag && sessions.getSessionGroup(sessionKey) == null) {
            return new CodaResponse(true, null, 2074);
        }

        boolean softDeleteFlag = this.isSoftDeleteTable(connection, prefix, tableName);

        if (!deployedApplications.hasTablePermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.TABLE_UPDATE, tableName)) {
            return new CodaResponse(true, null, 9014);
        }

        ExecutionContext context = new ExecutionContext(this, database);

        Hashtable<String,TableFieldDefinition> fields = this.getFieldsForTable(connection, prefix, tableId, serverConnection);

        //capitalize the columns.  This is just to make life easier
        for (int i = 0; i < columns.size(); i++) {
            columns.set(i, columns.get(i).toUpperCase());
        }

        //verify all of the columns specified are valid
        for (String columnName : columns) {
            if (!
                ( columnName.equalsIgnoreCase("ID") ||
                  columnName.equalsIgnoreCase("CREATE_USER_ID") ||
                  columnName.equalsIgnoreCase("CREATE_USER_NAME") ||
                  columnName.equalsIgnoreCase("CREATE_DATE") ||
                  columnName.equalsIgnoreCase("MOD_USER_ID") ||
                  columnName.equalsIgnoreCase("MOD_USER_NAME") ||
                  columnName.equalsIgnoreCase("MOD_DATE") ||
                  (softDeleteFlag && columnName.equalsIgnoreCase("ACTIVE_FLAG")) ||
                  (groupFlag && columnName.equalsIgnoreCase("GROUP_ID")) ||
                  (subTableFlag && ((!formFlag && columnName.equalsIgnoreCase("PARENT_TABLE_ID")) || (formFlag && columnName.equalsIgnoreCase("PARENT_FORM_ID")))) ||
                  fields.containsKey(columnName.toUpperCase())
                  )
                ) {
                return new CodaResponse(true, null, 2068,  "Column '" +columnName+ "' not found in table '"+ tableName +"'");
            }
        }

        // check for the parent table id if needed
        if (subTableFlag && ((!formFlag && !columns.contains("PARENT_TABLE_ID")) || (formFlag && !columns.contains("PARENT_FORM_ID")))) {
            return new CodaResponse(true, null, 2073);
        }

        //verify that every non-nullable column without a default is included in the insert clause
        /*
		for (TableFieldDefinition field : fields.values()) {
            if (columns.contains(field.getFieldName()) && columns. && !field.isNullableFlag() && field.getDefaultVariableId() == -1 && field.getDefaultValue() == null) {
                return new CodaResponse(true, null, 2070, "Must specify a value or default for NOT NULL column '" + field.getFieldName() + "'");
            }
        }
        */

        //validate all of the data before inserting
        CodaResponse resp = new CodaResponse();


        for (int i = 0; i < row.size(); i++) {
            if (row.get(i) == null) {
                if (fields.get(columns.get(i)).getDefaultVariableId() > 0) {
                    switch (fields.get(columns.get(i)).getDefaultVariableId()) {
                        case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
                            row.set(i, Long.toString(new GregorianCalendar().getTimeInMillis()));
                            break;
                        case CodaConstant.SYSVAR_CURRENT_USER_ID:
                            row.set(i, Long.toString(sessions.getSessionUserId(sessionKey)));
                            break;
                        case CodaConstant.SYSVAR_CURRENT_USERNAME:
                            row.set(i, sessions.getSessionUsername(sessionKey));
                            break;
                        case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
                            row.set(i, sessions.getSessionGroup(sessionKey));
                            break;
                    }
                } else if (fields.get(columns.get(i)).getDefaultValue() != null) {
                    row.set(i, fields.get(columns.get(i)).getDefaultValue());
                } else if (!fields.get(columns.get(i)).isNullableFlag()) {
                    resp.addError(new CodaError(2070, "Please specify a value for " + fields.get(columns.get(i)).getDisplayedAs()));
                }
            } else if (row.get(i).equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                row.set(i, Long.toString(new GregorianCalendar().getTimeInMillis()));
            } else if (row.get(i).equalsIgnoreCase("CURRENT_USER_ID")) {
                row.set(i, Long.toString(sessions.getSessionUserId(sessionKey)));
            } else if (row.get(i).equalsIgnoreCase("CURRENT_USERNAME")) {
                row.set(i, sessions.getSessionUsername(sessionKey));
            } else if (row.get(i).equalsIgnoreCase("CURRENT_GROUP_NAME")) {
                row.set(i, sessions.getSessionGroup(sessionKey));
            } else if (fields.get(columns.get(i)).isArrayFlag()) {
                if (TypeParser.isArray(row.get(i))) {
                    Vector<String> items = TypeParser.explodeArray(row.get(i));
                    for (String item : items) {
                        if (!context.validate(fields.get(columns.get(i)).getTypeId(), item)) {
                            resp.addError(new CodaError(2070, "Each of the " + fields.get(columns.get(i)).getDisplayedAs() + " must be of type " + context.getDisplayName(fields.get(columns.get(i)).getTypeId())));
                            break;
                        }
                    }
                } else {
                    resp.addError(new CodaError(2070, "An array is expected for the " + fields.get(columns.get(i)).getDisplayedAs()));
                }
            } else {
                if (!context.validate(this.getIdForObjectName(serverConnection, fields.get(columns.get(i)).getTypeName(), CodaServer.OBJECT_TYPE_TYPE), row.get(i))) {
                    resp.addError(new CodaError(2070, fields.get(columns.get(i)).getDisplayedAs() + " must be of type " + context.getDisplayName(fields.get(columns.get(i)).getTypeId())));
                }
            }
        }

        // return if there are errors
        if (resp.getError()) {
            return resp;
        }
        CodaResultSet rs = null;
        try {
			CodaFromClause fromClause = new CodaFromClause(tableName);
			fromClause.setDatabase(database);
			rs = connection.runQuery("select id from " + tableName + (whereClause != null ? " where " + whereClause.print(fromClause) : "" ), null);
        } catch (CodaException ex) {
            return new CodaResponse(true, null, 8004, "Something went wrong trying to figure out which records to update.");
        }
        if (rs == null || rs.getErrorStatus()) {
            return new CodaResponse(true, null, 8004, rs.getErrorString());
        } else {
            // grab ids to insert
            Vector<Long> idsToUpdate = new Vector();
            while (rs.next()) {
                idsToUpdate.add(rs.getDataLong(0));
            }

			if (idsToUpdate.size() > 0) {

				// create values hashtable
				Hashtable values = new Hashtable();
				Hashtable<String,Vector> arrayValues = new Hashtable();
				for (int i = 0; i < row.size(); i++) {
					if (fields.get(columns.get(i)).isArrayFlag()) {
						values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
						arrayValues.put(fields.get(columns.get(i)).getFieldName(), TypeParser.explodeArray(row.get(i)));
					} else {
						values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
					}
				}
				if (groupFlag && !values.containsKey("GROUP_ID")) {
					values.put("GROUP_ID", sessions.getSessionGroupId(sessionKey));
				}
				if (softDeleteFlag && !values.containsKey("ACTIVE_FLAG")) {
					values.put("ACTIVE_FLAG", 1);
				}
				if (!values.containsKey("MOD_USER_NAME")) {
					values.put("MOD_USER_NAME", sessions.getSessionUsername(sessionKey));
				}
				if (!values.containsKey("MOD_DATE")) {
					values.put("MOD_DATE", new GregorianCalendar().getTimeInMillis());
				}

				// set the classloader for the execution context
				context.setClassLoader(deployedApplications.get(applicationName.toUpperCase()).getEnvironmentClassLoader(environmentId, context.getClassLoader()));


				// call before trigger
				Vector<Vector> updatingRecords = new Vector();
				CodaResultSet triggerResult = connection.runQuery("select id from "+prefix+"triggers where table_id = " +connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)) + " and before_flag = 1 and operation_id = 1 ", null);
				if (!triggerResult.getErrorStatus() && triggerResult.next()) {
					CodaResultSet currentRows = connection.selectRows(tableName, "id", idsToUpdate);
					if (!currentRows.getErrorStatus()) {
						Vector<String> allColumnNames = currentRows.getColumnNames();
						while (currentRows.next()) {
							Hashtable rowHashtable = new Hashtable();
							for (int j = 0; j < allColumnNames.size(); j++) {
								rowHashtable.put(allColumnNames.get(j), currentRows.getData(j));
							}
							updatingRecords.add(this.prepareHashtablesForTrigger(context, serverConnection, fields, rowHashtable, values));
						}
					}
					for (Vector<Hashtable> i : updatingRecords) {
						try {
							deployedApplications.get(applicationName).runTrigger(context, tableName, "update", true, new Database(this, sessionKey, connection), i.get(1), i.get(0));
						} catch (ClassNotFoundException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (IllegalAccessException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (InstantiationException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (CodaException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						}
					}
				}


				// do update
				boolean success = connection.updateRows(tableName, "id", idsToUpdate, values);
				if(!success) {
					if (procConnection == null) {
						connection.rollback();
					}
					return new CodaResponse(true, null, 3005);
				} else {

					Enumeration enum1 = arrayValues.keys();
					for(Long id : idsToUpdate) {
						while (enum1.hasMoreElements()) {
							String key = (String)enum1.nextElement();
							connection.runStatement("delete from " + key + " where id = " + connection.formatStringForSQL(key, "id", Long.toString(id)));
							Vector<String> elts = arrayValues.get(key);
							for (String elt : elts) {
								Hashtable values2 = new Hashtable();
								values2.put("id", id);
								values2.put("value", elt);
								connection.insertRow(key, values2);
							}
						}
					}

					// call after trigger

					triggerResult = connection.runQuery("select id from "+prefix+"triggers where table_id = " +connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)) + " and before_flag = 0 and operation_id = 1 ", null);
					if (!triggerResult.getErrorStatus() && triggerResult.next()) {
						if (updatingRecords.size() == 0) {
							CodaResultSet currentRows = connection.selectRows(tableName, "id", idsToUpdate);
							if (!currentRows.getErrorStatus()) {
								Vector<String> allColumnNames = currentRows.getColumnNames();
								while (currentRows.next()) {
									Hashtable rowHashtable = new Hashtable();
									for (int j = 0; j < allColumnNames.size(); j++) {
										rowHashtable.put(allColumnNames.get(j), currentRows.getData(j));
									}
									updatingRecords.add(this.prepareHashtablesForTrigger(context, serverConnection, fields, rowHashtable, values));
								}
							}
						}
						for (Vector<Hashtable> i : updatingRecords) {
							try {
								deployedApplications.get(applicationName).runTrigger(context, tableName, "update", false, new Database(this, sessionKey, connection), i.get(1), i.get(0));
							} catch (ClassNotFoundException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							} catch (IllegalAccessException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							} catch (InstantiationException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							} catch (CodaException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							}
						}
					}
				}
			}
		}

        if (procConnection == null) {
            connection.commit();
        }

        return new CodaResponse(false, "Success!", -1, null);
    }

    public CodaResponse delete (String sessionKey, String tableName, CodaSearchCondition whereClause, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection, serverConnection;
		serverConnection = this.database.getConnection();
		CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}

		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
        if (tableId < 0) {
            return new CodaResponse(true, null, 2028);
        }

        boolean formFlag = this.isForm(connection, prefix, tableName);
        boolean subTableFlag = this.isSubTable(connection, prefix, tableName);

        boolean groupFlag = this.isGroupTable(connection, prefix, tableName);
        if (groupFlag && sessions.getSessionGroup(sessionKey) == null) {
            return new CodaResponse(true, null, 2074);
        }

        boolean softDeleteFlag = this.isSoftDeleteTable(connection, prefix, tableName);

        if (!deployedApplications.hasTablePermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.TABLE_DELETE, tableName)) {
            return new CodaResponse(true, null, 9014);
        }

        ExecutionContext context = new ExecutionContext(this, database);
        CodaResultSet rs = null;
        try {
			CodaFromClause fromClause = new CodaFromClause(tableName);
			fromClause.setDatabase(database);
			rs = connection.runQuery("select id from " + tableName + (whereClause != null ? " where " + whereClause.print(fromClause) : ""), null);
        } catch (CodaException ex) {
            return new CodaResponse(true, null, 8004, "Something went wrong trying to figure out which records to update.");
        }
        if (rs == null || rs.getErrorStatus()) {
            return new CodaResponse(true, null, 8004, rs.getErrorString());
        } else {
            // grab ids to delete
            Vector<Long> idsToUpdate = new Vector();
            while (rs.next()) {
                idsToUpdate.add(rs.getDataLong(0));
            }

			if (idsToUpdate.size() > 0) {
				// set the classloader for the execution context
				context.setClassLoader(deployedApplications.get(applicationName.toUpperCase()).getEnvironmentClassLoader(environmentId, context.getClassLoader()));

				// call before trigger
				Vector<Vector> updatingRecords = new Vector();
				CodaResultSet triggerResult = connection.runQuery("select id from "+prefix+"triggers where table_id = " +connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)) + " and before_flag = 1 and operation_id = 3 ", null);
				if (!triggerResult.getErrorStatus() && triggerResult.next()) {
					CodaResultSet currentRows = connection.selectRows(tableName, "id", idsToUpdate);
					if (!currentRows.getErrorStatus()) {
						Vector<String> allColumnNames = currentRows.getColumnNames();
						while (currentRows.next()) {
							Hashtable rowHashtable = new Hashtable();
							for (int j = 0; j < allColumnNames.size(); j++) {
								rowHashtable.put(allColumnNames.get(j), currentRows.getData(j));
							}
							updatingRecords.add(this.prepareHashtablesForTrigger(context, serverConnection, this.getFieldsForTable(connection, prefix, tableId, serverConnection), rowHashtable, null));
						}
					}
					for (Vector<Hashtable> i : updatingRecords) {
						try {
							deployedApplications.get(applicationName).runTrigger(context, tableName, "delete", true, new Database(this, sessionKey, connection), i.get(1), i.get(0));
						} catch (ClassNotFoundException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (IllegalAccessException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (InstantiationException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (CodaException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						}
					}
				}

				if (!softDeleteFlag) {
					Hashtable<String,TableFieldDefinition> fields = this.getFieldsForTable(connection, prefix, tableId, serverConnection);
					Vector<String> arrayColumns = new Vector();
					Enumeration<String> fieldNames = fields.keys();
					while (fieldNames.hasMoreElements()) {
						String fieldName = fieldNames.nextElement();
						if (fields.get(fieldName).isArrayFlag()) {
							arrayColumns.add(fieldName);
						}
					}

				   for (String key : arrayColumns) {
					   connection.deleteRows(key, "id", idsToUpdate);
				   }
				}

				if (softDeleteFlag) {
					Hashtable values = new Hashtable();
					values.put("active_flag", 0);
					connection.updateRows(tableName, "id", idsToUpdate, values);
				} else {
					connection.deleteRows(tableName, "id", idsToUpdate);
				}

				// call after trigger

				triggerResult = connection.runQuery("select id from "+prefix+"triggers where table_id = " +connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)) + " and before_flag = 0 and operation_id = 3 ", null);
				if (!triggerResult.getErrorStatus() && triggerResult.next()) {
					if (updatingRecords.size() == 0) {
						CodaResultSet currentRows = connection.selectRows(tableName, "id", idsToUpdate);
						if (!currentRows.getErrorStatus()) {
							Vector<String> allColumnNames = currentRows.getColumnNames();
							while (currentRows.next()) {
								Hashtable rowHashtable = new Hashtable();
								for (int j = 0; j < allColumnNames.size(); j++) {
									rowHashtable.put(allColumnNames.get(j), currentRows.getData(j));
								}
								updatingRecords.add(this.prepareHashtablesForTrigger(context, serverConnection, this.getFieldsForTable(connection, prefix, tableId, serverConnection), rowHashtable, null));
							}
						}
					}
					for (Vector<Hashtable> i : updatingRecords) {
						try {
							deployedApplications.get(applicationName).runTrigger(context, tableName, "delete", false, new Database(this, sessionKey, connection), i.get(1), i.get(0));
						} catch (ClassNotFoundException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (IllegalAccessException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (InstantiationException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						} catch (CodaException e) {
							return new CodaResponse(true, null, 4002, e.getMessage());
						}
					}
				}
			}
        }

        if (procConnection == null) {
            connection.commit();
        }
        return new CodaResponse(false, "Success!", -1, null);
    }

    public CodaResponse formUpdate (String sessionKey, String formStatusVerb, String tableName, Vector<String> columns, Vector<String> row, boolean initialize, CodaSearchCondition whereClause, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection, serverConnection;
		serverConnection = this.database.getConnection();
		CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}

		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
        if (tableId < 0) {
            return new CodaResponse(true, null, 2028);
        }

        long formStatusId = this.getIdForObjectName(Long.toString(tableId) + "." + formStatusVerb.toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_VERB, connection);
        if (formStatusId < 0) {
            return new CodaResponse(true, null, 2076);
        }

        boolean initialFlag = this.isInitialFormStatus(connection, prefix, formStatusId);
        if (initialize && !initialFlag) {
            return new CodaResponse(true, null, 2077);
        }

        boolean formFlag = this.isForm(connection, prefix, tableName);
        boolean subTableFlag = this.isSubTable(connection, prefix, tableName);

        boolean groupFlag = this.isGroupTable(connection, prefix, tableName);
        if (groupFlag && sessions.getSessionGroup(sessionKey) == null) {
            return new CodaResponse(true, null, 2074);
        }

        if (!deployedApplications.hasFormStatusPermission(applicationName, userId, (groupFlag ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.FORM_STATUS_CALL, tableName, formStatusVerb, false)) {
            return new CodaResponse(true, null, 9015);
        }

        ExecutionContext context = new ExecutionContext(this, database);

        Hashtable<String,TableFieldDefinition> fields = this.getFieldsForTable(connection, prefix, tableId, serverConnection);

        //capitalize the columns.  This is just to make life easier
        for (int i = 0; i < columns.size(); i++) {
            columns.set(i, columns.get(i).toUpperCase());
        }

        //verify all of the columns specified are valid
        for (String columnName : columns) {
            if (!
                ( columnName.equalsIgnoreCase("ID") ||
                  columnName.equalsIgnoreCase("CREATE_USER_ID") ||
                  columnName.equalsIgnoreCase("CREATE_USER_NAME") ||
                  columnName.equalsIgnoreCase("CREATE_DATE") ||
                  columnName.equalsIgnoreCase("MOD_USER_ID") ||
                  columnName.equalsIgnoreCase("MOD_USER_NAME") ||
                  columnName.equalsIgnoreCase("MOD_DATE") ||
                  (groupFlag && columnName.equalsIgnoreCase("GROUP_ID")) ||
                  (subTableFlag && ((!formFlag && columnName.equalsIgnoreCase("PARENT_TABLE_ID")) || (formFlag && columnName.equalsIgnoreCase("PARENT_FORM_ID")))) ||
                  fields.containsKey(columnName.toUpperCase())
                  )
                ) {
                return new CodaResponse(true, null, 2068,  "Column '" +columnName+ "' not found in table '"+ tableName +"'");
            }
        }

        // check for the parent table id if needed
        if (subTableFlag && ((!formFlag && !columns.contains("PARENT_TABLE_ID")) || (formFlag && !columns.contains("PARENT_FORM_ID")))) {
            return new CodaResponse(true, null, 2073);
        }


        //validate all of the data before inserting
        CodaResponse resp = new CodaResponse();

        for (int i = 0; i < row.size(); i++) {
            if (row.get(i) == null) {
                if (fields.get(columns.get(i)).getDefaultVariableId() > 0) {
                    switch (fields.get(columns.get(i)).getDefaultVariableId()) {
                        case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
                            row.set(i, Long.toString(new GregorianCalendar().getTimeInMillis()));
                            break;
                        case CodaConstant.SYSVAR_CURRENT_USER_ID:
                            row.set(i, Long.toString(sessions.getSessionUserId(sessionKey)));
                            break;
                        case CodaConstant.SYSVAR_CURRENT_USERNAME:
                            row.set(i, sessions.getSessionUsername(sessionKey));
                            break;
                        case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
                            row.set(i, sessions.getSessionGroup(sessionKey));
                            break;
                    }
                } else if (fields.get(columns.get(i)).getDefaultValue() != null) {
                    row.set(i, fields.get(columns.get(i)).getDefaultValue());
                } else {
                    resp.addError(new CodaError(2070, "Please specify a value for " + fields.get(columns.get(i)).getDisplayedAs()));
                }
            }  else if (row.get(i).equalsIgnoreCase("CURRENT_TIMESTAMP")) {
                row.set(i, Long.toString(new GregorianCalendar().getTimeInMillis()));
            } else if (row.get(i).equalsIgnoreCase("CURRENT_USER_ID")) {
                row.set(i, Long.toString(sessions.getSessionUserId(sessionKey)));
            } else if (row.get(i).equalsIgnoreCase("CURRENT_USERNAME")) {
                row.set(i, sessions.getSessionUsername(sessionKey));
            } else if (row.get(i).equalsIgnoreCase("CURRENT_GROUP_NAME")) {
                row.set(i, sessions.getSessionGroup(sessionKey));
            } else if (fields.get(columns.get(i)).isArrayFlag()) {
                if (TypeParser.isArray(row.get(i))) {
                    Vector<String> items = TypeParser.explodeArray(row.get(i));
                    for (String item : items) {
                        if (!context.validate(fields.get(columns.get(i)).getTypeId(), item)) {
                            resp.addError(new CodaError(2070, "Each of the " + fields.get(columns.get(i)).getDisplayedAs() + " must be of type " + context.getDisplayName(fields.get(columns.get(i)).getTypeId())));
                            break;
                        }
                    }
                } else {
                    resp.addError(new CodaError(2070, "An array is expected for the " + fields.get(columns.get(i)).getDisplayedAs()));
                }
            } else {
                if (!context.validate(this.getIdForObjectName(serverConnection, fields.get(columns.get(i)).getTypeName(), CodaServer.OBJECT_TYPE_TYPE), row.get(i))) {
                    resp.addError(new CodaError(2070, fields.get(columns.get(i)).getDisplayedAs() + " must be of type " + context.getDisplayName(fields.get(columns.get(i)).getTypeId())));
                }
            }
        }

        // return if there are errors
        if (resp.getError()) {
            return resp;
        }

        // Check if we need to do an update or an insert
        if (initialize) {
			// initialize the return value
			CodaResultSetColumnHeading crsch = new CodaResultSetColumnHeading("id", Types.BIGINT);
			Vector retvalHeadings = new Vector();
			retvalHeadings.add(crsch);
			CodaResultSet retval = new CodaResultSet(retvalHeadings);

			//verify that every non-nullable column without a default is included in the insert clause

            Hashtable values = new Hashtable();
            Hashtable<String,Vector> arrayValues = new Hashtable();
            for (int i = 0; i < row.size(); i++) {
                if (fields.get(columns.get(i)).isArrayFlag()) {
                    values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
                    arrayValues.put(fields.get(columns.get(i)).getFieldName(), TypeParser.explodeArray(row.get(i)));
                } else {
                    values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
                }
            }
            values.put("STATUS_ID", formStatusId);
            if (groupFlag && !values.containsKey("GROUP_ID")) {
                values.put("GROUP_ID", sessions.getSessionGroupId(sessionKey));
            }
            if (!values.containsKey("CREATE_USER_NAME")) {
                values.put("CREATE_USER_NAME", sessions.getSessionUsername(sessionKey));
            }
            if (!values.containsKey("CREATE_DATE")) {
                values.put("CREATE_DATE", new GregorianCalendar().getTimeInMillis());
            }
            if (!values.containsKey("MOD_USER_NAME")) {
                values.put("MOD_USER_NAME", sessions.getSessionUsername(sessionKey));
            }
            if (!values.containsKey("MOD_DATE")) {
                values.put("MOD_DATE", new GregorianCalendar().getTimeInMillis());
            }

			// set the classloader for the execution context
			context.setClassLoader(deployedApplications.get(applicationName.toUpperCase()).getEnvironmentClassLoader(environmentId, context.getClassLoader()));


			// call before trigger
            Vector<Hashtable> rowHashtables = this.prepareHashtablesForTrigger(context, serverConnection, fields, null, values);

            try {
                deployedApplications.get(applicationName).runTrigger(context, tableName, formStatusVerb, true, new Database(this, sessionKey, connection), rowHashtables.get(1), rowHashtables.get(0));
            } catch (ClassNotFoundException e) {
                        return new CodaResponse(true, null, 4002, e.getMessage());
                    } catch (IllegalAccessException e) {
                        return new CodaResponse(true, null, 4002, e.getMessage());
                    } catch (InstantiationException e) {
                        return new CodaResponse(true, null, 4002, e.getMessage());
                    } catch (CodaException e) {
                        return new CodaResponse(true, null, 4002, e.getMessage());
                    }


            long id = connection.insertRow(tableName.toUpperCase(), values);

			Vector rowVector = new Vector();
			rowVector.add(id);
			retval.addRow(rowVector);

			if (id < 0) {
				if (procConnection == null) {
					connection.rollback();
				}
				return new CodaResponse(true, null, 3005);
			} else {

				Enumeration enum1 = arrayValues.keys();
				while (enum1.hasMoreElements()) {
					String key = (String)enum1.nextElement();
					Vector<String> elts = arrayValues.get(key);
					for (String elt : elts) {
						Hashtable values2 = new Hashtable();
						values2.put("id", id);
						values2.put("value", elt);
						connection.insertRow(key, values2);
					}
				}

				// call after trigger
				try {
					rowHashtables.get(1).put("id", id);
					deployedApplications.get(applicationName).runTrigger(context, tableName, formStatusVerb, false, new Database(this, sessionKey, connection), rowHashtables.get(1), rowHashtables.get(0));
				} catch (ClassNotFoundException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				} catch (IllegalAccessException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				} catch (InstantiationException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				} catch (CodaException e) {
					return new CodaResponse(true, null, 4002, e.getMessage());
				}
			}

			if (procConnection == null) {
				connection.commit();
			}

			return new CodaResponse(retval);
		} else {

            // Get the valid statuses that can lead to the requested status
            CodaResultSet rs = connection.runQuery("select form_status_id from "+prefix+"form_status_relationships where next_form_status_id = "+ connection.formatStringForSQL(prefix+"form_status_relationships", "next_form_status_id", Long.toString(formStatusId)), null);
            StringBuffer formStatusString = new StringBuffer();
            if (rs.getErrorStatus()) {
                return new CodaResponse(true, null, 8004, rs.getErrorString());
            } else {
                boolean first = true;
                while (rs.next()) {
                    if (first) {
                        first = false;
                    } else {
                        formStatusString.append(", ");
                    }
                    formStatusString.append(connection.formatStringForSQL(tableName, "status_id", rs.getData(0)));
                }
            }

            // Do the query to get all of the IDs to be updated
            try {
				CodaFromClause fromClause = new CodaFromClause(tableName);
				fromClause.setDatabase(database);
				rs = connection.runQuery("select id from " + tableName + " where " + (whereClause != null ?  whereClause.print(fromClause) + " and " : "") + " status_id in (" + formStatusString.toString() + ")", null);
            } catch (CodaException ex) {
                return new CodaResponse(true, null, 8004, "Something went wrong trying to figure out which records to update.");
            }
            if (rs.getErrorStatus()) {
                return new CodaResponse(true, null, 8004, rs.getErrorString());
            } else {
                // grab ids to insert
                Vector<Long> idsToUpdate = new Vector();
                while (rs.next()) {
                    idsToUpdate.add(rs.getDataLong(0));
                }

				if (idsToUpdate.size() > 0) {
					// create values hashtable
					Hashtable values = new Hashtable();
					Hashtable<String,Vector> arrayValues = new Hashtable();
					for (int i = 0; i < row.size(); i++) {
						if (fields.get(columns.get(i)).isArrayFlag()) {
							values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
							arrayValues.put(fields.get(columns.get(i)).getFieldName(), TypeParser.explodeArray(row.get(i)));
						} else {
							values.put(fields.get(columns.get(i)).getFieldName(), row.get(i));
						}
					}
					values.put("STATUS_ID", formStatusId);
					if (groupFlag && !values.containsKey("GROUP_ID")) {
						values.put("GROUP_ID", sessions.getSessionGroupId(sessionKey));
					}

					if (!values.containsKey("MOD_USER_NAME")) {
						values.put("MOD_USER_NAME", sessions.getSessionUsername(sessionKey));
					}
					if (!values.containsKey("MOD_DATE")) {
						values.put("MOD_DATE", new GregorianCalendar().getTimeInMillis());
					}

					// set the classloader for the execution context
					context.setClassLoader(deployedApplications.get(applicationName.toUpperCase()).getEnvironmentClassLoader(environmentId, context.getClassLoader()));

					// call before trigger
					Vector<Vector> updatingRecords = new Vector();
					CodaResultSet triggerResult = connection.runQuery("select id from "+prefix+"triggers where table_id = " +connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)) + " and before_flag = 1 and form_status_id = " + connection.formatStringForSQL(prefix+"triggers", "form_status_id", Long.toString(formStatusId)), null);
					if (!triggerResult.getErrorStatus() && triggerResult.next()) {
						CodaResultSet currentRows = connection.selectRows(tableName, "id", idsToUpdate);
						if (!currentRows.getErrorStatus()) {
							Vector<String> allColumnNames = currentRows.getColumnNames();
							while (currentRows.next()) {
								Hashtable rowHashtable = new Hashtable();
								for (int j = 0; j < allColumnNames.size(); j++) {
									rowHashtable.put(allColumnNames.get(j), currentRows.getData(j));
								}
								updatingRecords.add(this.prepareHashtablesForTrigger(context, serverConnection, fields, rowHashtable, values));
							}
						}
						for (Vector<Hashtable> i : updatingRecords) {
							try {
								deployedApplications.get(applicationName).runTrigger(context, tableName, formStatusVerb, true, new Database(this, sessionKey, connection), i.get(1), i.get(0));
							} catch (ClassNotFoundException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							} catch (IllegalAccessException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							} catch (InstantiationException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							} catch (CodaException e) {
								return new CodaResponse(true, null, 4002, e.getMessage());
							}
						}
					}

					// do update
					boolean success = connection.updateRows(tableName, "id", idsToUpdate, values);

					if (!success) {
						if (procConnection == null) {
							connection.rollback();
						}
						return new CodaResponse(true, null, 3005);
					} else {

						Enumeration enum1 = arrayValues.keys();
						for(Long id : idsToUpdate) {
							while (enum1.hasMoreElements()) {
								String key = (String)enum1.nextElement();
								connection.runStatement("delete from " + key + " where id = " + connection.formatStringForSQL(key, "id", Long.toString(id)));
								Vector<String> elts = arrayValues.get(key);
								for (String elt : elts) {
									Hashtable values2 = new Hashtable();
									values2.put("id", id);
									values2.put("value", elt);
									connection.insertRow(key, values2);
								}
							}
						}

						triggerResult = connection.runQuery("select id from "+prefix+"triggers where table_id = " +connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId)) + " and before_flag = 0 and form_status_id = " + connection.formatStringForSQL(prefix+"triggers", "form_status_id", Long.toString(formStatusId)), null);
						if (!triggerResult.getErrorStatus() && triggerResult.next()) {
							if (updatingRecords.size() == 0) {
								CodaResultSet currentRows = connection.selectRows(tableName, "id", idsToUpdate);
								if (!currentRows.getErrorStatus()) {
									Vector<String> allColumnNames = currentRows.getColumnNames();
									while (currentRows.next()) {
										Hashtable rowHashtable = new Hashtable();
										for (int j = 0; j < allColumnNames.size(); j++) {
											rowHashtable.put(allColumnNames.get(j), currentRows.getData(j));
										}
										updatingRecords.add(this.prepareHashtablesForTrigger(context, serverConnection, fields, rowHashtable, values));
									}
								}
							}
							for (Vector<Hashtable> i : updatingRecords) {
								try {
									deployedApplications.get(applicationName).runTrigger(context, tableName, formStatusVerb, false, new Database(this, sessionKey, connection), i.get(1), i.get(0));
								} catch (Exception e) {
									return new CodaResponse(true, null, 4002, e.getMessage());
								}
							}
						}
					}
				}
			}
        }

        if (procConnection == null) {
            connection.commit();
        }

        return new CodaResponse(false, "Success!", -1, null);
    }

    public CodaResponse commit (String sessionKey) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);
        CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
        if (database == null) {
            return new CodaResponse(true, null, 8003);
        }

        CodaConnection connection = database.getConnection();

        connection.commit();

        return new CodaResponse(false, "Success!", -1, null);
    }

    public CodaResponse rollback (String sessionKey) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);
        CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
        if (database == null) {
            return new CodaResponse(true, null, 8003);
        }

        CodaConnection connection = database.getConnection();

        connection.rollback();

        return new CodaResponse(false, "Success!", -1, null);
    }

    public CodaResponse select (String sessionKey, String selectClause, CodaFromClause fromClause, CodaSearchCondition whereClause, String groupByClause, String havingClause, String orderByClause, long top, long startingAt, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection;
        CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}

		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        Hashtable<String,Vector> acceptableStatus = new Hashtable();
        Vector<CodaTableNameAlias> tables = fromClause.getMappings();
        for (CodaTableNameAlias tableName : tables) {
            if (!this.isForm(connection, prefix, tableName.getTableName())) {
                if (!deployedApplications.hasTablePermission(applicationName, userId, (this.isGroupTable(connection, prefix, tableName.getTableName()) ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.TABLE_SELECT, tableName.getTableName())) {
                    return new CodaResponse(true, null, 9012);
                }
            } else {
                acceptableStatus.put(tableName.getAlias() == null ? tableName.getTableName() : tableName.getAlias(), deployedApplications.getFormStatusesForPermission(applicationName, userId, (this.isGroupTable(connection, prefix, tableName.getTableName()) ? sessions.getSessionGroupId(sessionKey) : -1), environmentId, Datasource.FORM_STATUS_VIEW, tableName.getTableName()));
            }
        }

        CodaResultSet rs = null;
        try {
			if (fromClause != null) {
				fromClause.setColumns(this, database);
			}
			String whereClauseAddition = "";
            boolean first = true;
            for (String alias : acceptableStatus.keySet()) {
                if (first) {
                    first = false;
                } else {
                    whereClauseAddition += " and ";
                }
                if (acceptableStatus.get(alias).size() == 0) {
                    whereClauseAddition += alias + ".status_id is null ";
                } else {
                    whereClauseAddition += alias + ".status_id in (";
                    for (int i = 0; i < acceptableStatus.get(alias).size(); i++) {
                        if (i != 0) {
                            whereClauseAddition += ", ";
                        }
                        whereClauseAddition += acceptableStatus.get(alias).get(i).toString();
                    }
                    whereClauseAddition += ") ";
                }

            }
            String sql = selectClause + (fromClause != null ? fromClause.print() : "") + (fromClause != null ? (whereClauseAddition != "" || whereClause != null ? " where " : "") + (whereClause != null ? whereClause.print(fromClause) +  (whereClauseAddition != "" ? " and " + whereClauseAddition : "")  : whereClauseAddition) : "" ) + (groupByClause != null ? groupByClause : "") + (havingClause != null ? havingClause : "") + (orderByClause != null ? orderByClause : "");
            rs = connection.runQuery(sql  , null, top, startingAt);
			if (rs.getErrorStatus()) {
				return new CodaResponse(true, null, 8004, rs.getErrorString() + " [" + sql + "]");
			} else {
				return new CodaResponse(rs);
			}
		} catch (Exception e) {
            return new CodaResponse(true, null, 8004, e.getMessage());
        }

    }

    public CodaResponse sysSelect (String sessionKey, String selectClause, CodaFromClause fromClause, CodaSearchCondition whereClause, String groupByClause, String havingClause, String orderByClause, long top, long startingAt) {
        if (!sessions.hasServerPermission(sessionKey, "QUERY_SYSTEM_TABLES")) {
            return new CodaResponse(true, null, 9017);
        }

        CodaConnection connection = database.getConnection();

        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }

        CodaResultSet rs = null;
        try {
			if (fromClause != null) {
				fromClause.setColumns(this, database);
			}
			String sql = selectClause + " " + (fromClause != null ? fromClause.print() : "") + (fromClause != null && whereClause != null ? " where " + (whereClause != null ? whereClause.print(fromClause) : "") : "") + (groupByClause != null ? groupByClause : "") + (havingClause != null ? havingClause : "") + (orderByClause != null ? orderByClause : "");
            rs = connection.runQuery(sql  , null, top, startingAt);
        	if (rs.getErrorStatus()) {
				return new CodaResponse(true, null, 8004, rs.getErrorString());
			} else {
				return new CodaResponse(rs);
			}
		} catch (Exception e) {
            return new CodaResponse(true, null, 8004, e.getMessage());
        }


    }

    public CodaResponse rawSelect (String sessionKey, String selectStatement, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection;
        CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}

		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        CodaResultSet rs = connection.runQuery(selectStatement, null);

        if (rs.getErrorStatus()) {
            return new CodaResponse(true, null, 8004, rs.getErrorString());
        } else {
            return new CodaResponse(rs);
        }
    }

    public CodaResponse rawStatement (String sessionKey, String sqlStatement, CodaConnection procConnection) {
        long userId = sessions.getSessionUserId(sessionKey);
        if (userId < 0) {
            return new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection;
        if (procConnection == null) {
            CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }

            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

		try {
			connection.runStatementWithException(sqlStatement);
            if (procConnection == null) {
                connection.commit();
            }
            return new CodaResponse(false, "Success!", -1, null);
		} catch (Exception e) {
			return new CodaResponse(true, null, 8004, e.getMessage());
        }
    }

    public CodaResponse createReplaceProcedure(String sessionKey, CodaDatabase database, String command, String applicationName, String procedureName, Vector<ProcedureParameter> parameters, String returnType, boolean returnsArrayFlag, String procedureBody, boolean loadClass) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            CodaConnection serverConnection = this.database.getConnection();
			boolean insertFlag = true, resultSetFlag = false;
            long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
            if (procedureId > 0) {
                if (command.equalsIgnoreCase("create")) {
                    return new CodaResponse(true, null, 2086);
                } else {
                    insertFlag = false;
                }
            }

            long typeId = -1;
            if (returnType.equalsIgnoreCase("resultset")) {
                resultSetFlag = true;
            } else {
                typeId = this.getIdForObjectName(serverConnection, returnType.toUpperCase(), CodaServer.OBJECT_TYPE_TYPE);
                if (typeId < 0) {
                    return new CodaResponse(true, null, 2020);
                }
            }

            // validate parameters
            Vector<String> parameterNames = new Vector();
            for (ProcedureParameter parameter : parameters) {
                if (parameterNames.contains(parameter.getParameterName())) {
                    return new CodaResponse(true, null, 2087, "Parameter '" + parameter.getParameterName() +"' declared multiple times");
                } else {
                    parameterNames.add(parameter.getParameterName());
                }

                if (this.getIdForObjectName(serverConnection, parameter.getParameterType(), CodaServer.OBJECT_TYPE_TYPE) < 0) {
                    return new CodaResponse(true, null, 2020, "Type name '" + parameter.getParameterType() + "' is invalid");
                }
            }

            String classFile = null;
            try {
                classFile = GroovyClassGenerator.getProcedureClass(procedureName, parameters, procedureBody);
            } catch (Exception e) {
                return new CodaResponse(true, null, 8005, "There was a problem with your procedure syntax:" +e.getMessage());
            }

            Hashtable values = new Hashtable();
            values.put("procedure_name", procedureName.toUpperCase());
            values.put("return_resultset_flag", resultSetFlag ? 1 : 0);
            values.put("return_type_name", returnType.toUpperCase());
            values.put("return_array_flag", returnsArrayFlag ? 1 : 0);
            values.put("class_file", classFile);
            values.put("procedure_language", "GROOVY");
            values.put("procedure_body", procedureBody);
            values.put("recompile_needed_flag", 0);
            if (procedureId < 0) {
                values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                values.put("create_date", new GregorianCalendar().getTimeInMillis());
            }
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

            if (procedureId < 0) {
                procedureId = connection.insertRow(prefix+"procedures", values);
            } else {
                connection.updateRow(prefix+"procedures", "id", procedureId, values);
            }

            connection.runStatement("delete from "+prefix+"procedure_parameters where procedure_id = " + connection.formatStringForSQL(prefix+"procedure_parameters", "procedure_id", Long.toString(procedureId)));

            values = new Hashtable();
            values.put("procedure_id", procedureId);
            int orderNumber = 0;
            for (ProcedureParameter parameter : parameters) {
                values.put("parameter_name", parameter.getParameterName().toUpperCase());
                values.put("order_number", orderNumber);
                values.put("type_name", parameter.getParameterType().toUpperCase());
                values.put("array_flag", parameter.isArrayFlag() ? 1 : 0);
                connection.insertRow(prefix+"procedure_parameters", values);

                orderNumber++;
            }

            if (loadClass) {
                this.deployedApplications.get(applicationName).loadClass(1, "org.codalang.codaserver.language.procedures." + CodaServer.camelCapitalize(procedureName, true),classFile);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
             return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse alterProcedure(String sessionKey, CodaDatabase database, String applicationName, String procedureName, String newProcedureName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
            if (procedureId < 0) {
                return new CodaResponse(true, null, 2088);
            }

            if (this.getIdForObjectName(newProcedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection) > 0) {
                return new CodaResponse(true, null, 2086);
            }

            Hashtable values = new Hashtable();
            values.put("procedure_name", newProcedureName.toUpperCase());
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            connection.updateRow(prefix+"procedures", "id", procedureId, values);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
             return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse dropProcedure(String sessionKey, CodaDatabase database, String applicationName, String procedureName) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
            if (procedureId < 0) {
                return new CodaResponse(true, null, 2088);
            }

            connection.runStatement("delete from "+prefix+"procedure_parameters where procedure_id = " + connection.formatStringForSQL(prefix+"procedure_parameters", "procedure_id", Long.toString(procedureId)));
            connection.deleteRow(prefix+"procedures", "id", procedureId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
             return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse createReplaceTrigger(String sessionKey, CodaDatabase database, String command, String applicationName, String tableName, String operation, boolean beforeFlag, String triggerBody, boolean loadClass) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            boolean insertFlag = true, resultSetFlag = false;
            boolean formFlag = this.isForm(connection, prefix, tableName);
            long operationId = -1, formStatusId = -1;

            // get tableId
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }

            // get operation id
            if (formFlag) {
                formStatusId = this.getIdForObjectName(tableName + "." + operation, CodaServer.OBJECT_TYPE_FORM_STATUS_VERB, connection);
                if (formStatusId < 0 && !operation.equalsIgnoreCase("update")) {
                    return new CodaResponse(true, null, 2051);
                } else if (formStatusId < 0) {
                    operationId = 1;
                }
            } else {
                if (operation.equalsIgnoreCase("update")) {
                    operationId = 1;
                } else if (operation.equalsIgnoreCase("insert")) {
                    operationId = 2;
                } else if (operation.equalsIgnoreCase("delete")) {
                    operationId = 3;
                } else {
                    return new CodaResponse(true, null, 2094);
                }
            }

            long triggerId = this.getIdForObjectName(tableId + "." + operationId + "." + (formFlag ? formStatusId + "." : "") + (beforeFlag ? "1" : "0"), (formFlag ? CodaServer.OBJECT_TYPE_FORM_TRIGGER : CodaServer.OBJECT_TYPE_TABLE_TRIGGER), connection);
            if (triggerId > 0) {
                if (command.equalsIgnoreCase("insert")) {
                    return new CodaResponse(true, null, 2086);
                } else {
                    insertFlag = false;
                }
            }

            String classFile = null;
            try {
                classFile = GroovyClassGenerator.getTriggerClass(tableName, operation, (beforeFlag ? "before" : "after"), triggerBody);
            } catch (Exception e) {
                return new CodaResponse(true, null, 8006, "There was a problem with your trigger syntax:" +e.getMessage());
            }

            Hashtable values = new Hashtable();
            values.put("table_id", tableId);
			if (formStatusId > 0) {
				values.put("form_status_id", formStatusId);
			}
			if (operationId > 0) {
				values.put("operation_id", operationId);
			}
			values.put("before_flag", beforeFlag ? 1 : 0);
            values.put("class_file", classFile);
            values.put("procedure_language", "GROOVY");
            values.put("procedure_body", triggerBody);
            values.put("recompile_needed_flag", 0);
            if (triggerId < 0) {
                values.put("create_user_name", sessions.getSessionUsername(sessionKey));
                values.put("create_date", new GregorianCalendar().getTimeInMillis());
            }
            values.put("mod_user_name", sessions.getSessionUsername(sessionKey));
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());

            if (triggerId < 0) {
                triggerId = connection.insertRow(prefix+"triggers", values);
            } else {
                connection.updateRow(prefix+"triggers", "id", triggerId, values);
            }

            if (loadClass) {
                this.deployedApplications.get(applicationName).loadClass(1, "org.codalang.codaserver.language.triggers." + CodaServer.camelCapitalize(tableName, true) + CodaServer.camelCapitalize(beforeFlag ? "before" : "after", true) + CodaServer.camelCapitalize(operation, true),classFile);
            }

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
             return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse dropTrigger(String sessionKey, CodaDatabase database, String applicationName, String tableName, String operation, boolean beforeFlag) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        if (applicationName == null) {
            if (sessions.getSessionApplication(sessionKey) != null) {
                applicationName = sessions.getSessionApplication(sessionKey);
            } else {
                return new CodaResponse(true, null, 2027);
            }
        }
        if (database == null) {
            unlinkedDatabaseFlag = false;
            database = deployedApplications.getDatasource(applicationName, 1).getDatabase();
            if (database == null) {
                return new CodaResponse(true, null, 8002);
            }
        }

        CodaConnection connection = database.getConnection();

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }
        if (deployedApplications.hasApplicationPermission(applicationName, modUserId, -1, -1, "DEVELOPER") || unlinkedDatabaseFlag) {
            boolean formFlag = this.isForm(connection, prefix, tableName);
            long operationId = -1, formStatusId = -1;

            // get tableId
            long tableId = this.getIdForObjectName(tableName, CodaServer.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }

            // get operation id
            if (formFlag) {
                formStatusId = this.getIdForObjectName(tableName + "." + operation, CodaServer.OBJECT_TYPE_FORM_STATUS_VERB, connection);
                if (formStatusId < 0 && !operation.equalsIgnoreCase("update")) {
                    return new CodaResponse(true, null, 2051);
                } else if (formStatusId < 0) {
                    operationId = 1;
                }
            } else {
                if (operation.equalsIgnoreCase("update")) {
                    operationId = 1;
                } else if (operation.equalsIgnoreCase("insert")) {
                    operationId = 2;
                } else if (operation.equalsIgnoreCase("delete")) {
                    operationId = 3;
                } else {
                    return new CodaResponse(true, null, 2094);
                }
            }

            long triggerId = this.getIdForObjectName(tableId + "." + operationId + "." + (formFlag ? formStatusId + "." : "") + (beforeFlag ? "1" : "0"), (formFlag ? CodaServer.OBJECT_TYPE_FORM_TRIGGER : CodaServer.OBJECT_TYPE_TABLE_TRIGGER), connection);
            if (triggerId < 0) {
                return new CodaResponse(true, null, 2090);
            }

            connection.deleteRow(prefix+"triggers", "id", triggerId);

            connection.commit();
            return new CodaResponse(false, "Success!", -1, null);
        } else {
             return new CodaResponse(true, null, 9009);
        }
    }

    public CodaResponse execProcedure(String sessionKey, String procedureName, Vector<String> parameters, CodaConnection procConnection) {
        boolean unlinkedDatabaseFlag = true;
        long modUserId = sessions.getSessionUserId(sessionKey);
        if (modUserId < 0) {
            return  new CodaResponse(true, null, 1005);
        }
        int environmentId = sessions.getSessionEnvironmentId(sessionKey);
        if (environmentId <= 0) {
            return new CodaResponse(true, null, 1006);
        }
        String applicationName = sessions.getSessionApplication(sessionKey);

        CodaConnection connection, serverConnection;
        CodaDatabase database = deployedApplications.getDatasource(applicationName, environmentId).getDatabase();
		if (database == null) {
			return new CodaResponse(true, null, 8003);
		}

		serverConnection = this.database.getConnection();
		if (procConnection == null) {
            connection = database.getConnection();
        } else {
            connection = procConnection;
        }

        // get the prefix
        String prefix = connection.getMetadata().getSystemTable().getPrefixString();
        if (prefix == null) {
            prefix = "";
        }

        if (!deployedApplications.hasProcedurePermission(applicationName, modUserId, -1, environmentId, Datasource.PROCEDURE_EXECUTE, procedureName.toUpperCase())) {
            return new CodaResponse(true, null, 9011);
        }

        long procedureId = this.getIdForObjectName(procedureName, CodaServer.OBJECT_TYPE_PROCEDURE, connection);
        if (procedureId < 0) {
            return new CodaResponse(true, null, 2088);
        }

        // figure out the return value
        boolean returnArrayFlag, returnResultSetFlag;
        long returnTypeId;
        CodaResultSet returnResult = connection.runQuery("select return_type_name, return_array_flag, return_resultset_flag from "+prefix+"procedures where id = " + connection.formatStringForSQL(prefix+"procedures", "id", Long.toString(procedureId)), null);
        if (!returnResult.getErrorStatus() && returnResult.next()) {
            returnTypeId = this.getIdForObjectName(serverConnection, returnResult.getData(0), this.OBJECT_TYPE_TYPE);
            returnArrayFlag = returnResult.getDataBoolean(1);
            returnResultSetFlag = returnResult.getDataBoolean(2);
        } else {
            return new CodaResponse(true, null, 2088);
        }


        // check the inputs
        ExecutionContext context = new ExecutionContext(this, applicationName, environmentId);

        Vector<Long> procedureParameterIds = new Vector();
        Vector<Long> procedureTypeIds = new Vector();
        Vector<Boolean> procedureArrayFlags = new Vector();
        Vector<String> parameterNames = new Vector();
        CodaResultSet rs = connection.runQuery("select pp.type_name, pp.array_flag, pp.id, pp.parameter_name from "+prefix+"procedure_parameters pp where pp.procedure_id = " + connection.formatStringForSQL(prefix+"procedure_parameters", "procedure_id", Long.toString(procedureId)) + " order by order_number asc",null);
        if (!rs.getErrorStatus()) {
            while (rs.next()) {
                procedureTypeIds.add(this.getIdForObjectName(serverConnection, rs.getData(0), this.OBJECT_TYPE_TYPE));
                procedureArrayFlags.add(rs.getData(1).equals("1"));
                procedureParameterIds.add(rs.getDataLong(2));
                parameterNames.add(rs.getData(3));
            }
        }

        if (procedureTypeIds.size() != parameters.size()) {
            return new CodaResponse(true, null, 2058);
        }

        if (procedureTypeIds.size() > 0) {

            CodaResponse resp = new CodaResponse();

            for(int i = 0; i < procedureTypeIds.size(); i++) {
                if (procedureArrayFlags.get(i).booleanValue()) {
                    if (TypeParser.isArray(parameters.get(i))) {
                        Vector<String> arrayValues = TypeParser.explodeArray(parameters.get(i));
                        for (String arrayValue : arrayValues) {
                            if (!context.validate(procedureTypeIds.get(i), arrayValue)){
                                resp.addError(new CodaError(3001, "Parameter " + (i + 1) + " contains an invalid " + context.getDisplayName(procedureTypeIds.get(i))));
                            }
                        }
                    }
                } else {
                    if (!context.validate(procedureTypeIds.get(i), parameters.get(i))){
                        resp.addError(new CodaError(3001, "Parameter " + (i + 1) + " is not a valid " + context.getDisplayName(procedureTypeIds.get(i))));
                    }
                }
            }

            if (resp.getError()) {
                return resp;
            }
        }

        GroovyClassLoader executingClassLoader = context.getClassLoader();
        if (executingClassLoader == null) {
            return new CodaResponse(true, null, 8003);
        } else {
            try {
                Class procedureClass = executingClassLoader.loadClass("org.codalang.codaserver.language.procedures." + CodaServer.camelCapitalize(procedureName, true));
                BaseCodaProcedure procedure = (BaseCodaProcedure) procedureClass.newInstance();

                // create the parameter hashtable
                Hashtable parameterHashtable = new Hashtable();
                for(int i = 0; i < parameterNames.size(); i++) {
                    if (procedureArrayFlags.get(i).booleanValue()) {
                        parameterHashtable.put(parameterNames.get(i), TypeParser.explodeArray(parameters.get(i)));
                    } else {
                        parameterHashtable.put(parameterNames.get(i), context.parse(procedureTypeIds.get(i), parameters.get(i)));
                    }
                }

                Object returnValue = procedure.execute(new Database(this, sessionKey), parameterHashtable);
                if (returnArrayFlag) {
                    try {
                        Vector returnVector = new Vector();
                        List list = (List)returnValue;
                        for (Object item : list) {
                            if (returnResultSetFlag) {
                                try {
                                    CodaResultSet temp = (CodaResultSet)item;
                                    returnVector.add(temp);
                                } catch (ClassCastException e) {
                                    return new CodaResponse(true, null, 3003);
                                }
                            } else {
                                try {
                                    if (!context.validate(returnTypeId, (String)item)) {
                                        return new CodaResponse(true, null, 3004);
                                    } else {
                                        returnVector.add((String)item);
                                    }
                                } catch (ClassCastException e) {
                                    return new CodaResponse(true, null, 3004);
                                }
                            }
                        }
                        if (procConnection == null) {
                            connection.commit();
                        }
                        return new CodaResponse(returnVector);
                    } catch (ClassCastException e) {
                        return new CodaResponse(true, null, 3002);
                    }
                } else {
                    if (returnResultSetFlag) {
                        try {
                            CodaResultSet temp = (CodaResultSet)returnValue;
                            if (procConnection == null) {
                                connection.commit();
                            }
                            return new CodaResponse(temp);
                        } catch (ClassCastException e) {
                            return new CodaResponse(true, null, 3003);
                        }
                    } else {
                        try {
                            if (!context.validate(returnTypeId, returnValue.toString())) {
                                return new CodaResponse(true, null, 3004);
                            } else {
                                if (procConnection == null) {
                                    connection.commit();
                                }
                                return new CodaResponse(returnValue.toString());
                            }
                        } catch (ClassCastException e) {
                            return new CodaResponse(true, null, 3004);
                        }
                    }
                }


            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, "Classloader could not find 'org.codalang.codaserver.language.procedures." + CodaServer.camelCapitalize(procedureName, true)+"' in application " + applicationName + ":" +environmentId);
                return new CodaResponse(true, null, 2091);
            } catch (InstantiationException ex) {
                logger.log(Level.SEVERE, "Classloader could not find 'org.codalang.codaserver.language.procedures." + CodaServer.camelCapitalize(procedureName, true)+"' in application " + applicationName + ":" +environmentId);
                return new CodaResponse(true, null, 2091);
            } catch (IllegalAccessException ex) {
                logger.log(Level.SEVERE, "Classloader could not find 'org.codalang.codaserver.language.procedures." + CodaServer.camelCapitalize(procedureName, true)+"' in application " + applicationName + ":" +environmentId);
                return new CodaResponse(true, null, 2091);
            } catch (CodaException e) {
                return new CodaResponse(true, null, 4001, e.getMessage());
            }

        }
    }

    public CodaResponse show (String sessionKey, String entityType, CodaSearchCondition whereClause, CodaOrderByClause orderByClause, String tableName, String userName, String groupName, String roleName, String applicationName, String environment) {
        CodaResultSet rs = null;
        String sql = "";
        long groupId = -1, userId = -1, roleId = -1;
		CodaConnection serverConnection = database.getConnection();
		if (groupName != null) {
            groupId = this.getIdForObjectName(serverConnection, groupName, this.OBJECT_TYPE_GROUP);
            if (groupId < 0) {
                return new CodaResponse(true, null, 2017);
            }
        }
        if (userName != null) {
            userId = this.getIdForObjectName(serverConnection, userName, this.OBJECT_TYPE_USER);
            if (userId < 0) {
                return new CodaResponse(true, null, 2015, "Username is invalid");
            }
        }
        if (entityType.equalsIgnoreCase("users")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("users", "obj"));
                    temp.add(new CodaTableNameAlias("users", "c"));
                    temp.add(new CodaTableNameAlias("users", "m"));
					CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.user_name, obj.first_name, obj.middle_name, obj.last_name, obj.organization, obj.address, obj.city, obj.state_prov, obj.postal_code, obj.country, obj.phone, obj.alt_phone, obj.email, obj.robot_flag, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from users obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            if (!sessions.hasServerPermission(sessionKey, "MANAGE_USERS") && !sessions.hasServerPermission(sessionKey, "MANAGE_USER_DATA") && !sessions.hasServerPermission(sessionKey, "MANAGE_GROUPS") && !sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
                applicationName = sessions.getSessionApplication(sessionKey);
                environment = sessions.getSessionEnvironment(sessionKey);
                if (environment == null) {
                    return new CodaResponse(true, null, 9003);
                }
            }
            if (environment != null) {
               sql += " inner join user_application_permissions uap on uap.user_id = obj.id and uap.application_permission_name = 'CONNECT' and (uap.environment = "+ serverConnection.formatStringForSQL("user_application_permissions", "environment", Integer.toString(this.getIdForEnvironmentName(environment))) +" OR uap.environment IS NULL) "+(groupId > 0 ? " and aup.group_id = " + serverConnection.formatStringForSQL("user_application_permissions", "group_id", Long.toString(groupId)) + " " : "")+" inner join applications a on uap.application_id = a.id and a.application_name = " + serverConnection.formatStringForSQL("applications", "application_name", applicationName.toUpperCase()) + " ";
            }
            if (environment == null && groupId > 0) {
                sql += " inner join user_groups g on obj.id = g.user_id and g.group_id = " + serverConnection.formatStringForSQL("user_groups", "id", Long.toString(groupId)) + " ";
            }
            sql += " where obj.active_flag = 1 " + (whereClauseString != null ? " and " + whereClauseString : "") + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("groups")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("groups", "obj"));
                    temp.add(new CodaTableNameAlias("users", "c"));
                    temp.add(new CodaTableNameAlias("users", "m"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.group_name, obj.display_name, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from groups obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            if (!sessions.hasServerPermission(sessionKey, "MANAGE_GROUPS")) {
                applicationName = sessions.getSessionApplication(sessionKey);
                environment = sessions.getSessionEnvironment(sessionKey);
                if (environment == null) {
                    return new CodaResponse(true, null, 1006);
                }
            }
            if (environment != null) {
               sql += " inner join group_applications ga on ga.group_id = obj.id inner join applications a on a.id = ga.application_id and a.application_name = " + serverConnection.formatStringForSQL("applications", "application_name", applicationName)+" ";
            }
			if (userName != null) {
               sql += " inner join user_groups ug on ug.group_id = obj.id inner join users u on u.id = ug.user_id and u.user_name = " + serverConnection.formatStringForSQL("users", "user_name", userName)+" ";
            }
			sql += " where obj.active_flag = 1 " + (whereClauseString != null ? " and " + whereClauseString : "") + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("types")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("types", "obj"));
                    temp.add(new CodaTableNameAlias("users", "c"));
                    temp.add(new CodaTableNameAlias("users", "m"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.type_name, obj.built_in_flag, obj.validation_mask, obj.save_mask, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from types obj left outer join users c on c.id = obj.create_user_id left outer join users m on m.id = obj.mod_user_id ";
            sql += " where obj.active_flag = 1 " + (whereClauseString != null ? " and " + whereClauseString : "") + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("datasources")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("datasources", "obj"));
                    temp.add(new CodaTableNameAlias("users", "c"));
                    temp.add(new CodaTableNameAlias("users", "m"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.datasource_name, obj.display_name, obj.driver_name, obj.host_name, obj.schema_name, obj.user_name, obj.pass_word, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from datasources obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "") + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("sessions")) {
            if (!sessions.hasServerPermission(sessionKey, "MANAGE_SESSIONS")) {
                return new CodaResponse(true, null, 9016);
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("sessions", "obj"));
                    temp.add(new CodaTableNameAlias("users", "c"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.session_key, u.user_name, obj.application_name, obj.group_name, obj.environment, obj.session_timestamp from sessions obj inner join users u on u.id = obj.user_id ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "") + (orderByClause == null ? " order by obj.id asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("applications")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("applications", "obj"));
                    temp.add(new CodaTableNameAlias("datasources", "dev"));
                    temp.add(new CodaTableNameAlias("datasources", "test"));
                    temp.add(new CodaTableNameAlias("datasources", "prod"));
                    temp.add(new CodaTableNameAlias("users", "c"));
                    temp.add(new CodaTableNameAlias("users", "m"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.application_name, obj.display_name, obj.group_flag, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name, dev.datasource_name, test.datasource_name, prod.datasource_name from applications obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id left outer join datasources dev on dev.id = obj.dev_datasource_id left outer join datasources test on test.id = obj.test_datasource_id left outer join datasources prod on prod.id = obj.prod_datasource_id ";
            if (groupId > 0) {
                sql += " inner join group_applications g on obj.id = g.application_id and g.group_id = " + serverConnection.formatStringForSQL("user_groups", "id", Long.toString(groupId)) + " ";
            }
            sql += " where obj.active_flag = 1 " + (whereClauseString != null ? " and " + whereClauseString : "") + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("server")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("user_system_permissions", "obj"));
                    temp.add(new CodaTableNameAlias("users", "u"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select u.user_name, obj.server_permission_name from user_server_permissions obj inner join users u on u.id = obj.user_id and u.id = "+ serverConnection.formatStringForSQL("users", "id", Long.toString(userId)) + " ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by u.user_name asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("application")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("user_application_permissions", "obj"));
                    temp.add(new CodaTableNameAlias("applications", "a"));
                    temp.add(new CodaTableNameAlias("groups", "g"));
                    temp.add(new CodaTableNameAlias("users", "u"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select u.user_name, g.group_name, a.application_name, obj.environment, obj.application_permission_name from user_application_permissions obj inner join users u on u.id = obj.user_id " + ( userId > 0 ? " and u.id = "+ serverConnection.formatStringForSQL("users", "id", Long.toString(userId)) : "") +" inner join applications a on a.id = obj.application_id and a.active_flag = 1 left outer join groups g on g.id = obj.group_id " + (groupId > 0 ? " and g.id = " + serverConnection.formatStringForSQL("groups", "id", Long.toString(groupId)) : "");
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by u.user_name asc" : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("sys")) {
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("coda_system_information", "obj"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.* from coda_system_information obj ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " " : orderByClause.toString());
            rs = serverConnection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("app")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias("coda_system_information", "obj"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.* from coda_system_information obj ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? "" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("tables")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }

            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"tables", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"tables", "p"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.table_name, obj.display_name, obj.group_flag, obj.soft_delete_flag, obj.ref_table_flag, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date, p.table_name as parent_table_name from "+prefix+"tables obj left outer join "+prefix+"tables p on p.id = obj.parent_table_id ";
            sql += " where obj.form_flag = 0 " + (whereClauseString != null ? " and " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("forms")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"tables", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"tables", "p"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.table_name as form_name, obj.display_name, obj.group_flag, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date, p.table_name as parent_form_name from "+prefix+"tables obj left outer join "+prefix+"tables p on p.id = obj.parent_table_id ";
            sql += " where obj.form_flag = 1 " + (whereClauseString != null ? " and " + whereClauseString : "") + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("procedures")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"procedures", "obj"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.procedure_name, obj.return_resultset_flag, obj.return_array_flag, obj.return_type_name, obj.procedure_body, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"procedures obj ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("triggers")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"triggers", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"tables", "p"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            long tableId = this.getIdForObjectName(tableName, this.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }
            boolean formFlag = this.isForm(connection, prefix, tableName);
            if (!formFlag) {
                sql = "select p.table_name, obj.operation_id, obj.before_flag, obj.procedure_body as trigger_body, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"triggers obj inner join "+prefix+"tables p on p.id = obj.table_id ";
            } else {
                sql = "select p.table_name as form_name, obj.operation_id, fs.verb_status_name, obj.before_flag, obj.procedure_body, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"tables obj inner join "+prefix+"tables p on p.id = obj.table_id left outer join "+prefix+"form_statuses fs on fs.id = obj.form_status_id ";
            }
            sql += " where obj.table_id = "+connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId))+" " + (whereClauseString != null ? " and " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("indexes")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"indexes", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"tables", "p"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.index_name, p.table_name, obj.index_type_id, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"indexes obj left outer join "+prefix+"tables p on p.id = obj.table_id ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("crons")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"crons", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"procedures", "p"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.cron_name, obj.minute_part, obj.hour_part, obj.day_of_month_part, obj.month_part, obj.day_of_week_part, p.procedure_name, obj.executing_user_name, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"crons obj left outer join "+prefix+"procedures p on p.id = obj.procedure_id ";
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("roles")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"roles", "obj"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            sql = "select obj.role_name, obj.display_name, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"roles obj ";
            if (userId > 0) {
                sql += " inner join "+prefix+"user_roles r on obj.id = r.role_id and r.user_id = " + connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " " + (groupId > 0 ?"and r.group_id = " + connection.formatStringForSQL(prefix+"user_roles", "group_id", Long.toString(groupId)) + " " : "");
            }
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("permissions")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"permissions", "obj"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            if (roleName != null) {
                roleId = this.getIdForObjectName(roleName, this.OBJECT_TYPE_ROLE, connection);
            }
            sql = "select obj.permission_name, obj.display_name, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"permissions obj ";
            if (userId > 0) {
                sql += " inner join "+prefix+"role_permissions p on obj.id = p.permission_id inner join "+prefix+"user_roles r on p.role_id = r.role_id and r.user_id = " + connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " " + (groupId > 0 ?"and r.group_id = " + connection.formatStringForSQL(prefix+"user_roles", "group_id", Long.toString(groupId)) + " " : "");
            } else if (roleId > 0) {
                sql += " inner join "+prefix+"role_permissions r on obj.id = r.permission_id and r.role_id = " + connection.formatStringForSQL(prefix+"role_permissions", "role_id", Long.toString(roleId)) + " ";
            }
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by obj.create_date asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("table")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            if (tableName != null && this.isForm(connection, prefix, tableName)) {
                return new CodaResponse(true, null, 2092);
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"role_tables", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"roles", "r"));
                    temp.add(new CodaTableNameAlias(prefix +"tables", "t"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            if (roleName != null) {
                roleId = this.getIdForObjectName(roleName, this.OBJECT_TYPE_ROLE, connection);
                if (roleId == -1) {
                     return new CodaResponse(true, null, 2029);
                }
            }
            sql = "select r.role_name, t.table_name, obj.select_flag, obj.insert_flag, obj.update_flag, obj.delete_flag from "+prefix+"role_tables obj inner join "+prefix+"roles r on r.id = obj.role_id " + (roleId > 0 ? "and r.id = " + connection.formatStringForSQL(prefix + "roles", "id", Long.toString(roleId)) : "") + " inner join "+prefix+"tables t on t.id = obj.table_id and t.form_flag = 0 ";
            if (userId > 0) {
                sql += " inner join "+prefix+"user_roles u on r.id = u.role_id and u.user_id = " + connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " " + (groupId > 0 ?"and u.group_id = " + connection.formatStringForSQL(prefix+"user_roles", "group_id", Long.toString(groupId)) + " " : "");
            }
            sql += (whereClauseString != null ? " where " + whereClauseString : "") + (orderByClause == null ? " order by r.role_name asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("form")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            if (tableName != null && !this.isForm(connection, prefix, tableName)) {
                return new CodaResponse(true, null, 2092);
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"role_form_statuses", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"roles", "r"));
                    temp.add(new CodaTableNameAlias(prefix +"tables", "f"));
                    temp.add(new CodaTableNameAlias(prefix +"form_statuses", "s"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            if (roleName != null) {
                roleId = this.getIdForObjectName(roleName, this.OBJECT_TYPE_ROLE, connection);
                if (roleId == -1) {
                     return new CodaResponse(true, null, 2029);
                }
            }
            sql = "select r.role_name, f.table_name as form_name, s.adj_status_name, s.verb_status_name, obj.view_flag, obj.call_flag, obj.update_flag from "+prefix+"role_form_statuses obj inner join "+prefix+"form_statuses s on s.id = obj.form_status_id inner join "+prefix+"roles r on r.id = obj.role_id " + (roleId > 0 ? "and r.id = " + connection.formatStringForSQL(prefix + "roles", "id", Long.toString(roleId)) : "") + " inner join "+prefix+"tables f on f.id = s.table_id  and f.form_flag = 1  ";
            if (userId > 0) {
                sql += " inner join "+prefix+"user_roles u on r.id = u.role_id and u.user_id = " + connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " " + (groupId > 0 ?"and u.group_id = " + connection.formatStringForSQL(prefix+"user_roles", "group_id", Long.toString(groupId)) + " " : "");
            }
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by r.role_name asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("procedure")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (whereClause != null) {
                try {
                    Vector<CodaTableNameAlias> temp = new Vector();
                    temp.add(new CodaTableNameAlias(prefix +"role_procedures", "obj"));
                    temp.add(new CodaTableNameAlias(prefix +"roles", "r"));
                    temp.add(new CodaTableNameAlias(prefix +"procedures", "p"));
                    CodaFromClause fromClause =  new CodaFromClause(temp, new Vector());
					fromClause.setColumns(this, database);
					whereClauseString = whereClause.print(fromClause);
                } catch (CodaException e) {
                    return new CodaResponse(true, null, 8004, e.getMessage());
                }
            }
            if (roleName != null) {
                roleId = this.getIdForObjectName(roleName, this.OBJECT_TYPE_ROLE, connection);
                if (roleId == -1) {
                     return new CodaResponse(true, null, 2029);
                }
            }
            sql = "select r.role_name, p.procedure_name, obj.execute_flag from "+prefix+"role_procedures obj inner join "+prefix+"roles r on r.id = obj.role_id " + (roleId > 0 ? "and r.id = " + connection.formatStringForSQL(prefix + "roles", "id", Long.toString(roleId)) : "") + " inner join "+prefix+"procedures p on p.id = obj.procedure_id ";
            if (userId > 0) {
                sql += " inner join "+prefix+"user_roles u on r.id = u.role_id and u.user_id = " + connection.formatStringForSQL(prefix+"user_roles", "user_id", Long.toString(userId)) + " " + (groupId > 0 ?"and u.group_id = " + connection.formatStringForSQL(prefix+"user_roles", "group_id", Long.toString(groupId)) + " " : "");
            }
            sql += (whereClauseString != null ? " where " + whereClauseString : "")  + (orderByClause == null ? " order by r.role_name asc" : orderByClause.toString());
            rs = connection.runQuery(sql, null);
        }

        return new CodaResponse(rs);
    }

    public CodaResponse describe (String sessionKey, String entityType, String entityName, String specifier, boolean beforeFlag, String operation) {
        CodaResultSet rs = null;
        String sql = "";
        String applicationName = null;
        String environment = null;
        long groupId = -1;
        if (entityType.equalsIgnoreCase("user")) {
            CodaConnection connection = database.getConnection();
            sql = "select obj.user_name, obj.first_name, obj.middle_name, obj.last_name, obj.organization, obj.address, obj.city, obj.state_prov, obj.postal_code, obj.country, obj.phone, obj.alt_phone, obj.email, obj.robot_flag, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from users obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            if (!sessions.hasServerPermission(sessionKey, "MANAGE_USERS") && !sessions.hasServerPermission(sessionKey, "MANAGE_USER_DATA") && !sessions.hasServerPermission(sessionKey, "MANAGE_GROUPS") && !sessions.hasServerPermission(sessionKey, "MANAGE_APPLICATIONS")) {
                applicationName = sessions.getSessionApplication(sessionKey);
                groupId = sessions.getSessionGroupId(sessionKey);
                environment = sessions.getSessionEnvironment(sessionKey);
                if (environment == null) {
                    return new CodaResponse(true, null, 1006);
                }
            }
            if (environment != null) {
               sql += " inner join user_application_permissions uap on uap.user_id = obj.id and uap.application_permission_name = 'CONNECT' and ( uap.environment = "+ connection.formatStringForSQL("user_application_permissions", "environment", Integer.toString(this.getIdForEnvironmentName(environment))) +" OR uap.environment IS NULL) "+(groupId > 0 ? " and aup.group_id = " + connection.formatStringForSQL("user_application_permissions", "group_id", Long.toString(groupId)) + " " : "")+" inner join applications a on uap.application_id = a.id and a.application_name = " + connection.formatStringForSQL("applications", "application_name", applicationName.toUpperCase()) + " ";
            }
            sql += " where obj.active_flag = 1 and obj.user_name = " + connection.formatStringForSQL("users", "user_name", entityName.toUpperCase());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("group")) {
            CodaConnection connection = database.getConnection();
            sql = "select obj.group_name, obj.display_name, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from groups obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            sql += " where obj.active_flag = 1 and obj.group_name = " + connection.formatStringForSQL("groups", "group_name", entityName.toUpperCase());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("type")) {
            CodaConnection connection = database.getConnection();
            sql = "select obj.type_name, obj.built_in_flag, obj.validation_mask, obj.save_mask, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from types obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            sql += " where obj.active_flag = 1 and obj.type_name = " + connection.formatStringForSQL("types", "type_name", entityName.toUpperCase());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("datasource")) {
            CodaConnection connection = database.getConnection();
            sql = "select obj.datasource_name, obj.display_name, obj.driver_name, obj.host_name, obj.schema_name, obj.user_name, obj.pass_word, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name from datasources obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id ";
            sql += " where obj.datasource_name = " + connection.formatStringForSQL("datasources", "datasource_name", entityName.toUpperCase());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("application")) {
            CodaConnection connection = database.getConnection();
            sql = "select obj.application_name, obj.display_name, obj.group_flag, obj.create_date, obj.mod_date, c.user_name as create_user_name, m.user_name as mod_user_name, dev.datasource_name, test.datasource_name, prod.datasource_name from applications obj inner join users c on c.id = obj.create_user_id inner join users m on m.id = obj.mod_user_id left outer join datasources dev on dev.id = obj.dev_datasource_id left outer join datasources test on test.id = obj.test_datasource_id left outer join datasources prod on prod.id = obj.prod_datasource_id ";
            sql += " where obj.active_flag = 1 and obj.application_name = " + connection.formatStringForSQL("applications", "application_name", entityName.toUpperCase());
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("table")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();
            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            if (this.isForm(connection, prefix, entityName)) {
                return new CodaResponse(true, null, 2039);
            }
            if (specifier != null && specifier.equalsIgnoreCase("columns")) {
                sql = "select obj.field_name as column_name, obj.display_name, obj.type_name, obj.array_flag, obj.nullable_flag, r.table_name as ref_table_name, obj.default_variable_id, obj.default_value, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"table_fields obj inner join "+prefix+"tables t on t.id = obj.table_id left outer join " +prefix +"tables r on obj.ref_table_id = r.id ";
                sql += " where t.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", entityName.toUpperCase());
            } else {
                sql = "select obj.table_name, obj.display_name, obj.group_flag, obj.soft_delete_flag, obj.ref_table_flag, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date, p.table_name as parent_table_name from "+prefix+"tables obj left outer join "+prefix+"tables p on p.id = obj.parent_table_id ";
                sql += " where obj.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", entityName.toUpperCase());
            }
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("form")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            if (!this.isForm(connection, prefix, entityName)) {
                return new CodaResponse(true, null, 2092);
            }
            if (specifier != null && specifier.equalsIgnoreCase("fields")) {
                sql = "select obj.field_name, obj.display_name, obj.type_name, obj.array_flag,  r.table_name as ref_table_name, obj.default_variable_id, obj.default_value, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"table_fields obj inner join "+prefix+"tables t on t.id = obj.table_id left outer join " +prefix +"tables r on obj.ref_table_id = r.id ";
                sql += " where t.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", entityName.toUpperCase());
            } else if (specifier != null && specifier.equalsIgnoreCase("statuses")) {
                sql = "select obj.id, obj.adj_status_name, obj.adj_display_name, obj.verb_status_name, obj.verb_display_name, obj.initial_flag, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date, t.table_name as parent_table_name from "+prefix+"form_statuses obj inner join "+prefix+"tables t on t.id = obj.table_id ";
                sql += " where t.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", entityName.toUpperCase());
            } else if (specifier != null && specifier.equalsIgnoreCase("status")) {
                sql = "select orig.id as orig_id, orig.adj_status_name as orig_adj_status_name, orig.verb_status_name as orig_verb_status_name, next.id as next_id, next.adj_status_name as next_adj_status_name, next.verb_status_name as next_verb_status_name, obj.create_user_name, obj.create_date from "+prefix+"form_status_relationships obj inner join "+prefix+"form_statuses orig on orig.id = obj.form_status_id inner join "+prefix+"tables t on t.id = orig.table_id inner join " +prefix +"form_statuses next on obj.next_form_status_id = next.id ";
                sql += " where t.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", entityName.toUpperCase());
            } else {
                sql = "select obj.table_name as form_name, obj.display_name, obj.group_flag, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date, p.table_name as parent_form_name from "+prefix+"tables obj left outer join "+prefix+"tables p on p.id = obj.parent_table_id ";
                sql += " where obj.table_name = " + connection.formatStringForSQL(prefix+"tables", "table_name", entityName.toUpperCase());
            }
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("procedure")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            String whereClauseString = null;
            if (specifier != null && specifier.equalsIgnoreCase("parameters")) {
                sql = "select obj.parameter_name, obj.type_name, obj.array_flag from " +prefix+"procedure_parameters obj inner join " +prefix + "procedures p on p.id = obj.procedure_id ";
                sql += " where p.procedure_name = " + connection.formatStringForSQL(prefix+"procedures", "procedure_name", entityName.toUpperCase()) + "order by obj.order_number asc";
            } else {
                sql = "select obj.procedure_name, obj.return_resultset_flag, obj.return_array_flag, obj.return_type_name, obj.procedure_body, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"procedures obj ";
                sql += " where obj.procedure_name = " + connection.formatStringForSQL(prefix+"procedures", "procedure_name", entityName.toUpperCase());
            }
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("trigger")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            long tableId = this.getIdForObjectName(entityName, this.OBJECT_TYPE_TABLE, connection);
            if (tableId < 0) {
                return new CodaResponse(true, null, 2028);
            }
            boolean formFlag = this.isForm(connection, prefix, entityName);
            long formStatusId = -1;
            if (specifier != null) {
                formStatusId = this.getIdForObjectName(Long.toString(tableId) + "." + specifier.toUpperCase(), CodaServer.OBJECT_TYPE_FORM_STATUS_VERB, connection);
                if (formStatusId < 0) {
                    return new CodaResponse(true, null, 2093);
                }
            }
            if (!formFlag) {
                sql = "select p.table_name, obj.operation_id, obj.before_flag, obj.procedure_body as trigger_body, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"triggers obj inner join "+prefix+"tables p on p.id = obj.table_id ";
            } else {
                sql = "select p.table_name as form_name, obj.operation_id, fs.verb_status_name, obj.before_flag, obj.procedure_body, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"tables obj inner join "+prefix+"tables p on p.id = obj.table_id left outer join "+prefix+"form_statuses fs on fs.id = obj.form_status_id ";
            }
            sql += " where obj.table_id = "+connection.formatStringForSQL(prefix+"triggers", "table_id", Long.toString(tableId))+ " and obj.before_flag = " +connection.formatStringForSQL(prefix+"triggers", "before_flag", beforeFlag ? "1" : "0") + " and " + (!formFlag ? " obj.operation_id = " + connection.formatStringForSQL(prefix+"triggers", "operation_id", Integer.toString(getIdForDatabaseOperation(operation))) : (specifier.equalsIgnoreCase("update") ? " obj.operation_id = " + connection.formatStringForSQL(prefix+"triggers", "operation_id", Integer.toString(getIdForDatabaseOperation(operation))) : " obj.form_status_id = " + connection.formatStringForSQL(prefix+"triggers", "form_status_id", Long.toString(formStatusId)) ))  ;
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("index")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            if (specifier != null && specifier.equalsIgnoreCase("columns")) {
                sql = "select f.field_name, f.display_name, f.type_name, f.array_flag from " +prefix+"index_fields obj inner join " +prefix + "indexes i on i.id = obj.index_id inner join "+ prefix + "table_fields f on f.id = obj.table_field_id ";
                sql += " where i.index_name = " + connection.formatStringForSQL(prefix+"indexes", "index_name", entityName.toUpperCase());
            } else {
                sql = "select obj.index_name, t.table_name, obj.index_type_id, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"indexes obj left outer join "+prefix+"tables t on t.id = obj.table_id ";
                sql += " where obj.index_name = " + connection.formatStringForSQL(prefix+"indexes", "index_name", entityName.toUpperCase());
            }
            rs = connection.runQuery(sql, null);
        } else if (entityType.equalsIgnoreCase("cron")) {
            int environmentId = sessions.getSessionEnvironmentId(sessionKey);
            if (environmentId <= 0) {
                return new CodaResponse(true, null, 1006);
            }
            applicationName = sessions.getSessionApplication(sessionKey);
            CodaDatabase database = applicationName != null && deployedApplications.getDatasource(applicationName, environmentId) != null ? deployedApplications.getDatasource(applicationName, environmentId).getDatabase() : null;
            if (database == null) {
                return new CodaResponse(true, null, 8003);
            }
            CodaConnection connection = database.getConnection();

            String prefix = connection.getMetadata().getSystemTable().getPrefixString();
            if (prefix == null) {
                prefix = "";
            }
            if (specifier != null && specifier.equalsIgnoreCase("parameters")) {
                sql = "select p.parameter_name, p.type_name, p.array_flag, obj.parameter_value from " +prefix+"cron_parameters obj inner join " +prefix + "crons c on c.id = obj.cron_id inner join " +prefix+"procedure_parameters p on p.id = obj.procedure_parameter_id ";
                sql += " where c.cron_name = " + connection.formatStringForSQL(prefix+"crons", "cron_name", entityName.toUpperCase()) + "order by obj.id asc";
            } else {
                sql = "select obj.cron_name, obj.minute_part, obj.hour_part, obj.day_of_month_part, obj.month_part, obj.day_of_week_part, p.procedure_name, obj.executing_user_name, obj.create_user_name, obj.create_date, obj.mod_user_name, obj.mod_date from "+prefix+"crons obj left outer join "+prefix+"procedures p on p.id = obj.procedure_id ";
                sql += " where obj.cron_name = " + connection.formatStringForSQL(prefix+"crons", "cron_name", entityName.toUpperCase());
            }
            rs = connection.runQuery(sql, null);
        }

        return new CodaResponse(rs);
    }
}
