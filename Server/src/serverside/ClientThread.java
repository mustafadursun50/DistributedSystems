package serverside;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientThread extends Thread {

	private Socket socket = null;
	
	public ClientThread(Socket socket) {
		this.socket = socket;
	}
	
	public void run() {
		// 3. Get Input Stream / Output Stream
		// 4. Send / Receive Data
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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

			
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String clientInput = input.readLine();
			System.out.println(clientInput);
			
			// 5. Close Connection
			input.close();
			out.close();
			socket.close();
			
		}
		
		catch(Exception e){
			System.out.println(e.toString());
		}
	}

}
