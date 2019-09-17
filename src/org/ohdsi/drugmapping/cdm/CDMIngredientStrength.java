package org.ohdsi.drugmapping.cdm;

import java.util.List;

import org.ohdsi.utilities.files.Row;

public class CDMIngredientStrength {
	private String amount_value_string      = null;
	private Double amount_value             = null;
	private CDMConcept amount_unit          = null;
	private String numerator_value_string   = null;
	private Double numerator_value          = null;
	private CDMConcept numerator_unit       = null;
	private String denominator_value_string = null;
	private Double denominator_value        = null;
	private CDMConcept denominator_unit     = null;
	private String box_size                 = null;
	private CDMIngredient ingredient        = null;
	
	
	public static String getHeader() {
		return getHeader("");
	}
	
	
	public static String getHeader(String prefix) {
		String header = CDMIngredient.getHeader(prefix);
		header += "," + prefix + "NumeratorDosage";
		header += "," + prefix + "NumeratorUnit";
		header += "," + prefix + "DenominatorDosage";
		header += "," + prefix + "DenominatorUnit";
		
		return header;
	}
	
	
	public CDMIngredientStrength(Row queryRow, String prefix, CDMIngredient ingredient) {
		List<String> fieldNames = queryRow.getFieldNames();
		if (fieldNames.contains(prefix + "amount_value"))                amount_value_string      = queryRow.get(prefix + "amount_value");
		if (fieldNames.contains(prefix + "amount_unit_concept_id"))      amount_unit              = new CDMConcept(queryRow, prefix + "amount_unit_");
		if (fieldNames.contains(prefix + "numerator_value"))             numerator_value_string   = queryRow.get(prefix + "numerator_value");
		if (fieldNames.contains(prefix + "numerator_unit_concept_id"))   numerator_unit           = new CDMConcept(queryRow, prefix + "numerator_unit_");
		if (fieldNames.contains(prefix + "denominator_value"))           denominator_value_string = queryRow.get(prefix + "denominator_value");
		if (fieldNames.contains(prefix + "denominator_unit_concept_id")) denominator_unit         = new CDMConcept(queryRow, prefix + "denominator_unit_");
		if (fieldNames.contains(prefix + "box_size"))                    box_size                 = queryRow.get(prefix + "box_size");
		
		if (amount_value_string != null) {
			try {
				amount_value = Double.parseDouble(amount_value_string);
			}
			catch (NumberFormatException e) {
				amount_value = null;
			}
		}
		
		if (numerator_value_string != null) {
			try {
				numerator_value = Double.parseDouble(numerator_value_string);
			}
			catch (NumberFormatException e) {
				numerator_value = null;
			}
		}
		
		if (denominator_value_string != null) {
			try {
				denominator_value = Double.parseDouble(denominator_value_string);
			}
			catch (NumberFormatException e) {
				denominator_value = null;
			}
		}
		
		this.ingredient = ingredient;
	}
	
	
	public Double getAmountValue() {
		return amount_value;
	}
	
	
	public CDMConcept getAmountUnit() {
		return amount_unit;
	}
	
	
	public Double getNumeratorValue() {
		return numerator_value;
	}
	
	
	public CDMConcept getNumeratorUnit() {
		return numerator_unit;
	}
	
	
	public Double getDenominatorValue() {
		return denominator_value;
	}
	
	
	public CDMConcept getDenominatorUnit() {
		return denominator_unit;
	}
	
	
	public Double getNumeratorDosage() {
		Double numeratorDosage = getAmountValue(); 
		if (numeratorDosage == null) {
			numeratorDosage = getNumeratorValue(); 
		}
		return numeratorDosage;
	}
	
	
	public String getNumeratorDosageUnit() {
		String numeratorDosageUnit = null;
		if (getAmountValue() == null) {
			numeratorDosageUnit = getNumeratorUnit().getConceptId();
		}
		else {
			numeratorDosageUnit = getAmountUnit().getConceptId();
		}
		return numeratorDosageUnit;
	}
	
	
	public Double getDenominatorDosage() {
		Double denominatorDosage = 1.0; 
		if ((getAmountValue() == null) && (getDenominatorValue() != null)) {
			denominatorDosage = getDenominatorValue(); 
		}
		return denominatorDosage;
	}
	
	
	public String getDenominatorDosageUnit() {
		String denominatorDosageUnit = null;
		if (getAmountValue() == null) {
			denominatorDosageUnit = getDenominatorUnit().getConceptId();
		}
		return denominatorDosageUnit;
	}
	
	
	public String getBoxSize() {
		return box_size;
	}
	
	
	public CDMIngredient getIngredient() {
		return ingredient;
	}
	
	
	public String toStringLong() {
		
		String description = ingredient.toString();
		description += "," + (getNumeratorDosage() == null ? "" : getNumeratorDosage());
		description += "," + (numerator_unit.getConceptName() == null ? "" : numerator_unit.getConceptName());
		description += "," + (getDenominatorDosage() == null ? "" : getDenominatorDosage());
		description += "," + (denominator_unit.getConceptName() == null ? "" : denominator_unit.getConceptName());
		/*
		if (amount_value == null) {
			if ((numerator_value != null) || (denominator_value == null)) {
				description += "," + (numerator_value == null ? "" : numerator_value) + (numerator_unit.getConceptName() == null ? "" : numerator_unit.getConceptName());
				description += "/" + (denominator_value == null ? "" : denominator_value) + (denominator_unit.getConceptName() == null ? "" : denominator_unit.getConceptName());
			}
		}
		else {
			description += "," + (amount_value == null ? "" : amount_value) + (amount_unit.getConceptName() == null ? "" : amount_unit.getConceptName());
		}
		*/
		return description;
	}
	
	
	public static String emptyRecordLong() {
		return CDMIngredient.emptyRecord() + ",,,,";
	}
}
