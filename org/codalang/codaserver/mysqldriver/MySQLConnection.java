package org.codalang.codaserver.mysqldriver;

import org.codalang.codaserver.database.*;
import org.codalang.codaserver.language.objects.CodaConstant;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Feb 20, 2008
 * Time: 8:46:05 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */
public class MySQLConnection implements CodaConnection {

    private Connection conn = null;
    private java.util.logging.Logger logger;
	private String schemaName;

	public MySQLConnection (Connection conn, String schemaName, Logger logger) {
        this.conn = conn;
		this.schemaName = schemaName;
		try {
            this.conn.setAutoCommit(false);
        } catch (Exception e) {
            // skip it
        }
        
        this.logger = logger;

    }

    public boolean emptySchema() {
        CodaDatabaseMetadata metadata = this.getMetadata();

        String[] tables = new String[0];
        tables = metadata.getTables();

        for (int i = 0; i < tables.length; i++) {
            try {
                this.dropTable(tables[i]);
            } catch (SQLException ex) {
                logger.log(Level.WARNING, ex.getMessage());
            }
        }

        return true;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public CodaDatabaseMetadata getMetadata() {
        return new JDBCMetadata(conn);
    }

    public CodaResultSet runQuery(String query, Vector columnHeadings) {
        try {
            Statement sql = conn.createStatement();
            ResultSet rs = sql.executeQuery(query);
            return resultSetToCodaResultSet(rs, columnHeadings);
        } catch (SQLException ex) {

            logger.log(Level.WARNING, "Query Failed:\n"+query+"\n");
            return new CodaResultSet(ex.getMessage());
        }
    }

	public CodaResultSet runQuery(String query, Vector columnHeadings, long top, long startingAt) {
        try {
            Statement sql = conn.createStatement();
            ResultSet rs = sql.executeQuery(query);
            return resultSetToCodaResultSet(rs, columnHeadings, top, startingAt);
        } catch (SQLException ex) {

            logger.log(Level.WARNING, "Query Failed:\n"+query+"\n");
            return new CodaResultSet(ex.getMessage());
        }
    }

	public boolean runStatement(String sqlStatement) {
        try {
            Statement sql = conn.createStatement();
            sql.execute(sqlStatement);

            return true;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "SQL Statement Failed:\n"+sqlStatement+"\n");
            return false;
        }
    }

    public boolean runStatementWithException(String sqlStatement) throws SQLException {
        Statement sql = conn.createStatement();
        sql.execute(sqlStatement);

        return true;
    }

    public boolean createTable(String tableName, Vector columnDefinitions) throws SQLException {
        String sql = "CREATE TABLE " + tableName.toLowerCase() + " ( ";

        boolean firstFlag = true;
        Iterator it = columnDefinitions.iterator();
        while (it.hasNext()) {
            ColumnDefinition cd = (ColumnDefinition)it.next();
            if (firstFlag) {
                firstFlag = false;
            } else {
                sql += ", ";
            }
            sql = sql + cd.getName().toLowerCase() + " " + getSQLTypeName(cd.getSqlType()) + " ";
            if (!cd.getNullable()) {
                sql += "NOT NULL";
            } else {
				sql += "NULL";
			}
        }
        sql += ") ";
        if (runStatementWithException(sql)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean alterTable (String tableName, String newTableName) throws SQLException {
        if (tableExists(newTableName.toUpperCase())) {
            return false;
        } else {
            String sql = "ALTER TABLE " + tableName.toLowerCase() + " RENAME TO " + newTableName.toLowerCase();
            if (runStatementWithException(sql)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean addColumn(String tableName, ColumnDefinition columnDefinition) throws SQLException {
        String sql = "ALTER TABLE " + tableName.toLowerCase() + " ADD COLUMN " + columnDefinition.getName().toLowerCase() + " " + getSQLTypeName(columnDefinition.getSqlType()) + " ";

        if (!columnDefinition.getNullable()) {
            sql += "NOT NULL";
        } else {
            sql += "NULL";
        }
        if (runStatementWithException(sql)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean alterColumn (String tableName, String columnName, ColumnDefinition columnDefinition) throws SQLException {
        String sql = "";

        sql = "ALTER TABLE " + tableName.toLowerCase() + " CHANGE COLUMN " + columnName + " " + columnDefinition.getName().toLowerCase() + " " + getSQLTypeName(columnDefinition.getSqlType()) + " ";

        if (!columnDefinition.getNullable()) {
            sql += " NOT NULL";
        } else {
            sql += " NULL";
        }
        if (runStatementWithException(sql)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean dropColumn (String tableName, String columnName) throws SQLException {
        String sql = "ALTER TABLE " + tableName.toLowerCase() + " DROP COLUMN " + columnName;

        if (runStatementWithException(sql)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean dropTable(String tableName) throws SQLException {
        String sql = "DROP TABLE " + tableName;

        if (runStatementWithException(sql)) {
            conn.commit();
            
            return true;
        } else {
            return false;
        }
    }

    public boolean setPrimaryKey(String tableName, String columnName) throws SQLException {
        String sql =  "ALTER TABLE " + tableName.toLowerCase() + " MODIFY COLUMN " + columnName.toLowerCase() + " BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY";
        return this.runStatementWithException(sql);
    }

    public boolean setIdentityColumns(String indexName, String tableName, Vector columnNames) throws SQLException {
        String sql = "CREATE UNIQUE INDEX " + indexName + " ON " + tableName.toLowerCase() + " ( ";

        boolean firstFlag = true;
        Iterator it = columnNames.iterator();
        while (it.hasNext()) {
            if (firstFlag) {
                firstFlag = false;
            } else {
                sql += ", ";
            }
            sql += (String)it.next();
        }
        sql += ")";
        return runStatementWithException(sql);
    }

    public boolean createIndex(String indexName, String tableName, Vector columnNames, boolean uniqueFlag) throws SQLException {
        String sql = "CREATE " + (uniqueFlag ? "UNIQUE " : "") + "INDEX " + indexName.toLowerCase() + " ON " + tableName.toLowerCase() + " ( ";

        boolean firstFlag = true;
        Iterator it = columnNames.iterator();
        while (it.hasNext()) {
            if (firstFlag) {
                firstFlag = false;
            } else {
                sql += ", ";
            }
            sql += (String)it.next();
        }
        sql += ")";
        return runStatementWithException(sql);
    }

    public boolean dropIndex(String indexName) throws SQLException {
		String tableName = null;

		CodaResultSet rs = this.runQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE table_schema = '" + schemaName.toUpperCase() +"' and constraint_name = '" + indexName.toUpperCase() + "'", null);
		while (rs.next()) {
			tableName = rs.getData(0);
		}

		if (tableName != null) {
			String sql = "DROP INDEX " + indexName + " ON " + tableName;
			return runStatementWithException(sql);
		} else {
			return false;
		}
	}

    public boolean createForeignKey (String keyName, String tableName, String columnName, String targetTableName, String targetColumnName) throws SQLException {
        String sql = "ALTER TABLE " + tableName.toLowerCase() + " ADD FOREIGN KEY " + keyName + " ( "+columnName+" ) REFERENCES  " + targetTableName + " ( " + targetColumnName + " ) ";
        return runStatementWithException(sql);
    }

    public boolean dropForeignKey (String tableName, String keyName) throws SQLException {
        String sql = "ALTER TABLE " +tableName.toLowerCase() + " DROP KEY " +keyName;
        return runStatementWithException(sql);
    }

    public long insertRow(String tableName, Hashtable values) {
        long id = -1;

        String columnList = "", valueList = "";

        boolean firstFlag = true;
        Enumeration en = values.keys();
        while(en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if (firstFlag) {
                firstFlag = false;
            } else {
                columnList += ", ";
                valueList += ", ";
            }
            columnList += key + " ";

            Object temp = values.get(key);
            if (values.get(key).getClass().getName() == "org.codalang.codaserver.language.objects.CodaConstant" && ((CodaConstant)values.get(key)).getSysvar() == CodaConstant.SYSVAR_NULL) {
                valueList += "null ";                
            } else {
                valueList += formatStringForSQL(tableName, key, values.get(key).toString()) + " ";
            }
        }

        String sql = "INSERT INTO " + tableName + " ( " + columnList + ") VALUES (" + valueList + ")";

        try {
            Statement st = conn.createStatement();
            st.executeUpdate(sql);

            ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()");

            while (rs.next()) {
                id = rs.getLong(1);
            }
        } catch (SQLException e) {
			logger.log(Level.WARNING, e.getMessage());
        }
        return id;
    }


    public boolean updateRow(String tableName, String idColumnName, long id, Hashtable values) {
        String sql = "UPDATE " + tableName + " SET ";

        String valueList = "";

        boolean firstFlag = true;
        Enumeration en = values.keys();
        while(en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if (firstFlag) {
                firstFlag = false;
            } else {
                valueList += ", ";
            }
            if (values.get(key).getClass().getName() == "org.codalang.codaserver.language.objects.CodaConstant" && ((CodaConstant)values.get(key)).getSysvar() == CodaConstant.SYSVAR_NULL) {
                valueList += key + " = null ";
            } else {
                valueList += key + " = " + formatStringForSQL(tableName, key, values.get(key).toString()) + " ";
            }
        }

        sql += valueList + " WHERE " + idColumnName + " = " + id;

        return runStatement(sql);
    }

    public boolean deleteRow(String tableName, String idColumnName, long id) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumnName + " = " + id;

        return runStatement(sql);
    }

    public boolean updateRows(String tableName, String idColumnName, Vector<Long> ids, Hashtable values) {
        String sql = "UPDATE " + tableName + " SET ";

        String valueList = "";

        boolean firstFlag = true;
        Enumeration en = values.keys();
        while(en.hasMoreElements()) {
            String key = (String) en.nextElement();
            if (firstFlag) {
                firstFlag = false;
            } else {
                valueList += ", ";
            }
            if (values.get(key).getClass().getName() == "org.codalang.codaserver.language.objects.CodaConstant" && ((CodaConstant)values.get(key)).getSysvar() == CodaConstant.SYSVAR_NULL) {
                valueList += key + " = null ";
            } else {
				valueList += key + " = " + formatStringForSQL(tableName, key, values.get(key).toString()) + " ";
			}
        }

        sql += valueList + " WHERE " + idColumnName + " IN  (";
        boolean start = true;
        for (Long id : ids) {
            if (start) {
                sql += id;
                start = false;
            } else {
                sql += ", " + id;
            }
        }
        sql += ")";

        return runStatement(sql);
    }

    public boolean deleteRows(String tableName, String idColumnName, Vector<Long> ids) {
        String sql = "DELETE FROM " + tableName + " ";
        sql += " WHERE " + idColumnName + " IN  (";
        boolean start = true;
        for (Long id : ids) {
            if (start) {
                sql += id;
                start = false;
            } else {
                sql += ", " + id;
            }
        }
        sql += ")";

        return runStatement(sql);
    }

    public CodaResultSet selectRow(String tableName, String idColumnName, long id) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = " + id;

        return runQuery(sql, null);
    }

    public CodaResultSet selectRows(String tableName, String idColumnName, Vector<Long> ids) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " IN (";
        boolean start = true;
        for (Long id : ids) {
            if (start) {
                sql += id;
                start = false;
            } else {
                sql += ", " + id;
            }
        }
        sql += ")";

        return runQuery(sql, null);
    }

    public CodaResultSet selectRow(String tableName, long id) {
        return selectRow(tableName, "id", id);
    }

    private String getSQLTypeName(int javaSQLTypeID) {
        String retval = "";
        switch (javaSQLTypeID) {
            case java.sql.Types.BIGINT:
            case java.sql.Types.INTEGER:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
                retval = "BIGINT";
                break;
            case java.sql.Types.DOUBLE:
            case java.sql.Types.FLOAT:
            case java.sql.Types.REAL:
                retval = "FLOAT";
                break;
            case java.sql.Types.DECIMAL:
            case java.sql.Types.NUMERIC:
                retval = "NUMERIC";
                break;
            case java.sql.Types.DATE:
                retval = "DATE";
                break;
            case java.sql.Types.TIME:
                retval = "TIME";
                break;
            case java.sql.Types.TIMESTAMP:
                retval = "DATETIME";
                break;
            case java.sql.Types.BOOLEAN:
            case java.sql.Types.BIT:
                retval = "INTEGER";
                break;
            case java.sql.Types.VARCHAR:
            case java.sql.Types.CHAR:
				retval = "VARCHAR(255) ";
				break;
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.CLOB:
				retval = "LONGTEXT ";
				break;
            case java.sql.Types.BINARY:
            case java.sql.Types.VARBINARY:
            case java.sql.Types.LONGVARBINARY:
            case java.sql.Types.BLOB:
                retval = "LONGBLOB";
                break;
            default:
                retval = "VARCHAR(255) ";
                break;
        }
        return retval;
    }

    private String escapeString(String val) {
        return val.replace("'", "''").replace("\\", "\\\\");
    }

    private CodaResultSet resultSetToCodaResultSet(ResultSet rs, Vector columnHeadings) {
        CodaResultSet wrs = new CodaResultSet(columnHeadings);

        try {
            if (columnHeadings == null) {
                ResultSetMetaData rsmd = rs.getMetaData();
                columnHeadings = new Vector();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    columnHeadings.add(new CodaResultSetColumnHeading(rsmd.getColumnLabel(i+1), rsmd.getColumnType(i+1)));
                }
                wrs = new CodaResultSet(columnHeadings);
            }

            while (rs.next()) {
                Vector row = new Vector();
                for (int j = 0; j < columnHeadings.size(); j++) {
                    row.add(rs.getString(j+1));
                }

                wrs.addRow(row);

            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, ex.getMessage());
        }
        return wrs;
    }

	private CodaResultSet resultSetToCodaResultSet(ResultSet rs, Vector columnHeadings, long top, long startingAt) {
        CodaResultSet wrs = new CodaResultSet(columnHeadings);

        try {
            if (columnHeadings == null) {
                ResultSetMetaData rsmd = rs.getMetaData();
                columnHeadings = new Vector();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    columnHeadings.add(new CodaResultSetColumnHeading(rsmd.getColumnLabel(i+1).equals("") ? "COLUMN" : rsmd.getColumnLabel(i+1), rsmd.getColumnType(i+1)));
                }
                wrs = new CodaResultSet(columnHeadings);
            }

			if (startingAt > 0) {
				for (long i = 0; i < startingAt; i++) {
					if (!rs.next()) {
						i = startingAt;
					}
				}
			}

			boolean useLimit = false;
			if (top > 0) {
				useLimit = true;
			}

			long k = 0;

			while (rs.next() && (!useLimit || (useLimit && top > k))) {
                Vector row = new Vector();
                for (int j = 0; j < columnHeadings.size(); j++) {
                    row.add(rs.getString(j+1));
                }

                wrs.addRow(row);
				k++;
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, ex.getMessage());
        }
        return wrs;
    }

    private boolean tableExists(String tableName) {
		boolean retval = false;
		try {
			DatabaseMetaData metadata = conn.getMetaData();
			ResultSet rs = metadata.getTables(null, null, tableName.toLowerCase(), null);
			while (rs.next()) {
				if (rs.getString(3).equalsIgnoreCase(tableName)) {
					retval = true;
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Unable to read the schema metadata.");
		}
		
		return retval;
	}

	private boolean columnExists(String tableName, String columnName) {
		boolean retval = false;
		try {
			DatabaseMetaData metadata = conn.getMetaData();
			ResultSet rs = metadata.getColumns(null, null, tableName.toLowerCase(), columnName.toLowerCase());
			while (rs.next()) {
				if (rs.getString(4).equalsIgnoreCase(columnName)) {
					retval = true;
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Unable to read the schema metadata.");
		}

		return retval;
	}

	private String getColumnType(String tableName, String columnName) {
		CodaResultSet rs = runQuery("select upper(data_type) from information_schema.columns where upper(table_schema) = upper('"+schemaName+"') and upper(table_name) = upper('"+tableName+"') and upper(column_name) = upper('"+columnName+"')", null);
		while(rs.next()) {
			return rs.getData(0);
		}
		return "VARCHAR";
	}

	public String formatStringForSQL(String tableName, String columnName, String value) {
        tableName = tableName.toUpperCase();
        columnName = columnName.toUpperCase();
        if (tableExists(tableName)) {
			if (columnExists(tableName, columnName)) {
				int columnClass = getColumnClass(getColumnType(tableName, columnName));
				switch (columnClass) {
                    case 1:
                        value = value;
                        break;
                    case 2:
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        value =  sdf.format(new java.util.Date(Long.parseLong(value)));
                    default:
                        value = "'" + this.escapeString(value) + "'";
                        break;
                }
            }
        }
        return value;
    }

    /**
     * 0: String
     * 1: Numeric
     * 2: Date
    */
    private int getColumnClass(String type) {
        if (type.equals("NUMERIC") || type.equals("INTEGER") || type.equals("BIGINT") || type.equals("FLOAT") || type.equals("BOOLEAN")) {
            return 1;
        } else if (type.equals("DATE") || type.equals("TIMESTAMP")|| type.equals("DATETIME")) {
            return 2;
        } else {
            return 0;
        }
    }

    public void setAutoCommit(boolean autoCommit) {
        try {
            conn.setAutoCommit(autoCommit);
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Autocommit on '" + this.getMetadata().getSystemTable().getApplicationName() + "' is not toggling!");
        }
    }

    public boolean getAutoCommit() {
        try {
            return conn.getAutoCommit();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Autocommit on '" + this.getMetadata().getSystemTable().getApplicationName() + "' is not returning!");
        }

        return false;
    }

    public void commit() {
        try {
            conn.commit();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Cannot commit on '" + this.getMetadata().getSystemTable().getApplicationName() + "'!");
        }
    }

    public void rollback() {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Cannot rollback on '" + this.getMetadata().getSystemTable().getApplicationName() + "'!");
        }
    }

    public void closeConnection() {
        try {
            conn.close();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, ex.getMessage());
        }
    }
}
