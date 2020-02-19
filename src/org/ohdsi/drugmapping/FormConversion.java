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

public class FormConversion {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_NEW_UNITS = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	public static String FILENAME = "DrugMapping - FormConversionMap.csv";
	
	private String formMapDate = null;
	private Map<String, String> cdmFormNameToConceptIdMap = new HashMap<String, String>();                     // Map from CDM form concept_name to CDM form concept_id
	private Map<String, String> cdmFormConceptIdToNameMap = new HashMap<String, String>();                     // Map from CDM form concept_id to CDM form concept_name
	private List<String> cdmFormConceptNames = new ArrayList<String>();                                        // List of CDM form names for sorting
	private List<String> sourceFormNames = new ArrayList<String>();                                            // List of source form names for sorting
	private Map<String, Set<String>> formConversionMap = new HashMap<String, Set<String>>();                   // Map from Source form to CDM form concept_name
	
	private int status = STATE_EMPTY;
	
	
	public FormConversion(CDMDatabase database, Set<String> sourceForms) {
		System.out.println(DrugMapping.getCurrentTime() + " Create Forms Conversion Map ...");

		System.out.println("    Get CDM forms ...");
		
		sourceFormNames.addAll(sourceForms);
		Collections.sort(sourceFormNames);
		
		QueryParameters queryParameters = new QueryParameters();
		queryParameters.set("@vocab", database.getVocabSchema());
	
		// Connect to the database
		RichConnection connection = database.getRichConnection(this.getClass());
		
		// Get CDM Forms
		for (Row queryRow : connection.queryResource("cdm/GetCDMForms.sql", queryParameters)) {
			
			String concept_id   = queryRow.get("concept_id").trim();
			String concept_name = queryRow.get("concept_name").trim();
			
			cdmFormNameToConceptIdMap.put(concept_name, concept_id);
			cdmFormConceptIdToNameMap.put(concept_id, concept_name);
			if (!cdmFormConceptNames.contains(concept_name)) {
				cdmFormConceptNames.add(concept_name);
			}
		}
		
		// Close database connection
		connection.close();
		
		Collections.sort(cdmFormConceptNames);
		
		//for (String concept_name : cdmFormConceptNames) {
		//	System.out.println("        " + cdmFormNameToConceptIdMap.get(concept_name) + "," + concept_name);
		//} 
		
		System.out.println("    Done");

		readFromFile();
		if (status == STATE_NOT_FOUND) {
			System.out.println("    Creating empty form conversion map ...");
			writeFormConversionsToFile();
			status = STATE_EMPTY;
			System.out.println("    Done");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Done");
	}
	
	
	private void readFromFile() {
		System.out.println("    Get form conversion map from file " + DrugMapping.getCurrentPath() + "/" + FILENAME + " ...");

		boolean newForms = false;
		boolean lostForms = false;
		boolean conceptNamesRead = false;
		Set<String> oldSourceForms = new HashSet<String>();
		Set<String> oldCDMForms = new HashSet<String>();
		
		File formFile = new File(DrugMapping.getCurrentPath() + "/" + FILENAME);
		if (formFile.exists()) {
			ReadCSVFileWithHeader formConversionFile = new ReadCSVFileWithHeader(DrugMapping.getCurrentPath() + "/" + FILENAME, ',', '"');
			
			Iterator<Row> formConversionFileIterator = formConversionFile.iterator();
			Set<String> formConcepts = formConversionFile.getColumns();
			if (formConcepts != null) {
				while (formConversionFileIterator.hasNext()) {
					Row row = formConversionFileIterator.next();
					
					if (!conceptNamesRead) {
						conceptNamesRead = true;
						formMapDate = row.get("Local form").replace('/', '-');
					}
					else {
						String sourceForm = row.get("Local form");
						if (!sourceForm.trim().equals("")) {
							oldSourceForms.add(sourceForm);
							
							if (!sourceFormNames.contains(sourceForm)) {
								System.out.println("    WARNING: Source form '" + sourceForm + "' no longer exists!");
								if (!sourceFormNames.contains(sourceForm)) {
									sourceFormNames.add(sourceForm);
								}
							}
							
							String mappingLine = "        " + sourceForm;
							
							Set<String> sourceFormMapping = formConversionMap.get(sourceForm);
							if (sourceFormMapping == null) {
								sourceFormMapping = new HashSet<String>();
								formConversionMap.put(sourceForm, sourceFormMapping);
							}
							for (String concept_id : formConcepts) {
								oldCDMForms.add(concept_id);
								if (!concept_id.equals("Local form")) {
									if (cdmFormConceptIdToNameMap.keySet().contains(concept_id)) {
										String cell = row.get(concept_id).trim();
										if (!cell.equals("")) {
											sourceFormMapping.add(concept_id);
											mappingLine += "=" + concept_id + ",\"" + cdmFormConceptIdToNameMap.get(concept_id) + "\"";
										}
									}
									else {
										System.out.println("    WARNING: Source form '" + cdmFormConceptIdToNameMap.get(concept_id) + "' (" + concept_id + ") no longer exists!");
										lostForms = true;
									}
								}
							}
							System.out.println(mappingLine);
						}
					}
				}
				
				for (String sourceForm : sourceFormNames) {
					if (!oldSourceForms.contains(sourceForm)) {
						newForms = true;
						break;
					}
				}
				
				if (!newForms) {
					for (String cdmForm : cdmFormConceptIdToNameMap.keySet()) {
						if (!oldCDMForms.contains(cdmForm)) {
							newForms = true;
						}
					}
				}
				
				if (newForms) {
					status = STATE_NEW_UNITS;
				}
				else {
					status = STATE_OK;
				}
				
				if (newForms || lostForms) {
					writeFormConversionsToFile();
				}
			}
		}
		else {
			System.out.println("    WARNING: No form conversion map found!");
			status = STATE_NOT_FOUND;
		}
		
		System.out.println("    Done");
	}
	
	
	private void writeFormConversionsToFile() {
		String formFileName = DrugMapping.getCurrentPath() + "/" + FILENAME;
		File formFile = new File(formFileName);
		if (formFile.exists()) {
			// Backup old form conversion map
			String oldFormFileName = null;
			File oldFormFile = null;
			int fileNr = 0;
			do {
				fileNr++;
				String fileNrString = "00" + Integer.toString(fileNr);
				fileNrString = fileNrString.substring(fileNrString.length() - 2);
				oldFormFileName = DrugMapping.getCurrentPath() + "/" + formMapDate + " " + fileNrString + " " + FILENAME;
				oldFormFile = new File(oldFormFileName);
			} while (oldFormFile.exists());
			try {
				PrintWriter oldFormFileWriter = new PrintWriter(new File(oldFormFileName));
				try {
					BufferedReader oldFormFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(formFileName)));
					String line;
					while ((line = oldFormFileReader.readLine()) != null) {
						oldFormFileWriter.println(line);
					}
					oldFormFileReader.close();
					oldFormFileWriter.close();
				}
				catch (FileNotFoundException e) {
					System.out.println("    ERROR: Cannot find original form conversion map '" + formFileName + "'!");
					status = STATE_ERROR;
				}
				catch (IOException e) {
					System.out.println("    ERROR: Reading original form conversion map '" + formFileName + "'!");
					status = STATE_ERROR;
				}
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create backup form conversion map '" + oldFormFileName + "'!");
				status = STATE_ERROR;
			}
		}

		if (status != STATE_ERROR) {
			try {
				PrintWriter formFileWriter = new PrintWriter(formFile);

				String header1 = "Local form";
				String header2 = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
				for (String concept_name : cdmFormConceptNames) {
					header1 += "," + cdmFormNameToConceptIdMap.get(concept_name);
					header2 += "," + "\"" + concept_name + "\"";
				}
				formFileWriter.println(header1);
				formFileWriter.println(header2);
				for (String sourceFormName : sourceFormNames) {
					String record = "\"" + sourceFormName + "\""; 
					Set<String> sourceFormMap = formConversionMap.get(sourceFormName);
					if (sourceFormMap == null) {
						sourceFormMap = new HashSet<String>();
					}
					for (String concept_name : cdmFormConceptNames) {
						String concept_id = cdmFormNameToConceptIdMap.get(concept_name);
						record += "," + (sourceFormMap.contains(concept_id) ? "X" : "");
					}
					formFileWriter.println(record);
				}
				formFileWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create form conversion map '" + DrugMapping.getCurrentPath() + "/" + FILENAME + "'!");
				status = STATE_ERROR;
			}
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public String getCDMFormConceptName(String cdmFromConceptId) {
		return cdmFormConceptIdToNameMap.get(cdmFromConceptId);
	}
	
	
	public boolean matches(String sourceForm, String cdmForm) {
		boolean matches = false;

		if ((sourceForm != null) && sourceFormNames.contains(sourceForm)) {
			if (cdmForm != null) {
				if (cdmFormConceptNames.contains(cdmForm)) {
					if (cdmFormNameToConceptIdMap.keySet().contains(cdmForm)) {
						cdmForm = cdmFormNameToConceptIdMap.get(cdmForm);
					}
					else {
						cdmForm = null;
					}
				}
			}
			if (cdmForm != null) {
				matches = formConversionMap.get(sourceForm).contains(cdmForm);
			}
		}
		
		return matches;
	}
}