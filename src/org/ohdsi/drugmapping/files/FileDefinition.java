package org.ohdsi.drugmapping.files;

import java.util.ArrayList;
import java.util.List;

public class FileDefinition {
	
	private String fileName;
	private List<String> description = new ArrayList<String>();
	private FileColumnDefinition[] columns;
	private String defaultFile = "";
	private String defaultFieldDelimiter = null;
	private String defaultTextQualifier = null;
	private boolean required = true;
	private boolean usedInInterface = true;

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns) {
		initialize(fileName, description, columns, null, null, null, true, true);
	}

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns, String defaultFile, String defaultFieldDelimiter, String defaultTextQualifier) {
		initialize(fileName, description, columns, defaultFile, defaultFieldDelimiter, defaultTextQualifier, true, true);
	}

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns, boolean required, boolean usedInInterface) {
		initialize(fileName, description, columns, null, null, null, required, usedInInterface);
	}

	
	public FileDefinition(String fileName, String[] description, FileColumnDefinition[] columns, String defaultFile, String defaultFieldDelimiter, String defaultTextQualifier, boolean required, boolean usedInInterface) {
		initialize(fileName, description, columns, defaultFile, defaultFieldDelimiter, defaultTextQualifier, required, usedInInterface);
	}
	
	
	private void initialize(String fileName, String[] description, FileColumnDefinition[] columns, String defaultFile, String defaultFieldDelimiter, String defaultTextQualifier, boolean required, boolean usedInInterface) {
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
		this.defaultFile = defaultFile;
		this.defaultFieldDelimiter = defaultFieldDelimiter;
		this.defaultTextQualifier = defaultTextQualifier;
		this.required = required;
		this.usedInInterface = usedInInterface;
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
	
	
	public String getDefaultFile() {
		return defaultFile;
	}
	
	
	public String getDefaultFieldDelimiter() {
		return defaultFieldDelimiter;
	}
	
	
	public String getDefaultTextQualifier() {
		return defaultTextQualifier;
	}
	
	
	public boolean isRequired() {
		return required;
	}
	
	public boolean isUsedInInterface() {
		return usedInInterface;
	}
}
