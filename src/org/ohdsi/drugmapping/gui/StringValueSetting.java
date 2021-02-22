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

public class StringValueSetting extends Setting {
	private static final long serialVersionUID = 6643498008378245661L;

	private JTextField stringValueField = null;
	String value;
	
	
	public StringValueSetting(MainFrameTab mainFrameTab, String name, String label, String defaultValue, Boolean isSpecial) {
		valueType = Setting.SETTING_TYPE_STRING;
		this.name = name;
		this.label = label;
		this.value = defaultValue;
		this.isSpecial = isSpecial;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder());

		JLabel stringValueLabel = new JLabel(label);
		stringValueLabel.setMinimumSize(new Dimension(SETTING_LABEL_SIZE, stringValueLabel.getHeight()));
		stringValueLabel.setPreferredSize(new Dimension(SETTING_LABEL_SIZE, stringValueLabel.getHeight()));
		
		JPanel stringValueFieldPanel = new JPanel(new BorderLayout());
		stringValueField = new JTextField(6);
		stringValueField.getDocument().addDocumentListener(new DocumentListener() {
			
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
				value = stringValueField.getText();
				correct = (!value.trim().equals(""));
				mainFrameTab.checkReadyToStart();
			}
		});
		stringValueFieldPanel.add(stringValueField, BorderLayout.WEST);
		disableWhenRunning(stringValueField);
		
		setValue(defaultValue);
		
		add(stringValueLabel);
		add(stringValueFieldPanel);
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
		this.value = value;
		stringValueField.setText(value);
		correct = true;
	}
	
	
	public String getValueAsString() {
		return value.toString();
	}
	
	
	public void setValueAsString(String stringValue) {
		try {
			setValue(stringValue);
		}
		catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Illegal value for general setting '" + name + "!\nShould be a double value.\nCurrent value is: " + value, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
