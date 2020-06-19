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
	
	
	public UnitConversion(InputFile sourceUnitMappingFile) {
		System.out.println(DrugMapping.getCurrentTime() + " Create Units Conversion Map ...");
		
		readUnitConversionFile(sourceUnitMappingFile);
		if (status == STATE_EMPTY) {
			createUnitConversionFile(sourceUnitMappingFile);
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Done");
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	private void readUnitConversionFile(InputFile sourceUnitMappingFile) {
		if ((sourceUnitMappingFile != null) && sourceUnitMappingFile.openFile(true)) {
			fileName = sourceUnitMappingFile.getFileName();
			System.out.println("    Get unit conversion map from file " + fileName + " ...");
			while (sourceUnitMappingFile.hasNext()) {
				Row row = sourceUnitMappingFile.next();
				
				String sourceUnit = DrugMappingStringUtilities.removeExtraSpaces(row.get("SourceUnit", true)).toUpperCase();
				//String drugCountString = row.get("DrugCount", false);
				//String recordCountString = row.get("RecordCount", false);
				String factorString = row.get("Factor", true);
				String targetUnit = DrugMappingStringUtilities.removeExtraSpaces(row.get("TargetUnit", true)).toUpperCase();
				//String comment = row.get("Comment", false);
				
				Double factor = null;
				if (!factorString.equals("")) {
					try {
						factor = Double.parseDouble(factorString);
					}
					catch (NumberFormatException exception) {
						System.out.println("    ERROR: Illegal factor '" + factorString + "' for '" + sourceUnit + "' to '" + targetUnit + "' conversion. Ignored.");
						status = STATE_ERROR;
					}
				}
				else {
					System.out.println("    WARNING: No factor found for conversion from  '" + sourceUnit + "' to '" + targetUnit + "'. Defaults to 1.0.");
					factor = 1.0;
				}
				
				if (factor != null) {
					if ((!sourceUnit.equals("")) && (targetUnit.equals(""))) {
						System.out.println("    ERROR: No target unit specified for '" + sourceUnit + ". Ignored.");
						status = STATE_ERROR;
					}
					else {
						Map<String, Double> sourceUnitConversion = unitConversionMap.get(sourceUnit);
						if (sourceUnitConversion == null) {
							sourceUnitConversion = new HashMap<String, Double>();
							unitConversionMap.put(sourceUnit, sourceUnitConversion);
						}
						Double existingFactor = sourceUnitConversion.get(targetUnit);
						if (existingFactor != null) {
							System.out.println("    WARNING: Double conversion from  '" + sourceUnit + "' to '" + targetUnit + "' found: " + existingFactor + " (old) and " + factor + " (new). Last one used.");
						}
						sourceUnitConversion.put(targetUnit, factor);
					}
				}
			}
			
			System.out.println("    Done");
		}
		else {
			System.out.println("    ERROR: No unit conversion file found.");
			status = STATE_EMPTY;
		}
	}
	
	
	private void createUnitConversionFile(InputFile sourceUnitMappingFile) {
		fileName = DrugMapping.getBasePath() + "/" + DEFAULT_FILENAME;
		String fieldDelimiterName = "Comma";
		String textQualifierName = "\"";
		
		if ((sourceUnitMappingFile != null) && (!sourceUnitMappingFile.getFileName().equals(""))) {
			fileName = sourceUnitMappingFile.getFileName();
			fieldDelimiterName = sourceUnitMappingFile.getFieldDelimiter();
			textQualifierName = sourceUnitMappingFile.getTextQualifier();
		}
		
		String fieldDelimiter = Character.toString(InputFile.fieldDelimiter(fieldDelimiterName));
		String textQualifier = Character.toString(InputFile.textQualifier(textQualifierName));

		System.out.println("    Write unit conversion map to file " + fileName + " ...");
		
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
			List<String> units = new ArrayList<String>();
			units.addAll(SourceDrug.getAllUnits());
			Collections.sort(units);
			
			// Write all units to file
			for (String unit : units) {
				String unitName = unit;
				if (unitName.contains(fieldDelimiter)) {
					if (unitName.contains(textQualifier)) {
						unitName.replaceAll(textQualifier, textQualifier + textQualifier);
					}
					unitName = textQualifier + unitName + textQualifier;
				}
				
				String record = unitName;
				record += fieldDelimiter + SourceDrug.getUnitSourceDrugUsage(unit);
				record += fieldDelimiter + SourceDrug.getUnitRecordUsage(unit);
				record += fieldDelimiter;
				record += fieldDelimiter;
				record += fieldDelimiter;
				
				mappingFileWriter.println(record);
			}
			
			mappingFileWriter.close();
			
			System.out.println("    Done");
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
