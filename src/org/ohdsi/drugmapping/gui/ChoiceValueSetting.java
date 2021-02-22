package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ChoiceValueSetting extends Setting {
	private static final long serialVersionUID = -5697418430701146284L;

	List<String> choices = new ArrayList<String>();
	JComboBox<String> choiceValueField = null;
	String value = null;
	
	
	public ChoiceValueSetting(MainFrameTab mainFrameTab, String name, String label, String[] choices, String defaultValue, Boolean isSpecial) {
		valueType = Setting.SETTING_TYPE_STRING;
		this.name = name;
		this.label = label;
		this.value = defaultValue;
		this.isSpecial = isSpecial;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder());

		JLabel choiceValueLabel = new JLabel(label);
		choiceValueLabel.setMinimumSize(new Dimension(SETTING_LABEL_SIZE, choiceValueLabel.getHeight()));
		choiceValueLabel.setPreferredSize(new Dimension(SETTING_LABEL_SIZE, choiceValueLabel.getHeight()));

		JPanel choiceValueFieldPanel = new JPanel(new BorderLayout());
		choiceValueField = new JComboBox<String>();
		for (String choice : choices) {
			this.choices.add(choice);
			choiceValueField.addItem(choice);
		}
		choiceValueField.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				value = (String) choiceValueField.getSelectedItem();
				correct = true;
			}
		});
		choiceValueFieldPanel.add(choiceValueField, BorderLayout.WEST);
		disableWhenRunning(choiceValueField);
		
		setValue(defaultValue);
		
		add(choiceValueLabel);
		add(choiceValueFieldPanel);
		initialize();
	}


	public void initialize() {
		if (value != null) {
			setValue(value);
		}
		else {
			correct = false;
		}
	}

	
	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		if (choices.contains(value)) {
			this.value = value;
			choiceValueField.setSelectedItem(value);
		}
		else {
			String possibleValues = "";
			for (String choice : choices) {
				if (!possibleValues.equals("")) {
					possibleValues += ", ";
				}
				possibleValues += "'" + choice + "'";
			}
			JOptionPane.showMessageDialog(null, "Illegal value '" + value + "' for general setting '" + name + "!\nPossible values are: " + possibleValues + "\nCurrent value is: " + value, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public String getValueAsString() {
		return value.toString();
	}
	
	
	public void setValueAsString(String stringValue) {
		setValue(stringValue);
	}
	
}
