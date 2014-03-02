public class Packet {
	private int blockNum;
	private int packetNum;
	private byte[] data;
	
	public Packet(int blockNum, int packetNum, byte[] data){
		this.blockNum = blockNum;
		this.packetNum = packetNum;
		this.data = data;
	}
	
	public String toString(){
		return "Block Number = " + this.blockNum + " Packet Number " + this.packetNum;
	}
}
