package org.ohdsi.drugmapping;

import java.io.File;
import java.io.IOException;
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
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.gui.Setting;
import org.ohdsi.drugmapping.zindex.ZIndexConversion;
import org.ohdsi.drugmapping.zindex.ZIndexConversionInputFiles;

public class DrugMapping { 
	public static GeneralSettings settings = null;
	public static String outputVersion = "";
	public static String special = "";
	
	private static Set<JComponent> componentsToDisableWhenRunning = new HashSet<JComponent>();
	
	private static String currentDate = null;
	private static String currentPath = null;	
	private static String basePath = null;
	private static MappingInputDefinition inputFiles = null;
	
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
		String logFileName = null;
		special = parameters.get("special");
		
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
			dbSettings = mainFrame.readSettingsFromFile(parameters.get("databasesettings"));
		}
		if (parameters.containsKey("password")) {
			password = parameters.get("password");
		}
		if (parameters.containsKey("filesettings")) {
			fileSettings = mainFrame.readSettingsFromFile(parameters.get("filesettings"));
		}
		if (parameters.containsKey("generalsettings")) {
			generalSettings = mainFrame.readSettingsFromFile(parameters.get("generalsettings"));
		}
		if (parameters.containsKey("path")) {
			basePath = parameters.get("path");
			currentPath = basePath;
		}
		else {
			try {
				basePath = new File("./").getCanonicalPath().replaceAll("\\\\", "/");
				currentPath = basePath;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		File logFile;
		if (basePath != null) {
			outputVersion = getOutputVersion(logFileName);
			logFileName = basePath + "/" + outputVersion + logFileName;
		}
		else {
			outputVersion = "";
			logFile = new File(logFileName);
			try {
				basePath = logFile.getCanonicalPath().replaceAll("\\\\", "/");
				basePath = basePath.substring(0, basePath.lastIndexOf(File.pathSeparator));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (fileSettings != null) {
			mainFrame.loadFileSettingsFile(fileSettings);
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
		
		if (logFileName != null) {
			mainFrame.setLogFile(logFileName);
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
	}
	
	
	public void StartMapping() {
		//vocabulary = new Vocabulary(getDatabase());
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
				logFileSettings("ZIndex Ignored Words File", getFile("ZIndex Ignored Words File"));
				logFileSettings("ZIndex Ingredient Name Translation File", getFile("ZIndex Ingredient Name Translation File"));
				logFileSettings("ZIndex GPK IPCI Compositions File", getFile("ZIndex GPK IPCI Compositions File"));
				new ZIndexConversion(getFile("ZIndex GPK File"), getFile("ZIndex GSK File"), getFile("ZIndex GNK File"), getFile("ZIndex GPK Statistics File"), getFile("ZIndex Ignored Words File"), getFile("ZIndex Ingredient Name Translation File"), getFile("ZIndex GPK IPCI Compositions File"));
			}
			else {
				logDatabaseSettings(getDatabase());
				logFileSettings("Generic Drugs File", getFile("Generic Drugs File"));
				logFileSettings("CAS File", getFile("CAS File"));
				logGeneralSettings();
				new GenericMapping(getDatabase(), getFile("Generic Drugs File"), getFile("CAS File"), getFile("Manual Mappings File"));
			}

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
	
	
	private void logGeneralSettings() {
		System.out.println("General Settings:");
		for (Setting setting : DrugMapping.settings.getSettings()) {
			System.out.println(setting.getLabel() + " " + setting.getValueAsString());
		}
		System.out.println();
	}
	
	
	public static void main(String[] args) {
		Map<String, String> parameters = new HashMap<String, String>();

		for (int i = 0; i < args.length; i++) {
			int equalSignIndex = args[i].indexOf("=");
			String argVariable = args[i].substring(0, equalSignIndex).toLowerCase();
			String value = args[i].substring(equalSignIndex + 1);
			parameters.put(argVariable, value);
		}
		
		DrugMapping drugMapping = new DrugMapping(parameters);
		drugMapping.Show();
	}

}
