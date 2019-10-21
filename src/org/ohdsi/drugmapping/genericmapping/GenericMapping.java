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
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.Row;

public class GenericMapping extends Mapping {
	private static int MINIMUM_USE_COUNT = 1;
	
	private Set<String> forms = new HashSet<String>();
	private Set<String> units = new HashSet<String>();

	private UnitConversion unitConversionsMap = null;
	private FormConversion formConversionsMap = null;
	private Map<String, Set<String>> casMap = new HashMap<String, Set<String>>();
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
	private Map<CDMIngredient, Set<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, Set<CDMDrug>>();
	private Map<String, CDMDrug> cdmDrugComps = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, Set<CDMDrug>> cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, Set<CDMDrug>>();
	private Map<String, CDMDrug> cdmDrugForms = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, Set<CDMDrug>> cdmDrugFormsContainingIngredient = new HashMap<CDMIngredient, Set<CDMDrug>>();
	
	private Map<SourceDrug, CDMDrug> drugMappingClinicalDrug = new HashMap<SourceDrug, CDMDrug>();
	private Map<SourceDrug, List<CDMIngredientStrength>> drugMappingClinicalDrugIngredients = new HashMap<SourceDrug, List<CDMIngredientStrength>>();
	private Map<SourceDrug, List<CDMDrug>> rejectedClinicalDrugs = new HashMap<SourceDrug, List<CDMDrug>>();
	private Map<SourceDrug, List<String>> rejectedClinicalDrugsReasons = new HashMap<SourceDrug, List<String>>();
	private Map<SourceDrug, CDMDrug> drugMappingClinicalDrugComp = new HashMap<SourceDrug, CDMDrug>();
	private Map<SourceDrug, List<CDMIngredientStrength>> drugMappingClinicalDrugCompIngredients = new HashMap<SourceDrug, List<CDMIngredientStrength>>();
	private Map<SourceDrug, List<CDMDrug>> rejectedClinicalDrugComps = new HashMap<SourceDrug, List<CDMDrug>>();
	private Map<SourceDrug, List<String>> rejectedClinicalDrugCompsReasons = new HashMap<SourceDrug, List<String>>();
	private Map<SourceDrug, CDMDrug> drugMappingClinicalDrugForm = new HashMap<SourceDrug, CDMDrug>();
	private Map<SourceDrug, List<CDMIngredientStrength>> drugMappingClinicalDrugFormIngredients = new HashMap<SourceDrug, List<CDMIngredientStrength>>();
	private Map<SourceDrug, List<CDMDrug>> rejectedClinicalDrugForms = new HashMap<SourceDrug, List<CDMDrug>>();
	private Map<SourceDrug, List<String>> rejectedClinicalDrugFormsReasons = new HashMap<SourceDrug, List<String>>();
	
	private List<String> cdmIngredientNameIndexNameList = new ArrayList<String>();
	private Map<String, Map<String, Set<CDMIngredient>>> cdmIngredientNameIndexMap = new HashMap<String, Map<String, Set<CDMIngredient>>>();
	
	private List<String> report = new ArrayList<String>();
		
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile casFile) {
		boolean ok = true;
		
		cdmIngredientNameIndexNameList.add("Ingredient");
		cdmIngredientNameIndexMap.put("Ingredient", cdmIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("MapsToIngredient");
		cdmIngredientNameIndexMap.put("MapsToIngredient", cdmMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("ReplacedByIngredient");
		cdmIngredientNameIndexMap.put("ReplacedByIngredient", cdmReplacedByIngredientNameIndex);

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");
		SourceDrug.init();
		
		// Load source drug ingredient mapping
		ok = ok && getSourceDrugs(sourceDrugsFile, MINIMUM_USE_COUNT) && (!SourceDrug.errorOccurred());
		
		// Get unit conversion from local units to CDM units
		ok = getUnitConversion(database);
		
		// Get form conversion from local forms to CDM forms
		ok = getFormConversion(database);
		
		// Get CDM Ingredients
		ok = getCDMData(database);		
		
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
		saveMapping();
		
		// Save rejected mappings
		saveRejectedMappings();

		System.out.println(DrugMapping.getCurrentTime() + " Finished");
		
		// Create the final report
		finalReport();
	}
	
	
	private boolean getSourceDrugs(InputFile sourceDrugsFile, int minimumUseCount) {
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
				fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
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
			report.add("Found " + Integer.toString(sourceDrugs.size()) + " source drugs with a miminum use count of " + Integer.toString(minimumUseCount));
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
		if (unitConversionsMap.getStatus() == UnitConversion.STATE_EMPTY) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the unit conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getCurrentPath() + "/" + UnitConversion.FILENAME);
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getFormConversion(CDMDatabase database) {
		boolean ok = true;
		
		formConversionsMap = new FormConversion(database, forms);
		if (formConversionsMap.getStatus() == FormConversion.STATE_EMPTY) {
			// If no form conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the form conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getCurrentPath() + "/" + FormConversion.FILENAME);
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
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping RxNorm Ingredients.csv";
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
					String cdmIngredientNameNoSpaces = cdmIngredient.getConceptNameNoSpaces();
					String cdmIngredientNameModified = modifyName(cdmIngredient.getConceptName());
					
					List<String> nameList = new ArrayList<String>();
					nameList.add(cdmIngredientNameModified);
					nameList.add(cdmIngredientNameNoSpaces);
					
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
			String cdmIngredientSynonymNoSpaces = cdmIngredientSynonym.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			String cdmIngredientSynonymModified = modifyName(cdmIngredientSynonym); 

			List<String> nameList = new ArrayList<String>();
			nameList.add(cdmIngredientSynonymNoSpaces);
			nameList.add(cdmIngredientSynonymModified);
			
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
			/*
			String ingredientName = lastCdmIngredient.getConceptName();
			while (ingredientName.contains("F")) {
				ingredientName = ingredientName.replaceFirst("F", "PH");
				lastCdmIngredient.addSynonym(ingredientName);
				String ingredientNameNoSpaces = cdmIngredientSynonym.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
				existingCDMIngredients = cdmIngredientNameIndex.get(ingredientNameNoSpaces);
				if (existingCDMIngredients == null) {
					existingCDMIngredients = new HashSet<CDMIngredient>();
					cdmIngredientNameIndex.put(ingredientNameNoSpaces, existingCDMIngredients);
				}
				existingCDMIngredients.add(lastCdmIngredient);
			}
			*/
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
			String drugNameNoSpaces = drugName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			String drugNameModified = modifyName(drugName);
			String cdmIngredientConceptId = queryRow.get("mapsto_concept_id").trim();
			String drugNameSynonym = queryRow.get("drug_synonym_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String drugNameSynonymNoSpaces = drugNameSynonym.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			String drugNameSynonymModified = modifyName(drugNameSynonym);
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			
			if (cdmIngredient != null) {
				List<String> nameList = new ArrayList<String>();
				nameList.add(drugNameNoSpaces);
				nameList.add(drugNameModified);
				nameList.add(drugNameSynonymNoSpaces);
				nameList.add(drugNameSynonymModified);
				
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
			
			String cdmReplacedNameNoSpaces = cdmReplacedByName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
			String cdmReplacedNameModified = modifyName(cdmReplacedByName);
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmReplacedByConceptId);
			if (cdmIngredient != null) {				
				Set<CDMIngredient> cdmIngredients = cdmReplacedByIngredientNameIndex.get(cdmReplacedNameNoSpaces);
				if (cdmIngredients == null) {
					cdmIngredients = new HashSet<CDMIngredient>();
					cdmReplacedByIngredientNameIndex.put(cdmReplacedNameNoSpaces, cdmIngredients);
				}
				cdmIngredients.add(cdmIngredient);
				
				cdmIngredients = cdmReplacedByIngredientNameIndex.get(cdmReplacedNameModified);
				if (cdmIngredients == null) {
					cdmIngredients = new HashSet<CDMIngredient>();
					cdmReplacedByIngredientNameIndex.put(cdmReplacedNameModified, cdmIngredients);
				}
				cdmIngredients.add(cdmIngredient);
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
						
						Set<CDMDrug> drugsContainingIngredient = cdmDrugsContainingIngredient.get(cdmIngredient);
						if (drugsContainingIngredient == null) {
							drugsContainingIngredient = new HashSet<CDMDrug>();
							cdmDrugsContainingIngredient.put(cdmIngredient, drugsContainingIngredient);
						}
						drugsContainingIngredient.add(cdmDrug);
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
						
						Set<CDMDrug> drugsContainingIngredient = cdmDrugCompsContainingIngredient.get(cdmIngredient);
						if (drugsContainingIngredient == null) {
							drugsContainingIngredient = new HashSet<CDMDrug>();
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
						
						Set<CDMDrug> drugsContainingIngredient = cdmDrugFormsContainingIngredient.get(cdmIngredient);
						if (drugsContainingIngredient == null) {
							drugsContainingIngredient = new HashSet<CDMDrug>();
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
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping CDM RxNorm Ingredients Name Index.csv";
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
					
					if (!casNumber.equals("")) {
						casNumber = uniformCASNumber(casNumber);
						Set<String> casNames = new HashSet<String>();
						
						String[] synonymSplit = synonyms.split("[|]");
						for (String synonym : synonymSplit) {
							String synonymNoSpaces = synonym.trim().replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
							if (!synonymNoSpaces.equals("")) {
								casNames.add(synonymNoSpaces);
								casNames.add(modifyName(synonym));
							}
						}
						
						String chemicalNameNoSpaces = chemicalName.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", ""); 
						if (!chemicalNameNoSpaces.equals("")) {
							casNames.add(chemicalNameNoSpaces);
							casNames.add(modifyName(chemicalName));
						}
						
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
		String fileName = "";
		Integer multipleMappings = 0;
		
		multipleMappings += matchIngredientsByCASNumber();
		
		multipleMappings += matchIngredientsByATC();
		
		multipleMappings += matchIngredientsByName();

		report.add("Source ingredients mapped total : " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		
		System.out.println(DrugMapping.getCurrentTime() + "     Creating output files ...");

		try {
			// Create match ingredients file output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Match Ingredients.csv";
			PrintWriter matchIngredientsFile = new PrintWriter(new File(fileName));
			matchIngredientsFile.println(SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
			
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				if (sourceIngredient.getMatchingIngredient() != null) {
					matchIngredientsFile.println(sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatchingIngredient()));
				}
				else {
					matchIngredientsFile.println(sourceIngredient.toMatchString() + "," + CDMIngredient.emptyRecord());
				}
			}
			
			matchIngredientsFile.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("       WARNING: Cannot create output file '" + fileName + "'");
		}

		try {
			// Create match ingredients file output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Match SourceDrug Ingredients.csv";
			PrintWriter matchSourceDrugIngredientsFile = new PrintWriter(new File(fileName));
			matchSourceDrugIngredientsFile.println(SourceDrug.getHeader() + "," + SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
			
			for (SourceDrug sourceDrug : sourceDrugs) {
				for (SourceIngredient sourceIngredient : sourceDrug.getIngredients()) {
					matchSourceDrugIngredientsFile.println(sourceDrug + "," + sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatchingIngredient()));
				}
			}
			
			matchSourceDrugIngredientsFile.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("       WARNING: Cannot create output file '" + fileName + "'");
		}
		
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
				
				List<String> matchNameList = new ArrayList<String>();
				matchNameList.add(modifyName(sourceIngredient.getIngredientName()));
				matchNameList.add(modifyName(sourceIngredient.getIngredientNameEnglish()));
				matchNameList.add(sourceIngredient.getIngredientNameEnglishNoSpaces());
				
				boolean multipleMapping = false;
				
				for (String ingredientNameIndexName : cdmIngredientNameIndexNameList) {
					Map<String, Set<CDMIngredient>> ingredientNameIndex = cdmIngredientNameIndexMap.get(ingredientNameIndexName);
					
					boolean matchFound = false;
					
					for (String matchName : matchNameList) {
						
						Set<CDMIngredient> matchedCDMIngredients = ingredientNameIndex.get(matchName);
						if (matchedCDMIngredients != null) {
							if (matchedCDMIngredients.size() == 1) {
								CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
								ingredientMap.put(sourceIngredient, cdmIngredient);
								sourceIngredient.setMatchingIngredient(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
								sourceIngredient.setMatchString(ingredientNameIndexName + ": " + matchName);
								multipleMapping = false;
								matchFound = true;
								break;
							}
							else {
								multipleMapping = true;
							}
						}
					}
					
					if (matchFound || multipleMapping) {
						break;
					}
					
					if (multipleMapping) {
						multipleMappings++;
					}
					
					if (matchFound) {
						matchedByName++;
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
				boolean multipleMapping = false;
				String casNumber = sourceIngredient.getCASNumber();
				if (casNumber != null) {
					Set<String> casNamesNoSpaces = casMap.get(casNumber);
					if (casNamesNoSpaces != null) {
						Set<CDMIngredient>matchedCDMIngredients = new HashSet<CDMIngredient>();
						String matchingCASnameNoSpaces = null;
						for (String casNameNoSpaces : casNamesNoSpaces) {
							Set<CDMIngredient> casNameIngredients = cdmIngredientNameIndex.get(casNameNoSpaces);
							if (casNameIngredients != null) {
								matchedCDMIngredients.addAll(casNameIngredients);
								matchingCASnameNoSpaces = casNameNoSpaces;
							}
						}
						if (matchedCDMIngredients.size() == 1) {
							CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
							ingredientMap.put(sourceIngredient, cdmIngredient);
							sourceIngredient.setMatchingIngredient(cdmIngredient.getConceptId());
							sourceIngredient.setMatchString("CAS: " + matchingCASnameNoSpaces);
							matchedByCASName++;
						}
						else {
							multipleMapping = true;
						}
					}
				}
				
				if (multipleMapping) {
					multipleMappings++;
				}
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
				for (SourceIngredient sourceDrugIngredient : sourceDrugIngredients) {
					cdmDrugIngredients.add(ingredientMap.get(sourceDrugIngredient));
				}
				if (!cdmDrugIngredients.contains(null)) {
					sourceDrugsAllIngredientsMapped.add(sourceDrug);
					sourceDrugsCDMIngredients.put(sourceDrug, cdmDrugIngredients);
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		report.add("Source drugs with all ingredients mapped: " + Integer.toString(sourceDrugsAllIngredientsMapped.size()) + " (" + Long.toString(Math.round(((double) sourceDrugsAllIngredientsMapped.size() / (double) sourceDrugs.size()) * 100)) + "%)");
		
		return (sourceDrugsAllIngredientsMapped.size() > 0);
	}
	
	
	private boolean matchClinicalDrugs() {
		boolean ok = true;
		Integer multipleDrugMappings = 0;
		String fileName = "";
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drugs ...");
		
		PrintWriter multipleClinicalDrugsMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Multiple Clinical Drugs Mappings.csv";
			multipleClinicalDrugsMappingFile = new PrintWriter(new File(fileName));
			multipleClinicalDrugsMappingFile.println(SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			multipleClinicalDrugsMappingFile = null;
		}
		
		PrintWriter noClinicalDrugsMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping No Clinical Drugs Mappings.csv";
			noClinicalDrugsMappingFile = new PrintWriter(new File(fileName));
			noClinicalDrugsMappingFile.println(SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			noClinicalDrugsMappingFile = null;
		}
		
		for (SourceDrug sourceDrug : sourceDrugsAllIngredientsMapped) {
			List<CDMIngredient> cdmDrugIngredients = sourceDrugsCDMIngredients.get(sourceDrug);
			
			if (cdmDrugIngredients.size() > 0) {
				// Find CDM Clinical Drugs with corresponding ingredients
				Set<CDMDrug> cdmDrugsWithIngredients = null;
				for (CDMIngredient cdmIngredient : cdmDrugIngredients) {
					Set<CDMDrug> cdmDrugsWithIngredient = cdmDrugsContainingIngredient.get(cdmIngredient);
					if (cdmDrugsWithIngredient != null) {
						if (cdmDrugsWithIngredients == null) {
							cdmDrugsWithIngredients = new HashSet<CDMDrug>();
							cdmDrugsWithIngredients.addAll(cdmDrugsWithIngredient);
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
						cdmDrugsWithIngredients = new HashSet<CDMDrug>();
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
								List<CDMDrug> rejectedList = rejectedClinicalDrugs.get(sourceDrug);
								List<String> rejectedReasonsList = rejectedClinicalDrugsReasons.get(sourceDrug);
								if (rejectedList == null) {
									rejectedList = new ArrayList<CDMDrug>();
									rejectedClinicalDrugs.put(sourceDrug, rejectedList);
									rejectedReasonsList = new ArrayList<String>();
									rejectedClinicalDrugsReasons.put(sourceDrug, rejectedReasonsList);
								}
								rejectedList.add(cdmDrug);
								rejectedReasonsList.add("Incompatible Form");
							}
						}
						cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
						if (cdmDrugsWithIngredients.size() == 0) {
							break;
						}
					}
				}
				
				// Find CDM Clinical Drugs with corresponding ingredient strengths
				if (cdmDrugsWithIngredients.size() > 0) {
					List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
					Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
					for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
						if (sourceDrug.getComponents().size() == cdmDrug.getIngredients().size()) {
							List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), true);
							if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
								matchingCDMDrugs.add(cdmDrug);
								matchingIngredientsMap.put(cdmDrug, matchingIngredients);
							}
							else {
								List<CDMDrug> rejectedList = rejectedClinicalDrugs.get(sourceDrug);
								List<String> rejectedReasonsList = rejectedClinicalDrugsReasons.get(sourceDrug);
								if (rejectedList == null) {
									rejectedList = new ArrayList<CDMDrug>();
									rejectedClinicalDrugs.put(sourceDrug, rejectedList);
									rejectedReasonsList = new ArrayList<String>();
									rejectedClinicalDrugsReasons.put(sourceDrug, rejectedReasonsList);
								}
								rejectedList.add(cdmDrug);
								rejectedReasonsList.add("Incompatible Ingredients/Strengths");
							}
						}
					}
					if (matchingCDMDrugs.size() == 1) {
						drugMappingClinicalDrug.put(sourceDrug, matchingCDMDrugs.get(0)); 
						drugMappingClinicalDrugIngredients.put(sourceDrug, matchingIngredientsMap.get(matchingCDMDrugs.get(0)));
					}
					else if (matchingCDMDrugs.size() > 1) {
						List<CDMDrug> rejectedList = rejectedClinicalDrugs.get(sourceDrug);
						List<String> rejectedReasonsList = rejectedClinicalDrugsReasons.get(sourceDrug);
						if (rejectedList == null) {
							rejectedList = new ArrayList<CDMDrug>();
							rejectedClinicalDrugs.put(sourceDrug, rejectedList);
							rejectedReasonsList = new ArrayList<String>();
							rejectedClinicalDrugsReasons.put(sourceDrug, rejectedReasonsList);
						}
						multipleDrugMappings++;
						if (multipleClinicalDrugsMappingFile != null) {
							for (CDMDrug cdmDrug : matchingCDMDrugs) {
								for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
									multipleClinicalDrugsMappingFile.println(sourceDrug + "," + sourceDrug.getComponents().get(ingredientNr) + "," + cdmDrug + "," + cdmDrug.getIngredients().get(ingredientNr).toString());
								}
								rejectedList.add(cdmDrug);
								rejectedReasonsList.add("Multiple mappings");
							}
						}
					}
					else {
						if (noClinicalDrugsMappingFile != null) {
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								for (int ingredientNr = 0; ingredientNr < Math.max(sourceDrug.getComponents().size(), cdmDrug.getIngredients().size()) ; ingredientNr++) {
									String record = sourceDrug.toString();
									record += "," + (ingredientNr < sourceDrug.getComponents().size() ? sourceDrug.getComponents().get(ingredientNr).toString() : SourceDrugComponent.emptyRecord());
									record += "," + cdmDrug.toString();
									record += "," + (ingredientNr < cdmDrug.getIngredients().size() ? cdmDrug.getIngredients().get(ingredientNr).toString() : CDMIngredientStrength.emptyRecord());
									noClinicalDrugsMappingFile.println(record);
								}
							}
						}
					}
				}
			}
			else {
				List<CDMDrug> rejectedList = rejectedClinicalDrugs.get(sourceDrug);
				List<String> rejectedReasonsList = rejectedClinicalDrugsReasons.get(sourceDrug);
				if (rejectedList == null) {
					rejectedList = new ArrayList<CDMDrug>();
					rejectedClinicalDrugs.put(sourceDrug, rejectedList);
					rejectedReasonsList = new ArrayList<String>();
					rejectedClinicalDrugsReasons.put(sourceDrug, rejectedReasonsList);
				}
				rejectedList.add(null);
				rejectedReasonsList.add("No matching CDM Ingredients found");
			}
		}
		
		if (multipleClinicalDrugsMappingFile != null) {
			multipleClinicalDrugsMappingFile.close();
		}
		
		if (noClinicalDrugsMappingFile != null) {
			noClinicalDrugsMappingFile.close();
		}
		
		report.add("Source drugs mapped to multiple CDM Clinical Drugs: " + multipleDrugMappings);
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugComps() {
		boolean ok = true;
		Integer multipleDrugMappings = 0;
		String fileName = "";
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Comps ...");
		
		PrintWriter multipleClinicalDrugCompsMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Multiple Clinical Drug Comps Mappings.csv";
			multipleClinicalDrugCompsMappingFile = new PrintWriter(new File(fileName));
			multipleClinicalDrugCompsMappingFile.println(SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			multipleClinicalDrugCompsMappingFile = null;
		}
		
		PrintWriter noClinicalDrugCompsMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping No Clinical Drug Comps Mappings.csv";
			noClinicalDrugCompsMappingFile = new PrintWriter(new File(fileName));
			noClinicalDrugCompsMappingFile.println(SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			noClinicalDrugCompsMappingFile = null;
		}
		
		for (SourceDrug sourceDrug : sourceDrugsAllIngredientsMapped) {
			if (drugMappingClinicalDrug.get(sourceDrug) == null) {
				List<CDMIngredient> cdmDrugIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

				if (cdmDrugIngredients.size() > 0) {
					// Find CDM Clinical Drug Comps with corresponding ingredients
					Set<CDMDrug> cdmDrugCompsWithIngredients = null;
					for (CDMIngredient cdmIngredient : cdmDrugIngredients) {
						Set<CDMDrug> cdmDrugCompsWithIngredient = cdmDrugCompsContainingIngredient.get(cdmIngredient);
						if (cdmDrugCompsWithIngredient != null) {
							if (cdmDrugCompsWithIngredients == null) {
								cdmDrugCompsWithIngredients = new HashSet<CDMDrug>();
								cdmDrugCompsWithIngredients.addAll(cdmDrugCompsWithIngredient);
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
							cdmDrugCompsWithIngredients = new HashSet<CDMDrug>();
							break;
						}
					}
					
					// Find CDM Clinical Drug Comps with corresponding ingredient strengths
					if (cdmDrugCompsWithIngredients.size() > 0) {
						List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
						Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
						for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
							if (sourceDrug.getComponents().size() == cdmDrug.getIngredients().size()) {
								List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), true);
								if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
									matchingCDMDrugs.add(cdmDrug);
									matchingIngredientsMap.put(cdmDrug, matchingIngredients);
								}
								else {
									List<CDMDrug> rejectedList = rejectedClinicalDrugComps.get(sourceDrug);
									List<String> rejectedReasonsList = rejectedClinicalDrugCompsReasons.get(sourceDrug);
									if (rejectedList == null) {
										rejectedList = new ArrayList<CDMDrug>();
										rejectedClinicalDrugComps.put(sourceDrug, rejectedList);
										rejectedReasonsList = new ArrayList<String>();
										rejectedClinicalDrugCompsReasons.put(sourceDrug, rejectedReasonsList);
									}
									rejectedList.add(cdmDrug);
									rejectedReasonsList.add("Incompatible Ingredients/Strengths");
								}
							}
						}
						if (matchingCDMDrugs.size() == 1) {
							drugMappingClinicalDrugComp.put(sourceDrug, matchingCDMDrugs.get(0)); 
							drugMappingClinicalDrugCompIngredients.put(sourceDrug, matchingIngredientsMap.get(matchingCDMDrugs.get(0)));
						}
						else if (matchingCDMDrugs.size() > 1) {
							List<CDMDrug> rejectedList = rejectedClinicalDrugComps.get(sourceDrug);
							List<String> rejectedReasonsList = rejectedClinicalDrugCompsReasons.get(sourceDrug);
							if (rejectedList == null) {
								rejectedList = new ArrayList<CDMDrug>();
								rejectedClinicalDrugComps.put(sourceDrug, rejectedList);
								rejectedReasonsList = new ArrayList<String>();
								rejectedClinicalDrugCompsReasons.put(sourceDrug, rejectedReasonsList);
							}
							multipleDrugMappings++;
							if (multipleClinicalDrugCompsMappingFile != null) {
								for (CDMDrug cdmDrug : matchingCDMDrugs) {
									for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
										multipleClinicalDrugCompsMappingFile.println(sourceDrug + "," + sourceDrug.getComponents().get(ingredientNr) + "," + cdmDrug + "," + cdmDrug.getIngredients().get(ingredientNr).toString());
									}
									rejectedList.add(cdmDrug);
									rejectedReasonsList.add("Multiple mappings");
								}
							}
						}
						else {
							if (noClinicalDrugCompsMappingFile != null) {
								for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
									for (int ingredientNr = 0; ingredientNr < Math.max(sourceDrug.getComponents().size(), sourceDrug.getComponents().size()) ; ingredientNr++) {
										String record = sourceDrug.toString();
										record += "," + (ingredientNr < sourceDrug.getComponents().size() ? sourceDrug.getComponents().get(ingredientNr).toString() : SourceDrugComponent.emptyRecord());
										record += "," + cdmDrug.toString();
										record += "," + (ingredientNr < cdmDrug.getIngredients().size() ? cdmDrug.getIngredients().get(ingredientNr).toString() : CDMIngredientStrength.emptyRecord());
										noClinicalDrugCompsMappingFile.println(record);
									}
								}
							}
						}
					}
				}
				else {
					List<CDMDrug> rejectedList = rejectedClinicalDrugComps.get(sourceDrug);
					List<String> rejectedReasonsList = rejectedClinicalDrugCompsReasons.get(sourceDrug);
					if (rejectedList == null) {
						rejectedList = new ArrayList<CDMDrug>();
						rejectedClinicalDrugComps.put(sourceDrug, rejectedList);
						rejectedReasonsList = new ArrayList<String>();
						rejectedClinicalDrugCompsReasons.put(sourceDrug, rejectedReasonsList);
					}
					rejectedList.add(null);
					rejectedReasonsList.add("No matching CDM Ingredients found");
				}
			}
		}
		
		if (multipleClinicalDrugCompsMappingFile != null) {
			multipleClinicalDrugCompsMappingFile.close();
		}
		
		if (noClinicalDrugCompsMappingFile != null) {
			noClinicalDrugCompsMappingFile.close();
		}
		
		report.add("Source drugs mapped to multiple CDM Clinical Drug Comps: " + multipleDrugMappings);
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private boolean matchClinicalDrugForms() {
		boolean ok = true;
		Integer multipleDrugMappings = 0;
		String fileName = "";
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match source drugs to Clinical Drug Forms ...");
		
		PrintWriter multipleClinicalDrugFormsMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Multiple Clinical Drug Forms Mappings.csv";
			multipleClinicalDrugFormsMappingFile = new PrintWriter(new File(fileName));
			multipleClinicalDrugFormsMappingFile.println(SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			multipleClinicalDrugFormsMappingFile = null;
		}
		
		PrintWriter noClinicalDrugFormsMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping No Clinical Drug Forms Mappings.csv";
			noClinicalDrugFormsMappingFile = new PrintWriter(new File(fileName));
			noClinicalDrugFormsMappingFile.println(SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			noClinicalDrugFormsMappingFile = null;
		}
		
		for (SourceDrug sourceDrug : sourceDrugsAllIngredientsMapped) {
			if ((drugMappingClinicalDrug.get(sourceDrug) == null) && (drugMappingClinicalDrugComp.get(sourceDrug) == null)) {
				List<CDMIngredient> cdmDrugIngredients = sourceDrugsCDMIngredients.get(sourceDrug);

				if (cdmDrugIngredients.size() > 0) {
					// Find CDM Clinical Drugs with corresponding ingredients
					Set<CDMDrug> cdmDrugsWithIngredients = null;
					for (CDMIngredient cdmIngredient : cdmDrugIngredients) {
						Set<CDMDrug> cdmDrugsWithIngredient = cdmDrugFormsContainingIngredient.get(cdmIngredient);
						if (cdmDrugsWithIngredient != null) {
							if (cdmDrugsWithIngredients == null) {
								cdmDrugsWithIngredients = new HashSet<CDMDrug>();
								cdmDrugsWithIngredients.addAll(cdmDrugsWithIngredient);
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
							cdmDrugsWithIngredients = new HashSet<CDMDrug>();
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
									List<CDMDrug> rejectedList = rejectedClinicalDrugForms.get(sourceDrug);
									List<String> rejectedReasonsList = rejectedClinicalDrugFormsReasons.get(sourceDrug);
									if (rejectedList == null) {
										rejectedList = new ArrayList<CDMDrug>();
										rejectedClinicalDrugForms.put(sourceDrug, rejectedList);
										rejectedReasonsList = new ArrayList<String>();
										rejectedClinicalDrugFormsReasons.put(sourceDrug, rejectedReasonsList);
									}
									rejectedList.add(cdmDrug);
									rejectedReasonsList.add("Incompatible Form");
								}
							}
							cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
							if (cdmDrugsWithIngredients.size() == 0) {
								break;
							}
						}
					}
					

					if (cdmDrugsWithIngredients.size() > 0) {
						List<CDMDrug> matchingCDMDrugs = new ArrayList<CDMDrug>();
						Map<CDMDrug, List<CDMIngredientStrength>> matchingIngredientsMap = new HashMap<CDMDrug, List<CDMIngredientStrength>>();
						for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
							List<CDMIngredientStrength> matchingIngredients = matchingIngredients(sourceDrug.getComponents(), cdmDrug.getIngredientsMap(), false);
							if ((matchingIngredients != null) && (!matchingCDMDrugs.contains(cdmDrug))) {
								matchingCDMDrugs.add(cdmDrug);
								matchingIngredientsMap.put(cdmDrug, matchingIngredients);
							}
						}
						if (matchingCDMDrugs.size() == 1) {
							drugMappingClinicalDrugForm.put(sourceDrug, matchingCDMDrugs.get(0));
							drugMappingClinicalDrugFormIngredients.put(sourceDrug, matchingIngredientsMap.get(matchingCDMDrugs.get(0)));
						}
						else if (matchingCDMDrugs.size() > 1) {
							List<CDMDrug> rejectedList = rejectedClinicalDrugForms.get(sourceDrug);
							List<String> rejectedReasonsList = rejectedClinicalDrugFormsReasons.get(sourceDrug);
							if (rejectedList == null) {
								rejectedList = new ArrayList<CDMDrug>();
								rejectedClinicalDrugForms.put(sourceDrug, rejectedList);
								rejectedReasonsList = new ArrayList<String>();
								rejectedClinicalDrugFormsReasons.put(sourceDrug, rejectedReasonsList);
							}
							multipleDrugMappings++;
							if (multipleClinicalDrugFormsMappingFile != null) {
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
										multipleClinicalDrugFormsMappingFile.println(sourceDrug + "," + sourceDrug.getComponents().get(ingredientNr) + "," + cdmDrug + "," + cdmDrug.getIngredients().get(ingredientNr).toString());
									}
									rejectedList.add(cdmDrug);
									rejectedReasonsList.add("Form Multiple mappings");
								}
							}
						}
					}
					else {
						if (noClinicalDrugFormsMappingFile != null) {
							for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
								for (int ingredientNr = 0; ingredientNr < Math.max(sourceDrug.getComponents().size(), cdmDrug.getIngredients().size()) ; ingredientNr++) {
									String record = sourceDrug.toString();
									record += "," + (ingredientNr < sourceDrug.getComponents().size() ? sourceDrug.getComponents().get(ingredientNr).toString() : SourceDrugComponent.emptyRecord());
									record += "," + cdmDrug.toString();
									record += "," + (ingredientNr < cdmDrug.getIngredients().size() ? cdmDrug.getIngredients().get(ingredientNr).toString() : CDMIngredientStrength.emptyRecord());
									noClinicalDrugFormsMappingFile.println(record);
								}
							}
						}
					}
					
/*			
					// Find CDM Clinical Drugs with corresponding ingredient strengths
					if (cdmDrugsWithIngredients.size() > 0) {
						Set<CDMDrug> matchingCDMDrugs = new HashSet<CDMDrug>();
						for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
							if (sourceDrug.getComponents().size() == cdmDrug.getIngredients().size()) {
								for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
									SourceDrugComponent sourceComponent = sourceDrug.getComponents().get(ingredientNr);
									CDMIngredientStrength cdmIngredientStrength = cdmDrug.getIngredients().get(ingredientNr);
									if (sourceComponent.matches(unitConversionsMap, cdmIngredientStrength.getNumeratorDosage(), cdmIngredientStrength.getNumeratorDosageUnit(), cdmIngredientStrength.getDenominatorDosage(), cdmIngredientStrength.getDenominatorDosageUnit())) {
										matchingCDMDrugs.add(cdmDrug);
									}
								}
							}
						}
						if (matchingCDMDrugs.size() == 1) {
							drugMappingClinicalDrugForm.put(sourceDrug, (CDMDrug) matchingCDMDrugs.toArray()[0]); 
						}
						else if (matchingCDMDrugs.size() > 1) {
							multipleDrugMappings++;
							if (multipleClinicalDrugFormsMappingFile != null) {
								for (CDMDrug cdmDrug : matchingCDMDrugs) {
									for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
										multipleClinicalDrugFormsMappingFile.println(sourceDrug + "," + sourceDrug.getComponents().get(ingredientNr) + "," + cdmDrug + "," + cdmDrug.getIngredients().get(ingredientNr).toStringLong());
									}
								}
							}
						}
						else {
							if (noClinicalDrugFormsMappingFile != null) {
								for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
									for (int ingredientNr = 0; ingredientNr < Math.max(sourceDrug.getComponents().size(), cdmDrug.getIngredients().size()) ; ingredientNr++) {
										String record = sourceDrug.toString();
										record += "," + (ingredientNr < sourceDrug.getComponents().size() ? sourceDrug.getComponents().get(ingredientNr).toString() : SourceDrugComponent.emptyRecord());
										record += "," + cdmDrug.toString();
										record += "," + (ingredientNr < cdmDrug.getIngredients().size() ? cdmDrug.getIngredients().get(ingredientNr).toStringLong() : CDMIngredientStrength.emptyRecordLong());
										noClinicalDrugFormsMappingFile.println(record);
									}
								}
							}
						}
					}
*/
				}
				else {
					List<CDMDrug> rejectedList = rejectedClinicalDrugForms.get(sourceDrug);
					List<String> rejectedReasonsList = rejectedClinicalDrugFormsReasons.get(sourceDrug);
					if (rejectedList == null) {
						rejectedList = new ArrayList<CDMDrug>();
						rejectedClinicalDrugForms.put(sourceDrug, rejectedList);
						rejectedReasonsList = new ArrayList<String>();
						rejectedClinicalDrugFormsReasons.put(sourceDrug, rejectedReasonsList);
					}
					rejectedList.add(null);
					rejectedReasonsList.add("No matching CDM Ingredients found");
				}
			}
		}
		
		if (multipleClinicalDrugFormsMappingFile != null) {
			multipleClinicalDrugFormsMappingFile.close();
		}
		
		if (noClinicalDrugFormsMappingFile != null) {
			noClinicalDrugFormsMappingFile.close();
		}
		
		report.add("Source drugs mapped to multiple CDM Clinical Drug Forms: " + multipleDrugMappings);
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	
	private void saveMapping() {
		String fileName = "";
		
		System.out.println(DrugMapping.getCurrentTime() + "     Saving Drug Mapping ...");
		
		PrintWriter drugMappingFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping.csv";
			drugMappingFile = new PrintWriter(new File(fileName));
			drugMappingFile.println("MappingType," + SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + "," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			drugMappingFile = null;
		}

		for (SourceDrug sourceDrug : sourceDrugs) {
			String mappingType = "Unmapped";
			List<CDMIngredientStrength> ingredientStrengths = new ArrayList<CDMIngredientStrength>();
			CDMDrug cdmDrug = drugMappingClinicalDrug.get(sourceDrug);
			if (cdmDrug != null) {
				mappingType = "Clinical Drug";
				ingredientStrengths = drugMappingClinicalDrugIngredients.get(sourceDrug);
			}
			else {
				cdmDrug = drugMappingClinicalDrugComp.get(sourceDrug);
				if (cdmDrug != null) {
					mappingType = "Clinical Drug Comp";
					ingredientStrengths = drugMappingClinicalDrugCompIngredients.get(sourceDrug);
				}
				else {
					cdmDrug = drugMappingClinicalDrugForm.get(sourceDrug);
					if (cdmDrug != null) {
						mappingType = "Clinical Drug Form";
						ingredientStrengths = drugMappingClinicalDrugFormIngredients.get(sourceDrug);
					}
				}
			}
			if (cdmDrug != null) {
				if (sourceDrug.getComponents().size() != ingredientStrengths.size()) {
					System.out.println("ERROR: " + sourceDrug + "SourceIngredients: " + Integer.toString(sourceDrug.getComponents().size()) + " CDMIngredients: " + Integer.toString(ingredientStrengths.size()));
				}
				for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
					String record = mappingType;
					record += "," + sourceDrug.toString();
					record += "," + sourceDrug.getComponents().get(ingredientNr).toString();
					record += "," + cdmDrug.toString();
					record += "," + (mappingType == "Clinical Drug Form" ? ingredientStrengths.get(ingredientNr).toStringIngredient() : ingredientStrengths.get(ingredientNr).toString());

					drugMappingFile.println(record);
				}
			}
			else {
				for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
					String record = mappingType;
					record += "," + (ingredientNr == 0 ? sourceDrug.toString() : SourceDrug.emptyRecord());
					record += "," + sourceDrug.getComponents().get(ingredientNr).toString();
					record += "," + mappingType;
					record += "," + CDMDrug.emptyRecord();
					record += "," + CDMIngredientStrength.emptyRecord();

					drugMappingFile.println(record);
				}
			}
		}
		
		if (drugMappingFile != null) {
			drugMappingFile.close();
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void saveRejectedMappings() {
		String fileName = "";
		System.out.println(DrugMapping.getCurrentTime() + "     Saving Rejected Drug Mappings ...");


		PrintWriter drugMappingRejectedFile = null;
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Rejected.csv";
			drugMappingRejectedFile = new PrintWriter(new File(fileName));
			drugMappingRejectedFile.println("MappingType," + SourceDrug.getHeader() + "," + SourceDrugComponent.getHeader() + ",Reject Reason," + CDMDrug.getHeader("CDMDrug_") + "," + CDMIngredientStrength.getHeader("CDMIngredient_"));
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			drugMappingRejectedFile = null;
		}

		for (SourceDrug sourceDrug : sourceDrugs) {
			String mappingType = "Unknown";
			List<CDMIngredientStrength> ingredientStrengths = new ArrayList<CDMIngredientStrength>();
			List<CDMDrug> rejectedCDMDrugs = rejectedClinicalDrugs.get(sourceDrug);
			List<String> rejectReasons = rejectedClinicalDrugsReasons.get(sourceDrug);
			if (rejectedCDMDrugs != null) {
				mappingType = "Clinical Drug";
				ingredientStrengths = drugMappingClinicalDrugIngredients.get(sourceDrug);
			}
			else {
				rejectedCDMDrugs = rejectedClinicalDrugComps.get(sourceDrug);
				rejectReasons = rejectedClinicalDrugCompsReasons.get(sourceDrug);
				if (rejectedCDMDrugs != null) {
					mappingType = "Clinical Drug Comp";
					ingredientStrengths = drugMappingClinicalDrugCompIngredients.get(sourceDrug);
				}
				else {
					rejectedCDMDrugs = rejectedClinicalDrugForms.get(sourceDrug);
					rejectReasons = rejectedClinicalDrugFormsReasons.get(sourceDrug);
					if (rejectedCDMDrugs != null) {
						mappingType = "Clinical Drug Form";
						ingredientStrengths = drugMappingClinicalDrugFormIngredients.get(sourceDrug);
					}
				}
			}
			if (rejectedCDMDrugs != null) {
				for (int drugNr = 0; drugNr < rejectedCDMDrugs.size(); drugNr++) {
					CDMDrug rejectedCDMDrug = rejectedCDMDrugs.get(drugNr);
					String rejectReason = rejectReasons.get(drugNr);
					if (rejectedCDMDrug != null) {
						if (sourceDrug.getComponents() != null) {
							if (ingredientStrengths != null) {
								if (sourceDrug.getComponents().size() != ingredientStrengths.size()) {
									System.out.println("ERROR: " + sourceDrug + "SourceIngredients: " + Integer.toString(sourceDrug.getComponents().size()) + " CDMIngredients: " + Integer.toString(ingredientStrengths.size()));
								}
								for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
									String record = mappingType;
									record += "," + sourceDrug.toString();
									record += "," + sourceDrug.getComponents().get(ingredientNr).toString();
									record += "," + rejectReason;
									record += "," + rejectedCDMDrug.toString();
									record += "," + (mappingType == "Clinical Drug Form" ? ingredientStrengths.get(ingredientNr).toStringIngredient() : ingredientStrengths.get(ingredientNr).toString());

									drugMappingRejectedFile.println(record);
								}
							}
							else {
								for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
									String record = mappingType;
									record += "," + sourceDrug.toString();
									record += "," + sourceDrug.getComponents().get(ingredientNr).toString();
									record += "," + rejectReason;
									record += "," + rejectedCDMDrug.toString();
									record += "," + CDMIngredient.emptyRecord();

									drugMappingRejectedFile.println(record);
								}
							}
						}
						else {
							if ((ingredientStrengths != null) && (ingredientStrengths.size() > 0)) {
								for (int ingredientNr = 0; ingredientNr < ingredientStrengths.size(); ingredientNr++) {
									String record = mappingType;
									record += "," + sourceDrug.toString();
									record += "," + SourceDrugComponent.emptyRecord();
									record += "," + rejectReason;
									record += "," + rejectedCDMDrug.toString();
									record += "," + (mappingType == "Clinical Drug Form" ? ingredientStrengths.get(ingredientNr).toStringIngredient() : ingredientStrengths.get(ingredientNr).toString());

									drugMappingRejectedFile.println(record);
								}
							}
							else {
								String record = mappingType;
								record += "," + sourceDrug.toString();
								record += "," + SourceDrugComponent.emptyRecord();
								record += "," + rejectReason;
								record += "," + rejectedCDMDrug.toString();
								record += "," + CDMIngredient.emptyRecord();

								drugMappingRejectedFile.println(record);
							}
						}
					}
					else {
						if (sourceDrug.getComponents() != null) {
							for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
								String record = mappingType;
								record += "," + sourceDrug.toString();
								record += "," + sourceDrug.getComponents().get(ingredientNr).toString();
								record += "," + rejectReason;
								record += "," + CDMDrug.emptyRecord();
								record += "," + CDMIngredient.emptyRecord();

								drugMappingRejectedFile.println(record);
							}
						}
						else {
							String record = mappingType;
							record += "," + sourceDrug.toString();
							record += "," + SourceDrugComponent.emptyRecord();
							record += "," + rejectReason;
							record += "," + CDMDrug.emptyRecord();
							record += "," + CDMIngredient.emptyRecord();

							drugMappingRejectedFile.println(record);
						}
					}
				}
			}
		}
		
		if (drugMappingRejectedFile != null) {
			drugMappingRejectedFile.close();
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void finalReport() {
		Integer dataCountTotal = 0;
		Integer dataCoverageClinicalDrugs = 0;
		Integer dataCoverageClinicalDrugComps = 0;
		Integer dataCoverageClinicalDrugForms = 0;
		for (SourceDrug sourceDrug : sourceDrugs) {
			dataCountTotal += sourceDrug.getCount();
			if (drugMappingClinicalDrug.containsKey(sourceDrug)) {
				dataCoverageClinicalDrugs += sourceDrug.getCount();
			}
			else if (drugMappingClinicalDrugComp.containsKey(sourceDrug)) {
				dataCoverageClinicalDrugComps += sourceDrug.getCount();
			}
			else if (drugMappingClinicalDrugForm.containsKey(sourceDrug)) {
				dataCoverageClinicalDrugForms += sourceDrug.getCount();
			}
		}
		
		report.add("");
		report.add("Source drugs mapped to single CDM Clinical Drug: " + Integer.toString(drugMappingClinicalDrug.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Comp: " + Integer.toString(drugMappingClinicalDrugComp.size()));
		report.add("Source drugs mapped to single CDM Clinical Drug Form: " + Integer.toString(drugMappingClinicalDrugForm.size()));
		report.add("Total Source drugs mapped: " + (drugMappingClinicalDrug.size() + drugMappingClinicalDrugComp.size() + drugMappingClinicalDrugForm.size()) + " of " + sourceDrugs.size() + " (" + Long.toString(Math.round(((double) (drugMappingClinicalDrug.size() + drugMappingClinicalDrugComp.size() + drugMappingClinicalDrugForm.size()) / (double) sourceDrugs.size()) * 100)) + "%)");
		
		if (dataCountTotal != 0) {
			report.add("");
			report.add("Datacoverage CDM Clinical Drug mapping: " + dataCoverageClinicalDrugs + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugs / (double) dataCountTotal) * 100)) + "%)");
			report.add("Datacoverage CDM Clinical Drug Comp mapping: " + dataCoverageClinicalDrugComps + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugComps / (double) dataCountTotal) * 100)) + "%)");
			report.add("Datacoverage CDM Clinical Drug Form mapping: " + dataCoverageClinicalDrugForms + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugForms / (double) dataCountTotal) * 100)) + "%)");
			report.add("Total datacoverage drug mapping: " + (dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms) + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) (dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms) / (double) dataCountTotal) * 100)) + "%)");
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
	
	
	private List<CDMIngredientStrength> matchingIngredients(List<SourceDrugComponent> sourceDrugComponents, Map<String, Set<CDMIngredientStrength>> cdmIngredientsMap, boolean useStrength) {
		List<CDMIngredientStrength> matchingIngredients = new ArrayList<CDMIngredientStrength>();
		
		for (SourceDrugComponent sourceDrugComponent : sourceDrugComponents) {
			SourceIngredient sourceIngredient = sourceDrugComponent.getIngredient();
			if (sourceIngredient != null) {
				String cdmIngredientConceptId = sourceIngredient.getMatchingIngredient();
				if (cdmIngredientConceptId != null) {
					Set<CDMIngredientStrength> matchingCDMIngredients = cdmIngredientsMap.get(cdmIngredientConceptId);
					if (matchingCDMIngredients != null) {
						if (useStrength) {
							boolean found = false;
							for (CDMIngredientStrength cdmIngredientStrength : matchingCDMIngredients) {
								if (sourceDrugComponent.matches(unitConversionsMap, cdmIngredientStrength.getNumeratorDosage(), cdmIngredientStrength.getNumeratorDosageUnit(), cdmIngredientStrength.getDenominatorDosage(), cdmIngredientStrength.getDenominatorDosageUnit())) {
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
	
	
	private String uniformCASNumber(String casNumber) {
		casNumber = casNumber.replaceAll(" ", "").replaceAll("-", "");
		if (!casNumber.equals("")) {
			casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
			casNumber = ("000000"+casNumber).substring(casNumber.length() -5);
		}
		return  casNumber;
	}
	
	
	private String modifyName(String name) {
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
		
		return name.replaceAll(" ", "").replaceAll("-", "").replaceAll(",", "");
	}
	
	
/*
	public static void main(String[] args) {
	}
*/
}
