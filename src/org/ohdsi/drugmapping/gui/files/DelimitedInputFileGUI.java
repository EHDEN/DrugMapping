package org.ohdsi.drugmapping.gui.files;

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
import javax.swing.filechooser.FileFilter;

import org.ohdsi.drugmapping.files.DelimitedFileWithHeader;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.gui.JTextFieldLimit;
import org.ohdsi.drugmapping.gui.MainFrame;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class DelimitedInputFileGUI extends InputFileGUI {
	private static final long serialVersionUID = -8908651240263793215L;
	
	private final String[] FIELD_DELIMITERS = new String[]{ "Tab", "Semicolon", "Comma", "Space", "Other" };
	private final String[] TEXT_QUALIFIERS  = new String[]{ "\"", "'", "None" };
	
	private List<JComboBox<String>> comboBoxList;

	private String fieldDelimiter = "Comma";
	private String textQualifier = "\"";
	private Map<String, String> columnMapping = new HashMap<String, String>();
	
	private Iterator<DelimitedFileRow> fileIterator;
	
	
	public static char fieldDelimiter(String delimiterName) {
		if (delimiterName.equals("Tab")) return '\t';
		if (delimiterName.equals("Semicolon")) return ';';
		if (delimiterName.equals("Comma")) return ',';
		if (delimiterName.equals("Space")) return ' ';
		return delimiterName.charAt(0);
	}
	
	
	public static char textQualifier(String textQualifierName) {
		if (textQualifierName.equals("None")) return (char) 0;
		return textQualifierName.charAt(0);
	}
	
	
	public DelimitedInputFileGUI(Component parent, FileDefinition fileDefinition) {
		super(parent, fileDefinition);
		
		for (FileColumnDefinition column : fileDefinition.getColumns()) {
			columnMapping.put(column.getColumnName(), null);
		}
		
		if (fileDefinition.getDefaultFieldDelimiter() != null) {
			setFieldDelimiter(fileDefinition.getDefaultFieldDelimiter());
		}
		
		if (fileDefinition.getDefaultTextQualifier() != null) {
			setTextQualifier(fileDefinition.getDefaultTextQualifier());
		}
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
		for (FileColumnDefinition column : getFileDefinition().getColumns()) {
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
	
	
	public void addColumnMapping(String column, String inputColumn) {
		columnMapping.put(column, inputColumn);
	}
	
	
	public boolean fileExists() {
		boolean exists = false;
		if (getFileName() != null) {
			File inputFile = new File(getFileName());
			if (inputFile.exists()) {
				exists = true;
			}
		}
		return exists;
	}
	
	
	public boolean hasNext() {
		return fileIterator.hasNext();
	}
	
	
	public DelimitedFileRow next() {
		return fileIterator.next();
	}
	
	
	public boolean hasField(String fieldName) {
		return (columnMapping.get(fieldName) != null);
	}
	
	
	public String get(DelimitedFileRow row, String fieldName, boolean required) {
		String value = null;
		String mappedFieldName = columnMapping.get(fieldName);	
		if (required && (mappedFieldName == null)) {
			throw new RuntimeException("Field \"" + fieldName + "\" not found");
		}
		else {
			value = row.get(mappedFieldName, required);
		}
		return DrugMappingStringUtilities.convertToANSI(value);
	}
	
	
	void defineFile(InputFileGUI inputFile) {
		DelimitedInputFileGUI delimitedInputFile = (DelimitedInputFileGUI) inputFile;
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
		fileSectionPanel.setBorder(BorderFactory.createTitledBorder(getLabelText()));
		JPanel fileSectionSubPanel = new JPanel(new BorderLayout());
		
		JPanel fileDescriptionPanel = new JPanel(new BorderLayout());
		fileDescriptionPanel.setBorder(BorderFactory.createEmptyBorder());
		JTextArea fileDescriptionField = new JTextArea();
		fileDescriptionField.setEditable(false);
		fileDescriptionField.setBackground(fileDescriptionPanel.getBackground());
		String description = "";
		for (String line : getFileDefinition().getDescription()) {
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
		
		JTextField fileField = new JTextField(delimitedInputFile.getFileName());
		fileField.setEditable(false);
		
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
			radioButton.setSelected(delimiter.equals(delimitedInputFile.getFieldDelimiter()));
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
			radioButton.setSelected(qualifier.equals(delimitedInputFile.getTextQualifier()));
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
		for (FileColumnDefinition column : getFileDefinition().getColumns()) {
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
				if (saveFileSettings(delimitedInputFile, fileField.getText(), fieldDelimiter, textQualifier, comboBoxList)) {
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
		
		updateColumns(getFileName(), fieldDelimiter, comboBoxList);
		
		fileDialog.setVisible(true);
	}
	
	
	private String geFieldDelimiterFromInput(ButtonGroup fieldDelimiterButtons, String otherDelimiter) {
		String fieldDelimiter = fieldDelimiterButtons.getSelection().getActionCommand();
		if (fieldDelimiter.equals("Other")) {
			fieldDelimiter = otherDelimiter;
		}
		return fieldDelimiter;
	}
	
	
	private boolean saveFileSettings(DelimitedInputFileGUI inputFile, String fileName, String fieldDelimiter, String textQualifier, List<JComboBox<String>> comboBoxList) {
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
		if ((fileName != null) && (!fileName.equals(""))) {
			String fileHeader = getFileHeader(fileName);
			String[] columns = fileHeader.split(translateDelimiter(fieldDelimiter));
			int columnNr = 0;
			for (JComboBox<String> comboBox : comboBoxList) {
				String columnName = getFileDefinition().getColumns()[columnNr].getColumnName();
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
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputstream));
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
	

	@Override
	public List<String> getSettings() {
		List<String> settings = new ArrayList<String>();

		settings.add("#");
		settings.add("# " + getLabelText());
		settings.add("#");
		settings.add("");
		settings.add(getLabelText() + ".filename=" + getFileName());
		settings.add(getLabelText() + ".selected=" + (isSelected() ? "Yes" : "No"));
		if (getFileDefinition().getFileType() == FileDefinition.DELIMITED_FILE) {
			settings.add(getLabelText() + ".fieldDelimiter=" + getFieldDelimiter());
			settings.add(getLabelText() + ".textQualifier=" + getTextQualifier());
			for (String column : getColumns()) {
				settings.add(getLabelText() + ".column." + column + "=" + (getColumnMapping().get(column) == null ? "" : getColumnMapping().get(column)));
			}
		}
		
		return settings;
	}
	

	@Override
	public void putSettings(List<String> settings) {
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingPath = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1).trim();
				String[] settingPathSplit = settingPath.split("\\.");
				if ((settingPathSplit.length > 0) && (settingPathSplit[0].equals(getLabelText()))) {
					if ((settingPathSplit.length == 3) && (settingPathSplit[1].equals("column"))) { // Column mapping
						if (getColumns().contains(settingPathSplit[2])) {
							getColumnMapping().put(settingPathSplit[2], value);
						}
					}
					else if (settingPathSplit.length == 2) {
						if (settingPathSplit[1].equals("filename")) setFileName(value);
						else if (settingPathSplit[1].equals("fieldDelimiter")) setFieldDelimiter(value);
						else if (settingPathSplit[1].equals("selected")) setSelected(value.toUpperCase().equals("YES"));
						else if (settingPathSplit[1].equals("textQualifier")) setTextQualifier(value);
						else {
							// Unknown setting
						}
					}
				}
			}
		}
	}


	@Override
	List<FileFilter> getFileFilters() {
		List<FileFilter> fileFilters = new ArrayList<FileFilter>();
		fileFilters.add(new FileFilter() {

	        @Override
	        public boolean accept(File f) {
	            return f.getName().endsWith(".csv");
	        }

	        @Override
	        public String getDescription() {
	            return "Comma Separated File";
	        }

	    });
		fileFilters.add(new FileFilter() {

	        @Override
	        public boolean accept(File f) {
	            return f.getName().endsWith(".tsv");
	        }

	        @Override
	        public String getDescription() {
	            return "Tab Separated File";
	        }

	    });
		return fileFilters;
	}
	
	
	@Override
	public boolean openFileForReading() {
		return openFileForReading(false);
	}
	
	
	@Override
	public boolean openFileForReading(boolean suppressError) {
		boolean result = false;
		
		if (getFileName() != null) {
			File inputFile = new File(getFileName());
			if (inputFile.exists() && inputFile.canRead()) {
				char delimiter = fieldDelimiter(fieldDelimiter);
				char textDelimiter = textQualifier(textQualifier);
				
				DelimitedFileWithHeader readFile = new DelimitedFileWithHeader(getFileName(), delimiter, textDelimiter);
				if (readFile.openForReading()) {
					result = true;
					fileIterator = readFile.iterator();
				}
				else {
					if (!suppressError) {
						JOptionPane.showMessageDialog(null, "Couldn't open file for reading!", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
			else {
				if (!suppressError) {
					JOptionPane.showMessageDialog(null, "Cannot read file '" + getFileName() + "'!", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		return result;
	}


	@Override
	public void logFileSettings() {
		if (getFileName() != null) {
			System.out.println("Input File: " + getFileDefinition().getFileName());
			System.out.println("  Filename: " + getFileName());
			System.out.println("  File type: " + FileDefinition.getFileTypeName(getFileType()));
			System.out.println("  Field delimiter: '" + getFieldDelimiter() + "'");
			System.out.println("  Text qualifier: '" + getTextQualifier() + "'");
			System.out.println("  Fields:");
			List<String> columns = getColumns();
			Map<String, String> columnMapping = getColumnMapping();
			for (String column : columns) {
				System.out.println("    " + column + " -> " + columnMapping.get(column));
			}
			System.out.println();
		}
	}

}
