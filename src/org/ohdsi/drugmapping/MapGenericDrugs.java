package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class MapGenericDrugs extends Mapping {

	private Map<String, CDMIngredient> cdmIngredients = new HashMap<String, CDMIngredient>();
	private List<CDMIngredient> cdmIngredientsList = new ArrayList<CDMIngredient>();
	private Map<SourceIngredient, CDMIngredient> ingredientMap = new HashMap<SourceIngredient, CDMIngredient>();
	private Map<String, Set<CDMIngredient>> mapsToCDMIngredient = new HashMap<String, Set<CDMIngredient>>();
	private Map<String, Set<String>> drugNameSynonyms = new HashMap<String, Set<String>>();
	private Map<String, CDMDrug> cdmDrugs = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();
	private Map<SourceDrug, CDMDrug> drugMapping = new HashMap<SourceDrug, CDMDrug>();
	private Map<String, CDMDrug> cdmDrugComps = new HashMap<String, CDMDrug>();
	private Map<CDMIngredient, List<CDMDrug>> cdmDrugCompsContainingIngredient = new HashMap<CDMIngredient, List<CDMDrug>>();

	private List<SourceDrug> sourceDrugs = new ArrayList<SourceDrug>();
	private Map<String, SourceDrug> sourceDrugMap = new HashMap<String, SourceDrug>();
	private Set<String> forms = new HashSet<String>();
	private Set<String> units = new HashSet<String>();
	private int noATCCounter = 0; // Counter of source drugs without ATC code.

	private Map<SourceDrug, CDMDrug> drugCompMapping = new HashMap<SourceDrug, CDMDrug>();
	
	
	public MapGenericDrugs(CDMDatabase database, InputFile sourceDrugsFile) {
		boolean ok = true;
		
		// Get CDM Ingredients
		ok = getCDMIngredients(database);			
		
		// Load source drug ingredient mapping
		ok = ok && getSourceDrugIngredientMapping(sourceDrugsFile);
		
		// Get unit mappings
		ok = ok && getUnitMapping(database);
		
		// Get form mappings
		ok = ok && getFormMapping(database);
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
			System.out.println(DrugMapping.getCurrentTime() + " Get CDM ingredients ...");
			
			QueryParameters queryParameters = new QueryParameters();
			queryParameters.set("@vocab", database.getVocabSchema());
		
			// Connect to the database
			RichConnection connection = database.getRichConnection(this.getClass());
			
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
			
			// Close database connection
			connection.close();
			
			rxNormIngredientsFile.close();
			mapsToRxNormIngredientsFile.close();
		} 
		catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
			ok = false;
		}
		
		return ok;
	}
	
	
	private boolean getSourceDrugIngredientMapping(InputFile sourceDrugsFile) {
		boolean sourceDrugError = false;
		String fileName = "";
		
		System.out.println(DrugMapping.getCurrentTime() + " Loading source drugs ...");
		
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

		//TODO						// Apply manual translation correction
		//TODO						if (ingredientTranslationCorrections.containsKey(ingredientName)) {
		//TODO							ingredientNameEnglish = ingredientTranslationCorrections.get(ingredientName);
		//TODO						}
						
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
						/*
						if (!ingredientName.equals("")) {
							SourceIngredient sourceIngredient = SourceDrug.findIngredient(ingredientName, ingredientNameEnglish, casNumber);
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
						*/
						SourceIngredient sourceIngredient = null;
						if ((!ingredientName.equals("")) && (!ingredientNameEnglish.equals(""))) {
							sourceIngredient = sourceDrug.AddIngredientByCASnumber(ingredientName, ingredientNameEnglish, casNumber, dosage, dosageUnit);
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

						String cdmIngredientConceptId      = sourceDrugsFile.get(row, "ConceptId").trim();
						String cdmIngredientDomainId       = sourceDrugsFile.get(row, "DomainId").trim();
						String cdmIngredientConceptName    = sourceDrugsFile.get(row, "ConceptName").trim();
						String cdmIngredientcomment        = sourceDrugsFile.get(row, "Comment").trim();
						String cdmIngredientConceptClassId = sourceDrugsFile.get(row, "ConceptClassId").trim();
						String cdmIngredientVocabularyId   = sourceDrugsFile.get(row, "VocabularyId").trim();
						String cdmIngredientConceptCode    = sourceDrugsFile.get(row, "ConceptCode").trim();
						String cdmIngredientValidStartDate = sourceDrugsFile.get(row, "ValidStartDate").trim();
						String cdmIngredientValidEndDate   = sourceDrugsFile.get(row, "ValidEndDate").trim();
						String cdmIngredientInvalidReason  = sourceDrugsFile.get(row, "InvalidReason").trim();
						String cdmIngredientInfo           = sourceDrugsFile.get(row, "Info").trim();
						
						if ((!cdmIngredientConceptId.equals("")) && (!cdmIngredientConceptId.equals("0"))) {
							CDMIngredient cdmIngredient = cdmIngredients.get(cdmIngredientConceptId);
							
							if (sourceIngredient != null) {
								if (cdmIngredient != null) {
									CDMIngredient existingIngredientMapping = ingredientMap.get(sourceIngredient);
									if ((existingIngredientMapping != null) && (existingIngredientMapping != cdmIngredient)) {
										System.out.println("ERROR: Conflicting ingredient mapping: " + sourceIngredient + " -> " + existingIngredientMapping + " <=> " + cdmIngredient);
										sourceDrugError = true;
									}
									if (existingIngredientMapping == null) {
										ingredientMap.put(sourceIngredient, cdmIngredient);
									}
								}
								else {
									String cdmIngredientDescription = cdmIngredientConceptId;
									cdmIngredientDescription += "," + cdmIngredientConceptName;
									cdmIngredientDescription += "," + cdmIngredientDomainId;
									cdmIngredientDescription += "," + cdmIngredientConceptClassId;
									cdmIngredientDescription += "," + cdmIngredientVocabularyId;
									cdmIngredientDescription += "," + cdmIngredientConceptCode;
									cdmIngredientDescription += "," + cdmIngredientValidStartDate;
									cdmIngredientDescription += "," + cdmIngredientValidEndDate;
									cdmIngredientDescription += "," + cdmIngredientInvalidReason;
									System.out.println("WARNING: CDM Ingredient [" + cdmIngredientDescription + "] does not exist!");
								}
							}
						}
					}
				}
				
				int sourceDrugsWithAllIngredientsMapped = 0;
				for (SourceDrug sourceDrug : sourceDrugs) {
					List<SourceIngredient> ingredients = sourceDrug.getIngredients();
					if (ingredients.size() > 0) {
						boolean allIngredientsMapped = true;
						for (SourceIngredient ingredient : ingredients) {
							if (ingredientMap.get(ingredient) == null) {
								allIngredientsMapped = false;
								break;
							}
						}
						if (allIngredientsMapped) {
							sourceDrugsWithAllIngredientsMapped++;
						}
					}
				}
				
				missingATCFile.close();
				
				System.out.println("    Found " + Integer.toString(sourceDrugs.size()) + " source drugs");
				System.out.println("    Found " + Integer.toString(SourceDrug.getAllIngredients().size()) + " source ingredients");
				System.out.println("    Source ingredients mapped to CDM ingredients: " + Integer.toString(ingredientMap.size()) + " (" + Long.toString(Math.round(((double) ingredientMap.size() / (double) SourceDrug.getAllIngredients().size()) * 100)) + "%)");
				System.out.println("    Source drugs with all ingredients mapped to CDM ingredients: " + Integer.toString(sourceDrugsWithAllIngredientsMapped) + " (" + Long.toString(Math.round(((double) sourceDrugsWithAllIngredientsMapped / (double) sourceDrugs.size()) * 100)) + "%)");
			}
		} 
		catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
			sourceDrugError = true;
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
		
		return (!sourceDrugError);
	}
	
	
	private boolean getUnitMapping(CDMDatabase database) {
		// Create Units Map
		UnitConversion unitConversionsMap = new UnitConversion(database, units);
		if (unitConversionsMap.getStatus() == UnitConversion.STATE_EMPTY) {
			// If no unit conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the unit conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getCurrentPath() + "/" + UnitConversion.FILENAME);
		}
		else {
			System.out.println("    Found " + Integer.toString(units.size()) + " source units");
		}
		
		return ((unitConversionsMap.getStatus() != UnitConversion.STATE_EMPTY) && (unitConversionsMap.getStatus() != UnitConversion.STATE_ERROR));
	}
	
	
	private boolean getFormMapping(CDMDatabase database) {
		// Create Forms Map
		FormConversion formConversionsMap = new FormConversion(database, forms);
		if (formConversionsMap.getStatus() == FormConversion.STATE_EMPTY) {
			// If no form conversion is specified then stop.
			System.out.println("");
			System.out.println("First fill the form conversion map in the file:");
			System.out.println("");
			System.out.println(DrugMapping.getCurrentPath() + "/" + FormConversion.FILENAME);
		}
		else {
			System.out.println("    Found " + Integer.toString(forms.size()) + " source forms");
		}
		
		return ((formConversionsMap.getStatus() != FormConversion.STATE_EMPTY) && (formConversionsMap.getStatus() != FormConversion.STATE_ERROR));
	}

}
