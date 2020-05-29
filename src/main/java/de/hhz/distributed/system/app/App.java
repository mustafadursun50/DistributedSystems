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

		 List<Server> servers = new App().generateServers();
		
		FailureDedector failureDedector = new FailureDedector();
		new LeadElectorListener(failureDedector, servers);
		new Thread(failureDedector).start();
		ProductDb.initializeDb();
	}

	public List<Server> generateServers() throws IOException, ClassNotFoundException, InterruptedException {
		Server server = null;
        List<Server> servers=new ArrayList<Server>();

		for (int i = 0; i < Constants.NUMBER_OF_SERVERS; i++) {
			server = new Server(Constants.SERVER_PORT_START++);
			new Thread(server).start();
			servers.add(server);
		}
		return servers;
	}
}
