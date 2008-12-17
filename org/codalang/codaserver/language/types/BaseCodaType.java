/*
 * BaseCodaType.java
 *
 * Created on September 10, 2007, 11:50 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

import com.stevesoft.pat.Regex;

/**
 *
 * @author michaelarace
 */
public abstract class BaseCodaType {
    
    protected String value;
    
    protected abstract String getValidationString();
    
    protected abstract String getSaveString();
    
    public boolean parse(String input) throws Exception {
        Regex regex = new Regex(getValidationString());

        if (regex.search(input)) {
            if (input.equals(regex.stringMatched())) {
                this.value = input;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    
    public String getValue() {
        Regex regex = new Regex(getValidationString(), getSaveString());
        return regex.replaceAll((value == null ? "" : value));
    }
    
    public boolean equals (Object testValue) {
        if (this.getClass().getName().equals(testValue.getClass().getName()) && this.hashCode() == testValue.hashCode()) {
            return true;
        }
        return false;
    }
    
    public int hashCode() {
        return (this.getValidationString() + ","+ this.getSaveString()).hashCode();
    }
}
