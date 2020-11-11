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

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class CDM {

	private Map<String, CDMIngredient> cdmIngredients;
	
	private List<CDMIngredient> cdmIngredientsList;
	
	// Name indexes
	private MappingIngredientHitLibraryNames ingredientNames;
	private MappingIngredientHitLibraryNames ingredientNameSynonyms;
	private MappingIngredientHitLibraryNames ingredientNameRelations;
	
	private String matchString = "";
	private String nameMatchString = "";
	private String synonymMatchString = "";
	private String relationMatchString = "";
	
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
		
		// Name indexes
		ingredientNames = new MappingIngredientHitLibraryNames();
		ingredientNameSynonyms = new MappingIngredientHitLibraryNames();
		ingredientNameRelations = new MappingIngredientHitLibraryNames();
		
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
		cdmFormConceptNames = new ArrayList<String>();                                         // List of CDM form names for sorting
		
		try {
			// Connect to the database
			if (database.connect(CDM.class)) {
				// Get CDM Units
				//getCDMUnits(database, report);
				
				// Get CDM Forms
				getCDMForms(database, report);
				
				// Get CDM ingredients
				getRxNormIngredients(database, report);
				
				// Get CDM Ingredient relationships
				getRxNormIngredientRelationships(database, report);

				// Get RxNorm Clinical Drugs with Form and Ingredients
				getRxNormClinicalDrugsWithIngredients(database, report);
						
				// Get RxNorm Clinical Drugs with Form and Ingredients
				getRxNormClinicalDrugCompsWithIngredients(database, report);
						
				// Get RxNorm Clinical Drugs with Form and Ingredients
				getRxNormClinicalDrugFormsWithIngredients(database, report);

				// Get CDM RxNorm Drug ATCs
				getRxNormDrugATCs(database, report);
						
				// Get CAS code to CDM RxNorm (Extension) Ingredient mapping
				getCASToRxNormIngredientsMapping(database, report);
				
				// Close database connection
				database.disconnect();
				
				ingredientNames.reCalculateHitScores();
				ingredientNameSynonyms.reCalculateHitScores();
				ingredientNameRelations.reCalculateHitScores();
				
				ok = true;
			}
			else {
				System.out.println("ERROR: Counld not connect to the database.");
				ok = false;
			}
			
		}
		catch (Exception exception) {
			ok = false;
			exception.printStackTrace();
		}
		
		report.add("");
		
		return ok;
	}
	
	
	public Map<String, CDMIngredient> getCDMIngredients() {
		return cdmIngredients;
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
	
	
	private void getRxNormIngredients(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM RxNorm Ingredients ...");
		
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
		database.excuteQueryResource("GetRxNormIngredients.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
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
				}
				lastCdmIngredient = cdmIngredient;
			}
			
			String cdmIngredientSynonym = DrugMappingStringUtilities.safeToUpperCase(queryRow.get("concept_synonym_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim());
			
			ingredientNames.addNames(lastCdmIngredient.getConceptName(), lastCdmIngredient.getVocabularyId(), lastCdmIngredient.getConceptClassId(), "", false, lastCdmIngredient);
			ingredientNameSynonyms.addNames(cdmIngredientSynonym, lastCdmIngredient.getVocabularyId(), lastCdmIngredient.getConceptClassId(), "", true, lastCdmIngredient);
		}
		if ((rxNormIngredientsFile != null) && (lastCdmIngredient != null)) {
			rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
		}
		
		if (rxNormIngredientsFile != null) {
			rxNormIngredientsFile.close();
		}
		
		report.add("Used RxNorm Ingredients found: " + Integer.toString(cdmIngredients.size()));
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormIngredientRelationships(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM RxNorm Ingredient Relationships ...");

		database.excuteQueryResource("GetRxNormIngredientRelationships.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
			String relationshipId = queryRow.get("relationship_id", true);
			//String drugConceptId = queryRow.get("drug_concept_id", true);
			String drugConceptName = DrugMappingStringUtilities.safeToUpperCase(queryRow.get("drug_concept_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim());
			String drugVocabularyId = queryRow.get("drug_vocabulary_id", true);
			String drugConceptClassId = queryRow.get("drug_concept_class_id", true);
			String drugConceptCode = queryRow.get("drug_concept_code", true);
			String cdmIngredientConceptId = queryRow.get("ingredient_concept_id", true);
			String drugSynonymName = DrugMappingStringUtilities.safeToUpperCase(queryRow.get("drug_synonym_name", true).replaceAll("\n", " ").replaceAll("\r", " ").trim());
			
			CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
			
			if (cdmIngredient != null) {
				Map<String, Set<CDMIngredient>> atcIngredientMap = null;
				
				if (relationshipId.equals("ATC - RxNorm") && drugConceptClassId.equals("ATC 5th") && (!drugConceptCode.equals(""))) {
					atcIngredientMap = cdmATCIngredientMap;
				}
				else if (relationshipId.equals("Maps to")) {
				
					if (drugVocabularyId.equals("ATC") && drugConceptClassId.equals("ATC 5th") && (!drugConceptCode.equals(""))) {
						atcIngredientMap = cdmATCIngredientMap;
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
				
				ingredientNameRelations.addNames(drugConceptName, cdmIngredient.getVocabularyId(), drugConceptClassId, relationshipId, false, cdmIngredient);
				ingredientNameRelations.addNames(drugSynonymName, cdmIngredient.getVocabularyId(), drugConceptClassId, relationshipId, true, cdmIngredient);
			}
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormClinicalDrugsWithIngredients(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM RxNorm Clinical Drugs with ingredients ...");

		Set<CDMDrug> drugs = new HashSet<CDMDrug>();
		String lastCDMFormConceptId = "xxxxxxxx";
		int formCount = 0;
		database.excuteQueryResource("GetRxNormClinicalDrugsIngredients.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
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
			cdmDrugs.put(cdmDrug.getConceptId(), cdmDrug);
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
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormClinicalDrugCompsWithIngredients(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Comps with ingredients ...");

		database.excuteQueryResource("GetRxNormClinicalDrugCompsIngredients.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
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
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormClinicalDrugFormsWithIngredients(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM RxNorm Clinical Drug Forms with ingredients ...");
		
		Set<CDMDrug> drugForms = new HashSet<CDMDrug>();
		String lastCDMFormConceptId = "xxxxxxxx";
		int formCount = 0;
		database.excuteQueryResource("GetRxNormClinicalDrugFormsIngredients.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
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
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getRxNormDrugATCs(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM RxNorm Drug ATCs ...");

		database.excuteQueryResource("GetRxNormDrugATCs.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
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

		Integer atcDrugCount = 0;
		for (String cdmDrugConceptId : cdmDrugs.keySet()) {
			CDMDrug cdmDrug = cdmDrugs.get(cdmDrugConceptId);
			if (cdmDrug.getATCs().size() > 0) {
				atcDrugCount++;
			}
		}

		Integer atcDrugCompCount = 0;
		for (String cdmDrugConceptId : cdmDrugComps.keySet()) {
			CDMDrug cdmDrug = cdmDrugComps.get(cdmDrugConceptId);
			if (cdmDrug.getATCs().size() > 0) {
				atcDrugCompCount++;
			}
		}

		Integer atcDrugFormCount = 0;
		for (String cdmDrugConceptId : cdmDrugForms.keySet()) {
			CDMDrug cdmDrug = cdmDrugForms.get(cdmDrugConceptId);
			if (cdmDrug.getATCs().size() > 0) {
				atcDrugFormCount++;
			}
		}
		
		report.add("CDM Drugs with ATC: " + DrugMappingNumberUtilities.percentage((long) atcDrugCount, (long) cdmDrugs.size()));
		report.add("CDM Drug Comps with ATC: " + DrugMappingNumberUtilities.percentage((long) atcDrugCompCount, (long) cdmDrugComps.size()));
		report.add("CDM Drug Forms with ATC: " + DrugMappingNumberUtilities.percentage((long) atcDrugFormCount, (long) cdmDrugForms.size()));
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getCASToRxNormIngredientsMapping(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM CAS number to Ingredient mapping ...");
		
		Integer casCount = 0;
		database.excuteQueryResource("GetCASMapsToRxNormIngredients.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
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
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	@SuppressWarnings("unused")
	private void getCDMUnits(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM units ...");
		
		// Get CDM Forms
		database.excuteQueryResource("GetCDMUnits.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
			
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	private void getCDMForms(CDMDatabase database, List<String> report) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Get CDM forms ...");
		
		// Get CDM Forms
		database.excuteQueryResource("GetCDMForms.sql");
		while (database.hasNext()) {
			DelimitedFileRow queryRow = database.next();
			
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
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}


	public CDMIngredient findIngredientByName(String name, String baseContext) {
		matchString = "";

		CDMIngredient cdmIngredient = findIngredientByName(name);
		if (cdmIngredient != null) {
			matchString = baseContext + " - Ingredient Term" + nameMatchString + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "")  + " (\"" + name + "\")";
			return cdmIngredient;
		}

		cdmIngredient = findIngredientBySynonym(name);
		if (cdmIngredient != null) {
			matchString = baseContext + " - Synonym Term" + synonymMatchString + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "")  + " (\"" + name + "\")";
			return cdmIngredient;
		}

		cdmIngredient = findIngredientByRelation(name);
		if (cdmIngredient != null) {
			matchString = baseContext + " - Ingredient Relationship" + relationMatchString + (cdmIngredient.isOrphan() ? " (Orphan ingredient)" : "")  + " (\"" + name + "\")";
			return cdmIngredient;
		}
		
		return null;
	}


	public CDMIngredient findIngredientByName(String name) {
		nameMatchString = "";
		CDMIngredient cdmIngredient = ingredientNames.findName(name);
		if (cdmIngredient != null) {
			nameMatchString = ingredientNames.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNames.findStandardizedName(name);
		if (cdmIngredient != null) {
			nameMatchString = " (Standardised)" + ingredientNames.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNames.findSortedWordsName(name);
		if (cdmIngredient != null) {
			nameMatchString = " (Sorted)" + ingredientNames.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNames.findStandardizedSortedWordsName(name);
		if (cdmIngredient != null) {
			nameMatchString = " (Sorted, Standardised)" + ingredientNames.getMatchString();
			return cdmIngredient;
		}
		
		return null;
	}


	public CDMIngredient findIngredientBySynonym(String name) {
		synonymMatchString = "";
		CDMIngredient cdmIngredient = ingredientNameSynonyms.findName(name);
		if (cdmIngredient != null) {
			synonymMatchString = ingredientNameSynonyms.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNameSynonyms.findStandardizedName(name);
		if (cdmIngredient != null) {
			synonymMatchString = " (Standardised)" + ingredientNameSynonyms.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNameSynonyms.findSortedWordsName(name);
		if (cdmIngredient != null) {
			synonymMatchString = " (Sorted)" + ingredientNameSynonyms.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNameSynonyms.findStandardizedSortedWordsName(name);
		if (cdmIngredient != null) {
			synonymMatchString = " (Sorted, Standardised)" + ingredientNameSynonyms.getMatchString();
			return cdmIngredient;
		}
		
		return null;
	}
	
		
	public CDMIngredient findIngredientByRelation(String name) {
		relationMatchString = "";
		
		CDMIngredient cdmIngredient = ingredientNameRelations.findName(name);
		if (cdmIngredient != null) {
			relationMatchString = ingredientNameRelations.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNameRelations.findStandardizedName(name);
		if (cdmIngredient != null) {
			relationMatchString = " (Standardised)" + ingredientNameRelations.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNameRelations.findSortedWordsName(name);
		if (cdmIngredient != null) {
			relationMatchString = " (Sorted)" + ingredientNameRelations.getMatchString();
			return cdmIngredient;
		}

		cdmIngredient = ingredientNameRelations.findStandardizedSortedWordsName(name);
		if (cdmIngredient != null) {
			relationMatchString = " (Sorted, Standardised)" + ingredientNameRelations.getMatchString();
			return cdmIngredient;
		}
		
		return null;
	}
	
	
	public String getMatchString() {
		return matchString;
	}
	
	
	private class MappingIngredientHitLibraryNames {
		private MappingIngredientHitLibrary namesLibrary; 
		private MappingIngredientHitLibrary standardizedNamesLibrary;
		private MappingIngredientHitLibrary sortedWordsamesLibrary;
		private MappingIngredientHitLibrary standardizedSortedWordsamesLibrary;
		
		private String matchString = "";
		
		
		public MappingIngredientHitLibraryNames() {
			namesLibrary = new MappingIngredientHitLibrary();
			standardizedNamesLibrary = new MappingIngredientHitLibrary();
			sortedWordsamesLibrary = new MappingIngredientHitLibrary();
			standardizedSortedWordsamesLibrary = new MappingIngredientHitLibrary();
		}
		
		
		public void addNames(String name, String searchVocabulary, String searchConceptClass, String searchRelationship, boolean synonym, CDMIngredient cdmIngredient) {
			name = DrugMappingStringUtilities.safeToUpperCase(name);
			namesLibrary.addHit(name, searchVocabulary, searchConceptClass, searchRelationship, synonym, cdmIngredient);
			standardizedNamesLibrary.addHit(DrugMappingStringUtilities.standardizedName(name), searchVocabulary, searchConceptClass, searchRelationship, synonym, cdmIngredient);
			sortedWordsamesLibrary.addHit(DrugMappingStringUtilities.sortWords(name), searchVocabulary, searchConceptClass, searchRelationship, synonym, cdmIngredient);
			standardizedSortedWordsamesLibrary.addHit(DrugMappingStringUtilities.standardizedName(DrugMappingStringUtilities.sortWords(name)), searchVocabulary, searchConceptClass, searchRelationship, synonym, cdmIngredient);
		}
		
		
		public CDMIngredient findName(String name) {
			CDMIngredient cdmIngredient = namesLibrary.findBestTargetIngredient(name);
			if (cdmIngredient != null) {
				matchString = namesLibrary.getLastMatchString();
			}
			return cdmIngredient;
		}
		
		
		public CDMIngredient findStandardizedName(String name) {
			CDMIngredient cdmIngredient = standardizedNamesLibrary.findBestTargetIngredient(DrugMappingStringUtilities.standardizedName(name));
			if (cdmIngredient != null) {
				matchString = standardizedNamesLibrary.getLastMatchString();
			}
			return cdmIngredient;
		}
		
		
		public CDMIngredient findSortedWordsName(String name) {
			CDMIngredient cdmIngredient = sortedWordsamesLibrary.findBestTargetIngredient(DrugMappingStringUtilities.sortWords(name));
			if (cdmIngredient != null) {
				matchString = sortedWordsamesLibrary.getLastMatchString();
			}
			return cdmIngredient;
		}
		
		
		public CDMIngredient findStandardizedSortedWordsName(String name) {
			CDMIngredient cdmIngredient = standardizedSortedWordsamesLibrary.findBestTargetIngredient(DrugMappingStringUtilities.standardizedName(DrugMappingStringUtilities.sortWords(name)));
			if (cdmIngredient != null) {
				matchString = standardizedSortedWordsamesLibrary.getLastMatchString();
			}
			return cdmIngredient;
		}
		
		
		public void reCalculateHitScores() {
			namesLibrary.reCalculateHitScores();
			standardizedNamesLibrary.reCalculateHitScores();
			sortedWordsamesLibrary.reCalculateHitScores();
			standardizedSortedWordsamesLibrary.reCalculateHitScores();
		}
		
		
		public String getMatchString() {
			return matchString;
		}
	}
	
	
	private class MappingIngredientHitLibrary {
		private Map<String, MappingIngredientHits> ingredientHitsMap = new HashMap<String, MappingIngredientHits>();
		private String lastMatchString = null; // Contains last match result 
		
		
		public CDMIngredient findBestTargetIngredient(String name) {
			CDMIngredient cdmIngredient = null;
			MappingIngredientHits ingredientHits = findByName(name, false);
			if (ingredientHits != null) {
				cdmIngredient = ingredientHits.getBestTargetIngredient();
				lastMatchString = ingredientHits.getLastMatchString();
			}
			return cdmIngredient;
		}
		
		
		public void addHit(String name, String searchVocabulary, String searchConceptClass, String searchRelationship, boolean synonym, CDMIngredient cdmIngredient) {
			findByName(name, true).addHit(searchVocabulary, searchConceptClass, searchRelationship, synonym, cdmIngredient);
		}
		
		
		public MappingIngredientHits findByName(String name, boolean createWhenNotFound) {
			MappingIngredientHits ingredientHits = ingredientHitsMap.get(name);
			if ((ingredientHits == null) && createWhenNotFound) {
				ingredientHits = new MappingIngredientHits();
				ingredientHitsMap.put(name, ingredientHits);
			}
			return ingredientHits;
		}
		
		
		public void reCalculateHitScores() {
			for (String name : ingredientHitsMap.keySet()) {
				ingredientHitsMap.get(name).reCalculateHitScores();
			}
		}
		
		
		public String getLastMatchString() {
			return lastMatchString;
		}
	}
	
	
	private class MappingIngredientHits {
		private List<MappingIngredientHit> ingredientHits = new ArrayList<MappingIngredientHit>();
		private String lastMatchString = null; // Contains last match result 
		
		
		public void addHit(String searchVocabulary, String searchConceptClass, String searchRelationship, boolean synonym, CDMIngredient cdmIngredient) {
			MappingIngredientHit ingredientHit = null;
			for (MappingIngredientHit hit : ingredientHits) {
				if (
						hit.getIngredient() == cdmIngredient         &&
						hit.getSearchVocabulary().equals(searchVocabulary)       &&
						hit.getSearchConceptClass().equals(searchConceptClass)   &&
						hit.getSearchRelationship().equals(searchRelationship)   &&
						(hit.isSnonym() == synonym)
				) {
					ingredientHit = hit;
				}
			}
			if (ingredientHit == null) {
				ingredientHit = new MappingIngredientHit(searchVocabulary, searchConceptClass, searchRelationship, synonym, cdmIngredient);
				ingredientHits.add(ingredientHit);
			}
		}
		
		
		public void reCalculateHitScores() {
			for (MappingIngredientHit ingredientHit : ingredientHits) {
				ingredientHit.calculateHitScore();
			}
		}
		
		
		public CDMIngredient getBestTargetIngredient() {
			CDMIngredient result = null;
			lastMatchString = "";
			MappingIngredientHit bestIngredientHit = null;
			int sameHitScoreCount = 0;
			
			if (ingredientHits.size() == 1) {
				bestIngredientHit = ingredientHits.get(0);
			}
			else {
				int maxHitScore = -1;
				int minimumConceptId = Integer.MAX_VALUE;
				
				for (MappingIngredientHit ingredientHit : ingredientHits) {
					if (ingredientHit.getHitScore() > maxHitScore) {
						bestIngredientHit = ingredientHit;
						maxHitScore = ingredientHit.getHitScore();
						minimumConceptId = Integer.parseInt(ingredientHit.getIngredient().getConceptId());
						sameHitScoreCount = 1;
					}
					else if (ingredientHit.getHitScore() == maxHitScore) {
						sameHitScoreCount++;
						if (Integer.parseInt(ingredientHit.getIngredient().getConceptId()) < minimumConceptId) {
							minimumConceptId = Integer.parseInt(ingredientHit.getIngredient().getConceptId());
							bestIngredientHit = ingredientHit;
						}
						
					}
				}
			}
			
			if (bestIngredientHit != null) {
				result = bestIngredientHit.getIngredient();
				
				if (bestIngredientHit.isSnonym()) {
					lastMatchString += " Synonym of";
				}
				
				if (!bestIngredientHit.getSearchRelationship().equals("")) {
					lastMatchString += " \"" + bestIngredientHit.getSearchConceptClass() + "\" with relation \"" + bestIngredientHit.getSearchRelationship() + "\"";
				}
				
				if (ingredientHits.size() > 1) {
					if (ingredientHits.size() != sameHitScoreCount) {
						lastMatchString += " - Filter on best fit";
					}
					if (sameHitScoreCount > 1) {
						lastMatchString += " - Filter on lowest concept_id";
					}
				}
			}
			
			return result;
		}
		
		
		public String getLastMatchString() {
			return lastMatchString;
		}
	}
	
	
	private class MappingIngredientHit {
		private String searchVocabulary = null;
		private String searchConceptClass = null;
		private String searchRelationship = null;
		private boolean synonym = false;
		private CDMIngredient ingredient = null;
		private int hitScrore = -1;
		
		
		public MappingIngredientHit(String searchVocabulary, String searchConceptClass, String searchRelationship, boolean synonym, CDMIngredient cdmIngredient) {
			this.searchVocabulary = searchVocabulary;
			this.searchConceptClass = searchConceptClass;
			this.searchRelationship = searchRelationship;
			this.synonym = synonym;
			this.ingredient = cdmIngredient;
			
			calculateHitScore();
		}
		
		
		public String getSearchVocabulary() {
			return searchVocabulary;
		}
		
		
		public String getSearchConceptClass() {
			return searchConceptClass;
		}
		
		
		public String getSearchRelationship() {
			return searchRelationship;
		}
		
		
		public boolean isSnonym() {
			return synonym;
		}
		
		
		public CDMIngredient getIngredient() {
			return ingredient;
		}
		
		
		public int getHitScore() {
			return hitScrore;
		}
		
		
		public void calculateHitScore() {
			hitScrore = 0;
			if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_NON_ORPHAN_INGREDIENTS).equals("Yes")) {
				hitScrore += getIngredient().isOrphan() ? 100000 : 200000;
			}
			if (DrugMapping.settings.getStringSetting(MainFrame.PREFERENCE_RXNORM).equals("RxNorm")) {
				hitScrore += getSearchVocabulary().equals("RxNorm") ? 20000 : 10000;
			}
			else {
				hitScrore += getSearchVocabulary().equals("RxNorm") ? 10000 : 20000;
			}
			
			if (getSearchRelationship().equals("Concept replaced by")) {
				hitScrore += 4000;
			}
			else if (getSearchRelationship().endsWith("RxNorm eq")) {
				hitScrore += 3000;
			}
			else if (getSearchRelationship().equals("Form of")) {
				hitScrore += 2000;
			}
			else if (getSearchRelationship().equals("Maps to")) {
				hitScrore += 1000;
			}
			
			if (getSearchConceptClass().equals("Ingredient")) {
				hitScrore += 800;
			}
			else if (getSearchConceptClass().equals("Precise Ingredient")) {
				hitScrore += 700;
			}
			else if (getSearchConceptClass().equals("Substance")) {
				hitScrore += 600;
			}
			else if (getSearchConceptClass().equals("ATC 5th")) {
				hitScrore += 500;
			}
			else if (getSearchConceptClass().equals("ATC 4th")) {
				hitScrore += 400;
			}
			else if (getSearchConceptClass().equals("Pharma/Biol Product")) {
				hitScrore += 300;
			}
			else if (getSearchConceptClass().equals("11-digit NDC")) {
				hitScrore += 200;
			}
			else if (getSearchConceptClass().equals("9-digit NDC")) {
				hitScrore += 100;
			}

			hitScrore += isSnonym() ? 10 : 20;
		}
	}
}
