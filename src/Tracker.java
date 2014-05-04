package src;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import edu.rutgers.cs.cs352.bt.exceptions.*;
import edu.rutgers.cs.cs352.bt.util.*;

/**
 * @author Paul Tai
 * @author Alex Zhang
 * @author Anthony Wong
 */
public class Tracker {
	
	
	/**
	 * Client Object
	 */
	private Client client;
	
	/**
	 * The Tracker URL
	 */
	private URL url;
	
	/**
	 * The Torrent info hash
	 */
	private byte[] infoHash;
	
	/**
	 * The Client ID
	 */
	private byte[] clientID;
	
	/**
	 * Maps to the number of seconds the client
	 * should wait between regular rerequests.
	 */
	private int interval;
	
	/**
	 * Port that the client is listening on for incoming handshakes 
	 */
	private int listenPort;
	
	/**
	 * The Peers ByteBuffer
	 */
    private final ByteBuffer PEERS = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', 's' });
        
    /**
    * The IP ByteBuffer
    */
    private final ByteBuffer IP = ByteBuffer.wrap(new byte[]
    { 'i', 'p' });
        
    /**
     * The PeerID ByteBuffer
     */
    private final ByteBuffer PEERID = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', ' ', 'i', 'd' });
        
    /**
     * The Port ByteBuffer
     */
    private final ByteBuffer PORT = ByteBuffer.wrap(new byte[]
    { 'p', 'o', 'r', 't', });

    /**
     * The Intervals ByteBuffer
     */
    private final ByteBuffer INTERVALS = ByteBuffer.wrap(new byte[]
    { 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' });
    
    /**
     * HEXCHARS
     */
    private final char[] HEXCHARS = "0123456789ABCDEF".toCharArray();
    
    /**
     * The Tracker Constructor
     * @param client The Client
     * @param url torrentInfo.announce_url
     * @param infoHash torrentInfo.info_hash.array() 
     * @param clientID The Client ID
     * @param listenPort The open socket that takes incoming messages.
     */
    public Tracker(Client client, URL url, byte[] infoHash, byte[] clientID, int listenPort){ 
    	this.client = client;
    	this.url = url;
    	this.infoHash = infoHash;
    	this.clientID = clientID;
    	this.listenPort = listenPort;
    }//Tracker constructor
    
    /**
     * Sending a HTTP GET Message to the Tracker
     * @param upload How much the Client has uploaded so far.
     * @param download How much the Client has download so far.
     * @param left How much the Client need to download. If left == 0, This Will Assume the Client is a seeder.  
     * @param event i.e. "started", "completed", "stopped", or Empty String
     * @return A List of Peers
     */
    public ArrayList<Peer> sendHTTPGet(int upload, int download, int left, String event){
		//Create a HTTP Get request's URL out of the given inputs
    	String query = "";
		query = URLify(query,"announce?info_hash", this.infoHash);
		query = URLify(query,"&peer_id",this.clientID);
		query = URLify(query,"&port", Integer.toString(this.listenPort));
		query = URLify(query,"&uploaded", Integer.toString(upload));
		query = URLify(query,"&downloaded", Integer.toString(download));
		query = URLify(query,"&left", Integer.toString(left));
		
		//If there was an event for this, then append the event to the HTTP get request's URL
    	if(event.length() > 0){
    		/* When event == started, completed, or stopped*/
    		query = URLify(query, "&event", event);
    	}
    	
    	//Print statement for debug purposes
    	System.out.println("SENDING A HTTP GET REQUEST TO THE TRACKER ");
    	System.out.println("WITH THE FOLLOWING PARAMATER: ");
    	System.out.println("UPLOAD: " + upload );
    	System.out.println("DOWNLOAD: " + download);
    	System.out.println("LEFT: " + left);
    	System.out.println("Event: " + event);
		try {
			this.url = new URL(this.url,query);
			System.out.println(this.url.toString());
		} catch (MalformedURLException e1) {
			//This is from this.url
			System.out.println("FAILURE: INVALID URL");
			return null;
		}

		/* Get the Peer List */
		return getPeerList();

    }
    
    /**
     * Method will append a queryID and query to a URL using proper URL-encoded format
     */ 
    private String URLify(String url, String queryID, String query) {
        String base;
		if(url==null) {
			base = "";
        } else {
        	base = url;
        }
                
        try{
            return (base+queryID+"="+URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
			System.out.println("URL formation error:" + e.getMessage());
        }
                
        return null;
	}
        
    /**
     * Method will append a queryID and a byte array query to a URL using proper URL-encoded format
     */ 
	private String URLify(String url, String queryID, byte[] query) {
        String base;
		if(url==null) {
			base = "";
		} else {
			base = url;
		}
                
        String reply = base+queryID+"=";
        
        for(int i = 0; i<query.length; i++) {
			if((query[i] &0x80) == 0x80) { //if the byte data has the most significant byte set (e.g. it is negative)
				reply = reply+"%";
                //Mask the upper byte and lower byte and turn them into the correct chars
                reply = reply + this.HEXCHARS[(query[i]&0xF0)>>>4]+this.HEXCHARS[query[i]&0x0F];
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
	
	/**
	 * @return The List of Peers
	 */
	private ArrayList<Peer> getPeerList(){/* This is based on the create() */
		URLConnection connnection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;
		
		try{
			//Open an HTTP Connection to the given URL and send the HTTP get request
			httpConnection = (HttpURLConnection)this.url.openConnection();
			getStream = httpConnection.getInputStream();
			dataStream = new DataInputStream(getStream);
			
			getStreamBytes = new byte[httpConnection.getContentLength()];
			dataStream.readFully(getStreamBytes);//read causes an IOError
			//Decode the returned data using Bencoder
			Map<ByteBuffer, Object> response = (Map<ByteBuffer, Object>) Bencoder2.decode(getStreamBytes);
			
			//If there was no response, then the tracker is down or no one is connected
			if(response == null){
				System.out.println("FAILURE: NO PEER LIST");
				return null;
			}		
			
			//Cleanup I/Ostreams
			getStream.close();
			dataStream.close();
			
			//Parse the input and then return it
			return captureResponse(response);
		}catch(IOException e){
			System.out.println("");
			e.printStackTrace();
		}catch(BencodingException e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Capture the Response from the Tracker
	 * @param response The decode response
	 * @return The List of Peers from the Tracker
	 */
	private ArrayList<Peer> captureResponse(Map<ByteBuffer, Object> response){
		ArrayList<Peer> peerArrayList = new ArrayList<Peer>();
		//Typecast the response of the HTTP get request
		ArrayList<Map<ByteBuffer, Object>> peers = (ArrayList<Map<ByteBuffer, Object>>)response.get(this.PEERS);
		//Parse out the interval
		this.interval = (Integer)response.get(this.INTERVALS);
		
		//Loop through all the peers and then parse them into the proper objects
		for(int i = 0; i < peers.size(); i++){
			Map<ByteBuffer, Object> peerList = peers.get(i);
			byte[] peerID = ((ByteBuffer)peerList.get(this.PEERID)).array();
			String peerIPAddress = new String(((ByteBuffer)peerList.get(this.IP)).array());
			//Filter out the desired class-related clients only
			if(peerIPAddress.contains("128.6.171.130") || peerIPAddress.contains("128.6.171.131")){
				//More parsing of data into the proper types
				int port = Integer.valueOf((Integer)(peerList.get(this.PORT)));
				//Create a new peer using the parsed information and then add it to the peerList
				Peer peer = new Peer(this.client, peerID, peerIPAddress, port);
				peerArrayList.add(peer);
			}
		}
		return peerArrayList;
	}
	
	/**
	 * @return the number of seconds the client should wait between regular rerequests.
	 */
	public int getInterval(){
		return this.interval;
	}

}
