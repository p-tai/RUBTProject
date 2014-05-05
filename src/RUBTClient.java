package src;

import java.io.*;

import javax.swing.JFrame;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import gui.*;

/**
 * RUBTClient.java
 * Group 05:
 * @author Paul Tai
 * @author Anthony Wong
 * @author Alex Zhangoose
 *
 */
public class RUBTClient extends Thread{

	private static Client client;

	private static TorrentInfo parseTorrentInfo(String filename) {
		try {
			//Create input streams and file streams
			File torrentFile = new File(filename);
			FileInputStream torrentFileStream = new FileInputStream(torrentFile);
			DataInputStream torrentFileReader = new DataInputStream(torrentFileStream);

			//Read the file into torrentFileBytes
			byte[] torrentFileBytes = new byte[((int)torrentFile.length())];
			torrentFileReader.readFully(torrentFileBytes);

			//Close input streams and file streams
			torrentFileReader.close();
			torrentFileStream.close();

			return new TorrentInfo(torrentFileBytes);

		} catch(FileNotFoundException e) {
			System.err.println("Error: " + e.getMessage());
			return null;
		} catch (IOException e){
			System.err.println("Error: " + e.getMessage());
			return null;
		} catch (BencodingException e) {
			System.err.println("Error: " + e.getMessage());
			return null;
		}   
	}

	/**
	 * Alternative to System.exit(0); for quitting
	 */
	public static void shutdown(){
		if(client.isAlive()) {
			client.disconnectFromTracker();
			client.shutdown();
		}
	}
	
	/**
	 * Creation of the GUI, basically just starting it. 
	 *
	 */
	private static void createGUI(Client cli){
		System.out.println("START GUI\n\n\n\n\n\n\n");
		// for now: copy all code from gui/Client.java
		Display dis;
		dis = new Display(cli);
		dis.setSize(500,500);
		dis.setResizable(false);
		dis.setVisible(true);
		dis.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}//end of creategui
	
	/**
	 * Cited: http://stackoverflow.com/questions/12234526/java-writing-unittest-for-exiting-a-program-when-user-type-quit-in-the-console
	 * User: David Wallace
	 * Time: 4, 05, 2014
	 */
	public void run(){
		String line = "";
		String QUIT = "quit";

		try{
			final InputStreamReader input = new InputStreamReader(System.in);
			final BufferedReader in = new BufferedReader(input);    
			while (!(line.toLowerCase().equals(QUIT))) {    
				line = in.readLine();    
				if (line.equals(QUIT)) {    
					RUBTClient.shutdown();
					return;
					
				}    //end of if
			}  //end of while

		} catch(Exception e){
			System.err.println("Error: " + e.getMessage());
		}//end of catch
		return;
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args){

		/*
		 * "how to gracefully handle sigkill" aka armadillo
		 * http://stackoverflow.com/questions/2541597/how-to-gracefully-handle-the-sigkill-signal-in-java
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(){

			/*
			 * for the teddy bear: 
			 * http://www.chris.com/ascii/index.php?art=animals/bears+(teddybears)
			 * for the armadillo:
			 * http://www.retrojunkie.com/asciiart/animals/armadill.htm
			 * for the music notes:
			 * http://1lineart.kulaone.com/
			 */
			public void run(){
				int whichpicint = (int)Math.random()*10;
				
				System.out.println();
				System.out.println("You are now quiting the program");
				System.out.println();

				if(whichpicint % 2 == 0){
					System.out.println("      ('-^-/')     /-----------------\\");
					System.out.println("      `o__o' ]     |    byebye <3    |");
					System.out.println("      (_Y_) _/     |  ♬·¯·♩¸¸♪·¯·♫ ♪ |");
					System.out.println("    _..`--'-.`,	  /,_________________| ");
					System.out.println("   (__)_,--(__)    ");
					System.out.println("       7:   ; 1");
					System.out.println("     _/,`-.-' :");
					System.out.println("    (_,)-~~(_,)");
				}//end of if even
				else{
					System.out.println("          .::7777::-.             /-----------------\\");
					System.out.println("          /:'////' `::>/|/       |      byebye <3    |");
					System.out.println("        .',  ||||   `/( e\\       |   ♬·¯·♩¸¸♪·¯·♫ ♪  |");
					System.out.println("    -==~-'`-Xm````-mr' `-_\\      /,__________________|");

				}//end of else odd

				System.out.println();
				RUBTClient.shutdown();
				
			}//end of run
		});//end of new thread runtime thingie :3

		//Argument checking
		if(args == null || args.length != 2){
			System.err.println("Error: Incorrect number of paramaters");
			return;
		}

		//Parse arguments
		String torrentPath = args[0];
		String outputPath = args[1];

		//Open the torrent file specified
		TorrentInfo torrent = parseTorrentInfo(torrentPath);
		try {
			RandomAccessFile file = new RandomAccessFile(outputPath,"rw");
			/* The File exists */
			//System.out.println("The file exists");
			client = new Client(torrent, file);
		} catch (FileNotFoundException e) {
			/* The File does not exist */
			//System.out.println("The fill does not exist");
			client = new Client(torrent, outputPath);
			//e.printStackTrace();
		}
		
		int port = client.openSocket();
		if(client.connectToTracker(port) == false){
			System.out.println("THE TRACKER IS DOWN!");
			return;
		}
		
		System.out.println("Handshaking with PEERS");
		client.connectToPeers();
		
		//start running the socket reader thread in client (main thread)
		client.start();
		
		//Wait for a second to make sure we finish off any handshakes
		try {
			sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
		}
		
		//If we are seeding, we don't need to do piece requests.
		if(client.isSeeder() == false) {
			System.out.println("Beginning Download Thread...");
			//Start the piece request thread.
			client.startPeerDownloads();
		}

		createGUI(client);
		//Start running the shutdown hook as a separate thread.
		//(new RUBTClient()).run();
		return;


	}//end of public static void main

}//end of class
