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
	public static void main(String[] args) throws InterruptedException{
    	if(args == null || args.length != 2){
    		System.err.println("Error: Incorrest number of paramaters");
    		return;
    	}
		
		/*
		 * "how to gracefully handle sigkill"
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
				double whichpic = Math.random()*10;
				int whichpicint = (int)whichpic;

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
		while(true){
			//purely for testing purposes, get rid of thread later
			Thread.sleep(1000);

			//start of yo shitz
			String filePath = args[0];
			String picture = args[1];
			
			Client client = new Client(filePath, picture);
			client.HTTPGET();
			client.printPeerList();
			String[] getPeerList = client.getPeerList();
			String peer = "";
			if(getPeerList != null){
				for(int i = 0; i < getPeerList.length; i++){
					if(getPeerList[i].contains("RUBT11")){
						peer = getPeerList[i];
  					}//end of if 
				}//end of for 
			}//end of if 

            //TODO FIX THIS!
			client.connect(peer);
			if(client.completed()){
				System.out.println("FILE SUCCESSFULY DOWNLOAD!");
			}
			return;

		}//end ofw hile 
	}//end of public static void main
}//end of class
