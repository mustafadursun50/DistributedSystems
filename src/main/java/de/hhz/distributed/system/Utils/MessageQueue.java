package de.hhz.distributed.system.Utils;

import java.net.Socket;

public class MessageQueue {
	private String message;
	private Socket socket;

	public String getMessage() {
		return message;
	}

	public MessageQueue(Socket socket, String message) {
		this.socket = socket;
		this.message = message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
