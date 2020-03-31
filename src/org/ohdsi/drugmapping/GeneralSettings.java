package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.List;

public class GeneralSettings {

	public Long minimumUseCount = 1L;
	public Double maximumStrengthDeviationPercentage = 0.0;
	
	
	public List<String> getSettings() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# General Settings");
		settings.add("#");
		settings.add("");
		settings.add("minimumUseCount=" + minimumUseCount);
		settings.add("maximumStrengthDeviationPercentage=" + maximumStrengthDeviationPercentage);
		
		return settings;
	}
	
	
	public void putSettings(List<String> settings) {
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingVariable = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1);
				if (settingVariable.equals("minimumUseCount")) {
					try {
						DrugMapping.settings.minimumUseCount = Long.valueOf(value);
					}
					catch (NumberFormatException e) {
						DrugMapping.settings.minimumUseCount = 1L;
					}
				}
				else if (settingVariable.equals("maximumStrengthDeviationPercentage")) {
					try {
						DrugMapping.settings.maximumStrengthDeviationPercentage = Double.valueOf(value);
					}
					catch (NumberFormatException e) {
						DrugMapping.settings.maximumStrengthDeviationPercentage = 0.0;
					}
				}
			}
		}
	}
}
