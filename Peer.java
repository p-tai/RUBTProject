import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import edu.rutgers.cs.cs352.bt.util.*;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer extends Thread{

	private final Client RUBT;
	private final byte[] clientID;
	private final byte[] peerID;
	private String peerIDString;
	private final byte[] torrentSHA;
	private final String peerIP;
	private int peerPort;
	
	private boolean[] peerBooleanBitField;
	
	private Socket peerConnection;
	private DataOutputStream outgoing;
	private DataInputStream incoming;
	private PeerWriter writer;
	private Piece pieceInProgress;
	private boolean[] blocksDownloaded;
	
	/**
	 * Flags for local/remote choking/interested
	 */
	private boolean localChoking;
	private boolean localInterested;
	private boolean remoteChoking;
	private boolean remoteInterested;
	
	/**
	 * Timer code is sourced form Sakai on 3.29.14
	 * @author Rob Moore
	 */
	private static final long KEEP_ALIVE_TIMEOUT = 120000;
	private Timer keepAliveTimer = new Timer();
	private Timer peerTimeoutTimer = new Timer();
	private Timer rateChecker = new Timer();
	private long lastMessageTime = System.currentTimeMillis();
	private long lastPeerTime = System.currentTimeMillis();
	private double downloadRate;
	private double uploadRate;
	private int recentBytesDownloaded;
	private Object DLCountLock = new Object();
	private int recentBytesUploaded;
	private Object ULCountLock = new Object();
	
	/**
	 * Peer's Constructor
	 * @param localID The Client's ID
	 * @param peerID The Peer's ID
	 * @param peerIP The Peer's IP
	 * @param peerPort The Peer's Port
	 */
	public Peer(Client RUBT, byte[] peerID, String peerIP, int peerPort) {
		this.RUBT = RUBT;
		this.clientID = RUBT.getClientID();
		this.peerID = peerID;
		try {
			//FOR DEBUG PURPOSES ONLY (to make peer id human readable)
			this.peerIDString = new String(peerID, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.peerBooleanBitField = new boolean[RUBT.getNumPieces()];
		this.peerIP = peerIP;
		this.peerPort = peerPort;
		this.localChoking = true;
		this.localInterested= false;
		this.remoteChoking = true;
		this.remoteInterested = false;
		this.torrentSHA = Client.getHash();
		this.peerConnection = null;
		this.uploadRate = 0.0;
		this.downloadRate = 0.0;
		this.recentBytesDownloaded = 0;
		this.recentBytesUploaded = 0;
	}

	/*********************************
	 * Setters
	 ********************************/
	
	/**
	 * The status of the Peer chocking the Client.
	 * @param remoteChoking true = The Peer is Chocking the Client.
	 * Otherwise, false.
	 */
	public void setLocalChoking(boolean localChoking){
		this.localChoking = localChoking;
	}
	
	public void setPeerConnection(Socket peerConnection){
		this.peerConnection = peerConnection;
	}

	public void setOutgoing(DataOutputStream outgoing){
		this.outgoing = outgoing;
	}

	public void setIncoming(DataInputStream incoming){
		this.incoming = incoming;
	}

	/**
	 * The status of the Client being interested of the Peer. 
	 * @param localInterested true = The Client is the Peer.
	 * Otherwise, false.
	 */
	public void setLocalInterested(boolean localInterested){
		this.localInterested = localInterested;
	}
	
	/**
	 * The status of the Peer Choking the Client. 
	 * @param remoteChoking true = Peer is Choking the Client.
	 * Otherwise, false.
	 */
	public void setRemoteChoking(boolean remoteChoking){
		this.remoteChoking = remoteChoking;
	}
	
	/**
	 * The status of the Peer being interested of the Client.
	 * @param remoteInterested true = Peer is interested the Client.
	 * Otherwise, false.
	 */
	public void setRemoteInterested(boolean remoteInterested){
		this.remoteInterested = remoteInterested;
	}
	
	/**
	 * Set the Peer Boolean Bit Field. 
	 * @param peerBooleanBitField The Peer Boolean Bit Field. 
	 */
	public void setPeerBooleanBitField(boolean[] peerBooleanBitField){
		this.peerBooleanBitField = peerBooleanBitField;
	}
	
	/**
	 * Writes a byte[] to the peer's internal buffer.
	 * Also checks if the buffer is full.
	 * @param payload The payload that will be written to buffer. Should come from a peer message.
	 * @param blockOffset Offset to write into at the buffer - should come from a peer message.
	 * @return The entire contents of the buffer or null.
	 */ 
	public Piece writeToInternalBuffer(byte[] payload, int pieceOffset, int blockOffset) {
		if(this.pieceInProgress == null) {
			this.pieceInProgress = new Piece(pieceOffset, this.RUBT.getPieceLength(pieceOffset), this.RUBT.getNumBlocks(pieceOffset));
		}
		
		//Write the payload to the piece
		this.pieceInProgress.writeToBuffer(pieceOffset,blockOffset,payload);
		synchronized(this.DLCountLock){
			this.recentBytesDownloaded+=payload.length;
		}
		return this.pieceInProgress;
	}
	
	public void resetPiece(){
		if(this.pieceInProgress.isFull()) {
			this.pieceInProgress = null;
		}
	}
	
	private boolean isAllTrue(boolean[] blocks){
		for(boolean b: blocks) if(!b) return false;
		return true;
	}

	/*********************************
	 * Getters
	 ********************************/
	
	/**
	 * @return The Peer's IP
	 */
	public String getPeerIP(){
		return this.peerIP;
	}
	
	public double getDownloadRate() {
		double retVal;
		synchronized(this.DLCountLock) {
			retVal = this.downloadRate;
		}
		return retVal;
	}
	
	public double getUploadRate() {
		double retVal;
		synchronized(this.ULCountLock) {
			retVal = this.uploadRate;
		}
		return retVal;
		
	}
	
	/**
	 * @return The Peer's Port
	 */
	public int getPeerPort(){
		return this.peerPort;
	}
	
	/**
	 * @return The Peer's ID
	 */
	public byte[] getPeerID() {
		return this.peerID;
	}
	
	/**
	 * @return The Peer's ID as a String
	 */
	public String getPeerIDString(){
		return this.peerIDString;
	}
	
	/**
	 * @return The status of the Client choking the peer.
	 */
	public boolean isChokingLocal() {
		return this.localChoking;
	}
	
	/**
	 * @return The status of Peer interested of the Client. 
	 */
	public boolean isInterestedLocal() {
		return this.localInterested;
	}
	
	/**
	 * @return The status of the Client being choking by Peer.
	 */
	public boolean amChoked() {
		return this.remoteChoking;
	}
	
	/**
	 * @return This peer's bitfield as a boolean array
	 */
	public boolean[] getBitfields() {
		return this.peerBooleanBitField;
	}
	
	/**
	 * @return The status of the Client interested of the Peer.
	 */
	public boolean amInterested() {
		return this.remoteInterested;
	}
	
	public String toString(){
		return "Peer ID: " + this.peerID + " Peer IP: " + this.peerIP + " Peer Port: " + this.peerPort;
	}
	
	
	/**
	* Opens a connection to the peer.
	* @return true for success, otherwise false
	*/
	public void connect(){
		System.out.println("Connecting to " + this.peerIDString);
		try {
			//System.out.println(this.peerIP.split(":")[0]);
			this.peerConnection = new Socket(this.peerIP, this.peerPort);
			
			System.out.println("Opening Output Stream to " + this.peerIDString);
			this.outgoing = new DataOutputStream(peerConnection.getOutputStream());
			
			System.out.println("Opening Input Stream to " + this.peerIDString);
			this.incoming = new DataInputStream(peerConnection.getInputStream());
			
			if(peerConnection == null || this.outgoing == null || this.incoming == null) {
				System.err.println("Input/Output Stream Creation Failed");
			}
			
		} catch (UnknownHostException e) {
				System.err.println(this.peerID + " user not found.");
		} catch (IOException e) {
				System.err.println("Input/Output Stream Creation Failed");
		}
		
	}//end of connect()
	
	/**
	* Send a Handshake Message to the Peer. Will also verify that the returning handshake is valid.
	* @param byte[] containing the SHA1 of the entire file.
	* @return true for success, otherwise false (i.e. the handshake failed)
	*/
	private boolean handshake(byte[] infoHash){
		try {
			//Sends an outgoing message to the connected Peer.
			System.out.println("\nSENDING A HANDSHAKE TO" + this.peerIDString + "\n");
			outgoing.write(Message.handshakeMessage(infoHash, this.clientID));
			this.outgoing.flush();
			
			//Allocate space for the response and read it
			byte[] response = new byte[68];
			byte[] hash = new byte[20];
			this.incoming.readFully(response);
			
			//TO DO: Check that the PEER_ID given is a VALID PEER ID
			for(int i = 0; i < 20; i++){
				hash[i] = response[28 + i];
			}
			
			//System.out.println("Verify the SHA-1 HASH");
			
			//Check the peer's SHA-1 hash matches local SHA-1 hash
			for(int i = 0; i < 20; i++){
				if(this.torrentSHA[i] != hash[i]){
					//System.err.println("THE SHA-1 HASH IS INCORRECT!");
					return false;
				}
			}
			
			//System.out.println("THE SHA-1 HASH IS CORRECT!");
			return true;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("HANDSHAKE FAILURE!");
			return false;
		}
	}
	
	/**
	 * Update the Peer Bitfield.
	 * @param pieceIndex The Piece Index.
	 */
	public void updatePeerBitfield(int pieceIndex){
		if(pieceIndex >= this.peerBooleanBitField.length){
			System.out.println("ERROR: UPDATING PEER BIT FIELD");
			System.out.println("INVALID PIECE INDEX");
			return;
		}
		
		this.peerBooleanBitField[pieceIndex] = true;
	}
	
	
	/**
	 * Function to write a message to the outgoing socket.
	 * @param payload message to be sent to peer
	 */
	public void writeToSocket(Message payload){
		//In case the client, peer, or peerWriter threads wantto write to socket at the same time
		synchronized(this.outgoing) {
			try {
				if(payload.getLength() == 0){
					/* Keep Alive */
					System.out.println("Sending Keep Alive to " + this.peerIDString);
					System.out.println();
				}else{
					System.out.println("Sending " + Message.getMessageID(payload.getMessageID()) + " " + this.peerIDString);
					System.out.println();
				}
				//get message payload, write to socket, then update the keep alive timer
				
				//If the message payload is a piece message, update the uploaded bytes counter
				if(payload.getMessageID() == 7) {
					synchronized(this.ULCountLock) {
						this.recentBytesUploaded += payload.getLength();
					}
				}
					
				this.outgoing.write(payload.getBTMessage());
				this.outgoing.flush();
				updateTimer();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class PeerWriter extends Thread {

		private LinkedBlockingQueue<Message> messageQueue;
		private DataOutputStream outgoing;
		
		/**
		 * PeerWriter Constructor
		 * @param dataoutputstream = the stream to write out all data to
		 */
		public PeerWriter(DataOutputStream outgoing) {
			this.outgoing = outgoing;
			this.messageQueue = new LinkedBlockingQueue<Message>();
		}//peerWriter constuctor
		
		/**
		 * Public method to add a message to the peerWriter's internal queue
		 */
		public void enqueue(Message message) {
			messageQueue.add(message);
		}//enqueue
		
		/**
		 * Main runnable thread for the peerWriter private class
		 */
		public void run(){
			Message current;
			//if the queue contains a poison, exit the thread, otherwise just keep going
			while( (current = messageQueue.poll()) != Message.KILL_PEER_MESSAGE ) {
				if( current != null ) {
					Peer.this.writeToSocket(current);
				}
			}
		}//run
		
	}//peerWriter
	
	/**
	 * Main runnable thread process for the peer class.
	 * Will connect and handshake continously try to read from the incoming socket.
	 */
	public void run() {
		
		//Check if the peer exists. If not, connect. If it does, just keep the current socket (they handshaked with us)
		if(this.peerConnection == null) {
			connect();

			if(handshake(this.torrentSHA) == true){
			//System.out.println("Connected to PeerID: " + Arrays.toString(this.peerID));
				System.out.println("HANDSHAKE RECEIVED");
				System.out.println("FROM:" + this.peerIDString);
			
				//Send Bitfield to Peer
				if(this.RUBT.downloaded != 0) {
					Message bitfieldMessage = RUBT.generateBitfieldMessage();
					writeToSocket(bitfieldMessage);
				}
			} else {
				System.out.println("CONNECTION FAILURE");
			}
		}
		
		writer = new PeerWriter(this.outgoing);
		
		/**
		* Schedules a new anonymous implementation of a TimerTask that
		* will start now and execute every 10 seconds afterward.
		* Sourced from CS352 Sakai Forums on 3.29.14
		* @author Rob Moore
		*/
		this.keepAliveTimer.scheduleAtFixedRate(new TimerTask(){
			public void run() {
				// Let the peer figure out when to send a keepalive
				Peer.this.checkAndSendKeepAlive();
			}//run
		}, 10000, 10000); //keepAlive Transmission Timer
		
		updateTimer();
		
		this.peerTimeoutTimer.scheduleAtFixedRate(new TimerTask(){
			public void run() {
				// Let the peer figure out when to send a keepalive
				Peer.this.checkPeerTimeout();
			}//run
		}, 10000, 10000); //peerTimeout timer
		
		updatePeerTimeoutTimer();
		
		//Checks the upload and download rates every 10 seconds
		this.rateChecker.scheduleAtFixedRate(new TimerTask(){
			public void run() {
				Peer.this.updateRates();
			}//run
		}, 2000, 2000); //peerTimeout timer
		
		try {
			//while the socket is connected
			//read from socket (will block if it is empty) and parse message
			while(readSocketInputStream()){
				//Update the PEER TIMEOUT timer to a new value (because we received a packet).
				updatePeerTimeoutTimer();
			}//while
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//try
				
	}//run
	
	/**
	 * This method is called on shutdown to close all of the data streams and sockets.
	 */	
	public void shutdownPeer() {
		//to be implemented
		//close all input/output streams and then close the socket to peer.
		try {
			//kill the I/O streams
			this.incoming.close();
			this.outgoing.close();
			//kill the writer thread
			this.writer.enqueue(Message.KILL_PEER_MESSAGE);
			//kill the socket
			this.peerConnection.close();
			//cancel all the timers
			this.keepAliveTimer.cancel();
			this.peerTimeoutTimer.cancel();
			this.rateChecker.cancel();
		} catch (Exception e) {
			//Doesn't matter because the peer is closing anyway
		}
	}
	
	private boolean readSocketInputStream() throws IOException {
		
		//NEED TO DO: Check if the connection still exists.  If not, return false
		
		int length = this.incoming.readInt();
		//System.out.println("Length = " + length);
		byte classID;
		Message incomingMessage = null;
		
		if(length == 0) {
			//keep alive is the only packet you can receive with length zero
			incomingMessage = Message.keepAlive;
			this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
		} else if(length > 0) {
			
			//Read the next byte (this should be the classID of the message)
			classID = this.incoming.readByte();
			incomingMessage = new Message(length,classID);
			//Debug statement
			//TODO Remove this and add it to the Client readQueue. 
			System.out.println("Received " + Message.getMessageID(classID).toUpperCase() + " Message");
			System.out.println("FROM " + this.peerIDString);
			System.out.println();
			//Length includes the classID. We are using length to determine how many bytes are left.
			length--;
			
			//Handle the message based on the classID
			switch(classID) {
				case 0: //choke message
					incomingMessage = Message.choke;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 1: //unchoke message
					incomingMessage = Message.unchoke;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 2: //interested message
					incomingMessage = Message.interested;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 3: //not interested message
					incomingMessage = Message.uninterested;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 4: case 5: case 6: case 7: case 8: 
				//have message message. bitfield message, request message, piece message, cancel message
					byte[] temp = new byte[length];
					this.incoming.readFully(temp);
					incomingMessage.setPayload(temp);
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
				/*
				case 8: //Cancel message
					int reIndex = this.incoming.readInt();
					int reOffset = this.incoming.readInt();
					int reLength = this.incoming.readInt();
					
					incomingMessage.cancel(reIndex,reOffset,reLength);
					this.RUBT.queueMessage(incomingTask);
					break;
				*/
				default:
					System.err.println("Unknown class ID");
			}//switch
				
		}//if
		
		return true;
	}
	
	/**
	 * Sends a keep-alive message to the remote peer if the time between now
	 * and the previous message exceeds the limit set by KEEP_ALIVE_TIMEOUT.
	 * Sourced from CS352 Sakai Forums on 3.29.14
	 * @author Rob Moore
	 */
	protected void checkPeerTimeout(){
		long now = System.currentTimeMillis();
		if((now - Peer.this.lastPeerTime) > KEEP_ALIVE_TIMEOUT*1.05){
			//Peer timed out, should kill the peer
			Peer.this.shutdownPeer();
		}
	}//checkAndSendKeepAlive
	
	protected void checkAndSendKeepAlive(){
		long now = System.currentTimeMillis();
		if((now - Peer.this.lastMessageTime) > KEEP_ALIVE_TIMEOUT*0.25){
			Peer.this.writeToSocket(Message.keepAlive);
		}
	}//checkAndSendKeepAlive
	
	
	private void updateRates() {
		//System.out.println("Updating download/upload rates");
		
		synchronized((Peer)this.ULCountLock) {
			//If we have been downloading consistently...
			if(this.uploadRate != 0) {
				//Takes a weighted average of the current upload rate and the historical upload rate
				this.uploadRate = this.uploadRate*.65+this.recentBytesUploaded/2.0*.35;
				//reset the recent counter
				this.recentBytesUploaded = 0;
				
			} else if(this.recentBytesUploaded == 0){
				//you aren't uploading any data, reset the upload rate to 0
				this.uploadRate=0.0;
				
			} else {
				//if you just started downloading, just set the rate = to the window rate / 2 seconds
				this.uploadRate = this.recentBytesUploaded/2.0;
				
			}
			this.recentBytesUploaded = 0;
		}
		
		synchronized((Peer)this.DLCountLock) {
			//If we have been downloading consistently...
			if(this.downloadRate != 0) {
				//Takes a weighted average of the current download rate and the historical download rate
				this.downloadRate = this.downloadRate*.65+this.recentBytesDownloaded/2.0*.35;
				//reset the recent counter
				this.recentBytesDownloaded = 0;
				
			} else if(this.recentBytesUploaded == 0){
				//you aren't downloading any data reset the download rate
				this.downloadRate = 0.0;
				
			} else {
				//if you just started downloading, just set the rate = to the window rate / 2 seconds
				this.downloadRate = this.recentBytesDownloaded/2.0;
			}
			this.recentBytesDownloaded = 0;
		}
		
	}//updateRate
	
	private void updateTimer() {
		//System.out.println("Updating message time");
		this.lastMessageTime = System.currentTimeMillis();
	}//updateTimer
	
	private void updatePeerTimeoutTimer() {
		//System.out.println("Updating last packet from peer time");
		this.lastPeerTime = System.currentTimeMillis();
	}//updatePeerTimeoutTimer


	public boolean equals(Object obj){
		if(obj == null || !(obj instanceof Peer)){
			return false;
		}
		
		Peer peer = (Peer)obj;
		byte[] peerID = peer.getPeerID();
		
		for(int i = 0; i < this.peerID.length; i++){
			if(this.peerID[i] != peerID[i]){
				return false;
			}
		}
		
		return true;
	}
	
}//Peer.java
