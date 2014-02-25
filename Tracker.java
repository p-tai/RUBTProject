import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.exceptions.*;
import edu.rutgers.cs.cs352.bt.*;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Random;
import java.util.HashMap;

public class Tracker{

	private URL url;
        private byte[] hash;
        private int bytesLeft;
        private int bytesDownloaded;
        private int bytesUploaded;
        private String peer_id;
        

	/**
	 * takes a TorrentInfo
	 * and makes a tracker
	 * and gets the IP addresses ands tuff
	 */
	public Tracker(TorrentInfo torrentFile){
		this.url = torrentFile.announce_url;
                this.hash = torrentFile.info_hash.array();
                this.bytesLeft = torrentFile.file_length;
                this.bytesDownloaded = 0;
                this.bytesUploaded = 0;
                this.peer_id = Tracker.genPeerID();
	}//end of Tracker constructor :3

        
        final private static char[] intArray = "0123456789".toCharArray();
        
        private static String genPeerID() {
                String peerID = "AAA";
                Random randGen = new Random();
                for(int i = 0; i<17;i++) {
                        peerID = peerID + intArray[randGen.nextInt(9)];
                        
                }
                return peerID;
        }
        
	/**
	 * creates connection to tracker
	 * takes ipaddress and port <-- taken from the torrentfile obj
	 * -- also takes a tracker!!!
	 * send HTTP GET --> tracker thru ^^ 
	 */
	public void create(){
                
		URL url = this.url;
                byte[] hash = this.hash;
                
                String query = "";
                
		URLConnection connection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;

		try{    
                        query = URLify(query,"announce?info_hash", hash);
                        query = URLify(query,"&peer_id",this.peer_id);
                        query = URLify(query,"&uploaded", Integer.toString(this.bytesUploaded));
                        query = URLify(query,"&downloaded", Integer.toString(this.bytesDownloaded));
                        query = URLify(query,"&left", Integer.toString(this.bytesLeft));
                        
                        url = new URL(url, query);
                        
                        System.out.println(url);
                        
			httpConnection = (HttpURLConnection)url.openConnection();
			System.out.println("1");
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			System.out.println("2");
			System.out.println("RESPONSE: " + responseCode);
			getStream = httpConnection.getInputStream();
                        dataStream = new DataInputStream(getStream);
                        
			System.out.println("3");

			//getStream = connection.getInputStream();

			int byteAvailLen = getStream.available();
			//System.out.println("CONNECTION: " + connection.toString());
			System.out.println("GETSTREAM: " + getStream.toString());
			System.out.println("bytes: " + byteAvailLen);
			getStreamBytes = new byte[byteAvailLen];
			dataStream.readFully(getStreamBytes);
                        
                        //HashMap<String,String> key = new HashMap<String,String>();
                        Object peers = Bencoder2.decode(getStreamBytes);
                        ToolKit.print(peers);
                        
                        getStream.close();
                        dataStream.close();
                        

		}//end of try
		catch(IOException e){
			System.out.println("ERRORRR" + e.getMessage());
		} catch(BencodingException e) {
                	System.out.println("ERRORRR" + e.getMessage());
                }
                //endo f catch
		
	}//end create

	/**
	 * args: tracker <-- invisible for now
	 * return a list of peers 
	 */
	private void captureResponse(){


	}//end of captureresponse

	public static void main(){


	}//end of main
        
        
        /**
         * Helper function - will convert a query and append it to the current URL
         */
        private String URLify(String base, String queryID, String query) {
                
                if(base==null) {
                        base = "";
                }
                
                try{
                        //This messes up for char values over 127 in base 10. Need to fix.
                        query = URLEncoder.encode(query, "UTF-8");
                        return (base+queryID+"="+query);
                } catch (UnsupportedEncodingException e) {
                        System.out.println("URL formation error:" + e.getMessage());
                }
                return null;
        }


        final private static char[] HEXCHARS = "0123456789ABCDEF".toCharArray();
        //SHA-1 is 155187125F2CE9E45F1D09729D75C35F2E83DBF3
        
        private String URLify(String base, String queryID, byte[] query) {
                if(base==null) {
                        base = "";
                }
                String reply = base+queryID+"=";
                for(int i = 0; i<query.length; i++) {
                        if(query[i] < 0) { //if the byte data has the most significant byte set (e.g. it is negative)
                                reply = reply+"%";
                                reply = reply + HEXCHARS[(query[i]&0xF0)>>>4]+HEXCHARS[query[i]&0x0F];
                        }else{
                                try{
                                        reply = reply + URLEncoder.encode(new String(new byte[] {query[i]}),"UTF-8");
                                 }catch(UnsupportedEncodingException e){
                                         System.out.println("URL formation error:" + e.getMessage());
                                 }
                        }
                }
                return reply;
        }

}//end of connection class
