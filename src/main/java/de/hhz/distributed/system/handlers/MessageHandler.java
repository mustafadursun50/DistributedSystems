package de.hhz.distributed.system.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;

public class MessageHandler implements Runnable {

	private Socket mSocket;
	private MulticastSocket mMulticastSocket;
	private InetAddress group;

	public MessageHandler(Socket socket, int port) {
		this.mSocket = socket;
		try {
			group = InetAddress.getByName(Constants.MULTICAST_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		// 3. Get Input Stream / Output Stream
		// 4. Send / Receive Data
		try {
			PrintWriter out = new PrintWriter(mSocket.getOutputStream(), true);
			out.println("Choose one: banana, milk");
			out.println("------------------------------");
			BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			String clientInput = input.readLine();

			System.out.println("ordered: " + clientInput);
			
			if(ProductDb.updateProductDb(clientInput)) {
				
				this.sendMulticastMessage(ProductDb.getData());
			}		

			// 5. Close Connection
			input.close();
			out.close();
			mSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendMulticastMessage(String productUpdate) {
		StringBuilder sb = new StringBuilder();
		sb.append(productUpdate);
		DatagramPacket msgPacket = new DatagramPacket(sb.toString().getBytes(), sb.toString().getBytes().length,
				this.group, Constants.MULTICAST_PORT);
		try {
			this.mMulticastSocket.send(msgPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
