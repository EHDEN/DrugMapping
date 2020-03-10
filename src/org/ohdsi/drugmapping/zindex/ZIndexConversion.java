package org.ohdsi.drugmapping.zindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class ZIndexConversion extends Mapping {

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

	private Map<Integer, List<String[]>> gskMap = new HashMap<Integer, List<String[]>>();
	private Map<Integer, String[]> gnkMap = new HashMap<Integer, String[]>();
	private Map<String, Integer> gnkNameMap = new HashMap<String, Integer>();
	private Map<String, Integer> gpkStatisticsMap = new HashMap<String, Integer>();
	private List<String> wordsToRemove = new ArrayList<String>();
	private Map<String, List<String[]>> gpkIPCIMap = new HashMap<String, List<String[]>>();

	
	public ZIndexConversion(CDMDatabase database, InputFile gpkFile, InputFile gskFile, InputFile gnkFile, InputFile gpkStatsFile, InputFile wordsToRemoveFile, InputFile gpkIPCIFile) {
		
		boolean ok = true;

		System.out.println(DrugMapping.getCurrentTime() + " Converting ZIndex Files ...");
		
		if (ok && gskFile.openFile()) {
			System.out.println("  Loading ZIndex GSK File ...");
			while (gskFile.hasNext()) {
				Row row = gskFile.next();

				String[] record = new String[GSK_ColumnCount];
				record[GSK_GSKCode]       = gskFile.get(row, "GSKCode");
				record[GSK_PartNumber]    = gskFile.get(row, "PartNumber");
				record[GSK_Type]          = gskFile.get(row, "Type");
				record[GSK_Amount]        = gskFile.get(row, "Amount");
				record[GSK_AmountUnit]    = gskFile.get(row, "AmountUnit");
				record[GSK_GNKCode]       = gskFile.get(row, "GNKCode");
				record[GSK_GenericName]   = gskFile.get(row, "GenericName");
				record[GSK_CASNumber]     = gskFile.get(row, "CASNumber");

				record[GSK_CASNumber] = GenericMapping.uniformCASNumber(record[GSK_CASNumber]);

				int gskCode = Integer.valueOf(record[GSK_GSKCode]); 
				List<String[]> gskList = gskMap.get(gskCode);
				if (gskList == null) {
					gskList = new ArrayList<String[]>();
					gskMap.put(gskCode, gskList);
				}
				gskList.add(record);
			}
			System.out.println("  Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GSK file '" + gskFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && gnkFile.openFile()) {
			System.out.println("  Loading ZIndex GNK File ...");
			while (gnkFile.hasNext()) {
				Row row = gnkFile.next();

				String[] record = new String[GNK_ColumnCount];
				record[GNK_GNKCode]     = gnkFile.get(row, "GNKCode");
				record[GNK_Description] = gnkFile.get(row, "Description");
				record[GNK_CASCode]     = gnkFile.get(row, "CASCode");

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
			}
			System.out.println("  Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && gpkStatsFile.openFile()) {
			System.out.println("  Loading ZIndex GPK Statistics File ...");
			while (gpkStatsFile.hasNext()) {
				Row row = gpkStatsFile.next();

				String gpkCode  = gpkStatsFile.get(row, "GPKCode").trim();
				String gpkCount = gpkStatsFile.get(row, "GPKCount").trim();
				
				Integer count = null;
				try {
					count = Integer.valueOf(gpkCount);
					gpkStatisticsMap.put(gpkCode, count);
				}
				catch (NumberFormatException e) {
				}
			}
			System.out.println("  Done");
		}
		else {
			System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && wordsToRemoveFile.openFile()) {
			System.out.println("  Loading ZIndex Words To Ignore File ...");
			List<List<String>> wordsFoundToRemove = new ArrayList<List<String>>();
			while (wordsToRemoveFile.hasNext()) {
				Row row = wordsToRemoveFile.next();

				String word = wordsToRemoveFile.get(row, "Word").trim().toUpperCase();
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
			System.out.println("  Done");
		}
		else {
			System.out.println("  ERROR: Cannot load  Words To Ignore file '" + wordsToRemoveFile.getFileName() + "'");
			ok = false;
		}
		

		if (ok && gpkIPCIFile.openFile()) {
			System.out.println("  Loading ZIndex GPK IPCI Compositions File ...");
			while (gpkIPCIFile.hasNext()) {
				Row row = gpkIPCIFile.next();

				String[] record = new String[GPKIPCI_ColumnCount];
				record[GPKIPCI_GPKCode]    = gpkIPCIFile.get(row, "GPK").trim();
				record[GPKIPCI_PartNr]     = gpkIPCIFile.get(row, "PartNr").trim();
				record[GPKIPCI_Type]       = gpkIPCIFile.get(row, "Typ").trim();
				record[GPKIPCI_Amount]     = gpkIPCIFile.get(row, "Amount").trim();
				record[GPKIPCI_AmountUnit] = gpkIPCIFile.get(row, "AmountUnit").trim();
				record[GPKIPCI_GNKCode]    = gpkIPCIFile.get(row, "GNK").trim();
				record[GPKIPCI_GNKName]    = gpkIPCIFile.get(row, "GnkName").trim();
				record[GPKIPCI_CasNr]      = gpkIPCIFile.get(row, "CASNr").trim();
				record[GPKIPCI_BaseName]   = gpkIPCIFile.get(row, "BaseName").trim();
				record[GPKIPCI_Formula]    = gpkIPCIFile.get(row, "Formula").trim();
				
				List<String[]> gpkIPCIParts = gpkIPCIMap.get(record[GPKIPCI_GPKCode]);
				if (gpkIPCIParts == null) {
					gpkIPCIParts = new ArrayList<String[]>();
					gpkIPCIMap.put(record[GPKIPCI_GPKCode], gpkIPCIParts);
				}
				gpkIPCIParts.add(record);
			}
			System.out.println("  Done");
		}
		

		if (ok && gpkFile.openFile()) {
			System.out.println("  Creating ZIndex GPK Full File ...");

			String gpkFullFileName = DrugMapping.getCurrentPath() + "/ZIndex - GPK Full.csv";
			try {
				PrintWriter gpkFullFile = new PrintWriter(new File(gpkFullFileName));
				
				String header = "SourceCode";
				header += "," + "SourceName";
				header += "," + "SourceATCCode";
				header += "," + "SourceFormulation";
				header += "," + "SourceCount";

				header += "," + "IngredientNameStatus";
				header += "," + "IngredientName";
				header += "," + "IngredientNameEnglish";
				header += "," + "Dosage";
				header += "," + "DosageUnit";
				header += "," + "OrgDosage";
				header += "," + "OrgDosageUnit";
				header += "," + "CASNumber";
				
				gpkFullFile.println(header);
				
				while (gpkFile.hasNext()) {
					Row row = gpkFile.next();
					String name = gpkFile.get(row, "FullName").trim().toUpperCase();
					if (name.equals("")) name = gpkFile.get(row, "LabelName").trim().toUpperCase();
					if (name.equals("")) name = gpkFile.get(row, "ShortName").trim().toUpperCase();
					String gpkCode = gpkFile.get(row, "GPKCode");
					
					List<String[]> gpkIPCIIngredients = gpkIPCIMap.get(gpkCode);
					if (gpkIPCIIngredients != null) {
						// Overruled by IPCI derivation
						
						String gpkRecord = gpkCode;
						gpkRecord += "," + "\"" + name.replaceAll("\"", "\"\"") + "\"";
						gpkRecord += "," + gpkFile.get(row, "ATCCode");
						gpkRecord += "," + "\"" + gpkFile.get(row, "PharmForm").replaceAll("\"", "\"\"") + "\"";
						gpkRecord += "," + (gpkStatisticsMap.containsKey(gpkCode) ? gpkStatisticsMap.get(gpkCode) : "0");
						
						for (String[] gpkIPCIIngredient : gpkIPCIIngredients) {
							String gpkIngredientRecord = gpkRecord;
							gpkIngredientRecord += "," + "ZIndexIPCI";
							gpkIngredientRecord += "," + "\"" + gpkIPCIIngredient[GPKIPCI_GNKName].replaceAll("\"", "\"\"") + "\"";
							gpkIngredientRecord += ",";
							gpkIngredientRecord += "," + "\"" + gpkIPCIIngredient[GPKIPCI_Amount] + "\"";
							gpkIngredientRecord += "," + "\"" + gpkIPCIIngredient[GPKIPCI_AmountUnit] + "\"";
							gpkIngredientRecord += ",";
							gpkIngredientRecord += ",";
							gpkIngredientRecord += "," + GenericMapping.uniformCASNumber(gpkIPCIIngredient[GPKIPCI_CasNr]);

							gpkFullFile.println(gpkIngredientRecord);
						}
					}
					else {
						// Ignore empty names and names that start with a '*'
						if ((!name.equals("")) && (!name.substring(0, 1).equals("*"))) {
							
							String gpkRecord = gpkCode;
							gpkRecord += "," + "\"" + name.replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + gpkFile.get(row, "ATCCode");
							gpkRecord += "," + "\"" + gpkFile.get(row, "PharmForm").replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + (gpkStatisticsMap.containsKey(gpkCode) ? gpkStatisticsMap.get(gpkCode) : "0");
							
							List<String[]> gskList = null;
							
							if (!gpkFile.get(row, "GSKCode").equals("")) {
								int gskCode = Integer.valueOf(gpkFile.get(row, "GSKCode"));
								
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
										System.out.println("    WARNING: No active ingredient GSK records (GSKCode = " + gpkFile.get(row, "GSKCode") + ") found for GPK " + gpkFile.get(row, "GPKCode"));
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
								
								if (!name.equals("")) {
									if (name.substring(name.length() - 1).equals(",")) {
										name = name.substring(0, name.length() - 1);
									}
									if (name.contains("/")) {
										String[] nameSplit = name.split("/");
										for (String ingredientName : nameSplit) {
											ingredientName = cleanupExtractedIngredientName(ingredientName);
											
											if (ingredientName != null) {
												Integer gnkCode = gnkNameMap.get(ingredientName);
												if (gnkCode != null) {
													String[] gnkRecord = gnkMap.get(gnkCode);
													String gpkGskRecord = gpkRecord;
													gpkGskRecord += "," + "Mapped to GNK";
													gpkGskRecord += "," + "\"" + ingredientName + "\"";
													for (int cellCount = 0; cellCount < (GSK_ColumnCount - 3); cellCount++) {
														gpkGskRecord += ",";
													}
													gpkGskRecord += "," + gnkRecord[GNK_CASCode];
													gpkFullFile.println(gpkGskRecord);
												}
												else {
													String gpkGskRecord = gpkRecord;
													gpkGskRecord += "," + "Extracted";
													gpkGskRecord += "," + "\"" + ingredientName.trim() + "\"";
													for (int cellCount = 0; cellCount < (GSK_ColumnCount - 2); cellCount++) {
														gpkGskRecord += ",";
													}
													gpkFullFile.println(gpkGskRecord);
												}
											}
										}
									}
									else {
										name = cleanupExtractedIngredientName(name);
										
										if (name != null) {
											Integer gnkCode = gnkNameMap.get(name);
											if (gnkCode != null) {
												String[] gnkRecord = gnkMap.get(gnkCode);
												String gpkGskRecord = gpkRecord;
												gpkGskRecord += "," + "Mapped to GNK";
												gpkGskRecord += "," + "\"" + name + "\"";
												for (int cellCount = 0; cellCount < (GSK_ColumnCount - 3); cellCount++) {
													gpkGskRecord += ",";
												}
												gpkGskRecord += "," + gnkRecord[GNK_CASCode];
												gpkFullFile.println(gpkGskRecord);
											}
											else {
												String gpkGskRecord = gpkRecord;
												gpkGskRecord += "," + "Extracted";
												gpkGskRecord += "," + "\"" + name + "\"";
												for (int cellCount = 0; cellCount < (GSK_ColumnCount - 2); cellCount++) {
													gpkGskRecord += ",";
												}
												gpkFullFile.println(gpkGskRecord);
											}
										}
									}
								}
							}
							else {
								for (String[] gskObject : gskList) {
									String amount = gskObject[GSK_Amount];
									String amountUnit = gskObject[GSK_AmountUnit];
									// Extract unit from name
									if (name.lastIndexOf(" ") >= 0) {
										String strengthString = name.substring(name.lastIndexOf(" ")).trim();
										if ("1234567890".contains(strengthString.substring(0, 1))) {
											String strengthValueString = ""; 
											for (int charNr = 0; charNr < strengthString.length(); charNr++) {
												if ("1234567890,".contains(strengthString.subSequence(charNr, charNr + 1))) {
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
									
									if (!gskObject[GSK_GenericName].substring(0, 1).equals("*")) {
										// Cleanup ZIndex ingredient name
										String genericName = cleanupExtractedIngredientName(gskObject[GSK_GenericName]);
										
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "ZIndex";
										gpkGskRecord += "," + (genericName != null ? "\"" + genericName + "\"" : "\"" + gskObject[GSK_GenericName].replaceAll("\"", "\"\"") + "\"");
										gpkGskRecord += ",";
										gpkGskRecord += "," + "\"" + amount + "\"";
										gpkGskRecord += "," + "\"" + amountUnit + "\"";
										gpkGskRecord += "," + "\"" + gskObject[GSK_Amount] + "\"";
										gpkGskRecord += "," + "\"" + gskObject[GSK_AmountUnit] + "\"";
										gpkGskRecord += "," + gskObject[GSK_CASNumber];

										gpkFullFile.println(gpkGskRecord);
									}
									else {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "ZIndex";
										gpkGskRecord += ",";
										gpkGskRecord += ",";
										gpkGskRecord += "," + "\"" + amount + "\"";
										gpkGskRecord += "," + "\"" + amountUnit + "\"";
										gpkGskRecord += "," + "\"" + gskObject[GSK_Amount] + "\"";
										gpkGskRecord += "," + "\"" + gskObject[GSK_AmountUnit] + "\"";
										gpkGskRecord += "," + gskObject[GSK_CASNumber];

										gpkFullFile.println(gpkGskRecord);
									}
								}
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
}
