package de.hhz.distributed.system.algo;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Properties;
import java.util.TimerTask;
import java.util.UUID;

import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.server.MulticastReceiver;
import de.hhz.distributed.system.server.Server;

public class LeadElector {

	public static final String LCR_PREFIX = "LCR";
	public static final String MESSAGE_SEPARATOR = ":";
	private static final String MESSAGE_COOR = "COOR";
	private MulticastReceiver mMulticastReceiver;
	private Server mServer;
	private boolean firstRound = true;
	private UUID idReceivedInFistRound;

	public LeadElector(Server server) {
		this.mMulticastReceiver = server.getMulticastReceiver();
		this.mServer = server;
	}

	/**
	 * Initiate Ring voting
	 * 
	 * @throws NumberFormatException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void initiateVoting() throws Exception {
		Properties neihborProps = this.mMulticastReceiver.getNeihbor();
		if (neihborProps == null) {
			System.out.println("Server has no neihbor");
			if (this.mMulticastReceiver.getKnownHosts().size() == 0) {
				this.mServer.setIsLeader(true);
			}
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(LCR_PREFIX);
		sb.append(MESSAGE_SEPARATOR);
		sb.append(mServer.getUid());
		System.out.println("Server " + mServer.getUid() + " initiate voting!");

		this.mServer.sendElectionMessage(sb.toString(), neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString(),
				Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString()));
	}

	/**
	 * handle voting using ring algorithm
	 * 
	 * @param input
	 * @throws NumberFormatException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void handleVoting(String input) throws NumberFormatException, ClassNotFoundException, IOException {

		UUID recvUid = null;
		StringBuilder sb = new StringBuilder();
		boolean isCoorinationMsg = false;
		System.out.println("Sent: " + input + " --> " + this.mServer.getUid());
		this.mServer.setElectionRunning(true);
		if (input.split(MESSAGE_SEPARATOR).length > 1) {
			recvUid = UUID.fromString((input.split(MESSAGE_SEPARATOR)[1]));
		}
		if (input.split(MESSAGE_SEPARATOR).length == 3 && input.contains(MESSAGE_COOR)) {
			isCoorinationMsg = true;
		}

		// receive from left neighbor
		Properties neihborProps = this.mMulticastReceiver.getNeihbor();
		if (neihborProps == null) {
			System.out.println("Server is leader");
			this.mServer.setIsLeader(true);
			this.mServer.setElectionRunning(false);
			return;
		}
		String host = neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString();
		int port = Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString());

		// Send own uid in the first round
		if (!isCoorinationMsg && firstRound) {
			sb = new StringBuilder();
			sb.append(LCR_PREFIX);
			sb.append(MESSAGE_SEPARATOR);
			sb.append(mServer.getUid());
			this.idReceivedInFistRound = recvUid;
			this.mServer.sendElectionMessage(sb.toString(), host, port);
			firstRound = false;
			return;
		}
		sb = new StringBuilder();
		sb.append(LCR_PREFIX);
		sb.append(MESSAGE_SEPARATOR);
		// second round, compare received message with own id

		// server should declare itself as coordinator or received coordination message
		if ((recvUid.compareTo(mServer.getUid()) == 0) || isCoorinationMsg) {
			firstRound = true;// Election completed. Reset first round
			this.mServer.setElectionRunning(false);
			// coordination message was initiated by this server. End message transmission.
			if ((recvUid.compareTo(mServer.getUid()) == 0) && isCoorinationMsg) {
				return;
			}
			this.mServer.setLeadUid(recvUid);
			// Coordination message received. Forward the message to neihbor
			if (isCoorinationMsg) {
				sb.append(recvUid);
			} else {
				// Server declare itself as coordinator
				sb.append(this.mServer.getUid());
				this.mServer.setIsLeader(true);
				System.out.println("Election completed. " + this.mServer.getUid() + " won! " + LocalTime.now());
				System.out.println("Now send COOR to anothers servers..");
			}
			sb.append(MESSAGE_SEPARATOR);
			sb.append(MESSAGE_COOR);
			this.mServer.sendElectionMessage(sb.toString(), host, port);
			this.mServer.setElectionRunning(false);
		} else if (recvUid.compareTo(this.mServer.getUid()) == 1) {
			// Forward message to neihbor
			sb.append(recvUid);
			this.mServer.sendElectionMessage(sb.toString(), host, port);
		} else {
			// Forward message to own uid
			sb.append(mServer.getUid());
			this.mServer.sendElectionMessage(sb.toString(), host, port);
		}

	}
}