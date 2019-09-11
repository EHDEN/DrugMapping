package org.ohdsi.drugmapping;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.genericmapping.GenericMappingInputFiles;
import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.ipcimapping.IPCIMAPPING;
import org.ohdsi.drugmapping.ipcimapping.IPCIMAPPINGInputFiles;
import org.ohdsi.drugmapping.zindex.ZIndexConversion;
import org.ohdsi.drugmapping.zindex.ZIndexConversionInputFiles;

public class DrugMapping {
	private static List<JComponent> componentsToDisableWhenRunning = new ArrayList<JComponent>();
	
	private static String currentDate = null;
	private static String currentPath = "";	
	private static MappingInputDefinition inputFiles = null; 
	
	private MainFrame mainFrame;
	private String special = "";
	
	
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
	
	
	public static String getCurrentPath() {
		return currentPath;
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
		String logFileName = null;
		special = parameters.get("special");
		
		if (special != null) {
			special = special.toUpperCase();
			if (special.equals("ZINDEX")) {
				inputFiles = new ZIndexConversionInputFiles();
			}
			else if (special.equals("IPCIMAPPING")) {
				inputFiles = new IPCIMAPPINGInputFiles();
			}
			else {
				JOptionPane.showMessageDialog(null, "Unknown special definition '" + special + "'!", "Dialog",JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
		else {
			special = "";
			inputFiles = new GenericMappingInputFiles();
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
		if (parameters.containsKey("logfile")) {
			logFileName = parameters.get("logfile");
		}

		File logFile;
		if (logFileName == null) {
			try {
				currentPath = new File("./").getCanonicalPath().replaceAll("\\\\", "/");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int sequenceNr = 1;
			boolean reUse = false;
			do {
				if (sequenceNr == 100) {
					sequenceNr = 1;
					reUse = true;
				}
				String sequenceNrString = Integer.toString(sequenceNr);
				logFileName = currentPath + "/" + getCurrentDate() + " " + ("00" + sequenceNrString).substring(sequenceNrString.length()) + " DrugMapping Log.txt";
				logFile = new File(logFileName);
			} while ((!reUse) && logFile.exists());
		}
		else {
			logFile = new File(logFileName);
			try {
				currentPath = logFile.getCanonicalPath().replaceAll("\\\\", "/");
				currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (fileSettings != null) {
			mainFrame.loadFileSettingsFile(fileSettings);
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
				new ZIndexConversion(getDatabase(), getFile("ZIndex GPK File"), getFile("ZIndex GSK File"), getFile("ZIndex GNK File"), getFile("ZIndex GPK Statistics File"), getFile("ZIndex Ignored Words File"));
			}
			else if (special.equals("IPCIMAPPING")) {
				new IPCIMAPPING(getDatabase(), getFile("Generic Drugs File"));
			}
			else {
				new GenericMapping(getDatabase(), getFile("Generic Drugs File"), getFile("CAS File"));
			}

			for (JComponent component : componentsToDisableWhenRunning)
				component.setEnabled(true);
		}
		
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
