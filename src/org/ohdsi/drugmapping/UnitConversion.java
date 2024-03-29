package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.gui.files.DelimitedInputFileGUI;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class UnitConversion {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	private static String DEFAULT_FILENAME = "Unit Mapping File.csv";
	
	
	private int status = STATE_OK;
	private String fileName = "";
	
	private Map<String, Map<String, Double>> unitConversionMap = new HashMap<String, Map<String, Double>>();
	
	
	public static String getDefaultFileName() {
		return DrugMapping.getBasePath() + File.separator + DEFAULT_FILENAME;
	}
	
	
	public UnitConversion(DelimitedInputFileGUI sourceUnitMappingFile) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Create Units Conversion Map ...");
		
		readUnitConversionFile(sourceUnitMappingFile);
		if (status == STATE_EMPTY) {
			createUnitConversionFile(sourceUnitMappingFile);
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	private void readUnitConversionFile(DelimitedInputFileGUI sourceUnitMappingFile) {
		if ((sourceUnitMappingFile != null) && sourceUnitMappingFile.openFileForReading(true)) {
			fileName = sourceUnitMappingFile.getFileName();
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Get unit conversion map from file " + fileName + " ...");
			while (sourceUnitMappingFile.hasNext()) {
				DelimitedFileRow row = sourceUnitMappingFile.next();
				
				String sourceUnit = DrugMappingStringUtilities.removeExtraSpaces(sourceUnitMappingFile.get(row, "SourceUnit", true));
				//String drugCountString = sourceUnitMappingFile.get(row, "DrugCount", false);
				//String recordCountString = sourceUnitMappingFile.get(row, "RecordCount", false);
				String factorString = sourceUnitMappingFile.get(row, "Factor", true);
				String targetUnit = DrugMappingStringUtilities.safeToUpperCase(DrugMappingStringUtilities.removeExtraSpaces(sourceUnitMappingFile.get(row, "TargetUnit", true)));
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
					GenericMapping.addWarning(GenericMapping.UNIT_MAPPING_WARNING, "No factor found for conversion from  '" + sourceUnit + "' to '" + targetUnit + "'. Defaults to 1.0.");
					factor = 1.0;
				}
				
				if (factor != null) {
					if ((!sourceUnit.equals("")) && (targetUnit.equals(""))) {
						GenericMapping.addWarning(GenericMapping.UNIT_MAPPING_WARNING, "No target unit specified for '" + sourceUnit + ". Defaults to source unit with factor 1.0.");
					}
					else {
						Map<String, Double> sourceUnitConversion = unitConversionMap.get(sourceUnit);
						if (sourceUnitConversion == null) {
							sourceUnitConversion = new HashMap<String, Double>();
							unitConversionMap.put(sourceUnit, sourceUnitConversion);
						}
						Double existingFactor = sourceUnitConversion.get(targetUnit);
						if (existingFactor != null) {
							GenericMapping.addWarning(GenericMapping.UNIT_MAPPING_WARNING, "Double conversion from  '" + sourceUnit + "' to '" + targetUnit + "' found: " + existingFactor + " (old) and " + factor + " (new). Last one used.");
						}
						sourceUnitConversion.put(targetUnit, factor);
					}
				}
			}
			
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Done");
		}
		else {
			System.out.println("    ERROR: No unit conversion file found.");
			status = STATE_EMPTY;
		}
	}
	
	
	private void createUnitConversionFile(DelimitedInputFileGUI sourceUnitMappingFile) {
		fileName = getDefaultFileName();
		String fieldDelimiterName = "Comma";
		String textQualifierName = "\"";
		boolean newFile = true;
		
		if ((sourceUnitMappingFile != null) && (sourceUnitMappingFile.getFileName() != null) && (!sourceUnitMappingFile.getFileName().equals(""))) {
			fileName = sourceUnitMappingFile.getFileName();
			fieldDelimiterName = sourceUnitMappingFile.getFieldDelimiter();
			textQualifierName = sourceUnitMappingFile.getTextQualifier();
			newFile = false;
		}
		
		String fieldDelimiter = Character.toString(DelimitedInputFileGUI.fieldDelimiter(fieldDelimiterName));
		String textQualifier = Character.toString(DelimitedInputFileGUI.textQualifier(textQualifierName));

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Write unit conversion map to file " + fileName + " ...");
		
		File mappingFile = new File(fileName);
		try {
			PrintWriter mappingFileWriter = new PrintWriter(mappingFile);
			
			String header =            (newFile ? "SourceUnit"  : sourceUnitMappingFile.getColumnMapping().get("SourceUnit"));
			header += fieldDelimiter + (newFile ? "DrugCount"   : sourceUnitMappingFile.getColumnMapping().get("DrugCount"));
			header += fieldDelimiter + (newFile ? "RecordCount" : sourceUnitMappingFile.getColumnMapping().get("RecordCount"));
			header += fieldDelimiter + (newFile ? "Factor"      : sourceUnitMappingFile.getColumnMapping().get("Factor"));
			header += fieldDelimiter + (newFile ? "TargetUnit"  : sourceUnitMappingFile.getColumnMapping().get("TargetUnit"));
			header += fieldDelimiter + (newFile ? "Comment"     : sourceUnitMappingFile.getColumnMapping().get("Comment"));
			
			mappingFileWriter.println(header);
			
			// Get all units
			List<String> sortedUnits = new ArrayList<String>();
			sortedUnits.addAll(Source.getAllUnits());
			Collections.sort(sortedUnits);
			
			// Write all units to file
			for (String unit : sortedUnits) {
				
				String record = DrugMappingStringUtilities.escapeFieldValue(unit, fieldDelimiter, textQualifier);
				record += fieldDelimiter + Source.getUnitSourceDrugUsage(unit);
				record += fieldDelimiter + Source.getUnitRecordUsage(unit);
				record += fieldDelimiter;
				record += fieldDelimiter;
				record += fieldDelimiter;
				
				mappingFileWriter.println(record);
			}
			
			mappingFileWriter.close();
			
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Done");
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
			else if (sourceUnit.equals(targetUnit)) {
				result = sourceValue;
			}
		}
		return result;
	}

}
