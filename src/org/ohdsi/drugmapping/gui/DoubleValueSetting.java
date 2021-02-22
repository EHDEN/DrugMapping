package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DoubleValueSetting extends Setting {
	private static final long serialVersionUID = -3800881489329031554L;

	private JTextField doubleValueField = null;
	Double value;
	
	
	public DoubleValueSetting(MainFrameTab mainFrameTab, String name, String label, Double defaultValue, Boolean isSpecial) {
		valueType = Setting.SETTING_TYPE_DOUBLE;
		this.name = name;
		this.label = label;
		this.value = defaultValue;
		this.isSpecial = isSpecial;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder());

		JLabel doubleValueLabel = new JLabel(label);
		doubleValueLabel.setMinimumSize(new Dimension(SETTING_LABEL_SIZE, doubleValueLabel.getHeight()));
		doubleValueLabel.setPreferredSize(new Dimension(SETTING_LABEL_SIZE, doubleValueLabel.getHeight()));

		JPanel doubleValueFieldPanel = new JPanel(new BorderLayout());
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
					mainFrameTab.checkReadyToStart();
				}
				catch (NumberFormatException e) {
					correct = false;
					mainFrameTab.checkReadyToStart();
				}
			}
		});
		doubleValueFieldPanel.add(doubleValueField, BorderLayout.WEST);
		disableWhenRunning(doubleValueField);
		
		setValue(defaultValue);
		
		add(doubleValueLabel);
		add(doubleValueFieldPanel);
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
