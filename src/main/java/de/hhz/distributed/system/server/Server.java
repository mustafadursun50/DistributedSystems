package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hhz.distributed.system.algo.LeadElector;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.handlers.MessageHandler;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private UUID uid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;
	private LeadElector mElector;
	private Timer updateClientsTimer;
	private UUID leadUid;
	private boolean isLeader;

	public Server(final int port) throws IOException, ClassNotFoundException {

		this.mServerSocket = new ServerSocket(port);
		this.uid = UUID.randomUUID();
		this.port = port;
		this.mMulticastReceiver = new MulticastReceiver(this.uid, this.port);
		this.mElector = new LeadElector(this);
		doPing();
	}

	/**
	 * Leader send ping to replicas and say's i am here.
	 */
	public void doPing() {
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					if (isLeader) {
						for (Properties p : mMulticastReceiver.getKnownHosts().values()) {
							String host = p.get(Constants.PROPERTY_HOST_ADDRESS).toString();
							int port = Integer.parseInt(p.get(Constants.PROPERTY_HOST_PORT).toString());
							sendElectionMessage(Constants.PING_LEADER_TO_REPLICA, host, port);
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

	private synchronized void sendMessage(final String message) throws IOException {
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(this.mSocket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
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

	/**
	 * Send msg as client
	 * 
	 * @param message
	 * @param port
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void sendElectionMessage(final String message, String hostAddress, final int port)
			throws ClassNotFoundException {
		try {
			Socket socket = new Socket(hostAddress, port);
			ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			mObjectOutputStream.writeObject(message);
			mObjectOutputStream.flush();
			mObjectOutputStream.close();
			socket.close();
		} catch (IOException e) {
			// Member could not be reached
			// Clean list an
			this.mMulticastReceiver.removeNeighbor();
			// Send election to next neighbor
			Properties neihborProps = this.mMulticastReceiver.getNeihbor();
			String neihgborHost = neihborProps.get(Constants.PROPERTY_HOST_ADDRESS).toString();
			int neihgborPort = Integer.parseInt(neihborProps.get(Constants.PROPERTY_HOST_PORT).toString());

			this.sendElectionMessage(message, neihgborHost, neihgborPort);
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
		System.out.println("Server UID " + uid + " listing on " + this.host.getHostAddress() + ":" + this.port);

		while (true) {
			try {
				this.mSocket = this.mServerSocket.accept();
				String input = this.readMessage();
				if (input.startsWith(LeadElector.LCR_PREFIX)) {
					mSocket.close();
					this.mElector.handleVoting(input);
				} else if (input.equals(Constants.PING_LEADER_TO_REPLICA)) {
					mSocket.close();
					FailureDedector.updateLastOkayTime();
				} else {
					System.out.println("client connection accepted");
					new Thread(new MessageHandler(mSocket, Constants.CLIENT_MULTICAST_PORT)).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void startVoting() {
		try {
			mElector.initiateVoting();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public UUID getLeadUid() {
		return leadUid;
	}

	public void setLeadUid(UUID leadUid) {
		this.leadUid = leadUid;
	}

	public void setIsLeader(boolean isLeader) {
		this.isLeader = isLeader;
		if (updateClientsTimer != null) {
			updateClientsTimer.cancel();
		}
		if (isLeader) {
			updateClientsTimer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					try {
						SendUpdateDataStoreToClients();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			};
			updateClientsTimer.schedule(task, 10, 800);
		}
	}

	public void SendUpdateDataStoreToClients() throws IOException {
		MulticastSocket mMulticastSocket = new MulticastSocket(Constants.CLIENT_MULTICAST_PORT);
		StringBuilder sb = new StringBuilder();
		sb.append(this.port);
		DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
				InetAddress.getByName(Constants.CLIENT_MULTICAST_ADDRESS), Constants.CLIENT_MULTICAST_PORT);
		mMulticastSocket.send(msgPacket);
		mMulticastSocket.close();
	}

	public UUID getUid() {
		return this.uid;
	}

	public MulticastReceiver getMulticastReceiver() {
		return this.mMulticastReceiver;
	}

	public boolean isLeader() {
		return this.isLeader;
	}
}
