import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import edu.rutgers.cs.cs352.bt.util.*;

public class Peer extends Thread {

	private byte[] clientID;
	private byte[] peerID;
	private String peerIP;
	private int peerPort;
	private Socket peerConnection;
	private DataOutputStream outgoing;
	private DataInputStream incoming;
	private byte[] torrentSHA;	
	private ByteBuffer buffer;
	/**
	 * Flags for local/remote choking/interested
	 */
	private boolean amChoking;
	private boolean amInterested;
	private boolean peerChoking;
	private boolean peerInterested;
	
	/**
	 * They is interested in the client.  
	 */
	private boolean isInterested;
	
	/**
	 * Peer's Constructor
	 * @param localID The Client's ID
	 * @param peerID The Peer's ID
	 * @param peerIP The Peer's IP
	 * @param peerPort The Peer's Port
	 */
	public Peer(byte[] localID, byte[] peerID, String peerIP, int peerPort){
		this.clientID = localID;
		this.peerID = peerID;
		this.peerIP = peerIP;
		this.peerPort = peerPort;
		this.amChoking = true;
		this.amInterested= false;
		this.peerChoking = true;
		this.peerInterested = false;
		this.torrentSHA = Client.getHash();
	}
	
	/*********************************
	 * Getters
	 ********************************/
	
	public byte[] getPeerID() {
		return this.peerID;
	}
	
	public String toString(){
		return "Peer ID: " + this.peerID + " Peer IP: " + this.peerIP + " Peer Port: " + this.peerPort;
	}
	
	
	/**
	* Opens a connection to the peer.
	* @return true for success, otherwise false
	*/
	public void connect(){
		System.out.print("CONTACTING ");
		ToolKit.print(this.peerID);
		System.out.println();
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
	* Send a Handshake Message to the Peer
	* @return true for success, otherwise false
	*/
	private boolean sendHandshake(byte[] infoHash){
		try {
			outgoing.write(Message.handshakeMessage(infoHash, this.clientID));
			byte[] response = new byte[68];
			byte[] hash = new byte[20];
			this.incoming.readFully(response);
			
			for(int i = 0; i < 20; i++){
				hash[i] = response[28 + i];
			}
			
			System.out.println("Verify the SHA-1 HASH");
			
			for(int i = 0; i < 20; i++){
				if(this.torrentSHA[i] != hash[i]){
					System.err.println("THE SHA-1 HASH IS INCORRECT!");
					return false;
				}
			}
			
			this.outgoing.flush();
			System.out.println("THE SHA-1 HASH IS CORRECT!");
			return true;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("HANDSHAKE FAILURE!");
			return false;
		}
	}
	
	private boolean sendUnchoke(){
		return false;
	}
	
	private boolean sendInterested(){
		return false;
	}
	
	public void run() {
		//while the socket is connected
		//read from socket
		//parse message
		connect();
		if(sendHandshake(this.torrentSHA) == true){
			System.out.println("Connected to PeerID: " + Arrays.toString(this.peerID));
			try {
				while(readSocketOutputStream()){
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("CONNECTION FAILURE");
		}
		
	}
	
	private boolean readSocketOutputStream() throws IOException {
		
		int length = this.incoming.readInt();
		//System.out.println("Length = "+ length);
		byte classID;
		
		if(length == 0) {
			//keep alive
		} else if(length > 0) {
			length--;
			classID = this.incoming.readByte();
			System.out.println("Length: " + length);
			System.out.println("classID: " + classID);
			//System.out.println("Class ID = "+ classID); 
			//choke, unchoke, interested, or not interested
			switch(classID) {
				case 0:
					//choke
					break;
				case 1:
					//unchoke
					break;
				case 2:
					//interested
					break;
				case 3:
					//not interested
					break;
				case 4:
					//have message
					int piece = this.incoming.readInt();
					//update peer bitfield
					break;
				case 5:
					//bitfield
					byte[] bitfield = new byte[length];
					length-=4;
					//update peer bitfield
					break;
				case 6:
					//request
					int index = this.incoming.readInt();
					int begin = this.incoming.readInt();
					int requestLength = this.incoming.readInt();
					//if peerChoked == false
					//handle the request and send it to the peer
					break;
				case 7:
					//piece
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
						/*this.dataFile.seek((long)pieceIndex*this.torrentInfo.piece_length+blockOffset);
						this.dataFile.write(payload);
						this.numPacketsDownloaded++;
						this.packets[blockIndex] = true;*/
						System.out.println("updated data");
					
						//if(blockIndex == this.numPacketsDownloaded) {
						//	return true;
						//}
					
						return true;
					
					} catch(IOException e) {
						System.err.println("Received an incorrect input from peer");
					} 
				default:
					System.err.println("Unknown class ID");
			}
					
					
					
			}
		return false;
	}
}
