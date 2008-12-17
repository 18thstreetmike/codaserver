/*
 * DatabaseFormatter.java
 *
 * Created on June 6, 2007, 10:30 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.database;

import org.codalang.codaserver.CodaServer;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class DatabaseFormatter {
    
    /** Creates a new instance of DatabaseFormatter */
    public DatabaseFormatter() {
    }
    
    public static boolean formatCodaDatabase(CodaDatabase database, String rootPassword, boolean echo) {
        if (echo) {
			System.out.println("...Formatting");
		}
		CodaConnection connection = database.getConnection();

		connection.emptySchema();

		CodaTable [] tables = getCodaDatabaseTables();
        for(int i = 0; i < tables.length; i++ ) {
            try {
				if (echo) {
					System.out.println("...Creating table '"+ tables[i].getTableName() +"'");
				}
				connection.createTable(tables[i].getTableName(), new Vector(Arrays.asList(tables[i].getColumnDefinitions())));
            } catch (SQLException ex) {
                if (echo) {
					System.out.println("Failed while creating table '" + tables[i].getTableName() + "' with the following message: " + ex.getMessage());
				}
				return false;
            }
        }
        try {
            if (echo) {
				System.out.println("...Creating indexes and keys");
			}
			connection.setPrimaryKey("users", "id");
            connection.createIndex("users_username_idx", "users", new Vector(Arrays.asList(new String[]{"user_name"})), false);
            
            connection.setPrimaryKey("groups", "id");
            connection.createIndex("groups_groupname_idx", "groups", new Vector(Arrays.asList(new String[]{"group_name"})), false);
            
            connection.setIdentityColumns("user_groups_uk", "user_groups", new Vector(Arrays.asList(new String[]{"user_id", "group_id"})));
            
            connection.setPrimaryKey("datasources", "id");
            connection.setIdentityColumns("datasource_name_uk", "datasources", new Vector(Arrays.asList(new String[]{"datasource_name"})));
            
            connection.setPrimaryKey("applications", "id");
			connection.createIndex("applications_appname_idx", "applications", new Vector(Arrays.asList(new String[]{"application_name"})), false);
            //database.setIdentityColumns("application_name_uk", "applications", new Vector(Arrays.asList(new String[]{"application_name"})));

            connection.setIdentityColumns("group_applications_uk", "group_applications", new Vector(Arrays.asList(new String[]{"group_id", "application_id"})));
            
            connection.setIdentityColumns("application_permissions_uk", "application_permissions", new Vector(Arrays.asList(new String[]{"application_permission_name"})));
            
            connection.setIdentityColumns("server_permissions_uk", "server_permissions", new Vector(Arrays.asList(new String[]{"server_permission_name"})));
            
            connection.setIdentityColumns("user_application_permissions_uk", "user_application_permissions", new Vector(Arrays.asList(new String[]{"user_id", "application_id", "application_permission_name"})));
            
            connection.setIdentityColumns("user_server_permissions_uk", "user_server_permissions", new Vector(Arrays.asList(new String[]{"user_id", "server_permission_name"})));
            
            connection.setPrimaryKey("transactions", "id");
            connection.setIdentityColumns("transactions_uk", "transactions", new Vector(Arrays.asList(new String[]{"application_id", "revision_id"})));
                        
            connection.setPrimaryKey("types", "id");
            
            connection.setPrimaryKey("sessions", "id");
            connection.setIdentityColumns("session_key_uk", "sessions", new Vector(Arrays.asList(new String[]{"session_key"})));
            
            
            connection.setPrimaryKey("coda_system_information", "id");
            connection.setIdentityColumns("csi_system_property_uk", "coda_system_information", new Vector(Arrays.asList(new String[]{"system_property"})));
            
            if (echo) {
				System.out.println("...Inserting base data");
			}
			// Insert the basic data
            Hashtable values = new Hashtable();
            values.put("type_name", "STRING");
            values.put("built_in_flag", 1);
            connection.insertRow("types", values);
            values.put("type_name", "INTEGER");
            connection.insertRow("types", values);
            values.put("type_name", "FLOAT");
            connection.insertRow("types", values);
            values.put("type_name", "BOOLEAN");
            connection.insertRow("types", values);
            values.put("type_name", "LONGSTRING");
            connection.insertRow("types", values);
            values.put("type_name", "FILE");
            connection.insertRow("types", values);
            values.put("type_name", "REFERENCE");
            connection.insertRow("types", values);
            values.put("type_name", "TIMESTAMP");
            connection.insertRow("types", values);
            connection.commit();

			if (echo) {
				System.out.println("...Types");
			}

			values = new Hashtable();
            values.put("server_permission_name", "CONNECT");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_USERS");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_USER_DATA");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_GROUPS");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_TYPES");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_APPLICATIONS");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_DATASOURCES");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "QUERY_SYSTEM_TABLES");
            connection.insertRow("server_permissions", values);
            values.put("server_permission_name", "MANAGE_SESSIONS");
            connection.insertRow("server_permissions", values);
            connection.commit();

			if (echo) {
				System.out.println("...Server Permissions");
			}

			values = new Hashtable();
            values.put("application_permission_name", "CONNECT");
            connection.insertRow("application_permissions", values);
            values.put("application_permission_name", "MANAGE_USERS");
            connection.insertRow("application_permissions", values);
            values.put("application_permission_name", "MANAGE_ROLES");
            connection.insertRow("application_permissions", values);
            values.put("application_permission_name", "DEVELOPER");
            connection.insertRow("application_permissions", values);
            values.put("application_permission_name", "MANAGE_CRONS");
            connection.insertRow("application_permissions", values);
            connection.commit();

			if (echo) {
				System.out.println("...Application Permissions");
			}

			values = new Hashtable();
            values.put("user_name", "ROOT"); 
            values.put("pass_word", CodaServer.encrypt(rootPassword));
            values.put("robot_flag", "0");
            values.put("active_flag", "1");
            values.put("create_user_id", "1");
            values.put("create_date", new GregorianCalendar().getTimeInMillis());
            values.put("mod_user_id", "1");
            values.put("mod_date", new GregorianCalendar().getTimeInMillis());
            connection.insertRow("users", values);

			if (echo) {
				System.out.println("...Root User");
			}

			connection.commit();
			CodaServer.addRootPermissions(connection, null);

			if (echo) {
				System.out.println("...Root User Permissions");
			}

			values = new Hashtable();
            values.put("system_property", "FORMAT_VERSION");
            values.put("system_value", "1.0");
            connection.insertRow("coda_system_information", values);
            values.put("system_property", "CODA_SERVER");
            values.put("system_value", "TRUE");
            connection.insertRow("coda_system_information", values);
			connection.commit();

			if (echo) {
				System.out.println("...Formatting complete!");
			}
		} catch (SQLException ex) {
            connection.rollback();
            System.out.println(ex.getMessage());
            return false;
        }
        
        return true;
    }

    public static boolean formatApplicationDatabase(CodaDatabase database, String applicationName, String prefix) {
        CodaConnection connection = database.getConnection();

        connection.emptySchema();
        
        CodaTable [] tables = getApplicationTables();
        for(int i = 0; i < tables.length; i++ ) {
            try {
                if (tables[i].getTableName().equalsIgnoreCase("coda_system_information")) {
                    connection.createTable(tables[i].getTableName(), new Vector(Arrays.asList(tables[i].getColumnDefinitions())));
                } else {
                    connection.createTable(prefix + tables[i].getTableName(), new Vector(Arrays.asList(tables[i].getColumnDefinitions())));
                }
            } catch (SQLException ex) {
                return false;
            }
        }
        try {
            connection.setPrimaryKey(prefix + "tables", "id");
            connection.setIdentityColumns("table_name_uk", prefix + "tables", new Vector(Arrays.asList(new String[]{"table_name"})));
            
            connection.setPrimaryKey(prefix + "table_fields", "id");
            connection.setIdentityColumns("table_fields_uk", prefix + "table_fields", new Vector(Arrays.asList(new String[]{"table_id", "field_name"})));
            
            connection.setPrimaryKey(prefix + "form_statuses", "id");
            connection.setIdentityColumns("fs_adj_name_uk", prefix + "form_statuses", new Vector(Arrays.asList(new String[]{"table_id", "adj_status_name"})));
            connection.setIdentityColumns("fs_verb_name_uk", prefix + "form_statuses", new Vector(Arrays.asList(new String[]{"table_id", "verb_status_name"})));
            
            connection.setPrimaryKey(prefix + "form_status_relationships", "id");
            connection.setIdentityColumns("fsr_status_uk", prefix + "form_status_relationships", new Vector(Arrays.asList(new String[]{"form_status_id", "next_form_status_id"})));
            
            connection.setPrimaryKey(prefix + "triggers", "id");
            connection.createIndex("triggers_table_idx", prefix + "triggers", new Vector(Arrays.asList(new String[]{"table_id", "before_flag", "operation_id"})), false);
            connection.createIndex("triggers_fs_idx", prefix + "triggers", new Vector(Arrays.asList(new String[]{"table_id", "before_flag", "form_status_id"})), false);
            
            connection.setPrimaryKey(prefix + "indexes", "id");
            connection.setIdentityColumns("index_name_uk", prefix + "indexes", new Vector(Arrays.asList(new String[]{"index_name"})));
            
            connection.setPrimaryKey(prefix + "index_fields", "id");
            connection.setIdentityColumns("if_field_id_uk", prefix + "index_fields", new Vector(Arrays.asList(new String[]{"index_id", "table_field_id"})));
            
            connection.setPrimaryKey(prefix + "procedures", "id");
            connection.setIdentityColumns("procedures_uk", prefix + "procedures", new Vector(Arrays.asList(new String[]{"procedure_name"})));
            
            connection.setPrimaryKey(prefix + "procedure_parameters", "id");
            connection.setIdentityColumns("pp_param_name_uk", prefix + "procedure_parameters", new Vector(Arrays.asList(new String[]{"procedure_id", "parameter_name"})));
            connection.setIdentityColumns("pp_param_order_uk", prefix + "procedure_parameters", new Vector(Arrays.asList(new String[]{"procedure_id", "order_number"})));
            
            connection.setPrimaryKey(prefix + "roles", "id");
            connection.setIdentityColumns("roles_uk", prefix + "roles", new Vector(Arrays.asList(new String[]{"role_name"})));

			connection.setPrimaryKey(prefix + "crons", "id");
			connection.setPrimaryKey(prefix + "cron_parameters", "id");

			connection.setPrimaryKey(prefix + "permissions", "id");
            connection.setIdentityColumns("permissions_uk", prefix + "permissions", new Vector(Arrays.asList(new String[]{"permission_name"})));
            
            connection.setIdentityColumns("role_perms_uk", prefix + "role_permissions", new Vector(Arrays.asList(new String[]{"permission_id", "role_id"})));
               
            connection.setIdentityColumns("role_tables_uk", prefix + "role_tables", new Vector(Arrays.asList(new String[]{"table_id", "role_id"})));
            
            connection.setIdentityColumns("role_fs_uk", prefix + "role_form_statuses", new Vector(Arrays.asList(new String[]{"form_status_id", "role_id"})));
            
            connection.setIdentityColumns("role_procs_uk", prefix + "role_procedures", new Vector(Arrays.asList(new String[]{"procedure_id", "role_id"})));
            
            connection.setPrimaryKey(prefix + "object_types", "id");
            connection.setIdentityColumns("object_types_uk", prefix + "object_types", new Vector(Arrays.asList(new String[]{"object_type_name"})));
            
            connection.setPrimaryKey("coda_system_information", "id");
            connection.setIdentityColumns("wsi_system_property_uk", "coda_system_information", new Vector(Arrays.asList(new String[]{"system_property"})));
           
            Hashtable values = new Hashtable();
            values.put("system_property", "FORMAT_VERSION");
            values.put("system_value", "1.0");
            connection.insertRow("coda_system_information", values);
            values.put("system_property", "CODA_SERVER");
            values.put("system_value", "FALSE");
            connection.insertRow("coda_system_information", values);
            values.put("system_property", "PREFIX");
            values.put("system_value", prefix);
            connection.insertRow("coda_system_information", values);
            values.put("system_property", "APPLICATION_NAME");
            values.put("system_value", applicationName.toUpperCase());
            connection.insertRow("coda_system_information", values);
            values.put("system_property", "REVISION_ID");
            values.put("system_value", "-1");
            connection.insertRow("coda_system_information", values);
            values.put("system_property", "REF_TABLE_REVISION_ID");
            values.put("system_value", "-1");
            connection.insertRow("coda_system_information", values);
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            return false;
        }
        
        return true;
    }
    
    public static CodaTable[] getCodaDatabaseTables() {
        
        CodaTable[] retval =  {
            new CodaTable("users", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("user_name", Types.VARCHAR, false),
                new ColumnDefinition("pass_word", Types.VARCHAR, false),
                new ColumnDefinition("first_name", Types.VARCHAR, true),
                new ColumnDefinition("middle_name", Types.VARCHAR, true),
                new ColumnDefinition("last_name", Types.VARCHAR, true),
                new ColumnDefinition("organization", Types.VARCHAR, true),
                new ColumnDefinition("address", Types.VARCHAR, true),
                new ColumnDefinition("city", Types.VARCHAR, true),
                new ColumnDefinition("state_prov", Types.VARCHAR, true),
                new ColumnDefinition("postal_code", Types.VARCHAR, true),
                new ColumnDefinition("country", Types.VARCHAR, true),
                new ColumnDefinition("phone", Types.VARCHAR, true),
                new ColumnDefinition("alt_phone", Types.VARCHAR, true),
                new ColumnDefinition("email", Types.VARCHAR, true),
                new ColumnDefinition("robot_flag", Types.BOOLEAN, false),
                new ColumnDefinition("active_flag", Types.BOOLEAN, false),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }), 
            new CodaTable("groups", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("group_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("active_flag", Types.BOOLEAN, false),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("user_groups", new ColumnDefinition[] {
                new ColumnDefinition("user_id", Types.BIGINT, false),
                new ColumnDefinition("group_id", Types.BIGINT, false),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("datasources", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("datasource_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("driver_name", Types.VARCHAR, false),
                new ColumnDefinition("host_name", Types.VARCHAR, false),
                new ColumnDefinition("schema_name", Types.VARCHAR, false),
                new ColumnDefinition("user_name", Types.VARCHAR, false),
                new ColumnDefinition("pass_word", Types.VARCHAR, false),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false)
            }), 
            new CodaTable("datasource_options", new ColumnDefinition[] {
                new ColumnDefinition("datasource_id", Types.BIGINT, false),
                new ColumnDefinition("option_name", Types.VARCHAR, false),
                new ColumnDefinition("option_value", Types.VARCHAR, false)
            }),
            new CodaTable("applications", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("application_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("group_flag", Types.BOOLEAN, false),
                new ColumnDefinition("active_flag", Types.BOOLEAN, false),
                new ColumnDefinition("dev_datasource_id", Types.BIGINT, false),
                new ColumnDefinition("test_datasource_id", Types.BIGINT, true),
                new ColumnDefinition("prod_datasource_id", Types.BIGINT, true),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false)
            }),
            new CodaTable("group_applications", new ColumnDefinition[] {
                new ColumnDefinition("application_id", Types.BIGINT, false),
                new ColumnDefinition("group_id", Types.BIGINT, false),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("server_permissions", new ColumnDefinition[] {
                new ColumnDefinition("server_permission_name", Types.VARCHAR, false)
            }),
            new CodaTable("application_permissions", new ColumnDefinition[] {
                new ColumnDefinition("application_permission_name", Types.VARCHAR, false)
            }),
            new CodaTable("user_server_permissions", new ColumnDefinition[] {
                new ColumnDefinition("user_id", Types.BIGINT, false),
                new ColumnDefinition("server_permission_name", Types.VARCHAR, false)
            }),
            new CodaTable("user_application_permissions", new ColumnDefinition[] {
                new ColumnDefinition("user_id", Types.BIGINT, false),
                new ColumnDefinition("application_id", Types.BIGINT, false),
                new ColumnDefinition("environment", Types.INTEGER, true),
                new ColumnDefinition("group_id", Types.BIGINT, true),
                new ColumnDefinition("application_permission_name", Types.VARCHAR, false)
            }),
            new CodaTable("transactions", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("application_id", Types.BIGINT, false),
                new ColumnDefinition("revision_id", Types.BIGINT, false),
                new ColumnDefinition("ref_table_flag", Types.BOOLEAN, false),
                new ColumnDefinition("coda_statement", Types.LONGVARCHAR, false),
                new ColumnDefinition("create_user_id", Types.BIGINT, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_id", Types.BIGINT, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false)
            }),
            new CodaTable("types", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("type_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, true),
                new ColumnDefinition("built_in_flag", Types.BOOLEAN, false),
                new ColumnDefinition("regex_flag", Types.BOOLEAN, true),
                new ColumnDefinition("type_class", Types.VARCHAR, true),
                new ColumnDefinition("validation_mask", Types.LONGVARCHAR, true),
                new ColumnDefinition("save_mask", Types.LONGVARCHAR, true),
                new ColumnDefinition("active_flag", Types.BOOLEAN, true),
                new ColumnDefinition("class_file", Types.CLOB, true),
                new ColumnDefinition("create_user_id", Types.BIGINT, true),
                new ColumnDefinition("create_date", Types.TIMESTAMP, true),
                new ColumnDefinition("mod_user_id", Types.BIGINT, true),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, true)
            }),
            new CodaTable("sessions", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("session_key", Types.VARCHAR, false),
                new ColumnDefinition("user_id", Types.BIGINT, false),
                new ColumnDefinition("user_name", Types.VARCHAR, false),
                new ColumnDefinition("group_name", Types.VARCHAR, true),
                new ColumnDefinition("group_id", Types.INTEGER, true),
                new ColumnDefinition("application_name", Types.VARCHAR, true),
                new ColumnDefinition("application_id", Types.INTEGER, true),
                new ColumnDefinition("environment", Types.VARCHAR, true),
                new ColumnDefinition("environment_id", Types.INTEGER, true),
                new ColumnDefinition("session_timestamp", Types.TIMESTAMP, false)
            }),
            new CodaTable("cluster", new ColumnDefinition[] {
                new ColumnDefinition("ip_address", Types.VARCHAR, false),
                new ColumnDefinition("port", Types.INTEGER, false),
                new ColumnDefinition("run_crons", Types.INTEGER, false)
            }),
            new CodaTable("coda_system_information", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("system_property", Types.VARCHAR, false),
                new ColumnDefinition("system_value", Types.VARCHAR, false)
            })
        };
        
        return retval;
    }
    
    public static CodaTable[] getApplicationTables() {
        CodaTable[] retval =  {
            new CodaTable("tables", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("table_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("group_flag", Types.BOOLEAN, false),
                new ColumnDefinition("parent_table_id", Types.BIGINT, true),
                new ColumnDefinition("form_flag", Types.BOOLEAN, false),
                new ColumnDefinition("soft_delete_flag", Types.BOOLEAN, false),
                new ColumnDefinition("ref_table_flag", Types.BOOLEAN, false),
                new ColumnDefinition("class_file", Types.CLOB, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }), 
            new CodaTable("table_fields", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("field_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("type_name", Types.VARCHAR, false),
                new ColumnDefinition("array_flag", Types.BOOLEAN, false),
                new ColumnDefinition("nullable_flag", Types.BOOLEAN, false),
                new ColumnDefinition("table_id", Types.BIGINT, false),
                new ColumnDefinition("ref_table_id", Types.BIGINT, true),
                new ColumnDefinition("default_variable_id", Types.INTEGER, true),
                new ColumnDefinition("default_value", Types.VARCHAR, true),
				new ColumnDefinition("built_in_flag", Types.BOOLEAN, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("form_statuses", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("adj_status_name", Types.VARCHAR, false),
                new ColumnDefinition("adj_display_name", Types.VARCHAR, false),
                new ColumnDefinition("verb_status_name", Types.VARCHAR, false),
                new ColumnDefinition("verb_display_name", Types.VARCHAR, false),
                new ColumnDefinition("initial_flag", Types.BOOLEAN, false),
                new ColumnDefinition("table_id", Types.BIGINT, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("form_status_relationships", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("form_status_id", Types.BIGINT, false),
                new ColumnDefinition("next_form_status_id", Types.BIGINT, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false)
            }), 
            new CodaTable("triggers", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("table_id", Types.BIGINT, false),
                new ColumnDefinition("form_status_id", Types.BIGINT, true),
                new ColumnDefinition("operation_id", Types.INTEGER, true), // 1: UPDATE, 2: INSERT, 3: DELETE forms can have update
                new ColumnDefinition("before_flag", Types.BOOLEAN, false),
                new ColumnDefinition("procedure_language", Types.VARCHAR, false),
                new ColumnDefinition("procedure_body", Types.LONGVARCHAR, false),
                new ColumnDefinition("recompile_needed_flag", Types.BOOLEAN, false),
                new ColumnDefinition("class_file", Types.CLOB, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("trigger_dependencies", new ColumnDefinition[] {
                new ColumnDefinition("trigger_id", Types.BIGINT, false),
                new ColumnDefinition("dependency_object_type_id", Types.BIGINT, false),
                new ColumnDefinition("dependency_object_id", Types.BIGINT, false)
            }),
            new CodaTable("indexes", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("index_name", Types.VARCHAR, false),
                new ColumnDefinition("table_id", Types.BIGINT, false),
                new ColumnDefinition("index_type_id", Types.INTEGER, false), // 1: primary key, 2: unique key, 3: normal index
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false)  
            }),
            new CodaTable("index_fields", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("table_field_id", Types.BIGINT, false),
                new ColumnDefinition("index_id", Types.BIGINT, false),
            }),
            new CodaTable("procedures", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("procedure_name", Types.VARCHAR, false),
                new ColumnDefinition("return_resultset_flag", Types.BOOLEAN, false), //Used to return arbitrary queries
                new ColumnDefinition("return_type_name", Types.VARCHAR, true),
                new ColumnDefinition("return_array_flag", Types.BOOLEAN, false),
                new ColumnDefinition("procedure_language", Types.VARCHAR, false),
                new ColumnDefinition("procedure_body", Types.LONGVARCHAR, false),
                new ColumnDefinition("recompile_needed_flag", Types.BOOLEAN, false),
                new ColumnDefinition("class_file", Types.CLOB, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("procedure_parameters", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("parameter_name", Types.VARCHAR, false),
                new ColumnDefinition("procedure_id", Types.BIGINT, true),
                new ColumnDefinition("order_number", Types.INTEGER, true),
                new ColumnDefinition("type_name", Types.VARCHAR, false),
                new ColumnDefinition("array_flag", Types.BOOLEAN, false)
                }),
            new CodaTable("procedure_dependencies", new ColumnDefinition[] {
                new ColumnDefinition("procedure_id", Types.BIGINT, false),
                new ColumnDefinition("dependency_object_type_id", Types.BIGINT, false),
                new ColumnDefinition("dependency_object_id", Types.BIGINT, false)
            }),
            new CodaTable("crons", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("cron_name", Types.VARCHAR, false),
                new ColumnDefinition("minute_part", Types.VARCHAR, false),
                new ColumnDefinition("hour_part", Types.VARCHAR, false),
                new ColumnDefinition("day_of_month_part", Types.VARCHAR, false),
                new ColumnDefinition("month_part", Types.VARCHAR, false),
                new ColumnDefinition("day_of_week_part", Types.VARCHAR, false),
                new ColumnDefinition("procedure_id", Types.BIGINT, false),
                new ColumnDefinition("executing_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("cron_parameters", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("cron_id", Types.BIGINT, false),
                new ColumnDefinition("procedure_parameter_id", Types.BIGINT, false),
                new ColumnDefinition("parameter_value", Types.LONGVARCHAR, false)
            }),
            new CodaTable("roles", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("role_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("user_roles", new ColumnDefinition[] {
                new ColumnDefinition("user_id", Types.BIGINT, false),
                new ColumnDefinition("role_id", Types.BIGINT, false),
                new ColumnDefinition("group_id", Types.BIGINT, true),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("permissions", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("permission_name", Types.VARCHAR, false),
                new ColumnDefinition("display_name", Types.VARCHAR, false),
                new ColumnDefinition("description", Types.LONGVARCHAR, true),
                new ColumnDefinition("create_user_name", Types.VARCHAR, false),
                new ColumnDefinition("create_date", Types.TIMESTAMP, false),
                new ColumnDefinition("mod_user_name", Types.VARCHAR, false),
                new ColumnDefinition("mod_date", Types.TIMESTAMP, false) 
            }),
            new CodaTable("role_permissions", new ColumnDefinition[] {
                new ColumnDefinition("role_id", Types.BIGINT, false),
                new ColumnDefinition("permission_id", Types.BIGINT, false)
            }),
            new CodaTable("role_tables", new ColumnDefinition[] {
                new ColumnDefinition("role_id", Types.BIGINT, false),
                new ColumnDefinition("table_id", Types.BIGINT, false),
                new ColumnDefinition("select_flag", Types.BOOLEAN, false),
                new ColumnDefinition("insert_flag", Types.BOOLEAN, false),
                new ColumnDefinition("update_flag", Types.BOOLEAN, false),
                new ColumnDefinition("delete_flag", Types.BOOLEAN, false)
            }),
            new CodaTable("role_form_statuses", new ColumnDefinition[] {
                new ColumnDefinition("role_id", Types.BIGINT, false),
                new ColumnDefinition("form_status_id", Types.BIGINT, false),
                new ColumnDefinition("view_flag", Types.BOOLEAN, false),
                new ColumnDefinition("call_flag", Types.BOOLEAN, false),
                new ColumnDefinition("update_flag", Types.BOOLEAN, false)
            }),
            new CodaTable("role_procedures", new ColumnDefinition[] {
                new ColumnDefinition("role_id", Types.BIGINT, false),
                new ColumnDefinition("procedure_id", Types.BIGINT, false),
                new ColumnDefinition("execute_flag", Types.BOOLEAN, false)
            }),
            new CodaTable("object_types", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("object_type_name", Types.VARCHAR, false)
            }),
            new CodaTable("identifiers", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("identifier_name", Types.VARCHAR, false),
                new ColumnDefinition("object_type_id", Types.BIGINT, false),
                new ColumnDefinition("object_id", Types.BIGINT, false)
            }),
            new CodaTable("coda_system_information", new ColumnDefinition[] {
                new ColumnDefinition("id", Types.BIGINT, false),
                new ColumnDefinition("system_property", Types.VARCHAR, false),
                new ColumnDefinition("system_value", Types.VARCHAR, false)
            })
        };
        
        return retval;
    }
}
