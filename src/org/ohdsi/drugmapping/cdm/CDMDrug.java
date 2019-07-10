package org.ohdsi.drugmapping.cdm;

import java.util.ArrayList;
import java.util.List;

import org.ohdsi.utilities.files.Row;

public class CDMDrug extends CDMConcept {
	private String formConceptId = null;
	private List<CDMIngredientStrength> ingredients = new ArrayList<CDMIngredientStrength>(); 
	

	public CDMDrug(Row queryRow, String prefix) {
		super(queryRow, prefix);
	}
	
	
	public void setForm(String formConceptId) {
		this.formConceptId = formConceptId;
	}
	
	
	public String getForm() {
		return formConceptId;
	}
	
	
	public List<CDMIngredientStrength> getIngredients() {
		return ingredients;
	}
	
	
	public void addIngredientStrength(CDMIngredientStrength ingredient) {
		ingredients.add(ingredient);
	}
}
