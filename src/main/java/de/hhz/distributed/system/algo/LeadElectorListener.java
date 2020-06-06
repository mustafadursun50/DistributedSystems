package de.hhz.distributed.system.algo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hhz.distributed.system.server.FailureDedector;
import de.hhz.distributed.system.server.Server;

public class LeadElectorListener implements PropertyChangeListener {

	boolean leadElectorHasToBeRun;
	private FailureDedector failureDedector;
	Server server;
	boolean voting = false;

	public LeadElectorListener(FailureDedector failureDedector, Server server) {
		this.failureDedector = failureDedector;
		this.failureDedector.addChangeListener(this);
		this.server = server;
	}

	/**
	 * Listening for StartLeadElectionEvent from FailureDedector.java
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if (server.isElectionRunning()) {
			return;
		}
		System.out.println(evt.getPropertyName() + " occurred!!");
		server.startVoting();
	}
}