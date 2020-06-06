package de.hhz.distributed.system.algo;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.HashMap;

import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;

public class FifoDeliver {

	private HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;
	
	public String getCurrentDbDataWithUpdatedSequenceId() {		
		deliveryQueue.put(++sequenceNo, ProductDb.getCurrentData());
		String finalData =  deliveryQueue.get(sequenceNo)+ "," + sequenceNo;
		return finalData;
	}
	
	public boolean deliverAskedMessage(String input) {
		long sequenceId = Long.parseLong(input.substring(6));
		String messageWithSequenceId = deliveryQueue.get(sequenceId) + "," + sequenceId;
		sentTCPToClient(messageWithSequenceId);
		System.out.println("for sequenceId: "+ sequenceId + "askedMessage succussfully sent: "+ deliveryQueue.get(sequenceId));
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
	
	private void sentTCPToClient(String productUpdate) {
		try {
			Socket socket = new Socket("192.168.178.37", 800);
			ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			mObjectOutputStream.writeObject(productUpdate);
			mObjectOutputStream.flush();
			mObjectOutputStream.close();
			socket.close();	
		} catch (Exception e) {
		}
	}
}
