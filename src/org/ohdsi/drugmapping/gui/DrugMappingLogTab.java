package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
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
import org.ohdsi.drugmapping.genericmapping.GenericMapping;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.source.SourceDrug;

public class DrugMappingLogTab extends MainFrameTab {
	private static final long serialVersionUID = -2535974179089673874L;
	
	MainFrame mainFrame;
	
	private Source source;
	
	private JPanel drugsPanel;
	private JTable drugsTable;
	private JPanel drugMappingLogPanel;
	private JPanel drugMappingResultPanel;
	
	private List<Object[]> drugsList = null;
	private Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> drugMappingLog = null;
	private Map<String, Double> usedStrengthDeviationPercentageMap = null;
	private List<List<String>> sourceDrugMappingResultlog = null;
	
	private LogDrugListTableModel drugListTableModel;
	private TableRowSorter<? extends TableModel> rowSorter;
	private SourceDrug lastSelectedSourceDrug = null;

	private JTable logTable;
	private LogTableModel logTableModel; 
	private Integer lastSelectedLogRecord = null;

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
		
		JPanel drugMappingLogResultsPanel = new JPanel(new BorderLayout());
		
		drugMappingLogPanel = new JPanel(new BorderLayout());
		drugMappingLogPanel.setBorder(BorderFactory.createTitledBorder("Drug Mapping Log"));
		drugMappingLogPanel.setMinimumSize(new Dimension(100, 205));
		drugMappingLogPanel.setMaximumSize(new Dimension(100000, 205));
		drugMappingLogPanel.setPreferredSize(new Dimension(100, 250));
		
		drugMappingResultPanel = new JPanel(new BorderLayout());
		drugMappingResultPanel.setMinimumSize(new Dimension(100, 100));
		drugMappingResultPanel.setMaximumSize(new Dimension(100000, 100000));
		drugMappingResultPanel.setPreferredSize(new Dimension(100, 200));
		drugMappingResultPanel.setBorder(BorderFactory.createTitledBorder("Drug Mapping Results"));
						
		this.add(searchPanel, BorderLayout.NORTH);
		this.add(drugsResultsPanel, BorderLayout.CENTER);
		drugsResultsPanel.add(drugsPanel, BorderLayout.NORTH);
		drugsResultsPanel.add(drugMappingLogResultsPanel, BorderLayout.CENTER);
		drugMappingLogResultsPanel.add(drugMappingLogPanel, BorderLayout.NORTH);
		drugMappingLogResultsPanel.add(drugMappingResultPanel, BorderLayout.CENTER);
	}
	
	
	public void showDrugMappingLog(Source source, Map<SourceDrug, Map<Integer, List<Map<Integer, List<CDMConcept>>>>> drugMappingLog, Map<String, Double> usedStrengthDeviationPercentageMap) {
		this.source = source;
		this.drugMappingLog = drugMappingLog;
		this.usedStrengthDeviationPercentageMap = usedStrengthDeviationPercentageMap;
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
		
		drugListTableModel = new LogDrugListTableModel();
		drugsTable = new JTable(drugListTableModel);
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
						selectDrug((SourceDrug) drugsList.get(realIndex)[drugListTableModel.getColumnCount()]);
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

		selectDrug((SourceDrug) drugsList.get(0)[drugListTableModel.getColumnCount()]);
	}
	
	
	private void selectDrug(SourceDrug sourceDrug) {
		if (sourceDrug != lastSelectedSourceDrug) {
			//System.out.println(sourceDrug);
			getSourceDrugMappingResultLog(sourceDrug);
			lastSelectedLogRecord = null;
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
	
	
	private void getSourceDrugMappingResultLog(SourceDrug sourceDrug) {
		sourceDrugMappingResultlog = GenericMapping.getSourceDrugMappingResults(sourceDrug, drugMappingLog, usedStrengthDeviationPercentageMap, null);
		for (Integer recordNr = 0; recordNr < sourceDrugMappingResultlog.size(); recordNr++) {
			sourceDrugMappingResultlog.get(recordNr).add(recordNr.toString());
		}
		showSourceDrugMappingLog();
	}
	
	
	private void showSourceDrugMappingLog() {
		drugMappingLogPanel.removeAll();
		
		logTableModel = new LogTableModel();
		logTable = new JTable(logTableModel);
		logTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
		rightAlignmentRenderer.setHorizontalAlignment(JLabel.RIGHT);
		
		logTable.setAutoCreateRowSorter(false);

		// Set selection to first row
		ListSelectionModel selectionModel = logTable.getSelectionModel();
		selectionModel.setSelectionInterval(0, 0);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int index = logTable.getSelectedRow();
					if (index != -1) {
						int realIndex = logTable.convertRowIndexToModel(index);
						showDrugMappingResults(Integer.parseInt(sourceDrugMappingResultlog.get(realIndex).get(sourceDrugMappingResultlog.get(realIndex).size() - 1)));
					}
				}
			}
		});
		
		// Code
		logTable.getColumnModel().getColumn(0).setMaxWidth(80);
		
		// Name
		logTable.getColumnModel().getColumn(1).setMaxWidth(300);
		
		// ATCCode
		logTable.getColumnModel().getColumn(2).setMaxWidth(170);
		
		// Formulation
		logTable.getColumnModel().getColumn(3).setMaxWidth(300);
		logTable.getColumnModel().getColumn(3).setPreferredWidth(150);
		
		// Use Count
		logTable.getColumnModel().getColumn(4).setMaxWidth(80);
		logTable.getColumnModel().getColumn(4).setCellRenderer(rightAlignmentRenderer);
		
		// Ingredient Code
		logTable.getColumnModel().getColumn(5).setMaxWidth(80);
		
		// Ingredient Name
		logTable.getColumnModel().getColumn(6).setMaxWidth(200);
		
		// Ingredient Name English
		logTable.getColumnModel().getColumn(7).setMaxWidth(200);
		
		// CAS Number
		logTable.getColumnModel().getColumn(8).setMaxWidth(80);
		
		// Amount
		logTable.getColumnModel().getColumn(9).setMaxWidth(50);
		
		// Unit
		logTable.getColumnModel().getColumn(10).setMaxWidth(80);
		
		// Strength Margin%
		logTable.getColumnModel().getColumn(11).setMaxWidth(100);
		
		// Mapping Type
		logTable.getColumnModel().getColumn(12).setMaxWidth(200);
		
		// Mapping Result
		logTable.getColumnModel().getColumn(13).setMaxWidth(300);
		
		JScrollPane logScrollPane = new JScrollPane(logTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		drugMappingLogPanel.add(logScrollPane, BorderLayout.CENTER);
		
		mainFrame.getFrame().repaint();
		
		showDrugMappingResults(0);
	}
	
	
	private void showDrugMappingResults(Integer logNr) {
		if (logNr != lastSelectedLogRecord) {
			drugMappingResultPanel.removeAll();
			
			JTextArea resultsTextArea = new JTextArea();
			resultsTextArea.setEditable(false);
			
			String results = "";
			for (int columnNr = 14; columnNr < (sourceDrugMappingResultlog.get(logNr).size() - 1); columnNr++) {
				String result = sourceDrugMappingResultlog.get(logNr).get(columnNr);
				if (!result.equals("")) {
					results += (columnNr == 14 ? "" : "\r\n") + result;
				}
			}
			
			resultsTextArea.setText(results);

			drugMappingResultPanel.setBorder(BorderFactory.createTitledBorder(sourceDrugMappingResultlog.get(logNr).get(13)));

			JScrollPane resultsScrollPane = new JScrollPane(resultsTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			drugMappingResultPanel.add(resultsScrollPane, BorderLayout.CENTER);
			
			mainFrame.getFrame().repaint();
		}
		lastSelectedLogRecord = logNr;
	}
	
	
	private class LogTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 5448711693087165903L;
		
		private String[] columnNames = new String[] {
				"Code",
				"Name",
				"ATCCode",
				"Formulation",
				"Use Count",
				"Ingredient Code",
				"Ingredient Name",
				"Ingredient Name English",
				"CAS Number",
				"Amount",
				"Unit",
				"Strength Margin%",
				"Mapping Type",
				"Mapping Result"
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
			return sourceDrugMappingResultlog.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return sourceDrugMappingResultlog.get(rowIndex).get(columnIndex);
		}
	     
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	        if (sourceDrugMappingResultlog.isEmpty()) {
	            return Object.class;
	        }
	        return getValueAt(0, columnIndex).getClass();
	    }
		
	}
}
