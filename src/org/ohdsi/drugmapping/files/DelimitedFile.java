package org.ohdsi.drugmapping.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.ohdsi.drugmapping.utilities.DrugMappingStringUtilities;


public class DelimitedFile implements Iterable<List<String>> {
	private static char DEFAULT_DELIMITER      = ',';
	private static char DEFAULT_TEXT_DELIMITER = '"';
	
	protected InputStreamReader	streamReader;
	public boolean EOF = false;
	
	private InputStream inputStream = null; 
	private String fileName = null;
	
	private PrintWriter outputWriter = null;

	private char delimiter = DEFAULT_DELIMITER;
	private char textDelimiter = DEFAULT_TEXT_DELIMITER;
	private String delimiterString = Character.toString(DEFAULT_DELIMITER);
	private String textDelimiterString = Character.toString(DEFAULT_TEXT_DELIMITER);
	private String charSet = null; 
	

	public DelimitedFile(String fileName) {
		this(fileName, DEFAULT_DELIMITER, DEFAULT_TEXT_DELIMITER, null);
	}

	
	public DelimitedFile(String fileName, char delimiter) {
		this(fileName, delimiter, DEFAULT_TEXT_DELIMITER, null);
	}

	
	public DelimitedFile(String fileName, char delimiter, char textDelimiter) {
		this(fileName, delimiter, textDelimiter, null);
	}

	
	public DelimitedFile(String fileName, String charSet) {
		this(fileName, DEFAULT_DELIMITER, DEFAULT_TEXT_DELIMITER, charSet);
	}

	
	public DelimitedFile(String fileName, char delimiter, String charSet) {
		this(fileName, delimiter, DEFAULT_TEXT_DELIMITER, null);
	}

	
	public DelimitedFile(String fileName, char delimiter, char textDelimiter, String charSet) {
		this.fileName = fileName;
		this.delimiter = delimiter;
		this.textDelimiter = textDelimiter;
		this.charSet = charSet;
		
		delimiterString = Character.toString(delimiter);
		textDelimiterString = Character.toString(textDelimiter);
	}

	
	public DelimitedFile(InputStream inputStream, char delimiter) {
		this(inputStream, delimiter, DEFAULT_TEXT_DELIMITER, null);
	}

	
	public DelimitedFile(InputStream inputStream, char delimiter, char textDelimiter) {
		this(inputStream, delimiter, textDelimiter, null);
	}

	
	public DelimitedFile(InputStream inputStream, String charSet) {
		this(inputStream, DEFAULT_DELIMITER, DEFAULT_TEXT_DELIMITER, null);
	}

	
	public DelimitedFile(InputStream inputStream, char delimiter, String charSet) {
		this(inputStream, delimiter, DEFAULT_TEXT_DELIMITER, null);
	}

	
	public DelimitedFile(InputStream inputStream, char delimiter, char textDelimiter, String charSet) {
		this.inputStream = inputStream;
		this.delimiter = delimiter;
		this.textDelimiter = textDelimiter;
		this.charSet = charSet;
	}
	
	
	public String getFileName() {
		return fileName;
	}
	
	
	public boolean openForReading() {
		boolean result = false;
		try {
			if ((inputStream == null) && (fileName != null)) {
				inputStream = new FileInputStream(fileName);
			}
			if (inputStream != null) {
				if (charSet != null) {
					streamReader = new InputStreamReader(inputStream, charSet);
				}
				else {
					streamReader = new InputStreamReader(inputStream);
				}
				result = true;
			}
		} catch (FileNotFoundException e) {
			result = false;
		} catch (UnsupportedEncodingException e) {
			result = false;
		}
		return result;
	}
	
	
	public Iterator<List<String>> getIterator() {
		return iterator();
	}
	
	
	public boolean openForWriting() {
		boolean result = false;
		File file = new File(fileName);
		try {
			outputWriter = new PrintWriter(file);
			result = true;
		} catch (FileNotFoundException e) {
			result = false;
		}
		return result;
	}
	
	
	public void writeRecord(List<String> record) {
		String recordString = "";
		for (String column : record) {
			recordString += (recordString.equals("") ? "" : delimiterString) + DrugMappingStringUtilities.escapeFieldValue(column, delimiterString, textDelimiterString);
		}
		outputWriter.println(recordString);
	}
	
	
	public void closeForWriting() {
		outputWriter.close();
	}

	
	private class DelimitedFileIterator implements Iterator<List<String>> {
		private static final int BUFFERSIZE = 4194304; // 4 MB
		private char[]	buffer = new char[BUFFERSIZE]; // 4 MB
		private int bufferPosition = 0;
		private int numberRead;
		private List<String> nextLine;
		private long recordNr = 0L;
		private long lineNr = 1L;

		
		public DelimitedFileIterator() {
			try {
				nextLine = readLine();
			} catch (IOException readLineException) {
				System.out.println(readLineException.getMessage());
			}
			if (nextLine == null) {
				EOF = true;
				try {
					streamReader.close();
				} catch (IOException streamReaderException) {
					System.out.println(streamReaderException.getMessage());
				}
			}

		}
		

		public boolean hasNext() {
			return !EOF;
		}

		
		public List<String> next() throws NoSuchElementException {
			List<String> line = nextLine;
			try {
				nextLine = readLine();
			} catch (IOException readLineException) {
				throw new NoSuchElementException(readLineException.getMessage());
			}
			if (nextLine == null) {
				EOF = true;
				try {
					streamReader.close();
				} catch (IOException streamReaderException) {
					throw new NoSuchElementException(streamReaderException.getMessage());
				}
			}

			return line;
		}

		
		public void remove() {
			System.err.println("Unimplemented method 'remove' called");
		}
		
		
		private List<String> readLine() throws IOException {
			int columnNr = 0;
			long charPosition = 0L;
			boolean delimitedText = false;
			boolean wasDelimitedText = false;
			boolean textDelimiterInValue = false;
			List<String> line = new ArrayList<String>();
			if (numberRead != -1) {
				recordNr++;
				line.add("");
				boolean EOF = false;
				boolean EOL = false;
				while ((!EOF) && (!EOL)) {
					if (bufferPosition == numberRead) {
						try {
							numberRead = streamReader.read(buffer, 0, BUFFERSIZE);
						} catch (IOException streamReaderException) {
							throw new IOException(streamReaderException);
						}
						if (numberRead == -1) {
							if (delimitedText) {
								throw new IOException("Unclosed delimited field  (text delimiter = " + textDelimiter + ") at end of file");
							}
							EOF = true;
							break;
						}
						bufferPosition = 0;
					}
					charPosition++;
					if ((textDelimiter != ((char) 0)) && (buffer[bufferPosition] == textDelimiter)) {
						if (textDelimiterInValue) { 
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
							textDelimiterInValue = false;
							delimitedText = false;
						}
						else if (delimitedText) { // End of delimited text value or first of double text delimiter inside delimited value
							delimitedText = false;
							wasDelimitedText = true;
						}
						else if (wasDelimitedText) { // Add textDelimiter that was contained in value 
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]); 
							delimitedText = true;
							wasDelimitedText = false;
						}
						else if (line.get(columnNr).length() > 0) {
							textDelimiterInValue = true;
						}
						else {
							delimitedText = true;
						} 
					}
					else if (buffer[bufferPosition] == delimiter) {
						textDelimiterInValue = false;
						if (delimitedText) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						else {
							line.add("");
							columnNr++;
							delimitedText = false;
						}
						wasDelimitedText = false;
					}
					else if (buffer[bufferPosition] == '\r') {
						textDelimiterInValue = false;
						if (delimitedText) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						wasDelimitedText = false;
					}
					else if (buffer[bufferPosition] == '\n') {
						textDelimiterInValue = false;
						if (delimitedText) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						else {
							EOL = true;
						}
						wasDelimitedText = false;
						lineNr++;
						charPosition = 0L;
					}
					else {
						if (textDelimiterInValue) {
							throw new IOException("Not-doubled text delimiter (" + textDelimiter + ") in record " + Long.toString(recordNr) + " at line " + Long.toString(lineNr) + " position " + Long.toString(charPosition - 1));
						}
						else if (wasDelimitedText) {
							throw new IOException("Characters following field closing text delimiter (" + textDelimiter + ") in record " + Long.toString(recordNr) + " at line " + Long.toString(lineNr) + " position " + Long.toString(charPosition));
						}
						else {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						textDelimiterInValue = false;
					}
					bufferPosition++;
				}
				if ((line.size() == 1) && (line.get(0).length() == 0)) {
					line = null;
				}
			}
			else {
				line = null;
			}

			return line;
		}
		
		
		public void setRecordNr(long newValue) {
			recordNr = newValue;
		}
	}

	
	public Iterator<List<String>> iterator() {
		return new DelimitedFileIterator();
	}
	

	public Iterator<List<String>> iteratorWithHeader() {
		DelimitedFileIterator iterator = new DelimitedFileIterator();
		iterator.setRecordNr(0);
		return iterator;
	}
	

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}
	

	public char getDelimiter() {
		return delimiter;
	}
	
	
	public String getCharSet() {
		return charSet;
	}
}
