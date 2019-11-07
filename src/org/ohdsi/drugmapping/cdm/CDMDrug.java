package org.ohdsi.drugmapping.cdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.utilities.files.Row;

public class CDMDrug extends CDMConcept {
	private Set<String> formConceptIds = new HashSet<String>();
	private List<CDMIngredientStrength> ingredients = new ArrayList<CDMIngredientStrength>(); 
	private Map<String, Set<CDMIngredientStrength>> ingredientsMap = new HashMap<String, Set<CDMIngredientStrength>>();
	
	
	public static String getHeader() {
		return getHeader("");
	}
	
	public static String getHeader(String prefix) {
		String header = CDMConcept.getHeader(prefix);
		header += "," + prefix + "form_concept_id";
		return header;
	}
	
	
	public String toString() {
		String description = super.toString();
		String form_concept_ids = "";
		for (String form_concept_id : formConceptIds) {
			if (!form_concept_ids.equals("")) {
				form_concept_ids += "|";
			}
			form_concept_ids += form_concept_id;
		}
		description += "," + form_concept_ids;
		return description;
	}
	

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
	
	
	public Map<String, Set<CDMIngredientStrength>> getIngredientsMap() {
		return ingredientsMap;
	}
	
	
	public void addIngredientStrength(CDMIngredientStrength ingredient) {
		ingredients.add(ingredient);
		String ingredientConceptId = ingredient.getIngredient().getConceptId(); 
		Set<CDMIngredientStrength> ingredientSet = ingredientsMap.get(ingredientConceptId);
		if (ingredientSet == null) {
			ingredientSet = new HashSet<CDMIngredientStrength>();
			ingredientsMap.put(ingredientConceptId, ingredientSet);
		}
		ingredientSet.add(ingredient);
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
