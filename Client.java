import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

//DEFINATION: THERE ARE N-BLOCKS THAT MAKE THE FILES
//THERE ARE N-PACKETS THAT MAKE EACH BLOCKS

public class Client extends Thread{
	
	private byte[] clientID = new byte[20];
		
	private Tracker tracker;
    
	private static TorrentInfo torrentInfo;
    private URL url;
    
	private String saveName;
	private RandomAccessFile dataFile;
	
	private final int MAXIMUMLIMT = 16384;
	private boolean[] blocks;
	private boolean[] packets;
	private boolean[] bitfield;
	private boolean[] downloadsInProgress;
	private boolean userQuit;
	private int numBlocks = 0;
	private double numPackets;
	private int numPacketsDownloaded;
	
	/**
	 * The number of bytes download from peers.
	 */
	public int downloaded;
	
	/**
	 * The number of bytes of what left to download
	 */
	public int left;
	
	/**
	 * The number of bytes uploaded to all the peers
	 */
	public int uploaded;
	
	/**
	 * The interval of sending the HTTP GET Request to the Tracker
	 */
	public int interval;

	
	private DataOutputStream request;
	private DataInputStream  response;
	
	private ServerSocket listenSocket;
	
	private Map<byte[], String> peerList;
	private Map<byte[], Peer> peerHistory;
	private LinkedBlockingQueue<MessageTask> messagesQueue;
	
	private LinkedList<Integer>	havePiece;
	// just use removeFirst() to dequeue and addLast() to enqueue
	private LinkedList<Integer> needPiece;
	
	/**
	 * Client Constructor
	 * @param filePath Source of the torrent file
	 * @param saveName The file you want to save in.
	 */
	public Client(TorrentInfo torrent, String saveName){
		System.out.println("Booting");
		//this.numPacketsDownloaded = 0;
		this.saveName = saveName;
		this.torrentInfo = torrent;
		this.bitfield = new boolean[this.torrentInfo.piece_hashes.length];
		this.downloadsInProgress = new boolean[this.torrentInfo.piece_hashes.length];
		this.url = this.torrentInfo.announce_url;
		this.createFile();
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		this.havePiece = new LinkedList<Integer>();
		this.needPiece = new LinkedList<Integer>(); 
		this.userQuit = false;
		updateDownloaded();
		genClientID();
	}
	
	/**
	 * Client Constructor
	 * This is called when the file already exist. 
	 * @param torrent Source of the torrent file
	 * @param file The RandomAccessFile file
	 */
	public Client(TorrentInfo torrent, RandomAccessFile file){
		System.out.println("Booting");
		this.torrentInfo = torrent;
		this.bitfield = checkfile(torrent, file);
		this.url = this.torrentInfo.announce_url;
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		this.havePiece = new LinkedList<Integer>();
		this.needPiece = new LinkedList<Integer>(); 
		this.userQuit = false;
		ToolKit.print(this.blocks);
		updateDownloaded();
		genClientID();
	}
	
	/**
	 * @return The Client's ID.
	 */
	public byte[] getClientID() {
		return this.clientID;
	}
	
	public Message generateBitfieldMessage() {
		Message bitfieldMessage = new Message(this.bitfield.length+1,(byte)6);
		bitfieldMessage.bitfield(convertBooleanBitfield(this.bitfield));
		return bitfieldMessage;
	}
	
	
	/**
	 * Updates the downloaded values, left values, and uploaded values.
	 * Returns the number of bytes that have been successfully downloaded and confirmed to be correct
	 * Based on pieces that have been downloaded
	 **/
	private void updateDownloaded() {
		int retVal = 0;
		
		for( boolean bool : this.bitfield ) {
			if(bool) {
				retVal++;
			}
		}
		if(bitfield[bitfield.length-1]) {
			retVal -= 1;
			retVal *= this.torrentInfo.piece_length;
			retVal += (this.torrentInfo.file_length % this.torrentInfo.piece_length);
		} else {
			retVal *= this.torrentInfo.piece_length;
		}
		
		this.downloaded = retVal;
		this.left = this.torrentInfo.file_length - this.downloaded;
	}
	
	private byte[] convertBooleanBitfield(boolean[] bitfield) {
		//Calcuate the number of bytes needed to contain the bitfield
		byte[] bytes = new byte[(int)Math.ceil( bitfield.length / 8.0 )];
		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)0;
			for(int j = 0; j < 8; j++) {
				byte curr = (byte)0;			
				//If you hit the full length of the bitfield array, finish.
				if((i*8+j) >= bitfield.length) {
					bytes[i]<<=(7-j);
					break;
				}
				//If the boolean array contains a 1, set curr to 0x1.
				if( bitfield[i*8+j] ) {
					curr = (byte)1;
				}
				//bitwise calculation to append the bit to the end of the current byte
				bytes[i] = (byte)(bytes[i]|curr);
				//left shift for the next byte, unless you are already at the end of the current byte
				if(j != 7) {
					bytes[i]<<=1;
				}
			} //for each bit in a byte
		} //for each byte
		return bytes;
	}
	
	/**
	 * verify sha1 hashes right match with otrrentinfo.. .match with all pieces
	 * then set bitfield
	 * make bitfield
	 * of things that are donwloaded or not
	 * RETURNS BITFIELD.
	 */
	
	private boolean[] checkfile(TorrentInfo torrent, RandomAccessFile datafile){
	    boolean[] lovefield = new boolean[this.torrentInfo.piece_hashes.length];
	    try{
	        int piece_length = this.torrentInfo.piece_length;
	        //System.out.println("What the love is the piecelength: " + piece_length);
	        int dividend = (int)Math.ceil((double)datafile.length() / (double)this.torrentInfo.piece_length);
	        System.out.println("Dividend: " + dividend);
	        byte[] readbyte = new byte[piece_length];

	        System.out.println("DATAFILE LENGTH: " + datafile.length());
	        System.out.println("LOVE YOU: " + piece_length * (dividend-1));
	        int lastlength = (int)datafile.length() % piece_length;
	        System.out.println("lastlength::: " + lastlength);

	        for(int i = 0; i < dividend; i++){
	            boolean datacheck = false;
	            if(i == dividend-1){
	                byte[] readbyte2 = new byte[lastlength];
	                System.out.println("What the love is happening");
	                int loveoffset = i * piece_length;
	                datafile.seek((long)loveoffset);
	                System.out.println("I is " + i + " and loveoffset is " + loveoffset);
	                datafile.read(readbyte2, 0, lastlength);
	                datacheck = checkData(readbyte2, i);
	                System.out.println("Datacheck:: " + datacheck);

	            }//end of if
	            else{
	                int loveoffset = i * piece_length;
	                datafile.seek((long)loveoffset);
	                System.out.println("I is " + i + " and loveoffset is " + loveoffset);
	                datafile.read(readbyte, 0, piece_length);
	                datacheck = checkData(readbyte, i);
	                System.out.println("Datacheck:: " + datacheck);
	            }//end of else
	            lovefield[i] = datacheck;
	        }//end of for 
	    }//end of try
	    catch(IOException e){
	        System.out.println("IOEXCEPTION CHECKFILE");
	    }//end of catch
	    return lovefield;

	}//end of checkfile
	
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
	 * Initialize the tracker and send the HTTP GET Message to the Tracker
	 * @param port = LISTEN port that the client will use for incoming BT connections 
	 * @return true for success, otherwise false.
	 */
	
	public boolean connectToTracker(final int port){
		this.tracker = new Tracker(this.torrentInfo.announce_url, this.torrentInfo.info_hash.array(), clientID, port);
		this.peerList = tracker.sendHTTPGet(this.uploaded, this.downloaded, this.left, "started");
		this.peerHistory = new HashMap<byte[], Peer>();
		this.interval = tracker.getInterval() * 1000;
		if(this.peerList == null){
			return false;
		}
		System.out.println("Number of Peer List: " + peerList.size());
		return true;
	}
	
	/**
	 *	Send a "stopped" event to the tracker
	 */
	public void disconnectFromTracker(){
		if(tracker != null) {
			tracker.sendHTTPGet(this.uploaded, this.downloaded, this.left, "stopped");
			this.userQuit = true;
		}
		//response can be ignored because we're disconnecting anyway
		System.out.println("Sent STOPPED event to tracker");
	}
	
	private static class requestTracker extends Thread{
		private Timer requestTracker = new Timer();
		private long lastRequestSent = System.currentTimeMillis();
		
		/**
		 * RequestTracker Object
		 */
		public requestTracker(){
			/* DO NOTHING */
		}
		
		/**
		 * Sending the HTTP GET Request to the Tracker
		 * @param client The Client Object
		 */
		public void run(final Client client){
			System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			
			this.requestTracker.scheduleAtFixedRate(new TimerTask(){;

				@Override
				public void run() {
					if(System.currentTimeMillis() - lastRequestSent > client.tracker.getInterval()){
						client.updateDownloaded();
						Map<byte[], String> peerList = client.tracker.sendHTTPGet(client.uploaded, client.downloaded, client.left, "");
						if(peerList == null){
							return;
						}
						Map<byte[], Peer> peerHistory = client.peerHistory;
						ArrayList<String> peerListString = new ArrayList<String>(peerList.values());
						ArrayList<Peer> peerHistoryPeer = new ArrayList<Peer>(peerHistory.values());
						boolean found = false;
						for(int i = 0; i < peerListString.size(); i++){
							String[] ipPort = peerListString.get(i).split(":");
							for(int z = 0; z < peerHistoryPeer.size(); z++){
								Peer peer = peerHistoryPeer.get(z);
								if(ipPort[0].equals(peer.getPeerIP()) && 
										ipPort[1].equals(String.valueOf(peer.getPeerPort()))){
									found = true;
									break;
								}
							}
							if(found == true){
								//System.out.println("Same Peer");
								found = false;
							}else{
								//Add a new Peer
								System.out.println("Found a new Peer");
								byte[] peerID = findByteArray(peerList, peerListString.get(i));
								Peer newPeer = new Peer(client, peerID, ipPort[0], Integer.valueOf(ipPort[1]));
								peerHistory.put(peerID, newPeer);
								//TODO When we found a new peer, should we automatically 
								//connect to it. 
							}
						}
						/*Iterator it = peerList.entrySet().iterator();
						while(it.hasNext()){
							Map.Entry pairs = (Map.Entry)it.next();
							System.out.println(pairs.getKey() + " = " + pairs.getValue());
							it.remove();
						}*///end of while 
					}//end of if 
				}//end of void run()
				
				private byte[] findByteArray(Map<byte[], String> peerList, String ipPort){
					if(peerList == null){
						return null;
					}
					
					Set<byte[]> keys = peerList.keySet();
					Iterator<byte[]> iter = keys.iterator();
					while(iter.hasNext()){
						byte[] peerID = iter.next();
						if(peerList.get(peerID).equals(ipPort)){
							return peerID;
						}
					}
					
					return null;
				}
				
			}, new Date(), client.interval);
		}// end of run
	}//end of requestTracker method

	/**
	 * ConnectToPeers will go through the current list of peers and connect to them
	 */
	public void connectToPeers(){
		if(peerList.isEmpty()){
			/* DO NOTHING */
			System.out.println("THERE ARE NO PEERS");
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
		(new requestTracker()).run(this);
		this.start();
	}
	
	/**
	 * Start the thread that reads the messages
	 * from the peer
	 */
	public void run(){
		while(this.userQuit == false){
			readQueue();
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
		System.out.println("Reading the peer messages");
		if(message.getLength() == 0){
			/* Keep Alive Message */
			//TODO
			return;
		}
		
		ByteBuffer pieceBuffer;
		
		switch(message.getMessageID()){
			case 0: /* choke */
				peer.setRemoteChoking(true);
				break;
			case 1: /* unchoke */
				peer.setRemoteChoking(false);
				//TODO: Look at the Peer bitfield and compare that to the Client bitfield.
				//TODO: Request for pieces that the client do not have. 
				break;
			case 2: /* interested */
				//TODO: The Client can send a Unchoke or Choke Message to the Peer.
				peer.setRemoteInterested(true);
				peer.setLocalChoking(false);
				peer.writeToSocket(Message.unchoke);
				break;
			case 3: /* not interested */
				if(peer.isChokingLocal() == false) {
					peer.setLocalChoking(true);
					peer.writeToSocket(Message.choke);
				}
				peer.setRemoteInterested(false);
				break;
			case 4: /* have */
				pieceBuffer = ByteBuffer.allocate(message.getPayload().length);
				pieceBuffer.put(message.getPayload());
				pieceBuffer.reset();
				int pieceIndex = pieceBuffer.getInt();
				peer.updatePeerBitfield(pieceIndex);
				break;
			case 5: /* bitfield */
				byte[] bitfield = message.getPayload();
				peer.setPeerBooleanBitField(convert(bitfield));
				break;
			case 6: /* request */
				if(peer.isChokingLocal() == false) {
					pieceBuffer = ByteBuffer.allocate(message.getPayload().length);
					pieceBuffer.put(message.getPayload());
					pieceIndex = pieceBuffer.getInt();
					int beginIndex = pieceBuffer.getInt();
					int lengthReq = pieceBuffer.getInt();
					byte[] pieceRequested;
					pieceRequested = this.readLocalData(pieceIndex,beginIndex,lengthReq);
					Message pieceMessage = new Message((1+4+4+lengthReq),(byte)7); //Create a message with length 1+2 ints+length,7).
					pieceMessage.piece(pieceIndex,beginIndex,pieceRequested);
				}
				break;
			case 7: /* piece */
				//Reads the message
				pieceBuffer = ByteBuffer.allocate(message.getPayload().length);
				byte[] temp = new byte[message.getPayload().length - 8];
				pieceBuffer.put(message.getPayload());
				pieceBuffer.reset();
				int pieceNo = pieceBuffer.getInt();
				int offset = pieceBuffer.getInt();
				pieceBuffer.get(temp);
				
				//Stores this in the peer's internal buffer
				byte[] piece = peer.writeToInternalBuffer(temp,offset);
				
				//If the length of the buffer is equal to the piece, then check the SHA-1
				if(piece.length == this.getPieceLength()) {
					//If the SHA-1 matches, then write it to the file
					if(checkData(piece,pieceNo)) {
						this.downloadsInProgress[pieceNo]=false;
						this.bitfield[pieceNo]=true;
						writeData(piece,pieceNo);
						Message haveMessage = new Message(5,(byte)4); //Create a message with length 5 and classID 4.
						haveMessage.have(pieceNo);
						broadcastMessage(haveMessage);
						//write this message to all peers
						//peer.writeToSocket(haveMessage);
					} else {
						//failed sha-1, increment badPeer by 1, check if >3, if so, kill the peer
					}
				}
				
				break;
			case 8: /* cancel */
				//TODO: STOP THE SEND OF A PIECE IF THERE IS ONE
				//TODO: The Peer already have the piece. 
				//TODO: 
				break;
			case 9: /* Unofficial quit */
				
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
	
	/**
	 * @return The Torrent Info Hash.
	 */
	public static byte[] getHash(){
		return torrentInfo.info_hash.array();
	}
	
	
	public static int getLastPieceLength() {
		int temp = torrentInfo.file_length%torrentInfo.piece_length;
		if (temp == 0) {
			temp = torrentInfo.piece_length;
		}
		return temp;
	}
	/**
	 * @return The Torrent Piece Length.
	 */
	public static int getPieceLength(){
		return torrentInfo.piece_length;
	}
	
	/**
	 * @return The file length as per the torrent info file
	 */
	public int getFileLength(){
		return this.torrentInfo.file_length;
	}
	
	/**
	 * @return The number of pieces as per the torrent info file
	 */
	public int getNumPieces() {
		return this.torrentInfo.piece_hashes.length;
	}
	
	/**
	 * After handshaking, the Client starts download message from the 
	 * peers. 
	 * @return true for success, otherwise false
	 */
	private boolean beginDownload(){
		//System.out.println("ALL SYSTEMS GO!");
		//System.out.println("DOWNLOADING PACKETS!");
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
	 * This function will broadcast a message to all peers
	 */
    private void broadcastMessage(Message message) {
		Iterator iter = this.peerHistory.entrySet().iterator();
		Peer curr;
		//Iterate through all the values in the list and send the message
		while(iter.hasNext()) {
			Map.Entry pair = (Map.Entry)iter.next();
			curr = (Peer)pair.getValue();
			//Should check to see if the peer has not been killed because of bad messages.
			if(curr != null) {
				curr.writeToSocket(message);
			}
		}
	}
	
	/**
	 * Send piece requests for download a piece from peer
	 * @param pieceIndex zero based piece index
	 * @param peer the peer to download from
	 */
	private boolean getPiece(int pieceIndex, Peer remotePeer ) {
		this.downloadsInProgress[pieceIndex] = true;
		if(remotePeer.amChoked()) {
			return false;
		}
		Message request;
		int length;
		int blockOffset = 0;
		//Check if this is the oddball final piece)
		int leftToRequest = this.getPieceLength();
		if(pieceIndex == this.getNumPieces()-1) {
			leftToRequest = (this.getFileLength()%this.getPieceLength());
			if(leftToRequest == 0) {
				leftToRequest = this.getPieceLength();
			}
		} else {
			leftToRequest = this.getPieceLength();
		}
		
		
		while(leftToRequest>0) {
			if(leftToRequest > this.MAXIMUMLIMT) {
				length = this.MAXIMUMLIMT;
			} else {
				length = leftToRequest;
			}
			request = new Message((length+1),(byte)6);
			request.request(pieceIndex,blockOffset,length);
			leftToRequest-=length;
			remotePeer.writeToSocket(request);
		}
		
		return true;
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
     * Write a piece of data at the offset given
     * @param dataPiece A piece of a file
     * @param pieceOffset Where the piece is located to the file (0-based piece index)
     */
    private void writeData(byte[] dataPiece, int pieceOffset) {
		try {
			this.dataFile.seek(pieceOffset*this.getPieceLength());
			this.dataFile.write(dataPiece);
		} catch (IOException e) {
			System.err.println("ERROR IN WRITING TO FILE");
			System.err.println("Piece Offset: " + pieceOffset);
			System.err.println("Data Offset: " + pieceOffset*this.getPieceLength());
		}
	}
	
	/**
     * Read the piece of data at the offset given
     * @param dataOffset the zero-indexed piece index
     * @param blockOffset zero-indexed byte index
     * @param length number of bytes to read
     * @return a byte[] with the requested data
     */
    private byte[] readLocalData(int dataOffset, int blockOffset, int length) {
		byte[] retVal = new byte[length];
		try {
			this.dataFile.seek(dataOffset*this.getPieceLength()+blockOffset);
			this.dataFile.read(retVal);
			return retVal;
		} catch (IOException e) {
			System.err.println("ERROR IN READING PIECE FROM FILE");
			System.err.println("Piece Offset: " + dataOffset);
			System.err.println("Data Offset: " + dataOffset*this.getPieceLength()+blockOffset);
			return null; //If you reach here, there was an error.
		}
	}
    
    /**
     * Checks the current bitfield and the remote bitfield for a piece to download
     * @return the zero based index of the piece to download, or -1 if no piece to download
     */ 
    private int findPieceToDownload(Peer remote) {
		boolean[] peerBitfield = remote.getBitfields();
		for(int i = 0; i < peerBitfield.length; i++) {
			if(this.bitfield[i] == false && peerBitfield[i] == true && this.downloadsInProgress[i] == false) {
				if(remote.amChoked()) {
					remote.writeToSocket(Message.interested);
				}
				return i;
			}
		}
		return -1;
	}
    
    /**
     * Check the pieces with the torrentInfo.pieces_hash
     * @param dataPiece A piece of a file
     * @param dataOffset Where the piece is located to the file
     * @return true for success, otherwise false
     */
    private boolean checkData(byte[] dataPiece, int dataOffset) {
        
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