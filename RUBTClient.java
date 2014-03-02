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
 */
public class RUBTClient {
	public static void main(String[] args){
    	String filePath = "project1.torrent";
        String picture = "picture.jpg";
        
        Client client = new Client(filePath, picture);
        client.HTTPGET();
        client.printPeerList();
        String[] getPeerList = client.getPeerList();
        String peer = "";
        if(getPeerList != null){
        	for(int i = 0; i < getPeerList.length; i++){
        		if(getPeerList[i].contains("RUBT11")){
        			peer = getPeerList[i];
        		}
        	}
        }
        //TODO FIX THIS!
        client.connect(peer);
        return;
	}
	
}
