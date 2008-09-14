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

package edu.mit.lcs.haystack.ozone.core;


/**
 * @version 	1.0
 * @author		David Huynh
 */
public abstract class IdleRunnable implements Runnable, Comparable {
	boolean m_expired = false;
	public int m_priority = 5; // 0 = highest, 10 = lowest
	
	public IdleRunnable() {
	}
	
	public IdleRunnable(int priority) {
		m_priority = priority;
	}
	
	public IdleRunnable(Context context) {
		try {
			m_priority = ((Integer) context.getProperty(OzoneConstants.s_priority)).intValue();
		} catch (Exception e) {}
	}
	
	public boolean hasExpired() {
		return m_expired;
	}
	
	public void expire() {
		m_expired = true;
	}
	
	public String toString() {
		return "[" + getClass() + ": " + m_priority + " " + m_expired + "]";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object arg0) {
		return m_priority - ((IdleRunnable) arg0).m_priority;
	}
}
