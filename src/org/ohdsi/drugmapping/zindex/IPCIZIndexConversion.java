package org.ohdsi.drugmapping.zindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class IPCIZIndexConversion extends Mapping {
	private static final int GSKColumnCount = 8;
	private static final int GSKCode        = 0;
	private static final int PartNumber     = 1;
	private static final int Type           = 2;
	private static final int Amount         = 3;
	private static final int AmountUnit     = 4;
	private static final int GSK_GNKCode    = 5;
	private static final int GenericName    = 6;
	private static final int CASNumber      = 7;

	private Map<String, Integer> gpkStatisticsMap = new HashMap<String, Integer>();
	private Map<Integer, List<String[]>> gskMap = new HashMap<Integer, List<String[]>>();

	
	public IPCIZIndexConversion(CDMDatabase database, InputFile gpkFile, InputFile gskFile, InputFile gnkFile, InputFile gpkStatsFile) {

		System.out.println(DrugMapping.getCurrentTime() + " Converting ZIndex Files ...");
		
		if (gpkStatsFile.openFile()) {
			System.out.println("  Loading GPK Statistics File ...");
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

		if (gskFile.openFile()) {
			System.out.println("  Loading GSK File ...");
			while (gskFile.hasNext()) {
				Row row = gskFile.next();

				String[] record = new String[GSKColumnCount];
				record[GSKCode]       = gskFile.get(row, "GSKCode");
				record[PartNumber]    = gskFile.get(row, "PartNumber");
				record[Type]          = gskFile.get(row, "Type");
				record[Amount]        = gskFile.get(row, "Amount");
				record[AmountUnit]    = gskFile.get(row, "AmountUnit");
				record[GSK_GNKCode]   = gskFile.get(row, "GNKCode");
				record[GenericName]   = gskFile.get(row, "GenericName");
				record[CASNumber]     = gskFile.get(row, "CASNumber");

				record[CASNumber] = record[CASNumber].replaceAll(" ", "").replaceAll("-", "");
				if (!record[CASNumber].equals("")) {
					record[CASNumber] = record[CASNumber].substring(0, record[CASNumber].length() - 3) + "-" + record[CASNumber].substring(record[CASNumber].length() - 3, record[CASNumber].length() - 1) + "-" + record[CASNumber].substring(record[CASNumber].length() - 1);
				}

				int gskCode = Integer.valueOf(record[GSKCode]); 
				List<String[]> gskList = gskMap.get(gskCode);
				if (gskList == null) {
					gskList = new ArrayList<String[]>();
					gskMap.put(gskCode, gskList);
				}
				gskList.add(record);
			}
			System.out.println("  Done");
			
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

				if (gpkFile.openFile()) {
					System.out.println("  Creating Full GPK File ...");
					while (gpkFile.hasNext()) {
						Row row = gpkFile.next();
						String name = gpkFile.get(row, "FullName").trim().toUpperCase();
						if (name.equals("")) name = gpkFile.get(row, "LabelName").trim().toUpperCase();
						if (name.equals("")) name = gpkFile.get(row, "ShortName").trim().toUpperCase();
						String gpkCode = gpkFile.get(row, "GPKCode");
						
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
										if (!gskObject[Type].equals("W")) {
											remove.add(gskObject);
										}
									}
									gskList.removeAll(remove);

									if (gskList.size() == 0) {
										gskList = null;
										System.out.println("    WARNING: No active ingredient GSK records (GSKCode = " + gpkFile.get(row, "GSKCode") + " found for GPK " + gpkFile.get(row, "GPKCode"));
									}
								}
							}
							
							if (gskList == null) {
								// Try to extract ingredients from name (separated by '/')
/*	
   NOT USED YET							
								// List of words to remove from extracted parts.
								// IMPORTANT:
								//   This is an ordered list. The words are removed in the order of the list.
								//   The appearance of the words are checked with surrounding parenthesis, with
								//   surrounding spaces, and at the end of the extracted part.
								List<String> wordsToRemove = new ArrayList<String>();

								wordsToRemove.add("PDR V INFVLST");
								wordsToRemove.add("PDR V OPLOSSING");
								
								wordsToRemove.add("APPLVLST");
								wordsToRemove.add("BALSEM");
								wordsToRemove.add("CAPSULE");
								wordsToRemove.add("CONC");
								wordsToRemove.add("CONCENTRAAT");
								wordsToRemove.add("CREME");
								wordsToRemove.add("DRAGEE");
								wordsToRemove.add("DRANK");
								wordsToRemove.add("DRUPPELS");
								wordsToRemove.add("EXTRA");
								wordsToRemove.add("EXTRACT");
								wordsToRemove.add("FORTE");
								wordsToRemove.add("GEL");
								wordsToRemove.add("GELEREND");
								wordsToRemove.add("GO");
								wordsToRemove.add("GRANULAAT");
								wordsToRemove.add("HUIDSPRAY");
								wordsToRemove.add("HYDROFIEL");
								wordsToRemove.add("HYDROFOOB");
								wordsToRemove.add("HYDROGEL");
								wordsToRemove.add("INFVLST");
								wordsToRemove.add("INJECTIEPOEDER");
								wordsToRemove.add("INJPDR");
								wordsToRemove.add("INJVLST");
								wordsToRemove.add("LOTION");
								wordsToRemove.add("MGA");
								wordsToRemove.add("MSR");
								wordsToRemove.add("OMHULD");
								wordsToRemove.add("OOGDRUPPELS");
								wordsToRemove.add("OOGDR");
								wordsToRemove.add("OOGWASSING");
								wordsToRemove.add("OOGZALF");
								wordsToRemove.add("ORAAL");
								wordsToRemove.add("PDR");
								wordsToRemove.add("POEDER");
								wordsToRemove.add("PREPAR");
								wordsToRemove.add("PREPARAAT");
								wordsToRemove.add("SHAMPOO");
								wordsToRemove.add("SMEERSEL");
								wordsToRemove.add("STERK");
								wordsToRemove.add("STROOP");
								wordsToRemove.add("SUBLINGUAAL");
								wordsToRemove.add("SUSP");
								wordsToRemove.add("SUSPENSIE");
								wordsToRemove.add("TABLET");
								wordsToRemove.add("TINCTUUR");
								wordsToRemove.add("VERNEVELVLST");
								wordsToRemove.add("VETZALF");
								wordsToRemove.add("VLOEIBAAR");
								wordsToRemove.add("ZALF");
								wordsToRemove.add("ZUIGTABLET");
								wordsToRemove.add("ZUURBINDEND");

								wordsToRemove.add("WONDSPOELING");
								wordsToRemove.add("ZETP");
								wordsToRemove.add("ZETP + ZALF");
								wordsToRemove.add("OUD");
								wordsToRemove.add("KAUWTABLET");
								wordsToRemove.add("MODSPOELING");
								wordsToRemove.add("ZETPIL");
								wordsToRemove.add("ANTIRHEUMATICUM");
								wordsToRemove.add("ROCHE");
								wordsToRemove.add("CUTAAN");
								wordsToRemove.add("INJ");
								wordsToRemove.add("KAUWT");
								wordsToRemove.add("INJV");
								wordsToRemove.add("SCHUDMIXTUUR");
								wordsToRemove.add("NPBI");
								wordsToRemove.add("VOOR");
								wordsToRemove.add("KRISTALLIJN");
								wordsToRemove.add("BESILAAT");
								wordsToRemove.add("KLYSMA");
								wordsToRemove.add("INFUSIEPOEDER");
								wordsToRemove.add("BRUISGRANULAAT");
								wordsToRemove.add("MET ELECTR INFVLST");
								wordsToRemove.add("SACHET");
								wordsToRemove.add("OLIE");
								wordsToRemove.add("CLR");
								wordsToRemove.add("ESSENCE");
								wordsToRemove.add("CREME DE");
								wordsToRemove.add("ONVERDEELD");
								wordsToRemove.add("KUNSTMATIG");
								wordsToRemove.add("INHALATIEVLST");
								wordsToRemove.add("OROM");
								wordsToRemove.add("OROMUCOSAAL");
								wordsToRemove.add("INHALATIEPOEDER");
								wordsToRemove.add("PERIFEER");
								wordsToRemove.add("PCH");
								wordsToRemove.add("PDR VOOR APPLVLST");
								wordsToRemove.add("TRACHEAAL");
								wordsToRemove.add("POEDER V CEMENT");
								wordsToRemove.add("DO");
								wordsToRemove.add("ML");
								wordsToRemove.add("MG");
								wordsToRemove.add("PAR");
								wordsToRemove.add("SPOELING");
								wordsToRemove.add("STARTVERPAKKING");
								wordsToRemove.add("PD-OPL");
								wordsToRemove.add("KRUIDENTHEE");
								wordsToRemove.add("THEE");
								wordsToRemove.add("NEUSSPRAY");
								wordsToRemove.add("UG");
								wordsToRemove.add("UUR");
								wordsToRemove.add("XXXXX");
								wordsToRemove.add("XXXXX");
								wordsToRemove.add("XXXXX");
*/
								
								if (name.contains(" ")) {
									name = name.substring(0, name.indexOf(" "));
								}
								if (!name.equals("")) {
									if (name.substring(name.length() - 1).equals(",")) {
										name = name.substring(0, name.length() - 1);
									}
									if (name.contains("/")) {
										String[] nameSplit = name.split("/");
										for (String ingredientName : nameSplit) {
											String gpkGskRecord = gpkRecord;
											gpkGskRecord += "," + "Extracted";
											gpkGskRecord += "," + "\"" + ingredientName.trim() + "\"";
											for (int cellCount = 0; cellCount < (GSKColumnCount - 2); cellCount++) {
												gpkGskRecord += ",";
											}
											gpkFullFile.println(gpkGskRecord);
										}
									}
									else {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "Extracted";
										gpkGskRecord += "," + "\"" + name + "\"";
										for (int cellCount = 0; cellCount < (GSKColumnCount - 2); cellCount++) {
											gpkGskRecord += ",";
										}
										gpkFullFile.println(gpkGskRecord);
									}
								}
							}
							else {
								for (String[] gskObject : gskList) {
									String amount = gskObject[Amount];
									String amountUnit = gskObject[AmountUnit];
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
									
									if (!gskObject[GenericName].substring(0, 1).equals("*")) {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "ZIndex";
										gpkGskRecord += "," + "\"" + gskObject[GenericName].replaceAll("\"", "\"\"") + "\"";
										gpkGskRecord += ",";
										gpkGskRecord += "," + "\"" + amount + "\"";
										gpkGskRecord += "," + "\"" + amountUnit + "\"";
										gpkGskRecord += "," + "\"" + gskObject[Amount] + "\"";
										gpkGskRecord += "," + "\"" + gskObject[AmountUnit] + "\"";
										gpkGskRecord += "," + gskObject[CASNumber];

										gpkFullFile.println(gpkGskRecord);
									}
									else {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "ZIndex";
										gpkGskRecord += ",";
										gpkGskRecord += ",";
										gpkGskRecord += "," + "\"" + amount + "\"";
										gpkGskRecord += "," + "\"" + amountUnit + "\"";
										gpkGskRecord += "," + "\"" + gskObject[Amount] + "\"";
										gpkGskRecord += "," + "\"" + gskObject[AmountUnit] + "\"";
										gpkGskRecord += "," + gskObject[CASNumber];

										gpkFullFile.println(gpkGskRecord);
									}
								}
							}
						}
					}
					gpkFullFile.close();
					
					
					System.out.println("  Done");
				}
				else {
					System.out.println("  ERROR: Cannot open GPK file '" + gnkFile.getFileName() + "'");
				}
				
			} catch (FileNotFoundException e) {
				System.out.println("  ERROR: Cannot create output file '" + gpkFullFileName + "'");
			}
		}
		else {
			System.out.println("  ERROR: Cannot load GSK file '" + gskFile.getFileName() + "'");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
}
