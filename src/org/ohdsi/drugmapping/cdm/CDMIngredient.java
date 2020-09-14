package org.ohdsi.drugmapping.cdm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohdsi.utilities.files.Row;

public class CDMIngredient extends CDMConcept {
	private String atc = null;
	
	private List<String> synonyms = new ArrayList<String>();
	private List<String> synonymsNoSpaces = new ArrayList<String>();
	private Set<CDMDrug> cdmDrugs = new HashSet<CDMDrug>();
	
	
	public static String getHeader() {
		return getHeader("");
	}
	
	
	public static String getHeader(String prefix) {
		String header = CDMConcept.getHeader(prefix);
		header += "," + prefix + "ATC";
		return header;
	}
	
	
	public static String getHeaderWithSynonyms() {
		String header = getHeader();
		header += "," + "Synonym";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public static String emptyRecord() {
		return CDMConcept.emptyRecord() + ",";
	}

	
	public CDMIngredient(CDM cdm, Row queryRow, String prefix) {
		super(cdm, queryRow, prefix);
	}

	
	public CDMIngredient(CDM cdm, String conceptId, String conceptName, String domainId, String vocabularyId, String conceptClassId, String standardConcept, String conceptCode, String validStartDate, String validEndDate, String invalidReason) {
		super(cdm, conceptId, conceptName, domainId, vocabularyId, conceptClassId, standardConcept, conceptCode, validStartDate, validEndDate, invalidReason);
	}
	
	
	public void setATC(String atc) {
		this.atc = atc;
	}
	
	
	public String getATC() {
		return atc;
	}
	
	
	public List<String> getSynonyms() {
		return synonyms;
	}
	
	
	public List<String> getSynonymsNoSpaces() {
		return synonymsNoSpaces;
	}
	
	
	public void addSynonym(String synonym) {
		synonym = synonym.replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
		if (!synonyms.contains(synonym)) {
			synonyms.add(synonym);
			Collections.sort(synonyms);
		}
		String synonymNoSpaces = synonym.replaceAll(" ", "").replaceAll("-", "");
		if (synonymsNoSpaces.contains(synonymNoSpaces)) {
			synonymsNoSpaces.add(synonymNoSpaces);
			Collections.sort(synonymsNoSpaces);
		}
	}
	
	
	public String toString() {
		return super.toString() + "," + (getATC() == null ? "" : getATC());
	}
	
	
	public String toStringWithSynonyms() {
		String fullDescription = "";
		String description = toString();
		if (synonyms.size() > 0) {
			for (String synonym : synonyms) {
				if (!fullDescription.equals("")) {
					fullDescription += "\r\n";
				}
				fullDescription += description;
				fullDescription += "," + "\"" + synonym + "\"";
			}
		}
		else {
			fullDescription = description;
			fullDescription += ",";
		}
		
		return fullDescription;
	}
	
	
	public String toStringWithSynonymsSingleField() {
		String description = "";
		if (synonyms.size() > 0) {
			for (String synonym : synonyms) {
				if (!description.equals("")) {
					description += ",";
				}
				description += "'" + synonym + "'";
			}
			description += "=";
		}
		description += toString().replaceAll("\"", "'");
		
		return description;
	}
	
	
	public void addDrug(CDMDrug cdmDrug) {
		cdmDrugs.add(cdmDrug);
	}
	
	
	public int drugUseCount() {
		return cdmDrugs.size();
	}
	
	
	public boolean isOrphan() {
		return drugUseCount() == 0;
	}

}
