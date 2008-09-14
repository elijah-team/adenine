package edu.mit.lcs.haystack.server.extensions.infoextraction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Node;

import edu.mit.lcs.haystack.server.extensions.infoextraction.featureset.DefaultFeature;
import edu.mit.lcs.haystack.server.extensions.infoextraction.tagtree.IAugmentedNode;

/**
 * @author yks
 *
 * This data structure keeps a fixed size queue,
 * as nodes are added to the window, excess nodes will be kicked out.
 * 
 */
public class FixedSizeQueue {
    Vector /*WindowNode */ window;
    int windowSize; /* how large the queue should be
    				   maintained */
    
    /* this is the set of nodes
     * which fragments will be added to
     */
    HashMap /*IAttributedNode*/ consumerSet;
    
    public FixedSizeQueue(int size) {
        windowSize = size;
        clear();
    }
    
    public void clear() {
        window = new Vector();
        consumerSet = new HashMap();
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < window.size(); i++) {
            WindowNode wn = (WindowNode)window.get(i);
            buf.append("<"+ (wn.open?"":"/") + ((IAugmentedNode)(wn.node)).nodeName()+">");
        }
        return buf.toString();
    }
    
    public void feedConsumers() {
        Iterator consumers = consumerSet.keySet().iterator();
        while (consumers.hasNext()) {
            IAugmentedNode ian = (IAugmentedNode)consumers.next();
            if (ian != null) {
                ian.addFeature(new DefaultFeature(this.toString()));
            }
        }
    }
    
    public void add(Node node, boolean open) {
        window.add(new WindowNode(node, open));
                
        if (window.size() > windowSize) {
            //System.err.println("readjust: "+ window.size());

            int numRemove = window.size() - windowSize;
            for (int i = 0; i < numRemove; i++) {
                WindowNode wn = (WindowNode)window.remove(i);
                if (wn != null && !wn.open) {
                    if (consumerSet.get(wn.node) != null) {
                        consumerSet.remove(wn.node);
                    }
                }
            }
            
            /* we add a node to the consumer list
             * when it is the last node on the list and
             * is an open node
             */
            if (window.size() == windowSize) {
                WindowNode lastElement = (WindowNode)window.get(0);
                if (lastElement.open) {
                    consumerSet.put(lastElement.node, new Object());
                }
                
                if (windowSize >= 2) {
                    WindowNode secondElement = (WindowNode)window.get(windowSize-2);
                    if (secondElement != null && !secondElement.open) {
                        consumerSet.remove(secondElement.node);
                    }
                }
                
                //System.err.println("trigram: " + this.toString());
            }
        }
    }
}

class WindowNode {
    public Node node;
    public boolean open;
    
    public WindowNode(Node node, boolean open) {
        this.node = node;
        this.open = open;
    }
}