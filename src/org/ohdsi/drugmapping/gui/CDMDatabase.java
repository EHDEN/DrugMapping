package org.ohdsi.drugmapping.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.ohdsi.databases.DbType;
import org.ohdsi.databases.QueryParameters;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.databases.RichConnection.QueryResult;
import org.ohdsi.drugmapping.DBSettings;
import org.ohdsi.drugmapping.DrugMapping;
import org.ohdsi.drugmapping.files.DelimitedFileRow;
import org.ohdsi.drugmapping.files.DelimitedFileWithHeader;
import org.ohdsi.utilities.StringUtilities;
import org.ohdsi.utilities.files.Row;

public class CDMDatabase extends JPanel {
	private static final long serialVersionUID = -5203643429301521906L;
	
	private final int DATABASE_LABEL_SIZE = 260;

	private String cdmCacheLocation = null;
	private String cdmCache = null;
	private String cdmCacheFileName = null;
	private boolean readFromCache = false;
	private DelimitedFileWithHeader cache = null;
	private Iterator<DelimitedFileRow> cacheIterator = null;
	private boolean headerWritten = false;
	
	private QueryParameters queryParameters = null;
	private QueryResult queryResult = null;
	private Iterator<Row> queryResultIterator = null;

	private JPanel serverLabelPanel;
	private JCheckBox serverRefreshCacheCheckBox;
	private JLabel serverLabel;
	private JTextField serverField;
	private JButton serverSelectButton;

	private JTextField			databaseNameField;
	private JComboBox<String>	databaseTypeField;
	private JTextField			databaseServerField;
	private JTextField			databaseUserField;
	private JTextField			databasePasswordField;
	private JTextField			databaseVocabSchemaField;
	
	private DBSettings dbSettings = null;
	private RichConnection connection = null;
	
	
	public CDMDatabase() {
		try {
			cdmCacheLocation = new File(DrugMapping.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile().getPath() + File.separator + "CDM Cache";
			File cdmCacheFolder = new File(cdmCacheLocation); 
			if ((!cdmCacheFolder.mkdir()) && (!cdmCacheFolder.exists())) {
				cdmCacheLocation = null;
			}
		} catch (URISyntaxException e1) {
			cdmCacheLocation = null;
		}
		if (cdmCacheLocation == null) {
			System.out.println("WARNING: No CDM Cache location available!");
		}
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		JPanel serverRefreshLabelPanel = new JPanel(new BorderLayout());
		serverRefreshLabelPanel.setMinimumSize(new Dimension(DATABASE_LABEL_SIZE, serverRefreshLabelPanel.getHeight()));
		serverRefreshLabelPanel.setPreferredSize(new Dimension(DATABASE_LABEL_SIZE, serverRefreshLabelPanel.getHeight()));
		serverRefreshCacheCheckBox = new JCheckBox();
		serverRefreshCacheCheckBox.setSelected(false);
		serverRefreshCacheCheckBox.setToolTipText("Refresh Cache");
		serverRefreshLabelPanel.add(serverRefreshCacheCheckBox, BorderLayout.WEST);
		DrugMapping.disableWhenRunning(serverRefreshCacheCheckBox);
		
		serverLabelPanel = new JPanel(new BorderLayout());
		serverLabel = new JLabel("CDM Database:");
		serverLabelPanel.add(serverLabel, BorderLayout.WEST);
		
		serverRefreshLabelPanel.add(serverLabelPanel, BorderLayout.CENTER);
		
		serverField = new JTextField();
		serverField.setText("");
		serverField.setEditable(false);
		serverField.setPreferredSize(new Dimension(10000, serverField.getHeight()));

		serverSelectButton = new JButton("Select");

		add(serverRefreshLabelPanel);
		add(serverField);
		add(new JLabel("  "));
		add(serverSelectButton);
		
		final CDMDatabase currentCDMDatabase = this;
		serverSelectButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				defineDatabase(currentCDMDatabase);
			}
		});
		
		DrugMapping.disableWhenRunning(serverSelectButton);
	}
	
	
	public String getDBServerName() {
		return serverField == null ? "" : serverField.getText();
	}
	
	
	public DBSettings getDBSettings() {
		return dbSettings;
	}
	
	
	public void setDBSettings(DBSettings dbSettings) {
		this.dbSettings = dbSettings;
	}
	
	
	public String getVocabSchema() {
		return getDBSettings().database;
	}
	
	
	public List<String> getSettings() {
		List<String> settings = null;
		if (dbSettings != null) {
			settings = new ArrayList<String>();
			settings.add("#");
			settings.add("# " + dbSettings.name);
			settings.add("#");
			settings.add("");
			settings.add("name=" + dbSettings.name);
			settings.add("dbtype=" + dbSettings.dbType.toString());
			settings.add("server=" + dbSettings.server);
			settings.add("user=" + dbSettings.user);
			settings.add("vocabSchema=" + dbSettings.database);
		}
		return settings;
	}
	
	
	public void putSettings(List<String> settings) {
		dbSettings = new DBSettings();
		for (String setting : settings) {
			if ((!setting.trim().equals("")) && (!setting.substring(0, 1).equals("#"))) {
				int equalSignIndex = setting.indexOf("=");
				String settingVariable = setting.substring(0, equalSignIndex);
				String value = setting.substring(equalSignIndex + 1);
				if (settingVariable.equals("name")) dbSettings.name = value;
				if (settingVariable.equals("dbtype")) dbSettings.dbType = new DbType(value);
				if (settingVariable.equals("server")) dbSettings.server = value;
				if (settingVariable.equals("user")) dbSettings.user = value;
				if (settingVariable.equals("password")) dbSettings.password = value;
				if (settingVariable.equals("vocabSchema")) dbSettings.database = value;
			}
		}
		if (dbSettings.password == null) {
			defineDatabase(this);
		}
		serverField.setText(dbSettings.name);
	}
	
	
	public boolean connect(Class<?> context) {
		boolean connectionOK = false;

		queryParameters = new QueryParameters();
		queryParameters.set("@vocab", getVocabSchema());
		
		disconnect();
		
		if (cdmCacheLocation != null) {
			cdmCache = cdmCacheLocation + File.separator + serverField.getText();
			File cdmCacheFolder = new File(cdmCache);
			if ((!serverRefreshCacheCheckBox.isSelected()) && cdmCacheFolder.canRead()) {
				readFromCache = true;
				connectionOK = true;
			}
			else {
				readFromCache = false;
				cdmCacheFolder.mkdir();
				try {
					connection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType);
					if (connection != null) {
						connection.setContext(context);
						connectionOK = true;
					}
				} catch (RuntimeException e) {
					JOptionPane.showMessageDialog(null, StringUtilities.wordWrap(e.getMessage(), 80), "Error connecting to server", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		return connectionOK;
	}
	
	
	public boolean excuteQueryResource(String resourceName) {
		boolean result = true;
		cdmCacheFileName = cdmCache + File.separator + resourceName + ".csv";
		if (readFromCache) {
			if (new File(cdmCacheFileName).canRead()) {
				cache = new DelimitedFileWithHeader(cdmCacheFileName);
				result = cache.openForReading();
				if (result) {
					cacheIterator = cache.iterator();
				}
			}
			else {
				result = false;
			}
		}
		else {
			try {
				queryResult = connection.queryResource(resourceName, queryParameters);
				queryResultIterator = queryResult.iterator();
			} catch (RuntimeException e) {
				queryResult = null;
				result = false;
			}
			cache = new DelimitedFileWithHeader(cdmCacheFileName);
			headerWritten = false;
			if (!cache.openForWriting()) {
				cache = null;
			}
		}
		return result;
	}
	
	
	public boolean hasNext() {
		boolean hasNext = false;
		if (readFromCache) {
			hasNext = cacheIterator.hasNext();
		}
		else {
			if (queryResult != null) {
				hasNext = queryResultIterator.hasNext();
				if (!hasNext) {
					queryResultIterator = null;
					if (cache != null) {
						cache.closeForWriting();
						cache = null;
						cdmCacheFileName = null;
						cacheIterator = null;
					}
				}
			}
		}
		return hasNext;
	}
	
	
	public DelimitedFileRow next() {
		DelimitedFileRow delimitedFileRow = null;
		if (readFromCache) {
			delimitedFileRow = cacheIterator.next();
		}
		else {
			if (queryResultIterator != null) {
				Row row = queryResultIterator.next();
				if (row != null) {
					delimitedFileRow = new DelimitedFileRow(row.getCells(), row.getfieldName2ColumnIndex());
					if (cache != null) {
						if (!headerWritten) {
							cache.setHeader(delimitedFileRow.getFieldNames());
							headerWritten = true;
						}
						cache.writeRow(delimitedFileRow);
					}
				}
			}
		}
		return delimitedFileRow;
	}
	
	
	public void disconnect() {
		// Close current connection
		if (connection != null) {
			connection.close();
			connection = null;
		}
		cache = null;
		cdmCacheFileName = null;
		cacheIterator = null;
		headerWritten = false;
	}
	
	
	public RichConnection getRichConnection(Class<?> context) {
		RichConnection connection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType);
		connection.setContext(context);
		return connection;
	}
	
	
	private void defineDatabase(CDMDatabase database) {
		JDialog databaseDialog = new JDialog();
		databaseDialog.setLayout(new BorderLayout());
		databaseDialog.setModal(true);
		databaseDialog.setSize(500, 270);
		MainFrame.setIcon(databaseDialog);
		databaseDialog.setLocationRelativeTo(null);
		databaseDialog.setTitle("CDM Database Definition");
		databaseDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JPanel databasePanel = new JPanel(new BorderLayout());
		databasePanel.setBorder(BorderFactory.createTitledBorder("CDM Parameters"));
		
		JPanel databaseDefinitionPanel = new JPanel();
		databaseDefinitionPanel.setBorder(BorderFactory.createEmptyBorder());
		databaseDefinitionPanel.setLayout(new GridLayout(0, 2));
		databaseDefinitionPanel.add(new JLabel("Database name:"));
		databaseNameField = new JTextField("");
		databaseNameField.setToolTipText("A pretty name for the database");
		databaseDefinitionPanel.add(databaseNameField);
		databaseDefinitionPanel.add(new JLabel("Data type:"));
		databaseTypeField = new JComboBox<String>(new String[] { "PostgreSQL", "Oracle", "SQL Server" });
		databaseTypeField.setToolTipText("Select the type of server where the CDM and vocabulary will be stored");
		databaseDefinitionPanel.add(databaseTypeField);
		databaseDefinitionPanel.add(new JLabel("Server location:"));
		databaseServerField = new JTextField("");
		databaseDefinitionPanel.add(databaseServerField);
		databaseDefinitionPanel.add(new JLabel("User name:"));
		databaseUserField = new JTextField("");
		databaseDefinitionPanel.add(databaseUserField);
		databaseDefinitionPanel.add(new JLabel("Password:"));
		databasePasswordField = new JPasswordField("");
		databaseDefinitionPanel.add(databasePasswordField);
		databaseDefinitionPanel.add(new JLabel("Vocabulary schema:"));
		databaseVocabSchemaField = new JTextField("");
		databaseDefinitionPanel.add(databaseVocabSchemaField);
		
		databaseTypeField.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				
				if (arg0.getItem().toString().equals("Oracle")) {
					databaseServerField.setToolTipText(
							"For Oracle servers this field contains the SID, servicename, and optionally the port: '<host>/<sid>', '<host>:<port>/<sid>', '<host>/<service name>', or '<host>:<port>/<service name>'");
					databaseUserField.setToolTipText("For Oracle servers this field contains the name of the user used to log in");
					databasePasswordField.setToolTipText("For Oracle servers this field contains the password corresponding to the user");
					databaseVocabSchemaField
							.setToolTipText("For Oracle servers this field contains the schema (i.e. 'user' in Oracle terms) containing the target tables");
				} else if (arg0.getItem().toString().equals("PostgreSQL")) {
					databaseServerField.setToolTipText("For PostgreSQL servers this field contains the host name and database name (<host>/<database>)");
					databaseUserField.setToolTipText("The user used to log in to the server");
					databasePasswordField.setToolTipText("The password used to log in to the server");
					databaseVocabSchemaField.setToolTipText("For PostgreSQL servers this field contains the schema containing the target tables");
				} else {
					databaseServerField.setToolTipText("This field contains the name or IP address of the database server");
					if (arg0.getItem().toString().equals("SQL Server"))
						databaseUserField.setToolTipText(
								"The user used to log in to the server. Optionally, the domain can be specified as <domain>/<user> (e.g. 'MyDomain/Joe')");
					else
						databaseUserField.setToolTipText("The user used to log in to the server");
					databasePasswordField.setToolTipText("The password used to log in to the server");
					databaseVocabSchemaField.setToolTipText("The name of the database containing the target tables");
				}
			}
		});
		databaseTypeField.setSelectedIndex(1);
		
		JPanel databaseDefinitionSubPanel = new JPanel(new BorderLayout());
		databaseDefinitionPanel.setBorder(BorderFactory.createEmptyBorder());
		
		JPanel databaseTestPanel = new JPanel();
		databaseTestPanel.setBorder(BorderFactory.createEmptyBorder());
		databaseTestPanel.setLayout(new BoxLayout(databaseTestPanel, BoxLayout.X_AXIS));
		databaseTestPanel.add(Box.createHorizontalGlue());
		
		JButton testConnectionButton = new JButton("Test connection");
		testConnectionButton.setBackground(new Color(151, 220, 141));
		testConnectionButton.setToolTipText("Test the connection");
		testConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DBSettings testDBSettings = getDBSettingsFromInput();
				String testResult = testConnection(testDBSettings, true); 
				if (testResult.equals("OK")) {
					testResult = "Succesfully connected to " + testDBSettings.database + " on server " + testDBSettings.server;
					JOptionPane.showMessageDialog(null, StringUtilities.wordWrap(testResult, 80), "Error connecting to server", JOptionPane.ERROR_MESSAGE);
				}
				else {
					JOptionPane.showMessageDialog(null, StringUtilities.wordWrap(testResult, 80), "Error connecting to server", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		databaseTestPanel.add(testConnectionButton);
		
		// Button section
		JPanel buttonSectionPanel = new JPanel(new BorderLayout());
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton okButton = new JButton("    OK    ");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String databaseName = databaseNameField.getText();
				String databaseType = databaseTypeField.getSelectedItem().toString();
				String databaseServer = databaseServerField.getText();
				String databaseUser = databaseUserField.getText();
				String databasePassword = databasePasswordField.getText();
				String databaseVocabSchema = databaseVocabSchemaField.getText();
				if (saveDatabaseSettings(database, databaseName, databaseType, databaseServer, databaseUser, databasePassword, databaseVocabSchema)) {
					databaseDialog.dispose();
				}
			}
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				databaseDialog.dispose();				
			}
		});
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		buttonSectionPanel.add(buttonPanel, BorderLayout.WEST);

		databaseDialog.add(databasePanel, BorderLayout.NORTH);
		databasePanel.add(databaseDefinitionPanel, BorderLayout.NORTH);
		databasePanel.add(databaseDefinitionSubPanel, BorderLayout.CENTER);
		databaseDefinitionSubPanel.add(databaseTestPanel, BorderLayout.NORTH);
		databaseDialog.add(buttonSectionPanel, BorderLayout.SOUTH);
		
		if (dbSettings != null) {
			databaseNameField.setText(dbSettings.name);
			for (int itemNr = 0; itemNr < databaseTypeField.getModel().getSize(); itemNr++) {
				if (databaseTypeField.getItemAt(itemNr).toLowerCase().equals(dbSettings.dbType.toString())) {
					databaseTypeField.setSelectedIndex(itemNr);
					break;
				}
			}
			databaseServerField.setText(dbSettings.server);
			databaseUserField.setText(dbSettings.user);
			databaseVocabSchemaField.setText(dbSettings.database);
		}

		databaseDialog.setVisible(true);
	}
	
	
	private boolean saveDatabaseSettings(CDMDatabase database, String databaseName, String databaseType, String databaseServer, String databaseUser, String databasePassword, String databaseVocabSchema) {
		boolean saveOK = false;
		DBSettings settings = getDBSettingsFromInput();
		if (
				(!databaseName.equals("")) &&
				(!databaseType.equals("")) &&
				(!databaseServer.equals("")) &&
				(!databaseUser.equals("")) &&
				(!databasePassword.equals("")) &&
				(!databaseVocabSchema.equals("")) &&
				testConnection(settings, true).equals("OK")
			) {
			
			database.setDBSettings(settings);
			database.serverField.setText(databaseName);
			
			saveOK = true;
			/*
			System.out.println("Database name        : " + databaseName);
			System.out.println("Database type        : " + databaseType);
			System.out.println("Database server      : " + databaseServer);
			System.out.println("Database user        : " + databaseUser);
			System.out.println("Database password    : " + databasePassword);
			System.out.println("Database vocab schema: " + databaseVocabSchema);
			*/
		}
		else {
			JOptionPane.showMessageDialog(null, "Settings are not complete!", "Error", JOptionPane.ERROR_MESSAGE);
		}
		return saveOK;
	}
	
	
	private String testConnection(DBSettings dbSettings, boolean testConnectionToDb) {
		String result = "OK";
		RichConnection connection;
		try {
			connection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType);
			
			if (testConnectionToDb) {
				try {
					connection.getTableNames(dbSettings.database);
					
					connection.close();
				} catch (Exception e) {
					result = "Could not connect to database: " + e.getMessage();
				}
			}
			else {
				connection.close();
			}
		} catch (Exception e) {
			result = "Could not connect to source server: " + e.getMessage();
		}
		return result;
	}
	
	
	private DBSettings getDBSettingsFromInput() {
		DBSettings dbSettings = new DBSettings();
		dbSettings.name = databaseNameField.getText();
		dbSettings.user = databaseUserField.getText();
		dbSettings.password = databasePasswordField.getText();
		dbSettings.server = databaseServerField.getText();
		dbSettings.database = databaseVocabSchemaField.getText();
		if (databaseTypeField.getSelectedItem().toString().equals("MySQL"))
			dbSettings.dbType = DbType.MYSQL;
		else if (databaseTypeField.getSelectedItem().toString().equals("Oracle"))
			dbSettings.dbType = DbType.ORACLE;
		else if (databaseTypeField.getSelectedItem().toString().equals("PostgreSQL"))
			dbSettings.dbType = DbType.POSTGRESQL;
		else if (databaseTypeField.getSelectedItem().toString().equals("SQL Server")) {
			dbSettings.dbType = DbType.MSSQL;
			if (databaseUserField.getText().length() != 0) { // Not using windows authentication
				String[] parts = databaseUserField.getText().split("/");
				if (parts.length < 2) {
					dbSettings.user = databaseUserField.getText();
					dbSettings.domain = null;
				} else {
					dbSettings.user = parts[1];
					dbSettings.domain = parts[0];
				}
			}
		}
		
		if (dbSettings.database.trim().length() == 0) {
			String message = "Please specify a name for the database database";
			JOptionPane.showMessageDialog(null, StringUtilities.wordWrap(message, 80), "Database error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		return dbSettings;
	}

}
