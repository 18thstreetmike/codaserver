package org.codalang.codaserver.language.types;

import org.codalang.codaserver.database.CodaResultSet;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: michaelarace
 * Date: Apr 19, 2008
 * Time: 2:00:49 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */
public class ResultSet extends CodaResultSet {
	public ResultSet (Vector columnHeadings) {
        super(columnHeadings);
    }

    public ResultSet (String errorString) {
        super(errorString);
    }

	public ResultSet (CodaResultSet rs) {
		super(rs.getColumnHeadings());
		this.data = rs.getData();
    	this.nameColumns = rs.getNameColumns();

    	this.rowPointer = rs.getRowPointer();

		this.errorStatus = rs.getErrorStatus();
    	this.errorString = rs.getErrorString();
	}
}
