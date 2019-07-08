package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SourceDrug {
	private static boolean error = false;
	private static Set<SourceDrugComponent> allComponents = new HashSet<SourceDrugComponent>();
	private static Set<SourceIngredient> allIngredients = new HashSet<SourceIngredient>();
	private static Map<String, SourceIngredient> ingredientNameIndex = new HashMap<String, SourceIngredient>();
	
	
	private String code = null;
	private String name = null;
	private String atcCode = null;
	private String formulation = null;
	private List<SourceDrugComponent> components = new ArrayList<SourceDrugComponent>();
	
	
	public static Set<SourceDrugComponent> getAllComponents() {
		return allComponents;
	}
	
	
	public static Set<SourceIngredient> getAllIngredients() {
		return allIngredients;
	}
	
	
	public static SourceIngredient findIngredient(String ingredientName, String ingredientNameEnglish, String casNumber) {
		error = false;
		SourceIngredient sourceIngredient = null;

		String ingredientNameNoSpaces = ingredientName.replaceAll(" ", "").replaceAll("-", "");
		String ingredientNameEnglishNoSpaces = ingredientNameEnglish.replaceAll(" ", "").replaceAll("-", "");
		sourceIngredient = ingredientNameIndex.get(ingredientNameNoSpaces);
		if (sourceIngredient != null) {
			if (sourceIngredient.getIngredientNameEnglishNoSpaces().equals("")) {
				sourceIngredient.setIngredientNameEnglish(ingredientNameEnglish);
			}
			else if (!sourceIngredient.getIngredientNameEnglishNoSpaces().equals(ingredientNameEnglishNoSpaces)) {
				if (sourceIngredient.getIngredientNameEnglishNoSpaces().equals(sourceIngredient.getIngredientNameNoSpaces()) && (!sourceIngredient.getIngredientNameNoSpaces().equals(ingredientNameEnglishNoSpaces))) {
					sourceIngredient.setIngredientNameEnglish(ingredientNameEnglish);
				}
				else {
					System.out.println("    NameEnglish conflict: '" + ingredientNameEnglish + "' <-> " + sourceIngredient);
					error = true;
				}
			}
			
			if (!casNumber.equals("")) {
				if (sourceIngredient.getCASNumber() == null) {
					sourceIngredient.setCASNumber(casNumber);
				}
				else if (!sourceIngredient.getCASNumber().equals(casNumber)) {
					System.out.println("    CASNumber conflict: '" + casNumber + "' <-> " + sourceIngredient);
					error = true;
				}
			}
		}
		
		return error ? null : sourceIngredient;
	}
	
	
	public static boolean errorOccurred() {
		return error;
	}
	
	
	public static String getHeader() {
		String header = "SourceCode";
		header += "," + "SourceName";
		header += "," + "SourceATCCode";
		header += "," + "Formulation";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public SourceDrug(String sourceCode, String sourceName, String sourceATCCode, String formulation) {
		this.code = sourceCode.equals("") ? null : sourceCode;
		this.name = sourceName.equals("") ? null : sourceName;
		this.atcCode = sourceATCCode.equals("") ? null : sourceATCCode;
		this.formulation = formulation.equals("") ? null : formulation;
	}
	
	
	public String getCode() {
		return code;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public String getATCCode() {
		return atcCode;
	}
	
	
	public String getFormulation() {
		return formulation;
	}
	
	
	public SourceIngredient AddIngredient(String ingredientName, String ingredientNameEnglish, String casNumber, String dosage, String dosageUnit) {
		SourceIngredient sourceIngredient = null;
		for (SourceIngredient ingredient : allIngredients) {
			if (ingredient.matches(ingredientName, ingredientNameEnglish, casNumber)) {
				sourceIngredient = ingredient;
				break;
			}
		}
		if (sourceIngredient == null) {
			sourceIngredient = new SourceIngredient(ingredientName, ingredientNameEnglish, casNumber);
			allIngredients.add(sourceIngredient);
			if (!ingredientName.equals("")) {
				ingredientNameIndex.put(sourceIngredient.getIngredientNameNoSpaces(), sourceIngredient);
			}
		}
		SourceDrugComponent sourceComponent = null;
		for (SourceDrugComponent component : allComponents) {
			if (component.matches(sourceIngredient, dosage, dosageUnit)) {
				sourceComponent = component;
				break;
			}
		}
		if (sourceComponent == null) {
			sourceComponent = new SourceDrugComponent(sourceIngredient, dosage, dosageUnit);
			allComponents.add(sourceComponent);
		}
		components.add(sourceComponent);
		return sourceIngredient;
	}
	
	
	public SourceDrugComponent getIngredientComponent(SourceIngredient ingredient) {
		SourceDrugComponent component = null;
		for (SourceDrugComponent searchComponent : components) {
			if (searchComponent.getIngredient() == ingredient) {
				component = searchComponent;
				break;
			}
		}
		return component;
	}
	
	
	public Double getIngredientDosage(SourceIngredient ingredient) {
		Double dosage = null;
		SourceDrugComponent component = getIngredientComponent(ingredient);
		if (component != null) {
			dosage = component.getDosage();
		}
		return dosage;
	}
	
	
	public String getIngredientDosageUnit(SourceIngredient ingredient) {
		String dosageUnit = null;
		SourceDrugComponent component = getIngredientComponent(ingredient);
		if (component != null) {
			dosageUnit = component.getDosageUnit();
		}
		return dosageUnit;
	}
	
	
	public List<SourceDrugComponent> getComponents() {
		return components;
	}
	
	
	public Set<SourceIngredient> getIngredients() {
		Set<SourceIngredient> ingredients = new HashSet<SourceIngredient>();
		for (SourceDrugComponent component : components) {
			if (component.getIngredient() != null) {
				ingredients.add(component.getIngredient());
			}
		}
		return ingredients;
	}
	
	
	public String toString() {
		String description = (code == null ? "" : "\"" + code + "\"");
		description += "," + (name == null ? "" : "\"" + name + "\"");
		description += "," + (atcCode == null ? "" : atcCode);
		description += "," + (formulation == null ? "" : "\"" + formulation + "\"");
		return description;
	}
	
	
	public void writeDescriptionToFile(String prefix, PrintWriter file) {
		file.println(prefix + this);
	}
	
	
	public void writeFullDescriptionToFile(String prefix, PrintWriter file) {
		file.println(prefix + this);
		for (SourceDrugComponent component : components) {
			component.writeDescriptionToFile(prefix + "    ", file);
		}
	}

}
