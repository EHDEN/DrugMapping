package org.ohdsi.drugmapping.cdm;

import java.util.List;

import org.ohdsi.utilities.files.Row;

public class CDMIngredientStrength {
	private CDM cdm = null;
	
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
	
	private Double dosage                   = null;
	private String unit                     = null;
	
	
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
	
	
	public static String emptyRecord() {
		return CDMIngredient.emptyRecord() + ",,,,";
	}
	
	
	public CDMIngredientStrength(CDM cdm, Row queryRow, String prefix, CDMIngredient ingredient) {
		this.cdm = cdm;
		List<String> fieldNames = queryRow.getFieldNames();
		if (fieldNames.contains(prefix + "amount_value"))                amount_value_string      = queryRow.get(prefix + "amount_value", true);
		if (fieldNames.contains(prefix + "amount_unit_concept_id"))      amount_unit              = new CDMConcept(cdm, queryRow, prefix + "amount_unit_");
		if (fieldNames.contains(prefix + "numerator_value"))             numerator_value_string   = queryRow.get(prefix + "numerator_value", true);
		if (fieldNames.contains(prefix + "numerator_unit_concept_id"))   numerator_unit           = new CDMConcept(cdm, queryRow, prefix + "numerator_unit_");
		if (fieldNames.contains(prefix + "denominator_value"))           denominator_value_string = queryRow.get(prefix + "denominator_value", true);
		if (fieldNames.contains(prefix + "denominator_unit_concept_id")) denominator_unit         = new CDMConcept(cdm, queryRow, prefix + "denominator_unit_");
		if (fieldNames.contains(prefix + "box_size"))                    box_size                 = queryRow.get(prefix + "box_size", true);
		
		if ((amount_value_string != null) && (!amount_value_string.equals(""))) {
			try {
				amount_value = Double.parseDouble(amount_value_string);
			}
			catch (NumberFormatException e) {
				amount_value = null;
			}
		}
		
		if ((numerator_value_string != null) && (!numerator_value_string.equals(""))) {
			try {
				numerator_value = Double.parseDouble(numerator_value_string);
			}
			catch (NumberFormatException e) {
				numerator_value = null;
			}
		}
		
		if ((denominator_value_string != null) && (!denominator_value_string.equals(""))) {
			try {
				denominator_value = Double.parseDouble(denominator_value_string);
			}
			catch (NumberFormatException e) {
				denominator_value = null;
			}
		}
		
		if (amount_value != null) {
			dosage = amount_value;
			unit = amount_unit.getConceptCode().toUpperCase();
		}
		else {
			if (numerator_value != null) {
				dosage = numerator_value / (denominator_value == null ? 1.0 : denominator_value);
			}
			String numeratorCode = (((numerator_unit == null) || (numerator_unit.getConceptCode() == null)) ? "" : numerator_unit.getConceptCode());
			String denominatorCode = (((denominator_unit == null) || (denominator_unit.getConceptCode() == null)) ? "" : denominator_unit.getConceptCode());
			unit = (numeratorCode.equals("") ? "" : numeratorCode) + (denominatorCode.equals("") ? "" : "/" + denominatorCode);
		}
		
		this.ingredient = ingredient;
	}
	
	
	public String getAmountValueString() {
		return amount_value_string;
	}
	
	
	public Double getAmountValue() {
		return amount_value;
	}
	
	
	public CDMConcept getAmountUnit() {
		return amount_unit;
	}
	
	
	public String getNumeratorValueString() {
		return numerator_value_string;
	}
	
	
	public Double getNumeratorValue() {
		return numerator_value;
	}
	
	
	public CDMConcept getNumeratorUnit() {
		return numerator_unit;
	}
	
	
	public String getDenominatorValueString() {
		return denominator_value_string;
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
	
	
	public String getNumeratorDosageUnitName() {
		String numeratorDosageUnitName = null;
		if (getAmountValue() == null) {
			numeratorDosageUnitName = getNumeratorUnit().getConceptName();
		}
		else {
			numeratorDosageUnitName = getAmountUnit().getConceptName();
		}
		return numeratorDosageUnitName;
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

	
	
	public String getDenominatorDosageUnitName() {
		String denominatorDosageUnit = null;
		if (getAmountValue() == null) {
			denominatorDosageUnit = getDenominatorUnit().getConceptName();
		}
		return denominatorDosageUnit;
	}
	
	
	public Double getDosage() {
		return dosage;
	}
	
	
	public String getUnit() {
		return unit;
	}
	
	
	public String getBoxSize() {
		return box_size;
	}
	
	
	public CDMIngredient getIngredient() {
		return ingredient;
	}
	
	
	public String toStringIngredient() {
		
		String description = ingredient.toString();
		description += ",,,,";
		return description;
	}
	
	
	public String toString() {
		
		String description = ingredient.toString();
		description += "," + (getNumeratorDosage() == null ? "" : getNumeratorDosage());
		description += "," + (((numerator_unit == null) || (numerator_unit.getConceptCode() == null)) ? "" : numerator_unit.getConceptCode());
		description += "," + (getDenominatorDosage() == null ? "" : getDenominatorDosage());
		description += "," + (((denominator_unit == null) || (denominator_unit.getConceptCode() == null)) ? "" : denominator_unit.getConceptCode());
		return description;
	}
	
	
	public String getDescription() {
		String description = "";
		
		description = ingredient.getConceptName();
		description += " (" + ingredient.getConceptId() + ")";
		description += (getNumeratorDosage() == null ? "" : " " + getNumeratorDosage()) + ((getNumeratorDosageUnit() == null) ? "" : " " + getNumeratorDosageUnitName() + " (" + getNumeratorDosageUnit() + ")");
		description += " / ";
		description += (getDenominatorDosage() == null ? "" : " " +  getDenominatorDosage()) + ((getDenominatorDosageUnit() == null) ? "" : " " + getDenominatorDosageUnitName() + " (" + getDenominatorDosageUnit() + ")");
		
		return description;
	}
}
