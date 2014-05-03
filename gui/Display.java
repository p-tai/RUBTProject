import java.io.*;
//import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class Display extends JFrame{// implements ActionListener{
	private JTextArea test;
	private Progbar pb;
	private PeerTable pt; // these names are awesome
	private Timer timer;
	private static final int ONE_SECOND = 1000;

	private JButton quitmebutt;
	private JTable table;

	public Display(){

		super("RU BEAR T");
		setLayout(new FlowLayout());

		pb = new Progbar();
		pb.updateBar(50);
		this.setContentPane(pb);
		test = new JTextArea("Hi... testing");
		test.setEditable(false); // i think i'm cheating here oh well
		add(test);

		Object[][] pt_data = {
			{new Integer(1), new Integer(2), new Integer(50), new Integer(30), new Integer(70)},
			{new Integer(9), new Integer(10), new Integer(80), new Integer(70), new Integer(20)}
		}; //end of data

		String[] columns = {"IP", "Port", "Download Rate", "Upload Rate", "Percentage Complete"};

		quitmebutt = new JButton("QUIT ME");
		add(quitmebutt);
		

		table = new JTable(pt_data, columns);	
		add(new JScrollPane(table));
		
		/*
		 * used http://www.math.uni-hamburg.de/doc/java/tutorial/uiswing/components/example-1dot4/ProgressBarDemo.java
		 * bits and parts
		 * may 3 14
		 */
		/*
		//Create a timer.
		timer = new Timer(ONE_SECOND, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				pb.updateBar(3);// call the getDownloaded thing here :3 
				 
				if (download isDone()) { // if finshed
					Toolkit.getDefaultToolkit().beep();//idk
					timer.stop();//yes
					//startButton.setEnabled(true);
					//setCursor(null); //turn off the wait cursor
					progressBar.setValue(pb.getMin()); // reset
				}//end of if done
			}//end of actionperformed
		}); // end of timer maker 
		*/
		
	}//end of display

}//end of display calss
