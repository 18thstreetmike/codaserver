/*
 * ProcedureParameter.java
 *
 * Created on September 29, 2007, 1:07 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

/**
 *
 * @author michaelarace
 */
public class ProcedureParameter {
    private String parameterName;
    private String parameterType;
    private boolean arrayFlag;
    
    /** Creates a new instance of ProcedureParameter */
    public ProcedureParameter(String parameterName, String parameterType, boolean arrayFlag) {
        this.parameterName = parameterName;
        this.parameterType = parameterType;
        this.arrayFlag = arrayFlag;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterType() {
        return parameterType;
    }

    public boolean isArrayFlag() {
        return arrayFlag;
    }
    
}
