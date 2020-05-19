package org.ohdsi.drugmapping.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class BooleanValueSetting extends Setting {
	private static final long serialVersionUID = 1461696562209449449L;

	private JCheckBox checkBoxField = null;
	Boolean value = null;
	
	
	public BooleanValueSetting(MainFrame mainFrame, String name, String label, Boolean defaultValue) {
		valueType = Setting.SETTING_TYPE_BOOLEAN;
		this.name = name;
		this.label = label;
		this.value = defaultValue;
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		setBorder(BorderFactory.createEmptyBorder());
		JLabel checkBoxLabel = new JLabel(label);
		checkBoxField = new JCheckBox();
		checkBoxField.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkBoxField.isSelected()) {
					setValue("Yes");
				}
				else {
					setValue("No");
				}
			}
		});
		
		add(checkBoxLabel);
		add(checkBoxField);
		initialize();
	}


	public void initialize() {
		if (value != null) {
			setValue(value ? "Yes" : "No");
		}
		else {
			correct = false;
		}
	}

	
	public Boolean getValue() {
		return value;
	}


	public void setValue(String value) {
		if (value.equals("Yes")) {
			checkBoxField.setSelected(true);
			this.value = true;
			correct = true;
		}
		else if (value.equals("No")) {
			checkBoxField.setSelected(false);
			this.value = false;
			correct = true;
		}
		else {
			JOptionPane.showMessageDialog(null, "Illegal value for general setting '" + name + "!\nPossible values are: 'Yes' or 'No'\nCurrent value is: " + value, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public String getValueAsString() {
		return value == null ? "" : (value ? "Yes" : "No");
	}
	
	
	public void setValueAsString(String stringValue) {
		setValue(stringValue);
	}
}
