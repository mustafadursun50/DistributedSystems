////////SERVER/////////
// 1. Create a new Server Socket obj
// 2. Listen for incoming connection
// 3. Get Input Stream / Output Stream
// 4. Send / Receive Data
// 5. Close Connection
// 6. Wait for new Connection

package serverside;
import java.net.*;
import java.io.*;

public class Server {
	ServerSocket serverSocket;
	Socket clientSocket;
	InetAddress ip;
	boolean stop = false;

	// 1. Create a new Server Socket obj
	// Check if port already exist
	public void create(int port) {
		try {	
			serverSocket = new ServerSocket(port);
			System.out.println("Server socket created on port: "+ port);
			
			// 2. Listen for incoming connection
			// 6. Wait for new Connection
			while(!stop) {
				System.out.println("Waiting for client on port: " + serverSocket.getLocalPort());		
				System.out.println("IP Address:- " + InetAddress.getLocalHost() );	
				clientSocket = serverSocket.accept();	
				System.out.println("Client is connected");
				
				ClientThread clientThread = new ClientThread(clientSocket);
                clientThread.start();
			}
		} 	
		catch (IOException e) {	
			System.out.println("Port:" + port + " is open!");
			System.out.println("Cant create a new server socket object");
		}
	}
}