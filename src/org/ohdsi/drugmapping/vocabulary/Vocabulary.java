package org.ohdsi.drugmapping.vocabulary;

import org.ohdsi.drugmapping.gui.CDMDatabase;

public class Vocabulary {
	private CDMDatabase database;
	public ConceptTable conceptTable;

	public Vocabulary(CDMDatabase database) {
		this.database = database;
		
		VocabularyThread thread = new VocabularyThread();
		thread.start();
	}
	
	
	private class VocabularyThread extends Thread {
		
		public void run() {
			System.out.println("Loading vocabulary ...");
			conceptTable = new ConceptTable(database);
			System.out.println("Finished");
		}
		
	}
}
