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
	
	
	public CDMIngredientStrength getIngredientStrength(CDMIngredient ingredient) {
		CDMIngredientStrength ingredientStrength = null;
		for (CDMIngredientStrength strength : getIngredients()) {
			if (strength.getIngredient() == ingredient) {
				ingredientStrength = strength;
			}
		}
		return ingredientStrength;
	}
	
	
	public Double getNumeratorDosage(CDMIngredient ingredient) {
		Double numeratorDosage = null;
		CDMIngredientStrength ingredientStrength = getIngredientStrength(ingredient);
		if (ingredientStrength != null) {
			numeratorDosage = ingredientStrength.getNumeratorDosage();
		}
		return numeratorDosage;
	}
	
	
	public String getNumeratorDosageUnit(CDMIngredient ingredient) {
		String numeratorDosageUnit = null;
		CDMIngredientStrength ingredientStrength = getIngredientStrength(ingredient);
		if (ingredientStrength != null) {
			numeratorDosageUnit = ingredientStrength.getNumeratorDosageUnit();
		}
		return numeratorDosageUnit;
	}
	
	
	public Double getDenominatorDosage(CDMIngredient ingredient) {
		Double denominatorDosage = null;
		CDMIngredientStrength ingredientStrength = getIngredientStrength(ingredient);
		if (ingredientStrength != null) {
			denominatorDosage = ingredientStrength.getDenominatorDosage();
		}
		return denominatorDosage;
	}
	
	
	public String getDenominatorDosageUnit(CDMIngredient ingredient) {
		String denominatorDosageUnit = null;
		CDMIngredientStrength ingredientStrength = getIngredientStrength(ingredient);
		if (ingredientStrength != null) {
			denominatorDosageUnit = ingredientStrength.getDenominatorDosageUnit();
		}
		return denominatorDosageUnit;
	}
}
