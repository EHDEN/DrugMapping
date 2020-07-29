package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.cdm.CDM;
import org.ohdsi.drugmapping.cdm.CDMConcept;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.genericmapping.GenericMappingInputFiles;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.utilities.files.Row;

public class MainFrame {
	
	private static final String ICON = "/org/ohdsi/drugmapping/gui/OHDSI Icon Picture 048x048.gif"; 
	
	public static int VOCABULARY_ID;
	
	public static int MINIMUM_USE_COUNT;
	public static int MAXIMUM_STRENGTH_DEVIATION;

	public static int PREFERENCE_MATCH_COMP_FORM;
	public static int PREFERENCE_MATCH_INGREDIENTS_TO_COMP;	
	public static int PREFERENCE_RXNORM;
	public static int PREFERENCE_ATC;
	public static int PREFERENCE_PRIORITIZE_BY_DATE;
	public static int PREFERENCE_PRIORITIZE_BY_CONCEPT_ID;
	public static int PREFERENCE_TAKE_FIRST_OR_LAST;
	
	public static int SAVE_DRUGMAPPING_RESULTS;
	public static int SUPPRESS_WARNINGS;
	
	private DrugMapping drugMapping;
	private JFrame frame;
	private JTabbedPane tabbedPane;
	private ExecuteTab executeTab;
	private DrugMappingLogTab drugMappingLogTab;

	
	private Source source;
	private Map<String, InputFile> inputFilesMap;
	private Long minimumUseCount;
	private boolean compBeforeForm;
	private String baseName;
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> drugMappingLog = null;
	private Map<String, Double> usedStrengthDeviationPercentageMap = null;
	

	/**
	 * Sets an icon on a JFrame or a JDialog.
	 * @param container - the GUI component on which the icon is to be put
	 */
	public static void setIcon(Object container){
		URL url = DrugMapping.class.getResource(ICON);
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		if (container.getClass() == JFrame.class ||
				JFrame.class.isAssignableFrom(container.getClass()))
			((JFrame)container).setIconImage(img);
		else if (container.getClass() == JDialog.class  ||
				JDialog.class.isAssignableFrom(container.getClass()))
			((JDialog)container).setIconImage(img);
		else
			((JFrame)container).setIconImage(img);
	}
	
	
	public MainFrame(DrugMapping drugMapping) {
		this.drugMapping = drugMapping;
		createInterface();
		initialize();
	}
	
	
	public void show() {
		frame.setVisible(true);
	}
	
	
	private JFrame createInterface() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	String busy = isBusy();
		        if (
		        		(busy == null) ||
		        		(JOptionPane.showConfirmDialog(
		        						frame, 
		        						busy + "\r\n" + "Are you sure you want to exit?", "Exit?", 
		        						JOptionPane.YES_NO_OPTION,
		        						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        ) {
		            System.exit(0);
		        }
		    }
		});
		
		frame.setSize(1000, 800);
		frame.setMinimumSize(new Dimension(800, 600));
		frame.setTitle("OHDSI Drug Mapping Tool");
		MainFrame.setIcon(frame);
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());
		
		JMenuBar menuBar = createMenu();
		frame.setJMenuBar(menuBar);
		DrugMapping.disableWhenRunning(menuBar);
		
		tabbedPane = new JTabbedPane();
		DrugMapping.disableWhenRunning(tabbedPane);
		
		executeTab = new ExecuteTab(drugMapping, this);
		drugMappingLogTab = new DrugMappingLogTab(this);
		
		tabbedPane.addTab("Execute", executeTab);
		tabbedPane.addTab("Drug Mapping Log", drugMappingLogTab);
		
		frame.add(tabbedPane, BorderLayout.CENTER);
		
		return frame;
	}
	
	
	private JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		
		if (!DrugMapping.special.equals("ZINDEX")) {
			JMenuItem loadDatabaseSettingsMenuItem = new JMenuItem("Load Database Settings");
			loadDatabaseSettingsMenuItem.setToolTipText("Load Database Settings");
			loadDatabaseSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.loadDatabaseSettingsFile();
				}
			});
			file.add(loadDatabaseSettingsMenuItem);
			
			JMenuItem saveDatabaseSettingsMenuItem = new JMenuItem("Save Database Settings");
			saveDatabaseSettingsMenuItem.setToolTipText("Save Database Settings");
			saveDatabaseSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.saveDatabaseSettingsFile();
				}
			});
			file.add(saveDatabaseSettingsMenuItem);
		}
		
		JMenuItem loadFileSettingsMenuItem = new JMenuItem("Load File Settings");
		loadFileSettingsMenuItem.setToolTipText("Load File Settings");
		loadFileSettingsMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				executeTab.loadFileSettingsFile();
			}
		});
		file.add(loadFileSettingsMenuItem);
		
		JMenuItem saveFileSettingsMenuItem = new JMenuItem("Save File Settings");
		saveFileSettingsMenuItem.setToolTipText("Save File Settings");
		saveFileSettingsMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				executeTab.saveFileSettingsFile();
			}
		});
		file.add(saveFileSettingsMenuItem);

		if (!DrugMapping.special.equals("ZINDEX")) {
			JMenuItem loadGeneralSettingsMenuItem = new JMenuItem("Load General Settings");
			loadGeneralSettingsMenuItem.setToolTipText("Load General Settings");
			loadGeneralSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.loadGeneralSettingsFile();
				}
			});
			file.add(loadGeneralSettingsMenuItem);
			
			JMenuItem saveGeneralSettingsMenuItem = new JMenuItem("Save General Settings");
			saveGeneralSettingsMenuItem.setToolTipText("Save General Settings");
			saveGeneralSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.saveGeneralSettingsFile();
				}
			});
			file.add(saveGeneralSettingsMenuItem);

			JMenuItem loadMappingResultsMenuItem = new JMenuItem("Load Mapping Results");
			loadMappingResultsMenuItem.setToolTipText("Load Mapping Results");
			loadMappingResultsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					loadDrugMappingResults();
				}
			});
			file.add(loadMappingResultsMenuItem);
		}
		
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setToolTipText("Exit application");
		exitMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		file.add(exitMenuItem);
		
		menuBar.add(file);
		
		DrugMapping.disableWhenRunning(file);
		
		return menuBar;
	}
	
	
	private String isBusy() {
		String busy = null;
		if (GenericMapping.isMapping)              busy = ((busy == null) ? "" : "\r\n") + "Mapping in progress!";
		if (GenericMapping.isSavingDrugMapping)    busy = ((busy == null) ? "" : "\r\n") + "Saving Mapping in progress!";
		if (GenericMapping.isSavingDrugMappingLog) busy = ((busy == null) ? "" : "\r\n") + "Saving Mapping Log in progress!";
		return busy;
	}
	
	
	public void selectTab(String tabName) {
		int index = 0;
		for (int tabNr = 0; tabNr < tabbedPane.getTabCount(); tabNr++) {
			if (tabbedPane.getTitleAt(tabNr).equals(tabName)) {
				index = tabNr;
				break;
			}
		}
		tabbedPane.setSelectedIndex(index);
	}

	
	public void initialize() {
		if (DrugMapping.settings != null) {
			for (Setting setting : DrugMapping.settings.getSettings()) {
				setting.initialize();
			}
		}
	}

	
	public void loadFileSettingsFile(List<String> fileSettings) {
		executeTab.loadFileSettingsFile(fileSettings);
	}

	
	public void loadGeneralSettingsFile(List<String> generalSettings) {
		executeTab.loadGeneralSettingsFile(generalSettings);
	}
	
	
	public List<String> readSettingsFromFile(String settingsFileName, boolean mandatory) {
		return executeTab.readSettingsFromFile(settingsFileName, mandatory);
	}
	
	
	public JFrame getFrame() {
		return frame;
	}
	
	
	public CDMDatabase getDatabase() {
		return executeTab.getDatabase();
	}
	
	
	public InputFile getInputFile(String fileName) {
		return executeTab.getInputFile(fileName);
	}
	
	
	public Folder getOutputFolder() {
		return executeTab.getOutputFolder();
	}
	
	
	public void setLogFile(String logFile) {
		executeTab.setLogFile(logFile);
	}
	
	
	public void closeLogFile() {
		executeTab.closeLogFile();
	}
	
	
	private void loadDrugMappingResults() {
		String logFilePath = DrugMappingFileUtilities.selectCSVFile(getFrame(), GenericMapping.LOGFILE_NAME, "DrugMapping Log File");
		if (logFilePath != null) {
			GenericMappingInputFiles mappingInputFiles = new GenericMappingInputFiles();
			baseName = logFilePath.substring(0, logFilePath.length() - GenericMapping.LOGFILE_NAME.length());
			DrugMapping.baseName = baseName;
			getInfoFromLogFile(baseName + GenericMapping.LOGFILE_NAME);
			GenericMapping.setMappingTypes(compBeforeForm);
			InputFile genericDrugsFile = inputFilesMap.get("Generic Drugs File");
			FileDefinition mappingLogFileDefinition = mappingInputFiles.getInputFileDefinition("DrugMapping Mapping Log File");
			InputFile mappingLogFile = null;
			if (mappingLogFileDefinition != null) {
				mappingLogFile = new InputFile(mappingLogFileDefinition);
				mappingLogFile.setFileName(baseName + "DrugMapping Mapping Log.csv");
				for (FileColumnDefinition columnDefinition : mappingLogFileDefinition.getColumns()) {
					String columnName = columnDefinition.getColumnName();
					mappingLogFile.addColumnMapping(columnName, columnName);
				}
			}
			if ((genericDrugsFile != null) && (mappingLogFile != null)) {
				startLoadingMappingResults(this, genericDrugsFile, mappingLogFile);
			}
		}
	}
	
	
	private void getInfoFromLogFile(String logFileName) {
		inputFilesMap = new HashMap<String, InputFile>();
		File logFile = new File(logFileName);
		if (logFile.exists() && logFile.canRead()) {
			try {
				BufferedReader logFileReader = new BufferedReader(new FileReader(logFile));
				GenericMappingInputFiles mappingInputFiles = new GenericMappingInputFiles();
				
				String inputFileTag = "Input File: ";
				String fileNameTag = "  Filename: ";
				String fieldDelimiterTag = "  Field delimiter: ";
				String textQualifierTag = "  Text qualifier: ";
				String textFieldsTag = "  Fields:";
				String generalSettingsTag = "General Settings:";
				String settingMinimumUseCountTag = "  Minimum use count: ";
				String settingCompFormPreference = "  Comp Form matching preference: ";
				
				boolean fields = false;
				String line = logFileReader.readLine();
				InputFile inputFile = null;
				boolean generalSettings = false;
				while ((line != null) && (line.equals("") || (!"1234567890".contains(line.substring(0, 1))))) {
					if (line.startsWith(inputFileTag)) {
						fields = false;
						generalSettings = false;
						String inputFileName = line.substring(inputFileTag.length()); 
						FileDefinition inputFileDefinition = null;
						for (FileDefinition fileDefinition : mappingInputFiles.getInputFiles()) {
							if (fileDefinition.getFileName().equals(inputFileName)) {
								inputFileDefinition = fileDefinition;
								break;
							}
						}
						if (inputFileDefinition != null) {
							inputFile = new InputFile(inputFileDefinition);
							inputFilesMap.put(inputFile.getLabelText(), inputFile);
						}
						else {
							inputFile = null;
						}
					}
					else if (line.startsWith(generalSettingsTag)) {
						generalSettings = true;
						inputFile = null;
					}
					else if (inputFile != null) {
						if (line.startsWith(fileNameTag)) {
							inputFile.setFileName(line.substring(fileNameTag.length()));
						}
						else if (line.startsWith(fieldDelimiterTag)) {
							inputFile.setFieldDelimiter(line.substring(fieldDelimiterTag.length() + 1, line.length() - 1));
						}
						else if (line.startsWith(textQualifierTag)) {
							inputFile.setTextQualifier(line.substring(textQualifierTag.length() + 1, line.length() - 1));
						}
						else if (line.startsWith(textFieldsTag)) {
							fields = true;
						}
						else if (fields && line.startsWith("    ") && line.contains(" -> ")) {
							String[] lineSplit = line.split(" -> ");
							String genericName = lineSplit[0].trim();
							String sourceName = lineSplit[1].trim();
							inputFile.addColumnMapping(genericName, sourceName);
						} 
						else {
							fields = false;
						}
					} 
					else if (generalSettings) {
						fields = false;
						if (line.startsWith(settingMinimumUseCountTag)) {
							minimumUseCount = Long.parseLong(line.substring(settingMinimumUseCountTag.length()).trim());
						}
						else if (line.startsWith(settingCompFormPreference)) {
							compBeforeForm = line.substring(settingCompFormPreference.length()).trim().equals("Comp before Form");
						}
					}
					line = logFileReader.readLine();
				}
				logFileReader.close();
			}
			catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(getFrame(), "Error opening log file '" + logFileName + "'!", "Error", JOptionPane.ERROR_MESSAGE);
				inputFilesMap = null;
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(getFrame(), "Error reading log file '" + logFileName + "'!", "Error", JOptionPane.ERROR_MESSAGE);
				inputFilesMap = null;
			}
			
		}
	}
	
	
	private void startLoadingMappingResults(MainFrame mainFrame, InputFile genericDrugsFile, InputFile mappingLogFile) {
		if (genericDrugsFile != null) {
			LoadMappingResultsThread mappingResultsThread = new LoadMappingResultsThread(mainFrame, genericDrugsFile, mappingLogFile);
			mappingResultsThread.start();
		}
	}
	
	
	private class LoadMappingResultsThread extends Thread {
		MainFrame mainFrame;
		InputFile genericDrugsFile;
		InputFile drugMappingLogFile;
		
		public LoadMappingResultsThread(MainFrame mainFrame, InputFile genericDrugsFile, InputFile drugMappingLogFile) {
			super();
			this.mainFrame = mainFrame;
			this.genericDrugsFile = genericDrugsFile;
			this.drugMappingLogFile = drugMappingLogFile;
		}
		
		public void run() {
			for (JComponent component : DrugMapping.componentsToDisableWhenRunning)
				component.setEnabled(false);
			
			source = new Source();
			if (source.loadSourceDrugs(genericDrugsFile, minimumUseCount)) {
				loadDrugMappingLog(drugMappingLogFile);
				showDrugMappingLog(source, null, drugMappingLog, usedStrengthDeviationPercentageMap, baseName, true);
			}
			else {
				JOptionPane.showMessageDialog(mainFrame.getFrame(), "Error reading loading source drugs!", "Error", JOptionPane.ERROR_MESSAGE);
			}
			
			for (JComponent component : DrugMapping.componentsToDisableWhenRunning)
				component.setEnabled(true);
		}
		
	}
	
	
	private void loadDrugMappingLog(InputFile drugMappingLogFile) {
		
		System.out.println(DrugMapping.getCurrentTime() + "     Loading Mapping Log ...");
		
		usedStrengthDeviationPercentageMap = new HashMap<String, Double>();
		if (drugMappingLogFile.openFile()) {
			drugMappingLog = new HashMap<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>>();
			
			String lastSourceCode               = "";
			Integer lastMappingType             = null;
			Integer lastMappingResult           = null;
			String lastIngredientCode           = "";
			String lastSourceIngredientAmount   = "";
			String lastSourceIngredientUnit     = "";
			
			while (drugMappingLogFile.hasNext()) {
				Row row = drugMappingLogFile.next();
				//System.out.println("  " + row);
				
				//String mappingStatus            = drugMappingLogFile.get(row, "MappingStatus", true);
				String sourceCode               = drugMappingLogFile.get(row, "SourceCode", true);
				//String sourceName               = drugMappingLogFile.get(row, "SourceName", true);
				//String sourceATCCode            = drugMappingLogFile.get(row, "SourceATCCode", true);
				//String sourceFormulation        = drugMappingLogFile.get(row, "SourceFormulation", true);
				//String sourceCount              = drugMappingLogFile.get(row, "SourceCount", true);
				String ingredientCode           = drugMappingLogFile.get(row, "IngredientCode", true);
				//String ingredientName           = drugMappingLogFile.get(row, "IngredientName", true);
				String ingredientNameEnglish    = drugMappingLogFile.get(row, "IngredientNameEnglish", true);
				//String casNumber                = drugMappingLogFile.get(row, "CASNumber", true);
				String sourceIngredientAmount   = drugMappingLogFile.get(row, "SourceIngredientAmount", true);
				String sourceIngredientUnit     = drugMappingLogFile.get(row, "SourceIngredentUnit", true);
				String strengthMarginPercentage = drugMappingLogFile.get(row, "StrengthMarginPercentage", true);
				String mappingTypeDescription   = drugMappingLogFile.get(row, "MappingType", true);
				String mappingResultDescription = drugMappingLogFile.get(row, "MappingResult", true);

				Integer mappingType = GenericMapping.getMappingTypeValue(mappingTypeDescription);
				Integer mappingResult = GenericMapping.getMappingResultValue(mappingResultDescription);
				
				List<CDMConcept> conceptList = new ArrayList<CDMConcept>();
				for (int cellNr = 15; cellNr < row.getCells().size(); cellNr++) {
					String conceptDescription = row.getCells().get(cellNr);
					if (!conceptDescription.equals("")) {
						CDMConcept concept = null;
						if ((mappingResult == GenericMapping.DOUBLE_INGREDIENT_MAPPING) || (mappingResult == GenericMapping.UNMAPPED_SOURCE_INGREDIENTS)) {
							concept = new CDMConcept();
							concept.setAdditionalInfo(conceptDescription);
						}
						else {
							concept = new CDMConcept(conceptDescription);
						}
						conceptList.add(concept);
					}
					else {
						break;
					}
				}
				if (conceptList.size() == 0) {
					conceptList.add(null);
				}
				
				SourceDrug sourceDrug = source.getSourceDrug(sourceCode);
				SourceIngredient sourceIngredient = ingredientCode.equals("*") ? null : Source.getIngredient(ingredientCode);
				if ((ingredientNameEnglish != null) && (!ingredientNameEnglish.equals("")) && (!ingredientNameEnglish.equals("*"))) {
					sourceIngredient.setIngredientNameEnglish(ingredientNameEnglish);
				}
				
				if ((strengthMarginPercentage != null) && (!strengthMarginPercentage.equals("")) && (!strengthMarginPercentage.equals("*"))) {
					String key = "Drug " + sourceCode;
					if (mappingType == GenericMapping.INGREDIENT_MAPPING) {
						key = "Ingredient " + sourceDrug.getCode() + "," + ingredientCode;
					}
					usedStrengthDeviationPercentageMap.put(key, Double.parseDouble(strengthMarginPercentage));
				}
				
				Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingLog = drugMappingLog.get(sourceDrug);
				if (sourceDrugMappingLog == null) {
					sourceDrugMappingLog = new HashMap<Integer, List<Map<Integer, List<CDMConcept>>>>();
					drugMappingLog.put(sourceDrug, sourceDrugMappingLog);
				}
				
				List<Map<Integer, List<CDMConcept>>> sourceDrugMappingTypeLog = sourceDrugMappingLog.get(mappingType);
				if (sourceDrugMappingTypeLog == null) {
					sourceDrugMappingTypeLog = new ArrayList<Map<Integer, List<CDMConcept>>>();
					sourceDrugMappingLog.put(mappingType, sourceDrugMappingTypeLog);
				}

				Map<Integer, List<CDMConcept>> sourceDrugMappingResult;
				if (
						sourceCode.equals(lastSourceCode) && 
						(
							(mappingType != lastMappingType) ||
							(mappingResult != lastMappingResult) ||
							(!ingredientCode.equals(lastIngredientCode)) || 
							(!sourceIngredientAmount.equals(lastSourceIngredientAmount)) || 
							(!sourceIngredientUnit.equals(lastSourceIngredientUnit))
						)
					) {
					sourceDrugMappingResult = new HashMap<Integer, List<CDMConcept>>();
					sourceDrugMappingTypeLog.add(sourceDrugMappingResult);
				}
				else {
					if (sourceDrugMappingTypeLog.size() == 0) {
						sourceDrugMappingResult = new HashMap<Integer, List<CDMConcept>>();
						sourceDrugMappingTypeLog.add(sourceDrugMappingResult);
					}
				}
				sourceDrugMappingResult = sourceDrugMappingTypeLog.get(sourceDrugMappingTypeLog.size() - 1);
				sourceDrugMappingResult.put(mappingResult, conceptList);
				
				lastSourceCode               = sourceCode;
				lastMappingType              = mappingType;
				lastMappingResult            = mappingResult;
				lastIngredientCode           = ingredientCode;
				lastSourceIngredientAmount   = sourceIngredientAmount;
				lastSourceIngredientUnit     = sourceIngredientUnit;
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + "     Done");
	}
	
	
	public void showDrugMappingLog(Source source, CDM cdm, Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> drugMappingLog, Map<String, Double> usedStrengthDeviationPercentageMap, String baseName, boolean isSaved) {
		for (JComponent component : DrugMapping.componentsToDisableWhenRunning) {
			if (component != executeTab.startButton) {
				component.setEnabled(true);
			}
		}
		drugMappingLogTab.showDrugMappingLog(source, cdm, drugMappingLog, usedStrengthDeviationPercentageMap, isSaved);
		selectTab("Drug Mapping Log");
	}

}
