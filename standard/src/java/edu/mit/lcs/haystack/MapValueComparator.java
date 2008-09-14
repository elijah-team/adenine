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

import java.util.Comparator;
import java.util.Map;

/**
 * @author Vineet Sinha
 *
 * This class is designed to be used as a comparator to compare objects based 
 * on their values in a map. The classical example is when items are stored
 * in a HashMap and moving them into a sorted data-structure like a TreeSet.
 */
public class MapValueComparator implements Comparator {
	protected Map map = null;
	protected boolean uniqueItems = false;
	protected int fwdDirFactor = 100;
	
	/**
	 * This flag controls whether the system should force all the items to be 
	 * unique. This is needed since comparing two objects with the same key 
	 * will by default return a 0 (meaning they are the same). Note: this is 
	 * implemented as a hack right now, by making compare never return 0.
	 */
	public void forceUniqueItems() {
		uniqueItems = true;
	}
	/**
	 * @see edu.mit.lcs.haystack.MapValueComparator#forceUniqueItems()
	 */
	public void removeForceUniqueItems() {
		uniqueItems = false;
	}
	
	public MapValueComparator(Map map) {
		this.map = map;
	}

	public void setReverseDir() {
		fwdDirFactor = Math.abs(fwdDirFactor) * -1;
	}
	public void setForwardDir() {
		fwdDirFactor = Math.abs(fwdDirFactor);
	}

	/**
	 * @see java.util.Comparator#compare(Object, Object)
	 * 
	 * This function returns 100 x the difference between the comparison of 
	 * the keys (since an int has to be returned).
	 */
	public int compare(Object arg0, Object arg1) {
		Object val0 = map.get(arg0);
		Object val1 = map.get(arg1);
		if ( (val0 instanceof Integer) && (val1 instanceof Integer) )
		{
			int retVal = ((Integer)val0).compareTo((Integer)val1) * fwdDirFactor;
			if (retVal==0 && uniqueItems) retVal = 1;
			return retVal;
		}
		if ( (val0 instanceof Double) && (val1 instanceof Double) )
		{
			int retVal = ((Double)val0).compareTo((Double)val1) * fwdDirFactor;
			if (retVal==0 && uniqueItems) retVal = 1;
			return retVal;
		}
		
		// should not really come here
		return -1;
	}

}
