package gui;
import java.io.*;
//import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import src.*;
//import gui.Progbar;

public class Display extends JFrame implements ActionListener{// implements ActionListener{
	private JTextArea test;
	private Progbar pb;
	private Timer timer;
	private static final int ONE_SECOND = 1000;
	private Client cli;

	private JButton quitmebutt;
	private JTable table;

	public Display(Client cli2){
		super("RU BEAR T");
		this.cli = cli2;

		setLayout(new FlowLayout());


		this.pb = new Progbar();
		this.pb.updateBar(50);
		this.setContentPane(this.pb);
		this.test = new JTextArea("Hi... testing");
		this.test.setEditable(false); // i think i'm cheating here oh well
		add(this.test);

		Object[][] pt_data = {
			{new Integer(1), new Integer(2), new Integer(50), new Integer(30), new Integer(70)},
			{new Integer(9), new Integer(10), new Integer(80), new Integer(70), new Integer(20)}
		}; //end of data

		String[] columns = {"IP", "Port", "Download Rate", "Upload Rate", "Percentage"};

		this.quitmebutt = new JButton("QUIT ME");
		this.quitmebutt.addActionListener(this);
		add(this.quitmebutt);
		
		MyTableModel model = new MyTableModel(pt_data, columns);
		this.table = new JTable(model);	
		add(new JScrollPane(this.table));
		
		/*
		 * used http://www.math.uni-hamburg.de/doc/java/tutorial/uiswing/components/example-1dot4/ProgressBarDemo.java
		 * bits and parts
		 * may 3 14
		 */
		//Create a timer.
		
		this.timer = new Timer(ONE_SECOND, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Display.this.pb.updateBar(3);// call the getDownloaded thing here :3 
				 
				if (Display.this.cli.isSeeder()) { // if finshed
					Toolkit.getDefaultToolkit().beep();//idk
					Display.this.timer.stop();//yes
					//startButton.setEnabled(true);
					//setCursor(null); //turn off the wait cursor
					Display.this.pb.updateBar(Display.this.pb.getMin()); // reset
				}//end of if done
			}//end of actionperformed
		}); // end of timer maker 
		
		
	}//end of display

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		RUBTClient.shutdown();
		this.dispose();
	}

}//end of display calss
