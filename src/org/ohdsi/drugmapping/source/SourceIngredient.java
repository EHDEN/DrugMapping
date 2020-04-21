package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.drugmapping.genericmapping.GenericMapping;

public class SourceIngredient {
	
	private String ingredientCode = null;
	private String ingredientName = null;
	private String ingredientNameNoSpaces = null;
	private String ingredientNameEnglish = null;
	private String ingredientNameEnglishNoSpaces = null;
	private String casNumber = null;
	private Long count = 0L;
	private List<String> ingredientMatchingNames = new ArrayList<String>();
	
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
		ingredientName = ingredientName.trim();
		while (ingredientName.contains("  ")) {
			ingredientName = ingredientName.replaceAll("  ", " ");
		}
		
		ingredientNameEnglish = ingredientNameEnglish.trim();
		while (ingredientNameEnglish.contains("  ")) {
			ingredientNameEnglish = ingredientNameEnglish.replaceAll("  ", " ");
		}

		this.ingredientCode = ((ingredientCode == null) || ingredientCode.equals("")) ? null : ingredientCode;
		this.ingredientName = ingredientName.equals("") ? null : ingredientName;
		this.ingredientNameEnglish = ingredientNameEnglish.equals("") ? null : ingredientNameEnglish;
		this.casNumber = casNumber.equals("") ? null : casNumber;
		this.ingredientNameNoSpaces = this.ingredientName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
		this.ingredientNameEnglishNoSpaces = this.ingredientNameEnglish == null ? "" : this.ingredientNameEnglish.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
		
		Map<Integer, List<String>> matchNameMap = new HashMap<Integer, List<String>>();
		int maxNameLength = 0;
		if (!ingredientName.equals("")) {
			String[] ingredientNameSplit = ingredientName.toUpperCase().split(" ");
			for (int partNr = ingredientNameSplit.length - 1; partNr >= 0; partNr--) {
				String matchName = "";
				for (int matchNamePartNr = 0; matchNamePartNr <= partNr; matchNamePartNr++) {
					matchName += (matchNamePartNr > 0 ? " " : "") + ingredientNameSplit[matchNamePartNr];
				}
				maxNameLength = Math.max(maxNameLength, partNr + 1);
				List<String> currentLengthList = matchNameMap.get(partNr + 1);
				if (currentLengthList == null) {
					currentLengthList = new ArrayList<String>();
					matchNameMap.put(partNr + 1, currentLengthList);
				}
				currentLengthList.add("IngredientName: " + matchName);
			}
		}
		
		if (!ingredientNameEnglish.equals("")) {
			String[] ingredientNameEnglishSplit = ingredientNameEnglish.toUpperCase().split(" ");
			for (int partNr = ingredientNameEnglishSplit.length - 1; partNr >= 0; partNr--) {
				String matchName = "";
				for (int matchNamePartNr = 0; matchNamePartNr <= partNr; matchNamePartNr++) {
					matchName += (matchNamePartNr > 0 ? " " : "") + ingredientNameEnglishSplit[matchNamePartNr];
				}
				maxNameLength = Math.max(maxNameLength, partNr + 1);
				List<String> currentLengthList = matchNameMap.get(partNr + 1);
				if (currentLengthList == null) {
					currentLengthList = new ArrayList<String>();
					matchNameMap.put(partNr + 1, currentLengthList);
				}
				currentLengthList.add("IngredientNameEnglish: " + matchName);
			}
		}
		
		for (int nameLength = maxNameLength; nameLength >= 1; nameLength--) {
			List<String> nameLengthList = matchNameMap.get(nameLength);
			if (nameLengthList != null) {
				Set<String> matchNames = new HashSet<String>();
				for (String matchName : nameLengthList) {
					String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
					matchName = matchName.substring(matchName.indexOf(": ") + 2);
					if (matchNames.add(matchName)) {
						ingredientMatchingNames.add(matchType + matchName);
					}
				}
				for (String matchName : nameLengthList) {
					String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
					matchName = matchName.substring(matchName.indexOf(": ") + 2);
					matchName = GenericMapping.modifyName(matchName);
					if (matchNames.add(matchName)) {
						ingredientMatchingNames.add(matchType + matchName);
					}
				}
				for (String matchName : nameLengthList) {
					String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
					matchName = matchName.substring(matchName.indexOf(": ") + 2);
					matchName = GenericMapping.modifyName(matchName);
					if (matchNames.add(matchName)) {
						ingredientMatchingNames.add(matchType + matchName);
					}
				}
				/*
				for (String matchName : nameLengthList) {
					String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
					matchName = matchName.substring(matchName.indexOf(": ") + 2);
					matchName = matchName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
					if (matchNames.add(matchName)) {
						ingredientMatchingNames.add(matchType + matchName);
					}
				}
				for (String matchName : nameLengthList) {
					String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
					matchName = matchName.substring(matchName.indexOf(": ") + 2);
					matchName = GenericMapping.modifyName(matchName).replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
					if (matchNames.add(matchName)) {
						ingredientMatchingNames.add(matchType + matchName);
					}
				}
				*/
			}
		}
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
		String description = (ingredientCode == null ? "" : "\"" + ingredientCode + "\"");
		description += "," + (ingredientName == null ? "" : "\"" + ingredientName + "\"");
		description += "," + (ingredientNameEnglish == null ? "" : "\"" + ingredientNameEnglish + "\"");
		description += "," + (casNumber == null ? "" : "\"" + casNumber + "\"");
		return description;
	}
	
	
	public String toMatchString() {
		String description = (ingredientCode == null ? "" : "\"" + ingredientCode + "\"");
		description += "," + (ingredientName == null ? "" : "\"" + ingredientName + "\"");
		description += "," + (ingredientNameEnglish == null ? "" : "\"" + ingredientNameEnglish + "\"");
		description += "," + (casNumber == null ? "" : "\"" + casNumber + "\"");
		description += "," + "\"" + matchString + "\"";
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
