package org.ohdsi.drugmapping;

import org.ohdsi.utilities.files.Row;

public class CDMIngredient extends CDMDrug {
	private String amount_value         = null;
	private CDMConcept amount_unit      = null;
	private String numerator_value      = null;
	private CDMConcept numerator_unit   = null;
	private String denominator_value    = null;
	private CDMConcept denominator_unit = null;
	private String box_size             = null;
	
	
	public CDMIngredient(Row queryRow, String prefix) {
		super(queryRow, prefix);
		amount_value      = queryRow.get(prefix + "amount_value");
		amount_unit       = new CDMConcept(queryRow, prefix + "amount_unit_");
		numerator_value   = queryRow.get(prefix + "numerator_value");
		numerator_unit    = new CDMConcept(queryRow, prefix + "numerator_unit_");
		denominator_value = queryRow.get(prefix + "denominator_value");
		denominator_unit  = new CDMConcept(queryRow, prefix + "denominator_unit_");
		box_size          = queryRow.get(prefix + "box_size");
		
	}
	
	
	public String getAmountValue() {
		return amount_value;
	}
	
	
	public CDMConcept getAmountUnit() {
		return amount_unit;
	}
	
	
	public String getNumeratorValue() {
		return numerator_value;
	}
	
	
	public CDMConcept getNumeratorUnit() {
		return numerator_unit;
	}
	
	
	public String getDenominatorValue() {
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
