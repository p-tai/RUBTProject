import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;

import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;
import edu.rutgers.cs.cs352.bt.util.*;

//DEFINATION: THERE ARE N-BLOCKS THAT MAKE THE FILES
//THERE ARE N-PACKETS THAT MAKE EACH BLOCKS

public class Client {
	//TODO PUT MOST OF THIS STUFF IN TRACKER!
	
	private byte[] clientID = new byte[20];
		
	private Tracker tracker;
    
	private static TorrentInfo torrentInfo;
    private URL url;
 //  private HashMap<String, Peer> peerList = new HashMap<String, Peer>();
    
	private String saveName;
	private RandomAccessFile dataFile;
	
	private final int MAXIMUMLIMT = 16384;
	private boolean[] blocks;
	private boolean[] packets;
	private int numBlocks = 0;
	private double numPackets;
	private int numPacketsDownloaded;
	
	private DataOutputStream request;
	private DataInputStream  response;
	
	private Map<byte[], String> peerList;
	
	/**
	 * Client Constructor
	 * @param filePath Source of the torrent file
	 * @param saveName The file you want to save in.
	 */
	public Client(TorrentInfo torrent, String saveName){
		System.out.println("Booting");
		this.numPacketsDownloaded = 0;
		this.saveName = saveName;
		this.torrentInfo = torrent;
		this.url = this.torrentInfo.announce_url;
		this.createFile();
		genClientID();
	}
	
	public void connectToTracker(){
		Tracker tracker = new Tracker(this.torrentInfo.announce_url, this.torrentInfo.info_hash.array(), clientID);
		this.peerList = tracker.sendHTTPGet(0, 0, 100, "started");
		System.out.println("Number of Peer List: " + peerList.size());
	}
	
	public void connectToPeers(){
		Set<byte[]> keys = peerList.keySet();
		Iterator<byte[]> iter = keys.iterator();
		while(iter.hasNext()){
			byte[] peerID = iter.next();
			String[] ipPort = peerList.get(peerID).split(":");
			Peer peer = new Peer(clientID, peerID, ipPort[0], Integer.valueOf(ipPort[1]));
			peer.start();
		}		
	}
	
	/**
	 * Send the HTTP GET Message to the Tracker. 
	 * @return true for success, otherwise false.
	 */
/*	public boolean HTTPGET(){
		String query = "";
		genClientID();
		query = URLify(query,"announce?info_hash", this.torrentInfo.info_hash.array());
		query = URLify(query,"&peer_id",this.clientID);
		query = URLify(query,"&uploaded", Integer.toString(0));
		query = URLify(query,"&downloaded", Integer.toString(0));
		query = URLify(query,"&left", Integer.toString(this.torrentInfo.file_length));
		query = URLify(query, "&event", "started");
		try {
			System.out.println("SEND THE TRACKER A HTTP GET MESSSAGE");
			System.out.println("TO: " + this.url);
			this.url = new URL(url, query);
			this.tracker = new Tracker(this.url);
			this.peerList = this.tracker.started();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("FAILURE: WEBSITE NOT FOUND!");
		}
		if(this.peerList  == null){
			//IF TRACKER IS DOWN RETURN FALSE!
			System.out.println("THE TRACKER IS DOWN!");
			return false;
		}
		if(!this.peerList.isEmpty()){
			System.out.println("FOUND " + this.peerList.size() + " USERS!");
			return true;
		}
		return false;
	}*/
	
	/**
	 * Sending a Completed Message to the the Tracker
	 * @return true for success, otherwise false
	 */
/*	public boolean completed(){
		String query = "";
		//genClientID();
		query = URLify(query,"announce?info_hash", this.torrentInfo.info_hash.array());
		query = URLify(query,"&peer_id",this.clientID);
		query = URLify(query,"&uploaded", Integer.toString(0));
		query = URLify(query,"&downloaded", Integer.toString(this.numPacketsDownloaded));
		query = URLify(query,"&left", Integer.toString(this.torrentInfo.file_length));
		query = URLify(query, "&event", "completed");
		try {
			System.out.println("SEND THE TRACKER A HTTP GET MESSSAGE");
			System.out.println("TO: " + this.url);
			this.url = new URL(url, query);
			//this.tracker = new Tracker(this.url);
			//this.tracker.completed(url);
			return true;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Sending a Stopped Message to the the Tracker
	 * @return true for success, otherwise false
	 */
/*	public boolean stopped(){
		String query = "";
		genClientID();
		query = URLify(query,"announce?info_hash", this.torrentInfo.info_hash.array());
		query = URLify(query,"&peer_id",this.clientID);
		query = URLify(query,"&uploaded", Integer.toString(0));
		query = URLify(query,"&downloaded", Integer.toString(this.numPacketsDownloaded));
		query = URLify(query,"&left", Integer.toString(this.torrentInfo.file_length));
		query = URLify(query, "&event", "stopped");
		try {
			System.out.println("SEND THE TRACKER A HTTP GET MESSSAGE");
			System.out.println("TO: " + this.url);
			this.url = new URL(url, query);
			//this.tracker = new Tracker(this.url);
			//this.tracker.stopped(this.url);
			return true;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Prints out the List of Peers from the torrentInfo
	 */
/*	public void printPeerList(){
		if(this.peerList == null){
			//WHEN THE TRACKER IS DOWN
			return;
		}
		Set<String> keys = this.peerList.keySet();
		Iterator<String> iter = keys.iterator();
		System.out.println("Peer Lists:");
		while(iter.hasNext()){
			String peer = iter.next();
			System.out.println(this.peerList.get(peer).toString());
		}
	}
	
	/**
	 * @return The List of Peers from the torrentInfo
	 */
/*	public String[] getPeerList(){
		if(this.peerList == null){
			//WHEN THE TRACKER IS DOWN
			return null;
		}
		String[] peerList = new String[this.peerList.size()];
		Set<String> keys = this.peerList.keySet();
		Iterator<String> iter = keys.iterator();
		System.out.println("Peer Lists:");
		int i = 0;
		while(iter.hasNext()){
			peerList[i] = iter.next();
			i++;
		}
		return peerList;
	}
	
	/*********************************
	 * Client->Peers Public Functions
	 ********************************/
	
	/**
	 * Connect to the Peer based on peerID.
	 * @param peerID The peerID
	 */
/*	public void connect(String peerID){
		if(this.peerList.containsKey(peerID)){
			Peer peer = this.peerList.get(peerID);
			System.out.println("CONTACTING " + peer.getPeerID());
			try {
				Socket socket = new Socket(peer.getPeerIP(), peer.getPeerPort());
				request = new DataOutputStream(socket.getOutputStream());
				response = new DataInputStream(socket.getInputStream());
				System.out.println("Opening Output Stream");
				System.out.println("Opening Input Stream");
				
				if(socket != null && request != null && response != null){
					if(sendHandShakeMessaage()){
						
						/* 0. Getting the BitFields */
				/*		this.blocks = getBitFields();
						//printBooleanArray();
						numBlock();
						System.out.println("The total number of blocks the peer have " + this.numBlocks);
						if(this.blocks != null){
							/* 1. Send Interested Message */
			/*		this.request.flush();
							if(interested()){
								downloading();
							}
						}
					}
				}
				
				request.close();
				response.close();
				socket.close();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("THE USER IS NOT FOUND!");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Input/Output Stream INTRUPPED");
			}
		}else{
			System.out.println("THE USER DOES NOT EXIST!");
		}
	}//end of connect()*/
	
	
	private void genClientID(){
		this.clientID = new byte[20];
		clientID[0] = 'A';
		clientID[1] = 'A';
		clientID[2] = 'A';
		Random randGen = new Random();
		for(int i = 3; i < 20; i++){
			this.clientID[i] = (byte)randGen.nextInt(9);
		}
	}
	
	/**
	 * Sends a interested message to the peer.
	 * @return true for success, otherwise false.
	 */
	private boolean interested(){
		try {
			System.out.println("SENDING INTERESTED MESSAGE");
			this.request.write(Message.interested);
			byte[] message = new byte[5];
			this.response.readFully(message);
			if(Message.getMessageID(message[4]).equals("unchoke")){
				System.out.println("RESPONSE: UNCHOKED");
				return true;
			}
			
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("FAILURE OF SENDING INTERESTED MESSAGE!");
			return false;	
		}
	}
	
	public static byte[] getHash(){
		return torrentInfo.info_hash.array();
	}
	
	
	/**
	 * After handshaking, the Client starts download message from the 
	 * peers. 
	 * @return true for success, otherwise false
	 */
	private boolean downloading(){
		System.out.println("ALL SYSTEMS GO!");
		System.out.println("DOWNLOADING PACKETS!");
		this.packets = new boolean[this.blocks.length*2];
		/* DETERMING HOW MANY PACKETS WILL THERE BE */
		this.numPackets = Math.ceil(((double)torrentInfo.file_length / (double)this.MAXIMUMLIMT));
		System.out.println("THE TOTAL NUMBER OF PACKETS WILL WE DOWNLOAD IS " + this.numPackets);
		while( this.numPacketsDownloaded < this.numPackets ) {
			try {
				//System.out.println("Requesting Packet "+ numPacketsDownloaded);
				sendRequest();
			
				Thread.sleep(500);
				
				while( readSocketOutputStream() == false) {
					Thread.sleep(200);
				}
				
				
			} catch (IOException e) {
				System.err.println("FAILURE DURING READING PACKET IN DOWNLOADING");
				return false;
			} catch(Exception e) {
				System.err.println("FAILURE: INTERRUPTION DURING READING PACKET");
				return false;
			}
				
		}
		return true;
	}
	
	/**
	 * Sending a Request Message to the Peer
	 * @return true for success, otherwise false
	 */
	 private boolean sendRequest(){
		 if(this.numPacketsDownloaded > this.numPackets) {
			return false;
		 }
		 try{
			 	// When the last piece is not equal to the MAXIMUMLIMT
			 	//2 needs to be replaced with the number of requests per piece
			 	int index = (int)Math.floor(numPacketsDownloaded/2);
				System.out.printf("Requesting piece %d: %d offset %d \n", this.numPacketsDownloaded , index, numPacketsDownloaded%2 );
				if(this.numPacketsDownloaded == (int)this.numPackets-1) {
					this.request.write(Message.request(index, (numPacketsDownloaded%2)*this.MAXIMUMLIMT, this.torrentInfo.file_length % this.MAXIMUMLIMT));
				} else { 
					this.request.write(Message.request(index, (numPacketsDownloaded%2)*this.MAXIMUMLIMT, this.MAXIMUMLIMT));
				}
				this.request.flush();
				return true;
		}catch(IOException e){
			System.out.println("FAILURE FOR REQUEST A PACKET");
	        return false;
	    }
	}
	
	/**
	 * Reading the responses from the peer.
	 * @return True for success, otherwise false
	 * @throws IOException 
	 */
	private boolean readSocketOutputStream() throws IOException {
		
		int length = this.response.readInt();
		//System.out.println("Length = "+ length);
		byte classID;
		
		if(length == 0) {
			//keep alive, do nothing
		} else if(length > 0) {
			classID = this.response.readByte();
			//System.out.println("Class ID = "+ classID); 
			length--;
			//choke, unchoke, interested, or not interested
			if(classID==7){
				//Piece message
				try {
					int pieceIndex = this.response.readInt();
					int blockOffset = this.response.readInt();
					System.out.printf("Receiving piece %d offset %d \n", pieceIndex, blockOffset );
					length = length-8; //Remove the length of the indexes and class ID
					byte[] payload = new byte[length];
					

					
					this.response.readFully(payload);
					int blockIndex = pieceIndex + (int)Math.floor((blockOffset+1)/this.MAXIMUMLIMT);
					System.out.println("BlockIndex "+ blockIndex);
					
					this.dataFile.seek((long)pieceIndex*this.torrentInfo.piece_length+blockOffset);
					this.dataFile.write(payload);
					this.numPacketsDownloaded++;
					this.packets[blockIndex] = true;
					System.out.println("updated data");
					
					//if(blockIndex == this.numPacketsDownloaded) {
					//	return true;
					//}
					
					return true;
					
				} catch(IOException e) {
					System.err.println("Received an incorrect input from peer");
				} 
			} else if(length>0) {
				//byte[] payload = new byte[length-1];
				this.response.skipBytes(length);
					
			}
		}
		return false;
	}
	
	
	/**
	 * Send a Hand Shake Message to the Peer
	 * @return true for success, otherwise false
	 */
/*	private boolean sendHandShakeMessaage(){
		try {
			request.write(handshakeMessage(this.torrentInfo.info_hash.array(), clientID));
			byte[] response = new byte[68];
			byte[] hash = new byte[20];
			this.response.readFully(response);
			
			for(int i = 0; i < 20; i++){
				hash[i] = response[28 + i];
			}
			
			System.out.println("Verify the SHA-1 HASH");
			
			for(int i = 0; i < 20; i++){
				if(this.torrentInfo.info_hash.array()[i] != hash[i]){
					System.out.println("THE SHA-1 HASH IS INCORRECT!");
					return false;
				}
			}
			
			this.request.flush();
			System.out.println("THE SHA-1 HASH IS CORRECT!");
			return true;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("HANDSHAKE FAILURE!");
			return false;
		}
	}
	
	/**
	 * @return Boolean Array based on the Value of the BitFields
	 */
	private boolean[] getBitFields(){
		byte[] message = new byte[5];
		try {
			this.response.readFully(message);
			if(Message.getMessageID(message[4]).equals("bitfield")){
				System.out.println("BITFIELDS RECIEVE");	
				int length = (byteArrayToInt(reducedSize(message,0,4)) - 1);
				byte[] bitFields = new byte[length];
				this.blocks = new boolean[length*8];
				this.response.readFully(bitFields);
				String binary = "";
				for(int i = 0; i < length; i++){
					binary = binary + String.format("%8s", Integer.toBinaryString(bitFields[i] & 0xFF)).replace(' ', '0');
				}
				for(int i = 0; i < binary.length(); i++){
					if(binary.charAt(i) == '1'){
						this.blocks[i] = true;
					}else{
						this.blocks[i] = false;
					}
				}
				return this.blocks;
			}else{
				System.out.println("WERE UNABLE TO GET THE BITFIELDS!");
				return null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("NO BITFIELDS!");
			return null;
		}	
	}
	
	private byte[] reducedSize(byte[] b, int beginning, int size){
		byte[] test = new byte[size];
		for(int i = beginning; i < size; i++){
			test[i] = b[i];
		}
		return test;
	}
	
	private void numBlock(){
		for(int i = 0; i < this.blocks.length; i++){
			if(this.blocks[i] == true){
				this.numBlocks++;
			}
		}
	}
	
	/**
	 * Takes in a byte[] and returns the value of it.
	 * @param b Byte Array
	 * @return The value of the byte[] based on the values inside of it.
	 */
	private int byteArrayToInt(byte[] b) {
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}
	
	/**
	 * Print out the Boolean Array with 1's or 0's
	 */
	private void printBooleanArray(){
		String output = "[";
		for(int i = 0; i < this.blocks.length; i++){
			if(this.blocks[i] == false){
				output = output + "0,";
			}else{
				output = output + "1,";
			}
		}
		output = output + "]";
		System.out.println(output);
	}
	
	/**
	 * Creates a Handshake Message.
	 * @param SHA1 The SHA-1 Hash of the torrentInfo
	 * @param peerID The peerID.
	 * @return A Handshake Message.
	 */
	private byte[] handshakeMessage(byte[] SHA1, String peerID){
		byte[] handshake = new byte[68];
        handshake[0] = 19;
        handshake[1] = 'B';
        handshake[2] = 'i';
        handshake[3] = 't';
        handshake[4] = 'T';
        handshake[5] = 'o';
        handshake[6] = 'r'; 
		handshake[7] = 'r';
        handshake[8] = 'e';
        handshake[9] = 'n'; 
		handshake[10] = 't';
        handshake[11] = ' ';
        handshake[12] = 'p';
        handshake[13] = 'r';
        handshake[14] = 'o';
        handshake[15] = 't';
        handshake[16] = 'o';
        handshake[17] = 'c';
        handshake[18] = 'o';
        handshake[19] = 'l';    
		
        for(int i = 0; i < 8; i++){
        	handshake[19 + i + 1] = 0;
        }
        //28
        for(int i = 0; i < 20; i++){
        	handshake[28 + i] = SHA1[i];
        }
        //48
        for(int i = 0; i < 20; i++){
        	handshake[48 + i] = (byte) peerID.charAt(i);
        }
		return handshake;
	}
	
	/**
	 * The Byte Array in Hex Format
	 * @param target Byte Array
	 * @return The String of the Byte Array in Hex Format
	 */
    public static String readByteArray(byte[] target){
    	String result = "";
		for(int i = 0; i < target.length; ++i){
			result = result + String.format("%02x, ", target[i]);
			if(i == 19 || i == 27 || i == 47){
				result = result + "\n";
			}
		}
		return result;
    }
	
	/*********************************
	 * Client->Tracker Private Functions
	 ********************************/
	
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
        
/*	private static String URLify(String base, String queryID, byte[] query) {
                
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
    
	/**
	 * @return true for success, otherwise false.
	 */
	private boolean createFile(){
        try {
            this.dataFile = new RandomAccessFile(this.saveName,"rw");
            return true;
        } catch( FileNotFoundException e) {
            
            try { //If the file does not exist, create it and call createFile again
                FileWriter fileStream = new FileWriter(this.saveName);
                createFile();
                return true;
            } catch (IOException IOe) {
                System.err.println("Error: " + IOe.getMessage());
                return false;
            }
            
        }
    }
    
    /**
     * Check the pieces with the torrentInfo.pieces_hash
     * @param dataPiece A piece of a file
     * @param dataOffset Where the piece is located to the file
     * @return true for success, otherwise false
     */
    public boolean checkData(byte[] dataPiece, int dataOffset) {
        
        MessageDigest hasher = null;
        
        try {
                hasher = MessageDigest.getInstance("SHA");	
        } catch (NoSuchAlgorithmException e) {
                System.err.println("No such algorithm exception: " + e.getMessage());
                return false;
        }

        if(dataOffset > this.torrentInfo.piece_hashes.length) {
                //illegal dataOffset value
                System.err.println("illegal dataOffset");
                return false;
        }

        if(dataPiece.length > this.torrentInfo.piece_length) {
                System.err.println("illegal piece length");
                //ilegal piece length
                return false;
        }
        
        
        byte[] SHA1 = new byte[20];
        (this.torrentInfo.piece_hashes)[dataOffset].get(SHA1);
        
        byte[] checkSHA1 = hasher.digest(dataPiece);
        
        //check SHA-1
        for(int i = 0; i < SHA1.length; i++) {
                if(SHA1[i] != checkSHA1[i]){
                        System.err.println("fail in loop at index " + i);
                return false;
                }
        }

        return true;
    }
	
}
