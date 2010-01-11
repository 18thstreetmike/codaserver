/*
 * JDBCMetadata.java
 *
 * Created on June 6, 2007, 10:47 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.database;

import java.sql.*;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class JDBCMetadata implements CodaDatabaseMetadata {
    
    Connection conn;
    
    /** Creates a new instance of JDBCMetadata */
    public JDBCMetadata(Connection conn) {
        this.conn = conn;
    }

    public String[] getTables() {
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet rs = metadata.getTables(null, null, "%", null);
            Vector<String> retval = new Vector();
            while(rs.next()) {
                retval.add(rs.getString("TABLE_NAME").toUpperCase());
            }
            return retval.toArray(new String[retval.size()]);
        } catch (SQLException ex) {
			return new String[0];
        }
    }

    public ColumnDefinition[] getColumnsForTable(String tableName) {
        try {
            Vector<ColumnDefinition> retvalTemp = new Vector<ColumnDefinition>();
			DatabaseMetaData metadata = conn.getMetaData();
            ResultSet rs = metadata.getColumns(null, null, tableName.toUpperCase(), null);
			while(rs.next()) {
                retvalTemp.add(new ColumnDefinition(rs.getString("COLUMN_NAME").toUpperCase(), rs.getInt("DATA_TYPE"), (rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)));
            }
            return retvalTemp.toArray(new ColumnDefinition[retvalTemp.size()]);
        } catch (SQLException ex) {
            return new ColumnDefinition[0];
        }
    }

    public boolean isTable(String tableName) {
        try {
            String [] retval = new String[0];
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet rs = metadata.getTables(null, null, tableName.toUpperCase(), null);
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    public boolean isColumn(String tableName, String columnName) {
        try {
            String [] retval = new String[0];
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet rs = metadata.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase());
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    public boolean isColumnDefinition(String tableName, ColumnDefinition definition) {
        try {
            String [] retval = new String[0];
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet rs = metadata.getColumns(null, null, tableName.toUpperCase(), definition.getName().toUpperCase());
            if (rs.next()) {
                if (definition.getName().toUpperCase().equals(rs.getString("COLUMN_NAME").toUpperCase()) && definition.getSqlType() == rs.getInt("COLUMN_TYPE") && definition.getNullable() == (rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)) {
                    return true;
                } else {
                    return false;
                } 
            } else {
                return false;
            }
        } catch (SQLException ex) {
            return false;
        }
    }
    
    public boolean doTablesExist(CodaTable[] tables) {
        boolean retval = true;
        for (int i = 0; i < tables.length; i++) {
            if (retval) {
                ColumnDefinition[] tempColumnDefinitions = tables[i].getColumnDefinitions();
                for (int j = 0; j < tempColumnDefinitions.length; j++) {
                    if (retval) {
                        if (!isColumnDefinition(tables[i].getTableName(), tempColumnDefinitions[j])) {
                            retval = false;
                        }
                    }
                }
            }
        } 
        return retval;
    }

    public CodaSystemTable getSystemTable() {
        try {
            boolean codaServerFlag = false;
            long revisionId = 0, refTableRevisionId = 0;
            String applicationName = "", prefixString = "";
            float codaFormatVersion = 1;
            Statement sql = conn.createStatement();
            ResultSet rs = sql.executeQuery("select system_property, system_value from coda_system_information");
            while(rs.next()) {
                if (rs.getString(1).toUpperCase().equals("REVISION_ID")) {
                    revisionId = rs.getLong(2);
                } else if (rs.getString(1).toUpperCase().equals("REF_TABLE_REVISION_ID")) {
                    refTableRevisionId = rs.getLong(2);
                } else if (rs.getString(1).toUpperCase().equals("APPLICATION_NAME")) {
                    applicationName = rs.getString(2);
                } else if (rs.getString(1).toUpperCase().equals("PREFIX")) {
                    prefixString = rs.getString(2);
                } else if (rs.getString(1).toUpperCase().equals("FORMAT_VERSION")) {
                    codaFormatVersion = rs.getFloat(2);
                } else if (rs.getString(1).toUpperCase().equals("CODA_SERVER")) {
                    codaServerFlag = rs.getString(2).toUpperCase().equals("TRUE");
                }
            }
            return new CodaSystemTable(codaServerFlag, revisionId, refTableRevisionId, applicationName, prefixString, codaFormatVersion);
       } catch (Exception e) {
            return null;
    
       }
    }

    public int getMaxTableNameLength() {
        try {
            return conn.getMetaData().getMaxTableNameLength();
        } catch (SQLException ex) {
            return 30;
        }
    }

    public int getMaxColumnNameLength() {
        try {
            return conn.getMetaData().getMaxColumnNameLength();
        } catch (SQLException ex) {
            return 30;
        }
    }

    public int getMaxSchemaNameLength() {
        try {
            return conn.getMetaData().getMaxSchemaNameLength();
        } catch (SQLException ex) {
            return 30;
        }
    }
}
