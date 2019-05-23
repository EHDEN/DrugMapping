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
package org.ohdsi.utilities.collections;

/**
 * Basically a string, but when comparing (and therefore when sorting) and both are a number, they are 
 * treated as numbers. So new StringOrInteger("100").compareTo(new StringOrInteger("2") > 0
 * @author MSCHUEMI
 *
 */
public class StringOrLong implements Comparable<StringOrLong>{

	private String id;
	
	public static StringOrLong empty = new StringOrLong("");
	
	public StringOrLong(String id){
		this.id = id;
	}
	
	public StringOrLong(int id) {
		this.id = Integer.toString(id);
	}

	public String toString(){
		return id;
	}
	
	public boolean equals(Object o){
		if (o instanceof String)
			return id.equals(o);
		else if (o instanceof StringOrLong)
			return id.equals(((StringOrLong)o).id);
		else
			return false;
	}

	@Override
	public int compareTo(StringOrLong o) {
		if (isNumber(id)){
			if (isNumber(o.id))
				return efficientNumberCompare(id,o.id);
			else
				return 1;
		} else {
			if (isNumber(o.id))
			  return -1;
		  else
		  	return id.compareTo(o.id);
		}
	}
	
  private int efficientNumberCompare(String value1, String value2){
  	if (value1.length() > value2.length())
  		return 1;
  	else if (value1.length() < value2.length())
  		return -1;
  	else 
  		return value1.compareTo(value2);
  }
	
	private static boolean isNumber(String string){
		if (string.length() == 0)
			return false;
		for (int i = 0; i < string.length(); i++)
			if (!Character.isDigit(string.charAt(i)))
				return false;
		return true;
	}
}
