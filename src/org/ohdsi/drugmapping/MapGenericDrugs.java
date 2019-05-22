package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class MapGenericDrugs extends Mapping {
	
	
	public MapGenericDrugs(CDMDatabase database, InputFile genericDrugsFile) {
		
		String fileName = "";
		try {
			// Create all output files
			System.out.println(DrugMapping.getCurrentTime() + " Creating output files ...");

			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Ignored Missing Data.csv";
			System.out.println("    " + fileName);
			PrintWriter ignoredMissingDataFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(ignoredMissingDataFile);
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping Missing ATC.csv";
			System.out.println("    " + fileName);
			PrintWriter missingATCFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(missingATCFile);
			
			fileName = DrugMapping.getCurrentPath() + "/DrugMapping No ATC Concept Found.csv";
			System.out.println("    " + fileName);
			PrintWriter noATCConceptFoundFile = new PrintWriter(new File(fileName));
			writeGenericDrugHeaderToFile(noATCConceptFoundFile);
			
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			// Do the mapping
			System.out.println(DrugMapping.getCurrentTime() + " Mapping Generic Drugs ...");

			int genericDrugCounter        = 0; // Counter of generic drugs
			int ignoredMissingDataCounter = 0; // Counter of generic drugs that are ignored due to missing data
			int missingATCCounter         = 0; // Counter of generic drugs that have no ATC
			int noATCConceptFoundCounter  = 0; // Counter of generic drugs for which no ATC concept could be found
			
			if (genericDrugsFile.openFile()) {
				Map<String, String> genericDrugLine = null;
				Map<String, String> lastGenericDrugLine = null;
				List<Map<String, String>> genericDrug = new ArrayList<Map<String, String>>();
				while ((lastGenericDrugLine != null) || genericDrugsFile.hasNext() || (genericDrug.size() > 0)) {
					if (lastGenericDrugLine != null) {
						genericDrugLine = lastGenericDrugLine;
						lastGenericDrugLine = null;
					}
					else if (genericDrugsFile.hasNext()) {
						Row row = genericDrugsFile.next();
						
						genericDrugLine = new HashMap<String, String>();
						genericDrugLine.put("GenericDrugCode",      genericDrugsFile.get(row, "GenericDrugCode").trim());
						genericDrugLine.put("LabelName",            genericDrugsFile.get(row, "LabelName").trim());
						genericDrugLine.put("ShortName",            genericDrugsFile.get(row, "ShortName").trim());
						genericDrugLine.put("FullName",             genericDrugsFile.get(row, "FullName").trim());
						genericDrugLine.put("ATCCode",              genericDrugsFile.get(row, "ATCCode").trim());
						genericDrugLine.put("DDDPerUnit",           genericDrugsFile.get(row, "DDDPerUnit").trim());
						genericDrugLine.put("PrescriptionDays",     genericDrugsFile.get(row, "PrescriptionDays").trim());
						genericDrugLine.put("Dosage",               genericDrugsFile.get(row, "Dosage").trim());
						genericDrugLine.put("DosageUnit",           genericDrugsFile.get(row, "DosageUnit").trim());
						genericDrugLine.put("PharmaceuticalForm",   genericDrugsFile.get(row, "PharmaceuticalForm").trim());
						
						genericDrugLine.put("ComponentCode",        genericDrugsFile.get(row, "ComponentCode").trim());
						genericDrugLine.put("ComponentPartNumber",  genericDrugsFile.get(row, "ComponentPartNumber").trim());
						genericDrugLine.put("ComponentType",        genericDrugsFile.get(row, "ComponentType").trim());
						genericDrugLine.put("ComponentAmount",      genericDrugsFile.get(row, "ComponentAmount").trim());
						genericDrugLine.put("ComponentAmountUnit",  genericDrugsFile.get(row, "ComponentAmountUnit").trim());
						genericDrugLine.put("ComponentGenericName", genericDrugsFile.get(row, "ComponentGenericName").trim());
						genericDrugLine.put("ComponentCASNumber",   genericDrugsFile.get(row, "ComponentCASNumber").trim());
						
						genericDrugLine.put("SubstanceCode",        genericDrugsFile.get(row, "SubstanceCode").trim());
						genericDrugLine.put("SubstanceDescription", genericDrugsFile.get(row, "SubstanceDescription").trim());
						genericDrugLine.put("SubstanceCASNumber",   genericDrugsFile.get(row, "SubstanceCASNumber").trim());
					}
					
					if (genericDrugLine != null) {
						if ((genericDrug.size() > 0) && (!genericDrugLine.get("GenericDrugCode").equals(genericDrug.get(genericDrug.size() -1).get("GenericDrugCode")))) {
							lastGenericDrugLine = genericDrugLine;
							genericDrugLine = null;
						}
						else {
							genericDrug.add(genericDrugLine);
							genericDrugLine = null;
						}
					}
					
					if ((genericDrug.size() > 0) && ((lastGenericDrugLine != null) || (!genericDrugsFile.hasNext()))) {
						
						System.out.println("    " + genericDrug.get(0).get("GenericDrugCode") + "," + genericDrug.get(0).get("ATCCode") + "," + genericDrug.get(0).get("FullName"));
						genericDrugCounter++;
						
						// Map containing atc to CDM concept mapping
						Map<String, Map<String, String>> atcConceptMap = new HashMap<String, Map<String, String>>();
						
						if ((!genericDrug.get(0).get("GenericDrugCode").isEmpty()) && (!genericDrug.get(0).get("FullName").isEmpty())) {
							String query = null;
							
							// Get ATC concepts of components
							String atc = genericDrug.get(0).get("ATCCode");
							Map<String, String> atcConcept = null;
							if (!atc.isEmpty()) {
								atcConcept = atcConceptMap.get(atc);
								
								if (atcConcept == null) {
									query  = "select concept_id";
									query += "," + "concept_name"; 
									query += "," + "vocabulary_id";  
									query += "," + "standard_concept";
									query += "," + "concept_class_id"; 
									query += "," + "concept_code"; 
									query += " " + "from " + database.getVocabSchema() + ".concept"; 
									query += " " + "where upper(concept_code) = '" + atc + "'"; 
									query += " " + "and domain_id = 'Drug'"; 
									query += " " + "and vocabulary_id = 'ATC'"; 
									query += " " + "and concept_class_id = 'ATC " + (atc.length() == 1 ? "1st": (atc.length() == 3 ? "2nd" : (atc.length() == 4 ? "3rd" : (atc.length() == 5 ? "4th" : "5th")))) + "'";
									
									if (database.excuteQuery(query)) {
										if (database.hasNext()) {
											while (database.hasNext()) {
												Row queryRow = database.next();
												if (atcConcept != null) {
													database.disconnect();
													break;
												}
												atcConcept = new HashMap<String, String>(); 
												atcConcept.put("concept_id"      , queryRow.get("concept_id"));
												atcConcept.put("concept_name"    , queryRow.get("concept_name"));
												atcConcept.put("domain_id"       , "Drug");
												atcConcept.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
												atcConcept.put("concept_class_id", queryRow.get("concept_class_id"));
												atcConcept.put("standard_concept", queryRow.get("standard_concept"));
												atcConcept.put("concept_code"    , queryRow.get("concept_code"));
												atcConcept.put("match"           , atc);
												
												atcConceptMap.put(atc, atcConcept);
											}
										}
									}
								}
								
								if (atcConcept == null) {
									System.out.println("        ATC Concept:  NOT FOUND");
									writeGenericDrugToFile(genericDrug, noATCConceptFoundFile);
									noATCConceptFoundCounter++;
								}
								else {
									System.out.println("        ATC Concept: " + atcConcept.get("match") +
													   " -> "     + atcConcept.get("concept_id") +
											           ","        + atcConcept.get("concept_name") +
											           ","        + atcConcept.get("domain_id") +
											           ","        + atcConcept.get("vocabulary_id") +
											           ","        + atcConcept.get("concept_class_id") +
											           ","        + atcConcept.get("standard_concept") +
											           ","        + atcConcept.get("concept_code")
											);
									
									// TODO
								}
							}
							else {
								System.out.println("        NO ATC");
								writeGenericDrugToFile(genericDrug, missingATCFile);
								missingATCCounter++;
							}
							
							
						}
						else {
							writeGenericDrugToFile(genericDrug, ignoredMissingDataFile);
							ignoredMissingDataCounter++;
						}
						
						genericDrug.clear();
					}
				}
			}
			
			System.out.println("");
			System.out.println("    Generic drugs: " + Integer.toString(genericDrugCounter));
			System.out.println("    Ignored: " + Integer.toString(ignoredMissingDataCounter));
			System.out.println("    No ATC: " + Integer.toString(missingATCCounter));
			System.out.println("    No ATC Concept: " + Integer.toString(noATCConceptFoundCounter));
			System.out.println(DrugMapping.getCurrentTime() + " Finished");
			
			
			// Close all output files
			ignoredMissingDataFile.close();
			missingATCFile.close();
			noATCConceptFoundFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
		}
	}
	
	
	private void writeGenericDrugHeaderToFile(PrintWriter file) {
		String header = "GenericDrugCode";
		header += "," + "LabelName";
		header += "," + "ShortName";
		header += "," + "FullName";
		header += "," + "ATCCode";
		header += "," + "DDDPerUnit";
		header += "," + "PrescriptionDays";
		header += "," + "Dosage";
		header += "," + "DosageUnit";
		header += "," + "PharmaceuticalForm";
		
		header += "," + "ComponentCode";
		header += "," + "ComponentPartNumber";
		header += "," + "ComponentType";
		header += "," + "ComponentAmount";
		header += "," + "ComponentAmountUnit";
		header += "," + "ComponentGenericName";
		header += "," + "ComponentCASNumber";
		
		header += "," + "SubstanceCode";
		header += "," + "SubstanceDescription";
		header += "," + "SubstanceCASNumber";

		file.println(header);
	}
	
	
	private void writeGenericDrugToFile(List<Map<String, String>> genericDrug, PrintWriter file) {
		for (Map<String, String> record : genericDrug) {
			String line = record.get("GenericDrugCode") +
					"," + record.get("LabelName") +
					"," + record.get("ShortName") +
					"," + record.get("FullName") +
					"," + record.get("ATCCode") +
					"," + record.get("DDDPerUnit") +
					"," + record.get("PrescriptionDays") +
					"," + record.get("Dosage") +
					"," + record.get("DosageUnit") +
					"," + record.get("PharmaceuticalForm") +
					
					"," + record.get("ComponentCode") +
					"," + record.get("ComponentPartNumber") +
					"," + record.get("ComponentType") +
					"," + record.get("ComponentAmount") +
					"," + record.get("ComponentAmountUnit") +
					"," + record.get("ComponentGenericName") +
					"," + record.get("ComponentCASNumber") +
					
					"," + record.get("SubstanceCode") +
					"," + record.get("SubstanceDescription") +
					"," + record.get("SubstanceCASNumber");
			file.println(line);
		}
	}

}
