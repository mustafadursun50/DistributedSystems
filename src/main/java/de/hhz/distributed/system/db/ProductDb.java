package de.hhz.distributed.system.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import de.hhz.distributed.system.algo.FifoDeliver;
import de.hhz.distributed.system.app.Constants;

public class ProductDb {

	private static File fileDb;
	private static PrintWriter pw;

	public static void initializeDb() {
		try {
			fileDb = new File(Constants.PRODUCT_DB_NAME);
			if (!fileDb.exists()) {
				fileDb.createNewFile();
				String initalDbLoad = 100 + "," + 80 + "," + 110+","+0;
				pw = new PrintWriter(fileDb);
				pw.println(initalDbLoad);
				pw.close();
				System.out.println("database initialized.");
			} else {
				System.out.println("database already there..");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized static boolean updateProductDb(String dataReq) {
		boolean updateSuccessful = false;
		System.out.println("Update database with " + dataReq);
		try {
			System.out.println("incoming order request: " + dataReq);
			String products = Files.readAllLines(Paths.get(Constants.PRODUCT_DB_NAME)).get(0);
			String[] splitedDb = products.split(",");
			int bananaDb = Integer.parseInt(splitedDb[0]);
			int milkDb = Integer.parseInt(splitedDb[1]);
			int tomatoDb = Integer.parseInt(splitedDb[2]);

			String[] splitedReq = dataReq.split(",");
			int bananaReq = Integer.parseInt(splitedReq[1]);
			int milkReq = Integer.parseInt(splitedReq[2]);
			int tomatoReq = Integer.parseInt(splitedReq[3]);

			if (bananaDb >= bananaReq && bananaReq > 0) {
				bananaDb += -bananaReq;
				updateSuccessful = true;
			}
			if (milkDb >= milkReq && milkReq > 0) {
				milkDb += -milkReq;
				updateSuccessful = true;
			}
			if (tomatoDb >= tomatoReq && tomatoReq > 0) {
				tomatoDb += -tomatoReq;
				updateSuccessful = true;
			}
			System.out.println("FileDB: "+ fileDb);

			String updatedDb = bananaDb + "," + milkDb + "," + tomatoDb;
			
			System.out.println("updatedDb: "+ updatedDb);

			String msgToWrite = FifoDeliver.assigneSequenceId(updatedDb);
			
			System.out.println("msgToWrite: "+ msgToWrite);

			pw = new PrintWriter(fileDb);
			pw.println(msgToWrite);
			
		} catch (FileNotFoundException e) {
			updateSuccessful = false;
			e.printStackTrace();
		} catch (IOException e) {
			updateSuccessful = false;
			e.printStackTrace();
		} finally {
			System.out.println("PW: " + pw);

			pw.close();
		}
		return updateSuccessful;
	}

	public synchronized static boolean overrideProductDb(String dataReq) {
		boolean updateSuccessful = false;
		System.out.println("overrideProductDb(): " + dataReq);
		try {

			String[] splitedReq = dataReq.split(",");
			System.out.println("##### updateDB: start " +  dataReq);

			int bananaReq = Integer.parseInt(splitedReq[1]);
			int milkReq = Integer.parseInt(splitedReq[2]);
			int tomatoReq = Integer.parseInt(splitedReq[3]);
			int seq = Integer.parseInt(splitedReq[4]);
			
			String updatedDb = bananaReq + "," + milkReq + "," + tomatoReq+","+seq;
			System.out.println("##### updateDB: "+ updatedDb);
			pw = new PrintWriter(fileDb);
			pw.println(updatedDb);
		} catch (FileNotFoundException e) {
			updateSuccessful = false;
			e.printStackTrace();
		} catch (IOException e) {
			updateSuccessful = false;
			e.printStackTrace();
		} finally {
			pw.close();
		}
		return updateSuccessful;
	}
	public synchronized static String getCurrentData() {
		try {
			return Files.readAllLines(Paths.get(Constants.PRODUCT_DB_NAME)).get(0);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return "";
	}

}
