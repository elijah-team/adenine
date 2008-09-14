/*
 * Created on Aug 25, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction.cluster;

import java.util.Iterator;
import java.util.Vector;

/**
 * @author yks
 */
public interface ICluster {
    /* member manipulation API */
    public Iterator iterator();
    
    public int numMembers();
    
    public Object addMember(Object node);
    
    public Object removeMember(Object node);
    
    public Vector getMembers();
    
    public void clearMembers();
    
    public void join(ICluster cluster);
    
    /* debugging string */
    public String description();   
}
