package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ohdsi.drugmapping.cdm.CDM;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class FormConversion {
	public static int STATE_NOT_FOUND = 0;
	public static int STATE_EMPTY     = 1;
	public static int STATE_CRITICAL  = 2;
	public static int STATE_OK        = 3;
	public static int STATE_ERROR     = 4;
	
	private static String DEFAULT_FILENAME = "DrugMapping - FormConversionMap.csv";
	
	
	private int status = STATE_OK;
	private String fileName = "";
	private CDM cdm = null;
	
	private Map<String, List<String>> formConversionMap = new HashMap<String, List<String>>();
	
	
	public static String getDefaultFileName() {
		return DrugMapping.getBasePath() + "/" + DEFAULT_FILENAME;
	}
	
	
	public FormConversion(InputFile sourceFormMappingFile, CDM cdm) {
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Create Dose Forms Conversion Map ...");
		
		this.cdm = cdm;
		
		readFormConversionFile(sourceFormMappingFile);
		if (status == STATE_EMPTY) {
			createFormConversionFile(sourceFormMappingFile);
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     Done");
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	private void readFormConversionFile(InputFile sourceFormMappingFile) {
		Map<String, Map<Integer, String>> tempFormConversionMap = new HashMap<String, Map<Integer, String>>();
		if ((sourceFormMappingFile != null) && sourceFormMappingFile.openFile(true)) {
			fileName = sourceFormMappingFile.getFileName();
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Get form conversion map from file " + fileName + " ...");
			while (sourceFormMappingFile.hasNext()) {
				Row row = sourceFormMappingFile.next();
				
				String sourceForm = DrugMappingStringUtilities.removeExtraSpaces(sourceFormMappingFile.get(row, "DoseForm", true)).toUpperCase();
				String priorityString = sourceFormMappingFile.get(row, "Priority", false);
				String conceptId = sourceFormMappingFile.get(row, "ConceptId", false);
				//String conceptName = DrugMappingStringUtilities.removeExtraSpaces(sourceFormMappingFile.get(row, "ConceptName", true)).toUpperCase();
				//String comment = sourceFormMappingFile.get(row, "Comment", false);
				String conceptName = cdm.getCDMFormConceptName(conceptId);
				
				Integer priority = null;
				if (!priorityString.equals("")) {
					try {
						priority = Integer.parseInt(priorityString);
					}
					catch (NumberFormatException exception) {
						System.out.println("    ERROR: Illegal priority '" + priorityString + "' for '" + sourceForm + "' to '" + conceptName + "(" + conceptId + ")' conversion.");
						status = STATE_ERROR;
					}
				}
				else {
					GenericMapping.addWarning(GenericMapping.FORM_MAPPING_WARNING, "No priority found for conversion from  '" + sourceForm + "' to '" + conceptName + "(" + conceptId + ")'. Defaults to 0.");
					priority = 0;
				}
				
				if (priority != null) {
					Map<Integer, String> sourceFormConversion = tempFormConversionMap.get(sourceForm);
					if (sourceFormConversion == null) {
						sourceFormConversion = new HashMap<Integer, String>();
						tempFormConversionMap.put(sourceForm, sourceFormConversion);
					}
					
					if ((!sourceForm.equals("")) && (conceptId.equals(""))) {
						GenericMapping.addWarning(GenericMapping.FORM_MAPPING_WARNING, "No target form specified for '" + sourceForm + ". Defaults to source form.");
					}
					else {
						String existingConversion = sourceFormConversion.get(priority);
						if (existingConversion == null) {
							sourceFormConversion.put(priority, conceptName);
						}
						else {
							if (existingConversion.equals(conceptName)) {
								GenericMapping.addWarning(GenericMapping.FORM_MAPPING_WARNING, "Double definition found for conversion from  '" + sourceForm + "' to '" + conceptName + "(" + conceptId + ")'.");
							}
							else {
								System.out.println("    ERROR: Conflicting definition found for conversion from  '" + sourceForm + "'.");
								status = STATE_ERROR;
							}
						}
					}
				}
			}
			if (status == STATE_OK) {
				for (String sourceForm : tempFormConversionMap.keySet()) {
					Map<Integer, String> sourceFormConversion = tempFormConversionMap.get(sourceForm);
					
					List<Integer> priorities = new ArrayList<Integer>();
					priorities.addAll(sourceFormConversion.keySet());
					Collections.sort(priorities);
					
					List<String> orderedTargetForms = new ArrayList<String>();
					for (Integer priority : priorities) {
						orderedTargetForms.add(sourceFormConversion.get(priority));
					}
					formConversionMap.put(sourceForm, orderedTargetForms);
				}
			}
			
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Done");
		}
		else {
			System.out.println("    ERROR: No dose form conversion file found.");
			status = STATE_EMPTY;
		}
	}
	
	
	private void createFormConversionFile(InputFile sourceFormMappingFile) {
		fileName = getDefaultFileName();
		String fieldDelimiterName = "Comma";
		String textQualifierName = "\"";
		
		if ((sourceFormMappingFile != null) && (!sourceFormMappingFile.getFileName().equals(""))) {
			fileName = sourceFormMappingFile.getFileName();
			fieldDelimiterName = sourceFormMappingFile.getFieldDelimiter();
			textQualifierName = sourceFormMappingFile.getTextQualifier();
		}
		
		String fieldDelimiter = Character.toString(InputFile.fieldDelimiter(fieldDelimiterName));
		String textQualifier = Character.toString(InputFile.textQualifier(textQualifierName));

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Write dose form conversion map to file " + fileName + " ...");
		
		File mappingFile = new File(fileName);
		try {
			PrintWriter mappingFileWriter = new PrintWriter(mappingFile);
			
			String header = sourceFormMappingFile.getColumnMapping().get("DoseForm");
			header += fieldDelimiter + sourceFormMappingFile.getColumnMapping().get("Priority");
			header += fieldDelimiter + sourceFormMappingFile.getColumnMapping().get("ConceptId");
			header += fieldDelimiter + sourceFormMappingFile.getColumnMapping().get("ConceptName");
			header += fieldDelimiter + sourceFormMappingFile.getColumnMapping().get("Comment");
			
			mappingFileWriter.println(header);
			
			// Get all forms
			List<String> forms = new ArrayList<String>();
			forms.addAll(Source.getAllForms());
			Collections.sort(forms);
			
			// Write all forms to file
			for (String form : forms) {
				String formName = form;
				
				String record = DrugMappingStringUtilities.escapeFieldValue(formName, fieldDelimiter, textQualifier);
				record += fieldDelimiter;
				record += fieldDelimiter;
				record += fieldDelimiter;
				record += fieldDelimiter;
				
				mappingFileWriter.println(record);
			}
			
			mappingFileWriter.close();
			
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "        Done");
		} 
		catch (FileNotFoundException exception) {
			System.out.println("    ERROR: Cannot create dose form mapping file '" + fileName + "'.");
			status = STATE_ERROR;
		}
	}
	
	
	public int getStatus() {
		return status;
	}
	
	
	public List<String> getMatchingForms(String sourceForm) {
		List<String> targetForms = formConversionMap.get(sourceForm);
		if (targetForms == null) {
			targetForms = new ArrayList<String>();
		}
		if (targetForms.size() == 0) {
			targetForms.add(sourceForm);
		}
		return targetForms;
	}

}
