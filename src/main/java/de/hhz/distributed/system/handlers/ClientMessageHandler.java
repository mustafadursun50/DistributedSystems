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
			if(inputMsg.startsWith(Constants.PACKAGE_LOSS)) {
				String missedMsg = fifoDeliver.deliverAskedMessage(inputMsg);
				if (missedMsg != null && !missedMsg.isEmpty()) {
					sender.sendTCPMessage(missedMsg, this.socket);
				} else {
					System.out.println("ERROR: askedMessage not successfully sent");
				}
				
			}
			else if (inputMsg.startsWith("requestOrder")) {
				if(ProductDb.updateProductDb(this.inputMsg)) {
						sender.sendTCPMessage("responseOrder,OK", this.socket);
						String msgToSend = FifoDeliver.assigneSequenceId(ProductDb.getCurrentData());
                this.sender.sendMultiCastMessage(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
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
}
