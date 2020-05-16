package de.hhz.distributed.system.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/*
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
			LocalTime failureTime = LocalTime.now();
    		long diffInSec = Duration.between(lastOkay, failureTime).toSeconds();
    	
    		if(diffInSec > 5) {
    			notifyLeadElector();
    		}
    	}		
	}
	
    private void notifyLeadElector() {
        for (PropertyChangeListener name : leadElectorListener) {
            name.propertyChange(new PropertyChangeEvent(this, "StartLeadElection", "OK", "NOK"));
        }
    }

    public void addChangeListener(PropertyChangeListener newListener) {
    	leadElectorListener.add(newListener);
    }
	
}