import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import java.net.URL;
import java.net.URLConnection;
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

	}//end of Tracker constructor :3


	/**
	 * creates connection to tracker
	 * takes ipaddress and port <-- taken from the torrentfile obj
	 * -- also takes a tracker!!!
	 * send HTTP GET --> tracker thru ^^ 
	 */
	public void create(){
		URL url = this.url;
		URLConnection connection = null;
		InputStream getStream = null;
		DataInputStream dataStream = new DataInputStream(getStream);
		try{
			connection = url.openConnection();
			System.out.println(connection.toString());
			getStream = connection.getInputStream();
			System.out.println(getStream.toString());

			//dataStream = dataStream.
		}//end of try
		catch(IOException e){
			System.out.println(e.getMessage());
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
