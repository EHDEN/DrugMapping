package org.ohdsi.drugmapping.zindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.Mapping;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingNumberUtilities;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;
import org.ohdsi.utilities.files.Row;

public class ZIndexConversion extends Mapping {
	private static final boolean IGNORE_EMPTY_GPK_NAMES   = false;
	private static final boolean IGNORE_STARRED_GPK_NAMES = false;
	
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
	
	private static final int OUTPUT_ColumnCount           = 13;
	private static final int OUTPUT_SourceCode            =  0;
	private static final int OUTPUT_SourceName            =  1;
	private static final int OUTPUT_SourceATCCode         =  2;
	private static final int OUTPUT_SourceFormulation     =  3;
	private static final int OUTPUT_SourceCount           =  4;
	private static final int OUTPUT_IngredientNameStatus  =  5;
	private static final int OUTPUT_IngredientCode        =  6;
	private static final int OUTPUT_IngredientName        =  7;
	//private static final int OUTPUT_IngredientNameEnglish =  8;
	private static final int OUTPUT_Dosage                =  8;
	private static final int OUTPUT_DosageUnit            =  9;
	private static final int OUTPUT_OrgDosage             = 10;
	private static final int OUTPUT_OrgDosageUnit         = 11;
	private static final int OUTPUT_CASNumber             = 12;

	private boolean IPCIDerivation = false;
	private List<Integer> gpkList = new ArrayList<Integer>();
	private Map<Integer, String[]> gpkMap = new HashMap<Integer, String[]>();
	private Map<Integer, List<String[]>> gskMap = new HashMap<Integer, List<String[]>>();
	private Map<Integer, String[]> gnkMap = new HashMap<Integer, String[]>();
	private Map<String, Integer> gnkNameMap = new HashMap<String, Integer>();
	private Map<Integer, List<Integer>> gnkOneIngredientGpkMap = new HashMap<Integer,List<Integer>>();
	private Map<String, Integer> gpkStatisticsMap = new HashMap<String, Integer>();
	private List<String> wordsToRemove = new ArrayList<String>();
	private Map<String, String> ingredientNameTranslation = new HashMap<String, String>();
	private Map<String, List<String[]>> gpkIPCIMap = new HashMap<String, List<String[]>>();
	private Map<Integer, List<String[]>> outputMap = new HashMap<Integer, List<String[]>>();

	
	@SuppressWarnings("unused")
	public ZIndexConversion(InputFile gpkFile, InputFile gskFile, InputFile gnkFile, InputFile gpkStatsFile, InputFile gpkIPCIFile) {	
		boolean ok = true;
		
		String translationType = null; 

		System.out.println(DrugMappingDateUtilities.getCurrentTime() + " Converting ZIndex Files ...");
		

		// Load GNK file
		if (ok) {
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Loading ZIndex GNK File ...");
			try {
				if (gnkFile.openFile()) {
					while (gnkFile.hasNext()) {
						Row row = gnkFile.next();

						String[] gnk = new String[GNK_ColumnCount];
						gnk[GNK_GNKCode]         = DrugMappingStringUtilities.removeExtraSpaces(gnkFile.get(row, "GNKCode", true));
						gnk[GNK_Description]     = DrugMappingStringUtilities.removeExtraSpaces(gnkFile.get(row, "Description", true));
						gnk[GNK_CASNumber]       = DrugMappingStringUtilities.removeExtraSpaces(gnkFile.get(row, "CASNumber", true));
						gnk[GNK_BaseName]        = DrugMappingStringUtilities.removeExtraSpaces(gnkFile.get(row, "BaseName", true));
						gnk[GNK_ChemicalFormula] = DrugMappingStringUtilities.removeExtraSpaces(gnkFile.get(row, "ChemicalFormula", true));

						gnk[GNK_CASNumber]       = DrugMappingNumberUtilities.uniformCASNumber(gnk[GNK_CASNumber]);

						int gnkCode = Integer.valueOf(gnk[GNK_GNKCode]);
						gnkMap.put(gnkCode, gnk);
						if (gnkNameMap.get(gnk[GNK_Description]) == null) {
							gnkNameMap.put(gnk[GNK_Description], Integer.valueOf(gnk[GNK_GNKCode]));
						}
						else {
							System.out.println("  ERROR: Duplicate GNK name '" + gnk[GNK_Description] + "' (" + gnkNameMap.get(gnk[GNK_Description]) + " <-> " + gnk[GNK_GNKCode] + ")");
							ok = false;
						}
						
						// Store ingredient name for translation
						if (!gnk[GNK_Description].equals("")) {
							ingredientNameTranslation.put(gnk[GNK_Description], null);
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
		}

		
		// Load GSK file
		if (ok) {
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Loading ZIndex GSK File ...");
			try {
				if (gskFile.openFile()) {
					while (gskFile.hasNext()) {
						Row row = gskFile.next();

						String[] gsk = new String[GSK_ColumnCount];
						gsk[GSK_GSKCode]       = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "GSKCode", true));
						gsk[GSK_PartNumber]    = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "PartNumber", true));
						gsk[GSK_Type]          = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "Type", true).trim());
						gsk[GSK_Amount]        = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "Amount", true));
						gsk[GSK_AmountUnit]    = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "AmountUnit", true));
						gsk[GSK_GNKCode]       = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "GNKCode", true));
						gsk[GSK_GenericName]   = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "GenericName", true));
						gsk[GSK_CASNumber]     = DrugMappingStringUtilities.removeExtraSpaces(gskFile.get(row, "CASNumber", true));

						gsk[GSK_CASNumber] = DrugMappingNumberUtilities.uniformCASNumber(gsk[GSK_CASNumber]);

						int gskCode = Integer.valueOf(gsk[GSK_GSKCode]); 
						List<String[]> gskList = gskMap.get(gskCode);
						if (gskList == null) {
							gskList = new ArrayList<String[]>();
							gskMap.put(gskCode, gskList);
						}
						gskList.add(gsk);
						
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
		}
		
		
		// Load GPK file
		if (ok) {
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Loading ZIndex GPK File ...");
			try {
				if (gpkFile.openFile()) {
					while (gpkFile.hasNext()) {
						Row row = gpkFile.next();

						String[] gpk = new String[GPK_ColumnCount];
						gpk[GPK_GPKCode]          = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "GPKCode", true));
						gpk[GPK_MemoCode]         = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "MemoCode", true));
						gpk[GPK_LabelName]        = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "LabelName", true));
						gpk[GPK_ShortName]        = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "ShortName", true));
						gpk[GPK_FullName]         = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "FullName", true));
						gpk[GPK_ATCCode]          = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "ATCCode", true));
						gpk[GPK_GSKCode]          = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "GSKCode", true));
						gpk[GPK_DDDPerHPKUnit]    = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "DDDPerHPKUnit", true));
						gpk[GPK_PrescriptionDays] = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "PrescriptionDays", true));
						gpk[GPK_HPKMG]            = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "HPKMG", true));
						gpk[GPK_HPKMGUnit]        = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "HPKMGUnit", true));
						gpk[GPK_PharmForm]        = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "PharmForm", true));
						gpk[GPK_BasicUnit]        = DrugMappingStringUtilities.removeExtraSpaces(gpkFile.get(row, "BasicUnit", true));

						int gpkCode = Integer.valueOf(gpk[GPK_GPKCode]);
						
						gpkMap.put(gpkCode, gpk);
						gpkList.add(gpkCode);

						if (!gpk[GPK_GSKCode].equals("")) {
							int gskCode = Integer.valueOf(gpk[GPK_GSKCode]);
							List<String[]> gskList = gskMap.get(gskCode);
							if ((gskList != null) && (gskList.size() == 1)) {
								String[] gsk = gskList.get(0);
								if (!gsk[GSK_GNKCode].equals("")) {
									int gnkCode = Integer.valueOf(gsk[GSK_GNKCode]);
									List<Integer> gpkList = gnkOneIngredientGpkMap.get(gnkCode);
									if (gpkList == null) {
										gpkList = new ArrayList<Integer>();
										gnkOneIngredientGpkMap.put(gnkCode, gpkList);
									}
									gpkList.add(gpkCode);
								}
							}
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
		}
		

		// Load GPK Statistics File
		if (ok) {
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Loading ZIndex GPK Statistics File ...");
			try {
				if (gpkStatsFile.openFile()) {
					while (gpkStatsFile.hasNext()) {
						Row row = gpkStatsFile.next();

						String gpkCode  = DrugMappingStringUtilities.removeExtraSpaces(gpkStatsFile.get(row, "GPKCode", true));
						String gpkCount = DrugMappingStringUtilities.removeExtraSpaces(gpkStatsFile.get(row, "GPKCount", true));
						
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
		}
		
		// Load GPK IPCI Compositions File
		if (ok) {
			if ((gpkIPCIFile != null) && gpkIPCIFile.isSelected()) {
				try {
					System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Loading ZIndex GPK IPCI Compositions File ...");
					if (gpkIPCIFile.openFile()) {
						
						IPCIDerivation = true;
						
						while (gpkIPCIFile.hasNext()) {
							Row row = gpkIPCIFile.next();

							String[] record = new String[GPKIPCI_ColumnCount];
							record[GPKIPCI_GPKCode]         = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "GPKCode", true));
							record[GPKIPCI_PartNumber]      = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "PartNumber", true));
							record[GPKIPCI_Type]            = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "Type", true));
							record[GPKIPCI_Amount]          = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "Amount", true));
							record[GPKIPCI_AmountUnit]      = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "AmountUnit", true));
							record[GPKIPCI_GNKCode]         = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "GNKCode", true));
							record[GPKIPCI_GNKName]         = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "GenericName", true));
							record[GPKIPCI_CASNumber]       = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "CASNumber", true));
							record[GPKIPCI_BaseName]        = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "BaseName", true));
							record[GPKIPCI_ChemicalFormula] = DrugMappingStringUtilities.removeExtraSpaces(gpkIPCIFile.get(row, "ChemicalFormula", true));
							
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
						
						for (String gpkCodeString : gpkIPCIMap.keySet()) {
							int gpkCode = Integer.valueOf(gpkCodeString);
							List<String[]> gpkIPCIParts = gpkIPCIMap.get(gpkCodeString);
							if (gpkIPCIParts.size() == 1) {
								String gnkCodeString = gpkIPCIParts.get(0)[GPKIPCI_GNKCode];
								if (!gnkCodeString.equals("")) {
									int gnkCode = Integer.valueOf(gnkCodeString);
									List<Integer> gpksWithSameIngredient = gnkOneIngredientGpkMap.get(gnkCode);
									if (gpksWithSameIngredient == null) {
										gpksWithSameIngredient = new ArrayList<Integer>();
										gnkOneIngredientGpkMap.put(gnkCode, gpksWithSameIngredient);
									}
									if (!gpksWithSameIngredient.contains(gpkCode)) {
										gpksWithSameIngredient.add(gpkCode);
									}
								}
							}
						}
					}
					System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
				}
				catch (NoSuchElementException fileException) {
					System.out.println("  ERROR: " + fileException.getMessage());
					ok = false;
				}
			}
			else {
				System.out.println(DrugMappingDateUtilities.getCurrentTime() + "     No GPK IPCI Compositions File used.");
			}
		}
		

		// Analyze ZIndex
		if (ok) {
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Analyzing ZIndex ...");
			for (int gpkCode : gpkList) {
				String[] gpk = gpkMap.get(gpkCode);
				
				// When no ATC-code try to get it from other GPK's with one and the same ingredient.
				if (gpk[GPK_ATCCode].equals("")) {
					String foundATC = null;
					if (!gpk[GPK_GSKCode].equals("")) {
						int gskCode = Integer.valueOf(gpk[GPK_GSKCode]);
						List<String[]> gskList = gskMap.get(gskCode);
						if ((gskList != null) && (gskList.size() == 1)) {
							String[] gsk = gskList.get(0);
							if (!gsk[GSK_GNKCode].equals("")) {
								foundATC = getAtcOfSingleIngredientGPK(gsk[GSK_GNKCode]);
							}
						}
					}
					if (foundATC != null) {
						gpk[GPK_ATCCode] = foundATC;
					}
				}

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
					
					// When no ATC-code try to get it from other GPK's with one and the same ingredient.
					if ((gpkIPCIIngredients.size() == 1) && atcCode.equals("") && (!gpkIPCIIngredients.get(0)[GPKIPCI_GNKCode].equals(""))) {
						String foundATC = getAtcOfSingleIngredientGPK(gpkIPCIIngredients.get(0)[GPKIPCI_GNKCode]);
						if (foundATC != null) {
							atcCode = foundATC;
						}
					}
					
					for (String[] gpkIPCIIngredient : gpkIPCIIngredients) {
						String ingredientNumeratorUnit = gpkIPCIIngredient[GPKIPCI_AmountUnit];
						String ingredientDenominatorUnit = basicUnit.equals("Stuk") ? "" : basicUnit;
						String ingredientUnit = ingredientDenominatorUnit.equals("") ? ingredientNumeratorUnit : (ingredientNumeratorUnit + "/" + ingredientDenominatorUnit);
						
						String[] gpkIngredientRecord = new String[OUTPUT_ColumnCount];
						gpkIngredientRecord[OUTPUT_SourceCode]            = gpkCodeString;
						gpkIngredientRecord[OUTPUT_SourceName]            = name;
						gpkIngredientRecord[OUTPUT_SourceATCCode]         = atcCode;
						gpkIngredientRecord[OUTPUT_SourceFormulation]     = pharmForm;
						gpkIngredientRecord[OUTPUT_SourceCount]           = (gpkStatisticsMap.containsKey(gpkCodeString) ? gpkStatisticsMap.get(gpkCodeString).toString() : "0");
						gpkIngredientRecord[OUTPUT_IngredientNameStatus]  = "ZIndexIPCI";
						gpkIngredientRecord[OUTPUT_IngredientCode]        = gpkIPCIIngredient[GPKIPCI_GNKCode];
						gpkIngredientRecord[OUTPUT_IngredientName]        = gpkIPCIIngredient[GPKIPCI_GNKName];
						gpkIngredientRecord[OUTPUT_Dosage]                = gpkIPCIIngredient[GPKIPCI_Amount];
						gpkIngredientRecord[OUTPUT_DosageUnit]            = ingredientUnit;
						gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
						gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
						gpkIngredientRecord[OUTPUT_CASNumber]             = DrugMappingNumberUtilities.uniformCASNumber(gpkIPCIIngredient[GPKIPCI_CASNumber]);

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
							String ingredientDenominatorUnit = basicUnit.equals("Stuk") ? "" : basicUnit;
							String amountUnit = ingredientDenominatorUnit.equals("") ? ingredientNumeratorUnit : (ingredientNumeratorUnit + "/" + ingredientDenominatorUnit);
							
							String gnkCode = gskObject[GSK_GNKCode];
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
							String denominatorUnit = DrugMappingStringUtilities.removeExtraSpaces((((doseStringSplit != null) && (doseStringSplit.length > ingredientsSplit.length)) ? doseStringSplit[ingredientsSplit.length] : ""));
							String lastAmountUnit = null;
							
							for (int ingredientNr = 0; ingredientNr < ingredientsSplit.length; ingredientNr++) {
								String ingredientName = ingredientsSplit[ingredientNr];
								ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName);
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
										denominatorUnit = DrugMappingStringUtilities.removeExtraSpaces((doseStringSplit.length > 1 ? doseStringSplit[1] : ""));
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
								gpkIngredientRecord[OUTPUT_IngredientName]        = DrugMappingStringUtilities.removeExtraSpaces(ingredientName);
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
				outputIngredients = ipciOutputIngredients != null ? ipciOutputIngredients : zindexOutputIngredients;
				if (outputIngredients != null) {
					for (String[] outputIngredient : outputIngredients) {
						outputIngredient[OUTPUT_DosageUnit] = outputIngredient[OUTPUT_DosageUnit].replaceAll("/Stuk", "");
						if (outputIngredient[OUTPUT_DosageUnit].equals("Stuk")) {
							outputIngredient[OUTPUT_DosageUnit] = "";
						}
					}
				}						
				
				// When output ingredients are found write them to the output
				if (outputIngredients != null) {
					outputMap.put(gpkCode, outputIngredients);
				}
			}
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
		}
		

		// Writing output file
		if (ok) {
			if (gpkList.size() > 0) {
				
				// Write output file
				if (ok && (outputMap.size() > 0)) {
					String gpkFullFileName = DrugMappingFileUtilities.getNextFileName(DrugMapping.getBasePath(), " ZIndex" + (IPCIDerivation ? " IPCI" : "") + " - GPK" + (translationType == null ? "" : " - " + translationType) + ".csv");
					gpkFullFileName = DrugMapping.getBasePath() + (DrugMapping.getBasePath().contains("\\") ? "\\" : "/") + gpkFullFileName;
					
					System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Writing out to: " + gpkFullFileName);
					
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
									String record = "";
									for (int column = 0; column < OUTPUT_ColumnCount; column++) {
										record += (column == 0 ? "" : ",") + DrugMappingStringUtilities.escapeFieldValue(outputIngredient[column]);
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
										//gpkIngredientRecord[OUTPUT_IngredientNameEnglish] = "";
										gpkIngredientRecord[OUTPUT_Dosage]                = "";
										gpkIngredientRecord[OUTPUT_DosageUnit]            = "";
										gpkIngredientRecord[OUTPUT_OrgDosage]             = "";
										gpkIngredientRecord[OUTPUT_OrgDosageUnit]         = "";
										gpkIngredientRecord[OUTPUT_CASNumber]             = "";
										
										String record = "";
										for (int column = 0; column < OUTPUT_ColumnCount; column++) {
											record += (column == 0 ? "" : ",") + DrugMappingStringUtilities.escapeFieldValue(gpkIngredientRecord[column]);
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
			System.out.println(DrugMappingDateUtilities.getCurrentTime() + "   Done");
		}
		
		System.out.println(DrugMappingDateUtilities.getCurrentTime() + " Finished" + (ok ? "" : " WITH ERRORS"));
	}
	
	
	private String getAtcOfSingleIngredientGPK(String gnkCodeString) {
		String foundATC = null;
		if (!gnkCodeString.equals("")) {
			int gnkCode = Integer.valueOf(gnkCodeString);
			List<Integer> gpksWithSameIngredient = gnkOneIngredientGpkMap.get(gnkCode);
			if (gpksWithSameIngredient != null) {
				for (int gpkWithSameIngredientCode : gpksWithSameIngredient) {
					String[] gpkWithSameIngredient = gpkMap.get(gpkWithSameIngredientCode);
					if (!gpkWithSameIngredient[GPK_ATCCode].equals("")) {
						if (foundATC == null) {
							foundATC = gpkWithSameIngredient[GPK_ATCCode];
						}
						else if (!foundATC.equals(gpkWithSameIngredient[GPK_ATCCode])) {
							foundATC = null;
							break;
						}
					}
				}
			}
		}
		if ((foundATC != null) && (foundATC.length() != 7)) {
			foundATC = null;
		}
		return foundATC;
	}
	
	
	private String cleanupExtractedIngredientName(String ingredientName) {
		for (String word : wordsToRemove) {
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName);
			
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll("^" + word + " ", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(" " + word + " ", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(" " + word + ",", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll("," + word + " ", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll("," + word + ",", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + " ", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + ", ", " "));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + ",", " "));

			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(" " + word + "$", ""));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll("," + word + "$", ""));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(", " + word + "$", ""));
			ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.replaceAll(",$", ""));
			
			if (ingredientName.endsWith(" " + word)) {
				ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName.substring(0, ingredientName.length() - word.length()));
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
				ingredientName = DrugMappingStringUtilities.removeExtraSpaces(ingredientName);
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
		int lastSpaceIndex = fullName.lastIndexOf(" ");
		if (lastSpaceIndex > -1) {
			doseString = fullName.substring(lastSpaceIndex + 1);
		}
		return doseString;
	}
}
