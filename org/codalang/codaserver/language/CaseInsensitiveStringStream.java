/*
 * CaseInsensitiveStringStream.java
 *
 * Created on November 1, 2007, 10:26 PM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver.language;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;

/**
 *
 * @author michaelarace
 */
public class CaseInsensitiveStringStream extends ANTLRStringStream {
    public CaseInsensitiveStringStream(String fileName) {
        super(fileName);
    }

    public int LA(int i) {
        if ( i==0 ) {
            return 0; // undefined
        }
        if ( i<0 ) {
            i++; // e.g., translate LA(-1) to use offset 0
        }

        if ( (p+i-1) >= n ) {

            return CharStream.EOF;
        }
        return Character.toUpperCase(data[p+i-1]);
    }
}
