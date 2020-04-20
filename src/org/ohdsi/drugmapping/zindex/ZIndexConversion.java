package org.ohdsi.drugmapping.zindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class ZIndexConversion extends Mapping {
	private static final boolean IGNORE_EMPTY_GPK_NAMES   = false;
	private static final boolean IGNORE_STARRED_GPK_NAMES = false;
	
	private static final String DIGITS = "1234567890";
	private static final String NUMBER_CHARS = "1234567890,.";
	
	private static final int GPK_ColumnCount      = 12;
	private static final int GPK_GPKCode          =  0;
	private static final int GPK_MemoCode         =  1;
	private static final int GPK_LabelName        =  2;
	private static final int GPK_ShortName        =  3;
	private static final int GPK_FullName         =  4;
	private static final int GPK_ATCCode          =  5;
	private static final int GPK_GSKCode          =  6;
	private static final int GPK_DDDPerHPKUnit    =  7;
	private static final int GPK_PrescriptionDays =  8;
	private static final int GPK_HPKMG            =  9;
	private static final int GPK_HPKMGUnit        = 10;
	private static final int GPK_PharmForm        = 11;

	private static final int GSK_ColumnCount = 8;
	private static final int GSK_GSKCode     = 0;
	private static final int GSK_PartNumber  = 1;
	private static final int GSK_Type        = 2;
	private static final int GSK_Amount      = 3;
	private static final int GSK_AmountUnit  = 4;
	private static final int GSK_GNKCode     = 5;
	private static final int GSK_GenericName = 6;
	private static final int GSK_CASNumber   = 7;
	
	private static final int GNK_ColumnCount = 3;
	private static final int GNK_GNKCode     = 0;
	private static final int GNK_Description = 1;
	private static final int GNK_CASCode     = 2;

	private static final int GPKIPCI_ColumnCount = 10;
	private static final int GPKIPCI_GPKCode     = 0;
	private static final int GPKIPCI_PartNr      = 1;
	private static final int GPKIPCI_Type        = 2;
	private static final int GPKIPCI_Amount      = 3;
	private static final int GPKIPCI_AmountUnit  = 4;
	private static final int GPKIPCI_GNKCode     = 5;
	private static final int GPKIPCI_GNKName     = 6;
	private static final int GPKIPCI_CasNr       = 7;
	private static final int GPKIPCI_BaseName    = 8;
	private static final int GPKIPCI_Formula     = 9;
	
	private static final int OUTPUT_ColumnCount           = 14;
	private static final int OUTPUT_SourceCode            =  0;
	private static final int OUTPUT_SourceName            =  1;
	private static final int OUTPUT_SourceATCCode         =  2;
	private static final int OUTPUT_SourceFormulation     =  3;
	private static final int OUTPUT_SourceCount           =  4;
	private static final int OUTPUT_IngredientNameStatus  =  5;
	private static final int OUTPUT_IngredientCode        =  6;
	private static final int OUTPUT_IngredientName        =  7;
	private static final int OUTPUT_IngredientNameEnglish =  8;
	private static final int OUTPUT_Dosage                =  9;
	private static final int OUTPUT_DosageUnit            = 10;
	private static final int OUTPUT_OrgDosage             = 11;
	private static final int OUTPUT_OrgDosageUnit         = 12;
	private static final int OUTPUT_CASNumber             = 13;

	private boolean IPCIDerivation = false;
	private List<Integer> gpkList = new ArrayList<Integer>();
	private Map<Integer, String[]> gpkMap = new HashMap<Integer, String[]>();
	private Map<Integer, List<String[]>> gskMap = new HashMap<Integer, List<String[]>>();
	private Map<Integer, String[]> gnkMap = new HashMap<Integer, String[]>();
	private Map<String, Integer> gnkNameMap = new HashMap<String, Integer>();
	private Map<String, Integer> gpkStatisticsMap = new HashMap<String, Integer>();
	private List<String> wordsToRemove = new ArrayList<String>();
	private Map<String, String> ingredientNameTranslation = new HashMap<String, String>();
	private Map<String, List<String[]>> gpkIPCIMap = new HashMap<String, List<String[]>>();
	private Map<Integer, List<String[]>> outputMap = new HashMap<Integer, List<String[]>>();

	
	public ZIndexConversion(InputFile gpkFile, InputFile gskFile, InputFile gnkFile, InputFile gpkStatsFile, InputFile wordsToRemoveFile, InputFile ingredientNameTranslationFile, InputFile gpkIPCIFile) {
		
		boolean ok = true;
		
		String translationType = null; 

		System.out.println(DrugMapping.getCurrentTime() + " Converting ZIndex Files ...");
		
		
		// Load GSK file
		if (ok && gskFile.openFile()) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GSK File ...");
			while (gskFile.hasNext()) {
				Row row = gskFile.next();

				String[] record = new String[GSK_ColumnCount];
				record[GSK_GSKCode]       = gskFile.get(row, "GSKCode", true).trim();
				record[GSK_PartNumber]    = gskFile.get(row, "PartNumber", true).trim();
				record[GSK_Type]          = gskFile.get(row, "Type", true).trim();
				record[GSK_Amount]        = gskFile.get(row, "Amount", true).trim();
				record[GSK_AmountUnit]    = gskFile.get(row, "AmountUnit", true).trim();
				record[GSK_GNKCode]       = gskFile.get(row, "GNKCode", true).trim();
				record[GSK_GenericName]   = gskFile.get(row, "GenericName", true).trim();
				record[GSK_CASNumber]     = gskFile.get(row, "CASNumber", true).trim();

				record[GSK_CASNumber] = GenericMapping.uniformCASNumber(record[GSK_CASNumber]);

				int gskCode = Integer.valueOf(record[GSK_GSKCode]); 
				List<String[]> gskList = gskMap.get(gskCode);
				if (gskList == null) {
					gskList = new ArrayList<String[]>();
					gskMap.put(gskCode, gskList);
				}
				gskList.add(record);
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GSK file '" + gskFile.getFileName() + "'");
			ok = false;
		}
		
	
		// Load GNK file
		if (ok && gnkFile.openFile()) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GNK File ...");
			while (gnkFile.hasNext()) {
				Row row = gnkFile.next();

				String[] record = new String[GNK_ColumnCount];
				record[GNK_GNKCode]     = gnkFile.get(row, "GNKCode", true).trim();
				record[GNK_Description] = gnkFile.get(row, "Description", true).trim();
				record[GNK_CASCode]     = gnkFile.get(row, "CASCode", true).trim();

				record[GNK_CASCode] = GenericMapping.uniformCASNumber(record[GNK_CASCode]);

				int gnkCode = Integer.valueOf(record[GNK_GNKCode]);
				gnkMap.put(gnkCode, record);
				if (gnkNameMap.get(record[GNK_Description]) == null) {
					gnkNameMap.put(record[GNK_Description], Integer.valueOf(record[GNK_GNKCode]));
				}
				else {
					System.out.println("  ERROR: Duplicate GNK name '" + record[GNK_Description] + "' (" + gnkNameMap.get(record[GNK_Description]) + " <-> " + record[GNK_GNKCode] + ")");
					ok = false;
				}
				
				// Store ingredient name for translation
				if (!record[GNK_Description].equals("")) {
					ingredientNameTranslation.put(record[GNK_Description], null);
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && gpkStatsFile.openFile()) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GPK Statistics File ...");
			while (gpkStatsFile.hasNext()) {
				Row row = gpkStatsFile.next();

				String gpkCode  = gpkStatsFile.get(row, "GPKCode", true).trim();
				String gpkCount = gpkStatsFile.get(row, "GPKCount", true).trim();
				
				Integer count = null;
				try {
					count = Integer.valueOf(gpkCount);
					gpkStatisticsMap.put(gpkCode, count);
				}
				catch (NumberFormatException e) {
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && (wordsToRemoveFile != null) && wordsToRemoveFile.openFile(true)) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex Words To Ignore File ...");
			List<List<String>> wordsFoundToRemove = new ArrayList<List<String>>();
			while (wordsToRemoveFile.hasNext()) {
				Row row = wordsToRemoveFile.next();

				String word = wordsToRemoveFile.get(row, "Word", true).trim().toUpperCase();
				//System.out.println("    " + word);
				
				if (word != null) {
					if (wordsFoundToRemove.size() == 0) {
						List<String> equalWordsGroup = new ArrayList<String>();
						equalWordsGroup.add(word);
						wordsFoundToRemove.add(equalWordsGroup);
					}
					else {
						List<List<String>> wordsBeforeGroups = new ArrayList<List<String>>();
						List<List<String>> wordsAfterGroups = new ArrayList<List<String>>();
						List<List<String>> wordsEqualGroups = new ArrayList<List<String>>();
						
						for (List<String> equalWordsGroup : wordsFoundToRemove) {
							List<String> wordsBefore = new ArrayList<String>();
							List<String> wordsAfter = new ArrayList<String>();
							List<String> wordsEqual = new ArrayList<String>();

							wordsBeforeGroups.add(wordsBefore);
							wordsAfterGroups.add(wordsAfter);
							wordsEqualGroups.add(wordsEqual);
							
							for (String groupWord : equalWordsGroup) {
								int compare = compareWordsToRemove(word, groupWord);
								if (compare < 0) {
									wordsAfter.add(groupWord);
								}
								else if (compare > 0) {
									wordsBefore.add(groupWord);
								}
								else {
									wordsEqual.add(groupWord);
								}
							}
						}
						
						wordsFoundToRemove = new ArrayList<List<String>>();
						for (List<String> equalWordsGroup : wordsBeforeGroups) {
							if (equalWordsGroup.size() > 0) {
								wordsFoundToRemove.add(equalWordsGroup);
							}
						}
						List<String> firstEqualWordsGroup = null;
						for (List<String> equalWordsGroup : wordsEqualGroups) {
							if (equalWordsGroup.size() > 0) {
								wordsFoundToRemove.add(equalWordsGroup);
								firstEqualWordsGroup = equalWordsGroup;
							}
						}
						if (firstEqualWordsGroup != null) {
							firstEqualWordsGroup.add(word);
						}
						else {
							List<String> newEqualWordsGroup = new ArrayList<String>();
							newEqualWordsGroup.add(word);
							wordsFoundToRemove.add(newEqualWordsGroup);
						}
						for (List<String> equalWordsGroup : wordsAfterGroups) {
							if (equalWordsGroup.size() > 0) {
								wordsFoundToRemove.add(equalWordsGroup);
							}
						}
					}
				}				
			}
			// Collect the words in the wordsToRemove list
			for (List<String> equalWordsGroup : wordsFoundToRemove) {
				for (String word : equalWordsGroup) {
					wordsToRemove.add(word);
					System.out.println("    " + word);
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		else {
			System.out.println("  ERROR: Cannot load  Words To Ignore file '" + wordsToRemoveFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && gpkIPCIFile.openFile()) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GPK IPCI Compositions File ...");
			
			IPCIDerivation = true;
			
			while (gpkIPCIFile.hasNext()) {
				Row row = gpkIPCIFile.next();

				String[] record = new String[GPKIPCI_ColumnCount];
				record[GPKIPCI_GPKCode]    = gpkIPCIFile.get(row, "GPK", true).trim();
				record[GPKIPCI_PartNr]     = gpkIPCIFile.get(row, "PartNr", true).trim();
				record[GPKIPCI_Type]       = gpkIPCIFile.get(row, "Typ", true).trim();
				record[GPKIPCI_Amount]     = gpkIPCIFile.get(row, "Amount", true).trim();
				record[GPKIPCI_AmountUnit] = gpkIPCIFile.get(row, "AmountUnit", true).trim();
				record[GPKIPCI_GNKCode]    = gpkIPCIFile.get(row, "GNK", true).trim();
				record[GPKIPCI_GNKName]    = gpkIPCIFile.get(row, "GnkName", true).trim();
				record[GPKIPCI_CasNr]      = gpkIPCIFile.get(row, "CASNr", true).trim();
				record[GPKIPCI_BaseName]   = gpkIPCIFile.get(row, "BaseName", true).trim();
				record[GPKIPCI_Formula]    = gpkIPCIFile.get(row, "Formula", true).trim();
				
				List<String[]> gpkIPCIParts = gpkIPCIMap.get(record[GPKIPCI_GPKCode]);
				if (gpkIPCIParts == null) {
					gpkIPCIParts = new ArrayList<String[]>();
					gpkIPCIMap.put(record[GPKIPCI_GPKCode], gpkIPCIParts);
				}
				gpkIPCIParts.add(record);
				
				// Store ingredient name for translation
				if (!record[GPKIPCI_GNKName].equals("")) {
					ingredientNameTranslation.put(record[GPKIPCI_GNKName], null);
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		
		
		// Load GPK file
		if (ok && gpkFile.openFile()) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GPK File ...");
			while (gpkFile.hasNext()) {
				Row row = gpkFile.next();

				String[] gpk = new String[GPK_ColumnCount];
				gpk[GPK_GPKCode]          = gpkFile.get(row, "GPKCode", true).trim();
				gpk[GPK_MemoCode]         = gpkFile.get(row, "MemoCode", true).trim();
				gpk[GPK_LabelName]        = gpkFile.get(row, "LabelName", true).trim();
				gpk[GPK_ShortName]        = gpkFile.get(row, "ShortName", true).trim();
				gpk[GPK_FullName]         = gpkFile.get(row, "FullName", true).trim();
				gpk[GPK_ATCCode]          = gpkFile.get(row, "ATCCode", true).trim();
				gpk[GPK_GSKCode]          = gpkFile.get(row, "GSKCode", true).trim();
				gpk[GPK_DDDPerHPKUnit]    = gpkFile.get(row, "DDDPerHPKUnit", true).trim();
				gpk[GPK_PrescriptionDays] = gpkFile.get(row, "PrescriptionDays", true).trim();
				gpk[GPK_HPKMG]            = gpkFile.get(row, "HPKMG", true).trim().toUpperCase();
				gpk[GPK_HPKMGUnit]        = gpkFile.get(row, "HPKMGUnit", true).trim().toUpperCase();
				gpk[GPK_PharmForm]        = gpkFile.get(row, "PharmForm", true).trim().toUpperCase().replaceAll("\"", "\"\"").replaceAll("\"", "\"\"");

				String gpkCodeString = gpk[GPK_GPKCode];
				String labelName = gpk[GPK_LabelName];
				String shortName = gpk[GPK_ShortName];
				String fullName = gpk[GPK_FullName];
				String atcCode = gpk[GPK_ATCCode];
				String gskCodeString = gpk[GPK_GSKCode];
				String hpkAmount = gpk[GPK_HPKMG];
				String hpkUnit = gpk[GPK_HPKMGUnit];
				String pharmForm = gpk[GPK_PharmForm];

				Integer gpkCode = Integer.valueOf(gpk[GPK_GPKCode]);
				gpkList.add(gpkCode);
				gpkMap.put(gpkCode, gpk);
				
				String name = fullName;
				if (name.equals("")) name = labelName;
				if (name.equals("")) name = shortName;
				Integer gskCode = gskCodeString.equals("") ? null : Integer.valueOf(gskCodeString);
				
				List<String[]> gpkIPCIIngredients = gpkIPCIMap.get(gpkCodeString);
				if (gpkIPCIIngredients != null) {
					// Overruled by IPCI derivation
					List<String[]> outputIngredients = new ArrayList<String[]>();
					
					for (String[] gpkIPCIIngredient : gpkIPCIIngredients) {
						String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
						gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
						gpkIngredientRecord[OUTPUT_SourceName]            = name;
						gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
						gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
						gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
						gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "ZIndexIPCI";
						gpkIngredientRecord[OUTPUT_IngredientCode]        = gpkIPCIIngredient[GPKIPCI_GNKCode];
						gpkIngredientRecord[OUTPUT_IngredientName]        = gpkIPCIIngredient[GPKIPCI_GNKName];
						gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
						gpkIngredientRecord[OUTPUT_Dosage]                = gpkIPCIIngredient[GPKIPCI_Amount];
						gpkIngredientRecord[OUTPUT_DosageUnit]            = gpkIPCIIngredient[GPKIPCI_AmountUnit];
						gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
						gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
						gpkIngredientRecord[OUTPUT_CASNumber]             = GenericMapping.uniformCASNumber(gpkIPCIIngredient[GPKIPCI_CasNr]);

						if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
							ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
						}

						outputIngredients.add(gpkIngredientRecord);
					}
					
					outputMap.put(gpkCode, outputIngredients);
				}
				else {
					if (!IPCIDerivation) {
						// Ignore empty names and names that start with a '*'
						if ((IGNORE_EMPTY_GPK_NAMES || (!name.equals(""))) && (IGNORE_STARRED_GPK_NAMES || (!name.substring(0, 1).equals("*")))) {
							
							List<String[]> gskList = null;
							
							if (!gskCodeString.equals("")) {
								gskList = gskMap.get(gskCode);
								if (gskList != null) {
									// Remove non-active ingredients
									List<String[]> remove = new ArrayList<String[]>();
									for (String[] gskObject : gskList) {
										if (!gskObject[GSK_Type].equals("W")) {
											remove.add(gskObject);
										}
									}
									gskList.removeAll(remove);

									if (gskList.size() == 0) {
										gskList = null;
										System.out.println("    WARNING: No active ingredient GSK records (GSKCode = " + gskCodeString + ") found for GPK " + gpkCodeString);
									}
								}
							}
							
							if (gskList == null) {
								// Try to extract ingredients from name (separated by '/')
								// List of words to remove from extracted parts.
								// IMPORTANT:
								//   The List wordsToRemove is an ordered list. The words are removed in the order of the list.
								//   The appearance of the words are checked with surrounding parenthesis, with
								//   surrounding spaces, and at the end of the extracted part.
								
								//if (!name.equals("")) {
								//	if (name.substring(name.length() - 1).equals(",")) {
								//		name = name.substring(0, name.length() - 1);
								//	}
								if (!shortName.equals("")) {
									if (shortName.contains("/") || shortName.contains("+")) {
										String[] shortNameSplit = shortName.contains("/") ? shortName.split("/") :  shortName.split("\\+");
										String doseString = getDoseString(fullName);
										String[] doseStringSplit = doseString != null ? doseString.split("/") : null;
										String denominatorUnit = (((doseStringSplit != null) && (doseStringSplit.length > shortNameSplit.length)) ? doseStringSplit[shortNameSplit.length] : "").trim();
										
										List<String> ingredientNames = new ArrayList<String>();
										List<String> ingredientAmounts = new ArrayList<String>();
										List<String> ingredientAmountUnits = new ArrayList<String>();
										String lastAmountUnit = null;
										for (int ingredientNr = 0; ingredientNr < shortNameSplit.length; ingredientNr++) {
											String ingredientName = shortNameSplit[ingredientNr];
											ingredientName = cleanupExtractedIngredientName(ingredientName);
											if (ingredientName != null) {
												ingredientNames.add(ingredientName);
												
												String amount = "";
												String amountUnit = "";
												if (doseStringSplit != null) {
													if (ingredientNr < doseStringSplit.length) {
														String ingredientDoseString = doseStringSplit[ingredientNr];
														String numberChars = NUMBER_CHARS;
														for (int charNr = 0; charNr < ingredientDoseString.length(); charNr++) {
															if (numberChars.indexOf(ingredientDoseString.charAt(charNr)) < 0) {
																break;
															}
															amount += ingredientDoseString.charAt(charNr);
														}
														amount = amount.replace(",", ".");
														amountUnit = ingredientDoseString.substring(amount.length());
														if ((!amountUnit.equals("")) && (amountUnit.substring(0, 1).equals("-"))) { // Solve things like 5-WATER
															amount = "";
															amountUnit = "";
														}
													}
												}
												lastAmountUnit = amountUnit;
												ingredientAmounts.add(amount);
												ingredientAmountUnits.add(amountUnit);
											}
											else {
												ingredientNames.add(null);
												ingredientAmounts.add(null);
												ingredientAmountUnits.add(null);
											}
										}

										List<String[]> outputIngredients = new ArrayList<String[]>();
										for (int ingredientNr = 0; ingredientNr < shortNameSplit.length; ingredientNr++) {
											String ingredientName = ingredientNames.get(ingredientNr);
											
											if (ingredientName != null) {
												String amount = ingredientAmounts.get(ingredientNr);
												String amountUnit = ingredientAmountUnits.get(ingredientNr);
												if (amountUnit.equals("")) {
													amountUnit = lastAmountUnit;
												}
												amountUnit = amountUnit + (denominatorUnit.equals("") ? "" : "/" + denominatorUnit);
												
												Integer gnkCode = gnkNameMap.get(ingredientName);
												if (gnkCode != null) {
													String[] gnkRecord = gnkMap.get(gnkCode);
													
													String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
													gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
													gpkIngredientRecord[OUTPUT_SourceName]            = name;
													gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
													gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
													gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
													gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "Mapped to GNK";
													gpkIngredientRecord[OUTPUT_IngredientCode]        = gnkCode.toString();
													gpkIngredientRecord[OUTPUT_IngredientName]        = ingredientName.trim();
													gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
													gpkIngredientRecord[OUTPUT_Dosage]                = amount;
													gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
													gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
													gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
													gpkIngredientRecord[OUTPUT_CASNumber]             = gnkRecord[GNK_CASCode];

													if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
														ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
													}

													outputIngredients.add(gpkIngredientRecord);
												}
												else {
													String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
													gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
													gpkIngredientRecord[OUTPUT_SourceName]            = name;
													gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
													gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
													gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
													gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "Extracted";
													gpkIngredientRecord[OUTPUT_IngredientCode]        = "";
													gpkIngredientRecord[OUTPUT_IngredientName]        = ingredientName.trim();
													gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
													gpkIngredientRecord[OUTPUT_Dosage]                = amount;
													gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
													gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
													gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
													gpkIngredientRecord[OUTPUT_CASNumber]             = "";

													if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
														ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
													}

													outputIngredients.add(gpkIngredientRecord);
												}
											}
										}
										
										outputMap.put(gpkCode, outputIngredients);
									}
									else {
										List<String[]> outputIngredients = new ArrayList<String[]>();
										
										//name = cleanupExtractedIngredientName(name);
										String ingredientName = shortName;
										
										if (ingredientName != null) {
											String amount = "";
											String amountUnit = "";

											String doseString = getDoseString(fullName);
											String[] doseStringSplit = doseString != null ? doseString.split("/") : null;
											if (doseStringSplit != null) {
												String denominatorUnit = "";
												if (doseString.contains("/")) {
													doseString = doseStringSplit[0];
													denominatorUnit = (doseStringSplit.length > 1 ? doseStringSplit[1] : "").trim();
												}
												String numberChars = NUMBER_CHARS;
												for (int charNr = 0; charNr < doseString.length(); charNr++) {
													if (numberChars.indexOf(doseString.charAt(charNr)) < 0) {
														break;
													}
													amount += doseString.charAt(charNr);
												}
												amount = amount.replace(",", ".");
												amountUnit = doseString.substring(amount.length());
												if ((!amountUnit.equals("")) && (amountUnit.substring(0, 1).equals("-"))) { // Solve things like 5-WATER
													amount = "";
													amountUnit = "";
												}
												else {
													amountUnit = amountUnit + (denominatorUnit.equals("") ? "" : "/" + denominatorUnit);
												}
											}
											
											Integer gnkCode = gnkNameMap.get(ingredientName);
											if (gnkCode != null) {
												String[] gnkRecord = gnkMap.get(gnkCode);

												String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
												gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
												gpkIngredientRecord[OUTPUT_SourceName]            = name;
												gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
												gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
												gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
												gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "Mapped to GNK";
												gpkIngredientRecord[OUTPUT_IngredientCode]        = gnkCode.toString();
												gpkIngredientRecord[OUTPUT_IngredientName]        = ingredientName.trim();
												gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
												gpkIngredientRecord[OUTPUT_Dosage]                = amount;
												gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
												gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
												gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
												gpkIngredientRecord[OUTPUT_CASNumber]             = gnkRecord[GNK_CASCode];

												if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
													ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
												}

												outputIngredients.add(gpkIngredientRecord);
											}
											else {
												String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
												gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
												gpkIngredientRecord[OUTPUT_SourceName]            = name;
												gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
												gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
												gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
												gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "Extracted";
												gpkIngredientRecord[OUTPUT_IngredientCode]        = "";
												gpkIngredientRecord[OUTPUT_IngredientName]        = ingredientName.trim();
												gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
												gpkIngredientRecord[OUTPUT_Dosage]                = amount;
												gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
												gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
												gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
												gpkIngredientRecord[OUTPUT_CASNumber]             = "";

												if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
													ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
												}

												outputIngredients.add(gpkIngredientRecord);
											}
										}
										
										outputMap.put(gpkCode, outputIngredients);
									}
								}
							}
							else {
								List<String[]> outputIngredients = new ArrayList<String[]>();
								
								for (String[] gskObject : gskList) {
									String amount = gskObject[GSK_Amount];
									String amountUnit = gskObject[GSK_AmountUnit];
									// Extract unit from name
									if (name.lastIndexOf(" ") >= 0) {
										String strengthString = name.substring(name.lastIndexOf(" ")).trim();
										if (DIGITS.contains(strengthString.substring(0, 1))) {
											String strengthValueString = ""; 
											for (int charNr = 0; charNr < strengthString.length(); charNr++) {
												if (NUMBER_CHARS.contains(strengthString.subSequence(charNr, charNr + 1))) {
													strengthValueString += strengthString.subSequence(charNr, charNr + 1);
												}
												else {
													break;
												}
											}
											String strengthUnitString = strengthString.substring(strengthValueString.length()).trim();
											if (!
													(
															strengthUnitString.contains("1") ||
															strengthUnitString.contains("2") ||
															strengthUnitString.contains("3") ||
															strengthUnitString.contains("4") ||
															strengthUnitString.contains("5") ||
															strengthUnitString.contains("6") ||
															strengthUnitString.contains("7") ||
															strengthUnitString.contains("8") ||
															strengthUnitString.contains("9") ||
															strengthUnitString.contains("0") ||
															strengthUnitString.startsWith("-") ||
															(strengthUnitString.contains("(") && (!strengthUnitString.contains(")"))) ||
															((!strengthUnitString.contains("(")) && strengthUnitString.contains(")"))
													)
												) {
												try {
													Double.valueOf(strengthValueString);
													amount = strengthValueString;
													amountUnit = strengthUnitString;
												}
												catch (NumberFormatException e) {
													// Do nothing
												}
											}
										}
									}
									
									// When no amount is specified for the ingredient and it is the only ingredient and
									// a HPK amount is specified, use that as the amount.
									if (amount.equals("") && (gskList.size() == 1) && (!hpkAmount.equals("")) && (!hpkUnit.equals(""))) {
										amount = hpkAmount;
										amountUnit = hpkUnit;
									}
									
									String gnkCode = gskObject[GSK_GNKCode].trim();
									if (!gskObject[GSK_GenericName].substring(0, 1).equals("*")) {
										// Cleanup ZIndex ingredient name
										String genericName = cleanupExtractedIngredientName(gskObject[GSK_GenericName]);

										String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
										gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
										gpkIngredientRecord[OUTPUT_SourceName]            = name;
										gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
										gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
										gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
										gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "ZIndex";
										gpkIngredientRecord[OUTPUT_IngredientCode]        = gnkCode.toString();
										gpkIngredientRecord[OUTPUT_IngredientName]        = genericName != null ? genericName : gskObject[GSK_GenericName];
										gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
										gpkIngredientRecord[OUTPUT_Dosage]                = amount;
										gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
										gpkIngredientRecord[OUTPUT_OrgDosage]             = gskObject[GSK_Amount];
										gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = gskObject[GSK_AmountUnit];
										gpkIngredientRecord[OUTPUT_CASNumber]             = gskObject[GSK_CASNumber];

										if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
											ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
										}

										outputIngredients.add(gpkIngredientRecord);
									}
									else {
										String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
										gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
										gpkIngredientRecord[OUTPUT_SourceName]            = name;
										gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
										gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
										gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
										gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "ZIndex";
										gpkIngredientRecord[OUTPUT_IngredientCode]        = gnkCode.toString();
										gpkIngredientRecord[OUTPUT_IngredientName]        = "";
										gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
										gpkIngredientRecord[OUTPUT_Dosage]                = amount;
										gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
										gpkIngredientRecord[OUTPUT_OrgDosage]             = gskObject[GSK_Amount];
										gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = gskObject[GSK_AmountUnit];
										gpkIngredientRecord[OUTPUT_CASNumber]             = gskObject[GSK_CASNumber];

										outputIngredients.add(gpkIngredientRecord);
									}
								}
								
								outputMap.put(gpkCode, outputIngredients);
							}
						}
					}
					else {
						List<String[]> gskList = null;
						
						if (!gpkFile.get(row, "GSKCode", true).equals("")) {
							gskList = gskMap.get(gskCode);
							if (gskList != null) {
								// Remove non-active ingredients
								List<String[]> remove = new ArrayList<String[]>();
								for (String[] gskObject : gskList) {
									if (!gskObject[GSK_Type].equals("W")) {
										remove.add(gskObject);
									}
								}
								gskList.removeAll(remove);

								if (gskList.size() == 0) {
									gskList = null;
									System.out.println("    WARNING: No active ingredient GSK records (GSKCode = " + gskCodeString + ") found for GPK " + gpkCodeString);
								}
							}
						}

						if (gskList == null) {
							List<String[]> outputIngredients = new ArrayList<String[]>();

							String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
							gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
							gpkIngredientRecord[OUTPUT_SourceName]            = name;
							gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
							gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
							gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
							gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "ZIndex";
							gpkIngredientRecord[OUTPUT_IngredientCode]        = "";
							gpkIngredientRecord[OUTPUT_IngredientName]        = "";
							gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
							gpkIngredientRecord[OUTPUT_Dosage]                = "";
							gpkIngredientRecord[OUTPUT_DosageUnit]            = "";
							gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
							gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
							gpkIngredientRecord[OUTPUT_CASNumber]             = "";
							
							outputMap.put(gpkCode, outputIngredients);
						}
						else {
							List<String[]> outputIngredients = new ArrayList<String[]>();
							
							//name = cleanupExtractedIngredientName(name);
							String ingredientName = shortName;
							
							if (ingredientName != null) {
								String amount = "";
								String amountUnit = "";

								String doseString = getDoseString(fullName);
								String[] doseStringSplit = doseString != null ? doseString.split("/") : null;
								if (doseStringSplit != null) {
									String denominatorUnit = "";
									if (doseString.contains("/")) {
										doseString = doseStringSplit[0];
										denominatorUnit = (doseStringSplit.length > 1 ? doseStringSplit[1] : "").trim();
									}
									String numberChars = NUMBER_CHARS;
									for (int charNr = 0; charNr < doseString.length(); charNr++) {
										if (numberChars.indexOf(doseString.charAt(charNr)) < 0) {
											break;
										}
										amount += doseString.charAt(charNr);
									}
									amount = amount.replace(",", ".");
									amountUnit = doseString.substring(amount.length());
									if ((!amountUnit.equals("")) && (amountUnit.substring(0, 1).equals("-"))) { // Solve things like 5-WATER
										amount = "";
										amountUnit = "";
									}
									else {
										amountUnit = amountUnit + (denominatorUnit.equals("") ? "" : "/" + denominatorUnit);
									}
								}
								
								Integer gnkCode = gnkNameMap.get(ingredientName);
								if (gnkCode != null) {
									String[] gnkRecord = gnkMap.get(gnkCode);

									String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
									gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
									gpkIngredientRecord[OUTPUT_SourceName]            = name;
									gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
									gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
									gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
									gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "Mapped to GNK";
									gpkIngredientRecord[OUTPUT_IngredientCode]        = gnkCode.toString();
									gpkIngredientRecord[OUTPUT_IngredientName]        = ingredientName.trim();
									gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
									gpkIngredientRecord[OUTPUT_Dosage]                = amount;
									gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
									gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
									gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
									gpkIngredientRecord[OUTPUT_CASNumber]             = gnkRecord[GNK_CASCode];

									if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
										ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
									}

									outputIngredients.add(gpkIngredientRecord);
								}
								else {
									String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
									gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
									gpkIngredientRecord[OUTPUT_SourceName]            = name;
									gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
									gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
									gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
									gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "Extracted";
									gpkIngredientRecord[OUTPUT_IngredientCode]        = "";
									gpkIngredientRecord[OUTPUT_IngredientName]        = ingredientName.trim();
									gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
									gpkIngredientRecord[OUTPUT_Dosage]                = amount;
									gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
									gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
									gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
									gpkIngredientRecord[OUTPUT_CASNumber]             = "";

									if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
										ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
									}

									outputIngredients.add(gpkIngredientRecord);
								}
							}
							
							outputMap.put(gpkCode, outputIngredients);
						}
					}
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GPK file '" + gpkFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && (gpkList.size() > 0)) {
			// Load translation file
			System.out.println(DrugMapping.getCurrentTime() + "   Loading Ingredient Name Translation File ...");
			boolean newFile = false;
			Map<String, String> originalIngredientNameTranslation = new HashMap<String, String>();
			if ((ingredientNameTranslationFile != null) && ingredientNameTranslationFile.openFile(true)) {
				if (ingredientNameTranslationFile.getFileName().contains("Google")) {
					translationType = "Google";
				}
				if (ingredientNameTranslationFile.getFileName().contains("Amazon")) {
					translationType = "Amazon";
				}
				while (ingredientNameTranslationFile.hasNext()) {
					Row row = ingredientNameTranslationFile.next();
					
					String sourceIngredientName = ingredientNameTranslationFile.get(row, "SourceIngredientName", true).trim();
					String englishIngredientName = ingredientNameTranslationFile.get(row, "EnglishIngredientName", true).trim();
					
					if (!sourceIngredientName.equals("")) {
						if ((ingredientNameTranslation.get(sourceIngredientName) != null) && (!ingredientNameTranslation.get(sourceIngredientName).equals(englishIngredientName))) {
							System.out.println("    ERROR: Different translations for '" + sourceIngredientName + "': '" + englishIngredientName + "' and '" + ingredientNameTranslation.get(sourceIngredientName) + "'");
							ok = false;
						}
						else {
							originalIngredientNameTranslation.put(sourceIngredientName, englishIngredientName);
							if (!ingredientNameTranslation.containsKey(sourceIngredientName)) { // Not used anymore
								System.out.println("    WARNING: Source ingredient name '" + sourceIngredientName + "' ('" + englishIngredientName + "') is not used anymore. Will be removed.");
								newFile = true;
							}
							else {
								ingredientNameTranslation.put(sourceIngredientName, englishIngredientName);
							}
						}
					}
				}
			}
			else {
				System.out.println("  WARING: No translation file found.");
				newFile = true;
				ok = false;
			}

			boolean backupOk = true;
			if (newFile) {
				if (originalIngredientNameTranslation.size() > 0) {
					// Backup last translation
					String fileName = ingredientNameTranslationFile.getFileName();
					String date = DrugMapping.getCurrentDate();
					String baseFileName = fileName + " " + date;
					String extension = "";
					int extensionIndex = fileName.lastIndexOf(File.separator) + fileName.substring(fileName.lastIndexOf(File.separator)).lastIndexOf(".");
					if (extensionIndex > -1) {
						baseFileName = fileName.substring(0, extensionIndex) + " " + date;
						extension =  fileName.substring(extensionIndex);
					}
					// Find first free backup file name of today
					fileName = null;
					for (int backupNr = 1; backupNr < 100; backupNr++) {
						String backupNrString = "00"+ Integer.toString(backupNr);
						backupNrString = backupNrString.substring(backupNrString.length() - 2);
						fileName = baseFileName + " " + backupNrString + extension;
						if (!(new File(fileName)).exists()) {
							break;
						}
					}
					
					if (fileName != null) {
						try {
							PrintWriter translationFile = new PrintWriter(new File(fileName));
							System.out.println(DrugMapping.getCurrentTime() + "     Writing backup of current translation file to: " + fileName);

							translationFile.println("SourceIngredientName,EnglishIngredientName");
							
							List<String> sortedSourceIngredientNames = new ArrayList<String>();
							sortedSourceIngredientNames.addAll(originalIngredientNameTranslation.keySet());
							Collections.sort(sortedSourceIngredientNames);
							for (String sourceIngredientName : sortedSourceIngredientNames) {
								String record = escapeFieldValue(sourceIngredientName);
								record += "," + escapeFieldValue(originalIngredientNameTranslation.get(sourceIngredientName));
								translationFile.println(record);
							}
							translationFile.close();
							System.out.println(DrugMapping.getCurrentTime() + "     Done");
						}
						catch (FileNotFoundException e) {
							System.out.println("  ERROR: Cannot create backup file '" + fileName + "'");
						}
					}
					else {
						System.out.println("  ERROR: No free backup file found.");
						backupOk = false;
					}
					
				}
				
				if (backupOk) {
					// Create translation file
					String fileName = DrugMapping.getBasePath() + "/ZIndex - Ingredient Name Translation.csv";
					if (ingredientNameTranslationFile != null) {
						fileName = ingredientNameTranslationFile.getFileName();
					}
					try {
						PrintWriter translationFile = new PrintWriter(new File(fileName));
						System.out.println(DrugMapping.getCurrentTime() + "     Writing translation file to: " + fileName);
						System.out.println("  WARNING: Please add the missing translations.");

						translationFile.println("SourceIngredientName,EnglishIngredientName");
						
						List<String> sortedSourceIngredientNames = new ArrayList<String>();
						sortedSourceIngredientNames.addAll(ingredientNameTranslation.keySet());
						Collections.sort(sortedSourceIngredientNames);
						for (String sourceIngredientName : sortedSourceIngredientNames) {
							String record = escapeFieldValue(sourceIngredientName);
							record += "," + escapeFieldValue(ingredientNameTranslation.get(sourceIngredientName));
							translationFile.println(record);
						}
						translationFile.close();
						System.out.println(DrugMapping.getCurrentTime() + "     Done");
					}
					catch (FileNotFoundException e) {
						System.out.println("  ERROR: Cannot create output file '" + fileName + "'");
					}
				}
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");

			
			// Write output file
			if (ok && (outputMap.size() > 0)) {
				String gpkFullFileName = DrugMapping.getBasePath() + "/" + DrugMapping.getCurrentDate() + " ZIndex" + (IPCIDerivation ? " IPCI" : "") + " - GPK Full" + (translationType == null ? "" : " - " + translationType) + ".csv";
				System.out.println(DrugMapping.getCurrentTime() + "   Writing out to: " + gpkFullFileName);
				
				try {
					PrintWriter gpkFullFile = new PrintWriter(new File(gpkFullFileName));
					
					String header = "SourceCode";
					header += "," + "SourceName";
					header += "," + "SourceATCCode";
					header += "," + "SourceFormulation";
					header += "," + "SourceCount";
					header += "," + "IngredientNameStatus";
					header += "," + "IngredientCode";
					header += "," + "IngredientName";
					header += "," + "IngredientNameEnglish";
					header += "," + "Dosage";
					header += "," + "DosageUnit";
					header += "," + "OrgDosage";
					header += "," + "OrgDosageUnit";
					header += "," + "CASNumber";
					
					gpkFullFile.println(header);
					
					for (Integer gpkCode : gpkList) {
						List<String[]> outputIngredients = outputMap.get(gpkCode);
						if (outputIngredients != null) {
							for (String[] outputIngredient : outputIngredients) {
								if (outputIngredient[OUTPUT_IngredientName].equals("")) {
									outputIngredient[OUTPUT_IngredientNameEnglish] = "";
								}
								else {
									String translation = ingredientNameTranslation.get(outputIngredient[OUTPUT_IngredientName]);
									outputIngredient[OUTPUT_IngredientNameEnglish] = translation == null ? "" : translation;
								}
								String record = "";
								for (int column = 0; column < OUTPUT_ColumnCount; column++) {
									record += (column == 0 ? "" : ",") + escapeFieldValue(outputIngredient[column]);
								}
								gpkFullFile.println(record);
							}
						}
						else {
							//FIXME gpkCode == 999
							System.out.println("ERROR");
						}
					}
					gpkFullFile.close();
				}
				catch (FileNotFoundException e) {
					System.out.println("  ERROR: Cannot create output file '" + gpkFullFileName + "'");
				}
				System.out.println(DrugMapping.getCurrentTime() + "   Done");
			}
			else {
				System.out.println("  ERROR: No output to write.");
			}
		}
		else {
			System.out.println("  ERROR: Cannot open GPK file '" + gpkFile.getFileName() + "'");
			ok = false;
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
	
	
	private int compareWordsToRemove(String word1, String word2) {
		if (word1.startsWith(word2)) {
			return -1;
		}
		else if (word2.startsWith(word1)) {
			return 1;
		}
		else if (word1.endsWith(word2)) {
			return -1;
		}
		else if (word2.endsWith(word1)) {
			return 1;
		}
		else if (word1.contains(word2)) {
			return -1;
		}
		else {
			return 0;
		}
	}
	
	
	private String escapeFieldValue(String value) {
		if (value == null) {
			value = "";
		}
		else if (value.contains(",") || value.contains("\"")) {
			value = "\"" + value.replaceAll("\"", "\"\"") + "\"";
		}
		return value;
	}
	
	
	private String cleanupExtractedIngredientName(String ingredientName) {
		for (String word : wordsToRemove) {
			ingredientName = ingredientName.trim().replaceAll("  "," ");

			ingredientName = ingredientName.replaceAll("\t", " ").trim();
			ingredientName = ingredientName.replaceAll(" [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*$", "").trim().replaceAll("  "," ");
			
			ingredientName = ingredientName.replaceAll("^[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("^[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*$", "").trim().replaceAll("  "," ");

			ingredientName = ingredientName.replaceAll("[\"][^\"]*[\"]", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" \\(.*\\) ", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" \\(.*\\)$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("^\\(.*\\) ", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("^\\(.*\\)$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("\\([)]*$", "").trim();
			ingredientName = ingredientName.replaceAll("\\([^)]*$", "").trim().replaceAll("  "," ");
			
			ingredientName = ingredientName.replaceAll(" [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + ",", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + ", ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + ",", " ").trim().replaceAll("  "," ");

			ingredientName = ingredientName.replaceAll("^[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("^[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", " ").trim().replaceAll("  "," ");
			
			ingredientName = ingredientName.replaceAll(" [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + ",", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + ", ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + ",", " ").trim().replaceAll("  "," ");

			ingredientName = ingredientName.replaceAll("^[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("^[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", " ").trim().replaceAll("  "," ");

			ingredientName = ingredientName.replaceAll("^" + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" " + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(" " + word + ",", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("," + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("," + word + ",", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", " + word + " ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", " + word + ", ", " ").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", " + word + ",", " ").trim().replaceAll("  "," ");

			ingredientName = ingredientName.replaceAll(" " + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll("," + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(", " + word + "$", "").trim().replaceAll("  "," ");
			ingredientName = ingredientName.replaceAll(",$", "").trim().replaceAll("  "," ");
			
			if (ingredientName.endsWith(" " + word)) {
				ingredientName = ingredientName.substring(0, ingredientName.length() - word.length()).trim().replaceAll("  "," ");
			}
			else if ((ingredientName.startsWith(word + " "))) {
				ingredientName = null;
				break;
			}
			else if (ingredientName.equals(word)) {
				ingredientName = null;
				break;
			}
			if (ingredientName != null) {
				ingredientName = ingredientName.trim().replaceAll("  "," ");
				if ((ingredientName.length() > 0) && (("(),".contains(ingredientName.substring(0, 1))) || (")".contains(ingredientName.substring(ingredientName.length() - 1))))) {
					ingredientName = null;
					break;
				}
			}
		}
		
		if (ingredientName != null) {
			if (!ingredientName.equals("")) {
				try {
					Double.valueOf(ingredientName);
					ingredientName = null;
				}
				catch (NumberFormatException exception) {
					// Do nothing
				}
			}
			else {
				ingredientName = null;
			}
		}
		return ingredientName;
	}
	
	
	private String getDoseString(String fullName) {
		String doseString = null;
		
		// Remove piece between parenthesis at the end
		if ((!fullName.equals("")) && fullName.substring(fullName.length() - 1).equals(")")) {
			int openParenthesisIndex = fullName.lastIndexOf("(");
			if (openParenthesisIndex > 0) {
				fullName = fullName.substring(0, openParenthesisIndex).trim();
			}
		}
		
		// Get the last part as dose information
		String[] fullNameSplit = fullName.split(" ");
		if ((fullNameSplit.length > 2) && (!fullNameSplit[fullNameSplit.length - 1].trim().equals("")) && (DIGITS.contains(fullNameSplit[fullNameSplit.length - 1].trim().substring(0, 1)))) {
			doseString = fullNameSplit[fullNameSplit.length - 1].trim();
		}
		return doseString;
	}
}
