package src;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
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
	
	private final static int MAX_SIMUL_UPLOADS = 3;
	private final static int MAX_NUM_UNCHOKED = 6;
	private final Object counterLock = new Object();
	private int currentUploads;
	private int currentUnchoked;
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
	protected ArrayList<Peer> peerHistory;
	private LinkedBlockingQueue<MessageTask> messagesQueue;
	private PieceRequester pieceRequester;
	private Timer requestTracker = new Timer(true);
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
		this.isSeeder = false;
		this.currentUploads = 0;
		this.currentUnchoked = 0;
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
		this.currentUploads = 0;
		this.currentUnchoked = 0;
		this.isSeeder = false;
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
	
	public void removePeer(Peer peer){
		synchronized (this.peerHistory) {
			this.peerHistory.remove(peer);
		}
	}
	
	public void addPeer(Peer peer){
		synchronized (this.peerHistory) {
			this.peerHistory.add(peer);
		}
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
				this.listenSocket = new ServerSocket(Integer.parseInt(new String("688" + i)));
				System.out.println("PORT: " + Integer.valueOf(new String("688" + i)));
				//Start listing for peer connection by the Server Socket.
				(new ServerSocketConnection(this)).start();
				return (Integer.parseInt(new String("688" + i)));
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
			this.peerHistory.add(peer);
			peer.start();
		}
		
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
		 * Main thread of the PieceRequestor
		 * Responsible for reading the current idle peers and requesting pieces
		 */
		public void run(){
			//Continue until either the user quits or the program finishes downloading
			while(this.keepDownloading) {
				try {
					//If we have nothing left to download, exit the loop and clear the queue
					if(this.isAllTrue(this.client.getBitfield())) {
						this.keepDownloading = false;
						this.needPiece.clear();
						continue;
					}
					
					//get the next idle peer
					Peer current = this.needPiece.take();
					
					// Confirm we didn't already start downloading a piece for this peer 
					if(current.getPiece() == null) {
						//If the peer hasn't been shutdown, search for a piece
						if(current.isRunning()) {
							//Find the piece to download
							int pieceIndex = this.client.findPieceToDownload(current);
							if(pieceIndex >= 0) {
								//Tell the client to queue all the piece messages to the given peer's Writer class
								this.client.getPiece(pieceIndex,current);
							} 
							//In the event there is no piece we want to download, send uninterested if needed
							else if (current.amInterested()){
								//an uninterested message
								current.enqueueMessage(Message.uninterested);
							}
						}
					}
					//System.out.println("GET PIECE INDEX RETURNED: " + pieceIndex + "");
				} catch (InterruptedException ie) {
					//TODO handle exception
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
							System.out.println("New Peer from the Tracker");
							peerHistory.add(peer);
							peer.start();
						}
					}	
				}
			}//end of void run()
		}, Client.this.interval, Client.this.interval);

		//Start the download/upload peer ranker that will decide who gets a TCP connection
		this.requestTracker.scheduleAtFixedRate(new TimerTask() {
			/**
			 * Will sort the Peers based on their upload speed (to us) if we are downloading
			 * 
			 */
			@Override
			public void run() {
				//Initialize a list to store interested peers that are not downloading
				ArrayList<Peer> interested = new ArrayList<Peer>(Client.this.peerHistory.size());
				Peer slowest = null;
				synchronized(Client.this.peerHistory) {
					//Sort the arrays by download/upload rate (depending on whether we are seeder or not)
					Collections.sort(Client.this.peerHistory);
					int maxIndex = Client.this.peerHistory.size()-1;
					int count=0;
					int index = 0;
					while(index<maxIndex) {
						System.out.println("Currently reconsidering peers");
						Peer current = Client.this.peerHistory.get(index); 
						if(current.isInterestedLocal() && !current.isChokingLocal()) {
							count++;
							//hang onto the slowest peer...
							if (count >= MAX_SIMUL_UPLOADS) {
								slowest = current;
							} else {
								System.out.println("Kept " + current.getDownloadRate() + current);
							}
						} else if (current.isInterestedLocal()) {
							interested.add(current);
						}
						index++;
					}
				}
				//If there's no one else interested, there's no reason to choke/unchoke
				if(interested.size() < 1) {
					synchronized (Client.this.counterLock) {
						//choke the worst peer, if it exists
						if(slowest != null) {
							System.out.println("Removed " + slowest.getDownloadRate() + slowest);
							slowest.enqueueMessage(Message.choke);
							slowest.setLocalChoking(true);
							//If they were interested and unchoked, 
							if(slowest.isInterestedLocal()) {
								Client.this.currentUploads--;
							}
							Client.this.currentUnchoked--;
						}

						//pick random peers from the interested peers
						Collections.shuffle(interested);
						int index = 0;
						Peer random;
						while(Client.this.currentUploads < MAX_SIMUL_UPLOADS && index < interested.size()) {
							random = interested.get(index); 
							System.out.println("Added " + random.getDownloadRate() + random);
							//unchoke a random peer
							random.enqueueMessage(Message.unchoke);
							random.setLocalChoking(false);
							Client.this.currentUnchoked++;
							Client.this.currentUploads++;
							index++;
						}
					}
				}
			}
		}, 30000, 30000);
		
		
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
			while(this.client.keepReading){
				try {
					final Socket peerSocket = this.client.getListenSocket().accept();
					System.out.println("Server Socket Connection");
					Peer peer = new Peer(this.client, peerSocket);
					this.client.addPeer(peer);
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
		
		//If the poison message, stop the thread
		if(messageFromPeer.getMessage() == Message.KILL_PEER_MESSAGE) {
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
		case -1: /* Unofficial quit (poison)*/
			this.keepReading = false;
			break;
		case 0: /* choke */
			peer.setRemoteChoking(true);
			//If we were choked in the middle of downloading a piece...
			if(peer.getPiece() != null ) {
				//stop current download
				this.downloadsInProgress[peer.getPiece().getPieceIndex()] = false;
				peer.resetPiece();
				this.pieceRequester.queueForDownload(peer);
			}
			break;
		case 1: /* unchoke */
			peer.setRemoteChoking(false);
			//If we were unchoked add it to the pieceRequestor queue
			if(peer.getPiece() == null) {
				this.pieceRequester.queueForDownload(peer);
			}
			break;
		case 2: /* interested */
			//check the current number of choked peers and consider unchoking the peer.
			peer.setRemoteInterested(true);
			//If our client is choking them, consider unchoking them
			if(peer.isChokingLocal()) {
				synchronized(this.counterLock) {
					//If client can support more clients, do so
					if(this.currentUploads < MAX_SIMUL_UPLOADS && this.currentUnchoked < MAX_NUM_UNCHOKED) {
						peer.setLocalChoking(false);
						this.currentUploads++;
						this.currentUnchoked++;
						peer.enqueueMessage(Message.unchoke);
					}
				}
			}
			break;
		case 3: /* not interested */
			//Set remote interested to false
			//If they are unchoked, choke them and set their choked status true
			if((peer.isInterestedLocal()==true) && (peer.isChokingLocal() == false)) {
				peer.setLocalChoking(true);
				peer.enqueueMessage(Message.choke);
				synchronized(this.counterLock) {
					this.currentUploads--;
					this.currentUnchoked--;
				}
			}
			//If they are unchoked but not interested and send a not interested. Shouldn't occur but just in case.
			else if(peer.isChokingLocal() == false) {
				peer.setLocalChoking(true);
				peer.enqueueMessage(Message.choke);
				synchronized(this.counterLock) {
					this.currentUnchoked--;
				}
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
		this.requestTracker.cancel();
		try{
			System.out.println("Kill reader");
			this.messagesQueue.put(new MessageTask((Peer)null, Message.KILL_PEER_MESSAGE));
		} catch (InterruptedException ie) {
			//Don't care, shutting down
		}
		//iter all peers, shut down
		for(Peer peer: this.peerHistory) {
			if(peer != null) {
				System.out.println("Goodbye " + peer);
				peer.shutdownPeer();
				peer.interrupt();
			}
		}
		
		for(Peer peer: this.peerList) {
			if(peer != null) {
				System.out.println("Goodbye " + peer);System.out.println("Goodbye" + peer);
				peer.shutdownPeer();
				peer.interrupt();
			}
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
