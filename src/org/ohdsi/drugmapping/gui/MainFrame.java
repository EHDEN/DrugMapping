package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import org.ohdsi.drugmapping.DrugMapping;

public class MainFrame {
	
	private static final String ICON = "/org/ohdsi/drugmapping/gui/OHDSI Icon Picture 048x048.gif"; 
	
	public static int VOCABULARY_ID;
	
	public static int MINIMUM_USE_COUNT;
	public static int MAXIMUM_STRENGTH_DEVIATION;

	public static int PREFERENCE_MATCH_COMP_FORM;
	public static int PREFERENCE_MATCH_INGREDIENTS_TO_COMP;	
	public static int PREFERENCE_RXNORM;
	public static int PREFERENCE_ATC;
	public static int PREFERENCE_PRIORITIZE_BY_DATE;
	public static int PREFERENCE_PRIORITIZE_BY_CONCEPT_ID;
	public static int PREFERENCE_TAKE_FIRST_OR_LAST;
	
	public static int SAVE_DRUGMAPPING_RESULTS;
	public static int SUPPRESS_WARNINGS;
	
	private DrugMapping drugMapping;
	private JFrame frame;
	private ExecuteTab executeTab;
	private DrugMappingLogTab drugMappingLogTab;
	

	/**
	 * Sets an icon on a JFrame or a JDialog.
	 * @param container - the GUI component on which the icon is to be put
	 */
	public static void setIcon(Object container){
		URL url = DrugMapping.class.getResource(ICON);
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		if (container.getClass() == JFrame.class ||
				JFrame.class.isAssignableFrom(container.getClass()))
			((JFrame)container).setIconImage(img);
		else if (container.getClass() == JDialog.class  ||
				JDialog.class.isAssignableFrom(container.getClass()))
			((JDialog)container).setIconImage(img);
		else
			((JFrame)container).setIconImage(img);
	}
	
	
	public MainFrame(DrugMapping drugMapping) {
		this.drugMapping = drugMapping;
		createInterface();
		initialize();
	}
	
	
	public void show() {
		frame.setVisible(true);
	}
	
	
	private JFrame createInterface() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(1000, 800);
		frame.setMinimumSize(new Dimension(800, 600));
		frame.setTitle("OHDSI Drug Mapping Tool");
		MainFrame.setIcon(frame);
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());
		
		JMenuBar menuBar = createMenu();
		frame.setJMenuBar(menuBar);
		
		JTabbedPane tabbedPane = new JTabbedPane();
		DrugMapping.disableWhenRunning(tabbedPane);
		
		executeTab = new ExecuteTab(drugMapping, this);
		drugMappingLogTab = new DrugMappingLogTab();
		
		tabbedPane.addTab("Execute", executeTab);
		tabbedPane.addTab("Drug Mapping Log", drugMappingLogTab);
		
		frame.add(tabbedPane, BorderLayout.CENTER);
		
		return frame;
	}
	
	
	private JMenuBar createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		
		if (!DrugMapping.special.equals("ZINDEX")) {
			JMenuItem loadDatabaseSettingsMenuItem = new JMenuItem("Load Database Settings");
			loadDatabaseSettingsMenuItem.setToolTipText("Load Database Settings");
			loadDatabaseSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.loadDatabaseSettingsFile();
				}
			});
			file.add(loadDatabaseSettingsMenuItem);
			
			JMenuItem saveDatabaseSettingsMenuItem = new JMenuItem("Save Database Settings");
			saveDatabaseSettingsMenuItem.setToolTipText("Save Database Settings");
			saveDatabaseSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.saveDatabaseSettingsFile();
				}
			});
			file.add(saveDatabaseSettingsMenuItem);
		}
		
		JMenuItem loadFileSettingsMenuItem = new JMenuItem("Load File Settings");
		loadFileSettingsMenuItem.setToolTipText("Load File Settings");
		loadFileSettingsMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				executeTab.loadFileSettingsFile();
			}
		});
		file.add(loadFileSettingsMenuItem);
		
		JMenuItem saveFileSettingsMenuItem = new JMenuItem("Save File Settings");
		saveFileSettingsMenuItem.setToolTipText("Save File Settings");
		saveFileSettingsMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				executeTab.saveFileSettingsFile();
			}
		});
		file.add(saveFileSettingsMenuItem);

		if (!DrugMapping.special.equals("ZINDEX")) {
			JMenuItem loadGeneralSettingsMenuItem = new JMenuItem("Load General Settings");
			loadGeneralSettingsMenuItem.setToolTipText("Load General Settings");
			loadGeneralSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.loadGeneralSettingsFile();
				}
			});
			file.add(loadGeneralSettingsMenuItem);
			
			JMenuItem saveGeneralSettingsMenuItem = new JMenuItem("Save General Settings");
			saveGeneralSettingsMenuItem.setToolTipText("Save General Settings");
			saveGeneralSettingsMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					executeTab.saveGeneralSettingsFile();
				}
			});
			file.add(saveGeneralSettingsMenuItem);
		}
		
		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.setToolTipText("Exit application");
		exitMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		});
		file.add(exitMenuItem);
		
		menuBar.add(file);
		
		DrugMapping.disableWhenRunning(file);
		
		return menuBar;
	}

	
	public void initialize() {
		if (DrugMapping.settings != null) {
			for (Setting setting : DrugMapping.settings.getSettings()) {
				setting.initialize();
			}
		}
	}

	
	public void loadFileSettingsFile(List<String> fileSettings) {
		executeTab.loadFileSettingsFile(fileSettings);
	}

	
	public void loadGeneralSettingsFile(List<String> generalSettings) {
		executeTab.loadGeneralSettingsFile(generalSettings);
	}
	
	
	public List<String> readSettingsFromFile(String settingsFileName, boolean mandatory) {
		return executeTab.readSettingsFromFile(settingsFileName, mandatory);
	}
	
	
	public JFrame getFrame() {
		return frame;
	}
	
	
	public CDMDatabase getDatabase() {
		return executeTab.getDatabase();
	}
	
	
	public InputFile getInputFile(String fileName) {
		return executeTab.getInputFile(fileName);
	}
	
	
	public Folder getOutputFolder() {
		return executeTab.getOutputFolder();
	}
	
	
	public void setLogFile(String logFile) {
		executeTab.setLogFile(logFile);
	}
	
	
	public void closeLogFile() {
		executeTab.closeLogFile();
	}
	
	
	public void listDrugs(List<Object[]> drugList) {
		drugMappingLogTab.listDrugs(drugList);
	}

}
