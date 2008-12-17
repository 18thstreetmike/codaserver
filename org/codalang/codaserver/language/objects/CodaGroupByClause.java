package org.codalang.codaserver.language.objects;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Nov 25, 2007
 * Time: 12:34:35 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

public class CodaGroupByClause {
	Vector<CodaExpression> expressions;
	boolean allFlag = false;

	public CodaGroupByClause(Vector<CodaExpression> expressions, boolean allFlag) {
		this.expressions = expressions;
		this.allFlag = allFlag;
	}

	public String toString() {
		String retval = "GROUP BY " + (allFlag? "ALL " : "");
		boolean firstFlag = true;
		for (CodaExpression expression : expressions) {
			if (!firstFlag) {
				retval += ", ";
			} else {
				firstFlag = false;
			}
			retval += expression.toString();
		}
		return retval;
	}
}
