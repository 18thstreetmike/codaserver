/*
 * BaseCodaProcedure.java
 *
 * Created on September 29, 2007, 1:36 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

import org.codalang.codaserver.CodaException;

import java.util.Hashtable;

/**
 *
 * @author michaelarace
 */
public interface BaseCodaProcedure {
    public Object execute(Database database, Hashtable parameters) throws CodaException;
}
