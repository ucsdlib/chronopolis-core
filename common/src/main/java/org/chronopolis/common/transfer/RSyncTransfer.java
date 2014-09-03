package org.chronopolis.common.transfer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.chronopolis.common.exception.FileTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: If the rsync cannot connect it will simply hang.
 *       We need some sort of prevention against that.
 *       Note: Can make a ScheduledFuture w/ a timeout
 *
 * @author shake
 */
public class RSyncTransfer implements FileTransfer {
    private final Logger log = LoggerFactory.getLogger(RSyncTransfer.class);
    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final String user;

    public RSyncTransfer(String user) {
      this.user = user;
    }

    @Override
    public Path getFile(final String uri, final Path local) throws FileTransferException {
        // Taken from http://stackoverflow.com/questions/1246255/any-good-rsync-library-for-java
        // Need to test/modify command
        // Currently uses passwordless SSH keys to login to sword

        log.debug(local.toString());
        Callable<Path> download = new Callable<Path>() {
            @Override
            public Path call() throws Exception {
                String[] cmd = new String[]{"rsync", "-az", user + "@" + uri, local.toString()};
                String[] parts = uri.split(":", 2);
                String[] pathList = parts[1].split("/");
                ProcessBuilder pb = new ProcessBuilder(cmd);
                Process p = null;
                try {
                    log.info("Executing {} {} {} {}", cmd);
                    p = pb.start();
                    p.waitFor();
                } catch (IOException e) {
                    log.error("IO Exception in rsync ", e);
                    throw new FileTransferException("IOException in rsync", e);
                } catch (InterruptedException e) {
                    log.error("rsync was interrupted", e);
                    p.destroy();
                    throw new FileTransferException("rsync was interrupted", e);
                }

                Path dir = local.resolve(pathList[pathList.length - 1]);
                return dir;
            }
        };

        FutureTask<Path> timedTask = new FutureTask<>(download);
        threadPool.execute(timedTask);

        try {
            // TODO: Timeout based on collection size?
            return timedTask.get(15, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("rsync had critical error", e);
            throw new FileTransferException("rsync had a critical error", e);
        } finally {
            threadPool.shutdownNow();
        }
    }

    @Override
    public void put(final Path localFile, final String uri) throws FileTransferException {
        Callable<Boolean> upload = new Callable() {
            @Override
            public Boolean call() {
                String[] cmd = new String[]{"rsync", "-az", localFile.toString(), uri};
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
            }
        };

        FutureTask<Boolean> timedTask = new FutureTask<>(upload);
        threadPool.execute(timedTask);

        try {
             timedTask.get(15, TimeUnit.MINUTES);
        } catch (InterruptedException  | ExecutionException | TimeoutException e) {
            log.error("rsync had a critical error", e);
            throw new FileTransferException("rsync had a critical error", e);
        } finally {
            threadPool.shutdownNow();
        }

    }

}
