import edu.rutgers.cs.cs352.bt.util.*;
import edu.rutgers.cs.cs352.bt.*;
import java.net.URL;
import java.net.URLConnection;

public class Tracker{

	private URL url;
	private URLConnection 
	

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
	private void create(String IPAddr, int port, File f){
		URLConnection connection;
		
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
