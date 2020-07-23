package org.ohdsi.drugmapping.cdm;

import java.io.PrintWriter;
import java.util.List;

import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class CDMConcept {
	protected CDM cdm = null;
	
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
	protected String additional_info     = null;
	
	
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
	
	
	public CDMConcept() {}
		
	
	public CDMConcept(CDM cdm, Row queryRow, String prefix) {
		this.cdm = cdm;
		
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
		
		concept_name = DrugMappingStringUtilities.removeExtraSpaces(concept_name);
		if (vocabulary_id.equals("None"))         vocabulary_id    = "";
		if (concept_class_id.equals("Undefined")) concept_class_id = "";
		
		conceptNameNoSpaces = concept_name.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
	}
	
	
	
	public CDMConcept(CDM cdm, String conceptId, String conceptName, String domainId, String vocabularyId, String conceptClassId, String standardConcept, String conceptCode, String validStartDate, String validEndDate, String invalidReason) {
		this.cdm = cdm;
		
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
	
	
	public CDMConcept(String description) {
		// For review only
		
		try {
			List<String> descriptionSplit = DrugMappingStringUtilities.intelligentSplit(description, ',', '"');
			
			if (descriptionSplit.size() > 9) {
				concept_id       = descriptionSplit.get(0);
				concept_name     = descriptionSplit.get(1);
				domain_id        = descriptionSplit.get(2);
				vocabulary_id    = descriptionSplit.get(3);
				concept_class_id = descriptionSplit.get(4);
				standard_concept = descriptionSplit.get(5);
				concept_code     = descriptionSplit.get(6);
				valid_start_date = descriptionSplit.get(7);
				valid_end_date   = descriptionSplit.get(8);
				List<String> descriptionSplit9Split = DrugMappingStringUtilities.intelligentSplit(descriptionSplit.get(9), ':', '\0');
				invalid_reason   = descriptionSplit9Split.get(0);
				additional_info  = ""; 
				if (!descriptionSplit9Split.get(0).equals("")) {
					descriptionSplit9Split.set(0, descriptionSplit9Split.get(0).substring(1)); // Remove space at begin
				}
				for (int segmentNr = 1; segmentNr < descriptionSplit9Split.size(); segmentNr++) {
					additional_info += (additional_info.equals("") ? "" : ":") + descriptionSplit9Split.get(segmentNr);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	
	public String getValidEndDate() {
		return valid_end_date;
	}

	
	public String getInvalidReason() {
		return invalid_reason;
	}
	
	
	public void setAdditionalInfo(String additionalInfo) {
		additional_info = additionalInfo;
	}
	
	
	public String getAdditionalInfo() {
		return additional_info;
	}
	
	
	public String toString() {
		
		String description = "";
		if (concept_id != null) {
			description = concept_id;
			description += "," + DrugMappingStringUtilities.escapeFieldValue(concept_name == null ? "null" : concept_name);
			description += "," + (domain_id == null ? "null" : domain_id);
			description += "," + (vocabulary_id == null ? "null" : vocabulary_id);
			description += "," + (concept_class_id == null ? "null" : concept_class_id);
			description += "," + (standard_concept == null ? "null" : standard_concept);
			description += "," + DrugMappingStringUtilities.escapeFieldValue(concept_code == null ? "null" : concept_code);
			description += "," + (valid_start_date == null ? "null" : valid_start_date);
			description += "," + (valid_end_date == null ? "null" : valid_end_date);
			description += "," + (invalid_reason == null ? "null" : invalid_reason);
		}
		else {
			description = additional_info;
		}
		
		return description;
	}
}
