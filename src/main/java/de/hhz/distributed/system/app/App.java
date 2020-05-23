package de.hhz.distributed.system.app;

import java.io.IOException;

import de.hhz.distributed.system.algo.LeadElectorListener;
import de.hhz.distributed.system.server.FailureDedector;
import de.hhz.distributed.system.server.Server;

public class App {

	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException {

		new App().generateServers();
		
		FailureDedector failureDedector = new FailureDedector();
		new Thread(failureDedector).start();
		new LeadElectorListener(failureDedector);
	}

	public void generateServers() throws IOException, ClassNotFoundException, InterruptedException {
		Server server = null;


		for (int i = 0; i < Constants.NUMBER_OF_SERVERS; i++) {
			server = new Server(Constants.SERVER_PORT_START++,
					Constants.SERVER_UUID_START++);
			new Thread(server).start();
		}
		
		Thread.sleep(300);
		//Zur Beginn: letzte Server wird direkt Leader gewählt. Kann später mit LeadElector Logik abgelöst werden.
		server.sendMulticastMessage();
		Thread.sleep(300);
		server.startVoting();
	}
}
