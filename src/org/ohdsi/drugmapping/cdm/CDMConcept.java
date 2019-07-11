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
	protected String invalid_reason   = null;
	
	protected String conceptNameNoSpaces = null;
	
	
	public static String getHeader() {
		String header = "concept_id";
		header += "," + "concept_name";
		header += "," + "domain_id";
		header += "," + "vocabulary_id";
		header += "," + "concept_class_id";
		header += "," + "standard_concept";
		header += "," + "concept_code";
		header += "," + "invalid_reason";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	
	public CDMConcept(Row queryRow, String prefix) {
		concept_id       = queryRow.get(prefix + "concept_id").trim();
		concept_name     = queryRow.get(prefix + "concept_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
		domain_id        = queryRow.get(prefix + "domain_id").trim();
		vocabulary_id    = queryRow.get(prefix + "vocabulary_id").trim();
		concept_class_id = queryRow.get(prefix + "concept_class_id").trim();
		standard_concept = queryRow.get(prefix + "standard_concept").trim();
		concept_code     = queryRow.get(prefix + "concept_code").trim().toUpperCase();
		invalid_reason   = queryRow.get(prefix + "invalid_reason").trim();
		
		while (concept_name.contains("  ")) concept_name = concept_name.replaceAll("  ", " ");
		
		conceptNameNoSpaces = concept_name.replaceAll(" ", "").replaceAll("-", "");
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
	
	
	public String toString() {
		
		String description = (concept_id == null ? "null" : concept_id);
		description += "," + "\"" + (concept_name == null ? "null" : concept_name) + "\"";
		description += "," + (domain_id == null ? "null" : domain_id);
		description += "," + (vocabulary_id == null ? "null" : vocabulary_id);
		description += "," + (concept_class_id == null ? "null" : concept_class_id);
		description += "," + (standard_concept == null ? "null" : standard_concept);
		description += "," + "\"" + (concept_code == null ? "null" : concept_code) + "\"";
		
		return description;
	}
}
