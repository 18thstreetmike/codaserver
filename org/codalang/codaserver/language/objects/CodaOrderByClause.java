package org.codalang.codaserver.language.objects;

import java.util.Vector;
/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Nov 25, 2007
 * Time: 12:52:11 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */
public class CodaOrderByClause {
	Vector<String> orders;
	Vector<CodaExpression> expressions;

	public CodaOrderByClause (Vector<CodaExpression> expressions, Vector<String> orders) {
		this.expressions = expressions;
		this.orders = orders;
	}

	public String toString() {
		String retval = "ORDER BY ";
		boolean firstFlag = true;
		int i = 0;
		for (CodaExpression expression : expressions) {
			if (!firstFlag) {
				retval += ", ";
			} else {
				firstFlag = false;
			}
			retval += expression.toString() + " " + orders.get(i) + " ";
			i++;
		}

		return retval;
	}
}
