package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class SourceDrug {
	private static boolean error = false;
	
	private static Set<SourceDrugComponent> allComponents = new HashSet<SourceDrugComponent>();
	private static Set<SourceIngredient> allIngredients = new HashSet<SourceIngredient>();
	private static Map<String, Long> allUnits = new HashMap<String, Long>();
	private static Set<String> allForms = new HashSet<String>();
	
	private static Map<String, SourceIngredient> ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
	
	private static Map<String, Set<SourceDrug>> unitsUsedInSourceDrug = new HashMap<String, Set<SourceDrug>>(); 
	
	private static Integer casNumbersSet = 0;
	
	
	private String code = null;
	private String name = null;
	private List<String> atcCodeList = new ArrayList<String>();
	private String formulation = null;
	private Long count = null;
	private List<SourceDrugComponent> components = new ArrayList<SourceDrugComponent>();
	private String matchString = "";
	
	
	public static void init() {
		error = false;
		
		allComponents = new HashSet<SourceDrugComponent>();
		allIngredients = new HashSet<SourceIngredient>();
		ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
		
		casNumbersSet = 0;
	}
	
	
	public static Set<SourceDrugComponent> getAllComponents() {
		return allComponents;
	}
	
	
	public static Set<SourceIngredient> getAllIngredients() {
		return allIngredients;
	}
	
	
	public static Set<String> getAllUnits() {
		return allUnits.keySet();
	}
	
	
	public static Set<String> getAllForms() {
		return allForms;
	}
	
	
	public static Integer getUnitSourceDrugUsage(String unit) {
		Integer usage = 0;
		Set<SourceDrug> sourceDrugUsage = unitsUsedInSourceDrug.get(unit);
		if (sourceDrugUsage != null) {
			usage = sourceDrugUsage.size();
		}
		return usage;
	}
	
	
	public static Long getUnitRecordUsage(String unit) {
		Long usage = allUnits.get(unit);
		return usage == null ? 0L : usage;
	}
	
	
	public static SourceIngredient getIngredient(String ingredientCode, String ingredientName, String ingredientNameEnglish, String casNumber) {
		error = false;
		SourceIngredient sourceIngredient = null;
		
		if ((ingredientCode != null) && (!ingredientCode.equals(""))) {
			sourceIngredient = ingredientSourceCodeIndex.get(ingredientCode);
		}
		if (sourceIngredient == null) {
			sourceIngredient = new SourceIngredient(ingredientCode, ingredientName, ingredientNameEnglish, casNumber);

			ingredientSourceCodeIndex.put(ingredientCode, sourceIngredient);
			allIngredients.add(sourceIngredient);
		}

		return error ? null : sourceIngredient;
	}
	
	
	public static SourceIngredient getIngredient(String ingredientCode) {
		return ingredientSourceCodeIndex.get(ingredientCode);
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
	
	
	public SourceDrug(String sourceCode, String sourceName, String sourceATCCodes, String formulation, String count) {
		this.code = sourceCode.equals("") ? null : sourceCode;
		this.name = sourceName.equals("") ? null : sourceName;
		if ((sourceATCCodes != null) && (!sourceATCCodes.equals(""))) {
			String[] sourceATCCodesSplit = sourceATCCodes.split("\\|");
			for (String sourceATCode : sourceATCCodesSplit) {
				if (!sourceATCode.equals("")) {
					atcCodeList.add(sourceATCode);
				}
			}
		}
		this.formulation = formulation.equals("") ? null : formulation;
		try {
			this.count = Long.valueOf(count);
		}
		catch (NumberFormatException e) {
			this.count = -1L;
		}
		if (this.formulation != null) {
			allForms.add(this.formulation);
		}
	}
	
	
	public String getCode() {
		return code;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public List<String> getATCCodes() {
		return atcCodeList;
	}
	
	
	public String getFormulation() {
		return formulation;
	}
	
	
	public Long getCount() {
		return count;
	}
	
	
	public String getMatchString() {
		return matchString;
	}
	
	
	public void setMatchString(String matchString) {
		this.matchString = matchString;
	}
	
	
	public SourceIngredient AddIngredient(SourceIngredient sourceIngredient, String dosage, String dosageUnit) {
		SourceDrugComponent sourceComponent = new SourceDrugComponent(this, sourceIngredient, dosage, dosageUnit);
		allComponents.add(sourceComponent);
		components.add(sourceComponent);
		// Keep the components sorted by ingredient code
		Collections.sort(components, new Comparator<SourceDrugComponent>() {
			@Override
			public int compare(SourceDrugComponent component1, SourceDrugComponent component2) {
				return component1.getIngredient().getIngredientCode().compareTo(component2.getIngredient().getIngredientCode());
			}
		});
		sourceIngredient.addCount(getCount());
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
			if (component.getIngredient() != null) {
				ingredients.add(component.getIngredient());
			}
		}
		return ingredients;
	}
	
	
	public void countUnit(String unit) {
		Set<SourceDrug> sourceDrugUsage = unitsUsedInSourceDrug.get(unit);
		if (sourceDrugUsage == null) {
			sourceDrugUsage = new HashSet<SourceDrug>();
			unitsUsedInSourceDrug.put(unit, sourceDrugUsage);
		}
		if (sourceDrugUsage.add(this)) {
			Long recordUsage = allUnits.get(unit);
			allUnits.put(unit, (recordUsage == null ? 0 : recordUsage) + getCount());
		}
	}
	
	
	public String toString() {
		String description = (code == null ? "" : DrugMappingStringUtilities.escapeFieldValue(code));
		description += "," + (name == null ? "" : DrugMappingStringUtilities.escapeFieldValue(name));
		String atcCodes = "";
		for (String atcCode : atcCodeList) {
			atcCodes += atcCodes + (atcCode.equals("") ? "" : "|") + atcCode;
		}
		description += "," + (atcCodes == null ? "" : DrugMappingStringUtilities.escapeFieldValue(atcCodes));
		description += "," + (formulation == null ? "" : DrugMappingStringUtilities.escapeFieldValue(formulation));
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
