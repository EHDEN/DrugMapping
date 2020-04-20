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
		private char[]	peekBuffer = null;
		private int bufferPosition = 0;
		private int numberRead;
		private int peekNumberRead;
		private List<String> nextLine;
		private long lineNr = 1L;

		public CSVFileIterator() {
			try {
				nextLine = readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (nextLine == null) {
				EOF = true;
				try {
					streamReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		public boolean hasNext() {
			return !EOF;
		}

		public List<String> next() {
			List<String> line = nextLine;
			try {
				nextLine = readLine();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (nextLine == null) {
				EOF = true;
				try {
					streamReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return line;
		}

		public void remove() {
			System.err.println("Unimplemented method 'remove' called");
		}
		
		private List<String> readLine() throws IOException {
			int columnNr = 0;
			boolean delimitedText = false;
			boolean doubleTextDelimiter = false;
			List<String> line = new ArrayList<String>();
			if (numberRead != -1) {
				line.add("");
				boolean EOF = false;
				boolean EOL = false;
				while ((!EOF) && (!EOL)) {
					if (bufferPosition == numberRead) {
						try {
							if (peekBuffer == null) {
								numberRead = streamReader.read(buffer, 0, BUFFERSIZE);
							}
							else {
								buffer = peekBuffer;
								numberRead = peekNumberRead;
								peekBuffer = null;
							}
							if (numberRead == -1) {
								EOF = true;
								break;
							}
							bufferPosition = 0;
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if ((textDelimiter != ((char) 0)) && (buffer[bufferPosition] == textDelimiter)) {
						if (delimitedText) {
							if (doubleTextDelimiter) {
								doubleTextDelimiter = false;
							}
							else if (peekNextChar() == textDelimiter) {
								doubleTextDelimiter = true;
								line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]); // Add textDelimiter to line
							}
							else if ((peekNextChar() == ((char) 0)) || (peekNextChar() == delimiter) || (peekNextChar() == '\r') || (peekNextChar() == '\n')) {
								delimitedText = false;
							}
							else {
								throw new IOException("Characters following field closing quote in line " + Long.toString(lineNr));
							}
						}
						else if (line.get(columnNr).length() > 0) {
							throw new IOException("Unquoted field containing quote in line " + Long.toString(lineNr));
						}
						else {
							delimitedText = true;
						} 
					}
					else if (buffer[bufferPosition] == delimiter) {
						if (delimitedText) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						else {
							line.add("");
							columnNr++;
							delimitedText = false;
						}
					}
					else if (buffer[bufferPosition] == '\r') {
						if (delimitedText) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
					}
					else if (buffer[bufferPosition] == '\n') {
						if (delimitedText) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						else {
							EOL = true;
							lineNr++;
						}
					}
					else {
						line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
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
		
		private Character peekNextChar() {
			char character = (char) 0;
			if ((bufferPosition + 1) == numberRead) {
				try {
					if (peekBuffer == null) {
						peekBuffer = new char[BUFFERSIZE];
						peekNumberRead = streamReader.read(peekBuffer, 0, BUFFERSIZE);
					}
					if (peekNumberRead != -1) {
						character = peekBuffer[0];
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				character = buffer[bufferPosition + 1];
			}
			return character;
		}
	}

	public Iterator<List<String>> iterator() {
		return new CSVFileIterator();
	}

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}

	public char getDelimiter() {
		return delimiter;
	}
}
