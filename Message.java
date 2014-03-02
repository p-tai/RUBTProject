import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Message {

	/**
	 * When doing for special characters do:
	 * 10 = A, 11 = B, 12 = C
	 * 13 = D, 14 = E, 15 = F
	 */
	
	/**
	 * 
	 */
	public static final byte[] keepAlive = {0, 0, 0, 0};
	
	// NOTE TO ALEX(SELF ^__^): last element of each byte[] is the id!

	/**
	 * 
	 */
	public static final byte[] choke = {0, 0, 0, 1, 0}; 
	
	/**
	 * 
	 */
	public static final byte[] unchoke = {0, 0, 0, 1, 1};
	
	/**
	 * 
	 */
	public static final byte[] interested = {0, 0, 0, 1, 2};
	/**
	 * ALEX'S AWESOME CONTRIBUTION ^__^
	 * ANTHONY IS A RETARD
	 * HEHEHEHEHEH
	 * JK
	 */
	public static final String[] responses = {"choke", "unchoke", "interested", "uninterested", "have", "bitfield", "request", "pisece"};
	
	/**
	 * 
	 */
	public static final byte[] uninterested = {0, 0, 0, 1, 3};

	/**
	 * The payload is a zero-based index of the piece that
	 * has just been downloaded and verified
	 * @param piece int
	 * @return byte[] have
	 */
	public static byte[] have(int piece){
		byte[] have = {0, 0, 0, 5, 4, (byte) piece};
		return have;
	}
	
	/**
	 * 
	 * @param index
	 * @param begin
	 * @param length
	 * @return
	 */
	public static byte[] request(int index, int begin, int length){
		//ByteBuffer request = ByteBuffer.wrap(new byte[] {0, 0, 0, 13, 6, (byte)index, (byte)begin, length});
		byte[] request = new byte[17];
		byte[] indexByte = ByteBuffer.allocate(4).putInt(index).array();
		byte[] beginByte = ByteBuffer.allocate(4).putInt(begin).array();
		byte[] lengthByte = ByteBuffer.allocate(4).putInt(length).array();
		
		int a = 0;
		int b = 0;
		int c = 0;
		
		/* Length Prefix */
		request[0] = 0;
		request[1] = 0;
		request[2] = 0;
		request[3] = 13;
		/* Message ID */
		request[4] = 6;
		
		for(int i = 5; i < 17; i++){
			if(i < 9){
				request[i] = indexByte[a]; 
				a++;
			}else if(i < 13){
				request[i] = beginByte[b];
				b++;
			}else{
				request[i] = lengthByte[c];
				c++;
			}
		}
		
		//byte[] request = {0, 0, 0, 13, 6, (byte) index, (byte) begin, (byte) length};
		return request;
	}
	
	/**
	 * 
	 * @param x
	 * @param index
	 * @param begin
	 * @param block
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
		
		//byte[] request = {0, 0, 0, 13, 6, (byte) index, (byte) begin, (byte) length};
		return piece;
	}
	
	public static byte[] encrpt(){
		return null;
	}

}//end of class don't write past here ok

