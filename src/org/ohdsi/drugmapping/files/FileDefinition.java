package org.ohdsi.drugmapping.files;

public class FileDefinition {
	
	private String fileName;
	private String[] description;
	private FileColumnDefinition[] columns;

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns) {
		this.fileName = fileName;
		this.description = description;
		this.columns = columns;
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	
	public String[] getDescription() {
		return description;
	}
	
	
	public FileColumnDefinition[] getColumns() {
		return columns;
	}
}
