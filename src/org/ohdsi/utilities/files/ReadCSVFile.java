/*******************************************************************************
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.utilities.files;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ReadCSVFile implements Iterable<List<String>> {
	protected InputStreamReader	streamReader;
	//protected BufferedReader	bufferedReader;
	public boolean				EOF			  = false;
	private char				delimiter	  = ',';
	private char				textDelimiter = '"';
	private String				charSet		  = "ISO-8859-1";

	public ReadCSVFile(String filename, char delimiter) {
		this(filename);
		this.delimiter = delimiter;
	}
	
	public ReadCSVFile(String filename, String	charSet) {
		this.charSet = charSet;
		try {
			FileInputStream textFileStream = new FileInputStream(filename);
			streamReader = new InputStreamReader(textFileStream, charSet);
			//bufferedReader = new BufferedReader(new InputStreamReader(textFileStream, charSet));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}		
	}
	
	public ReadCSVFile(String filename, char delimiter, String	charSet) {
		this(filename, charSet);
		this.delimiter = delimiter;
	}

	public ReadCSVFile(String filename) {
		try {
			FileInputStream textFileStream = new FileInputStream(filename);
			streamReader = new InputStreamReader(textFileStream, charSet);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public ReadCSVFile(InputStream inputstream, char delimiter) {
		this(inputstream);
		this.delimiter = delimiter;
	}

	public ReadCSVFile(InputStream inputstream, char delimiter, char textDelimiter) {
		this(inputstream);
		this.delimiter = delimiter;
		this.textDelimiter = textDelimiter;
	}

	public ReadCSVFile(InputStream inputstream) {
		try {
			streamReader = new InputStreamReader(inputstream, charSet);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public ReadCSVFile(InputStream inputstream, String charSet) {
		this.charSet = charSet;
		try {
			streamReader = new InputStreamReader(inputstream, charSet);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public ReadCSVFile(InputStream inputstream, char delimiter, String charSet) {
		this(inputstream, charSet);
		this.delimiter = delimiter;
	}

	public ReadCSVFile(InputStream inputstream, char delimiter, char textDelimiter, String charSet) {
		this(inputstream, charSet);
		this.delimiter = delimiter;
	}
	
	public Iterator<List<String>> getIterator() {
		return iterator();
	}

	private class CSVFileIterator implements Iterator<List<String>> {
		private static final int BUFFERSIZE = 4194304; // 4 MB
		private char[]	buffer = new char[BUFFERSIZE]; // 4 MB
		private int bufferPosition = 0;
		private int numberRead;
		private List<String> nextLine;
		private long recordNr = 0L;
		private long lineNr = 1L;

		public CSVFileIterator() {
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
		return new CSVFileIterator();
	}

	public Iterator<List<String>> iteratorWithHeader() {
		CSVFileIterator iterator = new CSVFileIterator();
		iterator.setRecordNr(0);
		return iterator;
	}

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}

	public char getDelimiter() {
		return delimiter;
	}
}
