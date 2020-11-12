package org.ohdsi.drugmapping.cdm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.files.DelimitedFileRow;

public class CDMDrug extends CDMConcept {
	private List<String> formConceptIds = new ArrayList<String>();
	private List<String> formConceptNames = new ArrayList<String>();
	private List<CDMIngredientStrength> ingredientStrengths = new ArrayList<CDMIngredientStrength>();
	private List<CDMIngredient> ingredients = new ArrayList<CDMIngredient>(); 
	private Map<String, List<CDMIngredientStrength>> ingredientsMap = new HashMap<String, List<CDMIngredientStrength>>();
	private List<String> atcList = new ArrayList<String>();
	
	
	public static String getHeader() {
		return getHeader("");
	}
	
	public static String getHeader(String prefix) {
		return CDMConcept.getHeader(prefix);
	}

	public CDMDrug(CDM cdm, DelimitedFileRow queryRow, String prefix) {
		super(cdm, queryRow, prefix);
	}
	
	
	public void addForm(String formConceptId) {
		if (!formConceptIds.contains(formConceptId)) {
			formConceptIds.add(formConceptId);
			Collections.sort(formConceptIds);
		}
		String formConceptName = cdm.getCDMFormConceptName(formConceptId);
		if (!formConceptNames.contains(formConceptName)) {
			formConceptNames.add(formConceptName);
			Collections.sort(formConceptNames);
		}
	}
	
	
	public void addATC(String atc) {
		if (atc != null) {
			if (atcList.size() > 0) {
				if (atc.length() > atcList.get(0).length()) {
					atcList = new ArrayList<String>();
					atcList.add(atc);
				}
				else if (atc.length() == atcList.get(0).length()) {
					atcList.add(atc);
				}
			}
			else {
				atcList.add(atc);
			}
		}
	}
	
	
	public List<String> getATCs() {
		return atcList;
	}
	
	
	public List<String> getFormConceptIds() {
		return formConceptIds;
	}
	
	
	public List<String> getFormConceptNames() {
		return formConceptNames;
	}
	
	
	public List<CDMIngredient> getIngredients() {
		return ingredients;
	}
	
	
	public List<CDMIngredientStrength> getIngredientStrengths() {
		return ingredientStrengths;
	}
	
	
	public Map<String, List<CDMIngredientStrength>> getIngredientsMap() {
		return ingredientsMap;
	}
	
	
	public String getStrengthDescription() {
		String description = "";
		
		for (CDMIngredientStrength strength : ingredientStrengths) {
			description += (description.equals("") ? "" : "; ");
			description += strength.getDescription();
		}
		
		return description;
	}
	
	
	public void addIngredientStrength(CDMIngredientStrength ingredientStrength) {
		ingredientStrengths.add(ingredientStrength);
		CDMIngredient ingredient = ingredientStrength.getIngredient();
		ingredients.add(ingredient);
		if (ingredient != null) {
			ingredient.addDrug(this);
			String ingredientConceptId = ingredient.getConceptId(); 
			List<CDMIngredientStrength> ingredientSet = ingredientsMap.get(ingredientConceptId);
			if (ingredientSet == null) {
				ingredientSet = new ArrayList<CDMIngredientStrength>();
				ingredientsMap.put(ingredientConceptId, ingredientSet);
			}
			if (!ingredientSet.contains(ingredientStrength)) {
				ingredientSet.add(ingredientStrength);
				Collections.sort(ingredientSet, new Comparator<CDMIngredientStrength>() {

					@Override
					public int compare(CDMIngredientStrength ingredient1, CDMIngredientStrength ingredient2) {
						String ingredient_concept_id1 = ingredient1.getIngredient() == null ? "" : ingredient1.getIngredient().getConceptId();
						String ingredient_concept_id2 = ingredient1.getIngredient() == null ? "" : ingredient2.getIngredient().getConceptId();
						return ingredient_concept_id1.compareTo(ingredient_concept_id2);
					}
				});
			}
		}
	}
	
	
	public CDMIngredientStrength getIngredientStrength(CDMIngredient ingredient) {
		CDMIngredientStrength ingredientStrength = null;
		for (CDMIngredientStrength strength : getIngredientStrengths()) {
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
	
	
	public String generatedName(boolean includeDose) {
		String generatedName = "";
		
		for (CDMIngredientStrength ingredientStrength : getIngredientStrengths()) {
			if (!generatedName.equals("")) {
				generatedName += " / ";
			}
			generatedName += (ingredientStrength.getIngredient() == null ? "<UNKNOWN>" : ingredientStrength.getIngredient().getConceptName());
			if (includeDose) {
				if (ingredientStrength.getAmountValueString() == null) {
					generatedName += " " + (ingredientStrength.getNumeratorValueString() == null ? "1" : ingredientStrength.getNumeratorValueString());
					generatedName += ingredientStrength.getNumeratorDosageUnit() == null ? "" : (" " + ingredientStrength.getNumeratorDosageUnit());
					generatedName += "/" + (ingredientStrength.getDenominatorValueString() == null ? "1" : ingredientStrength.getDenominatorValueString());
					generatedName += ingredientStrength.getDenominatorDosageUnit() == null ? "" : (" " + ingredientStrength.getDenominatorDosageUnit());
				}
				else {
					generatedName += " " + (ingredientStrength.getAmountValueString() == null ? "1" : ingredientStrength.getAmountValueString());
					generatedName += ingredientStrength.getAmountUnit() == null ? "" : (" " + ingredientStrength.getAmountUnit().getConceptName());
				}
			}
		}
		
		return generatedName;
	}
}
