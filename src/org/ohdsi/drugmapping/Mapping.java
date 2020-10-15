package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class Mapping {
	private static Map<String, String> replacements = new HashMap<String, String>();
	private static List<String> replacementOrder = new ArrayList<String>();
	
	
	public static void loadReplacements(InputFile replacementsFile) {
		if (!replacementsFile.getFileName().equals("")) {
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + " Loading replacements ...");
			if (replacementsFile.openFile()) {
				while (replacementsFile.hasNext()) {
					Row row = replacementsFile.next();
					String replace = DrugMappingStringUtilities.safeToUpperCase(replacementsFile.get(row, "Replace", true));
					String by = DrugMappingStringUtilities.safeToUpperCase(replacementsFile.get(row, "By", true));
					
					System.out.println("    Replace \"" + replace + "\" by \"" + by);
					replacements.put(replace, by);
					replacementOrder.add(replace);
				}
			}
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + " Finished");
		}
	}

	
	public String getReplacement(String orgString) {
		String replacement = DrugMappingStringUtilities.safeToUpperCase(orgString);
		for (String replace : replacementOrder) {
			String replaceBy = replacements.get(replace);
			if (replace.startsWith("^")) {
				if (replace.endsWith("$")) {
					replace = replace.substring(0, replace.length() - 1);
					if (replacement.equals(replace)) {
						replacement = replaceBy;
					}
				}
				else {
					replace = replace.substring(1);
					if ((replacement.length() >= replace.length()) && replacement.substring(0, replace.length()).equals(replace)) {
						replacement = replaceBy + replacement.substring(replace.length());
					}
				}
			}
			else if (replace.endsWith("$")) {
				replace = replace.substring(0, replace.length() - 1);
				if ((replacement.length() >= replace.length()) && replacement.substring(replacement.length() - replace.length()).equals(replace)) {
					replacement = replacement.substring(0, replacement.length() - replace.length()) + replaceBy;
				}
			}
			else {
				replacement = replacement.replaceAll(replace, replaceBy);
			}
		}
		return replacement;
	}
}
