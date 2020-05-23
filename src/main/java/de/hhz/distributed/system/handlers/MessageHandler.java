package de.hhz.distributed.system.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageHandler implements Runnable {

	private Socket mSocket;

	public MessageHandler(Socket socket) {
		this.mSocket = socket;
	}

	public void run() {
		// 3. Get Input Stream / Output Stream
		// 4. Send / Receive Data
		try {
			PrintWriter out = new PrintWriter(mSocket.getOutputStream(), true);
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
			BufferedReader input = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			String clientInput = input.readLine();

			System.out.println(clientInput);

			// 5. Close Connection
			input.close();
			out.close();
			mSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
