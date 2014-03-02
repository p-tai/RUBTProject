import java.net.*;

public class Peer {

	private String peerID;
	private String peerIP;
	private int peerPort;
	private boolean isChoke;
	private boolean isInterested;
	
	/**
	 * 
	 * @param peerID The peer's ID.
	 * @param peerIP The peer's IP.
	 * @param peerPort The peer's Port.
	 */
	public Peer(String peerID, String peerIP, int peerPort){
		this.peerID = peerID;
		this.peerIP = peerIP;
		this.peerPort = peerPort;
	}
	
	/*********************************
	 * Getters
	 ********************************/
	
	public String getPeerID() {
		return peerID;
	}

	public String getPeerIP() {
		return peerIP;
	}

	public int getPeerPort() {
		return peerPort;
	}

	public boolean isChoke() {
		return isChoke;
	}

	public boolean isInterested() {
		return isInterested;
	}

	/*********************************
	 * Setters
	 ********************************/
	
	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}
	
	public void setPeerIP(String peerIP) {
		this.peerIP = peerIP;
	}
	
	public void setPeerPort(int peerPort) {
		this.peerPort = peerPort;
	}
	
	public void setChoke(boolean isChoke) {
		this.isChoke = isChoke;
	}
	
	public void setInterested(boolean isInterested) {
		this.isInterested = isInterested;
	}
	
	public String toString(){
		return "Peer ID: " + this.peerID + " Peer IP: " + this.peerIP + " Peer Port: " + this.peerPort;
	}
}
