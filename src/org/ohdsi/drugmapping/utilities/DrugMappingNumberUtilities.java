package org.ohdsi.drugmapping.utilities;

public class DrugMappingNumberUtilities {

	
	public static String percentage(Long count, Long total) {
		return count + " of " + total + " (" + Double.toString((double) Math.round((((double) (count) / (double) total) * 1000)) / 10.0) + "%)"; 
	}
	
	
	public static String uniformCASNumber(String casNumber) {
		casNumber = casNumber.replaceAll(" ", "").replaceAll("-", "");
		if (!casNumber.equals("")) {
			casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
			casNumber = ("000000"+casNumber).substring(casNumber.length() -5);
		}
		return  casNumber.equals("000000-00-0") ? "" : casNumber;
	}
}
