package org.ohdsi.drugmapping.cdm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ohdsi.utilities.files.Row;

public class CDMDrug extends CDMConcept {
	private Set<String> formConceptIds = new HashSet<String>();
	private List<CDMIngredientStrength> ingredients = new ArrayList<CDMIngredientStrength>(); 
	

	public CDMDrug(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}
	
	
	public void addForm(String formConceptId) {
		formConceptIds.add(formConceptId);
	}
	
	
	public Set<String> getForms() {
		return formConceptIds;
	}
	
	
	public List<CDMIngredientStrength> getIngredients() {
		return ingredients;
	}
	
	
	public void addIngredientStrength(CDMIngredientStrength ingredient) {
		ingredients.add(ingredient);
	}
}
