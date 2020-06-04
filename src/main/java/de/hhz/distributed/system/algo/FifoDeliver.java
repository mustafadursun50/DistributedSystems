package de.hhz.distributed.system.algo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;

public class FifoDeliver {

	private HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;
	
	public String getCurrentDbDataWithUpdatedSequenceId() {		
		deliveryQueue.put(sequenceNo++, ProductDb.getCurrentData());
		return sequenceNo + "," + deliveryQueue.get(sequenceNo);
	}
	
	public boolean deliverAskedMessage(String input) {
		long sequenceId = Long.parseLong(input.substring(5));
		String messageWithSequenceId = deliveryQueue.get(sequenceId) + "," + sequenceId;
		try {
			sendClientMulticastMessage(messageWithSequenceId);
		} catch (IOException e) {
			return false;
		}
		 return true;
	}
	
	private void sendClientMulticastMessage(String productUpdate) throws IOException {
	MulticastSocket mMulticastSocket = new MulticastSocket(Constants.CLIENT_MULTICAST_PORT);
		StringBuilder sb = new StringBuilder();
		sb.append(productUpdate);
		DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
				InetAddress.getByName(Constants.CLIENT_MULTICAST_ADDRESS), Constants.CLIENT_MULTICAST_PORT);
		mMulticastSocket.send(msgPacket);
		mMulticastSocket.close();
	}
}
