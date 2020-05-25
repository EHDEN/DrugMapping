package org.ohdsi.drugmapping.cdm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class CDM {
	private boolean ok = false;

	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<String, Set<CDMIngredient>> cdmIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	
	private Map<String, Set<CDMIngredient>> cdmEquivalentIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	
	private Map<String, Set<CDMIngredient>> cdmMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	
	private Map<String, Set<CDMIngredient>> cdmReplacedByIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	
	private Map<String, CDMIngredient> cdmReplacedByIngredientConceptIdIndex = new HashMap<String, CDMIngredient>();
	
	private List<String> cdmIngredientNameIndexNameList = new ArrayList<String>();
	private Map<String, Map<String, Set<CDMIngredient>>> cdmIngredientNameIndexMap = new HashMap<String, Map<String, Set<CDMIngredient>>>();
	
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();

	private Map<String, CDMDrug> cdmDrugComps = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();

	private Map<String, CDMDrug> cdmDrugForms = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugFormsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();

	private Map<String, Set<CDMIngredient>> cdmATCIngredientMap = new HashMap<String, Set<CDMIngredient>>();

	private Map<String, CDMIngredient> cdmCASIngredientMap = new HashMap<String, CDMIngredient>();
	
	private Map<String, String> cdmUnitNameToConceptIdMap = new HashMap<String, String>();                     // Map from CDM unit concept_name to CDM unit concept_id
	private Map<String, String> cdmUnitConceptIdToNameMap = new HashMap<String, String>();                     // Map from CDM unit concept_id to CDM unit concept_name
	private List<String> cdmUnitConceptNames = new ArrayList<String>();                                        // List of CDM unit names for sorting

	private Map<String, String> cdmFormNameToConceptIdMap = new HashMap<String, String>();                     // Map from CDM form concept_name to CDM form concept_id
	private Map<String, String> cdmFormConceptIdToNameMap = new HashMap<String, String>();                     // Map from CDM form concept_id to CDM form concept_name
	private List<String> cdmFormConceptNames = new ArrayList<String>();                                        // List of CDM form names for sorting
	
	
	
	public CDM(CDMDatabase database, List<String> report) {
		cdmIngredientNameIndexNameList.add("Ingredient");
		cdmIngredientNameIndexMap.put("Ingredient", cdmIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("MapsToIngredient");
		cdmIngredientNameIndexMap.put("MapsToIngredient", cdmMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("ReplacedByIngredient");
		cdmIngredientNameIndexMap.put("ReplacedByIngredient", cdmReplacedByIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("EquivalentToIngredient");
		cdmIngredientNameIndexMap.put("EquivalentToIngredient", cdmEquivalentIngredientNameIndex);
		
		try {
			QueryParameters queryParameters = new QueryParameters();
			queryParameters.set("@vocab", database.getVocabSchema());

			// Connect to the database
			RichConnection connection = database.getRichConnection(CDM.class);
			
			// Get CDM ingredients
			getRxNormIngredients(connection, queryParameters, report);
			
			// Get "%RxNorm eq" RxNorm Ingredients
			getRxNormEqRxNormIngredients(connection, queryParameters, report);
			
			// Get "Maps to" RxNorm Ingredients
			getMapsToRxNormIngredients(connection, queryParameters, report);
			
			// Get CDM concepts replaced by RxNorm Ingredients
			getConceptsReplacedByRxNormIngredients(connection, queryParameters, report);
					
			// Get RxNorm Clinical Drugs with Form and Ingredients
			getRxNormClinicalDrugsWithIngredients(connection, queryParameters, report);
					
			// Get RxNorm Clinical Drugs with Form and Ingredients
			getRxNormClinicalDrugCompsWithIngredients(connection, queryParameters, report);
					
			// Get RxNorm Clinical Drugs with Form and Ingredients
			getRxNormClinicalDrugFormsWithIngredients(connection, queryParameters, report);
					
			// Get CDM RxNorm Ingredient ATCs
			getRxNormIngredientATCs(connection, queryParameters, report);
					
			// Get CAS code to CDM RxNorm (Extension) Ingredient mapping
			getCASToRxNormIngredientsMapping(connection, queryParameters, report);
			
			// Get CDM units
			getCDMUnits(connection, queryParameters, report);
			
			// Get CDM Forms
			getCDMForms(connection, queryParameters, report);
			
			// Write Ingredients Name index to file
			writeIngredientsNameIndexToFile();
			
			// Close database connection
			connection.close();
			
			ok = true;
		}
		catch (Exception exception) {
			ok = false;
			exception.printStackTrace();
		}
	}
	
	
	public boolean isOK() {
		return ok;
	}
	
	
	public Map<String, CDMIngredient> getCDMIngredients() {
		return cdmIngredients;
	}


	public Map<String, CDMIngredient> getCDMReplacedByIngredientConceptIdIndex() {
		return cdmReplacedByIngredientConceptIdIndex;
	}


	public List<String> getCDMIngredientNameIndexNameList() {
		return cdmIngredientNameIndexNameList;
	}


	public Map<String, Map<String, Set<CDMIngredient>>> getCDMIngredientNameIndexMap() {
		return cdmIngredientNameIndexMap;
	}


	public Map<String, CDMDrug> getCDMDrugs() {
		return cdmDrugs;
	}


	public Map<CDMIngredient, List<CDMDrug>> getCDMDrugsContainingIngredient() {
		return cdmDrugsContainingIngredient;
	}


	public Map<String, CDMDrug> getCDMDrugComps() {
		return cdmDrugComps;
	}


	public Map<CDMIngredient, List<CDMDrug>> getCDMDrugCompsContainingIngredient() {
		return cdmDrugCompsContainingIngredient;
	}


	public Map<String, CDMDrug> getCDMDrugForms() {
		return cdmDrugForms;
	}


	public Map<CDMIngredient, List<CDMDrug>> getCDMDrugFormsContainingIngredient() {
		return cdmDrugFormsContainingIngredient;
	}


	public Map<String, Set<CDMIngredient>> getCDMATCIngredientMap() {
		return cdmATCIngredientMap;
	}


	public Map<String, CDMIngredient> getCDMCASIngredientMap() {
		return cdmCASIngredientMap;
	}
	
	
	public Map<String, String> getCDMUnitNameToConceptIdMap() {
		return cdmUnitNameToConceptIdMap;
	}
	
	
	public Map<String, String> getCDMUnitConceptIdToNameMap() {
		return cdmUnitConceptIdToNameMap;
	}
	
	
	public List<String> getCDMUnitConceptNames() {
		return cdmUnitConceptNames;
	}
	
	
	public String getCDMUnitConceptName(String conceptId) {
		return getCDMUnitConceptIdToNameMap().get(conceptId);
	}
	
	
	public String getCDMUnitConceptId(String conceptName) {
		return getCDMUnitNameToConceptIdMap().get(conceptName);
	}
	
	
	public Map<String, String> getCDMFormNameToConceptIdMap() {
		return cdmFormNameToConceptIdMap;
	}
	
	
	public Map<String, String> getCDMFormConceptIdToNameMap() {
		return cdmFormConceptIdToNameMap;
	}
	
	
	public List<String> getCDMFormConceptNames() {
		return cdmFormConceptNames;
	}
	
	
	public String getCDMFormConceptName(String conceptId) {
		return getCDMFormConceptIdToNameMap().get(conceptId);
	}
	
	
	public String getCDMFormConceptId(String conceptName) {
		return getCDMFormNameToConceptIdMap().get(conceptName);
	}

/*
	public List<CDMIngredient> getCDMIngredientsList() {
		return cdmIngredientsList;
	}


	public Map<String, Set<CDMIngredient>> getCDMIngredientNameIndex() {
		return cdmIngredientNameIndex;
	}


	public Map<String, Set<CDMIngredient>> getCDMEquivalentIngredientNameIndex() {
		return cdmEquivalentIngredientNameIndex;
	}


	public Map<String, Set<CDMIngredient>> getCDMMapsToIngredientNameIndex() {
		return cdmMapsToIngredientNameIndex;
	}


	public Map<String, Set<CDMIngredient>> getCDMReplacedByIngredientNameIndex() {
		return cdmReplacedByIngredientNameIndex;
	}
*/	
	
	private void getRxNormIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Ingredients ...");
		
		PrintWriter rxNormIngredientsFile = null;
		String fileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + "DrugMapping RxNorm Ingredients.csv";
		try {
			// Create output file
			rxNormIngredientsFile = new PrintWriter(new File(fileName));
			rxNormIngredientsFile.println(CDMIngredient.getHeaderWithSynonyms());
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
			rxNormIngredientsFile = null;
		}
		
		// Get RxNorm ingredients
		CDMIngredient lastCdmIngredient = null;
		for (Row queryRow : connection.queryResource("GetRxNormIngredients.sql", queryParameters)) {
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
					
					String cdmIngredientNameNoSpaces = cdmIngredient.getConceptName();
					if (cdmIngredientNameNoSpaces.contains("(")) {
						if (cdmIngredientNameNoSpaces.indexOf("(") == 0) {
							cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll("[(]", "").replaceAll("[)]", "");
						}
						else {
							cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.substring(0, cdmIngredientNameNoSpaces.indexOf("(")).trim();
						}
					}
					while (cdmIngredientNameNoSpaces.contains(",")) 
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll(",", "");
					while (cdmIngredientNameNoSpaces.contains("-")) 
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll("-", "");
					while (cdmIngredientNameNoSpaces.contains(" ")) 
						cdmIngredientNameNoSpaces = cdmIngredientNameNoSpaces.replaceAll(" ", "");
					String cdmIngredientNameModified = DrugMappingStringUtilities.modifyName(cdmIngredient.getConceptName());
					String cdmIngredientNameModifiedNoSpaces = cdmIngredientNameModified.replaceAll(" ", "");
					
					List<String> nameList = new ArrayList<String>();
					nameList.add(cdmIngredient.getConceptName());
					if (!nameList.contains(cdmIngredientNameModified))         nameList.add(cdmIngredientNameModified);
					if (!nameList.contains(cdmIngredientNameNoSpaces))         nameList.add(cdmIngredientNameNoSpaces);
					if (!nameList.contains(cdmIngredientNameModifiedNoSpaces)) nameList.add(cdmIngredientNameModifiedNoSpaces);
					
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
			nameList.add(DrugMappingStringUtilities.modifyName(cdmIngredientSynonym));
			
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
	}
	
	
	private void getRxNormEqRxNormIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM '%RxNorm eq' RxNorm Ingredients ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormEquivalentIngredients.sql", queryParameters)) {
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
				nameList.add(DrugMappingStringUtilities.modifyName(drugConceptName));
				nameList.add(DrugMappingStringUtilities.modifyName(drugNameSynonym));
				
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
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getMapsToRxNormIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM 'Maps to' RxNorm Ingredients ...");
		
		for (Row queryRow : connection.queryResource("GetMapsToRxNormIngredients.sql", queryParameters)) {
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
				nameList.add(DrugMappingStringUtilities.modifyName(drugConceptName));
				nameList.add(DrugMappingStringUtilities.modifyName(drugNameSynonym));
				
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
	}
	
	
	private void getConceptsReplacedByRxNormIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM concepts replaced by RxNorm Ingredients ...");
		
		for (Row queryRow : connection.queryResource("GetConceptReplacedByRxNormIngredient.sql", queryParameters)) {
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
				String cdmReplacedNameModified = DrugMappingStringUtilities.modifyName(cdmReplacedByName);
			
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
	}
	
	
	private void getRxNormClinicalDrugsWithIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drugs with ingredients ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormClinicalDrugsIngredients.sql", queryParameters)) {
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
	}
	
	
	private void getRxNormClinicalDrugCompsWithIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Comps with ingredients ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormClinicalDrugCompsIngredients.sql", queryParameters)) {
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
	}
	
	
	private void getRxNormClinicalDrugFormsWithIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Forms with ingredients ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormClinicalDrugFormsIngredients.sql", queryParameters)) {
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
	}
	
	
	private void getRxNormIngredientATCs(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Ingredient ATCs ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormIngredientATC.sql", queryParameters)) {
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
		
		report.add("CDM Ingredients with ATC: " + DrugMappingNumberUtilities.percentage((long) atcCount, (long) cdmIngredients.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getCASToRxNormIngredientsMapping(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM CAS number to Ingredient mapping ...");
		
		Integer casCount = 0;
		for (Row queryRow : connection.queryResource("GetCASMapsToRxNormIngredients.sql", queryParameters)) {
			String cdmCASNr = DrugMappingNumberUtilities.uniformCASNumber(queryRow.get("casnr", true));
			String cdmIngredientConceptId = queryRow.get("concept_id", true);
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			CDMIngredient previousIngredient = cdmCASIngredientMap.get(cdmCASNr);
			CDMIngredient lastCdmIngredient = null;
			
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
		
		report.add("CDM Ingredients with CAS number: " + DrugMappingNumberUtilities.percentage((long) casCount, (long) cdmIngredients.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getCDMUnits(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM units ...");
		
		// Get CDM Forms
		for (Row queryRow : connection.queryResource("GetCDMUnits.sql", queryParameters)) {
			
			String concept_id   = queryRow.get("concept_id", true).trim();
			String concept_name = queryRow.get("concept_name", true).trim();
			
			cdmUnitNameToConceptIdMap.put(concept_name, concept_id);
			cdmUnitConceptIdToNameMap.put(concept_id, concept_name);
			if (!cdmUnitConceptNames.contains(concept_name)) {
				cdmUnitConceptNames.add(concept_name);
			}
		}
		
		Collections.sort(cdmUnitConceptNames);
		
		//for (String concept_name : cdmUnitConceptNames) {
		//	System.out.println("        " + cdmUnitNameToConceptIdMap.get(concept_name) + "," + concept_name);
		//}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getCDMForms(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM forms ...");
		
		// Get CDM Forms
		for (Row queryRow : connection.queryResource("GetCDMForms.sql", queryParameters)) {
			
			String concept_id   = queryRow.get("concept_id", true).trim();
			String concept_name = queryRow.get("concept_name", true).trim();
			
			cdmFormNameToConceptIdMap.put(concept_name, concept_id);
			cdmFormConceptIdToNameMap.put(concept_id, concept_name);
			if (!cdmFormConceptNames.contains(concept_name)) {
				cdmFormConceptNames.add(concept_name);
			}
		}
		
		Collections.sort(cdmFormConceptNames);
		
		//for (String concept_name : cdmFormConceptNames) {
		//	System.out.println("        " + cdmFormNameToConceptIdMap.get(concept_name) + "," + concept_name);
		//}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void writeIngredientsNameIndexToFile() {
		PrintWriter cdmRxNormIngredientsNameIndexFile = DrugMappingFileUtilities.openOutputFile("DrugMapping CDM RxNorm Ingredients Name Index.csv", "Name," + CDMIngredient.getHeader());
		
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
		
		DrugMappingFileUtilities.closeOutputFile(cdmRxNormIngredientsNameIndexFile);
	}
}
