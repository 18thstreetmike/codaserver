/*
 * CodaResponse.java
 *
 * Created on July 1, 2007, 2:18 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaResponse {

    private boolean error = false;
    private Vector<CodaError> errors = new Vector();
    private Object returnValue;
    
    /**
     * Creates a new instance of CodaResponse
     */
    
    public CodaResponse() {
        error = false;
    }
    
    public CodaResponse(boolean error, Object returnValue, int errorCode, String errorMessage) {
        this.error = error;
        errors.add(new CodaError(errorCode, errorMessage));
        this.returnValue = returnValue;
    }
    
    public CodaResponse(boolean error, Object returnValue, int errorCode) {
        this.error = error;
        errors.add(new CodaError(errorCode, this.getMessageForCode(errorCode)));
        this.returnValue = returnValue;
    }
    
    public CodaResponse(Object returnValue) {
        this.error = false;
        this.returnValue = returnValue;
    }

    public boolean getError() {
        return error;
    }

    public Vector<CodaError> getErrors() {
        return errors;
    }
    
    public void addError(CodaError newError) {
        this.error = true;
        errors.add(newError);
    } 

    public Object getReturnValue() {
        return returnValue == null ? "" : returnValue;
    }
    
    public static String getMessageForCode(int errorCode) {
        switch (errorCode) {
            // Authentication errors
            case 1001: return "Invalid username/password";
            case 1002: return "Invalid environment specified";
            case 1003: return "User cannot connect to specified application/environment";
            case 1004: return "User not in specified group";
            case 1005: return "Your session seems to have expired.  Please log in again.";
            case 1006: return "DML and Metadata statements require that you set a session application and environment with the SET command.";
            case 1007: return "Parse error";
            case 1008: return "Client error";
			case 1009: return "Multiple errors occurred";


			// Server-oriented errors
            case 2001: return "Missing parameter HOSTNAME";
            case 2002: return "Missing parameter USERNAME";
            case 2003: return "Missing parameter PASSWORD";
            case 2004: return "Missing parameter SCHEMA";
            case 2005: return "Missing parameter DRIVER";
            case 2006: return "Driver not found";
            case 2007: return "Datasource name already in use";
            case 2008: return "Could not create new schema on database host";
            case 2009: return "Datasource name is invalid";
            case 2010: return "Datasource in use as a development database";
            case 2011: return "Application name already in use";
            case 2012: return "Application name is invalid";
            case 2013: return "Invalid environment specified";
            case 2014: return "Username already in use";
            case 2015: return "Username is invalid";
            case 2016: return "Group name already in use";
            case 2017: return "Group name is invalid";
            case 2018: return "ROOT does not have personal information.  It is eternal.";
            case 2019: return "Type name already in use";
            case 2020: return "Type name is invalid";
            case 2021: return "Types must contain both a validation and save mask";
            case 2022: return "Validation mask is an invalid regex";
            case 2023: return "Save mask is an invalid regex";
            case 2024: return "User already has role";
            case 2025: return "User does not have role";
            case 2026: return "No group specified in a group application";
            case 2027: return "No application specified.";
            case 2028: return "Table/form name is invalid";
            case 2029: return "Role name is invalid";
            case 2030: return "Permission name is invalid";
            case 2031: return "Form status adjective is invalid";
            case 2032: return "Form name is invalid";
            case 2033: return "Procedure is invalid";
            case 2034: return "Role name is already in use";
            case 2035: return "Permission name is already in use";
            case 2036: return "Table name is already in use";
            case 2037: return "Column name already in use";
            case 2038: return "Array column name must be globally unique";
            case 2039: return "The entity specified is not a table";
            case 2040: return "Field name is invalid";
            case 2041: return "Form name is invalid";
            case 2042: return "Form name is already in use";
            case 2043: return "Field name already in use";
            case 2044: return "Parent form must be a form";
            case 2045: return "Parent table must be a table";
            case 2046: return "Array field name must be globally unique";
            case 2047: return "Form status adjective defined multiple times";
            case 2048: return "Form status verb defined multiple times";
            case 2049: return "Form already has a status with this adjective name";
            case 2050: return "Form already has a status with this verb name";
            case 2051: return "Form status adjective is invalid";
            case 2052: return "Can not drop form status when there are still records using it";
            case 2053: return "Index name is already in use";
            case 2054: return "Index name prefixes ID_ and PK_ are reserved";
            case 2055: return "Index name is invalid";
            case 2056: return "Procedure name is invalid";
            case 2057: return "Cron name is already in use";
            case 2058: return "Invalid number of parameters passed to procedure";
            case 2059: return "Type mismatch";
            case 2060: return "Invalid cron arguments";
            case 2061: return "Cron scheduler error.  Not sure what you can do with that, but there it is";
            case 2062: return "Cron name is invalid";
            case 2063: return "Subtables cannot have the same name as a parent table column";
            case 2064: return "Subforms cannot have the same name as a parent form field";
            case 2065: return "Column name already in use by a subtable";
            case 2066: return "Field name already in use by a subform";
            case 2067: return "No object found matching the given ID";
            case 2068: return "Column not found in table";
            case 2069: return "Field not found in form";
            case 2070: return "Must specify a value or default for NOT NULL column";
            case 2071: return "INSERT and DELETE only allowed on tables, not forms";
            case 2072: return "INSERT, UPDATE, and DELETE are reserved words and can not be used for form status verbs";
            case 2073: return "Subtables must specify a PARENT_TABLE_ID value";
            case 2074: return "To access GROUP tables, you must set a group in your session";
            case 2075: return "The specified column name is reserved";
            case 2076: return "The action specified does not exist for this form";
            case 2077: return "The action specified requires a where clause be specified";
            case 2078: return "The query is ambiguous; if the same table is used multiple times in the FROM clause, they must be aliased";
            case 2079: return "The same table alias cannot be specified for multiple tables";
            case 2080: return "The WHERE clause is ambiguous.  Try using aliases to specify which columns/fields belong to which tables/forms.";
            case 2081: return "There was an error while formatting the application database.";
            case 2082: return "The specified datasource does not contain this application.";
            case 2083: return "Classloader exception when defining new type class.  This is probably a Groovy configuration problem.";
            case 2084: return "Error compiling type.  Please check your masks.";
            case 2085: return "Error compiling table.  Please check your fields.";
            case 2086: return "Procedure name is already in use";
            case 2087: return "Parameter name declared multiple times";
            case 2088: return "Procedure name is invalid";
            case 2089: return "Invalid table operation specified";
            case 2090: return "Trigger does not exist";
            case 2091: return "Procedure could not be loaded by the classloader";
            case 2092: return "The entity specified is not a form";
            case 2093: return "Form status verb is invalid";
			case 2094: return "Invalid table operation specified";
			case 2095: return "The column/field specified cannot be dropped";
			case 2096: return "The column/field specified cannot be altered";

			// Data errors
            case 3001: return "Type mismatch";
            case 3002: return "The return value should be an array";
            case 3003: return "The return type should be a resultset or array of resultsets";
            case 3004: return "The return type is invalid";
			case 3005: return "The operation could not be completed, most likely due to a unique key violation";

			// User Defined Errors
            case 4001: return "Procedure Exception";
            case 4002: return "Trigger Exception";
            
            // Database errors
            case 8001: return "Unspecified database error.  Is the CodaServer database up?";
            case 8002: return "Cannot connect to datasource.  Check the connection settings and database.";
            case 8003: return "There is no datasource attached to the requested application/environment";
            case 8004: return "The following error was reported by the database:";
            case 8005: return "There was a problem with your procedure syntax:";
            case 8006: return "There was a problem with your trigger syntax:";
            case 8007: return "Invalid command";
			case 8008: return "There was an error in your where clause";
			case 8009: return "The specified datasource needs to be formatted.  Please specify a prefix.";

			// Permission errors
            case 9001: return "Do not have permission to manage datasources";
            case 9002: return "Do not have permission to manage applications";
            case 9003: return "Do not have permission to view or manage users";
            case 9004: return "Do not have permission to modify ROOT account";
            case 9005: return "Do not have permission to modify groups";
            case 9006: return "Do not have permission to modify user data";
            case 9007: return "Do not have permission to modify types";
            case 9008: return "Do not have permission to assign roles in this environment";
            case 9009: return "Do not have DEVELOPER permission on this application";
            case 9010: return "Do not have MANAGE permission on this application and environment";
            case 9011: return "Specified user does not have execute permission on the specified procedure";
            case 9012: return "Do not have permission to view this table";
            case 9013: return "Do not have permission to view this form at this status";
            case 9014: return "Do not have the correct permissions on this table";
            case 9015: return "Do not have the correct permissions on this form";
            case 9016: return "Do not have MANAGE_SESSIONS permission";
            case 9017: return "Do not have QUERY_SYSTEM_TABLES permission";
            case 9018: return "ROOT user permissions can not change";


            default: return "Mystery error.  Who wrote your application?";
        }
    }
}
