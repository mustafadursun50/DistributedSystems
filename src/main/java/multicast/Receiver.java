package multicast;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class Receiver {

  public static void main(String[] args) {
    // Netzwerk-Gruppe
    String NETWORK_GROUP = "230.0.0.1";
    // Netzwerk-Gruppen Port
    int NETWORK_GROUP_PORT = 4447;
   
    // Nachrichten-Codierung
    String TEXT_ENCODING = "UTF8";
   
    InetAddress group;
    MulticastSocket socket;
 
    try {
      // Gruppe anlegen
      group = InetAddress.getByName(NETWORK_GROUP);
      socket = new MulticastSocket(NETWORK_GROUP_PORT);
     
      // Gruppe beitreten
      socket.joinGroup(group);
     
      byte[] bytes = new byte[65536];
      DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
     
      while(true){
        // Warten auf Nachricht
        socket.receive(packet);
        String message = new String(packet.getData(),0,packet.getLength(), TEXT_ENCODING);
        System.out.println(message);
      }   
     
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}