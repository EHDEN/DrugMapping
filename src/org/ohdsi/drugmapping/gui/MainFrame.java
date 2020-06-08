package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.GeneralSettings;
import org.ohdsi.drugmapping.files.FileDefinition;

public class MainFrame {
	
	private static final String ICON = "/org/ohdsi/drugmapping/gui/OHDSI Icon Picture 048x048.gif"; 
	
	public static int MINIMUM_USE_COUNT;
	public static int MAXIMUM_STRENGTH_DEVIATION;
	
	public static int PREFERENCE_RXNORM;
	public static int PREFERENCE_PRIORITIZE_BY_DATE;
	public static int PREFERENCE_PRIORITIZE_BY_CONCEPT_ID;
	public static int PREFERENCE_TAKE_FIRST_OR_LAST;

	public static Integer[] preferenceCheckingOrder = new Integer[] { PREFERENCE_RXNORM, PREFERENCE_PRIORITIZE_BY_DATE, PREFERENCE_PRIORITIZE_BY_CONCEPT_ID, PREFERENCE_TAKE_FIRST_OR_LAST };
	
	public static int SUPPRESS_WARNINGS;
	
	private DrugMapping drugMapping;
	private JFrame frame;
	private Console console;
	private CDMDatabase database = null;
	private List<InputFile> inputFiles = new ArrayList<InputFile>();
	private JButton startButton = null;
	private String logFile = null;
	

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
	
	
	public void checkReadyToStart() {
		if (startButton != null) {
			boolean readyToStart = true;
			if (DrugMapping.settings != null) {
				for (Setting setting : DrugMapping.settings.getSettings()) {
					readyToStart = readyToStart && setting.isSetCorrectly();
				}
			}
			startButton.setEnabled(readyToStart);
		}
	}
	
	
	private JFrame createInterface() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(1000, 800);
		frame.setTitle("OHDSI Drug Mapping Tool");
		MainFrame.setIcon(frame);
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());
		
		JMenuBar menuBar = createMenu();
		frame.setJMenuBar(menuBar);

		JPanel databasePanel = null;
		if (!DrugMapping.special.equals("ZINDEX")) {
			// Database settings
			databasePanel = new JPanel(new GridLayout(0, 1));
			databasePanel.setBorder(BorderFactory.createTitledBorder("CDM Database"));
			database = new CDMDatabase();
			databasePanel.add(database);
		}
		
		JPanel level1Frame = new JPanel(new BorderLayout());
		level1Frame.setBorder(BorderFactory.createEmptyBorder());

		
		// File settings
		JPanel filePanel = new JPanel(new GridLayout(0, 1));
		filePanel.setBorder(BorderFactory.createTitledBorder("Input Files"));
		
		for (FileDefinition fileDefinition : DrugMapping.getInputFiles()) {
			InputFile inputFile = new InputFile(fileDefinition);
			inputFiles.add(inputFile);
			filePanel.add(inputFile);
		}
		
		JPanel level2Frame = new JPanel(new BorderLayout());
		level2Frame.setBorder(BorderFactory.createEmptyBorder());


		// General Settings
		DrugMapping.settings = null;
		if (!DrugMapping.special.equals("ZINDEX")) {
			DrugMapping.settings = new GeneralSettings(this);
			
			MINIMUM_USE_COUNT                   = DrugMapping.settings.addSetting(new LongValueSetting(this, "minimumUseCount", "Minimum use count:", 1L));
			MAXIMUM_STRENGTH_DEVIATION          = DrugMapping.settings.addSetting(new DoubleValueSetting(this, "maximumStrengthDeviationPercentage", "Maximum strength deviation percentage:", 20.0));
			PREFERENCE_RXNORM                   = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "preferenceRxNorm", "RxNorm preference when multiple mappings found:", new String[] { "RxNorm", "RxNorm Extension", "None" }, "RxNorm"));
			PREFERENCE_PRIORITIZE_BY_DATE       = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "prioritizeByDate", "Prioritize by valid start date when multiple mappings found:", new String[] { "Latest", "Oldest", "No" }, "No"));
			PREFERENCE_PRIORITIZE_BY_CONCEPT_ID = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "prioritizeByConceptId", "Prioritize by concept_id when multiple mappings found:", new String[] { "Smallest (= oldest)", "Largest (= newest)", "No" }, "Smallest (= oldest)"));
			PREFERENCE_TAKE_FIRST_OR_LAST       = DrugMapping.settings.addSetting(new ChoiceValueSetting(this, "takeFirstOrLast", "Take first or last when multiple mappings left:", new String[] { "First", "Last", "None" }, "None"));
			SUPPRESS_WARNINGS                   = DrugMapping.settings.addSetting(new BooleanValueSetting(this, "suppressWarnings", "Suppress warnings:", false));
		}
		
		
		// Buttons
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
		buttonSectionPanel.add(buttonPanel, BorderLayout.WEST);
	
		
		// Build frame
		if (databasePanel != null) {
			frame.add(databasePanel, BorderLayout.NORTH);
		}
		frame.add(level1Frame, BorderLayout.CENTER);
		level1Frame.add(filePanel, BorderLayout.NORTH);
		level1Frame.add(level2Frame, BorderLayout.CENTER);
		if (DrugMapping.settings != null) {
			level2Frame.add(DrugMapping.settings, BorderLayout.NORTH);
		}
		level2Frame.add(createConsolePanel(), BorderLayout.CENTER);
		frame.add(buttonSectionPanel, BorderLayout.SOUTH);
		
		DrugMapping.disableWhenRunning(startButton);
		
		return frame;
	}
	
	private JScrollPane createConsolePanel() {
		JTextArea consoleArea = new JTextArea();
		consoleArea.setToolTipText("General progress information");
		consoleArea.setEditable(false);
		console = new Console();
		console.setTextArea(consoleArea);
		if (logFile != null) {
			console.setDebugFile(logFile);
		}
		System.setOut(new PrintStream(console));
		System.setErr(new PrintStream(console));
		JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
		consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Console"));
		consoleScrollPane.setAutoscrolls(true);
		ObjectExchange.console = console;
		return consoleScrollPane;
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
					loadDatabaseSettingsFile();
				}
			});
			file.add(loadDatabaseSettingsMenuItem);
			
			JMenuItem saveDatabaseSettingsMenuItem = new JMenuItem("Save Database Settings");
			saveDatabaseSettingsMenuItem.setToolTipText("Save Database Settings");
			saveDatabaseSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					saveDatabaseSettingsFile();
				}
			});
			file.add(saveDatabaseSettingsMenuItem);
		}
		
		JMenuItem loadFileSettingsMenuItem = new JMenuItem("Load File Settings");
		loadFileSettingsMenuItem.setToolTipText("Load File Settings");
		loadFileSettingsMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				loadFileSettingsFile();
			}
		});
		file.add(loadFileSettingsMenuItem);
		
		JMenuItem saveFileSettingsMenuItem = new JMenuItem("Save File Settings");
		saveFileSettingsMenuItem.setToolTipText("Save File Settings");
		saveFileSettingsMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveFileSettingsFile();
			}
		});
		file.add(saveFileSettingsMenuItem);

		if (!DrugMapping.special.equals("ZINDEX")) {
			JMenuItem loadGeneralSettingsMenuItem = new JMenuItem("Load General Settings");
			loadGeneralSettingsMenuItem.setToolTipText("Load General Settings");
			loadGeneralSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					loadGeneralSettingsFile();
				}
			});
			file.add(loadGeneralSettingsMenuItem);
			
			JMenuItem saveGeneralSettingsMenuItem = new JMenuItem("Save General Settings");
			saveGeneralSettingsMenuItem.setToolTipText("Save General Settings");
			saveGeneralSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					saveGeneralSettingsFile();
				}
			});
			file.add(saveGeneralSettingsMenuItem);
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

	
	public void initialize() {
		if (DrugMapping.settings != null) {
			for (Setting setting : DrugMapping.settings.getSettings()) {
				setting.initialize();
			}
		}
	}

	
	private void loadDatabaseSettingsFile() {
		List<String> databaseSettings = readSettingsFromFile();
		if (databaseSettings != null) {
			database.putSettings(databaseSettings);
		}
	}

	
	private void saveDatabaseSettingsFile() {
		if (database != null) {
			List<String> settings = database.getSettings();
			if (settings != null) {
				saveSettingsToFile(settings);
			}
			else {
				JOptionPane.showMessageDialog(frame, "No database settings to save!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	
	private void loadFileSettingsFile() {
		loadFileSettingsFile(readSettingsFromFile());
	}

	
	public void loadFileSettingsFile(List<String> fileSettings) {
		if (fileSettings != null) {
			for (InputFile inputFile : inputFiles) {
				inputFile.putSettings(fileSettings);
			}
		}
	}

	
	private void saveFileSettingsFile() {
		List<String> settings = new ArrayList<String>();
		for (InputFile inputFile : inputFiles) {
			settings.addAll(inputFile.getSettings());
			settings.add("");
			settings.add("");
		}
		saveSettingsToFile(settings);
	}

	
	private void loadGeneralSettingsFile() {
		List<String> generalSettings = readSettingsFromFile();
		loadGeneralSettingsFile(generalSettings);
	}

	
	public void loadGeneralSettingsFile(List<String> generalSettings) {
		if (generalSettings != null) {
			DrugMapping.settings.putSettings(generalSettings);
		}
	}

	
	private void saveGeneralSettingsFile() {
		if (database != null) {
			if (DrugMapping.settings != null) {
				List<String> settings = DrugMapping.settings.getSettingsSave();
				if (settings != null) {
					saveSettingsToFile(settings);
				}
				else {
					JOptionPane.showMessageDialog(frame, "No general settings to save!", "Error", JOptionPane.ERROR_MESSAGE);
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
				JOptionPane.showMessageDialog(frame, "Unable to write settings to file!", "Error", JOptionPane.ERROR_MESSAGE);
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
					JOptionPane.showMessageDialog(frame, "Unable to read settings from file!", "Error", JOptionPane.ERROR_MESSAGE);
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
		int returnVal = fileShouldExist ? fileChooser.showOpenDialog(frame) : fileChooser.showDialog(frame, "Save");
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
	
	
	public void setLogFile(String logFile) {
		this.logFile = logFile;
		if (logFile != null) {
			console.setDebugFile(logFile);
		}
	}

}
