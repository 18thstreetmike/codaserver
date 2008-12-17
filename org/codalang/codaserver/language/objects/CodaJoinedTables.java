package org.codalang.codaserver.language.objects;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Nov 28, 2007
 * Time: 9:49:36 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

public class CodaJoinedTables {
	CodaSubTableSource subTableSource;
	Vector<CodaJoinedTable> joinedTables;

	public CodaJoinedTables(CodaSubTableSource subTableSource, Vector<CodaJoinedTable> joinedTables) {
		this.subTableSource = subTableSource;
		this.joinedTables = joinedTables;
	}

	public String print(CodaFromClause fromClause) {
		String retval = subTableSource.print(fromClause);
		for (CodaJoinedTable joinedTable : joinedTables) {
			retval += joinedTable.print(fromClause);
		}
		return retval;
	}

}