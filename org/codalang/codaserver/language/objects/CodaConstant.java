/*
 * CodaConstant.java
 *
 * Created on August 6, 2007, 1:52 AM
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
public class CodaConstant {
    
    public static final int SYSVAR_CURRENT_TIMESTAMP = 1;
    public static final int SYSVAR_CURRENT_USER_ID = 2;
    public static final int SYSVAR_CURRENT_GROUP_NAME = 3; 
    public static final int SYSVAR_NULL = 4;
    public static final int SYSVAR_CURRENT_USERNAME = 5;
    
    
    private int sysvar;
    private String value;
    private boolean arrayFlag;
    
    /** Creates a new instance of CodaConstant */
    public CodaConstant(int sysvar) {
        this.setSysvar(sysvar);
        this.setValue(null);
    }
    
    public CodaConstant(String value) {
        this.setSysvar(-1);
        this.setValue(value);
        this.setArrayFlag(false);
    }
    
    public CodaConstant(String value, boolean arrayFlag) {
        this.setSysvar(-1);
        this.setValue(value);
        this.setArrayFlag(arrayFlag);
    }

    public int getSysvar() {
        return sysvar;
    }

    public void setSysvar(int sysvar) {
        this.sysvar = sysvar;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isArrayFlag() {
        return arrayFlag;
    }

    public void setArrayFlag(boolean arrayFlag) {
        this.arrayFlag = arrayFlag;
    }
  
}
