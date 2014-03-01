alexandras armadillos
====

where alex** tries to figure out what the fuck is going on here, yep ^__^

## Files we created

### RUBTClient.java
#### `private static TorrentInfo parseTorrentInfo(String filename)`
	* **Gist**: read file `filename` into a `TorrentInfo` object
	* **Note**: need to read into byte[] then readFully(byte[]) 
		* because constructor for `TorrentInfo` needs a friggin readfully thingie
	* Also catches errors yay :3 
* `private static boolean writeFile(String outputFileName)`
	* returns `TRUE` if wrote, and yeah
	* **Gist**: writes to a file indicated by outputfilename 
	* At the moment: writes "testing" 
		* supposed to put downloaded byte array there :3 
	* **Alex**: shouldn''t we accept a byte array as an argument too? ^__^
#### `public static void connect(bye[] handshake, String peerID, String peerIP, int peerPort) throws a fuckton of exceptions`
	* **Gist**: connects to given information (right now hardcoded lawlz)
	* Basically where all our shit happens
	* Opens a socket to ip/port
	* Write handshake to output stream
	* SHA-1 comparisons
	* Send interested message in ByteBuffer
	* Ben-code dat shit 
	* (not done yet from here on out)
	* Send an unchoke message
	* Send a request message for one of the pieces
	* Close sockets and shit
#### `public static byte[] handshakeMessage(int length, String protocol, int fixedHeaders, byte[] SHA1, String peerId)`
	* **Gist**: .... fills out a bytearray?? called the handshake??
	* Including: SHA-1 and peerID...? 
#### `public static String readByteArray(byte[] target)`
	* **Gist**: converts the byte[] into a string format
#### `public static void main(String args[]) throws a fuckton of exceptions`
	* **Gist**: accepts 2 arguments (.torrent and .jpg) 
	* Makes a torrentfile using the filepath given (.torrent)
	* Makes new Client object using torrentfile and picture
	* Makes new Tracker object using torrentFile (and creates it)
		* Error checks
	* Handshakes and stores to `byte[] message`
	* gets tracker''s hostIP
	* gets tracker''s hostID
	* gets tracker''s port
	* makes the connection with the ^ 


### Client.java

### Message.java

### Tracker.java
