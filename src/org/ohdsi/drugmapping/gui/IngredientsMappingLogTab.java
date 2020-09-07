package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class IngredientsMappingLogTab extends MainFrameTab {
	private static final long serialVersionUID = 1736241328985740613L;
	
	private JPanel ingredientsPanel;
	private JTable ingredientsTable;
	private JPanel ingredientsMappingLogPanel;
	private JPanel ingredientsMappingResultPanel;

	private TableRowSorter<? extends TableModel> rowSorter;

	
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
                String text = searchField.getText().toUpperCase();
                if (text.length() == 0) {
                    rowSorter.setRowFilter(null);
                }
                else {
                	//TODO escape special characters
                    rowSorter.setRowFilter(RowFilter.regexFilter(text));
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
		ingredientsPanel.setBorder(BorderFactory.createTitledBorder("Drugs"));
		ingredientsPanel.setMinimumSize(new Dimension(100, 205));
		ingredientsPanel.setMaximumSize(new Dimension(100000, 205));
		ingredientsPanel.setPreferredSize(new Dimension(100, 205));
		
		JPanel drugMappingLogResultsPanel = new JPanel(new BorderLayout());
		
		ingredientsMappingLogPanel = new JPanel(new BorderLayout());
		ingredientsMappingLogPanel.setBorder(BorderFactory.createTitledBorder("Drug Mapping Log"));
		ingredientsMappingLogPanel.setMinimumSize(new Dimension(100, 205));
		ingredientsMappingLogPanel.setMaximumSize(new Dimension(100000, 205));
		ingredientsMappingLogPanel.setPreferredSize(new Dimension(100, 250));
		
		ingredientsMappingResultPanel = new JPanel(new BorderLayout());
		ingredientsMappingResultPanel.setMinimumSize(new Dimension(100, 100));
		ingredientsMappingResultPanel.setMaximumSize(new Dimension(100000, 100000));
		ingredientsMappingResultPanel.setPreferredSize(new Dimension(100, 200));
		ingredientsMappingResultPanel.setBorder(BorderFactory.createTitledBorder("Drug Mapping Results"));
						
		this.add(searchPanel, BorderLayout.NORTH);
		this.add(drugsResultsPanel, BorderLayout.CENTER);
		drugsResultsPanel.add(ingredientsPanel, BorderLayout.NORTH);
		drugsResultsPanel.add(drugMappingLogResultsPanel, BorderLayout.CENTER);
		drugMappingLogResultsPanel.add(ingredientsMappingLogPanel, BorderLayout.NORTH);
		drugMappingLogResultsPanel.add(ingredientsMappingResultPanel, BorderLayout.CENTER);
	}
}
