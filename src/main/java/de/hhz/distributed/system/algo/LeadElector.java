package de.hhz.distributed.system.algo;

import java.io.IOException;
import java.util.Properties;

import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.server.MulticastReceiver;
import de.hhz.distributed.system.server.Server;

public class LeadElector {

	public static final String LCR_PREFIX = "LCR";
	public static final String MESSAGE_SEPARATOR = ":";
	private static final Object MESSAGE_COOR = "COOR";
	private MulticastReceiver mMulticastReceiver;
	private Server mServer;

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
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(LCR_PREFIX);
		sb.append(MESSAGE_SEPARATOR);
		sb.append(mServer.getUid());
		System.out.println("Server UID " + mServer.getUid() + " initiate voting");

		this.mServer.sendMessage(sb.toString(), neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString(),
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
		int recvUid = -1;
		StringBuilder sb = new StringBuilder();
		boolean isCoorinationMsg = false;
		System.out.println("Server UID " + this.mServer.getUid() + " Recv " + input);

		if (input.split(MESSAGE_SEPARATOR).length > 1) {
			recvUid = Integer.parseInt(input.split(MESSAGE_SEPARATOR)[1]);
		}
		if (input.split(MESSAGE_SEPARATOR).length == 3) {
			isCoorinationMsg = true;
		}
		Properties neihborProps = this.mMulticastReceiver.getNeihbor();
//server should declare itself as coordinator or received coordination message
		if ((recvUid == mServer.getUid()) || isCoorinationMsg) {
			// The coordination message was initiated by this server. End message
			// transmission.
			if (recvUid == mServer.getUid() && isCoorinationMsg) {
				return;
			}
			this.mServer.setLeadUid(recvUid);
			sb = new StringBuilder();
			sb.append(LCR_PREFIX);
			sb.append(MESSAGE_SEPARATOR);
			// Coordination message received. Forward the message to neihbor
			if (isCoorinationMsg) {
				sb.append(recvUid);
			} else {
				// Server declare itself as coordinator
				sb.append(this.mServer.getUid());
				this.mServer.setIsLeader(true);
				System.out.println(" Election completed. Server UID " + this.mServer.getUid()
						+ " won. Now send COOR to anothers servers");
			}
			sb.append(MESSAGE_SEPARATOR);
			sb.append(MESSAGE_COOR);
			String host = neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString();
			int port = Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString());
			this.mServer.sendMessage(sb.toString(), host, port);
		} else if (recvUid > this.mServer.getUid()) {
			// Forward message to neihbor
				sb = new StringBuilder();
				sb.append(LCR_PREFIX);
				sb.append(MESSAGE_SEPARATOR);
				sb.append(recvUid);
				this.mServer.sendMessage(sb.toString(), neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString(),
						Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString()));
			
		}else {
			// Forward message to own uid
			sb = new StringBuilder();
			sb.append(LCR_PREFIX);
			sb.append(MESSAGE_SEPARATOR);
			sb.append(mServer.getUid());
			this.mServer.sendMessage(sb.toString(), neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString(),
					Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString()));
		

		}

	}

}