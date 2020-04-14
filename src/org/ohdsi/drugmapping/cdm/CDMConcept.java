package org.ohdsi.drugmapping.cdm;

import java.io.PrintWriter;

import org.ohdsi.utilities.files.Row;

public class CDMConcept {
	protected String concept_id       = null;
	protected String concept_name     = null;
	protected String domain_id        = null;
	protected String vocabulary_id    = null;
	protected String concept_class_id = null;
	protected String standard_concept = null;
	protected String concept_code     = null;
	protected String valid_start_date = null;
	protected String valid_end_date   = null;
	protected String invalid_reason   = null;
	
	protected String conceptNameNoSpaces = null;
	
	
	public static String getHeader() {
		return getHeader("");
	}
	
	public static String getHeader(String prefix) {
		String header = prefix + "concept_id";
		header += "," + prefix + "concept_name";
		header += "," + prefix + "domain_id";
		header += "," + prefix + "vocabulary_id";
		header += "," + prefix + "concept_class_id";
		header += "," + prefix + "standard_concept";
		header += "," + prefix + "concept_code";
		header += "," + prefix + "valid_start_date";
		header += "," + prefix + "valid_end_date";
		header += "," + prefix + "invalid_reason";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public static String emptyRecord() {
		String[] headerSplit = getHeader().split(",");
		String emptyRecord = "";
		for (int commaNr = 0; commaNr < (headerSplit.length - 1); commaNr++) {
			emptyRecord += ",";
		}
		return emptyRecord;
	}
	
	
	
	public CDMConcept(Row queryRow, String prefix) {
		concept_id       = queryRow.get(prefix + "concept_id", true).trim();
		concept_name     = queryRow.get(prefix + "concept_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
		domain_id        = queryRow.get(prefix + "domain_id", true).trim();
		vocabulary_id    = queryRow.get(prefix + "vocabulary_id", true).trim();
		concept_class_id = queryRow.get(prefix + "concept_class_id", true).trim();
		standard_concept = queryRow.get(prefix + "standard_concept", true).trim();
		concept_code     = queryRow.get(prefix + "concept_code", true).trim().toUpperCase();
		valid_start_date = queryRow.get(prefix + "valid_start_date", true).trim();
		valid_end_date   = queryRow.get(prefix + "valid_end_date", true).trim();
		invalid_reason   = queryRow.get(prefix + "invalid_reason", true).trim();
		
		while (concept_name.contains("  ")) concept_name = concept_name.replaceAll("  ", " ");
		
		conceptNameNoSpaces = concept_name.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
	}
	
	
	
	public CDMConcept(String conceptId, String conceptName, String domainId, String vocabularyId, String conceptClassId, String standardConcept, String conceptCode, String validStartDate, String validEndDate, String invalidReason) {
		concept_id       = conceptId;
		concept_name     = conceptName;
		domain_id        = domainId;
		vocabulary_id    = vocabularyId;
		concept_class_id = conceptClassId;
		standard_concept = standardConcept;
		concept_code     = conceptCode;
		valid_start_date = validStartDate;
		valid_end_date   = validEndDate;
		invalid_reason   = invalidReason;
		
		while (concept_name.contains("  ")) concept_name = concept_name.replaceAll("  ", " ");
		
		conceptNameNoSpaces = concept_name.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
	}

	
	public String getConceptId() {
		return concept_id;
	}

	
	public String getConceptName() {
		return concept_name;
	}

	
	public String getConceptNameNoSpaces() {
		return conceptNameNoSpaces;
	}

	
	public String getDomainId() {
		return domain_id;
	}

	
	public String getVocabularyId() {
		return vocabulary_id;
	}

	
	public String getConceptClassId() {
		return concept_class_id;
	}

	
	public String getStandardConcept() {
		return standard_concept;
	}

	
	public String getConceptCode() {
		return concept_code;
	}

	
	public String getValidStartDate() {
		return valid_start_date;
	}
	
	
	public String toString() {
		
		String description = (concept_id == null ? "null" : concept_id);
		description += "," + "\"" + (concept_name == null ? "null" : concept_name) + "\"";
		description += "," + (domain_id == null ? "null" : domain_id);
		description += "," + (vocabulary_id == null ? "null" : vocabulary_id);
		description += "," + (concept_class_id == null ? "null" : concept_class_id);
		description += "," + (standard_concept == null ? "null" : standard_concept);
		description += "," + "\"" + (concept_code == null ? "null" : concept_code) + "\"";
		description += "," + (valid_start_date == null ? "null" : valid_start_date);
		description += "," + (valid_end_date == null ? "null" : valid_end_date);
		description += "," + (invalid_reason == null ? "null" : invalid_reason);
		
		return description;
	}
}
