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
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class IngredientNameTranslation {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	private static String DEFAULT_FILENAME = "DrugMapping - IngredientNameTranslationMap.csv";
	
	
	private int status = STATE_OK;
	private String fileName = "";
	
	private Map<SourceIngredient, String> ingredientNameTranslationMap = new HashMap<SourceIngredient, String>();
	
	
	public static String getDefaultFileName() {
		return DrugMapping.getBasePath() + "/" + DEFAULT_FILENAME;
	}
	
	
	public IngredientNameTranslation(DelimitedInputFileGUI ingredientNameTranslationFile) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Create Ingredient Name Translation Map ...");
		
		if ((ingredientNameTranslationFile != null) && ingredientNameTranslationFile.isSelected()) {
			readIngredientNameTranslationFile(ingredientNameTranslationFile);
			if (status == STATE_EMPTY) {
				createIngredientNameTranslationFile(ingredientNameTranslationFile);
			}
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	private void readIngredientNameTranslationFile(DelimitedInputFileGUI ingredientNameTranslationFile) {
		if ((ingredientNameTranslationFile != null) && ingredientNameTranslationFile.openFileForReading(true)) {
			fileName = ingredientNameTranslationFile.getFileName();
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Get ingredient name translation map from file " + fileName + " ...");
			while (ingredientNameTranslationFile.hasNext()) {
				DelimitedFileRow row = ingredientNameTranslationFile.next();

				String ingredientCode = ingredientNameTranslationFile.get(row, "IngredientCode", true);
				String ingredientName = ingredientNameTranslationFile.get(row, "IngredientName", false);
				
				SourceIngredient ingredient = Source.getIngredient(ingredientCode);
				if (ingredient != null) {
					ingredientName = ingredientName == null ? "" : DrugMappingStringUtilities.safeToUpperCase(ingredientName);
					String ingredientNameEnglish = ingredientNameTranslationFile.get(row, "IngredientNameEnglish", true);
					ingredientNameEnglish = ingredientNameEnglish == null ? "" : DrugMappingStringUtilities.safeToUpperCase(ingredientNameEnglish);
					
					if ((!ingredientName.equals("")) && (!ingredientNameEnglish.equals(""))) {
						String translation = ingredientNameTranslationMap.get(ingredient);
						if (translation != null) {
							if (translation.equals(ingredientNameEnglish)) {
								GenericMapping.addWarning(GenericMapping.TRANSLATION_WARNING, "Double translation definition for '" + ingredientCode + " (" + ingredientName + ")'. Ignored.");
							}
							else {
								System.out.println("    ERROR: Conflicting translations for '" + ingredientCode + " (" + ingredientName + ")'.");
								status = STATE_ERROR;
							}
						}
						else {
							ingredientNameTranslationMap.put(ingredient, ingredientNameEnglish);
						}
					}
				}
				else {
					GenericMapping.addWarning(GenericMapping.TRANSLATION_WARNING, "Unknown ingredient '" + ingredientCode + " (" + ingredientName + ")'. Ignored.");
				}
			}
			
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Done");
		}
		else {
			System.out.println("    ERROR: No ingredient name translation file found.");
			status = STATE_EMPTY;
		}
	}
	
	
	private void createIngredientNameTranslationFile(DelimitedInputFileGUI ingredientNameTranslationFile) {
		fileName = getDefaultFileName();
		String fieldDelimiterName = "Comma";
		String textQualifierName = "\"";
		
		if ((ingredientNameTranslationFile != null) && (!ingredientNameTranslationFile.getFileName().equals(""))) {
			fileName = ingredientNameTranslationFile.getFileName();
			fieldDelimiterName = ingredientNameTranslationFile.getFieldDelimiter();
			textQualifierName = ingredientNameTranslationFile.getTextQualifier();
		}
		
		String fieldDelimiter = Character.toString(DelimitedInputFileGUI.fieldDelimiter(fieldDelimiterName));
		String textQualifier = Character.toString(DelimitedInputFileGUI.textQualifier(textQualifierName));

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Write ingredient name translation map to file " + fileName + " ...");
		
		File mappingFile = new File(fileName);
		try {
			PrintWriter mappingFileWriter = new PrintWriter(mappingFile);
			
			String header = ingredientNameTranslationFile.getColumnMapping().get("IngredientCode");
			header += fieldDelimiter + ingredientNameTranslationFile.getColumnMapping().get("IngredientName");
			header += fieldDelimiter + ingredientNameTranslationFile.getColumnMapping().get("IngredientNameEnglish");
			
			mappingFileWriter.println(header);
			
			// Get all ingredient names
			List<SourceIngredient> sortedIngredients = new ArrayList<SourceIngredient>();
			sortedIngredients.addAll(Source.getAllIngredients());
			Collections.sort(sortedIngredients);
			
			// Write all units to file
			for (SourceIngredient ingredient : sortedIngredients) {
				if (ingredient.getIngredientNameEnglish().equals("")) {
					String record = DrugMappingStringUtilities.escapeFieldValue(ingredient.getIngredientCode(), fieldDelimiter, textQualifier);
					record += fieldDelimiter + DrugMappingStringUtilities.escapeFieldValue(ingredient.getIngredientName(), fieldDelimiter, textQualifier);
					record += fieldDelimiter;
					
					mappingFileWriter.println(record);
				}
			}
			
			mappingFileWriter.close();
			
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Done");
		} 
		catch (FileNotFoundException exception) {
			System.out.println("    ERROR: Cannot create ingredient name translation file '" + fileName + "'.");
			status = STATE_ERROR;
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public String getNameIngredientNameEnglish(SourceIngredient ingredient) {
		return ingredientNameTranslationMap.get(ingredient);
	}

}
