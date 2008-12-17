/*
 * FormStatusDefinition.java
 *
 * Created on June 20, 2007, 12:57 AM
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
public class FormStatusDefinition {
    private String formStatusAdj;
    private String formStatusVerb;
    private String formStatusVerbDisplayName;
    private String formStatusAdjDisplayName;
    private boolean initialFlag;
    
    /**
     * Creates a new instance of FormStatusDefinition
     */
    public FormStatusDefinition(String formStatusAdj) {
        this.setFormStatusAdj(formStatusAdj);
    }
    
    public FormStatusDefinition(String formStatusAdj, String formStatusAdjDisplayName, String formStatusVerb, String formStatusVerbDisplayName, boolean initialFlag) {
        this.setFormStatusAdj(formStatusAdj);
        this.setFormStatusVerb(formStatusVerb);
        this.formStatusAdjDisplayName = formStatusAdjDisplayName;
        this.formStatusVerbDisplayName = formStatusVerbDisplayName;
        this.initialFlag = initialFlag;
    }

    public String getFormStatusAdj() {
        return formStatusAdj;
    }

    public void setFormStatusAdj(String formStatusAdj) {
        this.formStatusAdj = formStatusAdj;
    }

    public String getFormStatusVerb() {
        return formStatusVerb;
    }

    public void setFormStatusVerb(String formStatusVerb) {
        this.formStatusVerb = formStatusVerb;
    }

    public String getFormStatusVerbDisplayName() {
        return formStatusVerbDisplayName;
    }

    public void setFormStatusVerbDisplayName(String formStatusVerbDisplayName) {
        this.formStatusVerbDisplayName = formStatusVerbDisplayName;
    }

    public String getFormStatusAdjDisplayName() {
        return formStatusAdjDisplayName;
    }

    public void setFormStatusAdjDisplayName(String formStatusAdjDisplayName) {
        this.formStatusAdjDisplayName = formStatusAdjDisplayName;
    }

    public boolean isInitialFlag() {
        return initialFlag;
    }

    public void setInitialFlag(boolean initialFlag) {
        this.initialFlag = initialFlag;
    }
    
}
