/*
 * LongString.java
 *
 * Created on September 11, 2007, 2:30 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

/**
 *
 * @author michaelarace
 */
public class LongString {
    
    private String value;
    
    /**
     * Creates a new instance of LongString
     */
    public LongString(String value) {
        this.value = value;
    }
    
    public LongString() {
        this.value = "";
    }

    public int hashCode() {
        return value.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj.getClass().getName().equals("java.lang.String") || obj.getClass().getName().equals("org.codalang.codaserver.language.types.LongString")) {
            return this.hashCode() == obj.hashCode();
        }
        return false;
    }
    
    public LongString plus(LongString other) {
        return new LongString(this.value + other.value);
    }
    
    public String getValue() {
        return value;
    }
}
