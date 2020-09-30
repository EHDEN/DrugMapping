package org.ohdsi.drugmapping.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	
	
	public static String unEscapeFieldValue(String value) {
		return unEscapeFieldValue(value, ",", "\"");
	}
	
	
	public static String unEscapeFieldValue(String value, String fieldDelimiter, String textQualifier) {
		if (value == null) {
			value = "";
		}
		else if (value.startsWith(textQualifier) && value.endsWith(textQualifier) && value.length() > 1) {
			value = value.substring(1, value.length() - 1).replaceAll(textQualifier + textQualifier, textQualifier);
		}
		return value;
	}
	
	
	public static List<String> intelligentSplit(String string, char separator, char textQualifier) throws Exception {
		List<String> split = new ArrayList<String>();
		
		if (string.length() > 0) {
			boolean quoted = false;
			String segment = "";
			int characterNr = 0;
			char nextCharacter = string.charAt(0);
			while (characterNr < string.length()) {
				char character = nextCharacter;
				nextCharacter = (characterNr + 1) < string.length() ? string.charAt(characterNr + 1) : '\0';
				
				if (quoted) {
					if (character == textQualifier) {
						if (nextCharacter != '\0') {
							if (nextCharacter == textQualifier) {
								segment += textQualifier;
							}
							else {
								quoted = false;
							}
						}
						else {
							throw new Exception("Unexpected end of string");
						}
					}
					else {
						segment += character;
					}
				}
				else {
					if (character == separator) {
						split.add(segment);
						segment = "";
					}
					else if (character == textQualifier) {
						quoted = true;
					}
					else {
						segment += character;
					}
				}
				characterNr++;
			}
			split.add(segment);
		}
		else {
			split.add("");
		}
		
		return split;
	}
	
	
	public static String standardizedName(String name) {
		
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

	
	public static List<String> generateMatchingNames(String name, String englishName) {
		List<String> matchingNames = new ArrayList<String>();
		Set<String> uniqueNames = new HashSet<String>();

		name = DrugMappingStringUtilities.removeExtraSpaces(name).toUpperCase();
		englishName = DrugMappingStringUtilities.removeExtraSpaces(englishName).toUpperCase();
		
		if (uniqueNames.add(name)) {
			matchingNames.add("SourceTerm: " + name);
		}
		if ((englishName != null) && (!englishName.equals("")) && uniqueNames.add(englishName)) {
			matchingNames.add("SourceTerm (Translated): " + englishName);
		}
		for (Integer length = 20; length > 0; length--) {
			String reducedName = getReducedName(name, length);
			if (reducedName != null) {
				if (uniqueNames.add(reducedName)) {
					matchingNames.add("First " + length + " words from SourceTerm: " + reducedName);
				}
				if (uniqueNames.add(reducedName + " EXTRACT")) {
					matchingNames.add("First " + length + " words + \" EXTRACT\" from SourceTerm: " + reducedName + " EXTRACT");
				}
			}
			if ((englishName != null) && (!englishName.equals(""))) {
				reducedName = getReducedName(englishName, length);
				if (reducedName != null) {
					if (uniqueNames.add(reducedName)) {
						matchingNames.add("First " + length + " words from SourceTerm (Translated): " + reducedName);
					}
					if (uniqueNames.add(reducedName + " EXTRACT")) {
						matchingNames.add("First " + length + " words + \" EXTRACT\" from SourceTerm (Translated): " + reducedName + " EXTRACT");
					}
				}
			}
		}

		return matchingNames;
	}
	
	
	public static String getReducedName(String name, int nrWords) {
		name += " ";
		String reducedName = null;
		if (nrWords > 0) {
			int delimiterCount = 0;
			boolean lastCharDelimiter = false;
			for (int charNr = 1; charNr < name.length(); charNr++) {
				if (" -[](),&+:;\"'/\\{}*%".contains(name.substring(charNr, charNr + 1))) {
					if (!lastCharDelimiter) {
						delimiterCount++;
					}
					lastCharDelimiter = true;
				}
				else {
					lastCharDelimiter = false;
				}
				if (delimiterCount == nrWords) {
					reducedName = name.substring(0, charNr);
					break;
				}
			}
		}
		
		return reducedName;
	}
	
	
	public static String cleanString(String string) {
		return string == null ? null : removeExtraSpaces(string.trim().replaceAll("\r\n", " ").replaceAll("\n", " ").replaceAll("\r", " ").replaceAll("\t", " ").replaceAll("ß", "SS").replaceAll("•", "*"));
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
					if (characterInRange(character, 'À', 'Å')) {
						convertedText += 'A';
					}
					else if (character == 'Æ') {
						convertedText += "AE";
					}
					else if (character == 'Ç') {
						convertedText += "C";
					}
					else if (characterInRange(character, 'È', 'Ë')) {
						convertedText += 'E';
					}
					else if (characterInRange(character, 'Ì', 'Ï')) {
						convertedText += 'I';
					}
					else if (character == 'Ð') {
						convertedText += "D";
					}
					else if (character == 'Ñ') {
						convertedText += "N";
					}
					else if (characterInRange(character, 'Ò', 'Ö')) {
						convertedText += 'O';
					}
					else if (character == 'Ø') {
						convertedText += "O";
					}
					else if (characterInRange(character, 'Ù', 'Ü')) {
						convertedText += 'U';
					}
					else if (character == 'Ý') {
						convertedText += "Y";
					}
					else if (character == 'Ý') {
						convertedText += "Y";
					}
					else if (character == 'Þ') {
						convertedText += "SH";
					}
					else if (character == 'ß') {
						convertedText += "B";
					}
					else if (characterInRange(character, 'à', 'å')) {
						convertedText += 'a';
					}
					else if (character == 'æ') {
						convertedText += "ae";
					}
					else if (character == 'ç') {
						convertedText += "c";
					}
					else if (characterInRange(character, 'è', 'ë')) {
						convertedText += 'e';
					}
					else if (characterInRange(character, 'ì', 'ï')) {
						convertedText += 'i';
					}
					else if (character == 'ð') {
						convertedText += "o";
					}
					else if (character == 'ñ') {
						convertedText += "n";
					}
					else if (characterInRange(character, 'ò', 'ö')) {
						convertedText += 'o';
					}
					else if (character == 'ø') {
						convertedText += "o";
					}
					else if (characterInRange(character, 'ù', 'ü')) {
						convertedText += 'u';
					}
					else if (character == 'ý') {
						convertedText += "y";
					}
					else if (character == 'ÿ') {
						convertedText += "y";
					}
					else if (character == 'þ') {
						convertedText += "sh";
					}

					// Other characters
					else if (character == '×') {
						convertedText += "x";
					}
					else if (character == '÷') {
						convertedText += "/";
					}
					else if (character == 'µ') {
						convertedText += "u";
					}
					else if (character == '¶') {
						convertedText += "n";
					}
					else if (character == '¹') {
						convertedText += "1";
					}
					else if (character == '²') {
						convertedText += "2";
					}
					else if (character == '³') {
						convertedText += "3";
					}
					else if (character == '–') {
						convertedText += "-";
					}
					else if (character == '¢') {
						convertedText += "c";
					}
					else if (character == '¡') {
						convertedText += "!";
					}
					else if (character == '¥') {
						convertedText += "Y";
					}
					else if (character == 'ª') {
						convertedText += "a";
					}
					else if (character == 'º') {
						convertedText += "o";
					}
					else if (character == '¿') {
						convertedText += "?";
					}
					else if (character == '©') {
						convertedText += "(C)";
					}
					else if (character == '®') {
						convertedText += "(R)";
					}
					else if (character == '¼') {
						convertedText += "1/4";
					}
					else if (character == '½') {
						convertedText += "1/2";
					}
					else if (character == '¾') {
						convertedText += "3/4";
					}
					else if (character == '•') {
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
	
	
	public static String sortWords(String string) {
		String splitCharacters = " ,.-()[]/\\*+&:'\"<>_=|{};#$%^@^~`\t\n\r";
		List<String> words = new ArrayList<String>();
		String word = "";
		for (int charNr = 0; charNr < string.length(); charNr++) {
			String currentCharacter = string.substring(charNr, charNr + 1);
			if (splitCharacters.contains(currentCharacter)) {
				if (!word.equals("")) {
					words.add(word);
					word = "";
				}
			}
			else {
				word += currentCharacter;
			}
		}
		if (!word.equals("")) {
			words.add(word);
		}
		Collections.sort(words);
		String result = "";
		for (String sortedWord : words) {
			result += (result.equals("") ? "" : " ") + sortedWord;
		}
		return result;
	}
	
	
	public static boolean characterInRange(char character, char startRange, char endRange) {
		int characterValue = (int) character;
		return ((characterValue >= (int) startRange) && (characterValue <= (int) endRange));
	}
	
	
	public static String removeLeadingZeros(String string) {
		while ((!string.equals("")) && string.substring(0, 1).equals("0")) {
			string = string.substring(1);
		}
		return string;
	}
	
	
	public static void main(String[] args) {
		String test = "FOLLICLE STIMULATI&NG H/ORM\\ONE 75 UNT INJECT\tABLE SOLU?TION";
		System.out.println(test);
		System.out.println(DrugMappingStringUtilities.sortWords(test));
	}

}
