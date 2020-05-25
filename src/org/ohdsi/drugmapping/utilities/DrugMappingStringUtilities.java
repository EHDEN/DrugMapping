package org.ohdsi.drugmapping.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Map<String, String> patternReplacement = new HashMap<String, String>();
		
		name = " " + name.toUpperCase() + " ";
		
		List<String> patterns = new ArrayList<String>();
		patterns.add("-");
		patterns.add(",");
		patterns.add("/");
		patterns.add("[(]");
		patterns.add("[)]");
		patterns.add("_");
		patterns.add("^");
		patterns.add("'");
		patterns.add("\\]");
		patterns.add("\\[");

		// Prevent these seperate letters to be patched
		patterns.add(" A ");
		patterns.add(" O ");
		patterns.add(" E ");
		patterns.add(" U ");
		patterns.add(" P ");
		patterns.add(" H ");

		patterns.add("AAT");
		patterns.add("OOT");
		patterns.add("ZUUR");
		patterns.add("AA");
		patterns.add("OO");
		patterns.add("EE");
		patterns.add("UU");
		patterns.add("TH");
		patterns.add("AE");
		patterns.add("EA");
		patterns.add("PH");
		patterns.add("S ");
		patterns.add("E ");
		//patterns.add(" ");

		patterns.add("_");

		patterns.add("AA");
		patterns.add("OO");
		patterns.add("EE");
		patterns.add("UU");
		patterns.add("TH");
		patterns.add("AE");
		patterns.add("EA");
		patterns.add("PH");
		
		patternReplacement.put("-", " ");
		patternReplacement.put(",", " ");
		patternReplacement.put("/", " ");
		patternReplacement.put("[(]", " ");
		patternReplacement.put("[)]", " ");
		patternReplacement.put("_", " ");
		patternReplacement.put("^", " ");
		patternReplacement.put("'", " ");
		patternReplacement.put("\\]", " ");
		patternReplacement.put("\\[", " ");

		// Prevent these seperate letters to be patched
		patternReplacement.put(" A ", "_A_");
		patternReplacement.put(" O ", "_O_");
		patternReplacement.put(" E ", "_E_");
		patternReplacement.put(" U ", "_U_");
		patternReplacement.put(" P ", "_P_");
		patternReplacement.put(" H ", "_H_");

		patternReplacement.put("AAT", "ATE");
		patternReplacement.put("OOT", "OTE");
		patternReplacement.put("ZUUR", "ACID");
		patternReplacement.put("AA", "A");
		patternReplacement.put("OO", "O");
		patternReplacement.put("EE", "E");
		patternReplacement.put("UU", "U");
		patternReplacement.put("TH", "T");
		patternReplacement.put("AE", "A");
		patternReplacement.put("EA", "A");
		patternReplacement.put("PH", "F");
		patternReplacement.put("S ", " ");
		patternReplacement.put("E ", " ");
		//patternReplacement.put(" ", "");

		patternReplacement.put("_", " ");

		patternReplacement.put("AA", "A");
		patternReplacement.put("OO", "O");
		patternReplacement.put("EE", "E");
		patternReplacement.put("UU", "U");
		patternReplacement.put("TH", "T");
		patternReplacement.put("AE", "A");
		patternReplacement.put("EA", "A");
		patternReplacement.put("PH", "F");
		
		for (String pattern : patterns) {
			if (pattern.substring(0, 1).equals("^")) {
				if (pattern.substring(pattern.length() - 1).equals("$")) {
					if (name.equals(pattern.substring(1, pattern.length() - 1))) {
						name = patternReplacement.get(pattern);
					}
				}
				else {
					if ((name.length() >= pattern.length() - 1) && name.substring(0, pattern.length() - 1).equals(pattern.substring(1))) {
						name = patternReplacement.get(pattern) + name.substring(pattern.length() - 1);
					}
				}
			}
			else if (pattern.substring(pattern.length() - 1).equals("$")) {
				if ((name.length() >= pattern.length() - 1) && name.substring(name.length() - pattern.length() + 1).equals(pattern.substring(0, pattern.length() - 1))) {
					name = name.substring(0, name.length() - pattern.length() + 1) + patternReplacement.get(pattern);
				}
			}
			else {
				name = name.replaceAll(pattern, patternReplacement.get(pattern));
			}
		}
		
		return name.trim();
	}

}
