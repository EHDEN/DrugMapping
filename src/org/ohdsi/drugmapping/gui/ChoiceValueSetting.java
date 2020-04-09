package org.ohdsi.drugmapping.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

public class ChoiceValueSetting extends Setting {
	private static final long serialVersionUID = -5697418430701146284L;

	private List<JRadioButton> choiceRadioButtons= new ArrayList<JRadioButton>();
	private ButtonGroup choiceRadioButtonGroup = null;
	List<String> choices = new ArrayList<String>();
	String value = null;
	
	
	public ChoiceValueSetting(MainFrame mainFrame, String name, String label, String[] choices) {
		valueType = Setting.SETTING_TYPE_STRING;
		this.name = name;
		this.label = label;
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		setBorder(BorderFactory.createEmptyBorder());
		JLabel preferenceRxNormLabel = new JLabel(label);
		choiceRadioButtonGroup = new ButtonGroup();
		for (String choice : choices) {
			this.choices.add(choice);
			JRadioButton choiceRadioButton = new JRadioButton(choice);
			choiceRadioButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					JRadioButton radioButton = (JRadioButton) e.getSource();
					value = radioButton.getText();
					correct = true;
				}
			});
			choiceRadioButtons.add(choiceRadioButton);
			choiceRadioButtonGroup.add(choiceRadioButton);
		}
		add(preferenceRxNormLabel);
		for (JRadioButton choiceRadioButton : choiceRadioButtons) {
			add(choiceRadioButton);
		}
		initialize();
	}


	public void initialize() {
		setValue(choices.get(choices.size() - 1));
	}

	
	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		if (choices.contains(value)) {
			this.value = value;
			choiceRadioButtons.get(choices.indexOf(value)).setSelected(true);
			correct = true;
		}
		else {
			String possibleValues = "";
			for (String choice : choices) {
				if (!possibleValues.equals("")) {
					possibleValues += ", ";
				}
				possibleValues += choice;
			}
			JOptionPane.showMessageDialog(null, "Illegal value for general setting '" + name + "!\nPossible values are: " + possibleValues + "\nCurrent value is: " + value, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	public String getValueAsString() {
		return value.toString();
	}
	
	
	public void setValueAsString(String stringValue) {
		setValue(stringValue);
	}
	
}
