/*
 * ICodaAPI.java
 *
 * Created on October 23, 2007, 2:25 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.httpServer;

import java.util.Hashtable;

/**
 *
 * @author michaelarace
 */

public interface ICodaAPI {
    public String login ( String username, String password, String applicationName, String environment, String groupName  );
    public void logout ( String sessionKey);
    public Hashtable execute (String sessionKey, String command);
	public void reloadType (String ipAddress, int port, long typeId);
	public void reloadSession (String ipAddress, int port, String sessionKey);
	public void reloadApplication (String ipAddress, int port, String applicationName, String environment);

}

