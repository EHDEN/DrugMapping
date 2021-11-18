package org.ohdsi.drugmapping.cdm;

import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

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
	
	
	public CDMIngredientStrength(CDM cdm, DelimitedFileRow queryRow, String prefix, CDMIngredient ingredient) {
		if (queryRow.get(prefix + "amount_value", true) != null)                amount_value_string      = queryRow.get(prefix + "amount_value", true);
		if (queryRow.get(prefix + "amount_unit_concept_id", true) != null)      amount_unit              = new CDMConcept(cdm, queryRow, prefix + "amount_unit_");
		if (queryRow.get(prefix + "numerator_value", true) != null)             numerator_value_string   = queryRow.get(prefix + "numerator_value", true);
		if (queryRow.get(prefix + "numerator_unit_concept_id", true) != null)   numerator_unit           = new CDMConcept(cdm, queryRow, prefix + "numerator_unit_");
		if (queryRow.get(prefix + "denominator_value", true) != null)           denominator_value_string = queryRow.get(prefix + "denominator_value", true);
		if (queryRow.get(prefix + "denominator_unit_concept_id", true) != null) denominator_unit         = new CDMConcept(cdm, queryRow, prefix + "denominator_unit_");
		if (queryRow.get(prefix + "box_size", true) != null)                    box_size                 = queryRow.get(prefix + "box_size", true);
		
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
			unit = DrugMappingStringUtilities.safeToUpperCase(amount_unit.getConceptCode());
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
			numeratorDosageUnit = getNumeratorUnit() == null ? null : getNumeratorUnit().getConceptId();
		}
		else {
			numeratorDosageUnit = getAmountUnit() == null ? null : getAmountUnit().getConceptId();
		}
		return numeratorDosageUnit;
	}
	
	
	public String getNumeratorDosageUnitName() {
		String numeratorDosageUnitName = null;
		if (getAmountValue() == null) {
			numeratorDosageUnitName = getNumeratorUnit() == null ? "NULL" : getNumeratorUnit().getConceptName();
		}
		else {
			numeratorDosageUnitName = getAmountUnit() == null ? "NULL" : getAmountUnit().getConceptName();
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
			denominatorDosageUnit = getDenominatorUnit() == null ? null : getDenominatorUnit().getConceptId();
		}
		return denominatorDosageUnit;
	}

	
	
	public String getDenominatorDosageUnitName() {
		String denominatorDosageUnit = null;
		if (getAmountValue() == null) {
			denominatorDosageUnit = getDenominatorUnit() == null ? "NULL" : getDenominatorUnit().getConceptName();
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
		
		String description = ingredient == null ? CDMIngredient.emptyRecord() : ingredient.toString();
		description += "," + (getNumeratorDosage() == null ? "" : getNumeratorDosage());
		description += "," + (((numerator_unit == null) || (numerator_unit.getConceptCode() == null)) ? "" : numerator_unit.getConceptCode());
		description += "," + (getDenominatorDosage() == null ? "" : getDenominatorDosage());
		description += "," + (((denominator_unit == null) || (denominator_unit.getConceptCode() == null)) ? "" : denominator_unit.getConceptCode());
		return description;
	}
	
	
	public String getDescription() {
		String description = "";
		
		if (ingredient != null) {
			description = ingredient.getConceptName();
			description += " (" + ingredient.getConceptId() + ")";
		}
		else {
			description = "NULL";
		}
		description += (getNumeratorDosage() == null ? "" : " " + getNumeratorDosage());
		description += ((getNumeratorDosageUnit() == null) ? "" : " " + getNumeratorDosageUnitName() + " (" + getNumeratorDosageUnit() + ")");
		description += " / ";
		description += (getDenominatorDosage() == null ? "" : " " +  getDenominatorDosage());
		description += ((getDenominatorDosageUnit() == null) ? "" : " " + getDenominatorDosageUnitName() + " (" + getDenominatorDosageUnit() + ")");
		
		return description;
	}
}
