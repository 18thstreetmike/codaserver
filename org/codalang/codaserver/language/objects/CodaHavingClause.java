package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.CodaException;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Nov 25, 2007
 * Time: 12:25:39 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

public class CodaHavingClause {
	CodaSearchCondition searchCondition;

	public CodaHavingClause(CodaSearchCondition searchCondition) {
		this.searchCondition = searchCondition;
	}

	public String print(CodaFromClause fromClause) throws CodaException {
		return "HAVING " + searchCondition.print(fromClause) + " ";
	}

}
