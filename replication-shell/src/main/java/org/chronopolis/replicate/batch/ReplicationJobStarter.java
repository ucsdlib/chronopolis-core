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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.util.Date;

/**
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

            Job job = jobBuilderFactory.get("collection-replicate")
                    .start(stepBuilderFactory.get("token-replicate")
                        .tasklet(new TokenDownloadStep(settings, msg, notifier))
                        .listener(tokenStepListener)
                        .build())
                    .next(stepBuilderFactory.get("bag-replicate")
                        .tasklet(new BagDownloadStep(settings, msg, notifier))
                        .listener(bagStepListener)
                        .build())
                    .next(stepBuilderFactory.get("ace-register")
                        .tasklet(new AceRegisterStep(aceService, settings, msg, notifier))
                        .listener(replicationStepListener)
                        .build())
                    .next(stepBuilderFactory.get("replication-success")
                        .tasklet(new ReplicationSuccessStep(producer, messageFactory, mailUtil, settings, notifier))
                        .listener(replicationStepListener)
                        .build())
                    .build();

            try {
                jobLauncher.run(job, new JobParametersBuilder()
                        .addString("depositor", depositor)
                        .addString("collection", collection)
                        .addString("token-store-location", msg.getTokenStore())
                        .addString("token-store-digest", msg.getTokenStoreDigest())
                        .addString("bag-location", msg.getBagLocation())
                        .addString("tag-manifest-digest", msg.getTagManifestDigest())
                        .addString("correlation-id", msg.getCorrelationId())
                        .toJobParameters());
            } catch (JobExecutionAlreadyRunningException e) {
                e.printStackTrace();
            } catch (JobRestartException e) {
                e.printStackTrace();
            } catch (JobInstanceAlreadyCompleteException e) {
                e.printStackTrace();
            } catch (JobParametersInvalidException e) {
                e.printStackTrace();
            }
        } else {
            CollectionInitCompleteMessage reply =
                    messageFactory.collectionInitCompleteMessage(msg.getCorrelationId());
            producer.send(reply, msg.getReturnKey());
        }
    }

    public void addJobFromRestful(Replication replication) {
        String depositor = replication.getBag().getDepositor();
        String collection = replication.getBag().getName();

        GsonCollection gsonCollection = aceService.getCollectionByName(collection, depositor);
        if (gsonCollection == null) {
            ReplicationNotifier notifier = new ReplicationNotifier(replication);
            TokenRESTStepListener tokenStepListener = new TokenRESTStepListener(ingestAPI,
                    replication,
                    notifier);
            BagRESTStepListener bagStepListener = new BagRESTStepListener(ingestAPI,
                    replication,
                    settings,
                    notifier);


            Job job = jobBuilderFactory.get("collection-replicate")
                    .start(stepBuilderFactory.get("token-replicate")
                        .tasklet(new TokenDownloadStep(settings, notifier, replication))
                        .listener(tokenStepListener)
                        .build())
                    .next(stepBuilderFactory.get("bag-replicate")
                        .tasklet(new BagDownloadStep(settings, notifier, replication))
                        .listener(bagStepListener)
                        .build())
                    .next(stepBuilderFactory.get("ace-register")
                        .tasklet(new AceRegisterStep(aceService, settings, notifier, replication))
                        .listener(replicationStepListener)
                        .build())
                    .next(stepBuilderFactory.get("replication-success")
                        .tasklet(new ReplicationSuccessStep(producer, messageFactory, mailUtil, settings, notifier))
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
                e.printStackTrace();
            } catch (JobRestartException e) {
                e.printStackTrace();
            } catch (JobInstanceAlreadyCompleteException e) {
                e.printStackTrace();
            } catch (JobParametersInvalidException e) {
                e.printStackTrace();
            }

        } else {
            log.info("Already have collection, probably should update the replication object");
        }
    }

}
