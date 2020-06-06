package de.hhz.distributed.system.app;

import java.io.IOException;

import de.hhz.distributed.system.algo.LeadElectorListener;
import de.hhz.distributed.system.server.FailureDedector;
import de.hhz.distributed.system.server.Server;

public class App {

	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException {


		int port = Integer.parseInt(System.getProperty("port"));
		//start server on the given port
		Server server = new Server(port);
		new Thread(server).start();
		FailureDedector failureDedector = new FailureDedector();
		new LeadElectorListener(failureDedector, server);
		new Thread(failureDedector).start();
	}
}
