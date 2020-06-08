package org.ohdsi.drugmapping.utilities;

public class DrugMappingStringUtilities {
	

	public static String removeExtraSpaces(String string) {
		String orgString;
		string = string.trim();
		do {
			orgString = string;
			string = orgString.replaceAll("  ", " ");
		} while (string.length() != orgString.length());
		return string;
	}
	
	
	public static String modifyName(String name) {
		
		name = " " + name.toUpperCase() + " ";
		
		name = name.replaceAll("-", " ");
		name = name.replaceAll(",", " ");
		name = name.replaceAll("/", " ");
		name = name.replaceAll("[(]", " ");
		name = name.replaceAll("[)]", " ");
		name = name.replaceAll("_", " ");
		name = name.replaceAll("^", " ");
		name = name.replaceAll("'", " ");
		name = name.replaceAll("\\]", " ");
		name = name.replaceAll("\\[", " ");

		// Prevent these seperate letters to be patched
		name = name.replaceAll(" A ", "_A_");
		name = name.replaceAll(" O ", "_O_");
		name = name.replaceAll(" E ", "_E_");
		name = name.replaceAll(" U ", "_U_");
		name = name.replaceAll(" P ", "_P_");
		name = name.replaceAll(" H ", "_H_");

		name = name.replaceAll("AAT", "ATE");
		name = name.replaceAll("OOT", "OTE");
		name = name.replaceAll("ZUUR", "ACID");
		name = name.replaceAll("AA", "A");
		name = name.replaceAll("OO", "O");
		name = name.replaceAll("EE", "E");
		name = name.replaceAll("UU", "U");
		name = name.replaceAll("TH", "T");
		name = name.replaceAll("AE", "A");
		name = name.replaceAll("EA", "A");
		name = name.replaceAll("PH", "F");
		name = name.replaceAll("S ", " ");
		name = name.replaceAll("E ", " ");
/* ADDED BEGIN 2020-06-08 */
		name = name.replaceAll("A ", " ");
		name = name.replaceAll(" ", "");
/* ADDED END 2020-06-08 */

		name = name.replaceAll("_", " ");

		name = name.replaceAll("AA", "A");
		name = name.replaceAll("OO", "O");
		name = name.replaceAll("EE", "E");
		name = name.replaceAll("UU", "U");
		name = name.replaceAll("TH", "T");
		name = name.replaceAll("AE", "A");
		name = name.replaceAll("EA", "A");
		name = name.replaceAll("PH", "F");
		
		return removeExtraSpaces(name).trim();
	}

}
