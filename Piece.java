import java.util.Arrays;

public class Piece {
	//private fields
	private int pieceNumber;
	private int bytesWritten;
	//Used a byte[] instead of a bytebuffer
	private byte[] dataBuffer; 
	
	
	/**
	 * Constructor for the packet class
	 * @param pieceNumber: Zero-based index of the piece
	 * @param pieceSize: size of the entire piece
	 * @param numBlocks: Number of blocks of the piece 
	 * 			(might not be needed, would be for error-checking purposes)
	 */
	public Piece(int pieceNumber, int pieceSize, int numBlocks){
		this.pieceNumber = pieceNumber;
		this.bytesWritten = 0;
		this.dataBuffer = new byte[pieceSize];
	}
	
	/**
	 * writeToBuffer:
	 * Writes the given byte[] to the corresponding place in the piece array
	 * @param
	 */
	public void writeToBuffer(int byteOffset, int length, byte[] dataToWrite) {
		
		//Error check
		if(byteOffset+length > dataBuffer.length) {
			System.out.println("ERROR:" + this + " writeToBuffer illegal parameters!");
			return;
		}
		
		//System method for copying an array into another array
		System.arraycopy(dataBuffer,byteOffset,dataToWrite,0, length);
		
		bytesWritten+=length;
		
	}
	
	/**
	 * Checks if the buffer has been filled with valid data
	 */
	public boolean isFull(){
		return (this.dataBuffer.length==this.bytesWritten);
	}
	
	/**
	 * Getter for the piece index
	 */
	public int getPieceIndex(){
		return this.pieceNumber;
	}
	
	/**
	 * Getter for the data buffer
	 */
	public byte[] getData() {
		return dataBuffer;
	}
	
	/**
	 * ToString...debug purposes.
	 */
	public String toString(){
		return "Piece Number: " + this.pieceNumber + " Bytes written: " + this.bytesWritten;
	}
}
