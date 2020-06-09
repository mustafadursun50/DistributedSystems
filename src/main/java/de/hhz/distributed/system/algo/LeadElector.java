package de.hhz.distributed.system.algo;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Properties;

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

		this.mServer.sendVotingMessage(sb.toString(), neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString(),
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

		String recvUid = null;
		StringBuilder sb = new StringBuilder();
		boolean isCoorinationMsg = false;
		System.out.println(this.mServer.getUid()+"="+mServer.getMulticastReceiver().getNeihbor());
		 System.out.println(this.mServer.getUid() + "<--------" + input);
		this.mServer.setElectionRunning(true);
		if (input.split(MESSAGE_SEPARATOR).length > 1) {
			recvUid = input.split(MESSAGE_SEPARATOR)[1];
		}
		if (input.split(MESSAGE_SEPARATOR).length == 3 && input.contains(MESSAGE_COOR)) {
			isCoorinationMsg = true;
		}

		// receive from left neighbor
		Properties neihborProps = this.mMulticastReceiver.getNeihbor();

		if (neihborProps == null) {
			this.mServer.setIsLeader(true);
			this.mServer.setElectionRunning(false);
			return;
		}
		String host = neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString();
		int port = Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString());

//		// Send own uid in the first round
//		if (!isCoorinationMsg && firstRound) {
//			sb = new StringBuilder();
//			sb.append(LCR_PREFIX);
//			sb.append(MESSAGE_SEPARATOR);
//			sb.append(mServer.getUid());
//			this.idReceivedInFistRound = recvUid;
//			System.out.println(this.mServer.getUid() + "------>" + sb.toString());
//
//			this.mServer.sendTCPMessage(sb.toString(), host, port);
//			firstRound = false;
//			return;
//		}
		sb = new StringBuilder();
		sb.append(LCR_PREFIX);
		sb.append(MESSAGE_SEPARATOR);
		// second round, compare received message with own id

		// server should declare itself as coordinator or received coordination message
		if (recvUid.equals(mServer.getUid()) || isCoorinationMsg) {
			firstRound = true;// Election completed. Reset first round
			this.mServer.setElectionRunning(false);
			// coordination message was initiated by this server. End message transmission.
			if (recvUid.equals(mServer.getUid()) && isCoorinationMsg) {
				this.mServer.setIsLeader(true);// Now start communication with clients
				return;
			}
			this.mServer.setLeadUid(recvUid);
			// Coordination message received. Forward the message to neihbor
			if (isCoorinationMsg) {
				sb.append(recvUid);

			} else {
				// Server declare itself as coordinator
				sb.append(this.mServer.getUid());
				System.out.println("Election completed. " + this.mServer.getUid() + " won! " + LocalTime.now());
			}
			sb.append(MESSAGE_SEPARATOR);
			sb.append(MESSAGE_COOR);
			this.mServer.sendVotingMessage(sb.toString(), host, port);
			this.mServer.setElectionRunning(false);
			 System.out.println(this.mServer.getUid() + "------>" + sb.toString());

			if (isCoorinationMsg) {
				if (mServer.isLeader()) {
					mServer.stopLeading();

				}
			}
		} else if (recvUid.compareTo(this.mServer.getUid()) > 0) {
			// Forward message to neihbor
			sb.append(recvUid);
			this.mServer.sendVotingMessage(sb.toString(), host, port);
			 System.out.println(this.mServer.getUid() + "------>" + sb.toString());
		} else {
			// Forward message to own uid
			sb.append(mServer.getUid());
			this.mServer.sendVotingMessage(sb.toString(), host, port);
			 System.out.println(this.mServer.getUid() + "------>" + sb.toString());

		}

	}
}