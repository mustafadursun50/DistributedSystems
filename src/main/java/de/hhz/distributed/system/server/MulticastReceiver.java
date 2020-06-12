package de.hhz.distributed.system.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.hhz.distributed.system.algo.LeadElector;
import de.hhz.distributed.system.app.Constants;

public class MulticastReceiver implements Runnable {
	private MulticastSocket mMulticastSocket;
	private InetAddress group;
	private byte[] buf = new byte[256];
	private int serverPort;
	String uid;
	String neighborUid;
	Server server;

	// List of host uid with ip and ports
	Map<String, Properties> knownHosts = new HashMap<String, Properties>();

	public MulticastReceiver(String uid, int port, Server server) {
		try {
			group = InetAddress.getByName(Constants.SERVER_MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.serverPort = port;
		this.uid = uid;
		this.server = server;
	}

	public Properties getAdressById(String uid) {
		return this.getKnownHosts().get(uid);
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
	 * 
	 * @return
	 */
	public Properties getNeihbor() {
		if (knownHosts.size() > 0) {
			List<String> uuids = new ArrayList<String>(knownHosts.keySet());
			// Sort list
			uuids.add(this.uid);
			Collections.sort(uuids);
			// Get my position in the list
			int pos = -1;
			for (int i = 0; i < uuids.size(); i++) {
				if (uuids.get(i).equals(this.uid)) {
					pos = i;
					break;
				}
			}
			// The neighbor sit at the next bigger position
			int neihborPos = (pos + 1) % uuids.size();
			if (this.knownHosts.containsKey(uuids.get(neihborPos))) {
				this.neighborUid = uuids.get(neihborPos);
				return knownHosts.get(uuids.get(neihborPos));
			}
		}
		return null;
	}

	public void removeNeighbor() {
		this.knownHosts.remove(this.neighborUid);
	}

	public void sendMulticastMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.uid);
		sb.append(LeadElector.MESSAGE_SEPARATOR);
		sb.append(this.serverPort);
		DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
				this.group, Constants.SERVER_MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
			// System.out.println("send multicast msg from port: " + portAsString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendClientMulticastMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.serverPort);
		sb.append(",");
		sb.append("5");
		sb.append(",");
		sb.append("10");
		sb.append(",");
		sb.append("15");
		sb.append(",");
		sb.append("1");
		try {
			DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
					InetAddress.getByName(Constants.CLIENT_MULTICAST_ADDRESS), Constants.CLIENT_MULTICAST_PORT);

			this.mMulticastSocket.send(msgPacket);
			 System.out.println("send multicast msg from port: " );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			mMulticastSocket = new MulticastSocket(Constants.SERVER_MULTICAST_PORT);
			mMulticastSocket.joinGroup(group);
			mMulticastSocket.setLoopbackMode(false);
			mMulticastSocket.setTimeToLive(1);
			while (true) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);

				mMulticastSocket.receive(packet);
				String receivedMsg = new String(packet.getData(), 0, packet.getLength());
				if (receivedMsg.equals(Constants.CLIENT_MULTICAST_MESSAGE)) {
					// Client sent a multicast message
					if (this.server.isLeader()) {
						this.sendClientMulticastMessage();
					}
				}
				else if (receivedMsg != null && receivedMsg.split(":").length == 2) {
					String hostUid = receivedMsg.split(":")[0];
					String hostPort = receivedMsg.split(":")[1];
					Properties hostProperties = new Properties();
					hostProperties.put(Constants.PROPERTY_HOST_ADDRESS, packet.getAddress().getHostAddress());
					hostProperties.put(Constants.PROPERTY_HOST_PORT, hostPort);
					// Fill host list
					if (!hostUid.equals(this.uid.toString()) && !knownHosts.containsKey(hostUid)) {
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
