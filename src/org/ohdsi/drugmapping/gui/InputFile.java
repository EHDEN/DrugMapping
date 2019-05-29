package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.utilities.files.ReadCSVFileWithHeader;
import org.ohdsi.utilities.files.Row;

public class InputFile extends JPanel {
	private static final long serialVersionUID = -8908651240263793215L;
	
	private final String[] FIELD_DELIMITERS = new String[]{ "Tab", "Semicolon", "Comma", "Space", "Other" };
	private final String[] TEXT_QUALIFIERS  = new String[]{ "\"", "'", "None" };
	private final String   CHAR_SET         = "ISO-8859-1";
	
	private FileDefinition fileDefinition;
	private String labelText;
	
	private JPanel fileLabelPanel;
	private JLabel fileLabel;
	private JTextField fileNameField;
	private JButton fileSelectButton;
	private List<JComboBox<String>> comboBoxList;

	private String fileName = "";
	private String fieldDelimiter = "Comma";
	private String textQualifier = "None";
	private Map<String, String> columnMapping = new HashMap<String, String>();
	
	private Iterator<Row> fileIterator;
	
	
	public InputFile(FileDefinition fileDefinition) {
		this.fileDefinition = fileDefinition;
		this.labelText = fileDefinition.getFileName();
		
		for (FileColumnDefinition column : fileDefinition.getColumns()) {
			columnMapping.put(column.getColumnName(), null);
		}
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		fileLabelPanel = new JPanel(new BorderLayout());
		fileLabelPanel.setMinimumSize(new Dimension(150, fileLabelPanel.getHeight()));
		fileLabelPanel.setPreferredSize(new Dimension(150, fileLabelPanel.getHeight()));
		fileLabel = new JLabel(labelText + ":");
		fileLabelPanel.add(fileLabel, BorderLayout.WEST);
		
		fileNameField = new JTextField();
		fileNameField.setText("");
		fileNameField.setPreferredSize(new Dimension(10000, fileNameField.getHeight()));
		fileNameField.setEditable(false);

		fileSelectButton = new JButton("Select");

		add(fileLabelPanel);
		add(fileNameField);
		add(new JLabel("  "));
		add(fileSelectButton);
		
		final InputFile currentInputFile = this;
		fileSelectButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				defineFile(currentInputFile);
			}
		});
		
		DrugMapping.disableWhenRunning(fileSelectButton);
	}
	
	
	public String getLabelText() {
		return labelText;
	}
	
	
	public JLabel getLabel() {
		return fileLabel;
	}
	
	
	public JTextField getFileNameLabel() {
		return fileNameField;
	}
	
	
	public JButton getSelectButton() {
		return fileSelectButton;
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	
	public void setFileName(String fileName) {
		fileNameField.setText(fileName);
		this.fileName = fileName;
	}
	
	
	public String getFieldDelimiter() {
		return fieldDelimiter;
	}
	
	
	public void setFieldDelimiter(String delimiter) {
		fieldDelimiter = delimiter;
	}
	
	
	public String getTextQualifier() {
		return textQualifier;
	}
	
	
	public void setTextQualifier(String qualifier) {
		textQualifier = qualifier;
	}
	
	
	public List<String> getColumns() {
		List<String> columns = new ArrayList<String>();
		for (FileColumnDefinition column : fileDefinition.getColumns()) {
			columns.add(column.getColumnName());
		}
		return columns;
	}
	
	
	public Map<String, String> getColumnMapping() {
		return columnMapping;
	}
	
	
	public void setColumnMapping(Map<String, String> columnMapping) {
		this.columnMapping = columnMapping;
	}
	
	public boolean openFile() {
		boolean result = false;
		
		char delmiter = ',';
		if      (fieldDelimiter.equals("Tab"))       delmiter = '\t';
		else if (fieldDelimiter.equals("Semicolon")) delmiter = ';';
		else if (fieldDelimiter.equals("Comma"))     delmiter = ',';
		else if (fieldDelimiter.equals("Space"))     delmiter = ' ';
		else                                         delmiter = this.fieldDelimiter.toCharArray()[0];

		char textDelimiter = '\"';
		if      (textQualifier.equals("None"))       textDelimiter = (char) 0;
		else                                         textDelimiter = this.textQualifier.toCharArray()[0];
		
		ReadCSVFileWithHeader readFile = new ReadCSVFileWithHeader(getFileName(), delmiter, textDelimiter);
		if (readFile.isOpen()) {
			result = true;
			fileIterator = readFile.iterator();
		}
		else {
			JOptionPane.showMessageDialog(null, "Couldn't open file for reading!", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return result;
	}
	
	
	public boolean hasNext() {
		return fileIterator.hasNext();
	}
	
	
	public Row next() {
		return fileIterator.next();
	}
	
	
	public boolean hasField(String fieldName) {
		return (columnMapping.get(fieldName) != null);
	}
	
	
	public String get(Row row, String fieldName) {
		String mappedFieldName = columnMapping.get(fieldName);	
		return row.get(mappedFieldName);
	}
	
	
	public List<String> getSettings() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# " + getLabelText());
		settings.add("#");
		settings.add("");
		settings.add(labelText + ".filename=" + fileName);
		settings.add(labelText + ".fieldDelimiter=" + fieldDelimiter);
		settings.add(labelText + ".textQualifier=" + textQualifier);
		for (String column : getColumns()) {
			settings.add(labelText + ".column." + column + "=" + (columnMapping.get(column) == null ? "" : columnMapping.get(column)));
		}
		
		return settings;
	}
	
	
	public void putSettings(List<String> settings) {
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingPath = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1);
				String[] settingPathSplit = settingPath.split("\\.");
				if ((settingPathSplit.length > 0) && (settingPathSplit[0].equals(getLabelText()))) {
					if ((settingPathSplit.length == 3) && (settingPathSplit[1].equals("column"))) { // Column mapping
						if (getColumns().contains(settingPathSplit[2])) {
							columnMapping.put(settingPathSplit[2], value);
						}
					}
					else if (settingPathSplit.length == 2) {
						if (settingPathSplit[1].equals("filename")) setFileName(value);
						else if (settingPathSplit[1].equals("fieldDelimiter")) setFieldDelimiter(value);
						else if (settingPathSplit[1].equals("textQualifier")) setTextQualifier(value);
						else {
							// Unknown setting
						}
					}
				}
			}
		}
	}
	
	
	private void defineFile(InputFile inputFile) {
		JDialog fileDialog = new JDialog();
		fileDialog.setLayout(new BorderLayout());
		fileDialog.setModal(true);
		fileDialog.setSize(500, 400);
		MainFrame.setIcon(fileDialog);
		fileDialog.setLocationRelativeTo(null);
		fileDialog.setTitle("Input File Definition");
		fileDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		// File section
		JPanel fileSectionPanel = new JPanel(new BorderLayout());
		fileSectionPanel.setBorder(BorderFactory.createTitledBorder(inputFile.labelText));
		JPanel fileSectionSubPanel = new JPanel(new BorderLayout());
		
		JPanel fileDescriptionPanel = new JPanel(new BorderLayout());
		fileDescriptionPanel.setBorder(BorderFactory.createEmptyBorder());
		JTextArea fileDescriptionField = new JTextArea();
		fileDescriptionField.setEditable(false);
		fileDescriptionField.setBackground(fileDescriptionPanel.getBackground());
		String description = "";
		for (String line : fileDefinition.getDescription()) {
			if (!description.equals("")) {
				description += "\n";
			}
			description += line;
		}
		fileDescriptionField.setText(description);
		fileDescriptionPanel.add(fileDescriptionField, BorderLayout.NORTH);
		
		JPanel fileChooserPanel = new JPanel();
		fileChooserPanel.setLayout(new BoxLayout(fileChooserPanel, BoxLayout.X_AXIS));
		fileChooserPanel.setBorder(BorderFactory.createEmptyBorder());
		
		JTextField fileField = new JTextField(inputFile.getFileName());
		fileField.setEditable(false);
		fileLabelPanel.setMinimumSize(new Dimension(100, fileLabelPanel.getHeight()));
		fileLabelPanel.setPreferredSize(new Dimension(100, fileLabelPanel.getHeight()));
		
		JButton fileButton = new JButton("Browse");

		fileChooserPanel.add(new JLabel("  File: "));
		fileChooserPanel.add(fileField);
		fileChooserPanel.add(new JLabel("  "));
		fileChooserPanel.add(fileButton);
		
		JPanel fileDelimiterPanel = new JPanel(new BorderLayout());
		
		JPanel fileDelimiterSubPanel = new JPanel(new FlowLayout());
		fileDelimiterSubPanel.add(new JLabel("Delimiter:"));
		ButtonGroup fieldDelimiterButtons = new ButtonGroup();
		JRadioButton otherButton = null;
		for (String delimiter : FIELD_DELIMITERS) {
			JRadioButton radioButton = new JRadioButton(delimiter);
			radioButton.setActionCommand(delimiter);
			radioButton.setSelected(delimiter.equals(inputFile.getFieldDelimiter()));
			fieldDelimiterButtons.add(radioButton);
			fileDelimiterSubPanel.add(radioButton);
			if (delimiter.equals("Other")) {
				otherButton = radioButton;
			}
		}
		final JRadioButton otherRadioButton = otherButton;

		JTextField otherDelimiter = new JTextField("");
		Dimension dimension = new Dimension(30, 20);
		otherDelimiter.setDocument(new JTextFieldLimit(1));
		otherDelimiter.setMinimumSize(dimension);
		otherDelimiter.setMaximumSize(dimension);
		otherDelimiter.setPreferredSize(dimension);
		fileDelimiterSubPanel.add(otherDelimiter);
		
		fileDelimiterPanel.add(fileDelimiterSubPanel, BorderLayout.WEST);
		
		JPanel fileTextQualifierPanel = new JPanel(new BorderLayout());
		
		JPanel fileTextQualifierSubPanel = new JPanel(new FlowLayout());
		fileTextQualifierSubPanel.add(new JLabel("Text qualifier:"));
		ButtonGroup textQualifierButtons = new ButtonGroup();
		for (String qualifier : TEXT_QUALIFIERS) {
			JRadioButton radioButton = new JRadioButton(qualifier);
			radioButton.setActionCommand(qualifier);
			radioButton.setSelected(qualifier.equals(inputFile.getTextQualifier()));
			textQualifierButtons.add(radioButton);
			fileTextQualifierSubPanel.add(radioButton);
		}
		fileTextQualifierPanel.add(fileTextQualifierSubPanel, BorderLayout.WEST);

		fileSectionPanel.add(fileDescriptionPanel, BorderLayout.NORTH);
		fileDescriptionPanel.add(fileChooserPanel, BorderLayout.SOUTH);
		fileSectionPanel.add(fileSectionSubPanel, BorderLayout.SOUTH);
		fileSectionSubPanel.add(fileDelimiterPanel, BorderLayout.NORTH);
		fileSectionSubPanel.add(fileTextQualifierPanel, BorderLayout.SOUTH);
		
		// Mapping section
		JPanel mappingSectionPanel = new JPanel(new BorderLayout());
		mappingSectionPanel.setBorder(BorderFactory.createTitledBorder("Column Mapping"));
		
		JPanel mappingScrollPanel = new JPanel(new BorderLayout());
		mappingScrollPanel.setBorder(BorderFactory.createEmptyBorder());
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getViewport().add(mappingScrollPanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		mappingSectionPanel.add(scrollPane, BorderLayout.CENTER);
		
		boolean first = true;
		JPanel lastPanel = mappingScrollPanel; 
		comboBoxList = new ArrayList<JComboBox<String>>();
		for (FileColumnDefinition column : fileDefinition.getColumns()) {
			if (!first) {
				JPanel newPanel = new JPanel(new BorderLayout());
				lastPanel.add(newPanel, BorderLayout.CENTER);
				lastPanel = newPanel;
			}
			JPanel columnPanel = new JPanel();
			columnPanel.setLayout(new GridLayout(0, 2));
			JComboBox<String> comboBox = new JComboBox<String>(new String[]{});
			comboBoxList.add(comboBox);
			JLabel columnLabel = new JLabel(column.getColumnName());
			String columnDescription = "";
			for (String line : column.getDescription()) {
				if (columnDescription.equals("")) {
					columnDescription += "<html>";
				}
				else {
					columnDescription += "<br>";
				}
				columnDescription += line;
			}
			columnDescription += "</html>";
			columnLabel.setToolTipText(columnDescription);
			columnPanel.add(columnLabel);
			columnPanel.add(comboBox);
			
			lastPanel.add(columnPanel, BorderLayout.NORTH);
			
			first = false;
		}
		
		// Button section
		JPanel buttonSectionPanel = new JPanel(new BorderLayout());
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton okButton = new JButton("    OK    ");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String fieldDelimiter = geFieldDelimiterFromInput(fieldDelimiterButtons, otherDelimiter.getText());
				String textQualifier = textQualifierButtons.getSelection().getActionCommand();
				if (saveFileSettings(inputFile, fileField.getText(), fieldDelimiter, textQualifier, comboBoxList)) {
					fileDialog.dispose();
				}				
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fileDialog.dispose();				
			}
		});
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		buttonSectionPanel.add(buttonPanel, BorderLayout.WEST); 
		
		// Browse action
		fileButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectFile(fileDialog, fileField)) {
					updateColumns(fileField.getText(), geFieldDelimiterFromInput(fieldDelimiterButtons, otherDelimiter.getText()), comboBoxList);
				}
			}
		});
		
		// Delimiter selection action
		for (Enumeration<AbstractButton> e = fieldDelimiterButtons.getElements(); e.hasMoreElements();) {
			JRadioButton fieldDelimiterButton = (JRadioButton)e.nextElement();
			fieldDelimiterButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					updateColumns(fileField.getText(), geFieldDelimiterFromInput(fieldDelimiterButtons, otherDelimiter.getText()), comboBoxList);
				}
			});
		}
		
		otherDelimiter.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				otherRadioButton.setSelected(true);
				updateColumns(fileField.getText(), geFieldDelimiterFromInput(fieldDelimiterButtons, otherDelimiter.getText()), comboBoxList);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				otherRadioButton.setSelected(true);
				updateColumns(fileField.getText(), geFieldDelimiterFromInput(fieldDelimiterButtons, otherDelimiter.getText()), comboBoxList);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				otherRadioButton.setSelected(true);
				updateColumns(fileField.getText(), geFieldDelimiterFromInput(fieldDelimiterButtons, otherDelimiter.getText()), comboBoxList);
			}
		});

		fileDialog.add(fileSectionPanel, BorderLayout.NORTH);
		fileDialog.add(mappingSectionPanel, BorderLayout.CENTER);
		fileDialog.add(buttonSectionPanel, BorderLayout.SOUTH);
		
		updateColumns(fileName, fieldDelimiter, comboBoxList);
		
		fileDialog.setVisible(true);
	}
	
	
	private boolean selectFile(Component parent, JTextField fileField) {
		boolean result = false;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile(new File(System.getProperty("user.dir")));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv", "txt"));
		int returnVal = fileChooser.showDialog(parent, "Select file");
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
			result = true;
		}
		return result;
	}
	
	
	private String geFieldDelimiterFromInput(ButtonGroup fieldDelimiterButtons, String otherDelimiter) {
		String fieldDelimiter = fieldDelimiterButtons.getSelection().getActionCommand();
		if (fieldDelimiter.equals("Other")) {
			fieldDelimiter = otherDelimiter;
		}
		return fieldDelimiter;
	}
	
	
	private boolean saveFileSettings(InputFile inputFile, String fileName, String fieldDelimiter, String textQualifier, List<JComboBox<String>> comboBoxList) {
		boolean saveOK = false;
		boolean columnMappingComplete = true;
		for (int columnNr = 0; columnNr < inputFile.getColumns().size(); columnNr++) {
			if (((String) comboBoxList.get(columnNr).getSelectedItem()).equals("")) {
				columnMappingComplete = false;
				break;
			}
		}
		if (
				(!fileName.equals("")) &&
				(!fieldDelimiter.equals("")) &&
				(!textQualifier.equals("")) &&
				columnMappingComplete
			) {
			inputFile.setFileName(fileName);
			inputFile.setFieldDelimiter(fieldDelimiter);
			inputFile.setTextQualifier(textQualifier);
			Map<String, String> columnMapping = new HashMap<String, String>();
			int columnNr = 0;
			for (String column : inputFile.getColumns()) {
				String mapping = (String) comboBoxList.get(columnNr).getSelectedItem();
				if (mapping.equals("")) {
					mapping = null;
				}
				columnMapping.put(column, mapping);
				columnNr++;
			}
			inputFile.setColumnMapping(columnMapping);
			saveOK = true;
			/*
			System.out.println("File name      : " + fileName);
			System.out.println("Field delimiter: " + fieldDelimiter);
			System.out.println("Text Qualifier : " + textQualifier);
			columnNr = 0;
			for (String column : inputFile.getColumns()) {
				if (columnNr == 0) System.out.print("Column mapping : ");
				else               System.out.print("                 ");
				System.out.println(column + " -> " + (columnMapping.get(column) == null ? "" : columnMapping.get(column)));
				columnNr++;
			}
			*/
		}
		else {
			JOptionPane.showMessageDialog(null, "Settings are not complete!", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return saveOK;
	}
	
	
	private void updateColumns(String fileName, String fieldDelimiter, List<JComboBox<String>> comboBoxList) {
		if (!fileName.equals("")) {
			String fileHeader = getFileHeader(fileName);
			String[] columns = fileHeader.split(translateDelimiter(fieldDelimiter));
			int columnNr = 0;
			for (JComboBox<String> comboBox : comboBoxList) {
				String columnName = fileDefinition.getColumns()[columnNr].getColumnName();
				String mappedColumn = columnMapping.get(columnName);
				int itemNrToSelect = 0;
				
				comboBox.removeAllItems();
				comboBox.addItem("");
				int itemNr = 1;
				for (String column : columns) {
					comboBox.addItem(column);
					if ((mappedColumn != null) && mappedColumn.equals(column)) {
						itemNrToSelect = itemNr;
					}
					itemNr++;
				}
				comboBox.setSelectedIndex(itemNrToSelect);
				if (itemNrToSelect == 0) {
					columnMapping.remove(columnName);
				}
				columnNr++;
			}
		}
	}
	
	
	private String translateDelimiter(String delimiter) {
		return delimiter.equals("Tab") ? "\t" : (delimiter.equals("Semicolon") ? ";" : (delimiter.equals("Comma") ? "," : (delimiter.equals("Space") ? " " : delimiter)));
	}
	
	
	private String getFileHeader(String fileName) {
		String header = "";
		try {
			FileInputStream inputstream = new FileInputStream(fileName);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputstream, CHAR_SET));
			header = bufferedReader.readLine();
			bufferedReader.close();
			inputstream.close();
		} catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (UnsupportedEncodingException e) {
			JOptionPane.showMessageDialog(null, "Unsupported encoding!", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Cannot read  file!", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return header;
	}

}
