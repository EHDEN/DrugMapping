package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class UnitConversion {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	private static String DEFAULT_FILENAME = "DrugMapping - UnitConversionMap.csv";
	
	
	private int status = STATE_OK;
	private String fileName = "";
	
	private Map<String, Map<String, Double>> unitConversionMap = new HashMap<String, Map<String, Double>>();
	
	
	public static String getDefaultFileName() {
		return DrugMapping.getBasePath() + "/" + DEFAULT_FILENAME;
	}
	
	
	public UnitConversion(InputFile sourceUnitMappingFile) {
		System.out.println(DrugMapping.getCurrentTime() + "     Create Units Conversion Map ...");
		
		readUnitConversionFile(sourceUnitMappingFile);
		if (status == STATE_EMPTY) {
			createUnitConversionFile(sourceUnitMappingFile);
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	private void readUnitConversionFile(InputFile sourceUnitMappingFile) {
		if ((sourceUnitMappingFile != null) && sourceUnitMappingFile.openFile(true)) {
			fileName = sourceUnitMappingFile.getFileName();
			System.out.println(DrugMapping.getCurrentTime() + "        Get unit conversion map from file " + fileName + " ...");
			while (sourceUnitMappingFile.hasNext()) {
				Row row = sourceUnitMappingFile.next();
				
				String sourceUnit = DrugMappingStringUtilities.removeExtraSpaces(sourceUnitMappingFile.get(row, "SourceUnit", true));
				//String drugCountString = sourceUnitMappingFile.get(row, "DrugCount", false);
				//String recordCountString = sourceUnitMappingFile.get(row, "RecordCount", false);
				String factorString = sourceUnitMappingFile.get(row, "Factor", true);
				String targetUnit = DrugMappingStringUtilities.removeExtraSpaces(sourceUnitMappingFile.get(row, "TargetUnit", true)).toUpperCase();
				//String comment = row.get("Comment", false);
				
				Double factor = null;
				if (!factorString.equals("")) {
					try {
						factor = Double.parseDouble(factorString);
					}
					catch (NumberFormatException exception) {
						System.out.println("    ERROR: Illegal factor '" + factorString + "' for '" + sourceUnit + "' to '" + targetUnit + "' conversion.");
						status = STATE_ERROR;
					}
				}
				else {
					if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
						System.out.println("    WARNING: No factor found for conversion from  '" + sourceUnit + "' to '" + targetUnit + "'. Defaults to 1.0.");
					}
					factor = 1.0;
				}
				
				if (factor != null) {
					if ((!sourceUnit.equals("")) && (targetUnit.equals(""))) {
						System.out.println("    WARNING: No target unit specified for '" + sourceUnit + ".");
					}
					else {
						Map<String, Double> sourceUnitConversion = unitConversionMap.get(sourceUnit);
						if (sourceUnitConversion == null) {
							sourceUnitConversion = new HashMap<String, Double>();
							unitConversionMap.put(sourceUnit, sourceUnitConversion);
						}
						Double existingFactor = sourceUnitConversion.get(targetUnit);
						if (existingFactor != null) {
							if (DrugMapping.settings.getStringSetting(MainFrame.SUPPRESS_WARNINGS).equals("No")) {
								System.out.println("    WARNING: Double conversion from  '" + sourceUnit + "' to '" + targetUnit + "' found: " + existingFactor + " (old) and " + factor + " (new). Last one used.");
							}
						}
						sourceUnitConversion.put(targetUnit, factor);
					}
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + "        Done");
		}
		else {
			System.out.println("    ERROR: No unit conversion file found.");
			status = STATE_EMPTY;
		}
	}
	
	
	private void createUnitConversionFile(InputFile sourceUnitMappingFile) {
		fileName = getDefaultFileName();
		String fieldDelimiterName = "Comma";
		String textQualifierName = "\"";
		
		if ((sourceUnitMappingFile != null) && (!sourceUnitMappingFile.getFileName().equals(""))) {
			fileName = sourceUnitMappingFile.getFileName();
			fieldDelimiterName = sourceUnitMappingFile.getFieldDelimiter();
			textQualifierName = sourceUnitMappingFile.getTextQualifier();
		}
		
		String fieldDelimiter = Character.toString(InputFile.fieldDelimiter(fieldDelimiterName));
		String textQualifier = Character.toString(InputFile.textQualifier(textQualifierName));

		System.out.println(DrugMapping.getCurrentTime() + "        Write unit conversion map to file " + fileName + " ...");
		
		File mappingFile = new File(fileName);
		try {
			PrintWriter mappingFileWriter = new PrintWriter(mappingFile);
			
			String header = "SourceUnit";
			header += fieldDelimiter + "DrugCount";
			header += fieldDelimiter + "RecordCount";
			header += fieldDelimiter + "Factor";
			header += fieldDelimiter + "TargetUnit";
			header += fieldDelimiter + "Comment";
			
			mappingFileWriter.println(header);
			
			// Get all units
			List<String> sortedUnits = new ArrayList<String>();
			sortedUnits.addAll(SourceDrug.getAllUnits());
			Collections.sort(sortedUnits);
			
			// Write all units to file
			for (String unit : sortedUnits) {
				
				String record = DrugMappingStringUtilities.escapeFieldValue(unit, fieldDelimiter, textQualifier);
				record += fieldDelimiter + SourceDrug.getUnitSourceDrugUsage(unit);
				record += fieldDelimiter + SourceDrug.getUnitRecordUsage(unit);
				record += fieldDelimiter;
				record += fieldDelimiter;
				record += fieldDelimiter;
				
				mappingFileWriter.println(record);
			}
			
			mappingFileWriter.close();
			
			System.out.println(DrugMapping.getCurrentTime() + "        Done");
		} 
		catch (FileNotFoundException exception) {
			System.out.println("    ERROR: Cannot create unit mapping file '" + fileName + "'.");
			status = STATE_ERROR;
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public Double getConversion(String sourceUnit, Double sourceValue, String targetUnit) {
		Double result = null;
		if (sourceValue != null) {
			Map<String, Double> sourceConversion = unitConversionMap.get(sourceUnit);
			if (sourceConversion != null) {
				Double factor = sourceConversion.get(targetUnit);
				if (factor != null) {
					result = sourceValue * factor;
				}
			}
		}
		return result;
	}

}
