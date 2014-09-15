/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.TopicProducer;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.batch.AceRegisterStep;
import org.chronopolis.replicate.batch.BagDownloadStep;
import org.chronopolis.replicate.batch.ReplicationStepListener;
import org.chronopolis.replicate.batch.ReplicationSuccessStep;
import org.chronopolis.replicate.batch.TokenDownloadStep;
import org.chronopolis.replicate.config.ReplicationSettings;
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

import java.util.HashMap;

/**
 * TODO: How to reply to collection init message if there is an error
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private static final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    public static final String TOKEN_DOWNLOAD = "TokenStore-Download";
    public static final String BAG_DOWNLOAD = "Bag-Download";
    public static final String ACE_REGISTER_COLLECTION = "Ace-Register-Collection";
    public static final String ACE_REGISTER_TOKENS = "Ace-Register-Tokens";
    private static final String INCOMPLETE = "Incomplete";

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final ReplicationSettings settings;
    private final MailUtil mailUtil;
    private final AceService aceService;

    // TODO: Move into a class for launching/checking job status
    private final ReplicationStepListener replicationStepListener;
    private JobLauncher jobLauncher;
    private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;

    public CollectionInitProcessor(final TopicProducer producer,
                                   final MessageFactory messageFactory,
                                   final ReplicationSettings replicationSettings,
                                   final MailUtil mailUtil,
                                   final AceService aceService,
                                   final ReplicationStepListener replicationStepListener,
                                   JobBuilderFactory jobBuilderFactory,
                                   JobLauncher jobLauncher,
                                   StepBuilderFactory stepBuilderFactory) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.settings = replicationSettings;
        this.mailUtil = mailUtil;
        this.aceService = aceService;
        this.replicationStepListener = replicationStepListener;
        this.jobBuilderFactory = jobBuilderFactory;
        this.jobLauncher = jobLauncher;
        this.stepBuilderFactory = stepBuilderFactory;
    }


    @Override
    public void process(ChronMessage chronMessage) {
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            log.error("Incorrect Message Type");
            return;
        }

        log.trace("Received collection init message");

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;
        String depositor = msg.getDepositor();
        String collection = msg.getCollection();

        // check to see if we already have the collection
        // if we don't, replicate it
        // if we do, just sent an init complete message
        GsonCollection gsonCollection = aceService.getCollectionByName(collection, depositor);
        if (gsonCollection == null) {
            Job job = jobBuilderFactory.get("collection-replicate")
                    .start(stepBuilderFactory.get("token-replicate")
                        .tasklet(new TokenDownloadStep(settings, msg))
                        .listener(replicationStepListener)
                        .build())
                    .next(stepBuilderFactory.get("bag-replicate")
                        .tasklet(new BagDownloadStep(settings, msg))
                        .listener(replicationStepListener)
                        .build())
                    .next(stepBuilderFactory.get("ace-register")
                        .tasklet(new AceRegisterStep(aceService, settings, msg))
                        .listener(replicationStepListener)
                        .build())
                    .next(stepBuilderFactory.get("replication-success")
                        .tasklet(new ReplicationSuccessStep(producer, msg, messageFactory, mailUtil, settings))
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

}
