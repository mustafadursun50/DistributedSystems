package sharedObjects;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ObjectFunctions {

	File file = new File("/Users/Guro/eclipse-workspace/Shared Object/store.txt");
	PrintWriter pw;
	boolean exist = false;
	boolean serverIsLead = true;
	
	public void readFile() {

		if(serverIsLead == true) {
			try {
				Scanner scan = new Scanner(file);
				System.out.println(scan.nextLine());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("check if server is lead and if file exist");
		}
	}
	
	public void writeFile(String information) {

		if(serverIsLead == true) {
			try {
				pw = new PrintWriter(file);
				pw.println(information);
				pw.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("check if server is lead and if file exist");
		}
	}
	
	
	// check if server is lead
	public void serverIsLead() {
		serverIsLead = true;
	}
	
	
	// check if file exist on path : C:\Users\Guro\eclipse-workspace\Shared Object
	public boolean checkIfFileExist() {
		
		try {
		File file = new File("store.txt");
		
		if (!file.exists()) {
			file.createNewFile();	
		}
		exist = true;
		}
		catch(IOException e){
			e.printStackTrace();
			exist = false;
			
		}
		return exist;
	}
}
