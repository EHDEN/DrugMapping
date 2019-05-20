package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class MapIngredients extends Mapping {
	private Map <String, Map<String, String>> ingredientMapping; 
	
	
	public MapIngredients(CDMDatabase database, InputFile ingredientsFile) {
		ingredientMapping = new HashMap<String, Map<String, String>>();
		
		String query;
		System.out.println(DrugMapping.getCurrentTime() + " Mapping ingredients ...");
		
		if (ingredientsFile.openFile()) {
			while (ingredientsFile.hasNext()) {
				Row row = ingredientsFile.next();
				String ingredientID = ingredientsFile.get(row, "IngredientID");
				String ingredientText = ingredientsFile.get(row, "IngredientText");
				String ingredientTextEnglish = ingredientsFile.get(row, "IngredientTextEnglish");
				String ingredientTextEnglishUpperCase = ingredientTextEnglish.toUpperCase().replaceAll("'", "''");
				
				System.out.println("    " + ingredientID + "," + ingredientText + "," + ingredientTextEnglish);
				
				Map<String, String> mappedRecord = null;
				
				// Collect name matching strings
				List<String> nameMatching = new ArrayList<String>();
				List<String> nameMatchingDescription = new ArrayList<String>();
				
				nameMatching.add(ingredientTextEnglishUpperCase);
				nameMatchingDescription.add("IngredientTextEnglish");
				
				nameMatching.add(getReplacement(ingredientText));
				nameMatchingDescription.add("IngredientText Replacements");
				
				// Match with name matching strings one by one
				Set<String> nameMatchesUsed = new HashSet<String>();
				int nameMatchIndex = 0;
				while (nameMatchIndex < nameMatching.size()) {
					String nameMatch = nameMatching.get(nameMatchIndex);
					String nameMatchDescription = nameMatchingDescription.get(nameMatchIndex);

					if (nameMatchesUsed.add(nameMatch)) {
						System.out.println("        Matching on " + nameMatch);
						
						// Try mapping to RxNorm (extension) ingredient with matching name
						query  = "select concept_id";
						query += "," + "concept_name"; 
						query += "," + "vocabulary_id";  
						query += "," + "concept_code"; 
						query += " " + "from " + database.getVocabSchema() + ".concept"; 
						query += " " + "where upper(concept_name) like '" + nameMatch + "'"; 
						query += " " + "and domain_id = 'Drug'"; 
						query += " " + "and vocabulary_id like 'RxNorm%'"; 
						query += " " + "and concept_class_id = 'Ingredient'"; 
						query += " " + "and standard_concept = 'S'";
						
						if (database.excuteQuery(query)) {
							if (database.hasNext()) {
								while (database.hasNext()) {
									Row queryRow = database.next();
									if (mappedRecord != null) {
										database.disconnect();
										mappedRecord = null;
										break;
									}
									mappedRecord = new HashMap<String, String>(); 
									mappedRecord.put("concept_id"      , queryRow.get("concept_id"));
									mappedRecord.put("concept_name"    , queryRow.get("concept_name"));
									mappedRecord.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
									mappedRecord.put("concept_code"    , queryRow.get("concept_code"));
									mappedRecord.put("match"           , nameMatchDescription + "-RxNorm%-Ingredient-S");
								}
							}
						}
						
						// Try mapping by 'Maps to' relation of concept with matching name
						if (mappedRecord == null) { 
							
							// Find all concepts matching the IngredientTextEnglish
							query  = "select concept_id"; 
							query += " " + "from " + database.getVocabSchema() + ".concept"; 
							query += " " + "where upper(concept_name) like '" + nameMatch + "'"; 
							
							Set<String> foundConcepts = new HashSet<String>(); // Store concept_id's to prevent cycles
							Map<String, Map<String, String>> matchingConcepts = new HashMap<String, Map<String, String>>();
							if (database.excuteQuery(query)) {
								if (database.hasNext()) {
									while (database.hasNext()) {
										Row queryRow = database.next();
										Map<String, String> matchingConcept = new HashMap<String, String>();
										matchingConcept.put("concept_id"      , queryRow.get("concept_id"));
										matchingConcept.put("match"           , nameMatchDescription + " " + queryRow.get("concept_id"));
										matchingConcepts.put(queryRow.get("concept_id"), matchingConcept);
										foundConcepts.add(queryRow.get("concept_id"));
									}
									
									// Go through the "Maps to" relations until you found a single standard
									// RxNorm or RxNorm extension ingredient but stop with failure when
									// more than one is found.
									while (matchingConcepts.size() > 0) {
										query  = "select concept_id_1, concept_id, concept_name, domain_id, vocabulary_id, standard_concept, concept_code, concept_class_id"; 
										query += " " + "from " + database.getVocabSchema() + ".concept_relationship"; 
										query += " " + "left outer join " + database.getVocabSchema() + ".concept"; 
										query += " " + "on concept_id_2 = concept_id"; 
										query += " " + "where relationship_id = 'Maps to'"; 
										query += " " + "and (";
										int conceptNr = 0;
										for (String concept_id : matchingConcepts.keySet()) {
											if (conceptNr > 0) query += " or ";
											query += "concept_id_1 = " + concept_id;
											conceptNr++;
										}
										query += ")";

										Set<String> conceptsForNextLevel = new HashSet<String>();
										Map<String, Map<String, String>> nextLevelMatchingConcepts = new HashMap<String, Map<String, String>>();
										if (database.excuteQuery(query)) {
											if (database.hasNext()) {
												while (database.hasNext()) {
													Row queryRow = database.next();
													if (foundConcepts.add(queryRow.get("concept_id"))) {
														if (
																queryRow.get("domain_id").equals("Drug") &&
																queryRow.get("vocabulary_id").substring(0, 6).equals("RxNorm") &&
																queryRow.get("concept_class_id").equals("Ingredient") &&
																queryRow.get("standard_concept").equals("S")
															) {
															// Found RxNorm ingredient
															mappedRecord = new HashMap<String, String>(); 
															mappedRecord.put("concept_id"      , queryRow.get("concept_id"));
															mappedRecord.put("concept_name"    , queryRow.get("concept_name"));
															mappedRecord.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
															mappedRecord.put("concept_code"    , queryRow.get("concept_code"));
															mappedRecord.put("match"           , matchingConcepts.get(queryRow.get("concept_id_1")).get("match") + "-" + "Maps to " + mappedRecord.get("concept_id"));
															
															database.disconnect();
															
															nextLevelMatchingConcepts.clear();

															break;
														}
														else {
															Map<String, String> matchingConcept = new HashMap<String, String>();
															matchingConcept.put("concept_id"      , queryRow.get("concept_id"));
															matchingConcept.put("match"           , matchingConcepts.get(queryRow.get("concept_id_1")).get("match") + "-" + "Maps to " + queryRow.get("concept_id"));
															
															conceptsForNextLevel.add(queryRow.get("concept_id_1"));
															nextLevelMatchingConcepts.put(queryRow.get("concept_id"), matchingConcept);
														}
													}
												}
											}
										}
										
										matchingConcepts = nextLevelMatchingConcepts;
									}
								}
							}
						}
						
						// Try mapping by 'Maps to' relation of concept with matching name
						if (mappedRecord == null) { 
							
							// Find all concept synonyms matching the IngredientTextEnglish
							query  = "select concept_id"; 
							query += " " + "from " + database.getVocabSchema() + ".concept_synonym"; 
							query += " " + "where upper(concept_synonym_name) like '" + nameMatch + "'"; 
							
							Set<String> foundConcepts = new HashSet<String>(); // Store concept_id's to prevent cycles
							Map<String, Map<String, String>> matchingConcepts = new HashMap<String, Map<String, String>>();
							if (database.excuteQuery(query)) {
								if (database.hasNext()) {
									while (database.hasNext()) {
										Row queryRow = database.next();
										Map<String, String> matchingConcept = new HashMap<String, String>();
										matchingConcept.put("concept_id"      , queryRow.get("concept_id"));
										matchingConcept.put("match"           , nameMatchDescription + " Synonym " + queryRow.get("concept_id"));
										matchingConcepts.put(queryRow.get("concept_id"), matchingConcept);
										foundConcepts.add(queryRow.get("concept_id"));
									}
									
									// Go through the "Maps to" relations until you found a single standard
									// RxNorm or RxNorm extension ingredient but stop with failure when
									// more than one is found.
									while (matchingConcepts.size() > 0) {
										query  = "select concept_id_1, concept_id, concept_name, domain_id, vocabulary_id, standard_concept, concept_code, concept_class_id"; 
										query += " " + "from " + database.getVocabSchema() + ".concept_relationship"; 
										query += " " + "left outer join " + database.getVocabSchema() + ".concept"; 
										query += " " + "on concept_id_2 = concept_id"; 
										query += " " + "where relationship_id = 'Maps to'"; 
										query += " " + "and (";
										int conceptNr = 0;
										for (String concept_id : matchingConcepts.keySet()) {
											if (conceptNr > 0) query += " or ";
											query += "concept_id_1 = " + concept_id;
											conceptNr++;
										}
										query += ")";

										Set<String> conceptsForNextLevel = new HashSet<String>();
										Map<String, Map<String, String>> nextLevelMatchingConcepts = new HashMap<String, Map<String, String>>();
										if (database.excuteQuery(query)) {
											if (database.hasNext()) {
												while (database.hasNext()) {
													Row queryRow = database.next();
													if (foundConcepts.add(queryRow.get("concept_id"))) {
														if (
																queryRow.get("domain_id").equals("Drug") &&
																queryRow.get("vocabulary_id").substring(0, 6).equals("RxNorm") &&
																queryRow.get("concept_class_id").equals("Ingredient") &&
																queryRow.get("standard_concept").equals("S")
															) {
															// Found RxNorm ingredient
															mappedRecord = new HashMap<String, String>(); 
															mappedRecord.put("concept_id"      , queryRow.get("concept_id"));
															mappedRecord.put("concept_name"    , queryRow.get("concept_name"));
															mappedRecord.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
															mappedRecord.put("concept_code"    , queryRow.get("concept_code"));
															mappedRecord.put("match"           , matchingConcepts.get(queryRow.get("concept_id_1")).get("match") + "-" + "Maps to " + mappedRecord.get("concept_id"));
															
															database.disconnect();
															
															nextLevelMatchingConcepts.clear();

															break;
														}
														else {
															Map<String, String> matchingConcept = new HashMap<String, String>();
															matchingConcept.put("concept_id"      , queryRow.get("concept_id"));
															matchingConcept.put("match"           , matchingConcepts.get(queryRow.get("concept_id_1")).get("match") + "-" + "Maps to " + queryRow.get("concept_id"));
															
															conceptsForNextLevel.add(queryRow.get("concept_id_1"));
															nextLevelMatchingConcepts.put(queryRow.get("concept_id"), matchingConcept);
														}
													}
												}
											}
										}
										
										matchingConcepts = nextLevelMatchingConcepts;
									}
								}
							}
						}
					}
					
					nameMatchIndex++;
				}
				
				if (mappedRecord != null) {
					ingredientMapping.put(ingredientID, mappedRecord);
					
					System.out.println("        " + mappedRecord.get("match") +
							" -> " + mappedRecord.get("concept_id") +
							","+ mappedRecord.get("concept_name") +
							","+ "Drug" +
							","+ mappedRecord.get("vocabulary_id") +
							","+ "Ingredient" +
							","+ "S" +
							","+ mappedRecord.get("concept_code")
							);
				}
				
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}

}
