package de.hhz.distributed.system.algo;

import java.util.HashMap;

import de.hhz.distributed.system.db.ProductDb;

public class FifoDeliver {

	private static HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;

	public static String assigneSequenceId(String data) {

		if (sequenceNo == 0) {
			String productAsString = ProductDb.getCurrentData();

			sequenceNo = Long.parseLong(productAsString.split(",")[productAsString.split(",").length -1]);


		}
		String productAsString = ProductDb.getCurrentData();

		sequenceNo = Long.parseLong(productAsString.split(",")[productAsString.split(",").length -1]);

		
		deliveryQueue.put(++sequenceNo, data);

		String dbWithSeqId = deliveryQueue.get(sequenceNo) + "," + sequenceNo;

		return dbWithSeqId;
	}

	public String deliverAskedMessage(String input) {
		String[] splited = input.split(",");
		long sequenceId = Integer.parseInt(splited[1]);
		String messageWithSequenceId = deliveryQueue.get(sequenceId) + "," + sequenceId;
		if(deliveryQueue.get(sequenceId)!= null) {
					System.out.println("packet loss for sequenceId: "+sequenceId+ " send data again to client:" + deliveryQueue.get(sequenceId));

		}
		return messageWithSequenceId;
	}
}
