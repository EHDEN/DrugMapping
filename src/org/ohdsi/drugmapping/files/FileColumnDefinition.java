package org.ohdsi.drugmapping.files;

import java.util.ArrayList;
import java.util.List;

public class FileColumnDefinition {
	
	private String columnName;
	private List<String> description = new ArrayList<String>();
	private boolean required = true;

	
	public FileColumnDefinition(String columnName, String[] description) {
		initialize(columnName, description, true);
	}

	
	public FileColumnDefinition(String columnName, String[] description, boolean required) {
		initialize(columnName, description, required);
	}
	
	
	private void initialize(String columnName, String[] description, boolean required) {
		this.columnName = columnName;
		for (String line : description) {
			this.description.add(line);
		}
		if (required) {
			this.description.add("This column is required.");
		}
		else {
			this.description.add("This column is optional.");
		}
	}
	
	
	public String getColumnName() {
		return columnName;
	}
	
	
	public List<String> getDescription() {
		return description;
	}
	
	
	public boolean isRequired() {
		return required;
	}
}
