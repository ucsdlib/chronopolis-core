package org.chronopolis.replicate.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shake on 6/13/14.
 */
public class TokenStoreDownloadJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(TokenStoreDownloadJobListener.class);

    private final String name;
    private final Scheduler scheduler;

    public TokenStoreDownloadJobListener(String name, Scheduler scheduler) {
        this.name = name;
        this.scheduler = scheduler;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobWasExecuted(final JobExecutionContext jobExecutionContext,
                               final JobExecutionException e) {
        // If there was no exception, schedule our next job
        if (e == null) {
            JobKey myKey = jobExecutionContext.getJobDetail().getKey();

            try {
                scheduler.triggerJob(new JobKey(myKey.getName(), "BagDownload"));
            } catch (SchedulerException e1) {
                log.error("Scheduler exception!!", e1);
            }

        } else { // requeue our job..?

        }
    }
}
