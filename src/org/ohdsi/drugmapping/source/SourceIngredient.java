package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class SourceIngredient {
	
	private String ingredientCode = null;
	private String ingredientName = null;
	private String ingredientNameNoSpaces = null;
	private String ingredientNameEnglish = null;
	private String ingredientNameEnglishNoSpaces = null;
	private String casNumber = null;
	private Long count = 0L;
	private List<String> ingredientMatchingNames = null;
	
	private String matchString = "";
	private String matchingIngredient = null;
	
	
	public static String getHeader() {
		String header = "IngredientCode";
		header += "," + "IngredientName";
		header += "," + "IngredientNameEnglish";
		header += "," + "CASNumber";
		return header;
	}
	
	
	public static String getMatchHeader() {
		String header = "IngredientCode";
		header += "," + "IngredientName";
		header += "," + "IngredientNameEnglish";
		header += "," + "CASNumber";
		header += "," + "MatchString";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public SourceIngredient(String ingredientCode, String ingredientName, String ingredientNameEnglish, String casNumber) {
		if (ingredientCode != null) {
			ingredientCode = ingredientCode.trim();
		}
		ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName);
		ingredientNameEnglish = DrugMappingStringUtilities.removeExtraSpaces(ingredientNameEnglish);

		this.ingredientCode = ((ingredientCode == null) || ingredientCode.equals("")) ? null : ingredientCode;
		this.ingredientName = ingredientName.equals("") ? null : ingredientName;
		this.ingredientNameEnglish = ingredientNameEnglish.equals("") ? null : ingredientNameEnglish;
		this.casNumber = casNumber.equals("") ? null : casNumber;
		this.ingredientNameNoSpaces = this.ingredientName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
		this.ingredientNameEnglishNoSpaces = this.ingredientNameEnglish == null ? "" : this.ingredientNameEnglish.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");

		ingredientMatchingNames = generateMatchingNames();
	}

	
	private List<String> generateMatchingNames() {
		List<String> matchingNames = new ArrayList<String>();
		Set<String> uniqueNames = new HashSet<String>();

		String name = DrugMappingStringUtilities.removeExtraSpaces(ingredientName).toUpperCase();
		String englishName = DrugMappingStringUtilities.removeExtraSpaces(ingredientNameEnglish).toUpperCase();
		
		if (uniqueNames.add(name)) {
			matchingNames.add("IngredientName: " + name);
		}
		if (uniqueNames.add(englishName)) {
			matchingNames.add("IngredientName Translated: " + englishName);
		}
		for (Integer length = 20; length > 0; length--) {
			String reducedName = getReducedName(name, length);
			if (reducedName != null) {
				if (uniqueNames.add(reducedName)) {
					matchingNames.add("IngredientName First " + length + " words: " + reducedName);
				}
				if (uniqueNames.add(reducedName + " EXTRACT")) {
					matchingNames.add("IngredientName First " + length + " words + EXTRACT: " + reducedName + " EXTRACT");
				}
			}
			reducedName = getReducedName(englishName, length);
			if (reducedName != null) {
				if (uniqueNames.add(reducedName)) {
					matchingNames.add("IngredientName Translated First " + length + " words: " + reducedName);
				}
				if (uniqueNames.add(reducedName + " EXTRACT")) {
					matchingNames.add("IngredientName Translated First " + length + " words + EXTRACT: " + reducedName + " EXTRACT");
				}
			}
		}

		return matchingNames;
	}
	
	
	private String getReducedName(String name, int nrWords) {
		name += " ";
		String reducedName = null;
		if (nrWords > 0) {
			int delimiterCount = 0;
			boolean lastCharDelimiter = false;
			for (int charNr = 1; charNr < name.length(); charNr++) {
				if (" -[](),&+:;\"'/\\{}*%".contains(name.substring(charNr, charNr + 1))) {
					if (!lastCharDelimiter) {
						delimiterCount++;
					}
					lastCharDelimiter = true;
				}
				else {
					lastCharDelimiter = false;
				}
				if (delimiterCount == nrWords) {
					reducedName = name.substring(0, charNr);
					break;
				}
			}
		}
		
		return reducedName;
	}
	
	
	public String getIngredientCode() {
		return ingredientCode;
	}
	
	
	public String getIngredientName() {
		return ingredientName;
	}
	
	
	public String getIngredientNameNoSpaces() {
		return ingredientNameNoSpaces;
	}
	
	
	public String getIngredientNameEnglish() {
		return ingredientNameEnglish;
	}
	
	
	public List<String> getIngredientMatchingNames() {
		return ingredientMatchingNames;
	}
	
	
	public void setIngredientNameEnglish(String ingredientNameEnglish) {
		this.ingredientNameEnglish = ingredientNameEnglish;
		this.ingredientNameEnglishNoSpaces = ingredientNameEnglish.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
	}
	
	
	public String getIngredientNameEnglishNoSpaces() {
		return ingredientNameEnglishNoSpaces;
	}
	
	
	public String getCASNumber() {
		return casNumber;
	}
	
	
	public void setCASNumber(String casNumber) {
		this.casNumber = casNumber;
	}
	
	
	public String getMatchString() {
		return matchString;
	}
	
	
	public void setMatchString(String matchString) {
		this.matchString = matchString;
	}
	
	
	public String getMatchingIngredient() {
		return matchingIngredient;
	}
	
	
	public void setMatchingIngredient(String matchingIngredient) {
		this.matchingIngredient = matchingIngredient;
	}
	
	
	public Long getCount() {
		return count;
	}
	
	
	public void addCount(Long additionalCount) {
		count += additionalCount == null ? 0 : additionalCount;
	}
	
	
	public String toString() {
		String description = (ingredientCode == null ? "" : DrugMapping.escapeFieldValue(ingredientCode));
		description += "," + (ingredientName == null ? "" : DrugMapping.escapeFieldValue(ingredientName));
		description += "," + (ingredientNameEnglish == null ? "" : DrugMapping.escapeFieldValue(ingredientNameEnglish));
		description += "," + (casNumber == null ? "" : DrugMapping.escapeFieldValue(casNumber));
		return description;
	}
	
	
	public String toMatchString() {
		String description = (ingredientCode == null ? "" : DrugMapping.escapeFieldValue(ingredientCode));
		description += "," + (ingredientName == null ? "" : DrugMapping.escapeFieldValue(ingredientName));
		description += "," + (ingredientNameEnglish == null ? "" : DrugMapping.escapeFieldValue(ingredientNameEnglish));
		description += "," + (casNumber == null ? "" : DrugMapping.escapeFieldValue(casNumber));
		description += "," + DrugMapping.escapeFieldValue(matchString);
		return description;
	}
	
	
	public void writeDescriptionToFile(String prefix, PrintWriter file) {
		file.println(prefix + this);
	}
	
	
	public boolean matches(String ingredientName, String ingredientNameEnglish, String casNumber) {
		ingredientName = ingredientName.equals("") ? null : ingredientName;
		ingredientNameEnglish = ingredientNameEnglish.equals("") ? null : ingredientNameEnglish;
		
		boolean matches = (
					(((ingredientName == null) && (this.ingredientName == null)) || ((ingredientName != null) && ingredientName.equals(this.ingredientName))) &&
					(((ingredientNameEnglish == null) && (this.ingredientNameEnglish == null)) || ((ingredientNameEnglish != null) && ingredientNameEnglish.equals(this.ingredientNameEnglish))) &&
					(((casNumber == null) && (this.casNumber == null)) || ((casNumber != null) && casNumber.equals(this.casNumber)))
				);
		
		return matches;
	}

}
