package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.ohdsi.drugmapping.cdm.CDMConcept;
import org.ohdsi.drugmapping.files.FileColumnDefinition;
import org.ohdsi.drugmapping.files.FileDefinition;
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.genericmapping.GenericMappingInputFiles;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.source.SourceDrug;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingFileUtilities;
import org.ohdsi.utilities.files.Row;

public class DrugMappingLogTab extends MainFrameTab {
	private static final long serialVersionUID = -2535974179089673874L;
	
	private MainFrame mainFrame;
	
	private Source source;
	
	private JPanel drugsPanel;
	private JTable drugsTable;
	private JPanel drugResultsPanel;
	
	Map<String, InputFile> inputFilesMap;
	Long minimumUseCount;
	

	private List<Object[]> drugsList = null;
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> drugMappingLog = null;
	private Map<String, Double> usedStrengthDeviationPercentageMap = null;
	
	private LogDrugListTableModel tableModel;
	private TableRowSorter<? extends TableModel> rowSorter;
	
	private SourceDrug lastSelectedSourceDrug = null;

	public DrugMappingLogTab(MainFrame mainFrame) {
		super();
		this.mainFrame = mainFrame;
		
		setLayout(new BorderLayout());
		
		JPanel searchPanel = new JPanel();
		searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		JLabel searchLabel = new JLabel("Search term: ");
		JTextField searchField = new JTextField(20);
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				filter();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				filter();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				filter();
			}
			
			private void filter() {
                String text = searchField.getText();
                if (text.length() == 0) {
                    rowSorter.setRowFilter(null);
                }
                else {
                	//TODO escape special characters
                    rowSorter.setRowFilter(RowFilter.regexFilter(text));
                }
			}
		});
		searchPanel.add(searchLabel);
		searchPanel.add(searchField);
		
		JPanel drugsResultsPanel = new JPanel(new BorderLayout());
		
		drugsPanel = new JPanel(new BorderLayout());
		drugsPanel.setBorder(BorderFactory.createTitledBorder("Drugs"));
		drugsPanel.setMinimumSize(new Dimension(100, 205));
		drugsPanel.setMaximumSize(new Dimension(100000, 205));
		drugsPanel.setPreferredSize(new Dimension(100, 205));
		
		drugResultsPanel = new JPanel(new BorderLayout());
		drugResultsPanel.setBorder(BorderFactory.createTitledBorder("Log"));		
				
		this.add(searchPanel, BorderLayout.NORTH);
		this.add(drugsResultsPanel, BorderLayout.CENTER);
		drugsResultsPanel.add(drugsPanel, BorderLayout.NORTH);
		drugsResultsPanel.add(drugResultsPanel, BorderLayout.CENTER);
	}
	
	
	public void listDrugs(Source source, Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> drugMappingLog) {
		this.source = source;
		this.drugMappingLog = drugMappingLog;
		listDrugs();
	}
	
	
	private void listDrugs() {
		drugsList = new ArrayList<Object[]>();
		for (SourceDrug sourceDrug : source.getSourceDrugs()) {
			String mappingStatus = null;
			Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingLog = drugMappingLog.get(sourceDrug);
			if (sourceDrugMappingLog != null) { 
				for (Integer mappingType : sourceDrugMappingLog.keySet()) {
					List<Map<Integer, List<CDMConcept>>> sourceDrugMappingTypeLog = sourceDrugMappingLog.get(mappingType);
					if (sourceDrugMappingTypeLog != null) {
						for (Map<Integer, List<CDMConcept>> sourceDrugComponentMappingLog : sourceDrugMappingTypeLog) {
							if (sourceDrugComponentMappingLog.keySet().contains(GenericMapping.getMappingResultValue("Overruled mapping"))) {
								mappingStatus = "ManualMapping";
								break;
							}
							else if (sourceDrugComponentMappingLog.keySet().contains(GenericMapping.getMappingResultValue("Mapped"))) {
								mappingStatus = "Mapped";
								break;
							}
						}
					}
					if (mappingStatus != null) {
						break;
					}
				}
			}
			if (mappingStatus == null) {
				mappingStatus = "Unmapped";
			}
			String drugCode = sourceDrug.getCode() == null ? "" : sourceDrug.getCode();
			String drugName = sourceDrug.getName() == null ? "" : sourceDrug.getName();
			String atcCodes = sourceDrug.getATCCodesString();
			String formulations = sourceDrug.getFormulationsString();
			Long useCount = sourceDrug.getCount() == null ? 0L : sourceDrug.getCount();

			Object[] drug = new Object[] {
					mappingStatus,
					drugCode,
					drugName,
					atcCodes,
					formulations,
					useCount,
				    sourceDrug
			};
			drugsList.add(drug);
		}
		
		showDrugList();
	}
	
	
	private void showDrugList() {
		drugsPanel.removeAll();
		
		tableModel = new LogDrugListTableModel();
		drugsTable = new JTable(tableModel);
		drugsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
		rightAlignmentRenderer.setHorizontalAlignment(JLabel.RIGHT);
		
		drugsTable.setAutoCreateRowSorter(true);
		rowSorter = (TableRowSorter<? extends TableModel>) drugsTable.getRowSorter();

		// Set selection to first row
		ListSelectionModel selectionModel = drugsTable.getSelectionModel();
		selectionModel.setSelectionInterval(0, 0);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int index = drugsTable.getSelectedRow();
					if (index != -1) {
						int realIndex = drugsTable.convertRowIndexToModel(index);
						selectDrug((SourceDrug) drugsList.get(realIndex)[tableModel.getColumnCount()]);
					}
				}
			}
		});
		// Status
		drugsTable.getColumnModel().getColumn(0).setMaxWidth(100);
		drugsTable.getColumnModel().getColumn(0).setMaxWidth(100);
		
		// Code
		drugsTable.getColumnModel().getColumn(1).setMaxWidth(120);
		
		// ATCCode
		drugsTable.getColumnModel().getColumn(3).setMaxWidth(170);
		
		// Formulation
		drugsTable.getColumnModel().getColumn(4).setMaxWidth(300);
		drugsTable.getColumnModel().getColumn(4).setPreferredWidth(200);
		
		// Use Count
		drugsTable.getColumnModel().getColumn(5).setMaxWidth(80);
		drugsTable.getColumnModel().getColumn(5).setCellRenderer(rightAlignmentRenderer);
		
		JScrollPane drugsScrollPane = new JScrollPane(drugsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		drugsPanel.add(drugsScrollPane, BorderLayout.CENTER);
		
		drugsPanel.invalidate();
	}
	
	
	private void selectDrug(SourceDrug sourceDrug) {
		if (sourceDrug != lastSelectedSourceDrug) {
			System.out.println(sourceDrug);
		}
		lastSelectedSourceDrug = sourceDrug;
	}
	
	
	private class LogDrugListTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 8941622653121454830L;
				
		private String[] columnNames = new String[] {
				"Status",
				"Code",
				"Name",
				"ATCCode",
				"Formulation",
				"Use Count"
			};

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}
		
		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public int getRowCount() {
			return drugsList.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return drugsList.get(rowIndex)[columnIndex];
		}
	     
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	        if (drugsList.isEmpty()) {
	            return Object.class;
	        }
	        return getValueAt(0, columnIndex).getClass();
	    }
	}
	
	
	public void loadDrugMappingResults() {
		String mappingLogFileName = "DrugMapping Mapping Log.csv";
		String mappingLogFilePath = DrugMappingFileUtilities.selectCSVFile(mainFrame.getFrame(), mappingLogFileName, "Drug Mapping Log File");
		if (mappingLogFilePath != null) {
			GenericMappingInputFiles mappingInputFiles = new GenericMappingInputFiles();
			String basePath = mappingLogFilePath.substring(0, mappingLogFilePath.length() - mappingLogFileName.length());
			getInfoFromLogFile(basePath + "DrugMapping Log.txt");
			InputFile genericDrugsFile = inputFilesMap.get("Generic Drugs File");
			FileDefinition mappingLogFileDefinition = mappingInputFiles.getInputFileDefinition("DrugMapping Mapping Log File");
			InputFile mappingLogFile = null;
			if (mappingLogFileDefinition != null) {
				mappingLogFile = new InputFile(mappingLogFileDefinition);
				mappingLogFile.setFileName(basePath + "DrugMapping Mapping Log.csv");
				for (FileColumnDefinition columnDefinition : mappingLogFileDefinition.getColumns()) {
					String columnName = columnDefinition.getColumnName();
					mappingLogFile.addColumnMapping(columnName, columnName);
				}
			}
			if ((genericDrugsFile != null) && (mappingLogFile != null)) {
				startLoadingMappingResults(mainFrame, genericDrugsFile, mappingLogFile);
			}
		}
	}
	
	
	private void getInfoFromLogFile(String logFileName) {
		inputFilesMap = new HashMap<String, InputFile>();
		File logFile = new File(logFileName);
		if (logFile.exists() && logFile.canRead()) {
			try {
				BufferedReader logFileReader = new BufferedReader(new FileReader(logFile));
				GenericMappingInputFiles mappingInputFiles = new GenericMappingInputFiles();
				
				String inputFileTag = "Input File: ";
				String fileNameTag = "  Filename: ";
				String fieldDelimiterTag = "  Field delimiter: ";
				String textQualifierTag = "  Text qualifier: ";
				String textFieldsTag = "  Fields:";
				String generalSettingsTag = "General Settings:";
				String settingMinimumUseCountTag = "  Minimum use count: ";
				
				boolean fields = false;
				String line = logFileReader.readLine();
				InputFile inputFile = null;
				boolean generalSettings = false;
				while ((line != null) && (line.equals("") || (!"1234567890".contains(line.substring(0, 1))))) {
					if (line.startsWith(inputFileTag)) {
						fields = false;
						generalSettings = false;
						String inputFileName = line.substring(inputFileTag.length()); 
						FileDefinition inputFileDefinition = null;
						for (FileDefinition fileDefinition : mappingInputFiles.getInputFiles()) {
							if (fileDefinition.getFileName().equals(inputFileName)) {
								inputFileDefinition = fileDefinition;
								break;
							}
						}
						if (inputFileDefinition != null) {
							inputFile = new InputFile(inputFileDefinition);
							inputFilesMap.put(inputFile.getLabelText(), inputFile);
						}
						else {
							inputFile = null;
						}
					}
					else if (line.startsWith(generalSettingsTag)) {
						generalSettings = true;
						inputFile = null;
					}
					if (inputFile != null) {
						if (line.startsWith(fileNameTag)) {
							inputFile.setFileName(line.substring(fileNameTag.length()));
						}
						else if (line.startsWith(fieldDelimiterTag)) {
							inputFile.setFieldDelimiter(line.substring(fieldDelimiterTag.length() + 1, line.length() - 1));
						}
						else if (line.startsWith(textQualifierTag)) {
							inputFile.setTextQualifier(line.substring(textQualifierTag.length() + 1, line.length() - 1));
						}
						else if (line.startsWith(textFieldsTag)) {
							fields = true;
						}
						else if (fields && line.startsWith("    ") && line.contains(" -> ")) {
							String[] lineSplit = line.split(" -> ");
							String genericName = lineSplit[0].trim();
							String sourceName = lineSplit[1].trim();
							inputFile.addColumnMapping(genericName, sourceName);
						} 
						else {
							fields = false;
						}
					} 
					else if (generalSettings) {
						fields = false;
						if (line.startsWith(settingMinimumUseCountTag)) {
							minimumUseCount = Long.parseLong(line.substring(settingMinimumUseCountTag.length()).trim());
						}
					}
					line = logFileReader.readLine();
				}
				logFileReader.close();
			}
			catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(null, "Error opening log file '" + logFileName + "'!", "Error", JOptionPane.ERROR_MESSAGE);
				inputFilesMap = null;
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Error reading log file '" + logFileName + "'!", "Error", JOptionPane.ERROR_MESSAGE);
				inputFilesMap = null;
			}
			
		}
	}
	
	
	private void startLoadingMappingResults(MainFrame mainFrame, InputFile genericDrugsFile, InputFile mappingLogFile) {
		if (genericDrugsFile != null) {
			LoadMappingResultsThread mappingResultsThread = new LoadMappingResultsThread(mainFrame, genericDrugsFile, mappingLogFile);
			mappingResultsThread.start();
		}
	}
	
	
	private class LoadMappingResultsThread extends Thread {
		MainFrame mainFrame;
		InputFile genericDrugsFile;
		InputFile drugMappingLogFile;
		
		public LoadMappingResultsThread(MainFrame mainFrame, InputFile genericDrugsFile, InputFile drugMappingLogFile) {
			super();
			this.mainFrame = mainFrame;
			this.genericDrugsFile = genericDrugsFile;
			this.drugMappingLogFile = drugMappingLogFile;
		}
		
		public void run() {
			source = new Source(genericDrugsFile, minimumUseCount);
			loadDrugMappingLog(drugMappingLogFile);
			listDrugs();
			mainFrame.selectTab(this.getName());
		}
		
	}
	
	
	private void loadDrugMappingLog(InputFile drugMappingLogFile) {
		usedStrengthDeviationPercentageMap = new HashMap<String, Double>();
		if (drugMappingLogFile.openFile()) {
			drugMappingLog = new HashMap<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>>();
			
			while (drugMappingLogFile.hasNext()) {
				Row row = drugMappingLogFile.next();
				
				String mappingStatus            = drugMappingLogFile.get(row, "MappingStatus", true);
				String sourceCode               = drugMappingLogFile.get(row, "SourceCode", true);
				String sourceName               = drugMappingLogFile.get(row, "SourceName", true);
				String sourceATCCode            = drugMappingLogFile.get(row, "SourceATCCode", true);
				String sourceFormulation        = drugMappingLogFile.get(row, "SourceFormulation", true);
				String sourceCount              = drugMappingLogFile.get(row, "SourceCount", true);
				String ingredientCode           = drugMappingLogFile.get(row, "IngredientCode", true);
				String ingredientName           = drugMappingLogFile.get(row, "IngredientName", true);
				String ingredientNameEnglish    = drugMappingLogFile.get(row, "IngredientNameEnglish", true);
				String casNumber                = drugMappingLogFile.get(row, "CASNumber", true);
				String sourceIngredientAmount   = drugMappingLogFile.get(row, "SourceIngredientAmount", true);
				String sourceIngredentUnit      = drugMappingLogFile.get(row, "SourceIngredentUnit", true);
				String strengthMarginPercentage = drugMappingLogFile.get(row, "StrengthMarginPercentage", true);
				String mappingTypeDescription   = drugMappingLogFile.get(row, "MappingType", true);
				String mappingResultDescription = drugMappingLogFile.get(row, "MappingResult", true);
				
				List<String> concepts = new ArrayList<String>();
				for (int cellNr = 15; cellNr < row.getCells().size(); cellNr++) {
					String concept = row.getCells().get(cellNr);
					if (!concept.equals("")) {
						concepts.add(concept);
					}
					else {
						break;
					}
				}
				
				Integer mappingType = GenericMapping.getMappingTypeValue(mappingTypeDescription);
				Integer mappingResult = GenericMapping.getMappingResultValue(mappingResultDescription);
				SourceDrug sourceDrug = source.getSourceDrug(sourceCode);
				SourceIngredient sourceIngredient = ingredientCode.equals("*") ? null : Source.getIngredient(ingredientCode);
				if ((ingredientNameEnglish != null) && (!ingredientNameEnglish.equals("")) && (!ingredientNameEnglish.equals("*"))) {
					sourceIngredient.setIngredientNameEnglish(ingredientNameEnglish);
				}
				
				if ((strengthMarginPercentage != null) && (!strengthMarginPercentage.equals("")) && (!strengthMarginPercentage.equals("*"))) {
					String key = "Drug " + sourceCode;
					if (mappingType == GenericMapping.INGREDIENT_MAPPING) {
						key = "Ingredient " + sourceDrug.getCode() + "," + ingredientCode;
					}
					usedStrengthDeviationPercentageMap.put(key, Double.parseDouble(strengthMarginPercentage));
				}
				
				Map<Integer, List<Map<Integer, List<CDMConcept>>>> sourceDrugMappingLog = drugMappingLog.get(sourceDrug);
				if (sourceDrugMappingLog == null) {
					sourceDrugMappingLog = new HashMap<Integer, List<Map<Integer, List<CDMConcept>>>>();
					drugMappingLog.put(sourceDrug, sourceDrugMappingLog);
				}
				
				List<Map<Integer, List<CDMConcept>>> sourceDrugMappingTypeLog = sourceDrugMappingLog.get(mappingType);
				if (sourceDrugMappingTypeLog == null) {
					sourceDrugMappingTypeLog = new ArrayList<Map<Integer, List<CDMConcept>>>();
					sourceDrugMappingLog.put(mappingType, sourceDrugMappingTypeLog);
				}
				//TODO
				//Map<Integer, List<CDMConcept>> mappingResultListMap = new HashMap<Integer, List<CDMConcept>>();
				
				//sourceDrugMappingTypeLog.ad
			}
		}
	}
}
