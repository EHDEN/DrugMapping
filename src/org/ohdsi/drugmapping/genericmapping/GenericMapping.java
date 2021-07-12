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
import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.gui.files.DelimitedInputFileGUI;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class GenericMapping extends Mapping {
	
	public static String LOGFILE_NAME = "DrugMapping Log.txt";
	
	private static double DEVIATION_MARGIN = 0.000001;

	// Mapping types
	// The mapping type values should start at 0 and incremented by 1.
	public static int CLINICAL_DRUG_MAPPING      = 0;
	public static int CLINICAL_DRUG_FORM_MAPPING = 1;
	public static int CLINICAL_DRUG_COMP_MAPPING = 2;
	public static int INGREDIENT_MAPPING         = 3;
	public static int SPLITTED_MAPPING           = 4;
	
	public static Map<Integer, String> mappingTypeDescriptions;
	
	// Mapping result types.
	// The mapping result type values for each mapping result type should incremented by 1.
	// Make sure that the MAPPED, INCOMPLETE and NO_MAPPED result types are the last three.
	public static int DRUGS_WITH_MATCHING_INGREDIENTS                         =  0; // CDM drugs with matching ingredients.
	public static int NO_SOURCE_INGREDIENTS                                   =  1; // The source drug has no ingredients
	public static int UNMAPPED_SOURCE_INGREDIENTS                             =  2; // The source drug has unmapped ingredients
	public static int DOUBLE_INGREDIENT_MAPPING                               =  3; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	public static int NO_SOURCE_FORMULATION                                   =  4; // The source drug has no formulation.
	public static int NO_SINGLE_INGREDIENT_DRUG                               =  5; // The source drug is not a single ingredient drug.
	public static int NO_DRUGS_WITH_MATCHING_INGREDIENTS                      =  6; // There is no CDM drug with matching ingredients.
	public static int AVAILABLE_FORMS                                         =  7; // The available forms.
	public static int DRUGS_WITH_MATCHING_FORM                                =  8; // CDM drugs with matching form.
	public static int DRUGS_WITH_MATCHING_ATC                                 =  9; // CDM drugs with matching ATC code.
	public static int NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM             = 10; // There is no CDM drug with matching ingredients and form.
	public static int NO_DRUGS_WITH_MATCHING_INGREDIENTS_FORM_AND_STRENGTH    = 11; // There is no CDM drug with matching ingredients strength and form.
	public static int NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_STRENGTH         = 12; // There is no CDM drug with matching ingredients and strength.
	public static int DRUGS_WITH_SMALLEST_STRENGTH_WITHIN_MARGIN              = 13; // The CDM drugs matched on smallest strength deviation within margin.
	public static int DRUGS_WITH_LOWEST_AVERAGE_STRENGTH_DEVIATION            = 14; // The CDM drugs with lowest average strength deviation of the ingredients.
	public static int NO_DRUGS_WITH_MATCHING_ATC                              = 15; // There is no CDM drug with a matching ATC code.
	public static int DRUGS_SELECTED_BY_FORM_PRIORITY                         = 16; // The CDM drugs are selected by dose form priority.
	public static int SELECTED_BY_RXNORM_PREFERENCE                           = 17; // The CDM drugs are rejected because they are not in the preferred RxNorm vocabulary.
	public static int SELECTED_BY_RXNORM_EXTENSION_PREFERENCE                 = 18; // The CDM drugs are rejected because they are not in the preferred RxNorm Extension vocabulary.
	public static int REJECTED_BY_ATC_PREFERENCE                              = 19; // The CDM drugs are rejected because they are not match on ATC code.
	public static int SELECTED_BY_LATEST_DATE_PREFERENCE                      = 20; // The CDM drugs are rejected because they do not have the latest valid start date.
	public static int SELECTED_BY_EARLIEST_DATE_PREFERENCE                    = 21; // The CDM drugs are rejected because they do not have the earliest recent valid start date.
	public static int SELECTED_BY_SMALLEST_CONCEPTID_PREFERENCE               = 22; // The CDM drugs are rejected because they do not have the smallest concept_id.
	public static int SELECTED_BY_GREATEST_CONCEPTID_PREFERENCE               = 23; // The CDM drugs are rejected because they do not have the greatest concept_id.
	public static int NO_UNIQUE_MAPPING                                       = 24; // There are several CDM drugs the source drug could be mapped to.
	public static int SELECTED_BY_FIRST_PREFERENCE                            = 25; // The CDM drugs are rejected because the first one found is taken.
	public static int SELECTED_BY_LAST_PREFERENCE                             = 26; // The CDM drugs are rejected because the last one found is taken.
	public static int OVERRULED_MAPPING                                       = 27; // A mapping to a single CDM drug or a failing mapping is overruled by a manual mapping.
	public static int MAPPED                                                  = 28; // The final mapping of the source drug to a CDM drug.
	public static int INCOMPLETE                                              = 29; // Incomplete splitted mapping.
	public static int NO_MAPPING                                              = 30; // No mapping found.

	public static Map<Integer, String> mappingResultDescriptions;
	static {
		mappingResultDescriptions = new HashMap<Integer  , String>();
		mappingResultDescriptions.put(DRUGS_WITH_MATCHING_INGREDIENTS                        , "Drugs with matching ingredients");
		mappingResultDescriptions.put(NO_SOURCE_INGREDIENTS                                  , "No source ingredients");
		mappingResultDescriptions.put(UNMAPPED_SOURCE_INGREDIENTS                            , "Unmapped source ingredients");
		mappingResultDescriptions.put(DOUBLE_INGREDIENT_MAPPING                              , "Double ingredient mapping");
		mappingResultDescriptions.put(NO_SOURCE_FORMULATION                                  , "No source formulation");
		mappingResultDescriptions.put(NO_SINGLE_INGREDIENT_DRUG                              , "No single ingredient drug");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_INGREDIENTS                     , "No drugs with matching ingredients");
		mappingResultDescriptions.put(AVAILABLE_FORMS                                        , "The available forms");
		mappingResultDescriptions.put(DRUGS_WITH_MATCHING_FORM                               , "Drugs with matching form");
		mappingResultDescriptions.put(DRUGS_WITH_MATCHING_ATC                                , "Drugs with matching ATC");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM            , "No drugs with matching ingredients and form");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_INGREDIENTS_FORM_AND_STRENGTH   , "No drugs with matching ingredients form and strength");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_STRENGTH        , "No drugs with matching ingredients and strength");
		mappingResultDescriptions.put(DRUGS_WITH_SMALLEST_STRENGTH_WITHIN_MARGIN             , "Drugs with smallest strength deviation within margin");
		mappingResultDescriptions.put(DRUGS_WITH_LOWEST_AVERAGE_STRENGTH_DEVIATION           , "Drugs with lowest average strength deviation of the ingredients");
		mappingResultDescriptions.put(NO_DRUGS_WITH_MATCHING_ATC                             , "No drugs with matching ATC");
		mappingResultDescriptions.put(DRUGS_SELECTED_BY_FORM_PRIORITY                        , "Drugs selected by form priority");
		mappingResultDescriptions.put(SELECTED_BY_RXNORM_PREFERENCE                          , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(SELECTED_BY_RXNORM_EXTENSION_PREFERENCE                , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(REJECTED_BY_ATC_PREFERENCE                             , "Rejected by ATC preference");
		mappingResultDescriptions.put(SELECTED_BY_LATEST_DATE_PREFERENCE                     , "Rejected by latest valid start date");
		mappingResultDescriptions.put(SELECTED_BY_EARLIEST_DATE_PREFERENCE                   , "Rejected by earliest valid start date");
		mappingResultDescriptions.put(SELECTED_BY_SMALLEST_CONCEPTID_PREFERENCE              , "Rejected by smallest concept_id");
		mappingResultDescriptions.put(SELECTED_BY_GREATEST_CONCEPTID_PREFERENCE              , "Rejected by greatest concept_id");
		mappingResultDescriptions.put(NO_UNIQUE_MAPPING                                      , "No unique mapping");
		mappingResultDescriptions.put(SELECTED_BY_FIRST_PREFERENCE                           , "Rejected because first is used");
		mappingResultDescriptions.put(SELECTED_BY_LAST_PREFERENCE                            , "Rejected because last is used");
		mappingResultDescriptions.put(OVERRULED_MAPPING                                      , "Overruled mapping");
		mappingResultDescriptions.put(MAPPED                                                 , "Mapped");
		mappingResultDescriptions.put(INCOMPLETE                                             , "Incomplete mapping");
		mappingResultDescriptions.put(NO_MAPPING                                             , "No mapping found");
	}
	
	public static int TRANSLATION_WARNING               = 0;
	public static int UNIT_MAPPING_WARNING              = 1;
	public static int FORM_MAPPING_WARNING              = 2;
	public static int MANUAL_CAS_MAPPING_WARNING        = 3;
	public static int MANUAL_INGREDIENT_MAPPING_WARNING = 4;
	public static int OVERRULE_MAPPING_WARNING          = 5;
	public static int FALLBACK_MAPPING_WARNING          = 6;

	private static Map<Integer, String> warningTypeDescriptions;
	static {
		warningTypeDescriptions = new HashMap<Integer  , String>();
		warningTypeDescriptions.put(TRANSLATION_WARNING                            , "Translation warnings");
		warningTypeDescriptions.put(UNIT_MAPPING_WARNING                           , "Unit mapping warnings");
		warningTypeDescriptions.put(FORM_MAPPING_WARNING                           , "Doseform mapping warnings");
		warningTypeDescriptions.put(MANUAL_CAS_MAPPING_WARNING                     , "Manual CAS ingredient mapping warnings");
		warningTypeDescriptions.put(MANUAL_INGREDIENT_MAPPING_WARNING              , "Manual ingredient mapping warnings");
		warningTypeDescriptions.put(OVERRULE_MAPPING_WARNING                       , "Overrule ingredient mapping warnings");
		warningTypeDescriptions.put(FALLBACK_MAPPING_WARNING                       , "Fallback ingredient mapping warnings");
	}
	
	private static Map<Integer, List<String>> warnings;
	
	public static void addWarning(int warningType, String warning) {
		List<String> warningList = warnings.get(warningType);
		if (warningList == null) {
			warningList = new ArrayList<String>();
			warnings.put(warningType, warningList);
		}
		warningList.add(warning);
		//if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
		//	System.out.println("    WARNING: " + warning);
		//}
	}
	
	public static boolean isMapping              = false;
	public static boolean isSavingDrugMapping    = false;
	public static boolean isSavingDrugMappingLog = false;
	
	
	public static void setMappingTypes(boolean compBeforeForm) {

		CLINICAL_DRUG_MAPPING      = 0;
		CLINICAL_DRUG_COMP_MAPPING = compBeforeForm ? 1 : 2;
		CLINICAL_DRUG_FORM_MAPPING = compBeforeForm ? 2 : 1;
		INGREDIENT_MAPPING         = 3;
		SPLITTED_MAPPING           = 4;
		
		mappingTypeDescriptions = new HashMap<Integer, String>();
		mappingTypeDescriptions.put(CLINICAL_DRUG_MAPPING     , "ClinicalDrug Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_COMP_MAPPING, "ClinicalDrugComp Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_FORM_MAPPING, "ClinicalDrugForm Mapping");
		mappingTypeDescriptions.put(INGREDIENT_MAPPING        , "Ingredient Mapping");
		mappingTypeDescriptions.put(SPLITTED_MAPPING          , "Splitted");
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
		String amount = ((sourceDrugComponent.getDosage() == null) || (sourceDrugComponent.getDosage() == -1.0)) ? "" : format5.format(sourceDrugComponent.getDosage());
		if (!amount.equals("")) {
			amount = amount.contains(".") ? (amount + "00000").substring(0, amount.indexOf(".") + 6) : amount + ".00000";
		}
		return amount;
	}

	
	private static int INGREDIENT_MATCH_OVERRULED    = 0;
	private static int INGREDIENT_MATCH_MANUAL_CAS   = 1;
	private static int INGREDIENT_MATCH_CDM_CAS      = 2;
	private static int INGREDIENT_MATCH_EXTERNAL_CAS = 3;
	private static int INGREDIENT_MATCH_NAME         = 4;
	private static int INGREDIENT_MATCH_ATC          = 5;
	private static int INGREDIENT_MATCH_FALLBACK     = 6;

	private static Map<Integer, String> ingredientMatchingTypeDescriptions;
	static {
		ingredientMatchingTypeDescriptions = new HashMap<Integer  , String>();
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_OVERRULED    , "overruled");
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_MANUAL_CAS   , "manually by CAS number");
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_CDM_CAS      , "by CDM CAS number");
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_EXTERNAL_CAS , "by external CAS number");
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_NAME         , "by name");
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_ATC          , "by ATC");
		ingredientMatchingTypeDescriptions.put(INGREDIENT_MATCH_FALLBACK     , "fallback");
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
	
	private Set<SourceDrug> mappedSourceDrugs = null;
	private Set<SourceDrug> partiallyMappedSourceDrugs = null;
	private Double usedStrengthDeviationPercentage = null;
	private Map<String, Double> usedStrengthDeviationPercentageMap = null;
	
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> sourceDrugMappingResults = null;
	private Map<Integer, Set<SourceDrug>> notUniqueMapping = null;
	
	private List<String> report = null;
	
	private String preferencesUsed = "";

	private Map<Integer, Long> ingredientMatchingStatistics;
	private Map<Integer, Boolean> ingredientMatchingFlags;
	
	
		
	
	public GenericMapping(
					MainFrame mainFrame,
					CDMDatabase database, 
					DelimitedInputFileGUI sourceDrugsFile, 
					DelimitedInputFileGUI ingredientNameTranslationFile, 
					DelimitedInputFileGUI unitMappingFile, 
					DelimitedInputFileGUI formMappingFile, 
					DelimitedInputFileGUI manualCASMappingFile, 
					DelimitedInputFileGUI manualIngredientOverruleMappingFile,
					DelimitedInputFileGUI manualIngredientFallbackMappingFile, 
					DelimitedInputFileGUI manualDrugMappingFile
					) {
		boolean ok = true;
		isMapping = true;
		
		this.mainFrame = mainFrame;
		this.mainFrame.setGenericMapping(this);
		
		setMappingTypes(DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_MATCH_COMP_FORM).equals("Comp before Form"));
		
		// Initialize ingredient matching statistics
		ingredientMatchingStatistics = new HashMap<Integer, Long>();
		for (int statistic = 0; ingredientMatchingTypeDescriptions.get(statistic) != null; statistic++) {
			ingredientMatchingStatistics.put(statistic, 0L);
		}

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
		
		mappedSourceDrugs = new HashSet<SourceDrug>();
		partiallyMappedSourceDrugs = new HashSet<SourceDrug>();
		usedStrengthDeviationPercentageMap = new HashMap<String, Double>();
		
		sourceDrugMappingResults = new HashMap<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>>(); // SourceDrug, Mapping, List of Mapping result, List of options
		notUniqueMapping = new HashMap<Integer, Set<SourceDrug>>();
		
		report = new ArrayList<String>();
		warnings = new HashMap<Integer, List<String>>();
		
		int mapping = 0;
		while (mappingTypeDescriptions.containsKey(mapping)) {
			notUniqueMapping.put(mapping, new HashSet<SourceDrug>());
			mapping++;
		}

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + " Generic Drug Mapping");

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
		
		// Match single ingredient source drugs to Ingredient
		ok = ok && matchSingleIngredient();
		
		// Match source drug ingredients to Clinical Drug Comps or Ingredients
		ok = ok && matchClinicalDrugSplitted();
		
		isMapping = false;

		// Showing Drugs List
		if (ok) showDrugsList();

		// Save mapping
		if (ok) saveMapping();
		
		// Create the final report
		if (ok && (report != null)) finalReport();
		
		writeWarnings();

		System.out.println();
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + " Finished");
	}
	
	
	private boolean getIngredientNameTranslationMap(DelimitedInputFileGUI ingredientNameTranslationFile) {
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
				String ingredientNameEnglish = ingredientNameTranslationMap.getNameIngredientNameEnglish(sourceIngredient);
				if ((ingredientNameEnglish != null) && (!ingredientNameEnglish.equals(""))) {
					sourceIngredient.setIngredientNameEnglish(ingredientNameEnglish);
				}
			}
		}
		
		return ok;
	}
	
	
	private boolean getUnitConversion(DelimitedInputFileGUI unitMappingFile) {
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
	
	
	private boolean getFormConversion(DelimitedInputFileGUI formMappingFile) {
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
	
	
	private boolean getManualCASMappings(DelimitedInputFileGUI manualMappingFile) {
		boolean ok = true;

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Loading manual CAS mappings ...");
		if ((manualMappingFile != null) && manualMappingFile.isSelected()) {
			try {
				
				if (manualMappingFile.openFileForReading(true)) {
					Integer lineNr = 1;
					while (manualMappingFile.hasNext()) {
						lineNr++;
						DelimitedFileRow row = manualMappingFile.next();
						
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
									GenericMapping.addWarning(GenericMapping.MANUAL_CAS_MAPPING_WARNING, "No CDM Ingredient found for concept_id " + cdmConceptId + " for CAS number " + casNumber + " in line " + lineNr + ".");
								}
							}
							else {
								GenericMapping.addWarning(GenericMapping.MANUAL_CAS_MAPPING_WARNING, "No concept_id found in line " + lineNr + ".");
							}
						}
						else {
							GenericMapping.addWarning(GenericMapping.MANUAL_CAS_MAPPING_WARNING, "No CAS number found in line " + lineNr + ".");
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     No manual CAS mappings used.");
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean getManualIngredientMappings(DelimitedInputFileGUI manualMappingFile, String type) {
		boolean ok = true;

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Loading manual ingredient " + type + " mappings ...");
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
				
				if (manualMappingFile.openFileForReading(true)) {

					Integer lineNr = 1;
					while (manualMappingFile.hasNext()) {
						lineNr++;
						DelimitedFileRow row = manualMappingFile.next();
						
						String sourceCode = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "SourceId", true));
						String sourceName = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "SourceName", true));
						String cdmConceptId = DrugMappingStringUtilities.removeExtraSpaces(manualMappingFile.get(row, "concept_id", true));
						//String cdmConceptName = manualMappingFile.get(row, "concept_name", true).trim();
						//String comment = manualMappingFile.get(row, "Comment", true).trim();

						String remark = null;
						if (!cdmConceptId.equals("")) {
							CDMIngredient cdmIngredient = cdm.getCDMIngredients().get(cdmConceptId);
							remark = "Manual mapping: " + cdmConceptId;
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
										GenericMapping.addWarning(GenericMapping.MANUAL_INGREDIENT_MAPPING_WARNING, "No source ingredient found for sourceCode " + sourceCode + " in line " + lineNr + ".");
									}
								}
								if (!sourceName.equals("")) {
									sourceNameFound = true;
									nameMappings.put(sourceName, cdmIngredient);
								}
								if ((!sourceCodeFound) && (!sourceNameFound)) {
									GenericMapping.addWarning(GenericMapping.MANUAL_INGREDIENT_MAPPING_WARNING, "No sourceCode and no sourceName found in line " + lineNr + ".");
								}
							}
							else {
								GenericMapping.addWarning(GenericMapping.MANUAL_INGREDIENT_MAPPING_WARNING, "No CDM Ingredient found for concept_id " + cdmConceptId + " in line " + lineNr + ".");
							}
						}
						else {
							GenericMapping.addWarning(GenericMapping.MANUAL_INGREDIENT_MAPPING_WARNING, "No concept_id found in line " + lineNr + ".");
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     No manual ingredient " + type + " mappings used.");
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean getManualDrugMappings(DelimitedInputFileGUI manualMappingFile) {
		boolean ok = true;

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Loading manual drug mappings ...");
		if ((manualMappingFile != null) && manualMappingFile.isSelected()) {
			try {
				
				if (manualMappingFile.openFileForReading(true)) {
					
					while (manualMappingFile.hasNext()) {
						DelimitedFileRow row = manualMappingFile.next();
						
						String sourceCode = manualMappingFile.get(row, "SourceId", true).trim();
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
							System.out.println("    ERROR: SourceId " + sourceCode + " does not exist!");
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     No manual drug mappings used.");
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchIngredients() {
		boolean ok = true;
		Integer mappedIngredients = 0;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Match Ingredients");
		
		// Initialize ingredient matching counters
		ingredientMatchingStatistics = new HashMap<Integer, Long>();
		for (int statistic = 0; ingredientMatchingTypeDescriptions.get(statistic) != null; statistic++) {
			ingredientMatchingStatistics.put(statistic, 0L);
		}

		for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
			
			// Initialize ingredient matching flags
			ingredientMatchingFlags = new HashMap<Integer, Boolean>();
			for (int statistic = 0; ingredientMatchingTypeDescriptions.get(statistic) != null; statistic++) {
				ingredientMatchingFlags.put(statistic, false);
			}
			
			CDMIngredient cdmIngredient = matchIngredientByCASNumber(sourceIngredient);
			
			if (cdmIngredient == null) {
				cdmIngredient = matchIngredientByExternalCASNumber(sourceIngredient);
			}
			
			if (cdmIngredient == null) {
				cdmIngredient = matchIngredientByName(sourceIngredient);
			}
			
			if (cdmIngredient == null) {
				cdmIngredient = matchIngredientByATC(sourceIngredient);
			}

			CDMIngredient cdmIngredientOverrule = getOverruleMapping(sourceIngredient);
			if (cdmIngredientOverrule != null) {
				if ((cdmIngredient != null) && (cdmIngredient == cdmIngredientOverrule)) {
					addWarning(OVERRULE_MAPPING_WARNING, sourceIngredient + " OVERRULE OBSOLETE");
				}
				else {
					cdmIngredient = cdmIngredientOverrule;
					if (!cdmIngredient.getConceptId().equals("0")) {
						sourceIngredient.setMatchingIngredient(cdmIngredient);
						sourceIngredient.setMatchString("Manual Overrule Mapping" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + sourceIngredient.getIngredientName() + "\")");
					}
					else {
						sourceIngredient.setMatchingIngredient(null);
						sourceIngredient.setMatchString("Manual Overrule to No Mapping" + " (\"" + sourceIngredient.getIngredientName() + "\")");
					}
				}
			}
			

			CDMIngredient cdmIngredientFallback = getFallbackMapping(sourceIngredient);
			if (cdmIngredientFallback != null) {
				if (cdmIngredient == null) {
					cdmIngredient = cdmIngredientFallback;
					if (!cdmIngredient.getConceptId().equals("0")) {
						sourceIngredient.setMatchingIngredient(cdmIngredient);
						sourceIngredient.setMatchString("Manual Fallback Mapping" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + sourceIngredient.getIngredientName() + "\")");
					}
					else {
						sourceIngredient.setMatchString("Manual Fallback to No Mapping" + " (\"" + sourceIngredient.getIngredientName() + "\")");
					}
				}
				else {
					if (cdmIngredient == cdmIngredientFallback) {
						addWarning(FALLBACK_MAPPING_WARNING, sourceIngredient + " FALLBACK OBSOLETE");
					}
					else {
						addWarning(FALLBACK_MAPPING_WARNING, sourceIngredient + " FALLBACK DIFFERENT");
					}
				}
			}

			for (int statistic = 0; ingredientMatchingTypeDescriptions.get(statistic) != null; statistic++) {
				if ((ingredientMatchingFlags.get(INGREDIENT_MATCH_OVERRULED) && (statistic == INGREDIENT_MATCH_OVERRULED)) || (!ingredientMatchingFlags.get(INGREDIENT_MATCH_OVERRULED))) {
					ingredientMatchingStatistics.put(statistic, ingredientMatchingStatistics.get(statistic) + (ingredientMatchingFlags.get(statistic) ? 1 : 0));
				}
			}
			
			if (cdmIngredient != null) {
				mappedIngredients++;
			}
		}

		if (report != null) {
			for (int statistic = 0; ingredientMatchingTypeDescriptions.get(statistic) != null; statistic++) {
				report.add("Source ingredients mapped " + ingredientMatchingTypeDescriptions.get(statistic) + ": " + DrugMappingNumberUtilities.percentage((long) ingredientMatchingStatistics.get(statistic), (long) Source.getAllIngredients().size()));
			}
			report.add("Source ingredients mapped total: " + DrugMappingNumberUtilities.percentage((long) mappedIngredients, (long) Source.getAllIngredients().size()));
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private CDMIngredient matchIngredientByCASNumber(SourceIngredient sourceIngredient) {
		CDMIngredient cdmIngredient = null;
		
		String casNr = sourceIngredient.getCASNumber();
		if (casNr != null) {
			String matchString = null;
			CDMIngredient cdmIngredientCDM = cdm.getCDMCASIngredientMap().get(casNr);
			CDMIngredient cdmIngredientManual = manualCASMappings.get(casNr);
			if (cdmIngredientCDM != null) {
				if (cdmIngredientManual != null) {
					if (cdmIngredientCDM != cdmIngredientManual) {
						cdmIngredient = cdmIngredientManual;
						matchString = "Manual Mapping CAS Code" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + DrugMappingStringUtilities.removeLeadingZeros(casNr) + "\")";
						ingredientMatchingFlags.put(INGREDIENT_MATCH_MANUAL_CAS, true);
					}
					else {
						cdmIngredient = cdmIngredientManual;
						GenericMapping.addWarning(GenericMapping.MANUAL_CAS_MAPPING_WARNING, sourceIngredient + " CAS MAPPING OBSOLETE");
						matchString = "CASCode from Vocab" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + DrugMappingStringUtilities.removeLeadingZeros(casNr) + "\")";
						ingredientMatchingFlags.put(INGREDIENT_MATCH_CDM_CAS, true);
					}
				}
				else {
					cdmIngredient = cdmIngredientCDM;
					matchString = "CASCode from Vocab" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + DrugMappingStringUtilities.removeLeadingZeros(casNr) + "\")";
					ingredientMatchingFlags.put(INGREDIENT_MATCH_CDM_CAS, true);
				}
			}
			else {
				if (cdmIngredientManual != null) {
					cdmIngredient = cdmIngredientManual;
					matchString = "Manual Mapping CAS Code" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + DrugMappingStringUtilities.removeLeadingZeros(casNr) + "\")";
					ingredientMatchingFlags.put(INGREDIENT_MATCH_MANUAL_CAS, true);
				}
			}
			if (cdmIngredient != null) {
				sourceIngredient.setMatchingIngredient(cdmIngredient);
				sourceIngredient.setMatchString(matchString);
			}
		}
		
		return cdmIngredient; 
	}
	
	
	private CDMIngredient matchIngredientByExternalCASNumber(SourceIngredient sourceIngredient) {
		CDMIngredient cdmIngredient = null;

		if (externalCASSynonymsMap != null) {
			String casNumber = sourceIngredient.getCASNumber();
			if (casNumber != null) {
				
				List<String> casNames = externalCASSynonymsMap.get(casNumber);
				if (casNames != null) {
					
					for (String casName : casNames) {
						List<String> matchNameList = DrugMappingStringUtilities.generateMatchingNames(casName, null);
						for (String matchName : matchNameList) {
							String matchType = matchName.substring(0, matchName.indexOf(": "));
							matchName = matchName.substring(matchName.indexOf(": ") + 2);
							
							cdmIngredient = cdm.findIngredientByName(matchName, matchType);
							if (cdmIngredient != null) {
								sourceIngredient.setMatchingIngredient(cdmIngredient);
								sourceIngredient.setMatchString(cdm.getMatchString());
								ingredientMatchingFlags.put(INGREDIENT_MATCH_EXTERNAL_CAS, true);
								break;
							}
						}
						
						if (cdmIngredient != null) {
							break;
						}
					}
				}
			}
		}
		
		return cdmIngredient; 
	}
	
	
	private CDMIngredient matchIngredientByName(SourceIngredient sourceIngredient) {
		CDMIngredient cdmIngredient = null;
		
		if (cdmIngredient == null) { // No manual mapping on ingredient name found
			preferencesUsed = "";

			List<String> matchNameList = sourceIngredient.getIngredientMatchingNames();
			for (String matchName : matchNameList) {
				String matchType = matchName.substring(0, matchName.indexOf(": "));
				matchName = matchName.substring(matchName.indexOf(": ") + 2);
				
				cdmIngredient = cdm.findIngredientByName(matchName, matchType);
				if (cdmIngredient != null) {
					sourceIngredient.setMatchingIngredient(cdmIngredient);
					sourceIngredient.setMatchString(cdm.getMatchString());
					ingredientMatchingFlags.put(INGREDIENT_MATCH_NAME, true);
					break;
				}
			}
		}
		
		return cdmIngredient;
	}
	
	
	private CDMIngredient matchIngredientByATC(SourceIngredient sourceIngredient) {
		CDMIngredient cdmIngredient = null;
		Set<CDMIngredient> cdmATCIngredients = new HashSet<CDMIngredient>();
		Set<SourceDrug> ingredientDrugs = sourceIngredient.getSourceDrugs();
		Set<String> atcCodes = new HashSet<String>();
		Integer multipleMappings = 0;
		
		for (SourceDrug sourceDrug : ingredientDrugs) {
			if (sourceDrug.getIngredients().size() == 1) {
				atcCodes.addAll(sourceDrug.getATCCodes());
			}
		}
		List<String> atcCodesList = new ArrayList<String>();
		atcCodesList.addAll(atcCodes);
		Collections.sort(atcCodesList);
		
		String atcCodesString = "";
		for (String atcCode : atcCodesList) {
			Set<CDMIngredient> atcMatchedIngredients = cdm.getCDMATCIngredientMap().get(atcCode);
			
			if ((atcMatchedIngredients != null) && (atcMatchedIngredients.size() == 1)) { // Ignore ATC with multiple ingredients
				cdmATCIngredients.addAll(atcMatchedIngredients);
				atcCodesString += (atcCodesString.equals("") ? "" : ", ") + atcCode;
			}
			if (cdmATCIngredients.size() > 1) {
				multipleMappings++;
				break;
			}
		}
		
		if (cdmATCIngredients.size() == 1) {
			cdmIngredient = (CDMIngredient) cdmATCIngredients.toArray()[0];
			String matchingATCCodes = "ATC - RxNorm" + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "") + " (\"" + atcCodesString + "\")";
			sourceIngredient.setMatchingIngredient(cdmIngredient);
			sourceIngredient.setMatchString(matchingATCCodes);
			ingredientMatchingFlags.put(INGREDIENT_MATCH_ATC, true);
		}
		
		return cdmIngredient;
	}
	
	
	private CDMIngredient getOverruleMapping(SourceIngredient sourceIngredient) {
		
		CDMIngredient cdmIngredient = manualIngredientCodeOverruleMappings.get(sourceIngredient);
		
		if (cdmIngredient != null) { // Manual mapping on ingredient code found
			ingredientMatchingFlags.put(INGREDIENT_MATCH_OVERRULED, true);
		}
		
		if (cdmIngredient == null) { // No manual mapping on ingredient code found
			if (!sourceIngredient.getIngredientName().equals("")) {
				cdmIngredient = manualIngredientNameOverruleMappings.get(sourceIngredient.getIngredientName());
				if (cdmIngredient != null) {
					ingredientMatchingFlags.put(INGREDIENT_MATCH_OVERRULED, true);
				}
			}
		}
		
		return cdmIngredient;
	}
	
	
	private CDMIngredient getFallbackMapping(SourceIngredient sourceIngredient) {

		CDMIngredient cdmIngredient = manualIngredientCodeFallbackMappings.get(sourceIngredient);
		
		if (cdmIngredient != null) { // Manual mapping on ingredient code found
			ingredientMatchingFlags.put(INGREDIENT_MATCH_FALLBACK, true);
		}
		
		if (cdmIngredient == null) { // No manual mapping on ingredient code found
			if (!sourceIngredient.getIngredientName().equals("")) {
				cdmIngredient = manualIngredientNameFallbackMappings.get(sourceIngredient.getIngredientName());
				if (cdmIngredient != null) {
					ingredientMatchingFlags.put(INGREDIENT_MATCH_FALLBACK, true);
				}
			}
		}
		
		return cdmIngredient;
	}
	
	
	private boolean getSourceDrugsWithAllIngredientsMapped() {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get source drugs with all ingredients mapped ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			List<SourceIngredient> sourceDrugIngredients = sourceDrug.getIngredients();
			if (sourceDrugIngredients.size() > 0) {
				List<CDMIngredient> cdmDrugIngredients = new ArrayList<CDMIngredient>();
				Set<SourceIngredient> sourceIngredientSet = new HashSet<SourceIngredient>();
				Set<CDMIngredient> cdmIngredientSet = new HashSet<CDMIngredient>();
				for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
					sourceIngredientSet.add(sourceDrugIngredient);
					cdmDrugIngredients.add(sourceDrugIngredient.getMatchingIngredient());
					cdmIngredientSet.add(sourceDrugIngredient.getMatchingIngredient());
				}
				if (!cdmDrugIngredients.contains(null)) {
					if (sourceIngredientSet.size() == cdmIngredientSet.size()) {
						sourceDrugsAllIngredientsMapped.add(sourceDrug);
						sourceDrugsCDMIngredients.put(sourceDrug, cdmDrugIngredients);
					}
					else {
						int mapping = 0;
						while ((mapping != INGREDIENT_MAPPING) && (mapping != SPLITTED_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
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
								mappingSourceIngredients.add(sourceDrugIngredient.getMatchingIngredient());
							}
							
							mapping++;
						}
					}
				}
				else {
					int mapping = 0;
					while ((mapping != INGREDIENT_MAPPING) && (mapping != SPLITTED_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
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
							mappingSourceIngredients.add(sourceDrugIngredient.getMatchingIngredient());
						}
						
						mapping++;
					}
				}
			}
			else {
				int mapping = 0;
				while ((mapping != INGREDIENT_MAPPING) && (mapping != SPLITTED_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		if (report != null) {
			report.add("Source drugs with all ingredients mapped: " + DrugMappingNumberUtilities.percentage((long) sourceDrugsAllIngredientsMapped.size(), (long) source.getSourceDrugs().size()));
			report.add("");
		}
		
		return (sourceDrugsAllIngredientsMapped.size() > 0);
	}
	
	
	private boolean matchClinicalDrugs() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_MAPPING;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Match source drugs to Clinical Drugs ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			CDMDrug automaticMapping = null;
			usedStrengthDeviationPercentage = null;
			preferencesUsed = "";
			
			if (sourceDrugsAllIngredientsMapped.contains(sourceDrug)) {
				List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);
				
				if (sourceDrug.getFormulations().size() > 0) {
					if (sourceDrugCDMIngredients.size() > 0) {
						
						// Find CDM Clinical Drugs with corresponding ingredients
						List<CDMDrug> selectedCDMDrugs = selectCDMDrugsWithMatchingIngredients(sourceDrug, mapping);
						
						if (selectedCDMDrugs.size() > 0) {
							// Log available dose forms
							logAvailableForms(sourceDrug, selectedCDMDrugs, mapping);
							
							// Get matching CDM forms
							List<String> matchingCDMForms = getMatchingCDMForms(sourceDrug);

							// Remove all drugs with the wrong form
							selectedCDMDrugs = selectCDMDrugsWithMatchingForm(sourceDrug, selectedCDMDrugs, matchingCDMForms, mapping);

							if (selectedCDMDrugs.size() > 0) {
								// Select drugs with corresponding closest ingredient strengths within margin
								selectedCDMDrugs = selectCDMDrugsWithMatchingStrength(sourceDrug, selectedCDMDrugs, mapping);

								if (selectedCDMDrugs.size() > 0) {
									// Select drug by matching ATC
									selectedCDMDrugs = selectCDMDrugsOnMatchingATC(sourceDrug, selectedCDMDrugs, mapping);


									if (selectedCDMDrugs.size() > 0) {
										// Select drug by matching form with priority
										selectedCDMDrugs = selectCDMDrugsWithMatchingFormByPriority(sourceDrug, selectedCDMDrugs, matchingCDMForms, mapping);

										if (selectedCDMDrugs.size() == 1) {
											automaticMapping = selectedCDMDrugs.get(0);
											sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage);
										}
										else if (selectedCDMDrugs.size() > 1) {
											selectedCDMDrugs = selectConcept(sourceDrug, selectedCDMDrugs, mapping);
											if (selectedCDMDrugs.size() > 1) {
												logMappingResult(sourceDrug, mapping, selectedCDMDrugs, NO_UNIQUE_MAPPING);
												notUniqueMapping.get(mapping).add(sourceDrug);
											}
											else {
												automaticMapping = selectedCDMDrugs.get(0);
												sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage + " " + preferencesUsed);
											}
										}
										else {
											logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS_FORM_AND_STRENGTH);
										}
									}
									else {
										logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_ATC);
									}
								}
								else {
									logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS_FORM_AND_STRENGTH);
								}
							}
							else {
								logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM);
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugComps() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_COMP_MAPPING;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Match source drugs to Clinical Drug Comps ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			CDMDrug automaticMapping = null;
			usedStrengthDeviationPercentage = null;
			
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
						
						// Find CDM Clinical Drug Comps with corresponding ingredient
						List<CDMDrug> selectedCDMDrugs = selectCDMDrugsWithMatchingIngredients(sourceDrug, mapping);
						
						if (selectedCDMDrugs.size() > 0) {
							// Select drugs with corresponding closest ingredient strengths within margin
							selectedCDMDrugs = selectCDMDrugsWithMatchingStrength(sourceDrug, selectedCDMDrugs, mapping);
							
							if (selectedCDMDrugs.size() > 0) {
								// Select drug by matching ATC
								selectedCDMDrugs = selectCDMDrugsOnMatchingATC(sourceDrug, selectedCDMDrugs, mapping);

								if (selectedCDMDrugs.size() > 0) {
									if (selectedCDMDrugs.size() == 1) {
										automaticMapping = selectedCDMDrugs.get(0);
										sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage);
									}
									else if (selectedCDMDrugs.size() > 1) {
										List<CDMDrug> selectedCDMDrugsWithIngredientsAndStrength = selectConcept(sourceDrug, selectedCDMDrugs, mapping);
										if (selectedCDMDrugsWithIngredientsAndStrength.size() > 1) {
											logMappingResult(sourceDrug, mapping, selectedCDMDrugsWithIngredientsAndStrength, NO_UNIQUE_MAPPING);
											notUniqueMapping.get(mapping).add(sourceDrug);
										}
										else if (selectedCDMDrugsWithIngredientsAndStrength.size() == 1) {
											automaticMapping = selectedCDMDrugsWithIngredientsAndStrength.get(0);
											sourceDrug.setMatchString("Strength margin: " + usedStrengthDeviationPercentage + " " + preferencesUsed);
										}
										else {
											logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_STRENGTH);
										}
									}
								}
								else {
									logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_ATC);
								}
							}
							else {
								logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_STRENGTH);
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugForms() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_FORM_MAPPING;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Match source drugs to Clinical Drug Forms ...");
		
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

						// Find CDM Clinical Drug Forms with corresponding ingredients
						List<CDMDrug> selectedCDMDrugs = selectCDMDrugsWithMatchingIngredients(sourceDrug, mapping);
						
						if (selectedCDMDrugs.size() > 0) {
							// Log available dose forms
							logAvailableForms(sourceDrug, selectedCDMDrugs, mapping);
							
							// Get matching CDM forms
							List<String> matchingCDMForms = getMatchingCDMForms(sourceDrug);

							// Remove all drugs with the wrong form
							selectedCDMDrugs = selectCDMDrugsWithMatchingForm(sourceDrug, selectedCDMDrugs, matchingCDMForms, mapping);

							if (selectedCDMDrugs.size() > 0) {
								// Select drug by matching ATC
								selectedCDMDrugs = selectCDMDrugsOnMatchingATC(sourceDrug, selectedCDMDrugs, mapping);

								if (selectedCDMDrugs.size() > 0) {
									// Select drug by matching form with priority
									selectedCDMDrugs = selectCDMDrugsWithMatchingFormByPriority(sourceDrug, selectedCDMDrugs, matchingCDMForms, mapping);

									if (selectedCDMDrugs.size() > 1) {
										preferencesUsed = "";
										selectedCDMDrugs = selectConcept(sourceDrug, selectedCDMDrugs, mapping);
									}

									if (selectedCDMDrugs.size() > 0) {
										List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
										Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
										
										for (CDMDrug cdmDrug : selectedCDMDrugs) {
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
									logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_ATC);
								}
							}
							else {
								logMappingResult(sourceDrug, mapping, NO_DRUGS_WITH_MATCHING_INGREDIENTS_AND_FORM);
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchSingleIngredient() {
		boolean ok = true;
		int mapping = INGREDIENT_MAPPING;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Match single ingredient source drugs to Ingredients ...");
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			boolean earlierNotUniqueMapping = false;
			for (int mappingType : notUniqueMapping.keySet()) {
				if (notUniqueMapping.get(mappingType).contains(sourceDrug)) {
					earlierNotUniqueMapping = true;
					break;
				}
			}
			if ((!mappedSourceDrugs.contains(sourceDrug)) && (!earlierNotUniqueMapping)) {
				if (sourceDrug.getComponents().size() == 1) {
					CDMIngredient cdmIngredient = sourceDrug.getComponents().get(0).getIngredient().getMatchingIngredient();
					if (cdmIngredient != null) {
						mappedSourceDrugs.add(sourceDrug);
						logMappingResult(sourceDrug, mapping, MAPPED, cdmIngredient, 0);
					}
					else {
						logMappingResult(sourceDrug, mapping, NO_MAPPING, 0);
					}
				}
			}
		}

		if (report != null) {
			report.add("Source drugs mapped to CDM Ingredient: " + DrugMappingNumberUtilities.percentage((long) notUniqueMapping.get(mapping).size(), (long) source.getSourceDrugs().size()));
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugSplitted() {
		boolean ok = true;
		int mapping = SPLITTED_MAPPING;
		long completeMappingCount = 0L;
		long incompleteMappingCount = 0L;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Match source drug ingredients to Clinical Drug Comps and Ingredients ...");
		
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
								logMappingResult(sourceDrug, mapping, cdmDrugCompsWithIngredient, DRUGS_WITH_MATCHING_INGREDIENTS, componentNr);

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
									logMappingResult(sourceDrug, mapping, matchingCDMDrugComps, DRUGS_WITH_SMALLEST_STRENGTH_WITHIN_MARGIN, componentNr);

									if (matchingCDMDrugComps != null) {
										if (matchingCDMDrugComps.size() > 1) {
											matchingCDMDrugComps = selectConcept(sourceDrug, matchingCDMDrugComps, mapping, componentNr);
										}
										if (matchingCDMDrugComps.size() == 1) {
											automaticMappings.set(componentNr, matchingCDMDrugComps.get(0));
											usedStrengthDeviationPercentageMap.put("Ingredient " + sourceDrug.getCode() + "," + sourceDrugIngredient.getIngredientCode(), usedStrengthDeviationPercentage.get(componentNr));
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
							}
						}
					}
				}

				int nrIngredientsMapped = 0;
				for (int componentNr = 0; componentNr < sourceDrugComponents.size(); componentNr++) {
					if (automaticMappings.get(componentNr) != null) {
						nrIngredientsMapped++;
					}
				}
				
				int mappingResultType = INCOMPLETE;
				if (nrIngredientsMapped == 0) {
					mappingResultType = NO_MAPPING;
				}
				else if (nrIngredientsMapped == sourceDrugComponents.size()) {
					mappingResultType = MAPPED;
					mappedSourceDrugs.add(sourceDrug);
					completeMappingCount++;
				}
				else {
					partiallyMappedSourceDrugs.add(sourceDrug);
					incompleteMappingCount++;
				}
				
				for (int componentNr = 0; componentNr < sourceDrugComponents.size(); componentNr++) {
					if (automaticMappings.get(componentNr) != null) {
						logMappingResult(sourceDrug, mapping, mappingResultType, automaticMappings.get(componentNr), componentNr);
					}
					else {
						logMappingResult(sourceDrug, mapping, mappingResultType, componentNr);
					}
				}
			}
		}

		if (report != null) {
			report.add("Source drugs completely mapped to multiple CDM Clinical Drug Comp or CDM Ingredient combinations: " + DrugMappingNumberUtilities.percentage(completeMappingCount, (long) source.getSourceDrugs().size()));
			report.add("Source drugs partially mapped to multiple CDM Clinical Drug Comp or CDM Ingredient combinations: " + DrugMappingNumberUtilities.percentage(incompleteMappingCount, (long) source.getSourceDrugs().size()));
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private List<CDMDrug> selectCDMDrugsWithMatchingIngredients(SourceDrug sourceDrug, int mapping) {
		// Find CDM Clinical Drugs with corresponding ingredients
		List<CDMDrug> cdmDrugsWithIngredients = null;
		List<CDMIngredient> foundIngredients = new ArrayList<CDMIngredient>();
		
		if ((mapping != CLINICAL_DRUG_COMP_MAPPING) || (sourceDrug.getIngredients().size() == 1)) {
			List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);
			for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
				List<CDMDrug> cdmDrugsWithIngredient = null;
				if (mapping == CLINICAL_DRUG_MAPPING) {
					cdmDrugsWithIngredient = cdm.getCDMDrugsContainingIngredient(sourceDrugCDMIngredients.size(), cdmIngredient);
				}
				else if (mapping == CLINICAL_DRUG_COMP_MAPPING) {
					cdmDrugsWithIngredient = cdm.getCDMDrugCompsContainingIngredient(cdmIngredient);
				}
				else if (mapping == CLINICAL_DRUG_FORM_MAPPING) {
					cdmDrugsWithIngredient = cdm.getCDMDrugFormsContainingIngredient(sourceDrug.getIngredients().size(), cdmIngredient);
				}
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
		}
		if (cdmDrugsWithIngredients == null) {
			cdmDrugsWithIngredients = new ArrayList<CDMDrug>();
		}
		logMappingResult(sourceDrug, mapping, cdmDrugsWithIngredients, DRUGS_WITH_MATCHING_INGREDIENTS);
		
		return cdmDrugsWithIngredients;
	}
	
	
	private void logAvailableForms(SourceDrug sourceDrug, List<CDMDrug> cdmDrugs, int mapping) {
		// Log available dose forms						
		if (cdmDrugs.size() > 0) {
			Set<CDMConcept> availableForms = new HashSet<CDMConcept>();
			
			for (CDMDrug cdmDrug : cdmDrugs) {
				List<String> cdmDrugFormIds = cdmDrug.getFormConceptIds();
				if (cdmDrugFormIds != null) {
					for (String cdmDrugFormId : cdmDrugFormIds) {
						availableForms.add(cdm.getCDMFormConcept(cdmDrugFormId));
					}
				}
			}
			logMappingResult(sourceDrug, mapping, availableForms, AVAILABLE_FORMS);
		}
	}
	
	
	private List<String> getMatchingCDMForms(SourceDrug sourceDrug) {
		// Get matching CDM forms
		List<String> matchingCDMForms = new ArrayList<String>();
		List<String> sourceDrugForms = sourceDrug.getFormulations();
		
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
		
		return matchingCDMForms;
	}
	
	
	private List<CDMDrug> selectCDMDrugsWithMatchingForm(SourceDrug sourceDrug, List<CDMDrug> cdmDrugs, List<String> matchingCDMForms, int mapping) {
		// Remove all drugs with the wrong form				
		if (cdmDrugs.size() > 0) {
			Set<CDMConcept> rejectedByForm = new HashSet<CDMConcept>();
			
			if (matchingCDMForms.size() > 0) {
				for (CDMDrug cdmDrug : cdmDrugs) {
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
				rejectedByForm.addAll(cdmDrugs);
			}
			
			cdmDrugs.removeAll(rejectedByForm);
			logMappingResult(sourceDrug, mapping, cdmDrugs, DRUGS_WITH_MATCHING_FORM);
		}
		
		return cdmDrugs;
	}
	
	
	private void saveMapping() {
		isSavingDrugMapping = true;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Saving Mappings ...");
		
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
		
		if (DrugMapping.settings.getStringSetting(MainFrame.SAVE_DRUGMAPPING_LOG).equals("Yes")) {
			saveDrugMappingMappingLog(source, sourceDrugMappingResults, usedStrengthDeviationPercentageMap, cdm);
		}
			
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void saveIngredientMapping(List<SourceIngredient> sourceIngredients) {
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Saving Ingredient Mapping Mapping Log ...");
		
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

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Done");
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Saving Ingredient Mapping Review ...");
		
		header =        "SourceIngredientCode";
		header += "," + "SourceIngredientName";
		header += "," + "SourceIngredientEnglishName";
		header += "," + "SourceRecordCount";
		header += "," + "concept_id";
		header += "," + "concept_name";
		header += "," + "concept_class_id";
		header += "," + "vocabulary_id";
		header += "," + "MatchLog";
		PrintWriter ingredientMappingReviewFile = DrugMappingFileUtilities.openOutputFile("IngredientMapping Review.csv", header);
		
		if (ingredientMappingReviewFile != null) {
			
			for (SourceIngredient sourceIngredient : sourceIngredients) {
				CDMIngredient cdmIngredient = sourceIngredient.getMatchingIngredient();
				
				String record = DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
				record += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
				record += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientNameEnglish());
				record += "," + (sourceIngredient.getCount() < 0 ? "?" : sourceIngredient.getCount());
				if (cdmIngredient != null) {
					record += "," + cdmIngredient.getConceptId();
					record += "," + DrugMappingStringUtilities.escapeFieldValue(cdmIngredient.getConceptName());
					record += "," + cdmIngredient.getConceptClassId();
					record += "," + cdmIngredient.getVocabularyId();
					record += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getMatchString());
				}
				else {
					String matchString = DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getMatchString());
					if (matchString.equals("")) {
						matchString = "No mapping found";
					}
					record += ",";
					record += ",";
					record += ",";
					record += ",";
					record += "," + matchString;
				}
				ingredientMappingReviewFile.println(record);
			}
			
			ingredientMappingReviewFile.close();
		}

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Done");
	}
	
	
	private void saveDrugMapping() {
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Saving Drug Mapping ...");
		
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
		header += "," + "vocabulary_id";
		header += "," + "MappingLog";
		
		PrintWriter drugMappingReviewFile = DrugMappingFileUtilities.openOutputFile("DrugMapping Review.csv", header);

		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			String mappingStatus = getMappingStatus(sourceDrug);
			Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappings = sourceDrugMappingResults.get(sourceDrug);
			
			// Get the mapping type
			int mappingType = -1;
			if (sourceDrugMappings != null) {
				int mapping = 0;
				while (mappingTypeDescriptions.get(mapping) != null) {
					if ((mapping == SPLITTED_MAPPING) && (mappingStatus.equals("Mapped") || mappingStatus.equals("Incomplete"))) {
						mappingType = SPLITTED_MAPPING;
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
			drugMappingReviewRecord += "," + (sourceDrug.getCount() < 0 ? "?" : sourceDrug.getCount());
			drugMappingReviewRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceDrug.getFormulationsString());
			
			if (mappingType != -1) {
				String mappingLog = sourceDrug.getMatchString();
				List< Map<Integer, List<CDMConcept>>> mappingResultList = sourceDrugMappings.get(mappingType);
				
				if ((mappingType == INGREDIENT_MAPPING) || (mappingType == SPLITTED_MAPPING)) {
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
						
						CDMConcept target = null;
						if (mappingResultList.get(sourceDrugComponents.indexOf(sourceDrugComponent)).get(MAPPED) != null) {
							target = mappingResultList.get(sourceDrugComponents.indexOf(sourceDrugComponent)).get(MAPPED).get(0);
						}
						else if (mappingResultList.get(sourceDrugComponents.indexOf(sourceDrugComponent)).get(INCOMPLETE) != null) {
							target = mappingResultList.get(sourceDrugComponents.indexOf(sourceDrugComponent)).get(INCOMPLETE).get(0);
						}
						mappingLog = sourceDrugComponent.getMatchString(); 
						
						String standardizedAmount = standardizedAmount(sourceDrugComponent);
						if (standardizedAmount.equals("0.00000")) {
							standardizedAmount = "";
						}
						String drugMappingIngredientRecord = drugMappingRecord;
						drugMappingIngredientRecord += "," + sourceIngredient;
						drugMappingIngredientRecord += "," + standardizedAmount;
						drugMappingIngredientRecord += "," + (standardizedAmount.equals("") ? "" : DrugMappingStringUtilities.escapeFieldValue(sourceDrugComponent.getDosageUnit()));
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
							sourceToConceptRecord += "," + DrugMappingDateUtilities.getCurrentDate();
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
						

						String drugMappingReviewIngredientRecord = drugMappingReviewRecord;
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
						drugMappingReviewIngredientRecord += "," + standardizedAmount;
						drugMappingReviewIngredientRecord += "," + (standardizedAmount.equals("") ? "" : DrugMappingStringUtilities.escapeFieldValue(sourceDrugComponent.getDosageUnit()));
						if (target != null) {
							drugMappingReviewIngredientRecord += "," + target.getConceptId();
							drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(target.getConceptName());
							drugMappingReviewIngredientRecord += "," + target.getConceptClassId();
							drugMappingReviewIngredientRecord += "," + target.getVocabularyId();
							drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(mappingLog);
						}
						else {
							drugMappingReviewIngredientRecord += ",";
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
						sourceToConceptRecord += "," + DrugMappingDateUtilities.getCurrentDate();
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
						drugMappingReviewRecord += "," + target.getVocabularyId();
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
				if (mappingResultList == null) {
					mappingResultList = sourceDrugMappings.get(SPLITTED_MAPPING);
				}
				if (mappingResultList != null) {
					for (int ingredientNr = 0; ingredientNr < mappingResultList.size(); ingredientNr++) {
						SourceDrugComponent sourceDrugComponent = sortedSourceDrugComponents.get(ingredientNr);
						SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
						
						String standardizedAmount = standardizedAmount(sourceDrugComponent);
						if (standardizedAmount.equals("0.00000")) {
							standardizedAmount = "";
						}
						
						String drugMappingReviewIngredientRecord = drugMappingReviewRecord;
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
						drugMappingReviewIngredientRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
						drugMappingReviewIngredientRecord += "," + standardizedAmount;
						drugMappingReviewIngredientRecord += "," + (standardizedAmount.equals("") ? "" : DrugMappingStringUtilities.escapeFieldValue(sourceDrugComponent.getDosageUnit()));
						drugMappingReviewIngredientRecord += ",";
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
					drugMappingReviewIngredientRecord += ",";
					
					drugMappingReviewFile.println(drugMappingReviewIngredientRecord);
				}
			}
		}
		
		for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
			String sourceToConceptRecord = "";
			CDMIngredient target = sourceIngredient.getMatchingIngredient();

			if (target != null) {
				sourceToConceptRecord +=       "Ingredient " + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientCode());
				sourceToConceptRecord += "," + "0";
				sourceToConceptRecord += "," + DrugMapping.settings.getStringSetting(MainFrame.VOCABULARY_ID);
				sourceToConceptRecord += "," + DrugMappingStringUtilities.escapeFieldValue(sourceIngredient.getIngredientName());
				sourceToConceptRecord += "," + target.getConceptId();
				sourceToConceptRecord += "," + target.getVocabularyId();
				sourceToConceptRecord += "," + DrugMappingDateUtilities.getCurrentDate();
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Done");
	}
	
	
	private String getMappingStatus(SourceDrug sourceDrug) {
		String mappingStatus = "Unmapped";
		if (manualDrugMappings.containsKey(sourceDrug)) {
			mappingStatus = "ManualMapping";
		}
		else if (mappedSourceDrugs.contains(sourceDrug)) {
			Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappings = sourceDrugMappingResults.get(sourceDrug);
			int mapping = 0;
			while (mappingTypeDescriptions.get(mapping) != null) {
				if ((sourceDrugMappings.get(mapping) != null) && (sourceDrugMappings.get(mapping).get(0).get(MAPPED) != null)) {
					mappingStatus = mappingTypeDescriptions.get(mapping);
					break;
				}
				mapping++;
			}
		}
		else if (partiallyMappedSourceDrugs.contains(sourceDrug)) {
			mappingStatus = "Incomplete";
		}
		return mappingStatus;
	}
	
	
	public void saveDrugMappingMappingLog(Source source, Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> sourceDrugMappingLog, Map<String, Double> strengthDeviationPercentageMap, CDM cdmData) {
		isSavingDrugMappingLog = true;
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Saving Drug Mapping Mapping Log ...");
		
		// Count the maximum number of concepts in the results
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

		// Add the concept column headers
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
					int countCompare = Long.compare(sourceDrug1.getCount() == null ? -1L : sourceDrug1.getCount(), sourceDrug2.getCount() == null ? -1L : sourceDrug2.getCount()); 
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
									mappingStatus = "Overruled Mapping";
									break;
								}
								else if (sourceDrugComponentMappingLog.keySet().contains(GenericMapping.getMappingResultValue("Incomplete mapping"))) {
									mappingStatus = "Incomplete Mapping";
									break;
								}
								else if (sourceDrugComponentMappingLog.keySet().contains(GenericMapping.getMappingResultValue("Mapped"))) {
									mappingStatus = getMappingStatus(sourceDrug);
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

			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "       Done");
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
					if ((mappingType == INGREDIENT_MAPPING) || (mappingType == SPLITTED_MAPPING)) {   // Mapping on source drug ingredients
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
							if ((mappingResultType == MAPPED) || (mappingResultType == INCOMPLETE)) {
								String key = ((mappingType == INGREDIENT_MAPPING) || (mappingType == SPLITTED_MAPPING)) ? ("Ingredient " + sourceDrug.getCode() + "," + sourceDrug.getIngredients().get(ingredientNr).getIngredientCode()) : ("Drug " + sourceDrug.getCode());
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
										if (mappingResultType == DRUGS_WITH_SMALLEST_STRENGTH_WITHIN_MARGIN) {
											if (cdmData != null) {
												CDMDrug cdmDrug = (CDMDrug) result;
												resultRecord.add(result == null ? "" : (cdmDrug.toString() + ": " + cdmDrug.getStrengthDescription()));
											}
											else {
												resultRecord.add(result == null ? "" : (result.toString() + ": " + result.getAdditionalInfo()));
											}
										}
										else if (mappingResultType == DRUGS_WITH_MATCHING_FORM) {
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
		int CLINICAL_DRUG_COUNTER       = CLINICAL_DRUG_MAPPING * 10;
		int CLINICAL_DRUG_COMP_COUNTER  = CLINICAL_DRUG_COMP_MAPPING * 10;
		int CLINICAL_DRUG_FORM_COUNTER  = CLINICAL_DRUG_FORM_MAPPING * 10;
		int INGREDIENT_COUNTER          = INGREDIENT_MAPPING * 10;
		int SPLITTED_COUNTER            = SPLITTED_MAPPING * 10;
		int SPLITTED_INCOMPLETE_COUNTER = (SPLITTED_MAPPING * 10) + 1;
		int TOTAL_COUNTER               = 10000;
		int NONE_COUNTER                = 10001;

		Long dataCountTotal = 0L;
		Long dataCoverageDrugsAllIngredientsMapped = 0L;
		
		Map<Integer, String> mappingCounterDescriptions = new HashMap<Integer, String>();
		mappingCounterDescriptions.put(CLINICAL_DRUG_COUNTER      , "to Clinical Drug");
		mappingCounterDescriptions.put(CLINICAL_DRUG_COMP_COUNTER , "to Clinical Drug Comp");
		mappingCounterDescriptions.put(CLINICAL_DRUG_FORM_COUNTER , "to Clinical Drug Form");
		mappingCounterDescriptions.put(INGREDIENT_COUNTER         , "to Ingredient");
		mappingCounterDescriptions.put(SPLITTED_COUNTER           , "Splitted");
		mappingCounterDescriptions.put(SPLITTED_INCOMPLETE_COUNTER, "Splitted Incomplete");
		mappingCounterDescriptions.put(TOTAL_COUNTER              , "Total");
		mappingCounterDescriptions.put(NONE_COUNTER               , "to None");
		
		List<Integer> sortedCounters = new ArrayList<Integer>();
		sortedCounters.addAll(mappingCounterDescriptions.keySet());
		Collections.sort(sortedCounters);
		
		Map<Integer, Long> mappingCounters = new HashMap<Integer, Long>();
		Map<Integer, Long> dataCoverageCounters = new HashMap<Integer, Long>();
		for (int counter : sortedCounters) {
			mappingCounters.put(counter, 0L);
			dataCoverageCounters.put(counter, 0L);
		}
		
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			boolean allIngredientsMapped = true;
			for (SourceIngredient sourceIngredient : sourceDrug.getIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {
					allIngredientsMapped = false;
					break;
				}
			}
			if (allIngredientsMapped) {
				dataCoverageDrugsAllIngredientsMapped += sourceDrug.getCount();
			}
			
			Long sourceDrugCount = (sourceDrug.getCount() < 0 ? 0 : sourceDrug.getCount());
			dataCountTotal += sourceDrugCount;
		
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING).get(0).get(MAPPED) != null) {
					mappingCounters.put(CLINICAL_DRUG_COUNTER, mappingCounters.get(CLINICAL_DRUG_COUNTER) + 1);
					dataCoverageCounters.put(CLINICAL_DRUG_COUNTER, dataCoverageCounters.get(CLINICAL_DRUG_COUNTER) + sourceDrugCount);
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING).get(0).get(MAPPED) != null) {
					mappingCounters.put(CLINICAL_DRUG_COMP_COUNTER, mappingCounters.get(CLINICAL_DRUG_COMP_COUNTER) + 1);
					dataCoverageCounters.put(CLINICAL_DRUG_COMP_COUNTER, dataCoverageCounters.get(CLINICAL_DRUG_COMP_COUNTER) + sourceDrugCount);
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING).get(0).get(MAPPED) != null) {
					mappingCounters.put(CLINICAL_DRUG_FORM_COUNTER, mappingCounters.get(CLINICAL_DRUG_FORM_COUNTER) + 1);
					dataCoverageCounters.put(CLINICAL_DRUG_FORM_COUNTER, dataCoverageCounters.get(CLINICAL_DRUG_FORM_COUNTER) + sourceDrugCount);
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING) != null) {
				List<Map<Integer, List<CDMConcept>>> mappingResultsList = sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING);
				if (mappingResultsList != null) {
					for (Map<Integer, List<CDMConcept>> mappingResult : mappingResultsList) {
						if (mappingResult.get(MAPPED) != null) {
							mappingCounters.put(INGREDIENT_COUNTER, mappingCounters.get(INGREDIENT_COUNTER) + 1);
							dataCoverageCounters.put(INGREDIENT_COUNTER, dataCoverageCounters.get(INGREDIENT_COUNTER) + sourceDrugCount);
							break;
						}
					}
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(SPLITTED_MAPPING) != null) {
				List<Map<Integer, List<CDMConcept>>> mappingResultsList = sourceDrugMappingResults.get(sourceDrug).get(SPLITTED_MAPPING);
				if (mappingResultsList != null) {
					for (Map<Integer, List<CDMConcept>> mappingResult : mappingResultsList) {
						if (mappingResult.get(MAPPED) != null) {
							mappingCounters.put(SPLITTED_COUNTER, mappingCounters.get(SPLITTED_COUNTER) + 1);
							dataCoverageCounters.put(SPLITTED_COUNTER, dataCoverageCounters.get(SPLITTED_COUNTER) + sourceDrugCount);
							break;
						}
						else if (mappingResult.get(INCOMPLETE) != null) {
							mappingCounters.put(SPLITTED_INCOMPLETE_COUNTER, mappingCounters.get(SPLITTED_INCOMPLETE_COUNTER) + 1);
							dataCoverageCounters.put(SPLITTED_INCOMPLETE_COUNTER, dataCoverageCounters.get(SPLITTED_INCOMPLETE_COUNTER) + sourceDrugCount);
							break;
						}
					}
				}
			}
		}

		List<String> mappingReport = new ArrayList<String>();
		List<String> dataCoverageReport = new ArrayList<String>();
		
		for (int counter : sortedCounters) {
			if (counter < TOTAL_COUNTER) {
				mappingCounters.put(TOTAL_COUNTER, mappingCounters.get(TOTAL_COUNTER) + mappingCounters.get(counter));
				dataCoverageCounters.put(TOTAL_COUNTER, dataCoverageCounters.get(TOTAL_COUNTER) + dataCoverageCounters.get(counter));
			}
			mappingCounters.put(NONE_COUNTER, (long) source.getSourceDrugs().size() - mappingCounters.get(TOTAL_COUNTER));
			dataCoverageCounters.put(NONE_COUNTER, dataCountTotal - dataCoverageCounters.get(TOTAL_COUNTER));
			
			mappingReport.add("Source drugs mapped " + mappingCounterDescriptions.get(counter) + ": " + DrugMappingNumberUtilities.percentage(mappingCounters.get(counter), (long) source.getSourceDrugs().size()));
			dataCoverageReport.add("Datacoverage source drugs mapped " + mappingCounterDescriptions.get(counter) + ": " + DrugMappingNumberUtilities.percentage(dataCoverageCounters.get(counter), (long) dataCountTotal));
		}

		report.add("");
		report.addAll(mappingReport);
		if (dataCountTotal != 0) {
			report.add("");
			report.add("Datacoverage source drugs with all ingredients mapped: " + DrugMappingNumberUtilities.percentage((long) dataCoverageDrugsAllIngredientsMapped, (long) dataCountTotal));
			report.addAll(dataCoverageReport);
		}
		else {
			report.add("");
			report.add("No datacoverage counts available.");
		}
		/*
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			boolean allIngredientsMapped = true;
			for (SourceIngredient sourceIngredient : sourceDrug.getIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {
					allIngredientsMapped = false;
					break;
				}
			}
			if (allIngredientsMapped) {
				dataCoverageDrugsAllIngredientsMapped += sourceDrug.getCount();
			}
			
			Long sourceDrugCount = (sourceDrug.getCount() < 0 ? 0 : sourceDrug.getCount());
			dataCountTotal += sourceDrugCount;
						
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING).get(0).get(MAPPED) != null) {
					mappingClinicalDrugs++;
					dataCoverageClinicalDrugs += sourceDrugCount;
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING).get(0).get(MAPPED) != null) {
					mappingClinicalDrugComps++;
					dataCoverageClinicalDrugComps += sourceDrugCount;
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING).get(0).get(MAPPED) != null) {
					mappingClinicalDrugForms++;
					dataCoverageClinicalDrugForms += sourceDrugCount;
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING) != null) {
				List<Map<Integer, List<CDMConcept>>> mappingResultsList = sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING);
				if (mappingResultsList != null) {
					for (Map<Integer, List<CDMConcept>> mappingResult : mappingResultsList) {
						if (mappingResult.get(MAPPED) != null) {
							mappingClinicalDrugToIngredient++;
							dataCoverageClinicalDrugToIngredient += sourceDrugCount;
							break;
						}
					}
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(SPLITTED_MAPPING) != null) {
				List<Map<Integer, List<CDMConcept>>> mappingResultsList = sourceDrugMappingResults.get(sourceDrug).get(SPLITTED_MAPPING);
				if (mappingResultsList != null) {
					for (Map<Integer, List<CDMConcept>> mappingResult : mappingResultsList) {
						if (mappingResult.get(MAPPED) != null) {
							mappingClinicalDrugSplitted++;
							dataCoverageClinicalDrugSplitted += sourceDrugCount;
							break;
						}
						else if (mappingResult.get(INCOMPLETE) != null) {
							mappingClinicalDrugSplittedIncomplete++;
							dataCoverageClinicalDrugSplittedIncomplete += sourceDrugCount;
							break;
						}
					}
				}
			}
		}
		
		dataCoverageTotal = dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms + dataCoverageClinicalDrugToIngredient + dataCoverageClinicalDrugSplitted;
		mappingTotal = mappingClinicalDrugs + mappingClinicalDrugComps + mappingClinicalDrugForms + mappingClinicalDrugToIngredient + mappingClinicalDrugSplitted;
		
		report.add("");
		report.add("Source drugs mapped to single CDM Clinical Drug: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugs, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugComps, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Form: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugForms, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Ingredient: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugToIngredient, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Splitted: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugSplitted, (long) source.getSourceDrugs().size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Splitted Incomplete: " + DrugMappingNumberUtilities.percentage((long) mappingClinicalDrugSplittedIncomplete, (long) source.getSourceDrugs().size()));
		report.add("Total Source drugs mapped: " + DrugMappingNumberUtilities.percentage((long) mappingTotal, (long) source.getSourceDrugs().size()));
		
		if (dataCountTotal != 0) {
			report.add("");
			report.add("Datacoverage Source drugs with all ingredients mapped: " + DrugMappingNumberUtilities.percentage((long) dataCoverageDrugsAllIngredientsMapped, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugs, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Comp mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugComps, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Form mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugForms, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Ingredient mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugToIngredient, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Splitted mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugSplitted, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Splitted Incomplete mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageClinicalDrugSplittedIncomplete, (long) dataCountTotal));
			report.add("Total datacoverage drug mapping: " + DrugMappingNumberUtilities.percentage((long) dataCoverageTotal, (long) dataCountTotal));
		}
		else {
			report.add("");
			report.add("No datacoverage counts available.");
		}
		*/
		System.out.println();
		for (String reportLine : report) {
			System.out.println(reportLine);
		}
		System.out.println();
	}
	
	
	private void writeWarnings() {
		if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
			int warningType = 0;
			while (warningTypeDescriptions.containsKey(warningType)) {
				List<String> warningTypeWarnings = warnings.get(warningType);
				if (warningTypeWarnings != null) {
					System.out.println();
					System.out.println(warningTypeDescriptions.get(warningType));
					for (String warning : warningTypeWarnings) {
						System.out.println("  " + warning);
					}
				}
				warningType++;
			}
		}
	}
	
	
	private List<CDMDrug> selectCDMDrugsWithMatchingStrength(SourceDrug sourceDrug, List<CDMDrug> cdmDrugs, int mapping) {
		// Find CDM Clinical Drugs with corresponding ingredient strengths within margin
		if (cdmDrugs.size() > 0) {
			Set<CDMConcept> rejectedByStrength = new HashSet<CDMConcept>();
			
			usedStrengthDeviationPercentage = null;
			List<CDMDrug> minimumDeviationDrugs = new ArrayList<CDMDrug>();
			for (CDMDrug cdmDrug : cdmDrugs) {
				if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
					Double deviationPercentage = matchingStrength(sourceDrug, cdmDrug);
					if (deviationPercentage != null) {
						if (usedStrengthDeviationPercentage == null) {
							usedStrengthDeviationPercentage = deviationPercentage;
							minimumDeviationDrugs.add(cdmDrug);
						}
						else if (deviationPercentage < usedStrengthDeviationPercentage - DEVIATION_MARGIN) {
							usedStrengthDeviationPercentage = deviationPercentage;
							rejectedByStrength.addAll(minimumDeviationDrugs);
							minimumDeviationDrugs.add(cdmDrug);
						}
						else if ((deviationPercentage >= usedStrengthDeviationPercentage - DEVIATION_MARGIN) && (deviationPercentage <= usedStrengthDeviationPercentage + DEVIATION_MARGIN)) {
							minimumDeviationDrugs.add(cdmDrug);
						}
						else {
							rejectedByStrength.add(cdmDrug);
						}
					}
					else {
						rejectedByStrength.add(cdmDrug);
					}
				}
				else {
					rejectedByStrength.add(cdmDrug);
				}
			}
			cdmDrugs.removeAll(rejectedByStrength);
			logMappingResult(sourceDrug, mapping, cdmDrugs, DRUGS_WITH_SMALLEST_STRENGTH_WITHIN_MARGIN);

			if (cdmDrugs.size() > 1) {
				// Select drugs on lowest average strength deviation
				cdmDrugs = getLowestAverageStrengthDeviation(sourceDrug, cdmDrugs, mapping);
			}
		}
		
		return cdmDrugs;
	}
	
	
	private Double matchingStrength(SourceDrug sourceDrug, CDMDrug cdmDrug) {
		Double matchDeviationPercentage = null;
		
		List<SourceDrugComponent> sourceDrugComponents = sourceDrug.getComponents();
		Map<String, List<CDMIngredientStrength>> cdmDrugComponentsMap = new HashMap<String, List<CDMIngredientStrength>>();
		for (String conceptId : cdmDrug.getIngredientsMap().keySet()) {
			List<CDMIngredientStrength> strengthList = new ArrayList<CDMIngredientStrength>();
			cdmDrugComponentsMap.put(conceptId, strengthList);
			strengthList.addAll(cdmDrug.getIngredientsMap().get(conceptId));
		}
		
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			List<CDMIngredientStrength> matchingCDMIngredients = cdmDrugComponentsMap.get(sourceDrugComponent.getIngredient().getMatchingIngredient().getConceptId());
			Double bestMatchDeviationPercentage = null;
			int bestMatchIngredientNr = -1;
			for (int ingredientNr = 0; ingredientNr < matchingCDMIngredients.size(); ingredientNr++) {
				CDMIngredientStrength matchingCDMIngredient = matchingCDMIngredients.get(ingredientNr);
				Double ingredientDeviationPercentage = getStrengthDeviationPercentage(sourceDrugComponent, matchingCDMIngredient);
				if (ingredientDeviationPercentage != null) {
					if (ingredientDeviationPercentage.compareTo(DrugMapping.settings.getDoubleSetting(MainFrame.MAXIMUM_STRENGTH_DEVIATION)) <= 0) {
						if ((bestMatchDeviationPercentage == null) || (ingredientDeviationPercentage.compareTo(bestMatchDeviationPercentage + DEVIATION_MARGIN) < 0)) {
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
	
	
	private List<CDMDrug> getLowestAverageStrengthDeviation(SourceDrug sourceDrug, List<CDMDrug> cdmDrugs, Integer mapping) {
		List<CDMDrug> lowestAverageStrengthDeviationDrugs = new ArrayList<CDMDrug>();
		Double lowestAverageStrengthDeviation = null;
		
		for (CDMDrug cdmDrug : cdmDrugs) {
			Double averageDeviationPercentage = averageMatchingStrength(sourceDrug, cdmDrug);
			if (averageDeviationPercentage != null) {
				if ((lowestAverageStrengthDeviation == null) || lowestAverageStrengthDeviation > (averageDeviationPercentage + DEVIATION_MARGIN)) {
					lowestAverageStrengthDeviation = averageDeviationPercentage;
					lowestAverageStrengthDeviationDrugs = new ArrayList<CDMDrug>();
					lowestAverageStrengthDeviationDrugs.add(cdmDrug);
				}
				else if ((lowestAverageStrengthDeviation >= (averageDeviationPercentage - DEVIATION_MARGIN)) && (lowestAverageStrengthDeviation <= (averageDeviationPercentage + DEVIATION_MARGIN))) {
					lowestAverageStrengthDeviationDrugs.add(cdmDrug);
				}
			}
		}
		logMappingResult(sourceDrug, mapping, lowestAverageStrengthDeviationDrugs, DRUGS_WITH_LOWEST_AVERAGE_STRENGTH_DEVIATION);
		
		return lowestAverageStrengthDeviationDrugs;
	}
	
	
	private Double averageMatchingStrength(SourceDrug sourceDrug, CDMDrug cdmDrug) {
		Double totalDeviationPercentage = null;
		Double averageCount = 0.0;
		
		List<SourceDrugComponent> sourceDrugComponents = sourceDrug.getComponents();
		Map<String, List<CDMIngredientStrength>> cdmDrugComponentsMap = new HashMap<String, List<CDMIngredientStrength>>();
		for (String conceptId : cdmDrug.getIngredientsMap().keySet()) {
			List<CDMIngredientStrength> strengthList = new ArrayList<CDMIngredientStrength>();
			cdmDrugComponentsMap.put(conceptId, strengthList);
			strengthList.addAll(cdmDrug.getIngredientsMap().get(conceptId));
		}
		
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			List<CDMIngredientStrength> matchingCDMIngredients = cdmDrugComponentsMap.get(sourceDrugComponent.getIngredient().getMatchingIngredient().getConceptId());
			Double bestMatchDeviationPercentage = null;
			int bestMatchIngredientNr = -1;
			for (int ingredientNr = 0; ingredientNr < matchingCDMIngredients.size(); ingredientNr++) {
				CDMIngredientStrength matchingCDMIngredient = matchingCDMIngredients.get(ingredientNr);
				Double ingredientDeviationPercentage = getStrengthDeviationPercentage(sourceDrugComponent, matchingCDMIngredient);
				if (ingredientDeviationPercentage != null) {
					if ((bestMatchDeviationPercentage == null) || (ingredientDeviationPercentage < bestMatchDeviationPercentage)) {
						bestMatchDeviationPercentage = ingredientDeviationPercentage;
						bestMatchIngredientNr = ingredientNr;
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
	
	
	private List<CDMDrug> selectCDMDrugsOnMatchingATC(SourceDrug sourceDrug, List<CDMDrug> cdmDrugs, int mapping) {
		// Select drugs with matching ATC
		if (cdmDrugs.size() > 1) {
			List<String> sourceATCCodes = sourceDrug.getATCCodes();
			if ((sourceATCCodes != null) && (sourceATCCodes.size() > 0)) {
				List<CDMDrug> remove = new ArrayList<CDMDrug>();
				for (CDMDrug cdmDrugWithIngredients : cdmDrugs) {
					boolean found = false;
					for (String sourceATC : sourceATCCodes) {
						if ((cdmDrugWithIngredients).getATCs().contains(sourceATC)) {
							found = true;
							break;
						}
					}
					if (!found) {
						remove.add(cdmDrugWithIngredients);
					}
				}
				if ((remove.size() > 0) && (cdmDrugs.size() != remove.size())) {
					cdmDrugs.removeAll(remove);
					logMappingResult(sourceDrug, mapping, cdmDrugs, DRUGS_WITH_MATCHING_ATC);
				}
			}
		}
		return cdmDrugs;
	}
	
	
	private List<CDMDrug> selectCDMDrugsWithMatchingFormByPriority(SourceDrug sourceDrug, List<CDMDrug> cdmDrugs, List<String> matchingCDMForms, int mapping) {
		List<CDMDrug> cdmDrugsWithMatchingFormByPriority = new ArrayList<CDMDrug>();

		if (cdmDrugs.size() > 1) {
			if ((matchingCDMForms != null) && (matchingCDMForms.size() > 0)) {
				for (String cdmForm : matchingCDMForms) {
					for (CDMDrug cdmDrug : cdmDrugs) {
						List<String> cdmDrugForms = cdmDrug.getFormConceptNames();
						if (cdmDrugForms.contains(cdmForm)) {
							cdmDrugsWithMatchingFormByPriority.add(cdmDrug);
						}
					}
					if (cdmDrugsWithMatchingFormByPriority.size() > 0) {
						break;
					}
				}
				logMappingResult(sourceDrug, mapping, cdmDrugsWithMatchingFormByPriority, DRUGS_SELECTED_BY_FORM_PRIORITY);
			}
		}
		else {
			cdmDrugsWithMatchingFormByPriority = cdmDrugs;
		}
		
		return cdmDrugsWithMatchingFormByPriority;
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
	

	private List<CDMConcept> selectConcept(SourceDrug sourceDrug, int mapping, List<CDMConcept> conceptList) {
		return selectConcept(sourceDrug, mapping, conceptList, 0);
	}
	
	
	private List<CDMConcept> selectConcept(SourceDrug sourceDrug, int mapping, List<CDMConcept> conceptList, int componentNr) {
		int resultType = -1;
		List<CDMConcept> remove;

		preferencesUsed = "";
		
		// Remove orphan ingredients when there are non-orphan ingredients
		if (conceptList.size() > 1) {
			if ((sourceDrug == null) && DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_NON_ORPHAN_INGREDIENTS).equals("Yes")) {
				Set<CDMIngredient> orphanIngredients = new HashSet<CDMIngredient>();
				for (CDMConcept cdmConcept : conceptList) {
					CDMIngredient cdmIngredient = (CDMIngredient) cdmConcept;
					if (cdmIngredient.isOrphan()) {
						orphanIngredients.add(cdmIngredient);
					}
				}
				if (orphanIngredients.size() != conceptList.size()) {
					conceptList.removeAll(orphanIngredients);
				}
			}
		}
		
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
				resultType = SELECTED_BY_RXNORM_PREFERENCE;
			}
			else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
				vocabulary_id = "RxNorm Extension";
				resultType = SELECTED_BY_RXNORM_EXTENSION_PREFERENCE;
			}
			remove = new ArrayList<CDMConcept>();
			for (CDMConcept cdmConcept : conceptList) {
				if (!cdmConcept.getVocabularyId().equals(vocabulary_id)) {
					remove.add(cdmConcept);
				}
			}
			if ((remove.size() > 0) && (conceptList.size() != remove.size())) {
				conceptList.removeAll(remove);
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
				}
				preferencesUsed += (preferencesUsed.equals("") ? "Preferences: " : ",") + vocabulary_id;
			}
		}
		if (conceptList.size() > 1) {
			if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No")) {
				boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
				resultType = latest ? SELECTED_BY_LATEST_DATE_PREFERENCE : SELECTED_BY_EARLIEST_DATE_PREFERENCE;
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
				conceptList.removeAll(remove);
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
				}
			}
		}
		if (conceptList.size() > 1) {
			if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No")) {
				boolean smallest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
				resultType = smallest ? SELECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : SELECTED_BY_GREATEST_CONCEPTID_PREFERENCE;
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
				conceptList.removeAll(remove);
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
				}
			}
		}
		if (conceptList.size() > 1) {
			if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None")) {
				boolean first = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First");
				resultType = first ? SELECTED_BY_FIRST_PREFERENCE : SELECTED_BY_LAST_PREFERENCE;
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
				conceptList.removeAll(remove);
				if (sourceDrug != null) {
					logMappingResult(sourceDrug, mapping, resultType, conceptList, componentNr);
				}
			}
		}
		
		return conceptList;
	}
	
	
	private void showDrugsList() {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Showing Mappings ...");

		mainFrame.showDrugMappingLog(source, cdm, sourceDrugMappingResults, usedStrengthDeviationPercentageMap, DrugMapping.baseName, DrugMapping.settings.getStringSetting(MainFrame.SAVE_DRUGMAPPING_LOG).equals("Yes"));
			
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
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
