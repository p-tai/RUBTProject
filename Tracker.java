import edu.rutgers.cs.cs352.bt.util.*;
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
		System.out.println("HASH: " + torrentFile.info_hash);
	}//end of Tracker constructor :3

        
        private static String genPeerID() {
                Random randGen = new Random();
                int suffix = randGen.nextInt(9)*100+randGen.nextInt(9)*10+randGen.nextInt(9);
                return new String("AAA"+Integer.toString(suffix));
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
                String query = null;

                
		URLConnection connection = null;
		InputStream getStream = null;
		HttpURLConnection httpConnection = null;
		DataInputStream dataStream = null;
		byte[] getStreamBytes;

		try{    
                        query = URLify(query,"announce?info_hash",new String(hash,"UTF-8"));
                        query = URLify(query,"&peer_id",this.peer_id);
                        query = URLify(query,"&uploaded", Integer.toString(this.bytesUploaded));
                        query = URLify(query,"&downloaded", Integer.toString(this.bytesDownloaded));
                        query = URLify(query,"&left", Integer.toString(this.bytesLeft));
                        System.out.println(query);
                        url = new URL(url, query);
                        
			httpConnection = (HttpURLConnection)url.openConnection();
			System.out.println("1");
			httpConnection.setRequestMethod("GET");
			int responseCode = httpConnection.getResponseCode();
			System.out.println("RESPONSE: " + responseCode);
			System.out.println("2");
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
                        
                        
                        
                        getStream.close();
                        dataStream.close();
                        

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
        
        
        /**
         * Helper function - will convert a query and append it to the current URL
         */
        private String URLify(String base, String queryID, String query) {
                if(base==null) {
                        base = "";
                }
                try{
                        query = URLEncoder.encode(query, "UTF-8");
                        return (base+queryID+"?"+query);
                } catch (UnsupportedEncodingException e) {
                        System.out.println("URL formation error:" + e.getMessage());
                }
                return null;
        }

}//end of connection class
