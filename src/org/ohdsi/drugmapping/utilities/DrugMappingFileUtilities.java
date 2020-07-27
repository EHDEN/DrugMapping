package org.ohdsi.drugmapping.utilities;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.ohdsi.drugmapping.DrugMapping;

public class DrugMappingFileUtilities {
	
	
	public static String selectCSVFile(Component parent) {
		return selectCSVFile(parent, ".csv", "CSV Files");
	}
	
	
	public static String selectCSVFile(Component parent, String fileNameEndsWith, String description) {
		String result = null;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(DrugMapping.getCurrentPath() == null ? (DrugMapping.getBasePath() == null ? System.getProperty("user.dir") : DrugMapping.getBasePath()) : DrugMapping.getCurrentPath()));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if ((fileNameEndsWith != null) && (!fileNameEndsWith.equals(""))) {
			fileChooser.setFileFilter(new FileFilter() {

		        @Override
		        public boolean accept(File f) {
		            return f.getName().endsWith(fileNameEndsWith);
		        }

		        @Override
		        public String getDescription() {
		            return description;
		        }

		    });
		}
		int returnVal = fileChooser.showDialog(parent, "Select file");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			result = fileChooser.getSelectedFile().getAbsolutePath();
		}
		return result;
	}
	
	
	public static PrintWriter openOutputFile(String fileName, String header) {
		PrintWriter outputPrintWriter = null;
		String fullFileName = "";
		try {
			// Create output file
			fullFileName = DrugMapping.baseName + fileName;
			outputPrintWriter = new PrintWriter(new File(fullFileName));
			outputPrintWriter.println(header);
		}
		catch (FileNotFoundException e) {
			System.out.println("      ERROR: Cannot create output file '" + fullFileName + "'");
			outputPrintWriter = null;
		}
		return outputPrintWriter;
	}
	
	
	public static void closeOutputFile(PrintWriter outputFile) {
		if (outputFile != null) {
			outputFile.close();
		}
	}
}
