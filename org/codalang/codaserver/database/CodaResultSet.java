/*
 * CodaResultSet.java
 *
 * Created on March 13, 2007, 3:52 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.database;

import org.glowacki.CalendarParser;
import org.glowacki.CalendarParserException;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class CodaResultSet {
    
    protected Vector<CodaResultSetColumnHeading> columnHeadings = new Vector();
    protected Vector<CodaResultSetRow> data =new Vector();
    protected Hashtable nameColumns = new Hashtable();
    
    // pointer to the current row
    protected int rowPointer = -1;
    
    // error fields
    protected boolean errorStatus = false;
    protected String errorString;
    
    public CodaResultSet (Vector columnHeadings) {
        this.columnHeadings = columnHeadings;
        if (columnHeadings != null) {
            Iterator it = columnHeadings.iterator();
            int i = 0;
            while(it.hasNext()) {
                CodaResultSetColumnHeading cd = (CodaResultSetColumnHeading)it.next();
                nameColumns.put(cd.getName(), new Integer(i));
                i++;
            }
        }
    }
    
    public CodaResultSet (String errorString) {
        this.errorStatus = true;
        this.errorString = errorString;
    }
    
    public void addRow(Vector data) throws ArrayIndexOutOfBoundsException {
        if (data.size() != this.columnHeadings.size()) {
            throw new ArrayIndexOutOfBoundsException("Data Vector contains incorrect number of columns.");
        } else {
            this.data.add(new CodaResultSetRow(data));
        }
    }
    
    // error functions
    public boolean getErrorStatus () {
        return errorStatus;
    }
    
    public String getErrorString () {
        return errorString;
    }
    
    // metadata functions
    public Vector getColumnHeadings() {
		return columnHeadings;
	}

	public Hashtable getNameColumns() {
		return nameColumns;
	}

	public int getRowPointer() {
		return rowPointer;
	}

	public int getColumnCount() {
        return columnHeadings.size();
    }
    
    public Vector getColumnNames () {
        Vector retval = new Vector();
        Iterator i = columnHeadings.iterator();
        while (i.hasNext()) {
            retval.add(((CodaResultSetColumnHeading)i.next()).getName());
        }
        return retval;
    }
    
    public String getColumnName(int index) {
        return ((CodaResultSetColumnHeading)columnHeadings.get(index)).getName();
    }
    
    public Vector getColumnTypes () {
        Vector retval = new Vector();
        Iterator i = columnHeadings.iterator();
        while (i.hasNext()) {
            retval.add(((CodaResultSetColumnHeading)i.next()).getType());
        }
        return retval;
    }
    
    public String getColumnType(int index) {
        return ((CodaResultSetColumnHeading)columnHeadings.get(index)).getType();
    }
    
    public int getRowsReturned() {
        return data.size();
    }
    
    // iterator and fetch data functions
    public void reset() {
        rowPointer = -1;
    }
    
    public boolean next() {
        rowPointer++;
        if (rowPointer >= this.getRowsReturned()) {
            rowPointer = this.getRowsReturned();
            return false;
        } else {
            return true;
        }
    }
    
    public boolean previous() {
        rowPointer--;
        if (rowPointer < 0) {
            rowPointer = -1;
            return false;
        } else {
            return true;
        }
    }

	public Vector getData() {
		return data;
	}

	public String getData(int index) {
        return ((CodaResultSetRow)data.get(rowPointer)).getDataValue(index);
    }

    public String getData(String index) {
        return ((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue());
    }
  
    public java.util.Calendar getDataDate(int index) {
        Calendar c = null;
        try {
            c = CalendarParser.parse(((CodaResultSetRow)data.get(rowPointer)).getDataValue(index));
        } catch (CalendarParserException ex) {
           // do nothing
        }
        return c;
    }
    
    public java.util.Calendar getDataDate(String index) {
        Calendar c = null;
        try {
            c = CalendarParser.parse(((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()));
        } catch (CalendarParserException ex) {
           // do nothing
        }
        return c;
    }
    
    public int getDataInt(int index) {
        return Integer.parseInt(((CodaResultSetRow)data.get(rowPointer)).getDataValue(index));
    }

    public int getDataInt(String index) {
        return Integer.parseInt(((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()));
    }
    
    public long getDataLong(int index) {
        return Long.parseLong(((CodaResultSetRow)data.get(rowPointer)).getDataValue(index));
    }

    public long getDataLong(String index) {
        return Long.parseLong(((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()));
    }
    
    public float getDataFloat(int index) {
        return Float.parseFloat(((CodaResultSetRow)data.get(rowPointer)).getDataValue(index));
    }

    public float getDataFloat(String index) {
        return Float.parseFloat(((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()));
    }
    
    public double getDataDouble(int index) {
        return Double.parseDouble(((CodaResultSetRow)data.get(rowPointer)).getDataValue(index));
    }

    public double getDataDouble(String index) {
        return Double.parseDouble(((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()));
    }
    
    public boolean getDataBoolean(int index) {
        return ((CodaResultSetRow)data.get(rowPointer)).getDataValue(index).equals("1");
    }

    public boolean getDataBoolean(String index) {
        return ((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()).equals("1");
    }
    
    public byte[] getDataByteArray(int index) {
        return ((CodaResultSetRow)data.get(rowPointer)).getDataValue(index).getBytes();
    }

    public byte[] getDataByteArray(String index) {
        return ((CodaResultSetRow)data.get(rowPointer)).getDataValue(((Integer)nameColumns.get(index)).intValue()).getBytes();
    }
    
    public Hashtable serialize () {
        Hashtable retval = new Hashtable();
        Vector<Hashtable> columnData = new Vector();
        Vector<Vector> rowData = new Vector();
        
        for (CodaResultSetColumnHeading heading : columnHeadings) {
            Hashtable temp = new Hashtable();
            temp.put("columnname", heading.getName());
            temp.put("sqltype", heading.getSqlType());
            columnData.add(temp);
        }
        
        for (CodaResultSetRow rsRow : data) {
            rowData.add(rsRow.getData());
        }
        
        retval.put("columns", columnData);
        retval.put("data", rowData);
        return retval;
    }
}
