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

package edu.mit.lcs.haystack.ozone.data;

import edu.mit.lcs.haystack.rdf.*;
import edu.mit.lcs.haystack.ozone.core.Context;
import edu.mit.lcs.haystack.ozone.core.Ozone;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author David Huynh
 */
public class SmartDateDataProvider extends ChainedDataProvider {
	final static SimpleDateFormat	s_dayOfWeekFormatter = new SimpleDateFormat("EEEE, h:mm a");
	final static SimpleDateFormat	s_timeFormatter = new SimpleDateFormat("h:mm a");
	final static SimpleDateFormat	s_fullFormatter = new SimpleDateFormat("EEE MMMM d, yyyy, h:mm a");
	
	protected String 	m_smartDate = "";
	protected Date		m_rawDate;
	transient protected TimerTask m_timerTask;
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#initialize(IRDFContainer, Context)
	 */
	public void initialize(IRDFContainer source, Context context) {
		internalInitialize(source, context, false);
		setupTimer();
	}
	
	protected void setupTimer() {
		Ozone.s_timer.scheduleAtFixedRate(m_timerTask = new TimerTask() {
			/**
			 * @see java.util.TimerTask#run()
			 */
			public void run() {
				onChange();
			}
		}, 0, 60000);		
	}

	/* (non-Javadoc)
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#initializeFromDeserialization(edu.mit.lcs.haystack.rdf.IRDFContainer)
	 */
	public void initializeFromDeserialization(IRDFContainer source) {
		super.initializeFromDeserialization(source);
		setupTimer();
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.core.IPart#dispose()
	 */
	public void dispose() {
		try {
			m_timerTask.cancel();
		} catch (Exception e) {}
		super.dispose();
	}
	
	protected IDataConsumer createDataConsumer() {
		return new StringDataConsumer() {
			protected void onStringChanged(String newString) {
				onRawDateChange(newString);
			}

			protected void onStringDeleted(String previousString) {
				onRawDateChange(null);
			}
		};
	}
	
	protected void onRawDateChange(String rawDate) {
		Date 		newRawDate = null;
		boolean	changed = false;
		
		if (rawDate != null) {
			newRawDate = Utilities.parseDateTime(rawDate);
		}
		
		if (rawDate == null) {
			if (m_rawDate != null) {
				changed = true;
			}
		} else if (!rawDate.equals(m_rawDate)) {
			changed = true;
		}
		
		if (changed) {
			m_rawDate = newRawDate;
			onChange();
		}
	}
	
	static public String makeSmartDate(Date date) {
		String newSmartDate;
		
		Calendar	now = Calendar.getInstance();
		Calendar	then = Calendar.getInstance();

		now.set(Calendar.HOUR_OF_DAY, 0);
		now.clear(Calendar.MINUTE);
		now.clear(Calendar.SECOND);
		now.clear(Calendar.MILLISECOND);
			
		then.setTime(date);

		then.set(Calendar.HOUR_OF_DAY, 0);
		then.clear(Calendar.MINUTE);
		then.clear(Calendar.SECOND);
		then.clear(Calendar.MILLISECOND);
			
		long diff = (now.getTime().getTime() - then.getTime().getTime()) / 86400000; // in days
			
		if (diff == 0) { // today
			newSmartDate = s_timeFormatter.format(date);
		} else if (diff == 1) { // yesterday
			newSmartDate = "Yesterday at " + s_timeFormatter.format(date);
		} else if (diff == -1) { // tomorrow
			newSmartDate = "Tomorrow at " + s_timeFormatter.format(date);
		} else if (diff > 0 && diff < 7) {
			newSmartDate = "Last " + s_dayOfWeekFormatter.format(date);
		} else if (diff < 0 && diff >= -7) {
			newSmartDate = "Next " + s_dayOfWeekFormatter.format(date);
		} else {
			newSmartDate = s_fullFormatter.format(date);
		}
		
		return newSmartDate;
	}
	
	protected void onChange() {
		String newSmartDate = "";
		
		if (m_rawDate != null) {
			newSmartDate = makeSmartDate(m_rawDate);
		}
		
		if (!newSmartDate.equals(m_smartDate)) {
			m_smartDate = newSmartDate;
			notifyDataConsumers(DataConstants.LITERAL_CHANGE, m_smartDate);
		}
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.GenericDataProvider#onConsumerAdded(edu.mit.lcs.haystack.ozone.data.IDataConsumer)
	 */
	protected void onConsumerAdded(IDataConsumer dataConsumer) {
		dataConsumer.onDataChanged(DataConstants.LITERAL_CHANGE, m_smartDate);
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#getData(Resource, Object)
	 */
	synchronized public Object getData(Resource dataType, Object specifications)
		throws DataNotAvailableException {
		
		if (dataType.equals(DataConstants.LITERAL))  {
			return new Literal(m_smartDate);
		} else if (dataType.equals(DataConstants.STRING)) {
			return m_smartDate;
		}
		return null;
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#requestChange(Resource, Object)
	 */
	synchronized public void requestChange(Resource changeType, Object change)
		throws UnsupportedOperationException, DataMismatchException {
		throw new UnsupportedOperationException("No data available to change");
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.IDataProvider#supportsChange(Resource)
	 */
	public boolean supportsChange(Resource changeType) {
		return false;
	}
	
	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementAdded(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementAdded(Statement s) {
	}

	/**
	 * @see edu.mit.lcs.haystack.ozone.data.ChainedDataProvider#onStatementRemoved(edu.mit.lcs.haystack.rdf.Statement)
	 */
	protected void onStatementRemoved(Statement s) {
	}
}
