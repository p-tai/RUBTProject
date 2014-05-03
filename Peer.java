import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Paul Tai
 * @author Alex Zhang
 * @author Anthony Wong
 */
public class Peer extends Thread {

	private final static int MAX_CONCURRENT_SENDS = 2;
	private final Client RUBT;
	private final byte[] clientID;
	private byte[] peerID;
	private String peerIDString;
	private final byte[] torrentSHA;
	private final String peerIP;
	private int peerPort;
	private boolean keepRunning;

	private int concurrentSends;
	private int concurrentRequests;

	private boolean[] peerBooleanBitField;

	private Socket peerConnection;
	private DataOutputStream outgoing;
	private DataInputStream incoming;
	private PeerWriter writer;
	private Piece pieceInProgress;

	/**
	 * Flags for local/remote choking/interested
	 */
	private boolean localChoking;
	private boolean localInterested;
	private boolean remoteChoking;
	private boolean remoteInterested;

	/**
	 * Timer code is sourced form Sakai on 3.29.14
	 * 
	 * @author Rob Moore
	 */
	private static final long KEEP_ALIVE_TIMEOUT = 120000;
	private Timer peerTimer = new Timer();
	private long lastMessageTime = System.currentTimeMillis();
	private long lastPeerTime = System.currentTimeMillis();
	private double downloadRate;
	private double uploadRate;
	private int recentBytesDownloaded;
	private Object DLCountLock = new Object();
	private int recentBytesUploaded;
	private Object ULCountLock = new Object();

	/**
	 * Peer's Constructor
	 * 
	 * @param RUBT
	 *            The Client Object
	 * @param localID
	 *            The Client's ID
	 * @param peerID
	 *            The Peer's ID
	 * @param peerIP
	 *            The Peer's IP
	 * @param peerPort
	 *            The Peer's Port
	 */
	public Peer(Client RUBT, byte[] peerID, String peerIP, int peerPort) {
		this.RUBT = RUBT;
		this.clientID = RUBT.getClientID();
		this.peerID = peerID;
		try {
			// FOR DEBUG PURPOSES ONLY (to make peer id human readable)
			this.peerIDString = new String(peerID, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.peerBooleanBitField = new boolean[RUBT.getNumPieces()];
		this.peerIP = peerIP;
		this.peerPort = peerPort;
		this.localChoking = true;
		this.localInterested = false;
		this.remoteChoking = true;
		this.remoteInterested = false;
		this.torrentSHA = this.RUBT.getHash();
		this.peerConnection = null;
		this.uploadRate = 0.0;
		this.downloadRate = 0.0;
		this.recentBytesDownloaded = 0;
		this.recentBytesUploaded = 0;
		this.concurrentSends = 0;
		this.concurrentRequests = 0;
		this.keepRunning = true;
	}

	/**
	 * Peer's Constructor
	 * 
	 * @param RUBT
	 *            The Client Object.
	 * @param socket
	 *            The connection between the Client and Peer.
	 */
	public Peer(Client RUBT, Socket socket) {
		this.RUBT = RUBT;
		this.clientID = RUBT.getClientID();
		this.peerID = new byte[20];
		this.peerBooleanBitField = new boolean[RUBT.getNumPieces()];
		this.peerIP = (socket.getInetAddress()).getHostAddress();
		this.peerPort = socket.getPort();
		this.localChoking = true;
		this.localInterested = false;
		this.remoteChoking = true;
		this.remoteInterested = false;
		this.torrentSHA = this.RUBT.getHash();
		this.peerConnection = socket;
		this.uploadRate = 0.0;
		this.downloadRate = 0.0;
		this.recentBytesDownloaded = 0;
		this.recentBytesUploaded = 0;
		this.concurrentSends = 0;
		this.concurrentRequests = 0;
		this.keepRunning = true;
	}

	/*********************************
	 * Setters
	 ********************************/

	/**
	 * The status of the Peer chocking the Client.
	 * 
	 * @param localChoking
	 *            true = The Peer is Chocking the Client. Otherwise, false.
	 */
	public void setLocalChoking(boolean localChoking) {
		this.localChoking = localChoking;
	}

	/**
	 * TODO
	 * 
	 * @param peerConnection
	 */
	public void setPeerConnection(Socket peerConnection) {
		this.peerConnection = peerConnection;
	}

	/**
	 * TODO
	 * 
	 * @param outgoing
	 */
	public void setOutgoing(DataOutputStream outgoing) {
		this.outgoing = outgoing;
	}

	/**
	 * TODO
	 * 
	 * @param incoming
	 */
	public void setIncoming(DataInputStream incoming) {
		this.incoming = incoming;
	}

	/**
	 * The status of the Client being interested of the Peer.
	 * 
	 * @param localInterested
	 *            true = The Client is the Peer. Otherwise, false.
	 */
	public void setLocalInterested(boolean localInterested) {
		this.localInterested = localInterested;
	}

	/**
	 * The status of the Peer Choking the Client.
	 * 
	 * @param remoteChoking
	 *            true = Peer is Choking the Client. Otherwise, false.
	 */
	public void setRemoteChoking(boolean remoteChoking) {
		this.remoteChoking = remoteChoking;
	}

	/**
	 * The status of the Peer being interested of the Client.
	 * 
	 * @param remoteInterested
	 *            true = Peer is interested the Client. Otherwise, false.
	 */
	public void setRemoteInterested(boolean remoteInterested) {
		this.remoteInterested = remoteInterested;
	}

	/**
	 * Set the Peer Boolean Bit Field.
	 * 
	 * @param peerBooleanBitField
	 *            The Peer Boolean Bit Field.
	 */
	public void setPeerBooleanBitField(boolean[] peerBooleanBitField) {
		this.peerBooleanBitField = peerBooleanBitField;
	}

	/**
	 * Writes a byte[] to the peer's internal buffer. Also checks if the buffer
	 * is full.
	 * 
	 * @param payload
	 *            The payload that will be written to buffer. Should come from a
	 *            peer message.
	 * @param pieceOffset
	 *            Offset to write into at the buffer - should come from a peer
	 *            message.
	 * @param blockOffset
	 *            TODO
	 * @return The entire contents of the buffer or null.
	 */
	public Piece writeToInternalBuffer(byte[] payload, int pieceOffset,
			int blockOffset) {
		if (this.pieceInProgress == null) {
			this.pieceInProgress = new Piece(pieceOffset,
					this.RUBT.getPieceLength(pieceOffset),
					this.RUBT.getNumBlocks(pieceOffset));
		}

		// Write the payload to the piece
		this.pieceInProgress.writeToBuffer(blockOffset, payload);
		synchronized (this.DLCountLock) {
			this.recentBytesDownloaded += payload.length;
		}
		return this.pieceInProgress;
	}

	/**
	 * TODO
	 */
	public void resetPiece() {
		if (this.pieceInProgress.isFull()) {
			this.pieceInProgress = null;
		}
	}

	/*********************************
	 * Getters
	 ********************************/

	/**
	 * @return The Peer's IP
	 */
	public String getPeerIP() {
		return this.peerIP;
	}

	/**
	 * @return The Current Download Rate
	 */
	public double getDownloadRate() {
		double retVal;
		synchronized (this.DLCountLock) {
			retVal = this.downloadRate;
		}
		return retVal;
	}

	/**
	 * @return The Current Upload Rate
	 */
	public double getUploadRate() {
		double retVal;
		synchronized (this.ULCountLock) {
			retVal = this.uploadRate;
		}
		return retVal;

	}

	/**
	 * @return The Peer's Port
	 */
	public int getPeerPort() {
		return this.peerPort;
	}

	/**
	 * @return The Peer's ID
	 */
	public byte[] getPeerID() {
		return this.peerID;
	}

	/**
	 * @return The Peer's ID as a String
	 */
	public String getPeerIDString() {
		return this.peerIDString;
	}

	/**
	 * @return The status of the Client choking the peer.
	 */
	public boolean isChokingLocal() {
		return this.localChoking;
	}

	/**
	 * @return The status of Peer interested of the Client.
	 */
	public boolean isInterestedLocal() {
		return this.localInterested;
	}

	/**
	 * @return The status of the Client being choking by Peer.
	 */
	public boolean amChoked() {
		return this.remoteChoking;
	}

	/**
	 * @return This peer's bitfield as a boolean array
	 */
	public boolean[] getBitfields() {
		return this.peerBooleanBitField;
	}

	/**
	 * @return The status of the Client interested of the Peer.
	 */
	public boolean amInterested() {
		return this.remoteInterested;
	}

	@Override
	public String toString() {
		return "(" + this.peerIP + ":" + this.peerPort + ")";
	}

	/**
	 * Opens a connection to the peer if it doesn't already exist. (Skips the
	 * socket creation if we accepted a connection from our server socket) Then
	 * sets up the input and output streams of the socket.
	 */
	public void initializePeerStreams() {
		System.out.println("Connecting to " + this);

		try {
			if (this.peerConnection == null) {
				this.peerConnection = new Socket(this.peerIP, this.peerPort);
			}

			System.out.println("Opening Output Stream to " + this);
			this.outgoing = new DataOutputStream(
					this.peerConnection.getOutputStream());

			System.out.println("Opening Input Stream to " + this);
			this.incoming = new DataInputStream(
					this.peerConnection.getInputStream());

			if (this.peerConnection == null || this.outgoing == null
					|| this.incoming == null) {
				System.err.println("Input/Output Stream Creation Failed");
			}

		} catch (UnknownHostException e) {
			System.err.println(this.peerID + " user not found.");
		} catch (IOException e) {
			System.err.println("Input/Output Stream Creation Failed");
		}

	}// end of initializePeerStreams()

	/**
	 * Send a Handshake Message to the Peer. Will also verify that the returning
	 * handshake is valid.
	 * 
	 * @param infoHash
	 *            containing the SHA1 of the entire file.
	 * @return true for success, otherwise false (i.e. the handshake failed)
	 */
	private boolean handshake(byte[] infoHash) {
		try {
			// Sends handshake message to the Peer.

			if (this.peerIDString != null) {
				System.out.println("\nSENDING A HANDSHAKE TO"
						+ this + "\n");
			}

			this.outgoing.write(Message.handshakeMessage(infoHash,
					this.clientID));
			this.outgoing.flush();

			// Allocate space for the handshake
			byte[] response = new byte[68];
			byte[] hash = new byte[20];
			this.incoming.readFully(response);

			// Check that the PEER ID given is a VALID PEER ID
			if (this.peerIDString != null) {
				for (int i = 0; i < 20; i++) {
					if (this.peerID[i] != response[48 + i]) {
						System.err.println("Invalid Peer ID. Disconnect!");
						return false;
					}
				}
			}

			// Saving the Peer SHA-1 Hash
			for (int i = 0; i < 20; i++) {
				hash[i] = response[28 + i];
			}

			// This Peer connect to the Client the Server Socket.
			if (this.peerIDString == null) {
				// Saving the Peer ID.
				for (int i = 0; i < 20; i++) {
					this.peerID[i] = response[48 + i];
				}
				try {
					this.peerIDString = new String(peerID, "UTF-8");
				} catch (UnsupportedEncodingException e) {
				}
			}

			// Check the peer's SHA-1 hash matches with local SHA-1 hash
			for (int i = 0; i < 20; i++) {
				if (this.torrentSHA[i] != hash[i]) {
					System.err
							.println("The Peer's SHA-1 does not match with the Client!");
					return false;
				}
			}
			return true;
		} catch (EOFException e) {
			System.err.println("Tracker sending garbage to the server socket");
			System.out.println();
			return false;
		} catch (IOException e) {
			System.err.println("HANDSHAKE FAILURE!");
			return false;
		}
	}

	/**
	 * Update the Peer Bitfield.
	 * 
	 * @param pieceIndex
	 *            The Piece Index.
	 */
	public void updatePeerBitfield(int pieceIndex) {
		if (pieceIndex >= this.peerBooleanBitField.length) {
			System.err.println("ERROR: UPDATING PEER BIT FIELD");
			System.err.println("INVALID PIECE INDEX");
			return;
		}

		this.peerBooleanBitField[pieceIndex] = true;
	}

	/**
	 * enqueueMessage: Method for the client to reach the linkedblockingqueue
	 * that will hold all outbound messages
	 * 
	 * @param message
	 *            TODO
	 */
	public void enqueueMessage(Message message) {
		if (this.writer != null) {
			this.writer.enqueue(message);
		}
	}

	/**
	 * Function to write a message to the outgoing socket.
	 * 
	 * @param payload
	 *            message to be sent to peer
	 */
	public void writeToSocket(Message payload) {
		// In case the client, peer, or peerWriter threads wantto write to
		// socket at the same time
		synchronized (this.outgoing) {
			try {

				// Keep Alive
				if (payload.getLength() == 0) {
					// System.out.println("Sending Keep Alive to " +
					// this.peerIDString);
				} else {

				}

				// get message payload, write to socket, then update the keep
				// alive timer

				// If the message payload is a piece message, update the
				// uploaded bytes counter
				if (Message.responses[payload.getMessageID()].equals("request")) {
					synchronized (this.DLCountLock) {
						this.concurrentRequests += 1;
						while (this.concurrentRequests > MAX_CONCURRENT_SENDS) {
							try {
								(this.DLCountLock).wait();
							} catch (InterruptedException ie) {
								// Whatever
							}
						}
						if (this.remoteChoking) {
							this.outgoing.write(Message.unchoke.getBTMessage());
							this.outgoing.flush();
						}
					}
				} else if (Message.responses[payload.getMessageID()]
						.equals("pieces")) {
					System.out.println("Sending " +
					Message.responses[payload.getMessageID()] +
					 " message to Peer: " + this);
					synchronized (this.ULCountLock) {
						this.recentBytesUploaded += payload.getLength();
					}
				}

				this.outgoing.write(payload.getBTMessage());
				this.outgoing.flush();
				updateTimer();
			} catch (SocketException e) {
				System.err.println(this
						+ "'s socket was closed. (SocketException)");
				if(payload.getMessageID() == 6){
					this.RUBT.getDownloadsInProgess()[payload.getRequestIndex()] = false;
				}				
				this.writer.clearQueue();
				this.enqueueMessage(Message.KILL_PEER_MESSAGE);
				this.RUBT.getPeerHistory().remove(this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Private class that has a thread and a LinkedBlockingQueue It will
	 * continuously read from the queue and send it through it's peer's outgoing
	 * data stream.
	 */
	private class PeerWriter extends Thread {

		private LinkedBlockingQueue<Message> messageQueue;
		private DataOutputStream outgoing;
		private boolean keepRunning = true;
		
		/**
		 * PeerWriter Constructor
		 * 
		 * @param outgoing
		 *            the stream to write out all data to
		 */
		public PeerWriter(DataOutputStream outgoing) {
			this.outgoing = outgoing;
			this.messageQueue = new LinkedBlockingQueue<Message>();
		}// peerWriter constructor

		/**
		 * Public method to add a message to the peerWriter's internal queue
		 * 
		 * @param message that needs to be added to the peerWriter's internal queue
		 */
		protected void enqueue(Message message) {
			try {
				this.messageQueue.put(message);
			} catch (InterruptedException ie) {
				// TODO something with this exception
			}
		}// enqueue
		
		/**
		 * Public method to empty the peerWriter's internal queue in the event the socket was closed.
		 */
		public void clearQueue(){
			this.messageQueue.clear();
		}

		/**
		 * Main runnable thread for the peerWriter private class
		 */
		public void run() {
			// Message current;

			// if the queue contains a poison, exit the thread, otherwise just
			// keep going
			while (this.keepRunning) {
				try {
					final Message current = this.messageQueue.take();

					if (current == Message.KILL_PEER_MESSAGE) {
						this.keepRunning = false;
						continue;
					}
					Peer.this.writeToSocket(current);
				} catch (InterruptedException ie) {
					// Whatever
				}
			}

		}// run

	}// peerWriter

	/**
	 * Main runnable thread process for the peer class. Will connect and
	 * handshake continuously try to read from the incoming socket.
	 */
	public void run() {

		// Check if the peer exists. If not, connect. If it does, just keep the
		// current socket (they handshaked with us)
		initializePeerStreams();

		if (handshake(this.torrentSHA) == true) {
			System.out.println("HANDSHAKE RECEIVED");
			System.out.println("FROM:" + this);
			// Send Bitfield to Peer
			if (this.RUBT.downloaded != 0) {
				Message bitfieldMessage = this.RUBT.generateBitfieldMessage();
				writeToSocket(bitfieldMessage);
			}
		} else {
			// An error was thrown by the handshake method.
			return;
		}

		// Intialize the socket writer
		this.writer = new PeerWriter(this.outgoing);
		this.writer.start();

		/**
		 * Schedules a new anonymous implementation of a TimerTask that will
		 * start now and execute every 10 seconds afterward. Sourced from CS352
		 * Sakai Forums on 3.29.14
		 * 
		 * @author Rob Moore
		 */
		this.peerTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// Let the peer figure out when to send a keepalive
				Peer.this.checkAndSendKeepAlive();
			}// run
		}, 10000, 10000); // keepAlive Transmission Timer

		// Initializes the keep-alive timer to the current system time.
		this.lastMessageTime = System.currentTimeMillis();

		this.peerTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// Let the peer instance figure out when to timeout the remote
				// peer
				Peer.this.checkPeerTimeout();
			}// run
		}, 10000, 10000); // peerTimeout timer

		// Initializes the timeout timer to the current system time
		this.lastPeerTime = System.currentTimeMillis();

		// Checks the upload and download rates every 10 seconds
		this.peerTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Peer.this.updateRates();
			}// run
		}, 2000, 2000); // peerTimeout timer

		//Main part of the thread, will repeatedly read from the input stream of the socket
		try {
			// while the socket is connected
			// read from socket (will block if it is empty) and parse message
			while (this.keepRunning && readSocketInputStream()) {
				// Update the PEER TIMEOUT timer to a new value (because we
				// received a packet).
				updatePeerTimeoutTimer();
			}// while
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}// try

	}// run

	/**
	 * This method is called on shutdown to close all of the data streams and
	 * sockets.
	 */
	public void shutdownPeer() {
		this.keepRunning = false;
		// kill the writer thread with a poison message
		this.writer.enqueue(Message.KILL_PEER_MESSAGE);

		// close all input/output streams and then close the socket to peer.
		try {
			// kill the I/O streams
			this.incoming.close();
			this.outgoing.close();
			// kill the socket
			this.peerConnection.close();
			// cancel all the timers
			this.peerTimer.cancel();

		} catch (Exception e) {
			System.out.println("exxxxxxception");
			// Doesn't matter because the peer is closing anyway
		}
	}

	private boolean readSocketInputStream() throws IOException {

		int length;

		//Check if the connection still exists. If not, return false
		try {
			length = this.incoming.readInt();
		} catch (EOFException e) {
			System.err.println(this + " socket closed. (EOF)");
			return false;
		} catch (SocketException e) {
			System.err.println(this + " socket closed. (SocketException)");
			return false;
		}
		
		byte classID;
		Message incomingMessage = null;

		if (length == 0) {
			//keep alive is the only packet you can receive with length zero
			incomingMessage = Message.keepAlive;
			this.RUBT.queueMessage(new MessageTask(this, incomingMessage));
		} else if (length > 0) {
			// Read the next byte (this should be the classID of the message)
			classID = this.incoming.readByte();
			// Debug statement
			System.out.println("Received " + Message.getMessageID(classID).toUpperCase() +
					" message "+this);
			incomingMessage = new Message(length, classID);

			// Length includes the classID. We are using length to determine how
			// many bytes are left.
			length--;

			// Handle the message based on the classID
			switch (classID) {
			case 0: // choke message
				incomingMessage = Message.choke;
				this.RUBT.queueMessage(new MessageTask(this, incomingMessage));
				break;
			case 1: // unchoke message
				incomingMessage = Message.unchoke;
				this.RUBT.queueMessage(new MessageTask(this, incomingMessage));
				break;
			case 2: // interested message
				incomingMessage = Message.interested;
				this.RUBT.queueMessage(new MessageTask(this, incomingMessage));
				break;
			case 3: // not interested message
				incomingMessage = Message.uninterested;
				this.RUBT.queueMessage(new MessageTask(this, incomingMessage));
				break;
			case 4: //have
			case 5: //bitfield
			case 6: //request
			case 7: //piece
				if (classID == 7) {
					synchronized (this.DLCountLock) {
						this.concurrentRequests -= 1;
						if (this.concurrentRequests <= 1) {
							(this.DLCountLock).notifyAll();
						}
					}
				}
			case 8:
				// have message. bitfield message, request message, piece
				// message, cancel message
				byte[] temp = new byte[length];
				this.incoming.readFully(temp);
				incomingMessage.setPayload(temp);
				this.RUBT.queueMessage(new MessageTask(this, incomingMessage));
				break;
				/*
				 * case 8: //Cancel message int reIndex = this.incoming.readInt();
				 * int reOffset = this.incoming.readInt(); int reLength =
				 * this.incoming.readInt();
				 * 
				 * incomingMessage.cancel(reIndex,reOffset,reLength);
				 * this.RUBT.queueMessage(incomingTask); break;
				 */
			default:
				System.err.println("Unknown class ID");
			}// switch

		}// if

		return true;
	}

	/**
	 * Sends a keep-alive message to the remote peer if the time between now and
	 * the previous message exceeds the limit set by KEEP_ALIVE_TIMEOUT. Sourced
	 * from CS352 Sakai Forums on 3.29.14
	 * 
	 * @author Rob Moore
	 */
	protected void checkPeerTimeout() {
		long now = System.currentTimeMillis();
		if ((now - Peer.this.lastPeerTime) > KEEP_ALIVE_TIMEOUT * 1.05) {
			// Peer timed out, should kill the peer
			Peer.this.shutdownPeer();
		}
	}// checkAndSendKeepAlive

	protected void checkAndSendKeepAlive() {
		long now = System.currentTimeMillis();
		if ((now - Peer.this.lastMessageTime) > KEEP_ALIVE_TIMEOUT * 0.25) {
			Peer.this.writeToSocket(Message.keepAlive);
		}
	}// checkAndSendKeepAlive

	void updateRates() {

		// If we have been downloading consistently...
		if (this.uploadRate < 1000.0) {
			// if you just started downloading, just set the rate = to the rate
			// / 2 seconds
			this.uploadRate = this.recentBytesUploaded / 2.0;
		} else {
			// Takes a weighted average of the current upload rate and the old
			// upload rate
			this.uploadRate = this.uploadRate * .65
					+ (this.recentBytesUploaded / 2.0) * .35;
		}

		// If we have been downloading consistently...
		if (this.downloadRate < 1000.0) {
			// if you just started downloading, just set the rate = to the rate
			// / 2 seconds
			this.downloadRate = this.recentBytesDownloaded / 2.0;
		} else {
			// Takes a weighted average of the current download rate and the old
			// download rate
			this.downloadRate = this.downloadRate * .65
					+ (this.recentBytesDownloaded / 2.0) * .35;
		}

		// reset the recent counter
		synchronized (this.ULCountLock) {
			this.recentBytesUploaded = 0;
		}

		// reset the recent counter
		synchronized (this.DLCountLock) {
			this.recentBytesDownloaded = 0;
		}

		if (this.uploadRate > 0 || this.downloadRate > 1000.0)
			System.out.format(
					"Update rate: %.2f kBps. Download rate: %.2f kBps. %s%n",
					(this.uploadRate / 1000.0), (this.downloadRate / 1000.0),
					this);
	}// updateRate

	private void updateTimer() {
		// System.out.println("Updating message time");
		this.lastMessageTime = System.currentTimeMillis();
	}// updateTimer

	private void updatePeerTimeoutTimer() {
		// System.out.println("Updating last packet from peer time");
		this.lastPeerTime = System.currentTimeMillis();
	}// updatePeerTimeoutTimer

	/**
	 * Checks to see if the Peer is equal to another Peer based on their peerID.
	 * 
	 * @return true when a peerID is equal to this peer. Otherwise false.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Peer)) {
			return false;
		}

		Peer peer = (Peer) obj;
		byte[] peerID = peer.getPeerID();

		for (int i = 0; i < this.peerID.length; i++) {
			if (this.peerID[i] != peerID[i]) {
				return false;
			}
		}

		return true;
	}

}// Peer.java
