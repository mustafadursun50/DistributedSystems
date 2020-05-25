package sharedObjects;

public class Main {
	static int tomaten = 8;
	public static void main(String[] args) {
		ObjectFunctions data = new ObjectFunctions();
		data.writeFile("Tomaten = " + tomaten + ", Bananen = " + 7);
		data.readFile();
		data.writeFile("Tomaten = " + tomaten + ", Bananen = " + 7);
		data.readFile();
	}

}
