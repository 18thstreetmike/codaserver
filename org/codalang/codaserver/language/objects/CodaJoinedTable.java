package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.CodaException;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Nov 25, 2007
 * Time: 4:05:03 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

public class CodaJoinedTable {
	boolean innerFlag = false;
	String direction = "LEFT";
	CodaSearchCondition searchCondition;
	CodaTableSource tableSource;

	public CodaJoinedTable (CodaTableSource tableSource, boolean innerFlag, String direction, CodaSearchCondition searchCondition) {
		this.tableSource = tableSource;
		this.innerFlag = innerFlag;
		this.direction = direction;
		this.searchCondition = searchCondition;
	}

	public String print(CodaFromClause fromClause) {
		try {
			return (innerFlag ? "INNER " : direction + " OUTER ")+ "JOIN " + tableSource.print(fromClause) + "ON " + searchCondition.print(fromClause);
		} catch (CodaException e) {
			return "";
		}
	}
	
}
