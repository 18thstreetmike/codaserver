package org.codalang.codaserver.database;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: mike
 * Date: Feb 20, 2008
 * Time: 8:43:01 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */
public interface CodaConnection {
    boolean emptySchema();

    void setLogger(Logger logger);

    CodaDatabaseMetadata getMetadata();

    CodaResultSet runQuery (String query, Vector columnHeadings);

    CodaResultSet runQuery (String query, Vector columnHeadings, long top, long startingAt);

    boolean runStatement(String sqlStatement);

    boolean runStatementWithException(String sqlStatement) throws SQLException;// create, modify, and drop tables

    boolean createTable (String tableName, Vector columnDefinitions) throws SQLException;

    boolean alterTable (String tableName, String newTableName) throws SQLException;

    boolean addColumn (String tableName, ColumnDefinition columnDefinition) throws SQLException;

    boolean alterColumn (String tableName, String columnName, ColumnDefinition columnDefinition) throws SQLException;

    boolean dropColumn (String tableName, String columnName) throws SQLException;

    boolean dropTable (String tableName) throws SQLException;// Modify constaints on tables

    boolean setPrimaryKey (String tableName, String columnName) throws SQLException;

    boolean setIdentityColumns (String indexName, String tableName, Vector columnNames) throws SQLException;

    boolean createIndex (String indexName, String tableName, Vector columnNames,  boolean uniqueFlag) throws SQLException;

    boolean dropIndex (String indexName) throws SQLException;

    boolean createForeignKey (String keyName, String tableName, String columnName, String targetTableName, String targetColumnName) throws SQLException;

    boolean dropForeignKey (String tableName, String keyName) throws SQLException;// dml statements

    long insertRow (String tableName, Hashtable values);

    boolean updateRow (String tableName, String idColumnName, long id, Hashtable values);

    boolean deleteRow(String tableName, String idColumnName, long id);

    boolean updateRows (String tableName, String idColumnName, Vector<Long> ids, Hashtable values);

    boolean deleteRows (String tableName, String idColumnName, Vector<Long> ids);

    CodaResultSet selectRow (String tableName, long id);

    CodaResultSet selectRow (String tableName, String idColumnName, long id);

    CodaResultSet selectRows (String tableName, String idColumnName,  Vector<Long> ids);

    String formatStringForSQL(String tableName, String columnName, String value);

    void setAutoCommit(boolean autoCommit);

    boolean getAutoCommit();

    void commit();

    void rollback();
}
