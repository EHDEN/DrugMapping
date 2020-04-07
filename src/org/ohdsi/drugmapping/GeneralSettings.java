package org.ohdsi.drugmapping;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.gui.Setting;

public class GeneralSettings extends JPanel {
	private static final long serialVersionUID = 4495095183509328565L;
	
	List<Setting> generalSettings = new ArrayList<Setting>();
	JPanel settingsListPanel = null;
	
	
	public GeneralSettings(MainFrame mainFrame) {
		
		setLayout(new GridLayout(0, 1));
		setBorder(BorderFactory.createTitledBorder("General Settings"));
		
		JPanel generalSettingsPanel = new JPanel(new BorderLayout());
		settingsListPanel = new JPanel();
		settingsListPanel.setLayout(new BoxLayout(settingsListPanel, BoxLayout.PAGE_AXIS));
		settingsListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		generalSettingsPanel.add(settingsListPanel, BorderLayout.WEST);
		add(generalSettingsPanel);
	}
	
	
	public int addSetting(Setting setting) {
		generalSettings.add(setting);
		settingsListPanel.add(setting);
		
		return generalSettings.indexOf(setting);
	}
	
	
	public List<String> getSettings() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# General Settings");
		settings.add("#");
		settings.add("");
		for (Setting generalSetting : generalSettings) {
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
		if (generalSettings.get(index).getValueType() == Setting.SETTING_TYPE_DOUBLE) {
			value = generalSettings.get(index).getValueAsString();
		}
		return value;
	}
}
