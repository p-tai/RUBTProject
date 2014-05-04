//package gui;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Client{
	public static void main(String[] args){
		Display dis = new Display();

		dis.setSize(500,500);
		dis.setResizable(false);
		dis.setVisible(true);
		dis.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


	}//end of main

}//end of client class
