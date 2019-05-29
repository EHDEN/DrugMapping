package org.ohdsi.drugmapping;

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
	
	
	
	public CDMConcept(Row queryRow, String prefix) {
		concept_id       = queryRow.get(prefix + "concept_id").trim();
		concept_name     = queryRow.get(prefix + "concept_name").trim().toUpperCase();
		domain_id        = queryRow.get(prefix + "domain_id").trim();
		vocabulary_id    = queryRow.get(prefix + "vocabulary_id").trim();
		concept_class_id = queryRow.get(prefix + "concept_class_id").trim();
		standard_concept = queryRow.get(prefix + "standard_concept").trim();
		concept_code     = queryRow.get(prefix + "concept_code").trim().toUpperCase();
	}

	
	public String getConceptId() {
		return concept_id;
	}

	
	public String getConceptName() {
		return concept_name;
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
		description += "," + (concept_name == null ? "null" : concept_name);
		description += "," + (domain_id == null ? "null" : domain_id);
		description += "," + (vocabulary_id == null ? "null" : vocabulary_id);
		description += "," + (concept_class_id == null ? "null" : concept_class_id);
		description += "," + (standard_concept == null ? "null" : standard_concept);
		description += "," + (concept_code == null ? "null" : concept_code);
		
		return description;
	}
}
