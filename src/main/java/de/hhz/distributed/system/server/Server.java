package de.hhz.distributed.system.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import de.hhz.distributed.system.handlers.MessageHandler;

public class Server implements Runnable {
	private ServerSocket mServerSocket;
	private Socket mSocket;
	private String uuid;
	private MulticastReceiver mMulticastReceiver;
	private InetAddress host = InetAddress.getLocalHost();
	private int port;

	public Server(final int port, final String uuid) throws IOException {
		this.mServerSocket = new ServerSocket(port);
		this.uuid = uuid;
		this.port = port;
		System.out.println("Server" + uuid + " listing on " + this.host.getHostAddress() + ":" + this.port);
	}

	private synchronized void sendMessage(final String message) throws IOException {
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(this.mSocket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		mObjectOutputStream.flush();
		mObjectOutputStream.close();
	}

	private String readMessage() throws IOException, ClassNotFoundException {
		ObjectInputStream mObjectInputStream = new ObjectInputStream(this.mSocket.getInputStream());
		return (String) mObjectInputStream.readObject();
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
	public synchronized void sendMessage(final String message, final int port)
			throws IOException, ClassNotFoundException {
		Socket socket = new Socket(this.host.getHostName(), port);
		ObjectOutputStream mObjectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		mObjectOutputStream.writeObject(message);
		ObjectInputStream mObjectInputStream = new ObjectInputStream(socket.getInputStream());
		String received = (String) mObjectInputStream.readObject();
		System.out.println("server " + uuid + " recv " + received);
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
				new Thread(new MessageHandler(mSocket)).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void sendMulticastMessage() {
		this.mMulticastReceiver.sendMulticastMessage();
	}

}
