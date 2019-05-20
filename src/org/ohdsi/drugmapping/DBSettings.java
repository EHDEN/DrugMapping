package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.List;

import org.ohdsi.databases.DbType;

public class DBSettings {
	public static int	DATABASE	= 1;
	public static int	CSVFILES	= 2;
	
	public int			dataType;
	public List<String>	tables		= new ArrayList<String>();
	
	// Database settings
	public String       name     = null;
	public DbType		dbType   = null;
	public String		user     = null;
	public String		password = null;
	public String		database = null;
	public String		server   = null;
	public String		domain   = null;
	
	// CSV file settings
	public char			delimiter	= ',';
}
