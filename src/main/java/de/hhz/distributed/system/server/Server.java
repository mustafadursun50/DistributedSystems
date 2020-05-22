package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.UUID;

import de.hhz.distributed.system.app.ApplicationConstants;
import de.hhz.distributed.system.handlers.MessageHandler;

public class Server implements Runnable {
	private static final String LCR_PREFIX = "LCR";
	private static final String MESSAGE_SEPARATOR = ":";
	private static final Object MESSAGE_COOR = "COOR";
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private int uid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;
	int leadUid;

	public Server(final int port, final int uid) throws IOException {
		this.mServerSocket = new ServerSocket(port);
		this.uid = uid;
		this.port = port;
		System.out.println("Server UID " + uid + " listing on " + this.host.getHostAddress() + ":" + this.port);
	}

	private synchronized void sendMessage(final String message) throws IOException {
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(this.mSocket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
	}

	private String readMessage() throws IOException, ClassNotFoundException {
		ObjectInputStream mObjectInputStream = new ObjectInputStream(this.mSocket.getInputStream());
		return (String) mObjectInputStream.readObject();
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
//		ObjectInputStream mObjectInputStream = new ObjectInputStream(socket.getInputStream());
//		String received = (String) mObjectInputStream.readObject();
//		System.out.println("server " + uid + " recv " + received);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
//		mObjectInputStream.close();
		socket.close();
	}

	public void run() {
		this.mMulticastReceiver = new MulticastReceiver(this.uid, this.port);
		new Thread(this.mMulticastReceiver).start();
		while (true) {
			try {
				this.mSocket = this.mServerSocket.accept();
				String input = this.readMessage();
				// LCR message
				if (input.startsWith(LCR_PREFIX)) {
					this.handleLCRMessage(input);
				} else {
					new Thread(new MessageHandler(mSocket)).start();

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Initiate LCR
	 * 
	 * @throws NumberFormatException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void initiateVoting() throws NumberFormatException, ClassNotFoundException, IOException {
		Properties neihborProps = this.mMulticastReceiver.getNeihbor();
		if (neihborProps == null) {
			System.out.println("Server has no neihbor");
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(LCR_PREFIX);
		sb.append(MESSAGE_SEPARATOR);
		sb.append(this.uid);
		System.out.println("Server UID " + this.uid + " initiate voting");

		this.sendMessage(sb.toString(), neihborProps.get(ApplicationConstants.PROPERTY_HOST_ADDRESS).toString(),
				Integer.parseInt(neihborProps.get(ApplicationConstants.PROPERTY_HOST_PORT).toString()));

	}

	public void handleLCRMessage(String input) throws NumberFormatException, ClassNotFoundException, IOException {
		int recvUid = -1;
		StringBuilder sb = new StringBuilder();
		boolean isCoorinationMsg = false;
		System.out.println("Server UID " + this.uid + " Recv " + input);

		if (input.split(MESSAGE_SEPARATOR).length > 1) {
			recvUid = Integer.parseInt(input.split(MESSAGE_SEPARATOR)[1]);
		}
		if (input.split(MESSAGE_SEPARATOR).length == 3) {
			isCoorinationMsg = true;
		}
		Properties neihborProps = this.mMulticastReceiver.getNeihbor();
//server should declare itself as coordinator or received coordination message
		if ((recvUid == this.uid) || isCoorinationMsg) {
			// The coordination message was initiated by this server. End message
			// transmission.
			if (recvUid == this.uid && isCoorinationMsg) {
				return;
			}
			this.leadUid = recvUid;
			sb = new StringBuilder();
			sb.append(LCR_PREFIX);
			sb.append(MESSAGE_SEPARATOR);
			// Coordination message received. Forward the message to neihbor
			if (isCoorinationMsg) {
				sb.append(recvUid);
			} else {
				// Server declare itself as coordinator
				sb.append(this.uid);
				System.out.println(
						" Election completed. Server UID " + this.uid + " won. Now send COOR to anothers servers");
			}
			sb.append(MESSAGE_SEPARATOR);
			sb.append(MESSAGE_COOR);
			String host = neihborProps.get(ApplicationConstants.PROPERTY_HOST_ADDRESS).toString();
			int port = Integer.parseInt(neihborProps.get(ApplicationConstants.PROPERTY_HOST_PORT).toString());
			this.sendMessage(sb.toString(), host, port);
		} else if (recvUid > this.uid) {
			// Forward message to neihbor
			if (neihborProps != null) {
				sb = new StringBuilder();
				sb.append(LCR_PREFIX);
				sb.append(MESSAGE_SEPARATOR);
				sb.append(recvUid);
				this.sendMessage(sb.toString(), neihborProps.get(ApplicationConstants.PROPERTY_HOST_ADDRESS).toString(),
						Integer.parseInt(neihborProps.get(ApplicationConstants.PROPERTY_HOST_PORT).toString()));
			}
		}

	}

	public void sendMulticastMessage() {
		this.mMulticastReceiver.sendMulticastMessage();
	}

}
