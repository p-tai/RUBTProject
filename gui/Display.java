package gui;
import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
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
		this.pb.updateBar((int)(this.cli.getPercentageCompletion() * 100 ));
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
		

		//Create a timer.
		this.timer = new Timer(true);
		this.timer.scheduleAtFixedRate(new TimerTask(){

			/**
			 * Update the progress bar and peer list.
			 */
			@Override
			public void run() {
				pb.updateBar((int)(cli.getPercentageCompletion() * 100 ));
			}
			
		}, 1, 1000);
		
		
		
	}//end of display

	@Override
	public void actionPerformed(ActionEvent e) {
		this.timer.cancel();
		this.dispose();
		RUBTClient.shutdown();
	}

}//end of display calss
