/**
 * @author Paul Tai
 * @author Alex Zhang
 * @author Anthony Wong
 */
public class Piece {
	//private fields
	private int pieceNumber;
	private int bytesWritten;
	//Used a byte[] instead of a bytebuffer
	private byte[] dataBuffer;
	
	
	/**
	 * Constructor for the packet class
	 * @param pieceNumber Zero-based index of the piece
	 * @param pieceSize size of the entire piece
	 * @param numBlocks Number of blocks of the piece 
	 * 			(might not be needed, would be for error-checking purposes)
	 */
	public Piece(int pieceNumber, int pieceSize, int numBlocks){
		this.pieceNumber = pieceNumber;
		this.bytesWritten = 0;
		this.dataBuffer = new byte[pieceSize];
	}//Piece Constructor
	
	/**
	 * writeToBuffer:
	 * Writes the given byte[] to the corresponding place in the piece array
	 * @param byteOffset = the offset, 0 based, of where the first byte should go in the piece
	 * @param length = the length of the array 
	 * @param dataToWrite = the data that should be written to the buffer
	 */
	public void writeToBuffer(int byteOffset, byte[] dataToWrite) {
		
		//Error check
		if(byteOffset+dataToWrite.length > this.dataBuffer.length) {
			System.out.println("ERROR:" + this + " writeToBuffer illegal parameters!");
			return;
		}
		
		//System method for copying an array into another array
		System.arraycopy(dataToWrite,0,this.dataBuffer,byteOffset, dataToWrite.length);
		//Increment the amount of bytes written to the data buffer
		this.bytesWritten+=dataToWrite.length;
		
	}//writeToBuffer
	
	/**
	 * @return Checks if the buffer has been filled with valid data
	 */
	public boolean isFull(){
		return (this.dataBuffer.length==this.bytesWritten);
	}
	
	/**
	 * @return The piece index 
	 */
	public int getPieceIndex(){
		return this.pieceNumber;
	}
	
	/**
	 * @return The Data Buffer
	 */
	public byte[] getData() {
		return this.dataBuffer;
	}
	
	@Override
	public String toString(){
		return "Piece Number: " + this.pieceNumber + " Bytes written: " + this.bytesWritten;
	}
}//Piece.java
