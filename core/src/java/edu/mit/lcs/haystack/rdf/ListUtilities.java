/* 
 * Copyright (c) 1998-2003 Massachusetts Institute of Technology. 
 * This code was developed as part of the Haystack research project 
 * (http://haystack.lcs.mit.edu/). Permission is hereby granted, 
 * free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit 
 * persons to whom the Software is furnished to do so, subject to 
 * the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be 
 * included in all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES 
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR 
 * OTHER DEALINGS IN THE SOFTWARE. 
 */

package edu.mit.lcs.haystack.rdf;

import edu.mit.lcs.haystack.Constants;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author David Huynh
 */
public class ListUtilities {
	
	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(ListUtilities.class);
	
	/**
	 * Returns an Iterator abstracting the contents of a DAML list or an hs:List.
	 */
	public static DAMLListIterator accessDAMLList(Resource res, IRDFContainer source) {
		if (Utilities.isType(res, Constants.s_haystack_List, source)) {
			Resource damlList = Utilities.getResourceProperty(res, Constants.s_haystack_list, source);
			
			return new DAMLListIterator(damlList, source);
		} else {
			return new DAMLListIterator(res, source);
		}
	}

	/**
	 * Returns a list of the contents of a DAML list or an hs:List.
	 */
	public static ArrayList retrieveDAMLList(
		Resource res,
		IRDFContainer source) {
		Iterator i = accessDAMLList(res, source);
		ArrayList al = new ArrayList();
		while (i.hasNext()) {
			al.add(i.next());
		}
		return al;
	}

	public static Resource createHSList(Iterator i, IRDFContainer source) throws RDFException {
		return createHSList(i, source, new URIGenerator());
	}

	public static Resource createHSList(Iterator i, IRDFContainer source, URIGenerator urig)
		throws RDFException {
		Resource damlList = createDAMLList(i, source, urig);
		Resource hsList = urig.generateNewResource();
		
		source.add(new Statement(hsList, Constants.s_rdf_type, Constants.s_haystack_List));
		source.add(new Statement(hsList, Constants.s_haystack_list, damlList));
		
		return hsList;
	}
		

	public static Resource createDAMLList(Iterator i, IRDFContainer source) throws RDFException {
		return createDAMLList(i, source, new URIGenerator());
	}

	public static Resource createDAMLList(Iterator i, IRDFContainer source, URIGenerator urig)
		throws RDFException {
		Resource resLast = null;
		Resource resFirst = urig.generateNewResource();
		Resource resNext = resFirst;
		while (i.hasNext()) {
			Object o = i.next();
			RDFNode res1;
			if (o instanceof RDFNode) {
				res1 = (RDFNode)o;
			} else if (o == null) {
				res1 = new Literal("null");
			} else {
				res1 = new Literal(o.toString());
			}
			source.add(
				new Statement(
					resNext,
					Constants.s_rdf_type,
					Constants.s_daml_List));
			source.add(new Statement(resNext, Constants.s_daml_first, res1));

			if (resLast != null) {
				source.add(
					new Statement(resLast, Constants.s_daml_rest, resNext));
			}
			resLast = resNext;
			resNext = urig.generateNewResource();
		}

		if (resLast == null) {
			return Constants.s_daml_nil;
		} else {
			source.add(
				new Statement(
					resLast,
					Constants.s_daml_rest,
					Constants.s_daml_nil));
			return resFirst;
		}
	}

	public static boolean addToHSList(Resource list, RDFNode item, int index, IRDFContainer source) {
		Resource current = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
		Resource previous = list;
		
		for (int i = 0; i < index; i++) {
			previous = current;
			current = Utilities.getResourceProperty(current, Constants.s_daml_rest, source);
			
			if (current == null) {
				return false;
			}
		}
		
		return addToHSListAt(previous, current, item, previous.equals(list), source);
	}
	
	public static boolean appendToHSList(Resource list, RDFNode item, IRDFContainer source) {
		Resource current = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
		Resource previous = list;
		
		while (!current.equals(Constants.s_daml_nil)) {
			previous = current;
			current = Utilities.getResourceProperty(current, Constants.s_daml_rest, source);
			
			if (current == null) {
				return false;
			}
		}

		return addToHSListAt(previous, current, item, previous.equals(list), source);
	}
	
	public static boolean prependToHSList(Resource list, RDFNode item, IRDFContainer source) {
		return addToHSList(list, item, 0, source);
	}

	public static boolean removeFromHSList(Resource list, RDFNode item, IRDFContainer source) {
		Resource current = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
		Resource previous = list;
		
		while (!current.equals(Constants.s_daml_nil) &&
			    !item.equals(Utilities.getProperty(current, Constants.s_daml_first, source))) {
			
			previous = current;
			current = Utilities.getResourceProperty(current, Constants.s_daml_rest, source);
			
			if (current == null) {
				return false;
			}
		}

		if (current.equals(Constants.s_daml_nil)) {
			return false;
		}
		
		return removeFromHSListAt(previous, current, previous.equals(list), source);
	}

	public static boolean removeFromHSList(Resource list, int index, IRDFContainer source) {
		Resource current = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
		Resource previous = list;
		
		for (int i = 0; i < index && current != null && !current.equals(Constants.s_daml_nil); i++) {
			previous = current;
			current = Utilities.getResourceProperty(current, Constants.s_daml_rest, source);
		}

		if (current == null || current.equals(Constants.s_daml_nil)) {
			return false;
		}
		
		return removeFromHSListAt(previous, current, previous.equals(list), source);
	}
	
	static boolean removeFromHSListAt(Resource previousNode, Resource nodeToRemove, boolean head, IRDFContainer source) {
		try {
			Resource nextNode = Utilities.getResourceProperty(nodeToRemove, Constants.s_daml_rest, source);
		
			source.replace(previousNode, head ? Constants.s_haystack_list : Constants.s_daml_rest, null, nextNode);
			
			dismantleDAMLListNode(nodeToRemove, source);
			
			return true;
		} catch (RDFException e) {
			return false;
		}
	}
	
	static boolean addToHSListAt(Resource previousNode, Resource nextNode, RDFNode item, boolean head, IRDFContainer source) {
		try {
			Resource newNode = new URIGenerator().generateNewResource();
		
			source.add(new Statement(newNode, Constants.s_rdf_type, Constants.s_daml_List));
			source.add(new Statement(newNode, Constants.s_daml_first, item));
			source.add(new Statement(newNode, Constants.s_daml_rest, nextNode));
			
			source.replace(previousNode, head ? Constants.s_haystack_list : Constants.s_daml_rest, null, newNode);

			return true;
		} catch (RDFException e) {
			return false;
		}
	}
	
	public static boolean clearHSList(Resource list, IRDFContainer source) {
		try {
			Resource node = (Resource) source.extract(list, Constants.s_haystack_list, null);
			
			source.replace(list, Constants.s_haystack_list, null, Constants.s_daml_nil);

			// dismantle the whole list
			while (node != null && !node.equals(Constants.s_daml_nil)) {
				Resource nextNode = (Resource) source.extract(node, Constants.s_daml_rest, null);
				
				dismantleDAMLListNode(node, source);
				
				node = nextNode;
			}
			
			return true;
		} catch (RDFException e) {
			return false;
		}
	}
	
	public static boolean changeHSList(Resource list, RDFNode item, int index, IRDFContainer source) {
		Resource current = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
		
		for (int i = 0; i < index && current != null && !current.equals(Constants.s_daml_nil); i++) {
			current = Utilities.getResourceProperty(current, Constants.s_daml_rest, source);
		}

		if (current == null || current.equals(Constants.s_daml_nil)) {
			return false;
		}

		try {
			source.replace(current, Constants.s_daml_first, null, item);
			return true;
		} catch (RDFException e) {
			return false;
		}
	}
	
	public static boolean isInHSList(Resource list, RDFNode item, IRDFContainer source) {
		Resource current = Utilities.getResourceProperty(list, Constants.s_haystack_list, source);
		
		try {
			while (current != null && !current.equals(Constants.s_daml_nil)) {
				RDFNode first = source.extract(current, Constants.s_daml_first, null);
				
				if (item.equals(first)) {
					return true;
				}
				
				current = (Resource) source.extract(current, Constants.s_daml_rest, null);
			}
		} catch (RDFException e) {
		}		
		return false;
	}
	
	static void dismantleDAMLListNode(Resource node, IRDFContainer source) throws RDFException {
		if (!node.equals(Constants.s_daml_nil)) {
			source.remove(
				new Statement(
					node, 
					Constants.s_daml_first, 
					Utilities.generateWildcardResource(1)),
				Utilities.generateWildcardResourceArray(1));
			source.remove(
				new Statement(
					node, 
					Constants.s_daml_rest, 
					Utilities.generateWildcardResource(1)),
				Utilities.generateWildcardResourceArray(1));
			source.remove(
				new Statement(
					node, 
					Constants.s_rdf_type, 
					Constants.s_daml_List),
				new Resource[] {});
		}
	}
	
	public static RDFNode makeElementNodeListener(Resource currentNode, ListWatchRecord record, int indexInList) throws RDFException {
		Resource 		firstCookie = record.m_rdfListener.addPattern(currentNode, Constants.s_daml_first, null);
		Resource 		restCookie = record.m_rdfListener.addPattern(currentNode, Constants.s_daml_rest, null);
		NodeListener 	nodeListener = new NodeListener(currentNode, firstCookie, restCookie) {
			public void statementAdded(Statement s, ListWatchRecord record) {
				synchronized (record)/*if (m_node.equals(s.getSubject()))*/ {
					int index = record.m_nodeListenersAsList.indexOf(this);
					
					if (index >= 0) {
						if (s.getPredicate().equals(Constants.s_daml_first)) {
							Resource first = Utilities.getResourceProperty(m_node, Constants.s_daml_first, record.m_source);
							if (record == null)
								s_logger.error("Null ListWatchRecord");
							else if (record.m_listener == null)
								s_logger.error("No listener for ListWatchRecord " + record);
							record.m_listener.onItemChanged(first, index);
						} else if (s.getPredicate().equals(Constants.s_daml_rest)) {
							Resource rest = Utilities.getResourceProperty(m_node, Constants.s_daml_rest, record.m_source);
							if (Constants.s_daml_nil.equals(rest)) {
								processListTruncated(record, index + 1);
							} else if (rest != null) {
								processListNodeChanged(record, rest, index + 1);
							} else {
								s_logger.error("Expecting a Resource but got " + rest);
							}
						} else {
							s_logger.error("Unexpected rdf event with predicate " + s.getPredicate());
						}
					}
				} /*else {
					s_logger.error("Incorrect routing of rdf events");
				}*/
			}
		};
		
		record.m_nodeListeners.put(firstCookie, nodeListener);
		record.m_nodeListeners.put(restCookie, nodeListener);
		
		if (indexInList == -1) {
			record.m_nodeListenersAsList.add(nodeListener);
		} else {
			record.m_nodeListenersAsList.add(indexInList, nodeListener);
		}
		
		return Utilities.getProperty(currentNode, Constants.s_daml_first, record.m_source);
	}
	
	public static void processListNodeChanged(ListWatchRecord record, Resource newNode, int indexOfChange) {
		// We see if items have been removed
		{
			Iterator 	i = record.m_nodeListenersAsList.listIterator(indexOfChange);
			int		removeCount = 0;
			
			while (i.hasNext()) {
				NodeListener nodeListener = (NodeListener) i.next();
				
				if (nodeListener.m_node.equals(newNode)) {
					break;
				}
				
				removeCount++;
			}
			
			if (i.hasNext()) {
				record.m_listener.onItemRemoved(indexOfChange, removeCount);
				
				i = record.m_nodeListenersAsList.listIterator(indexOfChange);
				while (removeCount > 0) {
					NodeListener nodeListener = (NodeListener) i.next();
					
					record.m_rdfListener.removePattern(nodeListener.m_firstCookie);
					record.m_rdfListener.removePattern(nodeListener.m_restCookie);
					
					record.m_nodeListeners.remove(nodeListener.m_firstCookie);
					record.m_nodeListeners.remove(nodeListener.m_restCookie);
					
					i.remove();
					
					removeCount--;
				}
				
				return; // done
			}
		}
		
		if (indexOfChange < record.m_nodeListenersAsList.size()) {
			NodeListener oldNodeListener = (NodeListener) record.m_nodeListenersAsList.get(indexOfChange);
			try {
				while (newNode != null && !newNode.equals(Constants.s_daml_nil) && !newNode.equals(oldNodeListener.m_node)) {
					RDFNode item = makeElementNodeListener(newNode, record, indexOfChange);
					
					record.m_listener.onItemAdded(item, indexOfChange);
					
					newNode = Utilities.getResourceProperty(newNode, Constants.s_daml_rest, record.m_source);
					
					indexOfChange++;
				}
				
				if (newNode == null || newNode.equals(Constants.s_daml_nil)) {
					processListTruncated(record, indexOfChange);
				}
			} catch (Exception e) {
				s_logger.error("Failed to process node change in HS list", e);
			}
		} else {
			try {
				while (newNode != null && !newNode.equals(Constants.s_daml_nil)) {
					RDFNode item = makeElementNodeListener(newNode, record, indexOfChange);
					
					record.m_listener.onItemAdded(item, indexOfChange);
					
					newNode = Utilities.getResourceProperty(newNode, Constants.s_daml_rest, record.m_source);
					
					indexOfChange++;
				}
			} catch (Exception e) {
				s_logger.error("Failed to process node change in HS list", e);
			}
		}
	}

	static void processListTruncated(ListWatchRecord record, int firstTruncated) {
		int length = record.m_nodeListenersAsList.size();
		
		Iterator i = record.m_nodeListenersAsList.listIterator(firstTruncated);
		while (i.hasNext()) {
			NodeListener nodeListener = (NodeListener) i.next();
			
			record.m_rdfListener.removePattern(nodeListener.m_firstCookie);
			record.m_rdfListener.removePattern(nodeListener.m_restCookie);
			
			record.m_nodeListeners.remove(nodeListener.m_firstCookie);
			record.m_nodeListeners.remove(nodeListener.m_restCookie);
			
			i.remove();
		}
		
		record.m_listener.onItemRemoved(firstTruncated, length - firstTruncated);
	}
	
	public static void cleanUpRecord(ListWatchRecord record) {
		if (record.m_rdfListener != null) {
			record.m_rdfListener.stop();
			record.m_rdfListener = null;
		}
		record.m_nodeListenersAsList.clear();
		record.m_nodeListeners.clear();
	}
	
	public static void unwatchHSList(Object watchData, IRDFContainer source) {
		cleanUpRecord((ListWatchRecord) watchData);
	}
}
