package org.ohdsi.drugmapping.cdm;

import java.util.ArrayList;
import java.util.List;

import org.ohdsi.utilities.files.Row;

public class CDMDrug extends CDMConcept {
	private List<CDMStrength> ingredients = new ArrayList<CDMStrength>(); 
	

	public CDMDrug(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}
	
	
	public List<CDMStrength> getIngredients() {
		return ingredients;
	}
	
	
	public void addIngredient(CDMStrength ingredient) {
		ingredients.add(ingredient);
	}
}
