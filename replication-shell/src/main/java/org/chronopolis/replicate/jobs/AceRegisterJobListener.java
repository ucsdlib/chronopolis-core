package org.chronopolis.replicate.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shake on 6/13/14.
 */
public class AceRegisterJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(AceRegisterJobListener.class);

    private final String name;
    private final Scheduler scheduler;

    public AceRegisterJobListener(final String name, final Scheduler scheduler) {
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
        // Send collection init complete
    }

}
