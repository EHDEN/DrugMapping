package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;

public class SourceDrugComponent {
	private SourceIngredient ingredient = null;
	private Double dosage = null;
	private String dosageUnit = null;
	
	
	public SourceDrugComponent(SourceIngredient ingredient, String dosage, String dosageUnit) {
		this.ingredient = ingredient;
		try {
			this.dosage = Double.parseDouble(dosage);
		}
		catch (NumberFormatException e) {
			this.dosage = null;
		}
		this.dosageUnit = dosageUnit.equals("") ? null : dosageUnit;
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

}
