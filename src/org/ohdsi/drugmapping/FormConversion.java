package org.ohdsi.drugmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormConversion {
	private List<MatchingFormPatterns> matchingPatternsList = new ArrayList<MatchingFormPatterns>();
	private Map<String, Map<String, MatchingFormPatterns>> matchingLog = new HashMap<String, Map<String, MatchingFormPatterns>>();
	
	
	public void add(String localPattern, String cdmPattern) {
		matchingPatternsList.add(new MatchingFormPatterns(localPattern, cdmPattern));
	}
	
	
	public boolean matchingPharmaceuticalForms(String localForm, String cdmForm) {
		MatchingFormPatterns match = null;
		
		for (MatchingFormPatterns matchingPatterns : matchingPatternsList) {
			match = matchingPatterns.match(localForm, cdmForm);
			if (match != null) {
				Map<String, MatchingFormPatterns> localFormLog = matchingLog.get(localForm);
				if (localFormLog == null) {
					localFormLog = new HashMap<String, MatchingFormPatterns>();
					matchingLog.put(localForm, localFormLog);
				}
				localFormLog.put(cdmForm, matchingPatterns);
				break;
			}
		}
		
		return (match != null);
	}
	
	
	public void writeLogToFile(String fileName) {
		try {
			PrintWriter logFile = new PrintWriter(new File(fileName));
			String header = "LocalForm";
			header += "," + "CDMForm";
			header += "," + "LocalPattern";
			header += "," + "CDMPattern";
			logFile.println(header);
			for (String localForm : matchingLog.keySet()) {
				Map<String, MatchingFormPatterns> localFormLog = matchingLog.get(localForm);
				for (String cdmForm : localFormLog.keySet()) {
					MatchingFormPatterns matchingPatterns = localFormLog.get(cdmForm);
					String record = localForm;
					record += "," + cdmForm;
					record += "," + matchingPatterns.getLocalPattern();
					record += "," + matchingPatterns.getCDMPattern();
					logFile.println(record);
				}
			}
			logFile.close();
		} catch (FileNotFoundException e) {
			System.out.println("    WARNING: Cannot write pharmaceutical form matching log to file '" + fileName + "'");
		}
	}
	
	
	private class MatchingFormPatterns {
		private String localPattern = null;
		private String cdmPattern = null;
		
		
		public MatchingFormPatterns(String localPattern, String cdmPattern) {
			this.localPattern = localPattern;
			this.cdmPattern = cdmPattern;
		}
		
		
		public String getLocalPattern() {
			return localPattern;
		}
		
		
		public String getCDMPattern() {
			return cdmPattern;
		}
		
		
		public MatchingFormPatterns match(String localForm, String cdmForm) {
			return (patternMatch(localForm, localPattern) && patternMatch(cdmForm, cdmPattern)) ? this : null;
		}
		
		
		private boolean patternMatch(String text, String pattern) {
			boolean match = false;

			if (pattern.startsWith("%")) {
				if (pattern.endsWith("%")) {
					match = text.contains(pattern.substring(1, pattern.length() - 1));
				}
				else {
					match = text.endsWith(pattern.substring(1));
				}
			}
			else if (pattern.endsWith("%")) {
				match = text.startsWith(pattern.substring(0, pattern.length() - 1));
			}
			else {
				match = text.equals(pattern);
			}
			
			return match;
		}
		
	}
}
