package org.ohdsi.drugmapping;

import java.util.HashMap;
import java.util.Map;

import org.ohdsi.drugmapping.gui.CDMDatabase;
import org.ohdsi.drugmapping.gui.InputFile;
import org.ohdsi.utilities.files.Row;

public class MapATC extends Mapping {
	private Map<String, Map<String, String>> ATCMapping;
	

	public MapATC(CDMDatabase database, InputFile atcFile) {
		ATCMapping = new HashMap<String, Map<String, String>>();
		
		String query;
		System.out.println(DrugMapping.getCurrentTime() + " Mapping ATC codes ...");
		
		if (atcFile.openFile()) {
			while (atcFile.hasNext()) {
				Row row = atcFile.next();
				String atc = atcFile.get(row, "ATC");
				String atcText = atcFile.get(row, "ATCText");
				String atcTextEnglish = atcFile.get(row, "ATCTextEnglish");
				//String atcTextEnglishUpperCase = atcTextEnglish.toUpperCase().replaceAll("'", "''");
				
				System.out.println("    " + atc + "," + atcText + "," + atcTextEnglish);
				
				Map<String, String> mappedRecord = null;
				
				// Try mapping to ATC concept
				query  = "select concept_id";
				query += "," + "concept_name"; 
				query += "," + "vocabulary_id";  
				query += "," + "standard_concept";
				query += "," + "concept_class_id"; 
				query += "," + "concept_code"; 
				query += " " + "from " + database.getVocabSchema() + ".concept"; 
				query += " " + "where upper(concept_code) = '" + atc + "'"; 
				query += " " + "and domain_id = 'Drug'"; 
				query += " " + "and vocabulary_id like 'ATC'"; 
				query += " " + "and concept_class_id = 'ATC " + (atc.length() == 1 ? "1st": (atc.length() == 3 ? "2nd" : (atc.length() == 4 ? "3rd" : (atc.length() == 5 ? "4th" : "5th")))) + "'";
				
				if (database.excuteQuery(query)) {
					if (database.hasNext()) {
						while (database.hasNext()) {
							Row queryRow = database.next();
							if (mappedRecord != null) {
								database.disconnect();
								mappedRecord = null;
								break;
							}
							mappedRecord = new HashMap<String, String>(); 
							mappedRecord.put("concept_id"      , queryRow.get("concept_id"));
							mappedRecord.put("concept_name"    , queryRow.get("concept_name"));
							mappedRecord.put("vocabulary_id"   , queryRow.get("vocabulary_id"));
							mappedRecord.put("concept_class_id", queryRow.get("concept_class_id"));
							mappedRecord.put("standard_concept", queryRow.get("standard_concept"));
							mappedRecord.put("concept_code"    , queryRow.get("concept_code"));
							mappedRecord.put("match"           , atc);
						}
					}
				}
				
				if (mappedRecord != null) {
					ATCMapping.put(atc, mappedRecord);
					
					System.out.println("        " + mappedRecord.get("match") +
							" -> " + mappedRecord.get("concept_id") +
							","+ mappedRecord.get("concept_name") +
							","+ "Drug" +
							","+ mappedRecord.get("vocabulary_id") +
							","+ mappedRecord.get("concept_class_id") +
							","+ mappedRecord.get("standard_concept") +
							","+ mappedRecord.get("concept_code")
							);
				}
				
			}
		}
		
		System.out.println(DrugMapping.getCurrentTime() + " Finished");
	}
}
