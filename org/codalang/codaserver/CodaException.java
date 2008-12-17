/*
 * CodaException.java
 *
 * Created on July 23, 2007, 10:29 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

/**
 *
 * @author michaelarace
 */
public class CodaException extends Exception {
    
    String message;
    
    /** Creates a new instance of CodaException */
    public CodaException(String message) {
        this.message = message;
        
    }
    
    public CodaException() {
        this.message = "Parse error";
        
    }
    
    public String getMessage() {
        return message;
    }
    
}
