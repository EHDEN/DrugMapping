/*******************************************************************************
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.databases;

public class DbType {
	public static DbType	MYSQL		= new DbType("MySQL");
	public static DbType	MSSQL		= new DbType("SQL Server");
	public static DbType	MSAZURE    	= new DbType("Azure");
	public static DbType	ORACLE		= new DbType("Oracle");
	public static DbType	POSTGRESQL	= new DbType("PostgreSQL");
	
	private static DbType[] allDbTypes = new DbType[] { MYSQL, MSSQL, ORACLE, POSTGRESQL };
	
	public static DbType getDbType(String name) {
		DbType dbType = null;
		
		for (DbType type : allDbTypes) {
			if (type.name.equals(name)) {
				dbType = type;
				break;
			}
		}
		return dbType;
	}

	private String	name;

	public DbType(String name) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}
