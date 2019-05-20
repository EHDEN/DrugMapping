package org.ohdsi.drugmapping.vocabulary;

import java.util.HashMap;
import java.util.Map;

import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.utilities.files.Row;

public class ConceptTable {
	private Map<String, Concept> table = new HashMap<String, Concept>();


	public ConceptTable(CDMDatabase database) {
		String query;

		System.out.println("    Loading table concept ... ");
		query  = "select concept_id";
		query += "," + "concept_name";
		query += "," + "domain_id";
		query += "," + "vocabulary_id";
		query += "," + "concept_class_id";
		query += "," + "standard_concept";
		query += "," + "valid_start_date";
		query += "," + "valid_end_date";
		query += "," + "invalid_reason";
		query += " " + "from " + database.getVocabSchema() + ".concept";
		
		int lastRecordCount = 0;
		int recordCount = 0;
		if (database.excuteQuery(query)) {
			if (database.hasNext()) {
				while (database.hasNext()) {
					Row queryRow = database.next();
					Concept concept = new Concept(
												queryRow.get("concept_id"),
												queryRow.get("concept_name"), 
												queryRow.get("domain_id"),
												queryRow.get("vocabulary_id"),
												queryRow.get("concept_class_id"),
												queryRow.get("standard_concept"),
												queryRow.get("valid_start_date"),
												queryRow.get("valid_end_date"),
												queryRow.get("invalid_reason")
											);
					table.put(concept.concept_id, concept);
					recordCount++;
					if ((recordCount - lastRecordCount) == 1000000) {
						System.out.println("        " + Integer.toString(recordCount) + " records");
						lastRecordCount = recordCount;
					}
				}
			}
		}
		System.out.println("    Done " + Integer.toString(recordCount) + " records");
	}

	public class Concept {
		public String concept_id;
		public String concept_name; 
		public String domain_id;
		public String vocabulary_id;
		public String concept_class_id;
		public String standard_concept;
		public String valid_start_date;
		public String valid_end_date;
		public String invalid_reason;


		public Concept(String concept_id, String concept_name, String domain_id, String vocabulary_id, String concept_class_id, String standard_concept, String valid_start_date, String valid_end_date, String invalid_reason) {
			this.concept_id = concept_id;
			this.concept_name = concept_name;
			this.domain_id = domain_id;
			this.vocabulary_id = vocabulary_id;
			this.concept_class_id = concept_class_id;
			this.standard_concept = standard_concept;
			this.valid_start_date = valid_start_date;
			this.valid_end_date = valid_end_date;
			this.invalid_reason = invalid_reason;
		}

	}

}
