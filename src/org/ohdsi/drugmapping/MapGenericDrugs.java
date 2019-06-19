package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.cdm.CDMStrength;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.Row;

public class MapGenericDrugs extends Mapping {
	private FormConversion formConversion = new FormConversion();
	private Set<String> units = new HashSet<String>();
	
	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	
	private int noATCCounter = 0; // Counter of source drugs without ATC code.
		
	
	public MapGenericDrugs(CDMDatabase database, InputFile unitMappingsFile, InputFile formMappingsFile, InputFile sourceDrugsFile) {
		
		QueryParameters queryParameters;
				
		String fileName = "";
		try {
			// Create all output files
			System.out.println(DrugMapping.getCurrentTime() + " Creating output files ...");
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
			System.out.println("    " + fileName);
			PrintWriter missingATCFile = new PrintWriter(new File(fileName));
			SourceDrug.writeHeaderToFile(missingATCFile);
			System.out.println(DrugMapping.getCurrentTime() + " Finished");

/*
			System.out.println(DrugMapping.getCurrentTime() + " Loading form mappings ...");
			if (formMappingsFile.openFile()) {
				while (formMappingsFile.hasNext()) {
					Row row = formMappingsFile.next();

					formConversion.add(formMappingsFile.get(row, "LocalForm").toLowerCase(), formMappingsFile.get(row, "CDMForm").toLowerCase());
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
*/


			System.out.println(DrugMapping.getCurrentTime() + " Loading source drugs ...");
			
			// Read the generic drugs file
			if (sourceDrugsFile.openFile()) {
				while (sourceDrugsFile.hasNext()) {
					Row row = sourceDrugsFile.next();
					String sourceCode = sourceDrugsFile.get(row, "SourceCode").trim();
					
					if (!sourceCode.equals("")) {
						SourceDrug sourceDrug = sourceDrugMap.get(sourceCode);
						
						if (sourceDrug == null) {
							sourceDrug = new SourceDrug(
												sourceCode, 
												sourceDrugsFile.get(row, "SourceName").trim().toUpperCase(), 
												sourceDrugsFile.get(row, "SourceATCCode").trim().toUpperCase(), 
												sourceDrugsFile.get(row, "Formulation").trim().toUpperCase()
												);
							sourceDrugs.add(sourceDrug);
							sourceDrugMap.put(sourceCode, sourceDrug);
							
							System.out.println("    " + sourceDrug);
							
							if (sourceDrug.getATCCode() == null) {
								sourceDrug.writeDescriptionToFile("", missingATCFile);
								noATCCounter++;
							}
						}

						String ingredientName        = sourceDrugsFile.get(row, "IngredientName").trim().toUpperCase(); 
						String ingredientNameEnglish = sourceDrugsFile.get(row, "IngredientNameEnglish").trim();
						String dosage                = sourceDrugsFile.get(row, "Dosage").trim(); 
						String dosageUnit            = sourceDrugsFile.get(row, "DosageUnit").trim().toUpperCase(); 
						String casNumber             = sourceDrugsFile.get(row, "CASNumber").trim();
						
						if (!ingredientName.equals("")) {
							SourceIngredient sourceIngredient = sourceDrug.AddIngredient(ingredientName, ingredientNameEnglish, casNumber, dosage, dosageUnit);
							
							String unit = sourceDrug.getIngredientDosageUnit(sourceIngredient);
							if (unit != null) {
								units.add(unit);
							}
						}
					}
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			// Create Units Map
			UnitConversion unitConversionsMap = new UnitConversion(database, units);

			if (unitConversionsMap.getStatus() == UnitConversion.STATE_EMPTY) {
				// If no unit conversion is specified then stop.
				System.out.println("");
				System.out.println("FIRST FILL THE UNIT CONVERSION MAP IN THE FILE:");
				System.out.println("");
				System.out.println(DrugMapping.getCurrentPath() + "/" + UnitConversion.FILENAME);
			}
			else if (unitConversionsMap.getStatus() != UnitConversion.STATE_ERROR) {
				System.out.println(DrugMapping.getCurrentTime() + " Match ingredients by name ...");
				
				// Connect to the database
				RichConnection connection = database.getRichConnection(this.getClass());
				
				// Match RxNorm Ingredients on IngredientNameEnglish 
				for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
					queryParameters = new QueryParameters();
					queryParameters.set("@vocab", database.getVocabSchema());
					queryParameters.set("@name", sourceIngredient.getIngredientNameEnglish().toUpperCase().replaceAll("'", "''"));
					
					List<CDMIngredient> matchingCDMIngredients = new ArrayList<CDMIngredient>();
					for (Row queryRow : connection.queryResource("FindRxNormIngredientsByName.sql", queryParameters)) {
						matchingCDMIngredients.add(new CDMIngredient(queryRow, ""));
					}
					if (matchingCDMIngredients.size() == 1) {
						ingredientMap.put(sourceIngredient, matchingCDMIngredients.get(0));
						System.out.println("    " + sourceIngredient);
						System.out.println("        " + matchingCDMIngredients.get(0));
					}
				}
				
				//TODO
				
				connection.close();
				
				int noIngredientsCount = 0;
				int singleIngredientCount = 0;
				for (SourceDrug sourceDrug : sourceDrugs) {
					if (sourceDrug.getIngredients().size() == 0) {
						noIngredientsCount++;
					}
					if (sourceDrug.getIngredients().size() == 1) {
						singleIngredientCount++;
					}
				}
				
				System.out.println("");
				System.out.println("    Source drugs: " + Integer.toString(sourceDrugs.size()));
				System.out.println("    Source drugs without ATC: " + Integer.toString(noATCCounter));
				System.out.println("    Source drugs without ingredients: " + Integer.toString(noIngredientsCount));
				System.out.println("    Unique source ingredients: " + Integer.toString(SourceDrug.getAllIngredients().size()));
				System.out.println("    Matched source ingredients: " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("    Single ingredient generic source drugs: " + Integer.toString(singleIngredientCount));
				System.out.println(DrugMapping.getCurrentTime() + " Finished");
			}
			
/*			
			System.out.println(DrugMapping.getCurrentTime() + " Get single ingredient drugs from CDM ...");

			//List<CDMDrug> cdmSingleIngredientDrugs = new ArrayList<CDMDrug>();
			
			// Collect all strengths of single ingredient drugs by ATC
			
			int singleIngredientCDMDrugCounter  = 0;
			int matchedCDMDrugs = 0;
			Set<List<Map<String, String>>> matchedGenericDrugs = new HashSet<List<Map<String, String>>>();
			
			RichConnection connection = database.getRichConnection(this.getClass());
			
			for (Row queryRow : connection.queryResource("SingleIngredients.sql", queryParameters)) {

				String atc = queryRow.get("atc");
				String form = queryRow.get("form").toLowerCase();
				
				CDMDrug cdmDrug = new CDMDrug(queryRow, "");
										
				singleIngredientCDMDrugCounter++;
				
				Double cdmIngredientAmount     = null;
				String cdmIngredientAmountUnit = queryRow.get("amount_unit_concept_id");

				try {
					cdmIngredientAmount = Double.parseDouble(queryRow.get("amount_value"));
				}
				catch (NumberFormatException e) {
					cdmIngredientAmount = null;
				}
								
				if (cdmIngredientAmount != null) {
					for (List<Map<String, String>> genericDrug : singleIngredientGenericDrugs) {
						Map<String, String> ingredient = genericDrug.get(0);
						
						if (ingredient.get("ATCCode").equals(atc)) {
							//System.out.println("    Forms: " + ingredient.get("PharmaceuticalForm").toLowerCase() + " <-> " + form);
							if (formConversion.matchingPharmaceuticalForms(ingredient.get("PharmaceuticalForm").toLowerCase(), form)) {
								if ((ingredient.get("Dosage") != null) && (!(ingredient.get("Dosage").trim().equals("")))) {
									try {
										double genericDrugAmount = Double.parseDouble(ingredient.get("Dosage"));
										String genricDrugAmountUnitName = ingredient.get("DosageUnit").trim().toUpperCase();
										if (unitConversionsMap.containsKey(genricDrugAmountUnitName)) {
											if (unitConversionsMap.get(genricDrugAmountUnitName).matches(genericDrugAmount, cdmIngredientAmountUnit, cdmIngredientAmount)) {
												System.out.println("    " + cdmDrug);
												//System.out.println("        Matching ingredient: " + cdmIngredient.toStringLong());
												System.out.println("        Generic source drug: " + genericDrugDescription(genericDrug)); 
												System.out.println("        Drug: " + cdmDrug);
												
												matchedCDMDrugs++;
												matchedGenericDrugs.add(genericDrug);
												//TODO
											}
										}
									}
									catch (NumberFormatException e) {
										// Skip generic drug
									}
								}
							}
						}  
					}
				}
				else {
					// Skip CDM drug
					//System.out.println("        No CDM ingredient amount available -> SKIP");
				}
				
				//System.out.println("    " + cdmDrug);
				//for (CDMIngredient ingredient : cdmDrug.getIngredients()) {
				//	System.out.println("        " + ingredient.toStringLong());
				//}
			}
			connection.close();
			
			// Write pharmaceutical form matching log file
			formConversion.writeLogToFile(DrugMapping.getCurrentPath() + "/DrugMapping Pharmaceutical Form Matching.csv");
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
*/			
			
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
			
			System.out.println("");
			System.out.println("    Generic source drugs: " + Integer.toString(genericDrugs.size()));
			System.out.println("    No ATC: " + Integer.toString(missingATCGenericDrugs.size()));
			System.out.println("    Single ingredient generic source drugs: " + Integer.toString(singleIngredientGenericDrugs.size()));
			//System.out.println("    Single ingredient CDM drugs: " + Integer.toString(singleIngredientCDMDrugCounter));
			//System.out.println("    Matched single ingredient generic source drugs: " + Integer.toString(matchedGenericDrugs.size()));
			//System.out.println("    Matched single ingredient CDM drugs: " + Integer.toString(matchedCDMDrugs));
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
*/
			
			
			// Close all output files
			missingATCFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
		}
	}

}
