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

	private Map<String, Set<String>> casMap = new HashMap<String, Set<String>>();
	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<String, Set<CDMIngredient>> cdmIngredientNameIndex = new HashMap<String, Set<CDMIngredient>>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<String, Set<CDMIngredient>> mapsToCDMIngredient = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<String>> drugNameSynonyms = new HashMap<String, Set<String>>();
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	
	private int noATCCounter = 0; // Counter of source drugs without ATC code.
	
	
	public GenericMapping(CDMDatabase database, InputFile sourceDrugsFile, InputFile casFile) {
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + " Generic Drug Mapping");
		SourceDrug.init();
		
		// Get CDM Ingredients
		ok = getCDMIngredients(database);		
		
		// Load CAS names
		ok = ok && getCASNames(casFile);
		
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
						String cdmIngredientNameNoSpaces = cdmIngredient.getConceptNameNoSpaces();
						Set<CDMIngredient> existingCDMIngredients = cdmIngredientNameIndex.get(cdmIngredientNameNoSpaces);
						if (existingCDMIngredients == null) {
							existingCDMIngredients = new HashSet<CDMIngredient>();
							cdmIngredientNameIndex.put(cdmIngredientNameNoSpaces, existingCDMIngredients);
						}
						existingCDMIngredients.add(cdmIngredient);
						if (existingCDMIngredients.size() > 1) {
							System.out.println("      WARNING: Multiple ingredients found for name '" + cdmIngredient.getConceptName() + "'!");
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
			if (lastCdmIngredient != null) {
				rxNormIngredientsFile.println(lastCdmIngredient.toStringWithSynonyms());
			}
			
			// Get "Maps to" RxNorm Ingredients
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
					System.out.println("      WARNING: Multiple ingredients found for name '" + drugName + "'!");
					//ok = false;
				}
				
				Set<CDMIngredient> synonymIngredients = cdmIngredientNameIndex.get(drugNameSynonymNoSpaces); 
				if (synonymIngredients == null) {
					synonymIngredients = new HashSet<CDMIngredient>();
					cdmIngredientNameIndex.put(drugNameSynonymNoSpaces, synonymIngredients);
				}
				synonymIngredients.add(cdmIngredient);
				if (synonymIngredients.size() > 1) {
					System.out.println("      WARNING: Multiple ingredients found for synonym '" + drugNameSynonym + "'!");
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
	
	
	private boolean getSourceDrugs(InputFile sourceDrugsFile) {
		boolean sourceDrugError = false;
		String fileName = "";
		
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
			System.out.println("         Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
			System.out.println("         For " + SourceDrug.getCASNumbersSet() + " source ingredients the CAS number is set");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
		
		return (!sourceDrugError);
	}
	
	
	private boolean matchIngredients() {
		boolean ok = true;
		String fileName = "";
		
		Integer multipleMappings = 0;
		
		System.out.println(DrugMapping.getCurrentTime() + "     Match ingredients by full name ...");

		PrintWriter matchIngredientsFile = null;
		try {
			// Create match ingredients file output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Match Ingredients.csv";
			matchIngredientsFile = new PrintWriter(new File(fileName));
			matchIngredientsFile.println(SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
		} 
		catch (FileNotFoundException e) {
			System.out.println("       WARNING: Cannot create output file '" + fileName + "'");
			matchIngredientsFile = null;
		}
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			boolean multipleMapping = false;
			Set<CDMIngredient> matchedCDMIngredients = cdmIngredientNameIndex.get(sourceIngredient.getIngredientNameEnglishNoSpaces());
			if (matchedCDMIngredients != null) {
				if (matchedCDMIngredients.size() == 1) {
					CDMIngredient cdmIngredient = (CDMIngredient) matchedCDMIngredients.toArray()[0];
					ingredientMap.put(sourceIngredient, cdmIngredient);
					sourceIngredient.setMatch(((CDMIngredient) matchedCDMIngredients.toArray()[0]).getConceptId());
					sourceIngredient.setMatchString(sourceIngredient.getIngredientNameEnglishNoSpaces());
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
							sourceIngredient.setMatch(cdmIngredient.getConceptId());
							sourceIngredient.setMatchString(matchingCASnameNoSpaces);
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
			
			if (matchIngredientsFile != null) {
				matchIngredientsFile.println(sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatch()));
			}
		}
		
		if (matchIngredientsFile != null) {
			matchIngredientsFile.close();
		}

		PrintWriter matchSourceDrugIngredientsFile = null;
		try {
			// Create match ingredients file output file
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Match SourceDrug Ingredients.csv";
			matchSourceDrugIngredientsFile = new PrintWriter(new File(fileName));
			matchSourceDrugIngredientsFile.println(SourceDrug.getHeader() + "," + SourceIngredient.getMatchHeader() + "," + CDMIngredient.getHeader());
			
			for (SourceDrug sourceDrug : sourceDrugs) {
				for (SourceIngredient sourceIngredient : sourceDrug.getIngredients()) {
					matchSourceDrugIngredientsFile.println(sourceDrug + "," + sourceIngredient.toMatchString() + "," + cdmIngredients.get(sourceIngredient.getMatch()));
				}
			}
			
			matchSourceDrugIngredientsFile.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("       WARNING: Cannot create output file '" + fileName + "'");
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
