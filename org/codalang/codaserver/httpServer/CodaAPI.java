/*
 * CodaAPI.java
 *
 * Created on October 23, 2007, 2:25 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.httpServer;

import org.antlr.runtime.*;
import org.codalang.codaserver.CodaError;
import org.codalang.codaserver.CodaResponse;
import org.codalang.codaserver.CodaServer;
import org.codalang.codaserver.database.CodaResultSet;
import org.codalang.codaserver.language.CaseInsensitiveStringStream;
import org.codalang.codaserver.language.CodaLexer;
import org.codalang.codaserver.language.CodaParser;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */

public class  CodaAPI implements ICodaAPI {
  
    public CodaServer server;
  
    public CodaAPI (CodaServer server) {
        this.server = server;
    }
    
    public String login ( String username, String password, String applicationName, String environment, String groupName  ) {
        String sessionKey = server.login(username, password);
        if (sessionKey != null && applicationName != null && environment != null) {
            int environmentId = -1;
            if (environment.equalsIgnoreCase("DEV")) {
                environmentId = 1;
            } else if (environment.equalsIgnoreCase("TEST")) {
                environmentId = 2;
            } else if (environment.equalsIgnoreCase("PROD")) {
                environmentId = 3;
            }
            if (environmentId == -1) {
                server.logout(sessionKey);
                return null;
            } else if (!server.setSessionApplication(sessionKey, applicationName, environmentId, groupName)) {
                server.logout(sessionKey);
                return null;
            }
        }
        return sessionKey;
    }
    
    public void logout ( String sessionKey ) {
        server.logout(sessionKey);
    }
    
    public Hashtable execute (String sessionKey, String command) {
        Hashtable retval = new Hashtable();
        if (server.touchSession(sessionKey)) {
            CodaParser parser = new CodaParser(new CommonTokenStream(new CodaLexer(new CaseInsensitiveStringStream(command))), server, sessionKey, null);
            CodaResponse response = new CodaResponse(true, null, 8007);
            try {
                response = parser.stat().response;
            } catch (MismatchedTokenException e) {
                String [] tokenNames = CodaParser.tokenNames;
                String msg = "";
                String tokenName = "<unknown>";

                if (e.expecting == Token.EOF) {
                        tokenName = "EOF";
                } else  {
                        tokenName = tokenNames[e.expecting];
                }

                msg = "Mismatched input " + getTokenErrorDisplay(e.token) + " expecting " + tokenName;
                response = new CodaResponse(true, null, 1007, msg + " at line " + e.line + ", position " + e.charPositionInLine);
            } catch (NoViableAltException e) {
                response = new CodaResponse(true, null, 1007, "No viable alternative at input " + getTokenErrorDisplay(e.token) + " on line " + e.line + ", position " + e.charPositionInLine);
            } catch (EarlyExitException e) {
                response = new CodaResponse(true, null, 1007, "Required (...)+ loop did not match anything at input " + getTokenErrorDisplay(e.token) + " on line " + e.line + ", position " + e.charPositionInLine);
            } catch (MismatchedSetException e) {
                response = new CodaResponse(true, null, 1007, "Mismatched input " + getTokenErrorDisplay(e.token) + " expecting set " + e.expecting + " on line " + e.line + ", position " + e.charPositionInLine);
            } catch (FailedPredicateException e) {
                response = new CodaResponse(true, null, 1007, "Rule " + e.ruleName + " failed predicate: {" + e.predicateText + "}?" + " on line " + e.line + ", position " + e.charPositionInLine);
            } catch (RecognitionException e) {
                response = new CodaResponse(true, null, 1007, "No viable alternative at input " + getTokenErrorDisplay(e.token) + " on line " + e.line + ", position " + e.charPositionInLine);
            } catch (Exception e) {
				response = new CodaResponse(true, null, 1007, e.getMessage());
            } catch (Error e) {
				response = new CodaResponse(true, null, 1007, e.getMessage());
            }
            if (response.getError()) {
                retval.put("errorstatus", true);
                Vector<Hashtable> errors = new Vector();
                for (CodaError error : response.getErrors()) {
                    Hashtable temp = new Hashtable();
                    temp.put("errorcode", error.getErrorCode());
                    temp.put("errormessage", error.getErrorMessage());
                    errors.add(temp);
                } 
                retval.put("errors", errors);
            } else {
                retval.put("errorstatus", false);
                Vector<Hashtable> errors = new Vector();
                retval.put("errors", errors);
            }
            if (response.getReturnValue() instanceof List) {
                Vector data = new Vector();
                for (Object obj : (List)response.getReturnValue()) {
                    if (obj instanceof CodaResultSet) {
                        data.add(((CodaResultSet)obj).serialize());
                    } else {
                        data.add(obj);
                    }
                }
                retval.put("data", data);
            } else {
                if (response.getReturnValue() instanceof CodaResultSet) {
                    retval.put("data", ((CodaResultSet)response.getReturnValue()).serialize());
                } else {
                    retval.put("data", response.getReturnValue());
                }
            }
        } else {
            retval.put("errorstatus", true);
            Vector<Hashtable> errors = new Vector();
            Hashtable temp = new Hashtable();
            temp.put("errorcode", 1005);
            temp.put("errormessage", CodaResponse.getMessageForCode(1005));
            errors.add(temp);
            retval.put("errors", errors);
            retval.put("data", "");
        }
        return retval;
    }

	public void reloadType(String ipAddress, int port, long typeId) {
		if (server.verifyClusterMember(ipAddress, port))
			server.reloadType(typeId);
	}

	public void reloadSession(String ipAddress, int port, String sessionKey) {
		if (server.verifyClusterMember(ipAddress, port)) {
			server.getSessions().loadSession(sessionKey);
		}
	}

	public void reloadApplication(String ipAddress, int port, String applicationName, String environment) {
		if (server.verifyClusterMember(ipAddress, port)) {
			if (server.getDeployedApplication(applicationName) != null) {
				server.getDeployedApplication(applicationName).reload();
				int environmentInt  = 1;

				if (environment.equalsIgnoreCase("DEV")) {
					environmentInt = 1;
				} else if (environment.equalsIgnoreCase("TEST")) {
					environmentInt = 2;
				} else if (environment.equalsIgnoreCase("PROD")) {
					environmentInt = 3;
				}
				server.getDeployedApplication(applicationName).updateEnvironmentClassLoader(environmentInt, server.getClassLoader());
			}
		}
	}

	public String getTokenErrorDisplay(Token t) {
        String s = t.getText();
        if (s == null) {
            if (t.getType() == Token.EOF) {
                s = "<EOF>";
            } else  {
                s = "<" + t.getType() + ">";
            }
        }
        s = s.replaceAll("\\n", "\\\\\\\\n");
        s = s.replaceAll("\\r", "\\\\\\\\r");
        s = s.replaceAll("\\t", "\\\\\\\\t");
        return "\'" + s + "\'";
    }
}
