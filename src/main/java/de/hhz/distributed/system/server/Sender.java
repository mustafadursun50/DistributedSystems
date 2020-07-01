package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;

public class Sender {

	public void sendTCPMessage(final String message, String hostAddress, final int port) throws IOException {

		try {
			Socket socket = new Socket(hostAddress, port);
			socket.setSoTimeout(10);
			ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			mObjectOutputStream.writeObject(message);
			mObjectOutputStream.flush();
			mObjectOutputStream.close();
			socket.close();
		} catch (IOException e) {
			throw e;
		}
	}

	public synchronized void sendTCPMessage(final String message, Socket socket) throws IOException {
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectOutputStream.writeObject(message);
		objectOutputStream.flush();
		// objectOutputStream.close();
		// socket.close();
	}

	public void sendMultiCastMessage(String message, String adress, final int port) {
		try {
			MulticastSocket mMulticastSocket = new MulticastSocket(port);
			StringBuilder sb = new StringBuilder();
			sb.append(message);
			DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
					InetAddress.getByName(adress), port);
			mMulticastSocket.send(msgPacket);
			mMulticastSocket.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			;
		}
	}

	public String sendAndReceiveTCPMessage(String message, String hostAddress, final int port) {
		String answer = null;
		try {
			
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(hostAddress, port), 1000);
			
			//Socket socket = new Socket(hostAddress, port);
			//socket.setSoTimeout(5);
			ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			mObjectOutputStream.writeObject(message);
			ObjectInputStream mObjectInputStream = new ObjectInputStream(socket.getInputStream());
			answer = (String) mObjectInputStream.readObject();
			mObjectOutputStream.flush();
			mObjectOutputStream.close();
			mObjectInputStream.close();
			socket.close();
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return answer;
	}
}
