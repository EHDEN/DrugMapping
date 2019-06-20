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

	private Map<Integer, List<String[]>> gskMap = new HashMap<Integer, List<String[]>>();

	
	public IPCIZIndexConversion(CDMDatabase database, InputFile gpkFile, InputFile gskFile, InputFile gnkFile) {

		System.out.println(DrugMapping.getCurrentTime() + " Converting ZIndex Files ...");

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
				header += "," + "Formulation";

				header += "," + "IngredientNameStatus";
				header += "," + "IngredientName";
				header += "," + "IngredientNameEnglish";
				header += "," + "Dosage";
				header += "," + "DosageUnit";
				header += "," + "CASNumber";
				
				gpkFullFile.println(header);

				if (gpkFile.openFile()) {
					System.out.println("  Creating Full GPK File ...");
					while (gpkFile.hasNext()) {
						Row row = gpkFile.next();
						String name = gpkFile.get(row, "FullName").trim();
						if (name.equals("")) name = gpkFile.get(row, "LabelName").trim();
						if (name.equals("")) name = gpkFile.get(row, "ShortName").trim();
						
						// Ignore empty names and names that start with a '*'
						if ((!name.equals("")) && (!name.substring(0, 1).equals("*"))) {
							
							String gpkRecord = gpkFile.get(row, "GPKCode");
							gpkRecord += "," + "\"" + name.replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + gpkFile.get(row, "ATCCode");
							gpkRecord += "," + "\"" + gpkFile.get(row, "PharmForm").replaceAll("\"", "\"\"") + "\"";
							
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
											for (int cellCount = 0; cellCount < (GSKColumnCount - 4); cellCount++) {
												gpkGskRecord += ",";
											}
											gpkFullFile.println(gpkGskRecord);
										}
									}
									else {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "Extracted";
										gpkGskRecord += "," + "\"" + name + "\"";
										for (int cellCount = 0; cellCount < (GSKColumnCount - 4); cellCount++) {
											gpkGskRecord += ",";
										}
										gpkFullFile.println(gpkGskRecord);
									}
								}
							}
							else {
								for (String[] gskObject : gskList) {
									if (!gskObject[GenericName].substring(0, 1).equals("*")) {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "ZIndex";
										gpkGskRecord += "," + "\"" + gskObject[GenericName].replaceAll("\"", "\"\"") + "\"";
										gpkGskRecord += ",";
										gpkGskRecord += "," + gskObject[Amount];
										gpkGskRecord += "," + gskObject[AmountUnit];
										gpkGskRecord += "," + gskObject[CASNumber];

										gpkFullFile.println(gpkGskRecord);
									}
									else {
										String gpkGskRecord = gpkRecord;
										gpkGskRecord += "," + "ZIndex";
										gpkGskRecord += ",";
										gpkGskRecord += ",";
										gpkGskRecord += "," + gskObject[Amount];
										gpkGskRecord += "," + gskObject[AmountUnit];
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