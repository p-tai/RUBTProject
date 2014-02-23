import java.io.*;
import java.net.*;
import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;

public class Client {
	
	private TorrentInfo torrentInfo;
    private String fileName;
    private RandomAccessFile dataFile;
    
    
    /**
	 * takes a TorrentInfo
	 * and makes a tracker
	 * and gets the IP addresses ands tuff
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
     
    public boolean setData(byte[] dataPiece, int dataOffset) {
        //check SHA-1
        return false;
    }
    
    private static void performHandshake(TorrentInfo torrentFile) {
        byte[] handshake = new byte[65];
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
        
        for(int i = 1; i < 9; i++){
        	handshake[19 + i] = 0;
        }
        
        byte[] infoHash = torrentFile.info_hash.array();
        for(int i = 0; i < 20; i++){
        	handshake[28+i] = infoHash[i];
        }
        
        
         //System.arraycopy(P_STRING,0,handshake,1,P_STRING.length);
        //array copy the infohash
        //array copy the peerID
        //check the handshake
        //check protocolstring
        //check infohash
        //check peerID against tracker peerID
        return;
    }
    
    /*
     * Getter for torrentInfo;
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