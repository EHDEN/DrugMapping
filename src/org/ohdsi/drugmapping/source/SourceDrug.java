package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.drugmapping.cdm.CDMDrug;

public class SourceDrug {
	private static boolean error = false;
	
	private static Set<SourceDrugComponent> allComponents = new HashSet<SourceDrugComponent>();
	private static Set<SourceIngredient> allIngredients = new HashSet<SourceIngredient>();
	private static Map<String, List<SourceIngredient>> ingredientNameIndex = new HashMap<String, List<SourceIngredient>>();
	private static Map<String, SourceIngredient> ingredientCASNumberIndex = new HashMap<String, SourceIngredient>();
	
	private static Integer casNumbersSet = 0;
	
	
	private String code = null;
	private String name = null;
	private String atcCode = null;
	private String formulation = null;
	private Integer count = null;
	private List<SourceDrugComponent> components = new ArrayList<SourceDrugComponent>();
	private CDMDrug mappedDrug = null;
	
	
	public static void init() {
		error = false;
		
		allComponents = new HashSet<SourceDrugComponent>();
		allIngredients = new HashSet<SourceIngredient>();
		ingredientNameIndex = new HashMap<String, List<SourceIngredient>>();
		ingredientCASNumberIndex = new HashMap<String, SourceIngredient>();
		
		casNumbersSet = 0;
	}
	
	
	public static Set<SourceDrugComponent> getAllComponents() {
		return allComponents;
	}
	
	
	public static Set<SourceIngredient> getAllIngredients() {
		return allIngredients;
	}
	
	
	public static SourceIngredient findIngredient(String casNumber) {
		SourceIngredient sourceIngredient = null;

		if ((casNumber != null) && (!casNumber.equals(""))) {
			sourceIngredient = ingredientCASNumberIndex.get(casNumber);
		}
		return sourceIngredient;
	}
	
	
	public static SourceIngredient getIngredient(String ingredientName, String ingredientNameEnglish, String casNumber) {
		error = false;
		SourceIngredient sourceIngredient = null;

		String ingredientNameNoSpaces = ingredientName.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(" ", "").replaceAll("-", "");
		String ingredientNameEnglishNoSpaces = ingredientNameEnglish.replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(" ", "").replaceAll("-", "");
		List<SourceIngredient> sourceIngredients = ingredientNameIndex.get(ingredientNameNoSpaces);
		if (sourceIngredients != null) {
			if (sourceIngredients.size() == 1) {
				sourceIngredient = sourceIngredients.get(0);
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
						casNumbersSet++;
					}
					else if (!sourceIngredient.getCASNumber().equals(casNumber)) {
						/*
						System.out.println("    CASNumber conflict: '" + casNumber + "' <-> " + sourceIngredient);
						error = true;
						*/
						sourceIngredient = new SourceIngredient(ingredientName, ingredientNameEnglish, casNumber);
						sourceIngredients.add(sourceIngredient);
						ingredientCASNumberIndex.put(casNumber, sourceIngredient);
					}
				}
			}
			else {
				if (!casNumber.equals("")) {
					sourceIngredient = ingredientCASNumberIndex.get(casNumber);
					if (sourceIngredient == null) {
						sourceIngredient = new SourceIngredient(ingredientName, ingredientNameEnglish, casNumber);
						sourceIngredients.add(sourceIngredient);
						ingredientCASNumberIndex.put(casNumber, sourceIngredient);
					}
				}
				else {
					for (SourceIngredient ingredient : sourceIngredients) {
						if (ingredient.getCASNumber() == null) {
							sourceIngredient = ingredient;
							break;
						}
					}
					if (sourceIngredient == null) {
						sourceIngredient = new SourceIngredient(ingredientName, ingredientNameEnglish, casNumber);
						sourceIngredients.add(sourceIngredient);
						ingredientCASNumberIndex.put(casNumber, sourceIngredient);
					}
				}
			}
		}
		else {
			sourceIngredients = new ArrayList<SourceIngredient>();
			ingredientNameIndex.put(ingredientNameNoSpaces, sourceIngredients);
			sourceIngredient = new SourceIngredient(ingredientName, ingredientNameEnglish, casNumber);
			sourceIngredients.add(sourceIngredient);
			ingredientCASNumberIndex.put(casNumber, sourceIngredient);
		}
		
		allIngredients.add(sourceIngredient);

		return error ? null : sourceIngredient;
	}
	
	
	public static boolean errorOccurred() {
		return error;
	}
	
	
	public static Integer getCASNumbersSet() {
		return casNumbersSet;
	}
	
	
	public static String getHeader() {
		String header = "SourceCode";
		header += "," + "SourceName";
		header += "," + "SourceATCCode";
		header += "," + "SourceFormulation";
		header += "," + "SourceCount";
		return header;
	}
	
	
	public static void writeHeaderToFile(PrintWriter file) {
		file.println(getHeader());
	}
	
	
	public static String emptyRecord() {
		String[] headerSplit = getHeader().split(",");
		String emptyRecord = "";
		for (int commaNr = 0; commaNr < (headerSplit.length - 1); commaNr++) {
			emptyRecord += ",";
		}
		return emptyRecord;
	}
	
	
	public SourceDrug(String sourceCode, String sourceName, String sourceATCCode, String formulation, String count) {
		this.code = sourceCode.equals("") ? null : sourceCode;
		this.name = sourceName.equals("") ? null : sourceName;
		this.atcCode = sourceATCCode.equals("") ? null : sourceATCCode;
		this.formulation = formulation.equals("") ? null : formulation;
		try {
			this.count = Integer.valueOf(count);
		}
		catch (NumberFormatException e) {
			this.count = -1;
		}
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
	
	
	public Integer getCount() {
		return count;
	}
	
	
	public CDMDrug getMappedDrug() {
		return mappedDrug;
	}
	
	
	public SourceIngredient AddIngredientByCASnumber(String ingredientName, String ingredientNameEnglish, String casNumber, String dosage, String dosageUnit) {
		SourceIngredient sourceIngredient = null;
		
		sourceIngredient = SourceDrug.findIngredient(casNumber);
		if (sourceIngredient == null) {
			sourceIngredient = new SourceIngredient(ingredientName, ingredientNameEnglish, casNumber);
			allIngredients.add(sourceIngredient);
			if ((casNumber != null) && (!casNumber.equals(""))) {
				ingredientCASNumberIndex.put(casNumber, sourceIngredient);
			}
		}
		sourceIngredient.addCount(getCount());

		return AddIngredient(sourceIngredient, dosage, dosageUnit);
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
				List<SourceIngredient> sourceIngredients = ingredientNameIndex.get(sourceIngredient.getIngredientNameNoSpaces());
				if (sourceIngredients == null) {
					sourceIngredients = new ArrayList<SourceIngredient>();
				}
				sourceIngredients.add(sourceIngredient);
				if (sourceIngredient.getCASNumber() != null) {
					ingredientCASNumberIndex.put(sourceIngredient.getCASNumber(), sourceIngredient);
				}
			}
		}
		sourceIngredient.addCount(getCount());

		return AddIngredient(sourceIngredient, dosage, dosageUnit);
	}
	
	
	public SourceIngredient AddIngredient(SourceIngredient sourceIngredient, String dosage, String dosageUnit) {
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
	
	
	public String getIngredientNumeratorDosageUnit(SourceIngredient ingredient) {
		String numeratorDosageUnit = null;
		SourceDrugComponent component = getIngredientComponent(ingredient);
		if (component != null) {
			numeratorDosageUnit = component.getNumeratorDosageUnit();
		}
		return numeratorDosageUnit;
	}
	
	
	public String getIngredientDenominatorDosageUnit(SourceIngredient ingredient) {
		String denominatorDosageUnit = null;
		SourceDrugComponent component = getIngredientComponent(ingredient);
		if (component != null) {
			denominatorDosageUnit = component.getDenominatorDosageUnit();
		}
		return denominatorDosageUnit;
	}
	
	
	public List<SourceDrugComponent> getComponents() {
		return components;
	}
	
	
	public List<SourceIngredient> getIngredients() {
		List<SourceIngredient> ingredients = new ArrayList<SourceIngredient>();
		for (SourceDrugComponent component : components) {
			if ((component.getIngredient() != null) && (!ingredients.contains(component.getIngredient()))) {
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
		description += "," + (count == null ? "" : count);
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
