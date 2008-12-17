/*
 * CodaTypeConverter.java
 *
 * Created on March 13, 2007, 4:11 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

import java.sql.Types;

/**
 *
 * @author michaelarace
 */
public class CodaTypeConverter {
    
    public static int getSQLTypeFromCodaType (String codaType) {
        if (codaType.equalsIgnoreCase("integer") || codaType.equalsIgnoreCase("reference")) {
            return Types.BIGINT;
        } else if (codaType.equalsIgnoreCase("float")) {
            return Types.FLOAT;
        } else if (codaType.equalsIgnoreCase("boolean")) {
            return Types.INTEGER;
        } else if (codaType.equalsIgnoreCase("timestamp")) {
            return Types.TIMESTAMP;
        } else if (codaType.equalsIgnoreCase("string")) {
            return Types.VARCHAR;
        } else if (codaType.equalsIgnoreCase("file")) {
            return Types.BLOB;
        } else {
            return Types.CLOB;
        }
    }
    
}
