/*
 * Created on Jan 2, 2004
 */
package edu.mit.lcs.haystack;

import java.io.Serializable;

/**
 * @author Dennis Quan
 */
final public class Pair implements Serializable {
	final protected Object m_left;
	final protected Object m_right;
	
	public Pair(Object left, Object right) {
		m_left = left;
		m_right = right;
	}
	
	final public Object getLeft() {
		return m_left;
	}
	
	final public Object getRight() {
		return m_right;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	final public int hashCode() {
		return (m_left == null ? 0 : m_left.hashCode()) +
		(m_right == null ? 0 : m_right.hashCode());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	final public boolean equals(Object arg0) {
		if (!(arg0 instanceof Pair)) {
			return false;
		}
		
		Pair p = (Pair) arg0;
		return !((m_left != null && !m_left.equals(p.m_left)) ||
				(m_left == null && p.m_left != null) ||
				(m_right != null && !m_right.equals(p.m_right)) ||
				(m_right == null && p.m_right != null));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + m_left + ", " + m_right + ")";
	}
}
