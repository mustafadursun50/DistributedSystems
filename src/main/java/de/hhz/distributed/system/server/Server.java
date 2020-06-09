package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hhz.distributed.system.algo.FifoDeliver;
import de.hhz.distributed.system.algo.LeadElector;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.handlers.MessageHandler;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private String uid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;
	private LeadElector mElector;
	private Timer updateClientsTimer;
	private String leadUid;
	private boolean isLeader;
	private boolean isElectionRunning;
	private FifoDeliver fifoDeliver;
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
		this.mMulticastReceiver = new MulticastReceiver(this.uid, this.port);
		this.mElector = new LeadElector(this);
		this.fifoDeliver = new FifoDeliver();
		this.sender = new Sender();
		doPing();
	}

	/**
	 * Leader send ping to replicas and say's i am here.
	 */
	public void doPing() {
		System.out.println("Start Ping mechanism..");
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
							// FailureDedector.updateLastOkayTime();
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

	public int getPort() {
		return this.port;
	}

	public void close() throws IOException {
		this.mServerSocket.close();
		this.mMulticastReceiver.close();
	}

	public void sendVotingMessage(final String message, String hostAddress, final int port) throws ClassNotFoundException {

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
		this.startVoting();

		while (true) {
			try {
				this.mSocket = this.mServerSocket.accept();
				String input = this.readMessage();
				String clientIp = mSocket.getInetAddress().getHostAddress();
				if (input.equals(Constants.PING_LEADER)) {
					sender.sendTCPMessage(Constants.PING_LEADER, this.mSocket);
				} else if (input.equals(Constants.PING_REPLICA)) {
					sender.sendTCPMessage(Constants.PING_REPLICA, this.mSocket);
				} else if (input.startsWith(LeadElector.LCR_PREFIX)) {
					isElectionRunning = true;
					this.mElector.handleVoting(input);
				} else {
					System.out.println("client connection accepted");
					System.out.println("handle msg: " + input);
					new Thread(new MessageHandler(input, clientIp, this.port)).start();
				}
				this.mSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startVoting() {
		try {
			this.isElectionRunning = true;
			mElector.initiateVoting();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getLeadUid() {
		return leadUid;
	}

	/**
	 * set uid of leader
	 * 
	 * @param leadUid
	 */
	public void setLeadUid(String leadUid) {
		this.leadUid = leadUid;
		// start failure detector once the leader is known
		isElectionRunning = false;// election completed. a new election can now be trigger.
	}

	public void setIsLeader(boolean isLeader) {
		this.isLeader = isLeader;
		if (updateClientsTimer != null) {
			updateClientsTimer.cancel();
		}
		if (isLeader) {
			System.out.println("server is leader: " + this.port);
			// stop failure detector because the server becomes leader
			this.isElectionRunning = false;
			updateClientsTimer = new Timer();
			System.out.println("start permanent client update..");
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					try {
						permanentClientUpdate();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			};
			updateClientsTimer.schedule(task, 10, 800);
		}
	}

	private void permanentClientUpdate() throws IOException {
		String dataWithSequenceId = this.fifoDeliver.getCurrentDbDataWithUpdatedSequenceId();
		String dataToSent = this.port + "," + dataWithSequenceId;
		sender.sendMultiCastMessage(dataToSent, Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
	}

	public void stopLeading() {
		System.out.println("Stop leading");
		this.isLeader = false;
		if (this.updateClientsTimer != null) {
			this.updateClientsTimer.cancel();
		}
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
