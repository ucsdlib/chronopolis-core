package org.chronopolis.replicate.jobs;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.ReplicationQueue;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.chronopolis.replicate.processor.CollectionInitProcessor.TOKEN_DOWNLOAD;

/**
 * TODO There's shared code between this and the BagDownloadJobListener.
 * Can probably make a function they can both use.
 *
 * Created by shake on 6/13/14.
 */
public class TokenStoreDownloadJob implements Job {
    private final Logger log = LoggerFactory.getLogger(TokenStoreDownloadJob.class);

    public static final String COMPLETED = "completed";
    public final static String PROPERTIES = "properties";
    public final static String MESSAGE = "message";

    private ReplicationProperties properties;
    private CollectionInitMessage message;
    private Map<String, String> completionMap;

    private String location;
    private String protocol;
    private String digest;

    private void initFromJobDataMap(final JobDataMap jobDataMap) {
        setProperties((ReplicationProperties) jobDataMap.get(PROPERTIES));
        setMessage((CollectionInitMessage) jobDataMap.get(MESSAGE));
        setCompletionMap((Map<String, String>) jobDataMap.get(COMPLETED));

        setDigest(message.getTokenStoreDigest());
        setLocation(message.getTokenStore());
        setProtocol(message.getProtocol());
    }

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        initFromJobDataMap(jobExecutionContext.getJobDetail().getJobDataMap());

        log.info("Downloading Token Store from {}", location);
        Path manifest;
        try {
            manifest = ReplicationQueue.getFileImmediate(location,
                    Paths.get(properties.getStage()),
                    protocol);

            jobExecutionContext.setResult(manifest);
        } catch (IOException e) {
            log.error("Error downloading token store", e);
            throw new JobExecutionException(e);
        } catch (FileTransferException e) {
            log.error("File transfer exception", e);
            throw new JobExecutionException(e);
        }

        HashFunction hashFunction = Hashing.sha256();
        HashCode hashCode;
        try {
            hashCode = Files.hash(manifest.toFile(), hashFunction);
        } catch (IOException e) {
            log.error("Error hashing token store", e);
            throw new JobExecutionException(e);
        }

        String calculatedDigest = hashCode.toString();
        log.trace("Calculated digest {} for token store", calculatedDigest);

        if (!calculatedDigest.equalsIgnoreCase(digest)) {
            log.error("Downloaded token store does not match expected digest!" +
                      "\nFound {}\nExpected {}",
                    calculatedDigest,
                    digest);
            jobExecutionContext.setResult(false);
            // throw JobExecutionException?
        } else {
            log.info("Successfully validated token store");
            jobExecutionContext.setResult(true);
            completionMap.put(TOKEN_DOWNLOAD, "Successfully downloaded from "
                    + location);
        }

    }

    public void setProperties(final ReplicationProperties properties) {
        this.properties = properties;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public void setDigest(final String digest) {
        this.digest = digest;
    }

    public void setMessage(final CollectionInitMessage message) {
        this.message = message;
    }

    public void setCompletionMap(final Map<String, String> completionMap) {
        this.completionMap = completionMap;
    }

}
