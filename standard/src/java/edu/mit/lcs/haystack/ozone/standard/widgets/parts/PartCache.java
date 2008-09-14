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

/*
 * Created on Jun 15, 2003
 */
package edu.mit.lcs.haystack.ozone.standard.widgets.parts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import edu.mit.lcs.haystack.SystemProperties;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.IPart;
import edu.mit.lcs.haystack.ozone.core.OzoneConstants;
import edu.mit.lcs.haystack.rdf.IRDFContainer;

class CacheEntry implements Serializable {
	CacheEntry(byte[] data) {
		m_cachedParts = new LinkedList();
		m_data = data;
		if (PartCache.s_cacheThreadsEnabled) {
			PartCache.s_thread.addCacheEntry(this);
		}
	}

	byte[] m_data;
	transient LinkedList m_cachedParts;
	int m_copiesToMake = 5;
	
	Object[] deserializeOnce() throws Exception {
//		System.out.println(">> serialized size: " + m_data.length);
		ByteArrayInputStream fis = new ByteArrayInputStream(m_data);		
		ObjectInputStream ois = new ObjectInputStream(fis);
		Object[] x = (Object[]) ois.readObject();
		ois.close();
		fis.close();
		return x;
	}
	
	Object[] getOne() throws Exception {
		if (PartCache.s_cacheThreadsEnabled) {
			synchronized (m_cachedParts) {
				if (!m_cachedParts.isEmpty()) {
					return (Object[]) m_cachedParts.removeFirst();
				}		
			}
			PartCache.s_logger.info("Cache miss");
			if (m_copiesToMake < 80) {
				m_copiesToMake *= 2;
			}
			PartCache.s_quickThread.addCacheEntry(this);
		}
		
		return deserializeOnce();
	}
}

/**
 * @author Dennis Quan
 */
public class PartCache {
	protected HashMap m_files = new HashMap();
	//kbakshi: part caching bugs are annoying...turning off cache
	public static boolean s_cachingEnabled = false;
	public static boolean s_cacheThreadsEnabled = true;
	public static boolean s_saveCache = false;
	static PartCacheThread s_thread; 
	static QuickPartCacheThread s_quickThread;
	
	static public void enableCache(boolean b) {
		s_cachingEnabled = b;
	}

	static org.apache.log4j.Logger s_logger = org.apache.log4j.Logger.getLogger(PartCache.class);
	static final public Object s_contextLock = new Object();

	static {
		if (s_cacheThreadsEnabled) {
			s_thread = new PartCacheThread(); 
			s_quickThread = new QuickPartCacheThread();
			new Timer().scheduleAtFixedRate(new TimerTask() {
				/* (non-Javadoc)
				 * @see java.util.TimerTask#run()
				 */
				public void run() {
					Runtime r = Runtime.getRuntime();
					long totalMemory = r.totalMemory();
					long maxMemory = r.maxMemory();
					long freeMemory = r.freeMemory();
					
					//System.out.println(">> memory: " + (totalMemory / 1024) + "k total; " + (maxMemory / 1024) + "k max; " + (freeMemory / 1024) + "k free");
					
					if ((maxMemory == totalMemory) && (freeMemory < 2 * 1024 * 1024)){
						s_logger.info("Low memory detected; reducing caches");

						LinkedList cacheEntries;
						synchronized (s_thread.m_cacheEntries) {
							cacheEntries = (LinkedList) s_thread.m_cacheEntries.clone();
						}
						
						Iterator i = cacheEntries.iterator();
						while (i.hasNext()) {
							WeakReference ref = (WeakReference) i.next();
							CacheEntry entry = (CacheEntry) ref.get();
							if (entry != null) {
								entry.m_copiesToMake /= 4;
								synchronized (entry.m_cachedParts) {
									while (entry.m_cachedParts.size() >= entry.m_copiesToMake) {
										entry.m_cachedParts.removeLast();
									}
								}
							}
						}
					}
				}
			}, 5000, 5000);
		}
	} 
	
	PartCache() {
		if (s_saveCache) {
			try {
				FileInputStream fis = new FileInputStream(new File(SystemProperties.s_userpath, "partcache"));
				ObjectInputStream ois = new ObjectInputStream(fis);
				m_files = (HashMap) ois.readObject();
				ois.close();
				fis.close();
				
				if (PartCache.s_cacheThreadsEnabled) {
					Iterator i = m_files.values().iterator();
					while (i.hasNext()) {
						CacheEntry ce = (CacheEntry) i.next();
						ce.m_cachedParts = new LinkedList();
						s_thread.addCacheEntry(ce);
					}
				}
			} catch (Exception e) {
				s_logger.warn("Could not restore cache", e);
			}
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						FileOutputStream fos = new FileOutputStream(new File(SystemProperties.s_userpath, "partcache"));
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(m_files);
						oos.close();
						fos.close();
					} catch (Exception e) {
						s_logger.warn("Could not save cache", e);
					}
				}
			});
		}
	}
	
/*	public static void test() {
		try {
			FileInputStream fis = new FileInputStream("\\test");
			byte[] buf = new byte[35973];
			int c = fis.read(buf);
			fis.close();
			
			for (int i = 0; i < 200; i++) {
				long start = System.currentTimeMillis();
				ByteArrayInputStream bais = new ByteArrayInputStream(buf);
				ObjectInputStream ois = new ObjectInputStream(bais);
				Object x = ois.readObject();
				ois.close();
				bais.close();
				long stop = System.currentTimeMillis();
				System.out.println(">> time: " + (stop - start));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	static PartCache s_partCache = new PartCache();

	static public PartCache getPartCache(Context context) {
		return s_partCache;
		//return (PartCache) ((UnserializableWrapper) context.getProperty(OzoneConstants.s_partCache)).m_object;
	}

	static public void serialize(Object key, IPart parent, Context parentContext, IPart child, Context childContext) {
		if (!s_cachingEnabled) {
			return;
		}
		
		getPartCache(parentContext).doSerialize(key, parent, parentContext, child, childContext);
	}
	
	static public void clearCache(Context context) {
		/*while (context != null) {
			getPartCache(context).m_files.clear();
			context = context.getParentContext();
		}*/
		s_partCache.m_files.clear();
	}
	
	public void doSerialize(Object resPart, IPart parent, Context parentContext, IPart child, Context childContext) {
		if (resPart == null) {
			return;
		}
		
		ByteArrayOutputStream fos = new ByteArrayOutputStream();
		synchronized (PartCache.s_contextLock) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				childContext.setParentContext(null);
				childContext.removeLocalProperty(OzoneConstants.s_parentPart);
				oos.writeObject(new Object[] { childContext, child });
				oos.close();
				CacheEntry entry = new CacheEntry(fos.toByteArray());
				m_files.put(resPart, entry);
	
	/*			if (resPart.getURI().equals("http://haystack.lcs.mit.edu/ui/messageView_29")) {
					FileOutputStream fos2 = new FileOutputStream("\\test");
					fos2.write(fos.toByteArray());
					fos2.close();
				}*/
	
				return;
			} catch (IOException ioe) {
				PartCache.s_logger.warn("Failed to serialize part " + resPart, ioe);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
					}
				}
				childContext.setParentContext(parentContext);
				childContext.putLocalProperty(OzoneConstants.s_parentPart, parent);
			}
		}
	}
	
	static public IPart deserialize(Object key, IPart parent, Context parentContext, IRDFContainer source) {
		if (!s_cachingEnabled) {
			return null;
		}
		
		return getPartCache(parentContext).doDeserialize(key, parent, parentContext, source);
	}

	public IPart doDeserialize(Object resPart, IPart parent, Context parentContext, IRDFContainer source) {
		if (resPart == null) {
			return null;
		}
		
		//long start = System.currentTimeMillis(), s1;
		
		if (!m_files.containsKey(resPart)) {
			return null;
		}
		IPart child;
		try {
			CacheEntry entry = (CacheEntry) m_files.get(resPart);
			if (entry == null) {
				return null;
			}
			
			Object[] x = (Object[]) entry.getOne();
			Context childContext = (Context) x[0];
			childContext.setParentContext(parentContext);
			childContext.putLocalProperty(OzoneConstants.s_parentPart, parent);
			child = (IPart) x[1];
			
			/*s1 = System.currentTimeMillis();
			if (start != s1) {
				System.out.println(">> deserialized " + resPart + " in " + (s1 - start) + " ms; cache size " + m_files.size());
			}*/

			try {
				child.initializeFromDeserialization(source);
			} catch (RuntimeException e) {
				s_logger.warn("Failed to reinitialize " + resPart, e);
				try {
					child.dispose();
				} catch (Exception e1) {}
				child = (IPart) child.getClass().getConstructors()[0].newInstance(null);
				child.initialize(source, childContext);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/*long s2 = System.currentTimeMillis();
		if (s2 != s1) {
			System.out.println(">> reinited " + resPart + " in " + (s2 - s1) + " ms");
		}*/
		
/*		if (resPart.getURI().equals("http://haystack.lcs.mit.edu/ui/messageView_29")) {
			test();
		}*/
		
		return child;
	} 
}

class PartCacheThread extends Thread {
	LinkedList m_cacheEntries = new LinkedList();

	PartCacheThread() {
		setDaemon(true);
		setPriority(Thread.MIN_PRIORITY);
		start();
	}
	
	void addCacheEntry(CacheEntry entry) {
		synchronized (m_cacheEntries) {
			m_cacheEntries.add(new WeakReference(entry));
		}
		interrupt();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
			LinkedList cacheEntries;
			synchronized (m_cacheEntries) {
				cacheEntries = (LinkedList) m_cacheEntries.clone();
			}
			
			LinkedList entriesToRemove = new LinkedList();
			Iterator i = cacheEntries.iterator();
			while (i.hasNext()) {
				WeakReference ref = (WeakReference) i.next();
				CacheEntry entry = (CacheEntry) ref.get();
				if (entry == null) {
					entriesToRemove.addFirst(ref);
				} else {
					while (entry.m_cachedParts.size() < entry.m_copiesToMake) {
						try {
							Object[] x = entry.deserializeOnce();
							synchronized (entry.m_cachedParts) {
								entry.m_cachedParts.addLast(x);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			if (!entriesToRemove.isEmpty()) {
				synchronized (m_cacheEntries) {
					m_cacheEntries.removeAll(entriesToRemove);
				}
			}
		}
	} 
}

class QuickPartCacheThread extends Thread {
	LinkedList m_cacheEntries = new LinkedList();
	int m_copiesToMake = 5;

	QuickPartCacheThread() {
		setDaemon(true);
		setPriority(Thread.MIN_PRIORITY);
		start();
	}
	
	void addCacheEntry(CacheEntry entry) {
		synchronized (m_cacheEntries) {
			m_cacheEntries.addFirst(entry);
		}
		interrupt();
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (true) {
			CacheEntry entry = null;
			synchronized (m_cacheEntries) {
				if (!m_cacheEntries.isEmpty()) {
					entry = (CacheEntry) m_cacheEntries.removeFirst();
				}
			}
			
			if (entry != null) {
				while (entry.m_cachedParts.size() < m_copiesToMake) {
					try {
						Object[] x = entry.deserializeOnce();
						synchronized (entry.m_cachedParts) {
							entry.m_cachedParts.addLast(x);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	} 
}