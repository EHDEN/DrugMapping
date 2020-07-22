package org.ohdsi.drugmapping;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.genericmapping.GenericMappingInputFiles;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.Folder;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.gui.Setting;
import org.ohdsi.drugmapping.zindex.ZIndexConversion;
import org.ohdsi.drugmapping.zindex.ZIndexConversionInputFiles;

public class DrugMapping { 
	public static GeneralSettings settings = null;
	public static String outputVersion = "";
	public static boolean debug = false;
	public static String special = "";
	public static Boolean autoStart = false;
	
	private static Set<JComponent> componentsToDisableWhenRunning = new HashSet<JComponent>();
	
	private static String currentDate = null;
	private static String currentPath = null;	
	private static String basePath = new File(".").getAbsolutePath();
	private static MappingInputDefinition inputFiles = null;
	
	private String logFileName;
	private MainFrame mainFrame;
	
	
	public static String getCurrentDate() {
		if (currentDate == null) {
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
			currentDate = sdf.format(cal.getTime());
		}
		return currentDate;
	}
	
	
	public static String getCurrentTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		return sdf.format(cal.getTime());
	}
	
	
	private static String getOutputVersion(String logFileName) {
		String version = "";
		
		String date = getCurrentDate();
		
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
			special = special.toUpperCase();
			if (special.equals("ZINDEX")) {
				inputFiles = new ZIndexConversionInputFiles();
				logFileName = "ZIndex - Conversion - Log.txt";
			}
			else {
				JOptionPane.showMessageDialog(null, "Unknown special definition '" + special + "'!", "Dialog",JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
		else {
			special = "";
			inputFiles = new GenericMappingInputFiles();
			logFileName = "DrugMapping Log.txt";
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
	
	
	private InputFile getFile(String fileName) {
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
		String fullLogFileName = basePath + "/" + outputVersion + logFileName;
		mainFrame.setLogFile(fullLogFileName);
		MappingThread mappingThread = new MappingThread();
		mappingThread.start();
	}
	
	
	private class MappingThread extends Thread {
		
		public void run() {
			for (JComponent component : componentsToDisableWhenRunning)
				component.setEnabled(false);
			
			if (special.equals("ZINDEX")) {
				logFileSettings("ZIndex GPK File", getFile("ZIndex GPK File"));
				logFileSettings("ZIndex GSK File", getFile("ZIndex GSK File"));
				logFileSettings("ZIndex GNK File", getFile("ZIndex GNK File"));
				logFileSettings("ZIndex GPK Statistics File", getFile("ZIndex GPK Statistics File"));
				logFileSettings("ZIndex GPK IPCI Compositions File", getFile("ZIndex GPK IPCI Compositions File"));
				new ZIndexConversion(
						getFile("ZIndex GPK File"), 
						getFile("ZIndex GSK File"), 
						getFile("ZIndex GNK File"), 
						getFile("ZIndex GPK Statistics File"), 
						getFile("ZIndex GPK IPCI Compositions File")
						);
			}
			else {
				logDatabaseSettings(getDatabase());
				logFileSettings("Generic Drugs File", getFile("Generic Drugs File"));
				logFileSettings("Ingredient Name Translation File", getFile("Ingredient Name Translation File"));
				logFileSettings("Unit Mapping File", getFile("Unit Mapping File"));
				logFileSettings("Dose Form Mapping File", getFile("Dose Form Mapping File"));
				logFileSettings("Manual CAS Mappings File", getFile("Manual CAS Mappings File"));
				logFileSettings("Manual Ingedient Overrule Mappings File", getFile("Manual Ingedient Overrule Mappings File"));
				logFileSettings("Manual Ingedient Fallback Mappings File", getFile("Manual Ingedient Fallback Mappings File"));
				logFileSettings("Manual Drug Mappings File", getFile("Manual Drug Mappings File"));
				logFolderSettings(mainFrame.getOutputFolder());
				logGeneralSettings();
				new GenericMapping(
						mainFrame,
						getDatabase(), 
						getFile("Generic Drugs File"),
						getFile("Ingredient Name Translation File"),
						getFile("Unit Mapping File"),
						getFile("Dose Form Mapping File"),
						getFile("Manual CAS Mappings File"), 
						getFile("Manual Ingedient Overrule Mappings File"), 
						getFile("Manual Ingedient Fallback Mappings File"), 
						getFile("Manual Drug Mappings File")
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
	
	
	private void logFileSettings(String fileId, InputFile file) {
		if (file.getFileName() != null) {
			System.out.println("Input File: " + fileId);
			System.out.println("  Filename: " + file.getFileName());
			System.out.println("  Field delimiter: '" + file.getFieldDelimiter() + "'");
			System.out.println("  Text qualifier: '" + file.getTextQualifier() + "'");
			System.out.println("  Fields:");
			List<String> columns = file.getColumns();
			Map<String, String> columnMapping = file.getColumnMapping();
			for (String column : columns) {
				System.out.println("    " + column + " -> " + columnMapping.get(column));
			}
			System.out.println();
		}
	}
	
	
	private void logFolderSettings(Folder folder) {
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
	}

}
