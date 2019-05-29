package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class MapGenericDrugs extends Mapping {
	
	
	public MapGenericDrugs(CDMDatabase database, InputFile unitMappingsFile, InputFile genericDrugsFile) {
				
		String fileName = "";
		try {
			// Create all output files
			System.out.println(DrugMapping.getCurrentTime() + " Creating output files ...");
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
			System.out.println("    " + fileName);
			PrintWriter missingATCFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(missingATCFile);
			System.out.println(DrugMapping.getCurrentTime() + " Finished");


			System.out.println(DrugMapping.getCurrentTime() + " Loading unit mappings ...");
			Map<String, UnitConversion> unitConversionsMap = new HashMap<String, UnitConversion>();
			if (unitMappingsFile.openFile()) {
				while (unitMappingsFile.hasNext()) {
					Row row = unitMappingsFile.next();

					String localUnitName = unitMappingsFile.get(row, "LocalUnit").trim();
					UnitConversion unitConversion = new UnitConversion(localUnitName);
					
					for (int conversionNr = 1; conversionNr <= 10; conversionNr++) {
						if (unitMappingsFile.hasField("concept_id_" + Integer.toString(conversionNr)) && unitMappingsFile.hasField("factor_" + Integer.toString(conversionNr))) {
							String concept_id = unitMappingsFile.get(row, "concept_id_" + Integer.toString(conversionNr));
							String factor     = unitMappingsFile.get(row, "factor_" + Integer.toString(conversionNr));
							
							if ((concept_id != null) && (factor != null)) {
								concept_id = concept_id.trim();
								factor     = factor.trim();
								unitConversion.addConversion(concept_id, factor);
							}
						}
					}
					
					unitConversionsMap.put(localUnitName, unitConversion);
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + " Finished");


			System.out.println(DrugMapping.getCurrentTime() + " Loading generic drugs ...");
			
			// Read the generic drugs file
			List<List<Map<String, String>>> genericDrugs = new ArrayList<List<Map<String, String>>>();
			List<List<Map<String, String>>> noIngredientGenericDrugs = new ArrayList<List<Map<String, String>>>();
			List<List<Map<String, String>>> singleIngredientGenericDrugs = new ArrayList<List<Map<String, String>>>();
			List<List<Map<String, String>>> multipleIngredientGenericDrugs = new ArrayList<List<Map<String, String>>>();
			
			List<List<Map<String, String>>> missingATCGenericDrugs = new ArrayList<List<Map<String, String>>>();
			
			if (genericDrugsFile.openFile()) {
				Map<String, String> genericDrugLine = null;
				Map<String, String> lastGenericDrugLine = null;
				List<Map<String, String>> genericDrug = new ArrayList<Map<String, String>>();
				while ((lastGenericDrugLine != null) || genericDrugsFile.hasNext() || (genericDrug.size() > 0)) {
					if (lastGenericDrugLine != null) {
						genericDrugLine = lastGenericDrugLine;
						lastGenericDrugLine = null;
					}
					else if (genericDrugsFile.hasNext()) {
						Row row = genericDrugsFile.next();
						
						genericDrugLine = new HashMap<String, String>();
						genericDrugLine.put("GenericDrugCode",      genericDrugsFile.get(row, "GenericDrugCode").trim());
						genericDrugLine.put("LabelName",            genericDrugsFile.get(row, "LabelName").trim().toUpperCase());
						genericDrugLine.put("ShortName",            genericDrugsFile.get(row, "ShortName").trim().toUpperCase());
						genericDrugLine.put("FullName",             genericDrugsFile.get(row, "FullName").trim().toUpperCase());
						genericDrugLine.put("ATCCode",              genericDrugsFile.get(row, "ATCCode").trim().toUpperCase());
						genericDrugLine.put("DDDPerUnit",           genericDrugsFile.get(row, "DDDPerUnit").trim());
						genericDrugLine.put("PrescriptionDays",     genericDrugsFile.get(row, "PrescriptionDays").trim());
						genericDrugLine.put("Dosage",               genericDrugsFile.get(row, "Dosage").trim());
						genericDrugLine.put("DosageUnit",           genericDrugsFile.get(row, "DosageUnit").trim());
						genericDrugLine.put("PharmaceuticalForm",   genericDrugsFile.get(row, "PharmaceuticalForm").trim().toUpperCase());
						
						genericDrugLine.put("IngredientCode",        genericDrugsFile.get(row, "IngredientCode").trim());
						genericDrugLine.put("IngredientPartNumber",  genericDrugsFile.get(row, "IngredientPartNumber").trim());
						genericDrugLine.put("IngredientAmount",      genericDrugsFile.get(row, "IngredientAmount").trim());
						genericDrugLine.put("IngredientAmountUnit",  genericDrugsFile.get(row, "IngredientAmountUnit").trim().toUpperCase());
						genericDrugLine.put("IngredientGenericName", genericDrugsFile.get(row, "IngredientGenericName").trim().toUpperCase());
						genericDrugLine.put("IngredientCASNumber",   genericDrugsFile.get(row, "IngredientCASNumber").trim());
						
						genericDrugLine.put("SubstanceCode",        genericDrugsFile.get(row, "SubstanceCode").trim());
						genericDrugLine.put("SubstanceDescription", genericDrugsFile.get(row, "SubstanceDescription").trim().toUpperCase());
					}
					
					if (genericDrugLine != null) {
						if ((genericDrug.size() > 0) && (!genericDrugLine.get("GenericDrugCode").equals(genericDrug.get(genericDrug.size() -1).get("GenericDrugCode")))) {
							lastGenericDrugLine = genericDrugLine;
							genericDrugLine = null;
						}
						else {
							genericDrug.add(genericDrugLine);
							genericDrugLine = null;
						}
					}
					
					if ((genericDrug.size() > 0) && ((lastGenericDrugLine != null) || (!genericDrugsFile.hasNext()))) {
						
						System.out.println("    " + genericDrugDescription(genericDrug));
						
						genericDrugs.add(genericDrug);
						if (genericDrug.size() == 0) {
							noIngredientGenericDrugs.add(genericDrug);
						}
						else if (genericDrug.size() == 1) {
							singleIngredientGenericDrugs.add(genericDrug);
						}
						else {
							multipleIngredientGenericDrugs.add(genericDrug);
						}
						if (genericDrug.get(0).get("ATCCode").equals("")) {
							missingATCGenericDrugs.add(genericDrug);
							writeGenericDrugToFile(genericDrug, missingATCFile);
						}
						
						genericDrug = new ArrayList<Map<String, String>>();
					}
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");

			
			System.out.println(DrugMapping.getCurrentTime() + " Get single ingredient drugs from CDM ...");

			List<CDMDrug> cdmSingleIngredientDrugs = new ArrayList<CDMDrug>();
			
			/* Collect all strengths of single ingredient drugs by ATC */
			String query = "WITH atc_ingredient AS (";
			query += " " + "	/* ATC to ingredient direct */";
			query += " " + "	SELECT atc.concept_code AS atc";
			query += " " + "	,      atc.concept_name AS atc_concept_name";
			query += " " + "	,      ingredient.concept_id AS ingredient_concept_id";
			query += " " + "	,      ingredient.concept_name AS ingredient_concept_name";
			query += " " + "	FROM " + database.getVocabSchema() + ".concept_ancestor atc_to_ingredient";
			query += " " + "	INNER JOIN " + database.getVocabSchema() + ".concept atc";
			query += " " + "		ON atc_to_ingredient.ancestor_concept_id = atc.concept_id";
			query += " " + "	INNER JOIN " + database.getVocabSchema() + ".concept ingredient";
			query += " " + "		ON atc_to_ingredient.descendant_concept_id = ingredient.concept_id";
			query += " " + "	WHERE atc.vocabulary_id = 'ATC'";
			query += " " + "	AND   ingredient.vocabulary_id = 'RxNorm'";
			query += " " + "	AND   ingredient.concept_class_id = 'Ingredient'";
			query += " " + "	AND   UPPER(atc.concept_name) = UPPER(ingredient.concept_name)";
			query += " " + ")";
			query += " " + "SELECT atc_ingredient.atc AS atc";
			
			query += " " + ",      drug.concept_id AS drug_concept_id";
			query += " " + ",      drug.concept_name AS drug_concept_name";
			query += " " + ",      drug.domain_id AS drug_domain_id";
			query += " " + ",      drug.vocabulary_id AS drug_vocabulary_id";
			query += " " + ",      drug.concept_class_id AS drug_concept_class_id";
			query += " " + ",      drug.standard_concept AS drug_standard_concept";
			query += " " + ",      drug.concept_code AS drug_concept_code";

			query += " " + ",      ingredient.concept_id AS ingredient_concept_id";
			query += " " + ",      ingredient.concept_name AS ingredient_concept_name";
			query += " " + ",      ingredient.domain_id AS ingredient_domain_id";
			query += " " + ",      ingredient.vocabulary_id AS ingredient_vocabulary_id";
			query += " " + ",      ingredient.concept_class_id AS ingredient_concept_class_id";
			query += " " + ",      ingredient.standard_concept AS ingredient_standard_concept";
			query += " " + ",      ingredient.concept_code AS ingredient_concept_code";

			query += " " + ",      strength.amount_value AS ingredient_amount_value";
			query += " " + ",      amount_unit.concept_id AS ingredient_amount_unit_concept_id";
			query += " " + ",      amount_unit.concept_name AS ingredient_amount_unit_concept_name";
			query += " " + ",      amount_unit.domain_id AS ingredient_amount_unit_domain_id";
			query += " " + ",      amount_unit.vocabulary_id AS ingredient_amount_unit_vocabulary_id";
			query += " " + ",      amount_unit.concept_class_id AS ingredient_amount_unit_concept_class_id";
			query += " " + ",      amount_unit.standard_concept AS ingredient_amount_unit_standard_concept";
			query += " " + ",      amount_unit.concept_code AS ingredient_amount_unit_concept_code";

			query += " " + ",      strength.numerator_value AS ingredient_numerator_value";
			query += " " + ",      numerator_unit.concept_id AS ingredient_numerator_unit_concept_id";
			query += " " + ",      numerator_unit.concept_name AS ingredient_numerator_unit_concept_name";
			query += " " + ",      numerator_unit.domain_id AS ingredient_numerator_unit_domain_id";
			query += " " + ",      numerator_unit.vocabulary_id AS ingredient_numerator_unit_vocabulary_id";
			query += " " + ",      numerator_unit.concept_class_id AS ingredient_numerator_unit_concept_class_id";
			query += " " + ",      numerator_unit.standard_concept AS ingredient_numerator_unit_standard_concept";
			query += " " + ",      numerator_unit.concept_code AS ingredient_numerator_unit_concept_code";

			query += " " + ",      strength.denominator_value AS ingredient_denominator_value";
			query += " " + ",      denominator_unit.concept_id AS ingredient_denominator_unit_concept_id";
			query += " " + ",      denominator_unit.concept_name AS ingredient_denominator_unit_concept_name";
			query += " " + ",      denominator_unit.domain_id AS ingredient_denominator_unit_domain_id";
			query += " " + ",      denominator_unit.vocabulary_id AS ingredient_denominator_unit_vocabulary_id";
			query += " " + ",      denominator_unit.concept_class_id AS ingredient_denominator_unit_concept_class_id";
			query += " " + ",      denominator_unit.standard_concept AS ingredient_denominator_unit_standard_concept";
			query += " " + ",      denominator_unit.concept_code AS ingredient_denominator_unit_concept_code";

			query += " " + ",      strength.box_size AS ingredient_box_size";
			query += " " + "FROM   atc_ingredient";
			query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".drug_strength strength";
			query += " " + "    ON strength.ingredient_concept_id = atc_ingredient.ingredient_concept_id";
			query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".concept drug";
			query += " " + "    ON strength.drug_concept_id = drug.concept_id";
			query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".concept ingredient";
			query += " " + "    ON strength.ingredient_concept_id = ingredient.concept_id";
			query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".concept amount_unit";
			query += " " + "    ON strength.amount_unit_concept_id = amount_unit.concept_id";
			query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".concept numerator_unit";
			query += " " + "    ON strength.numerator_unit_concept_id = numerator_unit.concept_id";
			query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".concept denominator_unit";
			query += " " + "    ON strength.denominator_unit_concept_id = denominator_unit.concept_id";
			
			if (database.excuteQuery(query)) {
				if (database.hasNext()) {
					while (database.hasNext()) {
						Row queryRow = database.next();

						String atc = queryRow.get("atc");
						
						CDMDrug cdmDrug = new CDMDrug(queryRow, "drug_");
						cdmDrug.addIngredient(new CDMIngredient(queryRow, "ingredient_"));
						
						//cdmSingleIngredientDrugs.add(cdmDrug);
						
						CDMIngredient cdmIngredient = cdmDrug.getIngredients().get(0);
						
						for (List<Map<String, String>> genericDrug : singleIngredientGenericDrugs) {
							Map<String, String> ingredient = genericDrug.get(0);
							
							if (ingredient.get("ATCCode").equals(atc)) {
								if ((ingredient.get("IngredientAmount") != null) && (!(ingredient.get("IngredientAmount").trim().equals("")))) {
									double genericDrugAmount = Double.parseDouble(ingredient.get("IngredientAmount"));
									String genricDrugAmountUnitName = ingredient.get("IngredientAmountUnit").trim().toUpperCase();
									if (unitConversionsMap.containsKey(genricDrugAmountUnitName)) {
										String cdmDrugAmountString = cdmIngredient.getAmountValue();
										String cdmDrugAmountUnit   = cdmIngredient.getAmountUnit().getConceptId();
										//TODO
									}
								}
							}  
						}
						
						System.out.println("    " + cdmDrug);
						for (CDMIngredient ingredient : cdmDrug.getIngredients()) {
							System.out.println("        " + ingredient.toStringLong());
						}
					}
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
/*			
			// Collect ATC ingredients
			System.out.println(DrugMapping.getCurrentTime() + " Collecting ATC ingredients ...");
			Map<String, Map<String, String>> atcIngredientsMap = new HashMap<String,Map<String, String>>();
			
			query  = "SELECT atc.concept_code AS atc";
			query += "," + "atc.concept_name AS atc_concept_name";
			query += "," + "ingredient.concept_id AS ingredient_concept_id";
			query += "," + "ingredient.concept_name AS ingredient_concept_name";
			query += "," + "ingredient.domain_id AS ingredient_domain_id";
			query += "," + "ingredient.vocabulary_id AS ingredient_vocabulary_id";
			query += "," + "ingredient.concept_class_id AS ingredient_concept_class_id";
			query += "," + "ingredient.standard_concept AS ingredient_standard_concept";
			query += "," + "ingredient.concept_code AS ingredient_concept_code";
			query += " " + "FROM " + database.getVocabSchema() + ".concept_ancestor atc_to_ingredient";
			query += " " + "INNER JOIN " + database.getVocabSchema() + ".concept atc";
			query += " " + "ON atc_to_ingredient.ancestor_concept_id = atc.concept_id";
			query += " " + "INNER JOIN " + database.getVocabSchema() + ".concept ingredient";
			query += " " + "ON atc_to_ingredient.descendant_concept_id = ingredient.concept_id";
			query += " " + "WHERE atc.vocabulary_id = 'ATC'";
			query += " " + "AND ingredient.vocabulary_id = 'RxNorm'";
			query += " " + "AND ingredient.concept_class_id = 'Ingredient'";
			query += " " + "AND atc.concept_name = ingredient.concept_name";
			
			if (database.excuteQuery(query)) {
				if (database.hasNext()) {
					while (database.hasNext()) {
						Row queryRow = database.next();

						String atc = queryRow.get("atc");
						
						Map<String, String>ingredientConcept = new HashMap<String, String>();
						
						ingredientConcept.put("concept_id"      , queryRow.get("ingredient_concept_id"));
						ingredientConcept.put("concept_name"    , queryRow.get("ingredient_concept_name"));
						ingredientConcept.put("domain_id"       , queryRow.get("ingredient_domain_id"));
						ingredientConcept.put("vocabulary_id"   , queryRow.get("ingredient_vocabulary_id"));
						ingredientConcept.put("concept_class_id", queryRow.get("ingredient_concept_class_id"));
						ingredientConcept.put("standard_concept", queryRow.get("ingredient_standard_concept"));
						ingredientConcept.put("concept_code"    , queryRow.get("ingredient_concept_code"));
						
						atcIngredientsMap.put(atc, ingredientConcept);

						System.out.println("        ATC Ingredient: " + atc + " -> " + getConceptDescription(ingredientConcept));
					}
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			

			Map<String, Map<String, String>> atcConceptMap = new HashMap<String, Map<String, String>>();
			Map<String, String> conceptATCMap = new HashMap<String, String>();
			Map<String, List<Map<String, String>>> atcRxNormMap = new HashMap<String, List<Map<String, String>>>();
			
			// Do the mapping
			System.out.println(DrugMapping.getCurrentTime() + " Mapping Generic Drugs ...");

			int ignoredMissingDataCounter = 0; // Counter of generic drugs that are ignored due to missing data
			int missingATCCounter         = 0; // Counter of generic drugs that have no ATC
			int noATCConceptFoundCounter  = 0; // Counter of generic drugs for which no ATC concept could be found
			int noRxNormForATCCounter     = 0; // Counter of ATC for which no RxNorm drugs could be found
			
			if (genericDrugsFile.openFile()) {
				Map<String, String> genericDrugLine = null;
				Map<String, String> lastGenericDrugLine = null;
				List<Map<String, String>> genericDrug = new ArrayList<Map<String, String>>();
				while ((lastGenericDrugLine != null) || genericDrugsFile.hasNext() || (genericDrug.size() > 0)) {
					if (lastGenericDrugLine != null) {
						genericDrugLine = lastGenericDrugLine;
						lastGenericDrugLine = null;
					}
					else if (genericDrugsFile.hasNext()) {
						Row row = genericDrugsFile.next();
						
						genericDrugLine = new HashMap<String, String>();
						genericDrugLine.put("GenericDrugCode",      genericDrugsFile.get(row, "GenericDrugCode").trim());
						genericDrugLine.put("LabelName",            genericDrugsFile.get(row, "LabelName").trim());
						genericDrugLine.put("ShortName",            genericDrugsFile.get(row, "ShortName").trim());
						genericDrugLine.put("FullName",             genericDrugsFile.get(row, "FullName").trim());
						genericDrugLine.put("ATCCode",              genericDrugsFile.get(row, "ATCCode").trim());
						genericDrugLine.put("DDDPerUnit",           genericDrugsFile.get(row, "DDDPerUnit").trim());
						genericDrugLine.put("PrescriptionDays",     genericDrugsFile.get(row, "PrescriptionDays").trim());
						genericDrugLine.put("Dosage",               genericDrugsFile.get(row, "Dosage").trim());
						genericDrugLine.put("DosageUnit",           genericDrugsFile.get(row, "DosageUnit").trim());
						genericDrugLine.put("PharmaceuticalForm",   genericDrugsFile.get(row, "PharmaceuticalForm").trim());
						
						genericDrugLine.put("IngredientCode",        genericDrugsFile.get(row, "IngredientCode").trim());
						genericDrugLine.put("IngredientPartNumber",  genericDrugsFile.get(row, "IngredientPartNumber").trim());
						genericDrugLine.put("IngredientAmount",      genericDrugsFile.get(row, "IngredientAmount").trim());
						genericDrugLine.put("IngredientAmountUnit",  genericDrugsFile.get(row, "IngredientAmountUnit").trim());
						genericDrugLine.put("IngredientGenericName", genericDrugsFile.get(row, "IngredientGenericName").trim());
						genericDrugLine.put("IngredientCASNumber",   genericDrugsFile.get(row, "IngredientCASNumber").trim());
						
						genericDrugLine.put("SubstanceCode",        genericDrugsFile.get(row, "SubstanceCode").trim());
						genericDrugLine.put("SubstanceDescription", genericDrugsFile.get(row, "SubstanceDescription").trim());
					}
					
					if (genericDrugLine != null) {
						if ((genericDrug.size() > 0) && (!genericDrugLine.get("GenericDrugCode").equals(genericDrug.get(genericDrug.size() -1).get("GenericDrugCode")))) {
							lastGenericDrugLine = genericDrugLine;
							genericDrugLine = null;
						}
						else {
							genericDrug.add(genericDrugLine);
							genericDrugLine = null;
						}
					}
					
					if ((genericDrug.size() > 0) && ((lastGenericDrugLine != null) || (!genericDrugsFile.hasNext()))) {
						
						System.out.println("    " + genericDrug.get(0).get("GenericDrugCode") + "," + genericDrug.get(0).get("ATCCode") + "," + genericDrug.get(0).get("FullName"));
						genericDrugCounter++;
						
						// Map containing atc to CDM concept mapping
						if ((!genericDrug.get(0).get("GenericDrugCode").isEmpty()) && (!genericDrug.get(0).get("FullName").isEmpty())) {
							
							// Get ATC concepts of components
							String atc = genericDrug.get(0).get("ATCCode");
							Map<String, String> atcConcept = null;
							if (!atc.isEmpty()) {
								atcConcept = atcConceptMap.get(atc);
								
								if (atcConcept == null) {
									query  = "SELECT concept_id";
									query += "," + "concept_name"; 
									query += "," + "vocabulary_id";  
									query += "," + "standard_concept";
									query += "," + "concept_class_id"; 
									query += "," + "concept_code"; 
									query += " " + "FROM " + database.getVocabSchema() + ".concept"; 
									query += " " + "WHERE upper(concept_code) = '" + atc + "'"; 
									query += " " + "AND domain_id = 'Drug'"; 
									query += " " + "AND vocabulary_id = 'ATC'"; 
									query += " " + "AND concept_class_id = 'ATC " + (atc.length() == 1 ? "1st": (atc.length() == 3 ? "2nd" : (atc.length() == 4 ? "3rd" : (atc.length() == 5 ? "4th" : "5th")))) + "'";
									
									if (database.excuteQuery(query)) {
										if (database.hasNext()) {
											while (database.hasNext()) {
												Row queryRow = database.next();
												if (atcConcept != null) {
													database.disconnect();
													break;
												}
												atcConcept = new HashMap<String, String>(); 
												atcConcept.put("concept_id"      , queryRow.get("concept_id"));
												atcConcept.put("concept_name"    , queryRow.get("concept_name"));
												atcConcept.put("domain_id"       , "Drug");
												atcConcept.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
												atcConcept.put("concept_class_id", queryRow.get("concept_class_id"));
												atcConcept.put("standard_concept", queryRow.get("standard_concept"));
												atcConcept.put("concept_code"    , queryRow.get("concept_code"));
												atcConcept.put("match"           , atc);
												
												atcConceptMap.put(atc, atcConcept);
												conceptATCMap.put(queryRow.get("concept_id"), atc);
											}
										}
									}
								}
								
								if (atcConcept == null) {
									System.out.println("        ATC Concept:  NOT FOUND");
									writeGenericDrugToFile(genericDrug, noATCConceptFoundFile);
									noATCConceptFoundCounter++;
								}
								else {
									System.out.println("        ATC Concept: " + atcConcept.get("match") + " -> " + getConceptDescription(atcConcept));
								}
								
								// Get RxNorm drugs for the ATC
								List<Map<String, String>> atcRxNormConcepts = atcRxNormMap.get(atc);
								
								if (atcRxNormConcepts == null) {
									query  = "SELECT concept_id";
									query += "," + "concept_name"; 
									query += "," + "domain_id";
									query += "," + "vocabulary_id";
									query += "," + "concept_class_id";  
									query += "," + "standard_concept"; 
									query += "," + "concept_code"; 
									query += " " + "FRO " + database.getVocabSchema() + ".concept_relationship";
									query += " " + "LEFT OUTER JOIN " + database.getVocabSchema() + ".concept";
									query += " " + "ON concept_id_1 = concept_id";
									query += " " + "WHERE concept_id_2 in ("; 
									query += " " + "    SELECT concept_id"; 
									query += " " + "    FRO " + database.getVocabSchema() + ".concept";
									query += " " + "    WHERE concept_code = '" + atc + "'";
									query += " " + "    AND vocabulary_id = 'ATC'";
									query += " " + "    )"; 
									query += " " + "AND relationship_id = 'RxNorm - ATC'";
									
									if (database.excuteQuery(query)) {
										if (database.hasNext()) {
											atcRxNormConcepts = new ArrayList<Map<String, String>>();
											atcRxNormMap.put(atc, atcRxNormConcepts);
											
											while (database.hasNext()) {
												Row queryRow = database.next();
												
												Map<String, String> rxNormConcept = new HashMap<String, String>();
												rxNormConcept.put("concept_id"      , queryRow.get("concept_id"));
												rxNormConcept.put("concept_name"    , queryRow.get("concept_name"));
												rxNormConcept.put("domain_id"       , queryRow.get("domain_id"));
												rxNormConcept.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
												rxNormConcept.put("concept_class_id", queryRow.get("concept_class_id"));
												rxNormConcept.put("standard_concept", queryRow.get("standard_concept"));
												rxNormConcept.put("concept_code"    , queryRow.get("concept_code"));
												atcRxNormConcepts.add(rxNormConcept);
											}
										}
									}
								}

								int rxNormConceptCount = atcRxNormConcepts == null ? 0 : atcRxNormConcepts.size();
								System.out.println("        RxNorm Concepts found for atc '" + atc + "' (" + Integer.toString(rxNormConceptCount) + "):");
								if (atcRxNormConcepts != null) {
									for (Map<String, String> rxNormConcept : atcRxNormConcepts) {
										System.out.println("            " + getConceptDescription(rxNormConcept));
									}
								}
								else {
									noRxNormForATCCounter++;
								}
								
							}
							else {
								System.out.println("        NO ATC");
								writeGenericDrugToFile(genericDrug, missingATCFile);
								missingATCCounter++;
							}
							
							
						}
						else {
							writeGenericDrugToFile(genericDrug, ignoredMissingDataFile);
							ignoredMissingDataCounter++;
						}
						
						genericDrug.clear();
					}
				}
			}
*/
			
			System.out.println("");
			System.out.println("    Generic drugs: " + Integer.toString(genericDrugs.size()));
			System.out.println("    No ATC: " + Integer.toString(missingATCGenericDrugs.size()));
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			// Close all output files
			missingATCFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
		}
	}
	
	
	private void writeGenericDrugHeaderToFile(PrintWriter file) {
		String header = "GenericDrugCode";
		header += "," + "LabelName";
		header += "," + "ShortName";
		header += "," + "FullName";
		header += "," + "ATCCode";
		header += "," + "DDDPerUnit";
		header += "," + "PrescriptionDays";
		header += "," + "Dosage";
		header += "," + "DosageUnit";
		header += "," + "PharmaceuticalForm";
		
		header += "," + "IngredientCode";
		header += "," + "IngredientPartNumber";
		header += "," + "IngredientAmount";
		header += "," + "IngredientAmountUnit";
		header += "," + "IngredientGenericName";
		header += "," + "IngredientCASNumber";
		
		header += "," + "SubstanceCode";
		header += "," + "SubstanceDescription";

		file.println(header);
	}
	
	
	private void writeGenericDrugToFile(List<Map<String, String>> genericDrug, PrintWriter file) {
		for (Map<String, String> record : genericDrug) {
			String line = record.get("GenericDrugCode") +
					"," + record.get("LabelName") +
					"," + record.get("ShortName") +
					"," + record.get("FullName") +
					"," + record.get("ATCCode") +
					"," + record.get("DDDPerUnit") +
					"," + record.get("PrescriptionDays") +
					"," + record.get("Dosage") +
					"," + record.get("DosageUnit") +
					"," + record.get("PharmaceuticalForm") +
					
					"," + record.get("IngredientCode") +
					"," + record.get("IngredientPartNumber") +
					"," + record.get("IngredientAmount") +
					"," + record.get("IngredientAmountUnit") +
					"," + record.get("IngredientGenericName") +
					"," + record.get("IngredientCASNumber") +
					
					"," + record.get("SubstanceCode") +
					"," + record.get("SubstanceDescription");
			file.println(line);
		}
	}
	
	
	private String genericDrugDescription(List<Map<String, String>> genericDrug) {
		String description = genericDrug.get(0).get("GenericDrugCode");
		description += "," + genericDrug.get(0).get("ATCCode");
		description += "," + (genericDrug.get(0).get("FullName").equals("") ? (genericDrug.get(0).get("ShortName").equals("") ? genericDrug.get(0).get("LabelName") : genericDrug.get(0).get("ShortName")) : genericDrug.get(0).get("FullName"));
		return description;
	}

}
