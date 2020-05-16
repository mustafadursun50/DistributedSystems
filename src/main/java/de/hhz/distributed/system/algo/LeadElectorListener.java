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

	public void propertyChange(PropertyChangeEvent evt) {
		LeadElector.runLeadElection();
	}
}