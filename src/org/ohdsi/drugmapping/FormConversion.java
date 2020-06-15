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
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class FormConversion {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	public static String FILENAME = "DrugMapping - FormConversionMap.csv";
	
	private CDM cdm = null;
	
	private String formMapDate = null;
	private List<String> sourceFormNames = new ArrayList<String>();                                            // List of source form names for sorting
	private Map<String, List<String>> formConversionMap = new HashMap<String, List<String>>();                   // Map from Source form to CDM form concept_name
	
	private int status = STATE_EMPTY;
	private Set<String> oldSourceForms = new HashSet<String>();
	private Set<String> oldCDMForms = new HashSet<String>();
	
	
	public FormConversion(Set<String> sourceForms, CDM cdm) {
		this.cdm = cdm;
		System.out.println(DrugMapping.getCurrentTime() + " Create Forms Conversion Map ...");
		
		sourceFormNames.addAll(sourceForms);
		Collections.sort(sourceFormNames); 
		
		System.out.println("    Done");

		readFromFile();
		if (status == STATE_NOT_FOUND) {
			System.out.println("    Creating empty form conversion map ...");
			writeFormConversionsToFile();
			status = STATE_EMPTY;
			System.out.println("    Done");
		}
		if (status == STATE_CRITICAL) {
			System.out.println("    Creating new form conversion map ...");
			writeFormConversionsToFile();
			System.out.println("    Done");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Done");
	}
	
	
	private void readFromFile() {
		System.out.println("    Get form conversion map from file " + DrugMapping.getBasePath() + "/" + FILENAME + " ...");

		boolean newSourceForms = false;
		boolean newCDMForms = false;
		boolean conceptNamesRead = false;
		
		File formFile = new File(DrugMapping.getBasePath() + "/" + FILENAME);
		if (formFile.exists() && formFile.canRead()) {
			status = STATE_OK;
			
			try {
				ReadCSVFileWithHeader formConversionFile = new ReadCSVFileWithHeader(DrugMapping.getBasePath() + "/" + FILENAME, ',', '"');
				
				Iterator<Row> formConversionFileIterator = formConversionFile.iterator();
				Set<String> formConcepts = formConversionFile.getColumns();
				if (formConcepts != null) {
					while (formConversionFileIterator.hasNext()) {
						Row row = formConversionFileIterator.next();
						
						if (!conceptNamesRead) {
							conceptNamesRead = true;
							formMapDate = row.get("Local form \\ CDM form", true).replace('/', '-');
						}
						else {
							String sourceForm = row.get("Local form \\ CDM form", true);
							if (!sourceForm.trim().equals("")) {
								oldSourceForms.add(sourceForm);
								
								if ((!sourceFormNames.contains(sourceForm)) && (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS))) {
									System.out.println("    WARNING: Source form '" + sourceForm + "' no longer exists!");
									sourceFormNames.add(sourceForm);
								}
								
								String mappingLine = "        " + sourceForm;

								List<Integer> priorities = new ArrayList<Integer>();
								Map<Integer, String> priorityMap = new HashMap<Integer, String>();
								List<String> sourceFormMapping = new ArrayList<String>();
								formConversionMap.put(sourceForm, sourceFormMapping);
								
								for (String concept_id : formConcepts) {
									oldCDMForms.add(concept_id);
									if (!concept_id.equals("Local form \\ CDM form")) {
										if (cdm.getCDMFormConceptIdToNameMap().keySet().contains(concept_id)) {
											String cell = row.get(concept_id, true).trim();
											if (!cell.equals("")) {
												try {
													Integer priority = Integer.parseInt(cell);
													priorities.add(priority);
													priorityMap.put(priority, concept_id);
												}
												catch (NumberFormatException exception) {
													// Ignore
												}
											}
										}
										else if (!DrugMapping.settings.getBooleanSetting(MainFrame.SUPPRESS_WARNINGS)) {
											System.out.println("    WARNING: CDM form '" + cdm.getCDMFormConceptIdToNameMap().get(concept_id) + "' (" + concept_id + ") no longer exists!");
										}
									}
								}
								Collections.sort(priorities);
								for (Integer priority : priorities) {
									String concept_id = priorityMap.get(priority);
									sourceFormMapping.add(concept_id);
									mappingLine += "=" + concept_id + ",\"" + cdm.getCDMFormConceptIdToNameMap().get(concept_id) + "\"";
								}
								System.out.println(mappingLine);
							}
						}
					}
					
					for (String sourceForm : sourceFormNames) {
						if (!oldSourceForms.contains(sourceForm)) {
							if (!newSourceForms) {
								System.out.println();
								System.out.println("    NEW SOURCE FORMS FOUND:");
							}
							System.out.println("        " + sourceForm);
							newSourceForms = true;
						}
					}

					for (String cdmForm : cdm.getCDMFormConceptIdToNameMap().keySet()) {
						if (!oldCDMForms.contains(cdmForm)) {
							if (!newCDMForms) {
								System.out.println();
								System.out.println("    NEW CDM FORMS FOUND:");
							}
							System.out.println("        " + cdmForm);
							newCDMForms = true;
						}
					}
					
					if (newSourceForms || newCDMForms) {
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
			System.out.println("    ERROR: No form conversion map found!");
			status = STATE_NOT_FOUND;
		}
		
		System.out.println("    Done");
	}
	
	
	private void writeFormConversionsToFile() {
		String formFileName = DrugMapping.getBasePath() + "/" + FILENAME;
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
				oldFormFileName = DrugMapping.getBasePath() + "/" + FILENAME.substring(0, FILENAME.length() - 4) + " " + formMapDate + " " + fileNrString + FILENAME.substring(FILENAME.length() - 4);
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

				String header1 = "Local form \\ CDM form";
				String header2 = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
				for (String concept_name : cdm.getCDMFormConceptNames()) {
					header1 += "," + cdm.getCDMFormConceptId(concept_name);
					header2 += "," + "\"" + concept_name + "\"";
				}
				formFileWriter.println(header1);
				formFileWriter.println(header2);
				Set<String> allSourceFormNamesSet = new HashSet<String>();
				allSourceFormNamesSet.addAll(sourceFormNames);
				allSourceFormNamesSet.addAll(oldSourceForms);
				List<String> allSourceFormNames = new ArrayList<String>();
				allSourceFormNames.addAll(allSourceFormNamesSet);
				Collections.sort(allSourceFormNames);
				for (String sourceFormName : allSourceFormNames) {
					String record = "\"" + sourceFormName + "\""; 
					List<String> sourceFormMap = formConversionMap.get(sourceFormName);
					if (sourceFormMap == null) {
						sourceFormMap = new ArrayList<String>();
					}
					for (String concept_name : cdm.getCDMFormConceptNames()) {
						String concept_id = cdm.getCDMFormConceptId(concept_name);
						record += "," + (sourceFormMap.contains(concept_id) ? Integer.toString(sourceFormMap.indexOf(concept_id) + 1) : "");
					}
					formFileWriter.println(record);
				}
				formFileWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create form conversion map '" + DrugMapping.getBasePath() + "/" + FILENAME + "'!");
				status = STATE_ERROR;
			}
		}
	}
	
	
	private void readFromFile2() {
		System.out.println("    Get form conversion map from file " + DrugMapping.getBasePath() + "/" + FILENAME + " ...");

		boolean newSourceForms = false;
		boolean newCDMForms = false;
		boolean conceptNamesRead = false;
		
		File formFile = new File(DrugMapping.getBasePath() + "/" + FILENAME);
		if (formFile.exists() && formFile.canRead()) {
			status = STATE_OK;
			
			try {
				ReadCSVFileWithHeader formConversionFile = new ReadCSVFileWithHeader(DrugMapping.getBasePath() + "/" + FILENAME, ',', '"');
				
				Iterator<Row> formConversionFileIterator = formConversionFile.iterator();
				Set<String> formConcepts = formConversionFile.getColumns();
				if (formConcepts != null) {
					while (formConversionFileIterator.hasNext()) {
						Row row = formConversionFileIterator.next();
						
						String sourceForm = row.get("DoseForm", true);
						String priorityString = row.get("Priority", true);
						String concept_id = row.get("ConceptId", true);
						String concept_name = row.get("ConceptName", false);

						if (!sourceForm.trim().equals("")) {
							oldSourceForms.add(sourceForm);
							
						}
						
					}
					
					for (String sourceForm : sourceFormNames) {
						if (!oldSourceForms.contains(sourceForm)) {
							if (!newSourceForms) {
								System.out.println();
								System.out.println("    NEW SOURCE FORMS FOUND:");
							}
							System.out.println("        " + sourceForm);
							newSourceForms = true;
						}
					}

					for (String cdmForm : cdm.getCDMFormConceptIdToNameMap().keySet()) {
						if (!oldCDMForms.contains(cdmForm)) {
							if (!newCDMForms) {
								System.out.println();
								System.out.println("    NEW CDM FORMS FOUND:");
							}
							System.out.println("        " + cdmForm);
							newCDMForms = true;
						}
					}
					
					if (newSourceForms || newCDMForms) {
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
			System.out.println("    ERROR: No form conversion map found!");
			status = STATE_NOT_FOUND;
		}
		
		System.out.println("    Done");
	}
	
	
	private void writeFormConversionsToFile2() {
		String formFileName = DrugMapping.getBasePath() + "/" + FILENAME;
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
				oldFormFileName = DrugMapping.getBasePath() + "/" + FILENAME.substring(0, FILENAME.length() - 4) + " " + formMapDate + " " + fileNrString + FILENAME.substring(FILENAME.length() - 4);
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

				String header1 = "Local form \\ CDM form";
				String header2 = (new SimpleDateFormat("yyyy-MM-dd")).format(new Date());
				for (String concept_name : cdm.getCDMFormConceptNames()) {
					header1 += "," + cdm.getCDMFormConceptId(concept_name);
					header2 += "," + "\"" + concept_name + "\"";
				}
				formFileWriter.println(header1);
				formFileWriter.println(header2);
				Set<String> allSourceFormNamesSet = new HashSet<String>();
				allSourceFormNamesSet.addAll(sourceFormNames);
				allSourceFormNamesSet.addAll(oldSourceForms);
				List<String> allSourceFormNames = new ArrayList<String>();
				allSourceFormNames.addAll(allSourceFormNamesSet);
				Collections.sort(allSourceFormNames);
				for (String sourceFormName : allSourceFormNames) {
					String record = "\"" + sourceFormName + "\""; 
					List<String> sourceFormMap = formConversionMap.get(sourceFormName);
					if (sourceFormMap == null) {
						sourceFormMap = new ArrayList<String>();
					}
					for (String concept_name : cdm.getCDMFormConceptNames()) {
						String concept_id = cdm.getCDMFormConceptId(concept_name);
						record += "," + (sourceFormMap.contains(concept_id) ? Integer.toString(sourceFormMap.indexOf(concept_id) + 1) : "");
					}
					formFileWriter.println(record);
				}
				formFileWriter.close();
			} catch (FileNotFoundException e) {
				System.out.println("    ERROR: Cannot create form conversion map '" + DrugMapping.getBasePath() + "/" + FILENAME + "'!");
				status = STATE_ERROR;
			}
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	//TODO Priorities
	public List<String> getMatchingForms(String sourceForm) {
		return sourceForm == null ? null : formConversionMap.get(sourceForm);
	}
	
	
	public boolean matches(String sourceForm, String cdmForm) {
		boolean matches = false;

		if ((sourceForm != null) && sourceFormNames.contains(sourceForm)) {
			if (cdmForm != null) {
				if (cdm.getCDMFormConceptNames().contains(cdmForm)) {
					if (cdm.getCDMFormNameToConceptIdMap().keySet().contains(cdmForm)) {
						cdmForm = cdm.getCDMFormConceptName(cdmForm);
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