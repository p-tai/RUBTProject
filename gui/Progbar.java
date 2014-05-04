//package gui;
import javax.swing.*;
import javax.swing.event.*;

/*
 * Taken from "endian"
 * http://stackoverflow.com/questions/8916064/how-to-add-a-progress-bar
 * on May 2nd, 2014: 11:13pm
 */

public class Progbar extends JPanel{

	JProgressBar pbar;

	static final int MY_MIN = 0;
	static final int MY_MAX = 100;

	public Progbar(){
		pbar = new JProgressBar();
		pbar.setMinimum(MY_MIN);
		pbar.setMaximum(MY_MAX);
		pbar.setStringPainted(true);
		add(pbar);


	}//end of progbar

	public void updateBar(int newValue){
		pbar.setValue(newValue);
	}//end of updatebar

	// gets the minimum % completed on the pbar
	public int getMin(){
		return MY_MIN;
	}//end of get min

/*	
	public static void main(String args[]) {

		final Progbar it = new Progbar();

		JFrame frame = new JFrame("Progress Bar Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(it);
		frame.pack();
		frame.setVisible(true);

		// run a loop to demonstrate raising
		for (int i = MY_MIN; i <= MY_MAX; i++) {
			final int percent = i;
			try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						it.updateBar(percent);
					}
				});
				java.lang.Thread.sleep(100);
			} catch (InterruptedException e) {
				System.out.println("Ohno");
			}
		}
	}
*/

}//end of progbar
