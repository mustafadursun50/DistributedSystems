package de.hhz.distributed.system.Utils;

import java.net.Socket;

public class MessageQueue {
	private String message;
	private Socket socket;
	private String timestamp;

	public String getMessage() {
		return message;
	}

	public MessageQueue(Socket socket, String message, String timestamp) {
		this.socket = socket;
		this.message = message;
		this.timestamp = timestamp;
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

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
}
