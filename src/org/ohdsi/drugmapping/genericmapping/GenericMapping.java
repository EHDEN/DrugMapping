package org.ohdsi.drugmapping.genericmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.FormConversion;
import org.ohdsi.drugmapping.IngredientNameTranslation;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.UnitConversion;
import org.ohdsi.drugmapping.cdm.CDM;
import org.ohdsi.drugmapping.cdm.CDMConcept;
import org.ohdsi.drugmapping.cdm.CDMDrug;
import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.cdm.CDMIngredientStrength;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class GenericMapping extends Mapping {

	// Mapping types
	// The mapping type values should start at 0 and incremented by 1.
	private static int CLINICAL_DRUG_MAPPING      = 0;
	private static int CLINICAL_DRUG_COMP_MAPPING = 1;
	private static int CLINICAL_DRUG_FORM_MAPPING = 2;
	private static int INGREDIENT_MAPPING         = 3;
	
	private static Map<Integer, String> mappingTypeDescriptions;
	static {
		mappingTypeDescriptions = new HashMap<Integer, String>();
		mappingTypeDescriptions.put(CLINICAL_DRUG_MAPPING     , "ClinicalDrug Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_COMP_MAPPING, "ClinicalDrugComp Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_FORM_MAPPING, "ClinicalDrugForm Mapping");
		mappingTypeDescriptions.put(INGREDIENT_MAPPING        , "Ingredient Mapping");
	}
	
	// Mapping result types.
	// The mapping result type values for each mapping type should start at 10 times
	// the mapping type value and incremented by 1.
	// Make sure that the MAPPED result type is the last one for each mapping type and
	// that there is a gap between the last result type value of one mapping type and
	// the first result type value of the next mapping type.
	private static int NO_SOURCE_INGREDIENTS                          =  0; // The source drug has no ingredients
	private static int UNMAPPED_SOURCE_INGREDIENTS                    =  1; // The source drug has unmapped ingredients
	private static int DOUBLE_INGREDIENT_MAPPING                      =  2; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int NO_SINGLE_INGREDIENT_DRUG                      =  3; // The source drug is not a single ingredient drug.
	private static int NO_DRUGS_WITH_MATCHING_INGREDIENTS             =  4; // There is no CDM drug with matching ingredients.
	private static int REJECTED_BY_FORM                               =  5; // The CDM drugs are rejected because they have a different form than the source drug.
	private static int REJECTED_BY_STRENGTH                           =  6; // The CDM drugs are rejected because they have a different strength than the source drug.
	private static int REJECTED_BY_RXNORM_PREFERENCE                  =  7; // The CDM drugs are rejected because they are not in the preferred RxNorm vocabulary.
	private static int REJECTED_BY_RXNORM_EXTENSION_PREFERENCE        =  8; // The CDM drugs are rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int REJECTED_BY_LATEST_DATE_PREFERENCE             =  9; // The CDM drugs are rejected because they do not have the latest valid start date.
	private static int REJECTED_BY_EARLIEST_DATE_PREFERENCE           = 10; // The CDM drugs are rejected because they do not have the earliest recent valid start date.
	private static int REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE      = 11; // The CDM drugs are rejected because they do not have the smallest concept_id.
	private static int REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE      = 12; // The CDM drugs are rejected because they do not have the greatest concept_id.
	private static int NO_UNIQUE_MAPPING                              = 13; // There are several CDM drugs the source drug could be mapped to.
	private static int REJECTED_BY_FIRST_PREFERENCE                   = 14; // The CDM drugs are rejected because the first one found is taken.
	private static int REJECTED_BY_LAST_PREFERENCE                    = 15; // The CDM drugs are rejected because the last one found is taken.
	private static int OVERRULED_MAPPING                              = 16; // A mapping to a single CDM drug or a failing mapping is overruled by a manual mapping.
	private static int MAPPED                                         = 17; // The final mapping of the source drug to a CDM drug.

	private static Map<Integer, String> mappingResultDescriptions;
	static {
		mappingResultDescriptions = new HashMap<Integer  , String>();
		mappingResultDescriptions.put(NO_SOURCE_INGREDIENTS                         , "No source ingredients");
		mappingResultDescriptions.put(UNMAPPED_SOURCE_INGREDIENTS                   , "Unmapped source ingredients");
		mappingResultDescriptions.put(DOUBLE_INGREDIENT_MAPPING                     , "Double ingredient mapping");
		mappingResultDescriptions.put(NO_SINGLE_INGREDIENT_DRUG                     , "No single ingredient drug");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_INGREDIENTS            , "No drugs with matching ingredients");
		mappingResultDescriptions.put(REJECTED_BY_FORM                              , "Rejected by form");
		mappingResultDescriptions.put(REJECTED_BY_STRENGTH                          , "Rejected by strength");
		mappingResultDescriptions.put(REJECTED_BY_RXNORM_PREFERENCE                 , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(REJECTED_BY_RXNORM_EXTENSION_PREFERENCE       , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(REJECTED_BY_LATEST_DATE_PREFERENCE            , "Rejected by latest valid start date");
		mappingResultDescriptions.put(REJECTED_BY_EARLIEST_DATE_PREFERENCE          , "Rejected by earliest valid start date");
		mappingResultDescriptions.put(REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE     , "Rejected by smallest concept_id");
		mappingResultDescriptions.put(REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE     , "Rejected by greatest concept_id");
		mappingResultDescriptions.put(NO_UNIQUE_MAPPING                             , "No unique mapping");
		mappingResultDescriptions.put(REJECTED_BY_FIRST_PREFERENCE                  , "Rejected because first is used");
		mappingResultDescriptions.put(REJECTED_BY_LAST_PREFERENCE                   , "Rejected because last is used");
		mappingResultDescriptions.put(OVERRULED_MAPPING                             , "Overruled mapping");
		mappingResultDescriptions.put(MAPPED                                        , "Mapped");
	}
	
	private Set<String> forms = null;
	private Set<String> units = null;

	private Map<String, CDMIngredient> manualCASMappings = null;
	private Map<SourceIngredient, CDMIngredient> manualIngredientCodeMappings = null;
	private Map<SourceIngredient, String> manualIngredientCodeMappingRemarks = null;
	private Map<String, CDMIngredient> manualIngredientNameMappings = null;
	private Map<SourceDrug, CDMDrug> manualDrugMappings = null;
	
	private CDM cdm = null;
	
	private UnitConversion unitConversionsMap = null;
	private FormConversion formConversionsMap = null;
	private IngredientNameTranslation ingredientNameTranslationMap = null;
	
	private Map<String, List<String>> externalCASSynonymsMap = null;
	
	private List<SourceDrug> sourceDrugs = null;
	private Map<String, SourceDrug> sourceDrugMap = null;
	
	private List<SourceDrug> sourceDrugsAllIngredientsMapped = null;
	private Map<SourceDrug, List<CDMIngredient>> sourceDrugsCDMIngredients = null;
	
	private Map<SourceIngredient, CDMIngredient> ingredientMap = null;
	
	private Set<SourceDrug> mappedSourceDrugs = null;
	private Map<String, Double> usedStrengthDeviationPercentageMap = null;
	
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> sourceDrugMappingResults = null;
	private Map<Integer, Set<SourceDrug>> notUniqueMapping = null;
	
	private Map<Integer, Map<Integer, Long>> counters;
	private Map<Integer, Map<Integer, Long>> dataCoverage;
	
	private List<String> report = null;
	
	private String preferencesUsed = null;
	private int maxResultConcepts = 0;
	
	
		
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile casFile, InputFile manualCASMappingFile, InputFile manualIngredientMappingFile, InputFile manualDrugMappingFile) {
		boolean ok = true;
		
		forms = new HashSet<String>();
		units = new HashSet<String>();

		manualCASMappings = new HashMap<String, CDMIngredient>();
		manualIngredientCodeMappings = new HashMap<SourceIngredient, CDMIngredient>();
		manualIngredientCodeMappingRemarks = new HashMap<SourceIngredient, String>();
		manualIngredientNameMappings = new HashMap<String, CDMIngredient>();
		manualDrugMappings = new HashMap<SourceDrug, CDMDrug>();
		
		sourceDrugs = new ArrayList<SourceDrug>();
		sourceDrugMap = new HashMap<String, SourceDrug>();
		
		sourceDrugsAllIngredientsMapped = new ArrayList<SourceDrug>();
		sourceDrugsCDMIngredients = new HashMap<SourceDrug, List<CDMIngredient>>();
		
		ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
		
		mappedSourceDrugs = new HashSet<SourceDrug>();
		usedStrengthDeviationPercentageMap = new HashMap<String, Double>();
		
		sourceDrugMappingResults = new HashMap<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>>(); // SourceDrug, Mapping, List of Mapping result, List of options
		notUniqueMapping = new HashMap<Integer, Set<SourceDrug>>();
		
		report = new ArrayList<String>();
		
		maxResultConcepts = 0;
		
		int mapping = 0;
		while (mappingTypeDescriptions.containsKey(mapping)) {
			notUniqueMapping.put(mapping, new HashSet<SourceDrug>());
			mapping++;
		}

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");
		SourceDrug.init();
		
		// Load source drug ingredient mapping
		ok = ok && getSourceDrugs(sourceDrugsFile, DrugMapping.settings.getLongSetting(MainFrame.MINIMUM_USE_COUNT)) && (!SourceDrug.errorOccurred());
		
		// Get CDM Ingredients
		cdm = new CDM(database, report);
		ok = ok && cdm.isOK();		
		
		// Get unit conversion from local units to CDM units
		boolean unitsOk = ok && getUnitConversion(database);
		
		// Get form conversion from local forms to CDM forms
		boolean formsOk = ok && getFormConversion();

		/* 2020-05-20 REPLACED BEGIN Translation */
		ok = ok && unitsOk && formsOk;
		/* 2020-05-20 REPLACED BEGIN Translation */
		/* 2020-05-20 REPLACED BY BEGIN Translation
		// Get the ingredient name translation map
		boolean translationOk = ok && getIngredientnameTranslationMap();
		
		ok = ok && unitsOk && formsOk && translationOk;
		/* 2020-05-20 REPLACED BY END Translation */
		
		// Load manual CAS mappings
		ok = ok && getManualCASMappings(manualCASMappingFile);		
		
		// Load manual Ingredient mappings
		ok = ok && getManualIngredientMappings(manualIngredientMappingFile);
		
		// Load manual drug mappings
		ok = ok && getManualDrugMappings(manualDrugMappingFile);
		
		// Load CAS names
		ok = ok && getCASNames(casFile);
		
		// Match ingredients by ATC and full name
		ok = ok && matchIngredients();
		
		// Get source drugs with all ingredients mapped
		ok = ok && getSourceDrugsWithAllIngredientsMapped();
		
		// Match source drugs to Clinical Drugs
		ok = ok && matchClinicalDrugs();
		
		// Match source drugs to Clinical Drug Comps
		ok = ok && matchClinicalDrugComps();
		
		// Match source drugs to Clinical Drug Forms
		ok = ok && matchClinicalDrugForms();
		
		// Match source drug ingredients to Clinical Drug Comps or Ingredients
		ok = ok && matchClinicalDrugCompsIngredients();

		// Save mapping
		if (ok) saveMapping();

		System.out.println(DrugMapping.getCurrentTime() + " Finished");
		
		// Create the final report
		if (ok) finalReport();
	}
	
	
	private boolean getSourceDrugs(InputFile sourceDrugsFile, long minimumUseCount) {
		boolean sourceDrugError = false;
		Integer sourceDrugCount = 0;
		String fileName = "";
		Integer noATCCounter = 0;
		Set<String> ignoredSourceCodes = new HashSet<String>();
		
		System.out.println(DrugMapping.getCurrentTime() + "     Loading source drugs ...");
		
		try {
			if (sourceDrugsFile.openFile()) {

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
				
				while (sourceDrugsFile.hasNext()) {
					Row row = sourceDrugsFile.next();
					
					String sourceCode = sourceDrugsFile.get(row, "SourceCode", true).trim();
					
					if ((!sourceCode.equals("")) && (!ignoredSourceCodes.contains(sourceCode))) {
						SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
						
						if (sourceDrug == null) {
							sourceDrugCount++;
							sourceDrug = new SourceDrug(
												sourceCode, 
												sourceDrugsFile.get(row, "SourceName", true).trim().toUpperCase(), 
												sourceDrugsFile.get(row, "SourceATCCode", true).trim().toUpperCase(), 
												sourceDrugsFile.get(row, "SourceFormulation", true).trim().toUpperCase(), 
												sourceDrugsFile.get(row, "SourceCount", true).trim()
												);
							if (sourceDrug.getCount() >= minimumUseCount) {
								sourceDrugs.add(sourceDrug);
								sourceDrugMap.put(sourceCode, sourceDrug);
								
								String form = sourceDrug.getFormulation();
								if (form != null) {
									forms.add(form);
								}
								
								//System.out.println("    " + sourceDrug);
								
								if (sourceDrug.getATCCode() == null) {
									if (missingATCFile != null) {
										sourceDrug.writeDescriptionToFile("", missingATCFile);
									}
									noATCCounter++;
								}
							}
							else {
								sourceDrug = null;
								ignoredSourceCodes.add(sourceCode);
							}
						}

						if (sourceDrug != null) {
							String ingredientCode        = sourceDrugsFile.get(row, "IngredientCode", false); 
							String ingredientName        = sourceDrugsFile.get(row, "IngredientName", true).trim().toUpperCase();
							/* 2020-05-20 REPLACE BEGIN Translation */
							String ingredientNameEnglish = sourceDrugsFile.get(row, "IngredientNameEnglish", true).trim().toUpperCase();
							/* 2020-05-20 REPLACE END Translation */
							/* 2020-05-20 REPLACED BY BEGIN Translation 
							String ingredientNameEnglish = null;
							/* 2020-05-20 REPLACED BY END */
							String dosage                = sourceDrugsFile.get(row, "Dosage", true).trim(); 
							String dosageUnit            = sourceDrugsFile.get(row, "DosageUnit", true).trim().toUpperCase(); 
							String casNumber             = sourceDrugsFile.get(row, "CASNumber", true).trim();
							
							if (ingredientCode != null) ingredientCode = ingredientCode.trim(); 
							ingredientName        = DrugMappingStringUtilities.removeExtraSpaces(ingredientName);
							ingredientNameEnglish = DrugMappingStringUtilities.removeExtraSpaces(ingredientNameEnglish);
							/* 2020-05-20 REMOVED END Translation */
							dosage                = DrugMappingStringUtilities.removeExtraSpaces(dosage);
							dosageUnit            = DrugMappingStringUtilities.removeExtraSpaces(dosageUnit);
							casNumber = DrugMappingNumberUtilities.uniformCASNumber(casNumber);

							// Remove comma's
							/* 2020-05-20 REMOVED BEGIN Translation 
							ingredientName = ingredientName.replaceAll(",", " ").replaceAll("  ", " ");
							ingredientNameEnglish = ingredientNameEnglish.replaceAll(",", " ").replaceAll("  ", " ");
							/* 2020-05-20 REMOVED END Translation */

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
				
				if (missingATCFile != null) {
					missingATCFile.close();
				}
				
				report.add("Source drugs: " + sourceDrugCount);
				List<SourceDrug> sourceDrugsToBeRemoved = new ArrayList<SourceDrug>();
				for (SourceDrug sourceDrug : sourceDrugs) {
					if (sourceDrug.getCount() < minimumUseCount) {
						sourceDrugsToBeRemoved.add(sourceDrug);
					}
				}
				sourceDrugs.removeAll(sourceDrugsToBeRemoved);
				report.add("Source drugs with a miminum use count of " + Long.toString(minimumUseCount) + ": " + DrugMappingNumberUtilities.percentage((long) sourceDrugs.size(), (long) sourceDrugCount));
				report.add("Source drugs without ATC: " + DrugMappingNumberUtilities.percentage((long) noATCCounter, (long) sourceDrugs.size()));
				report.add("Source ingredients: " + Integer.toString(SourceDrug.getAllIngredients().size()));
			}
		}
		catch (NoSuchElementException fileException) {
			System.out.println("  ERROR: " + fileException.getMessage());
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	private boolean getUnitConversion(CDMDatabase database) {
		boolean ok = true;
		
		// Create Units Map
		unitConversionsMap = new UnitConversion(units, cdm);
		if (unitConversionsMap.getStatus() != UnitConversion.STATE_OK) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the unit conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getBasePath() + "/" + UnitConversion.FILENAME);
			System.out.println("");
			System.out.println("The cells should contain a value so that: Local unit = <cell value> * CDM unit");
			System.out.println("");
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getFormConversion() {
		boolean ok = true;
		
		formConversionsMap = new FormConversion(forms, cdm);
		if (formConversionsMap.getStatus() != FormConversion.STATE_OK) {
			// If no form conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the form conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getBasePath() + "/" + FormConversion.FILENAME);
			System.out.println("");
			System.out.println("Fill the cells of matching forms with an non-space character.");
			System.out.println("");
			ok = false;
		}
		
		return ok;
	}
	
	/* 2020-05-20 ADDED BEGIN Translation
	private boolean getIngredientnameTranslationMap() {
		boolean ok = true;
		
		ingredientNameTranslationMap = new IngredientNameTranslation();
		if (ingredientNameTranslationMap.getStatus() != IngredientNameTranslation.STATE_OK) {
			// If no ingredient name translation map is specified then stop.
			System.out.println("");
			System.out.println("First fill the ingredient name translation map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getBasePath() + "/" + IngredientNameTranslation.FILENAME);
			System.out.println("");
			System.out.println("Fill the missing translations marked with <NEW>.");
			System.out.println("");
			ok = false;
		}
		
		return ok;
	}
	/* 2020-05-20 ADDED END Translation */
	
	
	private boolean getManualCASMappings(InputFile manualMappingFile) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + "     Loading manual CAS mappings ...");
		if ((manualMappingFile != null) && manualMappingFile.isSelected()) {
			try {
				
				if (manualMappingFile.openFile(true)) {
					Integer lineNr = 1;
					while (manualMappingFile.hasNext()) {
						lineNr++;
						Row row = manualMappingFile.next();
						
						String casNumber = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "CASNumber", true));
						String cdmConceptId = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "concept_id", true));
						//String cdmConceptName = manualMappingFile.get(row, "concept_name", true).trim();
						
						if (!casNumber.equals("")) {
							casNumber = DrugMappingNumberUtilities.uniformCASNumber(casNumber);
							
							if (!cdmConceptId.equals("")) {
								CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(cdmConceptId);
								if (cdmIngredient != null) {
									manualCASMappings.put(casNumber, cdmIngredient);
								}
								else {
									if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
										System.out.println("      WARNING: No CDM Ingredient found for concept_id " + cdmConceptId + " for CAS number " + casNumber + " in line " + lineNr + ".");
									}
								}
							}
							else {
								if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
									System.out.println("      WARNING: No concept_id found in line " + lineNr + ".");
								}
							}
						}
						else {
							if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
								System.out.println("      WARNING: No CAS number found in line " + lineNr + ".");
							}
						}
					}
				}
				else {
					System.out.println("    No manual CAS mappings found.");
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No manual CAS mappings used.");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean getManualIngredientMappings(InputFile manualMappingFile) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + "     Loading manual ingredient mappings ...");
		if ((manualMappingFile != null) && manualMappingFile.isSelected()) {
			try {
				
				if (manualMappingFile.openFile(true)) {

					Integer lineNr = 1;
					while (manualMappingFile.hasNext()) {
						lineNr++;
						Row row = manualMappingFile.next();
						
						String sourceCode = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "SourceCode", true));
						String sourceName = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "SourceName", true));
						String cdmConceptId = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "concept_id", true));
						//String cdmConceptName = manualMappingFile.get(row, "concept_name", true).trim();
						//String comment = manualMappingFile.get(row, "Comment", true).trim();

						String remark = null;
						if (!cdmConceptId.equals("")) {
							CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(cdmConceptId);
							if (cdmIngredient == null) { // Get replacing ingredient
								cdmIngredient = cdm.getCDMReplacedByIngredientConceptIdIndex().get(cdmConceptId);
								if (cdmIngredient != null) {
									remark = "Manual mapping: " + cdmConceptId + " replaced by " + cdmIngredient.getConceptId();
								}
							}
							else {
								remark = "Manual mapping: " + cdmConceptId;
							}
							if (cdmIngredient != null) {
								boolean sourceCodeFound = false;
								boolean sourceNameFound = false;
								
								if (!sourceCode.equals("")) {
									sourceCodeFound = true;
									SourceIngredient sourceIngredient = SourceDrug.getIngredient(sourceCode);
									if (sourceIngredient != null) {
										manualIngredientCodeMappings.put(sourceIngredient, cdmIngredient);
										manualIngredientCodeMappingRemarks.put(sourceIngredient, remark);
									}
									else {
										if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
											System.out.println("      WARNING: No source ingredient found for sourceCode " + sourceCode + " in line " + lineNr + ".");
										}
									}
								}
								if (!sourceName.equals("")) {
									sourceNameFound = true;
									manualIngredientNameMappings.put(sourceName, cdmIngredient);
								}
								if ((!sourceCodeFound) && (!sourceNameFound)) {
									if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
										System.out.println("      WARNING: No sourceCode and no sourceName found in line " + lineNr + ".");
									}
								}
							}
							else {
								if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
									System.out.println("      WARNING: No CDM Ingredient found for concept_id " + cdmConceptId + " in line " + lineNr + ".");
								}
							}
						}
						else {
							if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
								System.out.println("      WARNING: No concept_id found in line " + lineNr + ".");
							}
						}
					}
				}
				else {
					System.out.println("    No manual ingredient mappings found.");
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No manual ingredient mappings used.");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean getManualDrugMappings(InputFile manualMappingFile) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + "     Loading manual drug mappings ...");
		if ((manualMappingFile != null) && manualMappingFile.isSelected()) {
			try {
				
				if (manualMappingFile.openFile(true)) {
					
					while (manualMappingFile.hasNext()) {
						Row row = manualMappingFile.next();
						
						String sourceCode = manualMappingFile.get(row, "SourceCode", true).trim();
						String cdmConceptId = manualMappingFile.get(row, "concept_id", true).trim();
						
						SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
						CDMDrug cdmDrug = cdm.getCDMDrugs().get(cdmConceptId);
						if (cdmDrug == null) {
							cdmDrug = cdm.getCDMDrugComps().get(cdmConceptId);
						}
						if (cdmDrug == null) {
							cdmDrug = cdm.getCDMDrugForms().get(cdmConceptId);
						}
						System.out.println("Mapping " + (sourceDrug == null ? sourceCode : sourceDrug) + " -> " + (cdmDrug == null ? cdmConceptId : cdmDrug));
						if (sourceDrug == null) {
							System.out.println("    ERROR: SourceCode " + sourceCode + " does not exist!");
							ok = false;
						}
						if (cdmDrug == null) {
							System.out.println("    ERROR: CDM concept_id " + cdmConceptId + " does not exist or is not a standard concept!");
							ok = false;
						}
						if (sourceDrug != null) {
							CDMDrug mappedCDMDrug = manualDrugMappings.get(sourceDrug);
							if (mappedCDMDrug != null) {
								System.out.println("    ERROR: SourceDrug " + sourceDrug + " is already mapped to " + mappedCDMDrug);
								ok = false;
							}
						}
						
						if (ok) {
							manualDrugMappings.put(sourceDrug, cdmDrug);
						}
					}
				}
				else {
					System.out.println("    No manual drug mappings found.");
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No manual drug mappings used.");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean getCASNames(InputFile casFile) {
		boolean ok = true;
		
		if ((casFile != null) && casFile.isSelected()) {
			externalCASSynonymsMap = new HashMap<String, List<String>>();
			
			System.out.println(DrugMapping.getCurrentTime() + "     Loading CAS names ...");
			
			if (casFile.fileExists()) {
				try {
					if (casFile.openFile()) {
						while (casFile.hasNext()) {
							Row row = casFile.next();
							
							String casNumber = casFile.get(row, "CASNumber", true).trim();
							String chemicalName = casFile.get(row, "ChemicalName", true).replaceAll("\n", " ").replaceAll("\r", " ").toUpperCase().trim();
							String synonyms = casFile.get(row, "Synonyms", true).replaceAll("\n", " ").replaceAll("\r", " ").toUpperCase().trim();
							String[] synonymSplit = synonyms.split("[|]");
							
							if (!casNumber.equals("")) {
								casNumber = DrugMappingNumberUtilities.uniformCASNumber(casNumber);
								List<String> casNames = new ArrayList<String>();
								
								casNames.add(chemicalName);

								for (String synonym : synonymSplit) {
									if (!casNames.contains(synonym)) {
										casNames.add(synonym);
									}
								}
								
								String modifiedName = DrugMappingStringUtilities.modifyName(chemicalName);
								if (!casNames.contains(modifiedName)) {
									casNames.add(modifiedName);
								}

								for (String synonym : synonymSplit) {
									modifiedName = DrugMappingStringUtilities.modifyName(synonym);
									if (!casNames.contains(modifiedName)) {
										casNames.add(modifiedName);
									}
								}
								
								/*
								for (String synonym : synonymSplit) {
									String synonymNoSpaces = synonym.trim().replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
									if (!synonymNoSpaces.equals("")) {
										//casNames.add(synonymNoSpaces);
										casNames.add(synonym);
										casNames.add(DrugMappingStringUtilities.modifyName(synonym));
									}
								}
								
								String chemicalNameNoSpaces = chemicalName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", ""); 
								if (!chemicalNameNoSpaces.equals("")) {
									casNames.add(chemicalNameNoSpaces);
									casNames.add(DrugMappingStringUtilities.modifyName(chemicalName));
								}
								*/
								externalCASSynonymsMap.put(casNumber, casNames);
							}
						}
					}
					else {
						ok = false;
					}
				}
				catch (NoSuchElementException fileException) {
					System.out.println("  ERROR: " + fileException.getMessage());
				}
			}
			else {
				System.out.println("         No CAS File found.");
			}
			
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No CAS file used.");
		}
		
		return ok;
	}
	
	
	private boolean matchIngredients() {
		boolean ok = true;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match Ingredients");
		
		Integer multipleMappings = 0;
		
		multipleMappings += matchIngredientsByCASNumber();
		
		multipleMappings += matchIngredientsByName();
		
		multipleMappings += matchIngredientsByATC();

		report.add("Source ingredients mapped total: " + DrugMappingNumberUtilities.percentage((long) ingredientMap.size(), (long) SourceDrug.getAllIngredients().size()));
		report.add("Multiple mappings found: " + DrugMappingNumberUtilities.percentage((long) multipleMappings, (long) SourceDrug.getAllIngredients().size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private Integer matchIngredientsByCASNumber() {
		Integer matchedManualByCASNumber = 0;
		Integer matchedByCDMCASNumber = 0;
		Integer matchedByExternalCASName = 0;
		Integer multipleMappings = 0;

		if (cdm.getCDMCASIngredientMap().size() > 0) {
			System.out.println(DrugMapping.getCurrentTime() + "       Match ingredients by CDM CAS number ...");
			
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {
					
					String casNr = sourceIngredient.getCASNumber();
					if (casNr != null) {
						CDMIngredient cdmIngredient = manualCASMappings.get(casNr);
						if (cdmIngredient != null) { // Manual mapping found
							ingredientMap.put(sourceIngredient, cdmIngredient);
							sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
							sourceIngredient.setMatchString("MANUAL CAS");
							matchedManualByCASNumber++;
						}
						else {
							cdmIngredient = cdm.getCDMCASIngredientMap().get(casNr);
							if (cdmIngredient != null) {
								ingredientMap.put(sourceIngredient, cdmIngredient);
								sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
								sourceIngredient.setMatchString("CDM CAS");
								matchedByCDMCASNumber++;
							}
						}
					}
				}
			}
			
			String header = "IngredientCode";
			header += "," + "IngredientName";
			header += "," + "concept_id";
			header += "," + "concept_name";
			PrintWriter casIngredientMappingFile = DrugMappingFileUtilities.openOutputFile("IngredientMapping by CAS.csv", header);
			if (casIngredientMappingFile != null) {
				List<SourceIngredient> sourceIngredients = new ArrayList<SourceIngredient>();
				for (SourceDrug sourceDrug : sourceDrugs) {
					for (SourceIngredient sourceIngredient: sourceDrug.getIngredients()) {
						if (!sourceIngredients.contains(sourceIngredient)) {
							sourceIngredients.add(sourceIngredient);
						}
					}
				}
				Collections.sort(sourceIngredients, new Comparator<SourceIngredient>() {

					@Override
					public int compare(SourceIngredient sourceingredient1, SourceIngredient sourceingredient2) {
						return sourceingredient1.getIngredientName().compareTo(sourceingredient2.getIngredientName());
					}
				});
				for (SourceIngredient sourceIngredient : sourceIngredients) {
					String record = "\"" + sourceIngredient.getIngredientCode() + "\"";
					record += "," + "\"" + sourceIngredient.getIngredientName() + "\"";
					String concept_id = sourceIngredient.getMatchingIngredient();
					if (concept_id != null) {
						CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(concept_id);
						record += "," + "\"" + concept_id + "\"";
						record += "," + "\"" + (cdmIngredient == null ? "" : cdmIngredient.getConceptName()) + "\"";
					}
					else {
						record += ",";
						record += ",";
					}
					casIngredientMappingFile.println(record);
				}
				casIngredientMappingFile.close();
			}

			report.add("Source ingredients mapped manually by CAS number: " + DrugMappingNumberUtilities.percentage((long) matchedManualByCASNumber, (long) SourceDrug.getAllIngredients().size()));
			report.add("Source ingredients mapped by CDM CAS number: " + DrugMappingNumberUtilities.percentage((long) matchedByCDMCASNumber, (long) SourceDrug.getAllIngredients().size()));
			System.out.println(DrugMapping.getCurrentTime() + "       Done");
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "       No CDM CAS mapping found.");
		}
		
		
		if (externalCASSynonymsMap != null) {
			System.out.println(DrugMapping.getCurrentTime() + "       Match ingredients by external CAS number ...");
			
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {

					boolean matchFound = false;
					boolean multipleMapping = false;
					
					for (String ingredientNameIndexName : cdm.getCDMIngredientNameIndexNameList()) {
						Map<String, Set<CDMIngredient>> ingredientNameIndex = cdm.getCDMIngredientNameIndexMap().get(ingredientNameIndexName);
						String casNumber = sourceIngredient.getCASNumber();
						if (casNumber != null) {
							
							List<String> casNames = externalCASSynonymsMap.get(casNumber);
							if (casNames != null) {
								
								for (String casName : casNames) {
									
									Set<CDMIngredient> matchedCDMIngredients = ingredientNameIndex.get(casName);
									if (matchedCDMIngredients != null) {
										if (matchedCDMIngredients.size() == 1) {
											CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
											ingredientMap.put(sourceIngredient, cdmIngredient);
											sourceIngredient.setMatchingIngredient(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
											sourceIngredient.setMatchString("External CAS: " + ingredientNameIndexName + " " + casName);
											matchFound = true;
											matchedByExternalCASName++;
											break;
										}
										else {
											multipleMapping = true;
											multipleMappings++;
										}
									}
									
									if (matchFound || multipleMapping) {
										break;
									}
								}
								
								if (matchFound || multipleMapping) {
									break;
								}
							}
						}
					}
					
					/*
					String casNumber = sourceIngredient.getCASNumber();
					if (casNumber != null) {
						List<String> casNames = casMap.get(casNumber);
						if (casNames != null) {
							Set<CDMIngredient>matchedCDMIngredients = new HashSet<CDMIngredient>();
							String matchingCASname = null;
							for (String casName : casNames) {
								Set<CDMIngredient> casNameIngredients = cdmIngredientNameIndex.get(casName);
								if (casNameIngredients != null) {
									matchedCDMIngredients.addAll(casNameIngredients);
									matchingCASname = casName;
								}
							}
							if (matchedCDMIngredients.size() == 1) {
								CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
								ingredientMap.put(sourceIngredient, cdmIngredient);
								sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
								sourceIngredient.setMatchString("CAS: " + matchingCASname);
								matchedByCASName++;
							}
							else {
								multipleMappings++;
							}
						}
					}
					*/
				}
			}

			report.add("Source ingredients mapped by external CAS number: " + DrugMappingNumberUtilities.percentage((long) matchedByExternalCASName, (long) SourceDrug.getAllIngredients().size()));
			System.out.println(DrugMapping.getCurrentTime() + "       Done");
		}
		
		return multipleMappings;
	}
	
	
	private Integer matchIngredientsByName() {
		Integer matchedManualCode = 0;
		Integer matchedManualName = 0;
		Integer matchedByName = 0;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "       Match ingredients by name ...");
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			if (sourceIngredient.getMatchingIngredient() == null) {
				
				CDMIngredient cdmIngredient = manualIngredientCodeMappings.get(sourceIngredient);
				
				if (cdmIngredient != null) { // Manual mapping on ingredient code found
					ingredientMap.put(sourceIngredient, cdmIngredient);
					sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
					sourceIngredient.setMatchString("MANUAL CODE " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
					matchedManualCode++;
				}
				
				if (cdmIngredient == null) { // No manual mapping on ingredient code found
					preferencesUsed = "";
					boolean multipleMapping = false;

					List<String> matchNameList = sourceIngredient.getIngredientMatchingNames();
					for (String matchName : matchNameList) {
						String matchType = matchName.substring(0, matchName.indexOf(": "));
						matchName = matchName.substring(matchName.indexOf(": ") + 2);
						
						cdmIngredient = manualIngredientNameMappings.get(matchName);
						
						if (cdmIngredient != null) { // Manual mapping on part ingredient name found
							ingredientMap.put(sourceIngredient, cdmIngredient);
							sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
							sourceIngredient.setMatchString("MANUAL NAME: " + matchName);
							matchedManualName++;
							break;
						}
						
						List<String> matchList = new ArrayList<String>();
						matchList.add(matchType + ": " + matchName);
						matchList.add(matchType + " Standardized: " + DrugMappingStringUtilities.modifyName(matchName));

						for (String ingredientNameIndexName : cdm.getCDMIngredientNameIndexNameList()) {
							Map<String, Set<CDMIngredient>> ingredientNameIndex = cdm.getCDMIngredientNameIndexMap().get(ingredientNameIndexName);
							
							for (String searchName : matchList) {
								matchType = searchName.substring(0, searchName.indexOf(": "));
								searchName = searchName.substring(searchName.indexOf(": ") + 2);
								
								Set<CDMIngredient> matchedCDMIngredients = ingredientNameIndex.get(searchName);
								if (matchedCDMIngredients != null) {
									if (matchedCDMIngredients.size() > 1) {
										matchedCDMIngredients = selectConcept(matchedCDMIngredients);
									}
									
									if (matchedCDMIngredients.size() > 1) {
										String matchString = "Multiple mappings:";
										for (CDMIngredient ingredient : matchedCDMIngredients) {
											matchString += " " + ingredient.getConceptId();
										}
										sourceIngredient.setMatchString(matchString);
										multipleMapping = true;
										multipleMappings++;
									}
									else if (matchedCDMIngredients.size() == 1) {
										cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
										ingredientMap.put(sourceIngredient, cdmIngredient);
										sourceIngredient.setMatchingIngredient(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
										sourceIngredient.setMatchString(matchType + " " + ingredientNameIndexName + " " + searchName + " " + preferencesUsed);
										matchedByName++;
										break;
									}
								}
							}
							if ((cdmIngredient != null) || multipleMapping) {
								break;
							}
						}
						if ((cdmIngredient != null) || multipleMapping) {
							break;
						}
					}
				}
			}
		}

		report.add("Source ingredients mapped manually by code: " + DrugMappingNumberUtilities.percentage((long) matchedManualCode, (long) SourceDrug.getAllIngredients().size()));
		report.add("Source ingredients mapped manually by name: " + DrugMappingNumberUtilities.percentage((long) matchedManualName, (long) SourceDrug.getAllIngredients().size()));
		report.add("Source ingredients mapped by name: " + DrugMappingNumberUtilities.percentage((long) matchedByName, (long) SourceDrug.getAllIngredients().size()));
		System.out.println(DrugMapping.getCurrentTime() + "       Done");
		
		return multipleMappings;
	}
	
	
	private Integer matchIngredientsByATC() {
		Integer matchedByATC = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by ATC ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			List<SourceIngredient> sourceIngredients = sourceDrug.getIngredients();
			if ((sourceDrug.getATCCode() != null) && (sourceIngredients.size() == 1)) {
				SourceIngredient sourceIngredient = sourceIngredients.get(0);
				if (sourceIngredient.getMatchingIngredient() == null) {
					Set<CDMIngredient> cdmATCIngredients = cdm.getCDMATCIngredientMap().get(sourceDrug.getATCCode());
					if ((cdmATCIngredients != null) && (cdmATCIngredients.size() == 1)) {
						CDMIngredient cdmIngredient = (CDMIngredient) cdmATCIngredients.toArray()[0];
						ingredientMap.put(sourceIngredient, cdmIngredient);
						sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
						sourceIngredient.setMatchString(sourceDrug.getATCCode());
						matchedByATC++;
					}
				}
			}
		}

		report.add("Source ingredients mapped by ATC: " + DrugMappingNumberUtilities.percentage((long) matchedByATC, (long) SourceDrug.getAllIngredients().size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return 0;
	}
	
	
	private boolean getSourceDrugsWithAllIngredientsMapped() {
		System.out.println(DrugMapping.getCurrentTime() + "     Get source drugs with all ingredients mapped ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			List<SourceIngredient> sourceDrugIngredients = sourceDrug.getIngredients();
			if (sourceDrugIngredients.size() > 0) {
				List<CDMIngredient> cdmDrugIngredients = new ArrayList<CDMIngredient>();
				Set<SourceIngredient> sourceIngredientSet = new HashSet<SourceIngredient>();
				Set<CDMIngredient> cdmIngredientSet = new HashSet<CDMIngredient>();
				for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
					sourceIngredientSet.add(sourceDrugIngredient);
					cdmDrugIngredients.add(ingredientMap.get(sourceDrugIngredient));
					cdmIngredientSet.add(ingredientMap.get(sourceDrugIngredient));
				}
				if (!cdmDrugIngredients.contains(null)) {
					if (sourceIngredientSet.size() == cdmIngredientSet.size()) {
						sourceDrugsAllIngredientsMapped.add(sourceDrug);
						sourceDrugsCDMIngredients.put(sourceDrug, cdmDrugIngredients);
					}
					else {
						int mapping = 0;
						while ((mapping != INGREDIENT_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
							Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<CDMConcept>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<CDMConcept>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<CDMConcept>>>();
								Map<Integer, List<CDMConcept>> mappingTypeResults = new HashMap<Integer, List<CDMConcept>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<CDMConcept>> mappingTypeResults = mappingTypeResultsList.get(0);
							
							List<CDMConcept> mappingSourceIngredients = mappingTypeResults.get(DOUBLE_INGREDIENT_MAPPING);
							if (mappingSourceIngredients == null) {
								mappingSourceIngredients = new ArrayList<CDMConcept>();
								mappingTypeResults.put(DOUBLE_INGREDIENT_MAPPING, mappingSourceIngredients);
							}
							for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
								mappingSourceIngredients.add(ingredientMap.get(sourceDrugIngredient));
							}
							
							mapping++;
						}
					}
				}
				else {
					int mapping = 0;
					while ((mapping != INGREDIENT_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
						Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<CDMConcept>>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						
						List<Map<Integer, List<CDMConcept>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
						if (mappingTypeResultsList == null) {
							mappingTypeResultsList = new ArrayList<Map<Integer, List<CDMConcept>>>();
							Map<Integer, List<CDMConcept>> mappingTypeResults = new HashMap<Integer, List<CDMConcept>>();
							mappingTypeResultsList.add(mappingTypeResults);
							sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
						}
						
						Map<Integer, List<CDMConcept>> mappingTypeResults = mappingTypeResultsList.get(0);
						
						List<CDMConcept> mappingSourceIngredients = mappingTypeResults.get(UNMAPPED_SOURCE_INGREDIENTS);
						if (mappingSourceIngredients == null) {
							mappingSourceIngredients = new ArrayList<CDMConcept>();
							mappingTypeResults.put(UNMAPPED_SOURCE_INGREDIENTS, mappingSourceIngredients);
						}
						for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
							mappingSourceIngredients.add(ingredientMap.get(sourceDrugIngredient));
						}
						
						mapping++;
					}
				}
			}
			else {
				int mapping = 0;
				while ((mapping != INGREDIENT_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
					Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
					if (sourceDrugMappingResult == null) {
						sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<CDMConcept>>>>();
						sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
					}
					
					List<Map<Integer, List<CDMConcept>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
					if (mappingTypeResultsList == null) {
						mappingTypeResultsList = new ArrayList<Map<Integer, List<CDMConcept>>>();
						Map<Integer, List<CDMConcept>> mappingTypeResults = new HashMap<Integer, List<CDMConcept>>();
						mappingTypeResultsList.add(mappingTypeResults);
						sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
					}
					
					Map<Integer, List<CDMConcept>> mappingTypeResults = mappingTypeResultsList.get(0);
					
					List<CDMConcept> mappingSourceIngredients = mappingTypeResults.get(NO_SOURCE_INGREDIENTS);
					if (mappingSourceIngredients == null) {
						mappingSourceIngredients = new ArrayList<CDMConcept>();
						mappingSourceIngredients.add(null);
						mappingTypeResults.put(NO_SOURCE_INGREDIENTS, mappingSourceIngredients);
					}
					
					mapping++;
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		report.add("Source drugs with all ingredients mapped: " + DrugMappingNumberUtilities.percentage((long) sourceDrugsAllIngredientsMapped.size(), (long) sourceDrugs.size()));
		report.add("");
		
		return (sourceDrugsAllIngredientsMapped.size() > 0);
	}
	
	
	private boolean matchClinicalDrugs() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drugs ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			CDMDrug automaticMapping = null;
			Double usedStrengthDeviationPercentage = null;
			
			if (sourceDrugsAllIngredientsMapped.contains(sourceDrug)) {
				List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);
				
				if (sourceDrugCDMIngredients.size() > 0) {
					// Find CDM Clinical Drugs with corresponding ingredients
					List<CDMDrug> cdmDrugsWithIngredients = null;
					for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
						List<CDMDrug> cdmDrugsWithIngredient = cdm.getCDMDrugsContainingIngredient().get(cdmIngredient);
						if (cdmDrugsWithIngredient != null) {
							if (cdmDrugsWithIngredients == null) {
								// Initially add all drugs with the first ingredient
								cdmDrugsWithIngredients = new ArrayList<CDMDrug>();
								cdmDrugsWithIngredients.addAll(cdmDrugsWithIngredient);
							}
							else {
								// Remove all drugs that do not have the current ingredient.
								Set<CDMDrug> cdmDrugsMissingIngredient = new HashSet<CDMDrug>();
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									if (!cdmDrug.getIngredients().contains(cdmIngredient)) {
										cdmDrugsMissingIngredient.add(cdmDrug);
									}
								}
								cdmDrugsWithIngredients.removeAll(cdmDrugsMissingIngredient);
								if (cdmDrugsWithIngredients.size() == 0) {
									break;
								}
							}
						}
						else {
							cdmDrugsWithIngredients = new ArrayList<CDMDrug>();
							break;
						}
					}

					// Remove all drugs that do not have the same number of ingredients as the source drug
					Set<CDMDrug> cdmDrugsTooManyIngredients = new HashSet<CDMDrug>();
					for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
						if (cdmDrug.getIngredients().size() != sourceDrugCDMIngredients.size()) {
							cdmDrugsTooManyIngredients.add(cdmDrug);
						}
					}
					cdmDrugsWithIngredients.removeAll(cdmDrugsTooManyIngredients);
					
					// Find CDM Clinical Drugs with corresponding formulation
					if (cdmDrugsWithIngredients.size() > 0) {
						if (sourceDrug.getFormulation() != null) {
							String sourceDrugForm = sourceDrug.getFormulation(); 
							Set<CDMDrug> cdmDrugsMissingForm = new HashSet<CDMDrug>();
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								List<String> cdmDrugForms = cdmDrug.getForms();
								boolean formFound = false;
								for (String cdmDrugForm : cdmDrugForms) {
									if (formConversionsMap.matches(sourceDrugForm, cdmDrugForm, cdm)) {
										formFound = true;
										break;
									}
								}
								if (!formFound) {
									cdmDrugsMissingForm.add(cdmDrug);
								}
							}

							logMappingResult(sourceDrug, mapping, REJECTED_BY_FORM, cdmDrugsMissingForm);
							cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
						}
						
						// Find CDM Clinical Drugs with corresponding ingredient strengths
						if (cdmDrugsWithIngredients.size() > 0) {
							List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
							List<CDMConcept> rejectedDrugs = new ArrayList<CDMConcept>();
							Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
							
							List<Double> strengthDeviationPercentages = new ArrayList<Double>();

							// Try deviation percentage from 0.0 to maximumStrengthDeviationPercentage with steps of 0.1
							Double percentage = 0.0;
							while (percentage <= DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) {
								strengthDeviationPercentages.add(percentage);
								percentage += 0.1;
							}

							for (Double strengthDeviationPercentage : strengthDeviationPercentages) {
								matchingCDMDrugs = new ArrayList<CDMDrug>();
								rejectedDrugs = new ArrayList<CDMConcept>();
								
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
										List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), strengthDeviationPercentage);
										if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
											matchingCDMDrugs.add(cdmDrug);
											matchingIngredientsMap.put(cdmDrug, matchingIngredients);
										}
										else {
											rejectedDrugs.add(cdmDrug);
										}
									}
								}
								
								if ((matchingCDMDrugs != null) && (matchingCDMDrugs.size() > 0)) {
									usedStrengthDeviationPercentage = strengthDeviationPercentage;
									break;
								}
							}

							logMappingResult(sourceDrug, mapping, REJECTED_BY_STRENGTH, rejectedDrugs);
							
							if (matchingCDMDrugs.size() == 1) {
								automaticMapping = matchingCDMDrugs.get(0);
							}
							else if (matchingCDMDrugs.size() > 1) {
								// Discard drugs without strength units
								List<CDMDrug> matchingCDMDrugsWithTwoUnits = new ArrayList<CDMDrug>();
								List<CDMDrug> matchingCDMDrugsWithOneUnit = new ArrayList<CDMDrug>();
								for (CDMDrug matchingCDMDrug : matchingCDMDrugs) {
									List<CDMIngredientStrength> strengths = matchingCDMDrug.getIngredientStrengths();
									boolean twoUnitsFoundForAllIngredients = true;
									boolean oneUnitFoundForAllIngredients = true;
									for (CDMIngredientStrength strength : strengths) {
										if (strength.getNumeratorDosageUnit() != null) {
											if (strength.getDenominatorDosageUnit() == null) {
												twoUnitsFoundForAllIngredients = false;
											}
										}
										else {
											twoUnitsFoundForAllIngredients = false;
											oneUnitFoundForAllIngredients = false;
											break;
										}
									}
									if (twoUnitsFoundForAllIngredients) {
										matchingCDMDrugsWithTwoUnits.add(matchingCDMDrug);
									}
									else if (oneUnitFoundForAllIngredients) {
										matchingCDMDrugsWithOneUnit.add(matchingCDMDrug);
									}
								}
								if (matchingCDMDrugsWithTwoUnits.size() > 1) {
									matchingCDMDrugsWithTwoUnits = selectConcept(sourceDrug, matchingCDMDrugsWithTwoUnits, mapping);
								}
								if (matchingCDMDrugsWithTwoUnits.size() > 0) {
									if (matchingCDMDrugsWithTwoUnits.size() == 1) {
										automaticMapping = matchingCDMDrugsWithTwoUnits.get(0);
									}
									else {
										logMappingResult(sourceDrug, mapping, matchingCDMDrugsWithTwoUnits, NO_UNIQUE_MAPPING);
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
								}
								else {
									if (matchingCDMDrugsWithOneUnit.size() > 1) {
										matchingCDMDrugsWithOneUnit = selectConcept(sourceDrug, matchingCDMDrugsWithOneUnit, mapping);
									}

									if (matchingCDMDrugsWithOneUnit.size() > 0) {
										if (matchingCDMDrugsWithOneUnit.size() == 1) {
											automaticMapping = matchingCDMDrugsWithOneUnit.get(0);
										}
										else {
											logMappingResult(sourceDrug, mapping, matchingCDMDrugsWithOneUnit, NO_UNIQUE_MAPPING);
											notUniqueMapping.get(mapping).add(sourceDrug);
										}
									}
								}
							}
						}
					}
					else {
						logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS);
					}
				}
			}

			// Check for manual mapping and set final mapping.
			CDMDrug overruledMapping = null;
			CDMDrug finalMapping = automaticMapping;
			CDMDrug manualMapping = manualDrugMappings.get(sourceDrug);
			if (manualMapping != null) {
				// There is a manual mapping.
				overruledMapping = finalMapping;
				finalMapping = manualMapping;
			}
			
			if (finalMapping != null) {
				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if (finalMapping.getConceptClassId().equals("Clinical Drug")) {
					logMappingResult(sourceDrug, mapping, MAPPED, finalMapping);
					mappedSourceDrugs.add(sourceDrug);
					usedStrengthDeviationPercentageMap.put("Drug " + sourceDrug.getCode(), usedStrengthDeviationPercentage);
				}
				
				if (overruledMapping != null) {
					logMappingResult(sourceDrug, mapping, OVERRULED_MAPPING, overruledMapping);
					mappedSourceDrugs.add(sourceDrug);
				}
			}
		}

		report.add("Source drugs mapped to multiple CDM Clinical Drugs: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugComps() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_COMP_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Comps ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			CDMDrug automaticMapping = null;
			Double usedStrengthDeviationPercentage = null;
			
			if (sourceDrugsAllIngredientsMapped.contains(sourceDrug)) {
				boolean earlierNotUniqueMapping = false;
				for (int mappingType : notUniqueMapping.keySet()) {
					if (notUniqueMapping.get(mappingType).contains(sourceDrug)) {
						earlierNotUniqueMapping = true;
						break;
					}
				}
				if ((!mappedSourceDrugs.contains(sourceDrug)) && (!earlierNotUniqueMapping)) {
					if (sourceDrug.getIngredients().size() == 1) { // Clinical Drug Comp is always single ingredient
						List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

						// Find CDM Clinical Drug Comps with corresponding ingredient
						List<CDMDrug> cdmDrugCompsWithIngredients = null;
						for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
							List<CDMDrug> cdmDrugCompsWithIngredient = cdm.getCDMDrugCompsContainingIngredient().get(cdmIngredient);
							if (cdmDrugCompsWithIngredient != null) {
								if (cdmDrugCompsWithIngredients == null) {
									cdmDrugCompsWithIngredients = new ArrayList<CDMDrug>();
									for (CDMDrug cdmDrugCompWithIngredient : cdmDrugCompsWithIngredient) {
										if (!cdmDrugCompsWithIngredients.contains(cdmDrugCompWithIngredient)) {
											cdmDrugCompsWithIngredients.add(cdmDrugCompWithIngredient);
										}
									}
								}
								else {
									Set<CDMDrug> cdmDrugCompsMissingIngredient = new HashSet<CDMDrug>();
									for (CDMDrug cdmDrugComp : cdmDrugCompsWithIngredients) {
										if (!cdmDrugCompsWithIngredient.contains(cdmDrugComp)) {
											cdmDrugCompsMissingIngredient.add(cdmDrugComp);
										}
									}
									cdmDrugCompsWithIngredients.removeAll(cdmDrugCompsMissingIngredient);
									if (cdmDrugCompsWithIngredients.size() == 0) {
										break;
									}
								}
							}
							else {
								cdmDrugCompsWithIngredients = new ArrayList<CDMDrug>();
								break;
							}
						}
						
						// Find CDM Clinical Drug Comps with corresponding ingredient strengths
						if ((cdmDrugCompsWithIngredients != null) && (cdmDrugCompsWithIngredients.size() > 0)) {
							List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
							List<CDMConcept> rejectedDrugs = new ArrayList<CDMConcept>();
							Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
							
							List<Double> strengthDeviationPercentages = new ArrayList<Double>();
							
							// Try deviation percentage from 0.0 to maximumStrengthDeviationPercentage with steps of 0.1
							Double percentage = 0.0;
							while (percentage <= DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) {
								strengthDeviationPercentages.add(percentage);
								percentage += 0.1;
							}

							for (Double strengthDeviationPercentage : strengthDeviationPercentages) {
								matchingCDMDrugs = new ArrayList<CDMDrug>();
								rejectedDrugs = new ArrayList<CDMConcept>();
								
								for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
									if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
										List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), strengthDeviationPercentage);
										if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
											matchingCDMDrugs.add(cdmDrug);
											matchingIngredientsMap.put(cdmDrug, matchingIngredients);
										}
										else {
											rejectedDrugs.add(cdmDrug);
										}
									}
								}
								
								if ((matchingCDMDrugs != null) && (matchingCDMDrugs.size() > 0)) {
									usedStrengthDeviationPercentage = strengthDeviationPercentage;
									break;
								}
							}
							
							// Save the rejected drugs
							logMappingResult(sourceDrug, mapping, REJECTED_BY_STRENGTH, rejectedDrugs);

							if (matchingCDMDrugs.size() > 1) {
								matchingCDMDrugs = selectConcept(sourceDrug, matchingCDMDrugs, mapping);
							}
							
							if (matchingCDMDrugs.size() == 1) {
								automaticMapping = matchingCDMDrugs.get(0);
							}
							else if (matchingCDMDrugs.size() > 1) {
								// Discard drugs without strength units
								List<CDMDrug> matchingCDMDrugsWithTwoUnits = new ArrayList<CDMDrug>();
								List<CDMDrug> matchingCDMDrugsWithOneUnit = new ArrayList<CDMDrug>();
								for (CDMDrug matchingCDMDrug : matchingCDMDrugs) {
									List<CDMIngredientStrength> strengths = matchingCDMDrug.getIngredientStrengths();
									boolean twoUnitsFoundForAllIngredients = true;
									boolean oneUnitFoundForAllIngredients = true;
									for (CDMIngredientStrength strength : strengths) {
										if (strength.getNumeratorDosageUnit() != null) {
											if (strength.getDenominatorDosageUnit() == null) {
												twoUnitsFoundForAllIngredients = false;
											}
										}
										else {
											twoUnitsFoundForAllIngredients = false;
											oneUnitFoundForAllIngredients = false;
											break;
										}
									}
									if (twoUnitsFoundForAllIngredients) {
										matchingCDMDrugsWithTwoUnits.add(matchingCDMDrug);
									}
									else if (oneUnitFoundForAllIngredients) {
										matchingCDMDrugsWithOneUnit.add(matchingCDMDrug);
									}
								}
								if (matchingCDMDrugsWithTwoUnits.size() > 0) {
									if (matchingCDMDrugsWithTwoUnits.size() == 1) {
										automaticMapping = matchingCDMDrugs.get(0);
									}
									else {
										logMappingResult(sourceDrug, mapping, matchingCDMDrugs, NO_UNIQUE_MAPPING);
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
								}
								else {
									if (matchingCDMDrugsWithOneUnit.size() == 1) {
										automaticMapping = matchingCDMDrugs.get(0);
									}
									else {
										logMappingResult(sourceDrug, mapping, matchingCDMDrugs, NO_UNIQUE_MAPPING);
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
								}
							}
						}
						else {
							logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS);
						}
					}
					else {
						logMappingResult(sourceDrug, mapping, NO_SINGLE_INGREDIENT_DRUG);
					}
				}
			}

			// Check for manual mapping and set final mapping.
			CDMDrug overruledMapping = null;
			CDMDrug finalMapping = automaticMapping;
			CDMDrug manualMapping = manualDrugMappings.get(sourceDrug);
			if (manualMapping != null) {
				// There is a manual mapping.
				overruledMapping = finalMapping;
				finalMapping = manualMapping;
			}
			
			if (finalMapping != null) {
				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if (finalMapping.getConceptClassId().equals("Clinical Drug Comp")) {
					logMappingResult(sourceDrug, mapping, MAPPED, finalMapping);
					mappedSourceDrugs.add(sourceDrug);
					usedStrengthDeviationPercentageMap.put("Drug " + sourceDrug.getCode(), usedStrengthDeviationPercentage);
				}
				
				if (overruledMapping != null) {
					logMappingResult(sourceDrug, mapping, MAPPED, overruledMapping);
					mappedSourceDrugs.add(sourceDrug);
				}
			}
		}

		report.add("Source drugs mapped to multiple CDM Clinical Drug Comps: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugForms() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_FORM_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Forms ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			CDMDrug automaticMapping = null;
			
			if (sourceDrugsAllIngredientsMapped.contains(sourceDrug)) {
				boolean earlierNotUniqueMapping = false;
				for (int mappingType : notUniqueMapping.keySet()) {
					if (notUniqueMapping.get(mappingType).contains(sourceDrug)) {
						earlierNotUniqueMapping = true;
						break;
					}
				}
				if ((!mappedSourceDrugs.contains(sourceDrug)) && (!earlierNotUniqueMapping)) {
					List<CDMIngredient> cdmDrugIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

					// Find CDM Clinical Drugs with corresponding ingredients
					List<CDMDrug> cdmDrugsWithIngredients = null;
					for (CDMIngredient cdmIngredient : cdmDrugIngredients) {
						List<CDMDrug> cdmDrugsWithIngredient = cdm.getCDMDrugFormsContainingIngredient().get(cdmIngredient);
						if (cdmDrugsWithIngredient != null) {
							if (cdmDrugsWithIngredients == null) {
								cdmDrugsWithIngredients = new ArrayList<CDMDrug>();
								for (CDMDrug cdmDrug : cdmDrugsWithIngredient) {
									if (!cdmDrugsWithIngredients.contains(cdmDrug)) {
										cdmDrugsWithIngredients.add(cdmDrug);
									}
								}
							}
							else {
								Set<CDMDrug> cdmDrugsMissingIngredient = new HashSet<CDMDrug>();
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									if (!cdmDrugsWithIngredient.contains(cdmDrug)) {
										cdmDrugsMissingIngredient.add(cdmDrug);
									}
								}
								cdmDrugsWithIngredients.removeAll(cdmDrugsMissingIngredient);
								if (cdmDrugsWithIngredients.size() == 0) {
									break;
								}
							}
						}
						else {
							cdmDrugsWithIngredients = new ArrayList<CDMDrug>();
							break;
						}
					}
					
					// Find CDM Clinical Drugs with corresponding formulation
					if (cdmDrugsWithIngredients.size() > 0) {
						if (sourceDrug.getFormulation() != null) {
							String sourceDrugForm = sourceDrug.getFormulation(); 
							Set<CDMDrug> cdmDrugsMissingForm = new HashSet<CDMDrug>();
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								List<String> cdmDrugForms = cdmDrug.getForms();
								boolean formFound = false;
								for (String cdmDrugForm : cdmDrugForms) {
									if (formConversionsMap.matches(sourceDrugForm, cdmDrugForm, cdm)) {
										formFound = true;
										break;
									}
								}
								if (!formFound) {
									cdmDrugsMissingForm.add(cdmDrug);
								}
							}
							logMappingResult(sourceDrug, mapping, REJECTED_BY_FORM, cdmDrugsMissingForm);
							
							cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
						}

						if (cdmDrugsWithIngredients.size() > 1) {
							cdmDrugsWithIngredients = selectConcept(sourceDrug, cdmDrugsWithIngredients, mapping);
						}

						if (cdmDrugsWithIngredients.size() > 0) {
							List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
							Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
							
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), null);
								if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
									matchingCDMDrugs.add(cdmDrug);
									matchingIngredientsMap.put(cdmDrug, matchingIngredients);
								}
							}
							if (matchingCDMDrugs.size() == 1) {
								automaticMapping = matchingCDMDrugs.get(0);
							}
							else if (matchingCDMDrugs.size() > 1) {
								logMappingResult(sourceDrug, mapping, matchingCDMDrugs, NO_UNIQUE_MAPPING);
								notUniqueMapping.get(mapping).add(sourceDrug);
							}
						}
					}
					else {
						logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS);
					}
				}
			}

			// Check for manual mapping and set final mapping.
			CDMDrug overruledMapping = null;
			CDMDrug finalMapping = automaticMapping;
			CDMDrug manualMapping = manualDrugMappings.get(sourceDrug);
			if (manualMapping != null) {
				// There is a manual mapping.
				overruledMapping = finalMapping;
				finalMapping = manualMapping;
			}
			
			if (finalMapping != null) {
				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if (finalMapping.getConceptClassId().equals("Clinical Drug Form")) {
					logMappingResult(sourceDrug, mapping, MAPPED, finalMapping);
					mappedSourceDrugs.add(sourceDrug);
				}
				
				if (overruledMapping != null) {
					logMappingResult(sourceDrug, mapping, OVERRULED_MAPPING, overruledMapping);
					mappedSourceDrugs.add(sourceDrug);
				}
			}
		}

		report.add("Source drugs mapped to multiple CDM Clinical Drug Forms: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugCompsIngredients() {
		boolean ok = true;
		int mapping = INGREDIENT_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drug ingredients to Clinical Drug Comps and Ingredients ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			List<CDMConcept> automaticMappings = new ArrayList<CDMConcept>();
			for (int automaticMappingNr = 0; automaticMappingNr < sourceDrug.getComponents().size(); automaticMappingNr++) {
				automaticMappings.add(null);
			}

			boolean earlierNotUniqueMapping = false;
			for (int mappingType : notUniqueMapping.keySet()) {
				if (notUniqueMapping.get(mappingType).contains(sourceDrug)) {
					earlierNotUniqueMapping = true;
					break;
				}
			}
			
			List<Double> usedStrengthDeviationPercentage = new ArrayList<Double>();
			if ((!mappedSourceDrugs.contains(sourceDrug)) && (!earlierNotUniqueMapping)) {
				List<SourceDrugComponent> sourceDrugComponents = sourceDrug.getComponents();
				if (sourceDrugComponents.size() > 0) {
					// Find CDM Clinical Drug Comps with corresponding ingredients
					for (int componentNr = 0; componentNr < sourceDrugComponents.size(); componentNr++) {
						usedStrengthDeviationPercentage.add(null);
						SourceDrugComponent sourceDrugComponent = sourceDrugComponents.get(componentNr);
						SourceIngredient sourceDrugIngredient = sourceDrugComponent.getIngredient();
						String cdmIngredientConceptId = sourceDrugIngredient.getMatchingIngredient();
 						if (cdmIngredientConceptId != null) {
							CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(cdmIngredientConceptId);
							List<CDMDrug> cdmDrugCompsWithIngredients = cdm.getCDMDrugCompsContainingIngredient().get(cdmIngredient);

							// Find CDM Clinical Drug Comps with corresponding ingredient strengths
							if ((cdmDrugCompsWithIngredients != null) && (cdmDrugCompsWithIngredients.size() > 0)) {
								List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
								List<CDMConcept> rejectedDrugs = new ArrayList<CDMConcept>();
								Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
								
								List<Double> strengthDeviationPercentages = new ArrayList<Double>();
								
								// Try deviation percentage from 0.0 to maximumStrengthDeviationPercentage with steps of 0.1
								Double percentage = 0.0;
								while (percentage <= DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) {
									strengthDeviationPercentages.add(percentage);
									percentage += 0.1;
								}

								for (Double strengthDeviationPercentage : strengthDeviationPercentages) {
									matchingCDMDrugs = new ArrayList<CDMDrug>();
									rejectedDrugs = new ArrayList<CDMConcept>();
									
									for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
										if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
											List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), strengthDeviationPercentage);
											if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
												matchingCDMDrugs.add(cdmDrug);
												matchingIngredientsMap.put(cdmDrug, matchingIngredients);
											}
											else {
												rejectedDrugs.add(cdmDrug);
											}
										}
									}
									
									if ((matchingCDMDrugs != null) && (matchingCDMDrugs.size() > 0)) {
										usedStrengthDeviationPercentage.set(componentNr, strengthDeviationPercentage);
										break;
									}
								}
								
								// Save the rejected drugs
								logMappingResult(sourceDrug, mapping, REJECTED_BY_STRENGTH, rejectedDrugs, componentNr);

								if (matchingCDMDrugs.size() > 1) {
									matchingCDMDrugs = selectConcept(sourceDrug, matchingCDMDrugs, mapping, componentNr);
								}
								
								if (matchingCDMDrugs.size() == 1) {
									automaticMappings.set(componentNr, matchingCDMDrugs.get(0));
								}
								else if (matchingCDMDrugs.size() > 1) {
									// Discard drugs without strength units
									List<CDMDrug> matchingCDMDrugsWithTwoUnits = new ArrayList<CDMDrug>();
									List<CDMDrug> matchingCDMDrugsWithOneUnit = new ArrayList<CDMDrug>();
									for (CDMDrug matchingCDMDrug : matchingCDMDrugs) {
										List<CDMIngredientStrength> strengths = matchingCDMDrug.getIngredientStrengths();
										boolean twoUnitsFoundForAllIngredients = true;
										boolean oneUnitFoundForAllIngredients = true;
										for (CDMIngredientStrength strength : strengths) {
											if (strength.getNumeratorDosageUnit() != null) {
												if (strength.getDenominatorDosageUnit() == null) {
													twoUnitsFoundForAllIngredients = false;
												}
											}
											else {
												twoUnitsFoundForAllIngredients = false;
												oneUnitFoundForAllIngredients = false;
												break;
											}
										}
										if (twoUnitsFoundForAllIngredients) {
											matchingCDMDrugsWithTwoUnits.add(matchingCDMDrug);
										}
										else if (oneUnitFoundForAllIngredients) {
											matchingCDMDrugsWithOneUnit.add(matchingCDMDrug);
										}
									}
									if (matchingCDMDrugsWithTwoUnits.size() > 0) {
										if (matchingCDMDrugsWithTwoUnits.size() == 1) {
											automaticMappings.set(componentNr, matchingCDMDrugs.get(0));
										}
										else {
											logMappingResult(sourceDrug, mapping, matchingCDMDrugs, NO_UNIQUE_MAPPING, componentNr);
											notUniqueMapping.get(mapping).add(sourceDrug);
										}
									}
									else {
										if (matchingCDMDrugsWithOneUnit.size() == 1) {
											automaticMappings.set(componentNr, matchingCDMDrugs.get(0));
										}
										else {
											logMappingResult(sourceDrug, mapping, matchingCDMDrugs, NO_UNIQUE_MAPPING, componentNr);
											notUniqueMapping.get(mapping).add(sourceDrug);
										}
									}
								}
							}
						}
						
						// Try mapping to CDM Ingredient
						if (automaticMappings.get(componentNr) == null) {
							if (sourceDrugIngredient.getMatchingIngredient() != null) {
								CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(sourceDrugIngredient.getMatchingIngredient());
								automaticMappings.set(componentNr, cdmIngredient);
							}
						}
					}
				}
			}

			// Check for manual mapping and set final mapping.
			boolean mapped = false;
			List<CDMConcept> finalMappings = new ArrayList<CDMConcept>();
			List<CDMConcept> overruledMappings = new ArrayList<CDMConcept>();
			for (int automatedMappingNr = 0; automatedMappingNr < automaticMappings.size(); automatedMappingNr++) {
				CDMConcept automaticMapping = automaticMappings.get(automatedMappingNr);
				CDMConcept overruledMapping = null;
				CDMConcept finalMapping = automaticMapping;
				CDMDrug manualMapping = manualDrugMappings.get(sourceDrug);
				if (manualMapping != null) {
					// There is a manual mapping.
					overruledMapping = finalMapping;
					finalMapping = manualMapping;
				}
				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if ((finalMapping == null) || finalMapping.getConceptClassId().equals("Clinical Drug Comp") || finalMapping.getConceptClassId().equals("Ingredient")) {
					if (finalMapping != null) {
						finalMappings.add(finalMapping);
						mapped = true;
					}
					else {
						finalMappings.add(null);
					}
					if (overruledMapping != null) {
						overruledMappings.add(overruledMapping);
					}
					else {
						overruledMappings.add(null);
					}
				}
				else {
					finalMappings.add(null);
					overruledMappings.add(null);
				}
			}

			if (mapped) {
				for (int finalMappingNr = 0; finalMappingNr < finalMappings.size(); finalMappingNr++) {
					if (overruledMappings.get(finalMappingNr) != null) {
						logMappingResult(sourceDrug, mapping, OVERRULED_MAPPING, overruledMappings.get(finalMappingNr), finalMappingNr);
					}
					logMappingResult(sourceDrug, mapping, MAPPED, finalMappings.get(finalMappingNr), finalMappingNr);
					SourceIngredient sourceDrugIngredient = sourceDrug.getIngredients().get(finalMappingNr);
					usedStrengthDeviationPercentageMap.put("Ingredient " + sourceDrug.getCode() + "," + sourceDrugIngredient.getIngredientCode(), usedStrengthDeviationPercentage.get(finalMappingNr));
					mappedSourceDrugs.add(sourceDrug);
				}
			}

		}

		report.add("Source drugs mapped to multiple CDM Clinical Drug Comp or CDM Ingredient combinations: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private void saveMapping() {
		// Save ingredient mapping
		PrintWriter ingredientMappingFile = DrugMappingFileUtilities.openOutputFile("IngredientMapping Results.csv", SourceIngredient.getMatchHeader() + ",SourceCount," + CDMIngredient.getHeader());
		
		if (ingredientMappingFile != null) {
			
			// Sort the ingredients on use count descending.
			List<SourceIngredient> sourceIngredients = new ArrayList<SourceIngredient>();
			sourceIngredients.addAll(SourceDrug.getAllIngredients());
			
			Collections.sort(sourceIngredients, new Comparator<SourceIngredient>() {
				@Override
				public int compare(SourceIngredient ingredient1, SourceIngredient ingredient2) {
					int countCompare = Long.compare(ingredient1.getCount() == null ? 0L : ingredient1.getCount(), ingredient2.getCount() == null ? 0L : ingredient2.getCount()); 
					int compareResult = (countCompare == 0 ? (ingredient1.getIngredientCode() == null ? "" : ingredient1.getIngredientCode()).compareTo(ingredient2.getIngredientCode() == null ? "" : ingredient2.getIngredientCode()) : -countCompare);
					//System.out.println("Compare: " + sourceDrug1.getCode() + "," + sourceDrug1.getCount() + " <-> " + sourceDrug2.getCode() + "," + sourceDrug2.getCount() + " => " + Integer.toString(compareResult));
					return compareResult;
				}
			});
			
			for (SourceIngredient sourceIngredient : sourceIngredients) {
				if (sourceIngredient.getMatchingIngredient() != null) {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + sourceIngredient.getCount() + "," + cdm.getCDMIngredients().get(sourceIngredient.getMatchingIngredient()));
				}
				else {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + sourceIngredient.getCount() + "," + CDMIngredient.emptyRecord());
				}
			}
			
			ingredientMappingFile.close();
		}
		
		String header = "SourceIngredientCode";
		header += "," + "SourceIngredientName";
		header += "," + "SourceRecordCount";
		header += "," + "concept_id";
		header += "," + "concept_name";
		header += "," + "concept_vocabulary_id";
		header += "," + "concept_class_id";
		header += "," + "MatchLog";
		PrintWriter ingredientMappingDebugFile = DrugMappingFileUtilities.openOutputFile("IngredientMapping Debug.csv", header);
		if (ingredientMappingDebugFile != null) {
			
			// Sort the ingredients on use count descending.
			List<SourceIngredient> sourceIngredients = new ArrayList<SourceIngredient>();
			sourceIngredients.addAll(SourceDrug.getAllIngredients());
			
			Collections.sort(sourceIngredients, new Comparator<SourceIngredient>() {
				@Override
				public int compare(SourceIngredient ingredient1, SourceIngredient ingredient2) {
					int compareResult = (ingredient1.getIngredientName() == null ? "" : ingredient1.getIngredientName()).compareTo(ingredient2.getIngredientName() == null ? "" : ingredient2.getIngredientName());
					//System.out.println("Compare: " + sourceDrug1.getCode() + "," + sourceDrug1.getCount() + " <-> " + sourceDrug2.getCode() + "," + sourceDrug2.getCount() + " => " + Integer.toString(compareResult));
					return compareResult;
				}
			});
			
			for (SourceIngredient sourceIngredient : sourceIngredients) {
				CDMConcept cdmIngredient = cdm.getCDMIngredients().get(sourceIngredient.getMatchingIngredient());
				if (cdmIngredient == null) {
					cdmIngredient = cdm.getCDMDrugComps().get(sourceIngredient.getMatchingIngredient());
				}
				
				String record = sourceIngredient.getIngredientCode();
				record += "," + DrugMapping.escapeFieldValue(sourceIngredient.getIngredientName());
				record += "," + sourceIngredient.getCount();
				if (cdmIngredient != null) {
					record += "," + cdmIngredient.getConceptId();
					record += "," + DrugMapping.escapeFieldValue(cdmIngredient.getConceptName());
					record += "," + cdmIngredient.getVocabularyId();
					record += "," + cdmIngredient.getConceptClassId();
					record += "," + DrugMapping.escapeFieldValue(sourceIngredient.getMatchString());
				}
				else {
					record += ",";
					record += ",";
					record += ",";
					record += ",";
					record += ",";
				}
				ingredientMappingDebugFile.println(record);
			}
			
			ingredientMappingDebugFile.close();
		}
		
		// Save drug mapping
		counters = new HashMap<Integer, Map<Integer, Long>>();
		dataCoverage = new HashMap<Integer, Map<Integer, Long>>();

		int mappingType = 0;
		while (mappingTypeDescriptions.containsKey(mappingType)) {
			Map<Integer, Long> mappingTypeCounters = new HashMap<Integer, Long>();
			counters.put(mappingType, mappingTypeCounters);
			Map<Integer, Long> mappingTypeDataCoverage = new HashMap<Integer, Long>();
			dataCoverage.put(mappingType, mappingTypeDataCoverage);
			int mappingResultType = 0;
			while (mappingResultDescriptions.containsKey(mappingResultType)) {
				mappingTypeCounters.put(mappingResultType, 0L);
				mappingTypeDataCoverage.put(mappingResultType, 0L);
				mappingResultType++;
			}
			mappingType++;
		}
		
		header = "MappingStatus";
		header += "," + SourceDrug.getHeader();
		header += "," + SourceIngredient.getHeader();
		header += "," + "StrengthMarginPercentage";
		header += "," + "MappingType";
		header += "," + "MappingResult";
		header += "," + "concept_id";
		header += "," + "concept_name";
		header += "," + "domain_id";
		header += "," + "vocabulary_id";
		header += "," + "concept_class_id";
		header += "," + "standard_concept";
		header += "," + "concept_code";
		header += "," + "valid_start_date";
		header += "," + "valid_end_date";
		header += "," + "invalid_reason	atc";
		PrintWriter drugMappingFile = DrugMappingFileUtilities.openOutputFile("DrugMapping.csv", header);

		header = "MappingStatus";
		header += "," + SourceDrug.getHeader();
		header += "," + SourceIngredient.getHeader();
		header += "," + "StrengthMarginPercentage";
		header += "," + "MappingType";
		header += "," + "MappingResult";
		//header += "," + "Results";
		for (Integer conceptNr = 1; conceptNr <= maxResultConcepts; conceptNr++) {
			header += "," + "Concept_" + conceptNr; 
		}
		PrintWriter drugMappingResultsFile = DrugMappingFileUtilities.openOutputFile("DrugMapping Results.csv", header);
		
		header = "SourceCode";
		header += "," + "SourceName";
		header += "," + "SourceCount";
		header += "," + "SourceIngredientCode";
		header += "," + "SourceIngredientName";
		header += "," + "concept_id";
		header += "," + "concept_name";
		header += "," + "concept_class";
		header += "," + "Mapping_log";
		PrintWriter drugMappingDebugFile = DrugMappingFileUtilities.openOutputFile("DrugMapping Debug.csv", header);
		
		if ((drugMappingFile != null) && (drugMappingResultsFile != null)) {
			System.out.println(DrugMapping.getCurrentTime() + "     Saving Drug Mapping Results ...");

			// Sort source drugs on use count descending
			Collections.sort(sourceDrugs, new Comparator<SourceDrug>() {
				@Override
				public int compare(SourceDrug sourceDrug1, SourceDrug sourceDrug2) {
					int countCompare = Long.compare(sourceDrug1.getCount() == null ? 0L : sourceDrug1.getCount(), sourceDrug2.getCount() == null ? 0L : sourceDrug2.getCount()); 
					int compareResult = (countCompare == 0 ? (sourceDrug1.getCode() == null ? "" : sourceDrug1.getCode()).compareTo(sourceDrug2.getCode() == null ? "" : sourceDrug2.getCode()) : -countCompare);
					//System.out.println("Compare: " + sourceDrug1.getCode() + "," + sourceDrug1.getCount() + " <-> " + sourceDrug2.getCode() + "," + sourceDrug2.getCount() + " => " + Integer.toString(compareResult));
					return compareResult;
				}
			});
			
			for (SourceDrug sourceDrug : sourceDrugs) {
				String mappingStatus = manualDrugMappings.containsKey(sourceDrug) ? "ManualMapping" : (mappedSourceDrugs.contains(sourceDrug) ? "Mapped" : "Unmapped");
				Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappings = sourceDrugMappingResults.get(sourceDrug);
				
				if (sourceDrugMappings == null) {
					System.out.println("ERROR: " + sourceDrug);
				}

				mappingType = 0;
				while (mappingTypeDescriptions.containsKey(mappingType)) {
					List< Map<Integer, List<CDMConcept>>> mappingResultList = sourceDrugMappings.get(mappingType);
					if ((mappingResultList != null) && (mappingResultList.size() > 0)) {
						Set<Integer> counted = new HashSet<Integer>();
						for (int ingredientNr = 0; ingredientNr < mappingResultList.size(); ingredientNr++) {
							Map<Integer, List<CDMConcept>> mappingResult = mappingResultList.get(ingredientNr); 
							String sourceIngredientString = "*,*,*,*"; // Mapping on source drug
							String sourceIngredientDebugString = "*,*";
							if (mappingType == INGREDIENT_MAPPING) {   // Mapping on source drug ingredients
								sourceIngredientString = sourceDrug.getIngredients().get(ingredientNr).toString();
								sourceIngredientDebugString = sourceDrug.getIngredients().get(ingredientNr).getIngredientCode();
								sourceIngredientDebugString += "," + sourceDrug.getIngredients().get(ingredientNr).getIngredientName();
							}
							if (mappingResult != null) {
								// Write the result records
								int mappingResultType = 0;
								while (mappingResultDescriptions.containsKey(mappingResultType)) {
									String strengthDeviationPercentage = "";
									if (mappingResultType == MAPPED) {
										String key = mappingType == INGREDIENT_MAPPING ? ("Ingredient " + sourceDrug.getCode() + "," + sourceDrug.getIngredients().get(ingredientNr).getIngredientCode()) : ("Drug " + sourceDrug.getCode());
										if (usedStrengthDeviationPercentageMap.get(key) != null) {
											strengthDeviationPercentage = usedStrengthDeviationPercentageMap.get(key).toString();
										}
									}
									String record = DrugMapping.escapeFieldValue(mappingStatus);
									record += "," + sourceDrug;
									record += "," + sourceIngredientString;
									record += "," + strengthDeviationPercentage; 
									record += "," + DrugMapping.escapeFieldValue(mappingTypeDescriptions.get(mappingType));
									record += "," + DrugMapping.escapeFieldValue(mappingResultDescriptions.get(mappingResultType));
									
									String drugMappingRecord = record;
									
									String debugRecord = DrugMapping.escapeFieldValue(sourceDrug.getCode());
									debugRecord += "," + DrugMapping.escapeFieldValue(sourceDrug.getName());
									debugRecord += "," + DrugMapping.escapeFieldValue(sourceDrug.getCount().toString());
									debugRecord += "," + sourceIngredientDebugString;
									
									List<CDMConcept> results = mappingResult.get(mappingResultType);
									if ((results != null) && (results.size() > 0)) {
										if (mappingResultType == MAPPED) {
											drugMappingRecord += "," + (results.get(0) == null ? "" : results.get(0).toString());

											String conceptId = "";
											String conceptName = "";
											String conceptClass = "";
											if (results.get(0) != null) {
												String[] conceptSplit = results.get(0).toString().split(",");
												conceptId = conceptSplit.length > 0 ? conceptSplit[0] : "";
												conceptName = "";
												if (conceptSplit.length >= 9) {
													for (int namePart = 1; namePart < (conceptSplit.length - 7); namePart++) {
														conceptName += (namePart > 1 ? "," : "") + conceptSplit[namePart];
													}
												}
												conceptClass = conceptSplit[conceptSplit.length - 5];
											}
											
											debugRecord += "," + DrugMapping.escapeFieldValue(conceptId);
											debugRecord += "," + DrugMapping.escapeFieldValue(conceptName);
											debugRecord += "," + DrugMapping.escapeFieldValue(conceptClass);
											debugRecord += "," + DrugMapping.escapeFieldValue(mappingTypeDescriptions.get(mappingType));;
										}
										Collections.sort(results, new Comparator<CDMConcept>() {
											@Override
											public int compare(CDMConcept concept1, CDMConcept concept2) {
												return (concept1 == null ? "" : concept1.getConceptId()).compareTo(concept2 == null ? "" : concept2.getConceptId());
											}
										});
										for (CDMConcept result : results) {
											if (mappingResultType == REJECTED_BY_STRENGTH) {
												record += "," + DrugMapping.escapeFieldValue(result == null ? "" : ((CDMDrug) result).toString() + ": " + ((CDMDrug) result).getStrengthDescription());
											}
											else if (mappingResultType == REJECTED_BY_FORM) {
												if (result != null) {
													CDMDrug cdmDrug = (CDMDrug) result;
													String formsDescription = "";
													for (String cdmDrugFormConceptId : cdmDrug.getForms()) {
														if (!formsDescription.equals("")) {
															formsDescription += " | ";
														}
														formsDescription += cdm.getCDMFormConceptName(cdmDrugFormConceptId) + " (" + cdmDrugFormConceptId + ")";
													}
													record += "," + DrugMapping.escapeFieldValue(cdmDrug.toString() + ": " + formsDescription);
												}
												else {
													record += "," + DrugMapping.escapeFieldValue("");
												}
											}
											else if ((mappingResultType == DOUBLE_INGREDIENT_MAPPING) || (mappingResultType == UNMAPPED_SOURCE_INGREDIENTS)) {
												List<SourceIngredient> sourceDrugIngredients = sourceDrug.getIngredients();
												for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
													CDMIngredient cdmIngredient = (sourceDrugIngredient.getMatchingIngredient() == null ? null : cdm.getCDMIngredients().get(sourceDrugIngredient.getMatchingIngredient()));
													String description = sourceDrugIngredient.toString();
													description += " -> " + (cdmIngredient == null ? "" : cdmIngredient.toString());
													record += "," + DrugMapping.escapeFieldValue(description);
												}
											}
											else {
												record += "," + DrugMapping.escapeFieldValue(result == null ? "" : result.toString());
											}
										}
										if (mappingResultType == MAPPED) {
											drugMappingFile.println(drugMappingRecord);
											drugMappingDebugFile.println(debugRecord);
										}
										drugMappingResultsFile.println(record);
									}
									
									if (!counted.contains(mappingResultType)) {
										counters.get(mappingType).put(mappingResultType, counters.get(mappingType).get(mappingResultType) + 1);
										dataCoverage.get(mappingType).put(mappingResultType, dataCoverage.get(mappingType).get(mappingResultType) + sourceDrug.getCount());
										counted.add(mappingResultType);
									}
									
									mappingResultType++;
								}
							}
						}
					}
					mappingType++;
				}
			}

			DrugMappingFileUtilities.closeOutputFile(drugMappingFile);
			DrugMappingFileUtilities.closeOutputFile(drugMappingResultsFile);
			DrugMappingFileUtilities.closeOutputFile(drugMappingDebugFile);
			
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
	}
	
	
	private void finalReport() {
		Long dataCountTotal = 0L;
		Long dataCoverageTotal = 0L;
		Long mappingTotal = 0L;
		Long dataCoverageIngredients = 0L;
		Long mappingClinicalDrugs = 0L;
		Long dataCoverageClinicalDrugs = 0L;
		Long mappingClinicalDrugComps = 0L;
		Long dataCoverageClinicalDrugComps = 0L;
		Long mappingClinicalDrugForms = 0L;
		Long dataCoverageClinicalDrugForms = 0L;
		Long mappingClinicalDrugCompsIngredients = 0L;
		Long dataCoverageClinicalDrugCompsIngredients = 0L;
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			boolean allIngredientsMapped = true;
			for (SourceIngredient sourceIngredient : sourceDrug.getIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {
					allIngredientsMapped = false;
					break;
				}
			}
			if (allIngredientsMapped) {
				dataCoverageIngredients += sourceDrug.getCount();
			}
			
			dataCountTotal += sourceDrug.getCount();
						
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING).get(0).get(MAPPED) != null) {
					mappingClinicalDrugs++;
					dataCoverageClinicalDrugs += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING).get(0).get(MAPPED) != null) {
					mappingClinicalDrugComps++;
					dataCoverageClinicalDrugComps += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING).get(0).get(MAPPED) != null) {
					mappingClinicalDrugForms++;
					dataCoverageClinicalDrugForms += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING) != null) {
				List<Map<Integer, List<CDMConcept>>> mappingResultsList = sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING);
				if (mappingResultsList != null) {
					for (Map<Integer, List<CDMConcept>> mappingResult : mappingResultsList) {
						if (mappingResult.get(MAPPED) != null) {
							mappingClinicalDrugCompsIngredients++;
							dataCoverageClinicalDrugCompsIngredients += sourceDrug.getCount();
							break;
						}
					}
				}
			}
		}
		
		dataCoverageTotal = dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms + dataCoverageClinicalDrugCompsIngredients;
		mappingTotal = mappingClinicalDrugs + mappingClinicalDrugComps + mappingClinicalDrugForms + mappingClinicalDrugCompsIngredients;
		
		report.add("");
		report.add("Source drugs mapped to single CDM Clinical Drug: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugs, (long) sourceDrugs.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugComps, (long) sourceDrugs.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Form: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugForms, (long) sourceDrugs.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp/Ingredient: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugCompsIngredients, (long) sourceDrugs.size()));
		report.add("Total Source drugs mapped: " + DrugMappingNumberUtilities.percentage((long) mappingTotal, (long) sourceDrugs.size()));
		
		if (dataCountTotal != 0) {
			report.add("");
			report.add("Datacoverage Source drugs with all ingredients mapped: " + DrugMappingNumberUtilities.percentage((long) dataCoverageIngredients, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugs, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Comp mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugComps, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Form mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugForms, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Comp/Ingredient mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugCompsIngredients, (long) dataCountTotal));
			report.add("Total datacoverage drug mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageTotal, (long) dataCountTotal));
		}
		else {
			report.add("");
			report.add("No datacoverage counts available.");
		}
		
		System.out.println();
		for (String reportLine : report) {
			System.out.println(reportLine);
		}
	}
	
	
	private List<CDMIngredientStrength> matchingIngredients(List<SourceDrugComponent> sourceDrugComponents, Map<String, List<CDMIngredientStrength>> cdmIngredientsMap, Double strengthDeviationPercentage) {
		List<CDMIngredientStrength> matchingIngredients = new ArrayList<CDMIngredientStrength>();
		matchingIngredients = new ArrayList<CDMIngredientStrength>();
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
			if (sourceIngredient != null) {
				String cdmIngredientConceptId = sourceIngredient.getMatchingIngredient();
				if (cdmIngredientConceptId != null) {
					List<CDMIngredientStrength> matchingCDMIngredients = cdmIngredientsMap.get(cdmIngredientConceptId);
					if (matchingCDMIngredients != null) {
						if (strengthDeviationPercentage != null) {
							boolean found = false;
							for (CDMIngredientStrength cdmIngredientStrength : matchingCDMIngredients) {
								if (sourceDrugComponent.matches(unitConversionsMap, cdmIngredientStrength.getNumeratorDosage(), cdmIngredientStrength.getNumeratorDosageUnit(), cdmIngredientStrength.getDenominatorDosage(), cdmIngredientStrength.getDenominatorDosageUnit(), strengthDeviationPercentage, cdm)) {
									matchingIngredients.add(cdmIngredientStrength);
									found = true;
									break;
								}
							}
							if (!found) {
								// No matching ingredients with matching strength
								matchingIngredients = null;
								break;
							}
						}
						else {
							Set<CDMIngredient> cdmIngredients = new HashSet<CDMIngredient>();
							for (CDMIngredientStrength cdmIngredientStrength : matchingCDMIngredients) {
								cdmIngredients.add(cdmIngredientStrength.getIngredient());
							}
							if (cdmIngredients.size() == 1) {
								matchingIngredients.add((CDMIngredientStrength) matchingCDMIngredients.toArray()[0]);
							}
							else {
								// No matching ingredients with matching strength
								matchingIngredients = null;
								break;
							}
						}
					}
					else {
						// No matching ingredients
						matchingIngredients = null;
						break;
					}
				}
				else {
					// Should not happen
					matchingIngredients = null;
					break;
				}
			}
			else {
				// Should not happen
				matchingIngredients = null;
				break;
			}
		}
		
		return matchingIngredients;
	}
	

	private List<CDMDrug> selectConcept(SourceDrug sourceDrug, List<CDMDrug> cdmDrugList, int mapping) {
		return selectConcept(sourceDrug, cdmDrugList, mapping, 0);
	}
	
	private List<CDMDrug> selectConcept(SourceDrug sourceDrug, List<CDMDrug> cdmDrugList, int mapping, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.addAll(cdmDrugList);
		Collections.sort(conceptList, new Comparator<CDMConcept>() {

			@Override
			public int compare(CDMConcept concept1, CDMConcept concept2) {
				return (concept1 == null ? "" : concept1.getConceptId()).compareTo(concept2 == null ? "" : concept2.getConceptId());
			}
		});
		conceptList = selectConcept(sourceDrug, mapping, conceptList);
		cdmDrugList = new ArrayList<CDMDrug>();
		for (CDMConcept concept : conceptList) {
			cdmDrugList.add((CDMDrug) concept);
		}
		
		return cdmDrugList;
	}
	

	private Set<CDMIngredient> selectConcept(Set<CDMIngredient> cdmIngredientSet) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.addAll(cdmIngredientSet);
		conceptList = selectConcept(null, -1, conceptList);
		cdmIngredientSet = new HashSet<CDMIngredient>();
		for (CDMConcept concept : conceptList) {
			cdmIngredientSet.add((CDMIngredient) concept);
		}
		return cdmIngredientSet;
	}
	

	private List<CDMConcept> selectConcept(SourceDrug sourceDrug, int mapping, List<CDMConcept> conceptList) {
		return selectConcept(sourceDrug, mapping, conceptList, 0);
	}
	
	
	private List<CDMConcept> selectConcept(SourceDrug sourceDrug, int mapping, List<CDMConcept> conceptList, int componentNr) {
		int resultType = -1;
		List<CDMConcept> remove;

		preferencesUsed = "";
		if (conceptList.size() > 1) {
			String vocabulary_id = null;
			if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
				vocabulary_id = "RxNorm";
				resultType = REJECTED_BY_RXNORM_PREFERENCE;
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + "RxNorm";
			}
			else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
				vocabulary_id = "RxNorm Extension";
				resultType = REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + "RxNorm Extension";
			}
			remove = new ArrayList<CDMConcept>();
			for (CDMConcept cdmConcept : conceptList) {
				if (!cdmConcept.getVocabularyId().equals(vocabulary_id)) {
					remove.add(cdmConcept);
				}
			}
			if ((remove.size() > 0) && (conceptList.size() != remove.size())) {
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, remove, componentNr);
				}
				conceptList.removeAll(remove);
			}
		}
		if (conceptList.size() > 1) {
			if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No")) {
				boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
				resultType = latest ? REJECTED_BY_LATEST_DATE_PREFERENCE : REJECTED_BY_EARLIEST_DATE_PREFERENCE;
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + (latest ? "Latest Date" : "Earliest Date");
				
				remove = new ArrayList<CDMConcept>();
				List<CDMConcept> lastConcepts = new ArrayList<CDMConcept>();
				int lastDate = -1;
				for (CDMConcept cdmConcept : conceptList) {
					try {
						Integer date = Integer.parseInt(cdmConcept.getValidStartDate().replaceAll("-",""));
						if (lastDate == -1) {
							lastConcepts.add(cdmConcept);
							lastDate = date;
						}
						else {
							if (latest ? (date > lastDate) : (date < lastDate)) {
								remove.addAll(lastConcepts);
								lastConcepts.clear();
								lastConcepts.add(cdmConcept);
								lastDate = date;
							}
							else if (date == lastDate) {
								lastConcepts.add(cdmConcept);
							}
							else {
								remove.add(cdmConcept);
							}
						}
					}
					catch (NumberFormatException e) {
						remove.add(cdmConcept);
					}
				}
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, remove, componentNr);
				}
				conceptList.removeAll(remove);
			}
		}
		if (conceptList.size() > 1) {
			if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No")) {
				boolean smallest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
				resultType = smallest ? REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE;
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + (smallest ? "Smallest concept_id" : "Greatest concept_id");
				
				remove = new ArrayList<CDMConcept>();
				CDMConcept lastConcept = null;
				int lastConceptId = Integer.MAX_VALUE; 
				for (CDMConcept cdmConcept : conceptList) {
					if (lastConcept == null) {
						lastConcept = cdmConcept;
						lastConceptId = Integer.parseInt(cdmConcept.getConceptId()); 
					}
					else {
						int conceptId = Integer.parseInt(cdmConcept.getConceptId());
						if ((smallest && (conceptId < lastConceptId)) || ((!smallest) && (conceptId > lastConceptId))) {
							lastConceptId = conceptId;
							remove.add(lastConcept);
							lastConcept = cdmConcept;
						}
						else {
							remove.add(cdmConcept);
						}
					}
				}
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, remove, componentNr);
				}
				conceptList.removeAll(remove);
			}
		}
		if (conceptList.size() > 1) {
			if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None")) {
				boolean first = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First");
				resultType = first ? REJECTED_BY_FIRST_PREFERENCE : REJECTED_BY_LAST_PREFERENCE;
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + (first ? "First" : "Last");
				remove = new ArrayList<CDMConcept>();
				if (first) {
					for (int nr = 1; nr < conceptList.size(); nr++) {
						remove.add(conceptList.get(nr));
					}
				}
				else {
					for (int nr = 0; nr < (conceptList.size() - 1); nr++) {
						remove.add(conceptList.get(nr));
					}
				}
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, remove, componentNr);
				}
				conceptList.removeAll(remove);
			}
		}
		
		return conceptList;
	}
	
	
	// Logging functions
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.add(null);
		logMappingResult(sourceDrug, mapping, resultType, conceptList);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, CDMDrug cdmDrug) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.add(cdmDrug);
		logMappingResult(sourceDrug, mapping, resultType, conceptList);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, Set<CDMDrug> cdmDrugSet) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.addAll(cdmDrugSet);
		logMappingResult(sourceDrug, mapping, resultType, conceptList);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, List<CDMDrug> cdmDrugList, int resultType) {
		logMappingResult(sourceDrug, mapping, cdmDrugList, resultType, 0);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, List<CDMDrug> cdmDrugList, int resultType, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.addAll(cdmDrugList);
		logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, CDMConcept cdmConcept, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.add(cdmConcept);
		logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, List<CDMConcept> conceptList) {
		logMappingResult(sourceDrug, mapping, resultType, conceptList, 0);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, List<CDMConcept> conceptList, int componentNr) {

		Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
		if (sourceDrugMappingResult == null) {
			sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<CDMConcept>>>>();
			sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
		}
		
		List<Map<Integer, List<CDMConcept>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
		if (mappingTypeResultsList == null) {
			mappingTypeResultsList = new ArrayList<Map<Integer, List<CDMConcept>>>();
			Map<Integer, List<CDMConcept>> mappingTypeResults = new HashMap<Integer, List<CDMConcept>>();
			mappingTypeResultsList.add(mappingTypeResults);
			sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
		}
		
		if (componentNr >= mappingTypeResultsList.size()) {
			for (int nr = mappingTypeResultsList.size(); nr <= componentNr; nr++) {
				if (nr >= mappingTypeResultsList.size()) {
					mappingTypeResultsList.add(new HashMap<Integer, List<CDMConcept>>());
				}
			}
		}
		Map<Integer, List<CDMConcept>> resultMapping = mappingTypeResultsList.get(componentNr);
		
		List<CDMConcept> orgConceptList = resultMapping.get(resultType);
		if (orgConceptList == null) {
			orgConceptList = new ArrayList<CDMConcept>();
			resultMapping.put(resultType, orgConceptList);
		}
		orgConceptList.addAll(conceptList);
		maxResultConcepts = Math.max(maxResultConcepts, conceptList.size());
	}
	
	
/*
	public static void main(String[] args) {
	}
*/
}
