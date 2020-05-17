package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.hhz.distributed.system.algo.LeadElectorListener;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.handlers.MessageHandler;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private String uuid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;
	public boolean isLeader;

	public Server(final int port, final String uuid) throws IOException, ClassNotFoundException {
		this.mServerSocket = new ServerSocket(port);
		this.uuid = uuid;
		this.port = port;
		System.out.println("Server" + uuid + " listing on " + this.host.getHostAddress() + ":" + this.port);
		doPing();
	}
	
	public void doPing() {
	    Runnable runnable = new Runnable() {
	        public void run() {
	        	try {
	        		for(String s: mMulticastReceiver.knownHosts) {
	        			if(isLeader == true) {
	        				sendMessage(Constants.PING_LEADER_TO_REPLICA, Integer.parseInt(s.substring(s.lastIndexOf(":") + 1)));
	        			}
	        		}
				} catch (Exception e) {
					e.printStackTrace();
				}
	        }
	      };
	      
	      ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	      // Ping Configuration: For first run wait 5 sec. And then periodically every 10 sec.
	      service.scheduleAtFixedRate(runnable, 5, 10, TimeUnit.SECONDS);

	 }

	private synchronized void sendMessage(final String message) throws IOException {
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(this.mSocket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
	}

	private void readMessage() throws IOException, ClassNotFoundException {
		ObjectInputStream mObjectInputStream = new ObjectInputStream(this.mSocket.getInputStream());
		String s = (String) mObjectInputStream.readObject();
		System.out.println(s);
	}

	public void close() throws IOException {
		this.mServerSocket.close();
		this.mMulticastReceiver.close();
	}

	/**
	 * Send msg as client
	 * 
	 * @param message
	 * @param port
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public synchronized void sendMessage(final String message, final int port) throws IOException, ClassNotFoundException {
		Socket socket = new Socket(this.host.getHostName(), port);
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
		socket.close();
	}

	public void run() {
		this.mMulticastReceiver = new MulticastReceiver(this.uuid, this.port);
		new Thread(this.mMulticastReceiver).start();
		while (true) {
			try {
				this.mSocket = this.mServerSocket.accept();
				MessageHandler messageHandler = new MessageHandler(mServerSocket); 
				new Thread(messageHandler).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void sendMulticastMessage() {
		this.mMulticastReceiver.sendMulticastMessage();
	}

}
