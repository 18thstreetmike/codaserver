/*
 * TypeParser.java
 *
 * Created on July 23, 2007, 10:16 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import com.stevesoft.pat.Regex;
import org.codalang.codaserver.language.types.Timestamp;
import org.codalang.codaserver.util.Base64Coder;
import org.glowacki.CalendarParser;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;

/**
 *
 * @author michaelarace
 */
public class TypeParser implements Cloneable {
    
    long typeId;
    private String typeName;
    String validationMask;
    String saveMask;
    
    /** Creates a new instance of TypeParser */
    public TypeParser(long typeId, String typeName, String validationMask, String saveMask) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.validationMask = validationMask;
        this.saveMask = saveMask;
    }
    
    public TypeParser(long typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }
    
    public Object clone() {
        return new TypeParser(typeId, getTypeName(), validationMask, saveMask);
    }
    
    public boolean validate(String value) {
        if (getTypeName().equalsIgnoreCase("STRING")) {
            return (value.length() < 255);
        } else if (getTypeName().equalsIgnoreCase("INTEGER") || getTypeName().equalsIgnoreCase("REFERENCE")) {
            try {
                Long.parseLong(value);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (getTypeName().equalsIgnoreCase("FLOAT")) {
            try {
                Double.parseDouble(value);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (getTypeName().equalsIgnoreCase("BOOLEAN")) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("0")) {
                return true;
            } else {
                return false;
            }
        } else if (getTypeName().equalsIgnoreCase("LONGSTRING")) {
            return true;
        } else if (getTypeName().equalsIgnoreCase("FILE")) {
            int currentIndex = value.indexOf(";");
            if (currentIndex > 0) {
                currentIndex = value.indexOf(";", currentIndex + 1);
                if (currentIndex > 0) {
                    String base64Data = value.substring(currentIndex + 1);
                    try {
                        Base64Coder.decode(base64Data);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            return false;
        } else if (getTypeName().equalsIgnoreCase("TIMESTAMP")) {
            try {
                Calendar cal = CalendarParser.parse(value, CalendarParser.MM_DD_YY);
                return true;
            } catch (Exception e) {
                try {
                    Long.parseLong(value);
                    return true;
                } catch (Exception ef) {
                    return false;
                }
            }
        } else {
            Regex regex = new Regex(validationMask);

            if (regex.search(value)) {
                if (value.equals(regex.stringMatched())) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
    }

    public Object parse(String value) throws CodaException {
        if (getTypeName().equalsIgnoreCase("STRING")) {
            if (validate(value)) {
                return value;
            } else {
                throw new CodaException();
            }
        } else if (getTypeName().equalsIgnoreCase("INTEGER") || getTypeName().equalsIgnoreCase("REFERENCE")) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                throw new CodaException();
            }
        } else if (getTypeName().equalsIgnoreCase("FLOAT")) {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                throw new CodaException();
            }
        } else if (getTypeName().equalsIgnoreCase("BOOLEAN")) {
            if (validate(value)) {
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1")) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                throw new CodaException();
            }
        } else if (getTypeName().equalsIgnoreCase("LONGSTRING")) {
            return value;
        } else if (getTypeName().equalsIgnoreCase("FILE")) {
            if (validate(value)) {
                return value;
            } else {
                throw new CodaException();
            }
        } else if (getTypeName().equalsIgnoreCase("TIMESTAMP")) {
            try {
                return new Timestamp(CalendarParser.parse(value, CalendarParser.MM_DD_YY));
            } catch (Exception e) {
                try {
					GregorianCalendar cal = new GregorianCalendar();
					cal.setTimeInMillis(Long.parseLong(value));
					return new Timestamp(cal);
				} catch (Exception ex) {
					throw new CodaException();
				}
			}
        } else {
            if (validate(value)) {
                Regex regex = new Regex(validationMask, saveMask);
                return regex.replaceAll(value);
            } else {
                throw new CodaException();
            }
        }
    }
    
    public static boolean isArray(String array) {
        //Regex regex = new Regex("\\[(?:\\s*'[^'\\\\\\r\\n]*(?:\\\\.[^'\\\\\\r\\n]*)*'\\s*(?:,\\s*'[^'\\\\\\r\\n]*(?:\\\\.[^'\\\\\\r\\n]*)*')*){0,1}\\s*]");
        Regex regex = new Regex("\\[(?:\\s*'(?:[^']|'')*'\\s*(?:,\\s*'(?:[^']|'')*')*){0,1}\\s*]");
        return (regex.search(array.trim()) && array.trim().equals(regex.stringMatched()));
    }
    
    public static Vector<String> explodeArray(String array) {
        Vector retval = new Vector();
        array = array.trim().substring(1, array.trim().length() - 2).trim();
        if (array.length() > 2) {
			array = array.substring(1, array.length()-2);
		} else {
			array = array.substring(1, array.length()-1);
		}
		String [] fields = array.split("'\\s*,\\s*'");
        for(int i = 0; i < fields.length; i++ ) {
            retval.add(fields[i].replace("''", "'"));
        }
        return retval;
        
    }
    
    public static String implodeArray(Vector<String> stringVector ) {
        String retval = "[";
        for (String field : stringVector) {
            if (!retval.equals("[")) {
                retval += ",";
            }
            if (field.indexOf("''") < 0 && field.indexOf("'") >= 0) {
                retval += "'" + field.replace("'", "''") + "'";
            } else {
                retval += "'" + field + "'";
            }
        }
        retval += "]";
        return retval;
    }
    
    public static String getDisplayName(String typeName) {
        return typeName.toLowerCase().replaceAll("_", " ");
    }

    public String getTypeName() {
        return typeName;
    }
    
}
