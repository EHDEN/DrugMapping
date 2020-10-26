package org.ohdsi.drugmapping.files;

import java.util.ArrayList;
import java.util.List;

public class FileDefinition {
	public static final int GENERAL_FILE   = 0;
	public static final int DELIMITED_FILE = 1;
	public static final int EXCEL_FILE     = 2;
	public static final int XML_FILE       = 3;
	
	
	public static String getFileTypeName(int fileType) {
		if      (fileType == GENERAL_FILE)   return "General File";
		else if (fileType == DELIMITED_FILE) return "Delimited File";
		else if (fileType == EXCEL_FILE)     return "Excel File";
		else if (fileType == XML_FILE)       return "XML File";
		else                                 return "Unkown";
	}
	
	private String fileName;
	private List<String> description = new ArrayList<String>();
	private int fileType = -1;
	private FileColumnDefinition[] columns;
	private String defaultFile = "";
	private String defaultFieldDelimiter = null;
	private String defaultTextQualifier = null;
	private boolean required = true;
	private boolean usedInInterface = true;

	
	public FileDefinition(String fileName, String[] description, int fileType) {
		initialize(fileName, description, fileType, null, null, null, null, true, true);
	}

	
	public FileDefinition(String fileName, String[] description, int fileType, boolean required, boolean usedInInterface) {
		initialize(fileName, description, fileType, null, null, null, null, required, usedInInterface);
	}

	
	public FileDefinition(String fileName, String[] description, int fileType, FileColumnDefinition[] columns) {
		initialize(fileName, description, fileType, columns, null, null, null, true, true);
	}

	
	public FileDefinition(String fileName, String[] description, int fileType, FileColumnDefinition[] columns, String defaultFile, String defaultFieldDelimiter, String defaultTextQualifier) {
		initialize(fileName, description, fileType, columns, defaultFile, defaultFieldDelimiter, defaultTextQualifier, true, true);
	}

	
	public FileDefinition(String fileName, String[] description, int fileType, FileColumnDefinition[] columns, boolean required, boolean usedInInterface) {
		initialize(fileName, description, fileType, columns, null, null, null, required, usedInInterface);
	}

	
	public FileDefinition(String fileName, String[] description, int fileType, FileColumnDefinition[] columns, String defaultFile, String defaultFieldDelimiter, String defaultTextQualifier, boolean required, boolean usedInInterface) {
		initialize(fileName, description, fileType, columns, defaultFile, defaultFieldDelimiter, defaultTextQualifier, required, usedInInterface);
	}
	
	
	private void initialize(String fileName, String[] description, int fileType, FileColumnDefinition[] columns, String defaultFile, String defaultFieldDelimiter, String defaultTextQualifier, boolean required, boolean usedInInterface) {
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
		this.fileType = fileType;
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
	
	
	public int getFileType() {
		return fileType;
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
