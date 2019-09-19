package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;

public class SourceIngredient {
	
	private String ingredientName = null;
	private String ingredientNameNoSpaces = null;
	private String ingredientNameEnglish = null;
	private String ingredientNameEnglishNoSpaces = null;
	private String casNumber = null;
	private Integer count = 0;
	
	private String matchString = "";
	private String matchingIngredient = null;
	
	
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
		header += "," + "MatchString";
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
		this.ingredientNameNoSpaces = this.ingredientName.replaceAll(" ", "").replaceAll("-", "");
		this.ingredientNameEnglishNoSpaces = this.ingredientNameEnglish.replaceAll(" ", "").replaceAll("-", "");
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
	
	
	public void setIngredientNameEnglish(String ingredientNameEnglish) {
		this.ingredientNameEnglish = ingredientNameEnglish;
		this.ingredientNameEnglishNoSpaces = ingredientNameEnglish.replaceAll(" ", "").replaceAll("-", "");
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
	
	
	public Integer getCount() {
		return count;
	}
	
	
	public void addCount(Integer additionalCount) {
		count += additionalCount;
	}
	
	
	public String toString() {
		String description = (ingredientName == null ? "" : "\"" + ingredientName + "\"");
		description += "," + (ingredientNameEnglish == null ? "" : "\"" + ingredientNameEnglish + "\"");
		description += "," + (casNumber == null ? "" : "\"" + casNumber + "\"");
		return description;
	}
	
	
	public String toMatchString() {
		String description = (ingredientName == null ? "" : "\"" + ingredientName + "\"");
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
