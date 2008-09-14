package edu.mit.lcs.haystack.lucene.index;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import edu.mit.lcs.haystack.lucene.document.Document;
import edu.mit.lcs.haystack.lucene.document.Field;
import edu.mit.lcs.haystack.lucene.document.ForwardDocument;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.InputStream;

import java.io.IOException;
/* HAYSTACK END */
/**
 * Class responsible for access to stored document fields.
 * 
 * It uses &lt;segment&gt;.fdt and &lt;segment&gt;.fdx; files.
 * 
 * @version $Id: FieldsReader.java,v 1.4 2004/07/10 00:41:40 yks Exp $
 */
final class FieldsReader {
  private FieldInfos fieldInfos;
  private InputStream fieldsStream;
  private InputStream indexStream;
  private int size;

  FieldsReader(Directory d, String segment, FieldInfos fn) throws IOException {
    fieldInfos = fn;

    fieldsStream = d.openFile(segment + ".fdt");
    indexStream = d.openFile(segment + ".fdx");

    size = (int)(indexStream.length() / 8);
  }

  final void close() throws IOException {
    fieldsStream.close();
    indexStream.close();
  }

  final int size() {
    return size;
  }

  final Document doc(int n) throws IOException {
    indexStream.seek(n * 8L);
    long position = indexStream.readLong();
    fieldsStream.seek(position);

    /* 1.4 code */
    // Document doc = new Document();

    /* HAYSTACK */
    // instanciate a special Forward Document if 
    // a forwardReader exists
    Document doc;
    if (forwardReader != null) {
    	doc = new ForwardDocument(forwardReader.getFrequencyMap(n));
    } else {
    	doc = new Document();
    }
    /* HAYSTACK END */
    
    int numFields = fieldsStream.readVInt();
    for (int i = 0; i < numFields; i++) {
      int fieldNumber = fieldsStream.readVInt();
      FieldInfo fi = fieldInfos.fieldInfo(fieldNumber);

      byte bits = fieldsStream.readByte();

      doc.add(new Field(fi.name,		  // name
			fieldsStream.readString(), // read value
			true,			  // stored
			fi.isIndexed,		  // indexed
			(bits & 1) != 0, fi.storeTermVector)); // vector
    }

    return doc;
  }
  
  /* HAYSTACK START */
  private ForwardReader forwardReader = null;
  
  /**
   * Special constructor that takes a forwardReader.
   **/
  public FieldsReader(Directory d, String segment, FieldInfos fn, ForwardReader forwardReader) throws IOException {
    fieldInfos = fn;

    fieldsStream = d.openFile(segment + ".fdt");
    indexStream = d.openFile(segment + ".fdx");

    size = (int)(indexStream.length() / 8);
    
    this.forwardReader = forwardReader;
  }

  /** 
   * returns the document object corresponding to a unique field
   * index value.
   **/
  final Document doc(Object uniqueID) throws IOException {
  	return doc(getDocumentNumber(uniqueID));
  }

  /**
   * retrieves document number corresponding to the "primary" or
   * unique field
   **/
  final int getDocumentNumber(Object uniqueID) throws IOException {
  		if (forwardReader == null)
  			throw new RuntimeException(
  					"You must use a forward index to access this feature");

  		long docNum = forwardReader.getDocumentNumber(uniqueID);

  		return (int) docNum;
  }
  /* HAYSTACK END */
}
