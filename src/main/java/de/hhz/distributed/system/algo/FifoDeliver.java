package de.hhz.distributed.system.algo;

import java.util.HashMap;

import de.hhz.distributed.system.db.ProductDb;

public class FifoDeliver {

	private static HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;

	public static String assigneSequenceId(String data) {
		System.out.println("sequenceNo: " + sequenceNo);

		if (sequenceNo == 0) {
			String productAsString = ProductDb.getCurrentData();
			System.out.println("productAsString: " + productAsString + "productDataSPlit" + productAsString.split(","));
			System.out.println("p: " + productAsString);

			sequenceNo = Long.parseLong(productAsString.split(",")[productAsString.split(",").length -1]);


		}
		String productAsString = ProductDb.getCurrentData();
		System.out.println("productAsString: " + productAsString + "productDataSPlit" + productAsString.split(","));
		System.out.println("p: " + productAsString);

		sequenceNo = Long.parseLong(productAsString.split(",")[productAsString.split(",").length -1]);

		
		deliveryQueue.put(++sequenceNo, data);
		System.out.println("--------seqNo--------: "+ sequenceNo +"------data-----: " + data);

		String dbWithSeqId = deliveryQueue.get(sequenceNo) + "," + sequenceNo;
		System.out.println("dbWithSeqId: "+ dbWithSeqId);

		return dbWithSeqId;
	}

	public String deliverAskedMessage(String input) {
		String[] splited = input.split(",");
		long sequenceId = Integer.parseInt(splited[1]);
		String messageWithSequenceId = deliveryQueue.get(sequenceId) + "," + sequenceId;
		System.out.println("for sequenceId: " + sequenceId + " askedMsg will be: " + deliveryQueue.get(sequenceId));
		return messageWithSequenceId;
	}
}
