import java.io.*;
import java.net.*;
import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;


public class RUBTClient {
    
    private static void performHandshake() {
        byte[] handshake = new Byte[65];
        handshake[0] = 19;
        System.arraycopy(P_STRING,0,handshake,1,P_STRING.length);
        //array copy the infohash
        //array copy the peerID
        //check the handshake
        //check protocolstring
        //check infohash
        //check peerID against tracker peerID
        return;
    }
    
    private static boolean connect() {
    //check if the socket is "alive"
        if(this.connection != null) {
            if(this.connection.isConnected) {
                try{
                    this.connection = new Socket...
                    //do the echo socket stuff
                    //make a peer output stream
                    return true;
                }
            }
        }
        return false;
    }
    
    
    private static TorrentInfo parseTorrentInfo(String filename) {
        try {
            //Create input streams and file streams
            File torrentFile = new File(filename);
            FileInputStream torrentFileStream = new FileInputStream(torrentFile);
            DataInputStream torrentFileReader = new DataInputStream(torrentFileStream);
            
            //Read the file into torrentFileBytes
            byte[] torrentFileBytes = new byte[((int)torrentFile.length())];
            torrentFileReader.readFully(torrentFileBytes);
            
            //Close input streams and file streams
            torrentFileReader.close();
            torrentFileStream.close();
            //torrentFile.close();
            
            return new TorrentInfo(torrentFileBytes);
            
        } catch(FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        } catch (IOException e){
            System.err.println("Error: " + e.getMessage());
            return null;
        } catch (BencodingException e) {
            System.err.println("Error: " + e.getMessage());
            return null;
        }
        
    }
    
    private static boolean writeFile(String outputFileName) {
        try {
            FileWriter fileStream = new FileWriter(outputFileName);
            BufferedWriter bFileStream = new BufferedWriter(fileStream);
            bFileStream.write("testing"); //Put the downloaded byte array here
            bFileStream.flush();
            bFileStream.close();
            return true;
        } catch (IOException e){
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }
    
    public static void main(String args[]) {
    
        //Check for correct number parameters
        if(args == null || args.length != 2) {
            System.err.println("Error: Incorrect number of paramaters");
            return;
        }
        
        //Attempt to open the .torrent file and create a buffered reader from the file stream
        TorrentInfo torrentFile = parseTorrentInfo(args[0]);

		Tracker tracker = new Tracker(torrentFile);
		tracker.create();
        
        
        
        //Checks if the torrentfile was correctly made
        if(torrentFile == null) {
            System.err.println("Error: Could not read torrent info.");
            return;
        }
        //System.out.println(torrentFile);
        
        //
        
        //Write output to file
        if(!writeFile(args[1])) {
            System.err.println("Error: Could not write to file.");
        }
        
        //Exit gracefully
        return;
    }
    
}
