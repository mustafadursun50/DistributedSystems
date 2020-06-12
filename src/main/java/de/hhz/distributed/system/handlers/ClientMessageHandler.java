package de.hhz.distributed.system.handlers;

import java.io.IOException;
import java.net.Socket;

import de.hhz.distributed.system.algo.FifoDeliver;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;
import de.hhz.distributed.system.server.Sender;

public class ClientMessageHandler implements Runnable {
	private String inputMsg;
	private String clientIp;
	private int clientPort;
	private Sender sender;
	private FifoDeliver fifoDeliver;
	private Socket socket;

	public ClientMessageHandler(String input, String clientIp, int serverPort, Socket socket) {
		this.inputMsg = input;
		this.clientIp = clientIp;
		this.clientPort = serverPort;
		this.sender = new Sender();
		this.fifoDeliver = new FifoDeliver();
		this.socket = socket;
		
	}



	public void run() {		
		try {
			if(inputMsg.startsWith("getHistoryState")) {
				String missedMsg = fifoDeliver.deliverAskedMessage(inputMsg);
				if (missedMsg != null && !missedMsg.isEmpty()) {
					sender.sendTCPMessage(missedMsg, clientIp, clientPort);
				} else {
					System.out.println("ERROR: askedMessage not successfully sent");
				}
			}
			else if (inputMsg.startsWith("requestOrder")) {
				if(ProductDb.updateProductDb(this.inputMsg)) {
					String msgToSend = fifoDeliver.assigneSequenceId(this.inputMsg);
					System.out.println("clientIp "+ clientIp);
					System.out.println("clientPort "+ clientPort);
					System.out.println("Socket " + this.socket);
					sender.sendTCPMessage("Hallo Gürkan", this.socket);
					
					System.out.println("TCP an den kaufenden Client bescheid geben das kauf geklappt hat");
					System.out.println("UPD an die Gruppe das Lagerbestand sich geändert hat");
					//this.sendClientUdp(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
					//this.sendClientMessage("responseOrder,OK", this.clientIp, this.clientPort);
				}
				else {
					sender.sendTCPMessage("responseOrder,NOK", this.socket);
					
				}
			}
			else {
				System.out.println("Not supportd msg type");
				sender.sendTCPMessage("responseOrder,NOK", this.socket);
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
	private void sendClientUdp(String message, String adress, int port) {
		sender.sendMultiCastMessage(message, adress, port);
	}
}
