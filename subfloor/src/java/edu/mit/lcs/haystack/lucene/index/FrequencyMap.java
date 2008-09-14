package edu.mit.lcs.haystack.lucene.index;

/**
 * Copyright 2004 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.Map;
import java.util.Iterator;

public class FrequencyMap {
	protected Map m;

	Object primaryFieldValue;

	public FrequencyMap(Map m, Object primaryFieldValue) {
		this.m = m;
		this.primaryFieldValue = primaryFieldValue;
	}

	public Object getPrimaryFieldValue() {
		return primaryFieldValue;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("primary = " + getPrimaryFieldValue().toString() + ";");

		Iterator fields = m.entrySet().iterator();

		while (fields.hasNext()) {
			Map.Entry me = (Map.Entry) fields.next();
			String fieldName = (String) me.getKey();
			Map fm = (Map) me.getValue();

			sb.append(fieldName + " {");

			Iterator words = fm.entrySet().iterator();
			while (words.hasNext()) {
				Map.Entry wme = (Map.Entry) words.next();

				String word = (String) wme.getKey();
				Integer i = (Integer) wme.getValue();

				sb.append(word + " -> " + i.toString());
				if (words.hasNext())
					sb.append(", ");
			}
			sb.append("}");

			if (fields.hasNext())
				sb.append(" ");
		}

		return sb.toString();
	}

	public Map getMap() {
		return m;
	}
}