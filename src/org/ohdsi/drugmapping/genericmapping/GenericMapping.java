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

import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.FormConversion;
import org.ohdsi.drugmapping.IngredientNameTranslation;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.UnitConversion;
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
import org.ohdsi.utilities.StringUtilities;
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
	private static int CLINICAL_DRUG_MAPPING_NO_SOURCE_INGREDIENTS                          =   0; // The source drug has no ingredients
	private static int CLINICAL_DRUG_MAPPING_UNMAPPED_SOURCE_INGREDIENTS                    =   1; // The source drug has unmapped ingredients
	private static int CLINICAL_DRUG_MAPPING_DOUBLE_INGREDIENT_MAPPING                      =   2; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int CLINICAL_DRUG_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS             =   3; // There is no CDM clinical drug with matching ingredients.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_FORM                               =   4; // The CDM clinical drugs rejected because they have a different form than the source drug.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_STRENGTH                           =   5; // The CDM clinical drugs rejected because they have a different strength than the source drug.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_PREFERENCE                  =   6; // The CDM clinical drugs rejected because they are not in the preferred RxNorm vocabulary.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE        =   7; // The CDM clinical drugs rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE             =   8; // The CDM clinical drugs rejected because they do not have the latest valid start date.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE             =   9; // The CDM clinical drugs rejected because they do not have the oldest recent valid start date.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE      =  10; // The CDM clinical drugs rejected because they do not have the smallest concept_id.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE      =  11; // The CDM clinical drugs rejected because they do not have the greatest concept_id.
	private static int CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING                              =  12; // There are several clinical drugs the source drug could be mapped to.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_FIRST_PREFERENCE                   =  13; // The CDM clinical drugs rejected because the first one found is taken.
	private static int CLINICAL_DRUG_MAPPING_REJECTED_BY_LAST_PREFERENCE                    =  14; // The CDM clinical drugs rejected because the last one found is taken.
	private static int CLINICAL_DRUG_MAPPING_OVERRULED_MAPPING                              =  15; // A mapping to a single clinical drug or a failing mapping is overruled by a manual mapping.
	private static int CLINICAL_DRUG_MAPPING_MAPPED                                         =  16; // The final mapping of the source drug to a clinical drug.

	private static int CLINICAL_DRUG_COMP_MAPPING_NO_SOURCE_INGREDIENTS                     = 100; // The source drug has no ingredients
	private static int CLINICAL_DRUG_COMP_MAPPING_UNMAPPED_SOURCE_INGREDIENTS               = 101; // The source drug has unmapped ingredients
	private static int CLINICAL_DRUG_COMP_MAPPING_DOUBLE_INGREDIENT_MAPPING                 = 102; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int CLINICAL_DRUG_COMP_MAPPING_NO_SINGLE_INGREDIENT_DRUG                 = 103; // The source drug should have only one ingredient to map to a CDM clinical drug comp.
	private static int CLINICAL_DRUG_COMP_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS        = 104; // There is no CDM clinical drug comp with matching ingredients.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_STRENGTH                      = 105; // The CDM clinical drug comps rejected because they have a different strength than the source drug.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_PREFERENCE             = 106; // The CDM clinical drug comps rejected because they are not in the preferred RxNorm vocabulary.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE   = 107; // The CDM clinical drug comps rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE        = 108; // The CDM clinical drug comps rejected because they do not have the latest valid start date.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE        = 109; // The CDM clinical drug comps rejected because they do not have the oldest valid start date.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE = 110; // The CDM clinical drug comps rejected because they do not have the smallest concept_id.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE = 111; // The CDM clinical drug comps rejected because they do not have the greatest concept_id.
	private static int CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING                         = 112; // There are several clinical drug comps the source drug could be mapped to.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_FIRST_PREFERENCE              = 113; // The CDM clinical drug comps rejected because the first one found is taken.
	private static int CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LAST_PREFERENCE               = 114; // The CDM clinical drug comps rejected because the last one found is taken.
	private static int CLINICAL_DRUG_COMP_MAPPING_OVERRULED_MAPPING                         = 115; // A mapping to a single clinical drug comp or a failing mapping is overruled by a manual mapping.
	private static int CLINICAL_DRUG_COMP_MAPPING_MAPPED                                    = 116; // The final mapping of the source drug to a clinical drug comp.

	private static int CLINICAL_DRUG_FORM_MAPPING_NO_SOURCE_INGREDIENTS                     = 200; // The source drug has no ingredients
	private static int CLINICAL_DRUG_FORM_MAPPING_UNMAPPED_SOURCE_INGREDIENTS               = 201; // The source drug has unmapped ingredients
	private static int CLINICAL_DRUG_FORM_MAPPING_DOUBLE_INGREDIENT_MAPPING                 = 202; // Two or more source drug ingredients are mapped to the same CDM ingredient.
	private static int CLINICAL_DRUG_FORM_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS        = 203; // There is no CDM clinical drug form with matching ingredients.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FORM                          = 204; // The CDM clinical drug forms rejected because they have a different form than the source drug.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_PREFERENCE             = 205; // The CDM clinical drug forms rejected because they are not in the preferred RxNorm vocabulary.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE   = 206; // The CDM clinical drug forms rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE        = 207; // The CDM clinical drug forms rejected because they do not have the latest valid start date.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE        = 208; // The CDM clinical drug forms rejected because they do not have the oldest valid start date.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE = 209; // The CDM clinical drug forms rejected because they do not have the smallest concept_id.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE = 210; // The CDM clinical drug forms rejected because they do not have the greatest concept_id.
	private static int CLINICAL_DRUG_FORM_MAPPING_NO_UNIQUE_MAPPING                         = 211; // There are several clinical drug forms the source drug could be mapped to.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FIRST_PREFERENCE              = 212; // The CDM clinical drug forms rejected because the first one found is taken.
	private static int CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LAST_PREFERENCE               = 213; // The CDM clinical drug forms rejected because the last one found is taken.
	private static int CLINICAL_DRUG_FORM_MAPPING_OVERRULED_MAPPING                         = 214; // A mapping to a single clinical drug form or a failing mapping is overruled by a manual mapping.
	private static int CLINICAL_DRUG_FORM_MAPPING_MAPPED                                    = 215; // The final mapping of the source drug to a clinical drug form.

	private static int INGREDIENT_MAPPING_REJECTED_BY_STRENGTH                              = 300; // The CDM ingredients rejected because they have a different strength than the source drug.
	private static int INGREDIENT_MAPPING_REJECTED_BY_RXNORM_PREFERENCE                     = 301; // The CDM ingredients rejected because they are not in the preferred RxNorm vocabulary.
	private static int INGREDIENT_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE           = 302; // The CDM ingredients rejected because they are not in the preferred RxNorm Extension vocabulary.
	private static int INGREDIENT_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE                = 303; // The CDM ingredients rejected because they do not have the latest valid start date.
	private static int INGREDIENT_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE                = 304; // The CDM ingredients rejected because they do not have the oldest valid start date.
	private static int INGREDIENT_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE         = 209; // The CDM ingredients rejected because they do not have the smallest concept_id.
	private static int INGREDIENT_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE         = 210; // The CDM ingredients rejected because they do not have the greatest concept_id.
	private static int INGREDIENT_MAPPING_NO_UNIQUE_MAPPING                                 = 305; // One or more ingredients have several clinical drug comps or ingredients they could be mapped to.
	private static int INGREDIENT_MAPPING_REJECTED_BY_FIRST_PREFERENCE                      = 306; // The CDM clinical drug comps rejected because the first one found is taken.
	private static int INGREDIENT_MAPPING_REJECTED_BY_LAST_PREFERENCE                       = 307; // The CDM clinical drug comps rejected because the last one found is taken.
	private static int INGREDIENT_MAPPING_OVERRULED_MAPPING                                 = 308; // A mapping to a single clinical drug comp, ingredient or a failing mapping is overruled by a manual mapping.
	private static int INGREDIENT_MAPPING_MAPPED                                            = 309; // The final mapping of a source drug ingredient to a clinical drug comp or ingredient.

	private static Map<Integer, String> mappingResultDescriptions;
	static {
		mappingResultDescriptions = new HashMap<Integer  , String>();
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_NO_SOURCE_INGREDIENTS                         , "No source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_UNMAPPED_SOURCE_INGREDIENTS                   , "Unmapped source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_DOUBLE_INGREDIENT_MAPPING                     , "Double ingredient mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS            , "No drugs with matching ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_FORM                              , "Rejected by form");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_STRENGTH                          , "Rejected by strength");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_PREFERENCE                 , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE       , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE            , "Rejected by latest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE            , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE     , "Rejected by smallest concept_id");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE     , "Rejected by greatest concept_id");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_NO_UNIQUE_MAPPING                             , "No unique mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_FIRST_PREFERENCE                  , "Rejected because first is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_REJECTED_BY_LAST_PREFERENCE                   , "Rejected because last is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_OVERRULED_MAPPING                             , "Overruled mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_MAPPING_MAPPED                                        , "Mapped");

		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_SOURCE_INGREDIENTS                    , "No source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_UNMAPPED_SOURCE_INGREDIENTS              , "Unmapped source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_DOUBLE_INGREDIENT_MAPPING                , "Double ingredient mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_SINGLE_INGREDIENT_DRUG                , "No single ingredient drug");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS       , "No drugs with matching ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_STRENGTH                     , "Rejected by strength");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_PREFERENCE            , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE  , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE       , "Rejected by latest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE       , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE, "Rejected by smallest concept_id");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE, "Rejected by greatest concept_id");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_NO_UNIQUE_MAPPING                        , "No unique mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_FIRST_PREFERENCE             , "Rejected because first is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_LAST_PREFERENCE              , "Rejected because last is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_OVERRULED_MAPPING                        , "Overruled mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_COMP_MAPPING_MAPPED                                   , "Mapped");

		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_NO_SOURCE_INGREDIENTS                    , "No source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_UNMAPPED_SOURCE_INGREDIENTS              , "Unmapped source ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_DOUBLE_INGREDIENT_MAPPING                , "Double ingredient mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS       , "No drugs with matching ingredients");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FORM                         , "Rejected by form");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_PREFERENCE            , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE  , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE       , "Rejected by latest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE       , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE, "Rejected by smallest concept_id");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE, "Rejected by greatest concept_id");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_NO_UNIQUE_MAPPING                        , "No unique mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_FIRST_PREFERENCE             , "Rejected because first is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_LAST_PREFERENCE              , "Rejected because last is used");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_OVERRULED_MAPPING                        , "Overruled mapping");
		mappingResultDescriptions.put(CLINICAL_DRUG_FORM_MAPPING_MAPPED                                   , "Mapped");

		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_STRENGTH                             , "Rejected by strength");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_RXNORM_PREFERENCE                    , "Rejected by RxNorm preference");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE          , "Rejected by RxNorm Extension preference");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE               , "Rejected by latest valid start date");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE               , "Rejected by oldest valid start date");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE        , "Rejected by smallest concept_id");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE        , "Rejected by greatest concept_id");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_NO_UNIQUE_MAPPING                                , "No unique mapping");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_FIRST_PREFERENCE                     , "Rejected because first is used");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_REJECTED_BY_LAST_PREFERENCE                      , "Rejected because last is used");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_OVERRULED_MAPPING                                , "Overruled mapping");
		mappingResultDescriptions.put(INGREDIENT_MAPPING_MAPPED                                           , "Mapped");
	}
	
	private Set<String> forms = new HashSet<String>();
	private Set<String> units = new HashSet<String>();

	private Map<String, CDMIngredient> manualCASMappings = new HashMap<String, CDMIngredient>();
	private Map<SourceIngredient, CDMIngredient> manualIngredientCodeMappings = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<SourceIngredient, String> manualIngredientCodeMappingRemarks = new HashMap<SourceIngredient, String>();
	private Map<String, CDMIngredient> manualIngredientNameMappings = new HashMap<String, CDMIngredient>();
	private Map<SourceDrug, CDMDrug> manualDrugMappings = new HashMap<SourceDrug, CDMDrug>();
	
	private UnitConversion unitConversionsMap = null;
	private FormConversion formConversionsMap = null;
	private IngredientNameTranslation ingredientNameTranslationMap = null;
	
	private Map<String, List<String>> externalCASSynonymsMap = null;
	
	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private List<SourceDrug> sourceDrugsAllIngredientsMapped = new ArrayList<SourceDrug>();
	private Map<SourceDrug, List<CDMIngredient>> sourceDrugsCDMIngredients = new HashMap<SourceDrug, List<CDMIngredient>>();
	
	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<String, Set<CDMIngredient>> cdmIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<CDMIngredient>> cdmMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<CDMIngredient>> cdmReplacedByIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<CDMIngredient>> cdmEquivalentIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, CDMIngredient> cdmReplacedByIngredientConceptIdIndex = new HashMap<String, CDMIngredient>();
	private Map<String, Set<CDMIngredient>> cdmATCIngredientMap = new HashMap<String, Set<CDMIngredient>>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<String, CDMIngredient> cdmCASIngredientMap = new HashMap<String, CDMIngredient>();
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<String, CDMDrug> cdmDrugComps = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<String, CDMDrug> cdmDrugForms = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugFormsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	
	private Set<SourceDrug> mappedSourceDrugs = new HashSet<SourceDrug>();
	
	private List<String> cdmIngredientNameIndexNameList = new ArrayList<String>();
	private Map<String, Map<String, Set<CDMIngredient>>> cdmIngredientNameIndexMap = new HashMap<String, Map<String, Set<CDMIngredient>>>();
	
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<String>>>>> sourceDrugMappingResults = new HashMap<SourceDrug, Map<Integer, List<Map<Integer, List<String>>>>>(); // SourceDrug, Mapping, List of Mapping result, List of options
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
		patternReplacement.put("^", " ");
		patternReplacement.put("'", " ");
		patternReplacement.put("\\]'", " ");
		patternReplacement.put("\\['", " ");

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
	
	
		
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile casFile, InputFile manualCASMappingFile, InputFile manualIngredientMappingFile, InputFile manualDrugMappingFile) {
		boolean ok = true;

		cdmIngredientNameIndexNameList.add("Ingredient");
		cdmIngredientNameIndexMap.put("Ingredient", cdmIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("MapsToIngredient");
		cdmIngredientNameIndexMap.put("MapsToIngredient", cdmMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("ReplacedByIngredient");
		cdmIngredientNameIndexMap.put("ReplacedByIngredient", cdmReplacedByIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("EquivalentToIngredient");
		cdmIngredientNameIndexMap.put("EquivalentToIngredient", cdmEquivalentIngredientNameIndex);
		
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
		boolean unitsOk = ok && getUnitConversion(database);
		
		// Get form conversion from local forms to CDM forms
		boolean formsOk = ok && getFormConversion(database);

		/* 2020-05-20 REPLACED BEGIN Translation */
		ok = ok && unitsOk && formsOk;
		/* 2020-05-20 REPLACED BEGIN Translation */
		/* 2020-05-20 REPLACED BY BEGIN Translation
		// Get the ingredient name translation map
		boolean translationOk = ok && getIngredientnameTranslationMap();
		
		ok = ok && unitsOk && formsOk && translationOk;
		/* 2020-05-20 REPLACED BY END Translation */
		
		// Get CDM Ingredients
		ok = ok && getCDMData(database);		
		
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
							ingredientName        = StringUtilities.removeExtraSpaces(ingredientName);
							ingredientNameEnglish = StringUtilities.removeExtraSpaces(ingredientNameEnglish);
							/* 2020-05-20 REMOVED END Translation */
							dosage                = StringUtilities.removeExtraSpaces(dosage);
							dosageUnit            = StringUtilities.removeExtraSpaces(dosageUnit);
							casNumber = uniformCASNumber(casNumber);

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
				report.add("Source drugs with a miminum use count of " + Long.toString(minimumUseCount) + ": " + percentage((long) sourceDrugs.size(), (long) sourceDrugCount));
				report.add("Source drugs without ATC: " + percentage((long) noATCCounter, (long) sourceDrugs.size()));
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
		unitConversionsMap = new UnitConversion(database, units);
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
	
	
	private boolean getFormConversion(CDMDatabase database) {
		boolean ok = true;
		
		formConversionsMap = new FormConversion(database, forms);
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
	
	
	private boolean getCDMData(CDMDatabase database) {
		boolean ok = true;
		String fileName = "";
		
		QueryParameters queryParameters = new QueryParameters();
		queryParameters.set("@vocab", database.getVocabSchema());
	
		// Connect to the database
		RichConnection connection = database.getRichConnection(DrugMapping.class);
		
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
		for (Row queryRow : connection.queryResource("cdm/GetRxNormIngredients.sql", queryParameters)) {
			String cdmIngredientConceptId = queryRow.get("concept_id", true).trim();
			if ((lastCdmIngredient == null) || (!lastCdmIngredient.getConceptId().equals(cdmIngredientConceptId))) {
				if ((rxNormIngredientsFile != null) && (lastCdmIngredient != null)) {
					rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
				}
				CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
				if (cdmIngredient == null) {
					cdmIngredient = new CDMIngredient(queryRow, "");
					cdmIngredients.put(cdmIngredient.getConceptId(), cdmIngredient);
					cdmIngredientsList.add(cdmIngredient);
					
/* REPLACE BEGIN 2020-05-19 */
					String cdmIngredientNameNoSpaces = cdmIngredient.getConceptName();
					if (cdmIngredientNameNoSpaces.contains("(")) {
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.substring(0, cdmIngredientNameNoSpaces.indexOf("(")).trim();
					}
					while (cdmIngredientNameNoSpaces.contains(",")) 
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll(",", "");
					while (cdmIngredientNameNoSpaces.contains("-")) 
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll("-", "");
					while (cdmIngredientNameNoSpaces.contains(" ")) 
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll(" ", "");
					String cdmIngredientNameModified = modifyName(cdmIngredient.getConceptName());
					
					List<String> nameList = new ArrayList<String>();
					nameList.add(cdmIngredient.getConceptName());
					nameList.add(cdmIngredientNameNoSpaces);
					nameList.add(modifyName(cdmIngredientNameModified));
/* REPLACE END 2020-05-19 */
/* REPLACED BY BEGIN 2020-05-19 
					List<String> nameList = new ArrayList<String>();
					nameList.add(cdmIngredient.getConceptName());
					nameList.add(modifyName(cdmIngredient.getConceptName()));
/* REPLACED BY END 2020-05-19 */					
					
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
			
			String cdmIngredientSynonym = queryRow.get("concept_synonym_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String cdmIngredientSynonymNoSpaces = cdmIngredientSynonym;
			if (cdmIngredientSynonymNoSpaces.contains("(")) {
				cdmIngredientSynonymNoSpaces = cdmIngredientSynonymNoSpaces.substring(0, cdmIngredientSynonymNoSpaces.indexOf("(")).trim();
			} 
			while (cdmIngredientSynonymNoSpaces.contains(",")) 
				cdmIngredientSynonymNoSpaces = cdmIngredientSynonymNoSpaces.replaceAll(",", "");
			while (cdmIngredientSynonymNoSpaces.contains("-")) 
				cdmIngredientSynonymNoSpaces = cdmIngredientSynonymNoSpaces.replaceAll("-", "");
			while (cdmIngredientSynonymNoSpaces.contains(" ")) 
				cdmIngredientSynonymNoSpaces = cdmIngredientSynonymNoSpaces.replaceAll(" ", "");

			List<String> nameList = new ArrayList<String>();
			nameList.add(cdmIngredientSynonym);
			nameList.add(cdmIngredientSynonymNoSpaces);
			nameList.add(modifyName(cdmIngredientSynonym));
			
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
		
		// Get "%RxNorm eq" RxNorm Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM '%RxNorm eq' RxNorm Ingredients ...");
		
		for (Row queryRow : connection.queryResource("cdm/GetRxNormEquivalentIngredients.sql", queryParameters)) {
			//String drugConceptId = queryRow.get("drug_concept_id", true).trim();
			String drugConceptName = queryRow.get("drug_concept_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String cdmIngredientConceptId = queryRow.get("equivalent_concept_id", true).trim();
			String drugNameSynonym = queryRow.get("drug_synonym_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			
			if (cdmIngredient != null) {
				String drugNameNoSpaces = drugConceptName;
				if (drugNameNoSpaces.contains("(")) {
					drugNameNoSpaces = drugNameNoSpaces.substring(0, drugNameNoSpaces.indexOf("(")).trim();
				} 
				while (drugNameNoSpaces.contains(",")) 
					drugNameNoSpaces = drugNameNoSpaces.replaceAll(",", "");
				while (drugNameNoSpaces.contains("-")) 
					drugNameNoSpaces = drugNameNoSpaces.replaceAll("-", "");
				while (drugNameNoSpaces.contains(" ")) 
					drugNameNoSpaces = drugNameNoSpaces.replaceAll(" ", "");
			
				String drugNameSynonymNoSpaces = drugNameSynonym;
				if (drugNameSynonymNoSpaces.contains("(")) {
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.substring(0, drugNameSynonymNoSpaces.indexOf("(")).trim();
				} 
				while (drugNameSynonymNoSpaces.contains(",")) 
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.replaceAll(",", "");
				while (drugNameSynonymNoSpaces.contains("-")) 
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.replaceAll("-", "");
				while (drugNameSynonymNoSpaces.contains(" ")) 
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.replaceAll(" ", "");
				
				
				List<String> nameList = new ArrayList<String>();
				nameList.add(drugConceptName);
				nameList.add(drugNameNoSpaces);
				nameList.add(drugNameSynonymNoSpaces);
				nameList.add(modifyName(drugConceptName));
				nameList.add(modifyName(drugNameSynonym));
				
				for (String name : nameList) {
					Set<CDMIngredient> nameIngredients = cdmEquivalentIngredientNameIndex.get(name); 
					if (nameIngredients == null) {
						nameIngredients = new HashSet<CDMIngredient>();
						cdmMapsToIngredientNameIndex.put(name, nameIngredients);
					}
					nameIngredients.add(cdmIngredient);
				}
			}
		}
		
		// Get "Maps to" RxNorm Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM 'Maps to' RxNorm Ingredients ...");
		
		for (Row queryRow : connection.queryResource("cdm/GetMapsToRxNormIngredients.sql", queryParameters)) {
			//String drugConceptId = queryRow.get("drug_concept_id", true).trim();
			String drugConceptName = queryRow.get("drug_concept_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String cdmIngredientConceptId = queryRow.get("mapsto_concept_id", true).trim();
			String drugNameSynonym = queryRow.get("drug_synonym_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			
			if (cdmIngredient != null) {
				String drugNameNoSpaces = drugConceptName;
				if (drugNameNoSpaces.contains("(")) {
					drugNameNoSpaces = drugNameNoSpaces.substring(0, drugNameNoSpaces.indexOf("(")).trim();
				} 
				while (drugNameNoSpaces.contains(",")) 
					drugNameNoSpaces = drugNameNoSpaces.replaceAll(",", "");
				while (drugNameNoSpaces.contains("-")) 
					drugNameNoSpaces = drugNameNoSpaces.replaceAll("-", "");
				while (drugNameNoSpaces.contains(" ")) 
					drugNameNoSpaces = drugNameNoSpaces.replaceAll(" ", "");
	
				String drugNameSynonymNoSpaces = drugNameSynonym;
				if (drugNameSynonymNoSpaces.contains("(")) {
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.substring(0, drugNameSynonymNoSpaces.indexOf("(")).trim();
				} 
				while (drugNameSynonymNoSpaces.contains(",")) 
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.replaceAll(",", "");
				while (drugNameSynonymNoSpaces.contains("-")) 
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.replaceAll("-", "");
				while (drugNameSynonymNoSpaces.contains(" ")) 
					drugNameSynonymNoSpaces = drugNameSynonymNoSpaces.replaceAll(" ", "");
				
				List<String> nameList = new ArrayList<String>();
				nameList.add(drugConceptName);
				nameList.add(drugNameNoSpaces);
				nameList.add(drugNameSynonymNoSpaces);
				nameList.add(modifyName(drugConceptName));
				nameList.add(modifyName(drugNameSynonym));
				
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
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Get RxNorm Clinical Drugs with Form and Ingredients
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM concepts replaced by RxNorm ingredients ...");
		
		for (Row queryRow : connection.queryResource("cdm/GetConceptReplacedByRxNormIngredient.sql", queryParameters)) {
			String cdmReplacedByName      = queryRow.get("replaced_concept_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String cdmReplacedConceptId   = queryRow.get("replaced_concept_id", true);
			String cdmReplacedByConceptId = queryRow.get("replaced_by_concept_id", true);
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmReplacedByConceptId);
			if (cdmIngredient != null) {
				String cdmReplacedNameNoSpaces = cdmReplacedByName;
				if (cdmReplacedNameNoSpaces.contains("(")) {
					cdmReplacedNameNoSpaces = cdmReplacedNameNoSpaces.substring(0, cdmReplacedNameNoSpaces.indexOf("(")).trim();
				} 
				while (cdmReplacedNameNoSpaces.contains(",")) 
					cdmReplacedNameNoSpaces = cdmReplacedNameNoSpaces.replaceAll(",", "");
				while (cdmReplacedNameNoSpaces.contains("-")) 
					cdmReplacedNameNoSpaces = cdmReplacedNameNoSpaces.replaceAll("-", "");
				while (cdmReplacedNameNoSpaces.contains(" ")) 
					cdmReplacedNameNoSpaces = cdmReplacedNameNoSpaces.replaceAll(" ", "");
				String cdmReplacedNameModified = modifyName(cdmReplacedByName);
			
				List<String> nameList = new ArrayList<String>();
				if (!cdmReplacedByName.equals("")) {
					nameList.add(cdmReplacedByName);
				}
				if (!cdmReplacedNameNoSpaces.equals("")) {
					nameList.add(cdmReplacedNameNoSpaces);
				}
				if (!cdmReplacedNameModified.equals("")) {
					nameList.add(cdmReplacedNameModified);
				}
				
				for (String name : nameList) {
					Set<CDMIngredient> nameIngredients = cdmReplacedByIngredientNameIndex.get(name); 
					if (nameIngredients == null) {
						nameIngredients = new HashSet<CDMIngredient>();
						cdmReplacedByIngredientNameIndex.put(name, nameIngredients);
					}
					nameIngredients.add(cdmIngredient);
				}
				
				cdmReplacedByIngredientConceptIdIndex.put(cdmReplacedConceptId, cdmIngredient);
			} 
		}
		
		for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
			Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);

			for (String ingredientName : ingredientNameIndex.keySet()) {
				Set<CDMIngredient> ingredientNameIngredients = ingredientNameIndex.get(ingredientName);
				if ((ingredientNameIngredients != null) && (ingredientNameIngredients.size() > 1) && (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS))) {
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
		
		for (Row queryRow : connection.queryResource("cdm/GetRxNormClinicalDrugsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drug_concept_id", true);
			
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrug = cdmDrugs.get(cdmDrugConceptId);
				if (cdmDrug == null) {
					cdmDrug = new CDMDrug(queryRow, "drug_");
					cdmDrugs.put(cdmDrug.getConceptId(), cdmDrug);
				}
				String cdmFormConceptId = queryRow.get("form_concept_id", true);
				cdmDrug.addForm(cdmFormConceptId);
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
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
		
		for (Row queryRow : connection.queryResource("cdm/GetRxNormClinicalDrugCompsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drugcomp_concept_id", true);
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrugComp = cdmDrugComps.get(cdmDrugConceptId);
				if (cdmDrugComp == null) {
					cdmDrugComp = new CDMDrug(queryRow, "drugcomp_");
					cdmDrugComps.put(cdmDrugComp.getConceptId(), cdmDrugComp);
				}
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
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
		
		for (Row queryRow : connection.queryResource("cdm/GetRxNormClinicalDrugFormsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drugform_concept_id", true);
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrugForm = cdmDrugForms.get(cdmDrugConceptId);
				if (cdmDrugForm == null) {
					cdmDrugForm = new CDMDrug(queryRow, "drugform_");
					cdmDrugForms.put(cdmDrugForm.getConceptId(), cdmDrugForm);
				}
				
				String cdmFormConceptId = queryRow.get("form_concept_id", true);
				if ((cdmFormConceptId != null) && (!cdmFormConceptId.equals(""))) {
					cdmDrugForm.addForm(cdmFormConceptId);
				}
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
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
		
		for (Row queryRow : connection.queryResource("cdm/GetRxNormIngredientATC.sql", queryParameters)) {
			String cdmIngredientConceptId = queryRow.get("concept_id", true);
			String cdmIngredientATC = queryRow.get("atc", true);
			
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
		
		report.add("CDM Ingredients with ATC: " + percentage((long) atcCount, (long) cdmIngredients.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Get CAS code to CDM RxNorm (Extension) Ingredient mapping
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM CAS number to Ingredient mapping ...");
		
		Integer casCount = 0;
		for (Row queryRow : connection.queryResource("cdm/GetCASMapsToRxNormIngredients.sql", queryParameters)) {
			String cdmCASNr = uniformCASNumber(queryRow.get("casnr", true));
			String cdmIngredientConceptId = queryRow.get("concept_id", true);
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			CDMIngredient previousIngredient = cdmCASIngredientMap.get(cdmCASNr);
			
			CDMIngredient preferredIngredient = null;
			if (previousIngredient != null) {
				if (preferredIngredient == null) {
					if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("None")) {
						String preferredVocabulary_id = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM);

						if (cdmIngredient.getVocabularyId().equals(preferredVocabulary_id)) {
							if (!previousIngredient.getVocabularyId().equals(preferredVocabulary_id)) {
								preferredIngredient = cdmIngredient;
							}
						}
						else if (previousIngredient.getVocabularyId().equals(preferredVocabulary_id)) {
							preferredIngredient = previousIngredient;
						}
					}
				}
				if (preferredIngredient == null) {
					if ((!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No"))) {
						boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");

						Integer date = Integer.parseInt(cdmIngredient.getValidStartDate().replaceAll("-",""));
						Integer previousDate = Integer.parseInt(previousIngredient.getValidStartDate().replaceAll("-",""));
						
						if (latest) {
							if (date > previousDate) {
								preferredIngredient = cdmIngredient;
							}
							else if (previousDate > date) {
								preferredIngredient = lastCdmIngredient;
							}
						}
						else {
							if (date > previousDate) {
								preferredIngredient = previousIngredient;
							}
							else if (previousDate > date) {
								preferredIngredient = cdmIngredient;
							}
						}
					}
				}
				if (preferredIngredient == null) {
					if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No")) {
						boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
						
						Integer concept_id = Integer.parseInt(cdmIngredient.getConceptId());
						Integer previousConcept_id = Integer.parseInt(previousIngredient.getConceptId());
						
						if (oldest) {
							if (concept_id < previousConcept_id) {
								preferredIngredient = cdmIngredient;
							}
							else if (previousConcept_id < concept_id) {
								preferredIngredient = lastCdmIngredient;
							}
						}
						else {
							if (concept_id < previousConcept_id) {
								preferredIngredient = lastCdmIngredient;
							}
							else if (previousConcept_id < concept_id) {
								preferredIngredient = cdmIngredient;
							}
						}
					}
				}
				if (preferredIngredient == null) {
					if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST).equals("None")) {
						boolean first = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("First");
						
						if (first) {
							preferredIngredient = lastCdmIngredient;
						}
						else {
							preferredIngredient = cdmIngredient;
						}
					}
				}
			}
			// Fall back to last
			if (preferredIngredient == null) {
				preferredIngredient = cdmIngredient;
			}
			if (cdmCASIngredientMap.get(cdmCASNr) == null) {
				cdmCASIngredientMap.put(cdmCASNr, preferredIngredient);
				casCount++;
			}
		}
		
		report.add("CDM Ingredients with CAS number: " + percentage((long) casCount, (long) cdmIngredients.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		// Close database connection
		connection.close();
		
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
						
						String casNumber = StringUtilities.removeExtraSpaces(manualMappingFile.get(row, "CASNumber", true));
						String cdmConceptId = StringUtilities.removeExtraSpaces(manualMappingFile.get(row, "concept_id", true));
						//String cdmConceptName = manualMappingFile.get(row, "concept_name", true).trim();
						
						if (!casNumber.equals("")) {
							casNumber = uniformCASNumber(casNumber);
							
							if (!cdmConceptId.equals("")) {
								CDMIngredient cdmIngredient = cdmIngredients.get(cdmConceptId);
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
						
						String sourceCode = StringUtilities.removeExtraSpaces(manualMappingFile.get(row, "SourceCode", true));
						String sourceName = StringUtilities.removeExtraSpaces(manualMappingFile.get(row, "SourceName", true));
						String cdmConceptId = StringUtilities.removeExtraSpaces(manualMappingFile.get(row, "concept_id", true));
						//String cdmConceptName = manualMappingFile.get(row, "concept_name", true).trim();
						//String comment = manualMappingFile.get(row, "Comment", true).trim();

						String remark = null;
						if (!cdmConceptId.equals("")) {
							CDMIngredient cdmIngredient = cdmIngredients.get(cdmConceptId);
							if (cdmIngredient == null) { // Get replacing ingredient
								cdmIngredient = cdmReplacedByIngredientConceptIdIndex.get(cdmConceptId);
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
		Integer multipleMappings = 0;
		
		multipleMappings += matchIngredientsByCASNumber();
		
		multipleMappings += matchIngredientsByName();
		
		multipleMappings += matchIngredientsByATC();

		report.add("Source ingredients mapped total: " + percentage((long) ingredientMap.size(), (long) SourceDrug.getAllIngredients().size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		report.add("Multiple mappings found: " + percentage((long) multipleMappings, (long) SourceDrug.getAllIngredients().size()));
		
		return ok;
	}
	
	
	private Integer matchIngredientsByCASNumber() {
		Integer matchedManualByCASNumber = 0;
		Integer matchedByCDMCASNumber = 0;
		Integer matchedByExternalCASName = 0;
		Integer multipleMappings = 0;

		if (cdmCASIngredientMap.size() > 0) {
			System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by CDM CAS number ...");
			
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
							cdmIngredient = cdmCASIngredientMap.get(casNr);
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
			PrintWriter casIngredientMappingFile = openOutputFile("IngredientMapping by CAS.csv", header);
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
						CDMIngredient cdmIngredient = cdmIngredients.get(concept_id);
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

			report.add("Source ingredients mapped manually by CAS number: " + percentage((long) matchedManualByCASNumber, (long) SourceDrug.getAllIngredients().size()));
			report.add("Source ingredients mapped by CDM CAS number: " + percentage((long) matchedByCDMCASNumber, (long) SourceDrug.getAllIngredients().size()));
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
		else {
			System.out.println(DrugMapping.getCurrentTime() + "     No CDM CAS mapping found.");
		}
		
		
		if (externalCASSynonymsMap != null) {
			System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by external CAS number ...");
			
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				if (sourceIngredient.getMatchingIngredient() == null) {

					boolean matchFound = false;
					boolean multipleMapping = false;
					
					for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
						Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);
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

			report.add("Source ingredients mapped by external CAS number: " + percentage((long) matchedByExternalCASName, (long) SourceDrug.getAllIngredients().size()));
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
		
		return multipleMappings;
	}
	
	
	private Integer matchIngredientsByName() {
		Integer matchedManualCode = 0;
		Integer matchedManualName = 0;
		Integer matchedByName = 0;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by name ...");
		
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
					String sourceIngredientName = StringUtilities.removeExtraSpaces(sourceIngredient.getIngredientName()) + " XXX";
					do {
						sourceIngredientName = sourceIngredientName.substring(0, Math.max(sourceIngredientName.lastIndexOf(" "),Math.max(sourceIngredientName.lastIndexOf("/"), sourceIngredientName.lastIndexOf(","))));
						cdmIngredient = manualIngredientNameMappings.get(sourceIngredientName);
					} while ((cdmIngredient == null) && (sourceIngredientName.contains(" ") || sourceIngredientName.contains("/") || sourceIngredientName.contains(",")));
					
					if (cdmIngredient != null) { // Manual mapping on part ingredient name found
						ingredientMap.put(sourceIngredient, cdmIngredient);
						sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
						sourceIngredient.setMatchString("MANUAL NAME: " + sourceIngredientName);
						matchedManualName++;
					}
					else {
						List<String> matchNameList = sourceIngredient.getIngredientMatchingNames();

						boolean matchFound = false;
						boolean multipleMapping = false;
						
						for (String matchName : matchNameList) {
							String matchType = matchName.substring(0, matchName.indexOf(": ") + 2);
							matchName = matchName.substring(matchName.indexOf(": ") + 2);
							
							for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
								Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);
								boolean preferencesUsed = false;
								
								Set<CDMIngredient> matchedCDMIngredients = ingredientNameIndex.get(matchName);
								if (matchedCDMIngredients != null) {
									if (matchedCDMIngredients.size() > 1) {
										String vocabulary_id = null;
										if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
											vocabulary_id = "RxNorm";
										}
										else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
											vocabulary_id = "RxNorm Extension";
										}
										if (vocabulary_id != null) {
											preferencesUsed = true;
											List<CDMIngredient> remove = new ArrayList<CDMIngredient>();
											for (CDMIngredient ingredient : matchedCDMIngredients) {
												if (!ingredient.getVocabularyId().equals(vocabulary_id)) {
													remove.add(ingredient);
												}
											}
											if ((remove.size() > 0) && (matchedCDMIngredients.size() != remove.size())) {
												matchedCDMIngredients.removeAll(remove);
											}
										}
									}

									if (matchedCDMIngredients.size() > 1) {
										if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("No")) {
											preferencesUsed = true;
											boolean latest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_DATE).equals("Latest");
											List<CDMIngredient> remove = new ArrayList<CDMIngredient>();
											List<CDMIngredient> lastIngredients = new ArrayList<CDMIngredient>();
											int lastDate = -1;
											for (CDMIngredient ingredient : matchedCDMIngredients) {
												try {
													Integer date = Integer.parseInt(ingredient.getValidStartDate().replaceAll("-",""));
													if (lastDate == -1) {
														lastIngredients.add(ingredient);
														lastDate = date;
													}
													else {
														if (latest ? (date > lastDate) : (date < lastDate)) {
															remove.addAll(lastIngredients);
															lastIngredients.clear();
															lastIngredients.add(ingredient);
															lastDate = date;
														}
														else if (date == lastDate) {
															lastIngredients.add(ingredient);
														}
														else {
															remove.add(ingredient);
														}
													}
												}
												catch (NumberFormatException e) {
													remove.add(ingredient);
												}
											}
											if ((remove.size() > 0) && (matchedCDMIngredients.size() != remove.size())) {
												matchedCDMIngredients.removeAll(remove);
											}
										}
									}
									
									if (matchedCDMIngredients.size() > 1) {
										if (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No")) {
											preferencesUsed = true;
											boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
											
											List<CDMIngredient> remove = new ArrayList<CDMIngredient>();
											CDMIngredient lastDrug = null;
											int lastConceptId = Integer.MAX_VALUE; 
											for (CDMIngredient cdmDrug : matchedCDMIngredients) {
												if (lastDrug == null) {
													lastDrug = cdmDrug;
													lastConceptId = Integer.parseInt(cdmDrug.getConceptId()); 
												}
												else {
													int conceptId = Integer.parseInt(cdmDrug.getConceptId());
													if ((oldest && (conceptId < lastConceptId)) || ((!oldest) && (conceptId > lastConceptId))) {
														lastConceptId = conceptId;
														remove.add(lastDrug);
														lastDrug = cdmDrug;
													}
													else {
														remove.add(cdmDrug);
													}
												}
											}
											if ((remove.size() > 0) && (matchedCDMIngredients.size() != remove.size())) {
												matchedCDMIngredients.removeAll(remove);
											}
										}
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
										sourceIngredient.setMatchString(matchType + ingredientNameIndexName + " " + matchName + (preferencesUsed ? " - USED PREFERENCES" : ""));
										matchFound = true;
										matchedByName++;
										break;
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

		report.add("Source ingredients mapped manually by code: " + percentage((long) matchedManualCode, (long) SourceDrug.getAllIngredients().size()));
		report.add("Source ingredients mapped manually by name: " + percentage((long) matchedManualName, (long) SourceDrug.getAllIngredients().size()));
		report.add("Source ingredients mapped by name: " + percentage((long) matchedByName, (long) SourceDrug.getAllIngredients().size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
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

		report.add("Source ingredients mapped by ATC: " + percentage((long) matchedByATC, (long) SourceDrug.getAllIngredients().size()));
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
							Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
								Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(0);
							
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
					while ((mapping != INGREDIENT_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
						Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						
						List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
						if (mappingTypeResultsList == null) {
							mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
							Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
							mappingTypeResultsList.add(mappingTypeResults);
							sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
						}
						
						Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(0);
						
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
				while ((mapping != INGREDIENT_MAPPING) && mappingTypeDescriptions.containsKey(mapping)) {
					Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
					if (sourceDrugMappingResult == null) {
						sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
						sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
					}
					
					List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
					if (mappingTypeResultsList == null) {
						mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
						Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
						mappingTypeResultsList.add(mappingTypeResults);
						sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
					}
					
					Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(0);
					
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
		
		report.add("Source drugs with all ingredients mapped: " + percentage((long) sourceDrugsAllIngredientsMapped.size(), (long) sourceDrugs.size()));
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

							Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
								Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
							
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

							Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
								Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
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
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
												mappingTypeResultsList.add(mappingTypeResults);
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(0);
											
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
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
												mappingTypeResultsList.add(mappingTypeResults);
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(0);
											
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
									if ((matchingCDMDrugsWithTwoUnits.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No"))) {
										boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
										resultType = oldest ? CLINICAL_DRUG_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : CLINICAL_DRUG_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE;
										
										List<CDMDrug> remove = new ArrayList<CDMDrug>();
										CDMDrug lastDrug = null;
										int lastConceptId = Integer.MAX_VALUE; 
										for (CDMDrug cdmDrug : matchingCDMDrugsWithTwoUnits) {
											if (lastDrug == null) {
												lastDrug = cdmDrug;
												lastConceptId = Integer.parseInt(cdmDrug.getConceptId()); 
											}
											else {
												int conceptId = Integer.parseInt(cdmDrug.getConceptId());
												if ((oldest && (conceptId < lastConceptId)) || ((!oldest) && (conceptId > lastConceptId))) {
													lastConceptId = conceptId;
													remove.add(lastDrug);
													lastDrug = cdmDrug;
												}
												else {
													remove.add(cdmDrug);
												}
											}
										}
										if ((remove.size() > 0) && (matchingCDMDrugsWithTwoUnits.size() != remove.size())) {
											matchingCDMDrugsWithTwoUnits.removeAll(remove);

											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
												mappingTypeResultsList.add(mappingTypeResults);
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(0);
											
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
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
													sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
													sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
												}
												
												mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
												if (mappingTypeResultsList == null) {
													mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
													Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
													mappingTypeResultsList.add(mappingTypeResults);
													sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
												}
												
												rejectedForMapping = mappingTypeResultsList.get(0);
												
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
													sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
													sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
												}
												
												mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
												if (mappingTypeResultsList == null) {
													mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
													Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
													mappingTypeResultsList.add(mappingTypeResults);
													sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
												}
												
												rejectedForMapping = mappingTypeResultsList.get(0);
												
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
										if ((matchingCDMDrugsWithOneUnit.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No"))) {
											boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
											resultType = oldest ? CLINICAL_DRUG_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : CLINICAL_DRUG_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE;

											List<CDMDrug> remove = new ArrayList<CDMDrug>();
											CDMDrug lastDrug = null;
											int lastConceptId = Integer.MAX_VALUE; 
											for (CDMDrug cdmDrug : matchingCDMDrugsWithOneUnit) {
												if (lastDrug == null) {
													lastDrug = cdmDrug;
													lastConceptId = Integer.parseInt(cdmDrug.getConceptId()); 
												}
												else {
													int conceptId = Integer.parseInt(cdmDrug.getConceptId());
													if ((oldest && (conceptId < lastConceptId)) || ((!oldest) && (conceptId > lastConceptId))) {
														lastConceptId = conceptId;
														remove.add(lastDrug);
														lastDrug = cdmDrug;
													}
													else {
														remove.add(cdmDrug);
													}
												}
											}
											if ((remove.size() > 0) && (matchingCDMDrugsWithOneUnit.size() != remove.size())) {
												matchingCDMDrugsWithOneUnit.removeAll(remove);

												sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
												if (sourceDrugMappingResult == null) {
													sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
													sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
												}
												
												mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
												if (mappingTypeResultsList == null) {
													mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
													Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
													mappingTypeResultsList.add(mappingTypeResults);
													sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
												}
												
												rejectedForMapping = mappingTypeResultsList.get(0);
												
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
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
												mappingTypeResultsList.add(mappingTypeResults);
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(0);
											
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
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
												mappingTypeResultsList.add(mappingTypeResults);
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(0);
											
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
						Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						
						List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
						if (mappingTypeResultsList == null) {
							mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
							Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
							mappingTypeResultsList.add(mappingTypeResults);
							sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
						}
						
						Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
						
						List<String> emptyList = new ArrayList<String>();
						emptyList.add(" ");
						rejectedForMapping.put(CLINICAL_DRUG_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS, emptyList);
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

				Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
				if (sourceDrugMappingResult == null) {
					sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
					sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
				}
				
				List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
				if (mappingTypeResultsList == null) {
					mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
					Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
					mappingTypeResultsList.add(mappingTypeResults);
					sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
				}
				
				Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(0);

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

		report.add("Source drugs mapped to multiple CDM Clinical Drugs: " + percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
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
						if ((cdmDrugCompsWithIngredients != null) && (cdmDrugCompsWithIngredients.size() > 0)) {
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
							Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
								Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);

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
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
								if ((matchingCDMDrugs.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No"))) {
									boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
									resultType = oldest ? CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : CLINICAL_DRUG_COMP_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE;

									List<CDMDrug> remove = new ArrayList<CDMDrug>();
									CDMDrug lastDrug = null;
									int lastConceptId = Integer.MAX_VALUE; 
									for (CDMDrug cdmDrug : matchingCDMDrugs) {
										if (lastDrug == null) {
											lastDrug = cdmDrug;
											lastConceptId = Integer.parseInt(cdmDrug.getConceptId()); 
										}
										else {
											int conceptId = Integer.parseInt(cdmDrug.getConceptId());
											if ((oldest && (conceptId < lastConceptId)) || ((!oldest) && (conceptId > lastConceptId))) {
												lastConceptId = conceptId;
												remove.add(lastDrug);
												lastDrug = cdmDrug;
											}
											else {
												remove.add(cdmDrug);
											}
										}
									}
									if ((remove.size() > 0) && (matchingCDMDrugs.size() != remove.size())) {
										matchingCDMDrugs.removeAll(remove);

										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
										sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									
									mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
									if (mappingTypeResultsList == null) {
										mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
										Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
										mappingTypeResultsList.add(mappingTypeResults);
										sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
									}
									
									rejectedForMapping = mappingTypeResultsList.get(0);
									
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
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
											mappingTypeResultsList.add(mappingTypeResults);
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(0);
										
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
							Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
								Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
							
							List<String> emptyList = new ArrayList<String>();
							emptyList.add(" ");
							rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS, emptyList);
						}
					}
					else {
						Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						
						List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
						if (mappingTypeResultsList == null) {
							mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
							Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
							mappingTypeResultsList.add(mappingTypeResults);
							sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
						}
						
						Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
						
						List<String> emptyList = new ArrayList<String>();
						emptyList.add(" ");
						rejectedForMapping.put(CLINICAL_DRUG_COMP_MAPPING_NO_SINGLE_INGREDIENT_DRUG, emptyList);
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
				Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
				if (sourceDrugMappingResult == null) {
					sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
					sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
				}
				
				List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
				if (mappingTypeResultsList == null) {
					mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
					Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
					mappingTypeResultsList.add(mappingTypeResults);
					sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
				}
				
				Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(0);

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

		report.add("Source drugs mapped to multiple CDM Clinical Drug Comps: " + percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
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
							Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
							if (sourceDrugMappingResult == null) {
								sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
								sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
							}
							
							List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
							if (mappingTypeResultsList == null) {
								mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
								Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
								mappingTypeResultsList.add(mappingTypeResults);
								sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
							}
							
							Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
							
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
									Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
									if (sourceDrugMappingResult == null) {
										sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									
									List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
									if (mappingTypeResultsList == null) {
										mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
										Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
										mappingTypeResultsList.add(mappingTypeResults);
										sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
									}
									
									Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
									
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
									Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
									if (sourceDrugMappingResult == null) {
										sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									
									List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
									if (mappingTypeResultsList == null) {
										mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
										Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
										mappingTypeResultsList.add(mappingTypeResults);
										sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
									}
									
									Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
									
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
							if ((cdmDrugsWithIngredients.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No"))) {
								boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
								resultType = oldest ? CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : CLINICAL_DRUG_FORM_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE;

								List<CDMDrug> remove = new ArrayList<CDMDrug>();
								CDMDrug lastDrug = null;
								int lastConceptId = Integer.MAX_VALUE; 
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									if (lastDrug == null) {
										lastDrug = cdmDrug;
										lastConceptId = Integer.parseInt(cdmDrug.getConceptId()); 
									}
									else {
										int conceptId = Integer.parseInt(cdmDrug.getConceptId());
										if ((oldest && (conceptId < lastConceptId)) || ((!oldest) && (conceptId > lastConceptId))) {
											lastConceptId = conceptId;
											remove.add(lastDrug);
											lastDrug = cdmDrug;
										}
										else {
											remove.add(cdmDrug);
										}
									}
								}
								if ((remove.size() > 0) && (cdmDrugsWithIngredients.size() != remove.size())) {
									cdmDrugsWithIngredients.removeAll(remove);

									Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
									if (sourceDrugMappingResult == null) {
										sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
										sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
									}
									
									List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
									if (mappingTypeResultsList == null) {
										mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
										Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
										mappingTypeResultsList.add(mappingTypeResults);
										sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
									}
									
									Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
									
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
								Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
								if (sourceDrugMappingResult == null) {
									sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
									sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
								}
								
								List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
								if (mappingTypeResultsList == null) {
									mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
									Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
									mappingTypeResultsList.add(mappingTypeResults);
									sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
								}
								
								Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
								
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
								Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
								if (sourceDrugMappingResult == null) {
									sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
									sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
								}
								
								List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
								if (mappingTypeResultsList == null) {
									mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
									Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
									mappingTypeResultsList.add(mappingTypeResults);
									sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
								}
								
								Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
								
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
						Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
						if (sourceDrugMappingResult == null) {
							sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
							sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
						}
						
						List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
						if (mappingTypeResultsList == null) {
							mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
							Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
							mappingTypeResultsList.add(mappingTypeResults);
							sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
						}
						
						Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(0);
						
						List<String> emptyList = new ArrayList<String>();
						emptyList.add(" ");
						rejectedForMapping.put(CLINICAL_DRUG_FORM_MAPPING_NO_DRUGS_WITH_MATCHING_INGREDIENTS, emptyList);
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
				Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
				if (sourceDrugMappingResult == null) {
					sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
					sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
				}
				
				List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
				if (mappingTypeResultsList == null) {
					mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
					Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
					mappingTypeResultsList.add(mappingTypeResults);
					sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
				}
				
				Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(0);

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

		report.add("Source drugs mapped to multiple CDM Clinical Drug Forms: " + percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
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
			
			if ((!mappedSourceDrugs.contains(sourceDrug)) && (!earlierNotUniqueMapping)) {
				List<SourceDrugComponent> sourceDrugComponents = sourceDrug.getComponents();
				if (sourceDrugComponents.size() > 0) {
					// Find CDM Clinical Drug Comps with corresponding ingredients
					for (int componentNr = 0; componentNr < sourceDrugComponents.size(); componentNr++) {
						SourceDrugComponent sourceDrugComponent = sourceDrugComponents.get(componentNr);
						SourceIngredient sourceDrugIngredient = sourceDrugComponent.getIngredient();
						String cdmIngredientConceptId = sourceDrugIngredient.getMatchingIngredient();
 						if (cdmIngredientConceptId != null) {
							CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
							List<CDMDrug> cdmDrugCompsWithIngredients = cdmDrugCompsContainingIngredient.get(cdmIngredient);

							// Find CDM Clinical Drug Comps with corresponding ingredient strengths
							if ((cdmDrugCompsWithIngredients != null) && (cdmDrugCompsWithIngredients.size() > 0)) {
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
								Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
								if (sourceDrugMappingResult == null) {
									sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
									sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
								}
								
								List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
								if (mappingTypeResultsList == null) {
									mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
									for (int ingredientNr = 0; ingredientNr < sourceDrugComponents.size(); ingredientNr++) {
										mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
									}
									sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
								}
								
								Map<Integer, List<String>> rejectedForMapping = mappingTypeResultsList.get(componentNr);

								rejectedForMapping.put(INGREDIENT_MAPPING_REJECTED_BY_STRENGTH, rejectedDrugs);

								if (matchingCDMDrugs.size() > 1) {
									String vocabulary_id = null;
									int resultType = -1;
									if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
										vocabulary_id = "RxNorm";
										resultType = INGREDIENT_MAPPING_REJECTED_BY_RXNORM_PREFERENCE;
									}
									else if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm Extension")) {
										vocabulary_id = "RxNorm Extension";
										resultType = INGREDIENT_MAPPING_REJECTED_BY_RXNORM_EXTENSION_PREFERENCE;
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
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												for (int ingredientNr = 0; ingredientNr < sourceDrugComponents.size(); ingredientNr++) {
													mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
												}
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(componentNr);
											
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
										resultType = latest ? INGREDIENT_MAPPING_REJECTED_BY_LATEST_DATE_PREFERENCE : INGREDIENT_MAPPING_REJECTED_BY_OLDEST_DATE_PREFERENCE;
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
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												for (int ingredientNr = 0; ingredientNr < sourceDrugComponents.size(); ingredientNr++) {
													mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
												}
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(componentNr);
											
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
									if ((matchingCDMDrugs.size() > 1) && (!DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("No"))) {
										boolean oldest = DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID).equals("Smallest (= oldest)");
										resultType = oldest ? INGREDIENT_MAPPING_REJECTED_BY_SMALLEST_CONCEPTID_PREFERENCE : INGREDIENT_MAPPING_REJECTED_BY_GREATEST_CONCEPTID_PREFERENCE;

										List<CDMDrug> remove = new ArrayList<CDMDrug>();
										CDMDrug lastDrug = null;
										int lastConceptId = Integer.MAX_VALUE; 
										for (CDMDrug cdmDrug : matchingCDMDrugs) {
											if (lastDrug == null) {
												lastDrug = cdmDrug;
												lastConceptId = Integer.parseInt(cdmDrug.getConceptId()); 
											}
											else {
												int conceptId = Integer.parseInt(cdmDrug.getConceptId());
												if ((oldest && (conceptId < lastConceptId)) || ((!oldest) && (conceptId > lastConceptId))) {
													lastConceptId = conceptId;
													remove.add(lastDrug);
													lastDrug = cdmDrug;
												}
												else {
													remove.add(cdmDrug);
												}
											}
										}
										if ((remove.size() > 0) && (matchingCDMDrugs.size() != remove.size())) {
											matchingCDMDrugs.removeAll(remove);

											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												Map<Integer, List<String>> mappingTypeResults = new HashMap<Integer, List<String>>();
												mappingTypeResultsList.add(mappingTypeResults);
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(0);
											
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
											automaticMappings.set(componentNr, matchingCDMDrugs.get(0));
											resultType = INGREDIENT_MAPPING_REJECTED_BY_FIRST_PREFERENCE;
											for (int nr = 1; nr < matchingCDMDrugs.size(); nr++) {
												remove.add(matchingCDMDrugs.get(nr));
											}
										}
										else {
											automaticMappings.set(componentNr, matchingCDMDrugs.get(matchingCDMDrugs.size() - 1));
											resultType = INGREDIENT_MAPPING_REJECTED_BY_LAST_PREFERENCE;
											for (int nr = 0; nr < (matchingCDMDrugs.size() - 1); nr++) {
												remove.add(matchingCDMDrugs.get(nr));
											}
										}
										matchingCDMDrugs.removeAll(remove);

										sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
										if (sourceDrugMappingResult == null) {
											sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
											sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
										}
										
										mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
										if (mappingTypeResultsList == null) {
											mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
											for (int ingredientNr = 0; ingredientNr < sourceDrugComponents.size(); ingredientNr++) {
												mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
											}
											sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
										}
										
										rejectedForMapping = mappingTypeResultsList.get(componentNr);
										
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
											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												for (int ingredientNr = 0; ingredientNr < sourceDrugComponents.size(); ingredientNr++) {
													mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
												}
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(componentNr);
											
											List<String> multipleMappings = rejectedForMapping.get(INGREDIENT_MAPPING_NO_UNIQUE_MAPPING);
											if (multipleMappings == null) {
												multipleMappings = new ArrayList<String>();
												rejectedForMapping.put(INGREDIENT_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
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
											automaticMappings.set(componentNr, matchingCDMDrugs.get(0));
										}
										else {
											sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
											if (sourceDrugMappingResult == null) {
												sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
												sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
											}
											
											mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
											if (mappingTypeResultsList == null) {
												mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
												for (int ingredientNr = 0; ingredientNr < sourceDrugComponents.size(); ingredientNr++) {
													mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
												}
												sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
											}
											
											rejectedForMapping = mappingTypeResultsList.get(componentNr);
											
											List<String> multipleMappings = rejectedForMapping.get(INGREDIENT_MAPPING_NO_UNIQUE_MAPPING);
											if (multipleMappings == null) {
												multipleMappings = new ArrayList<String>();
												rejectedForMapping.put(INGREDIENT_MAPPING_NO_UNIQUE_MAPPING, multipleMappings);
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
						}
						
						// Try mapping to CDM Ingredient
						if (automaticMappings.get(componentNr) == null) {
							if (sourceDrugIngredient.getMatchingIngredient() != null) {
								CDMIngredient cdmIngredient = cdmIngredients.get(sourceDrugIngredient.getMatchingIngredient());
								automaticMappings.set(componentNr, cdmIngredient);
							}
						}
					}
				}
			}

			// Check for manual mapping and set final mapping.
			boolean mapped = false;
			List<String> finalMappings = new ArrayList<String>();
			List<String> overruledMappings = new ArrayList<String>();
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
						finalMappings.add(finalMapping.toString());
						mapped = true;
					}
					else {
						finalMappings.add("");
					}
					if (overruledMapping != null) {
						overruledMappings.add(overruledMapping.toString());
					}
					else {
						overruledMappings.add("");
					}
				}
				else {
					finalMappings.add("");
					overruledMappings.add("");
				}
			}

			if (mapped) {
				for (int finalMappingNr = 0; finalMappingNr < finalMappings.size(); finalMappingNr++) {
					Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappingResult = sourceDrugMappingResults.get(sourceDrug);
					if (sourceDrugMappingResult == null) {
						sourceDrugMappingResult = new HashMap<Integer, List<Map<Integer, List<String>>>>();
						sourceDrugMappingResults.put(sourceDrug, sourceDrugMappingResult);
					}
					
					List<Map<Integer, List<String>>> mappingTypeResultsList = sourceDrugMappingResult.get(mapping);
					if (mappingTypeResultsList == null) {
						mappingTypeResultsList = new ArrayList<Map<Integer, List<String>>>();
						for (int ingredientNr = 0; ingredientNr < automaticMappings.size(); ingredientNr++) {
							mappingTypeResultsList.add(new HashMap<Integer, List<String>>());
						}
						sourceDrugMappingResult.put(mapping, mappingTypeResultsList);
					}
					
					Map<Integer, List<String>> mappingTypeResults = mappingTypeResultsList.get(finalMappingNr);

					mappedSourceDrugs.add(sourceDrug);
					
					List<String> acceptedMappingList = mappingTypeResults.get(INGREDIENT_MAPPING_MAPPED);
					if (acceptedMappingList == null) {
						acceptedMappingList = new ArrayList<String>();
						mappingTypeResults.put(INGREDIENT_MAPPING_MAPPED, acceptedMappingList);
					}
					acceptedMappingList.add(finalMappings.get(finalMappingNr));

					List<String> overruledMappingList = mappingTypeResults.get(INGREDIENT_MAPPING_OVERRULED_MAPPING);
					if (overruledMappingList == null) {
						overruledMappingList = new ArrayList<String>();
						mappingTypeResults.put(INGREDIENT_MAPPING_OVERRULED_MAPPING, overruledMappingList);
					}
					overruledMappingList.add(overruledMappings.get(finalMappingNr));
				}
			}

		}

		report.add("Source drugs mapped to multiple CDM Clinical Drug Comp or CDM Ingredient combinations: " + percentage((long) notUniqueMapping.get(mapping).size(), (long) sourceDrugs.size()));
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private void saveMapping() {
		// Save ingredient mapping
		PrintWriter ingredientMappingFile = openOutputFile("IngredientMapping Results.csv", SourceIngredient.getMatchHeader() + ",SourceCount," + CDMIngredient.getHeader());
		
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
					/*
					int compare = -Long.compare(ingredient1.getCount(), ingredient2.getCount());
					if (compare == 0) {
						if (ingredient1.getIngredientCode() == null) {
							if (ingredient2.getIngredientCode() == null) {
								compare = 0;
							}
							else {
								compare = 1;
							}
						}
						else {
							if (ingredient2.getIngredientCode() == null) {
								compare = -1;
							}
							else {
								compare = ingredient1.getIngredientCode().compareTo(ingredient2.getIngredientCode());
							}
						}
					}
					return 0;
					*/
				}
			});
			
			for (SourceIngredient sourceIngredient : sourceIngredients) {
				if (sourceIngredient.getMatchingIngredient() != null) {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + sourceIngredient.getCount() + "," + cdmIngredients.get(sourceIngredient.getMatchingIngredient()));
				}
				else {
					ingredientMappingFile.println(sourceIngredient.toMatchString() + "," + sourceIngredient.getCount() + "," + CDMIngredient.emptyRecord());
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
		header += "," + SourceIngredient.getHeader();
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
				String mappingStatus = manualDrugMappings.containsKey(sourceDrug) ? "ManualMapping" : (mappedSourceDrugs.contains(sourceDrug) ? "Mapped" : "Unmapped");
				Map<Integer, List<Map<Integer, List<String>>>> sourceDrugMappings = sourceDrugMappingResults.get(sourceDrug);
				
				if (sourceDrugMappings == null) {
					System.out.println("ERROR: " + sourceDrug);
				}

				mappingType = 0;
				while (mappingTypeDescriptions.containsKey(mappingType)) {
					List< Map<Integer, List<String>>> mappingResultList = sourceDrugMappings.get(mappingType);
					if ((mappingResultList != null) && (mappingResultList.size() > 0)) {
						Set<Integer> counted = new HashSet<Integer>();
						for (int ingredientNr = 0; ingredientNr < mappingResultList.size(); ingredientNr++) {
							Map<Integer, List<String>> mappingResult = mappingResultList.get(ingredientNr); 
							String sourceIngredientString = "*,*,*,*"; // Mapping on source drug
							if (mappingType == INGREDIENT_MAPPING) {   // Mapping on source drug ingredients
								sourceIngredientString = sourceDrug.getIngredients().get(ingredientNr).toString();
							}
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
									String record = DrugMapping.escapeFieldValue(mappingStatus);
									record += "," + sourceDrug;
									record += "," + sourceIngredientString;
									record += "," + DrugMapping.escapeFieldValue(mappingTypeDescriptions.get(mappingType));
									record += "," + DrugMapping.escapeFieldValue(mappingResultDescriptions.get(mappingResultType));
									
									List<String> results = mappingResult.get(mappingResultType);
									if ((results != null) && (results.size() > 0)) {
										if (mappingResultType == mappedResultType) {
											record += "," + results.get(0);
										}
										else {
											Collections.sort(results);
											for (String result : results) {
												record += "," + DrugMapping.escapeFieldValue(result);
											}
										}
										drugMappingFile.println(record);
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
			
			closeOutputFile(drugMappingFile);
			
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
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_MAPPING).get(0).get(CLINICAL_DRUG_MAPPING_MAPPED) != null) {
					mappingClinicalDrugs++;
					dataCoverageClinicalDrugs += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_COMP_MAPPING).get(0).get(CLINICAL_DRUG_COMP_MAPPING_MAPPED) != null) {
					mappingClinicalDrugComps++;
					dataCoverageClinicalDrugComps += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING) != null) {
				if (sourceDrugMappingResults.get(sourceDrug).get(CLINICAL_DRUG_FORM_MAPPING).get(0).get(CLINICAL_DRUG_FORM_MAPPING_MAPPED) != null) {
					mappingClinicalDrugForms++;
					dataCoverageClinicalDrugForms += sourceDrug.getCount();
				}
			}
			if (sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING) != null) {
				List<Map<Integer, List<String>>> mappingResultsList = sourceDrugMappingResults.get(sourceDrug).get(INGREDIENT_MAPPING);
				if (mappingResultsList != null) {
					for (Map<Integer, List<String>> mappingResult : mappingResultsList) {
						if (mappingResult.get(INGREDIENT_MAPPING_MAPPED) != null) {
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
		report.add("Source drugs mapped to single CDM Clinical Drug: " + percentage((long) mappingClinicalDrugs, (long) sourceDrugs.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp: " + percentage((long) mappingClinicalDrugComps, (long) sourceDrugs.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Form: " + percentage((long) mappingClinicalDrugForms, (long) sourceDrugs.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp/Ingredient: " + percentage((long) mappingClinicalDrugCompsIngredients, (long) sourceDrugs.size()));
		report.add("Total Source drugs mapped: " + percentage((long) mappingTotal, (long) sourceDrugs.size()));
		
		if (dataCountTotal != 0) {
			report.add("");
			report.add("Datacoverage Source drugs with all ingredients mapped: " + percentage((long) dataCoverageIngredients, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug mapping: " + percentage((long) dataCoverageClinicalDrugs, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Comp mapping: " + percentage((long) dataCoverageClinicalDrugComps, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Form mapping: " + percentage((long) dataCoverageClinicalDrugForms, (long) dataCountTotal));
			report.add("Datacoverage CDM Clinical Drug Comp/Ingredient mapping: " + percentage((long) dataCoverageClinicalDrugCompsIngredients, (long) dataCountTotal));
			report.add("Total datacoverage drug mapping: " + percentage((long) dataCoverageTotal, (long) dataCountTotal));
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
	
	
	private String percentage(Long count, Long total) {
		return count + " of " + total + " (" + Double.toString((double) Math.round((((double) (count) / (double) total) * 1000)) / 10.0) + "%)"; 
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
