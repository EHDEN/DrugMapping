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
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.cdm.CDMDrug;
import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.cdm.CDMIngredientStrength;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.Row;

public class GenericMapping extends Mapping {
	private Set<String> forms = new HashSet<String>();
	private Set<String> units = new HashSet<String>();

	private Map<String, String> casMap = new HashMap<String, String>();
	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<String, List<CDMIngredient>> cdmIngredientNameIndex = new HashMap<String, List<CDMIngredient>>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<String, Set<CDMIngredient>> mapsToCDMIngredient = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, List<CDMIngredient>> synonymsToCDMIngredients = new HashMap<String, List<CDMIngredient>>();
	private Map<String, Set<String>> drugNameSynonyms = new HashMap<String, Set<String>>();
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<SourceDrug, CDMDrug> drugMapping = new HashMap<SourceDrug, CDMDrug>();
	private Map<String, CDMDrug> cdmDrugComps = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<SourceDrug, CDMDrug> drugCompMapping = new HashMap<SourceDrug, CDMDrug>();
	
	private int noATCCounter = 0; // Counter of source drugs without ATC code.
	
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");
		SourceDrug.init();
		
		// Get CDM Ingredients
		ok = getCDMIngredients(database);	
		
		// Load source drug ingredient mapping
		ok = ok && getSourceDrugs(sourceDrugsFile) && (!SourceDrug.errorOccurred());
		
		// Match ingredients by full name
		ok = ok && matchIngredients();
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
	
	
	private boolean getCDMIngredients(CDMDatabase database) {
		boolean ok = true;
		String fileName = "";
		try {
			// Create output files
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping RxNorm Ingredients.csv";
			PrintWriter rxNormIngredientsFile = new PrintWriter(new File(fileName));
			rxNormIngredientsFile.println(CDMIngredient.getHeaderWithSynonyms());
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Maps To RxNorm Ingredients.csv";
			PrintWriter mapsToRxNormIngredientsFile = new PrintWriter(new File(fileName));
			mapsToRxNormIngredientsFile.println("SourceName,Synonym," + CDMIngredient.getHeader());
			
			// Load CDM ingredients
			System.out.println(DrugMapping.getCurrentTime() + "     Get CDM ingredients ...");
			
			QueryParameters queryParameters = new QueryParameters();
			queryParameters.set("@vocab", database.getVocabSchema());
		
			// Connect to the database
			RichConnection connection = database.getRichConnection(this.getClass());
			
			// Get RxNorm ingredients
			CDMIngredient lastCdmIngredient = null;
			for (Row queryRow : connection.queryResource("../cdm/GetRxNormIngredients.sql", queryParameters)) {
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
						String cdmIngredientNameNoSpaces = cdmIngredient.getConceptName().replaceAll("\n", " ").replaceAll("\r", " ").replaceAll(" ", "").replaceAll("-", "");
						List<CDMIngredient> existingCDMIngredients = cdmIngredientNameIndex.get(cdmIngredientNameNoSpaces);
						if (existingCDMIngredients == null) {
							existingCDMIngredients = new ArrayList<CDMIngredient>();
							cdmIngredientNameIndex.put(cdmIngredientNameNoSpaces, existingCDMIngredients);
						}
						existingCDMIngredients.add(cdmIngredient);
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
			for (Row queryRow : connection.queryResource("../cdm/GetMapsToRxNormIngredients.sql", queryParameters)) {
				String drugName = queryRow.get("drug_name").trim().toUpperCase();
				String cdmIngredientConceptId = queryRow.get("mapsto_concept_id").trim();
				String drugNameSynonym = queryRow.get("drug_synonym_name").trim().toUpperCase();
				CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
				List<CDMIngredient> synonymIngredients = synonymsToCDMIngredients.get(drugNameSynonym); 
				if (synonymIngredients == null) {
					synonymIngredients = new ArrayList<CDMIngredient>();
					synonymsToCDMIngredients.put(drugNameSynonym, synonymIngredients);
				}
				if (!synonymIngredients.contains(cdmIngredient)) {
					synonymIngredients.add(cdmIngredient);
				}
				if (synonymIngredients.size() > 1) {
					System.out.println("      WARNING: Multiple ingredients found for synonym '" + drugNameSynonym + "'!");
					//ok = false;
				}
				
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
			System.out.println("         Found " + Integer.toString(cdmIngredients.size()) + " CDM RxNorm ingredients");
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
							drugsContainingIngredient.add(cdmDrug);
						}
					}
				} 
			}
			
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
			
			// Close database connection
			connection.close();
			
			rxNormIngredientsFile.close();
			mapsToRxNormIngredientsFile.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getSourceDrugs(InputFile sourceDrugsFile) {
		boolean sourceDrugError = false;
		String fileName = "";
		
		System.out.println(DrugMapping.getCurrentTime() + "     Loading source drugs ...");
		
		try {
			// Create output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
			PrintWriter missingATCFile = new PrintWriter(new File(fileName));
			SourceDrug.writeHeaderToFile(missingATCFile);
			
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
				
				missingATCFile.close();
				
				System.out.println("         Found " + Integer.toString(sourceDrugs.size()) + " source drugs");
				System.out.println("         Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
				System.out.println("         For " + SourceDrug.getCASNumbersSet() + " source ingredients the CAS number is set");
			}
		} 
		catch (FileNotFoundException e) {
			System.out.println("       ERROR: Cannot create output file '" + fileName + "'");
			sourceDrugError = true;
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	private boolean matchIngredients() {
		boolean ok = true;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by full name ...");
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			List<CDMIngredient> cdmIngredients = cdmIngredientNameIndex.get(sourceIngredient.getIngredientNameEnglishNoSpaces());
			if (cdmIngredients != null) {
				if (cdmIngredients.size() == 1) {
					ingredientMap.put(sourceIngredient, cdmIngredients.get(0));
				}
				else {
					multipleMappings++;
				}
			}
			else {
				cdmIngredients = synonymsToCDMIngredients.get(sourceIngredient.getIngredientNameEnglishNoSpaces());
				if (cdmIngredients != null) {
					if (cdmIngredients.size() == 1) {
						ingredientMap.put(sourceIngredient, cdmIngredients.get(0));
					}
					else {
						multipleMappings++;
					}
				}
			}
		}
		
		System.out.println("         Source ingredients mapped: " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println("         Multiple mappings found: " + multipleMappings + " (" + Long.toString(Math.round(((double) multipleMappings / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
	}
	
	/*
	public static void main(String[] args) {
	}
	*/
}
