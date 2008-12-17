/*
 * CodaFromClause.java
 *
 * Created on August 30, 2007, 9:49 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.language.objects;

import org.codalang.codaserver.CodaException;
import org.codalang.codaserver.CodaServer;
import org.codalang.codaserver.database.CodaDatabase;

import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaFromClause {
    
    private Hashtable<String,String> aliases = new Hashtable<String,String>();
    private Hashtable<String,Vector<String>> columns = new Hashtable<String,Vector<String>>();
    private Vector<String> tables = new Vector<String>();
    private Vector<CodaTableNameAlias> mappings = new Vector<CodaTableNameAlias>();
    private String fromClause;
    private int errorCode = 0;
	private CodaDatabase database;

	private String tableName = null;

	private Vector<CodaTableSource> tableSources = new Vector<CodaTableSource>();


	/** Creates a new instance of CodaFromClause */
    public CodaFromClause(String tableName) {
        this.tableName = tableName;
        mappings.add(new CodaTableNameAlias(tableName, tableName));
    }
    
    public CodaFromClause(Vector<CodaTableNameAlias> mappings, Vector<CodaTableSource> tableSources) {
        this.mappings = mappings;
        for (CodaTableNameAlias mapping : mappings) {
            tables.add(mapping.getTableName());
            if (mapping.getAlias() == null) {
                if (aliases.containsKey(mapping.getTableName())) {
                    setErrorCode(2078);
                    return;
                } else {
                    aliases.put(mapping.getTableName(), mapping.getTableName());
                }
            } else {
                if (aliases.containsKey(mapping.getAlias())) {
                    setErrorCode(2079);
                    return;
                } else {
                    aliases.put(mapping.getAlias(), mapping.getTableName());
                }
            }
        }
		this.tableSources = tableSources;
	}

	public void setColumns(CodaServer server, CodaDatabase database) {
		this.columns = server.getColumnsForTables(database.getConnection(), this.getTables());
		this.database = database;
	}
    
    public String getTableName(String alias, String columnName) throws CodaException {
        if (this.tableName != null) {
            return tableName.toUpperCase();
        } else if (alias != null && aliases.containsKey(alias.toUpperCase())) {
            return aliases.get(alias.toUpperCase());
        } else if (getColumns().containsKey(columnName.toUpperCase())) {
            if (getColumns().get(columnName.toUpperCase()).size() == 1) {
				return getColumns().get(columnName.toUpperCase()).get(0).toUpperCase();
			} else {
				throw new CodaException("The column '" + columnName + "' is ambiguous.");
			}
        } else {
            throw new CodaException("The column '" + columnName + "' is not found in the source tables.");
        }
    }

    public Hashtable<String, Vector<String>> getColumns() {
        return columns;
    }

    public String getFromClause() {
        return fromClause;
    }

    public void setFromClause(String fromClause) {
        this.fromClause = fromClause;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    public Vector<String> getTables () {
        return tables;
    }
    
    public Vector<CodaTableNameAlias> getMappings() {
        return mappings;
    }

	public String print () {
		String retval = "FROM ";
		if (tableSources.size() == 0) {
			retval += tableName + " ";
		} else {
			boolean firstFlag = true;
			for (CodaTableSource tableSource : tableSources) {
				if (firstFlag) {
					firstFlag = false;
				} else {
					retval += ", ";
				}
				retval += tableSource.print(this);
			}
		}
		return retval;
	}

	public CodaDatabase getDatabase() {
		return database;
	}

	public void setDatabase(CodaDatabase database) {
		this.database = database;
	}
}
