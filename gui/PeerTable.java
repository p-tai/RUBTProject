import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;

public class PeerTable extends JPanel{
	private String[] columns;
	private Object[][] data;
	private JTable table;

	public PeerTable(Object[][] data){
		//super(new GridLayout(1,0));

		String[] columns = {"IP", 
			"Port", 
			"Download Rate", 
			"Upload Rate",
			"Percentage Complete"};
		this.columns = columns;
		this.data = data;

		this.table = new JTable(data,columns);
		//this.table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		//this.table.setFillsViewportHeight(true);

	}//end of peertable constructor


}//end of peertable
