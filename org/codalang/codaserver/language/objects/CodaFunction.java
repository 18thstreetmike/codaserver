/*
 * CodaFunction.java
 *
 * Created on August 12, 2007, 7:13 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.language.objects;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaFunction {
    
    public static final int FUNCTION_EXPRESSION = 1;
    public static final int FUNCTION_STAR = 2;
    public static final int FUNCTION_DISTINCT = 3;
    
    private String functionName;
    private int type;
    
    private Vector<CodaExpression> arguments;
    private String aggregateQualifier;
    private CodaExpression expression;
    
    /** Creates a new instance of CodaFunction */
    public CodaFunction(String functionName, Vector<CodaExpression> arguments) {
        this.setType(CodaFunction.FUNCTION_EXPRESSION);
        this.setFunctionName(functionName);
        this.setArguments(arguments);
    }
    
    public CodaFunction(String functionName) {
        this.setType(CodaFunction.FUNCTION_STAR);
        this.setFunctionName(functionName);
    }
    
    public CodaFunction(String functionName, String aggregateQualifier, CodaExpression expression) {
        this.setType(CodaFunction.FUNCTION_DISTINCT);
        this.setFunctionName(functionName);
        this.setAggregateQualifier(aggregateQualifier);
        this.setExpression(expression);
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Vector<CodaExpression> getArguments() {
        return arguments;
    }

    public void setArguments(Vector<CodaExpression> arguments) {
        this.arguments = arguments;
    }

    public String getAggregateQualifier() {
        return aggregateQualifier;
    }

    public void setAggregateQualifier(String aggregateQualifier) {
        this.aggregateQualifier = aggregateQualifier;
    }

    public CodaExpression getExpression() {
        return expression;
    }

    public void setExpression(CodaExpression expression) {
        this.expression = expression;
    }

    public String toString () {
        switch (type) {
            case CodaFunction.FUNCTION_EXPRESSION:
                String retval = functionName + "(";
                for (CodaExpression e : arguments) {
                    retval += e.toString();
                }
                return retval + ") ";
            case CodaFunction.FUNCTION_STAR:
                return functionName + "(*) ";
            case CodaFunction.FUNCTION_DISTINCT:
                return functionName + "(" + aggregateQualifier + " " + (expression == null ? "*" : expression.toString()) + ") ";
        }
        return "";
    }
}
