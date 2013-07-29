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
 *
 * @author shake
 */
public class ReplicationQueue implements Runnable {
    private final Logger log = LoggerFactory.getLogger(ReplicationQueue.class);
    private LinkedBlockingQueue<ReplicationDownload> downloadQueue;
    private static HttpsTransfer xfer;
    private static final String SLASH =  "/"; // Not the guitarist
    private static GenericProperties props;

    private static String buildURL(String base, String collection, String group, String file) {
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
    
    public static Path getImmediateFile(String base, String collection, String group, String file) throws IOException {
        Path collPath = Paths.get(props.getStage(), group, collection);
        String url = buildURL(base, collection, group, file);
        return xfer.getFile(url, collPath);
    }

    public void getFileAsync(String base, String collection, String group, String file) {
        ReplicationDownload rFile = new ReplicationDownload(base, collection, group, file);
        downloadQueue.add(rFile);
    }

    public void run() {
        while ( !Thread.currentThread().isInterrupted() ) {
            try {
                ReplicationDownload dl = downloadQueue.poll();
                Path collPath = Paths.get(props.getStage(), dl.getGroup(), dl.getCollection());
                String url = buildURL(dl.getBase(), dl.getCollection(), dl.getGroup(), dl.getFile());
                xfer.getFile(url, collPath);
            } catch (IOException ex) {
                log.error("Unable to download file: {} ", ex);
            }
        }
    }

    // Small class to encapsulate the variables needed for a download
    private class ReplicationDownload {
        private String collection;
        private String group;
        private String file;
        private String base;

        private ReplicationDownload(String collection, String group, String file, String base) {
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