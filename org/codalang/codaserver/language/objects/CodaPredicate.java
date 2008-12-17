/*
 * CodaPredicate.java
 *
 * Created on August 13, 2007, 1:47 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.CodaException;
import org.codalang.codaserver.database.CodaConnection;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaPredicate {
    
    public static final int PREDICATE_CONDITIONAL = 1;
    public static final int PREDICATE_NULL = 2;
    public static final int PREDICATE_LIKE = 3;
    public static final int PREDICATE_IN = 4;
    public static final int PREDICATE_CONTAINS = 5;
    
    private CodaExpression expression;
    private int type;
    
    private String operator;
    private CodaExpression compExpression;
    private boolean notFlag;
    private Vector<String> inStrings;
    private String containsString;
    
    /** Creates a new instance of CodaPredicate */
    public CodaPredicate(CodaExpression expression, String operator, CodaExpression compExpression) {
        this.setType(CodaPredicate.PREDICATE_CONDITIONAL);
        this.setExpression(expression);
        this.setOperator(operator);
        this.setCompExpression(compExpression);
    }
    
    public CodaPredicate(CodaExpression expression, boolean notFlag) {
        this.setType(CodaPredicate.PREDICATE_NULL);
        this.setExpression(expression);
        this.setNotFlag(notFlag);
    }
    
    public CodaPredicate(CodaExpression expression, boolean notFlag, CodaExpression compExpression) {
        this.setType(CodaPredicate.PREDICATE_LIKE);
        this.setExpression(expression);
        this.setNotFlag(notFlag);
        this.setCompExpression(compExpression);
    }
    
    public CodaPredicate(CodaExpression expression, boolean notFlag, Vector<String> inStrings) {
        this.setType(CodaPredicate.PREDICATE_IN);
        this.setExpression(expression);
        this.setNotFlag(notFlag);
        this.setInStrings(inStrings);
    }
    
    public CodaPredicate(CodaExpression expression, String containString) {
        this.setType(CodaPredicate.PREDICATE_NULL);
        this.setExpression(expression);
        this.setContainsString(containString);
    }

    public CodaExpression getExpression() {
        return expression;
    }

    public void setExpression(CodaExpression expression) {
        this.expression = expression;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public CodaExpression getCompExpression() {
        return compExpression;
    }

    public void setCompExpression(CodaExpression compExpression) {
        this.compExpression = compExpression;
    }

    public boolean isNotFlag() {
        return notFlag;
    }

    public void setNotFlag(boolean notFlag) {
        this.notFlag = notFlag;
    }

    public Vector<String> getInStrings() {
        return inStrings;
    }

    public void setInStrings(Vector<String> inStrings) {
        this.inStrings = inStrings;
    }

    public String getContainsString() {
        return containsString;
    }

    public void setContainsString(String containsString) {
        this.containsString = containsString;
    }
    
    public String print(CodaFromClause fromClause) throws CodaException {
        CodaConnection connection = fromClause.getDatabase().getConnection();
        switch (this.type) {
            case CodaPredicate.PREDICATE_CONDITIONAL:
                if (expression.containsColumn()) {
                    return expression.toString() + " " + operator + " " + compExpression.toString(fromClause.getDatabase(), fromClause.getTableName(expression.getAliasName(), expression.getColumnName()), expression.getColumnName()) + " ";
                } else {
                    return expression.toString() + " " + operator + " " +compExpression.toString() + " ";
                }
            case CodaPredicate.PREDICATE_NULL:
                return expression.toString() + " IS " + (notFlag ? "NOT " : "") + "NULL ";
            case CodaPredicate.PREDICATE_LIKE:
                if (expression.containsColumn()) {
                    return expression.toString() + " LIKE " + compExpression.toString(fromClause.getDatabase(), fromClause.getTableName(expression.getAliasName(), expression.getColumnName()), expression.getColumnName()) + " ";
                } else {
                    return expression.toString() + " LIKE " + compExpression.toString() + " ";
                }
            case CodaPredicate.PREDICATE_IN:
                if (expression.containsColumn()) {
                    String retval = expression.toString() + " IN ( ";
                    boolean firstFlag = true;
                    for (String i : inStrings) {
                        if (firstFlag) {
                            retval += connection.formatStringForSQL(fromClause.getTableName(expression.getAliasName(), expression.getColumnName()), expression.getColumnName(), i);
                            firstFlag = false;
                        } else {
                            retval += ", " + connection.formatStringForSQL(fromClause.getTableName(expression.getAliasName(), expression.getColumnName()), expression.getColumnName(), i);
                        } 
                    }
                    return retval + ") ";
                } else {
                    String retval = expression.toString() + " IN ( ";
                    boolean firstFlag = true;
                    for (String i : inStrings) {
                        if (firstFlag) {
                            retval += "'" + i.replaceAll("'", "''") + "'";
                            firstFlag = false;
                        } else {
                            retval += ", " + "'" + i.replaceAll("'", "''") + "'";
                        } 
                    }
                    return retval + ") ";
                }
            case CodaPredicate.PREDICATE_CONTAINS:
                if (expression.containsColumn()) {
                    return expression.toString() + " LIKE '%" + containsString.replaceAll("'", "''") + "%' ";
                } else {
                    return expression.toString() + " LIKE " + "'%" + containsString.replaceAll("'", "''") + "%' ";
                }
        }
        return "";
    }
}
