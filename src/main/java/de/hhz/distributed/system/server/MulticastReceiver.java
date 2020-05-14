package de.hhz.distributed.system.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import de.hhz.distributed.system.app.ApplicationConstants;

public class MulticastReceiver implements Runnable {
	private MulticastSocket mMulticastSocket;
	private InetAddress group;
	private byte[] buf = new byte[256];
	private String uuid;
	private int port;

	List<String> knownHosts = new ArrayList<String>();

	public MulticastReceiver(String uuid, int port) {
		try {
			group = InetAddress.getByName(ApplicationConstants.MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.uuid = uuid;
		this.port = port;
	}

	public void close() throws IOException {
		this.mMulticastSocket.leaveGroup(group);
		this.mMulticastSocket.close();
	}

	private void sendMulticastMessage(String msg, DatagramPacket paket) {
		String portAsString = String.valueOf(this.port);
		DatagramPacket msgPacket = new DatagramPacket(portAsString.getBytes(), portAsString.getBytes().length,
				this.group, ApplicationConstants.MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
			System.out.println("MulticastSend to port: " + portAsString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMulticastMessage() {
		String portAsString = String.valueOf(this.port);
		DatagramPacket msgPacket = new DatagramPacket(portAsString.getBytes(), portAsString.getBytes().length,
				this.group, ApplicationConstants.MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
			System.out.println("First Multicast Send to port: " + portAsString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			mMulticastSocket = new MulticastSocket(ApplicationConstants.MULTICAST_PORT);
			mMulticastSocket.joinGroup(group);
			mMulticastSocket.setLoopbackMode(false);
			mMulticastSocket.setTimeToLive(1);
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				mMulticastSocket.receive(packet);
				String receivedMsg = new String(packet.getData(), 0, packet.getLength());
				if (receivedMsg != null && !String.valueOf(port).equals(receivedMsg)) {
					String myHost = packet.getAddress().getHostAddress() + ":" + receivedMsg;
					if (!knownHosts.contains(myHost)) {
						knownHosts.add(myHost);
						sendMulticastMessage(uuid, packet);
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
