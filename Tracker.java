import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;
import edu.rutgers.cs.cs352.bt.util.*;

public class Tracker {
	
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
     * HEXCHARS
     */
    private static char[] HEXCHARS = "0123456789ABCDEF".toCharArray();
    
    /**
     * Tracker Constructor
     * @param infoHash The Torrent InfoHash.toArray()
     * @param clientID The Client ID
     */
    public Tracker(byte[] infoHash, byte[] clientID){ 
    	/**
    	 * I did this because the tracker needs the
    	 * infoHash and clientID.
    	 */
    	this.infoHash = infoHash;
    	this.clientID = clientID;
    }
    
    /**
     * Sending a HTTP GET Message to the Tracker
     * @param upload How much the Client has uploaded so far.
     * @param download How much the Client has download so far.
     * @param left How much the Client need to download. If left == 0, This Will Assume the Client is a seeder.  
     * @param event i.e. "started", "completed", "stopped", or Empty String
     * @return A List of Peers
     */
    public Map<byte[], String> sendHTTPGet(int upload, int download, int left, String event){
    	String query = "";
		query = URLify(query,"announce?info_hash", this.infoHash);
		query = URLify(query,"&peer_id",this.clientID);
		query = URLify(query,"&uploaded", Integer.toString(upload));
		query = URLify(query,"&downloaded", Integer.toString(download));
		query = URLify(query,"&left", Integer.toString(left));
		
    	if(event.length() > 0){
    		/* When event == started, completed, or stopped*/
    		query = URLify(query, "&event", "event");
    	}
    	
    	System.out.println("SENDING A HTTP GET REQUEST TO THE TRACKER ");
    	System.out.println("WITH THE FOLLOWING PARAMATER: ");
    	System.out.println("UPLOAD: " + upload );
    	System.out.println("DOWNLOAD: " + download);
    	System.out.println("LEFT: " + left);
    	System.out.println("Event: " + event);
    	
		try {
			this.url = new URL(url,query);
		} catch (MalformedURLException e1) {
			//This is from this.url
			System.out.println("FAILURE: INVALID URL");
			return null;
		}
    	
    	if(left != 0){
    		/* Get the Peer List */
    		return this.getPeerList();
    	}else{
    		/* Send a Message */
    		try {
				HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
				httpConnection.disconnect();
			} catch (IOException e) {
				// This is from HttpURLConnection
				System.out.println("FAILURE: CONNECTION");
				e.printStackTrace();
			}
    	}
    	return null;
    }
    
    private String URLify(String base, String queryID, String query) {
        
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
        
	private String URLify(String base, String queryID, byte[] query) {
                
		if(base==null) {
			base = "";
		}
                
        String reply = base+queryID+"=";
                
        for(int i = 0; i<query.length; i++) {
			if((query[i] &0x80) == 0x80) { //if the byte data has the most significant byte set (e.g. it is negative)
				reply = reply+"%";
                //Mask the upper byte and lower byte and turn them into the correct chars
                reply = reply + HEXCHARS[(query[i]&0xF0)>>>4]+HEXCHARS[query[i]&0x0F];
            }else{
				try{ //If the byte is a valid ascii character, use URLEncoder
					reply = reply + URLEncoder.encode(new String(new byte[] {query[i]}),"UTF-8");
					System.out.println("REPLY: " + reply);
                }catch(UnsupportedEncodingException e){
					System.out.println("URL formation error:" + e.getMessage());
                }
            }
        }        
        return reply;
    }
    
    
/*	public Tracker(URL url){
		this.url = url;
	}*/
	
	/**
	 * @return List of Peers 
	 */
	public Map<byte[], String> getPeerList(){/* This is based on the create() */
		URLConnection connnection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;
		
		try{
			httpConnection = (HttpURLConnection)url.openConnection();
			
			getStream = httpConnection.getInputStream();
			dataStream = new DataInputStream(getStream);
			
			getStreamBytes = new byte[httpConnection.getContentLength()];
			dataStream.readFully(getStreamBytes);//TODO read causes an IOError
			
			Map<ByteBuffer, Object> response = (Map<ByteBuffer, Object>) Bencoder2.decode(getStreamBytes);
			if(response == null){
				System.out.println("FAILURE: NO PEER LIST");
				return null;
			}		
			getStream.close();
			dataStream.close();
			this.captureResponse(response);
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
	private Map<byte[], String> captureResponse(Map<ByteBuffer, Object> response){
		Map<byte[], String> peerMap = new HashMap<byte[], String>();
		ArrayList<Map<ByteBuffer, Object>> peers = (ArrayList<Map<ByteBuffer, Object>>)response.get(PEERS);
		
		for(int i = 0; i < peers.size(); i++){
			Map<ByteBuffer, Object> peerList = peers.get(i);
			byte[] peerID = ((ByteBuffer)peerList.get(PEERID)).array();
			String peerIPAddress = new String(((ByteBuffer)peerList.get(IP)).array());
			int port = Integer.valueOf((Integer)(peerList.get(PORT)));
			peerIPAddress =  peerIPAddress + ":" + Integer.toString(port);
			peerMap.put(peerID, peerIPAddress);
		}
		return peerMap;
	}
	
	/**
	 * Sending a Completed Message to the Tracker
	 * @param url The URL of the tracker.
	 */
/*	public void completed(URL url){
		URLConnection connnection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		try{
			httpConnection = (HttpURLConnection)url.openConnection();
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			
		}catch(IOException e){
			
		}
	}*/
	
	/**
	 * Sending a Stopped Message to the Tracker
	 * @param url The URL of the tracker.
	 */
/*	public void stopped(URL url){
		URLConnection connnection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		try{
			httpConnection = (HttpURLConnection)url.openConnection();
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
		}catch(IOException e){
			
		}
	}*/
}
