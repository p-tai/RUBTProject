import java.io.*;
//import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class Display extends JFrame{
	private JTextArea test;
	private Progbar pb;

	public Display(){

		super("RU BEAR T");
		setLayout(new FlowLayout());

		pb = new Progbar();
		pb.updateBar(50);
		this.setContentPane(pb);
		test = new JTextArea("Hi... testing");
		test.setEditable(false); // i think i'm cheating here oh well
		add(test);
		toUpdate();
	}//end of display

	public void toUpdate(){
		/*
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask(){
			public void run(){

				System.out.println("I'm running woo");
			}//end of run


		},100,100);
		*/
	}//end of toUpdate


}//end of display calss
