package de.hhz.distributed.system.algo;

import java.util.HashMap;

import de.hhz.distributed.system.db.ProductDb;

public class FifoDeliver {

	private HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;
	
	public String getCurrentDbDataWithUpdatedSequenceId() {		
		deliveryQueue.put(++sequenceNo, ProductDb.getCurrentData());
		String finalData =  deliveryQueue.get(sequenceNo)+ "," + sequenceNo;
		return finalData;
	}
	
	public String deliverAskedMessage(String input) {
		long sequenceId = Long.parseLong(input.substring(6));
		String messageWithSequenceId = deliveryQueue.get(sequenceId) + "," + sequenceId;
		//sentTCPToClient(messageWithSequenceId);
		System.out.println("for sequenceId: "+ sequenceId + " askedMsg will be: "+ deliveryQueue.get(sequenceId));
		 return messageWithSequenceId;
	}
}
