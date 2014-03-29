/**
 * 
 * 
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
  
  public MessageTask(final Peer peer, final Message message){
    this.peer = peer;
    this.message = message;
  }

  public Peer getPeer() {
    return this.peer;
  }

  public Message getMessage() {
    return this.message;
  }

}
