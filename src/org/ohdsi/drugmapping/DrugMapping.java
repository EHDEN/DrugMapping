package org.ohdsi.drugmapping;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.files.InputFileDefinition;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.genericmapping.GenericMappingInputFiles;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.gui.Setting;
import org.ohdsi.drugmapping.gui.files.DelimitedInputFileGUI;
import org.ohdsi.drugmapping.gui.files.FolderGUI;
import org.ohdsi.drugmapping.gui.files.InputFileGUI;
import org.ohdsi.drugmapping.utilities.DrugMappingDateUtilities;

public class DrugMapping { 
	public static GeneralSettings settings = null;
	public static String outputVersion = "";
	public static boolean debug = false;
	public static String special = "";
	public static Boolean autoStart = false;
	
	public static String baseName = "";
	
	public static Set<JComponent> componentsToDisableWhenRunning = new HashSet<JComponent>();
	
	private static String currentPath = null;	
	private static String basePath = new File(".").getAbsolutePath();
	private static InputFileDefinition inputFiles = null;
	
	private String logFileName;
	private MainFrame mainFrame;
	
	
	private static String getOutputVersion(String logFileName) {
		String version = "";
		
		String date = DrugMappingDateUtilities.getCurrentDate();
		
		for (Integer versionNr = 1; versionNr < 100; versionNr++) {
			String versionNrString = ("00" + versionNr).substring(versionNr.toString().length());
			File logFile = new File(basePath + "/" + date + " " + versionNrString + " " + logFileName);
			if (!logFile.exists()) {
				version = date + " " + versionNrString + " ";
				break;
			}
		}
		
		return version;
	}
	
	
	public static String getCurrentPath() {
		return currentPath;
	}
	
	
	public static void setCurrentPath(String path) {
		currentPath = path;
	}
	
	
	public static String getBasePath() {
		return basePath;
	}
	
	
	public static void setBasePath(String path) {
		basePath = path;
	}
	
	
	public static void disableWhenRunning(JComponent component) {
		componentsToDisableWhenRunning.add(component);
	}
	
	
	public static List<FileDefinition> getInputFiles() {
		return inputFiles.getInputFiles();
	}
	
	
	public DrugMapping(Map<String, String> parameters) {
		List<String> dbSettings = null;
		String password = null;
		List<String> fileSettings = null;
		List<String> generalSettings = null;
		autoStart = false;
		special = parameters.get("special");
		debug = (parameters.get("debug") != null);

		if (special != null) {
			// No specials defined
		}
		else {
			special = "";
			inputFiles = new GenericMappingInputFiles();
			logFileName = GenericMapping.LOGFILE_NAME;
		}
		
		mainFrame = new MainFrame(this);
		
		if (parameters.containsKey("databasesettings")) {
			dbSettings = mainFrame.readSettingsFromFile(parameters.get("databasesettings"), true);
		}
		if (parameters.containsKey("password")) {
			password = parameters.get("password");
		}
		if (parameters.containsKey("filesettings")) {
			fileSettings = mainFrame.readSettingsFromFile(parameters.get("filesettings"), true);
		}
		if (parameters.containsKey("generalsettings")) {
			generalSettings = mainFrame.readSettingsFromFile(parameters.get("generalsettings"), false);
		}
		if (parameters.containsKey("autostart")) {
			autoStart = parameters.get("autostart").toLowerCase().equals("yes");
		}
		
		if (fileSettings != null) {
			mainFrame.loadFileSettingsFile(fileSettings);
			setBasePath(mainFrame.getOutputFolder().getFolderName());
		}
		
		if (generalSettings != null) {
			mainFrame.loadGeneralSettingsFile(generalSettings);
		}
		
		if (dbSettings != null) {
			if (password != null) {
				dbSettings.add("password=" + password);
			}
			mainFrame.getDatabase().putSettings(dbSettings);
		}

	}
	
	
	private CDMDatabase getDatabase() {
		return mainFrame.getDatabase();
	}
	
	
	private InputFileGUI getFile(String fileName) {
		return mainFrame.getInputFile(fileName);
	}
	
	
	private void Show() {
		mainFrame.show();
		if (autoStart) {
			StartMapping();
		}
	}
	
	
	public void StartMapping() {
		// Create log file and set basePath
		basePath = mainFrame.getOutputFolder().getFolderName();
		if ((basePath == null) || basePath.equals("")) {
			basePath = new File(".").getAbsolutePath();
		}
		outputVersion = debug ? getOutputVersion(logFileName) : "";
		baseName = basePath + "/" + outputVersion;
		String fullLogFileName = baseName + logFileName;
		mainFrame.setLogFile(fullLogFileName);
		MappingThread mappingThread = new MappingThread();
		mappingThread.start();
	}
	
	
	private class MappingThread extends Thread {
		
		public void run() {
			for (JComponent component : componentsToDisableWhenRunning)
				component.setEnabled(false);
			
			if (special.equals("??????")) {
			}
			else {
				logDatabaseSettings(getDatabase());
				getFile("Generic Drugs File").logFileSettings();
				getFile("Ingredient Name Translation File").logFileSettings();
				getFile("Unit Mapping File").logFileSettings();
				getFile("Dose Form Mapping File").logFileSettings();
				getFile("Manual CAS Mappings File").logFileSettings();
				getFile("Manual Ingedient Overrule Mappings File").logFileSettings();
				getFile("Manual Ingedient Fallback Mappings File").logFileSettings();
				getFile("Manual Drug Mappings File").logFileSettings();
				logFolderSettings(mainFrame.getOutputFolder());
				logGeneralSettings();
				new GenericMapping(
						mainFrame,
						getDatabase(), 
						(DelimitedInputFileGUI) getFile("Generic Drugs File"),
						(DelimitedInputFileGUI) getFile("Ingredient Name Translation File"),
						(DelimitedInputFileGUI) getFile("Unit Mapping File"),
						(DelimitedInputFileGUI) getFile("Dose Form Mapping File"),
						(DelimitedInputFileGUI) getFile("Manual CAS Mappings File"), 
						(DelimitedInputFileGUI) getFile("Manual Ingedient Overrule Mappings File"), 
						(DelimitedInputFileGUI) getFile("Manual Ingedient Fallback Mappings File"), 
						(DelimitedInputFileGUI) getFile("Manual Drug Mappings File")
						);
			}

			mainFrame.closeLogFile();
			
			for (JComponent component : componentsToDisableWhenRunning)
				component.setEnabled(true);
		}
		
	}
	
	
	private void logDatabaseSettings(CDMDatabase database) {
		DBSettings databaseSettings = database.getDBSettings();
		System.out.println("Database Connection: " + database.getDBServerName());
		System.out.println("  Database Type: " + databaseSettings.dbType);
		System.out.println("  Database: " + databaseSettings.server);
		System.out.println("  Schema: " + databaseSettings.database);
		System.out.println("  User: " + databaseSettings.user);
		System.out.println();
	}
	
	
	private void logFolderSettings(FolderGUI folder) {
		if (folder.getFolderName() != null) {
			System.out.println(folder.getName() + ": " + folder.getFolderName());
			System.out.println();
		}
	}
	
	
	private void logGeneralSettings() {
		System.out.println("General Settings:");
		for (Setting setting : DrugMapping.settings.getSettings()) {
			System.out.println("  " + setting.getLabel() + " " + setting.getValueAsString());
		}
		System.out.println();
	}
	
	
	public static void main(String[] args) {
		/* */
		Map<String, String> parameters = new HashMap<String, String>();

		for (int i = 0; i < args.length; i++) {
			int equalSignIndex = args[i].indexOf("=");
			String argVariable = args[i].toLowerCase();
			String value = "";
			if (equalSignIndex != -1) {
				argVariable = args[i].substring(0, equalSignIndex).toLowerCase();
				value = args[i].substring(equalSignIndex + 1);
			}
			parameters.put(argVariable, value);
		}
		
		DrugMapping drugMapping = new DrugMapping(parameters);
		drugMapping.Show();
		/* */
	}

}
