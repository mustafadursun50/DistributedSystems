package de.hhz.distributed.system.db;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProductDb {

	private static File fileDb;
	private static PrintWriter pw;
	
	public static void initializeDb() {
		try {
			fileDb = new File("productDb.txt");
			if(fileDb.createNewFile()) {
				String initalDbLoad =  "banana "+ 10 +", "+ "milk "+ 10 + ", "+ "tomato " + 10;
				pw = new PrintWriter(fileDb);
				pw.println(initalDbLoad);
				pw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static boolean updateProductDb(String dataReq) {
		boolean updateSuccessful = false;
		try {
			String products = Files.readAllLines(Paths.get("product.txt")).get(0);
			String [] splitedDb = products.split(",");
			int bananaDb = Integer.parseInt(splitedDb[0].substring(7));
			int milkDb = Integer.parseInt(splitedDb[1].substring(6));
			int tomatoDb = Integer.parseInt(splitedDb[2].substring(6));
			
			String [] splitedReq = dataReq.split(",");
			int bananaReq = Integer.parseInt(splitedReq[0].substring(7));
			int milkReq = Integer.parseInt(splitedReq[1].substring(6));
			int tomatoReq = Integer.parseInt(splitedReq[2].substring(8));
			
			if(bananaDb >= bananaReq && bananaReq > 0) {
				bananaDb += -bananaReq;
				updateSuccessful = true;
			}		
			if(milkDb >= milkReq && milkReq > 0) {
				milkDb += -milkReq;
				updateSuccessful = true;
			}
			if(tomatoDb >= tomatoReq && tomatoReq > 0) {
				tomatoDb += -tomatoReq;
				updateSuccessful = true;
			}
			
			String updatedDb =  "banana "+ bananaDb +", "+ "milk "+ milkDb + ", "+ tomatoReq;
			pw = new PrintWriter(fileDb);
			pw.println(updatedDb);
		} catch (FileNotFoundException e) {
			updateSuccessful= false;
			e.printStackTrace();
		} catch (IOException e) {
			updateSuccessful = false;
			e.printStackTrace();
		}finally {
			pw.close();
		}
		return updateSuccessful;
	}


	public static String getData() {
		try {
			return Files.readAllLines(Paths.get("productDb.txt")).get(0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	
}