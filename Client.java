import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;

public class Client {
	
	private TorrentInfo torrentInfo;
    private String fileName;
    private RandomAccessFile dataFile;
    
    
    /**
	 * takes a TorrentInfo and makes a tracker
	 */
    
	public Client(TorrentInfo torrentFile, String fileName) {
		this.torrentInfo = torrentFile;
        this.fileName = fileName;
        createFile();
	}
    
    /*
     * Getter for data file
     */
    public RandomAccessFile getData() {
        return this.dataFile;
    }
    
    /*
     * Updater for data file
     * Takes input of a data piece and a data offset
     * Returns false if the SHA-1 does not match the piece
     * Returns true and updates the internal data array if the SHA-1 matches
     */
     
    public static boolean checkData(byte[] dataPiece, int dataOffset) {
		MessageDigest hasher = MessageDigest.getInstance("SHA");	
		
		if(dataOffset > this.torrentInfo.piece_hashes.length) {
			//illegal dataOffset value
			System.out.println("illegal dataOffset");
			return false;
		}
		
		if(dataPiece.length > this.torrentInfo.piece_length) {
			System.out.println("illegal piece length");
			//ilegal piece length
			return false;
		}
		
		byte[] SHA1 = this.torrentInfo.piece_hashes[dataOffset];
		byte[] checkSHA1 = hasher.digest(dataPiece);
		
		//check SHA-1
		for(int i = 0; i < sha1.length; i++) {
			if(SHA1[[i] != checkSHA1[i]){
				System.out.println("fail in loop at index " + i);
				return false;
			}
		}
		
        return true;
    }
    
    /*
     * Getter for torrentInfo
     */
    public TorrentInfo getTorrentInfo() {
        return this.torrentInfo;
    }
    
    // Creates the randomAccessFile for file that will be downloaded
    private boolean createFile(){
        try {
            this.dataFile = new RandomAccessFile(this.fileName,"rw");
            return true;
        } catch( FileNotFoundException e) {
            try { //If the file does not exist, create it and call createFile again
                FileWriter fileStream = new FileWriter(this.fileName);
                createFile();
                return true;
            } catch (IOException IOe) {
                System.err.println("Error: " + IOe.getMessage());
                return false;
            }
        }
    }   
}
