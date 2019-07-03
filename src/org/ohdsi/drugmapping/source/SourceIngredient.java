package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;

public class SourceIngredient {
	public static final String NO_MATCH                            = "No Match";
	
	public static final String MATCH_SINGLE                        = "Single";
	public static final String MATCH_EXACT                         = "Exact";
	public static final String MATCH_SINGLE_SYNONYM                = "Single Synonym";
	public static final String MATCH_EXACT_SYNONYM                 = "Exact Synonym";
	
	public static final String MATCH_MAPSTO_SINGLE                 = "Maps To Single";
	public static final String MATCH_MAPSTO_EXACT                  = "Maps To Exact";
	public static final String MATCH_MAPSTO_SINGLE_SYNONYM         = "Maps To Single Synonym";
	public static final String MATCH_MAPSTO_EXACT_SYNONYM          = "Maps To Exact Synonym";
	
	public static final String MATCH_VARIANT_SINGLE                = "Variant Single";
	public static final String MATCH_VARIANT_EXACT                 = "Variant Exact";
	public static final String MATCH_VARIANT_SINGLE_SYNONYM        = "Variant Single Synonym";
	public static final String MATCH_VARIANT_EXACT_SYNONYM         = "Variant Exact Synonym";
	
	public static final String MATCH_VARIANT_MAPSTO_SINGLE         = "Variant Maps To Single";
	public static final String MATCH_VARIANT_MAPSTO_EXACT          = "Variant Maps To Exact";
	public static final String MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM = "Variant Maps To Single Synonym";
	public static final String MATCH_VARIANT_MAPSTO_EXACT_SYNONYM  = "Variant Maps To Exact Synonym";
	
	private String ingredientName = null;
	private String ingredientNameEnglish = null;
	private String casNumber = null;
	
	private String match = NO_MATCH;
	private String matchString = "";
	private String matchingDrug = "";
	
	
	public static String getHeader() {
		String header = "IngredientName";
		header += "," + "IngredientNameEnglish";
		header += "," + "CASNumber";
		return header;
	}
	
	
	public static String getMatchHeader() {
		String header = "IngredientName";
		header += "," + "IngredientNameEnglish";
		header += "," + "CASNumber";
		header += "," + "Match";
		header += "," + "MatchString";
		header += "," + "MatchingDrug";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public SourceIngredient(String ingredientName, String ingredientNameEnglish, String casNumber) {
		while (ingredientNameEnglish.contains("  ")) {
			ingredientNameEnglish = ingredientNameEnglish.replaceAll("  ", " ");
		}
		this.ingredientName = ingredientName.equals("") ? null : ingredientName;
		this.ingredientNameEnglish = ingredientNameEnglish.equals("") ? null : ingredientNameEnglish;
		this.casNumber = casNumber.equals("") ? null : casNumber;
	}
	
	
	public String getIngredientName() {
		return ingredientName;
	}
	
	
	public String getIngredientNameEnglish() {
		return ingredientNameEnglish;
	}
	
	
	public String getCASNumber() {
		return casNumber;
	}
	
	
	public String getMatch() {
		return match;
	}
	
	
	public void setMatch(String match) {
		this.match = match;
	}
	
	
	public String getMatchString() {
		return matchString;
	}
	
	
	public void setMatchString(String matchString) {
		this.matchString = matchString;
	}
	
	
	public String getMatchingDrug() {
		return matchingDrug;
	}
	
	
	public void setMatchingDrug(String matchingDrug) {
		this.matchingDrug = matchingDrug;
	}
	
	
	public String toString() {
		String description = (ingredientName == null ? "" : "\"" + ingredientName + "\"");
		description += "," + (ingredientNameEnglish == null ? "" : "\"" + ingredientNameEnglish + "\"");
		description += "," + (casNumber == null ? "" : "\"" + casNumber + "\"");
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
