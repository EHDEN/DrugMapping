/*******************************************************************************
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.databases;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.utilities.SimpleCounter;
import org.ohdsi.utilities.StringUtilities;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.ReadTextFile;
import org.ohdsi.utilities.files.Row;
import org.ohdsi.utilities.files.WriteCSVFileWithHeader;

public class RichConnection {
	public static int				INSERT_BATCH_SIZE	= 100000;
	private static String			SUBSTITUTE 			= Character.toString((char) 26) + Character.toString((char) 26);
	private Connection				connection;
	private Class<?>				context;											// Used for locating resources
	private boolean					verbose				= false;
	private static DecimalFormat	decimalFormat		= new DecimalFormat("#.#");
	private Map<String, String>		localVariables		= new HashMap<String, String>();
	private DbType					dbType;
	private String					password;

	public RichConnection(String server, String domain, String user, String password, DbType dbType) {
		this.password = password;
		this.connection = DBConnector.connect(server, domain, user, password, dbType);
		this.dbType = dbType;
		
		// In case of SQL Server make sure the database is the default database for the user.
		if (this.dbType == DbType.MSSQL) {
			String database = null;
			String[] serverSplit = server.split(";");
			for (String option : serverSplit) {
				option = option.trim();
				if (option.startsWith("database") && option.contains("=")) {
					database = option.split("=")[1].trim();
					break;
				}
			}
			if ((database != null) && (!database.equals(""))) {
				execute("ALTER LOGIN " + user + " WITH DEFAULT_DATABASE = [" + database + "];");
			}
		}
	}

	/**
	 * Executes the SQL statement specified in the resource. The first parameter string is the parameter name, the second string the value, etc.
	 * 
	 * @param resourceName
	 * @param parameters
	 */
	public void executeResource(String resourceName, Object... parameters) {
		QueryParameters parameterMap = new QueryParameters();
		for (int i = 0; i < parameters.length; i += 2)
			parameterMap.set(parameters[i].toString(), parameters[i + 1].toString());
		executeResource(resourceName, parameterMap);
	}

	/**
	 * Executes the SQL statement specified in the resource
	 * 
	 * @param resourceName
	 * @param parameters
	 */
	public void executeResource(String resourceName, QueryParameters parameters) {
		executeResource(context.getResourceAsStream(resourceName), parameters);
	}

	/**
	 * Executes the SQL statement specified in the resource
	 * 
	 * @param sqlStream
	 * @param parameters
	 */
	public void executeResource(InputStream sqlStream, QueryParameters parameters) {
		String sql = loadSQL(sqlStream);
		if (parameters != null)
			sql = applyVariables(sql, parameters.getMap());
		execute(sql);
	}

	/**
	 * Executes the SQL statement specified in the resource
	 * 
	 * @param sqlStream
	 */
	public void executeResource(InputStream sqlStream) {
		executeResource(sqlStream, null);
	}

	/**
	 * Executes the SQL statement specified in the resource
	 * 
	 * @param sql
	 */
	public void executeResource(String sql) {
		if (context == null)
			throw new RuntimeException("Context not specified, unable to load resource");
		executeResource(context.getResourceAsStream(sql));
	}
	
	public void executeLocalFile(String sql) {
		File localFile = new File(sql);
		FileInputStream localFileStream;
		try {
			localFileStream = new FileInputStream(localFile);
			executeResource(localFileStream);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("ERROR reading file: " + sql);
		}
	}

	public void executeAsOne(String sql) {
		try {
			Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			statement.execute(sql);
		} catch (SQLException e) {
			System.err.println(sql);
			System.err.println("Message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Execute the given SQL statement.
	 * 
	 * @param sql
	 */
	public void execute(String sql) {
		try {
			sql = handleSQLDefineStatements(sql);
			if (sql.length() == 0)
				return;
			sql = stripComments(sql);
			
			Statement statement = null;

			statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			for (String subQuery : sql.split(";")) {
				subQuery = subQuery.trim();
				if (!subQuery.equals("")) {
					if (verbose) {
						String abbrSQL = subQuery.replace('\n', ' ').replace('\t', ' ').trim();
						//if (abbrSQL.length() > 100)
						//	abbrSQL = abbrSQL.substring(0, 100).trim() + "...";
						System.out.println("Adding query to batch: " + abbrSQL);
					}
					statement.addBatch(subQuery);
				}
			}
			long start = System.currentTimeMillis();
			if (verbose)
				System.out.println("Executing batch");
			statement.executeBatch();
			if (verbose)
				outputQueryStats(statement, System.currentTimeMillis() - start);
			statement.close();
		} catch (SQLException e) {
			if (
					(e != null) && 
					(!e.getMessage().contains("ORA-01435")) && // Suppress Oracle user (schema) does not exist error
					(!e.getMessage().contains("ORA-01918"))    // Suppress Oracle user (schema) does not exist error
				) {
				System.err.println(sql);
				e.printStackTrace();
				e = e.getNextException();
				String message = e.getMessage();
				message = message + "\n" + e.getMessage();
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace();
				throw new RuntimeException(message);
			}
		}
	}

	private String handleSQLDefineStatements(String sql) {
		sql = extractSQLDefineStatements(sql);
		sql = applyVariables(sql, localVariables);
		return sql;
	}

	private String applyVariables(String sql, Map<String, String> key2Value) {
		List<String> sortedKeys = new ArrayList<String>(key2Value.keySet());
		Collections.sort(sortedKeys, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				if (o1.length() > o2.length())
					return 1;
				else if (o1.length() < o2.length())
					return -1;
				else
					return 0;
			}
		});
		for (String key : sortedKeys)
			sql = sql.replaceAll(key, key2Value.get(key));
		return sql.trim();
	}

	private String extractSQLDefineStatements(String sql) {
		int start = sql.toLowerCase().indexOf("sqldefine");

		while (start != -1) {
			int end = sql.indexOf(";", start);
			if (end == -1)
				throw new RuntimeException("No closing semicolon found for SQLDEFINE in:\n" + sql);

			String definition = sql.substring(start, end);
			int as = definition.toLowerCase().indexOf(" as");
			if (as == -1)
				as = definition.toLowerCase().indexOf("\nas");
			if (as == -1)
				as = definition.toLowerCase().indexOf("\tas");
			if (as == -1)
				throw new RuntimeException("No AS found for SQLDEFINE in:\n" + sql);
			String variableName = definition.substring("SQLDEFINE".length(), as).trim();
			String variableValue = definition.substring(as + 3).trim();
			variableValue = applyVariables(variableValue, localVariables);
			if (verbose)
				System.out.println("Found definition for " + variableName);
			localVariables.put(variableName, variableValue);
			sql = sql.substring(0, start) + " " + sql.substring(end + 1);

			start = sql.toLowerCase().indexOf("sqldefine");
		}
		return sql;
	}
	
	private String stripComments(String sql) {
		String strippedSql = "";
		boolean commentBlock = false;
		boolean commentLine = false;
		String endComment = "";
		int index = 0;
		while (index < sql.length()) {
			if (commentBlock || commentLine) {
				if ((sql.length() > (index + endComment.length())) && (sql.substring(index, index + endComment.length()).equals(endComment))) {
					if (commentBlock) {
						index ++;
					}
					else {
						index--;
					}
					commentBlock = false;
					commentLine = false;
				}
			}
			else {
				if ((sql.length() > (index + 2)) && (sql.substring(index, index + 2).equals("/*"))) {
					commentBlock = true;
					endComment = "*/";
					index++;
				}
				else if ((sql.length() > (index + 2)) && (sql.substring(index, index + 2).equals("--"))) {
					commentLine = true;
					endComment = "\n";
					index++;
				}
				else {
					strippedSql += sql.substring(index, index + 1);
				}
			}
			index++;
		}
		while (strippedSql.startsWith("\n")) {
			strippedSql = strippedSql.substring(1);
		}
		return strippedSql;
	}

	private void outputQueryStats(Statement statement, long ms) throws SQLException {
		Throwable warning = statement.getWarnings();
		if (warning != null)
			System.out.println("- SERVER: " + warning.getMessage());
		String timeString;
		if (ms < 1000)
			timeString = ms + " ms";
		else if (ms < 60000)
			timeString = decimalFormat.format(ms / 1000d) + " seconds";
		else if (ms < 3600000)
			timeString = decimalFormat.format(ms / 60000d) + " minutes";
		else
			timeString = decimalFormat.format(ms / 3600000d) + " hours";
		System.out.println("- Query completed in " + timeString);
	}

	public QueryResult queryResource(String resourceName) {
		return queryResource(resourceName, new QueryParameters());
	}

	public QueryResult queryResource(String resourceName, QueryParameters parameters) {
		if (context == null)
			throw new RuntimeException("Context not specified, unable to load resource");
		return queryResource(context.getResourceAsStream(resourceName), parameters);
	}

	/**
	 * Query the database using the SQL statement specified in the resource
	 * 
	 * @param sqlStream
	 * @param parameters
	 * @return
	 */
	public QueryResult queryResource(InputStream sqlStream, QueryParameters parameters) {
		String sql = loadSQL(sqlStream);
		sql = applyVariables(sql, parameters.getMap());
		return query(sql);
	}

	/**
	 * Query the database using the SQL statement specified in the resource
	 * 
	 * @param sqlStream
	 * @return
	 */
	public QueryResult queryResource(InputStream sqlStream) {
		return queryResource(sqlStream, null);
	}

	/**
	 * Query the database using the provided SQL statement.
	 * 
	 * @param sql
	 * @return
	 */
	public QueryResult query(String sql) {
		return new QueryResult(sql);
	}

	/**
	 * Switch the database to use.
	 * 
	 * @param schema
	 */
	public void use(String schema) {
		if (schema == null)
			return;
		if (dbType == DbType.ORACLE)
			execute("ALTER SESSION SET current_schema = " + schema.toUpperCase());
		else if (dbType == DbType.POSTGRESQL)
			execute("SET search_path TO " + schema);
		else if (dbType != DbType.MSSQL) // Not possible in MSSQL
			execute("USE " + schema);
	}

	public List<String> getDatabaseNames() {
		List<String> names = new ArrayList<String>();
		String query = null;
		if (dbType == DbType.MYSQL)
			query = "SHOW DATABASES";
		else if (dbType == DbType.MSSQL)
			query = "SELECT name FROM master..sysdatabases";
		else
			throw new RuntimeException("Database type not supported");

		for (Row row : query(query))
			names.add(row.get(row.getFieldNames().get(0), true));
		return names;
	}

	public List<String> getTableNames(String schema) {
		List<String> names = new ArrayList<String>();
		String query = null;
		if (dbType == DbType.MYSQL) {
			if (schema == null)
				query = "SHOW TABLES";
			else
				query = "SHOW TABLES IN " + schema;
		} else if (dbType == DbType.MSSQL) {
			query = "IF SCHEMA_ID('" + schema + "') IS NOT NULL" + 
					"    SELECT TABLE_NAME" + 
					"    FROM INFORMATION_SCHEMA.TABLES" + 
					"    WHERE TABLE_TYPE = 'BASE TABLE'" + 
					"      AND TABLE_SCHEMA = '" + schema + "' " + 
					"ELSE" + 
					"    SELECT '' AS TABLE_NAME";
		} else if (dbType == DbType.ORACLE) {
			query = "SELECT table_name FROM all_tables WHERE owner='" + schema.toUpperCase() + "'";
		} else if (dbType == DbType.POSTGRESQL) {
			query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + schema.toLowerCase() + "'";
		}

		for (Row row : query(query)) {
			String tableName = row.get(row.getFieldNames().get(0), true);
			if (!tableName.equals("")) {
				names.add(row.get(row.getFieldNames().get(0), true));
			}
		}
		return names;
	}

	public List<String> getFieldNames(String schema, String table) {
		List<String> names = new ArrayList<String>();
		if (dbType == DbType.MSSQL) {
			for (Row row : query("SELECT name FROM sys.syscolumns WHERE id=OBJECT_ID('" + schema + "." + table + "')"))
				names.add(row.get("name", true));
		}
		else if (dbType == DbType.MYSQL) {
			for (Row row : query("SHOW COLUMNS FROM " + table)) {
				names.add(row.get("COLUMN_NAME", true));
			}
		}
		else if (dbType == DbType.POSTGRESQL) {
			for (Row row : query("SELECT column_name FROM information_schema.columns WHERE table_name='" + table.toLowerCase() + "'")) {
				names.add(row.get("column_name", true));
			}
		}
		else if (dbType == DbType.ORACLE) {
			for (Row row : query("SELECT COLUMN_NAME FROM ALL_TAB_COLS WHERE TABLE_NAME = '" + table.toUpperCase() + "' AND owner = '" + schema.toUpperCase() + "' AND NOT REGEXP_LIKE(COLUMN_NAME, '^SYS_')")) {
				names.add(row.get("COLUMN_NAME", true));
			}
		}
		else {
			throw new RuntimeException("DB type not supported");
		}

		return names;
	}

	public Map<String, String> getFieldTypes(String schema, String table) {
		Map<String, String> types = new HashMap<String, String>();
		if (dbType == DbType.MSSQL) {
			for (Row row : query("SELECT c.name as column_name, t.name as data_type FROM sys.syscolumns c LEFT OUTER JOIN sys.types t ON t.system_type_id = c.xtype WHERE id=OBJECT_ID('" + schema + "." + table + "')")) {
				String columnName = row.get("column_name", true).toUpperCase();
				String columnType = row.get("data_type", true).toUpperCase();
				types.put(columnName, columnType);
			}
		}
		//else if (dbType == DbType.MYSQL) {
		//	for (Row row : query("SHOW COLUMNS FROM " + table)) {
		//		String columnName = row.get("COLUMN_NAME");
		//		String columnType = row.get("data_type").toUpperCase();
		//		types.put(columnName, columnType);
		//	}
		//}
		else if (dbType == DbType.POSTGRESQL) {
			for (Row row : query("SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = '" + schema.toLowerCase() + "' AND table_name='" + table.toLowerCase() + "'")) {
				String columnName = row.get("column_name", true).toUpperCase();
				String columnType = row.get("data_type", true).toUpperCase();
				types.put(columnName, columnType);
			}
		}
		else if (dbType == DbType.ORACLE) {
			for (Row row : query("SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLS WHERE TABLE_NAME = '" + table.toUpperCase() + "' AND owner = '" + schema.toUpperCase() + "' AND NOT REGEXP_LIKE(COLUMN_NAME, '^SYS_')")) {
				String columnName = row.get("COLUMN_NAME", true).toUpperCase();
				String columnType = row.get("DATA_TYPE", true).toUpperCase();
				types.put(columnName, columnType);
			}
		}
		else {
			throw new RuntimeException("DB type not supported");
		}

		return types;
	}
	
	public Map<String, List<String>> getForeignKeyConstraints(String schema) {
		return getForeignKeyConstraints(schema, null);
	}
	
	public Map<String, List<String>> getForeignKeyConstraints(String schema, String table) {
		Map<String, List<String>> foreignKeyConstraints = new HashMap<String, List<String>>();
		if (dbType == DbType.MSSQL) {
			for (Row row : query("SELECT fk_tab.name as fk_table_name, fk.name as fk_constraint_name FROM sys.foreign_keys fk INNER JOIN sys.tables fk_tab ON fk_tab.object_id = fk.parent_object_id WHERE schema_name(fk_tab.schema_id) = '" + schema.toUpperCase() + "'" + (table == null ? "" : " AND fk_tab.name = '" + table.toUpperCase() + "'") + ";")) {
				String tableName = row.get("fk_table_name", true);
				String constraintName = row.get("fk_constraint_name", true);
				List<String> tableConstraints = foreignKeyConstraints.get(tableName);
				if ( tableConstraints == null) {
					tableConstraints = new ArrayList<String>();
					foreignKeyConstraints.put(tableName, tableConstraints);
				}
				tableConstraints.add(constraintName);
			}
		}
		else if (dbType == DbType.POSTGRESQL) {
			for (Row row : query("SELECT rel.relname, con.conname FROM pg_catalog.pg_constraint con INNER JOIN pg_catalog.pg_class rel ON rel.oid = con.conrelid INNER JOIN pg_catalog.pg_namespace nsp ON nsp.oid = connamespace WHERE nsp.nspname = '" + schema.toLowerCase() + "' AND con.contype = 'f'" + (table == null ? "" : " AND rel.relname = '" + table.toLowerCase() + "'") + ";")) {
				String tableName = row.get("relname", true);
				String constraintName = row.get("conname", true);
				List<String> tableConstraints = foreignKeyConstraints.get(tableName);
				if ( tableConstraints == null) {
					tableConstraints = new ArrayList<String>();
					foreignKeyConstraints.put(tableName, tableConstraints);
				}
				tableConstraints.add(constraintName);
			}
		}
		else if (dbType == DbType.ORACLE) {
			for (Row row : query("SELECT TABLE_NAME, CONSTRAINT_NAME FROM ALL_CONSTRAINTS WHERE OWNER = '" + schema.toUpperCase() + "' AND CONSTRAINT_TYPE = 'R'" + (table == null ? "" : " AND TABLE_NAME = '" + table.toUpperCase() + "'") + ";")) {
				String tableName = row.get("TABLE_NAME", true);
				String constraintName = row.get("CONSTRAINT_NAME", true);
				List<String> tableConstraints = foreignKeyConstraints.get(tableName);
				if ( tableConstraints == null) {
					tableConstraints = new ArrayList<String>();
					foreignKeyConstraints.put(tableName, tableConstraints);
				}
				tableConstraints.add(constraintName);
			}
		}
		//else if (dbType == DbType.MYSQL) {
		//}
		else {
			throw new RuntimeException("DB type not supported");
		}
		return foreignKeyConstraints;
	}
	
	public Map<String, List<String>> getIndices(String schema) {
		return getIndices(schema, null);
	}
	
	public Map<String, List<String>> getIndices(String schema, String table) {
		Map<String, List<String>> indices = new HashMap<String, List<String>>();
		if (dbType == DbType.MSSQL) {
			for (Row row : query("SELECT object_name(i.object_id) As tablename, i.name AS indexname FROM sys.indexes i LEFT OUTER JOIN sys.objects o ON i.object_id = o.object_id WHERE i.is_hypothetical = 0 AND i.index_id != 0 AND i.is_primary_key = 'false' AND schema_name(o.schema_id) = '" + schema.toLowerCase() + "'" + (table == null ? "" : " AND object_name(i.object_id) = '" + table.toUpperCase() + "'") + ";")) {
				String tableName = row.get("table_name", true);
				String indexName = row.get("constraint_name", true);
				List<String> tableIndices = indices.get(tableName);
				if ( tableIndices == null) {
					tableIndices = new ArrayList<String>();
					indices.put(tableName, tableIndices);
				}
				tableIndices.add(indexName);
			}
		}
		else if (dbType == DbType.POSTGRESQL) {
			for (Row row : query("SELECT tablename, indexname FROM pg_indexes WHERE indexdef NOT LIKE '% UNIQUE %' AND schemaname = '" + schema.toLowerCase() + "'" + (table == null ? "" : " AND tablename = '" + table.toLowerCase() + "'") + ";")) {
				String tableName = row.get("tablename", true);
				String indexName = row.get("indexname", true);
				List<String> tableIndices = indices.get(tableName);
				if ( tableIndices == null) {
					tableIndices = new ArrayList<String>();
					indices.put(tableName, tableIndices);
				}
				tableIndices.add(indexName);
			}
		}
		else if (dbType == DbType.ORACLE) {
			for (Row row : query("SELECT TABLE_NAME, INDEX_NAME FROM ALL_INDEXES WHERE UNIQUENESS = 'NONUNIQUE' AND OWNER = '" + schema.toUpperCase() + "'" + (table == null ? "" : " AND TABLE_NAME = '" + table.toUpperCase() + "'") + ";")) {
				String tableName = row.get("TABLE_NAME", true);
				String indexName = row.get("INDEX_NAME", true);
				List<String> tableIndices = indices.get(tableName);
				if ( tableIndices == null) {
					tableIndices = new ArrayList<String>();
					indices.put(tableName, tableIndices);
				}
				tableIndices.add(indexName);
			}
		}
		//else if (dbType == DbType.MYSQL) {
		//}
		else {
			throw new RuntimeException("DB type not supported");
		}
		return indices;
	}
	
	public Map<String, List<String>> getPrimaryKeyConstraints(String schema) {
		return getPrimaryKeyConstraints(schema, null);
	}
	
	public Map<String, List<String>> getPrimaryKeyConstraints(String schema, String table) {
		Map<String, List<String>> primaryKeyConstraints = new HashMap<String, List<String>>();
		if (dbType == DbType.MSSQL) {
			for (Row row : query("SELECT t.[name] AS table_name, isnull(c.[name], i.[name]) AS constraint_name FROM sys.objects t LEFT OUTER JOIN sys.indexes i ON t.object_id = i.object_id LEFT OUTER JOIN sys.key_constraints c ON i.object_id = c.parent_object_id AND i.index_id = c.unique_index_id WHERE is_unique = 1 AND t.[type] = 'U' AND c.[type] = 'PK' AND t.is_ms_shipped <> 1 AND schema_name(t.schema_id) = '" + schema.toLowerCase() + "'" + (table == null ? "" : " AND t.[name] = '" + table.toUpperCase() + "'") + ";")) {
				String tableName = row.get("table_name", true);
				String constraintName = row.get("constraint_name", true);
				List<String> tableConstraints = primaryKeyConstraints.get(tableName);
				if ( tableConstraints == null) {
					tableConstraints = new ArrayList<String>();
					primaryKeyConstraints.put(tableName, tableConstraints);
				}
				tableConstraints.add(constraintName);
			}
		}
		else if (dbType == DbType.POSTGRESQL) {
			for (Row row : query("SELECT rel.relname, con.conname FROM pg_catalog.pg_constraint con INNER JOIN pg_catalog.pg_class rel ON rel.oid = con.conrelid INNER JOIN pg_catalog.pg_namespace nsp ON nsp.oid = connamespace WHERE nsp.nspname = '" + schema.toLowerCase() + "' AND con.contype = 'p'" + (table == null ? "" : " AND rel.relname = '" + table.toLowerCase() + "'") + ";")) {
				String tableName = row.get("relname", true);
				String constraintName = row.get("conname", true);
				List<String> tableConstraints = primaryKeyConstraints.get(tableName);
				if ( tableConstraints == null) {
					tableConstraints = new ArrayList<String>();
					primaryKeyConstraints.put(tableName, tableConstraints);
				}
				tableConstraints.add(constraintName);
			}
		}
		else if (dbType == DbType.ORACLE) {
			for (Row row : query("SELECT TABLE_NAME, CONSTRAINT_NAME FROM ALL_CONSTRAINTS WHERE OWNER = '" + schema.toUpperCase() + "' AND CONSTRAINT_TYPE = 'P'" + (table == null ? "" : " AND TABLE_NAME = '" + table.toUpperCase() + "'") + ";")) {
				String tableName = row.get("TABLE_NAME", true);
				String constraintName = row.get("CONSTRAINT_NAME", true);
				List<String> tableConstraints = primaryKeyConstraints.get(tableName);
				if ( tableConstraints == null) {
					tableConstraints = new ArrayList<String>();
					primaryKeyConstraints.put(tableName, tableConstraints);
				}
				tableConstraints.add(constraintName);
			}
		}
		//else if (dbType == DbType.MYSQL) {
		//}
		else {
			throw new RuntimeException("DB type not supported");
		}
		return primaryKeyConstraints;
	}
	
	public void dropConstraintIfExists(String schema, String table, String constraint) {
		if (dbType == DbType.ORACLE) {
			try {
				Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				if (verbose) {
					System.out.println("Executing: ALTER TABLE " + table + "DROP CONSTRAINT " + constraint);
				}
				statement.execute("ALTER TABLE " + table + "DROP CONSTRAINT " + constraint);
				statement.close();
			} catch (Exception e) {
				if (verbose)
					System.out.println(e.getMessage());
			}
		} else if (dbType == DbType.MSSQL) {
			execute("IF OBJECT_ID('" + schema + "." + table + "') IS NOT NULL ALTER TABLE " + schema + "." + table + " DROP CONSTRAINT " + constraint + ";");
		} else if (dbType == DbType.POSTGRESQL) {
			// DO Nothing execute("ALTER TABLE " + schema + "." + table + " DROP CONSTRAINT IF EXISTS " + constraint + ";");
		}
		else {
			execute("ALTER TABLE " + table + "DROP CONSTRAINT " + constraint);
		}
	}
	
	public void dropIndexIfExists(String schema, String table, String index) {
		if (dbType == DbType.ORACLE) {
			/* Done with dropping tables
			try {
				Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				if (verbose) {
					System.out.println("Executing: ALTER TABLE " + table + "DROP CONSTRAINT " + constraint);
				}
				statement.execute("ALTER TABLE " + table + "DROP CONSTRAINT " + constraint);
				statement.close();
			} catch (Exception e) {
				if (verbose)
					System.out.println(e.getMessage());
			}
			*/
		} else if (dbType == DbType.MSSQL) {
			execute("DROP INDEX IF EXISTS index ON " + schema + "." + table + ";");
		} else if (dbType == DbType.POSTGRESQL) {
			// Do Nothing execute("DROP INDEX IF EXISTS " + schema + "." + index + " CASCADE;");
			// Done with dropping tables
		}
		else {
			execute("ALTER TABLE " + table + "DROP INDEX IF EXISTS " + index);
		}
	}

	public void dropTableIfExists(String schema,String table) {
		if (dbType == DbType.ORACLE) {
			try {
				Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				//if (verbose) {
				//	System.out.println("Executing: TRUNCATE TABLE " + schema.toUpperCase() + "." + table + " CASCADE");
				//}
				//statement.execute("BEGIN EXECUTE IMMEDIATE 'TRUNCATE TABLE " + schema.toUpperCase() + "." + table + " CASCADE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;");
				if (verbose) {
					System.out.println("Executing: BEGIN EXECUTE IMMEDIATE 'DROP TABLE " + schema.toUpperCase() + "." + table + "'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;");
				}
				statement.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE " + schema.toUpperCase() + "." + table + "'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;");
				statement.close();
			} catch (Exception e) {
				if (verbose)
					System.out.println(e.getMessage());
			}
		} else if (dbType == DbType.POSTGRESQL) {
			try {
				Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				//if (verbose) {
				//	System.out.println("Executing: TRUNCATE TABLE " + schema + "." + table + " CASCADE");
				//}
				//statement.execute("TRUNCATE TABLE " + schema + "." + table + " CASCADE");
				if (verbose) {
					System.out.println("Executing: DROP TABLE IF EXISTS " + schema + "." + table + " CASCADE");
				}
				statement.execute("DROP TABLE IF EXISTS " + schema + "." + table + " CASCADE");
				statement.close();
			} catch (Exception e) {
				if (verbose)
					System.out.println(e.getMessage());
			}
		} else if (dbType == DbType.MSSQL) {
			execute("IF OBJECT_ID('" + schema + "." + table + "') IS NOT NULL DROP TABLE " + schema + "." + table + ";");
		} else {
			execute("DROP TABLE " + table + " IF EXISTS");
		}
	}

	public void dropSchemaIfExists(String schema) {
		String query = null;
		if (dbType == DbType.ORACLE) {
			query = "ALTER SESSION SET \"_ORACLE_SCRIPT\"=true; DROP USER " + schema.toUpperCase() + " CASCADE;";
		}
		else if (dbType == DbType.POSTGRESQL) {
			query = "DROP SCHEMA IF EXISTS " + schema + " CASCADE;";
		}
		else if (dbType == DbType.MSSQL) { // CASCADE is not possible!!
			query = "DROP SCHEMA IF EXISTS " + schema + ";";
		}
		else {
			query = "DROP SCHEMA " + schema + ";";
		}
		if (query != null) {
			execute(query);
		}
	}

	public void createSchema(String schema) {
		if (dbType == DbType.ORACLE) {
			execute("ALTER SESSION SET \"_ORACLE_SCRIPT\"=true; CREATE USER " + schema.toUpperCase() + " IDENTIFIED BY \"" + password + "\";");
			execute("ALTER USER " + schema.toUpperCase() + " QUOTA UNLIMITED ON USERS");
		}
		else {
			execute("CREATE SCHEMA " + schema.toLowerCase() + ";");
		}
	}

	public void dropDatabaseIfExists(String database) {
		execute("DROP DATABASE " + database);
	}

	/**
	 * Returns the row count of the specified table.
	 * 
	 * @param tableName
	 * @return
	 */
	public long getTableSize(String schema, String tableName) {
		if (dbType == DbType.MSSQL)
			return Long.parseLong(query("SELECT COUNT(*) FROM " + schema + "." + tableName + ";").iterator().next().getCells().get(0));
		else
			return Long.parseLong(query("SELECT COUNT(*) FROM " + tableName + ";").iterator().next().getCells().get(0));
	}

	/**
	 * Close the connection to the database.
	 */
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	private String loadSQL(InputStream sqlStream) {
		StringBuilder sql = new StringBuilder();
		for (String line : new ReadTextFile(sqlStream)) {
			line = line.replaceAll("--.*", ""); // Remove comments
			line = line.replaceAll("#.*", ""); // Remove comments
			
			if (this.dbType == DbType.MSSQL) {
				line = line.replaceAll("ILIKE", "LIKE"); // Replace ILIKE with LIKE. Database definition should be case insensitive
			}
			
			line = line.trim();
			if (line.length() != 0) {
				sql.append(line.trim());
				sql.append('\n');
			}
		}
		return sql.toString();
	}

	public Class<?> getContext() {
		return context;
	}

	public void setContext(Class<?> context) {
		this.context = context;
	}

	public class QueryResult implements Iterable<Row> {
		private String				sql;

		private List<DBRowIterator>	iterators	= new ArrayList<DBRowIterator>();

		public QueryResult(String sql) {
			this.sql = sql;
		}

		@Override
		public Iterator<Row> iterator() {
			DBRowIterator iterator = new DBRowIterator(sql);
			iterators.add(iterator);
			return iterator;
		}

		public void close() {
			for (DBRowIterator iterator : iterators) {
				iterator.close();
			}
		}
	}

	/**
	 * Writes the results of a query to the specified file in CSV format.
	 * 
	 * @param queryResult
	 * @param filename
	 */
	public void writeToFile(QueryResult queryResult, String filename) {
		WriteCSVFileWithHeader out = new WriteCSVFileWithHeader(filename);
		for (Row row : queryResult)
			out.write(row);
		out.close();
	}

	/**
	 * Reads a table from a file in CSV format, and inserts it into the database.
	 * 
	 * @param filename
	 * @param table
	 * @param create
	 *            If true, the data format is determined based on the first batch of rows and used to create the table structure.
	 */
	public void readFromFile(String filename, String table, Map<String, String> columnTypes, boolean create, String nullValueString) {
		insertIntoTable(new ReadCSVFileWithHeader(filename).iterator(), table, columnTypes, create, nullValueString);
	}

	/**
	 * Inserts the rows into a table in the database.
	 * 
	 * @param iterator
	 * @param tableName
	 * @param create
	 *            If true, the data format is determined based on the first batch of rows and used to create the table structure.
	 */
	public void insertIntoTable(Iterator<Row> iterator, String table, Map<String, String> columnTypes, boolean create, String nullValueString) {
		List<Row> batch = new ArrayList<Row>(INSERT_BATCH_SIZE);

		boolean first = true;
		SimpleCounter counter = new SimpleCounter(1000000, true);
		while (iterator.hasNext()) {
			if (batch.size() == INSERT_BATCH_SIZE) {
				if (first && create)
					createTable(table, batch);
				insert(table, columnTypes, batch, nullValueString);
				batch.clear();
				first = false;
			}
			batch.add(iterator.next());
			counter.count();
		}
		if (batch.size() != 0) {
			if (first && create)
				createTable(table, batch);
			insert(table, columnTypes, batch, nullValueString);
		}
	}

	private void insert(String tableName, Map<String, String> columnTypes, List<Row> rows, String nullValueString) {
		List<String> columns = null;
		columns = rows.get(0).getFieldNames();
		for (int i = 0; i < columns.size(); i++)
			columns.set(i, columnNameToSqlName(columns.get(i)));

		String sql = "INSERT INTO " + tableName;
		sql = sql + " (" + StringUtilities.join(columns, ",") + ")";
		sql = sql + " VALUES (?";
		for (int i = 1; i < columns.size(); i++)
			sql = sql + ",?";
		sql = sql + ")";
		try {
			connection.setAutoCommit(false);
			PreparedStatement statement = connection.prepareStatement(sql);
			for (Row row : rows) {
				for (int i = 0; i < columns.size(); i++) {
					String value = row.get(columns.get(i), true);
					if (value != null && (nullValueString != null) && value.equals(nullValueString))
						value = null;
					if (dbType == DbType.POSTGRESQL) {// PostgreSQL does not allow unspecified types
						if (tableName.equals("note") && (value != null) && (value.contains("\\"))) {
							value = value.replace("\\\\", SUBSTITUTE).replace("\\n", "\n").replace("\\t", "\t").replace(SUBSTITUTE, "\\");
						}
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("DATE")))
							statement.setDate(i + 1, getSQLDate(value));
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("TIMESTAMP")))
							statement.setTimestamp(i + 1, getSQLTimeStamp(value));
						else
						statement.setObject(i + 1, value, Types.OTHER);
					}
					else if (dbType == DbType.ORACLE) {
						if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("DATE")))
							statement.setDate(i + 1, getSQLDate(value));
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).startsWith("TIMESTAMP(")))
							statement.setTimestamp(i + 1, getSQLTimeStamp(value));
						else
							statement.setString(i + 1, value);
					}
					else if (dbType == DbType.MSSQL) {
						if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("DATE")))
							statement.setDate(i + 1, getSQLDate(value));
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("DATETIME")))
							statement.setTimestamp(i + 1, getSQLTimeStamp(value));
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("DATETIME2")))
							statement.setTimestamp(i + 1, getSQLTimeStamp(value));
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("DATETIMEOFFSET")))
							statement.setTimestamp(i + 1, getSQLTimeStamp(value));
						else if ((columnTypes != null) && (columnTypes.get(columns.get(i).toUpperCase()).equals("SMALLDATETIME")))
							statement.setTimestamp(i + 1, getSQLTimeStamp(value));
						else
							statement.setString(i + 1, value);
					}
					else
						statement.setString(i + 1, value);
				}
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
			statement.close();
			connection.setAutoCommit(true);
			connection.clearWarnings();
		} catch (SQLException e) {
			e.printStackTrace();
			if (e instanceof BatchUpdateException) {
				System.err.println(((BatchUpdateException) e).getNextException().getMessage());
			}
			throw new RuntimeException(e);
		}
	}
	
	private java.sql.Timestamp getSQLTimeStamp(String timeStampValue) {
		if (timeStampValue != null) {
			String[] timeStampValueSplit = timeStampValue.split(" ");
			String dateString = "";
			String timeString = "";
			
			if (timeStampValueSplit.length > 0) {
				dateString = getSQLDateString(timeStampValueSplit[0].trim());
			}
			
			if (timeStampValueSplit.length > 1) {
				timeString = getSQLTimeString(timeStampValueSplit[1].trim());
			}
			
			if (dateString == null) {
				return null;
			}
			else if ((timeString == null) || (timeString.equals(""))) {
				timeString = "00:00:00";
			}
			return java.sql.Timestamp.valueOf(dateString + " " + timeString);
		}
		else {
			return null;
		}
	}
	
	private java.sql.Date getSQLDate(String dateValue) {
		// Return an SQL date or null
		String dateString = getSQLDateString(dateValue);
		java.sql.Date date = dateString == null ? null : java.sql.Date.valueOf(dateString);
		return date;
	}
	
	private String getSQLDateString(String dateValue) {
		String dateString = null;
		if (dateValue != null) {
			dateString = "";
			String dateSeparator = null;
			if (dateValue.contains("-") && (dateValue.length() == 10)) { // dd-mm-yyyy or yyyy-mm-dd
				dateSeparator = "-";
			}
			else if (dateValue.contains("/") && (dateValue.length() == 10)) { // dd/mm/yyyy or yyyy/mm/dd
				dateSeparator = "/";
			}
			if (dateSeparator != null) {
				String[] dateValueSplit = dateValue.split(dateSeparator);
				if (dateValueSplit.length >= 3) {
					try {
						int datePart0 = Integer.parseInt(dateValueSplit[0]);
						int datePart1 = Integer.parseInt(dateValueSplit[1]);
						int datePart2 = Integer.parseInt(dateValueSplit[2]);
						
						if ((datePart0 > 31) && (datePart1 > 0) && (datePart1 < 13) && (datePart2 > 0) && (datePart2 <= 31)) {
							dateString = dateValueSplit[0] + "-" + dateValueSplit[1] + "-" + dateValueSplit[2];
						}
						else if ((datePart2 > 31) && (datePart1 > 0) && (datePart1 < 13) && (datePart0 > 0) && (datePart0 <= 31)) {
							dateString = dateValueSplit[2] + "-" + dateValueSplit[1] + "-" + dateValueSplit[0];
						}
						else {
							dateString = null;
						}
					} catch (NumberFormatException e) {
						dateString = null;
					}
				}
			}
			else if (dateValue.length() == 8) {
				try {
					String year  = dateValue.substring(0, 4);
					String month = dateValue.substring(4, 6);
					String day   = dateValue.substring(6, 8);
					
					int yearPart  = Integer.parseInt(year);
					int monthPart = Integer.parseInt(month);
					int dayPart   = Integer.parseInt(day);
					
					if ((yearPart  > 31) && (monthPart > 0) && (monthPart < 13) && (dayPart > 0) && (dayPart < 33)) {
						dateString = year + "-" + month + "-" + day;
					}
					else {
						year  = dateValue.substring(4, 8);
						month = dateValue.substring(2, 4);
						day   = dateValue.substring(0, 2);
						
						yearPart  = Integer.parseInt(year);
						monthPart = Integer.parseInt(month);
						dayPart   = Integer.parseInt(day);

						if ((yearPart  > 31) && (monthPart > 0) && (monthPart < 13) && (dayPart > 0) && (dayPart < 31)) {
							dateString = year + "-" + month + "-" + day;
						}
						else {
							dateString = null;
						}
					}
				} catch (NumberFormatException e) {
					dateString = null;
				} catch (IndexOutOfBoundsException e) {
					dateString = null;
				}
			}
		}
		else {
			dateString = null;
		}
		return dateString;
	}
	
	private java.sql.Time getSQLTime(String timeValue) {
		// Return an SQL date or null
		String timeString = getSQLTimeString(timeValue);
		java.sql.Time time = timeString == null ? null : java.sql.Time.valueOf(timeString);
		return time;
	}
	
	private String getSQLTimeString(String timeValue) {
		// Return an SQL date or null
		String timeString = null;
		if (timeValue != null) {
			if (timeValue.contains(":")) {
				String[] timeValueSplit = timeValue.split(":");
				String hourString    = "00";
				String minutesString = "00";
				String secondsString = "00";
				
				if (timeValueSplit.length > 0) {
					hourString = timeValueSplit[0].trim();
					if (hourString.equals("")) {
						hourString = "00";
					}
				}
				if (timeValueSplit.length > 1) {
					minutesString = timeValueSplit[1].trim();
					if (minutesString.equals("")) {
						minutesString = "00";
					}
				}
				if (timeValueSplit.length > 2) {
					secondsString = timeValueSplit[2].trim();
					if (secondsString.equals("")) {
						secondsString = "00";
					}
				}
				
				try {
					int hour    = Integer.parseInt(hourString);
					int minutes = Integer.parseInt(minutesString);
					int seconds = Integer.parseInt(secondsString);
					
					if ((hour >= 0) && (hour <= 24) && (minutes >= 0) && (minutes <= 59) && (seconds >= 0) && (seconds <= 59)) {
						timeString = hourString + ":" + minutesString + ":" + secondsString;
					}
					else {
						timeString = null;
					}
				} catch (NumberFormatException e) {
					timeString = null;
				}
			}
		}
		else {
			timeString = null;
		}
		return timeString;
	}
	
/*
	private static boolean isDate(String string) {
		if (string != null && string.length() == 10 && string.charAt(4) == '-' && string.charAt(7) == '-')
			try {
				int year = Integer.parseInt(string.substring(0, 4));
				if (year < 1000 || year > 2200)
					return false;
				int month = Integer.parseInt(string.substring(5, 7));
				if (month < 1 || month > 12)
					return false;
				int day = Integer.parseInt(string.substring(8, 10));
				if (day < 1 || day > 31)
					return false;
				return true;
			} catch (Exception e) {
				return false;
			}
		return false;
	}
*/
	
	private Set<String> createTable(String tableName, List<Row> rows) {
		Set<String> numericFields = new HashSet<String>();
		Row firstRow = rows.get(0);
		List<FieldInfo> fields = new ArrayList<FieldInfo>(rows.size());
		for (String field : firstRow.getFieldNames())
			fields.add(new FieldInfo(field));
		for (Row row : rows) {
			for (FieldInfo fieldInfo : fields) {
				String value = row.get(fieldInfo.name, true);
				if (fieldInfo.isNumeric && !StringUtilities.isInteger(value))
					fieldInfo.isNumeric = false;
				if (value.length() > fieldInfo.maxLength)
					fieldInfo.maxLength = value.length();
			}
		}

		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + tableName + " (\n");
		for (FieldInfo fieldInfo : fields) {
			sql.append("  " + fieldInfo.toString() + ",\n");
			if (fieldInfo.isNumeric)
				numericFields.add(fieldInfo.name);
		}
		sql.append(");");
		execute(sql.toString());
		return numericFields;
	}

	private String columnNameToSqlName(String name) {
		return name.replaceAll(" ", "_").replace("-", "_").replace(",", "_").replaceAll("_+", "_");
	}

	private class FieldInfo {
		public String	name;
		public boolean	isNumeric	= true;
		public int		maxLength	= 0;

		public FieldInfo(String name) {
			this.name = name;
		}

		public String toString() {
			if (dbType == DbType.MYSQL) {
				if (isNumeric)
					return columnNameToSqlName(name) + " int(" + maxLength + ")";
				else if (maxLength > 255)
					return columnNameToSqlName(name) + " text";
				else
					return columnNameToSqlName(name) + " varchar(255)";
			} else if (dbType == DbType.MSSQL) {
				if (isNumeric) {
					if (maxLength < 10)
						return columnNameToSqlName(name) + " int";
					else
						return columnNameToSqlName(name) + " bigint";
				} else if (maxLength > 255)
					return columnNameToSqlName(name) + " varchar(max)";
				else
					return columnNameToSqlName(name) + " varchar(255)";
			} else
				throw new RuntimeException("Create table syntax not specified for type " + dbType);
		}
	}

	public void copyTable(String sourceDatabase, String sourceTable, RichConnection targetConnection, String targetDatabase, String targetTable) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE ");
		sql.append(targetDatabase);
		if (targetConnection.dbType == DbType.MSSQL)
			sql.append(".dbo.");
		else
			sql.append(".");
		sql.append(targetTable);
		sql.append("(");
		boolean first = true;
		String query;
		if (dbType == DbType.ORACLE || dbType == DbType.MSSQL)
			query = "SELECT COLUMN_NAME,DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_CATALOG='" + sourceDatabase + "' AND TABLE_NAME='" + sourceTable
					+ "';";
		else
			// mysql
			query = "SELECT COLUMN_NAME,DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + sourceDatabase + "' AND TABLE_NAME = '"
					+ sourceTable + "';";

		for (Row row : query(query)) {
			if (first)
				first = false;
			else
				sql.append(',');
			sql.append(row.get("COLUMN_NAME", true));
			sql.append(' ');
			if (targetConnection.dbType == DbType.ORACLE) {
				if (row.get("DATA_TYPE", true).equals("bigint"))
					sql.append("number");
				else if (row.get("DATA_TYPE", true).equals("varchar"))
					sql.append("VARCHAR(512)");
				else
					sql.append(row.get("DATA_TYPE", true));
			} else if (targetConnection.dbType == DbType.MYSQL) {
				sql.append(row.get("DATA_TYPE", true));
				if (row.get("DATA_TYPE", true).equals("varchar"))
					sql.append("(max)");
			} else if (targetConnection.dbType == DbType.POSTGRESQL) {
				sql.append(row.get("DATA_TYPE", true));
			}
		}
		sql.append(");");
		targetConnection.execute(sql.toString());
		targetConnection.use(targetDatabase);
		//TODO column types
		targetConnection.insertIntoTable(query("SELECT * FROM " + sourceDatabase + ".dbo." + sourceTable).iterator(), targetTable, null, false, null);
	}

	private class DBRowIterator implements Iterator<Row> {

		private ResultSet	resultSet;

		private boolean		hasNext;

		private Set<String>	columnNames	= new HashSet<String>();

		public DBRowIterator(String sql) {
			try {
				sql.trim();
				if (sql.endsWith(";"))
					sql = sql.substring(0, sql.length() - 1);
				if (verbose) {
					String abbrSQL = sql.replace('\n', ' ').replace('\t', ' ').trim();
					if (abbrSQL.length() > 100)
						abbrSQL = abbrSQL.substring(0, 100).trim() + "...";
					System.out.println("Executing query: " + abbrSQL);
				}
				long start = System.currentTimeMillis();
				Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				resultSet = statement.executeQuery(sql.toString());
				hasNext = resultSet.next();
				if (verbose)
					outputQueryStats(statement, System.currentTimeMillis() - start);
			} catch (SQLException e) {
				System.err.println(sql.toString());
				System.err.println(e.getMessage());
				throw new RuntimeException(e);
			}
		}

		public void close() {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				resultSet = null;
				hasNext = false;
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Row next() {
			try {
				Row row = new Row();
				ResultSetMetaData metaData;
				metaData = resultSet.getMetaData();
				columnNames.clear();

				for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
					String columnName = metaData.getColumnName(i);
					if (columnNames.add(columnName)) {
						String value = resultSet.getString(i);
						if (value == null)
							value = "";

						row.add(columnName, value.replace(" 00:00:00", ""));
					}
				}
				hasNext = resultSet.next();
				if (!hasNext) {
					resultSet.close();
					resultSet = null;
				}
				return row;
			} catch (SQLException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove() {
		}
	}
}
