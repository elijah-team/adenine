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
import java.io.File;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import edu.mit.lcs.haystack.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;

public class ForwardIndexWriter extends IndexWriter {
	protected String primaryField;

	public ForwardIndexWriter(String primaryField, String path, Analyzer a,
			boolean create) throws IOException {
		this(primaryField, FSDirectory.getDirectory(path, create), a, create);
	}

	public ForwardIndexWriter(String primaryField, File path, Analyzer a,
			boolean create) throws IOException {
		this(primaryField, FSDirectory.getDirectory(path, create), a, create);
	}

	public ForwardIndexWriter(String primaryField, Directory d, Analyzer a,
			final boolean create) throws IOException {
		super(d, a, create);

		this.primaryField = primaryField;
	}

	/** Adds a document to this index.*/
	public void addDocument(Document doc) throws IOException {
		addDocumentInternal(doc, true, primaryField);
	}

}