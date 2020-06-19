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
import java.util.NoSuchElementException;
import java.util.Set;

import org.ohdsi.drugmapping.cdm.CDM;
import org.ohdsi.drugmapping.cdm.CDMIngredientStrength;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.source.SourceDrugComponent;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class UnitConversionOld {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	public static String FILENAME = "DrugMapping - UnitConversionMap.csv";
	
	private CDM cdm = null;
	
	private String unitMapDate = null;
	private List<String> sourceUnitNames = new ArrayList<String>();                                            // List of source unit names for sorting
	private Map<String, Map<String, Double>> unitConversionMap = new HashMap<String, Map<String, Double>>();   // Map from Source unit to map from CDM unit concept_name to factor (CDM unit = Source unit * factor)
	
	private int status = STATE_EMPTY;
	private Set<String> oldSourceUnits = new HashSet<String>();
	private Set<String> oldCDMUnits = new HashSet<String>();
	
	
	public UnitConversionOld(Set<String> sourceUnits, CDM cdm) {
		this.cdm = cdm;
		System.out.println(DrugMapping.getCurrentTime() + " Create Units Conversion Map ...");
		
		sourceUnitNames.addAll(sourceUnits);
		Collections.sort(sourceUnitNames);

		readFromFile(cdm);
		if (status == STATE_NOT_FOUND) {
			System.out.println("    Creating empty unit conversion map ...");
			writeUnitConversionsToFile(cdm);
			status = STATE_EMPTY;
			System.out.println("    Done");
		}
		else if (status == STATE_CRITICAL) {
			System.out.println("    Creating new unit conversion map ...");
			writeUnitConversionsToFile(cdm);
			System.out.println("    Done");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Done");
	}
	
	
	private void readFromFile(CDM cdm) {
		System.out.println("    Get unit conversion map from file " + DrugMapping.getBasePath() + "/" + FILENAME + " ...");

		boolean newSourceUnits = false;
		boolean newCDMUnits = false;
		boolean conceptNamesRead = false;
		
		File unitFile = new File(DrugMapping.getBasePath() + "/" + FILENAME);
		if (unitFile.exists()) {
			status = STATE_OK;
			
			try {
				ReadCSVFileWithHeader unitConversionFile = new ReadCSVFileWithHeader(DrugMapping.getBasePath() + "/" + FILENAME, ',', '"');

				Iterator<Row> unitConversionFileIterator = unitConversionFile.iterator();
				Set<String> unitConcepts = unitConversionFile.getColumns();
				if (unitConcepts != null) {
					while (unitConversionFileIterator.hasNext()) {
						Row row = unitConversionFileIterator.next();
						
						if (!conceptNamesRead) {
							conceptNamesRead = true;
							unitMapDate = row.get("Local unit \\ CDM unit", true).replace('/', '-');
						}
						else {
							String sourceUnit = row.get("Local unit \\ CDM unit", true);
							oldSourceUnits.add(sourceUnit);

							String mappingLine = "        " + sourceUnit;
							if ((!sourceUnitNames.contains(sourceUnit)) && (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS))) {
								mappingLine = "    WARNING: Source unit '" + sourceUnit + "' no longer exists!";
								if (!sourceUnitNames.contains(sourceUnit)) {
									sourceUnitNames.add(sourceUnit);
								}
							}
							
							Map<String, Double> sourceUnitFactors = unitConversionMap.get(sourceUnit);
							if (sourceUnitFactors == null) {
								sourceUnitFactors = new HashMap<String, Double>();
								unitConversionMap.put(sourceUnit, sourceUnitFactors);
							}
							for (String concept_id : unitConcepts) {
								if (!concept_id.equals("Local unit \\ CDM unit")) {
									oldCDMUnits.add(concept_id);
									if (cdm.getCDMUnitConceptIdToNameMap().keySet().contains(concept_id)) {
										String factorString = row.get(concept_id, true).trim();
										if (!factorString.equals("")) {
											try {
												double factor = Double.parseDouble(factorString);
												sourceUnitFactors.put(concept_id, factor);
												mappingLine += "=" + Double.toString(factor) + "*(" + concept_id + ",\"" + cdm.getCDMUnitConceptName(concept_id) + "\")";
											}
											catch (NumberFormatException e) {
												System.out.println("    ERROR: Illegal factor '" + factorString + "' for '" + sourceUnit + "' to '" + cdm.getCDMUnitConceptName(concept_id) + "' (" + concept_id + ") conversion!");
												status = STATE_ERROR;
											}
										}
									}
									else if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
										System.out.println("    WARNING: CDM unit '" + cdm.getCDMUnitConceptName(concept_id) + "' (" + concept_id + ") no longer exists!");
									}
								}
							}
							System.out.println(mappingLine);
						}
					}
					
					for (String sourceUnit : sourceUnitNames) {
						if (!oldSourceUnits.contains(sourceUnit)) {
							if (!newSourceUnits) {
								System.out.println();
								System.out.println("    NEW SOURCE UNITS FOUND:");
							}
							System.out.println("        " + sourceUnit);
							newSourceUnits = true;
						}
					}

					for (String cdmUnit : cdm.getCDMUnitConceptIdToNameMap().keySet()) {
						if (!oldCDMUnits.contains(cdmUnit)) {
							if (!newCDMUnits) {
								System.out.println();
								System.out.println("    NEW CDM UNITS FOUND:");
							}
							System.out.println("        " + cdmUnit);
							newCDMUnits = true;
						}
					}
					
					if (newSourceUnits || newCDMUnits) {
						status = STATE_CRITICAL;
					}
					else {
						status = STATE_OK;
					}
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
			}
		}
		else {
			System.out.println("    ERROR: No unit conversion map found!");
			status = STATE_NOT_FOUND;
		}
		
		System.out.println("    Done");
	}
	
	
	private void writeUnitConversionsToFile(CDM cdm) {
		String unitFileName = DrugMapping.getBasePath() + "/" + FILENAME;
		File unitFile = new File(unitFileName);
		if (unitFile.exists()  & unitFile.canRead()) {
			// Backup old unit conversion map
			String oldUnitFileName = null;
			File oldUnitFile = null;
			int fileNr = 0;
			do {
				fileNr++;
				String fileNrString = "00" + Integer.toString(fileNr);
				fileNrString = fileNrString.substring(fileNrString.length() - 2);
				oldUnitFileName = DrugMapping.getBasePath() + "/" + FILENAME.substring(0, FILENAME.length() - 4) + " " + unitMapDate + " " + fileNrString + FILENAME.substring(FILENAME.length() - 4);
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

				String header1 = "Local unit \\ CDM unit";
				String header2 = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
				for (String concept_name : cdm.getCDMUnitConceptNames()) {
					header1 += "," + cdm.getCDMUnitConceptId(concept_name);
					header2 += "," + "\"" + concept_name + "\"";
				}
				unitFileWriter.println(header1);
				unitFileWriter.println(header2);
				Set<String> allSourceUnitNamesSet = new HashSet<String>();
				allSourceUnitNamesSet.addAll(sourceUnitNames);
				allSourceUnitNamesSet.addAll(oldSourceUnits);
				List<String> allSourceUnitNames = new ArrayList<String>();
				allSourceUnitNames.addAll(allSourceUnitNamesSet);
				Collections.sort(allSourceUnitNames);
				for (String sourceUnitName : allSourceUnitNames) {
					String record = "\"" + sourceUnitName + "\""; 
					Map<String, Double> sourceUnitMap = unitConversionMap.get(sourceUnitName);
					for (String concept_name : cdm.getCDMUnitConceptNames()) {
						String concept_id = cdm.getCDMUnitConceptId(concept_name);
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
				System.out.println("    ERROR: Cannot create unit conversion map '" + DrugMapping.getBasePath() + "/" + FILENAME + "'!");
				status = STATE_ERROR;
			}
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public boolean compatibleUnits(String sourceUnit, String cdmUnit, CDM cdm) {
		return (getFactor(sourceUnit, cdmUnit, cdm) != null);
	}
	
	
	private Double getFactor(String sourceUnit, String cdmUnit, CDM cdm) {
		Double factor = null;
		
		if ((sourceUnit != null) && sourceUnitNames.contains(sourceUnit)) {
			if (cdmUnit != null) {
				if (!cdm.getCDMUnitConceptIdToNameMap().keySet().contains(cdmUnit)) {
					cdmUnit = null;
				}
			}
		}
		
		if ((sourceUnit == null) && (cdmUnit == null)) {
			factor = 1.0;
		}
		
		if ((sourceUnit != null) && (cdmUnit != null)) {
			if (unitConversionMap.get(sourceUnit) == null) {
				System.out.println("ERROR: " + sourceUnit);
			}
			factor = unitConversionMap.get(sourceUnit).get(cdmUnit);
		}
		
		return factor;
	}
	
	
	public boolean matches(String sourceUnit, Double sourceValue, String cdmUnit, Double cdmValue, CDM cdm) {
		boolean matches = false;
		
		Double factor = getFactor(sourceUnit, cdmUnit, cdm);
		if (factor != null) {
			matches = (cdmValue == (sourceValue * factor));
		}
		
		return matches;
	}
	
	
	public boolean matches(String sourceNumeratorUnit, Double sourceNumeratorValue, String sourceDenominatorUnit, Double sourceDenominatorValue, String cdmNumeratorUnit, Double cdmNumeratorValue, String cdmDenominatorUnit, Double cdmDenominatorValue, double deviationPercentage, CDM cdm) {
		boolean matches = false;
		
		Double numeratorFactor = getFactor(sourceNumeratorUnit, cdmNumeratorUnit, cdm);
		Double denominatorFactor = getFactor(sourceDenominatorUnit, cdmDenominatorUnit, cdm);
		Double deviationFactor = deviationPercentage / 100.0;

		if (numeratorFactor != null) {
			if ((sourceNumeratorUnit != null) && (sourceDenominatorUnit == null)) {
				if (cdmDenominatorUnit == null) {
					matches = (Math.round(((sourceNumeratorValue * numeratorFactor) - ((sourceNumeratorValue * numeratorFactor) * deviationFactor)) * 1000000) <= Math.round(cdmNumeratorValue * 1000000)) && (Math.round(((sourceNumeratorValue * numeratorFactor) + ((sourceNumeratorValue * numeratorFactor) * deviationFactor)) * 1000000) >= Math.round(cdmNumeratorValue * 1000000));
					//matches = (((sourceNumeratorValue * numeratorFactor) - ((sourceNumeratorValue * numeratorFactor) * deviationFactor)) <= cdmNumeratorValue) && (((sourceNumeratorValue * numeratorFactor) + ((sourceNumeratorValue * numeratorFactor) * deviationFactor)) >= cdmNumeratorValue);
					//matches = ((sourceNumeratorValue * numeratorFactor) == cdmNumeratorValue);
				}
			}
			else if (
						(sourceNumeratorValue != null) &&
						(sourceDenominatorValue != null) &&
						(cdmNumeratorValue != null) &&
						(numeratorFactor != null) &&
						(cdmDenominatorValue != null) &&
						(denominatorFactor != null)
			) {
				Double compatibleSourceValue = ((sourceNumeratorValue * numeratorFactor) / (sourceDenominatorValue * denominatorFactor));
				Double compatibleCDMValue = cdmNumeratorValue/cdmDenominatorValue;
				matches = (Math.round((compatibleSourceValue - (compatibleSourceValue * deviationFactor)) * 1000000) <= Math.round(compatibleCDMValue * 1000000)) && (Math.round((compatibleSourceValue + (compatibleSourceValue * deviationFactor)) * 1000000) >= Math.round(compatibleCDMValue * 1000000));
				//matches = ((compatibleSourceValue - (compatibleSourceValue * deviationFactor)) <= compatibleCDMValue) && ((compatibleSourceValue + (compatibleSourceValue * deviationFactor)) >= compatibleCDMValue);
				//matches = (((sourceNumeratorValue * numeratorFactor) / (sourceDenominatorValue * denominatorFactor)) == (cdmNumeratorValue/cdmDenominatorValue)); 
			}
		}
		
		return matches;
	}
	
	
	public Double getStandardizedStrength(SourceDrugComponent sourceDrugComponent, CDMIngredientStrength cdmIngredientStrength) {
		Double strength = null;
		String sourceNumeratorUnit = sourceDrugComponent.getNumeratorDosageUnit();
		Double sourceNumeratorValue = sourceDrugComponent.getNumeratorDosage();
		String sourceDenominatorUnit = sourceDrugComponent.getDenominatorDosageUnit();
		Double sourceDenominatorValue = sourceDrugComponent.getDenominatorDosage();
		String cdmNumeratorUnit = cdmIngredientStrength.getNumeratorDosageUnit();
		String cdmDenominatorUnit = cdmIngredientStrength.getDenominatorDosageUnit();
		
		Double numeratorFactor = getFactor(sourceNumeratorUnit, cdmNumeratorUnit, cdm);
		Double denominatorFactor = getFactor(sourceDenominatorUnit, cdmDenominatorUnit, cdm);

		if (numeratorFactor != null) {
			if ((sourceNumeratorUnit != null) && (sourceDenominatorUnit == null)) {
				if (cdmDenominatorUnit == null) {
					strength = sourceNumeratorValue * numeratorFactor;
				}
			}
			else if (
						(sourceNumeratorValue != null) &&
						(sourceDenominatorValue != null) &&
						(numeratorFactor != null) &&
						(denominatorFactor != null)
			) {
				strength = (sourceNumeratorValue * numeratorFactor) / (sourceDenominatorValue * denominatorFactor); 
			}
		}
		
		return strength;
	}
}
