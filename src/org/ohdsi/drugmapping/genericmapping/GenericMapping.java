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
import java.util.Set;

import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.FormConversion;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.UnitConversion;
import org.ohdsi.drugmapping.cdm.CDMDrug;
import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.cdm.CDMIngredientStrength;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.Row;

public class GenericMapping extends Mapping {

	// Mapping types
	// The mapping type values should start at 0 and incremented by 1.
	private static int CLINICAL_DRUG_MAPPING      = 0;
	private static int CLINICAL_DRUG_COMP_MAPPING = 1;
	private static int CLINICAL_DRUG_FORM_MAPPING = 2;
	
	private static Map<Integer, String> mappingTypeDescriptions;
	static {
		mappingTypeDescriptions = new HashMap<Integer, String>();
		mappingTypeDescriptions.put(CLINICAL_DRUG_MAPPING     , "ClinicalDrug Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_COMP_MAPPING, "ClinicalDrugComp Mapping");
		mappingTypeDescriptions.put(CLINICAL_DRUG_FORM_MAPPING, "ClinicalDrugForm Mapping");
	}
	
	// Mapping result types.
	// The mapping result type values for each mapping type should start at 10 times
	// the mapping type value and incremented by 1.
	// Make sure that the MAPPED result type is the last one for each mapping type and
	// that there is a gap between the last result type value of one mapping type and
	// the first result type value of the next mapping type.
	private static int CLINICAL_DRUG_MAPPING_NO_SOURCE_INGREDIENTS                        =   0; // The source drug has no ingredients
	private static int CLINICAL_DRUG_MAPPING_UNMAPPED_SOURCE_INGREDIENTS                  =   1; // The source drug has unmapped ingredients
	private static int CLINICAL_DRUG_MAPPING_DOUBLE_INGREDIENT_MAPPING                    =   2; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int CLINICAL_DRUG_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS           =   3; // There is no CDM clinical drug with matching ingredients.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_FORM                             =   4; // The CDM clinical drugs rejected because they have a different form than the source drug.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_STRENGTH                         =   5; // The CDM clinical drugs rejected because they have a different strength than the source drug.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_PREFERENCE                =   6; // The CDM clinical drugs rejected because they are not in the preferred RxNorm vocabulary.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE      =   7; // The CDM clinical drugs rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE           =   8; // The CDM clinical drugs rejected because they do not have the latest valid start date.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE           =   9; // The CDM clinical drugs rejected because they do not have the oldest recent valid start date.
	private static int CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING                            =  10; // There are several clinical drugs the source drug could be mapped to.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_FIRST_PREFERENCE                 =  11; // The CDM clinical drugs rejected because the first one found is taken.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_LAST_PREFERENCE                  =  12; // The CDM clinical drugs rejected because the last one found is taken.
	private static int CLINICAL_DRUG_MAPPING_OVERRULED_MAPPING                            =  13; // A mapping to a single clinical drug or a failing mapping is overruled by a manual mapping.
	private static int CLINICAL_DRUG_MAPPING_MAPPED                                       =  14; // The final mapping of the source drug to a clinical drug.

	private static int CLINICAL_DRUG_COMP_MAPPING_NO_SOURCE_INGREDIENTS                   = 100; // The source drug has no ingredients
	private static int CLINICAL_DRUG_COMP_MAPPING_UNMAPPED_SOURCE_INGREDIENTS             = 101; // The source drug has unmapped ingredients
	private static int CLINICAL_DRUG_COMP_MAPPING_DOUBLE_INGREDIENT_MAPPING               = 102; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int CLINICAL_DRUG_COMP_MAPPING_NO_SINGLE_INGREDIENT_DRUG               = 103; // The source drug should have only one ingredient to map to a CDM clinical drug comp.
	private static int CLINICAL_DRUG_COMP_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS      = 104; // There is no CDM clinical drug comp with matching ingredients.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_STRENGTH                    = 105; // The CDM clinical drug comps rejected because they have a different strength than the source drug.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_PREFERENCE           = 106; // The CDM clinical drug comps rejected because they are not in the preferred RxNorm vocabulary.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE = 107; // The CDM clinical drug comps rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE      = 108; // The CDM clinical drug comps rejected because they do not have the latest valid start date.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE      = 109; // The CDM clinical drug comps rejected because they do not have the oldest valid start date.
	private static int CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING                       = 110; // There are several clinical drug comps the source drug could be mapped to.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_FIRST_PREFERENCE            = 111; // The CDM clinical drug comps rejected because the first one found is taken.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LAST_PREFERENCE             = 112; // The CDM clinical drug comps rejected because the last one found is taken.
	private static int CLINICAL_DRUG_COMP_MAPPING_OVERRULED_MAPPING                       = 113; // A mapping to a single clinical drug comp or a failing mapping is overruled by a manual mapping.
	private static int CLINICAL_DRUG_COMP_MAPPING_MAPPED                                  = 114; // The final mapping of the source drug to a clinical drug comp.

	private static int CLINICAL_DRUG_FORM_MAPPING_NO_SOURCE_INGREDIENTS                   = 200; // The source drug has no ingredients
	private static int CLINICAL_DRUG_FORM_MAPPING_UNMAPPED_SOURCE_INGREDIENTS             = 201; // The source drug has unmapped ingredients
	private static int CLINICAL_DRUG_FORM_MAPPING_DOUBLE_INGREDIENT_MAPPING               = 202; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int CLINICAL_DRUG_FORM_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS      = 203; // There is no CDM clinical drug form with matching ingredients.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FORM                        = 204; // The CDM clinical drug forms rejected because they have a different form than the source drug.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_PREFERENCE           = 205; // The CDM clinical drug forms rejected because they are not in the preferred RxNorm vocabulary.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE = 206; // The CDM clinical drug forms rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE      = 207; // The CDM clinical drug forms rejected because they do not have the latest valid start date.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE      = 208; // The CDM clinical drug forms rejected because they do not have the oldest valid start date.
	private static int CLINICAL_DRUG_FORM_MAPPING_NO_UNIQUE_MAPPING                       = 209; // There are several clinical drug forms the source drug could be mapped to.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FIRST_PREFERENCE            = 210; // The CDM clinical drug forms rejected because the first one found is taken.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LAST_PREFERENCE             = 211; // The CDM clinical drug forms rejected because the last one found is taken.
	private static int CLINICAL_DRUG_FORM_MAPPING_OVERRULED_MAPPING                       = 212; // A mapping to a single clinical drug form or a failing mapping is overruled by a manual mapping.
	private static int CLINICAL_DRUG_FORM_MAPPING_MAPPED                                  = 213; // The final mapping of the source drug to a clinical drug form.

	private static Map<Integer, String> mappingResultDescriptions;
	static {
		mappingResultDescriptions = new HashMap<Integer, String>();
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_NO_SOURCE_INGREDIENTS                       , "No source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_UNMAPPED_SOURCE_INGREDIENTS                 , "Unmapped source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_DOUBLE_INGREDIENT_MAPPING                   , "Double ingredient mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS          , "No drugs with matching ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_FORM                            , "Rejected by form");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_STRENGTH                        , "Rejected by strength");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_PREFERENCE               , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE     , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE          , "Rejected by latest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE          , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING                           , "No unique mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_FIRST_PREFERENCE                , "Rejected because first is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_LAST_PREFERENCE                 , "Rejected because last is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_MAPPED                                      , "Mapped");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_OVERRULED_MAPPING                           , "Overruled mapping");

		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_SOURCE_INGREDIENTS                  , "No source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_UNMAPPED_SOURCE_INGREDIENTS            , "Unmapped source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_DOUBLE_INGREDIENT_MAPPING              , "Double ingredient mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_SINGLE_INGREDIENT_DRUG              , "No single ingredient drug");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS     , "No drugs with matching ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_STRENGTH                   , "Rejected by strength");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_PREFERENCE          , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE, "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE     , "Rejected by latest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE     , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING                      , "No unique mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_FIRST_PREFERENCE           , "Rejected because first is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LAST_PREFERENCE            , "Rejected because last is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_MAPPED                                 , "Mapped");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_OVERRULED_MAPPING                      , "Overruled mapping");

		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_NO_SOURCE_INGREDIENTS                  , "No source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_UNMAPPED_SOURCE_INGREDIENTS            , "Unmapped source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_DOUBLE_INGREDIENT_MAPPING              , "Double ingredient mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS     , "No drugs with matching ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FORM                       , "Rejected by form");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_PREFERENCE          , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE, "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE     , "Rejected by latest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE     , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_NO_UNIQUE_MAPPING                      , "No unique mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FIRST_PREFERENCE           , "Rejected because first is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LAST_PREFERENCE            , "Rejected because last is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_MAPPED                                 , "Mapped");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_OVERRULED_MAPPING                      , "Overruled mapping");
	}
	
	private Set<String> forms = new HashSet<String>();
	private Set<String> units = new HashSet<String>();

	private Map<SourceDrug, CDMDrug> manualMappings = new HashMap<SourceDrug, CDMDrug>();
	private UnitConversion unitConversionsMap = null;
	private FormConversion formConversionsMap = null;
	private Map<String, List<String>> casMap = new HashMap<String, List<String>>();
	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private List<SourceDrug> sourceDrugsAllIngredientsMapped = new ArrayList<SourceDrug>();
	private Map<SourceDrug, List<CDMIngredient>> sourceDrugsCDMIngredients = new HashMap<SourceDrug, List<CDMIngredient>>();
	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<String, Set<CDMIngredient>> cdmIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<CDMIngredient>> cdmMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<CDMIngredient>> cdmReplacedByIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<CDMIngredient>> cdmATCIngredientMap = new HashMap<String, Set<CDMIngredient>>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<String, Set<CDMIngredient>> mapsToCDMIngredient = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<String>> drugNameSynonyms = new HashMap<String, Set<String>>();
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<String, CDMDrug> cdmDrugComps = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<String, CDMDrug> cdmDrugForms = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugFormsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	
	private Set<SourceDrug> mappedSourceDrugs = new HashSet<SourceDrug>();
	
	private List<String> cdmIngredientNameIndexNameList = new ArrayList<String>();
	private Map<String, Map<String, Set<CDMIngredient>>> cdmIngredientNameIndexMap = new HashMap<String, Map<String, Set<CDMIngredient>>>();
	
	private Map<SourceDrug, Map<Integer, Map<Integer, List<String>>>> sourceDrugMappingResults = new HashMap<SourceDrug, Map<Integer, Map<Integer, List<String>>>>(); // SourceDrug, Mapping, Mapping result, List of options
	private Map<Integer, Set<SourceDrug>> notUniqueMapping = new HashMap<Integer, Set<SourceDrug>>();
	
	private Map<Integer, Map<Integer, Long>> counters;
	private Map<Integer, Map<Integer, Long>> dataCoverage;
	
	private List<String> report = new ArrayList<String>();
	
	
	public static String uniformCASNumber(String casNumber) {
		casNumber = casNumber.replaceAll(" ", "").replaceAll("-", "");
		if (!casNumber.equals("")) {
			casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
			casNumber = ("000000"+casNumber).substring(casNumber.length() -5);
		}
		return  casNumber.equals("000000-00-0") ? "" : casNumber;
	}
	
	
	public static String modifyName(String name) {
		Map<String, String> patternReplacement = new HashMap<String, String>();
		
		name = " " + name.toUpperCase() + " ";
		
		List<String> patterns = new ArrayList<String>();
		patterns.add("-");
		patterns.add(",");
		patterns.add("/");
		patterns.add("[(]");
		patterns.add("[)]");
		patterns.add("_");

		// Prevent these seperate letters to be patched
		patterns.add(" A ");
		patterns.add(" O ");
		patterns.add(" E ");
		patterns.add(" U ");
		patterns.add(" P ");
		patterns.add(" H ");

		patterns.add("AAT");
		patterns.add("OOT");
		patterns.add("ZUUR");
		patterns.add("AA");
		patterns.add("OO");
		patterns.add("EE");
		patterns.add("UU");
		patterns.add("TH");
		patterns.add("AE");
		patterns.add("EA");
		patterns.add("PH");
		patterns.add("S ");
		patterns.add("E ");
		patterns.add(" ");

		patterns.add("_");

		patterns.add("AA");
		patterns.add("OO");
		patterns.add("EE");
		patterns.add("UU");
		patterns.add("TH");
		patterns.add("AE");
		patterns.add("EA");
		patterns.add("PH");
		
		patternReplacement.put("-", " ");
		patternReplacement.put(",", " ");
		patternReplacement.put("/", " ");
		patternReplacement.put("[(]", " ");
		patternReplacement.put("[)]", " ");
		patternReplacement.put("_", " ");

		// Prevent these seperate letters to be patched
		patternReplacement.put(" A ", "_A_");
		patternReplacement.put(" O ", "_O_");
		patternReplacement.put(" E ", "_E_");
		patternReplacement.put(" U ", "_U_");
		patternReplacement.put(" P ", "_P_");
		patternReplacement.put(" H ", "_H_");

		patternReplacement.put("AAT", "ATE");
		patternReplacement.put("OOT", "OTE");
		patternReplacement.put("ZUUR", "ACID");
		patternReplacement.put("AA", "A");
		patternReplacement.put("OO", "O");
		patternReplacement.put("EE", "E");
		patternReplacement.put("UU", "U");
		patternReplacement.put("TH", "T");
		patternReplacement.put("AE", "A");
		patternReplacement.put("EA", "A");
		patternReplacement.put("PH", "F");
		patternReplacement.put("S ", " ");
		patternReplacement.put("E ", " ");
		patternReplacement.put(" ", "");

		patternReplacement.put("_", " ");

		patternReplacement.put("AA", "A");
		patternReplacement.put("OO", "O");
		patternReplacement.put("EE", "E");
		patternReplacement.put("UU", "U");
		patternReplacement.put("TH", "T");
		patternReplacement.put("AE", "A");
		patternReplacement.put("EA", "A");
		patternReplacement.put("PH", "F");
		
		for (String pattern : patterns) {
			if (pattern.substring(0, 1).equals("^")) {
				if (pattern.substring(pattern.length() - 1).equals("$")) {
					if (name.equals(pattern.substring(1, pattern.length() - 1))) {
						name = patternReplacement.get(pattern);
					}
				}
				else {
					if ((name.length() >= pattern.length() - 1) && name.substring(0, pattern.length() - 1).equals(pattern.substring(1))) {
						name = patternReplacement.get(pattern) + name.substring(pattern.length() - 1);
					}
				}
			}
			else if (pattern.substring(pattern.length() - 1).equals("$")) {
				if ((name.length() >= pattern.length() - 1) && name.substring(name.length() - pattern.length() + 1).equals(pattern.substring(0, pattern.length() - 1))) {
					name = name.substring(0, name.length() - pattern.length() + 1) + patternReplacement.get(pattern);
				}
			}
			else {
				name = name.replaceAll(pattern, patternReplacement.get(pattern));
			}
		}
		
		return name.trim();
	}
	
	
		
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile casFile, InputFile manualMappingFile) {
		boolean ok = true;

		cdmIngredientNameIndexNameList.add("Ingredient");
		cdmIngredientNameIndexMap.put("Ingredient", cdmIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("MapsToIngredient");
		cdmIngredientNameIndexMap.put("MapsToIngredient", cdmMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("ReplacedByIngredient");
		cdmIngredientNameIndexMap.put("ReplacedByIngredient", cdmReplacedByIngredientNameIndex);
		
		int mapping = 0;
		while (mappingTypeDescriptions.containsKey(mapping)) {
			notUniqueMapping.put(mapping, new HashSet<SourceDrug>());
			mapping++;
		}

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");
		SourceDrug.init();
		
		// Load source drug ingredient mapping
		ok = ok && getSourceDrugs(sourceDrugsFile, DrugMapping.settings.getLongSetting(MainFrame.MINIMUM_USE_COUNT)) && (!SourceDrug.errorOccurred());
		
		// Get unit conversion from local units to CDM units
		ok = ok && getUnitConversion(database);
		
		// Get form conversion from local forms to CDM forms
		ok = ok && getFormConversion(database);
		
		// Get CDM Ingredients
		ok = ok && getCDMData(database);		
		
		// Load manual mappings
		ok = ok && getManualMappings(manualMappingFile);
		
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
		
		if (sourceDrugsFile.openFile()) {

			PrintWriter missingATCFile = null;
			try {
				// Create output file
				fileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "DrugMapping Missing ATC.csv";
				missingATCFile = new PrintWriter(new File(fileName));
				SourceDrug.writeHeaderToFile(missingATCFile);
			} 
			catch (FileNotFoundException e) {
				System.out.println("       WARNING: Cannot create output file '" + fileName + "'");
				missingATCFile = null;
			}
			
			while (sourceDrugsFile.hasNext()) {
				Row row = sourceDrugsFile.next();
				
				String sourceCode = sourceDrugsFile.get(row, "SourceCode").trim();
				
				if ((!sourceCode.equals("")) && (!ignoredSourceCodes.contains(sourceCode))) {
					SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
					
					if (sourceDrug == null) {
						sourceDrugCount++;
						sourceDrug = new SourceDrug(
											sourceCode, 
											sourceDrugsFile.get(row, "SourceName").trim().toUpperCase(), 
											sourceDrugsFile.get(row, "SourceATCCode").trim().toUpperCase(), 
											sourceDrugsFile.get(row, "SourceFormulation").trim().toUpperCase(), 
											sourceDrugsFile.get(row, "SourceCount").trim()
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
						String ingredientName        = sourceDrugsFile.get(row, "IngredientName").trim().toUpperCase(); 
						String ingredientNameEnglish = sourceDrugsFile.get(row, "IngredientNameEnglish").trim().toUpperCase();
						String dosage                = sourceDrugsFile.get(row, "Dosage").trim(); 
						String dosageUnit            = sourceDrugsFile.get(row, "DosageUnit").trim().toUpperCase(); 
						String casNumber             = sourceDrugsFile.get(row, "CASNumber").trim();
						
						while (ingredientName.contains("  "))        ingredientName        = ingredientName.replaceAll("  ", " ");
						while (ingredientNameEnglish.contains("  ")) ingredientNameEnglish = ingredientNameEnglish.replaceAll("  ", " ");
						while (dosage.contains("  "))                dosage                = dosage.replaceAll("  ", " ");
						while (dosageUnit.contains("  "))            dosageUnit            = dosageUnit.replaceAll("  ", " ");
						casNumber = uniformCASNumber(casNumber);

						// Remove comma's
						ingredientName = ingredientName.replaceAll(",", " ").replaceAll("  ", " ");
						ingredientNameEnglish = ingredientNameEnglish.replaceAll(",", " ").replaceAll("  ", " ");

						SourceIngredient sourceIngredient = null;
						if (!ingredientName.equals("")) {
							sourceIngredient = SourceDrug.getIngredient(ingredientName, ingredientNameEnglish, casNumber);
							if (sourceIngredient == null) {
								sourceDrugError = true;
							}
							else {
								sourceIngredient = sourceDrug.AddIngredient(sourceIngredient, dosage, dosageUnit);
								sourceIngredient.addCount(sourceDrug.getCount());
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
			
			report.add("Found " + sourceDrugCount + " source drugs");
			List<SourceDrug> sourceDrugsToBeRemoved = new ArrayList<SourceDrug>();
			for (SourceDrug sourceDrug : sourceDrugs) {
				if (sourceDrug.getCount() < minimumUseCount) {
					sourceDrugsToBeRemoved.add(sourceDrug);
				}
			}
			sourceDrugs.removeAll(sourceDrugsToBeRemoved);
			report.add("Found " + Integer.toString(sourceDrugs.size()) + " source drugs with a miminum use count of " + Long.toString(minimumUseCount));
			report.add("Found " + noATCCounter + " source drugs without ATC (" + Long.toString(Math.round(((double) noATCCounter / (double) sourceDrugs.size()) * 100)) + "%)");
			report.add("Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	private boolean getUnitConversion(CDMDatabase database) {
		boolean ok = true;
		
		// Create Units Map
		unitConversionsMap = new UnitConversion(database, units);
		if (unitConversionsMap.getStatus() != UnitConversion.STATE_OK) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the unit conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "" + UnitConversion.FILENAME);
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getFormConversion(CDMDatabase database) {
		boolean ok = true;
		
		formConversionsMap = new FormConversion(database, forms);
		if (formConversionsMap.getStatus() != FormConversion.STATE_OK) {
			// If no form conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the form conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "" + FormConversion.FILENAME);
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getCDMData(CDMDatabase database) {
		boolean ok = true;
		String fileName = "";
		
		QueryParameters queryParameters = new QueryParameters();
		queryParameters.set("@vocab", database.getVocabSchema());
	
		// Connect to the database
		RichConnection connection = database.getRichConnection(this.getClass());
		
		// Load CDM ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Ingredients ...");
		
		PrintWriter rxNormIngredientsFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "DrugMapping RxNorm Ingredients.csv";
			rxNormIngredientsFile = new PrintWriter(new File(fileName));
			rxNormIngredientsFile.println(CDMIngredient.getHeaderWithSynonyms());
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			rxNormIngredientsFile = null;
		}
		
		// Get RxNorm ingredients
		CDMIngredient lastCdmIngredient = null;
		for (Row queryRow : connection.queryResource("../cdm/GetRxNormIngredients.sql", queryParameters)) {
			String cdmIngredientConceptId = queryRow.get("concept_id").trim();
			if ((lastCdmIngredient == null) || (!lastCdmIngredient.getConceptId().equals(cdmIngredientConceptId))) {
				if ((rxNormIngredientsFile != null) && (lastCdmIngredient != null)) {
					rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
				}
				CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
				if (cdmIngredient == null) {
					cdmIngredient = new CDMIngredient(queryRow, "");
					cdmIngredients.put(cdmIngredient.getConceptId(), cdmIngredient);
					cdmIngredientsList.add(cdmIngredient);
					//String cdmIngredientNameNoSpaces = cdmIngredient.getConceptNameNoSpaces();
					String cdmIngredientNameModified = modifyName(cdmIngredient.getConceptName());
					
					List<String> nameList = new ArrayList<String>();
					nameList.add(cdmIngredient.getConceptName());
					nameList.add(modifyName(cdmIngredientNameModified));
					//nameList.add(cdmIngredientNameNoSpaces);
					
					for (String name : nameList) {
						Set<CDMIngredient> existingCDMIngredients = cdmIngredientNameIndex.get(name);
						if (existingCDMIngredients == null) {
							existingCDMIngredients = new HashSet<CDMIngredient>();
							cdmIngredientNameIndex.put(name, existingCDMIngredients);
						}
						existingCDMIngredients.add(cdmIngredient);
					}
				}
				lastCdmIngredient = cdmIngredient;
			}
			
			String cdmIngredientSynonym = queryRow.get("concept_synonym_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			//String cdmIngredientSynonymNoSpaces = cdmIngredientSynonym.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");

			List<String> nameList = new ArrayList<String>();
			nameList.add(cdmIngredientSynonym);
			nameList.add(modifyName(cdmIngredientSynonym));
			//nameList.add(cdmIngredientSynonymNoSpaces);
			
			for (String name : nameList) {
				Set<CDMIngredient> nameIngredients = cdmIngredientNameIndex.get(name); 
				if (nameIngredients == null) {
					nameIngredients = new HashSet<CDMIngredient>();
					cdmIngredientNameIndex.put(name, nameIngredients);
				}
				nameIngredients.add(lastCdmIngredient);
			}
			
			if ((cdmIngredientSynonym != null) && (!cdmIngredientSynonym.equals(""))) {
				lastCdmIngredient.addSynonym(cdmIngredientSynonym);
			}
		}
		if ((rxNormIngredientsFile != null) && (lastCdmIngredient != null)) {
			rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
		}
		
		if (rxNormIngredientsFile != null) {
			rxNormIngredientsFile.close();
		}
		
		report.add("Used RxNorm Ingredients found: " + Integer.toString(cdmIngredients.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		// Get "Maps to" RxNorm Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM 'Maps to' RxNorm Ingredients ...");
		
		for (Row queryRow : connection.queryResource("../cdm/GetMapsToRxNormIngredients.sql", queryParameters)) {
			String drugName = queryRow.get("drug_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			//String drugNameNoSpaces = drugName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			String cdmIngredientConceptId = queryRow.get("mapsto_concept_id").trim();
			String drugNameSynonym = queryRow.get("drug_synonym_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			//String drugNameSynonymNoSpaces = drugNameSynonym.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			
			if (cdmIngredient != null) {
				List<String> nameList = new ArrayList<String>();
				nameList.add(drugName);
				nameList.add(modifyName(drugName));
				nameList.add(modifyName(drugNameSynonym));
				//nameList.add(drugNameNoSpaces);
				//nameList.add(drugNameSynonymNoSpaces);
				
				for (String name : nameList) {
					Set<CDMIngredient> nameIngredients = cdmMapsToIngredientNameIndex.get(name); 
					if (nameIngredients == null) {
						nameIngredients = new HashSet<CDMIngredient>();
						cdmMapsToIngredientNameIndex.put(name, nameIngredients);
					}
					nameIngredients.add(cdmIngredient);
				}
			}
		}
		
		// Write 'Maps to' records to file 
		List<String> drugNames = new ArrayList<String>();
		drugNames.addAll(mapsToCDMIngredient.keySet());
		Collections.sort(drugNames);
		for (String drugName : drugNames) {
			List<CDMIngredient> cdmIngredients = new ArrayList<CDMIngredient>();
			cdmIngredients.addAll(mapsToCDMIngredient.get(drugName));
			Collections.sort(cdmIngredients, new Comparator<CDMIngredient>() {
				@Override
				public int compare(CDMIngredient ingredient1, CDMIngredient ingredient2) {
					return ingredient1.getConceptName().compareTo(ingredient2.getConceptName());
				}
			});
			
			List<String> synonyms = new ArrayList<String>();
			synonyms.addAll(drugNameSynonyms.get(drugName));
			Collections.sort(synonyms);
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Get RxNorm Clinical Drugs with Form and Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM concepts replaced by RxNorm ingredients ...");
		
		for (Row queryRow : connection.queryResource("../cdm/GetConceptReplacedByRxNormIngredient.sql", queryParameters)) {
			String cdmReplacedByName      = queryRow.get("concept_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String cdmReplacedByConceptId = queryRow.get("concept_id");
			
			//String cdmReplacedNameNoSpaces = cdmReplacedByName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			String cdmReplacedNameModified = modifyName(cdmReplacedByName);
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmReplacedByConceptId);
			if (cdmIngredient != null) {
				List<String> nameList = new ArrayList<String>();
				nameList.add(cdmReplacedByName);
				nameList.add(cdmReplacedNameModified);
				//nameList.add(cdmReplacedNameNoSpaces);
				
				for (String name : nameList) {
					Set<CDMIngredient> nameIngredients = cdmReplacedByIngredientNameIndex.get(name); 
					if (nameIngredients == null) {
						nameIngredients = new HashSet<CDMIngredient>();
						cdmReplacedByIngredientNameIndex.put(name, nameIngredients);
					}
					nameIngredients.add(cdmIngredient);
				}
			} 
		}
		
		for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
			Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);

			for (String ingredientName : ingredientNameIndex.keySet()) {
				Set<CDMIngredient> ingredientNameIngredients = ingredientNameIndex.get(ingredientName);
				if ((ingredientNameIngredients != null) && (ingredientNameIngredients.size() > 1)) {
					System.out.println("      WARNING: Multiple ingredients found for name '" + ingredientName + "' in " + ingredientNameIndexName + " index");
					for (CDMIngredient cdmIngredient : ingredientNameIngredients) {
						String ingredientDescription = cdmIngredient.toString();
						boolean firstSynonym = true;
						for (String synonym : cdmIngredient.getSynonyms()) {
							if (firstSynonym) {
								ingredientDescription += "  Synonyms: ";
								firstSynonym = false;	
							}
							else {
								ingredientDescription += ", ";
							}
							ingredientDescription += "'" + synonym + "'";
						}
						System.out.println("                   " + ingredientDescription);
					}
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Get RxNorm Clinical Drugs with Form and Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drugs with ingredients ...");
		
		for (Row queryRow : connection.queryResource("../cdm/GetRxNormClinicalDrugsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drug_concept_id");
			
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrug = cdmDrugs.get(cdmDrugConceptId);
				if (cdmDrug == null) {
					cdmDrug = new CDMDrug(queryRow, "drug_");
					cdmDrugs.put(cdmDrug.getConceptId(), cdmDrug);
				}
				String cdmFormConceptId = queryRow.get("form_concept_id");
				cdmDrug.addForm(cdmFormConceptId);
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id");
				if ((cdmIngredientConceptId != null) && (!cdmIngredientConceptId.equals(""))) {
					CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
					if (cdmIngredient != null) {
						CDMIngredientStrength cdmIngredientStrength = new CDMIngredientStrength(queryRow, "", cdmIngredient);
						cdmDrug.addIngredientStrength(cdmIngredientStrength);
						
						List<CDMDrug> drugsContainingIngredient = cdmDrugsContainingIngredient.get(cdmIngredient);
						if (drugsContainingIngredient == null) {
							drugsContainingIngredient = new ArrayList<CDMDrug>();
							cdmDrugsContainingIngredient.put(cdmIngredient, drugsContainingIngredient);
						}
						if (!drugsContainingIngredient.contains(cdmDrug)) {
							drugsContainingIngredient.add(cdmDrug);
						}
					}
				}
			} 
		}

		report.add("RxNorm Clinical Drugs found: " + Integer.toString(cdmDrugs.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Get RxNorm Clinical Drugs with Form and Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Comps with ingredients ...");
		
		for (Row queryRow : connection.queryResource("../cdm/GetRxNormClinicalDrugCompsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drugcomp_concept_id");
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrugComp = cdmDrugComps.get(cdmDrugConceptId);
				if (cdmDrugComp == null) {
					cdmDrugComp = new CDMDrug(queryRow, "drugcomp_");
					cdmDrugComps.put(cdmDrugComp.getConceptId(), cdmDrugComp);
				}
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id");
				if ((cdmIngredientConceptId != null) && (!cdmIngredientConceptId.equals(""))) {
					CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
					if (cdmIngredient != null) {
						CDMIngredientStrength cdmIngredientStrength = new CDMIngredientStrength(queryRow, "", cdmIngredient);
						cdmDrugComp.addIngredientStrength(cdmIngredientStrength);
						
						List<CDMDrug> drugsContainingIngredient = cdmDrugCompsContainingIngredient.get(cdmIngredient);
						if (drugsContainingIngredient == null) {
							drugsContainingIngredient = new ArrayList<CDMDrug>();
							cdmDrugCompsContainingIngredient.put(cdmIngredient, drugsContainingIngredient);
						}
						drugsContainingIngredient.add(cdmDrugComp);
					}
				}
			} 
		}

		report.add("RxNorm Clinical Drug Comps found: " + Integer.toString(cdmDrugComps.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Get RxNorm Clinical Drugs with Form and Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Forms with ingredients ...");
		
		for (Row queryRow : connection.queryResource("../cdm/GetRxNormClinicalDrugFormsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drugform_concept_id");
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrugForm = cdmDrugForms.get(cdmDrugConceptId);
				if (cdmDrugForm == null) {
					cdmDrugForm = new CDMDrug(queryRow, "drugform_");
					cdmDrugForms.put(cdmDrugForm.getConceptId(), cdmDrugForm);
				}
				
				String cdmFormConceptId = queryRow.get("form_concept_id");
				if ((cdmFormConceptId != null) && (!cdmFormConceptId.equals(""))) {
					cdmDrugForm.addForm(cdmFormConceptId);
				}
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id");
				if ((cdmIngredientConceptId != null) && (!cdmIngredientConceptId.equals(""))) {
					CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
					if (cdmIngredient != null) {
						CDMIngredientStrength cdmIngredientStrength = new CDMIngredientStrength(queryRow, "", cdmIngredient);
						cdmDrugForm.addIngredientStrength(cdmIngredientStrength);
						
						List<CDMDrug> drugsContainingIngredient = cdmDrugFormsContainingIngredient.get(cdmIngredient);
						if (drugsContainingIngredient == null) {
							drugsContainingIngredient = new ArrayList<CDMDrug>();
							cdmDrugFormsContainingIngredient.put(cdmIngredient, drugsContainingIngredient);
						}
						drugsContainingIngredient.add(cdmDrugForm);
					}
				}
			} 
		}

		report.add("RxNorm Clinical Drug Forms found: " + Integer.toString(cdmDrugForms.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		PrintWriter cdmRxNormIngredientsNameIndexFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "DrugMapping CDM RxNorm Ingredients Name Index.csv";
			cdmRxNormIngredientsNameIndexFile = new PrintWriter(new File(fileName));
			cdmRxNormIngredientsNameIndexFile.println("Name," + CDMIngredient.getHeader());
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			cdmRxNormIngredientsNameIndexFile = null;
		}

		
		if (cdmRxNormIngredientsNameIndexFile != null) {
			for (String name : cdmIngredientNameIndex.keySet()) {
				Set<CDMIngredient> cdmIngredientsByName = cdmIngredientNameIndex.get(name);
				for (CDMIngredient cdmIngredientByName : cdmIngredientsByName) {
					String record = "\"" + name + "\"";
					record += "," + cdmIngredientByName.toString();
					cdmRxNormIngredientsNameIndexFile.println(record);
				}
			}
		}
		
		cdmRxNormIngredientsNameIndexFile.close();
		
		
		// Get CDM RxNorm Ingredient ATCs
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Ingredient ATCs ...");
		
		for (Row queryRow : connection.queryResource("../cdm/GetRxNormIngredientATC.sql", queryParameters)) {
			String cdmIngredientConceptId = queryRow.get("concept_id");
			String cdmIngredientATC = queryRow.get("atc");
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			if (cdmIngredient != null) {
				cdmIngredient.setATC(cdmIngredientATC);
				Set<CDMIngredient> cdmATCIngredients = cdmATCIngredientMap.get(cdmIngredientATC);
				if (cdmATCIngredients == null) {
					cdmATCIngredients = new HashSet<CDMIngredient>();
					cdmATCIngredientMap.put(cdmIngredientATC, cdmATCIngredients);
				}
				cdmATCIngredients.add(cdmIngredient);
				
			}
		}

		Integer atcCount = 0;
		for (String cdmIngredientConceptId : cdmIngredients.keySet()) {
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			if (cdmIngredient.getATC() != null) {
				atcCount++;
			}
		}
		
		report.add("Found " + atcCount + " CDM Ingredients with ATC (" + Long.toString(Math.round(((double) atcCount / (double) cdmIngredients.size()) * 100)) + "%)");
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		// Close database connection
		connection.close();
		
		return ok;
	}
	
	
	private boolean getManualMappings(InputFile manualMappingFile) {
		boolean ok = true;
		
		if (manualMappingFile.openFile(true)) {
			System.out.println(DrugMapping.getCurrentTime() + "     Loading manual mappings ...");
			
			while (manualMappingFile.hasNext()) {
				Row row = manualMappingFile.next();
				
				String sourceCode = manualMappingFile.get(row, "SourceCode").trim();
				String cdmConceptId = manualMappingFile.get(row, "concept_id").trim();
				
				SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
				CDMDrug cdmDrug = cdmDrugs.get(cdmConceptId);
				if (cdmDrug == null) {
					cdmDrug = cdmDrugComps.get(cdmConceptId);
				}
				if (cdmDrug == null) {
					cdmDrug = cdmDrugForms.get(cdmConceptId);
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
					CDMDrug mappedCDMDrug = manualMappings.get(sourceDrug);
					if (mappedCDMDrug != null) {
						System.out.println("    ERROR: SourceDrug " + sourceDrug + " is already mapped to " + mappedCDMDrug);
						ok = false;
					}
				}
				
				if (ok) {
					manualMappings.put(sourceDrug, cdmDrug);
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No manual mappings found");
		}
		
		return ok;
	}
	
	
	private boolean getCASNames(InputFile casFile) {
		boolean ok = true;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Loading CAS names ...");
		
		if (casFile.fileExists()) {
			if (casFile.openFile()) {
				while (casFile.hasNext()) {
					Row row = casFile.next();
					
					String casNumber = casFile.get(row, "CASNumber").trim();
					String chemicalName = casFile.get(row, "ChemicalName").replaceAll("\n", " ").replaceAll("\r", " ").toUpperCase().trim();
					String synonyms = casFile.get(row, "Synonyms").replaceAll("\n", " ").replaceAll("\r", " ").toUpperCase().trim();
					String[] synonymSplit = synonyms.split("[|]");
					
					if (!casNumber.equals("")) {
						casNumber = uniformCASNumber(casNumber);
						List<String> casNames = new ArrayList<String>();
						
						casNames.add(chemicalName);

						for (String synonym : synonymSplit) {
							if (!casNames.contains(synonym)) {
								casNames.add(synonym);
							}
						}
						
						String modifiedName = modifyName(chemicalName);
						if (!casNames.contains(modifiedName)) {
							casNames.add(modifiedName);
						}

						for (String synonym : synonymSplit) {
							modifiedName = modifyName(synonym);
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
								casNames.add(modifyName(synonym));
							}
						}
						
						String chemicalNameNoSpaces = chemicalName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", ""); 
						if (!chemicalNameNoSpaces.equals("")) {
							casNames.add(chemicalNameNoSpaces);
							casNames.add(modifyName(chemicalName));
						}
						*/
						casMap.put(casNumber, casNames);
					}
				}
			}
			else {
				ok = false;
			}
		}
		else {
			System.out.println("         No CAS File found.");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchIngredients() {
		boolean ok = true;
		Integer multipleMappings = 0;
		
		multipleMappings += matchIngredientsByCASNumber();
		
		multipleMappings += matchIngredientsByName();
		
		multipleMappings += matchIngredientsByATC();

		report.add("Source ingredients mapped total : " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		report.add("Multiple mappings found: " + multipleMappings + " (" + Long.toString(Math.round(((double) multipleMappings / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		
		return ok;
	}
	
	
	private Integer matchIngredientsByATC() {
		Integer matchedByATC = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by ATC ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			List<SourceIngredient> sourceIngredients = sourceDrug.getIngredients();
			if ((sourceDrug.getATCCode() != null) && (sourceIngredients.size() == 1)) {
				SourceIngredient sourceIngredient = sourceIngredients.get(0);
				if (sourceIngredient.getMatchingIngredient() == null) {
					Set<CDMIngredient> cdmATCIngredients = cdmATCIngredientMap.get(sourceDrug.getATCCode());
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

		report.add("Source ingredients mapped by ATC: " + matchedByATC + " (" + Long.toString(Math.round(((double) matchedByATC / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return 0;
	}
	
	
	private Integer matchIngredientsByName() {
		Integer matchedByName = 0;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by name ...");
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			if (sourceIngredient.getMatchingIngredient() == null) {

				List<String> matchNameList = sourceIngredient.getIngredientMatchingNames();

				boolean matchFound = false;
				boolean multipleMapping = false;
				
				for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
					Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);
					
					for (String matchName : matchNameList) {
						String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
						matchName = matchName.substring(matchName.indexOf(": ") + 2);
						
						Set<CDMIngredient> matchedCDMIngredients = ingredientNameIndex.get(matchName);
						if (matchedCDMIngredients != null) {
							if (matchedCDMIngredients.size() == 1) {
								CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
								ingredientMap.put(sourceIngredient, cdmIngredient);
								sourceIngredient.setMatchingIngredient(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
								sourceIngredient.setMatchString(matchType + ingredientNameIndexName + " " + matchName);
								matchFound = true;
								matchedByName++;
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

		report.add("Source ingredients mapped by name: " + matchedByName + " (" + Long.toString(Math.round(((double) matchedByName / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return multipleMappings;
	}
	
	
	private Integer matchIngredientsByCASNumber() {
		Integer matchedByCASName = 0;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by CAS number ...");
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			if (sourceIngredient.getMatchingIngredient() == null) {

				boolean matchFound = false;
				boolean multipleMapping = false;
				
				for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
					Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);
					String casNumber = sourceIngredient.getCASNumber();
					if (casNumber != null) {
						
						List<String> casNames = casMap.get(casNumber);
						if (casNames != null) {
							
							for (String casName : casNames) {
								
								Set<CDMIngredient> matchedCDMIngredients = ingredientNameIndex.get(casName);
								if (matchedCDMIngredients != null) {
									if (matchedCDMIngredients.size() == 1) {
										CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
										ingredientMap.put(sourceIngredient, cdmIngredient);
										sourceIngredient.setMatchingIngredient(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
										sourceIngredient.setMatchString("CAS: " + ingredientNameIndexName + " " + casName);
										matchFound = true;
										matchedByCASName++;
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

		report.add("Source ingredients mapped by CAS number: " + matchedByCASName + " (" + Long.toString(Math.round(((double) matchedByCASName / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return multipleMappings;
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
						while (mappingTypeDescriptions.containsKey(mapping)) {
							Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							Map<Integer, List<String>> mappingTypeResults = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResults == null) {
								mappingTypeResults = new HashMap<Integer, List<String>>();
								sourceDrugMappingResult.put(mapping, mappingTypeResults);
							}
							
							List<String> mappingSourceIngredients = mappingTypeResults.get((mapping * 100) + 2); // <mapping>DOUBLE_INGREDIENT_MAPPING
							if (mappingSourceIngredients == null) {
								mappingSourceIngredients = new ArrayList<String>();
								mappingTypeResults.put((mapping * 100) + 2, mappingSourceIngredients); // <mapping>DOUBLE_INGREDIENT_MAPPING
							}
							for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
								CDMIngredient cdmIngredient = ingredientMap.get(sourceDrugIngredient);
								String description = sourceDrugIngredient.toString();
								description += " -> " + (cdmIngredient == null ? "" : cdmIngredient.toString());
								mappingSourceIngredients.add(description);
							}
							
							mapping++;
						}
					}
				}
				else {
					int mapping = 0;
					while (mappingTypeDescriptions.containsKey(mapping)) {
						Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						Map<Integer, List<String>> mappingTypeResults = sourceDrugMappingResult.get(mapping);
						if (mappingTypeResults == null) {
							mappingTypeResults = new HashMap<Integer, List<String>>();
							sourceDrugMappingResult.put(mapping, mappingTypeResults);
						}
						
						List<String> mappingSourceIngredients = mappingTypeResults.get((mapping * 100) + 1); // <mapping>UNMAPPED_SOURCE_INGREDIENTS
						if (mappingSourceIngredients == null) {
							mappingSourceIngredients = new ArrayList<String>();
							mappingTypeResults.put((mapping * 100) + 1, mappingSourceIngredients); // <mapping>UNMAPPED_SOURCE_INGREDIENTS
						}
						for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
							CDMIngredient cdmIngredient = ingredientMap.get(sourceDrugIngredient);
							String description = sourceDrugIngredient.toString();
							description += " -> " + (cdmIngredient == null ? "" : cdmIngredient.toString());
							mappingSourceIngredients.add(description);
						}
						
						mapping++;
					}
				}
			}
			else {
				int mapping = 0;
				while (mappingTypeDescriptions.containsKey(mapping)) {
					Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
					if (sourceDrugMappingResult == null) {
						sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
						sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
					}
					Map<Integer, List<String>> mappingTypeResults = sourceDrugMappingResult.get(mapping);
					if (mappingTypeResults == null) {
						mappingTypeResults = new HashMap<Integer, List<String>>();
						sourceDrugMappingResult.put(mapping, mappingTypeResults);
					}
					
					List<String> mappingSourceIngredients = mappingTypeResults.get((mapping * 100) + 0); // <mapping>NO_SOURCE_INGREDIENTS
					if (mappingSourceIngredients == null) {
						mappingSourceIngredients = new ArrayList<String>();
						mappingSourceIngredients.add(" ");
						mappingTypeResults.put((mapping * 100) + 0, mappingSourceIngredients); // <mapping>NO_SOURCE_INGREDIENTS
					}
					
					mapping++;
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		report.add("Source drugs with all ingredients mapped: " + Integer.toString(sourceDrugsAllIngredientsMapped.size()) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) sourceDrugsAllIngredientsMapped.size() / (double) sourceDrugs.size()) * 100)) + "%)");
		report.add("");
		
		return (sourceDrugsAllIngredientsMapped.size() > 0);
	}
	
	
	private boolean matchClinicalDrugs() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drugs ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			CDMDrug automaticMapping = null;
			
			if (sourceDrugsAllIngredientsMapped.contains(sourceDrug)) {
				List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);
				
				if (sourceDrugCDMIngredients.size() > 0) {
					// Find CDM Clinical Drugs with corresponding ingredients
					List<CDMDrug> cdmDrugsWithIngredients = null;
					for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
						List<CDMDrug> cdmDrugsWithIngredient = cdmDrugsContainingIngredient.get(cdmIngredient);
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
								Set<String> cdmDrugForms = cdmDrug.getForms();
								boolean formFound = false;
								for (String cdmDrugForm : cdmDrugForms) {
									if (formConversionsMap.matches(sourceDrugForm, cdmDrugForm)) {
										formFound = true;
										break;
									}
								}
								if (!formFound) {
									cdmDrugsMissingForm.add(cdmDrug);
								}
							}

							Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
							if (rejectedForMapping == null) {
								rejectedForMapping = new HashMap<Integer, List<String>>();
								sourceDrugMappingResult.put(mapping, rejectedForMapping);
							}
							
							List<String> rejectedForms = rejectedForMapping.get(CLINICAL_DRUG_MAPPING_REJECTED_BY_FORM);
							if (rejectedForms == null) {
								rejectedForms = new ArrayList<String>();
								rejectedForMapping.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_FORM, rejectedForms);
							}
							for (CDMDrug cdmDrug : cdmDrugsMissingForm) {
								String formsDescription = "";
								for (String cdmDrugFormConceptId : cdmDrug.getForms()) {
									if (!formsDescription.equals("")) {
										formsDescription += " | ";
									}
									formsDescription += formConversionsMap.getCDMFormConceptName(cdmDrugFormConceptId) + " (" + cdmDrugFormConceptId + ")";
								}
								String cdmDrugDescription = cdmDrug.toString() + ": " + formsDescription;
								if (!rejectedForms.contains(cdmDrugDescription)) {
									rejectedForms.add(cdmDrugDescription);
								}
							}
							
							cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
						}
						
						// Find CDM Clinical Drugs with corresponding ingredient strengths
						if (cdmDrugsWithIngredients.size() > 0) {
							List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
							List<String> rejectedDrugs = new ArrayList<String>();
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
								rejectedDrugs = new ArrayList<String>();
								
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
										List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), strengthDeviationPercentage);
										if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
											matchingCDMDrugs.add(cdmDrug);
											matchingIngredientsMap.put(cdmDrug, matchingIngredients);
										}
										else {
											rejectedDrugs.add(cdmDrug.toString() + ": " + cdmDrug.getStrengthDescription());
										}
									}
								}
								
								if ((matchingCDMDrugs != null) && (matchingCDMDrugs.size() > 0)) {
									break;
								}
							}

							Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
							if (rejectedForMapping == null) {
								rejectedForMapping = new HashMap<Integer, List<String>>();
								sourceDrugMappingResult.put(mapping, rejectedForMapping);
							}
							rejectedForMapping.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_STRENGTH, rejectedDrugs);
							
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
									String vocabulary_id = null;
									int resultType = -1;
									if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
										vocabulary_id = "RxNorm";
										resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_PREFERENCE;
									}
									else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
										vocabulary_id = "RxNorm Extension";
										resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
									}
									if (vocabulary_id != null) {
										List<CDMDrug> remove = new ArrayList<CDMDrug>();
										for (CDMDrug cdmDrug : matchingCDMDrugsWithTwoUnits) {
											if (!cdmDrug.getVocabularyId().equals(vocabulary_id)) {
												remove.add(cdmDrug);
											}
										}
										if ((remove.size() > 0) && (matchingCDMDrugsWithTwoUnits.size() != remove.size())) {
											matchingCDMDrugsWithTwoUnits.removeAll(remove);
											
											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											rejectedForMapping = sourceDrugMappingResult.get(mapping);
											if (rejectedForMapping == null) {
												rejectedForMapping = new HashMap<Integer, List<String>>();
												sourceDrugMappingResult.put(mapping, rejectedForMapping);
											}
											
											List<String> multipleMappings = rejectedForMapping.get(resultType);
											if (multipleMappings == null) {
												multipleMappings = new ArrayList<String>();
												rejectedForMapping.put(resultType, multipleMappings);
											}
											for (CDMDrug cdmDrug : remove) {
												String cdmDrugDescription = cdmDrug.toString();
												if (!multipleMappings.contains(cdmDrugDescription)) {
													multipleMappings.add(cdmDrugDescription);
												}
											}
										}
									}
									if ((matchingCDMDrugsWithTwoUnits.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No"))) {
										boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
										resultType = latest ? CLINICAL_DRUG_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE : CLINICAL_DRUG_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE;
										
										List<CDMDrug> remove = new ArrayList<CDMDrug>();
										List<CDMDrug> lastDrugs = new ArrayList<CDMDrug>();
										int lastDate = -1;
										for (CDMDrug cdmDrug : matchingCDMDrugsWithTwoUnits) {
											try {
												Integer date = Integer.parseInt(cdmDrug.getValidStartDate().replaceAll("-",""));
												if (lastDate == -1) {
													lastDrugs.add(cdmDrug);
													lastDate = date;
												}
												else {
													if (latest ? (date > lastDate) : (date < lastDate)) {
														remove.addAll(lastDrugs);
														lastDrugs.clear();
														lastDrugs.add(cdmDrug);
														lastDate = date;
													}
													else if (date == lastDate) {
														lastDrugs.add(cdmDrug);
													}
													else {
														remove.add(cdmDrug);
													}
												}
											}
											catch (NumberFormatException e) {
												remove.add(cdmDrug);
											}
										}
										if ((remove.size() > 0) && (matchingCDMDrugsWithTwoUnits.size() != remove.size())) {
											matchingCDMDrugsWithTwoUnits.removeAll(remove);
											
											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											rejectedForMapping = sourceDrugMappingResult.get(mapping);
											if (rejectedForMapping == null) {
												rejectedForMapping = new HashMap<Integer, List<String>>();
												sourceDrugMappingResult.put(mapping, rejectedForMapping);
											}
											
											List<String> multipleMappings = rejectedForMapping.get(resultType);
											if (multipleMappings == null) {
												multipleMappings = new ArrayList<String>();
												rejectedForMapping.put(resultType, multipleMappings);
											}
											for (CDMDrug cdmDrug : remove) {
												String cdmDrugDescription = cdmDrug.toString();
												if (!multipleMappings.contains(cdmDrugDescription)) {
													multipleMappings.add(cdmDrugDescription);
												}
											}
										}
									}
									if ((matchingCDMDrugsWithTwoUnits.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None"))) {
										resultType = -1;
										List<CDMDrug> remove = new ArrayList<CDMDrug>();
										if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First")) {
											automaticMapping = matchingCDMDrugsWithTwoUnits.get(0);
											resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_FIRST_PREFERENCE;
											for (int nr = 1; nr < matchingCDMDrugsWithTwoUnits.size(); nr++) {
												remove.add(matchingCDMDrugsWithTwoUnits.get(nr));
											}
										}
										else {
											automaticMapping = matchingCDMDrugsWithTwoUnits.get(matchingCDMDrugsWithTwoUnits.size() - 1);
											resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_LAST_PREFERENCE;
											for (int nr = 0; nr < (matchingCDMDrugsWithTwoUnits.size() - 1); nr++) {
												remove.add(matchingCDMDrugsWithTwoUnits.get(nr));
											}
										}
										matchingCDMDrugsWithTwoUnits.removeAll(remove);

										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										rejectedForMapping = sourceDrugMappingResult.get(mapping);
										if (rejectedForMapping == null) {
											rejectedForMapping = new HashMap<Integer, List<String>>();
											sourceDrugMappingResult.put(mapping, rejectedForMapping);
										}
										
										List<String> multipleMappings = rejectedForMapping.get(resultType);
										if (multipleMappings == null) {
											multipleMappings = new ArrayList<String>();
											rejectedForMapping.put(resultType, multipleMappings);
										}
										for (CDMDrug cdmDrug : remove) {
											String cdmDrugDescription = cdmDrug.toString();
											if (!multipleMappings.contains(cdmDrugDescription)) {
												multipleMappings.add(cdmDrugDescription);
											}
										}
									}
								}
								if (matchingCDMDrugsWithTwoUnits.size() > 0) {
									if (matchingCDMDrugsWithTwoUnits.size() == 1) {
										automaticMapping = matchingCDMDrugsWithTwoUnits.get(0);
									}
									else {
										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										rejectedForMapping = sourceDrugMappingResult.get(mapping);
										if (rejectedForMapping == null) {
											rejectedForMapping = new HashMap<Integer, List<String>>();
											sourceDrugMappingResult.put(mapping, rejectedForMapping);
										}
										
										List<String> multipleMappings = rejectedForMapping.get(CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING);
										if (multipleMappings == null) {
											multipleMappings = new ArrayList<String>();
											rejectedForMapping.put(CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
										}
										for (CDMDrug cdmDrug : matchingCDMDrugsWithTwoUnits) {
											String cdmDrugDescription = cdmDrug.toString();
											if (!multipleMappings.contains(cdmDrugDescription)) {
												multipleMappings.add(cdmDrugDescription);
											}
										}
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
								}
								else {
									if (matchingCDMDrugsWithOneUnit.size() > 1) {
										String vocabulary_id = null;
										int resultType = -1;
										if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
											vocabulary_id = "RxNorm";
											resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_PREFERENCE;
										}
										else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
											vocabulary_id = "RxNorm Extension";
											resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
										}
										if (vocabulary_id != null) {
											List<CDMDrug> remove = new ArrayList<CDMDrug>();
											for (CDMDrug cdmDrug : matchingCDMDrugsWithOneUnit) {
												if (!cdmDrug.getVocabularyId().equals(vocabulary_id)) {
													remove.add(cdmDrug);
												}
											}
											if ((remove.size() > 0) && (matchingCDMDrugsWithOneUnit.size() != remove.size())) {
												matchingCDMDrugsWithOneUnit.removeAll(remove);
												
												sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
												if (sourceDrugMappingResult == null) {
													sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
													sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
												}
												rejectedForMapping = sourceDrugMappingResult.get(mapping);
												if (rejectedForMapping == null) {
													rejectedForMapping = new HashMap<Integer, List<String>>();
													sourceDrugMappingResult.put(mapping, rejectedForMapping);
												}
												
												List<String> multipleMappings = rejectedForMapping.get(resultType);
												if (multipleMappings == null) {
													multipleMappings = new ArrayList<String>();
													rejectedForMapping.put(resultType, multipleMappings);
												}
												for (CDMDrug cdmDrug : remove) {
													String cdmDrugDescription = cdmDrug.toString();
													if (!multipleMappings.contains(cdmDrugDescription)) {
														multipleMappings.add(cdmDrugDescription);
													}
												}
											}
										}
										if ((matchingCDMDrugsWithOneUnit.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No"))) {
											boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
											resultType = latest ? CLINICAL_DRUG_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE : CLINICAL_DRUG_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE;
											List<CDMDrug> remove = new ArrayList<CDMDrug>();
											List<CDMDrug> lastDrugs = new ArrayList<CDMDrug>();
											int lastDate = -1;
											for (CDMDrug cdmDrug : matchingCDMDrugsWithOneUnit) {
												try {
													Integer date = Integer.parseInt(cdmDrug.getValidStartDate().replaceAll("-",""));
													if (lastDate == -1) {
														lastDrugs.add(cdmDrug);
														lastDate = date;
													}
													else {
														if (latest ? (date > lastDate) : (date < lastDate)) {
															remove.addAll(lastDrugs);
															lastDrugs.clear();
															lastDrugs.add(cdmDrug);
															lastDate = date;
														}
														else if (date == lastDate) {
															lastDrugs.add(cdmDrug);
														}
														else {
															remove.add(cdmDrug);
														}
													}
												}
												catch (NumberFormatException e) {
													remove.add(cdmDrug);
												}
											}
											if ((remove.size() > 0) && (matchingCDMDrugsWithOneUnit.size() != remove.size())) {
												matchingCDMDrugsWithOneUnit.removeAll(remove);
												
												sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
												if (sourceDrugMappingResult == null) {
													sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
													sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
												}
												rejectedForMapping = sourceDrugMappingResult.get(mapping);
												if (rejectedForMapping == null) {
													rejectedForMapping = new HashMap<Integer, List<String>>();
													sourceDrugMappingResult.put(mapping, rejectedForMapping);
												}
												
												List<String> multipleMappings = rejectedForMapping.get(resultType);
												if (multipleMappings == null) {
													multipleMappings = new ArrayList<String>();
													rejectedForMapping.put(resultType, multipleMappings);
												}
												for (CDMDrug cdmDrug : remove) {
													String cdmDrugDescription = cdmDrug.toString();
													if (!multipleMappings.contains(cdmDrugDescription)) {
														multipleMappings.add(cdmDrugDescription);
													}
												}
											}
										}
										if ((matchingCDMDrugsWithOneUnit.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None"))) {
											resultType = -1;
											List<CDMDrug> remove = new ArrayList<CDMDrug>();
											if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First")) {
												automaticMapping = matchingCDMDrugsWithOneUnit.get(0);
												resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_FIRST_PREFERENCE;
												for (int nr = 1; nr < matchingCDMDrugsWithOneUnit.size(); nr++) {
													remove.add(matchingCDMDrugsWithOneUnit.get(nr));
												}
											}
											else {
												automaticMapping = matchingCDMDrugsWithOneUnit.get(matchingCDMDrugsWithOneUnit.size() - 1);
												resultType = CLINICAL_DRUG_MAPPING_REJECTED_BY_LAST_PREFERENCE;
												for (int nr = 0; nr < (matchingCDMDrugsWithOneUnit.size() - 1); nr++) {
													remove.add(matchingCDMDrugsWithOneUnit.get(nr));
												}
											}
											matchingCDMDrugsWithOneUnit.removeAll(remove);

											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											rejectedForMapping = sourceDrugMappingResult.get(mapping);
											if (rejectedForMapping == null) {
												rejectedForMapping = new HashMap<Integer, List<String>>();
												sourceDrugMappingResult.put(mapping, rejectedForMapping);
											}
											
											List<String> multipleMappings = rejectedForMapping.get(resultType);
											if (multipleMappings == null) {
												multipleMappings = new ArrayList<String>();
												rejectedForMapping.put(resultType, multipleMappings);
											}
											for (CDMDrug cdmDrug : remove) {
												String cdmDrugDescription = cdmDrug.toString();
												if (!multipleMappings.contains(cdmDrugDescription)) {
													multipleMappings.add(cdmDrugDescription);
												}
											}
										}
									}

									if (matchingCDMDrugsWithOneUnit.size() > 0) {
										if (matchingCDMDrugsWithOneUnit.size() == 1) {
											automaticMapping = matchingCDMDrugsWithOneUnit.get(0);
										}
										else {
											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											rejectedForMapping = sourceDrugMappingResult.get(mapping);
											if (rejectedForMapping == null) {
												rejectedForMapping = new HashMap<Integer, List<String>>();
												sourceDrugMappingResult.put(mapping, rejectedForMapping);
											}
											
											List<String> multipleMappings = rejectedForMapping.get(CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING);
											if (multipleMappings == null) {
												multipleMappings = new ArrayList<String>();
												rejectedForMapping.put(CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
											}
											for (CDMDrug cdmDrug : matchingCDMDrugsWithOneUnit) {
												String cdmDrugDescription = cdmDrug.toString();
												if (!multipleMappings.contains(cdmDrugDescription)) {
													multipleMappings.add(cdmDrugDescription);
												}
											}
											notUniqueMapping.get(mapping).add(sourceDrug);
										}
									}
								}
							}
						}
					}
					else {
						Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
						if (rejectedForMapping == null) {
							rejectedForMapping = new HashMap<Integer, List<String>>();
							sourceDrugMappingResult.put(mapping, rejectedForMapping);
						}
						List<String> emptyList = new ArrayList<String>();
						emptyList.add(" ");
						rejectedForMapping.put(CLINICAL_DRUG_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS, emptyList);
					}
				}
			}

			// Check for manual mapping and set final mapping.
			CDMDrug overruledMapping = null;
			CDMDrug finalMapping = automaticMapping;
			CDMDrug manualMapping = manualMappings.get(sourceDrug);
			if (manualMapping != null) {
				// There is a manual mapping.
				overruledMapping = finalMapping;
				finalMapping = manualMapping;
			}
			
			if (finalMapping != null) {
				Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
				if (sourceDrugMappingResult == null) {
					sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
					sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
				}
				
				Map<Integer, List<String>> mappingTypeResults = sourceDrugMappingResult.get(mapping);
				if (mappingTypeResults == null) {
					mappingTypeResults = new HashMap<Integer, List<String>>();
					sourceDrugMappingResult.put(mapping, mappingTypeResults);
				}

				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if (finalMapping.getConceptClassId().equals("Clinical Drug")) {
					mappedSourceDrugs.add(sourceDrug);
					
					List<String> acceptedMappingList = mappingTypeResults.get(CLINICAL_DRUG_MAPPING_MAPPED);
					if (acceptedMappingList == null) {
						acceptedMappingList = new ArrayList<String>();
						mappingTypeResults.put(CLINICAL_DRUG_MAPPING_MAPPED, acceptedMappingList);
					}
					acceptedMappingList.add(finalMapping.toString());
				}
				
				if (overruledMapping != null) {
					List<String> overruledMappingList = mappingTypeResults.get(CLINICAL_DRUG_MAPPING_OVERRULED_MAPPING);
					if (overruledMappingList == null) {
						overruledMappingList = new ArrayList<String>();
						mappingTypeResults.put(CLINICAL_DRUG_MAPPING_OVERRULED_MAPPING, overruledMappingList);
					}
					overruledMappingList.add(overruledMapping.toString());
				}
			}
		}

		report.add("Source drugs mapped to multiple CDM Clinical Drugs: " + notUniqueMapping.get(mapping).size() + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) notUniqueMapping.get(mapping).size() / (double) sourceDrugs.size()) * 100)) + "%)");
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugComps() {
		boolean ok = true;
		int mapping = CLINICAL_DRUG_COMP_MAPPING;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Comps ...");
		
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
					if (sourceDrug.getIngredients().size() == 1) { // Clinical Drug Comp is always single ingredient
						List<CDMIngredient> sourceDrugCDMIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

						// Find CDM Clinical Drug Comps with corresponding ingredient
						List<CDMDrug> cdmDrugCompsWithIngredients = null;
						for (CDMIngredient cdmIngredient : sourceDrugCDMIngredients) {
							List<CDMDrug> cdmDrugCompsWithIngredient = cdmDrugCompsContainingIngredient.get(cdmIngredient);
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
						if (cdmDrugCompsWithIngredients.size() > 0) {
							List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
							List<String> rejectedDrugs = new ArrayList<String>();
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
								rejectedDrugs = new ArrayList<String>();
								
								for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
									if (sourceDrug.getComponents().size() == cdmDrug.getIngredientStrengths().size()) {
										List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), strengthDeviationPercentage);
										if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
											matchingCDMDrugs.add(cdmDrug);
											matchingIngredientsMap.put(cdmDrug, matchingIngredients);
										}
										else {
											rejectedDrugs.add(cdmDrug.toString() + ": " + cdmDrug.getStrengthDescription());
										}
									}
								}
								
								if ((matchingCDMDrugs != null) && (matchingCDMDrugs.size() > 0)) {
									break;
								}
							}
							
							// Save the rejected drugs
							Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
							if (rejectedForMapping == null) {
								rejectedForMapping = new HashMap<Integer, List<String>>();
								sourceDrugMappingResult.put(mapping, rejectedForMapping);
							}
							rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_STRENGTH, rejectedDrugs);

							if (matchingCDMDrugs.size() > 1) {
								String vocabulary_id = null;
								int resultType = -1;
								if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
									vocabulary_id = "RxNorm";
									resultType = CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_PREFERENCE;
								}
								else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
									vocabulary_id = "RxNorm Extension";
									resultType = CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
								}
								if (vocabulary_id != null) {
									List<CDMDrug> remove = new ArrayList<CDMDrug>();
									for (CDMDrug cdmDrug : matchingCDMDrugs) {
										if (!cdmDrug.getVocabularyId().equals(vocabulary_id)) {
											remove.add(cdmDrug);
										}
									}
									if ((remove.size() > 0) && (matchingCDMDrugs.size() != remove.size())) {
										matchingCDMDrugs.removeAll(remove);
										
										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										rejectedForMapping = sourceDrugMappingResult.get(mapping);
										if (rejectedForMapping == null) {
											rejectedForMapping = new HashMap<Integer, List<String>>();
											sourceDrugMappingResult.put(mapping, rejectedForMapping);
										}
										
										List<String> multipleMappings = rejectedForMapping.get(resultType);
										if (multipleMappings == null) {
											multipleMappings = new ArrayList<String>();
											rejectedForMapping.put(resultType, multipleMappings);
										}
										for (CDMDrug cdmDrug : remove) {
											String cdmDrugDescription = cdmDrug.toString();
											if (!multipleMappings.contains(cdmDrugDescription)) {
												multipleMappings.add(cdmDrugDescription);
											}
										}
									}
								}
								if ((matchingCDMDrugs.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No"))) {
									boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
									resultType = latest ? CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE : CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE;
									List<CDMDrug> remove = new ArrayList<CDMDrug>();
									List<CDMDrug> lastDrugs = new ArrayList<CDMDrug>();
									int lastDate = -1;
									for (CDMDrug cdmDrug : matchingCDMDrugs) {
										try {
											Integer date = Integer.parseInt(cdmDrug.getValidStartDate().replaceAll("-",""));
											if (lastDate == -1) {
												lastDrugs.add(cdmDrug);
												lastDate = date;
											}
											else {
												if (latest ? (date > lastDate) : (date < lastDate)) {
													remove.addAll(lastDrugs);
													lastDrugs.clear();
													lastDrugs.add(cdmDrug);
													lastDate = date;
												}
												else if (date == lastDate) {
													lastDrugs.add(cdmDrug);
												}
												else {
													remove.add(cdmDrug);
												}
											}
										}
										catch (NumberFormatException e) {
											remove.add(cdmDrug);
										}
									}
									if ((remove.size() > 0) && (matchingCDMDrugs.size() != remove.size())) {
										matchingCDMDrugs.removeAll(remove);
										
										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										rejectedForMapping = sourceDrugMappingResult.get(mapping);
										if (rejectedForMapping == null) {
											rejectedForMapping = new HashMap<Integer, List<String>>();
											sourceDrugMappingResult.put(mapping, rejectedForMapping);
										}
										
										List<String> multipleMappings = rejectedForMapping.get(resultType);
										if (multipleMappings == null) {
											multipleMappings = new ArrayList<String>();
											rejectedForMapping.put(resultType, multipleMappings);
										}
										for (CDMDrug cdmDrug : remove) {
											String cdmDrugDescription = cdmDrug.toString();
											if (!multipleMappings.contains(cdmDrugDescription)) {
												multipleMappings.add(cdmDrugDescription);
											}
										}
									}
								}
								if ((matchingCDMDrugs.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None"))) {
									resultType = -1;
									List<CDMDrug> remove = new ArrayList<CDMDrug>();
									if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First")) {
										automaticMapping = matchingCDMDrugs.get(0);
										resultType = CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_FIRST_PREFERENCE;
										for (int nr = 1; nr < matchingCDMDrugs.size(); nr++) {
											remove.add(matchingCDMDrugs.get(nr));
										}
									}
									else {
										automaticMapping = matchingCDMDrugs.get(matchingCDMDrugs.size() - 1);
										resultType = CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LAST_PREFERENCE;
										for (int nr = 0; nr < (matchingCDMDrugs.size() - 1); nr++) {
											remove.add(matchingCDMDrugs.get(nr));
										}
									}
									matchingCDMDrugs.removeAll(remove);

									sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
									if (sourceDrugMappingResult == null) {
										sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									rejectedForMapping = sourceDrugMappingResult.get(mapping);
									if (rejectedForMapping == null) {
										rejectedForMapping = new HashMap<Integer, List<String>>();
										sourceDrugMappingResult.put(mapping, rejectedForMapping);
									}
									
									List<String> multipleMappings = rejectedForMapping.get(resultType);
									if (multipleMappings == null) {
										multipleMappings = new ArrayList<String>();
										rejectedForMapping.put(resultType, multipleMappings);
									}
									for (CDMDrug cdmDrug : remove) {
										String cdmDrugDescription = cdmDrug.toString();
										if (!multipleMappings.contains(cdmDrugDescription)) {
											multipleMappings.add(cdmDrugDescription);
										}
									}
								}
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
										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										rejectedForMapping = sourceDrugMappingResult.get(mapping);
										if (rejectedForMapping == null) {
											rejectedForMapping = new HashMap<Integer, List<String>>();
											sourceDrugMappingResult.put(mapping, rejectedForMapping);
										}
										
										List<String> multipleMappings = rejectedForMapping.get(CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING);
										if (multipleMappings == null) {
											multipleMappings = new ArrayList<String>();
											rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
										}
										for (CDMDrug cdmDrug : matchingCDMDrugs) {
											String cdmDrugDescription = cdmDrug.toString();
											if (!multipleMappings.contains(cdmDrugDescription)) {
												multipleMappings.add(cdmDrugDescription);
											}
										}
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
								}
								else {
									if (matchingCDMDrugsWithOneUnit.size() == 1) {
										automaticMapping = matchingCDMDrugs.get(0);
									}
									else {
										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										rejectedForMapping = sourceDrugMappingResult.get(mapping);
										if (rejectedForMapping == null) {
											rejectedForMapping = new HashMap<Integer, List<String>>();
											sourceDrugMappingResult.put(mapping, rejectedForMapping);
										}
										
										List<String> multipleMappings = rejectedForMapping.get(CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING);
										if (multipleMappings == null) {
											multipleMappings = new ArrayList<String>();
											rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
										}
										for (CDMDrug cdmDrug : matchingCDMDrugs) {
											String cdmDrugDescription = cdmDrug.toString();
											if (!multipleMappings.contains(cdmDrugDescription)) {
												multipleMappings.add(cdmDrugDescription);
											}
										}
										notUniqueMapping.get(mapping).add(sourceDrug);
									}
								}
							}
						}
						else {
							Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
							if (rejectedForMapping == null) {
								rejectedForMapping = new HashMap<Integer, List<String>>();
								sourceDrugMappingResult.put(mapping, rejectedForMapping);
							}
							List<String> emptyList = new ArrayList<String>();
							emptyList.add(" ");
							rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS, emptyList);
						}
					}
					else {
						Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
						if (rejectedForMapping == null) {
							rejectedForMapping = new HashMap<Integer, List<String>>();
							sourceDrugMappingResult.put(mapping, rejectedForMapping);
						}
						List<String> emptyList = new ArrayList<String>();
						emptyList.add(" ");
						rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_NO_SINGLE_INGREDIENT_DRUG, emptyList);
					}
				}
			}

			// Check for manual mapping and set final mapping.
			CDMDrug overruledMapping = null;
			CDMDrug finalMapping = automaticMapping;
			CDMDrug manualMapping = manualMappings.get(sourceDrug);
			if (manualMapping != null) {
				// There is a manual mapping.
				overruledMapping = finalMapping;
				finalMapping = manualMapping;
			}
			
			if (finalMapping != null) {
				Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
				if (sourceDrugMappingResult == null) {
					sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
					sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
				}
				
				Map<Integer, List<String>> mappingTypeResults = sourceDrugMappingResult.get(mapping);
				if (mappingTypeResults == null) {
					mappingTypeResults = new HashMap<Integer, List<String>>();
					sourceDrugMappingResult.put(mapping, mappingTypeResults);
				}

				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if (finalMapping.getConceptClassId().equals("Clinical Drug Comp")) {
					mappedSourceDrugs.add(sourceDrug);
					
					List<String> acceptedMappingList = mappingTypeResults.get(CLINICAL_DRUG_COMP_MAPPING_MAPPED);
					if (acceptedMappingList == null) {
						acceptedMappingList = new ArrayList<String>();
						mappingTypeResults.put(CLINICAL_DRUG_COMP_MAPPING_MAPPED, acceptedMappingList);
					}
					acceptedMappingList.add(finalMapping.toString());
				}
				
				if (overruledMapping != null) {
					List<String> overruledMappingList = mappingTypeResults.get(CLINICAL_DRUG_COMP_MAPPING_OVERRULED_MAPPING);
					if (overruledMappingList == null) {
						overruledMappingList = new ArrayList<String>();
						mappingTypeResults.put(CLINICAL_DRUG_COMP_MAPPING_OVERRULED_MAPPING, overruledMappingList);
					}
					overruledMappingList.add(overruledMapping.toString());
				}
			}
		}

		report.add("Source drugs mapped to multiple CDM Clinical Drug Comps: " + notUniqueMapping.get(mapping).size() + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) notUniqueMapping.get(mapping).size() / (double) sourceDrugs.size()) * 100)) + "%)");
		
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
						List<CDMDrug> cdmDrugsWithIngredient = cdmDrugFormsContainingIngredient.get(cdmIngredient);
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
								Set<String> cdmDrugForms = cdmDrug.getForms();
								boolean formFound = false;
								for (String cdmDrugForm : cdmDrugForms) {
									if (formConversionsMap.matches(sourceDrugForm, cdmDrugForm)) {
										formFound = true;
										break;
									}
								}
								if (!formFound) {
									cdmDrugsMissingForm.add(cdmDrug);
								}
							}

							Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
							if (rejectedForMapping == null) {
								rejectedForMapping = new HashMap<Integer, List<String>>();
								sourceDrugMappingResult.put(mapping, rejectedForMapping);
							}
							
							List<String> rejectedForms = rejectedForMapping.get(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FORM);
							if (rejectedForms == null) {
								rejectedForms = new ArrayList<String>();
								rejectedForMapping.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FORM, rejectedForms);
							}
							for (CDMDrug cdmDrug : cdmDrugsMissingForm) {
								String formsDescription = "";
								for (String cdmDrugFormConceptId : cdmDrug.getForms()) {
									if (!formsDescription.equals("")) {
										formsDescription += " | ";
									}
									formsDescription += formConversionsMap.getCDMFormConceptName(cdmDrugFormConceptId) + " (" + cdmDrugFormConceptId + ")";
								}
								String cdmDrugDescription = cdmDrug.toString() + ": " + formsDescription;
								if (!rejectedForms.contains(cdmDrugDescription)) {
									rejectedForms.add(cdmDrugDescription);
								}
							}
							
							cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
						}

						if (cdmDrugsWithIngredients.size() > 1) {
							String vocabulary_id = null;
							int resultType = -1;
							if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
								vocabulary_id = "RxNorm";
								resultType = CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_PREFERENCE;
							}
							else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
								vocabulary_id = "RxNorm Extension";
								resultType = CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
							}
							if (vocabulary_id != null) {
								List<CDMDrug> remove = new ArrayList<CDMDrug>();
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									if (!cdmDrug.getVocabularyId().equals(vocabulary_id)) {
										remove.add(cdmDrug);
									}
								}
								if ((remove.size() > 0) && (cdmDrugsWithIngredients.size() != remove.size())) {
									cdmDrugsWithIngredients.removeAll(remove);
									
									Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
									if (sourceDrugMappingResult == null) {
										sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
									if (rejectedForMapping == null) {
										rejectedForMapping = new HashMap<Integer, List<String>>();
										sourceDrugMappingResult.put(mapping, rejectedForMapping);
									}
									
									List<String> multipleMappings = rejectedForMapping.get(resultType);
									if (multipleMappings == null) {
										multipleMappings = new ArrayList<String>();
										rejectedForMapping.put(resultType, multipleMappings);
									}
									for (CDMDrug cdmDrug : remove) {
										String cdmDrugDescription = cdmDrug.toString();
										if (!multipleMappings.contains(cdmDrugDescription)) {
											multipleMappings.add(cdmDrugDescription);
										}
									}
								}
							}
							if ((cdmDrugsWithIngredients.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No"))) {
								boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
								resultType = latest ? CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE : CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE;
								List<CDMDrug> remove = new ArrayList<CDMDrug>();
								List<CDMDrug> lastDrugs = new ArrayList<CDMDrug>();
								int lastDate = -1;
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									try {
										Integer date = Integer.parseInt(cdmDrug.getValidStartDate().replaceAll("-",""));
										if (lastDate == -1) {
											lastDrugs.add(cdmDrug);
											lastDate = date;
										}
										else {
											if (latest ? (date > lastDate) : (date < lastDate)) {
												remove.addAll(lastDrugs);
												lastDrugs.clear();
												lastDrugs.add(cdmDrug);
												lastDate = date;
											}
											else if (date == lastDate) {
												lastDrugs.add(cdmDrug);
											}
											else {
												remove.add(cdmDrug);
											}
										}
									}
									catch (NumberFormatException e) {
										remove.add(cdmDrug);
									}
								}
								if ((remove.size() > 0) && (cdmDrugsWithIngredients.size() != remove.size())) {
									cdmDrugsWithIngredients.removeAll(remove);
									
									Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
									if (sourceDrugMappingResult == null) {
										sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
									if (rejectedForMapping == null) {
										rejectedForMapping = new HashMap<Integer, List<String>>();
										sourceDrugMappingResult.put(mapping, rejectedForMapping);
									}
									
									List<String> multipleMappings = rejectedForMapping.get(resultType);
									if (multipleMappings == null) {
										multipleMappings = new ArrayList<String>();
										rejectedForMapping.put(resultType, multipleMappings);
									}
									for (CDMDrug cdmDrug : remove) {
										String cdmDrugDescription = cdmDrug.toString();
										if (!multipleMappings.contains(cdmDrugDescription)) {
											multipleMappings.add(cdmDrugDescription);
										}
									}
								}
							}
							if ((cdmDrugsWithIngredients.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None"))) {
								if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First")) {
									automaticMapping = cdmDrugsWithIngredients.get(0);
								}
								else {
									automaticMapping = cdmDrugsWithIngredients.get(cdmDrugsWithIngredients.size() - 1);
								}
								
								resultType = -1;
								List<CDMDrug> remove = new ArrayList<CDMDrug>();
								if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("First")) {
									automaticMapping = cdmDrugsWithIngredients.get(0);
									resultType = CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FIRST_PREFERENCE;
									for (int nr = 1; nr < cdmDrugsWithIngredients.size(); nr++) {
										remove.add(cdmDrugsWithIngredients.get(nr));
									}
								}
								else {
									automaticMapping = cdmDrugsWithIngredients.get(cdmDrugsWithIngredients.size() - 1);
									resultType = CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LAST_PREFERENCE;
									for (int nr = 0; nr < (cdmDrugsWithIngredients.size() - 1); nr++) {
										remove.add(cdmDrugsWithIngredients.get(nr));
									}
								}
								cdmDrugsWithIngredients.removeAll(remove);
								
								Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
								if (sourceDrugMappingResult == null) {
									sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
									sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
								}
								Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
								if (rejectedForMapping == null) {
									rejectedForMapping = new HashMap<Integer, List<String>>();
									sourceDrugMappingResult.put(mapping, rejectedForMapping);
								}
								
								List<String> multipleMappings = rejectedForMapping.get(resultType);
								if (multipleMappings == null) {
									multipleMappings = new ArrayList<String>();
									rejectedForMapping.put(resultType, multipleMappings);
								}
								for (CDMDrug cdmDrug : remove) {
									String cdmDrugDescription = cdmDrug.toString();
									if (!multipleMappings.contains(cdmDrugDescription)) {
										multipleMappings.add(cdmDrugDescription);
									}
								}
							}
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
								Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
								if (sourceDrugMappingResult == null) {
									sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
									sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
								}
								Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
								if (rejectedForMapping == null) {
									rejectedForMapping = new HashMap<Integer, List<String>>();
									sourceDrugMappingResult.put(mapping, rejectedForMapping);
								}
								
								List<String> multipleMappings = rejectedForMapping.get(CLINICAL_DRUG_FORM_MAPPING_NO_UNIQUE_MAPPING);
								if (multipleMappings == null) {
									multipleMappings = new ArrayList<String>();
									rejectedForMapping.put(CLINICAL_DRUG_FORM_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
								}
								for (CDMDrug cdmDrug : matchingCDMDrugs) {
									String cdmDrugDescription = cdmDrug.toString();
									if (!multipleMappings.contains(cdmDrugDescription)) {
										multipleMappings.add(cdmDrugDescription);
									}
								}
								notUniqueMapping.get(mapping).add(sourceDrug);
							}
						}
					}
					else {
						Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						Map<Integer, List<String>> rejectedForMapping = sourceDrugMappingResult.get(mapping);
						if (rejectedForMapping == null) {
							rejectedForMapping = new HashMap<Integer, List<String>>();
							sourceDrugMappingResult.put(mapping, rejectedForMapping);
						}
						List<String> emptyList = new ArrayList<String>();
						emptyList.add(" ");
						rejectedForMapping.put(CLINICAL_DRUG_FORM_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS, emptyList);
					}
				}
			}

			// Check for manual mapping and set final mapping.
			CDMDrug overruledMapping = null;
			CDMDrug finalMapping = automaticMapping;
			CDMDrug manualMapping = manualMappings.get(sourceDrug);
			if (manualMapping != null) {
				// There is a manual mapping.
				overruledMapping = finalMapping;
				finalMapping = manualMapping;
			}
			
			if (finalMapping != null) {
				Map<Integer, Map<Integer, List<String>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
				if (sourceDrugMappingResult == null) {
					sourceDrugMappingResult = new HashMap<Integer, Map<Integer, List<String>>>();
					sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
				}
				
				Map<Integer, List<String>> mappingTypeResults = sourceDrugMappingResult.get(mapping);
				if (mappingTypeResults == null) {
					mappingTypeResults = new HashMap<Integer, List<String>>();
					sourceDrugMappingResult.put(mapping, mappingTypeResults);
				}

				// Set mapping if it has the current mapping type.
				// The mapping type can be different in case of a manual mapping.
				if (finalMapping.getConceptClassId().equals("Clinical Drug Form")) {
					mappedSourceDrugs.add(sourceDrug);
					
					List<String> acceptedMappingList = mappingTypeResults.get(CLINICAL_DRUG_FORM_MAPPING_MAPPED);
					if (acceptedMappingList == null) {
						acceptedMappingList = new ArrayList<String>();
						mappingTypeResults.put(CLINICAL_DRUG_FORM_MAPPING_MAPPED, acceptedMappingList);
					}
					acceptedMappingList.add(finalMapping.toString());
				}
				
				if (overruledMapping != null) {
					List<String> overruledMappingList = mappingTypeResults.get(CLINICAL_DRUG_FORM_MAPPING_OVERRULED_MAPPING);
					if (overruledMappingList == null) {
						overruledMappingList = new ArrayList<String>();
						mappingTypeResults.put(CLINICAL_DRUG_FORM_MAPPING_OVERRULED_MAPPING, overruledMappingList);
					}
					overruledMappingList.add(overruledMapping.toString());
				}
			}
		}

		report.add("Source drugs mapped to multiple CDM Clinical Drug Forms: " + notUniqueMapping.get(mapping).size() + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) notUniqueMapping.get(mapping).size() / (double) sourceDrugs.size()) * 100)) + "%)");
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private void saveMapping() {
		// Save ingredient mapping
		PrintWriter ingredientMappingFile = openOutputFile("IngredientMapping Results.csv", SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
		
		if (ingredientMappingFile != null) {
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				if (sourceIngredient.getMatchingIngredient() != null) {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatchingIngredient()));
				}
				else {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + CDMIngredient.emptyRecord());
				}
			}
			
			ingredientMappingFile.close();
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
			int mappingResultType = (100 * mappingType);
			while (mappingResultDescriptions.containsKey(mappingResultType)) {
				mappingTypeCounters.put(mappingResultType, 0L);
				mappingTypeDataCoverage.put(mappingResultType, 0L);
				mappingResultType++;
			}
			mappingType++;
		}
		
		String header = "MappingStatus";
		header += "," + SourceDrug.getHeader();
		header += "," + "MappingType";
		header += "," + "MappingResult";
		header += "," + "Results";
		
		PrintWriter drugMappingFile = openOutputFile("DrugMapping Results.csv", header);
		
		if (drugMappingFile != null) {
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
				String mappingStatus = manualMappings.containsKey(sourceDrug) ? "ManualMapping" : (mappedSourceDrugs.contains(sourceDrug) ? "Mapped" : "Unmapped");
				Map<Integer, Map<Integer, List<String>>> sourceDrugMappings = sourceDrugMappingResults.get(sourceDrug);
				
				if (sourceDrugMappings == null) {
					System.out.println("ERROR: " + sourceDrug);
				}

				mappingType = 0;
				while (mappingTypeDescriptions.containsKey(mappingType)) {
					Map<Integer, List<String>> mappingResult = sourceDrugMappings.get(mappingType);
					if (mappingResult != null) {
						// Get the MAPPED result type value for the current mapping type
						int mappingResultType = (100 * mappingType);
						int mappedResultType = mappingResultType;
						while (mappingResultDescriptions.containsKey(mappingResultType)) {
							mappedResultType = mappingResultType;
							mappingResultType++;
						}

						// Write the result records
						mappingResultType = (100 * mappingType);
						while (mappingResultDescriptions.containsKey(mappingResultType)) {
							String record = mappingStatus;
							record += "," + sourceDrug;
							record += "," + mappingTypeDescriptions.get(mappingType);
							record += "," + mappingResultDescriptions.get(mappingResultType);
							
							counters.get(mappingType).put(mappingResultType, counters.get(mappingType).get(mappingResultType) + 1);
							dataCoverage.get(mappingType).put(mappingResultType, dataCoverage.get(mappingType).get(mappingResultType) + sourceDrug.getCount());
							//TODO
							/*
							counters.get(MAPPING_TYPE_COUNT).put(MAPPING_RESULTS_COUNT, counters.get(MAPPING_TYPE_COUNT).get(MAPPING_RESULTS_COUNT) + 1);
							dataCoverage.get(MAPPING_TYPE_COUNT).put(MAPPING_RESULTS_COUNT, dataCoverage.get(MAPPING_TYPE_COUNT).get(MAPPING_RESULTS_COUNT) + sourceDrug.getCount());
							*/
							
							List<String> results = mappingResult.get(mappingResultType);
							if ((results != null) && (results.size() > 0)) {
								if (mappingResultType == mappedResultType) {
									record += "," + results.get(0);
								}
								else {
									Collections.sort(results);
									for (String result : results) {
										record += "," + "\"" + result.replaceAll("\"", "") + "\"";
									}
								}
								drugMappingFile.println(record);
							}
							
							mappingResultType++;
						}
					}
					mappingType++;
				}
			}
			
			closeOutputFile(drugMappingFile);
			
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
	}
	
	
	private void finalReport() {
		Long dataCountTotal = 0L;
		Long dataCoverageIngredients = 0L;
		Long mappingClinicalDrugs = 0L;
		Long dataCoverageClinicalDrugs = 0L;
		Long mappingClinicalDrugComps = 0L;
		Long dataCoverageClinicalDrugComps = 0L;
		Long mappingClinicalDrugForms = 0L;
		Long dataCoverageClinicalDrugForms = 0L;
		
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
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING).get(CLINICAL_DRUG_MAPPING_MAPPED) != null) {
					mappingClinicalDrugs++;
					dataCoverageClinicalDrugs += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING).get(CLINICAL_DRUG_COMP_MAPPING_MAPPED) != null) {
					mappingClinicalDrugComps++;
					dataCoverageClinicalDrugComps += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING).get(CLINICAL_DRUG_FORM_MAPPING_MAPPED) != null) {
					mappingClinicalDrugForms++;
					dataCoverageClinicalDrugForms += sourceDrug.getCount();
				}
			}
		}
		
		report.add("");
		report.add("Source drugs mapped to single CDM Clinical Drug: " + Long.toString(mappingClinicalDrugs) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) mappingClinicalDrugs / (double) sourceDrugs.size()) * 100)) + "%)");
		report.add("Source drugs mapped to single CDM Clinical Drug Comp: " + Long.toString(mappingClinicalDrugComps) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) mappingClinicalDrugComps / (double) sourceDrugs.size()) * 100)) + "%)");
		report.add("Source drugs mapped to single CDM Clinical Drug Form: " + Long.toString(mappingClinicalDrugForms) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) mappingClinicalDrugForms / (double) sourceDrugs.size()) * 100)) + "%)");
		report.add("Total Source drugs mapped: " + (mappingClinicalDrugs + mappingClinicalDrugComps + mappingClinicalDrugForms) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) (mappingClinicalDrugs + mappingClinicalDrugComps + mappingClinicalDrugForms) / (double) sourceDrugs.size()) * 100)) + "%)");
		
		if (dataCountTotal != 0) {
			report.add("");
			report.add("Datacoverage Source drugs with all ingredients mapped: " + dataCoverageIngredients + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageIngredients / (double) dataCountTotal) * 100)) + "%)");
			report.add("Datacoverage CDM Clinical Drug mapping: " + dataCoverageClinicalDrugs + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugs / (double) dataCountTotal) * 100)) + "%)");
			report.add("Datacoverage CDM Clinical Drug Comp mapping: " + dataCoverageClinicalDrugComps + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugComps / (double) dataCountTotal) * 100)) + "%)");
			report.add("Datacoverage CDM Clinical Drug Form mapping: " + dataCoverageClinicalDrugForms + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugForms / (double) dataCountTotal) * 100)) + "%)");
			report.add("Total datacoverage drug mapping: " + (dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms) + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) (dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms) / (double) dataCountTotal) * 100)) + "%)");
		}
		else {
			report.add("");
			report.add("No datacoverage counts available.");
		}
		//TODO
		/*
		report.add("");
		for (int mappingType = 0; mappingType < MAPPING_TYPE_COUNT; mappingType++) {
			for (int mappingResultType = 0; mappingResultType < MAPPING_RESULTS_COUNT; mappingResultType++) {
				report.add("Source drugs with " + mappingTypeDescriptions.get(mappingType) + " " + mappingResultDescriptions.get(mappingResultType) + ": " + Long.toString(counters.get(mappingType).get(mappingResultType)) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) counters.get(mappingType).get(mappingResultType) / (double) sourceDrugs.size()) * 100)) + "%)");
			}
		}

		report.add("");
		for (int mappingType = 0; mappingType < MAPPING_TYPE_COUNT; mappingType++) {
			for (int mappingResultType = 0; mappingResultType < MAPPING_RESULTS_COUNT; mappingResultType++) {
				report.add("DataCoverage of source drugs with " + mappingTypeDescriptions.get(mappingType) + " " + mappingResultDescriptions.get(mappingResultType) + ": " + Long.toString(dataCoverage.get(mappingType).get(mappingResultType)) + " of " + dataCoverage.get(MAPPING_TYPE_COUNT).get(MAPPING_RESULTS_COUNT) + " (" + Long.toString(Math.round(((double) dataCoverage.get(mappingType).get(mappingResultType) / (double) dataCoverage.get(MAPPING_TYPE_COUNT).get(MAPPING_RESULTS_COUNT)) * 100)) + "%)");
			}
		}
		*/
		
		
		System.out.println();
		for (String reportLine : report) {
			System.out.println(reportLine);
		}
	}
	
	
	private List<CDMIngredientStrength> matchingIngredients(List<SourceDrugComponent> sourceDrugComponents, Map<String, Set<CDMIngredientStrength>> cdmIngredientsMap, Double strengthDeviationPercentage) {
		List<CDMIngredientStrength> matchingIngredients = new ArrayList<CDMIngredientStrength>();
		matchingIngredients = new ArrayList<CDMIngredientStrength>();
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
			if (sourceIngredient != null) {
				String cdmIngredientConceptId = sourceIngredient.getMatchingIngredient();
				if (cdmIngredientConceptId != null) {
					Set<CDMIngredientStrength> matchingCDMIngredients = cdmIngredientsMap.get(cdmIngredientConceptId);
					if (matchingCDMIngredients != null) {
						if (strengthDeviationPercentage != null) {
							boolean found = false;
							for (CDMIngredientStrength cdmIngredientStrength : matchingCDMIngredients) {
								if (sourceDrugComponent.matches(unitConversionsMap, cdmIngredientStrength.getNumeratorDosage(), cdmIngredientStrength.getNumeratorDosageUnit(), cdmIngredientStrength.getDenominatorDosage(), cdmIngredientStrength.getDenominatorDosageUnit(), strengthDeviationPercentage)) {
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
	
	
	private PrintWriter openOutputFile(String fileName, String header) {
		PrintWriter outputPrintWriter = null;
		String fullFileName = "";
		try {
			// Create output file
			fullFileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + fileName;
			outputPrintWriter = new PrintWriter(new File(fullFileName));
			outputPrintWriter.println(header);
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fullFileName + "'");
			outputPrintWriter = null;
		}
		return outputPrintWriter;
	}
	
	
	private void closeOutputFile(PrintWriter outputFile) {
		if (outputFile != null) {
			outputFile.close();
		}
	}
	
	
/*
	public static void main(String[] args) {
	}
*/
}
