/*
 * httpServer.java
 *
 * Created on October 23, 2007, 2:22 AM
 *
 * CodaServer and related original technologies are copyright 2008, 18th Street Software, LLC.
 *
 * Permission to use them is granted under the terms of the GNU GPLv2.
 */


package org.codalang.codaserver.httpServer;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import org.apache.http.*;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.protocol.BufferingHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.codalang.codaserver.CodaServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class httpServer {

    BasicHttpParams params = new BasicHttpParams();
    int port;
    ListeningIOReactor ioReactor;
    Logger logger;
    
    public httpServer(Logger logger, int port, int soTimeout, int socketBufferSize, boolean staleConnectorCheck, boolean tcpNoDelay, String listenerString) {
        this.params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout) //5000
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, socketBufferSize) // 8 * 1024
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, staleConnectorCheck)  // false
            .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, tcpNoDelay)  // true
            .setParameter(CoreProtocolPNames.ORIGIN_SERVER, listenerString);  // anything you want
        this.port = port;
        this.logger = logger;
    }
    
    public void start(CodaServer server) {
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());
        
        BufferingHttpServiceHandler handler = new BufferingHttpServiceHandler(
                httpproc,
                new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(),
                params);
        
        // Set up request handlers
        HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
        reqistry.register("*", new HttpFileHandler(server) );
        
        handler.setHandlerResolver(reqistry);
        
        // Provide an event logger
        handler.setEventListener(new EventLogger(logger));
        
        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(handler, params);
        
        
        try {
            ioReactor = new DefaultListeningIOReactor(2, params);
            ioReactor.listen(new InetSocketAddress(port));
            ioReactor.execute(ioEventDispatch);
        } catch (InterruptedIOException ex) {
            logger.log(Level.WARNING, "Listener interrupted");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Listener I/O error: " + e.getMessage());
        }
    }
    
    public void stop() {
        try {
            ioReactor.shutdown();
        } catch(Exception e) {
            logger.log(Level.WARNING, "Listener could not be shut down");
        }   
        
    }

    static class HttpFileHandler implements HttpRequestHandler  {
        
        private static CodaServer server;
        
        public HttpFileHandler (CodaServer server) {
            if (this.server == null) {
                this.server = server;
            }
             _objectSkeleton = new HessianSkeleton( new CodaAPI(server), ICodaAPI.class );
        }
        
        private SerializerFactory _serializerFactory;
        
        public SerializerFactory getSerializerFactory()
        {
          if (_serializerFactory == null)
            _serializerFactory = new SerializerFactory();

          return _serializerFactory;
        }

        private Hessian2Input in;
        private HessianSkeleton _objectSkeleton = null;
        public void handle(
                final HttpRequest request, 
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase();
//            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            if ( ! method.equals("POST") ) {
                response.setStatusCode( 500 );
                response.setReasonPhrase( "CodaServer Requires POST." );
                EntityTemplate body = new EntityTemplate( new ContentProducer() {
                  public void writeTo( final OutputStream outstream ) throws IOException {
                      OutputStreamWriter writer = new OutputStreamWriter( outstream, "UTF-8" ); 
                      writer.write("<html><body><h1>CodaServer Requires POST</h1></body></html>");
                      writer.flush();
                  }
                });
                body.setContentType( "text/html; charset=UTF-8" );
                response.setEntity( body );
                //throw new MethodNotSupportedException( "Only POST is supported" ); 
            }

            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                byte[] entityContent = EntityUtils.toByteArray(entity);

            try {
              InputStream is = new ByteArrayInputStream( entityContent );


              in = new Hessian2Input(is);

              getSerializerFactory();
              
              in.setSerializerFactory( _serializerFactory);

              int code = in.read();

              if (code != 'c') {
                // XXX: deflate
                throw new IOException("expected 'c' in hessian input at " + code);
              }

              EntityTemplate body = new EntityTemplate( new ContentProducer() {
                  @SuppressWarnings("unused")
                  public void writeTo( final OutputStream outstream ) throws IOException {
                    
                    int major = in.read();
                      int minor = in.read();
                      AbstractHessianOutput out;
                      out = new HessianOutput( outstream );
                      out.setSerializerFactory(_serializerFactory);

                        try {
                          _objectSkeleton.invoke(in, out);
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        } catch ( Throwable t ) {
                            t.printStackTrace();
                        }

                      out.close();
                  }
              });
              body.setContentType( "text/html; charset=UTF-8" );
              response.setEntity( body );
            } catch (RuntimeException e) {
              throw e;
            }
          }

        }
        
    }
    
    static class EventLogger implements EventListener {

        Logger logger;
        
        public EventLogger(Logger logger) {
           this.logger = logger; 
        }
        
        public void connectionOpen(final NHttpConnection conn) {
            logger.log(Level.INFO, "Connection open: " + conn);
        }

        public void connectionTimeout(final NHttpConnection conn) {
            logger.log(Level.INFO, "Connection timed out: " + conn);
        }

        public void connectionClosed(final NHttpConnection conn) {
            logger.log(Level.INFO, "Connection closed: " + conn);
        }

        public void fatalIOException(final IOException ex, final NHttpConnection conn) {
            logger.log(Level.WARNING, "I/O error: " + ex.getMessage());
        }

        public void fatalProtocolException(final HttpException ex, final NHttpConnection conn) {
            logger.log(Level.WARNING, "HTTP error: " + ex.getMessage());
        }
        
    }
        
}