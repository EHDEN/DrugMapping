package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;

public class SourceIngredient {
	public static final String NO_MATCH                    = "No Match";
	
	public static final String MATCH_SINGLE                = "Single";
	public static final String MATCH_EXACT                 = "Exact";
	public static final String MATCH_SINGLE_SYNONYM        = "Single Synonym";
	public static final String MATCH_EXACT_SYNONYM         = "Exact Synonym";
	
	public static final String MATCH_MAPSTO_SINGLE         = "Maps To Single";
	public static final String MATCH_MAPSTO_EXACT          = "Maps To Exact";
	public static final String MATCH_MAPSTO_SINGLE_SYNONYM = "Maps To Single Synonym";
	public static final String MATCH_MAPSTO_EXACT_SYNONYM  = "Maps To Exact Synonym";
	
	private String ingredientName = null;
	private String ingredientNameEnglish = null;
	private String casNumber = null;
	
	private String match = NO_MATCH;
	
	
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
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public SourceIngredient(String ingredientName, String ingredientNameEnglish, String casNumber) {
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
