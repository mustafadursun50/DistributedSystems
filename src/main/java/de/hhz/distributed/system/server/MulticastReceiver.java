package de.hhz.distributed.system.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.hhz.distributed.system.app.ApplicationConstants;

public class MulticastReceiver implements Runnable {
	private MulticastSocket mMulticastSocket;
	private InetAddress group;
	private byte[] buf = new byte[256];
	private int serverPort;
	int uid;
	private String multicastMessage;
	Map<String, Properties> knownHosts = new HashMap<String, Properties>();

	public MulticastReceiver(int uid, int serverPort) {
		try {
			group = InetAddress.getByName(ApplicationConstants.MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.serverPort = serverPort;
		this.uid = uid;
		StringBuilder sb = new StringBuilder();
		sb.append(this.uid);
		sb.append(":");
		sb.append(this.serverPort);
		this.multicastMessage = sb.toString();
	}

	public Properties getAdrressById(int uid) {
		return this.getKnownHosts().get(String.valueOf(uid));
	}

	public Map<String, Properties> getKnownHosts() {
		return this.knownHosts;
	}

	public void close() throws IOException {
		this.mMulticastSocket.leaveGroup(group);
		this.mMulticastSocket.close();
	}

	public Properties getNeihbor() {
		int neihborUid = (this.uid + 1) % knownHosts.size();
		if (this.knownHosts.containsKey(String.valueOf(neihborUid))) {
			return knownHosts.get(String.valueOf(neihborUid));
		}
		return null;
	}

	private void sendMulticastMessage(String msg, DatagramPacket paket) {
		String portAsString = String.valueOf(this.serverPort);
		DatagramPacket msgPacket = new DatagramPacket(portAsString.getBytes(), portAsString.getBytes().length,
				this.group, ApplicationConstants.MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMulticastMessage() {

		DatagramPacket msgPacket = new DatagramPacket(this.multicastMessage.getBytes(), this.multicastMessage.length(),
				this.group, ApplicationConstants.MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
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
				if (receivedMsg != null && receivedMsg.split(":").length == 2) {
					String hostUid = receivedMsg.split(":")[0];
					String hostPort = receivedMsg.split(":")[1];
					Properties hostProperties = new Properties();
					hostProperties.put(ApplicationConstants.PROPERTY_HOST_ADDRESS,
							packet.getAddress().getHostAddress());
					hostProperties.put(ApplicationConstants.PROPERTY_HOST_PORT, hostPort);

					if (!knownHosts.containsKey(hostUid)) {
						knownHosts.put(hostUid, hostProperties);
						sendMulticastMessage();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
