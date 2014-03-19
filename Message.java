import java.nio.ByteBuffer;
import java.util.Arrays;

//TODO Add Cancel Function

public class Message {

	/**
	 * The KeepAlive Message
	 */
	public static final byte[] keepAlive = {0, 0, 0, 0};
	
	/**
	 * The Choke Message
	 */
	public static final byte[] choke = {0, 0, 0, 1, 0}; 
	
	/**
	 * The Unchoke Message
	 */
	public static final byte[] unchoke = {0, 0, 0, 1, 1};
	
	/**
	 * The Interested Message
	 */
	public static final byte[] interested = {0, 0, 0, 1, 2};
	
	/**
	 * The Uninterested Message
	 */
	public static final byte[] uninterested = {0, 0, 0, 1, 3};
	
	/**
	 * The List of Responses from the peer.
	 */
	public static final String[] responses = {"choke", "unchoke", "interested", 
											  "uninterested", "have", "bitfield", 
											  "request", "pieces", "cancel"};
	
	/**
	 * Generates the Have Message
	 * @param piece The piece of the total file.
	 * @return The Have Message
	 */
	public static byte[] have(int piece){
		byte[] have = new byte[9];
		byte[] pieceByte = ByteBuffer.allocate(4).putInt(piece).array();
		
		/* Length Prefix */
		have[0] = 0;
		have[1] = 0;
		have[2] = 0;
		have[3] = 5;
		
		/* Message ID */
		have[4] = 4;
		
		/* Read in the pieceByte */
		for(int i = 5; i < 9; i++){
			have[i] = pieceByte[i-5];
		}
		
		return have;
	}
	
	/**
	 * Generates a Request Message
	 * @param index The index of the piece
	 * @param begin The offset of the data in integers
	 * @param length The size of the data in integers
	 * @return The Request Message 
	 */
	public static byte[] request(int index, int begin, int length){
		byte[] request = new byte[17];
		
		ByteBuffer requestBuff = ByteBuffer.allocate(17);
		requestBuff.putInt(13);
		byte b = 6;
		requestBuff.put(b);
		requestBuff.putInt(index);
		requestBuff.putInt(begin);
		requestBuff.putInt(length);
		
		request = requestBuff.array();
		System.out.println(Arrays.toString(request));
		
		return request;
	}
	
	/**
	 * Generates a Piece Message
	 * @param x The number of bytes of the block.
	 * @param index The index of the piece of file.
	 * @param begin The offset of the piece.
	 * @param block The Data itself.
	 * @return
	 */
	public static byte[] piece(int x, int index, int begin, int block){
		byte[] piece = new byte[17];
		byte[] prefixByte = ByteBuffer.allocate(4).putInt(9 + x).array();
		byte[] indexByte = ByteBuffer.allocate(4).putInt(index).array();
		byte[] beginByte = ByteBuffer.allocate(4).putInt(begin).array();
		byte[] lengthByte = ByteBuffer.allocate(4).putInt(block).array();
		
		int a = 0;
		int b = 0;
		int c = 0;
		
		/* Length Prefix */
		for(int i = 0; i < 4; i++){
			piece[i] = prefixByte[i];
		}
		/* Message ID */
		piece[4] = 7;
		
		for(int i = 5; i < 17; i++){
			if(i < 9){
				piece[i] = indexByte[a]; 
				a++;
			}else if(i < 13){
				piece[i] = beginByte[b];
				b++;
			}else{
				piece[i] = lengthByte[c];
				c++;
			}
		}
		return piece;
	}
	
	/**
	 * Return the reponses from the peer.
	 * @param x The messageID.
	 * @return The responses message
	 */
	public static String getMessageID(byte x){
		int y = (int)x;
		return responses[y];
	}
	
	/*
	 * Function that will return a byte[] containing a handshake.
	 */
	public static byte[] handshakeMessage(byte[] SHA1, byte[] peerID){
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
		
        for(int i = 0; i < 8; i++){
        	handshake[19 + i + 1] = 0;
        }
        //28
        for(int i = 0; i < 20; i++){
        	handshake[28 + i] = SHA1[i];
        }
        //48
        for(int i = 0; i < peerID.length; i++){
        	handshake[48 + i] = peerID[i];
        }
		return handshake;
	}
	
}
