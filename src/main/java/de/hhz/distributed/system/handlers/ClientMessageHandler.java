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
				if (server.quotationList.containsKey(socket.getLocalAddress().getHostAddress())) {
					server.quotationList.remove(socket.getLocalAddress().getHostAddress());
				}
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
							int quantity = bananaReq + 3;
							sender.sendTCPMessage("banana,reservation,OK," + quantity + ",tomato,reservation2,OK,3",
									this.socket);
							this.server.quotationList.put(this.socket.getLocalAddress().getHostAddress(),
									"3,t:" + quantity + ",b");

						} else {

							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq) + ","
									+ (milkDb - milkReq);

							sender.sendTCPMessage("banana,reservation,OK", this.socket);

						}
						this.sender.sendMultiCastMessage(reservationMsg, Constants.CLIENT_MULTICAST_ADDRESS,
								Constants.CLIENT_MULTICAST_PORT);
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
							int quantity = tomateReq + 1;
							sender.sendTCPMessage(
									"tomato,reservation,OK," + (tomateReq + 1) + ",banana,reservation2,OK,1",
									this.socket);
							this.server.quotationList.put(this.socket.getLocalAddress().getHostAddress(),
									"1,b:" + quantity + ",t");

						} else {
							reservationMsg = (bananaDb - bananaReq - 1) + "," + (tomatoDb - tomateReq) + ","
									+ (milkDb - milkReq);
							sender.sendTCPMessage("tomato,reservation,OK", this.socket);

						}
						this.sender.sendMultiCastMessage(reservationMsg, Constants.CLIENT_MULTICAST_ADDRESS,
								Constants.CLIENT_MULTICAST_PORT);
						lockProductTimer();

					} else {
						sender.sendTCPMessage("tomato,reservation,NOK," + tomatoDb, this.socket);
					}

				} else if (milkReq > 0) {
					// Reserve milk
					if (milkReq <= milkDb) {
						if (milkReq + 2 <= milkDb) {
							int quantity = milkReq + 2;

							sender.sendTCPMessage("milk,reservation,OK," + (milkReq + 2) + ",tomato,reservation2,OK,2",
									this.socket);
							this.server.quotationList.put(this.socket.getLocalAddress().getHostAddress(),
									"2,t:" + quantity + ",m");
							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq) + ","
									+ (milkDb - milkReq - 2);
						} else {
							sender.sendTCPMessage("milk,reservation,OK", this.socket);
							reservationMsg = (bananaDb - bananaReq) + "," + (tomatoDb - tomateReq - 2) + ","
									+ (milkDb - milkReq - 2);

						}
						this.sender.sendMultiCastMessage(reservationMsg, Constants.CLIENT_MULTICAST_ADDRESS,
								Constants.CLIENT_MULTICAST_PORT);
						lockProductTimer();
					} else {
						sender.sendTCPMessage("milk,reservation,NOK," + milkDb, this.socket);
					}

				}

			}

			else if (inputMsg.startsWith("requestOrder")) {
				String answer = null;
				String[] splitedReq = inputMsg.split(",");
				int bananaReq = Integer.parseInt(splitedReq[1]);
				int milkReq = Integer.parseInt(splitedReq[2]);
				int tomatoReq = Integer.parseInt(splitedReq[3]);
				if (bananaReq > 0) {
					answer = "responseOrder,OK,banana," + bananaReq;
				} else if (milkReq > 0) {
					answer = "responseOrder,OK,milk," + milkReq;
				} else if (tomatoReq > 0) {
					answer = "responseOrder,OK,tomato," + tomatoReq;
				}
				if (this.server.quotationList.containsKey(this.socket.getLocalAddress().getHostAddress())) {
					String quotationAsString = this.server.quotationList
							.get(this.socket.getLocalAddress().getHostAddress());
					String gift = quotationAsString.split(":")[0];
					String toBuy = quotationAsString.split(":")[1];

					// "2,t:"+quantity+",m"
					if (toBuy.split(",")[1].equals("b") && bananaReq >= Integer.parseInt(toBuy.split(",")[0])) {
						if (gift.split(",")[1].equals("t")) {
							tomatoReq = tomatoReq + Integer.parseInt(gift.split(",")[0]);
							answer = "responseOrder,OK,banana," + bananaReq + ",gift,tomato," + gift.split(",")[0];
						} else if (gift.split(",")[1].equals("m")) {
							milkReq = milkReq + Integer.parseInt(gift.split(",")[0]);
							answer = "responseOrder,OK,banana," + bananaReq + ",gift,milk," + gift.split(",")[0];
						}
					} else if (toBuy.split(",")[1].equals("m") && milkReq >= Integer.parseInt(toBuy.split(",")[0])) {
						if (gift.split(",")[1].equals("t")) {
							tomatoReq = tomatoReq + Integer.parseInt(gift.split(",")[0]);
							answer = "responseOrder,OK,milk," + milkReq + ",gift,tomato," + gift.split(",")[0];

						} else if (gift.split(",")[1].equals("b")) {
							bananaReq = bananaReq + Integer.parseInt(gift.split(",")[0]);
							answer = "responseOrder,OK,milk," + milkReq + ",gift,tomato," + gift.split(",")[0];

						}
					}

					else if (toBuy.split(",")[1].equals("t") && tomatoReq >= Integer.parseInt(toBuy.split(",")[0])) {
						if (gift.split(",")[1].equals("b")) {
							bananaReq = bananaReq + Integer.parseInt(gift.split(",")[0]);
							answer = "responseOrder,OK,tomato," + tomatoReq + ",gift,banana," + gift.split(",")[0];

						} else if (gift.split(",")[1].equals("m")) {
							milkReq = milkReq + Integer.parseInt(gift.split(",")[0]);
							answer = "responseOrder,OK,tomato," + tomatoReq + ",gift,milk," + gift.split(",")[0];

						}
					}

					this.inputMsg = "requestOrder," + bananaReq + "," + tomatoReq + "," + milkReq;
					server.quotationList.remove(socket.getLocalAddress().getHostAddress());
				}

				if (ProductDb.updateProductDb(this.inputMsg)) {
					sender.sendTCPMessage(answer, this.socket);
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
