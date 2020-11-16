package org.ohdsi.drugmapping.utilities;

import java.text.DecimalFormat;

public class DrugMappingNumberUtilities {

	
	public static String percentage(Long count, Long total) {
		return count + " of " + total + " (" + Double.toString((double) Math.round((((double) (count) / (double) total) * 100000)) / 1000.0) + "%)"; 
	}
	
	
	public static String uniformCASNumber(String casNumber) {
		casNumber = DrugMappingStringUtilities.removeLeadingZeros(casNumber.replaceAll(" ", "").replaceAll("-", "").replaceAll("\t", ""));
		if ((!casNumber.equals("")) && (casNumber.length() > 3)) {
			casNumber = casNumber.substring(0, casNumber.length() - 3) + "-" + casNumber.substring(casNumber.length() - 3, casNumber.length() - 1) + "-" + casNumber.substring(casNumber.length() - 1);
		}
		else {
			casNumber = "";
		}
		return  casNumber;
	}
	
	
	public static String doubleWithPrecision(Double value, Integer precision) {
		String valueString = null;
		if (precision >= 0) {
			String zeroString = "00000000000000000000000000000000000000000000000000000000000000000000000000";
	        DecimalFormat df = new DecimalFormat("#");
			if (precision >= 0) {
		        df.setMaximumFractionDigits(precision);
			}
			
			if (value != null) {
		        valueString = df.format(value);
			}
			if (valueString != null) {
				if (precision >= 0) {
					valueString =  df.format((double) Math.round(value * Math.pow(10, precision)) / Math.pow(10, precision));
					if (!valueString.contains(".")) {
						valueString += ".";
					}
					valueString = (valueString + zeroString).substring(0, valueString.indexOf(".") + precision + 1);
				}
				if (valueString.startsWith(".")) {
					valueString = "0" + valueString;
				}
			}
		}
		else {
			valueString = value == null ? null : value.toString();
		}
		return valueString;
	}
}
