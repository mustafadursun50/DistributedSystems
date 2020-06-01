package de.hhz.distributed.system.handlers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import de.hhz.distributed.system.algo.FifoDeliver;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;

public class MessageHandler implements Runnable {
	private MulticastSocket mMulticastSocket;
	private InetAddress group;
	private FifoDeliver fifoDeliver;
	private String message;
	private String clientIp;
	private int clientPort;

	public MessageHandler(String input, String clientIp, int serverPort) {
		this.message = input;
		this.clientIp = clientIp;
		this.clientPort = serverPort;
		try {
			group = InetAddress.getByName(Constants.CLIENT_MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		this.fifoDeliver = new FifoDeliver();
	}

	public void run() {
		
		
		
		try {
			System.out.println("THIS: "+this.message);
			if (ProductDb.updateProductDb(this.message)) {
				String updatedDbData = fifoDeliver.assigneSequenceId();
				this.sendClientMulticastMessage(updatedDbData);
			} else {
				//this.sendClientMessage("NotSupportedMessageType..", this.clientIp, this.clientPort);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	private void sendClientMessage(final String message, String hostAddress, final int port) {
		try {
			Socket socket = new Socket(hostAddress, port);
			ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			mObjectOutputStream.writeObject(message);
			mObjectOutputStream.flush();
			mObjectOutputStream.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
