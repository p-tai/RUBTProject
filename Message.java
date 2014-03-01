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
		byte[] request = {0, 0, 0, 13, 6, (byte) index, (byte) begin, (byte) length};
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
		byte[] piece = {0 , 0, 0, (byte) (9 + x) ,7, (byte) index, (byte) begin, (byte) block};
		return piece;
	}
	
	public static byte[] encrpt(){
		return null;
	}

	/*
	 * Alex's only contribution
	 * ;__;
	 * i'm so sad rn
	 */
	public static String isAvailable(byte poop){
		if(poop == 0)
			return "keepAlive";
		else if(poop == 1)
			return "choke";
		else if(poop == 2)
			return "unchoke";
		else if(poop == 3)
			return "interested";
		else if(poop == 4)
			return "uninterested";
		else
			return null;
	}//endo f isavailable

	
}
