import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;
import edu.rutgers.cs.cs352.bt.util.*;

public class Tracker {
	
	private URL url;
	
	public final static ByteBuffer PEERS = ByteBuffer.wrap(new byte[]
	{ 'p', 'e', 'e', 'r', 's' });
        
    public final static ByteBuffer IP = ByteBuffer.wrap(new byte[]
    { 'i', 'p' });
        
    public final static ByteBuffer PEERID = ByteBuffer.wrap(new byte[]
    { 'p', 'e', 'e', 'r', ' ', 'i', 'd' });
        
    public final static ByteBuffer PORT = ByteBuffer.wrap(new byte[]
    { 'p', 'o', 'r', 't', });
	
	
	public Tracker(URL url){
		this.url = url;
	}
	
	public HashMap<String, Peer> started(){/* This is based on the create() */
		URLConnection connnection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;
		
		try{
			httpConnection = (HttpURLConnection)url.openConnection();
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			
			/*TODO Print out the Response Code */
			getStream = httpConnection.getInputStream();
			dataStream = new DataInputStream(getStream);
			
			getStreamBytes = new byte[getStream.available()];
			dataStream.read(getStreamBytes);//TODO read causes an IOError
			
			Map<ByteBuffer, Object> response = (Map<ByteBuffer, Object>) Bencoder2.decode(getStreamBytes);
			getStream.close();
			dataStream.close();
			return captureResponse(response);
		}catch(IOException e){
			
		}catch(BencodingException e){
			
		}
		return null;
	}
	
	private HashMap<String, Peer> captureResponse(Map<ByteBuffer, Object> response){
		HashMap<String, Peer> peerHashMap = new HashMap<String, Peer>();
		ArrayList<Map<ByteBuffer, Object>> peers = (ArrayList<Map<ByteBuffer, Object>>)response.get(PEERS);
		
		for(int i = 0; i < peers.size(); i++){
			Map<ByteBuffer, Object> peerList = peers.get(i);
			String peerID = new String(((ByteBuffer)peerList.get(PEERID)).array());
			String peerIP = new String(((ByteBuffer)peerList.get(IP)).array());
			int port = (int)(peerList.get(PORT));
			Peer peer = new Peer(peerID, peerIP, port);
			peerHashMap.put(peerID, peer);
		}
		return peerHashMap;
	}
	
	public void completed(){
		
	}
	
	public void stopped(){
		
	}
}
