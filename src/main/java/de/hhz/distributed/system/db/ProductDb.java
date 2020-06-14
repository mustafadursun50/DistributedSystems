package de.hhz.distributed.system.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import de.hhz.distributed.system.app.Constants;

public class ProductDb {

	private static File fileDb;
	private static PrintWriter pw;

	public static void initializeDb() {
		try {
			fileDb = new File(Constants.PRODUCT_DB_NAME);
			if (!fileDb.exists()) {
				fileDb.createNewFile();
				String initalDbLoad = 100 + "," + 80 + "," + 110;
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

	public static boolean updateProductDb(String dataReq) {
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

			String updatedDb = bananaDb + "," + milkDb + "," + tomatoReq;
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

	public static boolean updateReplicaProductDb(String dataReq) {
		boolean updateSuccessful = false;
		System.out.println("Update database with " + dataReq);
		try {
	
			String[] splitedReq = dataReq.split(",");
			int bananaReq = Integer.parseInt(splitedReq[1]);
			int milkReq = Integer.parseInt(splitedReq[2]);
			int tomatoReq = Integer.parseInt(splitedReq[3]);

			String updatedDb = bananaReq + "," + milkReq + "," + tomatoReq;
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
	public static String getCurrentData() {
		try {
			return Files.readAllLines(Paths.get(Constants.PRODUCT_DB_NAME)).get(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

}
