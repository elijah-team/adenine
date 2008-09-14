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

package edu.mit.lcs.haystack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author vineet
 *
 * An implementation to store key-value pairs.
 * 
 * This class does not implement comparable, since the user will want to either
 * compare based on the key or the value (and both comparators can be retrieved)
 * 
 */
public class MapEntry implements Map.Entry {
	
	private Object key;
	private Object value;
	
	public MapEntry(Map.Entry arg0) {
		this.key = arg0.getKey();
		this.value = arg0.getValue();
	}

	public MapEntry(Object arg0, Object arg1) {
		this.key = arg0;
		this.value = arg1;
	}

	public static MapEntry[] constructArrayFromMap(HashMap inpMap) {
		MapEntry[] retMap = new MapEntry[inpMap.size()];
		Iterator it = inpMap.entrySet().iterator();
		int ndx = 0;
		while (it.hasNext()) {
			Map.Entry iME = (Map.Entry)it.next();
			retMap[ndx] = new MapEntry(iME);
			ndx++;
		}
		return retMap;
	}


	/**
	 * @see java.util.Map.Entry#getKey()
	 */
	public Object getKey() {
		return key;
	}

	/**
	 * @see java.util.Map.Entry#getValue()
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * @see java.util.Map.Entry#setValue(Object)
	 */
	public Object setValue(Object arg0) {
		Object oldValue = arg0;
		value = arg0;
		return oldValue;
	}

	public static java.util.Comparator getValueComparator() {
		return new java.util.Comparator() {
			public int compare(Object o1, Object o2) {
				o1 = ((MapEntry) o1).getValue();
				o2 = ((MapEntry) o2).getValue();
				if (o1 instanceof Comparable) {
					return ((Comparable)o1).compareTo(o2);
				}
				return -1;
			}
		};
	}

	public static java.util.Comparator getKeyComparator() {
		return new java.util.Comparator() {
			public int compare(Object o1, Object o2) {
				o1 = ((MapEntry) o1).getKey();
				o2 = ((MapEntry) o2).getKey();
				if (o1 instanceof Comparable) {
					return ((Comparable)o1).compareTo(o2);
				}
				return -1;
			}
		};
	}

}
