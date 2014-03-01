import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;
import edu.rutgers.cs.cs352.bt.*;

import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.ListIterator;

public class Tracker{
        
	public final static ByteBuffer PEERS = ByteBuffer.wrap(new byte[]
	{ 'p', 'e', 'e', 'r', 's' });
        
    public final static ByteBuffer IP = ByteBuffer.wrap(new byte[]
    { 'i', 'p' });
        
    public final static ByteBuffer PEERID = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', ' ', 'i', 'd' });
        
    public final static ByteBuffer PORT = ByteBuffer.wrap(new byte[]
    { 'p', 'o', 'r', 't', });
        
	private URL url;
    private byte[] hash;
    private int bytesLeft;
    private int bytesDownloaded;
    private int bytesUploaded;
    private String peer_id;
    private String hostIP;
    private int port;
    private String hostID;

	/**
	 * takes a TorrentInfo
	 * and makes a tracker
	 * and gets the IP addresses ands tuff
	 */
	public Tracker(TorrentInfo torrentFile){
		this.url = torrentFile.announce_url;
        this.hash = torrentFile.info_hash.array();
        this.bytesLeft = torrentFile.file_length;
        
        this.bytesDownloaded = 0;
        //Needs to calcuate the number of bytes remaining
                
        this.bytesUploaded = 0;
        
        this.peer_id = Tracker.genPeerID();
	}//end of Tracker constructor :3

        
    final private static char[] intArray = "0123456789".toCharArray();
        
    /*
     * Generates a random peer ID, length of 20, with a prefix of AAA.
     */    
    public static String genPeerID() {
		String peerID = "AAA";
		Random randGen = new Random();
        for(int i = 0; i<17;i++) {
			peerID = peerID + intArray[randGen.nextInt(9)];
        }
		return peerID; 
    }
        
	/**
	 * creates connection to tracker
	 * takes ipaddress and port <-- taken from the torrentfile obj
	 * -- also takes a tracker!!!
	 * send HTTP GET --> tracker thru ^^ 
	 */
	public void create(){
                
		URL url = this.url;
		byte[] hash = this.hash;
                
		String query = "";
                
		URLConnection connection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;

		try{
			
			//Create the HTTP-get request URL
			query = URLify(query,"announce?info_hash", hash);
			query = URLify(query,"&peer_id",this.peer_id);
			query = URLify(query,"&uploaded", Integer.toString(this.bytesUploaded));
			query = URLify(query,"&downloaded", Integer.toString(this.bytesDownloaded));
			query = URLify(query,"&left", Integer.toString(this.bytesLeft));
                        
			url = new URL(url, query);
                        
			System.out.println(url);
                        
            //Create the URL connection and send the GET request
			httpConnection = (HttpURLConnection)url.openConnection();
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			
			//Capture the response and save it to a byte buffer
			System.out.println("RESPONSE: " + responseCode);
			getStream = httpConnection.getInputStream();
            dataStream = new DataInputStream(getStream);
                        
			//getStream = connection.getInputStream();

			int byteAvailLen = getStream.available();
			getStreamBytes = new byte[byteAvailLen];
			dataStream.read(getStreamBytes); //Note: readFully causes an IOError(?)
			
			
			//System.out.println("CONNECTION: " + httpConnection.toString());
			//System.out.println("GETSTREAM: " + getStream.toString());
			//System.out.println("bytes: " + byteAvailLen);
			//System.out.println("byte array length " + getStreamBytes.length);
			
			
			//Typecast the response to a map object
            Map<ByteBuffer,Object> response = (Map<ByteBuffer,Object>)Bencoder2.decode(getStreamBytes);
            
            //ToolKit.print(response);
            
            //Decode the peers using Bencoder
			Object peers = Bencoder2.decode(getStreamBytes);
			
			
			//Call capture response to spin out new peers
			captureResponse(response);
			//ToolKit.print(peers);
			
			
			//Close the datastreams
			getStream.close();
			dataStream.close();
		}//end of try
		catch(IOException e){
			System.out.println("IO ERROR: " + e.getMessage());
		} catch(BencodingException e) {
            System.out.println("Bencoding ERROR:" + e.getMessage());
        }//end of catch
		
	}//end create

	public void started(){
        URL url = this.url;
        byte[] hash = this.hash;
        
        String query = "";
		        
        URLConnection connection = null;
        InputStream getStream = null;
        HttpURLConnection httpConnection = null;
        DataInputStream dataStream = null;
        byte[] getStreamBytes;

        try{    
			query = URLify(query,"announce?info_hash", hash);
			query = URLify(query,"&peer_id",this.peer_id);
			query = URLify(query,"&uploaded", Integer.toString(this.bytesUploaded));
			query = URLify(query,"&downloaded", Integer.toString(this.bytesDownloaded));
			query = URLify(query,"&left", Integer.toString(this.bytesLeft));
			query = URLify(query,"&event", "started");
                
			url = new URL(url, query);
                
			System.out.println(url);
                
			httpConnection = (HttpURLConnection)url.openConnection();
			//System.out.println("1");
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			//System.out.println("2");
			System.out.println("RESPONSE: " + responseCode);
			
		} catch(IOException e) {
			System.out.println("ERROR" + e.getMessage());
			
		}
	}

	/**
	 * args: tracker <-- invisible for now
	 * return a list of peers 
	 */
	private void captureResponse(Map<ByteBuffer,Object> response){
                
		//Get Peers from the Bencoded HTTP Get response
		ArrayList<Map<ByteBuffer,Object>> peers = (ArrayList<Map<ByteBuffer,Object>>)response.get(PEERS);
		//ToolKit.print(peers);
                 
		//Creates an iterator of all the peers returned by the tracker
        ListIterator<Map<ByteBuffer,Object>> iter = peers.listIterator();
        
        for(int i = 0; i < peers.size(); i++) {
			ToolKit.print(iter.next());
        }
                
        //ToolKit.print(peers.get(0));
                
        //Parse the peer's IP, PeerID, and Port
        Map<ByteBuffer,Object> aPeer = (Map<ByteBuffer,Object>)peers.get(0);
        String ip = new String(((ByteBuffer)aPeer.get(IP)).array());
        String remotePeerID = new String(((ByteBuffer)aPeer.get(PEERID)).array());
        int port = (int)(aPeer.get(PORT));
        this.hostIP = ip;
        this.hostID = remotePeerID;
        this.port = port;

        System.out.printf("%s %s %d\n", ip, remotePeerID, port);
        
	}//end of captureresponse

	public String getHostIP(){
		return this.hostIP;
	}
	
	public String getHostID(){
		return this.hostID;
	}
	
	public int getPort(){
		return this.port;
	}
        
        
    /**
     * Helper function - will convert a query and append it to the current URL
     */
    private static String URLify(String base, String queryID, String query) {
                
		if(base==null) {
			base = "";
        }
                
        try{
			query = URLEncoder.encode(query, "UTF-8");
            return (base+queryID+"="+query);
        } catch (UnsupportedEncodingException e) {
			System.out.println("URL formation error:" + e.getMessage());
        }
                
        return null;
	}


	final private static char[] HEXCHARS = "0123456789ABCDEF".toCharArray();
	//project 1 SHA-1 is 155187125F2CE9E45F1D09729D75C35F2E83DBF3
        
	private static String URLify(String base, String queryID, byte[] query) {
                
		if(base==null) {
			base = "";
		}
                
        String reply = base+queryID+"=";
                
        for(int i = 0; i<query.length; i++) {
			if(query[i] < 0) { //if the byte data has the most significant byte set (e.g. it is negative)
				reply = reply+"%";
                //Mask the upper byte and lower byte and turn them into the correct chars
                reply = reply + HEXCHARS[(query[i]&0xF0)>>>4]+HEXCHARS[query[i]&0x0F];
            }else{
				try{ //If the byte is a valid ascii character, use URLEncoder
					reply = reply + URLEncoder.encode(new String(new byte[] {query[i]}),"UTF-8");
                }catch(UnsupportedEncodingException e){
					System.out.println("URL formation error:" + e.getMessage());
                }
            }
        }        
        return reply;
    }

        
    public byte[] getSHA1(){
    	return this.hash;
    }
        
    public String peerID(){
    	return this.peer_id;
    }
    
    
    public void closeTracker() {
		String query = "";
                
		URLConnection connection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;

		try{    
			query = URLify(query,"announce?info_hash", hash);
			query = URLify(query,"&peer_id",this.peer_id);
			query = URLify(query,"&uploaded", Integer.toString(this.bytesUploaded));
			query = URLify(query,"&downloaded", Integer.toString(this.bytesDownloaded));
			query = URLify(query,"&left", Integer.toString(this.bytesLeft));
            query = URLify(query,"&event", "completed");
            
                        
			url = new URL(url, query);
                        
			System.out.println(url);
                        
			httpConnection = (HttpURLConnection)url.openConnection();
			//System.out.println("1");
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			//System.out.println("2");
			System.out.println("RESPONSE: " + responseCode);
			getStream = httpConnection.getInputStream();
            dataStream = new DataInputStream(getStream);
                        
			//System.out.println("3");

			//getStream = connection.getInputStream();

			int byteAvailLen = getStream.available();
			
			//System.out.println("CONNECTION: " + httpConnection.toString());
			//System.out.println("GETSTREAM: " + getStream.toString());
			//System.out.println("bytes: " + byteAvailLen);
			
			getStreamBytes = new byte[byteAvailLen];
			
			//System.out.println("byte array length " + getStreamBytes.length);
			
			dataStream.read(getStreamBytes); //Note: readFully causes an IOError(?)
			
            ToolKit.print(response);
			
			getStream.close();
			dataStream.close();
		}//end of try
		catch(IOException e){
			System.out.println("IO ERROR: " + e.getMessage());
		} catch(BencodingException e) {
            System.out.println("Bencoding ERROR:" + e.getMessage());
        }//end of catch
		
	}
    
}//end of connection class
