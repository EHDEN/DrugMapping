package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.drugmapping.cdm.CDMDrug;
import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.cdm.CDMIngredientStrength;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class MapGenericDrugs extends Mapping {
	private Set<String> forms = new HashSet<String>();
	private Set<String> units = new HashSet<String>();

	private Map<String, String> casMap = new HashMap<String, String>();
	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, String> ingredientTranslationCorrections = new HashMap<String, String>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<String, Set<CDMIngredient>> mapsToCDMIngredient = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<String>> drugNameSynonyms = new HashMap<String, Set<String>>();
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<SourceDrug, CDMDrug> drugMapping = new HashMap<SourceDrug, CDMDrug>();
	
	private int noATCCounter = 0; // Counter of source drugs without ATC code.
	
	
	private static List<String> getNameVariants(String name) {
		List<String> nameVariants = null;
		if (name.contains(" ")) {
			// Name contains several words.
			// Create variants with decreasing number of words (cutting from the end).
			// Also create variants with the last 'e' of the words removed.
			nameVariants = new ArrayList<String>();
			Map<Integer, List<String>> nameWordVariants = new HashMap<Integer, List<String>>();
			String[] nameSplit = name.split(" ");
			for (int wordNr = 0; wordNr < nameSplit.length; wordNr++) {
				List<String> wordVariants = new ArrayList<String>();
				String word = nameSplit[wordNr];
				wordVariants.add(word);
				if (word.contains("F")) {
					String fWord = word;
					while (fWord.contains("F")) {
						fWord = fWord.replaceFirst("F", "PH");
						wordVariants.add(fWord);
					}
				}
				if (word.contains("PH")) {
					String phword = word;
					while (phword.contains("PH")) {
						phword = phword.replaceFirst("PH", "F");
						wordVariants.add(phword);
					}
				}
				int wordVariantCount = wordVariants.size();
				for (int wordVariantNr = 0; wordVariantNr < wordVariantCount; wordVariantNr++) {
					String wordVariant = wordVariants.get(wordVariantNr);
					if (wordVariant.substring(wordVariant.length() - 1, wordVariant.length()).equals("E")) {
						wordVariants.add(wordVariant.substring(0, wordVariant.length() - 1));
					}
				}
				nameWordVariants.put(wordNr, wordVariants);
			}
			
			for (int length = nameSplit.length; length > 0; length--) {
				List<String> variants = new ArrayList<String>();
				
				for (int partNr = 0; partNr < length; partNr++) {
					if (partNr == 0) {
						for (String wordvariant : nameWordVariants.get(partNr)) {
							variants.add(wordvariant);
						}
					}
					else {
						List<String> variantPrefixes = variants;
						variants = new ArrayList<String>();
						for (String wordvariant : nameWordVariants.get(partNr)) {
							for (String variantPrefix : variantPrefixes) {
								variants.add(variantPrefix + " " + wordvariant);
							}
						}
					}
				}
				nameVariants.addAll(variants);
			}
		}
		if (name.contains("-")) { 
			if (nameVariants == null) {
				nameVariants = new ArrayList<String>();
			}
			List<String> removedDashVariants = getNameVariants(name.replaceAll("-", ""));
			if (removedDashVariants != null) {
				nameVariants.addAll(removedDashVariants);
			}
			List<String> replacedDashVariants = getNameVariants(name.replaceAll("- ", " ").replaceAll("-", " "));
			if (replacedDashVariants != null) {
				nameVariants.addAll(replacedDashVariants);
			}
		}
		
		if (nameVariants != null) {
			// Sort the variants on the number of words descending.
			Collections.sort(nameVariants, new Comparator<String>() {
				@Override
				public int compare(String name1, String name2) {
					String[] name1Split = name1.split(" ");
					String[] name2Split = name2.split(" ");
					return (name1Split.length == name2Split.length ? 0 : (name1Split.length > name2Split.length ? -1 : 1));
				}
			});
			
			// Remove spaces for comparison
			for (int variantNr = 0; variantNr < nameVariants.size(); variantNr++) {
				nameVariants.set(variantNr, nameVariants.get(variantNr).replaceAll(" ", ""));
			}
		}
		
		return nameVariants;
	}
		
	
	public MapGenericDrugs(CDMDatabase database, InputFile unitMappingsFile, InputFile formMappingsFile, InputFile sourceDrugsFile, InputFile translationCorrectionsFile) {
		
		QueryParameters queryParameters;
				
		String fileName = "";
		try {
			// Create all output files
			System.out.println(DrugMapping.getCurrentTime() + " Creating output files ...");
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
			System.out.println("    " + fileName);
			PrintWriter missingATCFile = new PrintWriter(new File(fileName));
			SourceDrug.writeHeaderToFile(missingATCFile);
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping RxNorm Ingredients.csv";
			System.out.println("    " + fileName);
			PrintWriter rxNormIngredientsFile = new PrintWriter(new File(fileName));
			rxNormIngredientsFile.println(CDMIngredient.getHeaderWithSynonyms());
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Maps To RxNorm Ingredients.csv";
			System.out.println("    " + fileName);
			PrintWriter mapsToRxNormIngredientsFile = new PrintWriter(new File(fileName));
			mapsToRxNormIngredientsFile.println("SourceName,Synonym," + CDMIngredient.getHeader());

			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Ingredient Mapping.csv";
			System.out.println("    " + fileName);
			PrintWriter ingredientMappingFile = new PrintWriter(new File(fileName));
			ingredientMappingFile.println(SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			String casMappingFileName = "DrugMapping - CAS Mapping.csv";
			File casMappingFile = new File(DrugMapping.getCurrentPath() + "/" + casMappingFileName);
			if (casMappingFile.exists() && casMappingFile.canRead()) {
				System.out.println(DrugMapping.getCurrentTime() + " Loading CAS names from '" + casMappingFileName + "' ...");
				ReadCSVFileWithHeader casMappingFileReader = new ReadCSVFileWithHeader(DrugMapping.getCurrentPath() + "/" + casMappingFileName, ',', '"');
				Iterator<Row> casMappingFileIterator = casMappingFileReader.iterator();
				while (casMappingFileIterator.hasNext()) {
					Row row = casMappingFileIterator.next();
					String chemicalName = row.get("ChemicalName").trim().toUpperCase().replaceAll(" ", "").replaceAll("-", "");
					String casNumber = row.get("CasRN").trim().replace("-", "");
					if ((!chemicalName.equals("")) && (!casNumber.equals(""))) {
						casMap.put(casNumber, chemicalName);
					}
				}
				System.out.println(DrugMapping.getCurrentTime() + " Finished");
			}
			else {
				System.out.println(DrugMapping.getCurrentTime() + "CAS Mapping File '" + DrugMapping.getCurrentPath() + "/" + casMappingFileName + "' NOT found.");
			}
			
			
			// Load manual translation corrections
			if (translationCorrectionsFile.openFile()) {
				System.out.println(DrugMapping.getCurrentTime() + " Loading translation corrections ...");

				while (translationCorrectionsFile.hasNext()) {
					Row row = translationCorrectionsFile.next();
					String ingredientName = translationCorrectionsFile.get(row, "IngredientName").trim().toUpperCase();
					String ingredientNameEnglish = translationCorrectionsFile.get(row, "IngredientNameEnglish").trim().toUpperCase();
					if ((!ingredientName.equals("")) && (!ingredientNameEnglish.equals(""))) {
						ingredientTranslationCorrections.put(ingredientName, ingredientNameEnglish);
					}
				}
				System.out.println(DrugMapping.getCurrentTime() + " Finished");
			}


			boolean sourceDrugError = false;
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
							
							String form = sourceDrug.getFormulation();
							if (form != null) {
								forms.add(form);
							}
							
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
						
						// Apply manual translation correction
						if (ingredientTranslationCorrections.containsKey(ingredientName)) {
							ingredientNameEnglish = ingredientTranslationCorrections.get(ingredientName);
						}
						
						while (ingredientName.contains("  "))        ingredientName        = ingredientName.replaceAll("  ", " ");
						while (ingredientNameEnglish.contains("  ")) ingredientNameEnglish = ingredientNameEnglish.replaceAll("  ", " ");
						while (dosage.contains("  "))                dosage                = dosage.replaceAll("  ", " ");
						while (dosageUnit.contains("  "))            dosageUnit            = dosageUnit.replaceAll("  ", " ");
						while (casNumber.contains("  "))             casNumber             = casNumber.replaceAll("  ", " ");
						
						if (!ingredientName.equals("")) {
							SourceIngredient sourceIngredient = SourceDrug.findIngredient(ingredientName, ingredientNameEnglish, casNumber);
							if (SourceDrug.errorOccurred()) {
								sourceDrugError = true;
							}
							else if (sourceIngredient == null) {
								sourceIngredient = sourceDrug.AddIngredient(ingredientName, ingredientNameEnglish, casNumber, dosage, dosageUnit);
							}
							
							String unit = sourceDrug.getIngredientDosageUnit(sourceIngredient);
							if (unit != null) {
								units.add(unit);
							}
						}
					}
				}
			}

			System.out.println("    Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
			System.out.println("    Found " + Integer.toString(forms.size()) + " source forms");
			System.out.println("    Found " + Integer.toString(units.size()) + " source units");
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			if (!sourceDrugError) {
							
							// Create Units Map
							UnitConversion unitConversionsMap = new UnitConversion(database, units);
							if (unitConversionsMap.getStatus() == UnitConversion.STATE_EMPTY) {
								// If no unit conversion is specified then stop.
								System.out.println("");
								System.out.println("FIRST FILL THE UNIT CONVERSION MAP IN THE FILE:");
								System.out.println("");
								System.out.println(DrugMapping.getCurrentPath() + "/" + UnitConversion.FILENAME);
							}
							
							FormConversion formConversionsMap = new FormConversion(database, forms);
							if (formConversionsMap.getStatus() == FormConversion.STATE_EMPTY) {
								// If no form conversion is specified then stop.
								System.out.println("");
								System.out.println("FIRST FILL THE FORM CONVERSION MAP IN THE FILE:");
								System.out.println("");
								System.out.println(DrugMapping.getCurrentPath() + "/" + FormConversion.FILENAME);
							}


							if ((unitConversionsMap.getStatus() != UnitConversion.STATE_ERROR) && (formConversionsMap.getStatus() != FormConversion.STATE_ERROR)) {
								// Match ingredients by name
							
								queryParameters = new QueryParameters();
								queryParameters.set("@vocab", database.getVocabSchema());
							
								// Connect to the database
								RichConnection connection = database.getRichConnection(this.getClass());								
								
								// Load CDM ingredients
								System.out.println(DrugMapping.getCurrentTime() + " Get CDM ingredients ...");
								
								// Get RxNorm ingredients
								CDMIngredient lastCdmIngredient = null;
								for (Row queryRow : connection.queryResource("cdm/GetRxNormIngredients.sql", queryParameters)) {
									String cdmIngredientConceptId = queryRow.get("concept_id").trim();
									if ((lastCdmIngredient == null) || (!lastCdmIngredient.getConceptId().equals(cdmIngredientConceptId))) {
										if (lastCdmIngredient != null) {
											rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
										}
										CDMIngredient cdmIngredient = new CDMIngredient(queryRow, "");
										cdmIngredients.put(cdmIngredient.getConceptId(), cdmIngredient);
										cdmIngredientsList.add(cdmIngredient);
										lastCdmIngredient = cdmIngredient;
									}
									String cdmIngredientSynonym = queryRow.get("concept_synonym_name").trim().toUpperCase();
									if ((cdmIngredientSynonym != null) && (!cdmIngredientSynonym.equals(""))) {
										lastCdmIngredient.addSynonym(cdmIngredientSynonym);
									}
									String ingredientName = lastCdmIngredient.getConceptName();
									while (ingredientName.contains("F")) {
										ingredientName = ingredientName.replaceFirst("F", "PH");
										lastCdmIngredient.addSynonym(ingredientName);
									}
								}
								if (lastCdmIngredient != null) {
									rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
								}
								
								// Get "Maps to" RxNorm Ingredients
								for (Row queryRow : connection.queryResource("cdm/GetMapsToRxNormIngredients.sql", queryParameters)) {
									String drugName = queryRow.get("drug_name").trim().toUpperCase();
									String cdmIngredientConceptId = queryRow.get("mapsto_concept_id").trim();
									String drugNameSynonym = queryRow.get("drug_synonym_name").trim().toUpperCase();
									CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
									Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(drugName);
									if (ingredientSet == null) {
										ingredientSet = new HashSet<CDMIngredient>();
										mapsToCDMIngredient.put(drugName, ingredientSet);
									}
									ingredientSet.add(cdmIngredient);
									if (!drugNameSynonym.equals("")) {
										Set<String> synonyms = drugNameSynonyms.get(drugName);
										if (synonyms == null) {
											synonyms = new HashSet<String>();
											drugNameSynonyms.put(drugName, synonyms);
										}
										synonyms.add(drugNameSynonym);
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
									
									for (CDMIngredient cdmIngredient : cdmIngredients) {
										for (String synonym : synonyms) {
											String record = "\"" + drugName + "\"";
											record += "," + "\"" + synonym + "\"";
											record += "," + cdmIngredient.toString();
											mapsToRxNormIngredientsFile.println(record);
										}
									}
								}
								System.out.println("    Found " + Integer.toString(cdmIngredients.size()) + " CDM RxNorm ingredients");
								System.out.println(DrugMapping.getCurrentTime() + " Finished");
								
								
								// Get RxNorm Clinical Drugs with Form and Ingredients
								System.out.println(DrugMapping.getCurrentTime() + " Get CDM RxNorm Clinical Drugs with ingredients ...");
								
								for (Row queryRow : connection.queryResource("cdm/GetRxNormClinicalDrugsIngredients.sql", queryParameters)) {
									String cdmDrugConceptId = queryRow.get("drug_concept_id");
									if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
										CDMDrug cdmDrug = cdmDrugs.get(cdmDrugConceptId);
										if (cdmDrug == null) {
											cdmDrug = new CDMDrug(queryRow, "drug_");
											cdmDrugs.put(cdmDrug.getConceptId(), cdmDrug);
										}
										String cdmFormConceptId = queryRow.get("form_concept_id");
										cdmDrug.setForm(cdmFormConceptId);
										
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
												drugsContainingIngredient.add(cdmDrug);
											}
										}
									} 
								}
								
								System.out.println(DrugMapping.getCurrentTime() + " Finished");
								
								
								// Close database connection
								connection.close();

								
								System.out.println(DrugMapping.getCurrentTime() + " Match ingredients by name ...");
								
								// Match RxNorm Ingredients on IngredientNameEnglish 
								for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {

									//System.out.println(sourceIngredient);
									boolean match = false;

									// Matches on name
									if (!match) {
										// Match RxNorm Ingredient by name 
										match = matchIngredientByName(sourceIngredient, false, null);
									}
									
									if (!match) {
										// Match RxNorm Ingredient by synonym name
										match = matchIngredientBySynonymName(sourceIngredient, false, null);
									}
									
									if (!match) {
										// Match on "Maps to" RxNorm Ingredient by name
										match = matchIngredientByNameMapsTo(sourceIngredient, false, null);
									}
									
									if (!match) {
										// Match on "Maps to" synonym name
										match = matchIngredientBySynonymNameMapsTo(sourceIngredient, false, null);
									}

									// Matches on CAS number
									if (!match) {
										// Match RxNorm Ingredient by CAS name 
										match = matchIngredientByName(sourceIngredient, true, null);
									}
									
									if (!match) {
										// Match RxNorm Ingredient by synonym CAS name
										match = matchIngredientBySynonymName(sourceIngredient, true, null);
									}
									
									if (!match) {
										// Match on "Maps to" RxNorm Ingredient by CAS name
										match = matchIngredientByNameMapsTo(sourceIngredient, true, null);
									}
									
									if (!match) {
										// Match on "Maps to" synonym CAS name
										match = matchIngredientBySynonymNameMapsTo(sourceIngredient, true, null);
									}
									
									// Matches on name variants
									if (!match) {
										// Match on generated name variants
										List<String> nameVariants = getNameVariants(sourceIngredient.getIngredientNameEnglish());
										
										if (nameVariants != null) {
											match = matchIngredientByName(sourceIngredient, false, nameVariants);
											
											if (!match) {
												// Match RxNorm Ingredient by synonym name
												match = matchIngredientBySynonymName(sourceIngredient, false, nameVariants);
											}
											
											if (!match) {
												// Match on "Maps to" RxNorm Ingredient
												match = matchIngredientByNameMapsTo(sourceIngredient, false, nameVariants);
											}
											
											if (!match) {
												// Match on "Maps to" synonyms
												match = matchIngredientBySynonymNameMapsTo(sourceIngredient, false, nameVariants);
											}
										}
									}
									
									if (match) {
										CDMIngredient cdmIngredient = ingredientMap.get(sourceIngredient);
										if (cdmIngredient != null) {
											//System.out.println("    " + sourceIngredient);
											//System.out.println("        " + sourceIngredient.getMatch() + " " + cdmIngredient);
											ingredientMappingFile.println(sourceIngredient + "," + sourceIngredient.getMatch() + "," + "\"" + sourceIngredient.getMatchString() + "\"" + "," + "\"" + sourceIngredient.getMatchingDrug() + "\"" + "," + cdmIngredient);
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

								Set<String> nameMatches = new HashSet<String>();
								nameMatches.add(SourceIngredient.MATCH_SINGLE);
								nameMatches.add(SourceIngredient.MATCH_EXACT);
								nameMatches.add(SourceIngredient.MATCH_SINGLE_SYNONYM);
								nameMatches.add(SourceIngredient.MATCH_EXACT_SYNONYM);
								nameMatches.add(SourceIngredient.MATCH_MAPSTO_SINGLE);
								nameMatches.add(SourceIngredient.MATCH_MAPSTO_EXACT);
								nameMatches.add(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM);
								nameMatches.add(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM);
								
								Set<String> casMatches = new HashSet<String>();
								casMatches.add(SourceIngredient.MATCH_CAS_SINGLE);
								casMatches.add(SourceIngredient.MATCH_CAS_EXACT);
								casMatches.add(SourceIngredient.MATCH_CAS_SINGLE_SYNONYM);
								casMatches.add(SourceIngredient.MATCH_CAS_EXACT_SYNONYM);
								casMatches.add(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE);
								casMatches.add(SourceIngredient.MATCH_CAS_MAPSTO_EXACT);
								casMatches.add(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM);
								casMatches.add(SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM);
								
								Set<String> variantMatches = new HashSet<String>();
								variantMatches.add(SourceIngredient.MATCH_VARIANT_SINGLE);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_EXACT);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM);
								variantMatches.add(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM);
								
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
								match.put(SourceIngredient.MATCH_CAS_SINGLE, 0);
								match.put(SourceIngredient.MATCH_CAS_EXACT, 0);
								match.put(SourceIngredient.MATCH_CAS_SINGLE_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_CAS_EXACT_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE, 0);
								match.put(SourceIngredient.MATCH_CAS_MAPSTO_EXACT, 0);
								match.put(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_VARIANT_SINGLE, 0);
								match.put(SourceIngredient.MATCH_VARIANT_EXACT, 0);
								match.put(SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE, 0);
								match.put(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT, 0);
								match.put(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM, 0);
								match.put(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM, 0);
								
								int totalNameMatch = 0;
								int totalCASMatch = 0;
								int totalVariantMatch = 0;
								for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
									match.put(sourceIngredient.getMatch(), match.get(sourceIngredient.getMatch()) + 1);
									
									if (nameMatches.contains(sourceIngredient.getMatch())) {
										totalNameMatch++;
									}
									else if (casMatches.contains(sourceIngredient.getMatch())) {
										totalCASMatch++;
									}
									else if (variantMatches.contains(sourceIngredient.getMatch())) {
										totalVariantMatch++;
									}
								}
								
								System.out.println("");
								System.out.println("    Source drugs: " + Integer.toString(sourceDrugs.size()));
								System.out.println("    Source drugs without ATC: " + Integer.toString(noATCCounter));
								System.out.println("    Source drugs without ingredients: " + Integer.toString(noIngredientsCount));
								System.out.println("    Source drugs with one ingredient: " + Integer.toString(singleIngredientCount));
								System.out.println("    Source drugs with ingredients: " + Integer.toString(withIngredientsCount));
								System.out.println("    Unique source ingredients: " + Integer.toString(SourceDrug.getAllIngredients().size()));
								
								System.out.println();
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
								
								System.out.println("      " + SourceIngredient.MATCH_CAS_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_CAS_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_CAS_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_CAS_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

								System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

								System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

								System.out.println();
								System.out.println("      TOTAL NAME MATCHES: " + Integer.toString(totalNameMatch) + " (" + Long.toString(Math.round(((double) totalNameMatch / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      TOTAL CAS MATCHES: " + Integer.toString(totalCASMatch) + " (" + Long.toString(Math.round(((double) totalCASMatch / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      TOTAL VARIANT MATCHES: " + Integer.toString(totalVariantMatch) + " (" + Long.toString(Math.round(((double) totalVariantMatch / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								System.out.println("      TOTAL MATCHES: " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
								
								System.out.println("    Source drugs with all ingredients matched: " + Integer.toString(fullMatchCount) + " (" + Long.toString(Math.round(((double) fullMatchCount / (double) withIngredientsCount) * 100)) + "%)");
								System.out.println(DrugMapping.getCurrentTime() + " Finished");
								
								
								

								System.out.println(DrugMapping.getCurrentTime() + " Map Source Drugs to RxNorm Clinical Drugs ...");
								
								for (SourceDrug sourceDrug : sourceDrugs) {
									String sourceDrugForm = sourceDrug.getFormulation();
									
									List<SourceIngredient> sourceIngredients = sourceDrug.getIngredients();
									List<CDMIngredient> mappedCDMIngredients = new ArrayList<CDMIngredient>();
									for (SourceIngredient sourceIngredient : sourceIngredients) {
										mappedCDMIngredients.add(ingredientMap.get(sourceIngredient));
									}
									if (!mappedCDMIngredients.contains(null)) {
										// All ingredients are mapped
										boolean firstIngredient = true;
										Set<CDMDrug> cdmDrugsWithIngredients = new HashSet<CDMDrug>();
										for (CDMIngredient mappedCDMIngredient : mappedCDMIngredients) {
											List<CDMDrug> ingredientDrugs = cdmDrugsContainingIngredient.get(mappedCDMIngredient);
											if (firstIngredient) {
												if (ingredientDrugs != null) {
													cdmDrugsWithIngredients.addAll(ingredientDrugs);
												}
												firstIngredient = false;
											}
											else {
												Set<CDMDrug> remove = new HashSet<CDMDrug>();
												for (CDMDrug ingredientDrug : cdmDrugsWithIngredients) {
													if (!ingredientDrugs.contains(ingredientDrug)) {
														remove.add(ingredientDrug);
													}
												}
												cdmDrugsWithIngredients.removeAll(remove);
											}
											if (cdmDrugsWithIngredients.size() == 0) {
												break;
											}
										}

										// Drugs found containing all ingredients.

										if (sourceDrugForm != null) {
											// Check form
											if (cdmDrugsWithIngredients.size() > 0) {
												Set<CDMDrug> remove = new HashSet<CDMDrug>();
												for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
													String cdmDrugForm = cdmDrug.getForm();
													if ((cdmDrugForm == null) || (!formConversionsMap.matches(sourceDrugForm, cdmDrugForm))) {
														remove.add(cdmDrug);
													} 
												}
												cdmDrugsWithIngredients.removeAll(remove);
											}
										}
										
										// Check ingredient strengths.
										if (cdmDrugsWithIngredients.size() > 0) {
											//TODO
										}
									}
								}
								
								System.out.println(DrugMapping.getCurrentTime() + " Finished");
							}
			}	
			
			
			// Close all output files
			missingATCFile.close();
			rxNormIngredientsFile.close();
			mapsToRxNormIngredientsFile.close();
			ingredientMappingFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
		}
	}
	
	
	private boolean matchIngredientByName(SourceIngredient sourceIngredient, boolean useCASName, List<String> nameVariants) {
		boolean match = false;
		boolean noVariants = false;
		
		if (nameVariants == null) {
			noVariants = true;
			nameVariants = new ArrayList<String>();
			if (useCASName) {
				String casNumber = sourceIngredient.getCASNumber();
				if (casNumber != null) {
					String casName = casMap.get(casNumber);
					if (casName != null) {
						nameVariants.add(casName);
					}
				}
			}
			else {
				nameVariants.add(sourceIngredient.getIngredientNameEnglishNoSpaces());
				nameVariants.add(sourceIngredient.getIngredientNameNoSpaces());
			}
		}
		
		for (String nameVariant : nameVariants) {
			Set<CDMIngredient> matchingRxNormIngredients = new HashSet<CDMIngredient>();
			
			for (CDMIngredient cdmIngredient : cdmIngredientsList) {
				if (cdmIngredient.getConceptName().replaceAll(" ", "").contains(nameVariant)) {
					matchingRxNormIngredients.add(cdmIngredient);
				}
			}
			if (matchingRxNormIngredients.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_SINGLE : SourceIngredient.MATCH_SINGLE) : SourceIngredient.MATCH_VARIANT_SINGLE);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((CDMIngredient) matchingRxNormIngredients.toArray()[0]).toString().replaceAll("\"", "'")); 
				match = true;
				break;
			}
			
			if ((!match) && (matchingRxNormIngredients.size() > 1)) {
				Set<CDMIngredient> exactMatch = new HashSet<CDMIngredient>();
				for (CDMIngredient cdmIngredient : matchingRxNormIngredients) {
					if (cdmIngredient.getConceptName().replaceAll(" ", "").equals(nameVariant)) {
						exactMatch.add(cdmIngredient);
					}
				}
				if (exactMatch.size() == 1) {
					ingredientMap.put(sourceIngredient, (CDMIngredient) exactMatch.toArray()[0]);
					sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_EXACT : SourceIngredient.MATCH_EXACT) : SourceIngredient.MATCH_VARIANT_EXACT);
					sourceIngredient.setMatchString(nameVariant);
					sourceIngredient.setMatchingDrug(((CDMIngredient) exactMatch.toArray()[0]).toString().replaceAll("\"", "'")); 
					match = true;
					break;
				}
			}
		}
		return match;
	}
	
	
	private boolean matchIngredientBySynonymName(SourceIngredient sourceIngredient, boolean useCASName, List<String> nameVariants) {
		boolean match = false;
		boolean noVariants = false;
		
		if (nameVariants == null) {
			noVariants = true;
			nameVariants = new ArrayList<String>();
			if (useCASName) {
				String casNumber = sourceIngredient.getCASNumber();
				if (casNumber != null) {
					String casName = casMap.get(casNumber);
					if (casName != null) {
						nameVariants.add(casName);
					}
				}
			}
			else {
				nameVariants.add(sourceIngredient.getIngredientNameEnglishNoSpaces());
				nameVariants.add(sourceIngredient.getIngredientNameNoSpaces());
			}
		}
		
		for (String nameVariant : nameVariants) {
			Set<CDMIngredient> matchingRxNormIngredientsSynonyms = new HashSet<CDMIngredient>();
			
			// Match on synonyms
			for (CDMIngredient cdmIngredient : cdmIngredientsList) {
				for (String synonym : cdmIngredient.getSynonyms()) {
					if (synonym.replaceAll(" ", "").contains(nameVariant)) {
						matchingRxNormIngredientsSynonyms.add(cdmIngredient);
					}
				}
			}
			if (matchingRxNormIngredientsSynonyms.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingRxNormIngredientsSynonyms.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_SINGLE_SYNONYM : SourceIngredient.MATCH_SINGLE_SYNONYM) : SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((CDMIngredient) matchingRxNormIngredientsSynonyms.toArray()[0]).toStringWithSynonymsSingleField());
				match = true;
				break;
			}
			if ((!match) && (matchingRxNormIngredientsSynonyms.size() > 1)) {
				Set<CDMIngredient> exactMatch = new HashSet<CDMIngredient>();
				for (CDMIngredient cdmIngredient : matchingRxNormIngredientsSynonyms) {
					for (String synonym : cdmIngredient.getSynonyms()) {
						if (synonym.replaceAll(" ", "").equals(nameVariant)) {
							exactMatch.add(cdmIngredient);
							break;
						}
					}
				}
				if (exactMatch.size() == 1) {
					ingredientMap.put(sourceIngredient, (CDMIngredient) exactMatch.toArray()[0]);
					sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_EXACT_SYNONYM : SourceIngredient.MATCH_EXACT_SYNONYM) : SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM);
					sourceIngredient.setMatchString(nameVariant);
					sourceIngredient.setMatchingDrug(((CDMIngredient) exactMatch.toArray()[0]).toStringWithSynonymsSingleField());
					match = true;
					break;
				}
			}
		}
		
		return match;
	}
	
	
	private boolean matchIngredientByNameMapsTo(SourceIngredient sourceIngredient, boolean useCASName, List<String> nameVariants) {
		boolean match = false;
		boolean noVariants = false;
		
		if (nameVariants == null) {
			noVariants = true;
			nameVariants = new ArrayList<String>();
			if (useCASName) {
				String casNumber = sourceIngredient.getCASNumber();
				if (casNumber != null) {
					String casName = casMap.get(casNumber);
					if (casName != null) {
						nameVariants.add(casName);
					}
				}
			}
			else {
				nameVariants.add(sourceIngredient.getIngredientNameEnglishNoSpaces());
				nameVariants.add(sourceIngredient.getIngredientNameNoSpaces());
			}
		}
		
		for (String nameVariant : nameVariants) {
			Set<CDMIngredient> matchingMapsToRxNormIngredients = new HashSet<CDMIngredient>();
			Set<String> matchingDrugNames = new HashSet<String>();
			
			for (String drugName : mapsToCDMIngredient.keySet()) {
				Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(drugName);
				if ((ingredientSet.size() == 1) && drugName.replaceAll(" ", "").contains(nameVariant)) {
					matchingMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
					matchingDrugNames.add(drugName);
				}
			}
			if (matchingMapsToRxNormIngredients.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingMapsToRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_SINGLE : SourceIngredient.MATCH_MAPSTO_SINGLE) : SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((String) matchingDrugNames.toArray()[0]));
				match = true;
				break;
			}
			
			if ((!match) && (matchingMapsToRxNormIngredients.size() > 1)) {
				Set<String> exactMatch = new HashSet<String>();
				for (String drugName : matchingDrugNames) {
					if (drugName.replaceAll(" ", "").equals(nameVariant)) {
						exactMatch.add(drugName);
					}
				}
				if (exactMatch.size() == 1) {
					ingredientMap.put(sourceIngredient, (CDMIngredient) mapsToCDMIngredient.get((String) exactMatch.toArray()[0]).toArray()[0]);
					sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_EXACT : SourceIngredient.MATCH_MAPSTO_EXACT) : SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT);
					sourceIngredient.setMatchString(nameVariant);
					sourceIngredient.setMatchingDrug(((String) exactMatch.toArray()[0]));
					match = true;
					break;
				}
			}
		}
		return match;
	}
	
	
	private boolean matchIngredientBySynonymNameMapsTo(SourceIngredient sourceIngredient, boolean useCASName, List<String> nameVariants) {
		boolean match = false;
		boolean noVariants = false;
		
		if (nameVariants == null) {
			noVariants = true;
			nameVariants = new ArrayList<String>();
			if (useCASName) {
				String casNumber = sourceIngredient.getCASNumber();
				if (casNumber != null) {
					String casName = casMap.get(casNumber);
					if (casName != null) {
						nameVariants.add(casName);
					}
				}
			}
			else {
				nameVariants.add(sourceIngredient.getIngredientNameEnglishNoSpaces());
				nameVariants.add(sourceIngredient.getIngredientNameNoSpaces());
			}
		}
		
		for (String nameVariant : nameVariants) {
			Set<CDMIngredient> matchingMapsToRxNormIngredients = new HashSet<CDMIngredient>();
			Set<String> matchingMapsToRxNormIngredientsDrugNames = new HashSet<String>();
			Set<String> matchingMapsToRxNormIngredientsSynonyms = new HashSet<String>();
			
			for (String drugName : drugNameSynonyms.keySet()) {
				for (String synonym : drugNameSynonyms.get(drugName)) {
					Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(drugName);
					if ((ingredientSet.size() == 1) && synonym.replaceAll(" ", "").contains(nameVariant)) {
						matchingMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
						matchingMapsToRxNormIngredientsDrugNames.add(drugName);
						matchingMapsToRxNormIngredientsSynonyms.add(synonym);
					}
				}
			}
			if (matchingMapsToRxNormIngredientsDrugNames.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingMapsToRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM : SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM) : SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((String) matchingMapsToRxNormIngredientsSynonyms.toArray()[0]) + "=" + ((String) matchingMapsToRxNormIngredientsDrugNames.toArray()[0]));
				match = true;
				break;
			}
			if ((!match) && (matchingMapsToRxNormIngredientsDrugNames.size() > 1)) {
				Set<String> exactMatch = new HashSet<String>();
				Set<String> exactMatchSynonym = new HashSet<String>();
				for (String drugName : matchingMapsToRxNormIngredientsDrugNames) {
					for (String synonym : drugNameSynonyms.get(drugName)) {
						if (synonym.replaceAll(" ", "").equals(nameVariant)) {
							exactMatch.add(drugName);
							exactMatchSynonym.add(synonym);
							break;
						}
					}
				}
				if (exactMatch.size() == 1) {
					ingredientMap.put(sourceIngredient, (CDMIngredient) mapsToCDMIngredient.get((String) exactMatch.toArray()[0]).toArray()[0]);
					sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM : SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM) : SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM);
					sourceIngredient.setMatchString(nameVariant);
					sourceIngredient.setMatchingDrug(((String) exactMatchSynonym.toArray()[0]) + "=" + ((String) exactMatch.toArray()[0]));
					match = true;
					break;
				}
			}
		}
		
		return match;
	}
	
	/*
	public static void main(String[] args) {
	}
	*/
}
