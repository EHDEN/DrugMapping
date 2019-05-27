package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
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

	private static final int GNKColumnCount = 3;
	private static final int GNKCode        = 0;
	private static final int Description    = 1;
	private static final int CASCode        = 2;

	private Map<Integer, List<String[]>> gskMap = new HashMap<Integer, List<String[]>>();
	private Map<Integer, String[]> gnkMap = new HashMap<Integer, String[]>();
	private Set<String> units = new HashSet<String>();

	
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
				
				String unit = gskFile.get(row, "AmountUnit").trim();
				if (unit.contains("/")) {
					String[] unitSplit = unit.split("/");
					for (String subUnit : unitSplit) {
						subUnit = subUnit.trim().toLowerCase();
						if (!subUnit.trim().equals("")) {
							units.add(subUnit.trim().toLowerCase());
						}
					}
				}
				else {
					if (!unit.trim().equals("")) {
						units.add(unit.trim().toLowerCase());
					}
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

			if (gnkFile.openFile()) {
				System.out.println("  Loading GNK File ...");
				while (gnkFile.hasNext()) {
					Row row = gnkFile.next();
					
					String[] record = new String[GNKColumnCount];
					record[GNKCode]       = gnkFile.get(row, "GNKCode");
					record[Description]   = gnkFile.get(row, "Description");
					record[CASCode]       = gnkFile.get(row, "CASCode");

					int gnkCode = Integer.valueOf(record[GNKCode]); 
					gnkMap.put(gnkCode, record);
				}
				System.out.println("  Done");
				
				String gpkFullFileName = DrugMapping.getCurrentPath() + "/ZIndex - GPK Full.csv";
				try {
					PrintWriter gpkFullFile = new PrintWriter(new File(gpkFullFileName));
					
					String header = "GPKCode";
					header += "," + "MemoCode";
					header += "," + "LabelName";
					header += "," + "ShortName";
					header += "," + "FullName";
					header += "," + "ATCCode";
					header += "," + "DDDPerHPKUnit";
					header += "," + "PrescriptionDays";
					header += "," + "HPKMG";
					header += "," + "HPKMGUnit";
					header += "," + "PharmForm";
					
					header += "," + "GSKCode";
					header += "," + "PartNumber";
					header += "," + "Type";
					header += "," + "Amount";
					header += "," + "AmountUnit";
					header += "," + "GenericName";
					header += "," + "CASNumber";
					
					header += "," + "GNKCode";
					header += "," + "Description";
					header += "," + "CASCode";
					
					gpkFullFile.println(header);

					if (gpkFile.openFile()) {
						System.out.println("  Creating Full GPK File ...");
						while (gpkFile.hasNext()) {
							Row row = gpkFile.next();
							
							String gpkRecord = gpkFile.get(row, "GPKCode");
							gpkRecord += "," + "\"" + gpkFile.get(row, "MemoCode").replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + "\"" + gpkFile.get(row, "LabelName").replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + "\"" + gpkFile.get(row, "ShortName").replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + "\"" + gpkFile.get(row, "FullName").replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + gpkFile.get(row, "ATCCode");
							gpkRecord += "," + gpkFile.get(row, "DDDPerHPKUnit");
							gpkRecord += "," + gpkFile.get(row, "PrescriptionDays");
							gpkRecord += "," + gpkFile.get(row, "HPKMG");
							gpkRecord += "," + gpkFile.get(row, "HPKMGUnit");
							gpkRecord += "," + "\"" + gpkFile.get(row, "PharmForm").replaceAll("\"", "\"\"") + "\"";
							gpkRecord += "," + gpkFile.get(row, "GSKCode");
							
							String unit = gpkFile.get(row, "HPKMGUnit").trim();
							if (unit.contains("/")) {
								String[] unitSplit = unit.split("/");
								for (String subUnit : unitSplit) {
									subUnit = subUnit.trim().toLowerCase();
									if (!subUnit.trim().equals("")) {
										units.add(subUnit.trim().toLowerCase());
									}
								}
							}
							else {
								if (!unit.trim().equals("")) {
									units.add(unit.trim().toLowerCase());
								}
							}
							
							if (!gpkFile.get(row, "GSKCode").equals("")) {
								int gskCode = Integer.valueOf(gpkFile.get(row, "GSKCode"));
								
								List<String[]> gskList = gskMap.get(gskCode);
								if (gskList == null) {
									System.out.println("    WARNING: No GSK records (GSKCode = " + gpkFile.get(row, "GSKCode") + " found for GPK " + gpkFile.get(row, "GPKCode"));
									for (int cellCount = 0; cellCount < (GSKColumnCount + GNKColumnCount - 2); cellCount++) {
										gpkRecord += ",";
									}
									gpkFullFile.println(gpkRecord);
								}
								else {
									// Remove non-active ingredients
									List<String[]> remove = new ArrayList<String[]>();
									for (String[] gskObject : gskList) {
										if (!gskObject[Type].equals("W")) {
											remove.add(gskObject);
										}
									}
									gskList.removeAll(remove);

									if (gskList.size() == 0) {
										System.out.println("    WARNING: No active ingredient GSK records (GSKCode = " + gpkFile.get(row, "GSKCode") + " found for GPK " + gpkFile.get(row, "GPKCode"));
										for (int cellCount = 0; cellCount < (GSKColumnCount + GNKColumnCount - 2); cellCount++) {
											gpkRecord += ",";
										}
										gpkFullFile.println(gpkRecord);
									}
									else {
										for (String[] gskObject : gskList) {
											String gpkGskGnkRecord = gpkRecord;
											gpkGskGnkRecord += "," + gskObject[PartNumber];
											gpkGskGnkRecord += "," + gskObject[Type];
											gpkGskGnkRecord += "," + gskObject[Amount];
											gpkGskGnkRecord += "," + gskObject[AmountUnit];
											gpkGskGnkRecord += "," + "\"" + gskObject[GenericName].replaceAll("\"", "\"\"") + "\"";
											gpkGskGnkRecord += "," + gskObject[CASNumber];
											gpkGskGnkRecord += "," + gskObject[GNKCode];
											
											if (!gskObject[GNKCode].equals("")) {
												int gnkCode = Integer.valueOf(gskObject[GNKCode]);
												
												String[] gnkObject = gnkMap.get(gnkCode);
												if (gnkObject == null) {
													System.out.println("    WARNING: No GNK record (GNKCode = " + gskObject[GNKCode] + " found for GSK " + gskObject[GSKCode] + " of GPK " + gpkFile.get(row, "GPKCode"));
													for (int cellCount = 0; cellCount < (GNKColumnCount - 1); cellCount++) {
														gpkGskGnkRecord += ",";
													}
													gpkFullFile.println(gpkGskGnkRecord);
												}
												else {
													gpkGskGnkRecord += "," + "\"" + gnkObject[Description].replaceAll("\"", "\"\"") + "\"";
													gpkGskGnkRecord += "," + gnkObject[CASCode];
													gpkFullFile.println(gpkGskGnkRecord);
												}
											}
											else {
												System.out.println("    WARNING: No GNK code found for GSK " + gskObject[GSKCode] + " of GPK " + gpkFile.get(row, "GPKCode"));
												for (int cellCount = 0; cellCount < (GNKColumnCount - 1); cellCount++) {
													gpkGskGnkRecord += ",";
												}
												gpkFullFile.println(gpkGskGnkRecord);
											}
										}
									}
								}
							}
							else {
								System.out.println("    WARNING: No GSK code found for GPK " + gpkFile.get(row, "GPKCode"));
								for (int cellCount = 0; cellCount < (GSKColumnCount + GNKColumnCount - 2); cellCount++) {
									gpkRecord += ",";
								}
								gpkFullFile.println(gpkRecord);
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
				System.out.println("  ERROR: Cannot load GNK file '" + gnkFile.getFileName() + "'");
			}
		}
		else {
			System.out.println("  ERROR: Cannot load GSK file '" + gskFile.getFileName() + "'");
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
		
		
		// Write CDM units to file
		String cdmUnitsFileNameShort = "CDM - Units.csv";
		System.out.println(DrugMapping.getCurrentTime() + " Write CDM units to file '" + cdmUnitsFileNameShort + "' ...");

		String query = null;
		
		query  = "select distinct * from (";
		query += " " + "select distinct amount_unit.*";
		query += " " + "from " + database.getVocabSchema() + ".drug_strength";
		query += " " + "left outer join " + database.getVocabSchema() + ".concept amount_unit";
		query += " " + "on amount_unit_concept_id = concept_id";
		query += " " + "union all";
		query += " " + "select distinct numerator_unit.*";
		query += " " + "from " + database.getVocabSchema() + ".drug_strength";
		query += " " + "left outer join " + database.getVocabSchema() + ".concept numerator_unit";
		query += " " + "on numerator_unit_concept_id = concept_id";
		query += " " + "union all";
		query += " " + "select distinct denominator_unit.*";
		query += " " + "from " + database.getVocabSchema() + ".drug_strength";
		query += " " + "left outer join " + database.getVocabSchema() + ".concept denominator_unit";
		query += " " + "on denominator_unit_concept_id = concept_id";
		query += " " + ") units";
		query += " " + "where domain_id = 'Unit'";
		query += " " + "and   concept_class_id = 'Unit'";
		query += " " + "and   standard_concept = 'S'";
		query += " " + "and   invalid_reason is null";
		query += " " + "order by concept_name";
		
		if (database.excuteQuery(query)) {
			if (database.hasNext()) {
				String cdmUnitsFileName = DrugMapping.getCurrentPath() + "/" + cdmUnitsFileNameShort;
				try {
					PrintWriter cdmUnitsFile = new PrintWriter(new File(cdmUnitsFileName));
					
					String header = "concept_id";
					header += "," + "concept_name";
					
					cdmUnitsFile.println(header);
					
					while (database.hasNext()) {
						Row queryRow = database.next();
						
						String concept_id   = queryRow.get("concept_id");
						String concept_name = queryRow.get("concept_name");
						
						String record = concept_id;
						record += "," + concept_name;
						
						cdmUnitsFile.println(record);
						
						concept_name = concept_name.trim().toLowerCase();
					}
					
					cdmUnitsFile.close();
				} catch (FileNotFoundException e) {
					System.out.println("  ERROR: Cannot create output file '" + cdmUnitsFileName + "'");
				}
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
		
		String gpkUnitsFileNameShort = "ZIndex - Units.csv";
		System.out.println(DrugMapping.getCurrentTime() + " Write ZIndex units to file '" + gpkUnitsFileNameShort + "' ...");
		String gpkUnitsFileName = DrugMapping.getCurrentPath() + "/" + gpkUnitsFileNameShort;

		System.out.println("    Get existing concept mapping ...");
		Map<String, String> unitConceptsMap = new HashMap<String, String>();

		File conceptsFile = new File(gpkUnitsFileName);
		if (conceptsFile.exists()) {
			ReadCSVFileWithHeader unitConceptsFile = new ReadCSVFileWithHeader(gpkUnitsFileName, ',', '"');
			Iterator<Row> unitConceptsFileIterator = unitConceptsFile.iterator();
			
			while (unitConceptsFileIterator.hasNext()) {
				Row row = unitConceptsFileIterator.next();
				String unit       = row.get("ZIndexUnit");
				String concept_id = row.get("concept_id");
				
				if ((!unit.trim().equals("")) && (!concept_id.trim().equals(""))) {
					unitConceptsMap.put(unit, concept_id);
				}
			}
		}
		else {
			System.out.println("        No existing mapping found.");
		}
		System.out.println("    Done");

		System.out.println("    Writing file '" + gpkUnitsFileNameShort + "' ...");
		try {
			PrintWriter gpkUnitsFile = new PrintWriter(new File(gpkUnitsFileName));
			
			String header = "ZIndexUnit";
			header += "," + "concept_id";
			
			gpkUnitsFile.println(header);
			
			List<String> unitsList = new ArrayList<String>(units);
			Collections.sort(unitsList);
			for (String unit : unitsList) {
				String record = unit;
				record += "," + (unitConceptsMap.containsKey(unit) ? unitConceptsMap.get(unit) : "");
				gpkUnitsFile.println(record);
			}
			
			gpkUnitsFile.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("  ERROR: Cannot create output file '" + gpkUnitsFileName + "'");
		}
		System.out.println("    Done");
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
}
