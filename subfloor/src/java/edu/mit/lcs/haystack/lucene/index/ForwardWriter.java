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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.OutputStream;

final class ForwardWriter {
	OutputStream docFwd = null;

	OutputStream fwdIndex = null;

	OutputStream fwURI = null;

	String primaryField;

	public ForwardWriter(Directory directory, String segment,
			String primaryField) throws IOException, SecurityException {
		docFwd = directory.createFile(segment + ".fwd");
		fwdIndex = directory.createFile(segment + ".fwi");
		fwURI = directory.createFile(segment + ".fwu");

		docFwd.writeString(primaryField);

		this.primaryField = primaryField;
	}

	public void writeDocument(FrequencyMap fq) throws IOException {
		Map map = fq.getMap();

		fwdIndex.writeLong((long) docFwd.getFilePointer());

		// URI, PTR, document #
		fwURI.writeString(fq.getPrimaryFieldValue().toString());
		fwURI.writeLong(docFwd.getFilePointer());
		fwURI.writeLong((fwdIndex.getFilePointer() - 8L) / 8L);

		docFwd.writeInt(map.size());

		Iterator fields = map.entrySet().iterator();
		while (fields.hasNext()) {
			Map.Entry fme = (Map.Entry) fields.next();
			String fieldName = (String) fme.getKey();
			Map mapWords = (Map) fme.getValue();

			if (fieldName.equals(this.primaryField)) {
				// make sure that mapWords contains only 1 entry -- the primary
				// field
				if (mapWords.size() != 1)
					throw new RuntimeException("mapWords.size() != 1");

				String s = (String) ((Map.Entry) mapWords.entrySet().iterator()
						.next()).getKey();
				if (!s.equals(fq.getPrimaryFieldValue()))
					throw new RuntimeException("hashTable != primaryFieldValue");
			}

			// print out forward index information for current field
			docFwd.writeString(fieldName);
			docFwd.writeInt(mapWords.size());

			Iterator words = mapWords.entrySet().iterator();
			while (words.hasNext()) {
				Map.Entry wme = (Map.Entry) words.next();
				String tok = (String) wme.getKey();
				Integer i = (Integer) wme.getValue();

				docFwd.writeString(tok);
				docFwd.writeInt(i.intValue());
			}
		}
	}

	public void close() throws IOException {
		if (docFwd != null)
			docFwd.close();
		if (fwdIndex != null)
			fwdIndex.close();
		if (fwURI != null)
			fwURI.close();
	}

}