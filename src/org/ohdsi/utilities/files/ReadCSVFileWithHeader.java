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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReadCSVFileWithHeader implements Iterable<Row> {
	private InputStream	inputstream = null;
	private char		delimiter	= ',';
	private char		textDelimiter = '"';
	private String 		charSet = "ISO-8859-1";
	
	private RowIterator rowIterator = null;

	public ReadCSVFileWithHeader(String filename, char delimiter) {
		this(filename);
		this.delimiter = delimiter;
	}

	public ReadCSVFileWithHeader(String filename, char delimiter, char textDelimiter) {
		this(filename);
		this.delimiter = delimiter;
		this.textDelimiter = textDelimiter;
	}

	public ReadCSVFileWithHeader(String filename, char delimiter, char textDelimiter, String charSet) {
		this(filename);
		this.delimiter = delimiter;
		this.textDelimiter = textDelimiter;
		this.charSet = charSet;
	}
	
	public ReadCSVFileWithHeader(String filename, String charSet) {
		this(filename);
		this.charSet = charSet;
	}
	
	public ReadCSVFileWithHeader(String filename, char delimiter, String charSet) {
		this(filename);
		this.delimiter = delimiter;
		this.charSet = charSet;
	}

	public ReadCSVFileWithHeader(String filename) {
		try {
			inputstream = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
			inputstream = null;
		}
	}

	public ReadCSVFileWithHeader(InputStream inputstream) {
		this.inputstream = inputstream;
	}
	
	public ReadCSVFileWithHeader(InputStream inputstream, String charSet) {
		this.charSet = charSet;
		this.inputstream = inputstream;
	}
	
	public boolean isOpen() {
		return (inputstream != null);
	}
	
	public Set<String> getColumns() {
		Set<String> columns = null;
		if (rowIterator != null) {
			columns = rowIterator.getColumns();
		}
		return columns;
	}

	@Override
	public Iterator<Row> iterator() {
		rowIterator = new RowIterator();
		return rowIterator;
	}

	public class RowIterator implements Iterator<Row> {

		private Iterator<List<String>>	iterator;
		private Map<String, Integer>	fieldName2ColumnIndex;

		public RowIterator() {
			iterator = new ReadCSVFile(inputstream, delimiter, textDelimiter, charSet).iteratorWithHeader();
			fieldName2ColumnIndex = new HashMap<String, Integer>();
			for (String header : iterator.next())
				fieldName2ColumnIndex.put(header, fieldName2ColumnIndex.size());
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Row next() {
			return new Row(iterator.next(), fieldName2ColumnIndex);
		}

		@Override
		public void remove() {
			throw new RuntimeException("Remove not supported");
		}
		
		public Set<String> getColumns() {
			return fieldName2ColumnIndex.keySet();
		}

	}
	
	
	public static void main(String[] args) {
		String fileName = "D:/Temp/Test.csv";
		ReadCSVFileWithHeader csvFileReader = new ReadCSVFileWithHeader(fileName, ',', '"');
		Iterator<Row> csvFile = csvFileReader.iterator();
		List<Row> rows = new ArrayList<Row>();
		while (csvFile.hasNext()) {
			rows.add(csvFile.next());
		}
		System.out.println("Finished");
	}
}
