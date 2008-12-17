/*
 * CodaSubSearchCondition.java
 *
 * Created on August 13, 2007, 12:02 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.CodaException;

/**
 *
 * @author michaelarace
 */
public class CodaSubSearchCondition {
    
    public static final int SUB_SEARCH_CONDITION_TYPE_SEARCH_CONDITION = 1;
    public static final int SUB_SEARCH_CONDITION_TYPE_PREDICATE = 2;
    
    private CodaPredicate predicate;
    private CodaSearchCondition condition;
    private int type;
    private boolean notFlag;
    
    /** Creates a new instance of CodaSubSearchCondition */
    public CodaSubSearchCondition(boolean notFlag, CodaPredicate predicate) {
        this.setType(CodaSubSearchCondition.SUB_SEARCH_CONDITION_TYPE_PREDICATE);
        this.setPredicate(predicate);
        this.notFlag = notFlag;
    }
    
    public CodaSubSearchCondition(boolean notFlag, CodaSearchCondition condition) {
        this.setType(CodaSubSearchCondition.SUB_SEARCH_CONDITION_TYPE_SEARCH_CONDITION);
        this.setCondition(condition);
        this.notFlag = notFlag;
    }

    public CodaPredicate getPredicate() {
        return predicate;
    }

    public void setPredicate(CodaPredicate predicate) {
        this.predicate = predicate;
    }

    public CodaSearchCondition getCondition() {
        return condition;
    }

    public void setCondition(CodaSearchCondition condition) {
        this.condition = condition;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isNotFlag() {
        return notFlag;
    }

    public void setNotFlag(boolean notFlag) {
        this.notFlag = notFlag;
    }
    
    public String print(CodaFromClause fromClause) throws CodaException {
        if (this.type == CodaSubSearchCondition.SUB_SEARCH_CONDITION_TYPE_PREDICATE) {
            String retval = (this.notFlag ? "NOT" : "") + predicate.print(fromClause);
            return retval;
        } else {
            return (this.notFlag ? "NOT" : "") + "(" + condition.toString() + ")";
        }
    }
}
