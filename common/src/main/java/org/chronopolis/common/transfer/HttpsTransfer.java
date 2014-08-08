package org.chronopolis.common.transfer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.chronopolis.common.exception.FileTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Stop requests that are sent w/ http
 *
 * @author shake
 */
public class HttpsTransfer implements FileTransfer {
    private final Logger log = LoggerFactory.getLogger(HttpsTransfer.class);

    @Override
    public Path getFile(final String uri, final Path stage) throws FileTransferException {
        // Make HTTP Connection
        log.info("Attempting HTTP Transfer from {}", uri);
        URL url;
        Path output = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            log.error("Error connecting to url ", e);
            return null;
        }

        output = Paths.get(stage.toString(),
                uri.substring(uri.lastIndexOf("/", uri.length())));
        Path parent = output.getParent();
        parent.toFile().mkdirs();
        try {
            output.toFile().createNewFile();
        } catch (IOException e) {
            log.error("Error creating file {}", output.toString(), e);
            throw new FileTransferException("Error creating " + output.toString(), e);
        }

        try (
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(output.toString())
        ) {
            FileChannel fc = fos.getChannel();
            fc.transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (FileNotFoundException e) {
            log.error("File not found ", e);
            throw new FileTransferException("File not found", e);
        } catch (IOException e) {
            log.error("IOException while downloading file ", e);
            throw new FileTransferException("IOException while downloading file", e);
        }
        return output;
    }

    @Override
    public void put(final Path localFile, final String uri) throws FileTransferException {
        // TBD
    }
}
