package de.hhz.distributed.system.algo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import de.hhz.distributed.system.server.FailureDedector;

public class LeadElectorListener implements PropertyChangeListener {
	
	boolean leadElectorHasToBeRun;
	private FailureDedector failureDedector;
	
	public LeadElectorListener(FailureDedector failureDedector) {
		this.failureDedector = failureDedector;
		this.failureDedector.addChangeListener(this);
	}

	/**
	 * Listening for StartLeadElectionEvent from FailureDedector.java
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		
		System.out.println(evt.getPropertyName() + " occurred -> start..");
		LeadElector.runLeadElection();
	}
}