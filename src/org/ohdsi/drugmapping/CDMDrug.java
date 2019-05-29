package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.List;

import org.ohdsi.utilities.files.Row;

public class CDMDrug extends CDMConcept {
	private List<CDMIngredient> ingredients = new ArrayList<CDMIngredient>(); 
	

	public CDMDrug(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}
	
	
	public List<CDMIngredient> getIngredients() {
		return ingredients;
	}
	
	
	public void addIngredient(CDMIngredient ingredient) {
		ingredients.add(ingredient);
	}
}
