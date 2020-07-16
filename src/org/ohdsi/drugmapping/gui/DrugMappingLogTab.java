package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.ohdsi.drugmapping.source.SourceDrug;

public class DrugMappingLogTab extends MainFrameTab {
	private static final long serialVersionUID = -2535974179089673874L;
	
	private JPanel drugsPanel;
	private JPanel drugResultsPanel;

	public DrugMappingLogTab() {
		super();
		
		setLayout(new BorderLayout());
		
		JPanel searchPanel = new JPanel();
		searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
		JLabel searchLabel = new JLabel("Search term: ");
		JTextField searchField = new JTextField(20);
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
		
		
		//JTable drugResultsTable = new JTable(10, 5);
		//JScrollPane drugResultsScrollPane = new JScrollPane(drugResultsTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//resultsPane.add(drugResultsScrollPane, BorderLayout.CENTER);
		
				
		this.add(searchPanel, BorderLayout.NORTH);
		this.add(drugsResultsPanel, BorderLayout.CENTER);
		drugsResultsPanel.add(drugsPanel, BorderLayout.NORTH);
		drugsResultsPanel.add(drugResultsPanel, BorderLayout.CENTER);
	}
	
	
	public void listDrugs(List<Object[]> drugList) {
		String[] header = new String[] {
			"Status",
			"Code",
			"Name",
			"ATCCode",
			"Formulation",
			"Use Count"
		};
		Object[][] drugs = new Object[drugList.size()][header.length + 1];
		for (int drugNr = 0; drugNr < drugList.size(); drugNr++) {
			Object[] drug = drugList.get(drugNr);
			for (int columnNr = 0; columnNr < (header.length + 1); columnNr++) {
				drugs[drugNr][columnNr] = drug[columnNr]; 
			}
		}
		JTable drugsTable = new JTable(drugs, header);
		drugsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		DefaultTableCellRenderer rightAlignmentRenderer = new DefaultTableCellRenderer();
		rightAlignmentRenderer.setHorizontalAlignment(JLabel.RIGHT);
		
		drugsTable.setAutoCreateRowSorter(true);

		/*/ Make header clickable to sort columns
		drugsTable.getTableHeader().addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e) {
		    	sorting = true;
		        int sortingColumn = drugsTable.columnAtPoint(e.getPoint());
		        sorter.sortColumn(sortingColumn);
		        sorting = false;
		    }
		});
		/**/

		// Set selection to first row
		ListSelectionModel selectionModel = drugsTable.getSelectionModel();
		selectionModel.setSelectionInterval(0, 0);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				System.out.print(drugsTable.getSelectedRow());
				selectDrug((SourceDrug) drugs[drugsTable.getSelectedRow()][header.length]);
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
	}
	
	
	private void selectDrug(SourceDrug sourceDrug) {
		System.out.println(": " + sourceDrug);
	}
}
