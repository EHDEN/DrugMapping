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

import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class IngredientNameTranslation {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	public static String FILENAME = "DrugMapping - IngredientNameTranslationMap.csv";
	
	private List<String> sourceIngredientNames = new ArrayList<String>();                                      // List of source ingredient names for sorting
	private Map<String, String> ingredientNameTranslationMap = new HashMap<String, String>();                  // Map from Source ingredient name to english ingredient name
	
	private int status = STATE_EMPTY;
	private Set<String> oldIngredientNames = new HashSet<String>();
	
	
	public IngredientNameTranslation() {
		System.out.println(DrugMapping.getCurrentTime() + " Create ingredient name translation map ...");

		System.out.println("    Get source ingredient names ...");
		
		for (SourceIngredient sourceIngredient : SourceDrug.getAllIngredients()) {
			if (!sourceIngredientNames.contains(sourceIngredient.getIngredientName())) {
				sourceIngredientNames.add(sourceIngredient.getIngredientName());
			}
		}
		Collections.sort(sourceIngredientNames); 
		
		System.out.println("    Done");

		readFromFile();
		if (status == STATE_NOT_FOUND) {
			System.out.println("    Creating empty ingredient name translation map ...");
			writeIngredientNameTranslationToFile();
			status = STATE_EMPTY;
			System.out.println("    Done");
		}
		if (status == STATE_CRITICAL) {
			System.out.println("    Creating new ingredient name translation map ...");
			writeIngredientNameTranslationToFile();
			System.out.println("    Done");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Done");
	}
	
	
	private void readFromFile() {
		System.out.println("    Get ingredient name translation map from file " + DrugMapping.getBasePath() + "/" + FILENAME + " ...");

		boolean newSourceIngredientNames = false;
		
		File translationFile = new File(DrugMapping.getBasePath() + "/" + FILENAME);
		if (translationFile.exists()) {
			status = STATE_OK;
			
			try {
				ReadCSVFileWithHeader ingredientNameTranslationFile = new ReadCSVFileWithHeader(DrugMapping.getBasePath() + "/" + FILENAME, ',', '"');
				
				Iterator<Row> translationFileIterator = ingredientNameTranslationFile.iterator();

				while (translationFileIterator.hasNext()) {
					Row row = translationFileIterator.next();

					String sourceIngredientName = row.get("SourceIngredientName", true);
					String englishIngredientName = row.get("EnglishIngredientName", true);
					
					if ((!sourceIngredientName.trim().equals("")) && (!englishIngredientName.equals("<NEW>"))) {
						oldIngredientNames.add(sourceIngredientName);
						
						if ((!sourceIngredientNames.contains(sourceIngredientName)) && (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS))) {
							System.out.println("    WARNING: Source ingredient name '" + sourceIngredientName + "' no longer exists!");
							sourceIngredientNames.add(sourceIngredientName);
						}

						ingredientNameTranslationMap.put(sourceIngredientName, englishIngredientName);
						
						System.out.println("        " + sourceIngredientName + "=" + englishIngredientName);
					}
				}
				
				for (String sourceIngredientName : sourceIngredientNames) {
					if (!oldIngredientNames.contains(sourceIngredientName)) {
						if (!newSourceIngredientNames) {
							System.out.println();
							System.out.println("    NEW SOURCE INGREDIENT NAMES FOUND:");
						}
						System.out.println("        " + sourceIngredientName);
						newSourceIngredientNames = true;
					}
				}
				
				if (newSourceIngredientNames) {
					status = STATE_CRITICAL;
				}
				else {
					status = STATE_OK;
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
			}
		}
		else {
			System.out.println("    ERROR: No ingredient name translation map found!");
			status = STATE_NOT_FOUND;
		}
		
		System.out.println("    Done");
	}
	
	
	private void writeIngredientNameTranslationToFile() {
		String translationFileName = DrugMapping.getBasePath() + "/" + FILENAME;
		File translationFile = new File(translationFileName);
		if (translationFile.exists()) {
			// Backup old ingredient name translation map
			String oldFileName = null;
			File oldFile = null;
			int fileNr = 0;
			do {
				fileNr++;
				String fileNrString = "00" + Integer.toString(fileNr);
				fileNrString = fileNrString.substring(fileNrString.length() - 2);
				oldFileName = DrugMapping.getBasePath() + "/" + FILENAME.substring(0, FILENAME.length() - 4) + " Backup " + DrugMapping.getCurrentDate() + " " + fileNrString + FILENAME.substring(FILENAME.length() - 4);
				oldFile = new File(oldFileName);
			} while (oldFile.exists());
			try {
				PrintWriter oldTranslationFileWriter = new PrintWriter(new File(oldFileName));
				try {
					BufferedReader oldTranslationFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(translationFileName)));
					String line;
					while ((line = oldTranslationFileReader.readLine()) != null) {
						oldTranslationFileWriter.println(line);
					}
					oldTranslationFileReader.close();
					oldTranslationFileWriter.close();
				}
				catch (FileNotFoundException e) {
					System.out.println("    ERROR: Cannot find original ingredient name translation map '" + translationFileName + "'!");
					status = STATE_ERROR;
				}
				catch (IOException e) {
					System.out.println("    ERROR: Reading original ingredient name translation map '" + translationFileName + "'!");
					status = STATE_ERROR;
				}
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create backup ingredient name translation map '" + oldFileName + "'!");
				status = STATE_ERROR;
			}
		}

		if (status != STATE_ERROR) {
			try {
				PrintWriter translationFileWriter = new PrintWriter(translationFile);

				String header = "SourceIngredientName";
				header += "," + "EnglishIngredientName";
				translationFileWriter.println(header);

				Set<String> allIngredientNamesSet = new HashSet<String>();
				allIngredientNamesSet.addAll(sourceIngredientNames);
				allIngredientNamesSet.addAll(oldIngredientNames);
				List<String> allSourceIngredientNames = new ArrayList<String>();
				allSourceIngredientNames.addAll(allIngredientNamesSet);
				Collections.sort(allSourceIngredientNames);
				for (String sourceIngredientName : allSourceIngredientNames) {
					String englishIngredientName = ingredientNameTranslationMap.get(sourceIngredientName);
					if (!oldIngredientNames.contains(sourceIngredientName)) {
						englishIngredientName = "<NEW>";
					}
					String record = "\"" + sourceIngredientName + "\""; 
					record += "," + "\"" + englishIngredientName + "\"";
					translationFileWriter.println(record);
				}
				translationFileWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create ingredient name translation map '" + DrugMapping.getBasePath() + "/" + FILENAME + "'!");
				status = STATE_ERROR;
			}
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public String getEnglishName(String sourceIngredientName) {
		return ingredientNameTranslationMap.containsKey(sourceIngredientName) ? ingredientNameTranslationMap.get(sourceIngredientName) : "";
	}
}
