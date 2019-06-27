package org.ohdsi.drugmapping.cdm;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import org.ohdsi.utilities.files.Row;

public class CDMIngredient extends CDMConcept {
	private Set<String> synonyms = new HashSet<String>();
	
	
	public static String getHeader() {
		return CDMConcept.getHeader();
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}

	
	public CDMIngredient(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}
	
	
	public Set<String> getSynonyms() {
		return synonyms;
	}
	
	
	public void addSynonym(String synonym) {
		synonyms.add(synonym);
	}

}
