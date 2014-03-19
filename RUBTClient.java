/*
 * RUBTClient.java
 * Group 05:
 * Paul Tai
 * Anthony Wong
 * Alex Zhangoose
 */

import java.io.*;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

import java.net.*;
import java.lang.*;

/**
 * For le error checking
 * 
 */
public class RUBTClient {
	
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
	
	public static void main(String[] args) throws InterruptedException{
    	
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
		
		Client client = new Client(torrent, outputPath);
		int port = client.openSocket();
		client.connectToTracker(port);
		client.connectToPeers();
/*		client.HTTPGET();
		client.printPeerList();
		String[] getPeerList = client.getPeerList();
		String peer = "";
		if(getPeerList != null){
			for(int i = 0; i < getPeerList.length; i++){
				if(getPeerList[i].contains("RUBT11")){
					peer = getPeerList[i];*/
//  				}//end of if 
//			}//end of for 
//		}//end of if 

        //TODO FIX THIS!
/*		client.connect(peer);
		if(client.completed()){
			System.out.println("FILE SUCCESSFULY DOWNLOAD!");
		}
		
		client.stopped();*/
		
		
		return;
		
				
	}//end of public static void main
	
}//end of class
