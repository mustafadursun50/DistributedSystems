package de.hhz.distributed.system.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class Sender {

  public static void main(String[] args) {
    // Netzwerk-Gruppe
    String NETWORK_GROUP = "230.0.0.1";
    // Netzwerk-Gruppen Port
    int NETWORK_GROUP_PORT = 4447;
    // Client-Port
    int CLIENT_MULTICAST_PORT = 4446;
   
    // Nachrichten-Codierung
    String TEXT_ENCODING = "UTF8";
   
    try {
      MulticastSocket socket =  new MulticastSocket(CLIENT_MULTICAST_PORT);
      byte[] message = "Hallo Welt".getBytes(TEXT_ENCODING);
     
      // Nachricht an Gruppe senden
      socket.send(new DatagramPacket(message, message.length , InetAddress.getByName(NETWORK_GROUP) ,
NETWORK_GROUP_PORT));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}