package org.ohdsi.drugmapping.utilities;

import java.util.HashMap;
import java.util.Map;

import org.ohdsi.drugmapping.DrugMapping;

public class DrugMappingStringUtilities {
	

	public static String removeExtraSpaces(String string) {
		/*
		String orgString;
		string = (string == null ? "" : string).trim();
		do {
			orgString = string;
			string = orgString.replaceAll("  ", " ");
		} while (string.length() != orgString.length());
		*/
		return string;
	}
	
	
	public static String escapeFieldValue(String value) {
		return escapeFieldValue(value, ",", "\"");
	}
	
	
	public static String escapeFieldValue(String value, String fieldDelimiter, String textQualifier) {
		if (value == null) {
			value = "";
		}
		else if (value.contains(fieldDelimiter) || value.contains(textQualifier)) {
			value = textQualifier + value.replaceAll(textQualifier, textQualifier + textQualifier) + textQualifier;
		}
		return value;
	}
	
	
	public static String modifyName(String name) {
		
		if (name != null) {
			name = " " + convertToStandardCharacters(name).toUpperCase() + " ";
			
			name = name.replaceAll("-", " ");
			name = name.replaceAll(",", " ");
			name = name.replaceAll("/", " ");
			name = name.replaceAll("[(]", " ");
			name = name.replaceAll("[)]", " ");
			name = name.replaceAll("_", " ");
			name = name.replaceAll("^", " ");
			name = name.replaceAll("'", " ");
			name = name.replaceAll("\\]", " ");
			name = name.replaceAll("\\[", " ");

			// Prevent these seperate letters to be patched
			name = name.replaceAll(" A ", "_A_");
			name = name.replaceAll(" O ", "_O_");
			name = name.replaceAll(" E ", "_E_");
			name = name.replaceAll(" U ", "_U_");
			name = name.replaceAll(" P ", "_P_");
			name = name.replaceAll(" H ", "_H_");

			name = name.replaceAll("AAT", "ATE");
			name = name.replaceAll("OOT", "OTE");
			name = name.replaceAll("ZUUR", "ACID");
			name = name.replaceAll("AA", "A");
			name = name.replaceAll("OO", "O");
			name = name.replaceAll("EE", "E");
			name = name.replaceAll("UU", "U");
			name = name.replaceAll("TH", "T");
			name = name.replaceAll("AE", "A");
			name = name.replaceAll("EA", "A");
			name = name.replaceAll("PH", "F");
			name = name.replaceAll("Y", "I");
			name = name.replaceAll("S ", " ");
			name = name.replaceAll("E ", " ");
			name = name.replaceAll("A ", " ");
			name = name.replaceAll("O ", " ");
			name = name.replaceAll(" ", "");

			name = name.replaceAll("_", " ");

			name = name.replaceAll("AA", "A");
			name = name.replaceAll("OO", "O");
			name = name.replaceAll("EE", "E");
			name = name.replaceAll("UU", "U");
			name = name.replaceAll("TH", "T");
			name = name.replaceAll("AE", "A");
			name = name.replaceAll("EA", "A");
			name = name.replaceAll("PH", "F");
			
			name = removeExtraSpaces(name).trim();
		}
		
		return name;
	}
	
	
	public static String cleanString(String string) {
		return string == null ? null : removeExtraSpaces(string.trim().replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\t", " ").replaceAll("�", "SS").replaceAll("�", "*"));
	}

	
	
	public static String convertToStandardCharacters(String text) {
		String convertedText = null;
		
		if (text != null) {
			convertedText = "";
			
			for (int charNr = 0; charNr < text.length(); charNr++) {
				char character = text.charAt(charNr);
				
				if (
						(character != ' ') &&
						(character != ',') &&
						(character != '.') &&
						(!characterInRange(character, 'a', 'z')) &&
						(!characterInRange(character, 'A', 'Z')) &&
						(!characterInRange(character, '0', '9'))
				) {
					if (characterInRange(character, '�', '�')) {
						convertedText += 'A';
					}
					else if (character == '�') {
						convertedText += "AE";
					}
					else if (character == '�') {
						convertedText += "C";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'E';
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'I';
					}
					else if (character == '�') {
						convertedText += "D";
					}
					else if (character == '�') {
						convertedText += "N";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'O';
					}
					else if (character == '�') {
						convertedText += "O";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'U';
					}
					else if (character == '�') {
						convertedText += "Y";
					}
					else if (character == '�') {
						convertedText += "Y";
					}
					else if (character == '�') {
						convertedText += "SH";
					}
					else if (character == '�') {
						convertedText += "B";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'a';
					}
					else if (character == '�') {
						convertedText += "ae";
					}
					else if (character == '�') {
						convertedText += "c";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'e';
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'i';
					}
					else if (character == '�') {
						convertedText += "o";
					}
					else if (character == '�') {
						convertedText += "n";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'o';
					}
					else if (character == '�') {
						convertedText += "o";
					}
					else if (characterInRange(character, '�', '�')) {
						convertedText += 'u';
					}
					else if (character == '�') {
						convertedText += "y";
					}
					else if (character == '�') {
						convertedText += "y";
					}
					else if (character == '�') {
						convertedText += "sh";
					}

					// Other characters
					else if (character == '�') {
						convertedText += "x";
					}
					else if (character == '�') {
						convertedText += "/";
					}
					else if (character == '�') {
						convertedText += "u";
					}
					else if (character == '�') {
						convertedText += "n";
					}
					else if (character == '�') {
						convertedText += "1";
					}
					else if (character == '�') {
						convertedText += "2";
					}
					else if (character == '�') {
						convertedText += "3";
					}
					else if (character == '�') {
						convertedText += "-";
					}
					else if (character == '�') {
						convertedText += "c";
					}
					else if (character == '�') {
						convertedText += "!";
					}
					else if (character == '�') {
						convertedText += "Y";
					}
					else if (character == '�') {
						convertedText += "a";
					}
					else if (character == '�') {
						convertedText += "o";
					}
					else if (character == '�') {
						convertedText += "?";
					}
					else if (character == '�') {
						convertedText += "(C)";
					}
					else if (character == '�') {
						convertedText += "(R)";
					}
					else if (character == '�') {
						convertedText += "1/4";
					}
					else if (character == '�') {
						convertedText += "1/2";
					}
					else if (character == '�') {
						convertedText += "3/4";
					}
					else if (character == '�') {
						convertedText += "*";
					}
					else {
						convertedText += character;
					}
				}
				else {
					convertedText += character;
				}
			}
		}
		
		return convertedText;
	}
	
	
	public static boolean characterInRange(char character, char startRange, char endRange) {
		int characterValue = (int) character;
		return ((characterValue >= (int) startRange) && (characterValue <= (int) endRange));
	}
	
	
	public static void main(String[] args) {
		System.out.println(DrugMappingStringUtilities.convertToStandardCharacters("CARBOMEER 974P"));
		System.out.println(DrugMappingStringUtilities.modifyName("CARBOMEER 974P"));
	}

}
