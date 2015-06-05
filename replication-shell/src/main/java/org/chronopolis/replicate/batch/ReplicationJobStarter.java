package org.chronopolis.replicate.batch;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.listener.BagAMQPStepListener;
import org.chronopolis.replicate.batch.listener.BagRESTStepListener;
import org.chronopolis.replicate.batch.listener.TokenAMQPStepListener;
import org.chronopolis.replicate.batch.listener.TokenRESTStepListener;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
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

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final ReplicationSettings settings;
    private final MailUtil mailUtil;
    private final AceService aceService;
    private final IngestAPI ingestAPI;

    private final ReplicationStepListener replicationStepListener;
    private JobLauncher jobLauncher;
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;


    public ReplicationJobStarter(final ChronProducer producer,
                                 final MessageFactory messageFactory,
                                 final ReplicationSettings replicationSettings,
                                 final MailUtil mailUtil,
                                 final AceService aceService,
                                 final IngestAPI ingestAPI,
                                 final ReplicationStepListener replicationStepListener,
                                 final JobLauncher jobLauncher,
                                 final JobBuilderFactory jobBuilderFactory,
                                 final StepBuilderFactory stepBuilderFactory) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.settings = replicationSettings;
        this.mailUtil = mailUtil;
        this.aceService = aceService;
        this.ingestAPI = ingestAPI;
        this.replicationStepListener = replicationStepListener;
        this.jobLauncher = jobLauncher;
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }


    @Deprecated
    public void addJobFromMessage(CollectionInitMessage msg) {
        String depositor = msg.getDepositor();
        String collection = msg.getCollection();

        // check to see if we already have the collection
        // if we don't, replicate it
        // if we do, just sent an init complete message
        GsonCollection gsonCollection = aceService.getCollectionByName(collection, depositor);
        if (gsonCollection == null) {
            ReplicationNotifier notifier = new ReplicationNotifier(msg);
            TokenAMQPStepListener tokenStepListener = new TokenAMQPStepListener(notifier,
                    settings,
                    mailUtil,
                    msg.getTokenStoreDigest());

            BagAMQPStepListener bagStepListener = new BagAMQPStepListener(settings,
                    notifier,
                    mailUtil,
                    msg.getTagManifestDigest());

            TokenDownloadStep tds = new TokenDownloadStep(settings, msg, notifier);
            BagDownloadStep bds = new BagDownloadStep(settings, msg, notifier);
            AceRegisterStep ars = new AceRegisterStep(aceService, settings, msg, notifier);
            ReplicationSuccessStep rss = new ReplicationSuccessStep(producer, messageFactory, mailUtil, settings, notifier);

            createJob(depositor, collection, tds, bds, ars, rss, tokenStepListener, bagStepListener);

        } else {
            CollectionInitCompleteMessage reply =
                    messageFactory.collectionInitCompleteMessage(msg.getCorrelationId());
            producer.send(reply, msg.getReturnKey());
        }
    }

    /**
     * Add a replication job which was received from the RESTful interface
     *
     * @param replication
     */
    public void addJobFromRestful(Replication replication) {
        String depositor = replication.getBag().getDepositor();
        String collection = replication.getBag().getName();

        GsonCollection gsonCollection = aceService.getCollectionByName(collection, depositor);
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
            ReplicationSuccessStep rss = new ReplicationSuccessStep(producer, messageFactory, mailUtil, settings, notifier);

            createJob(depositor, collection, tds, bds, ars, rss, tokenStepListener, bagStepListener);

        } else {
            // A active
            // N - never completely scanned (default for new collections)
            // E
            log.debug("Already have collection, state {}", gsonCollection.getState());
            if (gsonCollection.getState() == 'E') {
                log.info("Error in collection, replicating again");
            } else if (gsonCollection.getState() == 'N') {
                log.info("Loading ACE settings for collection");
            } else if (gsonCollection.getState() == 'A') {
                log.info("Updating replication to note success");
                replication.setStatus(ReplicationStatus.SUCCESS);
                ingestAPI.updateReplication(replication.getID(), replication);
            }
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
