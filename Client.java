import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import edu.rutgers.cs.cs352.bt.TorrentInfo;


//DEFINITION: THERE ARE N-BLOCKS THAT MAKE THE FILES
//THERE ARE N-PACKETS THAT MAKE EACH BLOCKS

/**
 * @author Paul Tai
 * @author Alex Zhang
 * @author Anthony Wong
 */
public class Client extends Thread{

	private byte[] clientID;
	protected TorrentInfo torrentInfo;
	protected Tracker tracker;
	
	private String saveName;
	private RandomAccessFile dataFile;
	
	/**
	 * The Maximum Limit of download 
	 */
	public final static int MAXIMUMLIMT = 16384;
	
	private boolean[] bitfield;
	private boolean[] downloadsInProgress;
	private boolean keepReading;
	private boolean isSeeder;

	/**
	 * The number of bytes download from peers.
	 */
	int downloaded;

	/**
	 * The number of bytes of what left to download
	 */
	int left;

	/**
	 * The number of bytes uploaded to all the peers
	 */
	int uploaded;
	private final Object ULCountLock = new Object();
	
	/**
	 * The interval of sending the HTTP GET Request to the Tracker
	 */
	int interval;
	
	private ServerSocket listenSocket;

	private ArrayList<Peer> peerList;
	private ArrayList<Peer> peerHistory;
	private LinkedBlockingQueue<MessageTask> messagesQueue;
	private PieceRequester pieceRequester;
	private Timer requestTracker = new Timer();
	long lastRequestSent;

	/**
	 * Client Constructor. Default constructor when the target output file does NOT exist.
	 * @param torrent Source of the torrent file
	 * @param saveName The file you want to save in.
	 */
	public Client(TorrentInfo torrent, String saveName){
		System.out.println("No output file detected. \n Booting");
		this.saveName = saveName;
		this.torrentInfo = torrent;
		this.createFile();
		this.peerHistory = new ArrayList<Peer>();
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		this.bitfield = new boolean[this.torrentInfo.piece_hashes.length];
		this.downloadsInProgress = new boolean[this.torrentInfo.piece_hashes.length];
		//Updates the downloaded, left, and uploaded fields that will be sent to the tracker
		this.downloaded = 0;
		this.uploaded = 0;
		updateLeft();
		//generate a random Client ID, begins with the letters AAA
		genClientID();
	}

	/**
	 * Client Constructor
	 * This is called when the target output file already exists (to resume the download). 
	 * @param torrent Source of the torrent file
	 * @param file The RandomAccessFile file
	 */
	public Client(TorrentInfo torrent, RandomAccessFile file){
		System.out.println("Previous file detected. \n Booting");
		this.torrentInfo = torrent;
		this.dataFile = file;
		this.bitfield = checkfile(torrent, file);
		this.messagesQueue = new LinkedBlockingQueue<MessageTask>();
		this.downloadsInProgress = new boolean[this.torrentInfo.piece_hashes.length];
		this.peerHistory = new ArrayList<Peer>();
		//ToolKit.print(this.blocks);
		//Updates the downloaded, left, and uploaded fields that will be sent to the tracker
		this.downloaded = 0;
		this.uploaded = 0;
		updateLeft();
		//generate a random Client ID, begins with the letters AAA
		genClientID();
	}

	/**
	 * @return The Client's ID.
	 */
	public byte[] getClientID() {
		return this.clientID;
	}

	/**
	 * Getter that returns the total number of VALIDATED bytes (in other words bytes of pieces that pass SHA-1 verification)
	 * @return this.downloaded
	 */
	public int getBytesDownloaded() {
		return this.downloaded;
	}
	
	/**
	 * Getter that returns the total number of uploaded bytes (in other words bytes of pieces that pass SHA-1 verification)
	 * @return this.uploaded
	 */
	public int getBytesUploaded() {
		synchronized (this.ULCountLock) {
			return this.uploaded;
		}
	}
	
	/**
	 * Getter that returns the total number of bytes left
	 * @return this.left
	 */
	public int getBytesLeft() {
		return this.left;
	}

	/**
	 * Getter that returns the percentage completion of the file
	 * @return 1.0-(bytes left/file length)
	 */
	public double getPercentageCompletion() {
		return (1.0 - (double)this.left/(double)this.torrentInfo.file_length);
	}
	
	/**
	 * @return The Peer History.
	 */
	public ArrayList<Peer> getPeerHistory(){
		return this.peerHistory;
	}
	
	/**
	 * @return
	 */
	public ServerSocket getListenSocket(){
		return this.listenSocket;
	}
	
	/**
	 * @return
	 */
	public boolean[] getDownloadsInProgess(){
		return this.downloadsInProgress;
	}
	
	/**
	 * TODO
	 * @return TODO
	 */
	public Message generateBitfieldMessage() {
		Message bitfieldMessage = new Message(((int)Math.ceil(this.bitfield.length / 8.0))+1,(byte)5);
		bitfieldMessage.bitfield(convertBooleanBitfield(this.bitfield));
		return bitfieldMessage;
	}

	/**
	 * Getter that returns whether or not the client is seeding (i.e. the client is no longer downloading.
	 * @return this.isSeeder private variable
	 */
	public boolean isSeeder() {
		return this.isSeeder;
	}
	/**
	 * Updates the downloaded values, left values, and uploaded values.
	 * Returns the number of bytes that have been successfully downloaded and confirmed to be correct
	 * Based on pieces that have been downloaded
	 **/
	void updateLeft() {
		//Nothing to do if you're a seeder already
		if(this.isSeeder == true) {
			this.left = 0;
			return;
		}
		
		//Calculate the number of valid pieces
		int numValid = 0;

		for( boolean bool : this.bitfield ) {
			if(bool) {
				numValid++;
			}
		}
		
		//Consider the case of the oddball size of the last piece
		if(this.bitfield[this.bitfield.length-1]) {
			numValid -= 1;
			numValid *= this.torrentInfo.piece_length;
			numValid += (this.torrentInfo.file_length % this.torrentInfo.piece_length);
		} else {
			numValid *= this.torrentInfo.piece_length;
		}

		
		//calculate the amount of bytes left to download and validate
		this.left = this.torrentInfo.file_length - numValid;
		
		//set ourselves to seeder status if we are out of data to download
		if(this.left == 0) {
			this.isSeeder = true;
		}
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
	 * verify sha1 hashes right match with torrentinfo.. .match with all pieces
	 * then set bitfield
	 * make bitfield
	 * of things that are donwloaded or not
	 * RETURNS BITFIELD.
	 */

	private boolean[] checkfile(TorrentInfo torrent, RandomAccessFile datafile){
		//initialize a boolean array with length equal to the number of pieces
		boolean[] bitfield = new boolean[(this.torrentInfo.piece_hashes).length];
		
		int piece_length = this.torrentInfo.piece_length;
		
		//get space for the byte[] that will store each piece
		byte[] readbyte = new byte[piece_length];
		
		//variable that will calculate the starting position to read from for each piece
		long offset = 0;
		
		//loop through all of the pieces in the data file
		for(int i = 0; i < bitfield.length; i++) {
			try {
				//If the datafile is not long enough, it cannot contain the correct data
				if(offset > datafile.length()) {
					bitfield[i] = false;
				} else {
					//Otherwise, seek to the offset and then check the data
					datafile.seek(offset);

					//In the event you are on the final piece
					if(i == bitfield.length-1) {
						readbyte = new byte[getPieceLength(i)];
					}

					//Read the data into the readbyte byte[]
					datafile.read(readbyte);

					//Check the data using checkData, requires the byte[] and the piece offset (which is i)
					bitfield[i] = checkData(readbyte,i);
				}
			} catch(IOException e){
				//Should not occur because the index should never be negative or out of bounds
				System.err.println("IOEXCEPTION CHECKFILE");
			}
			
			//This recalcuate the new byte offset
			offset += getPieceLength(i);
			System.out.println("index " + i + " is " + bitfield[i]);
		}
		
		return bitfield;
	}//end of checkfile

	/**
	 * Adding a message from Peer to the Client's Messages Queue. 
	 * @param task The message from the Peer. 
	 */
	public void queueMessage(MessageTask task) {
		if(task.getMessage() == null){
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
				//Start listing for peer connection by the Server Socket.
				(new ServerSocketConnection(this)).start();
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
		this.tracker = new Tracker(this, this.torrentInfo.announce_url, this.torrentInfo.info_hash.array(), this.clientID, port);
		this.peerList = this.tracker.sendHTTPGet(this.uploaded, this.downloaded, this.left, "started");
		this.peerHistory = new ArrayList<Peer>();
		this.interval = this.tracker.getInterval() * 1000;
		if(this.peerList == null){
			return false;
		}
		System.out.println("Number of Peer List: " + this.peerList.size());
		return true;
	}

	/**
	 *	Send a "stopped" event to the tracker, called when shutting down.
	 */
	public void disconnectFromTracker(){
		//Make sure our "left" value is correct
		this.updateLeft();
		if(this.tracker != null) {
			this.tracker.sendHTTPGet(this.uploaded, this.downloaded, this.left, "stopped");
			//response can be ignored because we're disconnecting anyway
		}
	}

	/**
	 * 
	 */
	public void startPeerDownloads() {
		this.pieceRequester = new PieceRequester(this);
		Iterator<Peer> iter = this.peerHistory.iterator();
		for(int i = 0; i < this.peerList.size(); i++){
			this.pieceRequester.queueForDownload(this.peerList.get(i));
		}
		this.pieceRequester.start();
	}

	/**
	 * ConnectToPeers will go through the current list of peers and connect to them
	 */
	public void connectToPeers(){
		if(this.peerList.isEmpty()){
			/* DO NOTHING */
			System.out.println("THERE ARE NO PEERS");
			return;
		}
		System.out.println("Connecting to Peers");
		for(Peer peer: this.peerList) {
			peer.start();
		}
		
		//create and start running the piece requester
		this.start();
	}

	private static class PieceRequester extends Thread{

		/**********************************
		 * Request 
		 * Length Prefix: 13
		 * MessageID: 6
		 * Payload: <index><begin><length>
		 **********************************/
		private boolean keepDownloading;
		private Client client;
		private LinkedBlockingQueue<Peer> needPiece;

		/**
		 * RequestPiece Constructor
		 * @param client The client Object
		 */
		public PieceRequester(Client client){
			this.client = client;
			this.needPiece = new LinkedBlockingQueue<Peer>();
			this.keepDownloading = true;

		}

		/**
		 * queueForDownload - Adds the given peer to the PieceRequester's 
		 * 		LinkedBlockingQueue that holds all the peers that need to search a piece to download
		 * @param peer - the peer that needs to get a piece
		 */
		public void queueForDownload(Peer peer) {
			try {
				this.needPiece.put(peer);
			} catch (InterruptedException ie) {
				//TODO something with this exception
			}
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
		 * Request Pieces to the peers
		 */
		public void run(){
			while(this.keepDownloading) {
				try {
					Peer current = this.needPiece.take();

					if(this.isAllTrue(this.client.getBitfield())) {
						this.keepDownloading = false;
						continue;
					}

					int pieceIndex = this.client.findPieceToDownload(current);
					if(pieceIndex >= 0) {
						this.client.getPiece(pieceIndex,current);
					}
					//System.out.println("GET PIECE INDEX RETURNED: " + pieceIndex + "");
				} catch (InterruptedException ie) {
					// Whatever
				}
			}
			this.client.updateLeft();
			this.client.tracker.sendHTTPGet(this.client.uploaded, this.client.downloaded, this.client.left, "completed");
			return;
		}//run
	}//PieceRequester

	/**
	 * Start the thread that reads the messages from the peer
	 */
	public void run(){
		this.keepReading = true;
		this.lastRequestSent = System.currentTimeMillis();
		
		//Start the request tracker which will run every interval (as defined by the tracker) to update the peerlist and dl/ul/left stats
		this.requestTracker.scheduleAtFixedRate(new TimerTask() {
			/**
			 * Sends the HTTP GET Request to the tracker based on
			 * the tracker interval. It 
			 */
			public void run() {
				Client.this.updateLeft();
				ArrayList<Peer> peerList = Client.this.tracker.sendHTTPGet(Client.this.uploaded, Client.this.downloaded, Client.this.left, "");
				if(peerList == null){
					return;
				}

				ArrayList<Peer> peerHistory = Client.this.peerHistory;
				if(!peerList.isEmpty()){
					for(Peer peer: peerList) {
						if(!peerHistory.contains(peer)){
							//peerHistory.add(peer);
							//peer.start();
						}
					}	
				}
			}//end of void run()
		}, new Date(), Client.this.interval);

		while(this.keepReading) {
			readQueue();
		}
	}
	
	private static class ServerSocketConnection extends Thread{
		
		/**
		 * The Client Object.
		 */
		private Client client;
		
		/**
		 * ServerSocketConnection Object.
		 * @param client The Client Object.
		 */
		public ServerSocketConnection(Client client){
			this.client = client;
		}
		
		/**
		 * Checking to see if any peer is connection to the client
		 * by the server socket.
		 */
		public void run(){
			while(true){
				try {
					final Socket peerSocket = client.getListenSocket().accept();
					System.out.println("Server Socket Connection");
					Peer peer = new Peer(this.client, peerSocket);
					this.client.getPeerHistory().add(peer);
					peer.start();
				}catch (IOException e) {
					System.err.println("ERROR: ServerSocket");
				}
				
			}
		}
	}
	
	

	private void readQueue(){
		MessageTask messageFromPeer;
		try{ 
			messageFromPeer = this.messagesQueue.take();
		} catch(InterruptedException ie) {
			//TODO some stack trace...
			return;
		}
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
		case -1: /* Unofficial quit */
			this.keepReading = false;
			break;
		case 0: /* choke */
			peer.setRemoteChoking(true);
			//stop current download
			//set respective flag to 0.
			break;
		case 1: /* unchoke */
			peer.setRemoteChoking(false); 
			break;
		case 2: /* interested */
			//check the current number of choked peers and consider unchoking the peer.
			//might want to write a method for this.
			peer.setRemoteInterested(true);
			//TODO: Check if you want to do this...
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
				pieceBuffer.mark();
				pieceBuffer.put(message.getPayload());
				pieceBuffer.reset();
				pieceIndex = pieceBuffer.getInt();
				int beginIndex = pieceBuffer.getInt();
				int lengthReq = pieceBuffer.getInt();
				byte[] pieceRequested;
				pieceRequested = this.readLocalData(pieceIndex,beginIndex,lengthReq);
				Message pieceMessage = new Message((1+4+4+lengthReq),(byte)7); //Create a message with length 1+2 ints+length,7).
				pieceMessage.piece(pieceIndex,beginIndex,pieceRequested);
				peer.enqueueMessage(pieceMessage);
				
				//Increment the amount of bytes uploaded by our client
				synchronized (this.ULCountLock) {
					this.uploaded+=lengthReq;
				}
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
			//System.out.println("PIECE NUMBER " + pieceNo + " BLOCK OFFSET " + offset + " LENGTH " + temp.length);
			//Stores this in the peer's internal buffer
			Piece piece = peer.writeToInternalBuffer(temp,pieceNo,offset);
			
			//Check if the piece is finished
			if(piece.isFull()) {
				System.out.println("..........FULL PIECE RECEIVED: " + pieceNo + " " +peer);
				//Check if the payload was correct according to the SHA
				if(this.checkData(piece.getData(),piece.getPieceIndex())){
					//System.out.println("...........SHA-SUCCESSFUL");
					//if so, write it to the random access file and reset the state of the piece
					this.writeData(piece.getData(), piece.getPieceIndex());
					
					//Update the downloaded bytes count
					if(this.bitfield[pieceNo]==false) {
						this.downloaded+=piece.getData().length;
					}
					
					//update the internal bitfields
					this.downloadsInProgress[pieceNo]=false;
					(this.bitfield)[pieceNo]=true;
					
					peer.resetPiece(); //reset piece for the next piece
					
					Message haveMessage = new Message(5,(byte)4); //Create a message with length 5 and classID 4.
					haveMessage.have(pieceNo); 
					broadcastMessage(haveMessage); //write this message to all peers

					//requeue the peer in the pieceRequestor queue.
					this.pieceRequester.queueForDownload(peer);
				} else {
					//System.err.println("...........SHA- UNSUCCESSFUL")
					this.downloadsInProgress[pieceNo]=false;
					this.pieceRequester.queueForDownload(peer);
					peer.resetPiece();
				}
			}
			break;
		case 8: /* cancel */
			//TODO: Stop sending the piece if there is one?
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
		byte[] clientID = new byte[20];
		clientID[0] = 'A';
		clientID[1] = 'A';
		clientID[2] = 'A';
		Random randGen = new Random();
		for(int i = 3; i < 20; i++){
			clientID[i] = (byte)randGen.nextInt(9);
		}
		this.clientID = clientID;
	}

	/**
	 * @return The Torrent Info Hash.
	 */
	public byte[] getHash(){
		return this.torrentInfo.info_hash.array();
	}

	/**
	 * @return the local bitfield
	 */
	public boolean[] getBitfield(){
		return this.bitfield;
	}

	/**
	 * TODO
	 * @param pieceIndex the zero-based piece index
	 * @return The Torrent Piece Length.
	 */
	public int getPieceLength(int pieceIndex){
		//if the piece is the last piece, return the last piece size, otherwise, default piece size.
		if(pieceIndex == (this.torrentInfo.piece_hashes.length-1)) {
			int temp = this.torrentInfo.file_length%this.torrentInfo.piece_length;
			if (temp == 0) {
				temp = this.torrentInfo.piece_length;
			}
			return temp;
		} else {
			return this.torrentInfo.piece_length;
		}
	}

	/**
	 * TODO
	 * @param pieceIndex the zero-based piece index
	 * @return The number of blocks for that one piece.
	 */
	public int getNumBlocks(int pieceIndex) {
		return ((int)(Math.ceil(getPieceLength(pieceIndex)/MAXIMUMLIMT)));
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
	 * TODO
	 */
	public void shutdown() {
		this.keepReading = false;
		//iter all peers, shut down
		for(Peer peer: this.peerHistory) {
			if(peer != null) {
				System.out.println("Goodbye " + peer);System.out.println("Goodbye" + peer);
				peer.shutdownPeer();
			}
		}
		try{
			System.out.println("Request");		
			this.requestTracker.cancel();
			System.out.println("Kill reader");
			this.messagesQueue.put(new MessageTask((Peer)null, Message.KILL_PEER_MESSAGE));
		} catch (InterruptedException ie) {
			//Don't care, shutting down
		}
	}

	/*********************************
	 * Client->Tracker Private Functions
	 ********************************/

	/**
	 * This function will broadcast a message to all peers
	 */
	private void broadcastMessage(Message message) {
		ArrayList<Peer> temp = this.peerHistory;
		for(Peer peer: temp) {
			//Iterate through all the values in the list and send the message
			peer.enqueueMessage(message);
		}
	}

	/**
	 * Send piece requests for download a piece from peer
	 * @param pieceIndex zero based piece index
	 * @param peer the peer to download from
	 */
	synchronized void getPiece(int pieceIndex, Peer remotePeer ) {
		
		if(remotePeer.amChoked()) {
			remotePeer.enqueueMessage(Message.unchoke);
		}

		if(!remotePeer.amInterested()) {
			remotePeer.enqueueMessage(Message.interested);
		}
		
		this.downloadsInProgress[pieceIndex] = true;
		Message request;
		int length;
		int blockOffset = 0;

		//get the amount of bytes for this piece
		int leftToRequest = this.getPieceLength(pieceIndex);

		//Request pieces based on the block size (MAXIMUMLIMT) until there is nothing left to request
		while(leftToRequest>0) {
			if(leftToRequest > MAXIMUMLIMT) {
				length = MAXIMUMLIMT;
			} else {
				length = leftToRequest;
			}
			request = new Message(13,(byte)6);
			request.request(pieceIndex,blockOffset,length);
			blockOffset+=MAXIMUMLIMT;
			leftToRequest-=length;
			remotePeer.enqueueMessage(request);
		}
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
				long index = pieceOffset*this.getPieceLength(0);
				this.dataFile.seek(index);
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
			this.dataFile.seek(pieceOffset*this.getPieceLength(0)+blockOffset);
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
	int findPieceToDownload(Peer remote) {
		boolean[] peerBitfield = remote.getBitfields();
		
		for(int i = 0; i < (this.bitfield).length; i++) {
			if(this.bitfield[i] == false && peerBitfield[i] == true && this.downloadsInProgress[i] == false) {
				return i;
			}
		}
		
		for(int i = 0; i < (this.bitfield).length; i++) {
			if(this.bitfield[i]==false && peerBitfield[i] == true) {
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
	private synchronized boolean checkData(byte[] dataPiece, int dataOffset) {

		MessageDigest hasher = null;

		try {
			hasher = MessageDigest.getInstance("SHA");	
		} catch (NoSuchAlgorithmException e) {
			System.err.println("No such algorithm exception: " + e.getMessage());
			return false;
		}

		if(dataOffset >= this.torrentInfo.piece_hashes.length) {
			//illegal dataOffset value
			System.err.println("illegal dataOffset");
			return false;
		}

		byte[] SHA1;
		//Read the piece hash from the torrentInfo file and put it into SHA1
		SHA1 = ((this.torrentInfo.piece_hashes)[dataOffset]).array();
		
		byte[] checkSHA1 = hasher.digest(dataPiece);
		
		//System.out.println("Piece offset = " + dataOffset);// + " SHA " + checkSHA1 + " vs " + SHA1);
		
		if(SHA1.length != checkSHA1.length) {
			return false;
		}
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
