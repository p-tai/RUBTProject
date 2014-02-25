import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

public class Tracker{

	private URL url;
        private byte[] hash;

	/**
	 * takes a TorrentInfo
	 * and makes a tracker
	 * and gets the IP addresses ands tuff
	 */
	public Tracker(TorrentInfo torrentFile){
		this.url = torrentFile.announce_url;
                this.hash = torrentFile.info_hash.array();
		System.out.println("HASH: " + torrentFile.info_hash);
	}//end of Tracker constructor :3

        
	/**
	 * creates connection to tracker
	 * takes ipaddress and port <-- taken from the torrentfile obj
	 * -- also takes a tracker!!!
	 * send HTTP GET --> tracker thru ^^ 
	 */
	public void create(){
		URL url = this.url;
                
                URLify(this.hash);
                
		System.out.println("URL: " + url.getPath());
		URLConnection connection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = new DataInputStream(getStream);
		byte[] getStreamBytes;

		try{
			httpConnection = (HttpURLConnection)url.openConnection();
			System.out.println("1");
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			System.out.println("RESPONSE: " + responseCode);
			System.out.println("2");
			getStream = httpConnection.getInputStream();

			System.out.println("3");

			//getStream = connection.getInputStream();

			int byteAvailLen = getStream.available();
			System.out.println("CONNECTION: " + connection.toString());
			System.out.println("GETSTREAM: " + getStream.toString());
			System.out.println("bytes: " + byteAvailLen);
//			getStreamBytes = new byte[byteAvailLen];
//			dataStream = dataStream.readFully(getStreamBytes);

		}//end of try
		catch(IOException e){
			System.out.println("ERRORRR" + e.getMessage());
		}//endo f catch
		
	}//end create

	/**
	 * args: tracker <-- invisible for now
	 * return a list of peers 
	 */
	private void captureResponse(){


	}//end of captureresponse

	public static void main(){


	}//end of main
        
        
        /*
         * Helper functin that translates byte data to a hex string
         * need to test
         */
        
        final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
        
        private static char[] convertToHex(byte raw) {
                char[] hexChars = new char[2];
                hexChars[0] = hexArray[(raw & 0xF0) >>> 4]; //Bitmask upper 4 bits and use as index
                hexChars[1] = hexArray[raw & 0x0F]; //bitmask lower 4 bits and use as index
                return hexChars;
        }        
        
        private static String URLify(byte[] data) {
                String query = "";
                for(int i = 0; i < data.length; i++) {
                        if(Character.isLetter((char)data[i]) || Character.isDigit((char)data[i])) {
                                query=query+(char)data[i];
                        } else {
                                query=query+"%";
                                query=query+convertToHex(data[i]);
                        }
                }
                System.out.printf("Query: %s", query);
                return query;
        }

}//end of connection class
