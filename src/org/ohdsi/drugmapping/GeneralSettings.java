package org.ohdsi.drugmapping;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.ohdsi.drugmapping.gui.Setting;

public class GeneralSettings extends JPanel {
	private static final long serialVersionUID = 4495095183509328565L;
	
	List<Setting> generalSettings = new ArrayList<Setting>();
	List<Setting> visibleSettings = new ArrayList<Setting>();
	JPanel leftSettingsListPanel = null;
	JPanel rightSettingsListPanel = null;
	
	
	public GeneralSettings() {
		setLayout(new GridLayout(1, 2));
		setBorder(BorderFactory.createTitledBorder("General Settings"));
		
		JPanel leftSettingsPanel = new JPanel(new BorderLayout());
		leftSettingsListPanel = new JPanel();
		leftSettingsListPanel.setLayout(new BoxLayout(leftSettingsListPanel, BoxLayout.PAGE_AXIS));
		leftSettingsListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		leftSettingsPanel.add(leftSettingsListPanel, BorderLayout.WEST);
		
		JPanel rightSettingsPanel = new JPanel(new BorderLayout());
		rightSettingsListPanel = new JPanel();
		rightSettingsListPanel.setLayout(new BoxLayout(rightSettingsListPanel, BoxLayout.PAGE_AXIS));
		rightSettingsListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rightSettingsPanel.add(rightSettingsListPanel, BorderLayout.WEST);
		
		add(leftSettingsPanel);
		add(rightSettingsPanel);
	}
	
	
	public int addSetting(Setting setting) {
		generalSettings.add(setting);
		
		leftSettingsListPanel.removeAll();
		rightSettingsListPanel.removeAll();
		
		if (DrugMapping.special || (!setting.isSpecial())) {
			visibleSettings.add(setting);
		}
		
		int leftCount = (visibleSettings.size() / 2) + (visibleSettings.size() % 2);
		for (int visibleSettingNr = 0; visibleSettingNr < visibleSettings.size(); visibleSettingNr++) {
			Setting visibleSetting = visibleSettings.get(visibleSettingNr);
			if (visibleSettingNr < leftCount) {
				leftSettingsListPanel.add(visibleSetting);
			}
			else {
				rightSettingsListPanel.add(visibleSetting);
			}
		}
		
		return generalSettings.indexOf(setting);
	}
	
	
	public List<Setting> getSettings() {
		return generalSettings;
	}
	
	
	public boolean hasVisibleSettings() {
		return visibleSettings.size() > 0;
	}
	
	
	public List<String> getSettingsSave() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# General Settings");
		settings.add("#");
		settings.add("");
		for (Setting generalSetting : visibleSettings) {
			settings.add(generalSetting.getName() + "=" + generalSetting.getValueAsString());
		}
		
		return settings;
	}
	
	
	public void putSettings(List<String> settings) {
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingVariable = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1);
				for (Setting generalSetting : generalSettings) {
					if (settingVariable.equals(generalSetting.getName())) {
						generalSetting.setValueAsString(value);
					}
				}
			}
		}
	}
	
	
	public String getValueAsString(int index) {
		return generalSettings.get(index).getValueAsString();
	}
	
	
	public Long getLongSetting(int index) {
		Long value = null;
		if (generalSettings.get(index).getValueType() == Setting.SETTING_TYPE_LONG) {
			value = Long.parseLong(generalSettings.get(index).getValueAsString());
		}
		return value;
	}
	
	
	public Double getDoubleSetting(int index) {
		Double value = null;
		if (generalSettings.get(index).getValueType() == Setting.SETTING_TYPE_DOUBLE) {
			value = Double.parseDouble(generalSettings.get(index).getValueAsString());
		}
		return value;
	}
	
	
	public String getStringSetting(int index) {
		String value = null;
		if (generalSettings.get(index).getValueType() == Setting.SETTING_TYPE_STRING) {
			value = generalSettings.get(index).getValueAsString();
		}
		return value;
	}
	
	
	public String toString() {
		String description = "General Settings [";
		for (Setting setting : generalSettings) {
			description += "\n    " + setting;
		}
		description += "\n]";
		return description;
	}
}
