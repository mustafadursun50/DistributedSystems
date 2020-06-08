package de.hhz.distributed.system.app;

import java.io.IOException;

import de.hhz.distributed.system.db.ProductDb;
import de.hhz.distributed.system.server.Server;

public class App {

	public static void main(String args[]) throws IOException, InterruptedException, ClassNotFoundException {

		ProductDb.initializeDb();
		Thread.sleep(1000);
		int port = Integer.parseInt(args[0]);
		Server server = new Server(port);
		new Thread(server).start();
	}
}
