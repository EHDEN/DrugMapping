package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.GeneralSettings;
import org.ohdsi.drugmapping.files.FileDefinition;

public class ExecuteTab extends MainFrameTab {
	private static final long serialVersionUID = 8907817283501911409L;
	
	
	private MainFrame mainFrame;
	private CDMDatabase database = null;
	private List<InputFile> inputFiles = new ArrayList<InputFile>();
	private Console console;
	private Folder outputFolder = null; 

	
	public ExecuteTab(DrugMapping drugMapping, MainFrame mainFrame) {
		super();
		
		this.mainFrame = mainFrame;
		
		setLayout(new BorderLayout());

		JPanel databasePanel = null;
		if (!DrugMapping.special.equals("ZINDEX")) {
			// Database settings
			databasePanel = new JPanel(new GridLayout(0, 1));
			databasePanel.setBorder(BorderFactory.createTitledBorder("CDM Vocabulary"));
			database = new CDMDatabase();
			databasePanel.add(database);
		}
		
		JPanel level1Panel = new JPanel(new BorderLayout());
		level1Panel.setBorder(BorderFactory.createEmptyBorder());

		
		// File settings
		JPanel filePanel = new JPanel(new GridLayout(0, 1));
		filePanel.setBorder(BorderFactory.createTitledBorder("Input"));
		
		for (FileDefinition fileDefinition : DrugMapping.getInputFiles()) {
			if (fileDefinition.isUsedInInterface()) {
				InputFile inputFile = new InputFile(fileDefinition);
				inputFiles.add(inputFile);
				filePanel.add(inputFile);
			}
		}
		
		JPanel level2Panel = new JPanel(new BorderLayout());
		level2Panel.setBorder(BorderFactory.createEmptyBorder());
		
		JPanel outputPanel = null;
		// Output Folder
		outputPanel = new JPanel(new GridLayout(0, 1));
		outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));
		outputFolder = new Folder("Output Folder", "Output Folder", DrugMapping.getBasePath());
		outputPanel.add(outputFolder);
		
		JPanel level3Panel = new JPanel(new BorderLayout());
		level3Panel.setBorder(BorderFactory.createEmptyBorder());


		// General Settings
		DrugMapping.settings = null;
		if (!DrugMapping.special.equals("ZINDEX")) {
			DrugMapping.settings = new GeneralSettings();

			MainFrame.VOCABULARY_ID                        = DrugMapping.settings.addSetting(new StringValueSetting(this, "VocabularyID", "Vocabulary ID:", ""));
			MainFrame.MINIMUM_USE_COUNT                    = DrugMapping.settings.addSetting(new LongValueSetting(this, "minimumUseCount", "Minimum use count:", 1L));
			MainFrame.MAXIMUM_STRENGTH_DEVIATION           = DrugMapping.settings.addSetting(new DoubleValueSetting(this, "maximumStrengthDeviationPercentage", "Maximum strength deviation percentage:", 20.0));
			MainFrame.PREFERENCE_MATCH_COMP_FORM           = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "preferenceCompForm", "Comp Form matching preference:", new String[] { "Comp before Form", "Form before Comp" }, "Form before Comp"));
			MainFrame.PREFERENCE_MATCH_INGREDIENTS_TO_COMP = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "preferenceMatchIngredientsToComp", "Ingredient Matching preference:", new String[] { "Ingredient Only", "Ingredient or Comp" }, "Ingredient Only"));
			MainFrame.PREFERENCE_NON_ORPHAN_INGREDIENTS    = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "preferenceNonOrphans", "Prefer not-orphan ingredients:", new String[] { "Yes", "No" }, "Yes"));
			MainFrame.PREFERENCE_RXNORM                    = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "preferenceRxNorm", "RxNorm preference:", new String[] { "RxNorm", "RxNorm Extension", "None" }, "RxNorm"));
			MainFrame.PREFERENCE_ATC                       = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "preferenceATC", "Prefer matching ATC:", new String[] { "Yes", "No" }, "Yes"));
			MainFrame.PREFERENCE_PRIORITIZE_BY_DATE        = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "prioritizeByDate", "Valid start date preference:", new String[] { "Latest", "Oldest", "No" }, "No"));
			MainFrame.PREFERENCE_PRIORITIZE_BY_CONCEPT_ID  = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "prioritizeByConceptId", "Concept_id preference:", new String[] { "Smallest (= oldest)", "Largest (= newest)", "No" }, "Smallest (= oldest)"));
			MainFrame.PREFERENCE_TAKE_FIRST_OR_LAST        = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "takeFirstOrLast", "First or last preferece:", new String[] { "First", "Last", "None" }, "None"));
			MainFrame.SAVE_DRUGMAPPING_LOG                 = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "saveDrugMappingsLog", "Save Drugmapping Log file:", new String[] { "Yes", "No" }, "Yes"));
			MainFrame.SUPPRESS_WARNINGS                    = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "suppressWarnings", "Suppress warnings:", new String[] { "Yes", "No" }, "No"));
		}
		
		
		// Buttons Panel
		JPanel buttonSectionPanel = new JPanel(new BorderLayout());

		// Start Button
		JPanel buttonPanel = new JPanel(new FlowLayout());
		startButton = new JButton("  Start  ");
		startButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				drugMapping.StartMapping();
			}
		});
		buttonPanel.add(startButton);
		DrugMapping.disableWhenRunning(startButton);
		
		buttonSectionPanel.add(buttonPanel, BorderLayout.WEST);
	
		
		// Build panel
		if (databasePanel != null) {
			add(databasePanel, BorderLayout.NORTH);
		}
		add(level1Panel, BorderLayout.CENTER);
		level1Panel.add(filePanel, BorderLayout.NORTH);
		level1Panel.add(level2Panel, BorderLayout.CENTER);
		level2Panel.add(outputPanel, BorderLayout.NORTH);
		level2Panel.add(level3Panel, BorderLayout.CENTER);
		if (DrugMapping.settings != null) {
			level3Panel.add(DrugMapping.settings, BorderLayout.NORTH);
		}
		level3Panel.add(createConsolePanel(), BorderLayout.CENTER);
		add(buttonSectionPanel, BorderLayout.SOUTH);
	}
	
	
	private JScrollPane createConsolePanel() {
		JTextArea consoleArea = new JTextArea();
		consoleArea.setToolTipText("General progress information");
		consoleArea.setEditable(false);
		console = new Console();
		console.setTextArea(consoleArea);
		if (!(System.getProperty("runInEclipse") == null ? false : System.getProperty("runInEclipse").equalsIgnoreCase("true"))) {
			System.setOut(new PrintStream(console));
			System.setErr(new PrintStream(console));
		}
		JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Console"));
		consoleScrollPane.setAutoscrolls(true);
		ObjectExchange.console = console;
		return consoleScrollPane;
	}

	
	public void loadDatabaseSettingsFile() {
		List<String> databaseSettings = readSettingsFromFile();
		if (databaseSettings != null) {
			database.putSettings(databaseSettings);
		}
	}

	
	public void saveDatabaseSettingsFile() {
		if (database != null) {
			List<String> settings = database.getSettings();
			if (settings != null) {
				saveSettingsToFile(settings);
			}
			else {
				JOptionPane.showMessageDialog(mainFrame.getFrame(), "No database settings to save!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	
	public void loadFileSettingsFile() {
		loadFileSettingsFile(readSettingsFromFile());
	}

	
	public void loadFileSettingsFile(List<String> fileSettings) {
		if (fileSettings != null) {
			if (outputFolder != null) {
				outputFolder.putSettings(fileSettings);
			}
			for (InputFile inputFile : inputFiles) {
				inputFile.putSettings(fileSettings);
			}
		}
	}

	
	public void saveFileSettingsFile() {
		List<String> settings = new ArrayList<String>();
		if (outputFolder != null) {
			settings.addAll(outputFolder.getSettings());
			settings.add("");
			settings.add("");
		}
		for (InputFile inputFile : inputFiles) {
			settings.addAll(inputFile.getSettings());
			settings.add("");
			settings.add("");
		}
		saveSettingsToFile(settings);
	}

	
	public void loadGeneralSettingsFile() {
		List<String> generalSettings = readSettingsFromFile();
		loadGeneralSettingsFile(generalSettings);
	}

	
	public void loadGeneralSettingsFile(List<String> generalSettings) {
		if (generalSettings != null) {
			DrugMapping.settings.putSettings(generalSettings);
		}
	}

	
	public void saveGeneralSettingsFile() {
		if (database != null) {
			if (DrugMapping.settings != null) {
				List<String> settings = DrugMapping.settings.getSettingsSave();
				if (settings != null) {
					saveSettingsToFile(settings);
				}
				else {
					JOptionPane.showMessageDialog(mainFrame.getFrame(), "No general settings to save!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
	
	
	private void saveSettingsToFile(List<String> settings) {
		String settingsFileName = getFile(new FileNameExtensionFilter("Settings Files", "ini"), false);
		if (settingsFileName != null) {
			try {
				PrintWriter settingsFile = new PrintWriter(settingsFileName);
				for (String line : settings) {
					settingsFile.println(line);
				}
				settingsFile.close();
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(mainFrame.getFrame(), "Unable to write settings to file!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	
	private List<String> readSettingsFromFile() {
		return readSettingsFromFile(getFile(new FileNameExtensionFilter("Settings Files", "ini"), true), true);
	}
	
	
	public List<String> readSettingsFromFile(String settingsFileName, boolean mandatory) {
		List<String> settings = new ArrayList<String>();
		if (settingsFileName != null) {
			try {
				BufferedReader settingsFileBufferedReader = new BufferedReader(new FileReader(settingsFileName));
				String line = settingsFileBufferedReader.readLine();
				while (line != null) {
					settings.add(line);
					line = settingsFileBufferedReader.readLine();
				}
				settingsFileBufferedReader.close();
			}
			catch (IOException e) {
				if (mandatory) {
					JOptionPane.showMessageDialog(mainFrame.getFrame(), "Unable to read settings from file '" + settingsFileName + "'!", "Error", JOptionPane.ERROR_MESSAGE);
				}
				settings = null;
			}
		}
		return settings;
	}
	
	
	private String getFile(FileNameExtensionFilter extensionsFilter, boolean fileShouldExist) {
		String fileName = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(DrugMapping.getCurrentPath() == null ? (DrugMapping.getBasePath() == null ? System.getProperty("user.dir") : DrugMapping.getBasePath()) : DrugMapping.getCurrentPath()));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (extensionsFilter != null) {
			fileChooser.setFileFilter(extensionsFilter);
		}
		int returnVal = fileShouldExist ? fileChooser.showOpenDialog(mainFrame.getFrame()) : fileChooser.showDialog(mainFrame.getFrame(), "Save");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileName = fileChooser.getSelectedFile().getAbsolutePath();
			DrugMapping.setCurrentPath(fileChooser.getSelectedFile().getAbsolutePath().substring(0, fileChooser.getSelectedFile().getAbsolutePath().lastIndexOf(File.separator)));
		}
		return fileName;
	}
	
	
	public CDMDatabase getDatabase() {
		return database;
	}
	
	
	public InputFile getInputFile(String fileName) {
		InputFile file = null;
		
		for (InputFile inputFile : inputFiles) {
			if (inputFile.getLabelText().equals(fileName)) {
				file = inputFile;
				break;
			}
		}
		
		return file;
	}
	
	
	public Folder getOutputFolder() {
		return outputFolder;
	}
	
	
	public void clearConsole() {
		console.clear();
	}
	
	
	public void setLogFile(String logFile) {
		clearConsole();
		if (logFile != null) {
			console.setDebugFile(logFile);
		}
	}
	
	
	public void closeLogFile() {
		console.closeDebugFile();
	}
}
