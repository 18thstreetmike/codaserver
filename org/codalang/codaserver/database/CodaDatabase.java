/*
 * CodaDatabaseDriver.java
 *
 * Created on March 13, 2007, 3:45 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.database;

import java.util.Hashtable;
import java.util.logging.Logger;

/**
 *
 * @author michaelarace
 */
public interface CodaDatabase  {
    
    public boolean createSchema(String hostname, String username, String password, String schema, Hashtable options, String adminUsername, String adminPassword);
    
    public boolean connect (String hostname, String username, String password, String schema, Hashtable options);
    
    public boolean disconnect();

    public CodaConnection getConnection();

    public void setLogger(Logger logger);

}
