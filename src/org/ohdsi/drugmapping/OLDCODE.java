package org.ohdsi.drugmapping;

import java.util.HashMap;
import java.util.Map;

public class OLDCODE {
/*	
	private Map<String, String> ingredientTranslationCorrections = new HashMap<String, String>();
	
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
	
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile translationCorrectionsFile) {
		
		QueryParameters queryParameters;
		
		SourceDrug.init();
				
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

			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Source Drugs Mapping.csv";
			System.out.println("    " + fileName);
			PrintWriter drugMappingFile = new PrintWriter(new File(fileName));
			drugMappingFile.println(SourceDrug.getHeader() + "," + CDMDrug.getHeader());

			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Source Drugs Unmapped.csv";
			System.out.println("    " + fileName);
			PrintWriter drugUnmappedFile = new PrintWriter(new File(fileName));
			drugUnmappedFile.println(SourceDrug.getHeader() + "," + "UnmappedReason" + "," + CDMDrug.getHeader() + "," + "RejectReason");
			
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
					String casNumber = row.get("CasRN").trim();
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
												sourceDrugsFile.get(row, "SourceFormulation").trim().toUpperCase(), 
												sourceDrugsFile.get(row, "SourceCount").trim()
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
						casNumber = casNumber.replaceAll(" ", "").replaceAll("-", "");
						if (!casNumber.equals("")) {
							casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
						}

						// Remove comma's
						ingredientName = ingredientName.replaceAll(",", " ").replaceAll("  ", " ");
						ingredientNameEnglish = ingredientNameEnglish.replaceAll(",", " ").replaceAll("  ", " ");
						
						if (!ingredientName.equals("")) {
							SourceIngredient sourceIngredient = SourceDrug.getIngredient(ingredientName, ingredientNameEnglish, casNumber);
							if (SourceDrug.errorOccurred()) {
								sourceDrugError = true;
							}
							else if (sourceIngredient == null) {
								sourceIngredient = sourceDrug.AddIngredient(ingredientName, ingredientNameEnglish, casNumber, dosage, dosageUnit);
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
					}
				}
			}

			System.out.println("    Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
			System.out.println("    Found " + Integer.toString(forms.size()) + " source forms");
			System.out.println("    Found " + Integer.toString(units.size()) + " source units");
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			if (!sourceDrugError) {
				boolean ingredientMappingFound = false;

				String ingredientMappingsFileName = DrugMapping.getCurrentPath() + "/" + "DrugMapping - Ingredient Mapping.csv";
				File mappingsFile = new File(DrugMapping.getCurrentPath() + "/" + ingredientMappingsFileName);
				if (mappingsFile.exists() && mappingsFile.canRead()) {
					
					FileDefinition ingredientMappingsFileDefinition = 
							new FileDefinition(
									"Ingredient Mapping File",
									new String[] {
											"This file should contain the ingredient mapping.",
											"This file is optional. When not available it is created to fill."
							  		},
									new FileColumnDefinition[] {
											new FileColumnDefinition("IngredientName",        new String[] { "This is the name of an ingredient." }),
											new FileColumnDefinition("IngredientNameEnglish", new String[] { "This is the English name of the ingredient." }),
											new FileColumnDefinition("CASNumber",             new String[] { "This is the CAS number of the ingredient." }),
											new FileColumnDefinition("SourceCount",           new String[] { "This is the English name of the ingredient." }),
											new FileColumnDefinition("CDMConceptId",          new String[] { "This is the CDM concept_id." }),
											new FileColumnDefinition("CDMDomainId",           new String[] { "This is the CDM domain_id." }),
											new FileColumnDefinition("CDMConceptName",        new String[] { "This is the CDM concept_name." }),
											new FileColumnDefinition("CDMConceptClassId",     new String[] { "This is the CDM concept_class_id." }),
											new FileColumnDefinition("CDMVocabularyId",       new String[] { "This is the CDM vocabulary_id." }),
											new FileColumnDefinition("CDMConceptCode",        new String[] { "This is the CDM concept_code." }),
											new FileColumnDefinition("CDMValidStartDate",     new String[] { "This is the CDM valid_start_date." }),
											new FileColumnDefinition("CDMValidEndDate",       new String[] { "This is the CDM valid_end_date." }),
											new FileColumnDefinition("CDMInvalidReason",      new String[] { "This is the CDM invalid_reason." })
									}
							);
					
					InputFile ingredientMappingsFile = new InputFile(ingredientMappingsFileDefinition);
					ingredientMappingsFile.setFileName(ingredientMappingsFileName);
					
					if (ingredientMappingsFile.openFile()) {
						// Load ingredient mapping
						ingredientMappingFound = true;
						while (ingredientMappingsFile.hasNext()) {
							Row row = ingredientMappingsFile.next();

							String ingredientName        = sourceDrugsFile.get(row, "IngredientName").trim().toUpperCase(); 
							String ingredientNameEnglish = sourceDrugsFile.get(row, "IngredientNameEnglish").trim().toUpperCase(); 
							String ingredientCASNumber   = sourceDrugsFile.get(row, "CASNumber").trim();
							//String sourceCountString     = sourceDrugsFile.get(row, "SourceCount").trim(); 
							String cdmConceptId          = sourceDrugsFile.get(row, "CDMConceptId").trim(); 
							String cdmConceptName        = sourceDrugsFile.get(row, "CDMConceptName").trim(); 
							String cdmDomainId           = sourceDrugsFile.get(row, "CDMDomainId").trim(); 
							String cdmConceptClassId     = sourceDrugsFile.get(row, "CDMConceptClassId").trim(); 
							String cdmVocabularyId       = sourceDrugsFile.get(row, "CDMVocabularyId").trim(); 
							String cdmStandardConcept    = sourceDrugsFile.get(row, "CDMStandardConcept").trim(); 
							String cdmConceptCode        = sourceDrugsFile.get(row, "CDMConceptCode").trim(); 
							String cdmValidStartDate     = sourceDrugsFile.get(row, "CDMValidStartDate").trim(); 
							String cdmValidEndDate       = sourceDrugsFile.get(row, "CDMValidEndDate").trim(); 
							String cdmInvalidReason      = sourceDrugsFile.get(row, "CDMInvalidReason").trim();
							
							if (!ingredientName.equals("")) {
								SourceIngredient sourceIngredient = SourceDrug.getIngredient(ingredientName, ingredientNameEnglish, ingredientCASNumber);
								if (SourceDrug.errorOccurred()) {
									sourceDrugError = true;
								}
								else if (sourceIngredient == null) {
									// Source Ingredient not found. Should not happen.
								}
								else {
									CDMIngredient cdmIngredient = cdmIngredients.get(cdmConceptId);
									if (cdmIngredient == null) {
										cdmIngredient = new CDMIngredient(cdmConceptId, cdmConceptName, cdmDomainId, cdmVocabularyId, cdmConceptClassId, cdmStandardConcept, cdmConceptCode, cdmValidStartDate, cdmValidEndDate, cdmInvalidReason);
										cdmIngredients.put(cdmConceptId, cdmIngredient);
										cdmIngredientsList.add(cdmIngredient);
									}
									ingredientMap.put(sourceIngredient, cdmIngredient);
								}
							}
						}
					}
					else {
						System.out.println("    ERROR: Cannot read ingredient mapping file '" + ingredientMappingsFileName + "'!");
					}
				}
				else {
					// Create empty ingredient mapping file for use with Usagi
					try {
						PrintWriter ingredientMappingFileWriter = new PrintWriter(ingredientMappingsFileName);

						String header = "IngredientName";
						header += "," + "IngredientNameEnglish";
						header += "," + "CASNumber";
						header += "," + "SourceCount";
						header += "," + "CDMConceptId";
						header += "," + "CDMConceptName";
						header += "," + "CDMDomainId";
						header += "," + "CDMConceptClassId";
						header += "," + "CDMVocabularyId";
						header += "," + "CDMStandardConcept";
						header += "," + "CDMConceptCode";
						header += "," + "CDMValidStartDate";
						header += "," + "CDMValidEndDate";
						header += "," + "CDMInvalidReason";

						ingredientMappingFileWriter.println(header);

						for (SourceIngredient ingredient : SourceDrug.getAllIngredients()) {
							String record = ingredient.getIngredientName();
							record += "," + ingredient.getIngredientNameEnglish();
							record += "," + (ingredient.getCASNumber() == null ? "" : "\"" + ingredient.getCASNumber() + "\"");
							record += "," + ingredient.getCount();
							record += ",,,,,,,,,,";

							ingredientMappingFileWriter.println(record);
						}
						
						ingredientMappingFileWriter.close();
						
						System.out.println("");
						System.out.println("First map the ingredients, for example with Usagi, in the file:");
						System.out.println("");
						System.out.println(DrugMapping.getCurrentPath() + "/" + ingredientMappingsFileName);
					} catch (FileNotFoundException e) {
						System.out.println("    ERROR: Cannot create empty ingredient mapping file '" + ingredientMappingsFileName + "'!");
					}
				}
				
				// Create Units Map
				UnitConversion unitConversionsMap = new UnitConversion(database, units);
				if (unitConversionsMap.getStatus() == UnitConversion.STATE_EMPTY) {
					// If no unit conversion is specified then stop.
					System.out.println("");
					System.out.println("First fill the unit conversion map in the file:");
					System.out.println("");
					System.out.println(DrugMapping.getCurrentPath() + "/" + UnitConversion.FILENAME);
				}
				
				FormConversion formConversionsMap = new FormConversion(database, forms);
				if (formConversionsMap.getStatus() == FormConversion.STATE_EMPTY) {
					// If no form conversion is specified then stop.
					System.out.println("");
					System.out.println("First fill the form conversion map in the file:");
					System.out.println("");
					System.out.println(DrugMapping.getCurrentPath() + "/" + FormConversion.FILENAME);
				}


				if (
						ingredientMappingFound                                         &&
						(unitConversionsMap.getStatus() != UnitConversion.STATE_EMPTY) &&
						(unitConversionsMap.getStatus() != UnitConversion.STATE_ERROR) && 
						(formConversionsMap.getStatus() != FormConversion.STATE_EMPTY) && 
						(formConversionsMap.getStatus() != FormConversion.STATE_ERROR)
					) {
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
							CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
							if (cdmIngredient == null) {
								cdmIngredient = new CDMIngredient(queryRow, "");
								cdmIngredients.put(cdmIngredient.getConceptId(), cdmIngredient);
								cdmIngredientsList.add(cdmIngredient);
							}
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
									drugsContainingIngredient.add(cdmDrug);
								}
							}
						} 
					}
					
					System.out.println(DrugMapping.getCurrentTime() + " Finished");
					
					
					// Get RxNorm Clinical Drugs with Form and Ingredients
					System.out.println(DrugMapping.getCurrentTime() + " Get CDM RxNorm Clinical Drug Comps with ingredients ...");
					
					for (Row queryRow : connection.queryResource("cdm/GetRxNormClinicalDrugCompsIngredients.sql", queryParameters)) {
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
					
					System.out.println(DrugMapping.getCurrentTime() + " Finished");
					
					
					// Close database connection
					connection.close();

					
					System.out.println(DrugMapping.getCurrentTime() + " Match ingredients by name ...");
					
					// Match RxNorm Ingredients on IngredientNameEnglish 
					for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
						
						if (ingredientMap.get(sourceIngredient) == null) {

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
								else {
									ingredientMappingFile.println(sourceIngredient + ",NO MATCH,,,,,,,,,,");
								}
							}
							else {
								ingredientMappingFile.println(sourceIngredient + ",NO MATCH,,,,,,,,,,");
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
					
					int mappedSourceDrugs = 0; 
					int mappedSourcePrescriptions = 0;
					int totalSourcePrescriptions = 0;
					for (String sourceDrugId : sourceDrugMap.keySet()) {
						SourceDrug sourceDrug = sourceDrugMap.get(sourceDrugId);
						if (sourceDrug.getCount() >= 0) {
							totalSourcePrescriptions = totalSourcePrescriptions + sourceDrug.getCount();
						} 
						if (sourceDrug.getMappedDrug() != null) {
							mappedSourceDrugs++;
							if (sourceDrug.getCount() >= 0) {
								mappedSourcePrescriptions = mappedSourcePrescriptions + sourceDrug.getCount();
							}
						}
					}
					
					System.out.println("");
					System.out.println("    Source drugs: " + Integer.toString(sourceDrugs.size()));
					System.out.println("    Source drugs without ATC: " + Integer.toString(noATCCounter) + " (" + Long.toString(Math.round(((double) noATCCounter / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Source drugs without ingredients: " + Integer.toString(noIngredientsCount) + " (" + Long.toString(Math.round(((double) noIngredientsCount / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Source drugs with one ingredient: " + Integer.toString(singleIngredientCount) + " (" + Long.toString(Math.round(((double) singleIngredientCount / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Source drugs with ingredients: " + Integer.toString(withIngredientsCount) + " (" + Long.toString(Math.round(((double) withIngredientsCount / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Unique source ingredients: " + Integer.toString(SourceDrug.getAllIngredients().size()));
					
					System.out.println();
					System.out.println("    Matched source ingredients:");
					System.out.println("      " + SourceIngredient.NO_MATCH + ": " + Integer.toString(match.get(SourceIngredient.NO_MATCH)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.NO_MATCH) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					
					System.out.println("      " + SourceIngredient.MATCH_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

					System.out.println("      " + SourceIngredient.MATCH_MAPSTO_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_MAPSTO_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

					System.out.println("      " + SourceIngredient.MATCH_CAS_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_CAS_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_CAS_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_CAS_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

					System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

					System.out.println("      " + SourceIngredient.MATCH_VARIANT_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_VARIANT_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

					System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      " + SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM + ": " + Integer.toString(match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM)) + " (" + Long.toString(Math.round(((double) match.get(SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM) / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");

					System.out.println();
					System.out.println("      TOTAL NAME MATCHES INGREDIENTS: " + Integer.toString(totalNameMatch) + " (" + Long.toString(Math.round(((double) totalNameMatch / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      TOTAL CAS MATCHES INGREDIENTS: " + Integer.toString(totalCASMatch) + " (" + Long.toString(Math.round(((double) totalCASMatch / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      TOTAL VARIANT MATCHES INGREDIENTS: " + Integer.toString(totalVariantMatch) + " (" + Long.toString(Math.round(((double) totalVariantMatch / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					System.out.println("      TOTAL MATCHES INGREDIENTS: " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
					
					System.out.println("    Source drugs with all ingredients matched: " + Integer.toString(fullMatchCount) + " (" + Long.toString(Math.round(((double) fullMatchCount / (double) withIngredientsCount) * 100)) + "%)");
					System.out.println(DrugMapping.getCurrentTime() + " Finished");
					
					
/*	
					System.out.println(DrugMapping.getCurrentTime() + " Map Source Drugs to RxNorm Clinical Drugs ...");
					
					for (SourceDrug sourceDrug : sourceDrugs) {
						String unmappedReason = "";
						Map<CDMDrug, String> cdmDrugRejectReason = new HashMap<CDMDrug, String>();
						String sourceDrugForm = sourceDrug.getFormulation();
						
						List<SourceIngredient> sourceIngredients = sourceDrug.getIngredients();
						List<CDMIngredient> mappedCDMIngredients = new ArrayList<CDMIngredient>();
						for (SourceIngredient sourceIngredient : sourceIngredients) {
							mappedCDMIngredients.add(ingredientMap.get(sourceIngredient));
						}
						if (!mappedCDMIngredients.contains(null)) {
							
							// Get all clinical drugs containing all ingredients
							Set<CDMDrug> removeClinicalDrugs = new HashSet<CDMDrug>();
							boolean firstIngredient = true;
							Set<CDMDrug> cdmDrugsWithAllIngredients = new HashSet<CDMDrug>();
							for (CDMIngredient mappedCDMIngredient : mappedCDMIngredients) {
								List<CDMDrug> ingredientDrugs = cdmDrugsContainingIngredient.get(mappedCDMIngredient);
								if (firstIngredient) {
									if (ingredientDrugs != null) {
										cdmDrugsWithAllIngredients.addAll(ingredientDrugs);
									}
								}
								else {
									removeClinicalDrugs = new HashSet<CDMDrug>();
									for (CDMDrug ingredientDrug : cdmDrugsWithAllIngredients) {
										if ((ingredientDrugs == null) || (!ingredientDrugs.contains(ingredientDrug))) {
											removeClinicalDrugs.add(ingredientDrug);
										}
									}
									cdmDrugsWithAllIngredients.removeAll(removeClinicalDrugs);
								}
								if (cdmDrugsWithAllIngredients.size() == 0) {
									// No ingredients left => no mapping on ingredients possible
									break;
								}
								firstIngredient = false;
							}

							// Drugs found containing all ingredients.
							
							// Check the number of ingredients. If not the same as source drug remove it from the list.
							removeClinicalDrugs = new HashSet<CDMDrug>();
							for (CDMDrug cdmDrug : cdmDrugsWithAllIngredients) {
								if (cdmDrug.getIngredients().size() != mappedCDMIngredients.size()) {
									removeClinicalDrugs.add(cdmDrug);
									cdmDrugRejectReason.put(cdmDrug, "Different number of ingredients: Source (" + Integer.toString(mappedCDMIngredients.size()) + ") <-> CDM (" + Integer.toString(cdmDrug.getIngredients().size()) + ")");
								}
							}
							cdmDrugsWithAllIngredients.removeAll(removeClinicalDrugs);
							
							// Get all clinical drug comps containing all ingredients
							Set<CDMDrug> removeClinicalDrugComps = new HashSet<CDMDrug>();
							firstIngredient = true;
							Set<CDMDrug> cdmDrugCompsWithAllIngredients = new HashSet<CDMDrug>();
							for (CDMIngredient mappedCDMIngredient : mappedCDMIngredients) {
								List<CDMDrug> ingredientDrugComps = cdmDrugCompsContainingIngredient.get(mappedCDMIngredient);
								if (firstIngredient) {
									if (ingredientDrugComps != null) {
										cdmDrugCompsWithAllIngredients.addAll(ingredientDrugComps);
									}
								}
								else {
									removeClinicalDrugComps = new HashSet<CDMDrug>();
									for (CDMDrug ingredientDrugComp : cdmDrugCompsWithAllIngredients) {
										if ((ingredientDrugComps == null) || (!ingredientDrugComps.contains(ingredientDrugComp))) {
											removeClinicalDrugComps.add(ingredientDrugComp);
										}
									}
									cdmDrugsWithAllIngredients.removeAll(removeClinicalDrugComps);
								}
								if (cdmDrugsWithAllIngredients.size() == 0) {
									// No ingredients left => no mapping on ingredients possible
									break;
								}
								firstIngredient = false;
							}

							// Drug comps found containing all ingredients.
							
							// Check the number of ingredients. If not the same as source drug remove it from the list.
							removeClinicalDrugComps = new HashSet<CDMDrug>();
							for (CDMDrug cdmDrugComp : cdmDrugCompsWithAllIngredients) {
								if (cdmDrugComp.getIngredients().size() != mappedCDMIngredients.size()) {
									removeClinicalDrugComps.add(cdmDrugComp);
									cdmDrugRejectReason.put(cdmDrugComp, "Different number of ingredients: Source (" + Integer.toString(mappedCDMIngredients.size()) + ") <-> CDM (" + Integer.toString(cdmDrugComp.getIngredients().size()) + ")");
								}
							}
							cdmDrugsWithAllIngredients.removeAll(removeClinicalDrugComps);
							
							if (sourceDrugForm != null) {
								// Check form
								removeClinicalDrugs = new HashSet<CDMDrug>();
								for (CDMDrug cdmDrug : cdmDrugsWithAllIngredients) {
									Set<String> cdmDrugForms = cdmDrug.getForms();
									if (cdmDrugForms.size() == 0) {
										removeClinicalDrugs.add(cdmDrug);
										cdmDrugRejectReason.put(cdmDrug, "No forms found");
									}
									else {
										Set<String> removeForms = new HashSet<String>();
										for (String cdmDrugForm : cdmDrugForms) {
											if (!formConversionsMap.matches(sourceDrugForm, cdmDrugForm)) {
												removeForms.add(cdmDrugForm);
											}
										}
										cdmDrugForms.removeAll(removeForms);
										if (cdmDrugForms.size() == 0) {
											removeClinicalDrugs.add(cdmDrug);
											cdmDrugRejectReason.put(cdmDrug, "No compatible forms found");
										}
									}
								}
								cdmDrugsWithAllIngredients.removeAll(removeClinicalDrugs);
								
								// Check ingredient strengths.
								if (cdmDrugsWithAllIngredients.size() > 0) {
									removeClinicalDrugs = new HashSet<CDMDrug>();
									for (int ingredientNr = 0; ingredientNr < sourceIngredients.size(); ingredientNr++) { 
										SourceIngredient sourceIngredient = sourceIngredients.get(ingredientNr);
										CDMIngredient cdmIngredient = mappedCDMIngredients.get(ingredientNr);
										
										SourceDrugComponent sourceDrugComponent = sourceDrug.getIngredientComponent(sourceIngredient);
										
										for (CDMDrug cdmDrug : cdmDrugsWithAllIngredients) {
											CDMIngredientStrength cdmIngredientStrength = cdmDrug.getIngredientStrength(cdmIngredient);
											
											if (!sourceDrugComponent.matches(unitConversionsMap, cdmIngredientStrength.getNumeratorDosage(), cdmIngredientStrength.getNumeratorDosageUnit(), cdmIngredientStrength.getDenominatorDosage(), cdmIngredientStrength.getDenominatorDosageUnit())) {
												removeClinicalDrugs.add(cdmDrug);
												cdmDrugRejectReason.put(cdmDrug, "No compatible ingredient strengths found");
											}
										}
									}
									cdmDrugsWithAllIngredients.removeAll(removeClinicalDrugs);
								}
								else {
									// Try mapping to clinical drug form
								}
							}
							else {
								
								// Check ingredient strengths.
								if (cdmDrugCompsWithAllIngredients.size() > 0) {
									removeClinicalDrugComps = new HashSet<CDMDrug>();
									for (int ingredientNr = 0; ingredientNr < sourceIngredients.size(); ingredientNr++) { 
										SourceIngredient sourceIngredient = sourceIngredients.get(ingredientNr);
										CDMIngredient cdmIngredient = mappedCDMIngredients.get(ingredientNr);
										
										SourceDrugComponent sourceDrugComponent = sourceDrug.getIngredientComponent(sourceIngredient);
										
										for (CDMDrug cdmDrugComp : cdmDrugCompsWithAllIngredients) {
// 2e of 3e passage
											CDMIngredientStrength cdmIngredientStrength = cdmDrugComp.getIngredientStrength(cdmIngredient);
											
											if (!sourceDrugComponent.matches(unitConversionsMap, cdmIngredientStrength.getNumeratorDosage(), cdmIngredientStrength.getNumeratorDosageUnit(), cdmIngredientStrength.getDenominatorDosage(), cdmIngredientStrength.getDenominatorDosageUnit())) {
												removeClinicalDrugComps.add(cdmDrugComp);
												cdmDrugRejectReason.put(cdmDrugComp, "No compatible ingredient strengths found");
											}
										}
									}
									cdmDrugCompsWithAllIngredients.removeAll(removeClinicalDrugComps);
								}
							}
							
							// Check what is left
							if (cdmDrugsWithAllIngredients.size() == 1) {
								drugMapping.put(sourceDrug, (CDMDrug) cdmDrugsWithAllIngredients.toArray()[0]);
							}
							else if (cdmDrugsWithAllIngredients.size() > 1) {
								// Multiple options
								unmappedReason = "Multiple Clinical Drug options left";
								for (CDMDrug cdmDrug : cdmDrugsWithAllIngredients) {
									cdmDrugRejectReason.put(cdmDrug, "Matches Clinical Drug");
								}
							}
							else if (cdmDrugCompsWithAllIngredients.size() == 1) {
								drugCompMapping.put(sourceDrug, (CDMDrug) cdmDrugCompsWithAllIngredients.toArray()[0]);
							}
							else if (cdmDrugCompsWithAllIngredients.size() > 1) {
								// Multiple options
								unmappedReason = "Multiple Clinical Drug Comp options left";
								for (CDMDrug cdmDrugComp : cdmDrugCompsWithAllIngredients) {
									cdmDrugRejectReason.put(cdmDrugComp, "Matches Clinical Drug Comp");
								}
							}
							else {
								// No options
								unmappedReason = "No options left";
							}
						}
						else {
							unmappedReason = "Not all ingredients are mapped";
						}
						
						if ((drugMapping.get(sourceDrug) == null) && (drugCompMapping.get(sourceDrug) == null)) {
							if (cdmDrugRejectReason.size() == 0) {
								drugUnmappedFile.println(sourceDrug + "," + unmappedReason + "," + CDMDrug.emptyRecord() + ",");
							}
							else {
								for (CDMDrug cdmDrug : cdmDrugRejectReason.keySet()) {
									drugUnmappedFile.println(sourceDrug + "," + unmappedReason + "," + cdmDrug + "," + cdmDrugRejectReason.get(cdmDrug));
								}
							}
						}
					}
					
					// Count prescriptions covered by mapping and write mapping file
					long total = 0;
					long covered = 0;
					for (SourceDrug sourceDrug : sourceDrugs) {
						total += sourceDrug.getCount();
						CDMDrug cdmDrug = drugMapping.get(sourceDrug); 
						if (cdmDrug != null) {
							covered += sourceDrug.getCount();
							drugMappingFile.println(sourceDrug + "," + cdmDrug);
						}
						else {
							CDMDrug cdmDrugComp = drugCompMapping.get(sourceDrug); 
							if (cdmDrugComp != null) {
								covered += sourceDrug.getCount();
								drugMappingFile.println(sourceDrug + "," + cdmDrugComp);
							}
						}
					}
					
					System.out.println("    Source drugs mapped to Clinical Drugs: " + drugMapping.size() + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) drugMapping.size() / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Source drugs mapped to Clinical Drug Comps: " + drugCompMapping.size() + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) drugCompMapping.size() / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Source drugs mapped: " + Integer.toString(drugMapping.size() + drugCompMapping.size()) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) (drugMapping.size() + drugCompMapping.size()) / (double) sourceDrugs.size()) * 100)) + "%)");
					System.out.println("    Source data coverage: " + covered + " of " + total + " (" + Long.toString(Math.round(((double) covered / (double) total) * 100)) + "%)");
					System.out.println(DrugMapping.getCurrentTime() + " Finished");
*/
/*
				}
			}	
			
			
			// Close all output files
			missingATCFile.close();
			rxNormIngredientsFile.close();
			mapsToRxNormIngredientsFile.close();
			ingredientMappingFile.close();
			drugMappingFile.close();
			drugUnmappedFile.close();
			
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
			Set<CDMIngredient> matchingEqualRxNormIngredients = new HashSet<CDMIngredient>();
			Set<CDMIngredient> matchingContainingRxNormIngredients = new HashSet<CDMIngredient>();
			
			for (CDMIngredient cdmIngredient : cdmIngredientsList) {
				if (cdmIngredient.getConceptNameNoSpaces().equals(nameVariant)) {
					matchingEqualRxNormIngredients.add(cdmIngredient);
				}
				if (cdmIngredient.getConceptNameNoSpaces().contains(nameVariant)) {
					matchingContainingRxNormIngredients.add(cdmIngredient);
				}
			}
			if (matchingEqualRxNormIngredients.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingEqualRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_EXACT : SourceIngredient.MATCH_EXACT) : SourceIngredient.MATCH_VARIANT_EXACT);
				match = true;
				break;
			}
			else if (matchingContainingRxNormIngredients.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingContainingRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_SINGLE : SourceIngredient.MATCH_SINGLE) : SourceIngredient.MATCH_VARIANT_SINGLE);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((CDMIngredient) matchingContainingRxNormIngredients.toArray()[0]).toString().replaceAll("\"", "'")); 
				match = true;
				break;
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
			Set<CDMIngredient> matchingEqualRxNormIngredientsSynonyms = new HashSet<CDMIngredient>();
			Set<CDMIngredient> matchingContainingRxNormIngredientsSynonyms = new HashSet<CDMIngredient>();
			
			// Match on synonyms
			for (CDMIngredient cdmIngredient : cdmIngredientsList) {
				for (String synonym : cdmIngredient.getSynonyms()) {
					String synonymName = synonym.replaceAll("\n", " ").replaceAll("\r", " ");
					while (synonymName.contains("  ")) synonymName = synonymName.replaceAll("  ", " ");
					synonymName = synonymName.replaceAll(" ", "").replaceAll("-", "");
					if (synonymName.equals(nameVariant)) {
						matchingEqualRxNormIngredientsSynonyms.add(cdmIngredient);
					}
					if (synonymName.contains(nameVariant)) {
						matchingContainingRxNormIngredientsSynonyms.add(cdmIngredient);
					}
				}
			}
			if (matchingEqualRxNormIngredientsSynonyms.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingEqualRxNormIngredientsSynonyms.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_EXACT_SYNONYM : SourceIngredient.MATCH_EXACT_SYNONYM) : SourceIngredient.MATCH_VARIANT_EXACT_SYNONYM);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((CDMIngredient) matchingEqualRxNormIngredientsSynonyms.toArray()[0]).toStringWithSynonymsSingleField());
				match = true;
				break;
			}
			else if (matchingContainingRxNormIngredientsSynonyms.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingContainingRxNormIngredientsSynonyms.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_SINGLE_SYNONYM : SourceIngredient.MATCH_SINGLE_SYNONYM) : SourceIngredient.MATCH_VARIANT_SINGLE_SYNONYM);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((CDMIngredient) matchingContainingRxNormIngredientsSynonyms.toArray()[0]).toStringWithSynonymsSingleField());
				match = true;
				break;
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
			Set<CDMIngredient> matchingEqualMapsToRxNormIngredients = new HashSet<CDMIngredient>();
			Set<String> matchingEqualDrugNames = new HashSet<String>();
			Set<CDMIngredient> matchingContainingMapsToRxNormIngredients = new HashSet<CDMIngredient>();
			Set<String> matchingContainingDrugNames = new HashSet<String>();
			
			for (String drugName : mapsToCDMIngredient.keySet()) {
				Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(drugName);
				// For testing
				for (CDMIngredient cdmIngredient : ingredientSet) {
					if (cdmIngredient.getConceptId().equals("955372")) {
						System.out.println("matchIngredientByNameMapsTo");
					}
				}
				
				String drugNameClean = drugName.replaceAll("\n", " ").replaceAll("\r", " ");
				while (drugNameClean.contains("  ")) drugNameClean = drugNameClean.replaceAll("  ", " ");
				drugNameClean = drugNameClean.replaceAll(" ", "").replaceAll("-", "");
				if ((ingredientSet.size() == 1) && drugNameClean.equals(nameVariant)) {
					matchingEqualMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
					matchingEqualDrugNames.add(drugName);
				}
				if ((ingredientSet.size() == 1) && drugNameClean.contains(nameVariant)) {
					matchingContainingMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
					matchingContainingDrugNames.add(drugName);
				}
			}
			if (matchingEqualMapsToRxNormIngredients.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingEqualMapsToRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_EXACT : SourceIngredient.MATCH_MAPSTO_EXACT) : SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((String) matchingEqualDrugNames.toArray()[0]));
				match = true;
				break;
			}
			else if (matchingContainingMapsToRxNormIngredients.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingContainingMapsToRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_SINGLE : SourceIngredient.MATCH_MAPSTO_SINGLE) : SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((String) matchingContainingDrugNames.toArray()[0]));
				match = true;
				break;
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
			Set<CDMIngredient> matchingEqualMapsToRxNormIngredients = new HashSet<CDMIngredient>();
			Set<String> matchingEqualMapsToRxNormIngredientsDrugNames = new HashSet<String>();
			Set<String> matchingEqualMapsToRxNormIngredientsSynonyms = new HashSet<String>();
			Set<CDMIngredient> matchingContainingMapsToRxNormIngredients = new HashSet<CDMIngredient>();
			Set<String> matchingContainingMapsToRxNormIngredientsDrugNames = new HashSet<String>();
			Set<String> matchingContainingMapsToRxNormIngredientsSynonyms = new HashSet<String>();
			
			for (String drugName : drugNameSynonyms.keySet()) {
				for (String synonym : drugNameSynonyms.get(drugName)) {
					String synonymName = synonym.replaceAll("\n", " ").replaceAll("\r", " ");
					while (synonymName.contains("  ")) synonymName = synonymName.replaceAll("  ", " ");
					synonymName = synonymName.replaceAll(" ", "").replaceAll("-", "");
					Set<CDMIngredient> ingredientSet = mapsToCDMIngredient.get(drugName);
					// For testing
					for (CDMIngredient cdmIngredient : ingredientSet) {
						if (cdmIngredient.getConceptId().equals("955372")) {
							System.out.println("matchIngredientByNameMapsTo");
						}
					}
					
					if ((ingredientSet.size() == 1) && synonymName.equals(nameVariant)) {
						matchingEqualMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
						matchingEqualMapsToRxNormIngredientsDrugNames.add(drugName);
						matchingEqualMapsToRxNormIngredientsSynonyms.add(synonym);
					}
					if ((ingredientSet.size() == 1) && synonymName.contains(nameVariant)) {
						matchingContainingMapsToRxNormIngredients.add((CDMIngredient) ingredientSet.toArray()[0]);
						matchingContainingMapsToRxNormIngredientsDrugNames.add(drugName);
						matchingContainingMapsToRxNormIngredientsSynonyms.add(synonym);
					}
				}
			}
			if (matchingEqualMapsToRxNormIngredientsDrugNames.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingEqualMapsToRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_EXACT_SYNONYM : SourceIngredient.MATCH_MAPSTO_EXACT_SYNONYM) : SourceIngredient.MATCH_VARIANT_MAPSTO_EXACT_SYNONYM);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((String) matchingEqualMapsToRxNormIngredientsSynonyms.toArray()[0]) + "=" + ((String) matchingEqualMapsToRxNormIngredientsDrugNames.toArray()[0]));
				match = true;
				break;
			}
			else if (matchingContainingMapsToRxNormIngredientsDrugNames.size() == 1) {
				ingredientMap.put(sourceIngredient, (CDMIngredient) matchingContainingMapsToRxNormIngredients.toArray()[0]);
				sourceIngredient.setMatch(noVariants ? (useCASName ? SourceIngredient.MATCH_CAS_MAPSTO_SINGLE_SYNONYM : SourceIngredient.MATCH_MAPSTO_SINGLE_SYNONYM) : SourceIngredient.MATCH_VARIANT_MAPSTO_SINGLE_SYNONYM);
				sourceIngredient.setMatchString(nameVariant);
				sourceIngredient.setMatchingDrug(((String) matchingContainingMapsToRxNormIngredientsSynonyms.toArray()[0]) + "=" + ((String) matchingContainingMapsToRxNormIngredientsDrugNames.toArray()[0]));
				match = true;
				break;
			}
		}
		
		return match;
	}
*/
}
