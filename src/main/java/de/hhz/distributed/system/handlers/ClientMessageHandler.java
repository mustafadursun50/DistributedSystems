package de.hhz.distributed.system.handlers;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	Timer mProductTimer;
	private TimerTask timerTask;

	public ClientMessageHandler(String input, Socket socket, Server server) {
		this.inputMsg = input;
		this.sender = new Sender();
		this.fifoDeliver = new FifoDeliver();
		this.socket = socket;
		this.server = server;
	}

	private void lockProductTimer() {
		if (mProductTimer != null) {
			mProductTimer.cancel();
			System.out.println("ProductTimer canceld");
		}

		timerTask = new TimerTask() {

			@Override
			public void run() {
				String msgToSend = ProductDb.getCurrentData();
				sender.sendMultiCastMessage(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS,
						Constants.CLIENT_MULTICAST_PORT);
				System.out.println("Timer fertig: " + msgToSend);
				mProductTimer.cancel();
			}
		};
		mProductTimer = new Timer();
		mProductTimer.schedule(timerTask, 100);
		System.out.println("mProductTimer: " + mProductTimer);

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

			} else if (inputMsg.startsWith("reserve")) {
				// Multicast an Gruppe mit ( "bananaLock" )
				System.out.println("IN RESERVE");

				System.out.println("mProductTimerAfter: " + mProductTimer);

				String[] order = inputMsg.split(",");
				int bananaReq = Integer.parseInt(order[1]);
				int milkReq = Integer.parseInt(order[2]);
				int tomateReq = Integer.parseInt(order[3]);

				String products = Files.readAllLines(Paths.get(Constants.PRODUCT_DB_NAME)).get(0);
				String[] splitedDb = products.split(",");
				int bananaDb = Integer.parseInt(splitedDb[0]);
				int milkDb = Integer.parseInt(splitedDb[1]);
				int tomatoDb = Integer.parseInt(splitedDb[2]);
				String reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq) + ","
						+ (milkDb - milkReq);
				if (bananaReq > 0) {
					// Reserver banana
					if (bananaDb >= bananaReq) {
						if (bananaReq + 3 <= bananaDb) {
							reservationMsg = (bananaDb - bananaReq - 3) + "," + (tomatoDb - tomateReq - 3) + ","
									+ (milkDb - milkReq);

							sender.sendTCPMessage("banana,reservation,OK,"+ (bananaReq + 3)+",tomato,reservation2,OK,3", this.socket);

						} else {

							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq) + ","
									+ (milkDb - milkReq);

							sender.sendTCPMessage("banana,reservation,OK", this.socket);

						}
						this.sender.sendMultiCastMessage(reservationMsg,
								Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
						lockProductTimer();

					} else {
						sender.sendTCPMessage("banana,reservation,NOK," + bananaDb, this.socket);
					}

				} else if (tomateReq > 0) {
					// Reserver tomato
					if (tomateReq <= tomatoDb) {
						if (tomateReq + 1 <= tomatoDb) {
							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq - 1) + ","
									+ (milkDb - milkReq);
							sender.sendTCPMessage("tomato,reservation,OK,"+ (tomateReq + 1)+",banana,reservation2,OK,1", this.socket);
						} else {
							reservationMsg = (bananaDb - bananaReq - 1) + "," + (tomatoDb - tomateReq) + ","
									+ (milkDb - milkReq);
							sender.sendTCPMessage("tomato,reservation,OK", this.socket);

						}
						this.sender.sendMultiCastMessage(reservationMsg,
								Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
						lockProductTimer();

					} 
					else {
						sender.sendTCPMessage("tomato,reservation,NOK," + tomatoDb, this.socket);
					}

				} else if (milkReq > 0) {
					// Reserve milk
					if (milkReq <= milkDb) {

						if (milkReq + 2 <= milkDb) {
							sender.sendTCPMessage("milk,reservation,OK,"+ (milkReq + 2)+",tomato,reservation2,OK,2", this.socket);

							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq) + ","
									+ (milkDb - milkReq - 2);

						} else {
							sender.sendTCPMessage("milk,reservation,OK", this.socket);
							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq - 2) + ","
									+ (milkDb - milkReq - 2);

						}
						this.sender.sendMultiCastMessage(reservationMsg,
								Constants.CLIENT_MULTICAST_ADDRESS, Constants.CLIENT_MULTICAST_PORT);
						lockProductTimer();
					}
					else {
						sender.sendTCPMessage("milk,reservation,NOK," + milkDb, this.socket);
					}
					
				} 
				
			}

			else if (inputMsg.startsWith("requestOrder")) {
				if (ProductDb.updateProductDb(this.inputMsg)) {
					sender.sendTCPMessage("responseOrder,OK", this.socket);
					Thread.sleep(100);

					System.out.println("input: " + this.inputMsg + " lock " + mProductTimer);

					if ((mProductTimer != null)) {
						mProductTimer.cancel();
						System.out.println("Product timer canceled");
						sender.sendTCPMessage(Constants.DISCOUNT, this.socket);
						return;
					}

					String msgToSend = ProductDb.getCurrentData();
					this.sender.sendMultiCastMessage(msgToSend, Constants.CLIENT_MULTICAST_ADDRESS,
							Constants.CLIENT_MULTICAST_PORT);

					StringBuilder sb = new StringBuilder();
					sb.append(Constants.UPDATE_REPLICA);
					sb.append(",");
					sb.append(ProductDb.getCurrentData());
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
