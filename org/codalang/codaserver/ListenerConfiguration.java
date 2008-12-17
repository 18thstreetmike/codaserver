/*
 * ListenerConfiguration.java
 *
 * Created on October 25, 2007, 3:05 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */

package org.codalang.codaserver;

/**
 *
 * @author michaelarace
 */
public class ListenerConfiguration {


    private String ipAddress = "127.0.0.1";
    private int port = 3407;
    private int soTimeout = 5000;
    private int socketBufferSize = 8192;
    private int staleConnectorCheck = 0;
    private int tcpNoDelay = 1;
    
    /** Creates a new instance of ListenerConfiguration */
    public ListenerConfiguration() {
    }

     public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int getSocketBufferSize() {
        return socketBufferSize;
    }

    public void setSocketBufferSize(int socketBufferSize) {
        this.socketBufferSize = socketBufferSize;
    }

    public int getStaleConnectorCheck() {
        return staleConnectorCheck;
    }

    public void setStaleConnectorCheck(int staleConnectorCheck) {
        this.staleConnectorCheck = staleConnectorCheck;
    }

    public int getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(int tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

   
}
