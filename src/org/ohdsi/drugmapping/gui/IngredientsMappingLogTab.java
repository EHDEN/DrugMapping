package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
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

import org.ohdsi.drugmapping.cdm.CDMIngredient;
import org.ohdsi.drugmapping.source.Source;
import org.ohdsi.drugmapping.source.SourceIngredient;
import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;

public class IngredientsMappingLogTab extends MainFrameTab {
	private static final long serialVersionUID = 1736241328985740613L;
	
	private JPanel ingredientsPanel;
	private JTable ingredientsTable;
	private JPanel ingredientMappingLogPanel;
	private JPanel ingredientMappingResultPanel;
	
	private List<Object[]> ingredientsList = null;
	private List<Object[]> ingredientMappingResultList = null;
	
	private LogIngredientListTableModel ingredientListTableModel;
	private TableRowSorter<? extends TableModel> rowSorter;
	
	private JTable resultConceptsTable;
	private ResultConceptsTableModel resultConceptsTableModel;

	
	public IngredientsMappingLogTab(MainFrame mainFrame) {
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
                String text = DrugMappingStringUtilities.safeToUpperCase(searchField.getText());
                if (text.length() == 0) {
                    rowSorter.setRowFilter(null);
                }
                else {
                	//TODO escape special characters
                    rowSorter.setRowFilter(RowFilter.regexFilter(text));
                }
                
                if (rowSorter.getViewRowCount() == 0) {
            		ingredientMappingLogPanel.removeAll();
            		ingredientMappingResultPanel.removeAll();
            		mainFrame.getFrame().repaint();
                }
                

                if (ingredientsTable.getRowCount() > 0) {
            		ListSelectionModel selectionModel = ingredientsTable.getSelectionModel();
            		selectionModel.setSelectionInterval(0, 0);
                }
             }
		});
		searchPanel.add(searchLabel);
		searchPanel.add(searchField);
		
		JPanel drugsResultsPanel = new JPanel(new BorderLayout());
		
		ingredientsPanel = new JPanel(new BorderLayout());
		ingredientsPanel.setBorder(BorderFactory.createTitledBorder("Ingredients"));
		
		JPanel drugMappingLogResultsPanel = new JPanel(new BorderLayout());
		
		ingredientMappingLogPanel = new JPanel(new BorderLayout());
		ingredientMappingLogPanel.setBorder(BorderFactory.createTitledBorder("Ingredient Match String"));
		ingredientMappingLogPanel.setMinimumSize(new Dimension(100, 50));
		ingredientMappingLogPanel.setMaximumSize(new Dimension(100000, 50));
		ingredientMappingLogPanel.setPreferredSize(new Dimension(100, 50));
		
		ingredientMappingResultPanel = new JPanel(new BorderLayout());
		ingredientMappingResultPanel.setMinimumSize(new Dimension(100, 62));
		ingredientMappingResultPanel.setMaximumSize(new Dimension(100, 62));
		ingredientMappingResultPanel.setPreferredSize(new Dimension(100, 62));
		ingredientMappingResultPanel.setBorder(BorderFactory.createTitledBorder("Mapped to CDM Ingredient"));
						
		this.add(searchPanel, BorderLayout.NORTH);
		this.add(drugsResultsPanel, BorderLayout.CENTER);
		drugsResultsPanel.add(ingredientsPanel, BorderLayout.CENTER);
		drugsResultsPanel.add(drugMappingLogResultsPanel, BorderLayout.SOUTH);
		drugMappingLogResultsPanel.add(ingredientMappingLogPanel, BorderLayout.NORTH);
		drugMappingLogResultsPanel.add(ingredientMappingResultPanel, BorderLayout.SOUTH);
	}
	
	
	public void showIngredientMappingLog() {
		listIngredients();
	}
	
	
	private void listIngredients() {
		ingredientsList = new ArrayList<Object[]>();
		for (SourceIngredient sourceIngredient : Source.getAllIngredients()) {
			String mappingStatus = sourceIngredient.getMatchingIngredient() == null ? "Unmapped" : "Mapped";
			String ingredientCode = sourceIngredient.getIngredientCode() == null ? "" : sourceIngredient.getIngredientCode();
			String ingredientName = sourceIngredient.getIngredientName() == null ? "" : sourceIngredient.getIngredientName();
			String ingredientNameEnglish = sourceIngredient.getIngredientNameEnglish() == null ? "" : sourceIngredient.getIngredientNameEnglish();
			String casNumber = sourceIngredient.getCASNumber() == null ? "" : sourceIngredient.getCASNumber();
			Long useCount = sourceIngredient.getCount() == null ? 0L : sourceIngredient.getCount();
			//String atcCode = sourceIngredient.getMatchingIngredient() == null ? null : sourceIngredient.getMatchingIngredient().getATC();
			//if (atcCode == null) {
			//	atcCode = "";
			//}

			Object[] ingredient = new Object[] {
					mappingStatus,
					ingredientCode,
					ingredientName,
					ingredientNameEnglish,
					casNumber,
					useCount,
				    sourceIngredient
			};
			ingredientsList.add(ingredient);
		}
		
		showIngredientList();
	}
	
	
	private void showIngredientList() {
		ingredientsPanel.removeAll();
		ingredientMappingLogPanel.removeAll();
		ingredientMappingResultPanel.removeAll();
		
		ingredientListTableModel = new LogIngredientListTableModel();
		ingredientsTable = new JTable(ingredientListTableModel) {
			private static final long serialVersionUID = 5410359974589005484L;

			//Implement table cell tool tips.           
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                try {
                    tip = getValueAt(rowIndex, colIndex).toString();
                } catch (RuntimeException e1) {
                    //catch null pointer exception if mouse is over an empty line
                }

                return tip;
            }
        };
        ingredientsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
		rightAlignmentRenderer.setHorizontalAlignment(JLabel.RIGHT);
		
		ingredientsTable.setAutoCreateRowSorter(true);
		rowSorter = (TableRowSorter<? extends TableModel>) ingredientsTable.getRowSorter();

		// Set selection to first row
		ListSelectionModel selectionModel = ingredientsTable.getSelectionModel();
		selectionModel.setSelectionInterval(0, 0);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int index = ingredientsTable.getSelectedRow();
					if (index != -1) {
						int realIndex = ingredientsTable.convertRowIndexToModel(index);
						selectIngredient((SourceIngredient) ingredientsList.get(realIndex)[ingredientListTableModel.getColumnCount()]);
					}
				}
			}
		});
		// Status
		ingredientsTable.getColumnModel().getColumn(0).setMaxWidth(100);
		ingredientsTable.getColumnModel().getColumn(0).setMaxWidth(100);
		
		// Ingredient Code
		ingredientsTable.getColumnModel().getColumn(1).setMaxWidth(120);
		
		// CAS Number
		ingredientsTable.getColumnModel().getColumn(4).setMaxWidth(120);
		
		// Use Count
		ingredientsTable.getColumnModel().getColumn(5).setMaxWidth(80);
		ingredientsTable.getColumnModel().getColumn(5).setCellRenderer(rightAlignmentRenderer);
		
		JScrollPane drugsScrollPane = new JScrollPane(ingredientsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		ingredientsPanel.add(drugsScrollPane, BorderLayout.CENTER);

		selectIngredient((SourceIngredient) ingredientsList.get(0)[ingredientListTableModel.getColumnCount()]);
		
		mainFrame.getFrame().repaint();
	}
	
	
	private void selectIngredient(SourceIngredient sourceIngredient) {
		//System.out.println(sourceIngredient);
		showSourceIngredientMappingLog(sourceIngredient);
		getSourceIngredientMappingResult(sourceIngredient);
		mainFrame.getFrame().repaint();
	}
	
	
	private class LogIngredientListTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 731444609946698707L;
		
		private String[] columnNames = new String[] {
				"Status",
				"Code",
				"Name",
				"English Name",
				"CAS Number",
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
			return ingredientsList.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return ingredientsList.get(rowIndex)[columnIndex];
		}
	     
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	        if (ingredientsList.isEmpty()) {
	            return Object.class;
	        }
	        return getValueAt(0, columnIndex).getClass();
	    }
	}
	
	
	private void showSourceIngredientMappingLog(SourceIngredient sourceIngredient) {
		ingredientMappingLogPanel.removeAll();
		
		if ((sourceIngredient != null) && (sourceIngredient.getMatchString() != null)) {
			JTextField matchStringField = new JTextField();
			matchStringField.setEditable(false);
			matchStringField.setText(sourceIngredient.getMatchString());
			ingredientMappingLogPanel.add(matchStringField, BorderLayout.CENTER);	
		}
		ingredientMappingLogPanel.invalidate();
	}
	
	
	private void getSourceIngredientMappingResult(SourceIngredient sourceIngredient) {
		ingredientMappingResultList = new ArrayList<Object[]>();
		if (sourceIngredient.getMatchingIngredient() != null) {
			CDMIngredient cdmIngredient = sourceIngredient.getMatchingIngredient();

			Object[] concept = new Object[] {
					(cdmIngredient.getConceptId() == null ? "" : cdmIngredient.getConceptId()),
					(cdmIngredient.getConceptName() == null ? "" : cdmIngredient.getConceptName()),
					(cdmIngredient.getDomainId() == null ? "" : cdmIngredient.getDomainId()),
					(cdmIngredient.getVocabularyId() == null ? "" : cdmIngredient.getVocabularyId()),
					(cdmIngredient.getConceptClassId() == null ? "" : cdmIngredient.getConceptClassId()),
					(cdmIngredient.getStandardConcept() == null ? "" : cdmIngredient.getStandardConcept()),
					(cdmIngredient.getConceptCode() == null ? "" : cdmIngredient.getConceptCode()),
					(cdmIngredient.getValidStartDate() == null ? "" : cdmIngredient.getValidStartDate()),
					(cdmIngredient.getValidEndDate() == null ? "" : cdmIngredient.getValidEndDate()),
					(cdmIngredient.getInvalidReason() == null ? "" : cdmIngredient.getInvalidReason()),
					(cdmIngredient.getATC() == null ? "" : cdmIngredient.getATC())
			};
			
			ingredientMappingResultList.add(concept);
		}
		showIngredientMappingResult(sourceIngredient);
	}
	
	
	private void showIngredientMappingResult(SourceIngredient sourceIngredient) {
		ingredientMappingResultPanel.removeAll();
		
		resultConceptsTableModel = new ResultConceptsTableModel();
		resultConceptsTable = new JTable(resultConceptsTableModel) {
			private static final long serialVersionUID = 582181068415724155L;

			//Implement table cell tool tips.           
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                try {
                    tip = getValueAt(rowIndex, colIndex).toString();
                } catch (RuntimeException e1) {
                    //catch null pointer exception if mouse is over an empty line
                }

                return tip;
            }
        };
        resultConceptsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		resultConceptsTable.setAutoCreateRowSorter(false);

		// Set selection mode
		//ListSelectionModel selectionModel = resultConceptsTable.getSelectionModel();
		//selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// concept_id
		resultConceptsTable.getColumnModel().getColumn(0).setMinWidth(80);
		resultConceptsTable.getColumnModel().getColumn(0).setMaxWidth(80);
		resultConceptsTable.getColumnModel().getColumn(0).setPreferredWidth(80);
		
		// concept_name
		resultConceptsTable.getColumnModel().getColumn(2).setMinWidth(80);
		resultConceptsTable.getColumnModel().getColumn(1).setPreferredWidth(500);
		
		// domain_id
		resultConceptsTable.getColumnModel().getColumn(2).setMinWidth(80);
		resultConceptsTable.getColumnModel().getColumn(2).setMaxWidth(200);
		resultConceptsTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		
		// vocabulary_id
		resultConceptsTable.getColumnModel().getColumn(3).setMinWidth(100);
		resultConceptsTable.getColumnModel().getColumn(3).setMaxWidth(200);
		resultConceptsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
		
		// concept_class_id
		resultConceptsTable.getColumnModel().getColumn(4).setMinWidth(120);
		resultConceptsTable.getColumnModel().getColumn(4).setMaxWidth(200);
		resultConceptsTable.getColumnModel().getColumn(4).setPreferredWidth(120);
		
		// standard_concept
		resultConceptsTable.getColumnModel().getColumn(5).setMinWidth(10);
		resultConceptsTable.getColumnModel().getColumn(5).setMaxWidth(120);
		resultConceptsTable.getColumnModel().getColumn(5).setPreferredWidth(120);
		
		// concept_code
		resultConceptsTable.getColumnModel().getColumn(6).setMinWidth(100);
		resultConceptsTable.getColumnModel().getColumn(6).setMaxWidth(200);
		resultConceptsTable.getColumnModel().getColumn(6).setPreferredWidth(100);
		
		// valid_start_date
		resultConceptsTable.getColumnModel().getColumn(7).setMinWidth(100);
		resultConceptsTable.getColumnModel().getColumn(7).setMaxWidth(100);
		resultConceptsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
		
		// valid_end_date
		resultConceptsTable.getColumnModel().getColumn(8).setMinWidth(100);
		resultConceptsTable.getColumnModel().getColumn(8).setMaxWidth(100);
		resultConceptsTable.getColumnModel().getColumn(8).setPreferredWidth(100);
		
		// invalid_reason
		resultConceptsTable.getColumnModel().getColumn(9).setMinWidth(100);
		resultConceptsTable.getColumnModel().getColumn(9).setMaxWidth(100);
		resultConceptsTable.getColumnModel().getColumn(9).setPreferredWidth(100);
		
		// atc
		resultConceptsTable.getColumnModel().getColumn(10).setMinWidth(80);
		resultConceptsTable.getColumnModel().getColumn(10).setMaxWidth(80);
		resultConceptsTable.getColumnModel().getColumn(10).setPreferredWidth(80);

		ingredientMappingResultPanel.setBorder(BorderFactory.createTitledBorder(sourceIngredient.getIngredientName()));
		
		JScrollPane resultsScrollPane = new JScrollPane(resultConceptsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		ingredientMappingResultPanel.add(resultsScrollPane, BorderLayout.CENTER);
		ingredientMappingResultPanel.invalidate();
	}
	
	
	private class ResultConceptsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 9209878821161770053L;
		
		private String[] columnNames = new String[] {
				"concept_id",
				"concept_name",
				"domain_id",
				"vocabulary_id",
				"concept_class_id",
				"standard_concept",
				"concept_code",
				"valid_start_date",
				"valid_end_date",
				"invalid_reason",
				"atc"
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
			return ingredientMappingResultList.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return ingredientMappingResultList.get(rowIndex)[columnIndex];
		}
	     
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	        if (ingredientMappingResultList.isEmpty()) {
	            return Object.class;
	        }
	        return getValueAt(0, columnIndex).getClass();
	    }
		
	}
}
