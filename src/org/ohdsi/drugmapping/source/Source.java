package org.ohdsi.drugmapping.source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.gui.files.DelimitedInputFileGUI;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class Source {
	private static Set<SourceDrugComponent> allComponents = new HashSet<SourceDrugComponent>();
	private static Set<SourceIngredient> allIngredients = new HashSet<SourceIngredient>();
	private static Map<String, Long> allUnits = new HashMap<String, Long>();
	private static Map<String, Long> allForms = new HashMap<String, Long>();
	
	private static Map<String, SourceIngredient> ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();

	private static Map<String, Set<SourceDrug>> formsUsedInSourceDrug = new HashMap<String, Set<SourceDrug>>();
	private static Map<String, Set<SourceDrug>> unitsUsedInSourceDrug = new HashMap<String, Set<SourceDrug>>();
	
	private static Integer casNumbersSet = 0;
	
	
	public static Set<SourceDrugComponent> getAllComponents() {
		return allComponents;
	}
	
	
	public static void addComponent(SourceDrugComponent sourceComponent) {
		allComponents.add(sourceComponent);
	}
	
	
	public static Set<SourceIngredient> getAllIngredients() {
		return allIngredients;
	}
	
	
	public static void addIngredient(SourceIngredient sourceIngredient) {
		ingredientSourceCodeIndex.put(sourceIngredient.getIngredientCode(), sourceIngredient);
		allIngredients.add(sourceIngredient);
	}
	
	
	public static SourceIngredient getIngredient(String sourceIngredientCode) {
		return ingredientSourceCodeIndex.get(sourceIngredientCode);
	}
	
	
	public static Set<String> getAllForms() {
		return allForms.keySet();
	}
	
	
	public static void addForms(List<String> forms, SourceDrug sourceDrug) {
		for (String form : forms) {
			Long recordUseCount = allForms.get(form);
			allForms.put(form, recordUseCount == null ? sourceDrug.getCount() : (recordUseCount + sourceDrug.getCount()));
			Set<SourceDrug> sourceDrugUsage = formsUsedInSourceDrug.get(form);
			if (sourceDrugUsage == null) {
				sourceDrugUsage = new HashSet<SourceDrug>();
				formsUsedInSourceDrug.put(form, sourceDrugUsage);
			}
			sourceDrugUsage.add(sourceDrug);
		}
	}
	
	
	public static Integer getFormSourceDrugUsage(String unit) {
		Integer usage = 0;
		Set<SourceDrug> sourceDrugUsage = formsUsedInSourceDrug.get(unit);
		if (sourceDrugUsage != null) {
			usage = sourceDrugUsage.size();
		}
		return usage;
	}
	
	
	public static Long getFormRecordUsage(String form) {
		Long usage = allForms.get(form);
		return usage == null ? 0L : usage;
	}
	
	
	public static Set<String> getAllUnits() {
		return allUnits.keySet();
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
	
	
	public static Integer getCASNumbersSet() {
		return casNumbersSet;
	}
	
	
	public static void countUnit(String unit, SourceDrug sourceDrug) {
		Set<SourceDrug> sourceDrugUsage = unitsUsedInSourceDrug.get(unit);
		if (sourceDrugUsage == null) {
			sourceDrugUsage = new HashSet<SourceDrug>();
			unitsUsedInSourceDrug.put(unit, sourceDrugUsage);
		}
		if (sourceDrugUsage.add(sourceDrug)) {
			Long recordUsage = allUnits.get(unit);
			allUnits.put(unit, (recordUsage == null ? 0 : recordUsage) + sourceDrug.getCount());
		}
	}
	
	private Set<String> forms;
	private Set<String> units;
	
	private List<SourceDrug> sourceDrugs;
	private Map<String, SourceDrug> sourceDrugMap;
	private List<SourceDrug> missingATC = new ArrayList<SourceDrug>();
	
	
	public boolean loadSourceDrugs(DelimitedInputFileGUI sourceDrugsFile, List<String> report) {
		allComponents = new HashSet<SourceDrugComponent>();
		allIngredients = new HashSet<SourceIngredient>();
		ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
		
		casNumbersSet = 0;
		
		return load(sourceDrugsFile, DrugMapping.settings.getLongSetting(MainFrame.MINIMUM_USE_COUNT), report);
	}
	
	
	public boolean loadSourceDrugs(DelimitedInputFileGUI sourceDrugsFile, long minimumUseCount) {
		allComponents = new HashSet<SourceDrugComponent>();
		allIngredients = new HashSet<SourceIngredient>();
		ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
		
		casNumbersSet = 0;
		
		return load(sourceDrugsFile, minimumUseCount, null);
	}
	
	
	private boolean load(DelimitedInputFileGUI sourceDrugsFile, long minimumUseCount, List<String> report) {
		boolean sourceDrugError = false;
		
		forms = new HashSet<String>();
		units = new HashSet<String>();
		
		sourceDrugs = new ArrayList<SourceDrug>();
		sourceDrugMap = new HashMap<String, SourceDrug>();
		
		missingATC.clear();
		
		Integer sourceDrugCount = 0;
		Set<String> ignoredSourceCodes = new HashSet<String>();
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Loading source drugs ...");
		
		try {
			if (sourceDrugsFile.openFileForReading()) {
				
				while (sourceDrugsFile.hasNext()) {
					DelimitedFileRow row = sourceDrugsFile.next();
					
					String sourceCode = sourceDrugsFile.get(row, "SourceCode", true).trim();
					
					if ((!sourceCode.equals("")) && (!ignoredSourceCodes.contains(sourceCode))) {
						SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
						
						if (sourceDrug == null) {
							sourceDrugCount++;
							
							String sourceName = DrugMappingStringUtilities.cleanString(sourceDrugsFile.get(row, "SourceName", true));
							String sourceATC = DrugMappingStringUtilities.uniformATCCode(DrugMappingStringUtilities.cleanString(sourceDrugsFile.get(row, "SourceATCCode", true)));
							String sourceFormulation = DrugMappingStringUtilities.cleanString(sourceDrugsFile.get(row, "SourceFormulation", true));
							String sourceCount = sourceDrugsFile.get(row, "SourceCount", true);
							
							sourceName = sourceName == null ? null : DrugMappingStringUtilities.safeToUpperCase(sourceName);
							sourceATC = sourceATC == null ? null : DrugMappingStringUtilities.safeToUpperCase(sourceATC);
							sourceFormulation = sourceFormulation == null ? null : DrugMappingStringUtilities.safeToUpperCase(sourceFormulation);
							sourceCount = sourceCount == null ? "0" : sourceCount.trim();
							
							sourceDrug = new SourceDrug(sourceCode, sourceName, sourceATC, sourceFormulation, sourceCount);
							
							if (sourceDrug.getCount() >= minimumUseCount) {
								sourceDrugs.add(sourceDrug);
								sourceDrugMap.put(sourceCode, sourceDrug);
								
								forms.addAll(sourceDrug.getFormulations());
								
								//System.out.println("    " + sourceDrug);
								
								if ((sourceDrug.getATCCodes() == null) || (sourceDrug.getATCCodes().size() == 0)) {
									missingATC.add(sourceDrug);
								}
							}
							else {
								sourceDrug = null;
								ignoredSourceCodes.add(sourceCode);
							}
						}

						if (sourceDrug != null) {
							String ingredientCode        = sourceDrugsFile.get(row, "IngredientCode", true); 
							String ingredientName        = sourceDrugsFile.get(row, "IngredientName", true);
							String ingredientNameEnglish = sourceDrugsFile.get(row, "IngredientNameEnglish", false);
							String dosage                = sourceDrugsFile.get(row, "Dosage", true); 
							String dosageUnit            = sourceDrugsFile.get(row, "DosageUnit", true); 
							String casNumber             = sourceDrugsFile.get(row, "CASNumber", true);

							ingredientCode        = ingredientCode == null ? null : DrugMappingStringUtilities.safeToUpperCase(ingredientCode.trim()); 
							ingredientName        = ingredientName == null ? "" : DrugMappingStringUtilities.safeToUpperCase(ingredientName.trim()).replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll("\r", " ");
							ingredientNameEnglish = ingredientNameEnglish == null ? "" : ingredientNameEnglish.trim();
							dosage                = dosage == null ? "" : dosage.trim(); 
							dosageUnit            = dosageUnit == null ? "" : dosageUnit.trim(); 
							casNumber             = casNumber == null ? "" : casNumber.trim();
							
							ingredientName        = DrugMappingStringUtilities.safeToUpperCase(DrugMappingStringUtilities.cleanString(ingredientName));
							ingredientNameEnglish = ingredientNameEnglish == null ? "" : DrugMappingStringUtilities.safeToUpperCase(DrugMappingStringUtilities.cleanString(ingredientNameEnglish));
							casNumber = DrugMappingNumberUtilities.uniformCASNumber(casNumber);

							SourceIngredient sourceIngredient = null;
							if (!ingredientName.equals("")) {
								sourceIngredient = SourceDrug.getIngredient(ingredientCode, ingredientName, ingredientNameEnglish, casNumber);
								if (sourceIngredient == null) {
									sourceDrugError = true;
								}
								else {
									sourceIngredient = sourceDrug.AddIngredient(sourceIngredient, dosage, dosageUnit);
								}
								
								String numeratorDosageUnit = sourceDrug.getIngredientNumeratorDosageUnit(sourceIngredient);
								if (numeratorDosageUnit != null) {
									units.add(numeratorDosageUnit);
								}
								
								String denominatorDosageUnit = sourceDrug.getIngredientDenominatorDosageUnit(sourceIngredient);
								if (denominatorDosageUnit != null) {
									units.add(denominatorDosageUnit);
								}
							}
							
							if (sourceIngredient != null) {
								String numeratorDosageUnit = sourceDrug.getIngredientNumeratorDosageUnit(sourceIngredient);
								if (numeratorDosageUnit != null) {
									units.add(numeratorDosageUnit);
								}
								
								String denominatorDosageUnit = sourceDrug.getIngredientDenominatorDosageUnit(sourceIngredient);
								if (denominatorDosageUnit != null) {
									units.add(denominatorDosageUnit);
								}
							}
						}
					}
				}
				
				if (report != null) {
					report.add("Source drugs: " + sourceDrugCount);
				}
				List<SourceDrug> sourceDrugsToBeRemoved = new ArrayList<SourceDrug>();
				for (SourceDrug sourceDrug : sourceDrugs) {
					if (sourceDrug.getCount() < minimumUseCount) {
						sourceDrugsToBeRemoved.add(sourceDrug);
					}
				}
				sourceDrugs.removeAll(sourceDrugsToBeRemoved);
				
				if (report != null) {
					report.add("Source drugs with a miminum use count of " + Long.toString(minimumUseCount) + ": " + DrugMappingNumberUtilities.percentage((long) sourceDrugs.size(), (long) sourceDrugCount));
					report.add("Source drugs without ATC: " + DrugMappingNumberUtilities.percentage((long) missingATC.size(), (long) sourceDrugs.size()));
					report.add("Source ingredients: " + Integer.toString(getAllIngredients().size()));
				}
			}
			else {
				sourceDrugError = true;
			}
		}
		catch (NoSuchElementException fileException) {
			System.out.println("  ERROR: " + fileException.getMessage());
			sourceDrugError = true;
		}

		if (report != null) {
			report.add("");
		}
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	public Set<String> getForms() {
		return forms;
	}
	
	
	public Set<String> getUnits() {
		return units;
	}
	
	
	public List<SourceDrug> getSourceDrugs() {
		return sourceDrugs;
	}
	
	
	public SourceDrug getSourceDrug(String sourceDrugCode) {
		return sourceDrugMap.get(sourceDrugCode);
	}

}
