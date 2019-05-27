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
	
	
	public MapGenericDrugs(CDMDatabase database, InputFile genericDrugsFile) {
				
		String fileName = "";
		try {
			// Create all output files
			System.out.println(DrugMapping.getCurrentTime() + " Creating output files ...");

			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Ignored Missing Data.csv";
			System.out.println("    " + fileName);
			PrintWriter ignoredMissingDataFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(ignoredMissingDataFile);
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
			System.out.println("    " + fileName);
			PrintWriter missingATCFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(missingATCFile);
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping No ATC Concept Found.csv";
			System.out.println("    " + fileName);
			PrintWriter noATCConceptFoundFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(noATCConceptFoundFile);
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			

			String query = null;
			
			
			// Collect ATC ingredients
			System.out.println(DrugMapping.getCurrentTime() + " Collecting ATC ingredients ...");
			Map<String, Map<String, String>> atcIngredientsMap = new HashMap<String,Map<String, String>>();
			
			query  = "select atc.concept_code as atc";
			query += "," + "atc.concept_name as atc_concept_name";
			query += "," + "ingredient.concept_id as ingredient_concept_id";
			query += "," + "ingredient.concept_name as ingredient_concept_name";
			query += "," + "ingredient.domain_id as ingredient_domain_id";
			query += "," + "ingredient.vocabulary_id as ingredient_vocabulary_id";
			query += "," + "ingredient.concept_class_id as ingredient_concept_class_id";
			query += "," + "ingredient.standard_concept as ingredient_standard_concept";
			query += "," + "ingredient.concept_code as ingredient_concept_code";
			query += " " + "from " + database.getVocabSchema() + ".concept_ancestor atc_to_ingredient";
			query += " " + "inner join " + database.getVocabSchema() + ".concept atc";
			query += " " + "on atc_to_ingredient.ancestor_concept_id = atc.concept_id";
			query += " " + "inner join " + database.getVocabSchema() + ".concept ingredient";
			query += " " + "on atc_to_ingredient.descendant_concept_id = ingredient.concept_id";
			query += " " + "where atc.vocabulary_id = 'ATC'";
			query += " " + "and ingredient.vocabulary_id = 'RxNorm'";
			query += " " + "and ingredient.concept_class_id = 'Ingredient'";
			query += " " + "and atc.concept_name = ingredient.concept_name";
			
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

			int genericDrugCounter        = 0; // Counter of generic drugs
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
						
						genericDrugLine.put("ComponentCode",        genericDrugsFile.get(row, "ComponentCode").trim());
						genericDrugLine.put("ComponentPartNumber",  genericDrugsFile.get(row, "ComponentPartNumber").trim());
						genericDrugLine.put("ComponentType",        genericDrugsFile.get(row, "ComponentType").trim());
						genericDrugLine.put("ComponentAmount",      genericDrugsFile.get(row, "ComponentAmount").trim());
						genericDrugLine.put("ComponentAmountUnit",  genericDrugsFile.get(row, "ComponentAmountUnit").trim());
						genericDrugLine.put("ComponentGenericName", genericDrugsFile.get(row, "ComponentGenericName").trim());
						genericDrugLine.put("ComponentCASNumber",   genericDrugsFile.get(row, "ComponentCASNumber").trim());
						
						genericDrugLine.put("SubstanceCode",        genericDrugsFile.get(row, "SubstanceCode").trim());
						genericDrugLine.put("SubstanceDescription", genericDrugsFile.get(row, "SubstanceDescription").trim());
						genericDrugLine.put("SubstanceCASNumber",   genericDrugsFile.get(row, "SubstanceCASNumber").trim());
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
									query  = "select concept_id";
									query += "," + "concept_name"; 
									query += "," + "vocabulary_id";  
									query += "," + "standard_concept";
									query += "," + "concept_class_id"; 
									query += "," + "concept_code"; 
									query += " " + "from " + database.getVocabSchema() + ".concept"; 
									query += " " + "where upper(concept_code) = '" + atc + "'"; 
									query += " " + "and domain_id = 'Drug'"; 
									query += " " + "and vocabulary_id = 'ATC'"; 
									query += " " + "and concept_class_id = 'ATC " + (atc.length() == 1 ? "1st": (atc.length() == 3 ? "2nd" : (atc.length() == 4 ? "3rd" : (atc.length() == 5 ? "4th" : "5th")))) + "'";
									
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
									query  = "select concept_id";
									query += "," + "concept_name"; 
									query += "," + "domain_id";
									query += "," + "vocabulary_id";
									query += "," + "concept_class_id";  
									query += "," + "standard_concept"; 
									query += "," + "concept_code"; 
									query += " " + "from " + database.getVocabSchema() + ".concept_relationship";
									query += " " + "left outer join " + database.getVocabSchema() + ".concept";
									query += " " + "on concept_id_1 = concept_id";
									query += " " + "where concept_id_2 in ("; 
									query += " " + "    select concept_id"; 
									query += " " + "    from " + database.getVocabSchema() + ".concept";
									query += " " + "    where concept_code = '" + atc + "'";
									query += " " + "    and vocabulary_id = 'ATC'";
									query += " " + "    )"; 
									query += " " + "and relationship_id = 'RxNorm - ATC'";
									
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
			System.out.println("    Generic drugs: " + Integer.toString(genericDrugCounter));
			System.out.println("    Ignored: " + Integer.toString(ignoredMissingDataCounter));
			System.out.println("    No ATC: " + Integer.toString(missingATCCounter));
			System.out.println("    No ATC Concept: " + Integer.toString(noATCConceptFoundCounter));
			System.out.println("    No RxNorm for ATC: " + Integer.toString(noRxNormForATCCounter));
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			// Close all output files
			ignoredMissingDataFile.close();
			missingATCFile.close();
			noATCConceptFoundFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
		}
	}
	
	
	private String getConceptDescription(Map<String, String> concept) {
		String description = (concept.get("concept_id") == null ? "null" : concept.get("concept_id"));
		description += "," + (concept.get("concept_name") == null ? "null" : concept.get("concept_name"));
		description += "," + (concept.get("domain_id") == null ? "null" : concept.get("domain_id"));
		description += "," + (concept.get("vocabulary_id") == null ? "null" : concept.get("vocabulary_id"));
		description += "," + (concept.get("concept_class_id") == null ? "null" : concept.get("concept_class_id"));
		description += "," + (concept.get("standard_concept") == null ? "null" : concept.get("standard_concept"));
		description += "," + (concept.get("concept_code") == null ? "null" : concept.get("concept_code"));
		return description;
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
		
		header += "," + "ComponentCode";
		header += "," + "ComponentPartNumber";
		header += "," + "ComponentType";
		header += "," + "ComponentAmount";
		header += "," + "ComponentAmountUnit";
		header += "," + "ComponentGenericName";
		header += "," + "ComponentCASNumber";
		
		header += "," + "SubstanceCode";
		header += "," + "SubstanceDescription";
		header += "," + "SubstanceCASNumber";

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
					
					"," + record.get("ComponentCode") +
					"," + record.get("ComponentPartNumber") +
					"," + record.get("ComponentType") +
					"," + record.get("ComponentAmount") +
					"," + record.get("ComponentAmountUnit") +
					"," + record.get("ComponentGenericName") +
					"," + record.get("ComponentCASNumber") +
					
					"," + record.get("SubstanceCode") +
					"," + record.get("SubstanceDescription") +
					"," + record.get("SubstanceCASNumber");
			file.println(line);
		}
	}

}