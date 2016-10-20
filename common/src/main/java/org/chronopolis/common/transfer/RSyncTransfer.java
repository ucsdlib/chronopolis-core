package org.chronopolis.common.transfer;

import org.chronopolis.common.exception.FileTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * TODO: If the rsync cannot connect it will simply hang.
 * We need some sort of prevention against that.
 * Note: Can make a ScheduledFuture w/ a timeout
 *
 * @author shake
 */
public class RSyncTransfer implements FileTransfer {
    private final Logger log = LoggerFactory.getLogger(RSyncTransfer.class);
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final String link;
    private String stats;

    public RSyncTransfer(String link) {
        this.link = link;
        this.stats = "";
    }

    @Override
    public Path getFile(final String uri, final Path local) throws FileTransferException {
        // Taken from http://stackoverflow.com/questions/1246255/any-good-rsync-library-for-java
        // Need to test/modify command
        // Currently uses passwordless SSH keys to login

        Callable<Path> download = () -> {
            String[] cmd = new String[]{"rsync",
                    "-a",
                    "-e ssh -o 'PasswordAuthentication no'",
                    "--stats",
                    link,
                    local.toString()};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = null;
            try {
                log.info("Rsyncing {} -> {}", link, local);

                p = pb.start();
                int exit = p.waitFor();

                stats = stringFromStream(p.getInputStream());

                log.info("rsync exit stats:\n {}", stats);
                if (exit != 0) {
                    log.error("rsync did not complete successfully (exit code {}) \n {}",
                            exit,
                            stringFromStream(p.getErrorStream()));
                    throw new FileTransferException("rsync did not complete successfully (exit code " + exit + ")");
                }

            } catch (IOException e) {
                log.error("IO Exception in rsync ", e);
                p.destroy();
                throw new FileTransferException("IOException in rsync", e);
            } catch (InterruptedException e) {
                log.error("rsync was interrupted", e);
                p.destroy();
                throw new FileTransferException("rsync was interrupted", e);
            }

            return local.resolve(last());
        };

        FutureTask<Path> timedTask = new FutureTask<>(download);
        threadPool.execute(timedTask);

        try {
            // TODO: Timeout based on collection size
            // return timedTask.get(1, TimeUnit.DAYS);
            return timedTask.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("rsync had critical error", e);
            throw new FileTransferException("rsync had a critical error", e);
        } finally {
            threadPool.shutdownNow();
        }
    }

    private String stringFromStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder out = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            out.append(line).append("\n");
        }
        return out.toString();
    }

    @Override
    public void put(final Path localFile, final String uri) throws FileTransferException {
        Callable<Boolean> upload = () -> {
            // Ensure that we don't include the directory
            String local = localFile.toString();
            if (!local.endsWith("/")) {
                local += "/";
            }
            String[] cmd = new String[]{"rsync", "-az", local, uri};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = null;
            try {
                log.info("Executing {} {} {} {}", cmd);
                p = pb.start();
                p.waitFor();
                log.info("rsync exit value: " + p.exitValue());
            } catch (IOException e) {
                log.error("Error starting rsync", e);
                return false;
            } catch (InterruptedException e) {
                log.error("rsync interrupted", e);
                return false;
            }
            return true;
        };

        FutureTask<Boolean> timedTask = new FutureTask<>(upload);
        threadPool.execute(timedTask);

        try {
            // timedTask.get(1, TimeUnit.DAYS);
            timedTask.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("rsync had a critical error", e);
            throw new FileTransferException("rsync had a critical error", e);
        } finally {
            threadPool.shutdownNow();
        }

    }

    @Override
    public String getStats() {
        return stats;
    }

    /**
     * retrieve the last directory in a link
     *
     * test remote (from :)
     * test local (from /)
     *  -> no / return link
     *  -> else substring lastidxof /
     *
     * @return the last directory
     */
    private String last() {
        // first test if we have a remote rsync
        int idx = link.indexOf(":");
        if (idx == -1) {
            return fromSlash(link);
        }

        return fromSlash(link.substring(++idx));
    }

    private String fromSlash(String link) {
        if (link == null || link.isEmpty()) {
            throw new IllegalArgumentException("Cannot retrieve directory of null/empty link");
        }

        int idx = link.lastIndexOf("/");
        if (idx == -1) {
            return link;
        }

        return link.substring(++idx);
    }

}
