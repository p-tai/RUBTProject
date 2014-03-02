import java.io.*;

import edu.rutgers.cs.cs352.bt.TorrentInfo;
import edu.rutgers.cs.cs352.bt.exceptions.BencodingException;
import edu.rutgers.cs.cs352.bt.util.Bencoder2;
import edu.rutgers.cs.cs352.bt.util.ToolKit;

import java.net.*;
import java.lang.*;

public class RUBTClient {
	public static void main(String[] args){
    	String filePath = "project1.torrent";
        String picture = "picture.jpg";
        
        Client client = new Client(filePath, picture);
        client.HTTPGET();
        client.printPeerList();
        //TODO FIX THIS!
        client.connect("RUBT11UCWQNPODEKNJZK");
	}
}
