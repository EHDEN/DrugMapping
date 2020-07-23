package org.ohdsi.drugmapping.cdm;

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
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class CDM {

	private Map<String, CDMIngredient> cdmIngredients;
	
	private List<CDMIngredient> cdmIngredientsList;
	
	private Map<String, Set<CDMIngredient>> cdmIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmEquivalentIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymEquivalentIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmFormOfIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymFormOfIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmIngredientMapsToIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymIngredientMapsToIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmSubstanceMapsToIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymSubstanceMapsToIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmPreciseIngredientMapsToIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymPreciseIngredientMapsToIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmOtherMapsToIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymOtherMapsToIngredientNameIndex;
	
	private Map<String, Set<CDMIngredient>> cdmReplacedByIngredientNameIndex;
	private Map<String, Set<CDMIngredient>> cdmSynonymReplacedByIngredientNameIndex;
	private Map<String, CDMIngredient> cdmReplacedByIngredientConceptIdIndex;
	
	private List<String> cdmIngredientNameIndexNameList;
	private Map<String, Map<String, Set<CDMIngredient>>> cdmIngredientNameIndexMap;
	
	private Map<String, CDMDrug> cdmDrugs;
	private Map<Integer, Map<CDMIngredient, List<CDMDrug>>> cdmDrugsContainingIngredient;

	private Map<String, CDMDrug> cdmDrugComps;
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugCompsContainingIngredient;

	private Map<String, CDMDrug> cdmDrugForms;
	private Map<Integer, Map<CDMIngredient, List<CDMDrug>>> cdmDrugFormsContainingIngredient;
	
	private Map<String, CDMConcept> cdmForms;

	private Map<String, Set<CDMIngredient>> cdmATCIngredientMap;

	private Map<String, CDMIngredient> cdmCASIngredientMap;
	
	private Map<String, String> cdmUnitNameToConceptIdMap;                     // Map from CDM unit concept_name to CDM unit concept_id
	private Map<String, String> cdmUnitConceptIdToNameMap;                     // Map from CDM unit concept_id to CDM unit concept_name
	private List<String> cdmUnitConceptNames;                                  // List of CDM unit names for sorting

	private Map<String, String> cdmFormNameToConceptIdMap;                     // Map from CDM form concept_name to CDM form concept_id
	private Map<String, String> cdmFormConceptIdToNameMap;                     // Map from CDM form concept_id to CDM form concept_name
	private List<String> cdmFormConceptNames;                                  // List of CDM form names for sorting
	
	
	
	public boolean LoadCDMFromDatabase(CDMDatabase database, List<String> report) {
		boolean ok = false;

		cdmIngredients = new HashMap<String, CDMIngredient>();
		
		cdmIngredientsList = new ArrayList<CDMIngredient>();
		
		cdmIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmEquivalentIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymEquivalentIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmFormOfIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymFormOfIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmIngredientMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymIngredientMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmSubstanceMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymSubstanceMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmPreciseIngredientMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymPreciseIngredientMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmOtherMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymOtherMapsToIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		
		cdmReplacedByIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmSynonymReplacedByIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
		cdmReplacedByIngredientConceptIdIndex = new HashMap<String, CDMIngredient>();
		
		cdmIngredientNameIndexNameList = new ArrayList<String>();
		cdmIngredientNameIndexMap = new HashMap<String, Map<String, Set<CDMIngredient>>>();
		
		cdmDrugs = new HashMap<String, CDMDrug>();
		cdmDrugsContainingIngredient = new HashMap<Integer, Map<CDMIngredient, List<CDMDrug>>>();

		cdmDrugComps = new HashMap<String, CDMDrug>();
		cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();

		cdmDrugForms = new HashMap<String, CDMDrug>();
		cdmDrugFormsContainingIngredient = new HashMap<Integer, Map<CDMIngredient, List<CDMDrug>>>();
		
		cdmForms = new HashMap<String, CDMConcept>();

		cdmATCIngredientMap = new HashMap<String, Set<CDMIngredient>>();

		cdmCASIngredientMap = new HashMap<String, CDMIngredient>();
		
		cdmUnitNameToConceptIdMap = new HashMap<String, String>();                     // Map from CDM unit concept_name to CDM unit concept_id
		cdmUnitConceptIdToNameMap = new HashMap<String, String>();                     // Map from CDM unit concept_id to CDM unit concept_name
		cdmUnitConceptNames = new ArrayList<String>();                                        // List of CDM unit names for sorting

		cdmFormNameToConceptIdMap = new HashMap<String, String>();                     // Map from CDM form concept_name to CDM form concept_id
		cdmFormConceptIdToNameMap = new HashMap<String, String>();                     // Map from CDM form concept_id to CDM form concept_name
		cdmFormConceptNames = new ArrayList<String>();                                        // List of CDM form names for sorting
		
		cdmIngredientNameIndexNameList.add("Ingredient");
		cdmIngredientNameIndexMap.put(     "Ingredient", cdmIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymIngredient", cdmSynonymIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("ReplacedByIngredient");
		cdmIngredientNameIndexMap.put(     "ReplacedByIngredient", cdmReplacedByIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymReplacedByIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymReplacedByIngredient", cdmSynonymReplacedByIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("EquivalentToIngredient");
		cdmIngredientNameIndexMap.put(     "EquivalentToIngredient", cdmEquivalentIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymEquivalentToIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymEquivalentToIngredient", cdmSynonymEquivalentIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("FormOfIngredient");
		cdmIngredientNameIndexMap.put(     "FormOfIngredient", cdmFormOfIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymFormOfIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymFormOfIngredient", cdmSynonymFormOfIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("IngredientMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "IngredientMapsToIngredient", cdmIngredientMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymIngredientMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymIngredientMapsToIngredient", cdmSynonymIngredientMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("PreciseIngredientMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "PreciseIngredientMapsToIngredient", cdmPreciseIngredientMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymPreciseIngredientMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymPreciseIngredientMapsToIngredient", cdmSynonymPreciseIngredientMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SubstanceMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "SubstanceMapsToIngredient", cdmSubstanceMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymSubstanceMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymSubstanceMapsToIngredient", cdmSynonymSubstanceMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("OtherMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "OtherMapsToIngredient", cdmOtherMapsToIngredientNameIndex);
		cdmIngredientNameIndexNameList.add("SynonymOtherMapsToIngredient");
		cdmIngredientNameIndexMap.put(     "SynonymOtherMapsToIngredient", cdmSynonymOtherMapsToIngredientNameIndex);
		
		try {
			QueryParameters queryParameters = new QueryParameters();
			queryParameters.set("@vocab", database.getVocabSchema());

			// Connect to the database
			RichConnection connection = database.getRichConnection(CDM.class);
			
			// Get CDM Units
			//getCDMUnits(connection, queryParameters, report);
			
			// Get CDM Forms
			getCDMForms(connection, queryParameters, report);
			
			// Get CDM ingredients
			getRxNormIngredients(connection, queryParameters, report);
			
			// Get CDM Ingredient relationships
			getRxNormIngredientRelationships(connection, queryParameters, report);

			// Get RxNorm Clinical Drugs with Form and Ingredients
			getRxNormClinicalDrugsWithIngredients(connection, queryParameters, report);
					
			// Get RxNorm Clinical Drugs with Form and Ingredients
			getRxNormClinicalDrugCompsWithIngredients(connection, queryParameters, report);
					
			// Get RxNorm Clinical Drugs with Form and Ingredients
			getRxNormClinicalDrugFormsWithIngredients(connection, queryParameters, report);

			// Get CDM RxNorm Drug ATCs
			getRxNormDrugATCs(connection, queryParameters, report);
					
			// Get CAS code to CDM RxNorm (Extension) Ingredient mapping
			getCASToRxNormIngredientsMapping(connection, queryParameters, report);
			
			// Write Ingredients Name index to file
			writeNameIndexesToFile();
			
			// Close database connection
			connection.close();
			
			ok = true;
		}
		catch (Exception exception) {
			ok = false;
			exception.printStackTrace();
		}
		
		return ok;
	}
	
	
	public boolean isOrphanIngredient(CDMIngredient cdmIngredient) {
		boolean isOrphan = true;
		for (int ingredientCount : cdmDrugsContainingIngredient.keySet()) {
			if (cdmDrugsContainingIngredient.get(ingredientCount).get(cdmIngredient) != null) {
				isOrphan = false;
				break;
			}
		}
		if (isOrphan) {
			isOrphan = (cdmDrugCompsContainingIngredient.get(cdmIngredient) == null);
		}
		if (isOrphan) {
			for (int ingredientCount : cdmDrugFormsContainingIngredient.keySet()) {
				if (cdmDrugFormsContainingIngredient.get(ingredientCount).get(cdmIngredient) != null) {
					isOrphan = false;
					break;
				}
			}
		}
		return isOrphan;
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


	public List<CDMDrug> getCDMDrugsContainingIngredient(Integer ingredientCount, CDMIngredient ingredient) {
		Map<CDMIngredient, List<CDMDrug>> drugsWithIngredientCount = cdmDrugsContainingIngredient.get(ingredientCount);
		return drugsWithIngredientCount == null ? null : drugsWithIngredientCount.get(ingredient);
	}


	public Map<String, CDMDrug> getCDMDrugComps() {
		return cdmDrugComps;
	}


	public List<CDMDrug> getCDMDrugCompsContainingIngredient(CDMIngredient ingredient) {
		return cdmDrugCompsContainingIngredient.get(ingredient);
	}


	public Map<String, CDMDrug> getCDMDrugForms() {
		return cdmDrugForms;
	}


	public List<CDMDrug> getCDMDrugFormsContainingIngredient(Integer ingredientCount, CDMIngredient ingredient) {
		Map<CDMIngredient, List<CDMDrug>> drugsWithIngredientCount = cdmDrugFormsContainingIngredient.get(ingredientCount);
		return drugsWithIngredientCount == null ? null : drugsWithIngredientCount.get(ingredient);
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
	
	
	public CDMConcept getCDMFormConcept(String conceptId) {
		return cdmForms.get(conceptId);
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
					cdmIngredient = new CDMIngredient(this, queryRow, "");
					cdmIngredients.put(cdmIngredient.getConceptId(), cdmIngredient);
					cdmIngredientsList.add(cdmIngredient);
					
					Set<String> nameSet = getMatchingNames(cdmIngredient.getConceptName());
					
					for (String name : nameSet) {
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
			Set<String> nameSet = getMatchingNames(cdmIngredientSynonym);
			
			for (String name : nameSet) {
				Set<CDMIngredient> nameIngredients = cdmSynonymIngredientNameIndex.get(name); 
				if (nameIngredients == null) {
					nameIngredients = new HashSet<CDMIngredient>();
					cdmSynonymIngredientNameIndex.put(name, nameIngredients);
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
	
	
	private void getRxNormIngredientRelationships(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Ingredient Relationships ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormIngredientRelationships.sql", queryParameters)) {
			String relationshipId = queryRow.get("relationship_id", true);
			String drugConceptId = queryRow.get("drug_concept_id", true);
			String drugConceptName = queryRow.get("drug_concept_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			String drugVocabularyId = queryRow.get("drug_vocabulary_id", true);
			String drugConceptClassId = queryRow.get("drug_concept_class_id", true);
			String drugConceptCode = queryRow.get("drug_concept_code", true);
			String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
			String drugSynonymName = queryRow.get("drug_synonym_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			
			if (cdmIngredient != null) {
				Set<String> nameSet = getMatchingNames(drugConceptName);
				Set<String> synonymNameSet = getMatchingNames(drugSynonymName);
				
				Map<String, Set<CDMIngredient>> ingredientNameIndex = null;
				Map<String, Set<CDMIngredient>> synonymIngredientNameIndex = null;
				Map<String, Set<CDMIngredient>> atcIngredientMap = null;
				
				if (relationshipId.equals("ATC - RxNorm") && drugConceptClassId.equals("ATC 5th") && (!drugConceptCode.equals(""))) {
					atcIngredientMap = cdmATCIngredientMap;
				}
				else if (relationshipId.equals("Form of")) {
					ingredientNameIndex = cdmFormOfIngredientNameIndex;
					synonymIngredientNameIndex = cdmSynonymFormOfIngredientNameIndex;
				}
				else if (relationshipId.equals("Concept replaced by")) {
					ingredientNameIndex = cdmReplacedByIngredientNameIndex;
					synonymIngredientNameIndex = cdmSynonymReplacedByIngredientNameIndex;
					if ((drugConceptId != null) && (!drugConceptId.equals(""))) {
						cdmReplacedByIngredientConceptIdIndex.put(drugConceptId, cdmIngredient);
					}
				}
				else if (relationshipId.equals("Maps to")) {
	
					ingredientNameIndex = cdmOtherMapsToIngredientNameIndex;
					synonymIngredientNameIndex = cdmSynonymOtherMapsToIngredientNameIndex;
					if (drugConceptClassId.equals("Ingredient")) {
						ingredientNameIndex = cdmIngredientMapsToIngredientNameIndex;
						synonymIngredientNameIndex = cdmSynonymIngredientMapsToIngredientNameIndex;
					}
					else if (drugConceptClassId.equals("Precise Ingredient")) {
						ingredientNameIndex = cdmPreciseIngredientMapsToIngredientNameIndex;
						synonymIngredientNameIndex = cdmSynonymPreciseIngredientMapsToIngredientNameIndex;
					}
					else if (drugConceptClassId.equals("Substance")) {
						ingredientNameIndex = cdmSubstanceMapsToIngredientNameIndex;
						synonymIngredientNameIndex = cdmSynonymSubstanceMapsToIngredientNameIndex;
					}
				
					if (drugVocabularyId.equals("ATC") && drugConceptClassId.equals("ATC 5th") && (!drugConceptCode.equals(""))) {
						atcIngredientMap = cdmATCIngredientMap;
					}
				}
				else if (relationshipId.endsWith(" - RxNorm eq")) {
					ingredientNameIndex = cdmEquivalentIngredientNameIndex;
					synonymIngredientNameIndex = cdmSynonymEquivalentIngredientNameIndex;
				}
				
				if (ingredientNameIndex != null) {
					for (String name : nameSet) {
						Set<CDMIngredient> nameIngredients = ingredientNameIndex.get(name); 
						if (nameIngredients == null) {
							nameIngredients = new HashSet<CDMIngredient>();
							ingredientNameIndex.put(name, nameIngredients);
						}
						nameIngredients.add(cdmIngredient);
					}
				}
				
				if (synonymIngredientNameIndex != null) {
					for (String name : synonymNameSet) {
						Set<CDMIngredient> nameIngredients = synonymIngredientNameIndex.get(name); 
						if (nameIngredients == null) {
							nameIngredients = new HashSet<CDMIngredient>();
							synonymIngredientNameIndex.put(name, nameIngredients);
						}
						nameIngredients.add(cdmIngredient);
					}
				}
				
				if ((drugConceptCode != null) && (!drugConceptCode.equals("")) && (atcIngredientMap != null)) {
					String ingredientATC = cdmIngredient.getATC();
					if ((ingredientATC != null) && (!ingredientATC.equals(""))) {
						if (!drugConceptCode.equals(ingredientATC)) {
							cdmIngredient.setATC(null);
						}
					}
					else {
						cdmIngredient.setATC(drugConceptCode);
					}
					Set<CDMIngredient> cdmATCIngredients = atcIngredientMap.get(drugConceptCode);
					if (cdmATCIngredients == null) {
						cdmATCIngredients = new HashSet<CDMIngredient>();
						atcIngredientMap.put(drugConceptCode, cdmATCIngredients);
					}
					cdmATCIngredients.add(cdmIngredient);
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormClinicalDrugsWithIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drugs with ingredients ...");

		Set<CDMDrug> drugs = new HashSet<CDMDrug>();
		String lastCDMFormConceptId = "xxxxxxxx";
		int formCount = 0;
		for (Row queryRow : connection.queryResource("GetRxNormClinicalDrugsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drug_concept_id", true);
			String cdmFormConceptId = queryRow.get("form_concept_id", true);
			String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
			
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrugForm = cdmDrugForms.get(cdmDrugConceptId);
				if (cdmDrugForm == null) {
					cdmDrugForm = new CDMDrug(this, queryRow, "drug_");
					cdmDrugForms.put(cdmDrugForm.getConceptId(), cdmDrugForm);
					drugs.add(cdmDrugForm);
					lastCDMFormConceptId = "xxxxxxxx";
					formCount = 0;
				}
				if (!cdmFormConceptId.equals(lastCDMFormConceptId)) {
					if ((cdmFormConceptId != null) && (!cdmFormConceptId.equals(""))) {
						cdmDrugForm.addForm(cdmFormConceptId);
					}
					formCount++;
				}
				if (formCount == 1) {
					if ((cdmIngredientConceptId != null) && (!cdmIngredientConceptId.equals(""))) {
						CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
						//if (cdmIngredient != null) {
							CDMIngredientStrength cdmIngredientStrength = new CDMIngredientStrength(this, queryRow, "", cdmIngredient);
							cdmDrugForm.addIngredientStrength(cdmIngredientStrength);
						//}
					}
				}
				lastCDMFormConceptId = cdmFormConceptId;
			} 
		}
		
		// Build map of drugs containing ingredients
		for (CDMDrug cdmDrug : drugs) {
			int ingredientCount = cdmDrug.getIngredients().size();
			Map<CDMIngredient, List<CDMDrug>> cdmDrugsEqualIngredientCount = cdmDrugsContainingIngredient.get(ingredientCount);
			if (cdmDrugsEqualIngredientCount == null) {
				cdmDrugsEqualIngredientCount = new HashMap<CDMIngredient, List<CDMDrug>>();
				cdmDrugsContainingIngredient.put(ingredientCount, cdmDrugsEqualIngredientCount);
			}
			for (CDMIngredient cdmIngredient : cdmDrug.getIngredients()) {
				List<CDMDrug> drugsContaingIngredient = cdmDrugsEqualIngredientCount.get(cdmIngredient);
				if (drugsContaingIngredient == null) {
					drugsContaingIngredient = new ArrayList<CDMDrug>();
					cdmDrugsEqualIngredientCount.put(cdmIngredient, drugsContaingIngredient);
				}
				drugsContaingIngredient.add(cdmDrug);
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
					cdmDrugComp = new CDMDrug(this, queryRow, "drugcomp_");
					cdmDrugComps.put(cdmDrugComp.getConceptId(), cdmDrugComp);
				}
				
				String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
				if ((cdmIngredientConceptId != null) && (!cdmIngredientConceptId.equals(""))) {
					CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
					if (cdmIngredient != null) {
						CDMIngredientStrength cdmIngredientStrength = new CDMIngredientStrength(this, queryRow, "", cdmIngredient);
						cdmDrugComp.addIngredientStrength(cdmIngredientStrength);
						List<CDMDrug> drugCompsContainingIngredient = cdmDrugCompsContainingIngredient.get(cdmIngredient);
						if (drugCompsContainingIngredient == null) {
							drugCompsContainingIngredient = new ArrayList<CDMDrug>();
							cdmDrugCompsContainingIngredient.put(cdmIngredient, drugCompsContainingIngredient);
						}
						if (!drugCompsContainingIngredient.contains(cdmDrugComp)) {
							drugCompsContainingIngredient.add(cdmDrugComp);
						}
					}
				}
			}
		}

		report.add("RxNorm Clinical Drug Comps found: " + Integer.toString(cdmDrugComps.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormClinicalDrugFormsWithIngredients(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Forms with ingredients ...");
		
		Set<CDMDrug> drugForms = new HashSet<CDMDrug>();
		String lastCDMFormConceptId = "xxxxxxxx";
		int formCount = 0;
		for (Row queryRow : connection.queryResource("GetRxNormClinicalDrugFormsIngredients.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("drugform_concept_id", true);
			String cdmFormConceptId = queryRow.get("form_concept_id", true);
			String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
			
			if ((cdmDrugConceptId != null) && (!cdmDrugConceptId.equals(""))) {
				CDMDrug cdmDrugForm = cdmDrugForms.get(cdmDrugConceptId);
				if (cdmDrugForm == null) {
					cdmDrugForm = new CDMDrug(this, queryRow, "drugform_");
					cdmDrugForms.put(cdmDrugForm.getConceptId(), cdmDrugForm);
					drugForms.add(cdmDrugForm);
					lastCDMFormConceptId = "xxxxxxxx";
					formCount = 0;
				}
				if (!cdmFormConceptId.equals(lastCDMFormConceptId)) {
					if ((cdmFormConceptId != null) && (!cdmFormConceptId.equals(""))) {
						cdmDrugForm.addForm(cdmFormConceptId);
					}
					formCount++;
				}
				if (formCount == 1) {
					if ((cdmIngredientConceptId != null) && (!cdmIngredientConceptId.equals(""))) {
						CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
						if (cdmIngredient != null) {
							CDMIngredientStrength cdmIngredientStrength = new CDMIngredientStrength(this, queryRow, "", cdmIngredient);
							cdmDrugForm.addIngredientStrength(cdmIngredientStrength);
						}
					}
				}
				lastCDMFormConceptId = cdmFormConceptId;
			} 
		}
		
		// Build map of drug forms containing ingredients
		for (CDMDrug cdmDrugForm : drugForms) {
			int ingredientCount = cdmDrugForm.getIngredients().size();
			Map<CDMIngredient, List<CDMDrug>> cdmDrugFormsEqualIngredientCount = cdmDrugFormsContainingIngredient.get(ingredientCount);
			if (cdmDrugFormsEqualIngredientCount == null) {
				cdmDrugFormsEqualIngredientCount = new HashMap<CDMIngredient, List<CDMDrug>>();
				cdmDrugFormsContainingIngredient.put(ingredientCount, cdmDrugFormsEqualIngredientCount);
			}
			for (CDMIngredient cdmIngredient : cdmDrugForm.getIngredients()) {
				List<CDMDrug> drugFormsContaingIngredient = cdmDrugFormsEqualIngredientCount.get(cdmIngredient);
				if (drugFormsContaingIngredient == null) {
					drugFormsContaingIngredient = new ArrayList<CDMDrug>();
					cdmDrugFormsEqualIngredientCount.put(cdmIngredient, drugFormsContaingIngredient);
				}
				drugFormsContaingIngredient.add(cdmDrugForm);
			}
		}

		report.add("RxNorm Clinical Drug Forms found: " + Integer.toString(cdmDrugForms.size()));
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormDrugATCs(RichConnection connection, QueryParameters queryParameters, List<String> report) {
		System.out.println(DrugMapping.getCurrentTime() + "     Get CDM RxNorm Drug ATCs ...");
		
		for (Row queryRow : connection.queryResource("GetRxNormDrugATCs.sql", queryParameters)) {
			String cdmDrugConceptId = queryRow.get("concept_id", true);
			String cdmDrugATC = queryRow.get("atc", true);
			
			CDMDrug cdmDrug = cdmDrugs.get(cdmDrugConceptId);
			if (cdmDrug == null) {
				cdmDrug = cdmDrugComps.get(cdmDrugConceptId);
			}
			if (cdmDrug == null) {
				cdmDrug = cdmDrugForms.get(cdmDrugConceptId);
			}
			if (cdmDrug != null) {
				cdmDrug.addATC(cdmDrugATC);
			}
		}

		Integer atcCount = 0;
		for (String cdmDrugConceptId : cdmDrugs.keySet()) {
			CDMDrug cdmDrug = cdmDrugs.get(cdmDrugConceptId);
			if (cdmDrug.getATCs().size() > 0) {
				atcCount++;
			}
		}
		
		report.add("CDM Drugs with ATC: " + DrugMappingNumberUtilities.percentage((long) atcCount, (long) cdmDrugs.size()));
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
	
	
	@SuppressWarnings("unused")
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
			
			CDMConcept formConcept = new CDMConcept(this, queryRow, "");
			
			cdmForms.put(formConcept.getConceptId(), formConcept);
			cdmFormNameToConceptIdMap.put(formConcept.getConceptName(), formConcept.getConceptId());
			cdmFormConceptIdToNameMap.put(formConcept.getConceptId(), formConcept.getConceptName());
			if (!cdmFormConceptNames.contains(formConcept.getConceptName())) {
				cdmFormConceptNames.add(formConcept.getConceptName());
			}
		}
		
		Collections.sort(cdmFormConceptNames);
		
		//for (String concept_name : cdmFormConceptNames) {
		//	System.out.println("        " + cdmFormNameToConceptIdMap.get(concept_name) + "," + concept_name);
		//}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}

	
	private Set<String> getMatchingNames(String name) {
		Set<String> nameSet = new HashSet<String>();
		
		nameSet.add(name);
		nameSet.add(DrugMappingStringUtilities.modifyName(name));
		nameSet.add(DrugMappingStringUtilities.sortWords(name));
		nameSet.add(DrugMappingStringUtilities.modifyName(DrugMappingStringUtilities.sortWords(name)));
		
		return nameSet;
	}
	
	
	private void writeNameIndexesToFile() {
		PrintWriter cdmRxNormIngredientsNameIndexFile = DrugMappingFileUtilities.openOutputFile("DrugMapping CDM RxNorm Ingredients Name Index.csv", "Index,Name," + CDMIngredient.getHeader());
		
		for (String indexName : cdmIngredientNameIndexNameList) {
			Map<String, Set<CDMIngredient>> index = cdmIngredientNameIndexMap.get(indexName);
			List<String> names = new ArrayList<String>();
			names.addAll(index.keySet());
			Collections.sort(names);
			
			for (String name : names) {
				List<CDMIngredient> cdmIngredients = new ArrayList<CDMIngredient>();
				cdmIngredients.addAll(index.get(name));
				Collections.sort(cdmIngredients, new Comparator<CDMIngredient>() {

					@Override
					public int compare(CDMIngredient cdmIngredient1, CDMIngredient cdmIngredient2) {
						
						return Integer.compare(Integer.valueOf(cdmIngredient1.getConceptId()), Integer.valueOf(cdmIngredient2.getConceptId()));
					}
				});
				for (CDMIngredient cdmIngredient : cdmIngredients) {
					String record = indexName;
					record += "," + DrugMappingStringUtilities.escapeFieldValue(name);
					record += "," + cdmIngredient.toString();
					cdmRxNormIngredientsNameIndexFile.println(record);
				}
			}
		}
		
		DrugMappingFileUtilities.closeOutputFile(cdmRxNormIngredientsNameIndexFile);
	}
}
