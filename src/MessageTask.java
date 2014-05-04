package src;
/**
 * Simple container class for messages received from peers.
 * Passed to RUBTClient for processing.
 * 
 * Source:
 * @author Robert Moore
 * Taken from sakai CS352 class resources on 3/29/14
 *
 */
public class MessageTask {
	  private final Peer peer;
	  private final Message message;
	  
	  /**
	   * Constructor for the Message task class
	   * @param peer - the peer that is associated with this task
	   * @param message - the message associated with this task
	   */
	  public MessageTask(final Peer peer, final Message message){
	    this.peer = peer;
	    this.message = message;
	  }//MessageTask constructor
	
	  /**
	   * @return The Peer that associated with this task
	   */
	  public Peer getPeer() {
	    return this.peer;
	  }//getPeer
	
	  /**
	   * @return The Message associated with this task
	   */
	  public Message getMessage() {
	    return this.message;
	  }//getMessage

}//MessageTask
