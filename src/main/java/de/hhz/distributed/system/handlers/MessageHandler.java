package de.hhz.distributed.system.handlers;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import de.hhz.distributed.system.app.Constants;
import de.hhz.distributed.system.server.FailureDedector;

public class MessageHandler implements Runnable {

	private ServerSocket serverSocket = null;

	public MessageHandler(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void run() {
		// 3. Get Input Stream / Output Stream
		// 4. Send / Receive Data
		try {
	/*		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println("Willkommen bei ESHOP");
			out.println("Unsere Produkte");
			out.println(" ID | Artikel | Anzahl");
			out.println(" 1  | Milch   |  5");
			out.println(" 2  | Wurst   |  6");
			out.println(" 3  | Mehl    |  7");
			out.println("---------------------------------");
			out.println("Was moechten Sie bestellen?");
			out.println("Waehlen Sie die entsprechende ID!");
			out.println("---------------------------------");
			out.close();
			*/
			
			while(true) {
				Socket socket = serverSocket.accept();	
				ObjectInputStream mObjectInputStream = new ObjectInputStream(socket.getInputStream());
				String incommingMsg = (String) mObjectInputStream.readObject();
				
				if(incommingMsg.equals(Constants.PING_LEADER_TO_REPLICA)) {
					FailureDedector.updateLastOkayTime();
				}
				
				System.out.println("received: " + incommingMsg);
				
				mObjectInputStream.close();
				socket.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
