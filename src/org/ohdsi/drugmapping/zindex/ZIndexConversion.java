package org.ohdsi.drugmapping.zindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.StringUtilities;
import org.ohdsi.utilities.files.Row;

public class ZIndexConversion extends Mapping {
	private static final boolean IGNORE_EMPTY_GPK_NAMES   = false;
	private static final boolean IGNORE_STARRED_GPK_NAMES = false;
	
	private static final String DIGITS = "1234567890";
	private static final String NUMBER_CHARS = "1234567890,.";
	
	private static final int GPK_ColumnCount      = 13;
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
	private static final int GPK_BasicUnit        = 12;

	private static final int GSK_ColumnCount = 8;
	private static final int GSK_GSKCode     = 0;
	private static final int GSK_PartNumber  = 1;
	private static final int GSK_Type        = 2;
	private static final int GSK_Amount      = 3;
	private static final int GSK_AmountUnit  = 4;
	private static final int GSK_GNKCode     = 5;
	private static final int GSK_GenericName = 6;
	private static final int GSK_CASNumber   = 7;
	
	private static final int GNK_ColumnCount     = 5;
	private static final int GNK_GNKCode         = 0;
	private static final int GNK_Description     = 1;
	private static final int GNK_CASNumber       = 2;
	private static final int GNK_BaseName        = 3;
	private static final int GNK_ChemicalFormula = 4;

	private static final int GPKIPCI_ColumnCount     = 10;
	private static final int GPKIPCI_GPKCode         = 0;
	private static final int GPKIPCI_PartNumber      = 1;
	private static final int GPKIPCI_Type            = 2;
	private static final int GPKIPCI_Amount          = 3;
	private static final int GPKIPCI_AmountUnit      = 4;
	private static final int GPKIPCI_GNKCode         = 5;
	private static final int GPKIPCI_GNKName         = 6;
	private static final int GPKIPCI_CASNumber       = 7;
	private static final int GPKIPCI_BaseName        = 8;
	private static final int GPKIPCI_ChemicalFormula = 9;
	
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
	private Set<String> nonDenominatorUnits = new HashSet<String>();
	private Map<String, String> ingredientNameTranslation = new HashMap<String, String>();
	private Map<String, List<String[]>> gpkIPCIMap = new HashMap<String, List<String[]>>();
	private Map<Integer, List<String[]>> outputMap = new HashMap<Integer, List<String[]>>();

	
	public ZIndexConversion(InputFile gpkFile, InputFile gskFile, InputFile gnkFile, InputFile gpkStatsFile, InputFile wordsToRemoveFile, InputFile nonDenominatorUnitsFile, InputFile ingredientNameTranslationFile, InputFile gpkIPCIFile) {
		
		boolean ok = true;
		
		String translationType = null; 

		System.out.println(DrugMapping.getCurrentTime() + " Converting ZIndex Files ...");

		
		// Load GSK file
		if (ok) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GSK File ...");
			try {
				if (gskFile.openFile()) {
					while (gskFile.hasNext()) {
						Row row = gskFile.next();

						String[] record = new String[GSK_ColumnCount];
						record[GSK_GSKCode]       = StringUtilities.removeExtraSpaces(gskFile.get(row, "GSKCode", true));
						record[GSK_PartNumber]    = StringUtilities.removeExtraSpaces(gskFile.get(row, "PartNumber", true));
						record[GSK_Type]          = StringUtilities.removeExtraSpaces(gskFile.get(row, "Type", true).trim());
						record[GSK_Amount]        = StringUtilities.removeExtraSpaces(gskFile.get(row, "Amount", true));
						record[GSK_AmountUnit]    = StringUtilities.removeExtraSpaces(gskFile.get(row, "AmountUnit", true));
						record[GSK_GNKCode]       = StringUtilities.removeExtraSpaces(gskFile.get(row, "GNKCode", true));
						record[GSK_GenericName]   = StringUtilities.removeExtraSpaces(gskFile.get(row, "GenericName", true));
						record[GSK_CASNumber]     = StringUtilities.removeExtraSpaces(gskFile.get(row, "CASNumber", true));

						record[GSK_CASNumber] = GenericMapping.uniformCASNumber(record[GSK_CASNumber]);

						int gskCode = Integer.valueOf(record[GSK_GSKCode]); 
						List<String[]> gskList = gskMap.get(gskCode);
						if (gskList == null) {
							gskList = new ArrayList<String[]>();
							gskMap.put(gskCode, gskList);
						}
						gskList.add(record);
					}
				}
				else {
					System.out.println("  ERROR: Cannot load GSK file '" + gskFile.getFileName() + "'");
					ok = false;
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		

		// Load GNK file
		if (ok) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GNK File ...");
			try {
				if (gnkFile.openFile()) {
					while (gnkFile.hasNext()) {
						Row row = gnkFile.next();

						String[] record = new String[GNK_ColumnCount];
						record[GNK_GNKCode]         = StringUtilities.removeExtraSpaces(gnkFile.get(row, "GNKCode", true));
						record[GNK_Description]     = StringUtilities.removeExtraSpaces(gnkFile.get(row, "Description", true));
						record[GNK_CASNumber]       = StringUtilities.removeExtraSpaces(gnkFile.get(row, "CASNumber", true));
						record[GNK_BaseName]        = StringUtilities.removeExtraSpaces(gnkFile.get(row, "BaseName", true));
						record[GNK_ChemicalFormula] = StringUtilities.removeExtraSpaces(gnkFile.get(row, "ChemicalFormula", true));

						record[GNK_CASNumber]       = GenericMapping.uniformCASNumber(record[GNK_CASNumber]);

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
				}
				else {
					System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
					ok = false;
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		

		// Load GPK Statistics File
		if (ok) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GPK Statistics File ...");
			try {
				if (gpkStatsFile.openFile()) {
					while (gpkStatsFile.hasNext()) {
						Row row = gpkStatsFile.next();

						String gpkCode  = StringUtilities.removeExtraSpaces(gpkStatsFile.get(row, "GPKCode", true));
						String gpkCount = StringUtilities.removeExtraSpaces(gpkStatsFile.get(row, "GPKCount", true));
						
						Integer count = null;
						try {
							count = Integer.valueOf(gpkCount);
							gpkStatisticsMap.put(gpkCode, count);
						}
						catch (NumberFormatException e) {
						}
					}
				}
				else {
					System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
					ok = false;
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		
/* SKIPPED BEGIN 2020-05-12
		// Load Words To Ignore File
		if (ok) {
			if (wordsToRemoveFile != null) {
				try {
					System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex Words To Ignore File ...");
					if (wordsToRemoveFile.openFile(true)) {
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
					}
					else {
						System.out.println("  ERROR: Cannot load  ZIndex Words To Ignore file '" + wordsToRemoveFile.getFileName() + "'");
						ok = false;
					}
				}
				catch (NoSuchElementException fileException) {
					System.out.println("  ERROR: " + fileException.getMessage());
					ok = false;
				}
				System.out.println(DrugMapping.getCurrentTime() + "   Done");
				
			}
		}
		

		// Load ZIndex Non-Denominator Units File
		if (ok) {
			if (nonDenominatorUnitsFile != null) {
				try {
					System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex Non-Denominator Units File ...");
					if (nonDenominatorUnitsFile.openFile(true)) {
						while (nonDenominatorUnitsFile.hasNext()) {
							Row row = nonDenominatorUnitsFile.next();

							String nonDenominatorUnit = nonDenominatorUnitsFile.get(row, "NonDenominatorUnit", true).trim().toUpperCase();
							//System.out.println("    " + nonDenominatorUnit);
							
							if ((nonDenominatorUnit != null) && (!nonDenominatorUnit.equals(""))) {
								nonDenominatorUnits.add(nonDenominatorUnit);
							}				
						}
					}
					else {
						System.out.println("  ERROR: Cannot load  ZIndex Non-Denominator Units file '" + nonDenominatorUnitsFile.getFileName() + "'");
						ok = false;
					}
				}
				catch (NoSuchElementException fileException) {
					System.out.println("  ERROR: " + fileException.getMessage());
					ok = false;
				}
				System.out.println(DrugMapping.getCurrentTime() + "   Done");
				
			}
		}
SKIPPED END 2020-05-12 */
		
		// Load GPK IPCI Compositions File
		if (ok) {
			if ((gpkIPCIFile != null) && gpkIPCIFile.isSelected()) {
				try {
					System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GPK IPCI Compositions File ...");
					if (gpkIPCIFile.openFile()) {
						
						IPCIDerivation = true;
						
						while (gpkIPCIFile.hasNext()) {
							Row row = gpkIPCIFile.next();

							String[] record = new String[GPKIPCI_ColumnCount];
							record[GPKIPCI_GPKCode]         = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "GPKCode", true));
							record[GPKIPCI_PartNumber]      = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "PartNumber", true));
							record[GPKIPCI_Type]            = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "Type", true));
							record[GPKIPCI_Amount]          = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "Amount", true));
							record[GPKIPCI_AmountUnit]      = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "AmountUnit", true));
							record[GPKIPCI_GNKCode]         = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "GNKCode", true));
							record[GPKIPCI_GNKName]         = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "GenericName", true));
							record[GPKIPCI_CASNumber]       = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "CASNumber", true));
							record[GPKIPCI_BaseName]        = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "BaseName", true));
							record[GPKIPCI_ChemicalFormula] = StringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "ChemicalFormula", true));
							
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
					}
					System.out.println(DrugMapping.getCurrentTime() + "   Done");
				}
				catch (NoSuchElementException fileException) {
					System.out.println("  ERROR: " + fileException.getMessage());
					ok = false;
				}
			}
			else {
				System.out.println(DrugMapping.getCurrentTime() + "     No GPK IPCI Compositions File used.");
			}
		}
		

		// Load GPK file
		if (ok) {
			System.out.println(DrugMapping.getCurrentTime() + "   Loading ZIndex GPK File ...");
			try {
				if (gpkFile.openFile()) {
					while (gpkFile.hasNext()) {
						Row row = gpkFile.next();

						String[] gpk = new String[GPK_ColumnCount];
						gpk[GPK_GPKCode]          = StringUtilities.removeExtraSpaces(gpkFile.get(row, "GPKCode", true));
						gpk[GPK_MemoCode]         = StringUtilities.removeExtraSpaces(gpkFile.get(row, "MemoCode", true));
						gpk[GPK_LabelName]        = StringUtilities.removeExtraSpaces(gpkFile.get(row, "LabelName", true));
						gpk[GPK_ShortName]        = StringUtilities.removeExtraSpaces(gpkFile.get(row, "ShortName", true));
						gpk[GPK_FullName]         = StringUtilities.removeExtraSpaces(gpkFile.get(row, "FullName", true));
						gpk[GPK_ATCCode]          = StringUtilities.removeExtraSpaces(gpkFile.get(row, "ATCCode", true));
						gpk[GPK_GSKCode]          = StringUtilities.removeExtraSpaces(gpkFile.get(row, "GSKCode", true));
						gpk[GPK_DDDPerHPKUnit]    = StringUtilities.removeExtraSpaces(gpkFile.get(row, "DDDPerHPKUnit", true));
						gpk[GPK_PrescriptionDays] = StringUtilities.removeExtraSpaces(gpkFile.get(row, "PrescriptionDays", true));
						gpk[GPK_HPKMG]            = StringUtilities.removeExtraSpaces(gpkFile.get(row, "HPKMG", true)).toUpperCase();
						gpk[GPK_HPKMGUnit]        = StringUtilities.removeExtraSpaces(gpkFile.get(row, "HPKMGUnit", true)).toUpperCase();
						gpk[GPK_PharmForm]        = StringUtilities.removeExtraSpaces(gpkFile.get(row, "PharmForm", true)).toUpperCase();
						gpk[GPK_BasicUnit]        = StringUtilities.removeExtraSpaces(gpkFile.get(row, "BasicUnit", true)).toUpperCase();

						String gpkCodeString = gpk[GPK_GPKCode];
						String labelName     = gpk[GPK_LabelName];
						String shortName     = gpk[GPK_ShortName];
						String fullName      = gpk[GPK_FullName];
						String atcCode       = gpk[GPK_ATCCode];
						String gskCodeString = gpk[GPK_GSKCode];
						String hpkAmount     = gpk[GPK_HPKMG];
						String hpkUnit       = gpk[GPK_HPKMGUnit];
						String pharmForm     = gpk[GPK_PharmForm];
						String basicUnit     = gpk[GPK_BasicUnit];

						String hpkNumeratorUnit = hpkUnit.contains("/") ? hpkUnit.substring(0, hpkUnit.indexOf("/")).trim() : hpkUnit;
						String hpkDenominatorUnit = hpkUnit.contains("/") ? hpkUnit.substring(hpkUnit.indexOf("/") + 1).trim() : null;

						Integer gpkCode = Integer.valueOf(gpk[GPK_GPKCode]);
						gpkList.add(gpkCode);
						gpkMap.put(gpkCode, gpk);
						
						String name = fullName;
						if (name.equals("")) name = labelName;
						if (name.equals("")) name = shortName;
						Integer gskCode = gskCodeString.equals("") ? null : Integer.valueOf(gskCodeString);

						List<String[]> ipciOutputIngredients = null;
						List<String[]> zindexOutputIngredients = null;
						List<String[]> zindexExtractedOutputIngredients = null;
						
						// Get IPCI derivation from Marcel de Wilde if it exists
						List<String[]> gpkIPCIIngredients = gpkIPCIMap.get(gpkCodeString);
						if (gpkIPCIIngredients != null) {
							// IPCI derivation found
							ipciOutputIngredients = new ArrayList<String[]>();
							
							for (String[] gpkIPCIIngredient : gpkIPCIIngredients) {
								String ingredientNumeratorUnit = gpkIPCIIngredient[GPKIPCI_AmountUnit];
								if (ingredientNumeratorUnit.equals("")) {
									if (!hpkNumeratorUnit.equals("")) {
										ingredientNumeratorUnit = hpkNumeratorUnit;
									}
								}
								String ingredientDenominatorUnit = hpkDenominatorUnit;
								if ((ingredientDenominatorUnit == null) && (gpkIPCIIngredients.size() > 1)) {
									ingredientDenominatorUnit = basicUnit.equals("") ? null : (nonDenominatorUnits.contains(basicUnit) ? null : basicUnit);
								}
								String ingredientUnit = ingredientDenominatorUnit == null ? ingredientNumeratorUnit : (ingredientNumeratorUnit.equals("") ? ingredientDenominatorUnit : (ingredientNumeratorUnit + "/" + ingredientDenominatorUnit));
								
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
								gpkIngredientRecord[OUTPUT_DosageUnit]            = ingredientUnit;
								gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
								gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
								gpkIngredientRecord[OUTPUT_CASNumber]             = GenericMapping.uniformCASNumber(gpkIPCIIngredient[GPKIPCI_CASNumber]);

								if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
									ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
								}

								ipciOutputIngredients.add(gpkIngredientRecord);
							}
						}

						// Get Z-Index derivation

						// Ignore empty names and names that start with a '*'
						if (((!IGNORE_EMPTY_GPK_NAMES) || (!name.equals(""))) && ((!IGNORE_STARRED_GPK_NAMES) || (!name.substring(0, 1).equals("*")))) {
							
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
							
							// Get ingredients and dosages from GSK and GNK tables
							if (gskList != null) {
								for (String[] gskObject : gskList) {
									String amount = gskObject[GSK_Amount];

									String ingredientNumeratorUnit = gskObject[GSK_AmountUnit];
									if (ingredientNumeratorUnit.equals("")) {
										if (!hpkNumeratorUnit.equals("")) {
											ingredientNumeratorUnit = hpkNumeratorUnit;
										}
									}
									String ingredientDenominatorUnit = hpkDenominatorUnit;
									if ((ingredientDenominatorUnit == null) && (gskList.size() > 1)) {
										ingredientDenominatorUnit = basicUnit.equals("") ? null : (nonDenominatorUnits.contains(basicUnit) ? null : basicUnit);
									}
									String amountUnit = ingredientDenominatorUnit == null ? ingredientNumeratorUnit : (ingredientNumeratorUnit.equals("") ? ingredientDenominatorUnit : (ingredientNumeratorUnit + "/" + ingredientDenominatorUnit));
									
									
									// Extract unit from name
									if (name.lastIndexOf(" ") >= 0) {
										String strengthString = StringUtilities.removeExtraSpaces(name.substring(name.lastIndexOf(" ")));
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
											String strengthUnitString = StringUtilities.removeExtraSpaces(strengthString.substring(strengthValueString.length()));
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
									
									
									String gnkCode = gskObject[GSK_GNKCode];
									String genericName = ""; 
									if (!gskObject[GSK_GenericName].substring(0, 1).equals("*")) {
										genericName = gskObject[GSK_GenericName]; //CHANGED 2020-04-29 cleanupExtractedIngredientName(gskObject[GSK_GenericName]);
									}

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

									if (zindexOutputIngredients == null) {
										zindexOutputIngredients = new ArrayList<String[]>();
									}
									zindexOutputIngredients.add(gpkIngredientRecord);
								}
							}

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
							List<String> ingredientNames = new ArrayList<String>();
							List<String> ingredientAmounts = new ArrayList<String>();
							List<String> ingredientAmountUnits = new ArrayList<String>();

							String ingredients = shortName;
							if (ingredients.contains(" ") && (!ingredients.substring(0, 1).equals("*"))) {
								ingredients = ingredients.substring(0, ingredients.lastIndexOf(" "));
							}
							if (!ingredients.equals("")) {
								if (ingredients.contains("/") || ingredients.contains("+")) {
									String[] ingredientsSplit = ingredients.contains("/") ? ingredients.split("/") :  ingredients.split("\\+");
									String doseString = getDoseString(fullName);
									String[] doseStringSplit = doseString != null ? doseString.split("/") : null;
									String denominatorUnit = StringUtilities.removeExtraSpaces((((doseStringSplit != null) && (doseStringSplit.length > ingredientsSplit.length)) ? doseStringSplit[ingredientsSplit.length] : ""));
									String lastAmountUnit = null;
									
									for (int ingredientNr = 0; ingredientNr < ingredientsSplit.length; ingredientNr++) {
										String ingredientName = ingredientsSplit[ingredientNr];
										ingredientName = StringUtilities.removeExtraSpaces(ingredientName); //CHANGED 2020-04-29 cleanupExtractedIngredientName(ingredientName);
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

									// Fill missing units
									for (int ingredientNr = 0; ingredientNr < ingredientNames.size(); ingredientNr++) {
										if (ingredientNames.get(ingredientNr) != null) {
											String amountUnit = ingredientAmountUnits.get(ingredientNr);
											if (amountUnit.equals("")) {
												amountUnit = lastAmountUnit;
											}
											ingredientAmountUnits.set(ingredientNr, amountUnit.equals("") ? denominatorUnit : (denominatorUnit.equals("") ? amountUnit : amountUnit + "/" + denominatorUnit));
										}
									}
								}
								else {
									//CHANGED 2020-04-29 ingredientName = cleanupExtractedIngredientName(name);
									String ingredientName = ingredients;
									
									if (ingredientName != null) {
										String amount = "";
										String amountUnit = "";

										String doseString = getDoseString(fullName);
										String[] doseStringSplit = doseString != null ? doseString.split("/") : null;
										if (doseStringSplit != null) {
											String denominatorUnit = "";
											if (doseString.contains("/")) {
												doseString = doseStringSplit[0];
												denominatorUnit = StringUtilities.removeExtraSpaces((doseStringSplit.length > 1 ? doseStringSplit[1] : ""));
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
												amountUnit = amountUnit.equals("") ? denominatorUnit : (denominatorUnit.equals("") ? amountUnit : amountUnit + "/" + denominatorUnit);
											}
										}
										
										ingredientNames.add(ingredientName);
										ingredientAmounts.add(amount);
										ingredientAmountUnits.add(amountUnit);
									}
								}

								for (int ingredientNr = 0; ingredientNr < ingredientNames.size(); ingredientNr++) {
									String ingredientName = ingredientNames.get(ingredientNr);
									
									if (ingredientName != null) {
										String amount = ingredientAmounts.get(ingredientNr);
										String amountUnit = ingredientAmountUnits.get(ingredientNr);
										
										Integer gnkCode = gnkNameMap.get(ingredientName);
										String[] gnkRecord = gnkCode == null ? null : gnkMap.get(gnkCode);
										
										String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
										gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
										gpkIngredientRecord[OUTPUT_SourceName]            = name;
										gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
										gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
										gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
										gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = gnkCode == null ? "Extracted" : "Mapped to GNK";
										gpkIngredientRecord[OUTPUT_IngredientCode]        = gnkCode == null ? "" : gnkCode.toString();
										gpkIngredientRecord[OUTPUT_IngredientName]        = StringUtilities.removeExtraSpaces(ingredientName);
										gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
										gpkIngredientRecord[OUTPUT_Dosage]                = amount;
										gpkIngredientRecord[OUTPUT_DosageUnit]            = amountUnit;
										gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
										gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
										gpkIngredientRecord[OUTPUT_CASNumber]             = gnkCode == null ? "" : gnkRecord[GNK_CASNumber];

										if ((gpkIngredientRecord[OUTPUT_IngredientName] != null) && (!gpkIngredientRecord[OUTPUT_IngredientName].equals(""))) {
											ingredientNameTranslation.put(gpkIngredientRecord[OUTPUT_IngredientName], null);
										}


										if (zindexExtractedOutputIngredients == null) {
											zindexExtractedOutputIngredients = new ArrayList<String[]>();
										}
										zindexExtractedOutputIngredients.add(gpkIngredientRecord);
									}
								}
							}
						}

						List<String[]> outputIngredients = null;
/* REPLACED BEGIN 2020-05-12
						// If IPCI ingredients are found
						if (ipciOutputIngredients != null) {
							// If Z-Index ingredients are found
							if (zindexOutputIngredients != null) {
								// If Z-Index extracted ingredients are found
								if (zindexExtractedOutputIngredients != null) {
									outputIngredients = new ArrayList<String[]>();
									
									List<String[]> zindexMatch = matchIngredients(ipciOutputIngredients, zindexOutputIngredients);
									List<String[]> zindexExtractedMatch = matchIngredients(ipciOutputIngredients, zindexExtractedOutputIngredients);
									
									// For each IPCI derived ingredient
									for (int ingredientNr = 0; ingredientNr < ipciOutputIngredients.size(); ingredientNr++) {
										// Get IPCI ingredient
										String[] ipciOutputIngredient = ipciOutputIngredients.get(ingredientNr);
										
										// Get corresponding Z-Index ingredient
										String[] zindexOutputIngredient = zindexMatch == null ? null : zindexMatch.get(ingredientNr);

										// get corresponding Z-Index extracted ingredient
										String[] zindexExtractedOutputIngredient = zindexExtractedMatch == null ? null : zindexExtractedMatch.get(ingredientNr);
										
										// When found check if dosage and dosage unit correspond
										if (zindexOutputIngredient != null) {
											if (ipciOutputIngredient[OUTPUT_Dosage].equals(zindexOutputIngredient[OUTPUT_Dosage]) && ipciOutputIngredient[OUTPUT_DosageUnit].equals(zindexOutputIngredient[OUTPUT_DosageUnit])) {
												if ((zindexExtractedOutputIngredient == null) || (ipciOutputIngredient[OUTPUT_Dosage].equals(zindexExtractedOutputIngredient[OUTPUT_Dosage]) && ipciOutputIngredient[OUTPUT_DosageUnit].equals(zindexExtractedOutputIngredient[OUTPUT_DosageUnit]))) {
													// If no Z-Index extracted ingredient is found or all dosages are the same take the IPCI ingredient
													outputIngredients.add(ipciOutputIngredient);
												}
												else {
													// The Z-Index extracted is different
													// If in Z-Index extracted it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index extracted values
													if ((!zindexExtractedOutputIngredient[OUTPUT_Dosage].equals("")) && zindexExtractedOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
														ipciOutputIngredient[OUTPUT_Dosage]     = zindexExtractedOutputIngredient[OUTPUT_Dosage]; 
														ipciOutputIngredient[OUTPUT_DosageUnit] = zindexExtractedOutputIngredient[OUTPUT_DosageUnit];
														outputIngredients.add(ipciOutputIngredient);
													}
													// Else take the IPCI ingredient
													else {
														outputIngredients.add(ipciOutputIngredient);
													}
												}
											}
											else {
												if ((zindexExtractedOutputIngredient == null) || (zindexOutputIngredient[OUTPUT_Dosage].equals(zindexExtractedOutputIngredient[OUTPUT_Dosage]) && zindexOutputIngredient[OUTPUT_DosageUnit].equals(zindexExtractedOutputIngredient[OUTPUT_DosageUnit]))) {
													// Z-Index and Z-Index extracted is not found or the same but both are different from IPCI
													// If in Z-Index it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index values
													if (zindexOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
														ipciOutputIngredient[OUTPUT_Dosage]     = zindexOutputIngredient[OUTPUT_Dosage]; 
														ipciOutputIngredient[OUTPUT_DosageUnit] = zindexOutputIngredient[OUTPUT_DosageUnit];
														outputIngredients.add(ipciOutputIngredient);
													}
												}
												else {
													// All differ: check if there is a numerator/denominator dosage
													// If in Z-Index it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index values
													if (zindexOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
														ipciOutputIngredient[OUTPUT_Dosage]     = zindexOutputIngredient[OUTPUT_Dosage]; 
														ipciOutputIngredient[OUTPUT_DosageUnit] = zindexOutputIngredient[OUTPUT_DosageUnit];
														outputIngredients.add(ipciOutputIngredient);
													}
													// Else if in Z-Index extracted it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index extracted values
													else if ((!zindexExtractedOutputIngredient[OUTPUT_Dosage].equals("")) && zindexExtractedOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
														ipciOutputIngredient[OUTPUT_Dosage]     = zindexExtractedOutputIngredient[OUTPUT_Dosage]; 
														ipciOutputIngredient[OUTPUT_DosageUnit] = zindexExtractedOutputIngredient[OUTPUT_DosageUnit];
														outputIngredients.add(ipciOutputIngredient);
													}
												}
											}
											
											// If in Z-Index it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index values
											if (zindexOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
												ipciOutputIngredient[OUTPUT_Dosage]     = zindexOutputIngredient[OUTPUT_Dosage]; 
												ipciOutputIngredient[OUTPUT_DosageUnit] = zindexOutputIngredient[OUTPUT_DosageUnit];
												outputIngredients.add(ipciOutputIngredient);
											}
											// Else take the IPCI ingredient
											else {
												outputIngredients.add(ipciOutputIngredient);
											}
										}
										else if ((zindexExtractedOutputIngredient == null) || (ipciOutputIngredient[OUTPUT_Dosage].equals(zindexExtractedOutputIngredient[OUTPUT_Dosage]) && ipciOutputIngredient[OUTPUT_DosageUnit].equals(zindexExtractedOutputIngredient[OUTPUT_DosageUnit]))) {
											// If no Z-Index extracted ingredient is found or the dosage of the Z=-Index extracted en IPCI are the same take the IPCI ingredient
											outputIngredients.add(ipciOutputIngredient);
										}
										else {
											// The Z-Index extracted is different from IPCI
											// If in Z-Index extracted it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index extracted values
											if ((!zindexExtractedOutputIngredient[OUTPUT_Dosage].equals("")) && zindexExtractedOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
												ipciOutputIngredient[OUTPUT_Dosage]     = zindexExtractedOutputIngredient[OUTPUT_Dosage]; 
												ipciOutputIngredient[OUTPUT_DosageUnit] = zindexExtractedOutputIngredient[OUTPUT_DosageUnit];
												outputIngredients.add(ipciOutputIngredient);
											}
											// Else take the IPCI ingredient
											else {
												outputIngredients.add(ipciOutputIngredient);
											}
										}
									}
									
								}
								else {
									outputIngredients = new ArrayList<String[]>();
									
									List<String[]> zindexMatch = matchIngredients(ipciOutputIngredients, zindexOutputIngredients);
									
									// For each IPCI derived ingredient
									for (int ingredientNr = 0; ingredientNr < ipciOutputIngredients.size(); ingredientNr++) {
										// Get IPCI ingredient
										String[] ipciOutputIngredient = ipciOutputIngredients.get(ingredientNr);
										
										// Get corresponding Z-Index ingredient
										String[] zindexOutputIngredient = zindexMatch.get(ingredientNr);
										
										// When found check if dosage and dosage unit correspond
										if (zindexOutputIngredient != null) {
											// If the Z-Index and IPCI dosages are the same take the IPCI ingredient
											if (ipciOutputIngredient[OUTPUT_Dosage].equals(zindexOutputIngredient[OUTPUT_Dosage]) && ipciOutputIngredient[OUTPUT_DosageUnit].equals(zindexOutputIngredient[OUTPUT_DosageUnit])) {
												outputIngredients.add(ipciOutputIngredient);
											}
											// If in Z-Index it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index values
											else if (zindexOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
												ipciOutputIngredient[OUTPUT_Dosage]     = zindexOutputIngredient[OUTPUT_Dosage]; 
												ipciOutputIngredient[OUTPUT_DosageUnit] = zindexOutputIngredient[OUTPUT_DosageUnit];
												outputIngredients.add(ipciOutputIngredient);
											}
											// Else take the IPCI ingredient
											else {
												outputIngredients.add(ipciOutputIngredient);
											}
										}
										// Else take the IPCI ingredient
										else {
											outputIngredients.add(ipciOutputIngredient);
										}
									}
								}
							}
							// Else if Z-Index extracted ingredients are found
							else if (zindexExtractedOutputIngredients != null) {
								outputIngredients = new ArrayList<String[]>();
								
								List<String[]> zindexExtractedMatch = matchIngredients(ipciOutputIngredients, zindexExtractedOutputIngredients);
								
								if (zindexExtractedMatch != null) {
									// For each IPCI derived ingredient
									for (int ingredientNr = 0; ingredientNr < ipciOutputIngredients.size(); ingredientNr++) {
										// Get IPCI ingredient
										String[] ipciOutputIngredient = ipciOutputIngredients.get(ingredientNr);

										// get corresponding Z-Index extracted ingredient
										String[] zindexExtractedOutputIngredient = zindexExtractedMatch.get(ingredientNr);
										
										// When found check if dosage and dosage unit correspond
										if (zindexExtractedOutputIngredient != null) {
											// If the Z-Index extracted and IPCI dosages are the same take the IPCI ingredient
											if (ipciOutputIngredient[OUTPUT_Dosage].equals(zindexExtractedOutputIngredient[OUTPUT_Dosage]) && ipciOutputIngredient[OUTPUT_DosageUnit].equals(zindexExtractedOutputIngredient[OUTPUT_DosageUnit])) {
												outputIngredients.add(ipciOutputIngredient);
											}
											// If in Z-Index extracted it is a numerator/denominator dosage and not in IPCI overrule the IPCI dosage/dosage unit with the Z-Index extracted values
											else if ((!zindexExtractedOutputIngredient[OUTPUT_Dosage].equals("")) && zindexExtractedOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!ipciOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
												ipciOutputIngredient[OUTPUT_Dosage]     = zindexExtractedOutputIngredient[OUTPUT_Dosage]; 
												ipciOutputIngredient[OUTPUT_DosageUnit] = zindexExtractedOutputIngredient[OUTPUT_DosageUnit];
												outputIngredients.add(ipciOutputIngredient);
											}
											// Else take the IPCI ingredient
											else {
												outputIngredients.add(ipciOutputIngredient);
											}
										}
										// Else take the IPCI ingredient
										else {
											outputIngredients.add(ipciOutputIngredient);
										}
									}
								}
								else {
									// Take the IPCI ingredients
									outputIngredients = ipciOutputIngredients;
								}
							}
							else {
								// Take the IPCI ingredients
								outputIngredients = ipciOutputIngredients;
							}
						}
						else {
							// If Z-Index ingredients are found
							if (zindexOutputIngredients != null) {
								// If Z-Index extracted ingredients are found
								if (zindexExtractedOutputIngredients != null) {
									outputIngredients = new ArrayList<String[]>();
									
									List<String[]> zindexExtractedMatch = matchIngredients(zindexOutputIngredients, zindexExtractedOutputIngredients);
									
									if (zindexExtractedMatch != null) {
										// For each IPCI derived ingredient
										for (int ingredientNr = 0; ingredientNr < zindexOutputIngredients.size(); ingredientNr++) {
											// Get Z-Index ingredient
											String[] zindexOutputIngredient = zindexOutputIngredients.get(ingredientNr);

											// get corresponding Z-Index extracted ingredient
											String[] zindexExtractedOutputIngredient = zindexExtractedMatch.get(ingredientNr);
											
											// When found check if dosage and dosage unit correspond
											if (zindexExtractedOutputIngredient != null) {
												// If the Z-Index and Z-Index extracted dosages are the same take the Z-Index ingredient
												if (zindexOutputIngredient[OUTPUT_Dosage].equals(zindexExtractedOutputIngredient[OUTPUT_Dosage]) && zindexOutputIngredient[OUTPUT_DosageUnit].equals(zindexExtractedOutputIngredient[OUTPUT_DosageUnit])) {
													outputIngredients.add(zindexOutputIngredient);
												}
												// If in Z-Index extracted it is a numerator/denominator dosage and not in Z-Index overrule the Z-Index dosage/dosage unit with the Z-Index extracted values
												else if ((!zindexExtractedOutputIngredient[OUTPUT_Dosage].equals("")) && zindexExtractedOutputIngredient[OUTPUT_DosageUnit].contains("/")  && (!zindexOutputIngredient[OUTPUT_DosageUnit].contains("/"))) { 
													zindexOutputIngredient[OUTPUT_Dosage]     = zindexExtractedOutputIngredient[OUTPUT_Dosage]; 
													zindexOutputIngredient[OUTPUT_DosageUnit] = zindexExtractedOutputIngredient[OUTPUT_DosageUnit];
													outputIngredients.add(zindexOutputIngredient);
												}
												// Else take the IPCI ingredient
												else {
													outputIngredients.add(zindexOutputIngredient);
												}
											}
											// Else take the IPCI ingredient
											else {
												outputIngredients.add(zindexOutputIngredient);
											}
										}
									}
									else {
										// Take the Z-Index ingredients
										outputIngredients = zindexOutputIngredients;
									}
								}
								else {
									// Take the Z-Index ingredients
									outputIngredients = zindexOutputIngredients;
								}
							}
							// Else if Z-Index extracted ingredients found take the Z-Index extracted ingredients
							else if (zindexExtractedOutputIngredients != null) {
								outputIngredients = zindexExtractedOutputIngredients;
							}
						}
REPLACED END 2020-05-11 */
/* REPLACED BY BEGIN 2020-05-11 */
						outputIngredients = ipciOutputIngredients != null ? ipciOutputIngredients : zindexOutputIngredients;
						if (outputIngredients != null) {
							for (String[] outputIngredient : outputIngredients) {
								outputIngredient[OUTPUT_DosageUnit] = outputIngredient[OUTPUT_DosageUnit].replaceAll("/ST", "");
								if (outputIngredient[OUTPUT_DosageUnit].equals("ST")) {
									outputIngredient[OUTPUT_DosageUnit] = "";
								}
							}
						}
/* REPLACED BY END 2020-05-11 */						
						
						// When output ingredients are found write them to the output
						if (outputIngredients != null) {
							outputMap.put(gpkCode, outputIngredients);
						}
					}
				}
				else {
					System.out.println("  ERROR: Cannot load GPK file '" + gpkFile.getFileName() + "'");
					ok = false;
				}
			}
			catch (NoSuchElementException fileException) {
				System.out.println("  ERROR: " + fileException.getMessage());
				ok = false;
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		

		// Load translation file
		if (ok) {
			if (gpkList.size() > 0) {
				System.out.println(DrugMapping.getCurrentTime() + "   Loading Ingredient Name Translation File ...");
				try {
					boolean newFile = false;
					Map<String, String> originalIngredientNameTranslation = new HashMap<String, String>();
					if (ingredientNameTranslationFile != null) {
						if (ingredientNameTranslationFile.openFile(true)) {
							if (ingredientNameTranslationFile.getFileName().contains("Google")) {
								translationType = "Google";
							}
							if (ingredientNameTranslationFile.getFileName().contains("Amazon")) {
								translationType = "Amazon";
							}
							while (ingredientNameTranslationFile.hasNext()) {
								Row row = ingredientNameTranslationFile.next();
								
								String sourceIngredientName = StringUtilities.removeExtraSpaces(ingredientNameTranslationFile.get(row, "SourceIngredientName", true));
								String englishIngredientName = StringUtilities.removeExtraSpaces(ingredientNameTranslationFile.get(row, "EnglishIngredientName", true));
								
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
							System.out.println("  ERROR: Couldn't open translation file '" + ingredientNameTranslationFile.getFileName() + "'");
							newFile = true;
							ok = false;
						}
					}
					else {
						System.out.println("  WARNING: No translation file found.");
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
										String record = DrugMapping.escapeFieldValue(sourceIngredientName);
										record += "," + DrugMapping.escapeFieldValue(originalIngredientNameTranslation.get(sourceIngredientName));
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
								System.out.println("  ACTION REQUIRED: Please add the missing translations.");

								translationFile.println("SourceIngredientName,EnglishIngredientName");
								
								List<String> sortedSourceIngredientNames = new ArrayList<String>();
								sortedSourceIngredientNames.addAll(ingredientNameTranslation.keySet());
								Collections.sort(sortedSourceIngredientNames);
								for (String sourceIngredientName : sortedSourceIngredientNames) {
									String record = DrugMapping.escapeFieldValue(sourceIngredientName);
									record += "," + DrugMapping.escapeFieldValue(ingredientNameTranslation.get(sourceIngredientName));
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
				}
				catch (NoSuchElementException fileException) {
					System.out.println("  ERROR: " + fileException.getMessage());
					ok = false;
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
										record += (column == 0 ? "" : ",") + DrugMapping.escapeFieldValue(outputIngredient[column]);
									}
									gpkFullFile.println(record);
								}
							}
							else {
								String[] gpk = gpkMap.get(gpkCode);
								if (gpk != null) {
									String labelName = gpk[GPK_LabelName];
									String shortName = gpk[GPK_ShortName];
									String fullName = gpk[GPK_FullName];
									
									String name = fullName;
									if (name.equals("")) name = labelName;
									if (name.equals("")) name = shortName;
									
									if (!name.equals("")) {
										String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
										gpkIngredientRecord[OUTPUT_SourceCode]            = gpk[GPK_GPKCode];
										gpkIngredientRecord[OUTPUT_SourceName]            = name;
										gpkIngredientRecord[OUTPUT_SourceATCCode]         = gpk[GPK_ATCCode];
										gpkIngredientRecord[OUTPUT_SourceFormulation]     = gpk[GPK_PharmForm];
										gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpk[GPK_GPKCode]) ? gpkStatisticsMap.get(gpk[GPK_GPKCode]).toString() : "0");
										gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "No Ingredients";
										gpkIngredientRecord[OUTPUT_IngredientCode]        = "";
										gpkIngredientRecord[OUTPUT_IngredientName]        = "";
										gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
										gpkIngredientRecord[OUTPUT_Dosage]                = "";
										gpkIngredientRecord[OUTPUT_DosageUnit]            = "";
										gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
										gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
										gpkIngredientRecord[OUTPUT_CASNumber]             = "";
										
										String record = "";
										for (int column = 0; column < OUTPUT_ColumnCount; column++) {
											record += (column == 0 ? "" : ",") + DrugMapping.escapeFieldValue(gpkIngredientRecord[column]);
										}
										gpkFullFile.println(record);
									}
								}
							}
						}
						gpkFullFile.close();
					}
					catch (FileNotFoundException e) {
						System.out.println("  ERROR: Cannot create output file '" + gpkFullFileName + "'");
					}
				}
				else {
					System.out.println("  ERROR: No output to write.");
				}
			}
			else {
				System.out.println("  ERROR: Cannot open GPK file '" + gpkFile.getFileName() + "'");
				ok = false;
			}
			System.out.println(DrugMapping.getCurrentTime() + "   Done");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished" + (ok ? "" : " WITH ERRORS"));
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
	
	
	private String cleanupExtractedIngredientName(String ingredientName) {
		for (String word : wordsToRemove) {
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName);
/*
			ingredientName = ingredientName.replaceAll("\t", " ").trim();
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*$", ""));
			
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*$", ""));

			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("[\"][^\"]*[\"]", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" \\(.*\\) ", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" \\(.*\\)$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^\\(.*\\) ", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^\\(.*\\)$", ""));
			ingredientName = ingredientName.replaceAll("\\([)]*$", "").trim();
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("\\([^)]*$", ""));
			
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + ",", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + ", ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + ",", " "));

			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + " ", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",[0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[,]??[0-9]*[,]??[0-9]*[,]??[0-9]*" + word + "$", " "));
			
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + ",", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + ", ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + ",", " "));

			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + " ", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",[0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", [0-9]*[.]??[0-9]*[.]??[0-9]*[.]??[0-9]*" + word + "$", " "));
*/
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("^" + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" " + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" " + word + ",", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("," + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("," + word + ",", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + " ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + ", ", " "));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + ",", " "));

			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(" " + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll("," + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + "$", ""));
			ingredientName = StringUtilities.removeExtraSpaces(ingredientName.replaceAll(",$", ""));
			
			if (ingredientName.endsWith(" " + word)) {
				ingredientName = StringUtilities.removeExtraSpaces(ingredientName.substring(0, ingredientName.length() - word.length()));
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
				ingredientName = StringUtilities.removeExtraSpaces(ingredientName);
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
		//OLD String[] fullNameSplit = fullName.split(" ");
		//OLD if ((fullNameSplit.length > 2) && (!fullNameSplit[fullNameSplit.length - 1].trim().equals("")) && (DIGITS.contains(fullNameSplit[fullNameSplit.length - 1].trim().substring(0, 1)))) {
		//OLD 	doseString = fullNameSplit[fullNameSplit.length - 1].trim();
		//OLD }
		int lastSpaceIndex = fullName.lastIndexOf(" ");
		if (lastSpaceIndex > -1) {
			doseString = fullName.substring(lastSpaceIndex + 1);
		}
		return doseString;
	}
	
	
	private List<String[]> matchIngredients(List<String[]> leadingDrug, List<String[]> matchingDrug) {
		List<String[]> match = leadingDrug;
		if (leadingDrug.size() == matchingDrug.size()) {
			int matchCount = 0;
			match = new ArrayList<String[]>();
			for (int ingredientNr = 0; ingredientNr < leadingDrug.size(); ingredientNr++) {
				String[] leadingIngredient = leadingDrug.get(ingredientNr);
				match.add(null);
				for (String[] matchingIngredient : matchingDrug) {
					if (leadingIngredient[OUTPUT_IngredientCode].equals(matchingIngredient[OUTPUT_IngredientCode])) {
						match.set(ingredientNr, matchingIngredient);
						matchingDrug.remove(matchingIngredient);
						matchCount++;
						break;
					}
				}
			}
			if (matchingDrug.size() > 0) {
				for (int ingredientNr = 0; ingredientNr < leadingDrug.size(); ingredientNr++) {
					if (match.get(ingredientNr) == null) {
						match.set(ingredientNr, leadingDrug.get(ingredientNr));
						matchCount++;
						if (matchCount == leadingDrug.size()) {
							break;
						}
					}
				}
			}
		}
		return match;
	}
}
