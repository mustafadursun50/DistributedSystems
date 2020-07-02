package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hhz.distributed.system.Utils.MessageQueue;
import de.hhz.distributed.system.algo.FifoDeliver;
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
	private Queue<MessageQueue> messageQueue;
	public Map<String, String> quotationList = new HashMap<String, String>();
	private Timer mProductTimer;
	private TimerTask mProductTimerTask;

	public Server(final int port) throws IOException, ClassNotFoundException {
		messageQueue = new LinkedList<MessageQueue>();
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
		this.startMessageQueueChecker();
	}

	/**
	 * Leader send ping to replicas and say's i am here.
	 */
	public void doPing() {
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
							System.out.println("ping ok " + port);

						} else {
							System.out.println("ping nok " + port);

							if (pingErrorCounter == 3) { // (Constants.MAX_PING_LIMIT_SEC /
															// Constants.PING_INTERVALL_SEC)) {
								System.out.println(mMulticastReceiver.getKnownHosts().toString());

								mMulticastReceiver.getKnownHosts().remove(leadUid.toString());
								leadUid = null;
								startVoting();
								pingErrorCounter = 0;
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

	private void startMessageQueueChecker() {

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (!messageQueue.isEmpty()) {
					MessageQueue message = messageQueue.peek();
					messageQueue.remove(message);
					System.out.println("Proceed message: " + message.getMessage());
					new Thread(new ClientMessageHandler(message.getMessage(), message.getSocket(), Server.this))
							.start();
				} else {
					// System.out.println("Queue is empty");
				}
			}
		};
		Timer timer = new Timer();
		timer.schedule(task, 0, 1000);
	}

	public void sendVotingMessage(final String message, String hostAddress, final int port)
			throws ClassNotFoundException {

		try {
			sender.sendTCPMessage(message, hostAddress, port);
		} catch (IOException e) {
			// Member could not be reached
			// Clean list an
			if (message.startsWith(LeadElector.LCR_PREFIX)) {
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
			try {
				this.mMulticastReceiver.sendMulticastMessage();
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			if (mMulticastReceiver.getKnownHosts().size() > 0) {
				this.startVoting();

			}
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
					if (this.isLeader) {
						String msg = Constants.UPDATE_REPLICA + "," + ProductDb.getCurrentData();
						this.updateReplicats(msg);
					}
					this.mElector.handleVoting(input);
					this.mSocket.close();
				} else if (input.startsWith(Constants.UPDATE_REPLICA)) {
					System.out.println("save update " + input);
					ProductDb.overrideProductDb(input);
				} else {
					System.out.println("client connection accepted");
					System.out.println("Put msg to queue" + input);
					this.messageQueue.add(new MessageQueue(this.mSocket, input));
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

	private void updateReplicats(String message) throws ClassNotFoundException, IOException {
		for (Properties p : this.mMulticastReceiver.getKnownHosts().values()) {
			String host = p.get(Constants.PROPERTY_HOST_ADDRESS).toString();
			int port = Integer.parseInt(p.get(Constants.PROPERTY_HOST_PORT).toString());
			this.sender.sendTCPMessage(message, host, port);
		}
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

	public void startReservationTimer(String address) {
		if (mProductTimer != null) {
			mProductTimer.cancel();
			System.out.println("ProductTimer canceld");
		}

		mProductTimerTask = new TimerTask() {

			@Override
			public void run() {
				String msgToSend = ProductDb.getCurrentData();

				String[] parts = msgToSend.split(",");
				String banana = parts[0];
				String milk = parts[1];
				String tomato = parts[2];
				String seqId = parts[3];

				msgToSend = banana + "," + milk + "," + tomato;
				sender.sendMultiCastMessage(FifoDeliver.assigneSequenceId(msgToSend),
						Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
				mProductTimer.cancel();
				if (quotationList.containsKey(address)) {
					quotationList.remove(address);
				}
			}
		};
		mProductTimer = new Timer();
		mProductTimer.schedule(mProductTimerTask, 10000);
		System.out.println("mProductTimer: " + mProductTimer);

	}

	public void cancelReservationTimer() {
		if (this.mProductTimer != null) {
			this.mProductTimer.cancel();
		}
	}

}
