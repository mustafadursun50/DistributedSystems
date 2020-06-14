package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hhz.distributed.system.algo.LeadElector;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;
import de.hhz.distributed.system.handlers.ClientMessageHandler;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private String uid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;
	private LeadElector mElector;
	private String leadUid;
	private boolean isLeader;
	private boolean isElectionRunning;
	int permanentUpdatedInterval;
	int pingErrorCounter;
	private Sender sender;

	public Server(final int port) throws IOException, ClassNotFoundException {

		this.mServerSocket = new ServerSocket(port);
		StringBuilder sb = new StringBuilder();
		sb.append(host.getHostAddress());
		sb.append("-");
		sb.append(port);
		this.uid = sb.toString();
		this.port = port;
		this.mMulticastReceiver = new MulticastReceiver(this.uid, this.port, this);
		this.mElector = new LeadElector(this);
		this.sender = new Sender();
		doPing();
	}

	/**
	 * Leader send ping to replicas and say's i am here.
	 */
	public void doPing() {
		System.out.println("Start Ping mechanism");
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					// ping leader
					if (!isLeader && (leadUid != null)) {
						Properties p = mMulticastReceiver.getAdressById(leadUid.toString());
						String host = p.get(Constants.PROPERTY_HOST_ADDRESS).toString();
						int port = Integer.parseInt(p.get(Constants.PROPERTY_HOST_PORT).toString());
						String answer = sender.sendAndReceiveTCPMessage(Constants.PING_LEADER, host, port);
						if (answer != null) {
							pingErrorCounter = 0;
						} else {
							System.out.println("ping nok " + port);
							if (pingErrorCounter == (Constants.MAX_PING_LIMIT_SEC / Constants.PING_INTERVALL_SEC)) {
								startVoting();
							}
							pingErrorCounter++;
						}
					}
					// ping replicates
					else if (isLeader()) {
						for (Properties p : mMulticastReceiver.getKnownHosts().values()) {
							String host = p.get(Constants.PROPERTY_HOST_ADDRESS).toString();
							int hostPort = Integer.parseInt(p.get(Constants.PROPERTY_HOST_PORT).toString());
							String answer = sender.sendAndReceiveTCPMessage(Constants.PING_REPLICA, host, hostPort);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		// Define: wait time for first run. And then periodically ping interval.
		service.scheduleAtFixedRate(runnable, Constants.START_FIRST_PING_AFTER_SEC, Constants.PING_INTERVALL_SEC,
				TimeUnit.SECONDS);

	}

	private String readMessage() throws IOException, ClassNotFoundException {
		ObjectInputStream mObjectInputStream = new ObjectInputStream(this.mSocket.getInputStream());
		String s = (String) mObjectInputStream.readObject();
		return s;
	}

	public void close() throws IOException {
		this.mServerSocket.close();
		this.mMulticastReceiver.close();
	}

	public void sendVotingMessage(final String message, String hostAddress, final int port)
			throws ClassNotFoundException {

		try {
			sender.sendTCPMessage(message, hostAddress, port);
		} catch (IOException e) {
			// Member could not be reached
			// Clean list an
			if (message.startsWith(LeadElector.LCR_PREFIX)) {
				e.printStackTrace();
				this.mMulticastReceiver.removeNeighbor();
				System.out.println(
						"Server " + hostAddress + ":" + port + " Could not be reached. Remove from known host");
				// Send election to next neighbor
				Properties neihborProps = this.mMulticastReceiver.getNeihbor();
				if (neihborProps == null) {
					this.setIsLeader(true);
					System.out.println("server has no more neihbor");
					// Server has no neighbor and should delacre itself as leader
					return;
				}
				String neihgborHost = neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString();
				int neihgborPort = Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString());

				this.sendVotingMessage(message, neihgborHost, neihgborPort);
			}
		}
	}

	/**
	 * Handle incoming messages
	 */
	public void run() {
		new Thread(this.mMulticastReceiver).start();
		// Wait for multicast receiver to start
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		this.mMulticastReceiver.sendMulticastMessage();
		try {
			Thread.sleep(300);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		System.out.println("Server UID " + uid + " listing on " + this.host.getHostAddress() + ":" + this.port);
		// Start election if host has a neighbor
		if (mMulticastReceiver.getKnownHosts().size() > 0) {
			this.startVoting();

		} else {
			this.isLeader = true;
		}

		while (true) {
			try {
				this.mSocket = this.mServerSocket.accept();
				String input = this.readMessage();
				if (input.equals(Constants.PING_LEADER)) {
					sender.sendTCPMessage(Constants.PING_LEADER, this.mSocket);
					this.mSocket.close();
				} else if (input.equals(Constants.PING_REPLICA)) {
					sender.sendTCPMessage(Constants.PING_REPLICA, this.mSocket);
					this.mSocket.close();
				} else if (input.startsWith(LeadElector.LCR_PREFIX)) {
					isElectionRunning = true;
					this.mElector.handleVoting(input);
					this.mSocket.close();
				} else if (input.startsWith(Constants.UPDATE_REPLICA)) {
					String data = input.substring(input.indexOf(":"), input.length());
					ProductDb.updateProductDb(data);
				} else {
					System.out.println("client connection accepted");
					System.out.println("handle msg: " + input);
					new Thread(new ClientMessageHandler(input, this.mSocket, this)).start();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startVoting() {
		if (!this.isElectionRunning) {
			try {
				this.isElectionRunning = true;
				mElector.initiateVoting();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int getPort() {
		return this.port;
	}

	public void setIsLeader(boolean isLeader) {
		this.isLeader = isLeader;

		if (isLeader) {
			System.out.println("server is leader: " + this.port);
			this.isElectionRunning = false;
			// Send multicast multiple times to ensure that the client received the message
			this.mMulticastReceiver.sendClientMulticastMessage();
			this.mMulticastReceiver.sendClientMulticastMessage();
		}
	}

	public void stopLeading() {
		System.out.println("Stop leading");
		this.isLeader = false;
	}

	public String getLeadUid() {
		return leadUid;
	}

	public void setLeadUid(String leadUid) {
		this.leadUid = leadUid;
		// start failure detector once the leader is known
		isElectionRunning = false;// election completed. a new election can now be trigger.
	}

	public String getUid() {
		return this.uid;
	}

	public MulticastReceiver getMulticastReceiver() {
		return this.mMulticastReceiver;
	}

	public boolean isLeader() {
		return this.isLeader;
	}

	public boolean isElectionRunning() {
		return isElectionRunning;
	}

	public void setElectionRunning(boolean isElectionRunning) {
		this.isElectionRunning = isElectionRunning;
	}
}
