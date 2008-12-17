/*
 * CodaExpression.java
 *
 * Created on August 13, 2007, 6:12 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.database.CodaDatabase;

import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaExpression {
    
    
    private String aliasName;
    private String columnName;
    private Vector<CodaSubExpression> expressions;
    private Vector<String> operators;
    
    /** Creates a new instance of CodaExpression */
    public CodaExpression(Vector<CodaSubExpression> expressions, Vector<String> operators) {
        boolean foundAlias = false;
        int i = 0;
		if (expressions != null) {
			while (!foundAlias && i < expressions.size()) {
				if (expressions.get(i).containsColumn()) {
					foundAlias = true;
					aliasName = expressions.get(i).getAliasName();
					columnName = expressions.get(i).getColumnName();
				}
				i++;
			}
		}
		this.setExpressions(expressions);
        this.setOperators(operators);
    }

    public Vector<CodaSubExpression> getExpressions() {
        return expressions;
    }

    public void setExpressions(Vector<CodaSubExpression> expressions) {
        this.expressions = expressions;
    }

    public Vector<String> getOperators() {
        return operators;
    }

    public void setOperators(Vector<String> operators) {
        this.operators = operators;
    }
    
    public boolean containsColumn() {
        return columnName != null;
    }
    
    public String getColumnName() {
        return columnName.toUpperCase();
    }
    
    public String getAliasName() {
        return aliasName;
    }
    
    public String toString() {
        String retval = "";
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) {
                retval += operators.get(i) + " ";
                retval += expressions.get(i).toString() + " ";
            } else {
                retval += expressions.get(i).toString() + " ";    
            }
        }
        return retval;
    }
    
    public String toString(CodaDatabase database, String tableName, String columnName) {
        String retval = "";
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) {
                retval += operators.get(i);
                retval += expressions.get(i).toString(database, tableName, columnName);
            } else {
                retval += expressions.get(i).toString(database, tableName, columnName);    
            }
        }
        return retval;
    }
}
