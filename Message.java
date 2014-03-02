import java.nio.ByteBuffer;
import edu.rutgers.cs.cs352.bt.util.*;

//TODO Add Cancel Function

public class Message {

	public static final byte[] keepAlive = {0, 0, 0, 0};
	
	public static final byte[] choke = {0, 0, 0, 1, 0}; 
	
	public static final byte[] unchoke = {0, 0, 0, 1, 1};
	
	public static final byte[] interested = {0, 0, 0, 1, 2};
	
	public static final byte[] uninterested = {0, 0, 0, 1, 3};
	
	public static final String[] responses = {"choke", "unchoke", "interested", 
											  "uninterested", "have", "bitfield", 
											  "request", "pieces", "cancel"};
	
	public static byte[] have(int piece){
		byte[] have = new byte[9];
		byte[] pieceByte = ByteBuffer.allocate(4).putInt(piece).array();
		
		/* Length Prefix */
		have[0] = 0;
		have[1] = 0;
		have[2] = 0;
		have[3] = 5;
		
		/* Message ID */
		have[4] = 4;
		
		/* Read in the pieceByte */
		for(int i = 5; i < 9; i++){
			have[i] = pieceByte[i-5];
		}
		
		return have;
	}
	
	public static byte[] request(int index, int begin, int length){
		
		
		ByteBuffer values = ByteBuffer.allocateDirect(4*4);
		
		values.putInt(index);
		
		values.putInt(begin);
		
		values.putInt(length);
		
		ToolKit.print(values);
		
		byte[] request = new byte[17];
		
		int a = 0;
		int b = 0;
		int c = 0;
		
		/* Length Prefix */
		request[0] = 0;
		request[1] = 0;
		request[2] = 0;
		request[3] = 13;
		/* Message ID */
		request[4] = 6;
		
		for(int i = 5; i < 17; i++){
			System.out.print(request[i]);
			if(i < 9){
				request[i] = values.get();
				a++;
			}else if(i < 13){
				request[i] = values.get();
				b++;
			}else{
				request[i] = values.get();
				c++;
			}
		}
		System.out.print('\n');
		return request;
	}
	
	public static byte[] piece(int x, int index, int begin, int block){
		byte[] piece = new byte[17];
		byte[] prefixByte = ByteBuffer.allocate(4).putInt(9 + x).array();
		byte[] indexByte = ByteBuffer.allocate(4).putInt(index).array();
		byte[] beginByte = ByteBuffer.allocate(4).putInt(begin).array();
		byte[] lengthByte = ByteBuffer.allocate(4).putInt(block).array();
		
		int a = 0;
		int b = 0;
		int c = 0;
		
		/* Length Prefix */
		for(int i = 0; i < 4; i++){
			piece[i] = prefixByte[i];
		}
		/* Message ID */
		piece[4] = 7;
		
		for(int i = 5; i < 17; i++){
			if(i < 9){
				piece[i] = indexByte[a]; 
				a++;
			}else if(i < 13){
				piece[i] = beginByte[b];
				b++;
			}else{
				piece[i] = lengthByte[c];
				c++;
			}
		}
		return piece;
	}
	
	public static String getMessageID(byte x){
		int y = (int)x;
		return responses[y];
	}
	
}
