grammar Coda;
// export CLASSPATH=/Users/michaelarace/WontonServer/lib/antlr-3.0.1.jar:/Users/michaelarace/WontonServer/lib/stringtemplate-3.0.jar:/Users/michaelarace/WontonServer/lib/antlr-2.7.7.jar 
// java -Xmx256m org.antlr.Tool Coda.g 


options {
   output = template;
   k=2; 
   backtrack=true; 
   memoize=true;
}
@lexer::header {
	package org.codalang.codaserver.language;
}

@header{
	package org.codalang.codaserver.language;
	
	import org.codalang.codaserver.language.objects.*;
	import org.codalang.codaserver.*;
	import org.codalang.codaserver.database.*;
	import java.util.Iterator;
	import java.util.Vector;
	import java.util.Hashtable;
}

@members {
	CodaServer server;
	String sessionKey;
	CodaDatabase database;
	
	public CodaParser(TokenStream input, CodaServer server, String sessionKey, CodaDatabase database) {
		this(input);
		this.server = server;
		this.sessionKey = sessionKey;
		this.database = database;
	}
	
	public CodaDatabase getDatabase() {
		return database;
	}
	
	public void logTransaction(String codaStatement, String applicationName, String tableName) {
		if (database == null) {
			if (applicationName == null) {
				applicationName = server.getSessionApplication(sessionKey);
			}
			if (applicationName != null) {
				CodaConnection serverConnection = server.getDatabase().getConnection();
				long applicationId = server.getIdForObjectName(serverConnection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
				long userId = server.getSessionUserId(sessionKey);
				server.logTransaction(serverConnection, applicationId, codaStatement, server.isRefTable(applicationName, tableName), userId);
			}
		} 
	}

	public void logTransaction(String codaStatement, String applicationName, boolean refTable) {
		if (database == null) {
			if (applicationName == null) {
				applicationName = server.getSessionApplication(sessionKey);
			}
			if (applicationName != null) {
				CodaConnection serverConnection = server.getDatabase().getConnection();
				long applicationId = server.getIdForObjectName(serverConnection, applicationName, CodaServer.OBJECT_TYPE_APPLICATION);
				long userId = server.getSessionUserId(sessionKey);
				server.logTransaction(serverConnection, applicationId, codaStatement, refTable, userId);
			}
		} 
	}

}

@rulecatch {
	catch (RecognitionException e) {
		throw e;
	}
	
    
}

stat	returns [CodaResponse response]:
	( connectResp=connect 
	| disconnectResp=disconnect 
	| setApplicationResp=setApplication
	| createDatasourceResp=createDatasource
	| alterDatasourceResp=alterDatasource
	| dropDatasourceResp=dropDatasource
	| formatDatasourceResp=formatDatasource
	| promoteApplicationResp=promoteApplication
	| createApplicationResp=createApplication
	| alterApplicationResp=alterApplication
	| dropApplicationResp=dropApplication
	| createUserResp=createUser
	| alterUserResp=alterUser
	| dropUserResp=dropUser
	| createGroupResp=createGroup
	| alterGroupResp=alterGroup
	| dropGroupResp=dropGroup
	| createTypeResp=createType
	| alterTypeResp=alterType
	| dropTypeResp=dropType
	| grantResp=grant
	| revokeResp=revoke
	| createRoleResp=createRole
	| alterRoleResp=alterRole
	| dropRoleResp=dropRole
	| createPermissionResp=createPermission
	| alterPermissionResp=alterPermission
	| dropPermissionResp=dropPermission
	| createTableResp=createTable
	| alterTableResp=alterTable
	| dropTableResp=dropTable
	| createFormResp=createForm
	| alterFormResp=alterForm
	| dropFormResp=dropForm
	| createIndexResp=createIndex
	| dropIndexResp=dropIndex
	| createTriggerResp=createTrigger
	| dropTriggerResp=dropTrigger
	| createProcedureResp=createProcedure
	| alterProcedureResp=alterProcedure
	| dropProcedureResp=dropProcedure
	| (insert) => insertResp=insert
	| (update) => updateResp=update
	| (delete) => deleteResp=delete
	| formUpdateResp=formUpdate
	| execProcedureResp=execProcedure
	| cronResp=cron
	| dropCronResp=dropCron
	| selectObjectResp=selectObject  
	| selectResp=select
	| rawSQLResp=rawSQL
	| rawSelectResp=rawSelect
	| sysSelectResp=sysSelect
	| showResp=show
	| describeResp=describe
	| commitResp=commit
	| rollbackResp=rollback
	) 
	{
		CodaResponse returnResponse = new CodaResponse();
		if (connectResp != null) {
			returnResponse = $connectResp.response;
		} else if (disconnectResp != null) {
			returnResponse = $disconnectResp.response;
		} else if (setApplicationResp != null) {
			returnResponse = $setApplicationResp.response;
		} else if (createDatasourceResp != null) {
			returnResponse = $createDatasourceResp.response;
		} else if (alterDatasourceResp != null) {
			returnResponse = $alterDatasourceResp.response;
		} else if (dropDatasourceResp != null) {
			returnResponse = $dropDatasourceResp.response;
		} else if (formatDatasourceResp != null) {
			returnResponse = $formatDatasourceResp.response;
		} else if (promoteApplicationResp != null) {
			returnResponse = $promoteApplicationResp.response;
		} else if (createApplicationResp != null) {
			returnResponse = $createApplicationResp.response;
		} else if (alterApplicationResp != null) {
			returnResponse = $alterApplicationResp.response;
		} else if (dropApplicationResp != null) {
			returnResponse = $dropApplicationResp.response;
		} else if (createUserResp != null) {
			returnResponse = $createUserResp.response;
		} else if (alterUserResp != null) {
			returnResponse = $alterUserResp.response;
		} else if (dropUserResp != null) {
			returnResponse = $dropUserResp.response;
		} else if (createGroupResp != null) {
			returnResponse = $createGroupResp.response;
		} else if (alterGroupResp != null) {
			returnResponse = $alterGroupResp.response;
		} else if (dropGroupResp != null) {
			returnResponse = $dropGroupResp.response;
		} else if (createTypeResp != null) {
			returnResponse = $createTypeResp.response;
		} else if (alterTypeResp != null) {
			returnResponse = $alterTypeResp.response;
		} else if (dropTypeResp != null) {
			returnResponse = $dropTypeResp.response;
		} else if (grantResp != null) {
			returnResponse = $grantResp.response;
		} else if (revokeResp != null) {
			returnResponse = $revokeResp.response;
		} else if (createRoleResp != null) {
			returnResponse = $createRoleResp.response;
		} else if (alterRoleResp != null) {
			returnResponse = $alterRoleResp.response;
		} else if (dropRoleResp != null) {
			returnResponse = $dropRoleResp.response;
		} else if (createPermissionResp != null) {
			returnResponse = $createPermissionResp.response;
		} else if (alterPermissionResp != null) {
			returnResponse = $alterPermissionResp.response;
		} else if (dropPermissionResp != null) {
			returnResponse = $dropPermissionResp.response;
		} else if (createTableResp != null) {
			returnResponse = $createTableResp.response;
		} else if (alterTableResp != null) {
			returnResponse = $alterTableResp.response;
		} else if (dropTableResp != null) {
			returnResponse = $dropTableResp.response;
		} else if (createFormResp != null) {
			returnResponse = $createFormResp.response;
		} else if (alterFormResp != null) {http://www.ubuntu.com/products/whatisubuntu/xubuntu
			returnResponse = $alterFormResp.response;
		} else if (dropFormResp != null) {
			returnResponse = $dropFormResp.response;
		} else if (createIndexResp != null) {
			returnResponse = $createIndexResp.response;
		} else if (dropIndexResp != null) {
			returnResponse = $dropIndexResp.response;
		} else if (createTriggerResp != null) {
			returnResponse = $createTriggerResp.response;
		} else if (dropTriggerResp != null) {
			returnResponse = $dropTriggerResp.response;
		} else if (createProcedureResp != null) {
			returnResponse = $createProcedureResp.response;
		} else if (alterProcedureResp != null) {
			returnResponse = $alterProcedureResp.response;
		} else if (dropProcedureResp != null) {
			returnResponse = $dropProcedureResp.response;
		} else if (insertResp != null) {
			returnResponse = $insertResp.response;
		} else if (updateResp != null) {
			returnResponse = $updateResp.response;
		} else if (deleteResp != null) {
			returnResponse = $deleteResp.response;
		} else if (formUpdateResp != null) {
			returnResponse = $formUpdateResp.response;
		} else if (execProcedureResp != null) {
			returnResponse = $execProcedureResp.response;
		} else if (cronResp != null) {
			returnResponse = $cronResp.response;
		} else if (dropCronResp != null) {
			returnResponse = $dropCronResp.response;
		} else if (selectObjectResp != null) {
			returnResponse = $selectObjectResp.response;
		} else if (selectResp != null) {
			returnResponse = $selectResp.response;
		} else if (rawSQLResp != null) {
			returnResponse = $rawSQLResp.response;
		} else if (rawSelectResp != null) {
			returnResponse = $rawSelectResp.response;
		} else if (sysSelectResp != null) {
			returnResponse = $sysSelectResp.response;
		} else if (showResp != null) {
			returnResponse = $showResp.response;
		} else if (describeResp != null) {
			returnResponse = $describeResp.response;
		} else if (commitResp != null) {
			returnResponse = $commitResp.response;
		} else if (rollbackResp != null) {
			returnResponse = $rollbackResp.response;
		} 
		$response = returnResponse;
	} 
	;

query[CodaConnection connection]	returns [CodaResponse response]:
	( 
	 selectObjectResp=selectObjectNoCommit[connection]  
	| selectResp=selectNoCommit[connection]
	| rawSelectResp=rawSelectNoCommit[connection]
	| sysSelectResp=sysSelect
	) 
	{
		CodaResponse returnResponse = new CodaResponse();
		if (selectObjectResp != null) {
			returnResponse = $selectObjectResp.response;
		} else if (selectResp != null) {
			returnResponse = $selectResp.response;
		} else if (rawSelectResp != null) {
			returnResponse = $rawSelectResp.response;
		} else if (sysSelectResp != null) {
			returnResponse = $sysSelectResp.response;
		} 
		$response = returnResponse;
	} 
	;

dml[CodaConnection connection]	returns [CodaResponse response]:
	( 
	 insertResp=insertNoCommit[connection]
	| updateResp=updateNoCommit[connection]
	| deleteResp=deleteNoCommit[connection]
	| formUpdateResp=formUpdateNoCommit[connection]
	| execProcedureResp=execProcedureNoCommit[connection]
	| rawSQLResp=rawSQLNoCommit[connection]
	) 
	{
		CodaResponse returnResponse = new CodaResponse();
		if (insertResp != null) {
			returnResponse = $insertResp.response;
		} else if (updateResp != null) {
			returnResponse = $updateResp.response;
		} else if (deleteResp != null) {
			returnResponse = $deleteResp.response;
		} else if (formUpdateResp != null) {
			returnResponse = $formUpdateResp.response;
		} else if (execProcedureResp != null) {
			returnResponse = $execProcedureResp.response;
		} else if (rawSQLResp != null) {
			returnResponse = $rawSQLResp.response;
		} 
		$response = returnResponse;
	} 
	;

objectName
	:	ObjectName | unreservedKeyword;
	
unreservedKeyword 
	:  'ADDRESS' | 'ALT_PHONE' | 'APPLICATIONS' | 'CITY' | 'CONNECT' | 'COUNTRY' | 'CRONS' | 'DEV' | 'DEVELOPER' | 'DRIVER' | 
	   'EMAIL' | 'FIELDS' | 'FIRST_NAME' | 'FORMAT' | 'FORMS' | 'GROUPS' | 'HOSTNAME' | 'ID' | 'INDEXES' | 'LAST_NAME' | 
	   'MANAGE_APPLICATIONS' | 'MANAGE_DATASOURCES' | 'MANAGE_DEVELOPMENT' | 'MANAGE_GROUPS' | 'MANAGE_PRODUCTION' | 
	   'MANAGE_SESSIONS' | 'MANAGE_TESTING' | 'MANAGE_TYPES' | 'MANAGE_USER_DATA' | 'MANAGE_USERS' | 
	   'MIDDLE_NAME' | 'OPTIONS' | 'ORGANIZATION' | 'PARAMETERS' | 'PASSWORD' | 'PERMISSIONS' | 'PHONE' | 'POSTAL_CODE' | 
	   'PREFIX' | 'PROCEDURES' | 'PROD' | 'QUERY_SYSTEM_TABLES' | 'RELATIONSHIPS' | 'ROLES' | 'ROOT' | 'SAVE_MASK' | 'SCHEMA' | 
	   'SESSIONS' | 'STATE_PROV' | 'STATUSES' | 'TABLES' | 'TEST' | 'TRIGGERS' | 'TYPES' | 'USERNAME' | 'USERS'  | 'VALIDATION_MASK';

userProperty
	:	'FIRST_NAME' | 'MIDDLE_NAME' | 'LAST_NAME' | 'ORGANIZATION' | 'ADDRESS' | 'CITY' | 'STATE_PROV' | 'POSTAL_CODE' | 'COUNTRY' | 'PHONE' | 'ALT_PHONE' | 'EMAIL';
serverPermission
	:	'CONNECT' | 'MANAGE_USERS' | 'MANAGE_USER_DATA' | 'MANAGE_GROUPS' | 'MANAGE_TYPES' | 'MANAGE_APPLICATIONS' | 'MANAGE_DATASOURCES' | 'MANAGE_SESSIONS' | 'QUERY_SYSTEM_TABLES';
applicationPermission
	:	'CONNECT' | 'MANAGE_USERS' | 'DEVELOPER' | 'MANAGE_ROLES' | 'MANAGE_CRONS';
environmentName 
	:	'DEV' | 'TEST' | 'PROD'; 
datasourceProperty
	:	'HOSTNAME' | 'USERNAME' | 'PASSWORD' | 'SCHEMA' | 'DRIVER';
typeProperty
	:	'VALIDATION_MASK' | 'SAVE_MASK';
tableOperation
	:	'INSERT' | 'UPDATE' | 'DELETE';
commit returns [CodaResponse response]
	:	'COMMIT'
	{
		$response =  server.commit(sessionKey);
	}
	;
rollback returns [CodaResponse response]
	:	'ROLLBACK'
	{
		$response =  server.rollback(sessionKey);
	}
	;

connect returns [CodaResponse response]
	:	'CONNECT' user=objectName ':' pass=stringLiteral ('TO' 'APPLICATION' app=objectName '.' env=environmentName ('IN' 'GROUP' gn=objectName)? )?
		{
			String localSessionKey=server.login($user.text, $pass.value); 
			if(sessionKey==null) {
				$response = new CodaResponse(true, null, 1001, "Invalid username/password");
			} else {
				if (app != null) {
					int environment = -1;
					if($env.text.equalsIgnoreCase("DEV")) {
						environment = 1;
					} else if ($env.text.equalsIgnoreCase("TEST")) {
						environment = 2;
					} else if ($env.text.equalsIgnoreCase("PROD")) {
						environment = 3;
					}
					if (environment == -1) {
						$response = new CodaResponse(true, null, 1002, "Invalid environment specified: " + $env.text);
					} else {
						if (server.setSessionApplication(localSessionKey, $app.text, environment, gn != null ? $gn.text : null)) {
							$response = new CodaResponse(false, localSessionKey, -1, null);
						} else {
							server.logout(localSessionKey);
							$response = new CodaResponse(true, null, 1003, "User cannot connect to specified application/environment");
						}
					}
				} else {
					$response = new CodaResponse(false, localSessionKey, -1, null);
				}
			}
		}
	;

disconnect returns [CodaResponse response]
	:	'DISCONNECT'
		{
			server.logout(this.sessionKey);
			$response = new CodaResponse();
		}
	;

setApplication returns [CodaResponse response]
	:	'SET' 'APPLICATION' app=objectName '.' env=environmentName ('IN' 'GROUP' gn=objectName )?
		{
			int environment = -1;
			if($env.text.equalsIgnoreCase("DEV")) {
				environment = 1;
			} else if ($env.text.equalsIgnoreCase("TEST")) {
				environment = 2;
			} else if ($env.text.equalsIgnoreCase("PROD")) {
				environment = 3;
			}
			if (server.setSessionApplication(this.sessionKey, $app.text, environment, gn != null ? $gn.text : null)) {
				$response = new CodaResponse(false, "Success!", -1, null);
			} else {
				$response = new CodaResponse(true, null, 1003, "User cannot connect to specified application/environment");
			}
		}
	;


createDatasource returns [CodaResponse response]
@init {
	Hashtable<String,String> properties = new Hashtable();
	Hashtable<String,String> options = new Hashtable();
}
	:	'CREATE' 'DATASOURCE' dn=objectName (disp=displayedAs)? 
		'(' prop=datasourceParameter { properties.put($prop.property.toUpperCase(), $prop.value); } (',' prop=datasourceParameter { properties.put($prop.property.toUpperCase(), $prop.value); })+ ')' 
		('WITH' 'OPTIONS' '(' opt=propertyValuePair { options.put($opt.property.toUpperCase(), $opt.value); } (',' opt=propertyValuePair { options.put($opt.property.toUpperCase(), $opt.value); } )* ')' )?
		('USING' 'ADMIN' user=stringLiteral ':' pass=stringLiteral )?
		{
			
			$response =  server.createDatasource(sessionKey, $dn.text, (disp == null ? null : $disp.displayName), properties, options, (user == null ? null : $user.value), (pass == null ? null : $pass.value));
		}
	;

alterDatasource returns [CodaResponse response]
@init {
	Hashtable<String,String> properties = new Hashtable();
	Hashtable<String,String> options = new Hashtable();
}
	:	'ALTER' 'DATASOURCE' dn=objectName ( disp=setDisplay | ndn=renameTo | 'SET' prop=datasourceParameter { properties.put($prop.property.toUpperCase(), $prop.value); } (',' prop=datasourceParameter { properties.put($prop.property.toUpperCase(), $prop.value); } )* | 'SET' 'OPTION' opt=propertyValuePair { options.put($opt.property.toUpperCase(), $opt.value); } (',' opt=propertyValuePair { options.put($opt.property.toUpperCase(), $opt.value); } )* )  
		{
			if(disp != null) {
				$response = server.alterDatasource(sessionKey, $dn.text, 1, null, $disp.displayName, null, null);
			} else if (ndn != null) {
				$response =  server.alterDatasource(sessionKey, $dn.text, 2, $ndn.newName, null, null, null);
			} else if (prop != null) {
				$response = server.alterDatasource(sessionKey, $dn.text, 3, null, null, properties, options);
			} else if (opt != null) {
				$response =  server.alterDatasource(sessionKey, $dn.text, 4, null, null, properties, options);
			}
		}
	;
	
dropDatasource returns [CodaResponse response]
	:	'DROP' 'DATASOURCE' dn=objectName
		{
			$response =  server.dropDatasource(sessionKey, $dn.text);
		}
	;
	
createApplication returns [CodaResponse response]
	:	'CREATE' (gf='GROUP')? 'APPLICATION' an=objectName (disp=displayedAs )? 'ON' 'DATASOURCE' dn=objectName ('PREFIX' pre=objectName )? 
		{
			$response = server.createApplication(sessionKey, $an.text, (disp == null ? null :$disp.displayName), (gf == null ? false : true), $dn.text, pre != null ? $pre.text : null);
		}
	;

alterApplication returns [CodaResponse response]
	:	'ALTER' 'APPLICATION' an=objectName (disp=setDisplay | nan=renameTo | 'SET' env=environmentName 'DATASOURCE' dn=objectName)
		{
			if (disp != null) {
				$response = server.alterApplication(sessionKey, $an.text, 1, null, $disp.displayName, null, null);
			} else if (nan != null) {
				$response = server.alterApplication(sessionKey, $an.text, 2, $nan.newName, null, null, null);
			} else {
				$response = server.alterApplication(sessionKey, $an.text, 3, null, null, $env.text, $dn.text);
			}
		}
	;

dropApplication returns [CodaResponse response]
	:	'DROP' 'APPLICATION' an=objectName
		{
			$response = server.dropApplication(sessionKey, $an.text);
		}
	;

createUser returns [CodaResponse response]
@init {
	Hashtable properties = new Hashtable();
}
	:	'CREATE' 'USER' user=objectName 'IDENTIFIED' 'BY' pass=stringLiteral ('AS' rob='ROBOT' )?
		('(' prop+=userParameter {properties.put(((userParameter_return)prop).property.toUpperCase(), ((userParameter_return)prop).value);} (',' prop+=userParameter {properties.put(((userParameter_return)prop).property.toUpperCase(), ((userParameter_return)prop).value);} )+ ')')?
		{
			
			$response = server.createUser(sessionKey, $user.text, $pass.value, (rob == null ? false : true), properties);
		}
	;
	
alterUser returns [CodaResponse response]
@init {
	Hashtable properties = new Hashtable();
}
	:	'ALTER' 'USER' user=objectName 'SET' ( 
			'PASSWORD' '=' pass=stringLiteral | 
			prop+=userParameter {properties.put(((userParameter_return)prop).property.toUpperCase(), ((userParameter_return)prop).value);} (',' prop+=userParameter {properties.put(((userParameter_return)prop).property.toUpperCase(), ((userParameter_return)prop).value);} )* )
		{
			if (pass != null) {
				$response = server.alterUser(sessionKey, $user.text, 1, $pass.value, null);
			} else {
				$response = server.alterUser(sessionKey, $user.text, 2, null, properties);
			}
		}
	;
	
dropUser returns [CodaResponse response]
	:	'DROP' 'USER' user=objectName
		{
			$response =  server.dropUser(sessionKey, $user.text);
		}
	;

createGroup returns [CodaResponse response]
	:	'CREATE' 'GROUP' gn=objectName (disp=displayedAs )?
		{
			$response = server.createGroup(sessionKey, $gn.text, (disp == null ? $gn.text : $disp.displayName));
		}
	;
	
alterGroup returns [CodaResponse response]
	:	'ALTER' 'GROUP' gn=objectName (ngn=renameTo | disp=setDisplay | 'ADD' 'USER' user=objectName | 'REMOVE' 'USER' user2=objectName)
		{
			if (disp != null) {
				$response = server.alterGroup(sessionKey, $gn.text, 1, null, $disp.displayName, null);
			} else if (ngn != null) {
				$response = server.alterGroup(sessionKey, $gn.text, 2, $ngn.newName, null, null);
			} else if (user != null) {
				$response = server.alterGroup(sessionKey, $gn.text, 3, null, null, $user.text);
			} else {
				$response = server.alterGroup(sessionKey, $gn.text, 4, null, null, $user2.text);
			}
			
		}
	;

dropGroup returns [CodaResponse response]
	:	'DROP' 'GROUP' gn=objectName
		{
			$response = server.dropGroup(sessionKey, $gn.text);
		}
	;	
	
createType returns [CodaResponse response]
@init {
	Hashtable<String,String> properties = new Hashtable();
}
	:	'CREATE' 'TYPE' tn=objectName 'AS' '(' prop=typeParameter { properties.put($prop.property.toUpperCase(), $prop.value); } ',' prop=typeParameter { properties.put($prop.property.toUpperCase(), $prop.value); } ')'
		{
			if (!properties.containsKey("VALIDATION_MASK") && !properties.containsKey("SAVE_MASK")) {
				$response = new CodaResponse(true, null, 2021);
			} else {
				$response = server.createType(sessionKey, $tn.text, properties.get("VALIDATION_MASK"), properties.get("SAVE_MASK"));
			}
		}
	;

alterType returns [CodaResponse response]
@init {
	Hashtable<String,String> properties = new Hashtable();
}
	:	'ALTER' 'TYPE' tn=objectName 'SET' prop=typeParameter { properties.put($prop.property.toUpperCase(), $prop.value); } ',' prop=typeParameter { properties.put($prop.property.toUpperCase(), $prop.value); }
		{
			if (!properties.containsKey("VALIDATION_MASK") && !properties.containsKey("SAVE_MASK")) {
				$response = new CodaResponse(true, null, 2021);
			} else {
				$response = server.alterType(sessionKey, $tn.text, properties.get("VALIDATION_MASK"), properties.get("SAVE_MASK"));
		}	}
	;
	
dropType returns [CodaResponse response]
	:	'DROP' 'TYPE' tn=objectName
		{
			$response =  server.dropType(sessionKey, $tn.text);
		}
	;
	
formatDatasource returns [CodaResponse response]
	:	'FORMAT' 'DATASOURCE' dn=objectName ('FOR' co='CODA' 'ROOT' 'IDENTIFIED' 'BY' pass=stringLiteral  | 'FOR' 'APPLICATION' an=objectName 'PREFIX' pre=objectName ('REVISION' rev=Integer )? )
	{
		if (co != null) {
			$response = server.formatDatasourceForCoda(sessionKey, $dn.text, $pass.value);
		} else {
			$response = server.formatDatasourceForApplication(sessionKey, $dn.text, $an.text, (rev == null ? -1 : Long.parseLong($rev.text)), $pre.text, true);
		}
	}
	;

promoteApplication returns [CodaResponse response]
	:	'PROMOTE' 'APPLICATION' an=objectName 'ON' (en=environmentName | 'DATASOURCE' dn=objectName) 'TO' 'REVISION' rev=Integer
	{
		String datasource = dn != null ? $dn.text : null;
		int environment = -1;
		
		if (datasource == null) {
			environment = 1;
			if (en != null) {
		            if ($en.text.equalsIgnoreCase("DEV")) {
		                environment = 1;
		            } else if ($en.text.equalsIgnoreCase("TEST")) {
		                environment = 2;
		            } else if ($en.text.equalsIgnoreCase("PROD")) {
		                environment = 3;
		            } 
		        }
			datasource = server.getApplicationDatasourceName($an.text, environment);
		        if (datasource == null) {
		        	$response = new CodaResponse(true, null, 8003);
		        }
		}
		if ($response == null) {
			$response = server.formatDatasourceForApplication(sessionKey, datasource, $an.text, Long.parseLong($rev.text), "", false);
			
			if (!$response.getError() && environment > -1) {
				server.sendApplicationUpdateToCluster($an.text, $en.text);
				server.getDeployedApplication($an.text).updateEnvironmentClassLoader(environment, server.getClassLoader());
			}
		}
	}
	;


grant returns [CodaResponse response]
@init {
	Vector<String> permissions = new Vector();
}	
	:	'GRANT' 
		( 
			sp=serverPermission { permissions.add($sp.text); } (',' sp=serverPermission { permissions.add($sp.text); } )* 'TO' user=objectName
		|	ap=applicationPermission { permissions.add($ap.text); } (',' ap=applicationPermission { permissions.add($ap.text); } )* 'ON' an=objectName ('.' env=environmentName )? ('FOR' 'GROUP' gn=objectName )? 'TO' user=objectName
		|	'CONNECT' 'ON' an=objectName 'TO' 'GROUP' group_name=objectName
		|	'ROLE' assigned_role=objectName 'ON' an=objectName '.' env=environmentName ('FOR' 'GROUP' gn=objectName )? 'TO' user=objectName
		|	permission=objectName { permissions.add($permission.text); } (',' permission=objectName { permissions.add($permission.text); } )? ('IN' an=objectName )? 'TO' rn=objectName
		|	table_permission=tablePermission { permissions.add($table_permission.text); } (',' table_permission=tablePermission { permissions.add($table_permission.text); } )* 'ON' 'TABLE' aname=applicationPrefix? tn=objectName 'TO' rn=objectName
		|	fs_permissions=formStatusPermission { permissions.add($fs_permissions.text); } (',' fs_permissions=formStatusPermission { permissions.add($fs_permissions.text); } )* 'ON' 'FORM' aname=applicationPrefix? fn=objectName ':' fsn=objectName 'TO' rn=objectName
		|	procedure_permission=procedurePermission { permissions.add($procedure_permission.text); } (',' procedure_permission=procedurePermission { permissions.add($procedure_permission.text); } )* 'ON' 'PROCEDURE' aname=applicationPrefix? pn=objectName 'TO' rn=objectName
		)
		{
			if (sp != null) {
				$response = server.grantServerPermissions(sessionKey, permissions, $user.text);
			} else if (ap != null) {
				$response = server.grantApplicationPermissions(sessionKey, permissions, $an.text, (env == null ? null : $env.text), (gn == null ? null : $gn.text), $user.text);
			} else if (group_name != null) {
				$response = server.grantApplicationToGroup(sessionKey, $an.text, $group_name.text);
			} else if (assigned_role != null) {
				$response = server.grantRoleToUser(sessionKey, $assigned_role.text, $an.text, $env.text, (gn == null ? null : $gn.text), $user.text);
			} else if (permission != null) {
				CodaResponse resp = server.grantPermissionsToRole (sessionKey, this.getDatabase(), permissions , an != null ? $an.text : null, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, an != null ? $an.text : null, null);
				}
				$response = resp;
			} else if (table_permission != null ) {
				CodaResponse resp = server.grantTablePermissionsToRole (sessionKey, this.getDatabase(), permissions , aname != null ? $aname.applicationName : null, $tn.text, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, aname != null ? $aname.applicationName : null, null);
				}
				$response = resp;
			} else if (fs_permissions != null ) {
				CodaResponse resp = server.grantFormStatusPermissionsToRole (sessionKey, this.getDatabase(),permissions , aname != null ? $aname.applicationName : null, $fn.text, $fsn.text, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, aname != null ? $aname.applicationName : null, null);
				}
				$response = resp;
			} else if (procedure_permission != null ) {
				CodaResponse resp = server.grantProcedurePermissionsToRole (sessionKey, this.getDatabase(), permissions , aname != null ? $aname.applicationName : null, $pn.text, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, aname != null ? $aname.applicationName : null, null);
				}
				$response = resp;
			}
			
		}
		;
		
revoke returns [CodaResponse response]
@init {
	Vector<String> permissions = new Vector();
}	
	:	'REVOKE'
		(
			sp=serverPermission { permissions.add($sp.text); } (',' sp=serverPermission { permissions.add($sp.text); } )* 'FROM' user=objectName  
		|	ap=applicationPermission { permissions.add($ap.text); } (',' ap=applicationPermission { permissions.add($ap.text); } )* 'ON' an=objectName ('.' env=environmentName )? ('FOR' 'GROUP' gn=objectName )? 'FROM' user=objectName
		|	'CONNECT' 'ON' an=objectName 'FROM' 'GROUP' group_name=objectName
		|	'ROLE' assigned_role=objectName 'ON' an=objectName '.' env=environmentName ('FOR' 'GROUP' gn=objectName )?  'FROM' user=objectName
		|	permission=objectName { permissions.add($permission.text); } (',' permission=objectName { permissions.add($permission.text); } )? ('IN' an=objectName )? 'FROM' rn=objectName
		|	table_permission=tablePermission { permissions.add($table_permission.text); } (',' table_permission=tablePermission { permissions.add($table_permission.text); } )* 'ON' 'TABLE' aname=applicationPrefix? tn=objectName 'FROM' rn=objectName
		|	fs_permission=formStatusPermission { permissions.add($fs_permission.text); } (',' fs_permission=formStatusPermission { permissions.add($fs_permission.text); } )* 'ON' 'FORM' aname=applicationPrefix? fn=objectName ':' fsn=objectName 'FROM' rn=objectName
		|	procedure_permission=procedurePermission { permissions.add($procedure_permission.text); } (',' procedure_permission=procedurePermission { permissions.add($procedure_permission.text); } )* 'ON' 'PROCEDURE' aname=applicationPrefix? pn=objectName 'FROM' rn=objectName
		)
		{
			if (sp != null) {
				$response = server.revokeServerPermissions(sessionKey, permissions, $user.text);
			} else if (ap != null) {
				$response = server.revokeApplicationPermissions(sessionKey, permissions, $an.text, (env == null ? null : $env.text), (gn == null ? null : $gn.text), $user.text);
			} else if (group_name != null) {
				$response = server.revokeApplicationFromGroup(sessionKey, $an.text, $group_name.text);
			} else if (assigned_role != null) {
				$response = server.revokeRoleFromUser(sessionKey, $assigned_role.text, $an.text, $env.text, (gn == null ? null : $gn.text), $user.text);
			} else if (permission != null ) {
				CodaResponse resp = server.revokePermissionsFromRole (sessionKey, this.getDatabase(), permissions , an != null ? $an.text : null, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, an != null ? $an.text : null, null);
				}
				$response = resp;
			} else if (table_permission != null ) {
				CodaResponse resp = server.revokeTablePermissionsFromRole (sessionKey, this.getDatabase(), permissions , aname != null ? $aname.applicationName : null, $tn.text, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, aname != null ? $aname.applicationName : null, null);
				}
				$response = resp;
			} else if (fs_permission != null ) {
				CodaResponse resp = server.revokeFormStatusPermissionsFromRole (sessionKey, this.getDatabase(), permissions , aname != null ? $aname.applicationName : null, $fn.text, $fsn.text, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, aname != null ? $aname.applicationName : null, null);
				}
				$response = resp;
			} else if (procedure_permission != null ) {
				CodaResponse resp = server.revokeProcedurePermissionsFromRole (sessionKey, this.getDatabase(), permissions , aname != null ? $aname.applicationName : null, $pn.text, $rn.text);
				if (!resp.getError()) {
					logTransaction($text, aname != null ? $aname.applicationName : null, null);
				}
				$response = resp;
			}
		};	
		
createRole returns [CodaResponse response]
	:	'CREATE' 'ROLE' rn=objectName da=displayedAs? an=inApplication?
		{
			CodaResponse resp = server.createRole (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $rn.text, da != null ? $da.displayName : null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response =  resp;
		}
	;
	
alterRole returns [CodaResponse response]
	:	'ALTER' 'ROLE' an=applicationPrefix? rn=objectName 
	( 
		rnt=renameTo 
	| 	da=setDisplay 
	)
		{
			int operation = 1;
			if ($rnt.newName != null) {
				operation = 2;
			}
		
			CodaResponse resp = server.alterRole (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $rn.text, operation, rnt != null ? $rnt.newName : null, da != null ? $da.displayName : null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response =  resp;
		}
	;
	
dropRole returns [CodaResponse response]
	:	'DROP' 'ROLE' an=applicationPrefix? rn=objectName
		{
			CodaResponse resp = server.dropRole (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $rn.text);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response =  resp;
		}
	;

createPermission returns [CodaResponse response]
	:	'CREATE' 'PERMISSION' pn=objectName da=displayedAs? an=inApplication?
		{
			CodaResponse resp = server.createPermission (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $pn.text, da != null ? $da.displayName : null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response =  resp;
		}
	;
	
alterPermission returns [CodaResponse response]
	:	'ALTER' 'PERMISSION' an=applicationPrefix? pn=objectName 
	( 
		rnt=renameTo 
	| 	da=setDisplay 
	)
		{
			int operation = 1;
			if (rnt != null) {
				operation = 2;
			}
		
			CodaResponse resp = server.alterPermission (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $pn.text, operation, rnt != null ? $rnt.newName : null, da != null ? $da.displayName : null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response = resp;
		}
	;
	
dropPermission returns [CodaResponse response]
	:	'DROP' 'PERMISSION' an=applicationPrefix? pn=objectName
		{
			CodaResponse resp = server.dropPermission (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $pn.text);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response = resp;
		}
	;

createTable returns [CodaResponse response]
@init {
	Vector<TableFieldDefinition> fields = new Vector();
}	
	:	'CREATE' (rf='REF' | gf='GROUP' )? ('TABLE' tn=objectName | 'SUBTABLE' tn=objectName 'OF' ptn=objectName) da=displayedAs? an=inApplication? ('WITH' 'SOFT' sdf='DELETE' )?
		'(' tfd=tableFieldDefinitionWithIdentity { fields.add(tfd.def); } (',' tfd=tableFieldDefinitionWithIdentity { fields.add(tfd.def); } )* ')'
		{
			CodaResponse resp = server.createTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, da != null ? $da.displayName : null, (gf == null ? false : true), (rf == null ? false : true), ptn != null ? $ptn.text : null, (sdf == null ? false : true), fields, this.database == null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, $tn.text);
			}

			$response = resp;
		}
	; 

alterTable returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
}	
	:	'ALTER' 'TABLE' an=applicationPrefix? tn=objectName 
	(
		rnt=renameTo
	|	dn=setDisplay
	|	'ADD' 'COLUMN' nc=tableFieldDefinition
	|	'ALTER' 'COLUMN' cn=objectName 'SET' cd=tableFieldDefinition
	|	'DROP' 'COLUMN' drop_n=objectName
	|	'SET' 'IDENTITY' '(' icn=objectName { fields.add($icn.text); } (',' icn=objectName { fields.add($icn.text); } )* ')' 
	)
		{
			CodaResponse resp = new CodaResponse(true, null, 8005);
			if (dn != null) {
				resp = server.alterTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 1, null, $dn.displayName, null, null, null, this.database == null);	
			} else if (rnt != null) {
				resp = server.alterTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 2, $rnt.newName, null, null, null, null, this.database == null);	
			} else if (nc != null) {
				resp = server.alterTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 3, null, null, $nc.def, null, null, this.database == null);	
			} else if (cd != null) {
				resp = server.alterTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 4, null, null, $cd.def, $cn.text, null, this.database == null);	
			} else if (drop_n != null) {
				resp = server.alterTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 5, null, null, null, $drop_n.text, null, this.database == null);	
			} else if (fields.size() != 0) {
				resp = server.alterTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 6, null, null, null, null, fields, this.database == null);	
			}	
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, $tn.text);
			}
			$response =  resp;
		}
	;
	
dropTable returns [CodaResponse response]
	:	'DROP' 'TABLE' an=applicationPrefix? tn=objectName
		{
			boolean refTable = server.isRefTable(an != null ? $an.applicationName : server.getSessionApplication(sessionKey), $tn.text);
			CodaResponse resp = server.dropTable (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, refTable);
			}
			$response =  resp;
		}
	;

createForm returns [CodaResponse response]
@init {
	Vector<TableFieldDefinition> fields = new Vector();
	Vector<FormStatusDefinition> statuses = new Vector();
}
	:	'CREATE' ( gf='GROUP' )? ('FORM' tn=objectName | 'SUBFORM' tn=objectName 'OF' ptn=objectName )  da=displayedAs? an=inApplication?
		'(' tfd=formFieldDefinitionWithIdentity { fields.add(tfd.def); } (',' tfd=formFieldDefinitionWithIdentity { fields.add(tfd.def); } )* ')'	
		'WITH' 'STATUSES' fsd=formStatusCreationDefinition { statuses.add(fsd.def); } (',' fsd=formStatusCreationDefinition { statuses.add(fsd.def); } )*
		{
			CodaResponse resp = server.createForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, da != null ? $da.displayName : null, (gf == null ? false : true), ptn != null ? $ptn.text : null, fields, statuses, this.database == null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, $tn.text);
			}
			$response = resp;
		}
	;

alterForm returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
	Vector<FormStatusLeadsTo> statuses = new Vector();
}
	:	'ALTER' 'FORM' an=applicationPrefix? tn=objectName
	(
		rnt=renameTo
	|	dn=setDisplay
	|	'ADD' 'FIELD' nc=formFieldDefinition
	|	'ALTER' 'FIELD' cn=objectName 'SET' cd=formFieldDefinition
	|	'DROP' 'FIELD' drop_n=objectName
	|	'SET' 'IDENTITY' '(' icn=objectName { fields.add($icn.text); } (',' icn=objectName { fields.add($icn.text); } )* ')'
	|	'ADD' 'STATUS' nfs=formStatusCreationDefinition
	|	'ALTER' 'STATUS' fsn=objectName 'SET' fsd=formStatusCreationDefinition
	|	'DROP' 'STATUS' drop_fsn=objectName
	|	'SET' 'STATUS' 'ORDER' fslt=formStatusDefinition { statuses.add($fslt.status); } (',' fslt=formStatusDefinition { statuses.add($fslt.status); } )*
	)	
		{
			CodaResponse resp = new CodaResponse(true, null, 8005);
			if (dn != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 1, null, $dn.displayName, null, null, null, null,null,null, this.database == null);	
			} else if (rnt != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 2, $rnt.newName, null, null, null, null, null,null,null, this.database == null);	
			} else if (nc != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 3, null, null, $nc.def, null, null, null,null,null, this.database == null);	
			} else if (cd != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 4, null, null, $cd.def, $cn.text, null, null,null,null, this.database == null);	
			} else if (drop_n != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 5, null, null, null, $drop_n.text, null, null,null,null, this.database == null);	
			} else if (fields.size() != 0) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 6, null, null, null, null, fields, null,null,null, this.database == null);	
			} else if (nfs != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 7, null, null, null, null, null, $nfs.def ,null,null, this.database == null);	
			} else if (fsd != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 8, null, null, null, null, null, $fsd.def,$fsn.text,null, this.database == null);	
			} else if (drop_fsn != null) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 9, null, null, null, null, null, null,$drop_fsn.text,null, this.database == null);	
			} else if (statuses.size() != 0) {
				resp = server.alterForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, 10, null, null, null, null, null, null,null, statuses, this.database == null);	
			}	
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, $tn.text);
			}
			$response = resp;
		}
	;

dropForm returns [CodaResponse response]
	:	'DROP' 'FORM' an=applicationPrefix? tn=objectName
		{
			boolean refTable = server.isRefTable(an != null ? $an.applicationName : server.getSessionApplication(sessionKey), $tn.text);
			CodaResponse resp = server.dropForm (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, refTable);
			}
			$response = resp;
		}
	;

createIndex returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
}
	:	'CREATE' 'INDEX' in=objectName 'ON' an=applicationPrefix? tn=objectName '(' cn=objectName { fields.add($cn.text); } (',' cn=objectName { fields.add($cn.text); } )* ')' (uf='UNIQUE' )?
		{
			CodaResponse resp = server.createIndex (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $in.text, $tn.text, fields, (uf != null));
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response = resp;
		}
	;

dropIndex returns [CodaResponse response]
	:	'DROP' 'INDEX' an=applicationPrefix? in=objectName
		{
			CodaResponse resp = server.dropIndex (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $in.text);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, null);
			}
			$response = resp;
		}
	;

cron returns [CodaResponse response]
@init {
	Vector<String> params = new Vector();
}
	:	'CRON' cn=objectName 'ON' an=objectName '.' env=environmentName 'IS' min=cronValue hour=cronValue dom=cronValue mon=cronValue dow=cronValue 
		pn=objectName ('(' (param=stringLiteral { params.add($param.value); } | param2=arrayLiteral { params.add($param2.text); } ) (',' (param=stringLiteral { params.add($param.value); } | param2=arrayLiteral { params.add($param2.text); }) )* ')' )?
		('AS' euser=objectName ':' epass=stringLiteral )?
		{
			CodaResponse resp = server.createCron (sessionKey, $an.text, $env.text, $cn.text, $min.value, $hour.value, $dom.value, $mon.value, $dow.value, $pn.text, params, euser != null ? $euser.text : null, epass != null ? $epass.value : null);
			$response = resp;
		}
	;
	
dropCron returns [CodaResponse response]
	:	'DROP' 'CRON' cn=objectName 'ON' an=objectName '.' env=environmentName
		{
			CodaResponse resp = server.dropCron (sessionKey, $an.text, $env.text, $cn.text);
			$response = resp;
		}
	;

selectObject returns [CodaResponse response]
	:	'SELECTOBJECT' 'FROM' tn=objectName 'WHERE' 'ID' '=' id=Integer (gr='GREEDY')?
	{
		$response = server.selectObject(sessionKey, $tn.text, Long.parseLong($id.text), (gr == null ? false : true), null);
	}
	;

selectObjectNoCommit[CodaConnection connection] returns [CodaResponse response]
	:	'SELECTOBJECT' 'FROM' tn=objectName 'WHERE' 'ID' '=' id=Integer (gr='GREEDY')?
	{
		$response = server.selectObject(sessionKey, $tn.text, Long.parseLong($id.text), (gr == null ? false : true), connection);
	}
	;

rawSQL returns [CodaResponse response]	
	:	'RAW' 'SQL' ':' sql=rawSQLBody
	{
		$response = server.rawStatement(sessionKey, $sql.text, null);
	}
	;	

rawSQLNoCommit[CodaConnection connection] returns [CodaResponse response]	
	:	'RAW' 'SQL' ':' sql=rawSQLBody
	{
		$response = server.rawStatement(sessionKey, $sql.text, connection);
	}
	;	

rawSelect returns [CodaResponse response]	
	:	'RAW' 'SELECT' ':' sql=rawSQLBody
	{
		$response = server.rawSelect(sessionKey, $sql.text, null);
	}
	;
rawSelectNoCommit[CodaConnection connection] returns [CodaResponse response]	
	:	'RAW' 'SELECT' ':' sql=rawSQLBody
	{
		$response = server.rawSelect(sessionKey, $sql.text, connection);
	}
	;	


createTrigger returns [CodaResponse response]
	:	cmd=('CREATE' | 'REPLACE' ) 'TRIGGER' 'ON'
	(	'TABLE' (an=applicationPrefix)? tn=objectName (bf='BEFORE' | 'AFTER') op=tableOperation
	|	'FORM' (an=applicationPrefix)? tn=objectName (bf='BEFORE' | 'AFTER') opo=objectName )
		'AS' tb=triggerBody 'ENDTRIGGER'
		{
			CodaResponse resp = server.createReplaceTrigger (sessionKey, this.getDatabase(), $cmd.text, an != null ? $an.applicationName : null, $tn.text, op == null ? $opo.text : $op.text, bf != null,$tb.text, this.database == null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, "");
			}
			$response = resp;
		}
	;
dropTrigger returns [CodaResponse response]
	:	'DROP' 'TRIGGER' 'ON'
	(	'TABLE' (an=applicationPrefix)? tn=objectName bf=('BEFORE' | 'AFTER') op=tableOperation
	|	'FORM' (an=applicationPrefix)? tn=objectName bf=('BEFORE' | 'AFTER') opo=objectName )
		{
			CodaResponse resp = server.dropTrigger (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $tn.text, op == null ? $opo.text : $op.text, $bf.text.equalsIgnoreCase("BEFORE"));
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, "");
			}
			$response = resp;
		}
	;
	
createProcedure returns [CodaResponse response]
@init {
	Vector<ProcedureParameter> params = new Vector();
}
	:	cmd=('CREATE' | 'REPLACE') 'PROCEDURE' pn=objectName (an=inApplication)?  
		('(' param=procedureParameter { params.add($param.parameter); }  (',' param=procedureParameter { params.add($param.parameter); } )* ')' )?
		'RETURNS' rv=objectName (rva='ARRAY')? 'AS' procBody=procedureBody 'ENDPROCEDURE'
		{
			CodaResponse resp = server.createReplaceProcedure (sessionKey, this.getDatabase(), $cmd.text, an != null ? $an.applicationName : null, $pn.text, params, $rv.text, rva != null, $procBody.text, this.database == null);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, "");
			}
			$response = resp;
		}
	;

alterProcedure returns [CodaResponse response]
	:	'ALTER' 'PROCEDURE' (an=applicationPrefix)? pn=objectName npn=renameTo
		{
			CodaResponse resp = server.alterProcedure (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $pn.text, $npn.newName);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, "");
			}
			$response = resp;
		}
	;
	
dropProcedure returns [CodaResponse response]
	:	'DROP' 'PROCEDURE' (an=applicationPrefix)? pn=objectName
		{
			CodaResponse resp = server.dropProcedure (sessionKey, this.getDatabase(), an != null ? $an.applicationName : null, $pn.text);
			if (!resp.getError()) {
				logTransaction($text, an != null ? $an.applicationName : null, "");
			}
			$response = resp;
		}
	;
	
execProcedure returns [CodaResponse response]
@init {
	Vector<String> params = new Vector();
}
	:	'EXEC' 'PROCEDURE' pn=objectName ('(' ( param=stringLiteral { params.add($param.value); } | param2=arrayLiteral { params.add($param2.text); } ) (',' ( param=stringLiteral { params.add($param.value); } | param2=arrayLiteral { params.add($param2.text); }) )* ')' )?		
		{
			CodaResponse resp = server.execProcedure (sessionKey, $pn.text, params, null);
			$response = resp;
		}
	;
execProcedureNoCommit[CodaConnection connection] returns [CodaResponse response]
	:	'EXEC' 'PROCEDURE' pn=objectName ('(' param+=( stringLiteral | arrayLiteral ) (',' param+=( stringLiteral | arrayLiteral) )* ')' )?		
		{
			CodaResponse resp = server.execProcedure (sessionKey, $pn.text, new Vector($param), connection);
			$response = resp;
		}
	;
	
show returns [CodaResponse response]
	:	'SHOW'  (
			us='USERS' ('IN' 'GROUP' gn=objectName )? ('FOR' 'APPLICATION' appn=objectName '.' envn=environmentName)? 
		|	tgr='GROUPS' ('FOR' 'APPLICATION' appn=objectName '.' envn=environmentName)?
		|	ty='TYPES'
		|	ds='DATASOURCES'
		|	sess='SESSIONS'
		|	app='APPLICATIONS' ('FOR' 'GROUP' gn=objectName )?
		|	sysRoles='SERVER' 'PERMISSIONS' 'FOR' 'USER' uname=objectName
		|	appRoles='APPLICATION' 'PERMISSIONS' 'FOR' 'USER' uname=objectName ('IN' 'GROUP' gn=objectName)?
		|	sys='SYS' 'INFO'
		|	appInfo='APP' 'INFO'
		|	tables='TABLES'
		|	forms='FORMS'
		|	procedures='PROCEDURES'
		|	triggers='TRIGGERS' 'FOR' ('TABLE' | 'FORM' ) trigTable=objectName
		|	indexes='INDEXES'
		|	crons='CRONS'
		|	roles='ROLES' ('FOR' 'USER' uname=objectName ('IN' 'GROUP' gn=objectName)? )?
		|	perms='PERMISSIONS' ('FOR' 'USER' uname=objectName ('IN' 'GROUP' gn=objectName)? | 'FOR' 'ROLE' rn=objectName )?
		|	tabPerms='TABLE' 'PERMISSIONS' ('FOR' 'USER' uname=objectName ('IN' 'GROUP' gn=objectName)? | 'FOR' 'ROLE' rn=objectName )?
		|	formPerms='FORM' 'PERMISSIONS' ('FOR' 'USER' uname=objectName ('IN' 'GROUP' gn=objectName)? | 'FOR' 'ROLE' rn=objectName  )?
		|	procPerms='PROCEDURE' 'PERMISSIONS' ('FOR' 'USER' uname=objectName ('IN' 'GROUP' gn=objectName)? | 'FOR' 'ROLE' rn=objectName  )?
			)
			(wc=whereClause)? (obc=orderByClause)? 
		{
			CodaResponse resp = new CodaResponse();
			if (us != null) {
				resp = server.show (sessionKey, $us.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, gn != null ? $gn.text : null, null, appn != null ? $appn.text : null, envn != null ? $envn.text : null);
			} else if (tgr != null) {
				resp = server.show (sessionKey, $tgr.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, appn != null ? $appn.text : null, envn != null ? $envn.text : null);
			} else if (ty != null) {
				resp = server.show (sessionKey, $ty.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (ds != null) {
				resp = server.show (sessionKey, $ds.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (sess != null) {
				resp = server.show (sessionKey, $sess.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (app != null) {
				resp = server.show (sessionKey, $app.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, gn != null ? $gn.text : null, null, null, null);
			} else if (sysRoles != null) {
				resp = server.show (sessionKey, $sysRoles.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null,  uname != null ? $uname.text : null, null, null, null, null);
			} else if (appRoles != null) {
				resp = server.show (sessionKey, $appRoles.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null,  uname != null ? $uname.text : null, gn != null ? $gn.text : null, null, null, null);
			} else if (sys != null) {
				resp = server.show (sessionKey, $sys.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (appInfo != null) {
				resp = server.show (sessionKey, $appInfo.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (tables != null) {
				resp = server.show (sessionKey, $tables.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (forms != null) {
				resp = server.show (sessionKey, $forms.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (procedures != null) {
				resp = server.show (sessionKey, $procedures.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (triggers != null) {
				resp = server.show (sessionKey, $triggers.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, trigTable != null ? $trigTable.text : null, null, null, null, null, null);
			} else if (indexes != null) {
				resp = server.show (sessionKey, $indexes.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (crons != null) {
				resp = server.show (sessionKey, $crons.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, null, null, null, null, null);
			} else if (roles != null) {
				resp = server.show (sessionKey, $roles.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, uname != null ? $uname.text : null, gn != null ? $gn.text : null, null, null, null);
			} else if (perms != null) {
				resp = server.show (sessionKey, $perms.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, uname != null ? $uname.text : null, gn != null ? $gn.text : null, rn != null ? $rn.text : null, null, null);
			} else if (tabPerms != null) {
				resp = server.show (sessionKey, $tabPerms.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, uname != null ? $uname.text : null, gn != null ? $gn.text : null, rn != null ? $rn.text : null, null, null);
			} else if (formPerms != null) {
				resp = server.show (sessionKey, $formPerms.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, uname != null ? $uname.text : null, gn != null ? $gn.text : null, rn != null ? $rn.text : null, null, null);
			} else if (procPerms != null) {
				resp = server.show (sessionKey, $procPerms.text, wc != null ? $wc.value : null, obc != null ? $obc.value : null, null, uname != null ? $uname.text : null, gn != null ? $gn.text : null, rn != null ? $rn.text : null, null, null);
			}
			$response = resp;
		}
		
		;
describe returns [CodaResponse response] 
	:
	'DESCRIBE' (
		'USER' un=objectName
	|	'GROUP' gn=objectName
	|	'TYPE' tn=objectName
	|	'DATASOURCE' dsn=objectName
	|	'APPLICATION' appn=objectName
	|	'TABLE' tablen=objectName (cols='COLUMNS')?
	|	'FORM' formn=objectName (cols='FIELDS' | status='STATUSES' | statusr='STATUS' 'RELATIONSHIPS')?
	|	'INDEX' indexn=objectName (cols='COLUMNS')?
	|	'PROCEDURE' procn=objectName (params='PARAMETERS')?
	|	'TRIGGER' trigtable=objectName (bef='BEFORE' | 'AFTER') (opo=tableOperation | op=objectName)
	|	'CRON' cronn=objectName (params='PARAMETERS')?
	)
	{
		CodaResponse resp = new CodaResponse();
		if (un !=null) {
			resp = server.describe (sessionKey, "USER", $un.text, null, true, null);
		} else if (gn !=null) {
			resp = server.describe (sessionKey, "GROUP", $gn.text, null, true, null);
		} else if (tn !=null) {
			resp = server.describe (sessionKey, "TYPE", $tn.text, null, true, null);
		} else if (dsn !=null) {
			resp = server.describe (sessionKey, "DATASOURCE", $dsn.text, null, true, null);
		} else if (appn !=null) {
			resp = server.describe (sessionKey, "APPLICATION", $appn.text, null, true, null);
		} else if (tablen !=null) {
			resp = server.describe (sessionKey, "TABLE", $tablen.text, cols != null ? $cols.text : null, true, null);
		} else if (formn !=null) {
			String formOp = null;
			if (cols != null) {
				formOp = $cols.text;
			} else if (status != null) {
				formOp = $status.text;
			} else if (statusr != null) {
				formOp = "STATUS";
			}
			resp = server.describe (sessionKey, "FORM", $formn.text, formOp, true, null);
		} else if (indexn !=null) {
			resp = server.describe (sessionKey, "INDEX", $indexn.text, cols != null ? $cols.text : null, true, null);
		} else if (procn !=null) {
			resp = server.describe (sessionKey, "PROCEDURE", $procn.text, params != null ? $params.text : null, true, null);
		} else if (trigtable !=null) {
			resp = server.describe (sessionKey, "TRIGGER", $trigtable.text, op != null ? $op.text : null, bef != null, opo != null ? $opo.text : null);
		} else if (cronn !=null) {
			resp = server.describe (sessionKey, "CRON", $cronn.text, params != null ? $params.text : null, true, null);
		}
		$response = resp;
	};



// Subgrammars
procedureParameter returns [ProcedureParameter parameter]
	:	pn=objectName pt=objectName (af='ARRAY')?
		{
			$parameter = new ProcedureParameter($pn.text, $pt.text, af != null);
		}
	;

datasourceParameter returns [String property, String value]
	:	p=datasourceProperty '=' v=stringLiteral {$property = $p.text; $value=$v.value; }
	;
userParameter returns [String property, String value]
	:	p=userProperty '=' v=stringLiteral
	{
		$property = $p.text;
		$value = $v.value;
	}
	;
/*	
userParameter returns [String property, String value]
	:	p=UserProperty ':' v=stringLiteral {$property = $p.text; $value=$v.value; }
	;
*/
typeParameter returns [String property, String value]
	:	p=typeProperty '=' v=stringLiteral {$property = $p.text; $value=$v.value; }
	;
propertyValuePair returns [String property, String value]
	:	p=objectName '=' v=stringLiteral {$property = $p.text; $value=$v.value; }
	;
inApplication returns [String applicationName]
	:	'IN' 'APPLICATION' p=objectName {$applicationName = $p.text; }
	;
applicationPrefix returns [String applicationName]
	:	p=objectName '.' {$applicationName = $p.text;}
	;
fieldType returns [String typeName, boolean arrayFlag, boolean refFlag, String refTable]
	:	(t=objectName | r='REFERENCE' 'TO' rt=objectName) a='ARRAY'? 
		{
			if (r == null) {
				$typeName=$t.text; 
				$refFlag=false; 
				$refTable=null;
			}else {
				$typeName="REFERENCE"; 
				$refFlag=true; 
				$refTable=$rt.text;
			} 
			$arrayFlag= !( a == null);
		}
	;
tableFieldDefinition returns [TableFieldDefinition def]
	:	fn=objectName ft=fieldType da=displayedAs? ('NULL' | nn='NOT' 'NULL' )? ('DEFAULT' (sv=systemVariable | sl=stringLiteral | al=arrayLiteral ) )?
		{
			if (sv == null) {
				$def=new TableFieldDefinition($fn.text, $ft.typeName, da != null ? $da.displayName : null, $ft.arrayFlag, $ft.refFlag, $ft.refTable, (al != null ? $al.text : (sl != null ? $sl.value : null))); 
			} else {
				$def=new TableFieldDefinition($fn.text, $ft.typeName, da != null ? $da.displayName : null, $ft.arrayFlag, $ft.refFlag, $ft.refTable, (sv != null ? ($sv.text.equalsIgnoreCase("CURRENT_TIMESTAMP") ? 1 : ($sv.text.equalsIgnoreCase("CURRENT_USERNAME") ? 5 : ($sv.text.equalsIgnoreCase("CURRENT_GROUP_NAME") ? 3 : null))) : null)); 
			}
			$def.setNullableFlag(nn == null);
		}
	;
tableFieldDefinitionWithIdentity returns [TableFieldDefinition def]
	:	tfd=tableFieldDefinition i='IDENTITY'?
		{$def = $tfd.def; $def.setIdentityFlag(!(i == null));}
	;
formFieldDefinition returns [TableFieldDefinition def]
	:	fn=objectName ft=fieldType da=displayedAs? ('DEFAULT' (sv=systemVariable | sl=stringLiteral | al=arrayLiteral ) )?
		{$def=new TableFieldDefinition($fn.text, $ft.typeName, da != null ? $da.displayName : null, $ft.arrayFlag, $ft.refFlag, $ft.refTable, (al != null ? $al.text : (sl != null ? $sl.value : (sv != null ? $sv.text : null))));}
	;
formFieldDefinitionWithIdentity returns [TableFieldDefinition def]
	:	tfd=tableFieldDefinition i='IDENTITY'?
		{$def = $tfd.def; $def.setIdentityFlag(!(i == null));}
	;
formStatusDefinition returns [FormStatusLeadsTo status]
@init {
	Vector<String> statuses = new Vector();
}
	:	sa=objectName 'LEADS' 'TO' ( '(' sna=objectName { statuses.add($sna.text); }  (',' sna=objectName { statuses.add($sna.text); } )* ')' | 'NOTHING' )
		{
			$status = new FormStatusLeadsTo( $sa.text, (statuses.size() == 0 ? null : statuses ));
		}
	;
formStatusCreationDefinition  returns [FormStatusDefinition def ]
	:	'(' sa=objectName sad=displayedAs? ',' sv=objectName svd=displayedAs? (ini='INITIAL')? ')' 
		{
			$def= new FormStatusDefinition($sa.text, sad != null ? $sad.displayName : null, $sv.text, svd != null ? $svd.displayName : null, (ini != null));
		}
	;
	
insertValuesList returns [Vector<String> values]
@init {
	Vector<CodaConstant> constants = new Vector();
}
	:	'(' vl=insertUpdateValue { constants.add($vl.value); }  (',' vl=insertUpdateValue { constants.add($vl.value); } )*   ')'
	{
		$values = new Vector();
		for (int i = 0; i < constants.size(); i++) {
			CodaConstant temp = constants.get(i);
			switch (temp.getSysvar()) {
				case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
					$values.add("CURRENT_TIMESTAMP");
					break;
				case CodaConstant.SYSVAR_CURRENT_USER_ID:
					$values.add("CURRENT_USER_ID");
					break;
				case CodaConstant.SYSVAR_CURRENT_USERNAME:
					$values.add("CURRENT_USERNAME");
					break;
				case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
					$values.add("CURRENT_GROUP_NAME");
					break;
				case CodaConstant.SYSVAR_NULL:
					$values.add(null);
					break;
				default:
					$values.add(temp.getValue());
			}
		}
	}
	;
	
insertUpdateValue returns [CodaConstant value]
	:  (n='NULL' | sv=systemVariable | sl=stringLiteral | al=arrayLiteral )
	{
		if (n != null) {
	    		$value = new CodaConstant(CodaConstant.SYSVAR_NULL);
	    	} else if (sv != null) {
	    		if ($sv.text.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
	    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_TIMESTAMP);
	    		} else if ($sv.text.equalsIgnoreCase("CURRENT_USER_ID")) {
	    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_USER_ID);
	    		} else if ($sv.text.equalsIgnoreCase("CURRENT_USERNAME")) {
	    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_USERNAME);
	    		} else if ($sv.text.equalsIgnoreCase("CURRENT_GROUP_NAME")) {
	    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_GROUP_NAME);
	    		} 
	    	} else if (al != null) {
	    		$value = new CodaConstant($al.text, true);
	    	} else {
	    		$value = new CodaConstant($sl.value);
	    	}		
	}
	;
	
// Command specific rules

/*
stringLiteral returns [String value]
	:	'\'' v=(EscapeSequence | ~('\''|'\\') )* '\''
		{$value=$v.text;}
	;
*/
setDisplay returns [String displayName]
	:	'SET' 'DISPLAY' dn=stringLiteral
		{$displayName=$dn.value;}
	;
renameTo returns [String newName]
	:	'RENAME' 'TO' nn=objectName
		{ $newName=$nn.text;}
	;
arrayLiteral returns [Vector values]
@init {
	$values = new Vector();
}
	:	'[' val=stringLiteral { $values.add($val.value); } (',' val=stringLiteral { $values.add($val.value); } )* ']'
		
	;
displayedAs returns [String displayName]
	:	'DISPLAYED' 'AS' dn=stringLiteral
		{$displayName=$dn.value;}
	;
	
/////////////
// DML Statements
/////////////
/*
tokens {
	INNER	=	'INNER';
	LEFT	=	'LEFT';
	RIGHT	=	'RIGHT';
	FULL	=	'FULL';
	OUTER	=	'OUTER';
	INNER	=	'INNER';
	JOIN	=	'JOIN';
	SELECT	=	'SELECT';
	AS	=	'AS';
	FROM	=	'FROM';
	COLON	=	':';
	WHERE	=	'WHERE';
	BY	=	'BY';
	ORDER	=	'ORDER';
	GROUP	=	'GROUP';
	HAVING	=	'HAVING';
	WITH	=	'WITH';
	ASC	=	'ASC';
	DESC	=	'DESC';
	DOT	=	'.';
	NOT	=	'NOT';
	TOP	=	'TOP';
	LPAREN	=	'(';
	RPAREN	=	')';
	AND	=	'AND';
	OR	=	'OR';
	ALL	=	'ALL';
	SOME	=	'SOME';
	ANY	=	'ANY';
	IS	=	'IS';
	LIKE	=	'LIKE';
	BETWEEN	=	'BETWEEN';
	IN	=	'IN';
	ON	=	'ON';
	ESCAPE	=	'ESCAPE';
	COMMA	=	',';
	STAR	=	'*';
	DISTINCT =	'DISTINCT';
	NULL	=	'NULL';
	CONTAINS =	'CONTAINS';
	STARTING =  	'STARTING';
	AT	= 	'AT';
	
	ASSIGNEQUAL = '=' ;
	NOTEQUAL = '<>' ;
	LESSTHANOREQUALTO = '<=' ;
	LESSTHAN = '<' ;
	GREATERTHANOREQUALTO = '>=' ;
	GREATERTHAN = '>' ;
	
	DIVIDE = '/' ;
	PLUS = '+' ;
	MINUS = '-' ;
	MOD = '%' ;
	
	AMPERSAND = '&' ;
	TILDE = '~' ;
}
*/

select  returns [CodaResponse response]
    :
    sc=selectClause
    (fc=fromClause
	    (wc=whereClause)?
	    (gb=groupByClause (hc=havingClause)? )?
	    (oc=orderByClause)?
    )?
    {
    	CodaFromClause localFromClause = $fc.value;
    	localFromClause.setFromClause($fc.text);
    	try {
    		$response = server.select(sessionKey, $sc.value, (fc != null ? localFromClause : null), (wc != null ? $wc.value : null), gb != null ? $gb.value.toString() : null, hc != null ? $hc.value.print(localFromClause) : null, oc != null ? $oc.value.toString() : null, $sc.top, $sc.startingAt, null); 
    	} catch (CodaException e) {
    		$response = new CodaResponse(true, null, 8004);
    	}
    }
    ;

selectNoCommit[CodaConnection connection]  returns [CodaResponse response]
    :
    sc=selectClause
    (fc=fromClause
	    (wc=whereClause)?
	    (gb=groupByClause (hc=havingClause)? )?
	    (oc=orderByClause)?
    )?
    {
    	CodaFromClause localFromClause = $fc.value;
    	localFromClause.setFromClause($fc.text);
    	try {
    		$response = server.select(sessionKey, $sc.value, (fc != null ? localFromClause : null), (wc != null ? $wc.value : null), gb != null ? $gb.value.toString() : null, hc != null ? $hc.value.print(localFromClause) : null, oc != null ? $oc.value.toString() : null, $sc.top, $sc.startingAt, connection); 
    	} catch (CodaException e) {
    		$response = new CodaResponse(true, null, 8004);
    	}
    }
    ;
    
sysSelect  returns [CodaResponse response]
    :
    sc=sysSelectClause
    (fc=fromClause
	    (wc=whereClause)?
	    (gb=groupByClause (hc=havingClause)? )?
	    (oc=orderByClause)?
    )?
    {
    	CodaFromClause localFromClause = $fc.value;
    	localFromClause.setFromClause($fc.text);
    	try {
    		$response = server.sysSelect(sessionKey, $sc.value, (fc != null ? localFromClause : null), (wc != null ? $wc.value : null), gb != null ? $gb.value.toString() : null, hc != null ? $hc.value.print(localFromClause) : null, oc != null ? $oc.value.toString() : null, $sc.top, $sc.startingAt); 
    	} catch (CodaException e) {
    		$response = new CodaResponse(true, null, 8004);
    	}
    }
    ;
    
insert	 returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
	Vector<CodaConstant> values = new Vector();
	Vector<Vector> rows = new Vector();
}
	:	'INSERT' 'INTO' tn=objectName
	(	'(' fn=objectName { fields.add($fn.text); } (',' fn=objectName  { fields.add($fn.text); } )* ')' 'VALUES' ivl=insertValuesList { rows.add($ivl.values); } (',' ivl=insertValuesList { rows.add($ivl.values); } )*
	|	'SET' fn=objectName  { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } (',' fn=objectName  { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } )* )
	{
		if (values.size() == 0) {
			Vector<String> row = new Vector();
			for (int i = 0; i < values.size(); i++) {
				CodaConstant temp = (CodaConstant)values.get(i);
				switch (temp.getSysvar()) {
					case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
						row.add("CURRENT_TIMESTAMP");
						break;
					case CodaConstant.SYSVAR_CURRENT_USER_ID:
						row.add("CURRENT_USER_ID");
						break;
					case CodaConstant.SYSVAR_CURRENT_USERNAME:
						row.add("CURRENT_USERNAME");
						break;
					case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
						row.add("CURRENT_GROUP_NAME");
						break;
					case CodaConstant.SYSVAR_NULL:
						row.add(null);
						break;
					default:
						row.add(temp.getValue());
				}
			}
			rows.add(row);
		}
	
		$response = server.insert(sessionKey, $tn.text, fields, rows, null);
	}
	;

insertNoCommit[CodaConnection connection]	 returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
	Vector<CodaConstant> values = new Vector();
	Vector<Vector> rows = new Vector();
}
	:	'INSERT' 'INTO' tn=objectName
	(	'(' fn=objectName { fields.add($fn.text); } (',' fn=objectName  { fields.add($fn.text); } )* ')' 'VALUES' ivl=insertValuesList { rows.add($ivl.values); } (',' ivl=insertValuesList { rows.add($ivl.values); } )*
	|	'SET' fn=objectName  { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } (',' fn=objectName  { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } )* )
	{
		if (values.size() == 0) {
			Vector<String> row = new Vector();
			for (int i = 0; i < values.size(); i++) {
				CodaConstant temp = (CodaConstant)values.get(i);
				switch (temp.getSysvar()) {
					case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
						row.add("CURRENT_TIMESTAMP");
						break;
					case CodaConstant.SYSVAR_CURRENT_USER_ID:
						row.add("CURRENT_USER_ID");
						break;
					case CodaConstant.SYSVAR_CURRENT_USERNAME:
						row.add("CURRENT_USERNAME");
						break;
					case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
						row.add("CURRENT_GROUP_NAME");
						break;
					case CodaConstant.SYSVAR_NULL:
						row.add(null);
						break;
					default:
						row.add(temp.getValue());
				}
			}
			rows.add(row);
		}
	
		$response = server.insert(sessionKey, $tn.text, fields, rows, connection);
	}
	;
	
update returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
	Vector<CodaConstant> values = new Vector();
}	
	:	'UPDATE' tn=objectName
		'SET' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } (',' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } )* 
		(wc=whereClause)?
	{
		Vector<String> row = new Vector();
		for (int i = 0; i < values.size(); i++) {
			CodaConstant temp = (CodaConstant)values.get(i);
			switch (temp.getSysvar()) {
				case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
					row.add("CURRENT_TIMESTAMP");
					break;
				case CodaConstant.SYSVAR_CURRENT_USER_ID:
					row.add("CURRENT_USER_ID");
					break;
				case CodaConstant.SYSVAR_CURRENT_USERNAME:
					row.add("CURRENT_USERNAME");
					break;
				case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
					row.add("CURRENT_GROUP_NAME");
					break;
				case CodaConstant.SYSVAR_NULL:
					row.add(null);
					break;
				default:
					row.add(temp.getValue());
			}
		}
		
		$response = server.update(sessionKey, $tn.text, fields, row, (wc != null ? $wc.value : null), null);
	}
	;

updateNoCommit[CodaConnection connection] returns [CodaResponse response]	
@init {
	Vector<String> fields = new Vector();
	Vector<CodaConstant> values = new Vector();
}
	:	'UPDATE' tn=objectName
		'SET' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } (',' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } )* 
		(wc=whereClause)?
	{
		Vector<String> row = new Vector();
		for (int i = 0; i < values.size(); i++) {
			CodaConstant temp = (CodaConstant)values.get(i);
			switch (temp.getSysvar()) {
				case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
					row.add("CURRENT_TIMESTAMP");
					break;
				case CodaConstant.SYSVAR_CURRENT_USER_ID:
					row.add("CURRENT_USER_ID");
					break;
				case CodaConstant.SYSVAR_CURRENT_USERNAME:
					row.add("CURRENT_USERNAME");
					break;
				case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
					row.add("CURRENT_GROUP_NAME");
					break;
				case CodaConstant.SYSVAR_NULL:
					row.add(null);
					break;
				default:
					row.add(temp.getValue());
			}
		}
		
		$response = server.update(sessionKey, $tn.text, fields, row, (wc != null ? $wc.value : null), connection);
	}
	;


delete returns [CodaResponse response]
	:	'DELETE' 'FROM' tn=objectName
		(wc=whereClause)?
	{
		$response = server.delete(sessionKey, $tn.text, (wc != null ? $wc.value : null), null);
	}
	;
	
deleteNoCommit[CodaConnection connection] returns [CodaResponse response]
	:	'DELETE' 'FROM' tn=objectName
		(wc=whereClause)?
	{
		$response = server.delete(sessionKey, $tn.text, (wc != null ? $wc.value : null), connection);
	}
	;
	
formUpdate returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
	Vector<CodaConstant> values = new Vector();
}
	:	fav=objectName tn=objectName 
		( 'SET' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } (',' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } )* )?
		( 'AS' anf='NEW' 'FORM' | (wc=whereClause)? )
	{
		Vector<String> row = new Vector();
		for (int i = 0; i < values.size(); i++) {
			CodaConstant temp = (CodaConstant)values.get(i);
			switch (temp.getSysvar()) {
				case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
					row.add("CURRENT_TIMESTAMP");
					break;
				case CodaConstant.SYSVAR_CURRENT_USER_ID:
					row.add("CURRENT_USER_ID");
					break;
				case CodaConstant.SYSVAR_CURRENT_USERNAME:
					row.add("CURRENT_USERNAME");
					break;
				case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
					row.add("CURRENT_GROUP_NAME");
					break;
				case CodaConstant.SYSVAR_NULL:
					row.add(null);
					break;
				default:
					row.add(temp.getValue());
			}
		}
		
		$response = server.formUpdate(sessionKey, $fav.text, $tn.text, fields, row, (anf != null ? true : false), (wc != null ? $wc.value : null), null);
	}
	;
formUpdateNoCommit[CodaConnection connection] returns [CodaResponse response]
@init {
	Vector<String> fields = new Vector();
	Vector<CodaConstant> values = new Vector();
}
	:	fav=objectName tn=objectName 
		'SET' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } (',' fn=objectName { fields.add($fn.text); } '=' iv=insertUpdateValue { values.add($iv.value); } )* 
		( 'AS' anf='NEW' 'FORM' | (wc=whereClause)? )
	{
		Vector<String> row = new Vector();
		for (int i = 0; i < values.size(); i++) {
			CodaConstant temp = (CodaConstant)values.get(i);
			switch (temp.getSysvar()) {
				case CodaConstant.SYSVAR_CURRENT_TIMESTAMP:
					row.add("CURRENT_TIMESTAMP");
					break;
				case CodaConstant.SYSVAR_CURRENT_USER_ID:
					row.add("CURRENT_USER_ID");
					break;
				case CodaConstant.SYSVAR_CURRENT_USERNAME:
					row.add("CURRENT_USERNAME");
					break;
				case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
					row.add("CURRENT_GROUP_NAME");
					break;
				case CodaConstant.SYSVAR_NULL:
					row.add(null);
					break;
				default:
					row.add(temp.getValue());
			}
		}
		
		$response = server.formUpdate(sessionKey, $fav.text, $tn.text, fields, row, (anf != null ? false : true), (wc != null ? $wc.value : null), connection);
	}
	;
    
whereClause returns [CodaSearchCondition value]
    : 'WHERE' sc=searchCondition
    {
    	$value = $sc.value;
    }
    ;

fromClause returns [CodaFromClause value]
@init {
	Vector<CodaTableNameAlias> defs = new Vector();
	Vector<CodaTableSource> tableSources = new Vector();
}
    : ('FROM') => 'FROM' tab=tableSource { for (CodaTableNameAlias alias : ((tableSource_return)tab).value) { defs.add(alias); } tableSources.add($tab.retObj); } (',' tab=tableSource { for (CodaTableNameAlias alias : ((tableSource_return)tab).value) { defs.add(alias); } tableSources.add($tab.retObj); } )*
    {
    	
    	$value = new CodaFromClause(defs, tableSources);
    }
    ;

tableSource returns [Vector<CodaTableNameAlias> value, CodaTableSource retObj]
@init{
	if ($value == null) {
		$value = new Vector();
	}
	Vector<CodaJoinedTable> joinedTables = new Vector();
}
    : st=subTableSource (jt=joinedTable 
    	{ 
    		for (CodaTableNameAlias temp : $jt.value) {
    			$value.add(temp);
    			joinedTables.add($jt.retObj);
    		}
    	
    	} )*
    {
    	for (CodaTableNameAlias temp2 : $st.value) {
    		$value.add(temp2);
    	}
    	$retObj = new CodaTableSource($st.retObj, joinedTables);
    }
    ;

subTableSource returns [Vector<CodaTableNameAlias> value, CodaSubTableSource retObj]
@init{
	if ($value == null) {
		$value = new Vector();
	}
}
    :
    (
    tn=objectName (al=alias1)?  
    {
    	$value.add(new CodaTableNameAlias($tn.text, (al == null ? null : $al.value)));
    	$retObj = new CodaSubTableSource($tn.text.trim(), al == null ? null : $al.value);
    }
    |
     '(' jt=joinedTables 
     {
     	$value = $jt.value;
     	$retObj = new CodaSubTableSource($jt.retObj);
     }
      ')'
    )
    ;

alias1 returns [String value]
    : 
    ('AS')? al=objectName
    {
    	$value = $al.text;
    }
    ;

alias2 returns [String value]
    : 
    al=objectName '.'
    {
    	$value = $al.text;
    }
    ;

joinedTable returns [Vector<CodaTableNameAlias> value, CodaJoinedTable retObj]
    :
     ( (inn='INNER' | joint=('LEFT' | 'RIGHT' | 'FULL') (oute='OUTER')? ) )? 'JOIN' ts=tableSource 'ON' sc=searchCondition
     {
     	$value = $ts.value;
     	$retObj = new CodaJoinedTable($ts.retObj, inn != null, joint == null ? null : $joint.text.toUpperCase(), $sc.value);
     }
    ;    

joinedTables returns [Vector<CodaTableNameAlias> value, CodaJoinedTables retObj]
@init{
	if ($value == null) {
		$value = new Vector();
	}
	Vector<CodaJoinedTable> joinedTables = new Vector();
}
    : sts=subTableSource (jt=joinedTable
    { 
    	for (CodaTableNameAlias temp : $jt.value) {
    		$value.add(temp);
    	}
    	joinedTables.add($jt.retObj);
    }	
    	
    )+
    {
    	for (CodaTableNameAlias temp : $sts.value) {
    		$value.add(temp);
    	}
    	$retObj = new CodaJoinedTables($sts.retObj, joinedTables);
    }
    ;

       
selectClause returns [String value, long top, long startingAt]
    : 'SELECT' ('TOP' topi=Integer ('STARTING' 'AT' sa=Integer)? )? (dist='DISTINCT')? sl=selectList
    {
    	$value = "SELECT " + (dist == null ? "" : "DISTINCT ") + $sl.value;
    	if (topi != null) {
    		$top = Long.parseLong($topi.text);
    	} else {
    		$top = -1;
    	} 
    	if (sa != null) {
    		$startingAt = Long.parseLong($sa.text);
    	} else {
    		$startingAt = -1;
    	}
    }
    ; 
    
sysSelectClause returns [String value, long top, long startingAt]
    : 'SYSSELECT' (dist='DISTINCT')? ('TOP' topi=Integer ('STARTING' 'AT' sa=Integer)? )? sl=selectList
    {
    	$value = "SELECT " + (dist == null ? "" : "DISTINCT ") + $sl.value;
    	if (topi != null) {
    		$top = Long.parseLong($topi.text);
    	} else {
    		$top = -1;
    	} 
    	if (sa != null) {
    		$startingAt = Long.parseLong($sa.text);
    	} else {
    		$startingAt = -1;
    	}
    }
    ;    

selectList returns [String value]
@init {
	$value = "";
}
    : it=selectItem { $value += $it.value + " ";}  ( ',' it=selectItem { $value += ", " + $it.value + " ";} )*
    ;
    
selectItem returns [String value]
    :
      st1='*' {if (st1 != null) { $value = "* ";}}// "*, *" is a valid select list
    | (
      // starts with: "alias = column_name"
      (alias2) => (
          (alias2 '*') => a2=alias2 st2='*'
        | (alias2 dbObject ',') => a2=alias2 co=column (a1=alias1)?
        | (alias2 dbObject (arithmeticOperator | '(')) => a2=alias2 ex=expression (a1=alias1)?
        | (alias2 column) => a2=alias2 co=column (a1=alias1)?
        | (alias2 expression) => a2=alias2 ex=expression (a1=alias1)?
        )
      // some shortcuts:
    | (dbObject (alias1)? ',') => co=column (a1=alias1)?
    | (dbObject (arithmeticOperator | '(') ) => ex=expression (a1=alias1)?
    
      // less obvious cases:
    | (column) => co=column (a1=alias1)?
    | (expression) => ex=expression (a1=alias1)?
    )
    
    {
    	if (st1 != null) {
    		$value = "* ";
    	} else if (a2 != null) {
    		if (st2 != null) {
    			$value = $a2.value + ".* "; 
    		} else if (ex != null) {
    			$value = $a2.value + "." + $ex.value.toString() + " ";
    		} else {
    			$value = $a2.value + "." + $co.text + " ";
    		}
    	} else if (co != null) {
    		$value = $co.text + " ";
    		if (a1 != null) {
    			$value += " AS " + $a1.value + " ";
    		}
    	} else if (ex != null) {
    		$value = $ex.value.toString() + " ";
    		if (a1 != null) {
    			$value += " AS " + $a1.value + " ";
    		}
    	} else {
    		$value = " ";
    	}
    	
    }
    ;
    
column
    :
      dbObject
    ;
    
searchCondition returns [CodaSearchCondition value]
@init {
Vector operators = new Vector();
Vector subConditions = new Vector();
}	:	
    ssc=subSearchCondition { subConditions.add($ssc.value);} (op=('AND' | 'OR') { operators.add($op.text);} ssc=subSearchCondition { subConditions.add($ssc.value); } )*
    {
    	$value = new CodaSearchCondition(subConditions, operators);
    }
    ;
    
subSearchCondition returns [CodaSubSearchCondition value]
    :
    (not='NOT')? (
         ('(' searchCondition ')') =>  '(' sc=searchCondition ')'
        | pred=predicate
        )
    {
    	if (sc != null) {
    		$value = new CodaSubSearchCondition((not != null ? true : false), $sc.value);
    	} else {
    		$value = new CodaSubSearchCondition((not != null ? true : false), $pred.value);
    	}
    }
    ;
    
predicate returns [CodaPredicate value]
    :
    
      exp1=expression (
        // expression comparisonOperator expression
        co=comparisonOperator exp2=expression 
      | 'IS' (not='NOT')? nil='NULL' 
      | (not='NOT')? (
            li='LIKE' exp2=expression // only single char
          | in='IN' '(' stl+=stringLiteral (',' stl+=stringLiteral)* ')'
          )
      | 'CONTAINS' sl=stringLiteral
      )
    {
    	if (co != null) {
    		$value = new CodaPredicate($exp1.value, $co.text, $exp2.value);
    	} else if (nil != null) {
    		$value = new CodaPredicate($exp1.value, (not != null ? true : false));
    	} else if (li != null) {
    		$value = new CodaPredicate($exp1.value, (not != null ? true : false), $exp2.value);
    	} else if (in != null) {
    		$value = new CodaPredicate($exp1.value, (not != null ? true : false), new Vector($stl));
    	} else {
    		$value = new CodaPredicate($exp1.value, $sl.value);
    	}
    }
    ;
comparisonOperator
    :
      '=' | '<>' | '<=' | '!='
    | '<' | '>=' | '>'
    ;    
expression returns [CodaExpression value]
@init {
Vector arithmeticOperators = new Vector();
Vector subExpressions = new Vector();
}
    : // current definition ignores operator precedence
      sube=subExpression {subExpressions.add($sube.value);} (ae=arithmeticOperator {arithmeticOperators.add($ae.text);} sube=subExpression {subExpressions.add($sube.value);} )*
      {
      	$value = new CodaExpression(subExpressions, arithmeticOperators);
      }
    ;

subExpression returns [CodaSubExpression value]
    :
    (uo=unaryOperator)?
    (
      (function) => funct=function
    | '(' exp=expression ')'
    | ((alias2)? dbObject) => (al=alias2)? obj=dbObject   // column
    |  con=constant
    )
    {
    	if (funct != null) {
    		$value = new CodaSubExpression(server.getSessionUsername(sessionKey), server.getSessionGroup(sessionKey), uo != null ? $uo.text : null, $funct.value);
    	} else if (exp != null) {
    		$value = new CodaSubExpression(server.getSessionUsername(sessionKey), server.getSessionGroup(sessionKey), uo != null ? $uo.text : null, $exp.value);
    	} else if (obj != null) {
    		$value = new CodaSubExpression(server.getSessionUsername(sessionKey), server.getSessionGroup(sessionKey), uo != null ? $uo.text : null, al != null ? $al.value : null, $obj.text);
    	} else {
    		$value = new CodaSubExpression(server.getSessionUsername(sessionKey), server.getSessionGroup(sessionKey), uo != null ? $uo.text : null, $con.value);
    	}
    }
    ;

// todo: create a separate rule for aggregate functions
function returns [CodaFunction value]
@init{
	Vector<CodaExpression> expressions = new Vector();
}
    : // LEFT and RIGHT keywords are also function names
    fn=dbObject '(' (
          ex=expression { expressions.add($ex.value); } (',' ex=expression { expressions.add($ex.value); })*
        | st1='*'    // aggregate functions like Count(), Checksum() accept "*" as a parameter
        | agg=('ALL' | 'DISTINCT') (st2='*' | ex2=expression) // aggregate function
        )?
    ')'
    {
    	if (expressions.size() > 0) {
    		$value = new CodaFunction($fn.text, expressions);
    	} else if (st1 != null) {
 		$value = new CodaFunction($fn.text);   	
    	} else {
    		$value = new CodaFunction($fn.text, $agg.text, (ex2 != null ? $ex2.value : null));
    	}
    }
    ;
    
dbObject 
	:	objectName;
    
/*
stringLiteral returns [String value]
	:	'\'' v=(EscapeSequence | ~('\''|'\\') )* '\''
		{$value=$v.text;}
	;
*/    
stringLiteral returns [String value]
    :
      	v=(UnicodeStringLiteral | ASCIIStringLiteral)
      	{
      		$value=$v.text.substring(1, $v.text.length() - 1).replace("''", "'");
      	}
    ;
constant returns [CodaConstant value]
    : (n='NULL' | sl=stringLiteral | sv=systemVariable)
    {
    	if (n != null) {
    		$value = new CodaConstant(CodaConstant.SYSVAR_NULL);
    	} else if (sv != null) {
    		if ($sv.text.equalsIgnoreCase("CURRENT_TIMESTAMP")) {
    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_TIMESTAMP);
    		} else if ($sv.text.equalsIgnoreCase("CURRENT_USER_ID")) {
    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_USER_ID);
    		} else if ($sv.text.equalsIgnoreCase("CURRENT_USERNAME")) {
    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_USERNAME);
    		} else if ($sv.text.equalsIgnoreCase("CURRENT_GROUP_NAME")) {
    			$value = new CodaConstant(CodaConstant.SYSVAR_CURRENT_GROUP_NAME);
    		} 
    	} else {
    		$value = new CodaConstant($sl.value);
    	}
    }
    ;

unaryOperator returns [String operator]
    : op=('+' | '-' | '~')
    {
    	$operator = $op.text.toUpperCase();
    }
    ;
        
arithmeticOperator returns [String operator]
    : op=('+' | '-' | '*' | '/' | '%')
    {
    	$operator = $op.text.toUpperCase();
    }
    ;
systemVariable
    	:	'CURRENT_TIMESTAMP' | 'CURRENT_USER_ID' | 'CURRENT_GROUP_NAME' | 'CURRENT_USERNAME';
    	
orderByClause returns [CodaOrderByClause value]
@init {
	Vector<CodaExpression> expressions = new Vector();
	Vector<String> directions = new Vector();
}
    : 'ORDER' 'BY' exp=expression {expressions.add($exp.value); des = null; } ('ASC' | des='DESC')? {directions.add(des == null ? "ASC" : "DESC");} (',' exp=expression {expressions.add($exp.value); des = null; } ('ASC' | des='DESC')? {directions.add(des == null ? "ASC" : "DESC");} )*
    {
    	$value = new CodaOrderByClause(expressions, directions);
    }
    ;

groupByClause returns [CodaGroupByClause value]
@init {
	Vector<CodaExpression> expressions = new Vector();
}
    : 'GROUP' 'BY' (al='ALL')? ex=expression {expressions.add($ex.value); } (',' ex=expression {expressions.add($ex.value);})*
    {
    	$value = new CodaGroupByClause(expressions, al != null);
    }
    ;

havingClause returns [CodaHavingClause value] 
    : 'HAVING' sc=searchCondition
    {
    	$value = new CodaHavingClause($sc.value);
    }
    ;
    
triggerBody returns [String groovy]
@init {
	((CommonTokenStream)this.getTokenStream()).discardOffChannelTokens(false);
	$groovy = "";
}
	:	(t=~('ENDTRIGGER') { $groovy += $t.text; } )*
	;
procedureBody returns [String groovy]
@init {
	((CommonTokenStream)this.getTokenStream()).discardOffChannelTokens(false);
	$groovy = "";
}
	:	(p=~('ENDPROCEDURE') { $groovy += $p.text; } )*
	;

rawSQLBody	:	(~EOF)*;

tablePermission returns [String perm]
	:	p=('SELECT' | 'INSERT' | 'UPDATE' | 'DELETE')
	{
		$perm = $p.text;
	}
	;
formStatusPermission returns [String perm]
	:	p=('VIEW' | 'CALL' | 'UPDATE')
	{
		$perm = $p.text;
	}
	;
procedurePermission returns [String perm]
	:	p='EXECUTE'
	{
		$perm = $p.text;
	}
	; 
cronValue returns [String value] 
@init {
	$value = "";
}
	:	( num=Integer { $value += $num.text; }  ( (',' | '-' | '/' ) num=Integer { $value += $num.text; } )* | star='*' { $value += $star.text; } | quest='?' { $value += $quest.text; } )
	;	

ASCIIStringLiteral
    :
    '\'' (~'\'')* '\'' ( '\'' (~'\'')* '\'' )*
    //'\'' (EscapeSequence | ~('\'\'') )* '\''
    ;
UnicodeStringLiteral
    :
    'U' '\'' (~'\'')* '\'' ( '\'' (~'\'')* '\'' )*
    //'U' '\'' (EscapeSequence | ~('\''|'\\') )* '\''
    ;	
	
// Basic types
ObjectName
	:	('a'..'z' | 'A'..'Z') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_' )*;

Integer	:	'0' | '1'..'9' ('0'..'9')*;

WS : ( ' ' | '\t' | '\n' | '\r' )+
     { $channel=HIDDEN; }
   ;

OTHER_CHARS 
	:	(';' | '"' | '}' | '{' );
