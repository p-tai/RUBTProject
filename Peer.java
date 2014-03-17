import java.net.*;

public class Peer extends Thread {

	private byte[] clientID;
	private byte[] peerID;
	private String peerIP;
	private int peerPort;
	private Socket peerConnection;
	private DataOutputStream outgoing;
	private DataInputStream incoming;
	
	
	/**
	 * Flags for local/remote choking/interested
	 */
	private boolean amChoking;
	private boolean amInterested;
	private boolean peerChoking;
	private boolean peerIntersted;
	
	/**
	 * They is interested in the client.  
	 */
	private boolean isInterested;
	
	/**
	 * 
	 * @param peerID The peer's ID.
	 * @param peerIP The peer's IP.
	 * @param peerPort The peer's Port.
	 */
	public Peer(byte[] localID, byte[] peerID, String peerIP, int peerPort){
		this.clientID = localID;
		this.peerID = peerID;
		this.peerIP = peerIP;
		this.peerPort = peerPort;
		this.amChoking = true;
		this.amIntersted = false;
		this.peerChoking = true;
		this.peerInterested = false;
	}
	
	/*********************************
	 * Getters
	 ********************************/
	
	public String getPeerID() {
		return peerID;
	}
	
	public String toString(){
		return "Peer ID: " + this.peerID + " Peer IP: " + this.peerIP + " Peer Port: " + this.peerPort;
	}
	
	
	/**
	* Opens a connection to the peer.
	* @return true for success, otherwise false
	*/
	public void connect(){
		System.out.println("CONTACTING " + this.peerID);
		
		try {
			Socket socket = new Socket(this.peerIP, this.peerPort);
			
			System.out.println("Opening Output Stream");
			this.outgoing = new DataOutputStream(socket.getOutputStream());
			
			System.out.println("Opening Input Stream");
			this.incoming = new DataInputStream(socket.getInputStream());
			
			if(socket != null && request != null && response != null) {
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
			outgoing.write(handshakeMessage(infoHash, this.clientID));
			byte[] response = new byte[68];
			byte[] hash = new byte[20];
			this.response.readFully(response);
			
			for(int i = 0; i < 20; i++){
				hash[i] = response[28 + i];
			}
			
			System.out.println("Verify the SHA-1 HASH");
			
			for(int i = 0; i < 20; i++){
				if(this.torrentInfo.info_hash.array()[i] != hash[i]){
					System.err.println("THE SHA-1 HASH IS INCORRECT!");
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
	
	private boolean sendUnchoke(){
		
	}
	
	private boolean sendInterested(){
		
	}
	
	public run() {
		//while the socket is connected
		//read from socket
		//parse message
		
		
	}
	
	private boolean readSocketOutputStream() throws IOException {
		
		int length = this.incoming.readInt();
		//System.out.println("Length = "+ length);
		byte classID;
		
		if(length == 0) {
			//keep alive
		} else if(length > 0) {
			length--;
			classID = this.response.readInt();
			
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
					byte[] bitfield = [length];
					length-=4;
					//update peer bitfield
					break;
				case 6:
					//request
					int index = this.incoming.readInt();
					int begin = this.incoming.readInt();
					int length = this.incoming.readInt();
					//if peerChoked == false
					//handle the request and send it to the peer
					break;
				case 7:
					//piece
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
				default:
					System.err.println("Unknown class ID");
			}
					
					
					
			}
		return false;
	}
}
