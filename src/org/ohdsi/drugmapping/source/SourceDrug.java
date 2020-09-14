package org.ohdsi.drugmapping.source;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class SourceDrug {
	private static boolean error = false;
	
	
	private String code = null;
	private String name = null;
	private List<String> atcCodeList = new ArrayList<String>();
	private List<String> formulationsList = new ArrayList<String>();
	private Long count = null;
	private List<SourceDrugComponent> components = new ArrayList<SourceDrugComponent>();
	private String matchString = "";
	
	
	public static SourceIngredient getIngredient(String ingredientCode, String ingredientName, String ingredientNameEnglish, String casNumber) {
		error = false;
		SourceIngredient sourceIngredient = null;
		
		if ((ingredientCode != null) && (!ingredientCode.equals(""))) {
			sourceIngredient = Source.getIngredient(ingredientCode);
		}
		if (sourceIngredient == null) {
			sourceIngredient = new SourceIngredient(ingredientCode, ingredientName, ingredientNameEnglish, casNumber);

			Source.addIngredient(sourceIngredient);
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
	
	
	public SourceDrug(String sourceCode, String sourceName, String sourceATCCodes, String sourceFormulations, String count) {
		this.code = sourceCode.equals("") ? null : sourceCode;
		this.name = sourceName.equals("") ? null : sourceName;
		if ((sourceATCCodes != null) && (!sourceATCCodes.equals(""))) {
			String[] sourceATCCodesSplit = sourceATCCodes.split("\\|");
			for (String sourceATCCode : sourceATCCodesSplit) {
				if ((!sourceATCCode.equals("")) && (!atcCodeList.contains(sourceATCCode))) {
					atcCodeList.add(sourceATCCode);
				}
			}
		}
		if ((sourceFormulations != null) && (!sourceFormulations.equals(""))) {
			String[] sourceFormulationsSplit = sourceFormulations.split("\\|");
			for (String sourceFormulation : sourceFormulationsSplit) {
				if ((!sourceFormulation.equals("")) && (!formulationsList.contains(sourceFormulation))) {
					formulationsList.add(sourceFormulation);
				}
			}
		}
		try {
			this.count = Long.valueOf(count);
			if (this.count < 0L) {
				this.count = 0L;
			}
		}
		catch (NumberFormatException e) {
			this.count = -1L;
		}
		if (this.formulationsList != null) {
			Source.addForms(formulationsList);
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
	
	
	public String getATCCodesString() {
		String atcCodes = "";
		for (String atcCode : atcCodeList) {
			atcCodes += (atcCodes.equals("") ? "" : "|") + atcCode;
		}
		return atcCodes;
	}
	
	
	public List<String> getFormulations() {
		return formulationsList;
	}
	
	
	public String getFormulationsString() {
		String formulations = "";
		for (String formulation : formulationsList) {
			formulations += (formulations.equals("") ? "" : "|") + formulation;
		}
		return formulations;
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
		Source.addComponent(sourceComponent);
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
	
	
	public String toString() {
		String description = (code == null ? "" : DrugMappingStringUtilities.escapeFieldValue(code));
		description += "," + (name == null ? "" : DrugMappingStringUtilities.escapeFieldValue(name));
		description += "," + DrugMappingStringUtilities.escapeFieldValue(getATCCodesString());
		description += "," + DrugMappingStringUtilities.escapeFieldValue(getFormulationsString());
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
