package de.hhz.distributed.system.handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

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
	Timer lockProductTimer;
	
	public ClientMessageHandler(String input, Socket socket, Server server) {
		this.inputMsg = input;
		this.sender = new Sender();
		this.fifoDeliver = new FifoDeliver();
		this.socket = socket;
		this.server = server;

	}

	private void lockProductTimer() {
		TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				System.out.println("startet");

				String msgToSend = FifoDeliver.assigneSequenceId(ProductDb.getCurrentData());
				sender.sendMultiCastMessage(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS,
						Constants.CLIENT_MULTICAST_PORT);
				
				System.out.println("fertig");

			}
		};
		lockProductTimer = new Timer();
		lockProductTimer.schedule(timerTask, 10000);
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

			} 
			else if (inputMsg.startsWith("reserve")) {
				// Multicast an Gruppe mit ( "bananaLock" )
				System.out.println("IN RESERVE");
				this.sender.sendMultiCastMessage("lock,1,0,0", Constants.CLIENT_MULTICAST_ADDRESS,
						Constants.CLIENT_MULTICAST_PORT);
				lockProductTimer();
				// auf TCP Message antworten mit     reservieren erfolgreich 
				sender.sendTCPMessage(Constants.RESERVESUCCES, this.socket);
				
				
			}

			
			else if (inputMsg.startsWith("requestOrder")) {
				if (ProductDb.updateProductDb(this.inputMsg)) {
					sender.sendTCPMessage("responseOrder,OK", this.socket);
					Thread.sleep(100);
					
					System.out.println("input: "+ this.inputMsg + " lock " + lockProductTimer);

					if(this.inputMsg.equals("requestOrder,1,0,0") && lockProductTimer != null) {
						lockProductTimer.cancel();
						System.out.println("input: "+ this.inputMsg + " lock " + lockProductTimer);

					}

					
					String msgToSend = FifoDeliver.assigneSequenceId(ProductDb.getCurrentData());
					this.sender.sendMultiCastMessage(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS,
							Constants.CLIENT_MULTICAST_PORT);

					StringBuilder sb = new StringBuilder();
					sb.append(Constants.UPDATE_REPLICA);
					sb.append(",");
					sb.append(ProductDb.getCurrentData());
					this.updateReplicats(sb.toString());
					
					
				
					
					
					
					
				} 
				else {
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
