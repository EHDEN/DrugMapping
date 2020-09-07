package org.ohdsi.drugmapping.genericmapping;

import java.io.PrintWriter;
import java.text.DecimalFormat;
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
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class GenericMapping extends Mapping {
	
	public static String LOGFILE_NAME = "DrugMapping Log.txt";
	
	private static double DEVIATION_MARGIN = 0.0000000001;

	// Mapping types
	// The mapping type values should start at 0 and incremented by 1.
	public static int CLINICAL_DRUG_MAPPING      = 0;
	public static int CLINICAL_DRUG_FORM_MAPPING = 1;
	public static int CLINICAL_DRUG_COMP_MAPPING = 2;
	public static int INGREDIENT_MAPPING         = 3;
	
	public static Map<Integer, String> mappingTypeDescriptions;
	
	// Mapping result types.
	// The mapping result type values for each mapping result type should incremented by 1.
	// Make sure that the MAPPED and NO_MAPPED result types are the last two.
	public static int DRUGS_WITH_MATCHING_INGREDIENTS                =  0; // CDM Drugs with matching ingredients.
	public static int DRUG_COMPS_WITH_MATCHING_INGREDIENT            =  1; // CDM Drug Comps with matching ingredient.
	public static int NO_SOURCE_INGREDIENTS                          =  2; // The source drug has no ingredients
	public static int UNMAPPED_SOURCE_INGREDIENTS                    =  3; // The source drug has unmapped ingredients
	public static int DOUBLE_INGREDIENT_MAPPING                      =  4; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	public static int NO_SOURCE_FORMULATION                          =  5; // The source drug has no formulation.
	public static int NO_SINGLE_INGREDIENT_DRUG                      =  6; // The source drug is not a single ingredient drug.
	public static int NO_DRUGS_WITH_MATCHING_INGREDIENTS             =  7; // There is no CDM drug with matching ingredients.
	public static int AVAILABLE_FORMS                                =  8; // The available forms.
	public static int REJECTED_BY_FORM                               =  9; // The CDM drugs are rejected because they have a different form than the source drug.
	public static int DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM       = 10; // CDM Drugs with matching ingredients and form.
	public static int REJECTED_BY_MAXIMUM_STRENGTH_DEVIATION         = 11; // The CDM drugs are rejected by maximum strength deviation of the ingredients.
	public static int REJECTED_BY_AVERAGE_STRENGTH_DEVIATION         = 12; // The CDM drugs are rejected by average strength deviation of the ingredients.
	public static int REJECTED_BY_FORM_PRIORITY                      = 13; // The CDM drugs are rejected because they have a lower priority form than other CDM drugs drug.
	public static int REJECTED_BY_UNSPECIFIED_UNITS                  = 14; // The CDM drugs are rejected because they have not all units specified.
	public static int REJECTED_BY_RXNORM_PREFERENCE                  = 15; // The CDM drugs are rejected because they are not in the preferred RxNorm vocabulary.
	public static int REJECTED_BY_RXNORM_EXTENSION_PREFERENCE        = 16; // The CDM drugs are rejected because they are not in the preferred RxNorm Extension vocabulary.
	public static int REJECTED_BY_ATC_PREFERENCE                     = 17; // The CDM drugs are rejected because they are not match on ATC code.
	public static int REJECTED_BY_LATEST_DATE_PREFERENCE             = 18; // The CDM drugs are rejected because they do not have the latest valid start date.
	public static int REJECTED_BY_EARLIEST_DATE_PREFERENCE           = 19; // The CDM drugs are rejected because they do not have the earliest recent valid start date.
	public static int REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE      = 20; // The CDM drugs are rejected because they do not have the smallest concept_id.
	public static int REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE      = 21; // The CDM drugs are rejected because they do not have the greatest concept_id.
	public static int NO_UNIQUE_MAPPING                              = 22; // There are several CDM drugs the source drug could be mapped to.
	public static int REJECTED_BY_FIRST_PREFERENCE                   = 23; // The CDM drugs are rejected because the first one found is taken.
	public static int REJECTED_BY_LAST_PREFERENCE                    = 24; // The CDM drugs are rejected because the last one found is taken.
	public static int OVERRULED_MAPPING                              = 25; // A mapping to a single CDM drug or a failing mapping is overruled by a manual mapping.
	public static int MAPPED                                         = 26; // The final mapping of the source drug to a CDM drug.
	public static int NO_MAPPING                                     = 27; // No mapping found.

	public static Map<Integer, String> mappingResultDescriptions;
	static {
		mappingResultDescriptions = new HashMap<Integer  , String>();
		mappingResultDescriptions.put(DRUGS_WITH_MATCHING_INGREDIENTS               , "Drugs with matching ingredients");
		mappingResultDescriptions.put(DRUG_COMPS_WITH_MATCHING_INGREDIENT           , "Drug Comps with matching ingredient");
		mappingResultDescriptions.put(NO_SOURCE_INGREDIENTS                         , "No source ingredients");
		mappingResultDescriptions.put(UNMAPPED_SOURCE_INGREDIENTS                   , "Unmapped source ingredients");
		mappingResultDescriptions.put(DOUBLE_INGREDIENT_MAPPING                     , "Double ingredient mapping");
		mappingResultDescriptions.put(NO_SOURCE_FORMULATION                         , "No source formulation");
		mappingResultDescriptions.put(NO_SINGLE_INGREDIENT_DRUG                     , "No single ingredient drug");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_INGREDIENTS            , "No drugs with matching ingredients");
		mappingResultDescriptions.put(AVAILABLE_FORMS                               , "The available forms");
		mappingResultDescriptions.put(REJECTED_BY_FORM                              , "Rejected by form");
		mappingResultDescriptions.put(DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM      , "Drugs with matching ingredients and form.");
		mappingResultDescriptions.put(REJECTED_BY_MAXIMUM_STRENGTH_DEVIATION        , "Rejected by maximum strength deviation");
		mappingResultDescriptions.put(REJECTED_BY_AVERAGE_STRENGTH_DEVIATION        , "Rejected by average strength deviation");
		mappingResultDescriptions.put(REJECTED_BY_FORM_PRIORITY                     , "Rejected by form priority");
		mappingResultDescriptions.put(REJECTED_BY_UNSPECIFIED_UNITS                 , "Rejected by unspecified units");
		mappingResultDescriptions.put(REJECTED_BY_RXNORM_PREFERENCE                 , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(REJECTED_BY_RXNORM_EXTENSION_PREFERENCE       , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(REJECTED_BY_ATC_PREFERENCE                    , "Rejected by ATC preference");
		mappingResultDescriptions.put(REJECTED_BY_LATEST_DATE_PREFERENCE            , "Rejected by latest valid start date");
		mappingResultDescriptions.put(REJECTED_BY_EARLIEST_DATE_PREFERENCE          , "Rejected by earliest valid start date");
		mappingResultDescriptions.put(REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE     , "Rejected by smallest concept_id");
		mappingResultDescriptions.put(REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE     , "Rejected by greatest concept_id");
		mappingResultDescriptions.put(NO_UNIQUE_MAPPING                             , "No unique mapping");
		mappingResultDescriptions.put(REJECTED_BY_FIRST_PREFERENCE                  , "Rejected because first is used");
		mappingResultDescriptions.put(REJECTED_BY_LAST_PREFERENCE                   , "Rejected because last is used");
		mappingResultDescriptions.put(OVERRULED_MAPPING                             , "Overruled mapping");
		mappingResultDescriptions.put(MAPPED                                        , "Mapped");
		mappingResultDescriptions.put(NO_MAPPING                                    , "No mapping found");
	}
	
	public static boolean isMapping              = false;
	public static boolean isSavingDrugMapping    = false;
	public static boolean isSavingDrugMappingLog = false;
	
	
	public static void setMappingTypes(boolean compBeforeForm) {

		CLINICAL_DRUG_MAPPING      = 0;
		CLINICAL_DRUG_COMP_MAPPING = compBeforeForm ? 1 : 2;
		CLINICAL_DRUG_FORM_MAPPING = compBeforeForm ? 2 : 1;
		INGREDIENT_MAPPING         = 3;
		
		mappingTypeDescriptions = new HashMap<Integer, String>();
		mappingTypeDescriptions.put(CLINICAL_DRUG_MAPPING     , "ClinicalDrug Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_COMP_MAPPING, "ClinicalDrugComp Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_FORM_MAPPING, "ClinicalDrugForm Mapping");
		mappingTypeDescriptions.put(INGREDIENT_MAPPING        , "Ingredient Mapping");
	}
	
	
	public static int getMappingTypeValue(String mappingTypeDescription) {
		int mappingTypeValue = -1;
		for (int key : mappingTypeDescriptions.keySet()) {
			if (mappingTypeDescriptions.get(key).equals(mappingTypeDescription)) {
				mappingTypeValue = key;
				break;
			}
		}
		return mappingTypeValue;
	}
	
	
	public static int getMappingResultValue(String mappingResultDescription) {
		int mappingResultValue = -1;
		for (int key : mappingResultDescriptions.keySet()) {
			if (mappingResultDescriptions.get(key).equals(mappingResultDescription)) {
				mappingResultValue = key;
				break;
			}
		}
		return mappingResultValue;
	}
	
	
	private static String standardizedAmount(SourceDrugComponent sourceDrugComponent) {
	    DecimalFormat format5 = new DecimalFormat("##########0.00000");
		String amount = sourceDrugComponent.getDosage() == null ? "" : format5.format(sourceDrugComponent.getDosage());
		if (!amount.equals("")) {
			amount = amount.contains(".") ? (amount + "00000").substring(0, amount.indexOf(".") + 6) : amount + ".00000";
		}
		return amount;
	}
	
	
	private MainFrame mainFrame = null;

	private Map<String, CDMIngredient> manualCASMappings = null;
	private Map<SourceIngredient, CDMIngredient> manualIngredientCodeOverruleMappings = null;
	private Map<SourceIngredient, String> manualIngredientCodeMappingRemarks = null;
	private Map<String, CDMIngredient> manualIngredientNameOverruleMappings = null;
	private Map<SourceIngredient, CDMIngredient> manualIngredientCodeFallbackMappings = null;
	private Map<SourceIngredient, String> manualIngredientCodeFallbackMappingRemarks = null;
	private Map<String, CDMIngredient> manualIngredientNameFallbackMappings = null;
	private Map<SourceDrug, CDMDrug> manualDrugMappings = null;
	
	private Source source = null;
	private CDM cdm = null;
	
	private UnitConversion unitConversionsMap = null;
	private FormConversion formConversionsMap = null;
	private IngredientNameTranslation ingredientNameTranslationMap = null;
	
	private Map<String, List<String>> externalCASSynonymsMap = null;
	
	private List<SourceDrug> sourceDrugsAllIngredientsMapped = null;
	private Map<SourceDrug, List<CDMIngredient>> sourceDrugsCDMIngredients = null;
	
	private Map<SourceIngredient, CDMIngredient> ingredientMap = null;
	
	private Set<SourceDrug> mappedSourceDrugs = null;
	private Map<String, Double> usedStrengthDeviationPercentageMap = null;
	
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> sourceDrugMappingResults = null;
	private Map<Integer, Set<SourceDrug>> notUniqueMapping = null;
	
	private List<String> report = null;
	
	private String preferencesUsed = "";
	
	
		
	
	public GenericMapping(
					MainFrame mainFrame,
					CDMDatabase database, 
					InputFile sourceDrugsFile, 
					InputFile ingredientNameTranslationFile, 
					InputFile unitMappingFile, 
					InputFile formMappingFile, 
					InputFile manualCASMappingFile, 
					InputFile manualIngredientOverruleMappingFile,
					InputFile manualIngredientFallbackMappingFile, 
					InputFile manualDrugMappingFile
					) {
		boolean ok = true;
		isMapping = true;
		
		this.mainFrame = mainFrame;
		
		setMappingTypes(DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_MATCH_COMP_FORM).equals("Comp before Form"));
		

		manualCASMappings = new HashMap<String, CDMIngredient>();
		manualIngredientCodeOverruleMappings = new HashMap<SourceIngredient, CDMIngredient>();
		manualIngredientCodeMappingRemarks = new HashMap<SourceIngredient, String>();
		manualIngredientNameOverruleMappings = new HashMap<String, CDMIngredient>();
		manualIngredientCodeFallbackMappings = new HashMap<SourceIngredient, CDMIngredient>();
		manualIngredientCodeFallbackMappingRemarks = new HashMap<SourceIngredient, String>();
		manualIngredientNameFallbackMappings = new HashMap<String, CDMIngredient>();
		manualDrugMappings = new HashMap<SourceDrug, CDMDrug>();
		
		sourceDrugsAllIngredientsMapped = new ArrayList<SourceDrug>();
		sourceDrugsCDMIngredients = new HashMap<SourceDrug, List<CDMIngredient>>();
		
		ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
		
		mappedSourceDrugs = new HashSet<SourceDrug>();
		usedStrengthDeviationPercentageMap = new HashMap<String, Double>();
		
		sourceDrugMappingResults = new HashMap<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>>(); // SourceDrug, Mapping, List of Mapping result, List of options
		notUniqueMapping = new HashMap<Integer, Set<SourceDrug>>();
		
		report = new ArrayList<String>();
		
		int mapping = 0;
		while (mappingTypeDescriptions.containsKey(mapping)) {
			notUniqueMapping.put(mapping, new HashSet<SourceDrug>());
			mapping++;
		}

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");

		// Load source drugs with ingredients
		source = new Source();
		ok = ok && source.loadSourceDrugs(sourceDrugsFile, report);
		
		// Get CDM Ingredients
		cdm = new CDM();
		ok = ok && cdm.LoadCDMFromDatabase(database, report);	

		if (ok) {
			// Get the ingredient name translation map
			boolean translationOk = getIngredientNameTranslationMap(ingredientNameTranslationFile);

			// Get unit conversion from local units to CDM units
			boolean unitsOk = getUnitConversion(unitMappingFile);

			// Get unit conversion from local dose forms to CDM dose forms
			boolean formsOk = getFormConversion(formMappingFile);

			ok = ok && translationOk && unitsOk && formsOk;
		}
		
		// Load manual CAS mappings
		ok = ok && getManualCASMappings(manualCASMappingFile);		
		
		// Load manual Ingredient Overrule mappings
		ok = ok && getManualIngredientMappings(manualIngredientOverruleMappingFile, "overrule");		
		
		// Load manual Ingredient Fallback mappings
		ok = ok && getManualIngredientMappings(manualIngredientFallbackMappingFile, "fallback");
		
		// Load manual drug mappings
		ok = ok && getManualDrugMappings(manualDrugMappingFile);
		
		// Match ingredients by ATC and full name
		ok = ok && matchIngredients();
		
		// Get source drugs with all ingredients mapped
		ok = ok && getSourceDrugsWithAllIngredientsMapped();
		
		// Match source drugs to Clinical Drugs
		ok = ok && matchClinicalDrugs();
		
		if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_MATCH_COMP_FORM).equals("Comp before Form")) {
			// Match source drugs to Clinical Drug Comps
			ok = ok && matchClinicalDrugComps();
			
			// Match source drugs to Clinical Drug Forms
			ok = ok && matchClinicalDrugForms();
		}
		else {
			// Match source drugs to Clinical Drug Forms
			ok = ok && matchClinicalDrugForms();
			
			// Match source drugs to Clinical Drug Comps
			ok = ok && matchClinicalDrugComps();
		}
		
		// Match source drug ingredients to Clinical Drug Comps or Ingredients
		ok = ok && matchClinicalDrugCompsIngredients();
		
		// Create the final report
		if (ok && (report != null)) finalReport();
		
		isMapping = false;

		// Showing Drugs List
		if (ok) showDrugsList();

		// Save mapping
		if (ok) saveMapping();

		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
	
	
	private boolean getIngredientNameTranslationMap(InputFile ingredientNameTranslationFile) {
		boolean ok = true;
		
		// Create Translation Map
		ingredientNameTranslationMap = new IngredientNameTranslation(ingredientNameTranslationFile);
		if (ingredientNameTranslationMap.getStatus() != FormConversion.STATE_OK) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the ingredient name translation map in the file:");
			System.out.println("");
			System.out.println(ingredientNameTranslationMap.getFileName());
			System.out.println("");
			ok = false;
		}
		else {
			for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
				sourceIngredient.setIngredientNameEnglish(ingredientNameTranslationMap.getNameIngredientNameEnglish(sourceIngredient.getIngredientName()));
			}
		}
		
		return ok;
	}
	
	
	private boolean getUnitConversion(InputFile unitMappingFile) {
		boolean ok = true;
		
		// Create Units Map
		unitConversionsMap = new UnitConversion(unitMappingFile);
		if (unitConversionsMap.getStatus() != UnitConversion.STATE_OK) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the unit conversion map in the file:");
			System.out.println("");
			System.out.println(unitConversionsMap.getFileName());
			System.out.println("");
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getFormConversion(InputFile formMappingFile) {
		boolean ok = true;
		
		// Create Units Map
		formConversionsMap = new FormConversion(formMappingFile, cdm);
		if (formConversionsMap.getStatus() != FormConversion.STATE_OK) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the dose form conversion map in the file:");
			System.out.println("");
			System.out.println(formConversionsMap.getFileName());
			System.out.println("");
			ok = false;
		}
		
		return ok;
	}
	
	
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
									if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
										System.out.println("      WARNING: No CDM Ingredient found for concept_id " + cdmConceptId + " for CAS number " + casNumber + " in line " + lineNr + ".");
									}
								}
							}
							else {
								if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
									System.out.println("      WARNING: No concept_id found in line " + lineNr + ".");
								}
							}
						}
						else {
							if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
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
	
	
	private boolean getManualIngredientMappings(InputFile manualMappingFile, String type) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + "     Loading manual ingredient " + type + " mappings ...");
		if ((manualMappingFile != null) && manualMappingFile.isSelected()) {
			Map<SourceIngredient, CDMIngredient> codeMappings = manualIngredientCodeOverruleMappings;
			Map<SourceIngredient, String> codeMappingRemarks = manualIngredientCodeMappingRemarks;
			Map<String, CDMIngredient> nameMappings = manualIngredientNameOverruleMappings;
			if (type.equals("fallback")) {
				codeMappings = manualIngredientCodeFallbackMappings;
				codeMappingRemarks = manualIngredientCodeFallbackMappingRemarks;
				nameMappings = manualIngredientNameFallbackMappings;
			}
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
									SourceIngredient sourceIngredient = Source.getIngredient(sourceCode);
									if (sourceIngredient != null) {
										codeMappings.put(sourceIngredient, cdmIngredient);
										codeMappingRemarks.put(sourceIngredient, remark);
									}
									else {
										if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
											System.out.println("      WARNING: No source ingredient found for sourceCode " + sourceCode + " in line " + lineNr + ".");
										}
									}
								}
								if (!sourceName.equals("")) {
									sourceNameFound = true;
									nameMappings.put(sourceName, cdmIngredient);
								}
								if ((!sourceCodeFound) && (!sourceNameFound)) {
									if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
										System.out.println("      WARNING: No sourceCode and no sourceName found in line " + lineNr + ".");
									}
								}
							}
							else {
								if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
									System.out.println("      WARNING: No CDM Ingredient found for concept_id " + cdmConceptId + " in line " + lineNr + ".");
								}
							}
						}
						else {
							if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
								System.out.println("      WARNING: No concept_id found in line " + lineNr + ".");
							}
						}
					}
				}
				else {
					System.out.println("    No manual ingredient " + type + " mappings found.");
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No manual ingredient " + type + " mappings used.");
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
						
						SourceDrug sourceDrug = source.getSourceDrug(sourceCode);
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
	
	
	private boolean matchIngredients() {
		boolean ok = true;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match Ingredients");
		
		Integer multipleMappings = 0;
		
		multipleMappings += matchIngredientsByCASNumber();
		
		multipleMappings += matchIngredientsByName();
		
		multipleMappings += matchIngredientsByATC();

		if (report != null) {
			report.add("Source ingredients mapped total: " + DrugMappingNumberUtilities.percentage((long) ingredientMap.size(), (long) Source.getAllIngredients().size()));
			report.add("Multiple mappings found: " + DrugMappingNumberUtilities.percentage((long) multipleMappings, (long) Source.getAllIngredients().size()));
		}
		
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
			
			for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {
					
					String casNr = sourceIngredient.getCASNumber();
					if (casNr != null) {
						String matchString = null;
						CDMIngredient cdmIngredient = cdm.getCDMCASIngredientMap().get(casNr);
						CDMIngredient cdmIngredientManual = manualCASMappings.get(casNr);
						if (cdmIngredient != null) {
							if (cdmIngredientManual != null) {
								if (cdmIngredient != cdmIngredientManual) {
									cdmIngredient = cdmIngredientManual;
									matchString = "MANUAL CAS OVERRULED CDM CAS";
									matchedManualByCASNumber++;
								}
								else {
									matchString = "CDM CAS - MANUAL CAS OBSOLETE";
									matchedByCDMCASNumber++;
								}
							}
							else {
								matchString = "CDM CAS";
								matchedByCDMCASNumber++;
							}
						}
						else {
							if (cdmIngredientManual != null) {
								cdmIngredient = cdmIngredientManual;
								matchString = "MANUAL CAS";
								matchedManualByCASNumber++;
							}
						}
						if (cdmIngredient != null) {
							ingredientMap.put(sourceIngredient, cdmIngredient);
							sourceIngredient.setMatchingIngredient(cdmIngredient);
							sourceIngredient.setMatchString(matchString);
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
				for (SourceDrug sourceDrug : source.getSourceDrugs()) {
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
					CDMIngredient concept = sourceIngredient.getMatchingIngredient();
					if (concept != null) {
						String concept_id = concept.getConceptId();
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

			if (report != null) {
				report.add("Source ingredients mapped manually by CAS number: " + DrugMappingNumberUtilities.percentage((long) matchedManualByCASNumber, (long) Source.getAllIngredients().size()));
				report.add("Source ingredients mapped by CDM CAS number: " + DrugMappingNumberUtilities.percentage((long) matchedByCDMCASNumber, (long) Source.getAllIngredients().size()));
			}
			System.out.println(DrugMapping.getCurrentTime() + "       Done");
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "       No CDM CAS mapping found.");
		}
		
		
		if (externalCASSynonymsMap != null) {
			System.out.println(DrugMapping.getCurrentTime() + "       Match ingredients by external CAS number ...");
			
			for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
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
											sourceIngredient.setMatchingIngredient(((CDMIngredient) matchedCDMIngredients.toArray()[0]));
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
				}
			}

			if (report != null) {
				report.add("Source ingredients mapped by external CAS number: " + DrugMappingNumberUtilities.percentage((long) matchedByExternalCASName, (long) Source.getAllIngredients().size()));
			}
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
		
		for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
			boolean multipleMapping = false;
			
			if (sourceIngredient.getMatchingIngredient() == null) {
				
				CDMIngredient cdmIngredient = manualIngredientCodeOverruleMappings.get(sourceIngredient);
				
				if (cdmIngredient != null) { // Manual mapping on ingredient code found
					ingredientMap.put(sourceIngredient, cdmIngredient);
					sourceIngredient.setMatchingIngredient(cdmIngredient);
					sourceIngredient.setMatchString("MANUAL CODE OVERRULED " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
					matchedManualCode++;
				}
				
				if (cdmIngredient == null) { // No manual mapping on ingredient code found
					if (!sourceIngredient.getIngredientName().equals("")) {
						cdmIngredient = manualIngredientNameOverruleMappings.get(sourceIngredient.getIngredientName());
						if (cdmIngredient != null) {
							ingredientMap.put(sourceIngredient, cdmIngredient);
							sourceIngredient.setMatchingIngredient(cdmIngredient);
							sourceIngredient.setMatchString("MANUAL NAME OVERRULED " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
							matchedManualCode++;
						}
					}
				}
				
				if (cdmIngredient == null) { // No manual mapping on ingredient name found
					preferencesUsed = "";

					List<String> matchNameList = sourceIngredient.getIngredientMatchingNames();
					for (String matchName : matchNameList) {
						String matchType = matchName.substring(0, matchName.indexOf(": "));
						matchName = matchName.substring(matchName.indexOf(": ") + 2);
						/*
						cdmIngredient = manualIngredientNameOverruleMappings.get(matchName);
						
						if (cdmIngredient != null) { // Manual mapping on part ingredient name found
							ingredientMap.put(sourceIngredient, cdmIngredient);
							sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
							sourceIngredient.setMatchString("MANUAL NAME: " + matchName);
							matchedManualName++;
							break;
						}
						*/
						List<String> matchList = new ArrayList<String>();
						matchList.add(matchType + ": " + matchName);
						matchList.add(matchType + " Standardized: " + DrugMappingStringUtilities.modifyName(matchName));
						matchList.add(matchType + " Sorted: " + DrugMappingStringUtilities.sortWords(matchName));
						matchList.add(matchType + " Standardized: " + DrugMappingStringUtilities.modifyName(matchName));
						matchList.add(matchType + " Sorted Standardized: " + DrugMappingStringUtilities.modifyName(DrugMappingStringUtilities.sortWords(matchName)));

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
										sourceIngredient.setMatchingIngredient(cdmIngredient);
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
				if (cdmIngredient == null) { // No mapping found
					cdmIngredient = manualIngredientCodeFallbackMappings.get(sourceIngredient);
					
					if (cdmIngredient != null) { // Manual mapping on ingredient code found
						ingredientMap.put(sourceIngredient, cdmIngredient);
						sourceIngredient.setMatchingIngredient(cdmIngredient);
						sourceIngredient.setMatchString("MANUAL CODE FALLBACK " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
						matchedManualCode++;
					}
					
					if (cdmIngredient == null) { // No manual mapping on ingredient code found
						if (!sourceIngredient.getIngredientName().equals("")) {
							cdmIngredient = manualIngredientNameFallbackMappings.get(sourceIngredient.getIngredientName());
							if (cdmIngredient != null) {
								ingredientMap.put(sourceIngredient, cdmIngredient);
								sourceIngredient.setMatchingIngredient(cdmIngredient);
								sourceIngredient.setMatchString("MANUAL NAME FALLBACK " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
								matchedManualCode++;
							}
						}
					}
					
					if ((cdmIngredient != null) && (multipleMapping)) {
						multipleMappings--;
					}
				}
			}
			else {
				CDMIngredient cdmIngredientManualCode = manualIngredientCodeOverruleMappings.get(sourceIngredient);
				
				if (cdmIngredientManualCode != null) { // Manual mapping on ingredient code found
					ingredientMap.put(sourceIngredient, cdmIngredientManualCode);
					sourceIngredient.setMatchingIngredient(cdmIngredientManualCode);
					sourceIngredient.setMatchString("MANUAL CODE OVERRULED " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
					matchedManualCode++;
				}
				
				if (cdmIngredientManualCode == null) { // No manual mapping on ingredient code found
					if (!sourceIngredient.getIngredientName().equals("")) {
						CDMIngredient cdmIngredientManualName = manualIngredientNameOverruleMappings.get(sourceIngredient.getIngredientName());
						if (cdmIngredientManualName != null) {
							ingredientMap.put(sourceIngredient, cdmIngredientManualName);
							sourceIngredient.setMatchingIngredient(cdmIngredientManualName);
							sourceIngredient.setMatchString("MANUAL NAME OVERRULED " + manualIngredientCodeMappingRemarks.get(sourceIngredient));
							matchedManualName++;
						}
					}
				}
			}
		}

		if (report != null) {
			report.add("Source ingredients mapped manually by code: " + DrugMappingNumberUtilities.percentage((long) matchedManualCode, (long) Source.getAllIngredients().size()));
			report.add("Source ingredients mapped manually by name: " + DrugMappingNumberUtilities.percentage((long) matchedManualName, (long) Source.getAllIngredients().size()));
			report.add("Source ingredients mapped by name: " + DrugMappingNumberUtilities.percentage((long) matchedByName, (long) Source.getAllIngredients().size()));
		}
		System.out.println(DrugMapping.getCurrentTime() + "       Done");
		
		return multipleMappings;
	}
	
	
	private Integer matchIngredientsByATC() {
		Integer matchedByATC = 0;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by ATC ...");
		/*
		for (SourceDrug sourceDrug : sourceDrugs) {
			List<SourceIngredient> sourceIngredients = sourceDrug.getIngredients();
			if ((sourceDrug.getATCCodes() != null) && (sourceDrug.getATCCodes().size() > 0) && (sourceIngredients.size() == 1)) {
				SourceIngredient sourceIngredient = sourceIngredients.get(0);
				if (sourceIngredient.getMatchingIngredient() == null) {
					String matchingATCCodes = "ATC:";
					Set<CDMIngredient> cdmATCIngredients = new HashSet<CDMIngredient>();
					for (String sourceATCCode : sourceDrug.getATCCodes()) {
						Set<CDMIngredient> matchingIngredients = cdm.getCDMATCIngredientMap().get(sourceATCCode);
						if ((matchingIngredients != null) && (matchingIngredients.size() > 0)) {
							matchingATCCodes = matchingATCCodes + " " + sourceATCCode;
							cdmATCIngredients.addAll(matchingIngredients);
						}
					}
					if ((cdmATCIngredients != null) && (cdmATCIngredients.size() == 1)) {
						CDMIngredient cdmIngredient = (CDMIngredient) cdmATCIngredients.toArray()[0];
						ingredientMap.put(sourceIngredient, cdmIngredient);
						sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
						sourceIngredient.setMatchString(matchingATCCodes);
						matchedByATC++;
					}
				}
			}
		}
		*/
		
		for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
			if (sourceIngredient.getMatchingIngredient() == null) {
				Set<CDMIngredient> cdmATCIngredients = new HashSet<CDMIngredient>();
				Set<SourceDrug> ingredientDrugs = sourceIngredient.getSourceDrugs();
				Set<String> atcCodes = new HashSet<String>();
				for (SourceDrug sourceDrug : ingredientDrugs) {
					if (sourceDrug.getIngredients().size() == 1) {
						atcCodes.addAll(sourceDrug.getATCCodes());
					}
				}
				String matchingATCCodes = "ATC:";
				for (String atcCode : atcCodes) {
					Set<CDMIngredient> cdmIngredients = cdm.getCDMATCIngredientMap().get(atcCode);
					
					if (cdmIngredients != null) {
						cdmATCIngredients.addAll(cdm.getCDMATCIngredientMap().get(atcCode));
					}
					if (cdmATCIngredients.size() > 1) {
						multipleMappings++;
						break;
					}
					matchingATCCodes = matchingATCCodes + " " + atcCode;
				}
				if (cdmATCIngredients.size() == 1) {
					CDMIngredient cdmIngredient = (CDMIngredient) cdmATCIngredients.toArray()[0];
					ingredientMap.put(sourceIngredient, cdmIngredient);
					sourceIngredient.setMatchingIngredient(cdmIngredient);
					sourceIngredient.setMatchString(matchingATCCodes);
					matchedByATC++;
				}
			}
		}
		

		if (report != null) {
			report.add("Source ingredients mapped by ATC: " + DrugMappingNumberUtilities.percentage((long) matchedByATC, (long) Source.getAllIngredients().size()));
		}
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return multipleMappings;
	}
	
	
	private boolean getSourceDrugsWithAllIngredientsMapped() {
		System.out.println(DrugMapping.getCurrentTime() + "     Get source drugs with all ingredients mapped ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
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
		
		if (report != null) {
			report.add("Source drugs with all ingredients mapped: " + DrugMappingNumberUtilities.percentage((long) sourceDrugsAllIngredientsMapped.size(), (long) source.getSourceDrugs().size()));
			report.add("");
		}
		
		return (sourceDrugsAllIngredientsMapped.size() > 0);
	}
	
	
	private boolean matchClinicalDrugs() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drugs ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			CDMDrug automaticMapping = null;
			Double usedStrengthDeviationPercentage = null;
			preferencesUsed = "";
			
			if (sourceDrugsAllIngredientsMapped.contains(sourceDrug)) {
				List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);
				
				if (sourceDrug.getFormulations().size() > 0) {
					if (sourceDrugCDMIngredients.size() > 0) {
						// Find CDM Clinical Drugs with corresponding ingredients
						List<CDMIngredient> foundIngredients = new ArrayList<CDMIngredient>();
						List<CDMDrug> cdmDrugsWithIngredients = null;
						for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
							List<CDMDrug> cdmDrugsWithIngredient = cdm.getCDMDrugsContainingIngredient(sourceDrugCDMIngredients.size(), cdmIngredient);
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
										if (!cdmDrugsWithIngredient.contains(cdmDrug)) {
											cdmDrugsMissingIngredient.add(cdmDrug);
										}
										else {
											List<CDMIngredient> cdmDrugIngredientsList = new ArrayList<CDMIngredient>();
											cdmDrugIngredientsList.addAll(cdmDrug.getIngredients());
											for (CDMIngredient foundIngredient : foundIngredients) {
												cdmDrugIngredientsList.remove(foundIngredient);
											}
											if (!cdmDrugIngredientsList.contains(cdmIngredient)) {
												cdmDrugsMissingIngredient.add(cdmDrug);
											}
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
							foundIngredients.add(cdmIngredient);
						}
						logMappingResult(sourceDrug, mapping, cdmDrugsWithIngredients, DRUGS_WITH_MATCHING_INGREDIENTS);
						
						Set<CDMConcept> availableForms = new HashSet<CDMConcept>();
						for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
							List<String> cdmDrugFormIds = cdmDrug.getFormConceptIds();
							if (cdmDrugFormIds != null) {
								for (String cdmDrugFormId : cdmDrugFormIds) {
									availableForms.add(cdm.getCDMFormConcept(cdmDrugFormId));
								}
							}
 						}
						logMappingResult(sourceDrug, mapping, availableForms, AVAILABLE_FORMS);

						// Remove all drugs with the wrong form
						Set<CDMConcept> rejectedByForm = new HashSet<CDMConcept>();
						List<String> sourceDrugForms = sourceDrug.getFormulations();
						List<String> matchingCDMForms = new ArrayList<String>();
						for (String sourceDrugForm : sourceDrugForms) {
							List<String> matchingCDMFormList = formConversionsMap.getMatchingForms(sourceDrugForm);
							if (matchingCDMFormList != null) {
								for (String matchingCDMForm : matchingCDMFormList) {
									if (!matchingCDMForms.contains(matchingCDMForm)) {
										matchingCDMForms.add(matchingCDMForm);
									}
								}
							}
						}		
						if (matchingCDMForms.size() > 0) {
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								List<String> cdmDrugForms = cdmDrug.getFormConceptNames();
								List<String> formOverlap = new ArrayList<String>();
								if (cdmDrugForms != null) {
									for (String cdmDrugForm : cdmDrugForms) {
										if (matchingCDMForms.contains(cdmDrugForm)) {
											formOverlap.add(cdmDrugForm);
										}
									}
									if (formOverlap.size() == 0) {
										rejectedByForm.add(cdmDrug);
									}
								}
							}
						}
						else {
							rejectedByForm.addAll(cdmDrugsWithIngredients);
						}
						cdmDrugsWithIngredients.removeAll(rejectedByForm);
						logMappingResult(sourceDrug, mapping, rejectedByForm, REJECTED_BY_FORM);
						
						if (cdmDrugsWithIngredients.size() > 0) {
							logMappingResult(sourceDrug, mapping, cdmDrugsWithIngredients, DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM);
													
							// Find CDM Clinical Drugs with corresponding ingredient strengths
							Set<CDMConcept> rejectedByStrength = new HashSet<CDMConcept>();
							
							usedStrengthDeviationPercentage = null;
							Map<Double, List<CDMDrug>> strengthMatchingCDMDrugs = new HashMap<Double, List<CDMDrug>>();
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
									Double deviationPercentage = matchingStrength(sourceDrug, cdmDrug);
									if (deviationPercentage != null) {
										List<CDMDrug> deviationPercentageList = strengthMatchingCDMDrugs.get(deviationPercentage);
										if (deviationPercentageList == null) {
											deviationPercentageList = new ArrayList<CDMDrug>();
											strengthMatchingCDMDrugs.put(deviationPercentage, deviationPercentageList);
										}
										deviationPercentageList.add(cdmDrug);
									}
									else {
										rejectedByStrength.add(cdmDrug);
									}
								}
								else {
									rejectedByStrength.add(cdmDrug);
								}
							}
							
							List<Double> foundDeviationPercentages = new ArrayList<Double>();
							foundDeviationPercentages.addAll(strengthMatchingCDMDrugs.keySet());
							Collections.sort(foundDeviationPercentages);
							
							// Find the drug with a matching formulation
							List<CDMDrug> cdmDrugsWithIngredientsAndForm = new ArrayList<CDMDrug>();

							if ((matchingCDMForms != null) && (matchingCDMForms.size() > 0)) {
								String foundForm = null; 
								for (Double deviationPercentage : foundDeviationPercentages) {
									for (String cdmForm : matchingCDMForms) {
										List<CDMDrug> deviationPercentageDrugs = getLowestAverageStrengthDeviation(mapping, sourceDrug, strengthMatchingCDMDrugs.get(deviationPercentage));
										
										// Match by form with priority
										for (CDMDrug cdmDrug : deviationPercentageDrugs) {
											List<String> cdmDrugForms = cdmDrug.getFormConceptNames();
											if (cdmDrugForms.contains(cdmForm)) {
												cdmDrugsWithIngredientsAndForm.add(cdmDrug);
												foundForm = cdmForm;
											}
										}
										if (cdmDrugsWithIngredientsAndForm.size() > 0) {
											usedStrengthDeviationPercentage = deviationPercentage;
											break;
										}
									}
									if (cdmDrugsWithIngredientsAndForm.size() > 0) {
										usedStrengthDeviationPercentage = deviationPercentage;
										break;
									}
								}
								
								// Logging administration
								Set<CDMConcept> rejectedByFormPriority = new HashSet<CDMConcept>();
								for (Double deviationPercentage : foundDeviationPercentages) {
									for (CDMDrug cdmDrug : strengthMatchingCDMDrugs.get(deviationPercentage)) {
										if (!cdmDrugsWithIngredientsAndForm.contains(cdmDrug)) {
											if (cdmDrug.getFormConceptIds().contains(foundForm)) {
												rejectedByStrength.add(cdmDrug);
											}
											else {
												rejectedByFormPriority.add(cdmDrug);
											}
										}
									}
								}
								logMappingResult(sourceDrug, mapping, rejectedByFormPriority, REJECTED_BY_FORM_PRIORITY);
							}
							
							logMappingResult(sourceDrug, mapping, rejectedByStrength, REJECTED_BY_MAXIMUM_STRENGTH_DEVIATION);
							
							if (cdmDrugsWithIngredientsAndForm.size() == 1) {
								automaticMapping = cdmDrugsWithIngredientsAndForm.get(0);
								sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage);
							}
							else if (cdmDrugsWithIngredientsAndForm.size() > 1) {
								List<CDMDrug> selectedCDMDrugsWithIngredientsAndForm = selectConcept(sourceDrug, cdmDrugsWithIngredientsAndForm, mapping);
								if (selectedCDMDrugsWithIngredientsAndForm.size() > 1) {
									logMappingResult(sourceDrug, mapping, selectedCDMDrugsWithIngredientsAndForm, NO_UNIQUE_MAPPING);
									notUniqueMapping.get(mapping).add(sourceDrug);
								}
								else {
									automaticMapping = selectedCDMDrugsWithIngredientsAndForm.get(0);
									sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage + " " + preferencesUsed);
								}
							}
							else {
								logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS);
							}
						}
						else {
							logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS);
						}
					}
				}
				else {
					logMappingResult(sourceDrug, mapping, NO_SOURCE_FORMULATION);
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
			else {
				logMappingResult(sourceDrug, mapping, NO_MAPPING);
			}
		}

		if (report != null) {
			report.add("Source drugs mapped to multiple CDM Clinical Drugs: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) source.getSourceDrugs().size()));
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugComps() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_COMP_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Comps ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
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
					preferencesUsed = "";
					if (sourceDrug.getIngredients().size() == 1) { // Clinical Drug Comp is always single ingredient
						List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

						// Find CDM Clinical Drug Comps with corresponding ingredient
						List<CDMDrug> cdmDrugCompsWithIngredients = null;
						for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
							List<CDMDrug> cdmDrugCompsWithIngredient = cdm.getCDMDrugCompsContainingIngredient(cdmIngredient);
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
						
						// Find CDM Clinical Drugs with corresponding ingredient strengths
						if ((cdmDrugCompsWithIngredients != null) && (cdmDrugCompsWithIngredients.size() > 0)) {
							logMappingResult(sourceDrug, mapping, cdmDrugCompsWithIngredients, DRUGS_WITH_MATCHING_INGREDIENTS);
							
							List<CDMDrug> matchingCDMDrugs = null;
							Set<CDMConcept> rejectedByStrength = new HashSet<CDMConcept>();
							
							usedStrengthDeviationPercentage = null;
							Map<Double, List<CDMDrug>> strengthMatchingCDMDrugs = new HashMap<Double, List<CDMDrug>>();
							for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
								if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
									Double deviationPercentage = matchingStrength(sourceDrug, cdmDrug);
									if (deviationPercentage != null) {
										List<CDMDrug> deviationPercentageList = strengthMatchingCDMDrugs.get(deviationPercentage);
										if (deviationPercentageList == null) {
											deviationPercentageList = new ArrayList<CDMDrug>();
											strengthMatchingCDMDrugs.put(deviationPercentage, deviationPercentageList);
										}
										deviationPercentageList.add(cdmDrug);
										usedStrengthDeviationPercentage = Math.min(usedStrengthDeviationPercentage == null ? DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION) : usedStrengthDeviationPercentage, deviationPercentage);
									}
									else {
										rejectedByStrength.add(cdmDrug);
									}
								}
								else {
									rejectedByStrength.add(cdmDrug);
								}
							}
							if (usedStrengthDeviationPercentage != null) {
								matchingCDMDrugs = strengthMatchingCDMDrugs.get(usedStrengthDeviationPercentage);
								for (double deviationPercentage : strengthMatchingCDMDrugs.keySet()) {
									if (deviationPercentage != (double) usedStrengthDeviationPercentage) {
										rejectedByStrength.addAll(strengthMatchingCDMDrugs.get(deviationPercentage));
									}
								}
							}
							logMappingResult(sourceDrug, mapping, rejectedByStrength, REJECTED_BY_MAXIMUM_STRENGTH_DEVIATION);

							if (matchingCDMDrugs != null) {
								if (matchingCDMDrugs.size() == 1) {
									automaticMapping = matchingCDMDrugs.get(0);
									sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage);
								}
								else if (matchingCDMDrugs.size() > 1) {
									List<CDMDrug> selectedCDMDrugsWithIngredientsAndStrength = selectConcept(sourceDrug, matchingCDMDrugs, mapping);
									if (selectedCDMDrugsWithIngredientsAndStrength.size() > 1) {
										logMappingResult(sourceDrug, mapping, selectedCDMDrugsWithIngredientsAndStrength, NO_UNIQUE_MAPPING);
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
									else {
										automaticMapping = selectedCDMDrugsWithIngredientsAndStrength.get(0);
										sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage + " " + preferencesUsed);
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
					else {
						logMappingResult(sourceDrug, mapping, NO_MAPPING);
					}
				}
			}
			else {
				logMappingResult(sourceDrug, mapping, NO_MAPPING);
			}
		}

		if (report != null) {
			report.add("Source drugs mapped to multiple CDM Clinical Drug Comps: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) source.getSourceDrugs().size()));
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugForms() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_FORM_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Forms ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
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
					if (sourceDrug.getFormulations().size() > 0) {
						preferencesUsed = "";
						List<CDMIngredient> cdmDrugIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

						// Find CDM Clinical Drug Forms with corresponding ingredients
						List<CDMIngredient> foundIngredients = new ArrayList<CDMIngredient>();
						List<CDMDrug> cdmDrugsWithIngredients = null;
						for (CDMIngredient cdmIngredient : cdmDrugIngredients) {
							List<CDMDrug> cdmDrugsWithIngredient = cdm.getCDMDrugFormsContainingIngredient(sourceDrug.getIngredients().size(), cdmIngredient);
							if (cdmDrugsWithIngredient != null) {
								if (cdmDrugsWithIngredients == null) {
									cdmDrugsWithIngredients = new ArrayList<CDMDrug>();
									cdmDrugsWithIngredients.addAll(cdmDrugsWithIngredient);
								}
								else {
									Set<CDMDrug> cdmDrugsMissingIngredient = new HashSet<CDMDrug>();
									for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
										if (!cdmDrugsWithIngredient.contains(cdmDrug)) {
											cdmDrugsMissingIngredient.add(cdmDrug);
										}
										else {
											List<CDMIngredient> cdmDrugIngredientsList = new ArrayList<CDMIngredient>();
											cdmDrugIngredientsList.addAll(cdmDrug.getIngredients());
											for (CDMIngredient foundIngredient : foundIngredients) {
												cdmDrugIngredientsList.remove(foundIngredient);
											}
											if (!cdmDrugIngredientsList.contains(cdmIngredient)) {
												cdmDrugsMissingIngredient.add(cdmDrug);
											}
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
							foundIngredients.add(cdmIngredient);
						}
						
						// Find CDM Clinical Drugs with corresponding formulation
						if (cdmDrugsWithIngredients.size() > 0) {
							logMappingResult(sourceDrug, mapping, cdmDrugsWithIngredients, DRUGS_WITH_MATCHING_INGREDIENTS);
							
							Set<CDMConcept> availableForms = new HashSet<CDMConcept>();
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								List<String> cdmDrugFormIds = cdmDrug.getFormConceptIds();
								if (cdmDrugFormIds != null) {
									for (String cdmDrugFormId : cdmDrugFormIds) {
										availableForms.add(cdm.getCDMFormConcept(cdmDrugFormId));
									}
								}
	 						}
							logMappingResult(sourceDrug, mapping, availableForms, AVAILABLE_FORMS);

							// Remove all drugs with the wrong form
							Set<CDMConcept> rejectedByForm = new HashSet<CDMConcept>();
							List<String> sourceDrugForms = sourceDrug.getFormulations();
							List<String> matchingCDMForms = new ArrayList<String>();
							for (String sourceDrugForm : sourceDrugForms) {
								List<String> matchingCDMFormList = formConversionsMap.getMatchingForms(sourceDrugForm);
								if (matchingCDMFormList != null) {
									for (String matchingCDMForm : matchingCDMFormList) {
										if (!matchingCDMForms.contains(matchingCDMForm)) {
											matchingCDMForms.add(matchingCDMForm);
										}
									}
								}
							}		
							if (matchingCDMForms.size() > 0) {
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									List<String> cdmDrugForms = cdmDrug.getFormConceptNames();
									List<String> formOverlap = new ArrayList<String>();
									if (cdmDrugForms != null) {
										for (String cdmDrugForm : cdmDrugForms) {
											if (matchingCDMForms.contains(cdmDrugForm)) {
												formOverlap.add(cdmDrugForm);
											}
										}
										if (formOverlap.size() == 0) {
											rejectedByForm.add(cdmDrug);
										}
									}
								}
							}
							else {
								rejectedByForm.addAll(cdmDrugsWithIngredients);
							}
							cdmDrugsWithIngredients.removeAll(rejectedByForm);
							logMappingResult(sourceDrug, mapping, rejectedByForm, REJECTED_BY_FORM);

							if (cdmDrugsWithIngredients.size() > 1) {
								for (String matchingCDMForm : matchingCDMForms) {
									List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
									for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
										if (cdmDrug.getFormConceptNames().contains(matchingCDMForm)) {
											matchingCDMDrugs.add(cdmDrug);
										}
									}
									if (matchingCDMDrugs.size() > 0) {
										List<CDMDrug> rejectedByFormPriority = new ArrayList<CDMDrug>();
										for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
											if (!matchingCDMDrugs.contains(cdmDrug)) {
												rejectedByFormPriority.add(cdmDrug);
											}
										}
										cdmDrugsWithIngredients = matchingCDMDrugs;
										logMappingResult(sourceDrug, mapping, rejectedByFormPriority, REJECTED_BY_FORM_PRIORITY);
										break;
									}
								}
							}

							if (cdmDrugsWithIngredients.size() > 1) {
								preferencesUsed = "";
								cdmDrugsWithIngredients = selectConcept(sourceDrug, cdmDrugsWithIngredients, mapping);
							}

							if (cdmDrugsWithIngredients.size() > 0) {
								List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
								Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
								
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap());
									if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
										matchingCDMDrugs.add(cdmDrug);
										matchingIngredientsMap.put(cdmDrug, matchingIngredients);
									}
								}
								if (matchingCDMDrugs.size() == 1) {
									automaticMapping = matchingCDMDrugs.get(0);
									sourceDrug.setMatchString(preferencesUsed);
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
						else {
							logMappingResult(sourceDrug, mapping, NO_MAPPING);
						}
					}
					else {
						logMappingResult(sourceDrug, mapping, NO_SOURCE_FORMULATION);
						logMappingResult(sourceDrug, mapping, NO_MAPPING);
					}
				}
			}
			else {
				logMappingResult(sourceDrug, mapping, NO_MAPPING);
			}
		}

		if (report != null) {
			report.add("Source drugs mapped to multiple CDM Clinical Drug Forms: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) source.getSourceDrugs().size()));
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugCompsIngredients() {
		boolean ok = true;
		int mapping = INGREDIENT_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drug ingredients to Clinical Drug Comps and Ingredients ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
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
						preferencesUsed = "";
						usedStrengthDeviationPercentage.add(null);
						SourceDrugComponent sourceDrugComponent = sourceDrugComponents.get(componentNr);
						SourceIngredient sourceDrugIngredient = sourceDrugComponent.getIngredient();
						String cdmIngredientConceptId = sourceDrugIngredient.getMatchingIngredient() == null ? null : sourceDrugIngredient.getMatchingIngredient().getConceptId();

						if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_MATCH_INGREDIENTS_TO_COMP).equals("Ingredient or Comp")) {
							// Try matching to CDM Clinical Drug Comp
	 						if (cdmIngredientConceptId != null) {
								CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(cdmIngredientConceptId);
								List<CDMDrug> cdmDrugCompsWithIngredient = cdm.getCDMDrugCompsContainingIngredient(cdmIngredient);
								logMappingResult(sourceDrug, mapping, cdmDrugCompsWithIngredient, DRUG_COMPS_WITH_MATCHING_INGREDIENT, componentNr);

								// Find CDM Clinical Drug Comps with corresponding ingredient strengths
								if ((cdmDrugCompsWithIngredient != null) && (cdmDrugCompsWithIngredient.size() > 0)) {
									Set<CDMConcept> rejectedDrugComps = new HashSet<CDMConcept>();
									Map<Double, List<CDMDrug>> strengthMatchingCDMDrugs = new HashMap<Double, List<CDMDrug>>();
									for (CDMDrug cdmDrugCompWithIngredient : cdmDrugCompsWithIngredient) {
										Double deviationPercentage = matchingStrength(sourceDrugComponent, cdmDrugCompWithIngredient.getIngredientStrengths().get(0));
										if (deviationPercentage != null) {
											List<CDMDrug> deviationPercentageList = strengthMatchingCDMDrugs.get(deviationPercentage);
											if (deviationPercentageList == null) {
												deviationPercentageList = new ArrayList<CDMDrug>();
												strengthMatchingCDMDrugs.put(deviationPercentage, deviationPercentageList);
											}
											deviationPercentageList.add(cdmDrugCompWithIngredient);
											usedStrengthDeviationPercentage.set(componentNr, Math.min(usedStrengthDeviationPercentage.get(componentNr) == null ? DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION) : usedStrengthDeviationPercentage.get(componentNr), deviationPercentage));
										}
										else {
											rejectedDrugComps.add(cdmDrugCompWithIngredient);
										}
									}

									List<CDMDrug> matchingCDMDrugComps = null;
									if (usedStrengthDeviationPercentage.get(componentNr) != null) {
										matchingCDMDrugComps = strengthMatchingCDMDrugs.get(usedStrengthDeviationPercentage.get(componentNr));
										for (double deviationPercentage : strengthMatchingCDMDrugs.keySet()) {
											if (deviationPercentage != (double) usedStrengthDeviationPercentage.get(componentNr)) {
												rejectedDrugComps.addAll(strengthMatchingCDMDrugs.get(deviationPercentage));
											}
										}
									}
									
									// Save the rejected drugs
									logMappingResult(sourceDrug, mapping, REJECTED_BY_MAXIMUM_STRENGTH_DEVIATION, rejectedDrugComps, componentNr);

									if (matchingCDMDrugComps != null) {
										if (matchingCDMDrugComps.size() > 1) {
											matchingCDMDrugComps = selectConcept(sourceDrug, matchingCDMDrugComps, mapping, componentNr);
										}
										if (matchingCDMDrugComps.size() == 1) {
											automaticMappings.set(componentNr, matchingCDMDrugComps.get(0));
											usedStrengthDeviationPercentageMap.put("Ingredient " + sourceDrug.getCode() + "," + sourceDrugIngredient.getIngredientCode(), usedStrengthDeviationPercentage.get(componentNr));
											mappedSourceDrugs.add(sourceDrug);
											sourceDrugComponent.setMatchString("Strength margin: " + usedStrengthDeviationPercentage.get(componentNr) + " " + preferencesUsed);
										}
										if (matchingCDMDrugComps.size() > 1) {
											logMappingResult(sourceDrug, mapping, matchingCDMDrugComps, NO_UNIQUE_MAPPING, componentNr);
											notUniqueMapping.get(mapping).add(sourceDrug);
										}
									}
								}
							}
						}
						
						// Try mapping to CDM Ingredient
						if (automaticMappings.get(componentNr) == null) {
							if (sourceDrugIngredient.getMatchingIngredient() != null) {
								CDMIngredient cdmIngredient = sourceDrugIngredient.getMatchingIngredient();
								automaticMappings.set(componentNr, cdmIngredient);
								mappedSourceDrugs.add(sourceDrug);
							}
						}
					}
				}

				for (int componentNr = 0; componentNr < sourceDrugComponents.size(); componentNr++) {
					if (automaticMappings.get(componentNr) != null) {
						logMappingResult(sourceDrug, mapping, MAPPED, automaticMappings.get(componentNr), componentNr);
					}
					else {
						logMappingResult(sourceDrug, mapping, NO_MAPPING, componentNr);
					}
				}
			}
		}

		if (report != null) {
			report.add("Source drugs mapped to multiple CDM Clinical Drug Comp or CDM Ingredient combinations: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) source.getSourceDrugs().size()));
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private void saveMapping() {
		isSavingDrugMapping = true;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Saving Mappings ...");
		
		// Sort the ingredients on use count descending.
		List<SourceIngredient> sourceIngredients = new ArrayList<SourceIngredient>();
		sourceIngredients.addAll(Source.getAllIngredients());
		
		Collections.sort(sourceIngredients, new Comparator<SourceIngredient>() {
			@Override
			public int compare(SourceIngredient ingredient1, SourceIngredient ingredient2) {
				int countCompare = Long.compare(ingredient1.getCount() == null ? 0L : ingredient1.getCount(), ingredient2.getCount() == null ? 0L : ingredient2.getCount()); 
				int compareResult = (countCompare == 0 ? (ingredient1.getIngredientCode() == null ? "" : ingredient1.getIngredientCode()).compareTo(ingredient2.getIngredientCode() == null ? "" : ingredient2.getIngredientCode()) : -countCompare);
				//System.out.println("Compare: " + sourceDrug1.getCode() + "," + sourceDrug1.getCount() + " <-> " + sourceDrug2.getCode() + "," + sourceDrug2.getCount() + " => " + Integer.toString(compareResult));
				return compareResult;
			}
		});

		// Sort source drugs on use count descending
		Collections.sort(source.getSourceDrugs(), new Comparator<SourceDrug>() {
			@Override
			public int compare(SourceDrug sourceDrug1, SourceDrug sourceDrug2) {
				int countCompare = Long.compare(sourceDrug1.getCount() == null ? 0L : sourceDrug1.getCount(), sourceDrug2.getCount() == null ? 0L : sourceDrug2.getCount()); 
				int compareResult = (countCompare == 0 ? (sourceDrug1.getCode() == null ? "" : sourceDrug1.getCode()).compareTo(sourceDrug2.getCode() == null ? "" : sourceDrug2.getCode()) : -countCompare);
				//System.out.println("Compare: " + sourceDrug1.getCode() + "," + sourceDrug1.getCount() + " <-> " + sourceDrug2.getCode() + "," + sourceDrug2.getCount() + " => " + Integer.toString(compareResult));
				return compareResult;
			}
		});
		
		source.saveMissingATCToFile();
		saveIngredientMapping(sourceIngredients);
		saveDrugMapping();
		isSavingDrugMapping = false;
		
		if (DrugMapping.settings.getStringSetting(MainFrame.SAVE_DRUGMAPPING_RESULTS).equals("Yes")) {
			saveDrugMappingMappingLog(source, sourceDrugMappingResults, usedStrengthDeviationPercentageMap, cdm);
		}
			
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void saveIngredientMapping(List<SourceIngredient> sourceIngredients) {
		
		System.out.println(DrugMapping.getCurrentTime() + "       Saving Ingredient Mapping Mapping Log ...");
		
		// Save ingredient mapping
		String header = SourceIngredient.getMatchHeader();
		header += "," + "SourceCount";
		header += "," + CDMIngredient.getHeader();
		PrintWriter ingredientMappingFile = DrugMappingFileUtilities.openOutputFile("IngredientMapping Mapping Log.csv", header);
		
		if (ingredientMappingFile != null) {
			
			for (SourceIngredient sourceIngredient : sourceIngredients) {
				if (sourceIngredient.getMatchingIngredient() != null) {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + sourceIngredient.getCount() + "," + cdm.getCDMIngredients().get(sourceIngredient.getMatchingIngredient().getConceptId()));
				}
				else {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + sourceIngredient.getCount() + "," + CDMIngredient.emptyRecord());
				}
			}
			
			ingredientMappingFile.close();
		}

		System.out.println(DrugMapping.getCurrentTime() + "       Done");
		
		System.out.println(DrugMapping.getCurrentTime() + "       Saving Ingredient Mapping Review ...");
		
		header =        "SourceIngredientCode";
		header += "," + "SourceIngredientName";
		header += "," + "SourceIngredientEnglishName";
		header += "," + "SourceRecordCount";
		header += "," + "concept_id";
		header += "," + "concept_name";
		header += "," + "concept_vocabulary_id";
		header += "," + "concept_class_id";
		header += "," + "MatchLog";
		PrintWriter ingredientMappingReviewFile = DrugMappingFileUtilities.openOutputFile("IngredientMapping Review.csv", header);
		
		if (ingredientMappingReviewFile != null) {
			
			for (SourceIngredient sourceIngredient : sourceIngredients) {
				CDMIngredient cdmIngredient = sourceIngredient.getMatchingIngredient();
				
				String record = DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
				record += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
				record += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientNameEnglish());
				record += "," + sourceIngredient.getCount();
				if (cdmIngredient != null) {
					record += "," + cdmIngredient.getConceptId();
					record += "," + DrugMappingStringUtilities.escapeFieldValue(cdmIngredient.getConceptName());
					record += "," + cdmIngredient.getVocabularyId();
					record += "," + cdmIngredient.getConceptClassId();
					record += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getMatchString());
				}
				else {
					record += ",";
					record += ",";
					record += ",";
					record += ",";
					record += ",";
				}
				ingredientMappingReviewFile.println(record);
			}
			
			ingredientMappingReviewFile.close();
		}

		System.out.println(DrugMapping.getCurrentTime() + "       Done");
	}
	
	
	private void saveDrugMapping() {
		
		System.out.println(DrugMapping.getCurrentTime() + "       Saving Drug Mapping ...");
		
		String header = "MappingStatus";
		header += "," + SourceDrug.getHeader();
		header += "," + SourceIngredient.getHeader();
		header += "," + "SourceIngredientAmount";
		header += "," + "SourceIngredentUnit";
		header += "," + "StrengthMarginPercentage";
		header += "," + "MappingType";
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
		
		header = "source_code";
		header += "," + "source_concept_id";
		header += "," + "source_vocabulary_id";
		header += "," + "source_code_description";
		header += "," + "target_concept_id";
		header += "," + "target_vocabulary_id";
		header += "," + "valid_start_date";
		header += "," + "valid_end_date";
		header += "," + "invalid_reason";
		
		PrintWriter sourceToConceptMapFile = DrugMappingFileUtilities.openOutputFile("SourceToConceptMap.csv", header);
		
		header = "SourceCode";
		header += "," + "SourceName";
		header += "," + "SourceCount";
		header += "," + "SourceDoseForm";
		header += "," + "SourceIngredientCode";
		header += "," + "SourceIngredientName";
		header += "," + "SourceIngredientAmount";
		header += "," + "SourceIngredentUnit";
		header += "," + "concept_id";
		header += "," + "concept_name";
		header += "," + "concept_class";
		header += "," + "Mapping_log";
		
		PrintWriter drugMappingReviewFile = DrugMappingFileUtilities.openOutputFile("DrugMapping Review.csv", header);

		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			String mappingStatus = manualDrugMappings.containsKey(sourceDrug) ? "ManualMapping" : (mappedSourceDrugs.contains(sourceDrug) ? "Mapped" : "Unmapped");
			Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappings = sourceDrugMappingResults.get(sourceDrug);
			
			// Get the mapping type
			int mappingType = -1;
			if (sourceDrugMappings != null) {
				int mapping = 0;
				while (mappingTypeDescriptions.get(mapping) != null) {
					if ((mapping == INGREDIENT_MAPPING) && mappingStatus.equals("Mapped")) {
						mappingType = INGREDIENT_MAPPING;
						break;
					}
					else {
						if ((sourceDrugMappings.get(mapping) != null) && (sourceDrugMappings.get(mapping).get(0).get(MAPPED) != null)) {
							mappingType = mapping;
							break;
						}
					}
					mapping++;
				}
			}
			
			String drugMappingRecord = mappingStatus;
			drugMappingRecord += "," + sourceDrug; 
			
			String sourceToConceptRecord = "";
			
			String drugMappingReviewRecord = DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getCode());
			drugMappingReviewRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getName());
			drugMappingReviewRecord += "," + sourceDrug.getCount();
			drugMappingReviewRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getFormulationsString());
			
			if (mappingType != -1) {
				String mappingLog = sourceDrug.getMatchString();
				List< Map<Integer, List<CDMConcept>>> mappingResultList = sourceDrugMappings.get(mappingType);
				
				if (mappingType == INGREDIENT_MAPPING) {
					List<SourceDrugComponent> sourceDrugComponents = new ArrayList<SourceDrugComponent>();
					sourceDrugComponents.addAll(sourceDrug.getComponents());
					List<SourceDrugComponent> sortedSourceDrugComponents = new ArrayList<SourceDrugComponent>();
					sortedSourceDrugComponents.addAll(sourceDrug.getComponents());
					Collections.sort(sortedSourceDrugComponents, new Comparator<SourceDrugComponent>() {

						@Override
						public int compare(SourceDrugComponent sourceDrugComponent1, SourceDrugComponent sourceDrugComponent2) {
							int compare = sourceDrugComponent1.getIngredient().getIngredientCode().compareTo(sourceDrugComponent2.getIngredient().getIngredientCode());
							if (compare == 0) {
								Double amount1 = sourceDrugComponent1.getDosage();
								Double amount2 = sourceDrugComponent2.getDosage();
								compare = (amount1 == null ? (amount2 == null ? 0 : -1) : (amount2 == null ? 1 : amount1.compareTo(amount2)));
							}
							return compare;
						}
						
					});
					
					for (int ingredientNr = 0; ingredientNr < mappingResultList.size(); ingredientNr++) {
						sourceToConceptRecord = "";
						
						SourceDrugComponent sourceDrugComponent = sortedSourceDrugComponents.get(ingredientNr);
						SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
						String key = "Ingredient " + sourceDrug.getCode() + "," + sourceDrug.getIngredients().get(ingredientNr).getIngredientCode();
						String strengthDeviationPercentage = "";
						if (usedStrengthDeviationPercentageMap.get(key) != null) {
							strengthDeviationPercentage = usedStrengthDeviationPercentageMap.get(key).toString();
						}
						
						CDMConcept target = mappingResultList.get(sourceDrugComponents.indexOf(sourceDrugComponent)).get(MAPPED) == null ? null : mappingResultList.get(sourceDrugComponents.indexOf(sourceDrugComponent)).get(MAPPED).get(0);
						mappingLog = sourceDrugComponent.getMatchString(); 
						
						String drugMappingIngredientRecord = drugMappingRecord;
						drugMappingIngredientRecord += "," + sourceIngredient;
						drugMappingIngredientRecord += "," + standardizedAmount(sourceDrugComponent);
						drugMappingIngredientRecord += "," + (standardizedAmount(sourceDrugComponent).equals("") ? "" : DrugMappingStringUtilities.escapeFieldValue(sourceDrugComponent.getDosageUnit()));
						drugMappingIngredientRecord += "," + strengthDeviationPercentage;
						drugMappingIngredientRecord += "," + mappingTypeDescriptions.get(mappingType);
						drugMappingIngredientRecord += "," + (target == null ? CDMConcept.emptyRecord() : target.toString());
						
						drugMappingFile.println(drugMappingIngredientRecord);
						
						if (target != null) {
							sourceToConceptRecord +=       "Drug " + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getCode());
							sourceToConceptRecord += "," + "0";
							sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
							sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getName());
							sourceToConceptRecord += "," + target.getConceptId();
							sourceToConceptRecord += "," + target.getVocabularyId();
							sourceToConceptRecord += "," + DrugMapping.getCurrentDate();
							sourceToConceptRecord += ",";
							sourceToConceptRecord += ",";
							
							sourceToConceptMapFile.println(sourceToConceptRecord);
						}
						else {
							sourceToConceptRecord +=       "Drug " + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getCode());
							sourceToConceptRecord += "," + "0";
							sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
							sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getName());
							sourceToConceptRecord += "," + "0";
							sourceToConceptRecord += ",";
							sourceToConceptRecord += ",";
							sourceToConceptRecord += ",";
							sourceToConceptRecord += ",";
							
							sourceToConceptMapFile.println(sourceToConceptRecord);
						}
						

						String drugMappingReviewIngredientRecord =       drugMappingReviewRecord;
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
						drugMappingReviewIngredientRecord += "," + standardizedAmount(sourceDrugComponent);
						drugMappingReviewIngredientRecord += "," + (standardizedAmount(sourceDrugComponent).equals("") ? "" : DrugMappingStringUtilities.escapeFieldValue(sourceDrugComponent.getDosageUnit()));
						if (target != null) {
							drugMappingReviewIngredientRecord += "," + target.getConceptId();
							drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(target.getConceptName());
							drugMappingReviewIngredientRecord += "," + target.getConceptClassId();
							drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(mappingLog);
						}
						else {
							drugMappingReviewIngredientRecord += ",";
							drugMappingReviewIngredientRecord += ",";
							drugMappingReviewIngredientRecord += ",";
							drugMappingReviewIngredientRecord += ",";
						}
						
						drugMappingReviewFile.println(drugMappingReviewIngredientRecord);
					}
				}
				else {
					String key = "Drug " + sourceDrug.getCode();
					String strengthDeviationPercentage = "";
					if (usedStrengthDeviationPercentageMap.get(key) != null) {
						strengthDeviationPercentage = usedStrengthDeviationPercentageMap.get(key).toString();
					}
					CDMConcept target = mappingResultList.get(0).get(MAPPED) == null ? null : mappingResultList.get(0).get(MAPPED).get(0);
					
					drugMappingRecord += "," + SourceIngredient.getStarredRecord();
					drugMappingRecord += "," + "*";
					drugMappingRecord += "," + "*";
					drugMappingRecord += "," + strengthDeviationPercentage;
					drugMappingRecord += "," + mappingTypeDescriptions.get(mappingType);
					drugMappingRecord += "," + (target == null ? CDMConcept.emptyRecord() : target.toString());
					
					drugMappingFile.println(drugMappingRecord);
					
					
					if (target != null) {
						sourceToConceptRecord +=       "Drug " + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getCode());
						sourceToConceptRecord += "," + "0";
						sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
						sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getName());
						sourceToConceptRecord += "," + target.getConceptId();
						sourceToConceptRecord += "," + target.getVocabularyId();
						sourceToConceptRecord += "," + DrugMapping.getCurrentDate();
						sourceToConceptRecord += ",";
						sourceToConceptRecord += ",";
						
						sourceToConceptMapFile.println(sourceToConceptRecord);
					}
					else {
						sourceToConceptRecord +=       "Drug " + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getCode());
						sourceToConceptRecord += "," + "0";
						sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
						sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getName());
						sourceToConceptRecord += "," + "0";
						sourceToConceptRecord += ",";
						sourceToConceptRecord += ",";
						sourceToConceptRecord += ",";
						sourceToConceptRecord += ",";
						
						sourceToConceptMapFile.println(sourceToConceptRecord);
					}
					

					drugMappingReviewRecord += "," + "*";
					drugMappingReviewRecord += "," + "*";
					drugMappingReviewRecord += "," + "*";
					drugMappingReviewRecord += "," + "*";
					if (target != null) {
						drugMappingReviewRecord += "," + target.getConceptId();
						drugMappingReviewRecord += "," + DrugMappingStringUtilities.escapeFieldValue(target.getConceptName());
						drugMappingReviewRecord += "," + target.getConceptClassId();
						drugMappingReviewRecord += "," + DrugMappingStringUtilities.escapeFieldValue(mappingLog);
					}
					else {
						drugMappingReviewRecord += ",";
						drugMappingReviewRecord += ",";
						drugMappingReviewRecord += ",";
						drugMappingReviewRecord += ",";
					}
					
					drugMappingReviewFile.println(drugMappingReviewRecord);
				}
			}
			else {
				drugMappingRecord += "," + SourceIngredient.getStarredRecord();
				drugMappingRecord += ",";
				drugMappingRecord += ",";
				drugMappingRecord += ",";
				drugMappingRecord += ",";
				drugMappingRecord += "," + CDMDrug.emptyRecord();
				
				drugMappingFile.println(drugMappingRecord);

				
				sourceToConceptRecord +=       "Drug " + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getCode());
				sourceToConceptRecord += "," + "0";
				sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
				sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getName());
				sourceToConceptRecord += "," + "0";
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				
				sourceToConceptMapFile.println(sourceToConceptRecord);

				
				List<SourceDrugComponent> sortedSourceDrugComponents = new ArrayList<SourceDrugComponent>();
				sortedSourceDrugComponents.addAll(sourceDrug.getComponents());
				Collections.sort(sortedSourceDrugComponents, new Comparator<SourceDrugComponent>() {

					@Override
					public int compare(SourceDrugComponent sourceDrugComponent1, SourceDrugComponent sourceDrugComponent2) {
						int compare = sourceDrugComponent1.getIngredient().getIngredientCode().compareTo(sourceDrugComponent2.getIngredient().getIngredientCode());
						if (compare == 0) {
							Double amount1 = sourceDrugComponent1.getDosage();
							Double amount2 = sourceDrugComponent2.getDosage();
							compare = (amount1 == null ? (amount2 == null ? 0 : -1) : (amount2 == null ? 1 : amount1.compareTo(amount2)));
						}
						return compare;
					}
					
				});
				
				List< Map<Integer, List<CDMConcept>>> mappingResultList = sourceDrugMappings.get(INGREDIENT_MAPPING);
				if (mappingResultList != null) {
					for (int ingredientNr = 0; ingredientNr < mappingResultList.size(); ingredientNr++) {
						SourceDrugComponent sourceDrugComponent = sortedSourceDrugComponents.get(ingredientNr);
						SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
						
						String drugMappingReviewIngredientRecord = drugMappingReviewRecord;
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
						drugMappingReviewIngredientRecord += "," + standardizedAmount(sourceDrugComponent);
						drugMappingReviewIngredientRecord += "," + (standardizedAmount(sourceDrugComponent).equals("") ? "" : DrugMappingStringUtilities.escapeFieldValue(sourceDrugComponent.getDosageUnit()));
						drugMappingReviewIngredientRecord += ",";
						drugMappingReviewIngredientRecord += ",";
						drugMappingReviewIngredientRecord += ",";
						drugMappingReviewIngredientRecord += ",";
						
						drugMappingReviewFile.println(drugMappingReviewIngredientRecord);
					}
				}
				else {
					String drugMappingReviewIngredientRecord = drugMappingReviewRecord;
					drugMappingReviewIngredientRecord += "," + "*";
					drugMappingReviewIngredientRecord += "," + "*";
					drugMappingReviewIngredientRecord += "," + "*";
					drugMappingReviewIngredientRecord += "," + "*";
					drugMappingReviewIngredientRecord += ",";
					drugMappingReviewIngredientRecord += ",";
					drugMappingReviewIngredientRecord += ",";
					drugMappingReviewIngredientRecord += ",";
					
					drugMappingReviewFile.println(drugMappingReviewIngredientRecord);
				}
			}
		}
		
		for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
			String sourceToConceptRecord = "";
			CDMIngredient target = ingredientMap.get(sourceIngredient);

			if (target != null) {
				sourceToConceptRecord +=       "Ingredient " + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
				sourceToConceptRecord += "," + "0";
				sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
				sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
				sourceToConceptRecord += "," + target.getConceptId();
				sourceToConceptRecord += "," + target.getVocabularyId();
				sourceToConceptRecord += "," + DrugMapping.getCurrentDate();
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				
				sourceToConceptMapFile.println(sourceToConceptRecord);
			}
			else {
				sourceToConceptRecord +=       "Ingredient " + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
				sourceToConceptRecord += "," + "0";
				sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
				sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
				sourceToConceptRecord += "," + "0";
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				sourceToConceptRecord += ",";
				
				sourceToConceptMapFile.println(sourceToConceptRecord);
			}
		}

		DrugMappingFileUtilities.closeOutputFile(drugMappingFile);
		DrugMappingFileUtilities.closeOutputFile(sourceToConceptMapFile);
		DrugMappingFileUtilities.closeOutputFile(drugMappingReviewFile);
		
		System.out.println(DrugMapping.getCurrentTime() + "       Done");
	}
	
	
	public static void saveDrugMappingMappingLog(Source source, Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> sourceDrugMappingLog, Map<String, Double> strengthDeviationPercentageMap, CDM cdmData) {
		isSavingDrugMappingLog = true;
		
		System.out.println(DrugMapping.getCurrentTime() + "       Saving Drug Mapping Mapping Log ...");
		
		int maxResultConcepts = 0;
		for (SourceDrug sourceDrug : sourceDrugMappingLog.keySet()) {
			Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugLog = sourceDrugMappingLog.get(sourceDrug); 
			for (Integer mappingType : sourceDrugLog.keySet()) {
				List<Map<Integer, List<CDMConcept>>> mappingTypeLog = sourceDrugLog.get(mappingType);
				for (Map<Integer, List<CDMConcept>> componentLog : mappingTypeLog) {
					for (Integer mappingResult : componentLog.keySet()) {
						List<CDMConcept> result = componentLog.get(mappingResult);
						if (result != null) {
							maxResultConcepts = Math.max(maxResultConcepts, result.size());
						}
					}
				}
			}
		}

		String[] columns = getHeader(maxResultConcepts);
		String header = "";
		for (String column : columns) {
			header += (header.equals("") ? "" : ",") + column;
		}
		PrintWriter drugMappingResultsFile = DrugMappingFileUtilities.openOutputFile("DrugMapping Mapping Log.csv", header);
		
		if (drugMappingResultsFile != null) {

			// Sort source drugs on use count descending
			Collections.sort(source.getSourceDrugs(), new Comparator<SourceDrug>() {
				@Override
				public int compare(SourceDrug sourceDrug1, SourceDrug sourceDrug2) {
					int countCompare = Long.compare(sourceDrug1.getCount() == null ? 0L : sourceDrug1.getCount(), sourceDrug2.getCount() == null ? 0L : sourceDrug2.getCount()); 
					int compareResult = (countCompare == 0 ? (sourceDrug1.getCode() == null ? "" : sourceDrug1.getCode()).compareTo(sourceDrug2.getCode() == null ? "" : sourceDrug2.getCode()) : -countCompare);
					//System.out.println("Compare: " + sourceDrug1.getCode() + "," + sourceDrug1.getCount() + " <-> " + sourceDrug2.getCode() + "," + sourceDrug2.getCount() + " => " + Integer.toString(compareResult));
					return compareResult;
				}
			});
			
			for (SourceDrug sourceDrug : source.getSourceDrugs()) {
				String mappingStatus = null;
				Map<Integer, List<Map<Integer, List<CDMConcept>>>> drugMappingLog = sourceDrugMappingLog.get(sourceDrug);
				if (drugMappingLog != null) { 
					for (Integer mappingType : drugMappingLog.keySet()) {
						List<Map<Integer, List<CDMConcept>>> sourceDrugMappingTypeLog = drugMappingLog.get(mappingType);
						if (sourceDrugMappingTypeLog != null) {
							for (Map<Integer, List<CDMConcept>> sourceDrugComponentMappingLog : sourceDrugMappingTypeLog) {
								if (sourceDrugComponentMappingLog.keySet().contains(GenericMapping.getMappingResultValue("Overruled mapping"))) {
									mappingStatus = "ManualMapping";
									break;
								}
								else if (sourceDrugComponentMappingLog.keySet().contains(GenericMapping.getMappingResultValue("Mapped"))) {
									mappingStatus = "Mapped";
									break;
								}
							}
						}
						if (mappingStatus != null) {
							break;
						}
					}
				}
				if (mappingStatus == null) {
					mappingStatus = "Unmapped";
				}
				
				List<List<String>> sourceDrugResultRecords = getSourceDrugMappingResults(sourceDrug, sourceDrugMappingLog, strengthDeviationPercentageMap, cdmData);
				for (List<String> sourceDrugResultRecord : sourceDrugResultRecords) {
					String resultRecord = mappingStatus; //manualDrugMappings.containsKey(sourceDrug) ? "ManualMapping" : (mappedSourceDrugs.contains(sourceDrug) ? "Mapped" : "Unmapped");
					for (int column = 0; column < columns.length; column++) {
						resultRecord += "," + (column < sourceDrugResultRecord.size() ? DrugMappingStringUtilities.escapeFieldValue(sourceDrugResultRecord.get(column)) : "");
					}
					drugMappingResultsFile.println(resultRecord);
				}
			}

			DrugMappingFileUtilities.closeOutputFile(drugMappingResultsFile);

			System.out.println(DrugMapping.getCurrentTime() + "       Done");
		}
		
		isSavingDrugMappingLog = false;
	}
	
	
	public static List<List<String>> getSourceDrugMappingResults(SourceDrug sourceDrug, Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> sourceDrugMappingLog, Map<String, Double> strengthDeviationPercentageMap, CDM cdmData) {
		List<List<String>> resultRecords = new ArrayList<List<String>>();
		
		Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappings = sourceDrugMappingLog.get(sourceDrug);
		
		if (sourceDrugMappings == null) {
			System.out.println("ERROR: " + sourceDrug);
		}

		Integer mappingType = 0;
		while (mappingTypeDescriptions.containsKey(mappingType)) {
			List< Map<Integer, List<CDMConcept>>> mappingResultList = sourceDrugMappings.get(mappingType);
			if ((mappingResultList != null) && (mappingResultList.size() > 0)) {
				List<SourceDrugComponent> sourceDrugComponents = new ArrayList<SourceDrugComponent>();
				sourceDrugComponents.addAll(sourceDrug.getComponents());
				List<SourceDrugComponent> sortedSourceDrugComponents = new ArrayList<SourceDrugComponent>();
				sortedSourceDrugComponents.addAll(sourceDrug.getComponents());
				Collections.sort(sortedSourceDrugComponents, new Comparator<SourceDrugComponent>() {

					@Override
					public int compare(SourceDrugComponent sourceDrugComponent1, SourceDrugComponent sourceDrugComponent2) {
						int compare = sourceDrugComponent1.getIngredient().getIngredientCode().compareTo(sourceDrugComponent2.getIngredient().getIngredientCode());
						if (compare == 0) {
							Double amount1 = sourceDrugComponent1.getDosage();
							Double amount2 = sourceDrugComponent2.getDosage();
							compare = (amount1 == null ? (amount2 == null ? 0 : -1) : (amount2 == null ? 1 : amount1.compareTo(amount2)));
						}
						return compare;
					}
					
				});

				for (int ingredientNr = 0; ingredientNr < mappingResultList.size(); ingredientNr++) {
					Map<Integer, List<CDMConcept>> mappingResult = mappingResultList.get(ingredientNr); 
					// Mapping on source drug
					String[] sourceIngredientResults = new String[] { "*","*", "*", "*", "*", "*" };
					if (mappingType == INGREDIENT_MAPPING) {   // Mapping on source drug ingredients
						SourceDrugComponent sourceDrugComponent = sortedSourceDrugComponents.size() == 0 ? null : sortedSourceDrugComponents.get(ingredientNr);
						mappingResult = mappingResultList.get(sourceDrugComponent == null ? 0 : sourceDrugComponents.indexOf(sourceDrugComponent));
						
						sourceIngredientResults[0] = (sourceDrugComponent.getIngredient().getIngredientCode() == null ? "" : sourceDrugComponent.getIngredient().getIngredientCode());
						sourceIngredientResults[1] = (sourceDrugComponent.getIngredient().getIngredientName() == null ? "" : sourceDrugComponent.getIngredient().getIngredientName());
						sourceIngredientResults[2] = (sourceDrugComponent.getIngredient().getIngredientNameEnglish() == null ? "" : sourceDrugComponent.getIngredient().getIngredientNameEnglish());
						sourceIngredientResults[3] = (sourceDrugComponent.getIngredient().getCASNumber() == null ? "" : sourceDrugComponent.getIngredient().getCASNumber());
						sourceIngredientResults[4] = standardizedAmount(sourceDrugComponent);
						sourceIngredientResults[5] = sourceDrugComponent.getDosageUnit();
					}
					if (mappingResult != null) {
						// Write the result records
						int mappingResultType = 0;
						while (mappingResultDescriptions.containsKey(mappingResultType)) {
							String strengthDeviationPercentage = "";
							if (mappingResultType == MAPPED) {
								String key = mappingType == INGREDIENT_MAPPING ? ("Ingredient " + sourceDrug.getCode() + "," + sourceDrug.getIngredients().get(ingredientNr).getIngredientCode()) : ("Drug " + sourceDrug.getCode());
								if (strengthDeviationPercentageMap.get(key) != null) {
									strengthDeviationPercentage = strengthDeviationPercentageMap.get(key).toString();
								}
							}
							List<String> resultRecord = new ArrayList<String>();
							
							resultRecord.add(sourceDrug.getCode() == null ? "" : sourceDrug.getCode());
							resultRecord.add(sourceDrug.getName() == null ? "" : sourceDrug.getName());
							resultRecord.add(sourceDrug.getATCCodesString());
							resultRecord.add(sourceDrug.getFormulationsString());
							resultRecord.add(sourceDrug.getCount() == null ? "" : Long.toString(sourceDrug.getCount()));
							for (String sourceIngredientResult : sourceIngredientResults) {
								resultRecord.add(sourceIngredientResult);
							}
							resultRecord.add(strengthDeviationPercentage);
							resultRecord.add(mappingTypeDescriptions.get(mappingType));
							resultRecord.add(mappingResultDescriptions.get(mappingResultType));
							
							
							List<CDMConcept> results = mappingResult.get(mappingResultType);
							if ((results != null) && (results.size() > 0)) {
								Collections.sort(results, new Comparator<CDMConcept>() {
									@Override
									public int compare(CDMConcept concept1, CDMConcept concept2) {
										String conceptId1 = concept1 == null ? "" : (concept1.getConceptId() == null ? concept1.getAdditionalInfo() : concept1.getConceptId());
										String conceptId2 = concept2 == null ? "" : (concept2.getConceptId() == null ? concept2.getAdditionalInfo() : concept2.getConceptId());
										return conceptId1.compareTo(conceptId2);
									}
								});
								if ((mappingResultType == DOUBLE_INGREDIENT_MAPPING) || (mappingResultType == UNMAPPED_SOURCE_INGREDIENTS)) {
									if (cdmData != null) {
										List<SourceIngredient> sourceDrugIngredients = sourceDrug.getIngredients();
										for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
											CDMIngredient cdmIngredient = (sourceDrugIngredient.getMatchingIngredient() == null ? null : sourceDrugIngredient.getMatchingIngredient());
											String description = sourceDrugIngredient.toString();
											description += " -> " + (cdmIngredient == null ? "" : cdmIngredient.toString());
											resultRecord.add(description);
										}
									}
									else {
										for (CDMConcept result : results) {
											resultRecord.add(result == null ? "" : ((CDMConcept) result).getAdditionalInfo());
										}
									}
								}
								else {
									for (CDMConcept result : results) {
										if (mappingResultType == REJECTED_BY_MAXIMUM_STRENGTH_DEVIATION) {
											if (cdmData != null) {
												CDMDrug cdmDrug = (CDMDrug) result;
												resultRecord.add(result == null ? "" : (cdmDrug.toString() + ": " + cdmDrug.getStrengthDescription()));
											}
											else {
												resultRecord.add(result == null ? "" : (result.toString() + ": " + result.getAdditionalInfo()));
											}
										}
										else if (mappingResultType == REJECTED_BY_FORM) {
											if (result != null) {
												String formsDescription = "";
												if (cdmData != null) {
													CDMDrug cdmDrug = (CDMDrug) result;
													for (String cdmDrugFormConceptId : cdmDrug.getFormConceptIds()) {
														if (!formsDescription.equals("")) {
															formsDescription += " | ";
														}
														formsDescription += cdmData.getCDMFormConceptName(cdmDrugFormConceptId) + " (" + cdmDrugFormConceptId + ")";
													}
												}
												else {
													// For review
													formsDescription = ((CDMConcept) result).getAdditionalInfo();
												}
												resultRecord.add(result.toString() + ": " + formsDescription);
											}
											else {
												resultRecord.add("");
											}
										}
										else {
											resultRecord.add(result == null ? "" : result.toString());
										}
									}
								}
								resultRecords.add(resultRecord);
							}
							
							mappingResultType++;
						}
					}
				}
			}
			mappingType++;
		}
		
		return resultRecords;
	}
	
	
	public int getMaximumRecordLength(List<List<String>> sourceDrugMappingResultRecords) {
		int maxLength = 0;
		for (List<String> record : sourceDrugMappingResultRecords) {
			maxLength = Math.max(maxLength, record.size());
		}
		return maxLength;
	}
	
	
	private static String[] getHeader(int numberOfResultConcepts) {
		String[] baseHeader = getBaseHeader();
		String[] header = new String[baseHeader.length + numberOfResultConcepts];
		for (int columnNr = 0; columnNr < baseHeader.length; columnNr++) {
			header[columnNr] = baseHeader[columnNr];
		}
		for (Integer resultNr = 1; resultNr <= numberOfResultConcepts; resultNr++) {
			header[baseHeader.length + resultNr - 1] = "Concept_" + resultNr; 
		}
		return header;
	}
	
	
	private static String[] getBaseHeader() {
		String[] header = new String[] {
				"MappingStatus",
				"SourceCode",
				"SourceName",
				"SourceATCCode",
				"SourceFormulation",
				"SourceCount",
				"IngredientCode",
				"IngredientName",
				"IngredientNameEnglish",
				"CASNumber",
				"SourceIngredientAmount",
				"SourceIngredentUnit",
				"StrengthMarginPercentage",
				"MappingType",
				"MappingResult"
		};
		return header;
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
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
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
		report.add("Source drugs mapped to single CDM Clinical Drug: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugs, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugComps, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Form: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugForms, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp/Ingredient: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugCompsIngredients, (long) source.getSourceDrugs().size()));
		report.add("Total Source drugs mapped: " + DrugMappingNumberUtilities.percentage((long) mappingTotal, (long) source.getSourceDrugs().size()));
		
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
		System.out.println();
	}
	
	
	private Double matchingStrength(SourceDrug sourceDrug, CDMDrug cdmDrug) {
		Double matchDeviationPercentage = null;
		
		List<SourceDrugComponent> sourceDrugComponents = sourceDrug.getComponents();
		Map<String, List<CDMIngredientStrength>> cdmDrugComponentsMap = cdmDrug.getIngredientsMap();
		
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			List<CDMIngredientStrength> matchingCDMIngredients = new ArrayList<CDMIngredientStrength>(); 
			for (CDMIngredientStrength cdmIngredient : cdmDrugComponentsMap.get(sourceDrugComponent.getIngredient().getMatchingIngredient().getConceptId())) {
				matchingCDMIngredients.add(cdmIngredient);
			}
			Double bestMatchDeviationPercentage = null;
			int bestMatchIngredientNr = -1;
			for (int ingredientNr = 0; ingredientNr < matchingCDMIngredients.size(); ingredientNr++) {
				CDMIngredientStrength matchingCDMIngredient = matchingCDMIngredients.get(ingredientNr);
				Double ingredientDeviationPercentage = getStrengthDeviationPercentage(sourceDrugComponent, matchingCDMIngredient);
				if (ingredientDeviationPercentage != null) {
					if (ingredientDeviationPercentage.compareTo(DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) <= 0) {
						if ((bestMatchDeviationPercentage == null) || (ingredientDeviationPercentage < bestMatchDeviationPercentage)) {
							bestMatchDeviationPercentage = ingredientDeviationPercentage;
							bestMatchIngredientNr = ingredientNr;
						}
					}
					else {
						break;
					}
				}
				else {
					break;
				}
			}
			if (bestMatchIngredientNr != -1) {
				matchingCDMIngredients.remove(bestMatchIngredientNr);
				matchDeviationPercentage = Math.max(matchDeviationPercentage == null ? 0.0 : matchDeviationPercentage, bestMatchDeviationPercentage);
			}
			else {
				matchDeviationPercentage = null;
				break;
			}
		}
		
		return matchDeviationPercentage;
	}
	
	
	private List<CDMDrug> getLowestAverageStrengthDeviation(Integer mapping, SourceDrug sourceDrug, List<CDMDrug> cdmDrugs) {
		List<CDMDrug> lowestAverageStrengthDeviationDrugs = new ArrayList<CDMDrug>();
		Set<CDMConcept> rejectedByAverageStrength = new HashSet<CDMConcept>();
		Double lowestAverageStrengthDeviation = null;
		
		for (CDMDrug cdmDrug : cdmDrugs) {
			Double averageDeviationPercentage = averageMatchingStrength(sourceDrug, cdmDrug);
			if (averageDeviationPercentage != null) {
				if ((lowestAverageStrengthDeviation == null) || (lowestAverageStrengthDeviation.compareTo(averageDeviationPercentage) > 0)) {
					rejectedByAverageStrength.addAll(lowestAverageStrengthDeviationDrugs);
					lowestAverageStrengthDeviation = averageDeviationPercentage;
					lowestAverageStrengthDeviationDrugs = new ArrayList<CDMDrug>();
					lowestAverageStrengthDeviationDrugs.add(cdmDrug);
				}
				else if (lowestAverageStrengthDeviation.compareTo(averageDeviationPercentage) == 0) {
					lowestAverageStrengthDeviationDrugs.add(cdmDrug);
				}
				else {
					rejectedByAverageStrength.add(cdmDrug);
				}
			}
			else {
				rejectedByAverageStrength.add(cdmDrug);
			}
		}
		if (rejectedByAverageStrength.size() > 0) {
			logMappingResult(sourceDrug, mapping, rejectedByAverageStrength, REJECTED_BY_AVERAGE_STRENGTH_DEVIATION);
		}
		
		return lowestAverageStrengthDeviationDrugs;
	}
	
	
	private Double averageMatchingStrength(SourceDrug sourceDrug, CDMDrug cdmDrug) {
		Double totalDeviationPercentage = null;
		Double averageCount = 0.0;
		
		List<SourceDrugComponent> sourceDrugComponents = sourceDrug.getComponents();
		Map<String, List<CDMIngredientStrength>> cdmDrugComponentsMap = cdmDrug.getIngredientsMap();
		
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			List<CDMIngredientStrength> matchingCDMIngredients = new ArrayList<CDMIngredientStrength>(); 
			for (CDMIngredientStrength cdmIngredient : cdmDrugComponentsMap.get(sourceDrugComponent.getIngredient().getMatchingIngredient().getConceptId())) {
				matchingCDMIngredients.add(cdmIngredient);
			}
			Double bestMatchDeviationPercentage = null;
			int bestMatchIngredientNr = -1;
			for (int ingredientNr = 0; ingredientNr < matchingCDMIngredients.size(); ingredientNr++) {
				CDMIngredientStrength matchingCDMIngredient = matchingCDMIngredients.get(ingredientNr);
				Double ingredientDeviationPercentage = getStrengthDeviationPercentage(sourceDrugComponent, matchingCDMIngredient);
				if (ingredientDeviationPercentage != null) {
					if (ingredientDeviationPercentage.compareTo(DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) <= 0) {
						if ((bestMatchDeviationPercentage == null) || (ingredientDeviationPercentage < bestMatchDeviationPercentage)) {
							bestMatchDeviationPercentage = ingredientDeviationPercentage;
							bestMatchIngredientNr = ingredientNr;
						}
					}
					else {
						break;
					}
				}
				else {
					break;
				}
			}
			if (bestMatchIngredientNr != -1) {
				matchingCDMIngredients.remove(bestMatchIngredientNr);
				totalDeviationPercentage = (totalDeviationPercentage == null ? 0.0 : totalDeviationPercentage) + bestMatchDeviationPercentage;
				averageCount++;
			}
			else {
				totalDeviationPercentage = null;
				break;
			}
		}
		
		return totalDeviationPercentage == null ? null : totalDeviationPercentage / averageCount;
	}
	
	
	private Double matchingStrength(SourceDrugComponent sourceDrugComponent, CDMIngredientStrength cdmIngredientStrength) {
		Double matchDeviationPercentage = null;
		
		Double ingredientDeviationPercentage = getStrengthDeviationPercentage(sourceDrugComponent, cdmIngredientStrength);
		if ((ingredientDeviationPercentage != null) && (ingredientDeviationPercentage.compareTo(DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) <= 0)) {
			matchDeviationPercentage = ingredientDeviationPercentage;
		}
		
		return matchDeviationPercentage;
	}
	
	
	private List<CDMIngredientStrength> matchingIngredients(List<SourceDrugComponent> sourceDrugComponents, Map<String, List<CDMIngredientStrength>> cdmIngredientsMap) {
		List<CDMIngredientStrength> matchingIngredients = new ArrayList<CDMIngredientStrength>();
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
			if (sourceIngredient != null) {
				String cdmIngredientConceptId = sourceIngredient.getMatchingIngredient() == null ? null : sourceIngredient.getMatchingIngredient().getConceptId();
				if (cdmIngredientConceptId != null) {
					List<CDMIngredientStrength> matchingCDMIngredients = cdmIngredientsMap.get(cdmIngredientConceptId);
					if (matchingCDMIngredients != null) {
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
	
	
	private Double getStrengthDeviationPercentage(SourceDrugComponent sourceDrugComponent, CDMIngredientStrength cdmIngredientStrength) {
		Double percentage = null;
		Double sourceStrength = unitConversionsMap.getConversion(sourceDrugComponent.getDosageUnit(), sourceDrugComponent.getDosage(), cdmIngredientStrength.getUnit());
		Double cdmStrength = cdmIngredientStrength.getDosage();
		if ((sourceStrength != null) && (cdmStrength != null)) {
			percentage = (Math.abs(cdmStrength - sourceStrength) / sourceStrength) * 100;
			if (percentage.compareTo(DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION) + DEVIATION_MARGIN) <= 0) {
				percentage = Math.min(DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION), percentage);
			}
		}
		return percentage;
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
			if ((sourceDrug != null) && DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_ATC).equals("Yes")) {
				resultType = REJECTED_BY_ATC_PREFERENCE;
				remove = new ArrayList<CDMConcept>();
				List<String> sourceATCCodes = sourceDrug.getATCCodes();
				for (CDMConcept cdmConcept : conceptList) {
					if (!cdmConcept.getConceptClassId().equals("Ingredient")) {
						boolean found = false;
						for (String sourceATC : sourceATCCodes) {
							if (((CDMDrug) cdmConcept).getATCs().contains(sourceATC)) {
								found = true;
								break;
							}
						}
						if (!found) {
							remove.add(cdmConcept);
						}
					}
				}
				if ((remove.size() > 0) && (conceptList.size() != remove.size())) {
					if (sourceDrug != null) {
						logMappingResult(sourceDrug, mapping, resultType, remove, componentNr);
					}
					conceptList.removeAll(remove);
					preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + "ATC";
				}
			}
		}
		if (conceptList.size() > 1) {
			String vocabulary_id = null;
			if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
				vocabulary_id = "RxNorm";
				resultType = REJECTED_BY_RXNORM_PREFERENCE;
			}
			else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
				vocabulary_id = "RxNorm Extension";
				resultType = REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
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
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + vocabulary_id;
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
	
	
	private void showDrugsList() {
		System.out.println(DrugMapping.getCurrentTime() + "     Showing Drugs List ...");

		mainFrame.showDrugMappingLog(source, cdm, sourceDrugMappingResults, usedStrengthDeviationPercentageMap, DrugMapping.baseName, DrugMapping.settings.getStringSetting(MainFrame.SAVE_DRUGMAPPING_RESULTS).equals("Yes"));
			
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
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
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, List<CDMDrug> cdmDrugList, int resultType) {
		logMappingResult(sourceDrug, mapping, cdmDrugList, resultType, 0);
	}
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.add(null);
		logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, List<CDMDrug> cdmDrugList, int resultType, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		if (cdmDrugList != null) {
			conceptList.addAll(cdmDrugList);
		}
		else {
			conceptList.add(null);
		}
		logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, CDMConcept cdmConcept, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		conceptList.add(cdmConcept);
		logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, Set<CDMConcept> conceptSet, int resultType) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		if (conceptSet != null) {
			conceptList.addAll(conceptSet);
		}
		else {
			conceptList.add(null);
		}
		logMappingResult(sourceDrug, mapping, resultType, conceptList, 0);
	}
	
	
	private void logMappingResult(SourceDrug sourceDrug, int mapping, int resultType, Set<CDMConcept> conceptSet, int componentNr) {
		List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
		if (conceptSet != null) {
			conceptList.addAll(conceptSet);
		}
		else {
			conceptList.add(null);
		}
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
	}
	
	
/*
	public static void main(String[] args) {
	}
*/
}
