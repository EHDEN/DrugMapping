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
		private int bufferPosition = 0;
		private int numberRead;
		private List<String> nextLine;

		public CSVFileIterator() {
			nextLine = readLine();
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
			nextLine = readLine();
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
		
		private List<String> readLine() {
			int columnNr = 0;
			boolean quoted = false;
			List<String> line = new ArrayList<String>();
			if (numberRead != -1) {
				line.add("");
				boolean EOF = false;
				boolean EOL = false;
				while ((!EOF) && (!EOL)) {
					if (bufferPosition == numberRead) {
						try {
							numberRead = streamReader.read(buffer, 0, BUFFERSIZE);
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
						if ((!quoted) && line.get(columnNr).length() > 0) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						quoted = !quoted; 
					}
					else if (buffer[bufferPosition] == delimiter) {
						if (quoted) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						else {
							line.add("");
							columnNr++;
						}
					}
					else if (buffer[bufferPosition] == '\r') {
						if (quoted) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
					}
					else if (buffer[bufferPosition] == '\n') {
						if (quoted) {
							line.set(columnNr, line.get(columnNr) + buffer[bufferPosition]);
						}
						else {
							EOL = true;
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
