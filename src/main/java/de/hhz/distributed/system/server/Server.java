package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hhz.distributed.system.algo.LeadElector;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.handlers.MessageHandler;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private int uid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;
	private LeadElector mElector;

	private int leadUid;
	private boolean isLeader;

	public Server(final int port, final int uid) throws IOException, ClassNotFoundException {

		this.mServerSocket = new ServerSocket(port);
		this.uid = uid;
		this.port = port;
		this.mMulticastReceiver = new MulticastReceiver(this.uid, this.port);
		this.mElector = new LeadElector(this);
		System.out.println("Server UID " + uid + " listing on " + this.host.getHostAddress() + ":" + this.port);

		doPing();
	}
/**
 * Leader send ping to replicas
 */
	public void doPing() {
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					if (isLeader) {
						for (Properties p : mMulticastReceiver.getKnownHosts().values()) {
							String host = p.get(Constants.PROPERTY_HOST_ADDRESS).toString();
							int port = Integer.parseInt(p.get(Constants.PROPERTY_HOST_PORT).toString());
							sendMessage(Constants.PING_LEADER_TO_REPLICA, host, port);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		// Ping Configuration: For first run wait 5 sec. And then periodically every 10
		// sec.
		service.scheduleAtFixedRate(runnable, 5, 10, TimeUnit.SECONDS);

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
	public void sendMessage(final String message, String hostAddress, final int port)
			throws IOException, ClassNotFoundException {
		Socket socket = new Socket(hostAddress, port);
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
		socket.close();
	}
/**
 * Handle incoming messages
 */
	public void run() {
		new Thread(this.mMulticastReceiver).start();
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
					new Thread(new MessageHandler(mSocket)).start();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMulticastMessage() {
		this.mMulticastReceiver.sendMulticastMessage();
	}

	public void startVoting() {
		try {
			mElector.initiateVoting();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getLeadUid() {
		return leadUid;
	}

	public void setLeadUid(int leadUid) {
		this.leadUid = leadUid;
	}

	public void setIsLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}

	public int getUid() {
		return this.uid;
	}

	public MulticastReceiver getMulticastReceiver() {
		return this.mMulticastReceiver;
	}

	public boolean isLeader() {
		return this.isLeader;
	}

}
