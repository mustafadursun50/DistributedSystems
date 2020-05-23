package de.hhz.distributed.system.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.hhz.distributed.system.algo.LeadElector;
import de.hhz.distributed.system.app.Constants;

public class MulticastReceiver implements Runnable {
	private MulticastSocket mMulticastSocket;
	private InetAddress group;
	private byte[] buf = new byte[256];
	private int serverPort;
	int uid;

	//List of host uid with ip and ports
	Map<String, Properties> knownHosts = new HashMap<String, Properties>();

	public MulticastReceiver(int uid, int port) {
		try {
			group = InetAddress.getByName(Constants.MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.serverPort = port;
		this.uid = uid;
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

	/**
	 * Get neihbor server
	 * @return
	 */
	public Properties getNeihbor() {
		if (knownHosts.size() > 0) {
			int neihborUid = (this.uid + 1) % knownHosts.size();
			if (this.knownHosts.containsKey(String.valueOf(neihborUid))) {
				return knownHosts.get(String.valueOf(neihborUid));
			}
		}
		return null;
	}

	public void sendMulticastMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.uid);
		sb.append(LeadElector.MESSAGE_SEPARATOR);
		sb.append(this.serverPort);
		DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
				this.group, Constants.MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
			// System.out.println("send multicast msg from port: " + portAsString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			mMulticastSocket = new MulticastSocket(Constants.MULTICAST_PORT);
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
					hostProperties.put(Constants.PROPERTY_HOST_ADDRESS, packet.getAddress().getHostAddress());
					hostProperties.put(Constants.PROPERTY_HOST_PORT, hostPort);
//Fill host list
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
