/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which will fire off downloads
 * Do we want the queue to be static so that all threads can poll from it?
 *
 * @author shake
 */
public class ReplicationQueue implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ReplicationQueue.class);
    private static final String SLASH =  "/"; // Not the guitarist
    private static LinkedBlockingQueue<ReplicationDownload> downloadQueue = new LinkedBlockingQueue<>();
    private static HttpsTransfer xfer = new HttpsTransfer();
    private static GenericProperties props;


    public static void setProperties(GenericProperties props) {
        ReplicationQueue.props = props;
    }

    private static String buildURL(String base, 
                                   String collection, 
                                   String group, 
                                   String file) {
        StringBuilder uriBuilder = new StringBuilder(base);

        if ( group != null && !group.isEmpty()) {
            uriBuilder.append(group).append(SLASH);
        }
        if ( collection != null && !collection.isEmpty()) {
            uriBuilder.append(collection).append(SLASH);
        }
        if ( file != null && !file.isEmpty()) {
            uriBuilder.append(file);
        }

        return uriBuilder.toString();
    }
    
    public static Path getImmediateFile(String url, Path stage) throws IOException {
        if ( url == null ) {
            System.out.println("Null url");
        } else if ( stage == null ) {
            System.out.println("Null stage");
        } else if (xfer == null ) {
            System.out.println("This shouldn't happen");
        }
        return xfer.getFile(url, stage);
    }

    public static void getFileAsync(String base, 
                                    String collection, 
                                    String group, 
                                    String file) {
        ReplicationDownload rFile = new ReplicationDownload(base, collection, group, file);
        try {
            log.info("Queueing {}", file);
            downloadQueue.put(rFile);
        } catch (InterruptedException ex) {
            log.error("Thread interrupted " + ex);
        }
    }

    @Override
    public void run() {
        while ( !Thread.currentThread().isInterrupted() ) {
            Path file = null;
            ReplicationDownload dl = downloadQueue.poll();
            try {
                Path collPath = Paths.get(props.getStage(), dl.getGroup(), dl.getCollection());
                String url = buildURL(dl.getBase(), dl.getCollection(), dl.getGroup(), dl.getFile());
                file = xfer.getFile(url, collPath);
            } catch (IOException ex) {
                log.info("Unable to download file: {} ", ex);
            } finally {
                // Requeue if necessary
                if ( file == null ) {
                    log.info("Requeuing " + dl.getFile());
                    getFileAsync(dl.getBase(), dl.getCollection(), dl.getGroup(), dl.getFile());
                }
            }
        }
    }

    // Small class to encapsulate the variables needed for a download
    private static class ReplicationDownload {
        private String collection;
        private String group;
        private String file;
        private String base;

        private ReplicationDownload(String base, 
                                    String collection, 
                                    String group, 
                                    String file) {
            this.collection = collection;
            this.group = group;
            this.file = file;
            this.base = base;
        }

        public String getCollection() {
            return collection;
        }

        public String getGroup() {
            return group;
        }

        public String getFile() {
            return file;
        }

        public String getBase() {
            return base;
        }
    }
} 