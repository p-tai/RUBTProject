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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractQueue;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

//DEFINITION: THERE ARE N-BLOCKS THAT MAKE THE FILES
//THERE ARE N-PACKETS THAT MAKE EACH BLOCKS

public class Client extends Thread{
	
	private byte[] clientID = new byte[20];
		
	private Tracker tracker;
    
	private static TorrentInfo torrentInfo;
    private URL url;
    
	private String saveName;
	private RandomAccessFile dataFile;
	
	public final static int MAXIMUMLIMT = 16384;
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
	private DataInputStream response;
	
	private ServerSocket listenSocket;
	
	private ArrayList<Peer> peerList;
	private ArrayList<Peer> peerHistory;
	private LinkedBlockingQueue<MessageTask> messagesQueue;
	
	private LinkedBlockingQueue<String> processPiece;
	// just use removeFirst() to dequeue and addLast() to enqueue
	private LinkedBlockingQueue<String> needPiece;
	
	/**
	 * Client Constructor. Default constructor when the target output file does NOT exist.
	 * @param filePath Source of the torrent file
	 * @param saveName The file you want to save in.
	 */
	public Client(TorrentInfo torrent, String saveName){
		System.out.println("Booting");
		//this.numPacketsDownloaded = 0;
		this.saveName = saveName;
		this.torrentInfo = torrent;
		
		this.url = this.torrentInfo.announce_url;
		this.createFile();
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		this.bitfield = new boolean[this.torrentInfo.piece_hashes.length];
		this.downloadsInProgress = new boolean[this.torrentInfo.piece_hashes.length];
		//this.havePiece = new LinkedList<Integer>();
		//this.needPiece = new LinkedList<Integer>(); 
		this.userQuit = false;
		updateDownloaded();
		genClientID();
	}
	
	/**
	 * Client Constructor
	 * This is called when the target output file already exists (to resume the download). 
	 * @param torrent Source of the torrent file
	 * @param file The RandomAccessFile file
	 */
	public Client(TorrentInfo torrent, RandomAccessFile file){
		System.out.println("Booting");
		this.torrentInfo = torrent;
		this.bitfield = checkfile(torrent, file);
		this.url = this.torrentInfo.announce_url;
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		this.downloadsInProgress = new boolean[this.torrentInfo.piece_hashes.length];
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
		Message bitfieldMessage = new Message(((int)Math.ceil(bitfield.length / 8.0))+1,(byte)6);
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
		byte[] bytes = new byte[(int)Math.ceil(bitfield.length / 8.0)];
		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte)0;
			for(int j = 0; j < 8; j++) {
				if((i*8+j)==bitfield.length) {
					break;
				}
				byte curr = (byte)0;
				//If the boolean array contains a 1, set curr to 0x1.
				if( bitfield[i*8+j] ) {
					//bitwise or to append the bit to the end of the current byte
					curr = (byte)1;
					curr <<= (7-j);
					bytes[i] = (byte)(bytes[i]|curr);
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
	        //System.out.println("Dividend: " + dividend);
	        byte[] readbyte = new byte[piece_length];

	        //System.out.println("DATAFILE LENGTH: " + datafile.length());
	        //System.out.println("LOVE YOU: " + piece_length * (dividend-1));
	        int lastlength = (int)datafile.length() % piece_length;
	        //System.out.println("lastlength::: " + lastlength);

	        for(int i = 0; i < dividend; i++){
	            boolean datacheck = false;
	            if(i == dividend-1){
	                byte[] readbyte2 = new byte[lastlength];
	                //System.out.println("What the love is happening");
	                int loveoffset = i * piece_length;
	                datafile.seek((long)loveoffset);
	                //System.out.println("I is " + i + " and loveoffset is " + loveoffset);
	                datafile.read(readbyte2, 0, lastlength);
	                datacheck = checkData(readbyte2, i);
	                //System.out.println("Datacheck:: " + datacheck);

	            }//end of if
	            else{
	                int loveoffset = i * piece_length;
	                datafile.seek((long)loveoffset);
	                //System.out.println("I is " + i + " and loveoffset is " + loveoffset);
	                datafile.read(readbyte, 0, piece_length);
	                datacheck = checkData(readbyte, i);
	                //System.out.println("Datacheck:: " + datacheck);
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
		this.tracker = new Tracker(this, this.torrentInfo.announce_url, this.torrentInfo.info_hash.array(), clientID, port);
		this.peerList = tracker.sendHTTPGet(this.uploaded, this.downloaded, this.left, "started");
		this.peerHistory = new ArrayList<Peer>();
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
						ArrayList<Peer> peerList = client.tracker.sendHTTPGet(client.uploaded, client.downloaded, client.left, "");
						if(peerList == null){
							return;
						}
						ArrayList<Peer> peerHistory = client.peerHistory;
						
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
		for(int i = 0; i < this.peerList.size(); i++){
			this.peerList.get(i).start();
		}
		
		//Start the Request the peers list from the tracker
		(new requestTracker()).run(this);
		this.start();
		(new requestPieces(this)).start();
	}
	
	private static class requestPieces extends Thread{

		/**********************************
		 * Request 
		 * Length Prefix: 13
		 * MessageID: 6
		 * Payload: <index><begin><length>
		 **********************************/
		private Client client;
		private int fileLength;
		private int pieceLength;
		private boolean[] bitfield;
		private LinkedBlockingQueue<String> processPiece;
		private LinkedBlockingQueue<String> needPiece;
		
		/**
		 * RequestPiece Constructor
		 * @param client The client Object
		 */
		public requestPieces(Client client){
			this.client = client;
			this.client.processPiece = new LinkedBlockingQueue<String>();
			this.client.needPiece = new LinkedBlockingQueue<String>();
			this.processPiece = this.client.processPiece;
			this.needPiece = this.client.needPiece;
			this.fileLength = this.client.torrentInfo.file_length;
			this.pieceLength = this.client.torrentInfo.piece_length;
			this.bitfield = this.client.bitfield;
			updateNeedPiece();
		}
		
		/**
		 * 
		 */
		private void updateNeedPiece(){
			double numberOfPieces = Math.ceil((double)(this.fileLength/this.pieceLength));
			double numberOfIndexPerPiece = (this.pieceLength/this.client.MAXIMUMLIMT);	
			double leftOver = this.fileLength - (this.pieceLength * numberOfPieces);
			/*
			System.out.println();
			System.out.println("File Length = " + this.fileLength);
			System.out.println("Piece Length = " + this.pieceLength);
			System.out.println("Number of Pieces = " + numberOfPieces);
			System.out.println("Number of Index Per Piece = " + numberOfIndexPerPiece);
			System.out.println("LeftOver = " + leftOver);
			System.out.println();
			TODO Remove this print statments
			*/
			if(isAllTrue(this.bitfield)){
				/* Have the file */
				// SEEDER
				return;
			}
			
			for(int i = 0; i < numberOfPieces; i++){
				if(this.bitfield[i] == false){
					for(int z = 0; z < numberOfIndexPerPiece; z++){
						String requestMessage = i + ":" + 
							(z * this.client.MAXIMUMLIMT) + ":" + this.client.MAXIMUMLIMT;
						this.needPiece.add(requestMessage);
					}						
				}
			}
			
			if((int)leftOver != 0){
				if(this.bitfield[(int) numberOfPieces] == false){
					String requestMessage = ((int)numberOfPieces) + ":" + 0 + ":" + (int)leftOver; 
					this.needPiece.add(requestMessage);					
				}
			}
			
			//System.out.println(this.needPiece);
		}
		
		/**
		 * Checks to see if the bitField is all false
		 * @param bitField The bitfield
		 * @return true if it is all false, otherwise false
		 */
		private boolean isAllFalse(boolean[] bitField){
			for(boolean b: bitField) if(b) return false;
			return true;
		}
		
		/**
		 * Checks to see if the bitField is all true
		 * @param bitField The bitfield
		 * @return true if it is all true, otherwise false
		 */
		private boolean isAllTrue(boolean[] bitField){
			for(boolean b: bitField) if(!b) return false;
			return true;
		}
		
		/**
		 * Sends a Interested Message to the Peer
		 * @param peer The Peer Object
		 */
		private void sendInterestedMessage(Peer peer){
			peer.enqueueMessage(Message.interested);
			while(!peer.amChoked()){
				System.out.println("Client unchoke");
				return;
			}
			return;
		}
		
		/**
		 * Request Pieces to the peers
		 */
		public void run(){
			if(this.needPiece.isEmpty()){
				/* Seeder */
				return;
			}
			//System.out.println("Need Piece size == " + this.needPiece.size());
			while(!this.needPiece.isEmpty()){
				//System.out.println(Arrays.toString(this.bitfield));
				Iterator<Peer> iter = this.client.peerHistory.iterator();
				//Iterator<byte[]> iter = keys.iterator();
				String[] request = this.needPiece.peek().split(":");
				int index = Integer.valueOf(request[0]);
				int begin = Integer.valueOf(request[1]);
				int length = Integer.valueOf(request[2]);
				while(iter.hasNext()){
					try {
					//TODO Remove this
					sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					//byte[] peerID = iter.next();
					//Peer peer = this.client.peerHistory.get(peerID);
					Peer peer = iter.next();
					int pieceIndex = this.client.findPieceToDownload(peer);
					if (pieceIndex == -1) {
						continue;
					}
					this.client.getPiece(pieceIndex, peer);
					
					/*
					
					if(peer.getBitfields()[index] == true){
						if(peer.isInterestedLocal() == false){
							peer.setLocalInterested(true);
							sendInterestedMessage(peer);
						}
						System.out.println(peer.isInterestedLocal());
						this.processPiece.add(this.needPiece.poll());
						Message message = new Message(13, (byte)6);
						System.out.println("SEND A REQUEST MESSAGE TO ");
						System.out.println(peer.getPeerIDString());
						System.out.println("WITH THE FOLLOWING PARAMTER");
						System.out.println("INDEX: " + index);
						System.out.println("BEGIN: " + begin);
						System.out.println("LENGTH: " + length);
						message.request(index, begin, length);
						peer.enqueueMessage(message);
						break;
					}
					*/
				}
			}
		}
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
		//System.out.println("Reading the peer messages");
		if(message.getLength() == 0){
			/* Keep Alive Message */
			//TODO
			return;
		}
		
		ByteBuffer pieceBuffer;
		
		switch(message.getMessageID()){
			case 0: /* choke */
				peer.setRemoteChoking(true);
				//stop current download
				//set respective flag to 0.
				break;
			case 1: /* unchoke */
				peer.setRemoteChoking(false);
				int index = findPieceToDownload(peer);
				if(index!=-1) {
					getPiece(index,peer);
				}
				//TODO: Request for pieces that the client do not have. 
				break;
			case 2: /* interested */
				//check the current number of choked peers and consider unchoking the peer.
				//might want to write a method for this.
				peer.setRemoteInterested(true);
				peer.setLocalChoking(false);
				peer.enqueueMessage(Message.unchoke);
				break;
			case 3: /* not interested */
				if(peer.isChokingLocal() == false) {
					peer.setLocalChoking(true);
					peer.enqueueMessage(Message.choke);
				}
				peer.setRemoteInterested(false);
				break;
			case 4: /* have */
				pieceBuffer = ByteBuffer.allocate(message.getPayload().length);
				pieceBuffer.mark();
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
				pieceBuffer.mark();
				pieceBuffer.put(message.getPayload());
				pieceBuffer.reset();
				int pieceNo = pieceBuffer.getInt();
				int offset = pieceBuffer.getInt();
				pieceBuffer.get(temp);
				
				//Stores this in the peer's internal buffer
				Piece piece = peer.writeToInternalBuffer(temp,pieceNo,offset);
				System.out.println("PIECE NUMBER " + pieceNo);
				
				//Check if the piece is finished
				if(piece.isFull()) {
					//Check if the payload was correct according to the SHA
					if(this.checkData(piece.getData(),piece.getPieceIndex())){
						//if so, write it to the random access file and reset the state of the piece
						this.writeData(piece.getData(), piece.getPieceIndex());
						this.downloadsInProgress[pieceNo]=false;
						this.bitfield[pieceNo]=true;
						writeData(piece.getData(),pieceNo);
						peer.resetPiece(); //reset piece for the next piece
						Message haveMessage = new Message(5,(byte)4); //Create a message with length 5 and classID 4.
						haveMessage.have(pieceNo);
						//write this message to all peers
						broadcastMessage(haveMessage);
					}
				} else {
					//failed sha-1, increment badPeer by 1, check if >3, if so, kill the peer
				}
				
				break;
			case 8: /* cancel */
				//TODO: STOP THE SEND OF A PIECE IF THERE IS ONE
				break;
			case 9: /* Unofficial quit */
				//TODO: iterate through all peers and shutdown all peers, announce to tracker, quit
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
	 * @return The Torrent Info Hash.
	 */
	public static byte[] getHash(){
		return torrentInfo.info_hash.array();
	}
	
	/**
	 * @param the zero-based piece index
	 * @return The Torrent Piece Length.
	 */
	public static int getPieceLength(int pieceIndex){
		//if the piece is the last piece, return the last piece size, otherwise, default piece size.
		if(pieceIndex == (torrentInfo.piece_hashes.length-1)) {
			int temp = torrentInfo.file_length%torrentInfo.piece_length;
			if (temp == 0) {
				temp = torrentInfo.piece_length;
			}
			return temp;
		} else {
			return torrentInfo.piece_length;
		}
	}
	
	/**
	 * @param the zero-based piece index
	 * @return The number of blocks for that one piece.
	 */
	public static int getNumBlocks(int pieceIndex) {
		return ((int)(Math.ceil(getPieceLength(pieceIndex)/MAXIMUMLIMT)));
	}
	
	/**
	 * @return The file length as per the torrent info file
	 */
	public static int getFileLength(){
		return torrentInfo.file_length;
	}
	
	/**
	 * @return The number of pieces as per the torrent info file
	 */
	public static int getNumPieces() {
		return torrentInfo.piece_hashes.length;
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
		Iterator<Peer> iter = this.peerHistory.iterator();
		Peer curr;
		//Iterate through all the values in the list and send the message
		while(iter.hasNext()) {
			Map.Entry pair = (Map.Entry)iter.next();
			curr = (Peer)pair.getValue();
			//Should check to see if the peer has not been killed because of bad messages.
			if(curr != null) {
				curr.enqueueMessage(message);
			}
		}
	}
	
	/**
	 * Send piece requests for download a piece from peer
	 * @param pieceIndex zero based piece index
	 * @param peer the peer to download from
	 */
	private boolean getPiece(int pieceIndex, Peer remotePeer ) {
		if(remotePeer.amChoked()) {
			return false;
		}
		this.downloadsInProgress[pieceIndex] = true;
		Message request;
		int length;
		int blockOffset = 0;
		//Check if this is the oddball final piece)
		int leftToRequest = this.getPieceLength(pieceIndex);
		
		while(leftToRequest>0) {
			if(leftToRequest > this.MAXIMUMLIMT) {
				length = this.MAXIMUMLIMT;
			} else {
				length = leftToRequest;
			}
			request = new Message(13,(byte)6);
			request.request(pieceIndex,blockOffset,length);
			leftToRequest-=length;
			remotePeer.enqueueMessage(request);
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
		synchronized(this.dataFile) {
			try {
				this.dataFile.seek(pieceOffset*this.getPieceLength(pieceOffset));
				this.dataFile.write(dataPiece);
			} catch (IOException e) {
				System.err.println("ERROR IN WRITING TO FILE");
				System.err.println("Piece Offset: " + pieceOffset);
				System.err.println("Data Offset: " + pieceOffset*this.getPieceLength(pieceOffset));
			}
		}
	}
	
	/**
     * Read the piece of data at the offset given
     * @param pieceOffset the zero-indexed piece index
     * @param blockOffset zero-indexed byte index
     * @param length number of bytes to read
     * @return a byte[] with the requested data
     */
    private byte[] readLocalData(int pieceOffset, int blockOffset, int length) {
		byte[] retVal = new byte[length];
		try {
			this.dataFile.seek(pieceOffset*this.getPieceLength(pieceOffset)+blockOffset);
			this.dataFile.read(retVal);
			return retVal;
		} catch (IOException e) {
			System.err.println("ERROR IN READING PIECE FROM FILE");
			System.err.println("Piece Offset: " + pieceOffset);
			System.err.println("Data Offset: " + pieceOffset*this.getPieceLength(pieceOffset)+blockOffset);
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
					remote.enqueueMessage(Message.interested);
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

        if(dataPiece.length != this.torrentInfo.piece_length || dataPiece.length != this.getPieceLength(torrentInfo.piece_hashes.length-1)) {
                //System.err.println("illegal piece length");
                //ilegal piece length
                return false;
        }
        
        byte[] SHA1 = new byte[20];
        System.out.println("Piece offset = " + dataOffset);
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
