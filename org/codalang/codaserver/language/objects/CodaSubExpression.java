/*
 * CodaSubExpression.java
 *
 * Created on August 13, 2007, 9:50 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.database.CodaConnection;
import org.codalang.codaserver.database.CodaDatabase;

/**
 *
 * @author michaelarace
 */
public class CodaSubExpression {
    
    public static final int SUB_EXPRESSION_FUNCTION = 1;
    public static final int SUB_EXPRESSION_EXPRESSION = 2;
    public static final int SUB_EXPRESSION_COLUMN = 3;
    public static final int SUB_EXPRESSION_CONSTANT = 4;
    private String aliasName;
    private String columnName;
    private int type;
    private String unaryOperator;
    private String currentUserName, currentGroupName;
    
    private CodaFunction function;
    private CodaExpression expression;
    private CodaConstant constant;
    
    /** Creates a new instance of CodaSubExpression */
    public CodaSubExpression(String currentUserName, String currentGroupName, String unaryOperator, CodaFunction function) {
        this.setType(CodaSubExpression.SUB_EXPRESSION_FUNCTION);
        this.setUnaryOperator(unaryOperator);
        this.setFunction(function);
    }
    
    public CodaSubExpression(String currentUserName, String currentGroupName, String unaryOperator, CodaExpression expression) {
        this.setType(CodaSubExpression.SUB_EXPRESSION_EXPRESSION);
        this.setUnaryOperator(unaryOperator);
        this.setExpression(expression);
    }
    
    public CodaSubExpression(String currentUserName, String currentGroupName, String unaryOperator, String aliasName, String columnName) {
        this.setType(CodaSubExpression.SUB_EXPRESSION_COLUMN);
        this.setUnaryOperator(unaryOperator);
        this.setColumnName(columnName);
        this.setAliasName(aliasName);
    }
    
    public CodaSubExpression(String currentUserName, String currentGroupName, String unaryOperator, CodaConstant constant) {
        this.setType(CodaSubExpression.SUB_EXPRESSION_CONSTANT);
        this.setUnaryOperator(unaryOperator);
        this.setConstant(constant);
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public boolean containsColumn() {
        return type == CodaSubExpression.SUB_EXPRESSION_COLUMN;
    }
    
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUnaryOperator() {
        return unaryOperator;
    }

    public void setUnaryOperator(String unaryOperator) {
        this.unaryOperator = unaryOperator;
    }

    public CodaFunction getFunction() {
        return function;
    }

    public void setFunction(CodaFunction function) {
        this.function = function;
    }

    public CodaExpression getExpression() {
        return expression;
    }

    public void setExpression(CodaExpression expression) {
        this.expression = expression;
    }

    public CodaConstant getConstant() {
        return constant;
    }

    public void setConstant(CodaConstant constant) {
        this.constant = constant;
    }
    
    public String toString() {
        switch (type) {
            case CodaSubExpression.SUB_EXPRESSION_FUNCTION:
                return (unaryOperator != null ? unaryOperator + " " : "") + function.toString() + " ";
            case CodaSubExpression.SUB_EXPRESSION_EXPRESSION:
                return (unaryOperator != null ? unaryOperator + " " : "")  + "(" + expression.toString() + ") ";
            case CodaSubExpression.SUB_EXPRESSION_COLUMN:
                return (unaryOperator != null ? unaryOperator + " " : "") + (aliasName != null ? aliasName + "." : "") + columnName + " ";
            case CodaSubExpression.SUB_EXPRESSION_CONSTANT:
                String retval = (unaryOperator != null ? unaryOperator + " " : "");
                if (constant.getSysvar() == -1) {
                    retval += "'" + constant.getValue().replace("'", "''") + "'";
                } else {
                    switch (constant.getSysvar()) {
                        case CodaConstant.SYSVAR_CURRENT_USER_ID:
                            retval += currentUserName;
                            break;
                        case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
                            retval += currentGroupName;
                            break;
                        default:
                            retval += "CURRENT_TIMESTAMP";
                            break;
                    }
                }
                retval += " ";
                return retval;
        }
        return "";
    }
    
    public String toString(CodaDatabase database, String inputTableName, String inputColumnName) {
        CodaConnection connection = database.getConnection();
        switch (type) {
            case CodaSubExpression.SUB_EXPRESSION_FUNCTION:
                return (unaryOperator != null ? unaryOperator + " " : "") + function.toString() + " ";
            case CodaSubExpression.SUB_EXPRESSION_EXPRESSION:
                return (unaryOperator != null ? unaryOperator + " " : "")  + "(" + expression.toString() + ") ";
            case CodaSubExpression.SUB_EXPRESSION_COLUMN:
                return (unaryOperator != null ? unaryOperator + " " : "") + (aliasName != null ? aliasName + "." : "") + columnName + " ";
            case CodaSubExpression.SUB_EXPRESSION_CONSTANT:
                String retval = (unaryOperator != null ? unaryOperator + " " : "");
                if (constant.getSysvar() == -1) {
                    retval += connection.formatStringForSQL(inputTableName, inputColumnName, constant.getValue());
                } else {
                    switch (constant.getSysvar()) {
                        case CodaConstant.SYSVAR_CURRENT_USER_ID:
                            retval += currentUserName;
                            break;
                        case CodaConstant.SYSVAR_CURRENT_GROUP_NAME:
                            retval += currentGroupName;
                            break;
                        default:
                            retval += "CURRENT_TIMESTAMP";
                            break;
                    }
                }
                retval += " ";
                return retval;
        }
        return "";
    }
}
