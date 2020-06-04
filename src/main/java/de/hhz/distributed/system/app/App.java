package de.hhz.distributed.system.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hhz.distributed.system.algo.LeadElectorListener;
import de.hhz.distributed.system.db.ProductDb;
import de.hhz.distributed.system.server.FailureDedector;
import de.hhz.distributed.system.server.Server;

public class App {

	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException {

	//	int port = Integer.parseInt(System.getProperty("port"));
		int port = 400;
		ProductDb.initializeDb();
		Server server = new Server(port);
		List<Server> servers = new ArrayList<Server>();
		servers.add(server);
		new Thread(server).start();
		FailureDedector failureDedector = new FailureDedector();
		new LeadElectorListener(failureDedector, servers);
		new Thread(failureDedector).start();
	}

	public List<Server> generateServers() throws IOException, ClassNotFoundException, InterruptedException {
		Server server = null;
		List<Server> servers = new ArrayList<Server>();

		for (int i = 0; i < Constants.NUMBER_OF_SERVERS; i++) {
			server = new Server(Constants.SERVER_PORT_START++);
			new Thread(server).start();
			servers.add(server);
		}
		return servers;
	}
}
