package de.hhz.distributed.system.algo;

import java.util.HashMap;

import de.hhz.distributed.system.db.ProductDb;

public class FifoDeliver {

	private HashMap<Long, String> deliveryQueue = new HashMap<Long, String>();
	private static long sequenceNo;
	
	public String assigneSequenceId() {		
		deliveryQueue.put(sequenceNo++, ProductDb.getCurrentData());
		return sequenceNo + "," + deliveryQueue.get(sequenceNo);
	}
	
	public String deliverAskedMessage(long sequenceNo) {
		 return deliveryQueue.get(sequenceNo);
	}
}
