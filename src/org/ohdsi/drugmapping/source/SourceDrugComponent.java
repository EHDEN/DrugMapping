package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;

public class SourceDrugComponent {
	private SourceIngredient ingredient = null;
	private Double dosage = null;
	private String dosageUnit = null;
	private Double numeratorDosage = null;
	private String numeratorDosageUnit = null;
	private Double denominatorDosage = null;
	private String denominatorDosageUnit = null;
	private String matchString = "";
	
	
	public static String getHeader() {
		String header = SourceIngredient.getHeader();
		header += "," + "Dosage";
		header += "," + "Unit";
		
		return header;
	}
	
	
	public SourceDrugComponent(SourceDrug sourceDrug, SourceIngredient ingredient, String dosage, String dosageUnit) {
		this.ingredient = ingredient;
		Double dosageValue = null;
		
		try {
			dosageValue = Double.parseDouble(dosage);
		}
		catch (NumberFormatException e) {
			dosageValue = null;
		}
		if (dosageValue != null) {
			if (dosageUnit.contains("/")) {
				String[] dosageUnitSplit = dosageUnit.split("/");
				numeratorDosageUnit = dosageUnitSplit[0].trim();
				numeratorDosageUnit = numeratorDosageUnit.equals("") ? null : numeratorDosageUnit;
				denominatorDosageUnit = dosageUnitSplit[1].trim();
				denominatorDosageUnit = denominatorDosageUnit.equals("") ? null : denominatorDosageUnit;
				numeratorDosage = dosageValue;
				denominatorDosage = 1.0;
			}
			else {
				numeratorDosage = this.dosage;
				numeratorDosageUnit = this.dosageUnit;
				denominatorDosage = 1.0;
				denominatorDosageUnit = null;
			}
			Source.countUnit(dosageUnit, sourceDrug);
		}
		this.dosage = dosageValue;
		this.dosageUnit = dosageUnit;
		
		ingredient.addDrug(sourceDrug);
	}
	
	
	public SourceIngredient getIngredient() {
		return ingredient;
	}
	
	
	public Double getDosage() {
		return dosage;
	}
	
	
	public String getDosageUnit() {
		return dosageUnit;
	}
	
	
	public Double getNumeratorDosage() {
		return numeratorDosage;
	}
	
	
	public String getNumeratorDosageUnit() {
		return numeratorDosageUnit;
	}
	
	
	public Double getDenominatorDosage() {
		return denominatorDosage;
	}
	
	
	public String getDenominatorDosageUnit() {
		return denominatorDosageUnit;
	}
	
	
	public String getMatchString() {
		return matchString;
	}
	
	
	public void setMatchString(String matchString) {
		this.matchString = matchString;
	}
	
	
	public String toString() {
		String description = ingredient.toString();
		description += "," + (dosage == null ? "" : dosage);
		description += "," + "\"" + (dosageUnit == null ? "" : dosageUnit) + "\"";
		return description;
	}
	
	
	public static String emptyRecord() {
		return ",,,,";
	}
	
	
	public void writeDescriptionToFile(String prefix, PrintWriter file) {
		file.println(prefix + this);
	}
	
	
	public boolean matches(SourceIngredient ingredient, String dosage, String dosageUnit) {
		Double dosageValue = null;
		try {
			dosageValue = Double.parseDouble(dosage);
		}
		catch (NumberFormatException e) {
			dosageValue = null;
		}
		dosageUnit = dosageUnit.equals("") ? null : dosageUnit;
		
		boolean matches = (
					(ingredient == this.ingredient) &&
					(dosageValue == this.dosage) && 
					(((dosageUnit == null) && (this.dosageUnit == null)) || ((dosageUnit != null) && dosageUnit.equals(this.dosageUnit)))
				);
		
		return matches;
	}

}
