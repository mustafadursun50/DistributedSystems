package de.hhz.distributed.system.algo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
//		if (server.isElectionRunning()) {
//			return;
//		}
//
//        //server has no neighbor
//		if (server.getMulticastReceiver().getKnownHosts().size() == 0) {
//			return;
//		}
//		System.out.println(evt.getPropertyName() + " occurred!!");
//		server.startVoting();
	}
}