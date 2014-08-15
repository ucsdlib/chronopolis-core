/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.jobs.AceRegisterJob;
import org.chronopolis.replicate.jobs.BagDownloadJob;
import org.chronopolis.replicate.jobs.TokenStoreDownloadJob;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Scheduler scheduler;

    private HashMap<String, String> completionMap;

    public CollectionInitProcessor(ChronProducer producer,
                                   MessageFactory messageFactory,
                                   ReplicationSettings settings,
                                   MailUtil mailUtil,
                                   Scheduler scheduler,
                                   AceService aceService) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.settings = settings;
        this.mailUtil = mailUtil;
        this.scheduler = scheduler;

        // Set up our map of tasks and their progress for more info in mail messages
        completionMap = new HashMap<>();
        completionMap.put(TOKEN_DOWNLOAD, INCOMPLETE);
        completionMap.put(BAG_DOWNLOAD, INCOMPLETE);
        completionMap.put(ACE_REGISTER_COLLECTION, INCOMPLETE);
        completionMap.put(ACE_REGISTER_TOKENS, INCOMPLETE);

        // This might be better done through the dependency injection framework
        this.aceService = aceService;
    }



    // TODO: Reply if there is an error with the collection (ie: already registered in ace), or ack
    @Override
    public void process(ChronMessage chronMessage) {
        // TODO: Replace these with the values from the properties
        boolean checkCollection = false;
        boolean register = true;

        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            log.error("Incorrect Message Type");
            return;
        }

        log.trace("Received collection init message");

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        // Set up our data maps and jobs
        // TODO: We only need one data map w/ the properties, message, completed, etc

        JobDataMap tsDataMap = new JobDataMap();
        tsDataMap.put(TokenStoreDownloadJob.SETTINGS, settings);
        tsDataMap.put(TokenStoreDownloadJob.MESSAGE, msg);
        tsDataMap.put(TokenStoreDownloadJob.COMPLETED, completionMap);
        JobDetail tsJobDetail = JobBuilder.newJob(TokenStoreDownloadJob.class)
                .setJobData(tsDataMap)
                .withIdentity(msg.getCorrelationId(), "TokenDownload")
                .storeDurably()
                .build();

        JobDataMap bdDataMap = new JobDataMap();
        bdDataMap.put(BagDownloadJob.SETTINGS, settings);
        bdDataMap.put(BagDownloadJob.MESSAGE, msg);
        bdDataMap.put(BagDownloadJob.COMPLETED, completionMap);
        JobDetail bdJobDetail = JobBuilder.newJob(BagDownloadJob.class)
                .setJobData(bdDataMap)
                .withIdentity(msg.getCorrelationId(), "BagDownload")
                .storeDurably()
                .build();

        JobDataMap arDataMap = new JobDataMap();
        arDataMap.put(AceRegisterJob.TOKEN_STORE, msg.getCollection() + "-tokens");
        arDataMap.put(AceRegisterJob.REGISTER, true);
        arDataMap.put(AceRegisterJob.ACE_SERVICE, aceService);
        arDataMap.put(AceRegisterJob.SETTINGS, settings);
        arDataMap.put(AceRegisterJob.MESSAGE, msg);
        arDataMap.put(AceRegisterJob.COMPLETED, completionMap);
        JobDetail arJobDetail = JobBuilder.newJob(AceRegisterJob.class)
                .setJobData(arDataMap)
                .withIdentity(msg.getCorrelationId(), "AceRegister")
                .storeDurably()
                .build();

        try {
            scheduler.addJob(tsJobDetail, false);
            scheduler.addJob(bdJobDetail, false);
            scheduler.addJob(arJobDetail, false);
            scheduler.triggerJob(tsJobDetail.getKey());
        } catch (SchedulerException e) {
            log.error("", e);
        }

    }

}
