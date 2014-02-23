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

	/**
	 * takes a TorrentInfo
	 * and makes a tracker
	 * and gets the IP addresses ands tuff
	 */
	public Tracker(TorrentInfo torrentFile){
		this.url = torrentFile.announce_url;

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


	}//end of cpatureresponse

	public static void main(){


	}//end of main

}//end of connection class
