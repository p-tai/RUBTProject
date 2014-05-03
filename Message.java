import java.nio.ByteBuffer;

//TODO Add Cancel method and function?

/**
 * @author Paul Tai
 * @author Alex Zhang
 * @author Anthony Wong
 */
public class Message {


	/**
	 * Static KILL_PEER_MESSAGE that will be inserted to message queues
	 * when shutdown methods are called, notifying threads to stop reading
	 */
	public static final Message KILL_PEER_MESSAGE = new Message(0,(byte)-1);
	/**
	 * The KeepAlive Message representing a BT Keep-Alive message
	 */
	public static final Message keepAlive = new Message(0,(byte)1);
	
	/**
	 * The Choke Message representing a BT Choke message
	 */
	public static final Message choke = new Message(1,(byte)0); 
	
	/**
	 * The Unchoke Message representing a BT Unchoke message
	 */
	public static final Message unchoke = new Message(1,(byte)1);
	
	/**
	 * The Interested Message representing a BT interested message
	 */
	public static final Message interested = new Message(1,(byte)2);
	
	/**
	 * The Uninterested Message representing a BT uninterested message
	 */
	public static final Message uninterested = new Message(1,(byte)3);
	
	/**
	 * The List of possible message BT responses
	 */
	public static final String[] responses = {"choke", "unchoke", "interested", 
											  "uninterested", "have", "bitfield", 
											  "request", "pieces", "cancel"};
	
	//local fields
	private final int length;
	private final byte messageID;
	private byte[] payload;
	
	/**
	 * Constructor for Message class.
	 * @param length = length of the payload + 1 byte for the class id
	 * @param id = class ID of the message
	 */ 
	public Message(final int length, final byte id) {
		this.length = length;
		this.messageID = id;
		this.payload = null;
	}
	
	/**
	 * Setting the Message Payload (everything after the length and class ID in the BT message)
	 * @param payload The Message Payload
	 */
	public void setPayload(final byte[] payload) {
		this.payload = payload;
	}
	
	/**
	 * @return The Peer Message Length (based on how BT Messages define length)
	 */
	public int getLength() {
		return this.length;
	}
	
	/**
	 * @return The Peer Message Payload 
	 * (everything after the length and class ID in the BT message)
	 */
	public byte[] getPayload() {
		return this.payload;
	}
	
	/**
	 * @return The Peer message as a byte array (in the format it would be sent across a network)
	 */
	public byte[] getBTMessage(){
		//Starting minimum length of a BT message
		int messageLength = 4;
		//if this is a keepalive, just return the message immediately
		if (this.length == 0) {
			ByteBuffer bt = ByteBuffer.allocate(messageLength);
			bt.putInt(this.length);
			return bt.array();
		}
		//if choke/unchoke/interested/uninterested messages, also return immediately
		else if(this.length == 1){
			ByteBuffer bt = ByteBuffer.allocate(5);
			bt.putInt(this.length);
			bt.put(this.messageID);
			return bt.array();
		}
		//Otherwise, add space for the length and message id and payload.
		ByteBuffer bt = ByteBuffer.allocate(messageLength+this.length);
		bt.putInt(this.length);
		bt.put(this.messageID);
		bt.put(this.payload);
		return bt.array();
	}
	
	/**
	 * @return The Message ID.
	 */
	public byte getMessageID() {
		return this.messageID;
	}
	
	/**
	 * Get a Message String based on the Message ID
	 * @param x The Message ID
	 * @return The Message String
	 */
	public static String getMessageID(final byte x){
		if((int)x >= responses.length){
			return "NOT A VALID MESSAGE";
		}
		return responses[(int)x];
	}
	
	/**
	 * Generates the Have Message Payload and sets it to the local payload field
	 * @param piece The piece of the total file.
	 */
	public void have(final int piece){
		ByteBuffer responseBuff = ByteBuffer.allocate(4);
		responseBuff.putInt(piece);
		this.payload = responseBuff.array();
	}
	
	/**
	 * Generates a Request Message Payload and sets it to the local payload field
	 * @param index The index of the piece
	 * @param begin The offset of the data in integer format
	 * @param length The size of the data in integer format
	 */
	public void request(final int index, final int begin, final int length){
		ByteBuffer responseBuff = ByteBuffer.allocate(12);
		responseBuff.putInt(index);
		responseBuff.putInt(begin);
		responseBuff.putInt(length);
		this.payload = responseBuff.array();
	}
	
	/**
	 * Generates a Cancel Message Payload and sets it to the local payload field
	 * @param index The index of the piece
	 * @param begin The offset of the data in integer format
	 * @param length The size of the data in integer format
	 */
	public void cancel(final int index, final int begin, final int length){
		ByteBuffer responseBuff = ByteBuffer.allocate(12);
		responseBuff.putInt(index);
		responseBuff.putInt(begin);
		responseBuff.putInt(length);
		this.payload = responseBuff.array();
	}
	
	/**
	 * Generates a Piece Message and sets it to the local payload field
	 * @param x The number of bytes of the block.
	 * @param index The index of the piece of file.
	 * @param begin The offset of the piece.
	 * @param block The Data itself.
	 */
	public void piece(final int index, final int begin, final byte[] block){
		ByteBuffer responseBuff = ByteBuffer.allocate(12);
		responseBuff.putInt(index);
		responseBuff.putInt(begin);
		responseBuff.put(block);
		this.payload = responseBuff.array();
	}
	
	/**
	 * Generates a Bitfield Message and sets it to the local payload field
	 * @param bitfield - The bitfield that is a bit array of the pieces
	 */
	public void bitfield(final byte[] bitfield) {
		ByteBuffer responseBuff = ByteBuffer.allocate(bitfield.length);
		responseBuff.put(bitfield);
		this.payload = responseBuff.array();
	}
	
	/**
	 * Generates a handshake message payload for handshaking with a Bittorrent peer.
	 * @param SHA1 : The sha-1 of the
	 * @param peerID : the byte[] containing the LOCAL peer's peerID
	 * @return byte[] containing a handshake message
	 */
	public static byte[] handshakeMessage(final byte[] SHA1, final byte[] peerID) {
		//Set the first bits equal to "BitTorrent protocol" as specified by BT protocol
		byte[] handshake = new byte[68];
        handshake[0] = 19;
        handshake[1] = 'B';
        handshake[2] = 'i';
        handshake[3] = 't';
        handshake[4] = 'T';
        handshake[5] = 'o';
        handshake[6] = 'r'; 
		handshake[7] = 'r';
        handshake[8] = 'e';
        handshake[9] = 'n'; 
		handshake[10] = 't';
        handshake[11] = ' ';
        handshake[12] = 'p';
        handshake[13] = 'r';
        handshake[14] = 'o';
        handshake[15] = 't';
        handshake[16] = 'o';
        handshake[17] = 'c';
        handshake[18] = 'o';
        handshake[19] = 'l';    
	
	//Set the next 8 bytes as '0' byte paddings
        for(int i = 0; i < 8; i++){
        	handshake[19 + i + 1] = 0;
        }
        //Set the next bytes equal to the SHA-1 from the torrent file
        for(int i = 0; i < 20; i++){
        	handshake[28 + i] = SHA1[i];
        }
        //Set the next bytes equal to the local PeerID
        for(int i = 0; i < peerID.length; i++){
        	handshake[48 + i] = peerID[i];
        }
		return handshake;
	}
	
}
