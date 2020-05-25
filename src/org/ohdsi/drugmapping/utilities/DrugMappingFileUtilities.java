package org.ohdsi.drugmapping.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.ohdsi.drugmapping.DrugMapping;

public class DrugMappingFileUtilities {
	
	
	public static PrintWriter openOutputFile(String fileName, String header) {
		PrintWriter outputPrintWriter = null;
		String fullFileName = "";
		try {
			// Create output file
			fullFileName = DrugMapping.getBasePath() + "/" + DrugMapping.outputVersion + fileName;
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
