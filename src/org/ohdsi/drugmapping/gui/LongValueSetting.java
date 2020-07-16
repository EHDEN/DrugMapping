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


public class LongValueSetting extends Setting {
	private static final long serialVersionUID = 5333685802924611718L;

	private JTextField longValueField = null;
	private Long value = null;

	
	public LongValueSetting(MainFrameTab mainFrameTab, String name, String label, Long defaultValue) {
		valueType = Setting.SETTING_TYPE_LONG;
		this.name = name;
		this.label = label;
		this.value = defaultValue;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder());

		JLabel longValueLabel = new JLabel(label);
		longValueLabel.setMinimumSize(new Dimension(SETTING_LABEL_SIZE, longValueLabel.getHeight()));
		longValueLabel.setPreferredSize(new Dimension(SETTING_LABEL_SIZE, longValueLabel.getHeight()));
		
		JPanel longValueFieldPanel = new JPanel(new BorderLayout());
		longValueField = new JTextField(6);
		longValueField.getDocument().addDocumentListener(new DocumentListener() {
			
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
					value = Long.parseLong(longValueField.getText());
					correct = true;
					mainFrameTab.checkReadyToStart();
				}
				catch (NumberFormatException e) {
					correct = false;
					mainFrameTab.checkReadyToStart();
				}
			}
		});
		longValueFieldPanel.add(longValueField, BorderLayout.WEST);
		
		setValue(defaultValue);
		
		add(longValueLabel);
		add(longValueFieldPanel);
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
	
	
	public Long getValue() {
		return value;
	}
	
	
	public void setValue(Long value) {
		this.value = value;
		longValueField.setText(value.toString());
		correct = true;
	}
	
	
	public String getValueAsString() {
		return value.toString();
	}
	
	
	public void setValueAsString(String stringValue) {
		try {
			setValue(Long.parseLong(stringValue));
		}
		catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Illegal value for general setting '" + name + "!\nShould be a long value.\nCurrent value is: " + value, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
