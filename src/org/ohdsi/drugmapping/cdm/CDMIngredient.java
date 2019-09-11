package org.ohdsi.drugmapping.cdm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohdsi.utilities.files.Row;

public class CDMIngredient extends CDMConcept {
	private Set<String> synonyms = new HashSet<String>();
	private Set<String> synonymsNoSpaces = new HashSet<String>();
	
	
	public static String getHeader() {
		return CDMConcept.getHeader();
	}
	
	
	public static String getHeaderWithSynonyms() {
		String header = CDMConcept.getHeader();
		header += "," + "Synonym";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}

	
	public CDMIngredient(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}

	
	public CDMIngredient(String conceptId, String conceptName, String domainId, String vocabularyId, String conceptClassId, String standardConcept, String conceptCode, String validStartDate, String validEndDate, String invalidReason) {
		super(conceptId, conceptName, domainId, vocabularyId, conceptClassId, standardConcept, conceptCode, validStartDate, validEndDate, invalidReason);
	}
	
	
	public Set<String> getSynonyms() {
		return synonyms;
	}
	
	
	public Set<String> getSynonymsNoSpaces() {
		return synonymsNoSpaces;
	}
	
	
	public void addSynonym(String synonym) {
		synonym = synonym.replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
		synonyms.add(synonym);
		synonymsNoSpaces.add(synonym.replaceAll(" ", "").replaceAll("-", ""));
	}
	
	
	public String toStringWithSynonyms() {
		String fullDescription = "";
		String description = super.toString();
		if (synonyms.size() > 0) {
			List<String> orderedSynonyms = new ArrayList<String>();
			orderedSynonyms.addAll(synonyms);
			Collections.sort(orderedSynonyms);
			for (String synonym : orderedSynonyms) {
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
			List<String> orderedSynonyms = new ArrayList<String>();
			orderedSynonyms.addAll(synonyms);
			Collections.sort(orderedSynonyms);
			for (String synonym : orderedSynonyms) {
				if (!description.equals("")) {
					description += ",";
				}
				description += "'" + synonym + "'";
			}
			description += "=";
		}
		description += super.toString().replaceAll("\"", "'");
		
		return description;
	}

}
