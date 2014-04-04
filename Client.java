import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
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
	
	private byte[] clientID = new byte[20];
		
	private Tracker tracker;
    
	private static TorrentInfo torrentInfo;
    private URL url;
    
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
	
	private ServerSocket listenSocket;
	
	private Map<byte[], String> peerList;
	private Map<byte[], Peer> peerHistory;
	private LinkedBlockingQueue<MessageTask> messagesQueue;
	
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
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		genClientID();
	}
	
	public byte[] getClientID() {
		return this.clientID;
	}
	
	/**
	 * Adding a message from Peer to the Client's Messages Queue. 
	 * @param task The message from the Peer. 
	 */
	public void queueMessage(MessageTask task) {
		if(task == null){
			System.out.println("Invalid message from " + task.getPeer());
			return;
		}
		this.messagesQueue.add(task);
	}
	
	/**
	 * Opens a server socket for incoming messages.
	 * Attempts to open the socket on port 6881 and continues (on failures) up to port 6889.
	 * @return Available port number or 0 if port fails to open.
	 */
	public int openSocket(){
		System.out.println("OPENING THE SOCKET: ");
		for(int i = 1; i < 10; i++){
			try {
				this.listenSocket = new ServerSocket(Integer.valueOf(new String("688" + i)));
				System.out.println("PORT: " + Integer.valueOf(new String("688" + i)));
				return Integer.valueOf(new String("688" + i));
			} catch (NumberFormatException e) {
				/* DO NOTHING */
			} catch (IOException e) {
				System.out.println("PORT: " + Integer.valueOf(new String("688" + i)) + " FAIL!");
			}
		}
		return 0;
	}
	
	/**
	 * Send the HTTP GET Message to the Tracker
	 * @param port = LISTEN port that the client will use for incoming BT connections 
	 * @return true for success, otherwise false.
	 */
	
	public boolean connectToTracker(final int port){
		Tracker tracker = new Tracker(this.torrentInfo.announce_url, this.torrentInfo.info_hash.array(), clientID, port);
		this.peerList = tracker.sendHTTPGet(0, 0, 100, "started");
		this.peerHistory = new HashMap<byte[], Peer>();
		if(this.peerList == null){
			return false;
		}
		System.out.println("Number of Peer List: " + peerList.size());
		return true;
	}
	
	/**
	 * ConnectToPeers will go through the current list of peers and connect to them
	 */
	public void connectToPeers(){
		if(peerList.isEmpty()){
			/* DO NOTHING */
			return;
		}
		System.out.println("Connecting to Peers");
		Set<byte[]> keys = peerList.keySet();
		Iterator<byte[]> iter = keys.iterator();
		while(iter.hasNext()){
			byte[] peerID = iter.next();
			if(this.peerHistory.containsKey(peerID)){
				continue;
			}
			String[] ipPort = peerList.get(peerID).split(":");
			Peer peer = new Peer(this, peerID, ipPort[0], Integer.valueOf(ipPort[1]));
			this.peerHistory.put(peerID, peer);
			peer.start();
		}		
	}
	
	private void readQueue(){
		//TODO: This should be call by a run method.
		//TODO: Check to see this method works.
		if(this.messagesQueue.isEmpty()){
			/* DO NOTHING */
			return;
		}
		MessageTask messageFromPeer = messagesQueue.poll();
		Peer peer = messageFromPeer.getPeer();
		Message message = messageFromPeer.getMessage();
		if(message.getLength() == 0){
			/* Keep Alive Message */
			//TODO
			return;
		}
		
		switch(message.getMessageID()){
			case 0: /* choke */
				peer.setLocalChoking(true);
				break;
			case 1: /* unchoke */
				peer.setLocalChoking(false);
				//TODO: Look at the Peer bitfield and compare that to the Client bitfield.
				//TODO: Request for pieces that the client do not have. 
				break;
			case 2: /* interested */
				//TODO: The Client can send a Unchoke or Choke Message to the Peer.
				peer.setRemoteInterested(true);
				break;
			case 3: /* not interested */
				peer.setRemoteInterested(false);
				break;
			case 4: /* have */
				peer.updatePeerBitfield(message.getPayload()[5]);
				break;
			case 5: /* bitfield */
				byte[] bitfield = Arrays.copyOfRange(message.getPayload(), 5, message.getPayload().length);
				peer.setPeerBooleanBitField(convert(bitfield));
				break;
			case 6: /* request */
				//TODO: Send the Peer the requested piece.
				//TODO: 1. Check to see you have the piece to begin with. 
				//TODO: 2. Send. 
				break;
			case 7: /* piece */
				//TODO: Verify wit the SHA-1 and send a Have Message.
				break;
			case 8: /* cancel */
				//TODO: The Peer already have the piece. 
				//TODO: 
				break;
			case 9: /* quit */
				
				break;
			default:
				System.out.println("Unknown Message");
				break;
		}
	}
	
	/**
	 * Source:
	 * @author Robert Moore
	 * Taken from sakai CS352 class resources on 3/29/14
	 * @param bits
	 * @param significantBits
	 * @return
	 */
	private boolean[] convert(byte[] bits, int significantBits) {
		boolean[] retVal = new boolean[significantBits];
		int boolIndex = 0;
		for (int byteIndex = 0; byteIndex < bits.length; ++byteIndex) {
			for (int bitIndex = 7; bitIndex >= 0; --bitIndex) {
				if (boolIndex >= significantBits) {
					// Bad to return within a loop, but it's the easiest way
					return retVal;
				}

				retVal[boolIndex++] = (bits[byteIndex] >> bitIndex & 0x01) == 1 ? true
						: false;
			}
		}
		return retVal;
	}

	/**
	 * Source:
	 * @author Robert Moore
	 * Taken from sakai CS352 class resources on 3/29/14
	 * @param bits
	 * @return
	 */
	private boolean[] convert(byte[] bits) {
		return this.convert(bits, bits.length * 8);
	}
	
	/**
	 * Generates a random byte[] Client ID. 
	 * First 3 characters will be AAA, followed by 17 random integers.
	 * Saved locally, used as an identifier for other peers and tracker.
	 */
	
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
			this.request.write(Message.interested.getPayload());
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
	
	public static int getPieceLength(){
		return torrentInfo.piece_length;
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
					//this.request.write(Message.request(index, (numPacketsDownloaded%2)*this.MAXIMUMLIMT, this.torrentInfo.file_length % this.MAXIMUMLIMT));
				} else { 
					//this.request.write(Message.request(index, (numPacketsDownloaded%2)*this.MAXIMUMLIMT, this.MAXIMUMLIMT));
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
