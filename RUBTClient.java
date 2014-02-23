import java.io.*;
import java.net.*;
import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;


public class RUBTClient {
        
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
            fileStream.close();
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
        
        try {
            String decoded = new String(torrentFile.info_hash.array(), "UTF-8");
            System.out.println(decoded);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error: Could not read torrent info.");
            return;
        }
        
        //Create client instance which will hold file information
        Client torrentClient = null;
        torrentClient = new Client(torrentFile, args[1]);

		Tracker tracker = new Tracker(torrentFile);
		tracker.create();
        
        //Checks if the torrentfile was correctly made
        if(torrentFile == null) {
            System.err.println("Error: Could not read torrent info.");
            return;
        }
        //System.out.println(torrentFile);
        
        //Exit gracefully
        return;
    }
    
}
