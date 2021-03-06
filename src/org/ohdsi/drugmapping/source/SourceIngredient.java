package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class SourceIngredient implements Comparable<SourceIngredient> {
	
	private String ingredientCode = null;
	private String ingredientName = null;
	private String ingredientNameNoSpaces = null;
	private String ingredientNameEnglish = "";
	private String casNumber = null;
	private Long count = -1L;
	private List<String> ingredientMatchingNames = null;
	
	private String matchString = "";
	private CDMIngredient matchingIngredient = null;
	private Set<SourceDrug> sourceDrugs = new HashSet<SourceDrug>();
	
	
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
	
	
	public static String getStarredRecord() {
		return "*,*,*,*";
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
		this.ingredientName = ingredientName;
		this.ingredientNameEnglish = ingredientNameEnglish;
		this.casNumber = casNumber.equals("") ? null : casNumber;
		this.ingredientNameNoSpaces = this.ingredientName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");

		ingredientMatchingNames = DrugMappingStringUtilities.generateMatchingNames(ingredientName, ingredientNameEnglish);
	}
	
	
	public int compareTo(SourceIngredient compareToIngredient) {
		if (ingredientCode == null) {
			if (compareToIngredient.getIngredientCode() == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			if (compareToIngredient.getIngredientCode() == null) {
				return 1;
			}
			else {
				return ingredientCode.compareTo(compareToIngredient.getIngredientCode());
			}
		}
	}
	
	
	public void addDrug(SourceDrug sourceDrug) {
		sourceDrugs.add(sourceDrug);
	}
	
	
	public Set<SourceDrug> getSourceDrugs() {
		return sourceDrugs;
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
		this.ingredientNameEnglish = ingredientNameEnglish == null ? "" : ingredientNameEnglish;
		ingredientMatchingNames = DrugMappingStringUtilities.generateMatchingNames(ingredientName, ingredientNameEnglish);
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
	
	
	public CDMIngredient getMatchingIngredient() {
		return matchingIngredient;
	}
	
	
	public void setMatchingIngredient(CDMIngredient matchingIngredient) {
		this.matchingIngredient = matchingIngredient;
	}
	
	
	public Long getCount() {
		return count;
	}
	
	
	public void addCount(Long additionalCount) {
		if (additionalCount > 0) {
			if (count < 0) {
				count = additionalCount;
			}
			else {
				count += additionalCount;
			}
		}
	}
	
	
	public String toString() {
		String description = (ingredientCode == null ? "" : DrugMappingStringUtilities.escapeFieldValue(ingredientCode));
		description += "," + (ingredientName == null ? "" : DrugMappingStringUtilities.escapeFieldValue(ingredientName));
		description += "," + (ingredientNameEnglish == null ? "" : DrugMappingStringUtilities.escapeFieldValue(ingredientNameEnglish));
		description += "," + (casNumber == null ? "" : DrugMappingStringUtilities.escapeFieldValue(casNumber));
		return description;
	}
	
	
	public String toMatchString() {
		String description = (ingredientCode == null ? "" : DrugMappingStringUtilities.escapeFieldValue(ingredientCode));
		description += "," + (ingredientName == null ? "" : DrugMappingStringUtilities.escapeFieldValue(ingredientName));
		description += "," + (ingredientNameEnglish == null ? "" : DrugMappingStringUtilities.escapeFieldValue(ingredientNameEnglish));
		description += "," + (casNumber == null ? "" : DrugMappingStringUtilities.escapeFieldValue(casNumber));
		description += "," + DrugMappingStringUtilities.escapeFieldValue(matchString);
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
