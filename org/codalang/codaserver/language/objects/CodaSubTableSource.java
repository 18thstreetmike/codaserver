package org.codalang.codaserver.language.objects;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Nov 28, 2007
 * Time: 9:08:16 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */
public class CodaSubTableSource {
	private String alias;
	private String tableName;
	CodaJoinedTables joinedTables;

	public CodaSubTableSource(String tableName, String alias) {
		this.setAlias(alias);
		this.setTableName(tableName);
		this.joinedTables = null;
	}

	public CodaSubTableSource(CodaJoinedTables joinedTables) {
		this.setAlias(null);
		this.setTableName(null);
		this.joinedTables = joinedTables;
	}

	public String print(CodaFromClause fromClause) {
		if (joinedTables == null) {
			return getTableName() + " " + (getAlias() != null ? "AS " + getAlias() + " ": "");
		} else {
			return joinedTables.print(fromClause);
		}
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
}
