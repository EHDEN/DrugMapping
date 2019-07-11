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
	
	
	public String toSTring() {
		String description = ingredient.toString();
		description += "," + (dosage == null ? "" : dosage);
		description += "," + (dosageUnit == null ? "" : "\"" + dosageUnit + "\"");
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
	
	
	public boolean matches(SourceIngredient ingredient, String numeratorDosage, String numeratorDosageUnit, String denominatorDosage, String denominatorDosageUnit) {
		boolean matches = false;
		//TODO
		/*
		Double dosageValue = null;
		try {
			dosageValue = Double.parseDouble(dosage);
		}
		catch (NumberFormatException e) {
			dosageValue = null;
		}
		dosageUnit = dosageUnit.equals("") ? null : dosageUnit;
		
		matches = (
					(ingredient == this.ingredient) &&
					(dosageValue == this.dosage) && 
					(((dosageUnit == null) && (this.dosageUnit == null)) || ((dosageUnit != null) && dosageUnit.equals(this.dosageUnit)))
				);
		*/
		return matches;
	}

}
