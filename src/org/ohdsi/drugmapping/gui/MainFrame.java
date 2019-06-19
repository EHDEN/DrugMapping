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
import org.ohdsi.drugmapping.DrugMappingDefinitions;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.zindex.IPCIZIndexMappingDefinitions;

public class MainFrame {
	
	private static final String ICON = "/org/ohdsi/drugmapping/gui/OHDSI Icon Picture 048x048.gif"; 
	
	private DrugMapping drugMapping;
	private JFrame frame;
	private Console console;
	private CDMDatabase database = null;
	private List<InputFile> inputFiles = new ArrayList<InputFile>();
	private String logFile = null;
	private String special = "";
	

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
	
	
	public MainFrame(DrugMapping drugMapping, String special) {
		this.special = special;
		this.drugMapping = drugMapping;
		createInterface();
	}
	
	
	public void show() {
		frame.setVisible(true);
	}
	
	
	private JFrame createInterface() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setTitle("OHDSI Drug Mapping Tool");
		MainFrame.setIcon(frame);
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());
		
		JMenuBar menuBar = createMenu();
		frame.setJMenuBar(menuBar);

		JPanel databasePanel = new JPanel(new GridLayout(0, 1));
		databasePanel.setBorder(BorderFactory.createTitledBorder("CDM Database"));
		database = new CDMDatabase();
		databasePanel.add(database);
		
		JPanel subFrame = new JPanel(new BorderLayout());
		subFrame.setBorder(BorderFactory.createEmptyBorder());

		JPanel filePanel = new JPanel(new GridLayout(0, 1));
		filePanel.setBorder(BorderFactory.createTitledBorder("Input Files"));
		
		for (FileDefinition fileDefinition : (special.toUpperCase().equals("ZINDEX") ? IPCIZIndexMappingDefinitions.FILES : DrugMappingDefinitions.FILES)) {
			InputFile inputFile = new InputFile(fileDefinition);
			inputFiles.add(inputFile);
			filePanel.add(inputFile);
		}
		
		// Start Button
		JPanel buttonSectionPanel = new JPanel(new BorderLayout());
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton startButton = new JButton("  Start  ");
		startButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				drugMapping.StartMapping();
			}
		});
		buttonPanel.add(startButton);
		
		buttonSectionPanel.add(buttonPanel, BorderLayout.WEST);
	
		frame.add(databasePanel, BorderLayout.NORTH);
		frame.add(subFrame, BorderLayout.CENTER);
		subFrame.add(filePanel, BorderLayout.NORTH);
		subFrame.add(createConsolePanel(), BorderLayout.CENTER);
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
		return readSettingsFromFile(getFile(new FileNameExtensionFilter("Settings Files", "ini"), true));
	}
	
	
	public List<String> readSettingsFromFile(String settingsFileName) {
		List<String> settings = new ArrayList<String>();
		if (settingsFileName != null) {
			try {
				BufferedReader settingsFile = new BufferedReader(new FileReader(settingsFileName));
				String line = settingsFile.readLine();
				while (line != null) {
					settings.add(line);
					line = settingsFile.readLine();
				}
				settingsFile.close();
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(frame, "Unable to read settings from file!", "Error", JOptionPane.ERROR_MESSAGE);
				settings = null;
			}
		}
		return settings;
	}
	
	
	private String getFile(FileNameExtensionFilter extensionsFilter, boolean fileShouldExist) {
		String fileName = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(System.getProperty("user.dir")));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (extensionsFilter != null) {
			fileChooser.setFileFilter(extensionsFilter);
		}
		int returnVal = fileShouldExist ? fileChooser.showOpenDialog(frame) : fileChooser.showDialog(frame, "Save");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileName = fileChooser.getSelectedFile().getAbsolutePath();
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
	
	
	public void setSpecial(String special) {
		this.special = special;
	}

}
