package org.ohdsi.drugmapping.files;

import java.util.ArrayList;
import java.util.List;

public class FileDefinition {
	
	private String fileName;
	private List<String> description = new ArrayList<String>();
	private FileColumnDefinition[] columns;
	private boolean required = true;

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns) {
		initialize(fileName, description, columns, true);
	}

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns, boolean required) {
		initialize(fileName, description, columns, required);
	}
	
	
	private void initialize(String fileName, String[] description, FileColumnDefinition[] columns, boolean required) {
		this.fileName = fileName;
		for (String line : description) {
			this.description.add(line);
		}
		if (required) {
			this.description.add("This file is required.");
		}
		else {
			this.description.add("This file is optional.");
		}
		this.columns = columns;
		this.required = required;
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	
	public List<String> getDescription() {
		return description;
	}
	
	
	public FileColumnDefinition[] getColumns() {
		return columns;
	}
	
	
	public boolean isRequired() {
		return required;
	}
}
