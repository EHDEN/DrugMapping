package org.ohdsi.drugmapping.gui;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DoubleValueSetting extends Setting {
	private static final long serialVersionUID = -3800881489329031554L;

	private JTextField doubleValueField = null;
	Double value;
	
	
	public DoubleValueSetting(MainFrame mainFrame, String name, String label) {
		valueType = Setting.SETTING_TYPE_DOUBLE;
		this.name = name;
		
		setLayout(new FlowLayout(FlowLayout.LEFT));
		setBorder(BorderFactory.createEmptyBorder());
		JLabel maximumStrengthDeviationLabel = new JLabel("Max. strength deviation percentage:");
		doubleValueField = new JTextField(6);
		doubleValueField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				check();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				check();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				check();
			}
			
			
			private void check() {
				try {
					value = Double.parseDouble(doubleValueField.getText());
					correct = true;
					mainFrame.checkReadyToStart();
				}
				catch (NumberFormatException e) {
					correct = false;
					mainFrame.checkReadyToStart();
				}
			}
		});
		add(maximumStrengthDeviationLabel);
		add(doubleValueField);
		initialize();
	}


	public void initialize() {
		setValue(0.0);
	}

	
	public Double getValue() {
		return value;
	}


	public void setValue(Double value) {
		this.value = value;
		doubleValueField.setText(value.toString());
		correct = true;
	}
	
	
	public String getValueAsString() {
		return value.toString();
	}
	
	
	public void setValueAsString(String stringValue) {
		try {
			setValue(Double.parseDouble(stringValue));
		}
		catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Illegal value for general setting '" + name + "!\nShould be a double value.\nCurrent value is: " + value, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
