package org.ohdsi.drugmapping;

import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class UnitConversion2 {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	
	private int status = STATE_EMPTY;
	
	
	public UnitConversion2(InputFile sourceUnitMappingFile) {
		if (sourceUnitMappingFile.openFile()) {
			while (sourceUnitMappingFile.hasNext()) {
				Row row = sourceUnitMappingFile.next();
				
				String sourceUnit = DrugMappingStringUtilities.removeExtraSpaces(row.get("SourceUnit", true).toUpperCase());
				String drugCountString = row.get("DrugCount", false);
				String recordCountString = row.get("RecordCount", false);
				String factorString = row.get("Factor", true);
				String targetUnit = DrugMappingStringUtilities.removeExtraSpaces(row.get("TargetUnit", true));
				String comment = row.get("Comment", false);
				
				Double factor = null;
				if (!factorString.equals("")) {
					try {
						factor = Double.parseDouble(factorString);
					}
					catch (NumberFormatException exception) {
						System.out.println("    ERROR: Illegal factor '" + factorString + "' for '" + sourceUnit + "' to '" + targetUnit + "' conversion!");
						status = STATE_ERROR;
					}
				}
			}
		}
	}

}
