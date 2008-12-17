/*
 * Timestamp.java
 *
 * Created on September 11, 2007, 3:16 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

import org.glowacki.CalendarParser;
import org.glowacki.CalendarParserException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author michaelarace
 */
public class Timestamp {
    
    public static final int MONTH = GregorianCalendar.MONTH;
    public static final int DAY_OF_MONTH = GregorianCalendar.DAY_OF_MONTH;
    public static final int YEAR = GregorianCalendar.YEAR;
    public static final int HOUR = GregorianCalendar.HOUR;
    public static final int MINUTE = GregorianCalendar.MINUTE;
    public static final int SECOND = GregorianCalendar.SECOND;
    public static final int WEEK_OF_MONTH = GregorianCalendar.WEEK_OF_MONTH;
    public static final int WEEK_OF_YEAR = GregorianCalendar.WEEK_OF_YEAR;
    public static final int AM_PM = GregorianCalendar.AM_PM;
    
    
    java.util.Calendar value = new GregorianCalendar();
    
    /** Creates a new instance of Timestamp */
    public Timestamp(long timestamp) {
        value.setTimeInMillis(timestamp);
    }

	public Timestamp(java.util.Calendar value) {
		this.value = value;
	}

	public Timestamp() {
        value = new GregorianCalendar();
    }
    
    public boolean parse(String date) {
        long timeInMillis = value.getTimeInMillis();
        try {
            value.setTimeInMillis(CalendarParser.parse(date).getTimeInMillis());
            return true;
        } catch (CalendarParserException ex) {
            value.setTimeInMillis(timeInMillis);
            return false;
        }
    }
    
    public void add(int field, int amount) {
        value.add(field, amount);
    }
    
    public void subtract(int field, int amount) {
        this.add(-1 * field, amount);
    }
    
    public long getValue() {
        return value.getTimeInMillis();
    }
    
    public String format(String mask) {
        SimpleDateFormat df = new SimpleDateFormat(mask);
        df.setCalendar(value);
        return df.format(new Date(value.getTimeInMillis()));
    }
}
