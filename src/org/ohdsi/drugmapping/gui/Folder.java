package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.ohdsi.drugmapping.DrugMapping;

public class Folder extends JPanel {
	private static final long serialVersionUID = -23890588306917810L;

	private final int FOLDER_LABEL_SIZE = 260;
	
	private String name = "";
	private String folderName = ".";
	
	private JPanel folderLabelPanel;
	private JLabel folderLabel;
	private JTextField folderNameField;
	private JButton folderSelectButton;
	
	
	public Folder(String name, String labelText, String defaultFolderName) {
		this.name = name;
		this.folderName = defaultFolderName;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		folderLabelPanel = new JPanel(new BorderLayout());
		folderLabelPanel.setMinimumSize(new Dimension(FOLDER_LABEL_SIZE, folderLabelPanel.getHeight()));
		folderLabelPanel.setPreferredSize(new Dimension(FOLDER_LABEL_SIZE, folderLabelPanel.getHeight()));
		folderLabel = new JLabel(labelText + ":");
		folderLabelPanel.add(folderLabel, BorderLayout.WEST);
		
		folderNameField = new JTextField();
		folderNameField.setText(defaultFolderName);
		folderNameField.setPreferredSize(new Dimension(10000, folderNameField.getHeight()));
		folderNameField.setEditable(false);

		folderSelectButton = new JButton("Select");

		add(folderLabelPanel);
		add(folderNameField);
		add(new JLabel("  "));
		add(folderSelectButton);
		
		folderSelectButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser folderChooser = new JFileChooser();
				folderChooser.setCurrentDirectory(new File(folderName)); // start at application current directory
				folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = folderChooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
				    folderName = folderChooser.getSelectedFile().getAbsolutePath();
				    folderNameField.setText(folderName);
				}
			}
		});
		
		DrugMapping.disableWhenRunning(folderSelectButton);
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public String getFolderName() {
		return folderName;
	}
	
	
	public List<String> getSettings() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# " + name);
		settings.add("#");
		settings.add("");
		settings.add(name + ".folderName=" + folderName);
		
		return settings;
	}
	
	
	public void putSettings(List<String> settings) {
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingPath = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1).trim();
				if (settingPath.equals(name + ".folderName")) {
					folderName = value;
					folderNameField.setText(folderName);
					break;
				}
			}
		}
	}

}
