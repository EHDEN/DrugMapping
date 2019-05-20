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
package org.ohdsi.databases;

import java.util.HashMap;
import java.util.Map;

public class QueryParameters {
	private Map<String, String> key2Value = new HashMap<String, String>();
	public void set(String key, String value){
		key2Value.put(key, value);
	}
	
	public void set(String key, int value){
		key2Value.put(key, Integer.toString(value));
	}
	
	protected Map<String, String> getMap() {
		return key2Value;
	}
	
}
