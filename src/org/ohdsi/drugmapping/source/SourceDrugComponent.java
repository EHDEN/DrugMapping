package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;

import org.ohdsi.drugmapping.UnitConversion;

public class SourceDrugComponent {
	private SourceIngredient ingredient = null;
	private Double dosage = null;
	private String dosageUnit = null;
	private Double numeratorDosage = null;
	private String numeratorDosageUnit = null;
	private Double denominatorDosage = null;
	private String denominatorDosageUnit = null;
	
	
	public static String getHeader() {
		String header = SourceIngredient.getHeader();
		header += "," + "NumeratorDosage";
		header += "," + "NumeratorDosageUnit";
		header += "," + "DenominatorDosage";
		header += "," + "DenominatorDosageUnit";
		
		return header;
	}
	
	
	public SourceDrugComponent(SourceIngredient ingredient, String dosage, String dosageUnit) {
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
				this.dosage = null;
				this.dosageUnit = null;
				numeratorDosageUnit = dosageUnitSplit[0].trim();
				numeratorDosageUnit = numeratorDosageUnit.equals("") ? null : numeratorDosageUnit;
				denominatorDosageUnit = dosageUnitSplit[1].trim();
				denominatorDosageUnit = denominatorDosageUnit.equals("") ? null : denominatorDosageUnit;
				numeratorDosage = dosageValue;
				denominatorDosage = 1.0;
			}
			else {
				this.dosage = dosageValue;
				this.dosageUnit = dosageUnit.equals("") ? null : dosageUnit;
				numeratorDosage = this.dosage;
				numeratorDosageUnit = this.dosageUnit;
				denominatorDosage = 1.0;
				denominatorDosageUnit = null;
			}
		}
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
	
	
	public String getNumeratorDosageUnit() {
		return numeratorDosageUnit;
	}
	
	
	public String getDenominatorDosageUnit() {
		return denominatorDosageUnit;
	}
	
	
	public String toString() {
		String description = ingredient.toString();
		description += "," + (numeratorDosage == null ? "" : numeratorDosage);
		description += "," + "\"" + (numeratorDosageUnit == null ? "" : numeratorDosageUnit) + "\"";
		description += "," + (denominatorDosage == null ? "" : denominatorDosage);
		description += "," + "\"" + (denominatorDosageUnit == null ? "" : denominatorDosageUnit) + "\"";
		/*
		if ((dosage == null) && (dosageUnit == null)) {
			description += "," + (numeratorDosage == null ? "" : numeratorDosage) + "/" + (denominatorDosage == null ? "" : denominatorDosage);
			description += "," + "\"" + (numeratorDosageUnit == null ? "" : numeratorDosageUnit) + "/" + (denominatorDosageUnit == null ? "" : denominatorDosageUnit) + "\"";
		}
		else {
			description += "," + (dosage == null ? "" : dosage);
			description += "," + (dosageUnit == null ? "" : "\"" + dosageUnit + "\"");
		}
		*/
		return description;
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
	
	
	public boolean matches(UnitConversion unitConversion, Double numeratorDosage, String numeratorDosageUnit, Double denominatorDosage, String denominatorDosageUnit) {
		return unitConversion.matches(this.numeratorDosageUnit, this.numeratorDosage, this.denominatorDosageUnit, this.denominatorDosage, numeratorDosageUnit, numeratorDosage, denominatorDosageUnit, denominatorDosage);
	}

}
