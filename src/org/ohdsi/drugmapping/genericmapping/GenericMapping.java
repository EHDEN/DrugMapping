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
	private Map<SourceDrug, CDMDrug> drugMappingClinicalDrugComp = new HashMap<SourceDrug, CDMDrug>();
	private Map<SourceDrug, CDMDrug> drugMappingClinicalDrugForm = new HashMap<SourceDrug, CDMDrug>();
		
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile casFile) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");
		SourceDrug.init();
		
		// Load source drug ingredient mapping
		ok = ok && getSourceDrugs(sourceDrugsFile) && (!SourceDrug.errorOccurred());
		
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
		
		// Create the final report
		finalReport();
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
	
	
	private boolean getSourceDrugs(InputFile sourceDrugsFile) {
		boolean sourceDrugError = false;
		String fileName = "";
		Integer noATCCounter = 0;
		
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
							if (missingATCFile != null) {
								sourceDrug.writeDescriptionToFile("", missingATCFile);
							}
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
			
			if (missingATCFile != null) {
				missingATCFile.close();
			}
			
			System.out.println("         Found " + Integer.toString(sourceDrugs.size()) + " source drugs");
			System.out.println("         Found " + noATCCounter + " source drugs without ATC (" + Long.toString(Math.round(((double) noATCCounter / (double) sourceDrugs.size()) * 100)) + "%)");
			System.out.println("         Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	private boolean getCDMData(CDMDatabase database) {
		boolean ok = true;
		String fileName = "";
		
		QueryParameters queryParameters = new QueryParameters();
		queryParameters.set("@vocab", database.getVocabSchema());
	
		// Connect to the database
		RichConnection connection = database.getRichConnection(this.getClass());
		
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
		
		formConversionsMap = new FormConversion(database, forms);
		if (formConversionsMap.getStatus() == FormConversion.STATE_EMPTY) {
			// If no form conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the form conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getCurrentPath() + "/" + FormConversion.FILENAME);
			ok = false;
		}
		
		if (ok) {
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
						Set<CDMIngredient> existingCDMIngredients = cdmIngredientNameIndex.get(cdmIngredientNameNoSpaces);
						if (existingCDMIngredients == null) {
							existingCDMIngredients = new HashSet<CDMIngredient>();
							cdmIngredientNameIndex.put(cdmIngredientNameNoSpaces, existingCDMIngredients);
						}
						existingCDMIngredients.add(cdmIngredient);
						if (existingCDMIngredients.size() > 1) {
							System.out.println("      WARNING: Multiple ingredients found for name '" + cdmIngredient.getConceptName() + "'");
							//ok = false;
						}
					}
					lastCdmIngredient = cdmIngredient;
				}
				String cdmIngredientSynonym = queryRow.get("concept_synonym_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
				String cdmIngredientSynonymNoSpaces = cdmIngredientSynonym.replaceAll(" ", "").replaceAll("-", "");
				Set<CDMIngredient> existingCDMIngredients = cdmIngredientNameIndex.get(cdmIngredientSynonymNoSpaces);
				if (existingCDMIngredients == null) {
					existingCDMIngredients = new HashSet<CDMIngredient>();
					cdmIngredientNameIndex.put(cdmIngredientSynonymNoSpaces, existingCDMIngredients);
				}
				existingCDMIngredients.add(lastCdmIngredient);
				
				if ((cdmIngredientSynonym != null) && (!cdmIngredientSynonym.equals(""))) {
					lastCdmIngredient.addSynonym(cdmIngredientSynonym);
				}
				String ingredientName = lastCdmIngredient.getConceptName();
				while (ingredientName.contains("F")) {
					ingredientName = ingredientName.replaceFirst("F", "PH");
					lastCdmIngredient.addSynonym(ingredientName);
					String ingredientNameNoSpaces = cdmIngredientSynonym.replaceAll(" ", "").replaceAll("-", "");
					existingCDMIngredients = cdmIngredientNameIndex.get(ingredientNameNoSpaces);
					if (existingCDMIngredients == null) {
						existingCDMIngredients = new HashSet<CDMIngredient>();
						cdmIngredientNameIndex.put(ingredientNameNoSpaces, existingCDMIngredients);
					}
					existingCDMIngredients.add(lastCdmIngredient);
				}
			}
			if ((rxNormIngredientsFile != null) && (lastCdmIngredient != null)) {
				rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
			}
			
			if (rxNormIngredientsFile != null) {
				rxNormIngredientsFile.close();
			}
			
			System.out.println("         Found " + Integer.toString(cdmIngredients.size()) + " CDM Ingredients");
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
			
			// Get "Maps to" RxNorm Ingredients
			System.out.println(DrugMapping.getCurrentTime() + "     Get CDM 'Maps to' RxNorm Ingredients ...");
			
			PrintWriter mapsToRxNormIngredientsFile = null;
			try {
				// Create output file
				fileName = DrugMapping.getCurrentPath() + "/DrugMapping Maps To RxNorm Ingredients.csv";
				mapsToRxNormIngredientsFile = new PrintWriter(new File(fileName));
				mapsToRxNormIngredientsFile.println("SourceName,Synonym," + CDMIngredient.getHeader());
			}
			catch (FileNotFoundException e) {
				System.out.println("      ERROR: Cannot create output file '" + fileName + "'");
				mapsToRxNormIngredientsFile = null;
			}
			
			for (Row queryRow : connection.queryResource("../cdm/GetMapsToRxNormIngredients.sql", queryParameters)) {
				String drugName = queryRow.get("drug_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
				String drugNameNoSpaces = drugName.replaceAll(" ", "").replaceAll("-", "");
				String cdmIngredientConceptId = queryRow.get("mapsto_concept_id").trim();
				String drugNameSynonym = queryRow.get("drug_synonym_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
				String drugNameSynonymNoSpaces = drugNameSynonym.replaceAll(" ", "").replaceAll("-", "");
				CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
				
				Set<CDMIngredient> drugNameIngredients = cdmIngredientNameIndex.get(drugNameNoSpaces); 
				if (drugNameIngredients == null) {
					drugNameIngredients = new HashSet<CDMIngredient>();
					cdmIngredientNameIndex.put(drugNameNoSpaces, drugNameIngredients);
				}
				drugNameIngredients.add(cdmIngredient);
				if (drugNameIngredients.size() > 1) {
					System.out.println("      WARNING: Multiple ingredients found for name '" + drugName + "'");
					//ok = false;
				}
				
				Set<CDMIngredient> synonymIngredients = cdmIngredientNameIndex.get(drugNameSynonymNoSpaces); 
				if (synonymIngredients == null) {
					synonymIngredients = new HashSet<CDMIngredient>();
					cdmIngredientNameIndex.put(drugNameSynonymNoSpaces, synonymIngredients);
				}
				synonymIngredients.add(cdmIngredient);
				if (synonymIngredients.size() > 1) {
					System.out.println("      WARNING: Multiple ingredients found for synonym '" + drugNameSynonym + "'");
					//ok = false;
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
				
				if (mapsToRxNormIngredientsFile != null) {
					for (CDMIngredient cdmIngredient : cdmIngredients) {
						for (String synonym : synonyms) {
							String record = "\"" + drugName + "\"";
							record += "," + "\"" + synonym + "\"";
							record += "," + cdmIngredient.toString();
							mapsToRxNormIngredientsFile.println(record);
						}
					}
					mapsToRxNormIngredientsFile.close();
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
			
			
			// Get RxNorm Clinical Drugs with Form and Ingredients
			System.out.println(DrugMapping.getCurrentTime() + "     Get CDM concepts replaced by RxNorm ingredients ...");
			
			for (Row queryRow : connection.queryResource("../cdm/GetConceptReplacedByRxNormIngredient.sql", queryParameters)) {
				String cdmReplacedName        = queryRow.get("concept_name").replaceAll("\n", " ").replaceAll("\r", " ").trim().toUpperCase();
				String cdmReplacedByConceptId = queryRow.get("concept_id");
				
				String cdmReplacedNameNoSpaces = cdmReplacedName.replaceAll(" ", "").replaceAll("-", "");
				
				CDMIngredient cdmIngredient = cdmIngredients.get(cdmReplacedByConceptId);
				if (cdmIngredient != null) {
					Set<CDMIngredient> cdmIngredients = cdmIngredientNameIndex.get(cdmReplacedNameNoSpaces);
					if (cdmIngredients == null) {
						cdmIngredients = new HashSet<CDMIngredient>();
						cdmIngredientNameIndex.put(cdmReplacedNameNoSpaces, cdmIngredients);
					}
					cdmIngredients.add(cdmIngredient);
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

			System.out.println(DrugMapping.getCurrentTime() + "     Done");
			
			
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
			
			System.out.println("         Found " + atcCount + " CDM Ingredients with ATC (" + Long.toString(Math.round(((double) atcCount / (double) cdmIngredients.size()) * 100)) + "%)");
			System.out.println(DrugMapping.getCurrentTime() + "     Done");
		}
		
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
						casNumber = casNumber.replaceAll(" ", "").replaceAll("-", "");
						if (!casNumber.equals("")) {
							casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
						}
						Set<String> casNames = new HashSet<String>();
						String[] synonymSplit = synonyms.split("|");
						for (String synonym : synonymSplit) {
							String synonymNoSpaces = synonym.trim().replaceAll(" ", "").replaceAll("-", "");
							if (!synonymNoSpaces.equals("")) {
								casNames.add(synonymNoSpaces);
							}
						}
						String chemicalNameNoSpaces = chemicalName.replaceAll(" ", "").replaceAll("-", ""); 
						if (!chemicalNameNoSpaces.equals("")) {
							casNames.add(chemicalNameNoSpaces);
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
		Integer matchedByATC = 0;
		Integer matchedByFullName = 0;
		Integer matchedByCASName = 0;
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by ATC ...");
		
		for (SourceDrug sourceDrug : sourceDrugs) {
			List<SourceIngredient> sourceIngredients = sourceDrug.getIngredients();
			if ((sourceDrug.getATCCode() != null) && (sourceIngredients.size() == 1)) {
				SourceIngredient sourceIngredient = sourceIngredients.get(0);
				if (sourceIngredient.getMatchingDrug() == null) {
					Set<CDMIngredient> cdmATCIngredients = cdmATCIngredientMap.get(sourceDrug.getATCCode());
					if ((cdmATCIngredients != null) && (cdmATCIngredients.size() == 1)) {
						CDMIngredient cdmIngredient = (CDMIngredient) cdmATCIngredients.toArray()[0];
						ingredientMap.put(sourceIngredient, cdmIngredient);
						sourceIngredient.setMatchingDrug(cdmIngredient.getConceptId());
						sourceIngredient.setMatchString(sourceDrug.getATCCode());
						matchedByATC++;
					}
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by full name ...");
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			if (sourceIngredient.getMatchingDrug() == null) {
				boolean multipleMapping = false;
				Set<CDMIngredient> matchedCDMIngredients = cdmIngredientNameIndex.get(sourceIngredient.getIngredientNameEnglishNoSpaces());
				if (matchedCDMIngredients != null) {
					if (matchedCDMIngredients.size() == 1) {
						CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
						ingredientMap.put(sourceIngredient, cdmIngredient);
						sourceIngredient.setMatchingDrug(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
						sourceIngredient.setMatchString(sourceIngredient.getIngredientNameEnglishNoSpaces());
						matchedByFullName++;
					}
					else {
						multipleMapping = true;
					}
				}
				if (ingredientMap.get(sourceIngredient) == null) {
					String casNumber = sourceIngredient.getCASNumber();
					if (casNumber != null) {
						Set<String> casNamesNoSpaces = casMap.get(casNumber);
						if (casNamesNoSpaces != null) {
							matchedCDMIngredients = new HashSet<CDMIngredient>();
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
								sourceIngredient.setMatchingDrug(cdmIngredient.getConceptId());
								sourceIngredient.setMatchString("CAS: " + matchingCASnameNoSpaces);
								matchedByCASName++;
							}
							else {
								multipleMapping = true;
							}
						}
					}
				}
				if (multipleMapping) {
					multipleMappings++;
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");

		
		System.out.println(DrugMapping.getCurrentTime() + "     Creating output files ...");

		try {
			// Create match ingredients file output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Match Ingredients.csv";
			PrintWriter matchIngredientsFile = new PrintWriter(new File(fileName));
			matchIngredientsFile.println(SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
			
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				if (sourceIngredient.getMatchingDrug() != null) {
					matchIngredientsFile.println(sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatchingDrug()));
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
					matchSourceDrugIngredientsFile.println(sourceDrug + "," + sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatchingDrug()));
				}
			}
			
			matchSourceDrugIngredientsFile.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("       WARNING: Cannot create output file '" + fileName + "'");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		
		System.out.println();
		System.out.println("Source ingredients mapped by ATC: " + matchedByATC + " (" + Long.toString(Math.round(((double) matchedByATC / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println("Source ingredients mapped by full name: " + matchedByFullName + " (" + Long.toString(Math.round(((double) matchedByFullName / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println("Source ingredients mapped by CAS: " + matchedByCASName + " (" + Long.toString(Math.round(((double) matchedByCASName / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println("Source ingredients mapped total : " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		System.out.println("Multiple mappings found: " + multipleMappings + " (" + Long.toString(Math.round(((double) multipleMappings / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
		
		return ok;
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
		
		System.out.println();
		System.out.println("Source drugs with all ingredients mapped: " + Integer.toString(sourceDrugsAllIngredientsMapped.size()) + " (" + Long.toString(Math.round(((double) sourceDrugsAllIngredientsMapped.size() / (double) sourceDrugs.size()) * 100)) + "%)");
		
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
					drugMappingClinicalDrug.put(sourceDrug, (CDMDrug) matchingCDMDrugs.toArray()[0]); 
				}
				else if (matchingCDMDrugs.size() > 1) {
					multipleDrugMappings++;
					if (multipleClinicalDrugsMappingFile != null) {
						for (CDMDrug cdmDrug : matchingCDMDrugs) {
							for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
								multipleClinicalDrugsMappingFile.println(sourceDrug + "," + sourceDrug.getComponents().get(ingredientNr) + "," + cdmDrug + "," + cdmDrug.getIngredients().get(ingredientNr).toStringLong());
							}
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
								record += "," + (ingredientNr < cdmDrug.getIngredients().size() ? cdmDrug.getIngredients().get(ingredientNr).toStringLong() : CDMIngredientStrength.emptyRecordLong());
								noClinicalDrugsMappingFile.println(record);
							}
						}
					}
				}
			}
		}
		
		if (multipleClinicalDrugsMappingFile != null) {
			multipleClinicalDrugsMappingFile.close();
		}
		
		if (noClinicalDrugsMappingFile != null) {
			noClinicalDrugsMappingFile.close();
		}
		
		System.out.println("Source drugs mapped to multiple CDM Clinical Drugs: " + multipleDrugMappings);
		
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
					Set<CDMDrug> matchingCDMDrugs = new HashSet<CDMDrug>();
					for (CDMDrug cdmDrug : cdmDrugCompsWithIngredients) {
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
						drugMappingClinicalDrugComp.put(sourceDrug, (CDMDrug) matchingCDMDrugs.toArray()[0]); 
					}
					else if (matchingCDMDrugs.size() > 1) {
						multipleDrugMappings++;
						if (multipleClinicalDrugCompsMappingFile != null) {
							for (CDMDrug cdmDrug : matchingCDMDrugs) {
								for (int ingredientNr = 0; ingredientNr < sourceDrug.getComponents().size(); ingredientNr++) {
									multipleClinicalDrugCompsMappingFile.println(sourceDrug + "," + sourceDrug.getComponents().get(ingredientNr) + "," + cdmDrug + "," + cdmDrug.getIngredients().get(ingredientNr).toStringLong());
								}
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
									record += "," + (ingredientNr < cdmDrug.getIngredients().size() ? cdmDrug.getIngredients().get(ingredientNr).toStringLong() : CDMIngredientStrength.emptyRecordLong());
									noClinicalDrugCompsMappingFile.println(record);
								}
							}
						}
					}
				}
			}
		}
		
		if (multipleClinicalDrugCompsMappingFile != null) {
			multipleClinicalDrugCompsMappingFile.close();
		}
		
		if (noClinicalDrugCompsMappingFile != null) {
			noClinicalDrugCompsMappingFile.close();
		}
		
		System.out.println();
		System.out.println("Source drugs mapped to multiple CDM Clinical Drug Comps: " + multipleDrugMappings);
		
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
							}
						}
						cdmDrugsWithIngredients.removeAll(cdmDrugsMissingForm);
						if (cdmDrugsWithIngredients.size() == 0) {
							break;
						}
					}
				}
				

				if (cdmDrugsWithIngredients.size() == 1) {
					drugMappingClinicalDrugForm.put(sourceDrug, (CDMDrug) cdmDrugsWithIngredients.toArray()[0]);
				}
				else if (cdmDrugsWithIngredients.size() > 1) {
					multipleDrugMappings++;
					if (multipleClinicalDrugFormsMappingFile != null) {
						for (CDMDrug cdmDrug : cdmDrugsWithIngredients) {
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
		}
		
		if (multipleClinicalDrugFormsMappingFile != null) {
			multipleClinicalDrugFormsMappingFile.close();
		}
		
		if (noClinicalDrugFormsMappingFile != null) {
			noClinicalDrugFormsMappingFile.close();
		}
		
		System.out.println("Source drugs mapped to multiple CDM Clinical Drug Forms: " + multipleDrugMappings);
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return ok;
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
		
		System.out.println();
		System.out.println("Source drugs mapped to single CDM Clinical Drug: " + Integer.toString(drugMappingClinicalDrug.size()));
		System.out.println("Source drugs mapped to single CDM Clinical Drug Comp: " + Integer.toString(drugMappingClinicalDrugComp.size()));
		System.out.println("Source drugs mapped to single CDM Clinical Drug Form: " + Integer.toString(drugMappingClinicalDrugForm.size()));
		
		if (dataCountTotal != 0) {
			System.out.println();
			System.out.println("Datacoverage CDM Clinical Drug mapping: " + dataCoverageClinicalDrugs + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugs / (double) dataCountTotal) * 100)) + "%)");
			System.out.println("Datacoverage CDM Clinical Drug Comp mapping: " + dataCoverageClinicalDrugComps + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugComps / (double) dataCountTotal) * 100)) + "%)");
			System.out.println("Datacoverage CDM Clinical Drug Form mapping: " + dataCoverageClinicalDrugForms + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) dataCoverageClinicalDrugForms / (double) dataCountTotal) * 100)) + "%)");
			System.out.println("Total datacoverage drug mapping: " + (dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms) + " of " + dataCountTotal + " (" + Long.toString(Math.round(((double) (dataCoverageClinicalDrugs + dataCoverageClinicalDrugComps + dataCoverageClinicalDrugForms) / (double) dataCountTotal) * 100)) + "%)");
		}
		else {
			System.out.println();
			System.out.println("No datacoverage counts available.");
		}
		
	}
	
	
	/*
	public static void main(String[] args) {
	}
	*/
}
