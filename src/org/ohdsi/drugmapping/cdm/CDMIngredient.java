package org.ohdsi.drugmapping.cdm;

import java.io.PrintWriter;

import org.ohdsi.utilities.files.Row;

public class CDMIngredient extends CDMConcept {
	
	
	public static String getHeader() {
		return CDMConcept.getHeader();
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}

	
	public CDMIngredient(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}

}
