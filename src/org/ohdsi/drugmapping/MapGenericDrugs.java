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
	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
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

			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Ingredient Mapping.csv";
			System.out.println("    " + fileName);
			PrintWriter ingredientMappingFile = new PrintWriter(new File(fileName));
			ingredientMappingFile.println(SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
			
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
							
							//System.out.println("    " + sourceDrug);
							
							if (sourceDrug.getATCCode() == null) {
								sourceDrug.writeDescriptionToFile("", missingATCFile);
								noATCCounter++;
							}
						}

						String ingredientName        = sourceDrugsFile.get(row, "IngredientName").trim().toUpperCase(); 
						String ingredientNameEnglish = sourceDrugsFile.get(row, "IngredientNameEnglish").trim().toUpperCase();
						String dosage                = sourceDrugsFile.get(row, "Dosage").trim(); 
						String dosageUnit            = sourceDrugsFile.get(row, "DosageUnit").trim().toUpperCase(); 
						String casNumber             = sourceDrugsFile.get(row, "CASNumber").trim();
						
						while (ingredientName.contains("  "))        ingredientName        = ingredientName.replaceAll("  ", " ");
						while (ingredientNameEnglish.contains("  ")) ingredientNameEnglish = ingredientNameEnglish.replaceAll("  ", " ");
						while (dosage.contains("  "))                dosage                = dosage.replaceAll("  ", " ");
						while (dosageUnit.contains("  "))            dosageUnit            = dosageUnit.replaceAll("  ", " ");
						while (casNumber.contains("  "))             casNumber             = casNumber.replaceAll("  ", " ");
						
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

			System.out.println("    Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			// Create Units Map
			UnitConversion unitConversionsMap = new UnitConversion(database, units);
			
			
			// Load CDM ingredients
			System.out.println(DrugMapping.getCurrentTime() + " Get CDM ingredients ...");
			
			queryParameters = new QueryParameters();
			queryParameters.set("@vocab", database.getVocabSchema());
			
			// Connect to the database
			RichConnection connection = database.getRichConnection(this.getClass());
			
			// Get RxNorm ingredients
			CDMIngredient lastCdmIngredient = null;
			for (Row queryRow : connection.queryResource("cdm/GetRxNormIngredients.sql", queryParameters)) {
				String cdmIngredientConceptId = queryRow.get("concept_id").trim();
				if ((lastCdmIngredient == null) || (!lastCdmIngredient.getConceptId().equals(cdmIngredientConceptId))) {
					CDMIngredient cdmIngredient = new CDMIngredient(queryRow, "");
					cdmIngredients.put(cdmIngredient.getConceptId(), cdmIngredient);
					cdmIngredientsList.add(cdmIngredient);
					lastCdmIngredient = cdmIngredient;
				}
				String cdmIngredientSynonym = queryRow.get("concept_synonym_name").trim();
				if ((cdmIngredientSynonym != null) && (!cdmIngredientSynonym.equals(""))) {
					lastCdmIngredient.addSynonym(cdmIngredientSynonym);
				}
			}
			
			// Get "Maps to" RxNorm Ingredients
			Map<String, Set<CDMIngredient>> mapsToCDMIngredient = new HashMap<String, Set<CDMIngredient>>();
			Map<String, Set<String>> sourceNameSynonyms = new HashMap<String, Set<String>>();
			for (Row queryRow : connection.queryResource("cdm/GetMapsToRxNormIngredients.sql", queryParameters)) {
				String sourceName = queryRow.get("source_name").trim();
				String cdmIngredientConceptId = queryRow.get("concept_id").trim();
				String sourceNameSynonym = queryRow.get("concept_synonym_name").trim();
				CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
				Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(sourceName);
				if (ingredientSet == null) {
					ingredientSet = new HashSet<CDMIngredient>();
					mapsToCDMIngredient.put(sourceName, ingredientSet);
				}
				ingredientSet.add(cdmIngredient);
				if (!sourceNameSynonym.equals("")) {
					Set<String> synonyms = sourceNameSynonyms.get(sourceName);
					if (synonyms == null) {
						synonyms = new HashSet<String>();
						sourceNameSynonyms.put(sourceName, synonyms);
					}
					synonyms.add(sourceNameSynonym);
				}
			}
			
			// Close database connection
			connection.close();

			System.out.println("    Found " + Integer.toString(cdmIngredients.size()) + " CDM RxNorm ingredients");
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			

			// Match ingredients by name
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
				connection = database.getRichConnection(this.getClass());
				
				// Match RxNorm Ingredients on IngredientNameEnglish 
				for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {

					boolean match = false;
					Set<CDMIngredient> matchingRxNormIngredients = new HashSet<CDMIngredient>();
					Set<CDMIngredient> matchingRxNormIngredientsSynonyms = new HashSet<CDMIngredient>();
					
					for (CDMIngredient cdmIngredient : cdmIngredientsList) {
						if (cdmIngredient.getConceptName().contains(sourceIngredient.getIngredientNameEnglish())) {
							matchingRxNormIngredients.add(cdmIngredient);
						}
					}
					if (matchingRxNormIngredients.size() == 1) {
						ingredientMap.put(sourceIngredient, (CDMIngredient) matchingRxNormIngredients.toArray()[0]);
						sourceIngredient.setMatch(SourceIngredient.MATCH_SINGLE);
						match = true;
					}
					
					if ((!match) && (matchingRxNormIngredients.size() > 1)) {
						Set<CDMIngredient> exactMatch = new HashSet<CDMIngredient>();
						for (CDMIngredient cdmIngredient : matchingRxNormIngredients) {
							if (cdmIngredient.getConceptName().equals(sourceIngredient.getIngredientNameEnglish())) {
								exactMatch.add(cdmIngredient);
							}
						}
						if (exactMatch.size() == 1) {
							ingredientMap.put(sourceIngredient, (CDMIngredient) exactMatch.toArray()[0]);
							sourceIngredient.setMatch(SourceIngredient.MATCH_EXACT);
							match = true;
						}
					}
					
					if (!match) {
						// Match on synonyms
						for (CDMIngredient cdmIngredient : cdmIngredientsList) {
							for (String synonym : cdmIngredient.getSynonyms()) {
								if (synonym.contains(sourceIngredient.getIngredientNameEnglish())) {
									matchingRxNormIngredientsSynonyms.add(cdmIngredient);
								}
							}
						}
						if (matchingRxNormIngredientsSynonyms.size() == 1) {
							ingredientMap.put(sourceIngredient, (CDMIngredient) matchingRxNormIngredientsSynonyms.toArray()[0]);
							sourceIngredient.setMatch(SourceIngredient.MATCH_SINGLE_SYNONYM);
							match = true;
						}
						if ((!match) && (matchingRxNormIngredientsSynonyms.size() > 1)) {
							Set<CDMIngredient> exactMatch = new HashSet<CDMIngredient>();
							for (CDMIngredient cdmIngredient : matchingRxNormIngredientsSynonyms) {
								for (String synonym : cdmIngredient.getSynonyms()) {
									if (synonym.equals(sourceIngredient.getIngredientNameEnglish())) {
										exactMatch.add(cdmIngredient);
										break;
									}
								}
							}
							if (exactMatch.size() == 1) {
								ingredientMap.put(sourceIngredient, (CDMIngredient) exactMatch.toArray()[0]);
								sourceIngredient.setMatch(SourceIngredient.MATCH_EXACT_SYNONYM);
								match = true;
							}
						}
					}
					
					if (match) {
						CDMIngredient cdmIngredient = ingredientMap.get(sourceIngredient);
						if (cdmIngredient != null) {
							//System.out.println("    " + sourceIngredient);
							//System.out.println("        " + sourceIngredient.getMatch() + " " + cdmIngredient);
							ingredientMappingFile.println(sourceIngredient + "," + sourceIngredient.getMatch() + "," + cdmIngredient);
						}
					}
					else {
						/*
						System.out.println("    " + sourceIngredient);
						for (CDMIngredient cdmIngredient : matchingCDMIngredients) {
							System.out.println("        Matching CDM Ingredient: " + cdmIngredient);
						}
						for (CDMIngredient cdmIngredient : matchingCDMIngredientsSynonyms) {
							for (String synonym : cdmIngredient.getSynonyms()) {
								if (synonym.equals(sourceIngredient.getIngredientNameEnglish())) {
									System.out.println("        Matching CDM Ingredient Synonym: " + synonym + " -> " + cdmIngredient);
								}
							}
						}
						*/
						
						// Match on "Maps to" RxNorm Ingredient
						match = false;
						Set<CDMIngredient> matchingMapsToRxNormIngredients = new HashSet<CDMIngredient>();
						Set<String> matchingSourceNames = new HashSet<String>();
						Set<String> matchingMapsToRxNormIngredientsSynonyms = new HashSet<String>();
						
						for (String sourceName : mapsToCDMIngredient.keySet()) {
							Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(sourceName);
							if ((ingredientSet.size() == 1) && sourceName.contains(sourceIngredient.getIngredientNameEnglish())) {
								matchingMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
								matchingSourceNames.add(sourceName);
							}
						}
						if (matchingMapsToRxNormIngredients.size() == 1) {
							ingredientMap.put(sourceIngredient, (CDMIngredient) matchingMapsToRxNormIngredients.toArray()[0]);
							sourceIngredient.setMatch(SourceIngredient.MATCH_MAPSTO_SINGLE);
							match = true;
						}
						
						if ((!match) && (matchingMapsToRxNormIngredients.size() > 1)) {
							Set<String> exactMatch = new HashSet<String>();
							for (String sourceName : matchingSourceNames) {
								if (sourceName.equals(sourceIngredient.getIngredientNameEnglish())) {
									exactMatch.add(sourceName);
								}
							}
							if (exactMatch.size() == 1) {
								ingredientMap.put(sourceIngredient, (CDMIngredient) mapsToCDMIngredient.get((String) exactMatch.toArray()[0]).toArray()[0]);
								sourceIngredient.setMatch(SourceIngredient.MATCH_MAPSTO_EXACT);
								match = true;
							}
						}
						
						if (!match) {
							// Match on synonyms
							matchingMapsToRxNormIngredients = new HashSet<CDMIngredient>();
							for (String sourceName : sourceNameSynonyms.keySet()) {
								for (String synonym : sourceNameSynonyms.get(sourceName)) {
									Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(sourceName);
									if ((ingredientSet.size() == 1) && synonym.contains(sourceIngredient.getIngredientNameEnglish())) {
										matchingMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
										matchingMapsToRxNormIngredientsSynonyms.add(sourceName);
									}
								}
							}
							if (matchingMapsToRxNormIngredientsSynonyms.size() == 1) {
								ingredientMap.put(sourceIngredient, (CDMIngredient) matchingMapsToRxNormIngredients.toArray()[0]);
								sourceIngredient.setMatch(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM);
								match = true;
							}
							if ((!match) && (matchingMapsToRxNormIngredientsSynonyms.size() > 1)) {
								Set<String> exactMatch = new HashSet<String>();
								for (String sourceName : matchingMapsToRxNormIngredientsSynonyms) {
									for (String synonym : sourceNameSynonyms.get(sourceName)) {
										if (synonym.equals(sourceIngredient.getIngredientNameEnglish())) {
											exactMatch.add(sourceName);
											break;
										}
									}
								}
								if (exactMatch.size() == 1) {
									ingredientMap.put(sourceIngredient, (CDMIngredient) mapsToCDMIngredient.get((String) exactMatch.toArray()[0]).toArray()[0]);
									sourceIngredient.setMatch(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM);
									match = true;
								}
							}
						}
						
						if (match) {
							CDMIngredient cdmIngredient = ingredientMap.get(sourceIngredient);
							if (cdmIngredient != null) {
								//System.out.println("    " + sourceIngredient);
								//System.out.println("        " + sourceIngredient.getMatch() + " " + cdmIngredient);
								ingredientMappingFile.println(sourceIngredient + "," + sourceIngredient.getMatch() + "," + cdmIngredient);
							}
						}
						else {
							//TODO
						}
					}
				}
				
				int noIngredientsCount = 0;
				int singleIngredientCount = 0;
				int withIngredientsCount = 0;
				int fullMatchCount = 0;
				for (SourceDrug sourceDrug : sourceDrugs) {
					if (sourceDrug.getIngredients().size() == 1) {
						singleIngredientCount++;
					}
					
					if (sourceDrug.getIngredients().size() == 0) {
						noIngredientsCount++;
					}
					else {
						withIngredientsCount++;
						boolean fullMatch = true;
						for (SourceIngredient sourceIngredient : sourceDrug.getIngredients()) {
							if (sourceIngredient.getMatch().equals(SourceIngredient.NO_MATCH)) {
								fullMatch = false;
								break;
							}
						}
						if (fullMatch) {
							fullMatchCount++;
						}
						
					}
				}

				Map<String, Integer> match = new HashMap<String, Integer>();
				match.put(SourceIngredient.NO_MATCH, 0);
				match.put(SourceIngredient.MATCH_SINGLE, 0);
				match.put(SourceIngredient.MATCH_EXACT, 0);
				match.put(SourceIngredient.MATCH_SINGLE_SYNONYM, 0);
				match.put(SourceIngredient.MATCH_EXACT_SYNONYM, 0);
				match.put(SourceIngredient.MATCH_MAPSTO_SINGLE, 0);
				match.put(SourceIngredient.MATCH_MAPSTO_EXACT, 0);
				match.put(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM, 0);
				match.put(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM, 0);
				for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
					Integer count = match.get(sourceIngredient.getMatch());
					match.put(sourceIngredient.getMatch(), match.get(sourceIngredient.getMatch()) + 1);
				}
				
				System.out.println("");
				System.out.println("    Source drugs: " + Integer.toString(sourceDrugs.size()));
				System.out.println("    Source drugs without ATC: " + Integer.toString(noATCCounter));
				System.out.println("    Source drugs without ingredients: " + Integer.toString(noIngredientsCount));
				System.out.println("    Source drugs with one ingredient: " + Integer.toString(singleIngredientCount));
				System.out.println("    Source drugs with ingredients: " + Integer.toString(withIngredientsCount));
				System.out.println("    Unique source ingredients: " + Integer.toString(SourceDrug.getAllIngredients().size()));
				
				System.out.println("    Matched source ingredients:");
				System.out.println("      " + SourceIngredient.NO_MATCH + ": " + Integer.toString(match.get(SourceIngredient.NO_MATCH)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.NO_MATCH) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				
				System.out.println("      " + SourceIngredient.MATCH_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("      " + SourceIngredient.MATCH_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("      " + SourceIngredient.MATCH_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("      " + SourceIngredient.MATCH_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

				System.out.println("      " + SourceIngredient.MATCH_MAPSTO_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("      " + SourceIngredient.MATCH_MAPSTO_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("      " + SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("      " + SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				
				System.out.println("      TOTAL: " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				
				System.out.println("    Source drugs with all ingredients matched: " + Integer.toString(fullMatchCount) + " (" + Long.toString(Math.round(((double) fullMatchCount / (double) withIngredientsCount) * 100)) + "%)");
				System.out.println(DrugMapping.getCurrentTime() + " Finished");
			}	
			
			
			// Close all output files
			missingATCFile.close();
			ingredientMappingFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
		}
	}

}
