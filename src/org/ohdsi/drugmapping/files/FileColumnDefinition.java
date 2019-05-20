package org.ohdsi.drugmapping.files;

public class FileColumnDefinition {
	
	private String columnName;
	private String[] description;

	
	public FileColumnDefinition(String columnName, String[] description) {
		this.columnName = columnName;
		this.description = description;
	}
	
	
	public String getColumnName() {
		return columnName;
	}
	
	
	public String[] getDescription() {
		return description;
	}
}
