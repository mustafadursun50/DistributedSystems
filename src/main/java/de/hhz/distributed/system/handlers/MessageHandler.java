package de.hhz.distributed.system.handlers;

import java.io.IOException;

import de.hhz.distributed.system.algo.FifoDeliver;
import de.hhz.distributed.system.db.ProductDb;
import de.hhz.distributed.system.server.Sender;

public class MessageHandler implements Runnable {
	private String inputMsg;
	private String clientIp;
	private int clientPort;
	private Sender sender;
	private FifoDeliver fifoDeliver;

	public MessageHandler(String input, String clientIp, int serverPort) {
		this.inputMsg = input;
		this.clientIp = clientIp;
		this.clientPort = serverPort;
		this.sender = new Sender();
		this.fifoDeliver = new FifoDeliver();
	}

	public void run() {		
		try {
			if(inputMsg.startsWith("0,0,0,")) {
				String missedMsg = fifoDeliver.deliverAskedMessage(inputMsg);
				if (missedMsg != null && !missedMsg.isEmpty()) {
					sender.sendTCPMessage(missedMsg, clientIp, clientPort);
				} else {
					System.out.println("ERROR: askedMessage not successfully sent");
				}
			}else {
			if (ProductDb.updateProductDb(this.inputMsg)) {
				this.sendClientMessage("OK", this.clientIp, this.clientPort);
			} else {
				this.sendClientMessage("NOK", this.clientIp, this.clientPort);
			}
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendClientMessage(final String message, String hostAddress, final int port) throws ClassNotFoundException {
		try {
			sender.sendTCPMessage(message, hostAddress, port);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
