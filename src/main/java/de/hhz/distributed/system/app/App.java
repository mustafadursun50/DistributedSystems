package de.hhz.distributed.system.app;

import java.io.IOException;

import de.hhz.distributed.system.server.Server;

public class App {

	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException {

		new App().generateServers();

	}

	public void generateServers() throws IOException, ClassNotFoundException, InterruptedException {
		Server server = null;

		for (int i = 0; i < ApplicationConstants.NUMBER_OF_SERVERS; i++) {
			server = new Server(ApplicationConstants.SERVER_PORT_START++,
					ApplicationConstants.SERVER_UUID_START++);
			new Thread(server).start();
		}
		Thread.sleep(300);

		server.sendMulticastMessage();
		Thread.sleep(300);

		server.initiateVoting();
	}
}
