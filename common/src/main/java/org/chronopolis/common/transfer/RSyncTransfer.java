package org.chronopolis.common.transfer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.chronopolis.common.exception.FileTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * TODO: If the rsync cannot connect it will simply hang.
 * We need some sort of prevention against that.
 * Could use the ConnectionTimeout option for ssh
 * Note: Can make a ScheduledFuture w/ a timeout
 *
 * @author shake
 */
public class RSyncTransfer implements FileTransfer {
    private final Logger log = LoggerFactory.getLogger("rsync-log");
    private final String link;
    private final Path storage;

    // should this be a List?
    private final List<String> arguments;

    // Set during the execution of our process
    private InputStream stream;
    private InputStream errors;

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    @Deprecated
    public RSyncTransfer(String link) {
        this.link = link;
        this.storage = null;
        this.arguments = ImmutableList.of("-aL", "--stats");
    }

    public RSyncTransfer(final String link, final Path local) {
        this.link = link;
        this.storage = local;
        this.arguments = ImmutableList.of("-aL", "--stats");
    }

    public RSyncTransfer(final String link, final Path local, List<String> arguments) {
        this.link = link;
        this.storage = local;
        this.arguments = arguments;
    }

    /**
     * Execute an external rsync process in order to transfer data
     *
     * Enforces key based SSH authentication by disabling PasswordAuthentication
     *
     * @param uri   The location of the file
     * @param local The local destination for the transfer
     * @return The {@link Path} to the transferred files
     * @throws FileTransferException if there is a problem with the transfer
     */
    @Override
    public Path getFile(@Deprecated final String uri,
                        @Deprecated final Path local) throws FileTransferException {
        final String fteMessage = "rsync did not complete successfully (exit code %d)";

        // search for rsync in the path? or pass in path?
        final String rsyncCommand = "rsync";
        final String stats = "--stats";

        // always disable password auth
        final String sshConfig = "-e ssh -o 'PasswordAuthentication no'";

        Callable<Path> download = () -> {
            Path parent = storage.getParent();
            if (!parent.toFile().exists()) {
                log.debug("Creating parent directory {}", parent);
                Files.createDirectories(parent);
            }

            ImmutableList<String> command = new ImmutableList.Builder<String>()
                    .add(rsyncCommand)
                    .add(sshConfig)
                    .addAll(arguments)
                    .add(link)
                    .add(storage.toString()).build();
            ProcessBuilder pb = new ProcessBuilder(command);
            Process p = null;
            try {
                log.info("rsync {} -> {}", link, storage);

                p = pb.start();
                int exit = p.waitFor();
                stream = p.getInputStream();

                if (exit != 0) {
                    errors = p.getErrorStream();
                    throw new FileTransferException(String.format(fteMessage, exit));
                }

            } catch (IOException e) {
                log.error("IO Exception in rsync ", e);
                p.destroy();
                errors = new ByteArrayInputStream(e.getMessage().getBytes());
                throw new FileTransferException("IOException in rsync", e);
            } catch (InterruptedException e) {
                log.error("rsync was interrupted", e);
                p.destroy();
                errors = new ByteArrayInputStream(e.getMessage().getBytes());
                throw new FileTransferException("rsync was interrupted", e);
            }

            return storage.resolve(last());
        };

        FutureTask<Path> timedTask = new FutureTask<>(download);
        threadPool.execute(timedTask);

        try {
            return timedTask.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("rsync had critical error", e);
            throw new FileTransferException("rsync had a critical error", e);
        } finally {
            threadPool.shutdownNow();
        }
    }

    @Override
    public Path get() throws FileTransferException {
        return getFile(link, storage);
    }

    @Override
    public String getStats() {
        return "";
    }

    /**
     * Return the input stream from the output of the rsync
     *
     * @return InputStream
     */
    public InputStream getOutput() {
        return stream;
    }

    /**
     * Return the input stream from the stderr of the rsync
     *
     * @return InputStream
     */
    public InputStream getErrors() {
        return errors;
    }

    /**
     * retrieve the last directory in a link
     * <p>
     * test remote (from :)
     * test local (from /)
     * -> no / return link
     * -> else substring lastIndexOf /
     *
     * @return the last directory
     */
    @VisibleForTesting
    String last() {
        if (link == null) {
            throw new IllegalArgumentException("Cannot retrieve directory of null link");
        }

        // first test if we have a remote rsync
        int idx = link.lastIndexOf(":");
        if (idx == -1) {
            return fromSlash(link);
        }

        return fromSlash(link.substring(++idx));
    }

    private String fromSlash(String link) {
        if (link == null || link.isEmpty()) {
            throw new IllegalArgumentException("Cannot retrieve directory of empty link");
        }

        int idx = link.lastIndexOf("/");
        if (idx == -1) {
            return link;
        }

        return link.substring(++idx);
    }

}
