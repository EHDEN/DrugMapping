package org.ohdsi.drugmapping.cdm;

import org.ohdsi.utilities.files.Row;

public class CDMStrength {
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
	
	
	public CDMStrength(Row queryRow, String prefix) {
		amount_value_string      = queryRow.get(prefix + "amount_value");
		amount_unit              = new CDMConcept(queryRow, prefix + "amount_unit_");
		numerator_value_string   = queryRow.get(prefix + "numerator_value");
		numerator_unit           = new CDMConcept(queryRow, prefix + "numerator_unit_");
		denominator_value_string = queryRow.get(prefix + "denominator_value");
		denominator_unit         = new CDMConcept(queryRow, prefix + "denominator_unit_");
		box_size                 = queryRow.get(prefix + "box_size");
		
		try {
			amount_value = Double.parseDouble(amount_value_string);
		}
		catch (NumberFormatException e) {
			amount_value = null;
		}
		
		try {
			numerator_value = Double.parseDouble(numerator_value_string);
		}
		catch (NumberFormatException e) {
			numerator_value = null;
		}
		
		try {
			denominator_value = Double.parseDouble(denominator_value_string);
		}
		catch (NumberFormatException e) {
			denominator_value = null;
		}
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
	
	
	public String getBoxSize() {
		return box_size;
	}
	
	
	public String toStringLong() {
		
		String description = super.toString();
		if (amount_value == null) {
			if ((numerator_value != null) || (denominator_value == null)) {
				description += "," + (numerator_value == null ? "" : numerator_value) + (numerator_unit.getConceptName() == null ? "" : numerator_unit.getConceptName());
				description += "/" + (denominator_value == null ? "" : denominator_value) + (denominator_unit.getConceptName() == null ? "" : denominator_unit.getConceptName());
			}
		}
		else {
			description += "," + (amount_value == null ? "" : amount_value) + (amount_unit.getConceptName() == null ? "" : amount_unit.getConceptName());
		}
		
		return description;
	}
}
