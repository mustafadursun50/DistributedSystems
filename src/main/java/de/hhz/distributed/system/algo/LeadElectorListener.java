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
	List<Server> servers;
	boolean voting = false;

	public LeadElectorListener(FailureDedector failureDedector, List<Server> servers) {
		this.failureDedector = failureDedector;
		this.failureDedector.addChangeListener(this);
		this.servers = servers;
	}

	/**
	 * Listening for StartLeadElectionEvent from FailureDedector.java
	 */
	public void propertyChange(PropertyChangeEvent evt) {

		if (voting) {
			return;
		}
		final Timer leaderElectedTimer = new Timer();

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				// Leader elected?
				for (Server s : servers) {
					if (s.isLeader()) {
						voting = false;
						leaderElectedTimer.cancel();
						break;
					}
				}

			}
		};
		voting = true;
		// Get actual leader
		Server leader = null;
		for (Server s : servers) {
			if (s.isLeader()) {
				leader = s;
				break;
			}
		}
		servers.remove(leader);
		System.out.println(evt.getPropertyName() + " occurred -> start..");
		servers.get(0).startVoting();
		leaderElectedTimer.schedule(task, 0, 10);

	}
}