package de.hhz.distributed.system.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import de.hhz.distributed.system.algo.FifoDeliver;
import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.db.ProductDb;
import de.hhz.distributed.system.server.Sender;
import de.hhz.distributed.system.server.Server;

public class ClientMessageHandler implements Runnable {
	private String inputMsg;
	private Sender sender;
	private FifoDeliver fifoDeliver;
	private Socket socket;
	private Server server;

	public ClientMessageHandler(String input, Socket socket, Server server) {
		this.inputMsg = input;
		this.sender = new Sender();
		this.fifoDeliver = new FifoDeliver();
		this.socket = socket;
		this.server = server;

	}

	public void run() {
		try {
			if (inputMsg.startsWith(Constants.PACKAGE_LOSS)) {
				String missedMsg = fifoDeliver.deliverAskedMessage(inputMsg);
				if (missedMsg != null && !missedMsg.isEmpty()) {
					sender.sendTCPMessage(missedMsg, this.socket);
				} else {
					System.out.println("ERROR: askedMessage not successfully sent");
				}

			} else if (inputMsg.startsWith("requestOrder")) {
				if (ProductDb.updateProductDb(this.inputMsg)) {
					sender.sendTCPMessage("responseOrder,OK", this.socket);
					Thread.sleep(2000);
					String msgToSend = FifoDeliver.assigneSequenceId(ProductDb.getCurrentData());
					this.sender.sendMultiCastMessage(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS,
							Constants.CLIENT_MULTICAST_PORT);

					StringBuilder sb = new StringBuilder();
					sb.append(Constants.UPDATE_REPLICA);
					sb.append(":");
					sb.append(inputMsg);
					this.updateReplicats(sb.toString());

				} else {
					sender.sendTCPMessage("responseOrder,NOK", this.socket);

				}
			} else {
				System.out.println("Not supportd msg type");
				sender.sendTCPMessage("responseOrder,NOK", this.socket);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateReplicats(String message) throws ClassNotFoundException, IOException {
		for (Properties p : this.server.getMulticastReceiver().getKnownHosts().values()) {
			String host = p.get(Constants.PROPERTY_HOST_ADDRESS).toString();
			int port = Integer.parseInt(p.get(Constants.PROPERTY_HOST_PORT).toString());
			this.sender.sendTCPMessage(message, host, port);
		}
	}
}
