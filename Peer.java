import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import edu.rutgers.cs.cs352.bt.util.*;

public class Peer extends Thread {

	private Client RUBT;
	private byte[] clientID;
	private byte[] peerID;
	private byte[] torrentSHA;
	private String peerIP;
	private int peerPort;
	
	private byte[] peerBitfield;
	
	private Socket peerConnection;
	private DataOutputStream outgoing;
	private DataInputStream incoming;
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
		if(handshake(this.torrentSHA) == true){
//			System.out.println("Connected to PeerID: " + Arrays.toString(this.peerID));
			try {
				//while the socket is connected
				//read from socket
				//parse message
				while(readSocketInputStream()){
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("CONNECTION FAILURE");
		}
		
	}
	
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
		//System.out.println("Length = "+ length);
		byte classID;
		Message incomingMessage;
		if(length == 0) {
			//keep alive is the only packet you can receive with length zero
			incomingMessage = Message.keepAlive;
		} else if(length > 0) {
			
			//Read the next byte (this should be the classID of the message)
			classID = this.incoming.readByte();
			
			//Debug statement
			System.out.println("Received classID: " + classID);
			
			//Length includes the classID. We are using length to determine how many bytes are left.
			length--;			
			incomingMessage = new Message(length+1,classID);
			
			//Handle the message based on the classID
			switch(classID) {
				case 0: //choke message
					this.localChoking = true;
					incomingMessage = Message.choke;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 1: //unchoke message
					this.localChoking = false;
					incomingMessage = Message.unchoke;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 2: //interested message
					this.localInterested = true;
					incomingMessage = Message.interested;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 3: //not interested message
					this.localInterested = false;
					incomingMessage = Message.uninterested;
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
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
					
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 5: //bitfield message
					byte[] bitfield = new byte[length];
					//update peer bitfield
					this.incoming.readFully(bitfield);
					this.peerBitfield = bitfield;
					//System.out.println(this.peerBitfield);
					
					
					
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
					
				case 6: //request message
					int requestPiece = this.incoming.readInt();
					int requestOffset = this.incoming.readInt();
					int requestLength = this.incoming.readInt();
					//if peerChoked == false
					//sendRequest(requestPiece, requestOffset, requestLength)
					//handle the request and send it to the peer
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
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
						this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
						return true;
					} catch(IOException e) {
						System.err.println("Received an incorrect input from peer during piece download");
					}
				case 8: //Cancel message
					int reIndex = this.incoming.readInt();
					int reOffset = this.incoming.readInt();
					int reLength = this.incoming.readInt();
					this.RUBT.queueMessage(new MessageTask(this,incomingMessage));
					break;
				default:
					System.err.println("Unknown class ID");
			}//switch
				
		}//if
		
		return true;
	}
	
}//Peer.java
