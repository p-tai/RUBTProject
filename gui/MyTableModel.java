import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.AbstractTableModel;

public class MyTableModel extends DefaultTableModel {

	public MyTableModel(Object[][] tableData, String[] colNames) {
		super(tableData, colNames);
	}

	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
