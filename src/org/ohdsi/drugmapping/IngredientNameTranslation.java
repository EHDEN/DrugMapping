package org.ohdsi.drugmapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class IngredientNameTranslation {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	private static String DEFAULT_FILENAME = "DrugMapping - IngredientNameTranslationMap.csv";
	
	
	private int status = STATE_OK;
	private String fileName = "";
	
	private Map<String, String> ingredientNameTranslationMap = new HashMap<String, String>();
	
	
	public static String getDefaultFileName() {
		return DrugMapping.getBasePath() + "/" + DEFAULT_FILENAME;
	}
	
	
	public IngredientNameTranslation(InputFile ingredientNameTranslationFile) {
		System.out.println(DrugMapping.getCurrentTime() + "     Create Ingredient Name Translation Map ...");
		
		readIngredientNameTranslationFile(ingredientNameTranslationFile);
		if (status == STATE_EMPTY) {
			createIngredientNameTranslationFile(ingredientNameTranslationFile);
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	private void readIngredientNameTranslationFile(InputFile ingredientNameTranslationFile) {
		if ((ingredientNameTranslationFile != null) && ingredientNameTranslationFile.openFile(true)) {
			fileName = ingredientNameTranslationFile.getFileName();
			System.out.println(DrugMapping.getCurrentTime() + "        Get ingredient name translation map from file " + fileName + " ...");
			while (ingredientNameTranslationFile.hasNext()) {
				Row row = ingredientNameTranslationFile.next();
				
				String ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientNameTranslationFile.get(row, "IngredientName", true)).toUpperCase();
				String ingredientNameEnglish = DrugMappingStringUtilities.removeExtraSpaces(ingredientNameTranslationFile.get(row, "IngredientNameEnglish", true)).toUpperCase();
				
				if ((!ingredientName.equals("")) && (!ingredientNameEnglish.equals(""))) {
					String translation = ingredientNameTranslationMap.get(ingredientName);
					if (translation != null) {
						if (translation.equals(ingredientNameEnglish)) {
							if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
								System.out.println("    WARNING: Double translation definition for '" + ingredientName + "'. Ignored.");
							}
						}
						else {
							System.out.println("    ERROR: Conflicting translations for '" + ingredientName + ".");
							status = STATE_ERROR;
						}
					}
					else {
						ingredientNameTranslationMap.put(ingredientName, ingredientNameEnglish);
					}
				}
			}
			
			System.out.println(DrugMapping.getCurrentTime() + "        Done");
		}
		else {
			System.out.println("    ERROR: No ingredient name translation file found.");
			status = STATE_EMPTY;
		}
	}
	
	
	private void createIngredientNameTranslationFile(InputFile ingredientNameTranslationFile) {
		fileName = getDefaultFileName();
		String fieldDelimiterName = "Comma";
		String textQualifierName = "\"";
		
		if ((ingredientNameTranslationFile != null) && (!ingredientNameTranslationFile.getFileName().equals(""))) {
			fileName = ingredientNameTranslationFile.getFileName();
			fieldDelimiterName = ingredientNameTranslationFile.getFieldDelimiter();
			textQualifierName = ingredientNameTranslationFile.getTextQualifier();
		}
		
		String fieldDelimiter = Character.toString(InputFile.fieldDelimiter(fieldDelimiterName));
		String textQualifier = Character.toString(InputFile.textQualifier(textQualifierName));

		System.out.println(DrugMapping.getCurrentTime() + "        Write ingredient name translation map to file " + fileName + " ...");
		
		File mappingFile = new File(fileName);
		try {
			PrintWriter mappingFileWriter = new PrintWriter(mappingFile);
			
			String header = "IngredientName";
			header += fieldDelimiter + "IngredientNameEnglish";
			
			mappingFileWriter.println(header);
			
			// Get all ingredient names
			Set<String> uniqueIngredientNames = new HashSet<String>();
			for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
				uniqueIngredientNames.add(sourceIngredient.getIngredientName());
			}
			List<String> sortedIngredientNames = new ArrayList<String>();
			sortedIngredientNames.addAll(uniqueIngredientNames);
			Collections.sort(sortedIngredientNames);
			
			// Write all units to file
			for (String ingredientName : sortedIngredientNames) {
				
				String record = DrugMappingStringUtilities.escapeFieldValue(ingredientName, fieldDelimiter, textQualifier);
				record += fieldDelimiter;
				
				mappingFileWriter.println(record);
			}
			
			mappingFileWriter.close();
			
			System.out.println(DrugMapping.getCurrentTime() + "        Done");
		} 
		catch (FileNotFoundException exception) {
			System.out.println("    ERROR: Cannot create ingredient name translation file '" + fileName + "'.");
			status = STATE_ERROR;
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public String getNameIngredientNameEnglish(String ingredientName) {
		return ingredientNameTranslationMap.get(ingredientName);
	}

}
