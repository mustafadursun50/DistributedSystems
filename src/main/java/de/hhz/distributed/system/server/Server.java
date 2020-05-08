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
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private String uuid;
	private static final int PORT_MULTICAST = 4446;
	private MulticastSocket mMulticastSocket;
	private byte[] buf = new byte[256];
	private InetAddress group;
	private InetAddress host = InetAddress.getLocalHost();
	List<String> knownHosts = new ArrayList<String>();
	private static final String MULTICAST_ADDRESS = "230.0.0.0";
	private int port;

	public Server(final int port, final String uuid) throws IOException {
		this.mServerSocket = new ServerSocket(port);
		this.uuid = uuid;
		this.port=port;
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
		this.mMulticastSocket.leaveGroup(group);
		this.mMulticastSocket.close();
	}

	public synchronized void sendMessage(final String message, final int port)
			throws IOException, ClassNotFoundException {
		Socket socket = new Socket(this.host.getHostName(), port);
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		ObjectInputStream mObjectInputStream = new ObjectInputStream(socket.getInputStream());
		String received = (String) mObjectInputStream.readObject();
		System.out.println("server " + uuid + " recv " + received);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
		socket.close();
	}

	public void run() {
		MulticastReceiver multicastReceiver = new MulticastReceiver();
		new Thread(multicastReceiver).start();
		while (true) {
			try {
				this.mSocket = this.mServerSocket.accept();
				String input = this.readMessage();
				if (input != null) {
					System.out.println("server " + uuid + " : " + input);
					this.sendMessage("Got your msg");
				}
				this.mSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void sendMulticastMessage(String msg, DatagramPacket paket) {
		String portAsString = String.valueOf(this.port);
		DatagramPacket msgPacket = new DatagramPacket(portAsString.getBytes(), portAsString.getBytes().length, this.group,
				PORT_MULTICAST);
		try {
			this.mMulticastSocket.send(msgPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMulticastMessage() {
		String portAsString = String.valueOf(this.port);
		DatagramPacket msgPacket = new DatagramPacket(portAsString.getBytes(), portAsString.getBytes().length, this.group,
				PORT_MULTICAST);
		try {
			this.mMulticastSocket.send(msgPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class MulticastReceiver implements Runnable {

		public MulticastReceiver() {
			try {
				group = InetAddress.getByName(MULTICAST_ADDRESS);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				mMulticastSocket = new MulticastSocket(PORT_MULTICAST);
				mMulticastSocket.joinGroup(group);
				mMulticastSocket.setLoopbackMode(false);
				mMulticastSocket.setTimeToLive(1);
				while (true) {
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					mMulticastSocket.receive(packet);
					String receivedMsg = new String(packet.getData(), 0, packet.getLength());
					if (receivedMsg != null && !String.valueOf(port).equals(receivedMsg)) {
						String myHost = packet.getAddress().getHostAddress()+":"+receivedMsg;
						if (!knownHosts.contains(myHost)) {
							knownHosts.add(myHost);
							System.out.println("######server" + uuid + "##############");
							knownHosts.forEach(s -> {
								System.out.print(s+" ");
							});
							System.out.println("####################");
						}
						sendMulticastMessage(uuid, packet);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
