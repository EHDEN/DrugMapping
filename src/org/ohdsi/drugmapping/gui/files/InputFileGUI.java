package org.ohdsi.drugmapping.gui.files;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.files.FileDefinition;

public abstract class InputFileGUI extends JPanel {
	private static final long serialVersionUID = -931424973880160388L;
	
	private static final int FILE_LABEL_SIZE = 260;
	
	private static String currentDirectory = System.getProperty("user.dir");
	
	
	public static InputFileGUI getInputFile(Component parent, FileDefinition fileDefinition) {
		//if (fileDefinition.getFileType() == FileDefinition.GENERAL_FILE)   return new GeneralInputFileGUI(parent, fileDefinition);
		if (fileDefinition.getFileType() == FileDefinition.DELIMITED_FILE) return new DelimitedInputFileGUI(parent, fileDefinition);
		//if (fileDefinition.getFileType() == FileDefinition.EXCEL_FILE)     return new ExcelInputFileGUI(parent, fileDefinition);
		//if (fileDefinition.getFileType() == FileDefinition.XML_FILE)       return new XMLInputFileGUI(parent, fileDefinition);
		return null;
	}
	
	
	private String fileName = null;
	private String labelText;

	private Component parent;
	private FileDefinition fileDefinition;
	private JPanel fileLabelPanel;
	private JCheckBox fileSelectCheckBox;
	private JLabel fileLabel;
	private JTextField fileNameField;
	private JButton fileSelectButton;
	
	
	public static void setCurrentDirectory(String directory) {
		currentDirectory = directory;
	}
	
	
	public InputFileGUI(Component parent, FileDefinition fileDefinition) {
		this.parent = parent;
		this.fileDefinition = fileDefinition;
		this.labelText = fileDefinition.getFileName();
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		JPanel fileSelectLabelPanel = new JPanel(new BorderLayout());
		fileSelectLabelPanel.setMinimumSize(new Dimension(FILE_LABEL_SIZE, fileSelectLabelPanel.getHeight()));
		fileSelectLabelPanel.setPreferredSize(new Dimension(FILE_LABEL_SIZE, fileSelectLabelPanel.getHeight()));
		fileSelectCheckBox = new JCheckBox();
		fileSelectCheckBox.setSelected(true);
		fileSelectCheckBox.setEnabled(!fileDefinition.isRequired());
		fileSelectCheckBox.setToolTipText("Select/Deselect file");
		fileSelectLabelPanel.add(fileSelectCheckBox, BorderLayout.WEST);
		if ((!fileDefinition.isRequired())) {
			DrugMapping.disableWhenRunning(fileSelectCheckBox);
		}
		
		fileLabelPanel = new JPanel(new BorderLayout());
		fileLabel = new JLabel(labelText + ":");
		fileLabelPanel.add(fileLabel, BorderLayout.WEST);
		
		fileSelectLabelPanel.add(fileLabelPanel, BorderLayout.CENTER);
		
		fileNameField = new JTextField();
		fileNameField.setText("");
		fileNameField.setPreferredSize(new Dimension(10000, fileNameField.getHeight()));
		fileNameField.setEditable(false);

		fileSelectButton = new JButton("Select");

		add(fileSelectLabelPanel);
		add(fileNameField);
		add(new JLabel("  "));
		add(fileSelectButton);
		
		final InputFileGUI currentInputFile = this;
		fileSelectButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				defineFile(currentInputFile);
			}
		});
		
		fileLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (fileSelectCheckBox.isEnabled()) {
					fileSelectCheckBox.setSelected(!fileSelectCheckBox.isSelected());
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {}

			@Override
			public void mouseExited(MouseEvent arg0) {}

			@Override
			public void mousePressed(MouseEvent arg0) {}

			@Override
			public void mouseReleased(MouseEvent arg0) {}
			
		});
		
		DrugMapping.disableWhenRunning(fileSelectButton);
	}
	
	
	public Component getInterfaceParent() {
		return parent;
	}
	
	
	public FileDefinition getFileDefinition() {
		return fileDefinition;
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	
	public int getFileType() {
		return fileDefinition.getFileType();
	}
	
	
	public void setFileName(String fileName) {
		fileNameField.setText(fileName);
		this.fileName = fileName;
	}
	
	
	public String getLabelText() {
		return labelText;
	}
	
	
	public JLabel getLabel() {
		return fileLabel;
	}
	
	
	public JTextField getFileNameField() {
		return fileNameField;
	}
	
	
	public JButton getSelectButton() {
		return fileSelectButton;
	}
	
	
	public boolean isSelected() {
		return fileSelectCheckBox.isSelected();
	}
	
	
	public void setSelected(boolean selected) {
		fileSelectCheckBox.setSelected(selected);
	}
	
	
	public boolean selectFile(Component parent, JTextField fileField) {
		boolean result = false;
		String fileName = selectFile(parent, getFileFilters());
		if (fileName != null) {
			setFileName(fileName);
			fileField.setText(fileName);
			result = true;
		}
		return result;
	}
	
	
	private static String selectFile(Component parent, List<FileFilter> fileFilters) {
		String result = null;
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setCurrentDirectory(new File(currentDirectory));
		if (fileFilters != null) {
			for (int fileFilterNr = 0; fileFilterNr < fileFilters.size(); fileFilterNr++) {
				if (fileFilterNr == 0) {
					fileChooser.setFileFilter(fileFilters.get(fileFilterNr));
				}
				else {
					fileChooser.addChoosableFileFilter(fileFilters.get(fileFilterNr));
				}
			}
		}
		int returnVal = fileChooser.showDialog(parent, "Select file");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			result = fileChooser.getSelectedFile().getAbsolutePath();
			setCurrentDirectory(result.substring(0, result.lastIndexOf(File.separator)));
		}
		return result;
	}
	
	
	abstract public List<String> getSettings();
	
	
	abstract public void putSettings(List<String> settings);
	
	
	abstract void defineFile(InputFileGUI inputFile);
	
	
	abstract List<FileFilter> getFileFilters();
	
	
	abstract public boolean openFileForReading();
	
	
	abstract public boolean openFileForReading(boolean suppressError);
	
	
	abstract public void logFileSettings();

}
