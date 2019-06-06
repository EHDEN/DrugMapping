package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class UnitConversion {
	private String name = null;
	private Map<String, Double> conversionTable = new HashMap<String, Double>();
	
	
	public static Map<String, UnitConversion> readUnitConversionsFromFile(String unitsFileName) {
		Map<String, UnitConversion> unitConversionMap = new HashMap<String, UnitConversion>();

		File conceptsFile = new File(unitsFileName);
		if (conceptsFile.exists()) {
			ReadCSVFileWithHeader unitConceptsFile = new ReadCSVFileWithHeader(unitsFileName, ',', '"');
			Iterator<Row> unitConceptsFileIterator = unitConceptsFile.iterator();
			
			while (unitConceptsFileIterator.hasNext()) {
				Row row = unitConceptsFileIterator.next();
				String unitName     = row.get("LocalUnit").toUpperCase();
				
				if (!unitName.trim().equals("")) {
					UnitConversion unitConversion = new UnitConversion(unitName);
					int conversionNr = 1;
					while (row.hasField("concept_id_" + Integer.toString(conversionNr))) {
						String concept_id = row.get("concept_id_" + Integer.toString(conversionNr));
						String factor     = row.get("factor_" + Integer.toString(conversionNr));
						
						if ((concept_id != null) && (factor != null)) {
							concept_id = concept_id.trim();
							factor     = factor.trim();
							if ((!concept_id.equals("")) && (!factor.equals(""))) {
								unitConversion.addConversion(concept_id, factor);
							}
						}
						conversionNr++;
					}
					unitConversionMap.put(unitName, unitConversion);
				}
			}
		}
		
		return unitConversionMap;
	}
	
	
	public static boolean writeUnitConversionsToFile(Map<String, UnitConversion> unitConversionsMap, String unitsFileName) {
		boolean ok = true;

		try {
			List<String> unitRecords = new ArrayList<String>();
			int maxConversions = 5;
			List<String> unitsList = new ArrayList<String>(unitConversionsMap.keySet());
			Collections.sort(unitsList);
			for (String unitName : unitsList) {
				String record = unitName;
				if (unitConversionsMap.containsKey(unitName)) {
					record = unitConversionsMap.get(unitName).toString();
					maxConversions = Math.max(maxConversions, record.split(",").length - 1);
				}
				unitRecords.add(record);
			}

			PrintWriter gpkUnitsFile = new PrintWriter(new File(unitsFileName));
			
			String header = "LocalUnit";
			for (int conversionNr = 1; conversionNr <= maxConversions; conversionNr++) {
				header += "," + "concept_id_" + Integer.toString(conversionNr);
				header += "," + "factor_" + Integer.toString(conversionNr);
			}
			
			gpkUnitsFile.println(header);
			
			for (String record : unitRecords) {
				gpkUnitsFile.println(record);
			}
			
			gpkUnitsFile.close();
			
		} catch (FileNotFoundException e) {
			ok = false;
		}
		
		return ok;
	}
	
	
	public UnitConversion(String name) {
		this.name = name;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public Double getFactor(String concept_id) {
		return conversionTable.get(concept_id);
	}
	

	public void addConversion(String concept_id, String factor) {
		try {
			double conversionFactor = Double.parseDouble(factor);
			if (!conversionTable.containsKey(concept_id)) {
				conversionTable.put(concept_id, conversionFactor);
			}
			else {
				System.out.println("ERROR: Duplicate conversion for unit '" + name + "' to concept " + concept_id + " factor = " + factor);
			}
		}
		catch (NumberFormatException e) {
			System.out.println("ERROR: Non-numeric conversion factor for unit '" + name + "' to concept " + concept_id + " factor = " + factor);
		}
	}
	
	
	public boolean matches(double sourceValue, String cdmUnitConceptId, Double cdmValue) {
		boolean result = false;
		
		if (conversionTable.containsKey(cdmUnitConceptId)) {
			result = (sourceValue == (cdmValue * conversionTable.get(cdmUnitConceptId)));
		}
		
		return result;
	}
	
	
	public String toString() {
		String description;
		
		description = name;
		for (String concept_id : conversionTable.keySet()) {
	        DecimalFormat df = new DecimalFormat("#");
	        df.setMaximumFractionDigits(8);
	        String factor = df.format(conversionTable.get(concept_id));
			
			description += "," + concept_id;
			description += "," + factor;
		}
		
		return description;
	}
}
