package org.ohdsi.drugmapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class UnitConversion {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_NEW_UNITS = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	public static String FILENAME = "DrugMapping - UnitConversionMap.csv";
	
	private String unitMapDate = null;
	private Map<String, String> cdmUnitNameToConceptIdMap = new HashMap<String, String>();                     // Map from CDM unit concept_name to CDM unit concept_id
	private Map<String, String> cdmUnitConceptIdToNameMap = new HashMap<String, String>();                     // Map from CDM unit concept_id to CDM unit concept_name
	private List<String> cdmUnitConceptNames = new ArrayList<String>();                                        // List of CDM unit names for sorting
	private List<String> sourceUnitNames = new ArrayList<String>();                                            // List of source unit names for sorting
	private Map<String, Map<String, Double>> unitConversionMap = new HashMap<String, Map<String, Double>>();   // Map from Source unit to map from CDM unit concept_name to factor (CDM unit = Source unit * factor)
	
	private int status = STATE_EMPTY;
	
	
	public UnitConversion(CDMDatabase database, Set<String> sourceUnits) {
		System.out.println(DrugMapping.getCurrentTime() + " Create Units Conversion Map ...");

		System.out.println("    Get CDM units ...");
		
		sourceUnitNames.addAll(sourceUnits);
		Collections.sort(sourceUnitNames);
		
		QueryParameters queryParameters = new QueryParameters();
		queryParameters.set("@vocab", database.getVocabSchema());
	
		// Connect to the database
		RichConnection connection = database.getRichConnection(this.getClass());
		
		// Get CDM Forms
		for (Row queryRow : connection.queryResource("cdm/GetCDMUnits.sql", queryParameters)) {
			
			String concept_id   = queryRow.get("concept_id").trim();
			String concept_name = queryRow.get("concept_name").trim();
			
			cdmUnitNameToConceptIdMap.put(concept_name, concept_id);
			cdmUnitConceptIdToNameMap.put(concept_id, concept_name);
			if (!cdmUnitConceptNames.contains(concept_name)) {
				cdmUnitConceptNames.add(concept_name);
			}
		}
		
		// Close database connection
		connection.close();
		
		Collections.sort(cdmUnitConceptNames);
		
		//for (String concept_name : cdmUnitConceptNames) {
		//	System.out.println("        " + cdmUnitNameToConceptIdMap.get(concept_name) + "," + concept_name);
		//} 
		
		System.out.println("    Done");

		readFromFile();
		if (status == STATE_NOT_FOUND) {
			System.out.println("    Creating empty unit conversion map ...");
			writeUnitConversionsToFile();
			status = STATE_EMPTY;
			System.out.println("    Done");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Done");
	}
	
	
	private void readFromFile() {
		System.out.println("    Get unit conversion map ...");

		boolean newUnits = false;
		boolean lostUnits = false;
		boolean conceptNamesRead = false;
		Set<String> oldSourceUnits = new HashSet<String>();
		Set<String> oldCDMUnits = new HashSet<String>();
		
		File unitFile = new File(DrugMapping.getCurrentPath() + "/" + FILENAME);
		if (unitFile.exists()) {
			ReadCSVFileWithHeader unitConversionFile = new ReadCSVFileWithHeader(DrugMapping.getCurrentPath() + "/" + FILENAME, ',', '"');
			Iterator<Row> unitConversionFileIterator = unitConversionFile.iterator();
			Set<String> unitConcepts = unitConversionFile.getColumns();
			if (unitConcepts != null) {
				while (unitConversionFileIterator.hasNext()) {
					Row row = unitConversionFileIterator.next();
					
					if (!conceptNamesRead) {
						conceptNamesRead = true;
						unitMapDate = row.get("Local unit").replace('/', '-');
					}
					else {
						String sourceUnit = row.get("Local unit");
						oldSourceUnits.add(sourceUnit);
						
						if (!sourceUnitNames.contains(sourceUnit)) {
							System.out.println("    WARNING: Source unit '" + sourceUnit + "' no longer exists!");
							if (!sourceUnitNames.contains(sourceUnit)) {
								sourceUnitNames.add(sourceUnit);
							}
						}
						String mappingLine = "        " + sourceUnit;
						
						Map<String, Double> sourceUnitFactors = unitConversionMap.get(sourceUnit);
						if (sourceUnitFactors == null) {
							sourceUnitFactors = new HashMap<String, Double>();
							unitConversionMap.put(sourceUnit, sourceUnitFactors);
						}
						for (String concept_id : unitConcepts) {
							if (!concept_id.equals("Local unit")) {
								oldCDMUnits.add(concept_id);
								if (cdmUnitConceptIdToNameMap.keySet().contains(concept_id)) {
									String factorString = row.get(concept_id).trim();
									if (!factorString.equals("")) {
										try {
											double factor = Double.parseDouble(factorString);
											sourceUnitFactors.put(concept_id, factor);
											mappingLine += "=" + Double.toString(factor) + "*(" + concept_id + ",\"" + cdmUnitConceptIdToNameMap.get(concept_id) + "\")";
										}
										catch (NumberFormatException e) {
											System.out.println("    ERROR: Illegal factor '" + factorString + "' for '" + sourceUnit + "' to '" + cdmUnitConceptIdToNameMap.get(concept_id) + "' (" + concept_id + ") conversion!");
											status = STATE_ERROR;
										}
									}
								}
								else {
									System.out.println("    WARNING: CDM unit '" + cdmUnitConceptIdToNameMap.get(concept_id) + "' (" + concept_id + ") no longer exists!");
									lostUnits = true;
								}
							}
						}
						System.out.println(mappingLine);
					}
				}
				
				for (String sourceUnit : sourceUnitNames) {
					if (!oldSourceUnits.contains(sourceUnit)) {
						newUnits = true;
						break;
					}
				}
				
				if (!newUnits) {
					for (String cdmUnit : cdmUnitConceptIdToNameMap.keySet()) {
						if (!oldCDMUnits.contains(cdmUnit)) {
							newUnits = true;
						}
					}
				}
				
				if (newUnits) {
					status = STATE_NEW_UNITS;
				}
				else {
					status = STATE_OK;
				}
				
				if (newUnits || lostUnits) {
					writeUnitConversionsToFile();
				}
			}
		}
		else {
			System.out.println("    WARNING: No unit conversion map found!");
			status = STATE_NOT_FOUND;
		}
		
		System.out.println("    Done");
	}
	
	
	private void writeUnitConversionsToFile() {
		String unitFileName = DrugMapping.getCurrentPath() + "/" + FILENAME;
		File unitFile = new File(unitFileName);
		if (unitFile.exists()) {
			// Backup old unit conversion map
			String oldUnitFileName = null;
			File oldUnitFile = null;
			int fileNr = 0;
			do {
				fileNr++;
				String fileNrString = "00" + Integer.toString(fileNr);
				fileNrString = fileNrString.substring(fileNrString.length());
				oldUnitFileName = DrugMapping.getCurrentPath() + "/" + unitMapDate + " " + fileNrString + " " + FILENAME;
				oldUnitFile = new File(oldUnitFileName);
			} while (oldUnitFile.exists());
			try {
				PrintWriter oldUnitFileWriter = new PrintWriter(new File(oldUnitFileName));
				try {
					BufferedReader oldUnitFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(unitFileName)));
					String line;
					while ((line = oldUnitFileReader.readLine()) != null) {
						oldUnitFileWriter.println(line);
					}
					oldUnitFileReader.close();
					oldUnitFileWriter.close();
				}
				catch (FileNotFoundException e) {
					System.out.println("    ERROR: Cannot find original unit conversion map '" + unitFileName + "'!");
					status = STATE_ERROR;
				}
				catch (IOException e) {
					System.out.println("    ERROR: Reading original unit conversion map '" + unitFileName + "'!");
					status = STATE_ERROR;
				}
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create backup unit conversion map '" + oldUnitFileName + "'!");
				status = STATE_ERROR;
			}
		}

		if (status != STATE_ERROR) {
			try {
				PrintWriter unitFileWriter = new PrintWriter(unitFile);

				String header1 = "Local unit";
				String header2 = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
				for (String concept_name : cdmUnitConceptNames) {
					header1 += "," + cdmUnitNameToConceptIdMap.get(concept_name);
					header2 += "," + "\"" + concept_name + "\"";
				}
				unitFileWriter.println(header1);
				unitFileWriter.println(header2);
				Collections.sort(sourceUnitNames);
				for (String sourceUnitName : sourceUnitNames) {
					String record = "\"" + sourceUnitName + "\""; 
					Map<String, Double> sourceUnitMap = unitConversionMap.get(sourceUnitName);
					for (String concept_name : cdmUnitConceptNames) {
						String concept_id = cdmUnitNameToConceptIdMap.get(concept_name);
						Double factor = null;
						if (sourceUnitMap != null) {
							factor = sourceUnitMap.get(concept_id);
						}
						record += "," + (factor == null ? "" : factor);
					}
					unitFileWriter.println(record);
				}
				unitFileWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create unit conversion map '" + DrugMapping.getCurrentPath() + "/" + FILENAME + "'!");
				status = STATE_ERROR;
			}
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public boolean compatibleUnits(String sourceUnit, String cdmUnit) {
		return (getFactor(sourceUnit, cdmUnit) != null);
	}
	
	
	private Double getFactor(String sourceUnit, String cdmUnit) {
		Double factor = null;
		
		if ((sourceUnit != null) && sourceUnitNames.contains(sourceUnit)) {
			if (cdmUnit != null) {
				if (cdmUnitConceptNames.contains(cdmUnit)) {
					if (cdmUnitNameToConceptIdMap.keySet().contains(cdmUnit)) {
						cdmUnit = cdmUnitNameToConceptIdMap.get(cdmUnit);
					}
					else {
						cdmUnit = null;
					}
				}
			}
		}
		
		if ((sourceUnit == null) && (cdmUnit == null)) {
			factor = 1.0;
		}
		
		if ((sourceUnit != null) && (cdmUnit != null)) {
			factor = unitConversionMap.get(sourceUnit).get(cdmUnit);
		}
		
		return factor;
	}
	
	
	public boolean matches(String sourceUnit, Double sourceValue, String cdmUnit, Double cdmValue) {
		boolean matches = false;
		
		Double factor = getFactor(sourceUnit, cdmUnit);
		if (factor != null) {
			matches = (cdmValue == (sourceValue * factor));
		}
		
		return matches;
	}
	
	
	public boolean matches(String sourceNumeratorUnit, Double sourceNumeratorValue, String sourceDenominatorUnit, Double sourceDenominatorValue, String cdmNumeratorUnit, Double cdmNumeratorValue, String cdmDenominatorUnit, Double cdmDenominatorValue) {
		boolean matches = false;
		
		Double numeratorFactor = getFactor(sourceNumeratorUnit, cdmNumeratorUnit);
		Double denominatorFactor = getFactor(sourceDenominatorUnit, cdmDenominatorUnit);

		if (numeratorFactor != null) {
			if ((sourceNumeratorUnit != null) && (sourceDenominatorUnit == null)) {
				matches = ((sourceNumeratorValue * numeratorFactor) == cdmNumeratorValue);
			}
			else if (
						(sourceNumeratorValue != null) &&
						(sourceDenominatorValue != null) &&
						(cdmNumeratorValue != null) &&
						(numeratorFactor != null) &&
						(cdmDenominatorValue != null) &&
						(denominatorFactor != null)
			) {
				matches = (((sourceNumeratorValue * numeratorFactor) / (sourceDenominatorValue * denominatorFactor)) == (cdmNumeratorValue/cdmDenominatorValue)); 
			}
		}
		
		return matches;
	}
}
