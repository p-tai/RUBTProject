package gui;
import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.AbstractTableModel;

/**
 * cited: http://stackoverflow.com/questions/8372799/making-jtable-cells-uneditable
 * user: Hovercraft Full Of Eels
 * Date: May 3 14
 */
public class MyTableModel extends DefaultTableModel {

	public MyTableModel(Object[][] tableData, String[] colNames) {
		super(tableData, colNames);
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
