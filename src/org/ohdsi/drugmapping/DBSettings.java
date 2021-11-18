package org.ohdsi.drugmapping;

import java.util.ArrayList;
import java.util.List;

import org.ohdsi.databases.DbType;

public class DBSettings {
	public List<String>	tables		= new ArrayList<String>();
	
	// Database settings
	public String       name     = null;
	public DbType		dbType   = null;
	public String		user     = null;
	public String		password = null;
	public String		schema   = null;
	public String		server   = null;
	public String		domain   = null;
	
	// CSV file settings
	public char			delimiter	= ',';
	
	
	public String toString() {
		String description = "Database Settings [";
		if (name     != null) description += "\n    " + "Name=" + name;
		if (dbType   != null) description += "\n    " + "Type=" + dbType.toString();
		if (server   != null) description += "\n    " + "Server=" + server;
		if (user     != null) description += "\n    " + "User=" + user;
		if (schema   != null) description += "\n    " + "Vocab Schema=" + schema;
		if (domain   != null) description += "\n    " + "Domain=" + domain;
		description += "\n]";
		return description;
	}
}
