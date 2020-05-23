package de.hhz.distributed.system.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import de.hhz.distributed.system.app.Constants;

/**
 * Check Ping interval and notify the listener of LeadElector if needed.
 */
public class FailureDedector implements Runnable {
	
    private List<PropertyChangeListener> leadElectorListener = new ArrayList<PropertyChangeListener>();
	static LocalTime lastOkay = LocalTime.now();
	
	public static void updateLastOkayTime() {
		lastOkay = LocalTime.now();
	}

	public void run() {	
		
		while (true) {		
			LocalTime now = LocalTime.now();
    		long diffInSec = Duration.between(lastOkay, now).toSeconds();
    	
    		if(diffInSec > Constants.MAX_PING_LIMIT_SEC) {
    			notifyLeadElector();
    		}
    	}		
	}
	
    private void notifyLeadElector() {
    	System.out.println("Warning last successfully ping was at: "+ lastOkay);
        for (PropertyChangeListener name : leadElectorListener) {
            name.propertyChange(new PropertyChangeEvent(this, "StartLeadElectionEvent", "", ""));
        }
    }

    public void addChangeListener(PropertyChangeListener newListener) {
    	leadElectorListener.add(newListener);
    }
	
}