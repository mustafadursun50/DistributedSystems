package de.hhz.distributed.system.algo;

import java.util.HashMap;

public class FifoDeliver {

	private HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;
	
	public String assigneSequenceId(String data) {		
		deliveryQueue.put(++sequenceNo, data);
		String dbWithSeqId =  deliveryQueue.get(sequenceNo)+ "," + sequenceNo;
		return dbWithSeqId;
	}
	
	public String deliverAskedMessage(String input) {
		String [] splited = input.split(",");
		long sequenceId = Integer.parseInt(splited[1]);
		String messageWithSequenceId = deliveryQueue.get(sequenceId) + "," + sequenceId;
		System.out.println("for sequenceId: "+ sequenceId + " askedMsg will be: "+ deliveryQueue.get(sequenceId));
		 return messageWithSequenceId;
	}
}
