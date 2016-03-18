package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.ace.AceTasklet;
import org.chronopolis.replicate.batch.listener.BagRESTStepListener;
import org.chronopolis.replicate.batch.listener.TokenRESTStepListener;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Date;

/**
 * Submits a Job to the {@link JobLauncher}
 * <p/>
 * TODO: Trim the fat
 * <p/>
 * Created by shake on 12/1/14.
 */
public class ReplicationJobStarter {
    private final Logger log = LoggerFactory.getLogger(ReplicationJobStarter.class);

    private final ReplicationSettings settings;
    private final MailUtil mailUtil;
    private final AceService aceService;
    private final IngestAPI ingestAPI;

    private final ReplicationStepListener replicationStepListener;
    private JobLauncher jobLauncher;
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;


    public ReplicationJobStarter(final ReplicationSettings replicationSettings,
                                 final MailUtil mailUtil,
                                 final AceService aceService,
                                 final IngestAPI ingestAPI,
                                 final ReplicationStepListener replicationStepListener,
                                 final JobLauncher jobLauncher,
                                 final JobBuilderFactory jobBuilderFactory,
                                 final StepBuilderFactory stepBuilderFactory) {
        this.settings = replicationSettings;
        this.mailUtil = mailUtil;
        this.aceService = aceService;
        this.ingestAPI = ingestAPI;
        this.replicationStepListener = replicationStepListener;
        this.jobLauncher = jobLauncher;
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }


    /**
     * Add a replication job which was received from the RESTful interface
     *
     * @param replication
     */
    public void addJobFromRestful(Replication replication) {
        String depositor = replication.getBag().getDepositor();
        String collection = replication.getBag().getName();

        GsonCollection gsonCollection = null;
        Call<GsonCollection> call = aceService.getCollectionByName(collection, depositor);
        try {
            Response<GsonCollection> response = call.execute();
            gsonCollection = response.body();
        } catch (IOException e) {
            log.error("Error communicating with server", e);
        }

        if (gsonCollection == null) {
            createJob(replication);
        } else {
            // A active
            // N - never completely scanned (default for new collections)
            // E

            createJob(replication);
            /*
            log.debug("Already have collection, state {}", gsonCollection.getState());
            if (gsonCollection.getState() == 'E') {
                log.info("Error in collection, replicating again");
                replication.setStatus(ReplicationStatus.FAILURE);
            } else if (gsonCollection.getState() == 'N') {
                log.info("Loading ACE settings for collection");
                replication.setStatus(ReplicationStatus.TRANSFERRED);
            } else if (gsonCollection.getState() == 'A') {
                log.info("Updating replication to note success");
                replication.setStatus(ReplicationStatus.SUCCESS);
            }

            // ingestAPI.updateReplication(replication.getId(), replication);
            Call<Replication> ingestCall = ingestAPI.updateReplicationStatus(replication.getId(), new RStatusUpdate(replication.getStatus()));
            ingestCall.enqueue(new Callback<Replication>() {
                @Override
                public void onResponse(Response<Replication> response) {

                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            });
            */
        }
    }

    private void createJob(Replication replication) {
        SimpleJobBuilder builder = null;
        ReplicationNotifier notifier = new ReplicationNotifier(replication);

        switch (replication.getStatus()) {
            // Run through the full flow
            case PENDING:
            case STARTED:
                builder = fromPending(replication, notifier);
                break;
            // Do all ACE work
            case TRANSFERRED:
            // TODO: Separate for registered/loaded
            case ACE_REGISTERED:
            case ACE_TOKEN_LOADED:
                builder = fromTransferred(null, replication, notifier);
                break;
            case ACE_AUDITING:
                builder = fromAceAuditing(replication);
                break;
            default:
                return;
        }

        Bag bag = replication.getBag();
        try {
            jobLauncher.run(builder.build(), new JobParametersBuilder()
                .addString("depositor", bag.getDepositor())
                .addString("collection", bag.getName())
                .addDate("date", new Date())
                .toJobParameters());
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("JobExecutionException", e);
        } catch (JobRestartException e) {
            log.error("JobRestartException", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("JobInstanceAlreadyCompletedException", e);
        } catch (JobParametersInvalidException e) {
            log.error("JobParametersInvalidException", e);
        }
    }

    /**
     * PENDING,
     * STARTED,
     * TRANSFERRED,
     * SUCCESS,
     * ACE_REGISTERED,
     * ACE_TOKEN_LOADED,
     * ACE_AUDITING,
     *
     */

    /**
     * Create a JobBuilder with all steps needed from the PENDING state
     *
     * @param replication
     * @param notifier
     * @return
     */
    private SimpleJobBuilder fromPending(Replication replication, ReplicationNotifier notifier) {
        TokenDownloadStep tokenDownloadStep = new TokenDownloadStep(settings, notifier, replication);
        TokenRESTStepListener tokenStepListener = new TokenRESTStepListener(mailUtil, ingestAPI, replication, settings, notifier);

        BagDownloadStep bagDownloadStep = new BagDownloadStep(settings, notifier, replication);
        BagRESTStepListener bagStepListener = new BagRESTStepListener(mailUtil, ingestAPI, replication, settings, notifier);

        JobBuilder builder = jobBuilderFactory.get("collection-replicate");
        SimpleJobBuilder start = builder.start(stepBuilderFactory.get("token-replicate")
                        .tasklet(tokenDownloadStep)
                        .listener(tokenStepListener)
                        .build())
                .next(stepBuilderFactory.get("bag-replicate")
                        .tasklet(bagDownloadStep)
                        .listener(bagStepListener)
                        .build());
        return fromTransferred(start, replication, notifier);
    }

    /**
     * Create a JobBuilder with all steps needed after a transfer has completed
     *
     * @param builder
     * @param replication
     * @param notifier
     * @return
     */
    private SimpleJobBuilder fromTransferred(SimpleJobBuilder builder, Replication replication, ReplicationNotifier notifier) {
        AceTasklet at = new AceTasklet(ingestAPI, aceService, replication, settings, notifier);
        TaskletStep step = stepBuilderFactory.get("ace-task")
                .tasklet(at)
                .listener(replicationStepListener)
                .build();

        if (builder == null) {
            JobBuilder jb = jobBuilderFactory.get("collection-replicate");
            return jb.start(step);
        } else {
            return builder.next(step);
        }
    }

    private SimpleJobBuilder fromAceRegistered(SimpleJobBuilder builder, Replication replication) {
        return null;
    }

    private SimpleJobBuilder fromAceTokenLoaded(SimpleJobBuilder builder, Replication replication) {
        return null;
    }

    /**
     * Create a JobBuilder for checking the status of ACE audits
     *
     * @param replication
     * @return
     */
    private SimpleJobBuilder fromAceAuditing(Replication replication) {
        AceCheckTasklet tasklet = new AceCheckTasklet(ingestAPI, aceService, replication);
        JobBuilder builder = jobBuilderFactory.get("ace-check");
        return builder.start(stepBuilderFactory.get("ace-check")
                .tasklet(tasklet)
                .build());
    }

}
