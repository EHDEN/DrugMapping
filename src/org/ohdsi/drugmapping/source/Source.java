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
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class Source {
	private static Set<SourceDrugComponent> allComponents = new HashSet<SourceDrugComponent>();
	private static Set<SourceIngredient> allIngredients = new HashSet<SourceIngredient>();
	private static Map<String, Long> allUnits = new HashMap<String, Long>();
	private static Set<String> allForms = new HashSet<String>();
	
	private static Map<String, SourceIngredient> ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
	
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
	
	
	public static Set<String> getAllUnits() {
		return allUnits.keySet();
	}
	
	
	public static Set<String> getAllForms() {
		return allForms;
	}
	
	
	public static void addForms(List<String> forms) {
		allForms.addAll(forms);
	}
	
	
	public static Integer getCASNumbersSet() {
		return casNumbersSet;
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
	private List<SourceDrug> missingATC;
	
	
	public boolean loadSourceDrugs(InputFile sourceDrugsFile, List<String> report) {
		allComponents = new HashSet<SourceDrugComponent>();
		allIngredients = new HashSet<SourceIngredient>();
		ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
		
		casNumbersSet = 0;
		
		missingATC = new ArrayList<SourceDrug>();
		
		return load(sourceDrugsFile, DrugMapping.settings.getLongSetting(MainFrame.MINIMUM_USE_COUNT), report);
	}
	
	
	public boolean loadSourceDrugs(InputFile sourceDrugsFile, long minimumUseCount) {
		allComponents = new HashSet<SourceDrugComponent>();
		allIngredients = new HashSet<SourceIngredient>();
		ingredientSourceCodeIndex = new HashMap<String, SourceIngredient>();
		
		casNumbersSet = 0;
		
		return load(sourceDrugsFile, minimumUseCount, null);
	}
	
	
	private boolean load(InputFile sourceDrugsFile, long minimumUseCount, List<String> report) {
		boolean sourceDrugError = false;
		
		forms = new HashSet<String>();
		units = new HashSet<String>();
		
		sourceDrugs = new ArrayList<SourceDrug>();
		sourceDrugMap = new HashMap<String, SourceDrug>();
		
		Integer sourceDrugCount = 0;
		Set<String> ignoredSourceCodes = new HashSet<String>();
		
		System.out.println(DrugMapping.getCurrentTime() + "     Loading source drugs ...");
		
		try {
			if (sourceDrugsFile.openFile()) {
				
				while (sourceDrugsFile.hasNext()) {
					Row row = sourceDrugsFile.next();
					
					String sourceCode = sourceDrugsFile.get(row, "SourceCode", true).trim();
					
					if ((!sourceCode.equals("")) && (!ignoredSourceCodes.contains(sourceCode))) {
						SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
						
						if (sourceDrug == null) {
							sourceDrugCount++;
							sourceDrug = new SourceDrug(
												sourceCode, 
												DrugMappingStringUtilities.cleanString(sourceDrugsFile.get(row, "SourceName", true)).toUpperCase(), 
												DrugMappingStringUtilities.cleanString(sourceDrugsFile.get(row, "SourceATCCode", true)).toUpperCase(), 
												DrugMappingStringUtilities.cleanString(sourceDrugsFile.get(row, "SourceFormulation", true)).toUpperCase(), 
												sourceDrugsFile.get(row, "SourceCount", true).trim()
												);
							if (sourceDrug.getCount() >= minimumUseCount) {
								sourceDrugs.add(sourceDrug);
								sourceDrugMap.put(sourceCode, sourceDrug);
								
								forms.addAll(sourceDrug.getFormulations());
								
								//System.out.println("    " + sourceDrug);
								
								if ((sourceDrug.getATCCodes() == null) && (sourceDrug.getATCCodes().size() == 0)) {
									missingATC.add(sourceDrug);
								}
							}
							else {
								sourceDrug = null;
								ignoredSourceCodes.add(sourceCode);
							}
						}

						if (sourceDrug != null) {
							String ingredientCode        = sourceDrugsFile.get(row, "IngredientCode", false).trim().toUpperCase(); 
							String ingredientName        = sourceDrugsFile.get(row, "IngredientName", true).trim().toUpperCase().replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll("\r", " ");
							String ingredientNameEnglish = ""; //sourceDrugsFile.get(row, "IngredientNameEnglish", true).trim().toUpperCase();
							String dosage                = sourceDrugsFile.get(row, "Dosage", true).trim(); 
							String dosageUnit            = sourceDrugsFile.get(row, "DosageUnit", true).trim(); 
							String casNumber             = sourceDrugsFile.get(row, "CASNumber", true).trim();
							
							if (ingredientCode != null) ingredientCode = ingredientCode.trim(); 
							ingredientName        = DrugMappingStringUtilities.cleanString(ingredientName).toUpperCase();
							ingredientNameEnglish = DrugMappingStringUtilities.cleanString(ingredientNameEnglish).toUpperCase();
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
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	public void saveMissingATCToFile() {
		String fileName = "";

		PrintWriter missingATCFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "DrugMapping Missing ATC.csv";
			missingATCFile = new PrintWriter(new File(fileName));
			SourceDrug.writeHeaderToFile(missingATCFile);
		} 
		catch (FileNotFoundException e) {
			System.out.println("       ERROR: Cannot create output file '" + fileName + "'");
			missingATCFile = null;
		}
		
		for (SourceDrug sourceDrug : missingATC) {
			if (missingATCFile != null) {
				sourceDrug.writeDescriptionToFile("", missingATCFile);
			}
		}
		
		if (missingATCFile != null) {
			missingATCFile.close();
		}
		
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
