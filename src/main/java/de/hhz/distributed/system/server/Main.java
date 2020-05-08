package de.hhz.distributed.system.server;

import java.io.IOException;

public class Main {
	final static int port1 = 800;
	final static int port2 = 801;
	final static int port3 = 802;
	final static int port4 = 803;
	final static int port5 = 804;

	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException {

		Server server1 = new Server(port1, "100");
		Server server2 = new Server(port2, "101");
		Server server3 = new Server(port3, "102");
		Server server4 = new Server(port4, "103");
		Server server5 = new Server(port5, "104");

		new Thread(server1).start();
		new Thread(server2).start();
		new Thread(server3).start();
		new Thread(server4).start();
		new Thread(server5).start();
		Thread.sleep(300);

		server1.sendMulticastMessage();

		server1.sendMessage("Hallo 1", port2);
		Thread.sleep(300);
		server2.sendMessage("Hallo 2", port1);

	}
}
