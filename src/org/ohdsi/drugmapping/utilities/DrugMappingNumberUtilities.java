package org.ohdsi.drugmapping.utilities;

public class DrugMappingNumberUtilities {

	
	public static String percentage(Long count, Long total) {
		return count + " of " + total + " (" + Double.toString((double) Math.round((((double) (count) / (double) total) * 100000)) / 1000.0) + "%)"; 
	}
	
	
	public static String uniformCASNumber(String casNumber) {
		casNumber = DrugMappingStringUtilities.removeLeadingZeros(casNumber.replaceAll(" ", "").replaceAll("-", ""));
		if ((!casNumber.equals("")) && (casNumber.length() > 3)) {
			casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
		}
		else {
			casNumber = "";
		}
		return  casNumber;
	}
}
