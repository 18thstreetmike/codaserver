/*
 * File.java
 *
 * Created on September 11, 2007, 2:41 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language.types;

import org.codalang.codaserver.util.Base64Coder;

/**
 *
 * @author michaelarace
 */
public class File {
    
    String fileName, mimeType, encodedData;
    
    /** Creates a new instance of File */
    public File(String fileName, String mimeType, String encodedData) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.encodedData = encodedData;
    }
    
    public int hashCode() {
        return (fileName + mimeType + encodedData).hashCode();
    }
    
    public boolean equals (Object obj) {
        if (this.getClass().getName().equals(obj.getClass().getName())) {
            return (this.mimeType + this.encodedData).hashCode() == (((File)obj).mimeType + ((File)obj).encodedData).hashCode(); 
        }
        return false;
    }
    
    public File plus(File other) {
        byte[] one = Base64Coder.decode(this.encodedData);
        byte[] two = Base64Coder.decode(other.encodedData);
        
        byte[] combined = new byte[one.length + two.length];
        System.arraycopy(one, 0, combined, 0, one.length);
        System.arraycopy(two, 0, combined, one.length, two.length);
        return new File(this.fileName, this.mimeType, new String(Base64Coder.encode(combined)));
    }
    
    public String getValue() {
        return fileName + ";" + mimeType + ";" + encodedData;
    }
}
