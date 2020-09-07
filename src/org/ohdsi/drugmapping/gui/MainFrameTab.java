package org.ohdsi.drugmapping.gui;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.ohdsi.drugmapping.DrugMapping;

public class MainFrameTab extends JPanel {
	private static final long serialVersionUID = -2611669075696826114L;
	
	protected MainFrame mainFrame;
	protected JButton startButton = null;

	public MainFrameTab() {
		super();
	}
	
	
	public void checkReadyToStart() {
		if (startButton != null) {
			boolean readyToStart = true;
			if (DrugMapping.settings != null) {
				for (Setting setting : DrugMapping.settings.getSettings()) {
					readyToStart = readyToStart && setting.isSetCorrectly();
				}
			}
			startButton.setEnabled(readyToStart);
		}
	}
}
