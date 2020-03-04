package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.List;

public class GeneralSettings {

	public Integer strengthDeviationPercentage = 0;
	
	
	public List<String> getSettings() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# General Settings");
		settings.add("#");
		settings.add("");
		settings.add("strengthDeviationPercentage=" + strengthDeviationPercentage);
		
		return settings;
	}
	
	
	public void putSettings(List<String> settings) {
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingVariable = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1);
				if (settingVariable.equals("strengthDeviationPercentage")) strengthDeviationPercentage = Integer.valueOf(value);
			}
		}
	}
}
