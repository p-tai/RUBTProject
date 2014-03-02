
/*
 * RUBTClient.java
 * Group 05:
 * Paul Tai
 * Anthony Wong
 * Alex Zhang
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;


public class RUBTClient {
        
	private static TorrentInfo torrent;
	private static Tracker tracker;
	
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
    
    public static void main(String args[]) throws IOException, BencodingException, InterruptedException {
    
        //Check for correct number parameters
        /*   if(args == null || args.length != 2) {
            System.err.println("Error: Incorrect number of paramaters");
            return;
        } */
    	
    	String filePath = "project1.torrent";
        String picture = "picture.jpg";
        //Attempt to open the .torrent file and create a buffered reader from the file stream
        TorrentInfo torrentFile = parseTorrentInfo(filePath);
        torrent = torrentFile;
        //Create client instance which will hold file information
        Client torrentClient = null;
        torrentClient = new Client(torrentFile, picture);

		tracker = new Tracker(torrentFile);
		tracker.create();

        //Checks if the torrentfile was correctly made
        if(torrentFile == null) {
            System.err.println("Error: Could not read torrent info.");
            return;
        }
        
        byte[] message = handshakeMessage(19, "BitTorrent protocol", 8, tracker.getSHA1(), tracker.peerID());
        String ip = tracker.getHostIP();
        String id = tracker.getHostID();
        int port = tracker.getPort();
        connect(message, id, ip, port);
        //System.out.println(torrentFile);
        
        //Exit gracefully
        return;
    }
    
    //@SuppressWarnings("deprecation")
    public static void connect(byte[] handshake, String peerID, String peerIP, int peerPort) throws InterruptedException{
    	try{
    		//TODO REMOVE HARD CODE
    		String IP = "128.6.171.130";
    		int PORT = 30164;
    		/* We DON'T HAVE TO ENCODE BEFORE WE SEND THE MESSAGE */
    		Socket socket = new Socket(IP, PORT);
			// output stream (wtf man os??? operating system?)
    		DataOutputStream os = new DataOutputStream(socket.getOutputStream());
			// input stream (dat variable naming)
    		DataInputStream is = new DataInputStream(socket.getInputStream());
    		
    		if(socket != null && os != null && is != null){
    			os.write(handshake);
    			
    			byte[] temp = new byte[68];
    			byte[] varify = new byte[20]; // seriously? varify? verify?
    			is.readFully(temp);

    			for(int i = 0; i < 20; i++){
    				varify[i] = temp[28 + i];
    			}
    		
    			//TODO : DO NOT COMPARE SHA1 via Strings
    			if(readByteArray(varify).equals(readByteArray(tracker.getSHA1()))){
    				
    				os.flush();
					
					byte[] read_five = new byte[5];
					is.readFully(read_five);
					System.out.println("#####################" + readByteArray(read_five));

					//0. Getting bitchfields
					byte[]  bitfield1 = new byte[1];
					is.readFully(bitfield1);
					boolean[] bitfield = Client.getBitfield(bitfield1);

    				//1. Send an Interested Message
					os.write(Message.interested);
					
    				//ByteBuffer interestMessage = ByteBuffer.wrap(Message.interested);
    				//System.out.println(readByteArray(temp));
    				//2. Send an unchoke Message
					is.readFully(read_five);
					System.out.println("\t\t\t" + readByteArray(read_five));
    				
					System.out.println(readByteArray(Message.piece(1, 3, 2, torrent.piece_length)));
    				//3. Send a request Message for one of the pieces
    				
    			}else{
    				System.out.println("Oh noez, SHA-1 y u different gawhgawhgawhgawhgawh");
    			}
    		}
    		os.close();
    		is.close();
    		socket.close();
    	}catch(UnknownHostException e){
    		System.err.println("Don't know about the host" + peerIP);
    	}catch(IOException e) {
			System.err.println("IOException in peer connection method" + e.getMessage());
		}
		/*
		catch(BencodingException e) {
			System.err.println("BencodingException in peer connection method" + e.getMessage());
		}
		*/
    }
    
    public static byte[] handshakeMessage(int length, String protocol, int fixedHeaders, byte[] SHA1, String peerID){
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
        for(int i = 0; i < 20; i++){
        	handshake[48 + i] = (byte) peerID.charAt(i);
        }
        //ToolKit.print(handshake);
        //System.out.println(handshake.length);
    	return handshake;
    }
    
    public static String readByteArray(byte[] target){
    	String result = "";
		for(int i = 0; i < target.length; ++i){
			/*
			 * Return: 19, BitTorrent protocol
			 * 		   8, 0's
			 * 		   Info Hash
			 * 		   There PEER ID
			 */
			result = result + String.format("%02x, ", target[i]);
			//System.out.print(String.format("%02x, ",target[i]));
			if(i == 19 || i == 27 || i == 47){
				result = result + "\n";
			}
		}
		return result;
    }

}//end of class :3 


