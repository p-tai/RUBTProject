import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import edu.rutgers.cs.cs352.bt.util.*;
import java.util.Timer;
import java.util.TimerTask;

public class Peer extends Thread {

	private final Client RUBT;
	private final byte[] clientID;
	private final byte[] peerID;
	private final byte[] torrentSHA;
	private final String peerIP;
	private int peerPort;
	
	private byte[] peerBitfield;
	
	private final Socket peerConnection;
	private final DataOutputStream outgoing;
	private final DataInputStream incoming;
	private ByteBuffer buffer;
	private int bytesRead;
	
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
	private long lastMessageTime = System.currentTimeMillis();
	
	/**
	 * Peer's Constructor
	 * @param localID The Client's ID
	 * @param peerID The Peer's ID
	 * @param peerIP The Peer's IP
	 * @param peerPort The Peer's Port
	 */
	public Peer(Client RUBT, byte[] peerID, String peerIP, int peerPort){
		this.RUBT = RUBT;
		this.clientID = RUBT.getClientID();
		this.peerID = peerID;
		this.peerIP = peerIP;
		this.peerPort = peerPort;
		this.localChoking = true;
		this.localInterested= false;
		this.remoteChoking = true;
		this.remoteInterested = false;
		this.torrentSHA = Client.getHash();
	}
	
	/*********************************
	 * Getters
	 ********************************/
	
	public byte[] getPeerID() {
		return this.peerID;
	}
	
	public boolean isChokingLocal() {
		return this.localChoking;
	}
	
	public boolean isInterestedLocal() {
		return this.localInterested;
	}
	
	public boolean amChoked() {
		return this.remoteChoking;
	}
	
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
		ToolKit.print(this.peerID);
		try {
			this.peerConnection = new Socket(this.peerIP, this.peerPort);
			
			System.out.println("Opening Output Stream");
			this.outgoing = new DataOutputStream(peerConnection.getOutputStream());
			
			System.out.println("Opening Input Stream");
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
			
			System.out.println("Verify the SHA-1 HASH");
			
			//Check the peer's SHA-1 hash matches local SHA-1 hash
			for(int i = 0; i < 20; i++){
				if(this.torrentSHA[i] != hash[i]){
					System.err.println("THE SHA-1 HASH IS INCORRECT!");
					return false;
				}
			}
			
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
	 * Function to write a message to the outgoing socket.
	 */
	public void writeToSocket(Message payload){
		synchronized(this.outgoing) {
			try {
				this.outgoing.write(payload.getPayload());
				this.outgoing.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Main runnable thread process for the peer class.
	 * Will continously try to read from the incoming socket.
	 */
	public void run() {
		connect();
		
		//Create a timer task
		updateTimer();
		
		if(handshake(this.torrentSHA) == true){
//			System.out.println("Connected to PeerID: " + Arrays.toString(this.peerID));
			try {
				//while the socket is connected
				//read from socket (will block if it is empty)
				//parse message
				while(readSocketInputStream()){
					updateTimer();
				}//while
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("CONNECTION FAILURE");
		}
		
	}//run
	
	/**
	 * Schedules a new anonymous implementation of a TimerTask that
	 * will start now and execute every 10 seconds afterward.
	 * Sourced from CS352 Sakai Forums on 3.29.14
	 * @author Rob Moore
	 */
	private void updateTimer() {
		try {
			lastMessageTime = System.currentTimeMillis();
			this.keepAliveTimer.cancel();
			this.keepAliveTimer.scheduleAtFixedRate(new TimerTask(){
				public void run() {
					// Let the peer figure out how/when to send a keep-alive
					Peer.this.checkAndSendKeepAlive();
				}//run
			}, new Date(), 10000); //keepAliveTimer
		} catch(Exception e) { 
			//Catch this exception for now, caused by canceling the timer?
		}//try
	}//updateTimer
	
	/**
	 * This method is called on shutdown to close all of the data streams and sockets.
	 */
	public void shutdownPeer() {
		//to be implemented
		//Needs to close all input/output streams and then close the socket to peer.
		try {
			this.incoming.close();
			this.outgoing.close();
			this.peerConnection.close();
		} catch (Exception e) {
			//Doesn't matter because the peer is closing anyway
		}
	}
	
	private boolean readSocketInputStream() throws IOException {
		
		//NEED TO DO: Check if the connection still exists.  If not, return false
		//NEED TO DO: Pass the message up to the client class into a LinkedBlockedQueue
		
		int length = this.incoming.readInt();
		//System.out.println("Length = " + length);
		byte classID;
		Message incomingMessage = null;
		MessageTask incomingTask = new MessageTask(this,incomingMessage);
		
		if(length == 0) {
			//keep alive is the only packet you can receive with length zero
			incomingMessage = Message.keepAlive;
			this.RUBT.queueMessage(incomingTask);
		} else if(length > 0) {
			
			//Read the next byte (this should be the classID of the message)
			classID = this.incoming.readByte();
			incomingMessage = new Message(length,classID);
			
			//Debug statement
			System.out.println("Received classID: " + classID);
			
			//Length includes the classID. We are using length to determine how many bytes are left.
			length--;
			
			//Handle the message based on the classID
			switch(classID) {
				case 0: //choke message
					this.localChoking = true;
					incomingMessage = Message.choke;
					this.RUBT.queueMessage(incomingTask);
					break;
					
				case 1: //unchoke message
					this.localChoking = false;
					incomingMessage = Message.unchoke;
					this.RUBT.queueMessage(incomingTask);
					break;
					
				case 2: //interested message
					this.localInterested = true;
					incomingMessage = Message.interested;
					this.RUBT.queueMessage(incomingTask);
					break;
					
				case 3: //not interested message
					this.localInterested = false;
					incomingMessage = Message.uninterested;
					this.RUBT.queueMessage(incomingTask);
					break;
					
				case 4: //have message message
					int piece = this.incoming.readInt();
					byte[] temp = new byte[this.peerBitfield.length];
					
					//find the corresponding offset/bit that represents this piece.
					int index = piece / 8;
					int offset = piece % 8;
					
					//set the specific bit
					temp[index] = (byte)(0x01<<offset);
					
					//update peer bitfield
					this.peerBitfield[index] = (byte)(this.peerBitfield[index] & temp[index]);
					
					incomingMessage.have(piece);
					this.RUBT.queueMessage(incomingTask);
					
					break;
					
				case 5: //bitfield message
					byte[] bitfield = new byte[length];
					//update peer bitfield
					this.incoming.readFully(bitfield);
					this.peerBitfield = bitfield;
					
					incomingMessage.bitfield(bitfield);
					this.RUBT.queueMessage(incomingTask);
					break;
					
				case 6: //request message
					int requestPiece = this.incoming.readInt();
					int requestOffset = this.incoming.readInt();
					int requestLength = this.incoming.readInt();
					
					//handle the request and send it to the peer
					incomingMessage.request(requestPiece, requestOffset, requestLength);
					this.RUBT.queueMessage(incomingTask);
					break;
					
				case 7: //piece message
					try {
						int pieceIndex = this.incoming.readInt();
						int blockOffset = this.incoming.readInt();
						System.out.printf("Receiving piece %d offset %d \n", pieceIndex, blockOffset );
						length = length - 8; //Remove the length of the indexes and class ID
						
						byte[] payload = new byte[length];
						this.incoming.readFully(payload);
//						int blockIndex = pieceIndex + (int)Math.floor((blockOffset+1)/this.MAXIMUMLIMT);
//						System.out.println("BlockIndex "+ blockIndex);
						this.buffer.put(payload);
						this.bytesRead += length;
						
						System.out.println("Read " + length + " bytes from peer " + this.peerID);
						
						if (this.bytesRead >= Client.getPieceLength() ) {
							//check the piece data
							//put the entire buffer into the LinkedBlockQueue if SHA correct
							//If not, increment the number of failed downloads.
							//If the number of failed downloads is 3, kill the peer connection because it has corrupt data
							
						}
						/*this.dataFile.seek((long)pieceIndex*this.torrentInfo.piece_length+blockOffset);
						this.dataFile.write(payload);
						this.numPacketsDownloaded++;
						this.packets[blockIndex] = true;*/
						
					
						//if(blockIndex == this.numPacketsDownloaded) {
						//	return true;
						//}
						incomingMessage.piece(pieceIndex, blockOffset, payload);
						this.RUBT.queueMessage(incomingTask);
						return true;
					} catch(IOException e) {
						System.err.println("Received an incorrect input from peer during piece download");
					}
				case 8: //Cancel message
					int reIndex = this.incoming.readInt();
					int reOffset = this.incoming.readInt();
					int reLength = this.incoming.readInt();
					
					incomingMessage.cancel(reIndex,reOffset,reLength);
					this.RUBT.queueMessage(incomingTask);
					break;
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
	protected void checkAndSendKeepAlive(){
		long now = System.currentTimeMillis();
		if(now - this.lastMessageTime > KEEP_ALIVE_TIMEOUT){
			// The "sendMessage" method should update lastMessageTime
			this.sendMessage(new KeepAliveMessage());
			// Validate that the timestamp was updated
			if(now > this.lastMessageTime){
				throw new Exception("Didn't update lastMessageTime when sending a keep-alive!");
			}
			System.out.println("Sent Keep-Alive");
		}
	}//checkAndSendKeepAlive
	
}//Peer.java
