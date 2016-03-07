package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.listener.BagRESTStepListener;
import org.chronopolis.replicate.batch.listener.TokenRESTStepListener;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import retrofit2.Call;
import retrofit2.Callback;
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
            ReplicationNotifier notifier = new ReplicationNotifier(replication);
            TokenRESTStepListener tokenStepListener = new TokenRESTStepListener(mailUtil,
                    ingestAPI,
                    replication,
                    settings,
                    notifier);
            BagRESTStepListener bagStepListener = new BagRESTStepListener(mailUtil,
                    ingestAPI,
                    replication,
                    settings,
                    notifier);
            TokenDownloadStep tds = new TokenDownloadStep(settings, notifier, replication);
            BagDownloadStep bds = new BagDownloadStep(settings, notifier, replication);
            AceRegisterStep ars = new AceRegisterStep(aceService, settings, notifier, replication);
            ReplicationSuccessStep rss = new ReplicationSuccessStep(mailUtil, settings, notifier);

            createJob(depositor, collection, tds, bds, ars, rss, tokenStepListener, bagStepListener);

        } else {
            // A active
            // N - never completely scanned (default for new collections)
            // E
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
        }
    }

    /**
     * Build a job and launch it with the given parameters
     *
     * @param depositor
     * @param collection
     * @param tokenDownloadStep
     * @param bagDownloadStep
     * @param aceRegisterStep
     * @param replicationSuccessStep
     * @param tokenStepListener
     * @param bagStepListener
     */
    private void createJob(String depositor,
                           String collection,
                           TokenDownloadStep tokenDownloadStep,
                           BagDownloadStep bagDownloadStep,
                           AceRegisterStep aceRegisterStep,
                           ReplicationSuccessStep replicationSuccessStep,
                           StepExecutionListener tokenStepListener,
                           StepExecutionListener bagStepListener) {
        Job job = jobBuilderFactory.get("collection-replicate")
                .start(stepBuilderFactory.get("token-replicate")
                        .tasklet(tokenDownloadStep)
                        .listener(tokenStepListener)
                        .build())
                .next(stepBuilderFactory.get("bag-replicate")
                        .tasklet(bagDownloadStep)
                        .listener(bagStepListener)
                        .build())
                .next(stepBuilderFactory.get("ace-register")
                        .tasklet(aceRegisterStep)
                        .listener(replicationStepListener)
                        .build())
                .next(stepBuilderFactory.get("replication-success")
                        .tasklet(replicationSuccessStep)
                        .listener(replicationStepListener)
                        .build())
                .build();

        try {
            jobLauncher.run(job, new JobParametersBuilder()
                    .addString("depositor", depositor)
                    .addString("collection", collection)
                    .addDate("date", new Date())
                    .toJobParameters());
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("JobExecutionException", e);
        } catch (JobRestartException e) {
            log.error("JobRestartException", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("JobAlreadyCompletedException", e);
        } catch (JobParametersInvalidException e) {
            log.error("JobInvalidParamsException", e);
        }

    }

}
