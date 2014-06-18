package org.chronopolis.replicate.jobs;

import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.replicate.ReplicationProperties;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 6/13/14.
 */
public class BagDownloadJob implements Job {
    private final Logger log = LoggerFactory.getLogger(BagDownloadJob.class);

    public static final String DEPOSITOR = "depositor";
    public static final String LOCATION = "location";
    public static final String PROTOCOL = "protocol";
    public static final String PROPERTIES = "properties";

    private String depositor;
    private String location;
    private String protocol;
    private ReplicationProperties properties;

    private void initFromJobDataMap(final JobDataMap jobDataMap) {
        setProperties((ReplicationProperties) jobDataMap.get(PROPERTIES));
        setDepositor(jobDataMap.getString(DEPOSITOR));
        setLocation(jobDataMap.getString(LOCATION));
        setProtocol(jobDataMap.getString(PROPERTIES));
    }

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        initFromJobDataMap(jobExecutionContext.getJobDetail().getJobDataMap());

        FileTransfer transfer;
        Path bagPath = Paths.get(properties.getStage(), depositor);

        String uri;
        if (protocol.equalsIgnoreCase("https")) {
            transfer = new HttpsTransfer();
            uri = location;
        } else {
            String[] parts = location.split("@", 2);
            String user = parts[0];
            uri = parts[1];
            transfer = new RSyncTransfer(user);
        }

        try {
            transfer.getFile(uri, bagPath);
        } catch (FileTransferException e) {
            log.error("File transfer exception", e);
            throw new JobExecutionException(e);
        }
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public void setProperties(final ReplicationProperties properties) {
        this.properties = properties;
    }
}
