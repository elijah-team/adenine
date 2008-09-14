package edu.mit.lcs.haystack.lucene;

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

import org.apache.lucene.analysis.*;
import edu.mit.lcs.haystack.lucene.document.*;
import edu.mit.lcs.haystack.lucene.index.*;
import edu.mit.lcs.haystack.lucene.document.Field;
import org.apache.lucene.store.*;

import java.util.Vector;

// TODO[vineet]: devise test for multiple segment readers

/*
 * ./jython/cachedir/packages/lucene-1.2.pkc
 * ./lib/java/cachedir/packages/lucene-1.2-rc2.pkc ./lib/java/lucene-1.2.jar
 * ./src/adenine/agents/lucene.ad
 */

public class IndexTest {
    static void _assert(boolean b) {
        if (!b) {
            throw new RuntimeException("ASSERTION FAILURE");
        }
    }

    static boolean compareTwoVectorsNoOrder(Vector v1, Vector v2) {
        // compare them. Order does not matter

        Vector cp = (Vector) v2.clone();

        for (int x = 0; x < v1.size(); x++) {
            Object o = v1.get(x);

            // find it in cp
            for (int y = 0; y < cp.size(); y++) {
                if (cp.get(y).equals(o)) {
                    cp.remove(y);
                    break;
                }
            }
        }

        if (cp.size() == 0)
            return true;
        else
            return false;
    }

    /**
     * tests the forward index writer.
     */
    public static void testDocumentWriter() {
        try {
            Runtime.getRuntime().exec("rm -rf index").waitFor();

            // URI is the primary (unique) field
            Directory dir = FSDirectory.getDirectory("index", true);
            IndexWriter iw = new ForwardIndexWriter("URI", dir, new SimpleAnalyzer(), true);
            {
                Document d = new Document();
                d.add(Field.Keyword("URI", "1"));
                d.add(Field.Text("text", "mark mark joe joe"));
                iw.addDocument(d);
            }

            {
                Document d = new Document();
                d.add(Field.Keyword("URI", "2"));
                d.add(Field.Text("text", "mark joe mel mel"));
                iw.addDocument(d);
            }

            {
                Document d = new Document();
                d.add(Field.Keyword("URI", "3"));
                d.add(Field.Text("text", "mark melanie mel melanie"));
                iw.addDocument(d);
            }
            iw.close();
            dir.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void testDocumentReader() {
        try {
            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "index", false));

                Vector v = new Vector();
                for (int i = 0; i < ir.maxDoc(); i++) {
                    Document d = ir.document(i);
                    FrequencyMap fm = d.getFrequencyMap();
                    if (fm != null) {
                        v.add(fm.toString());
                    }
                }

                Vector compare = new Vector();
                compare.add(new String(
                        "primary = 1;URI {1 -> 1} text {joe -> 2, mark -> 2}"));
                compare
                        .add(new String(
                                "primary = 2;URI {2 -> 1} text {joe -> 1, mel -> 2, mark -> 1}"));
                compare
                        .add(new String(
                                "primary = 3;URI {3 -> 1} text {melanie -> 2, mel -> 1, mark -> 1}"));

                _assert(compareTwoVectorsNoOrder(compare, v));

                ir.close();
            }

            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "index", false));

                _assert(ir.document("1").getFrequencyMap().toString().equals(
                        "primary = 1;URI {1 -> 1} text {joe -> 2, mark -> 2}"));
                _assert(ir
                        .document("2")
                        .getFrequencyMap()
                        .toString()
                        .equals(
                                "primary = 2;URI {2 -> 1} text {joe -> 1, mel -> 2, mark -> 1}"));
                _assert(ir
                        .document("3")
                        .getFrequencyMap()
                        .toString()
                        .equals(
                                "primary = 3;URI {3 -> 1} text {melanie -> 2, mel -> 1, mark -> 1}"));

                ir.close();
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void testDocumentDeleting() {
        try {
            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "index", false));
                ir.delete(1);
                ir.close();
            }

            {
                IndexWriter iw = new ForwardIndexWriter("URI", FSDirectory
                        .getDirectory("index", false), new SimpleAnalyzer(),
                        false);
                iw.optimize();
                iw.close();
            }

            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "index", false));

                Vector v = new Vector();
                for (int i = 0; i < ir.maxDoc(); i++) {
                    if (!ir.isDeleted(i)) {
                        Document d = ir.document(i);
                        FrequencyMap fm = d.getFrequencyMap();
                        if (fm != null)
                            v.add(fm.toString());
                    }
                }

                Vector compare = new Vector();
                compare.add(new String(
                        "primary = 1;URI {1 -> 1} text {joe -> 2, mark -> 2}"));
                compare
                        .add(new String(
                                "primary = 3;URI {3 -> 1} text {melanie -> 2, mel -> 1, mark -> 1}"));

                _assert(compareTwoVectorsNoOrder(compare, v));

                ir.close();
            }

            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "index", false));

                _assert(ir.document("1").getFrequencyMap().toString().equals(
                        "primary = 1;URI {1 -> 1} text {joe -> 2, mark -> 2}"));

                _assert(ir
                        .document("3")
                        .getFrequencyMap()
                        .toString()
                        .equals(
                                "primary = 3;URI {3 -> 1} text {melanie -> 2, mel -> 1, mark -> 1}"));

                ir.close();
            }

        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void testBackwardCompatability() {
        try {
            {
                Runtime.getRuntime().exec("rm -rf backindex").waitFor();

                IndexWriter iw = new IndexWriter(FSDirectory.getDirectory(
                        "backindex", true), new SimpleAnalyzer(), true);
                {
                    Document d = new Document();
                    d.add(Field.Keyword("URI", "1"));
                    d.add(Field.Text("text", "mark mark joe joe"));
                    iw.addDocument(d);
                }

                {
                    Document d = new Document();
                    d.add(Field.Keyword("URI", "2"));
                    d.add(Field.Text("text", "mark joe mel mel"));
                    iw.addDocument(d);
                }

                {
                    Document d = new Document();
                    d.add(Field.Keyword("URI", "3"));
                    d.add(Field.Text("text", "mark melanie mel melanie"));
                    iw.addDocument(d);
                }

                iw.close();
            }

            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "backindex", false));

                Vector v = new Vector();
                for (int i = 0; i < ir.maxDoc(); i++) {
                    Document d = ir.document(i);
                    FrequencyMap fm = d.getFrequencyMap();
                    if (fm != null) {
                        v.add(fm.toString());
                    }
                }

                Vector compare = new Vector();
                compare.add(new String(
                        "primary = 1;URI {1 -> 1} text {joe -> 2, mark -> 2}"));
                compare
                        .add(new String(
                                "primary = 2;URI {2 -> 1} text {joe -> 1, mel -> 2, mark -> 1}"));
                compare
                        .add(new String(
                                "primary = 3;URI {3 -> 1} text {melanie -> 2, mel -> 1, mark -> 1}"));

                _assert(compareTwoVectorsNoOrder(compare, v));

                ir.close();
            }

            {
                IndexReader ir = IndexReader.open(FSDirectory.getDirectory(
                        "backindex", false));

                _assert(ir.document("1") == null);
                _assert(ir.document("2") == null);
                _assert(ir.document("3") == null);

                ir.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        testDocumentWriter();
        
        testDocumentReader();
        testDocumentDeleting();
        testBackwardCompatability();
        
    }
}