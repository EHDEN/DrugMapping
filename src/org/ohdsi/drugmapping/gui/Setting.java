package org.ohdsi.drugmapping.gui;

import javax.swing.JPanel;

public class Setting extends JPanel {
	private static final long serialVersionUID = -1756870820181266594L;

	protected final int SETTING_LABEL_SIZE = 260;
	
	public static final int SETTING_TYPE_LONG    = 0;
	public static final int SETTING_TYPE_DOUBLE  = 1;
	public static final int SETTING_TYPE_STRING  = 2;
	public static final int SETTING_TYPE_BOOLEAN = 3;
	
	protected boolean correct = false;
	protected String name = "";
	protected String label = "";
	protected int valueType = -1;

	public void initialize() {
		// Should be overloaded
	};
	
	
	public String getName() {
		return name;
	}
	
	
	public String getLabel() {
		return label;
	}
	
	
	public String getValueAsString() {
		// Should be overloaded
		return null;
	}
	
	
	public void setValueAsString(String stringValue) {
		// Should be overloaded
	}
	
	
	public int getValueType() {
		return valueType;
	}
	
	
	public boolean isSetCorrectly() {
		return correct;
	};

}