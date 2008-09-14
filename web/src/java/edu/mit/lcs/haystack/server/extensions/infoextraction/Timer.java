/*
 * Created on Aug 12, 2004
 */
package edu.mit.lcs.haystack.server.extensions.infoextraction;


/**
 * @author yks
 * 
 * Utility class to calculate the time elapsed between events
 *  
 */
public class Timer {
    private static Timer TIMER = null;

    public static Timer getTimer() {
        if (TIMER == null) {
            TIMER = new Timer();
        }
        return TIMER;
    }

    public static long printTimeElapsed(String message) {
        long diff = timeElapsed(getTimer());
        if (message != null) {
            System.err.println(message + ": " + diff + "ms");
        }
        return diff;
    }

    private static long timeElapsed(Timer timer) {
        Timer now = new Timer();
        long diff = timer.elapsed(now);
        TIMER.sync(now);
        return diff;
    }

    private void sync(Timer t) {
        time = t.time;
    }

    private long time;

    public Timer() {
        time = System.currentTimeMillis();
    }

    public long elapsed(Timer timer) {
        return timer.time - this.time;
    }
}