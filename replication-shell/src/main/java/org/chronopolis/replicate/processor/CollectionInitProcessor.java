/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.CredentialRequestInterceptor;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.digest.DigestUtil;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.ReplicationQueue;
import org.chronopolis.replicate.jobs.AceRegisterJob;
import org.chronopolis.replicate.jobs.BagDownloadJob;
import org.chronopolis.replicate.jobs.TokenStoreDownloadJob;
import org.chronopolis.replicate.util.URIUtil;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO: How to reply to collection init message if there is an error
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private static final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    private static final String TOKEN_DOWNLOAD = "TokenStore-Download";
    private static final String BAG_DOWNLOAD = "Bag-Download";
    private static final String ACE_REGISTER_COLLECTION = "Ace-Register-Collection";
    private static final String ACE_REGISTER_TOKENS = "Ace-Register-Tokens";
    private static final String INCOMPLETE = "Incomplete";

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final ReplicationProperties props;
    private final MailUtil mailUtil;
    private final AceService aceService;
    private final Scheduler scheduler;

    private HashMap<String, String> completionMap;
    private AtomicBoolean callbackComplete = new AtomicBoolean(false);

    public CollectionInitProcessor(ChronProducer producer,
                                   MessageFactory messageFactory,
                                   ReplicationProperties props,
                                   MailUtil mailUtil,
                                   Scheduler scheduler) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.props = props;
        this.mailUtil = mailUtil;
        this.scheduler = scheduler;

        // Set up our map of tasks and their progress for more info in mail messages
        completionMap = new HashMap<>();
        completionMap.put(TOKEN_DOWNLOAD, INCOMPLETE);
        completionMap.put(BAG_DOWNLOAD, INCOMPLETE);
        completionMap.put(ACE_REGISTER_COLLECTION, INCOMPLETE);
        completionMap.put(ACE_REGISTER_TOKENS, INCOMPLETE);

        // This might be better done through the dependency injection framework
        String endpoint = URIUtil.buildAceUri(props.getAceFqdn(),
                props.getAcePort(),
                props.getAcePath()).toString();

        CredentialRequestInterceptor interceptor = new CredentialRequestInterceptor(
                props.getAceUser(),
                props.getAcePass());

        RestAdapter restAdapter = new RestAdapter.Builder()
                                                 .setEndpoint(endpoint)
                                                 .setRequestInterceptor(interceptor)
                                                 .build();

        aceService = restAdapter.create(AceService.class);
    }



    // TODO: Reply if there is an error with the collection (ie: already registered in ace), or ack
    // TODO: Fix the flow of this so that we don't return on each failure...
    // that way we send mail and return in one spot instead of 4
    // TODO: Replace with tasks (quartz?)
    //       -> download tokens
    //       -> download bag
    //       -> ace stuff
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

        JobDataMap tsDataMap = new JobDataMap();
        tsDataMap.put(TokenStoreDownloadJob.LOCATION, msg.getTokenStore());
        tsDataMap.put(TokenStoreDownloadJob.PROTOCOL, msg.getProtocol());
        tsDataMap.put(TokenStoreDownloadJob.PROPERTIES, props);
        JobDetail tsJobDetail = JobBuilder.newJob(TokenStoreDownloadJob.class)
                .setJobData(tsDataMap)
                .withIdentity(msg.getCorrelationId(), "TokenDownload")
                .storeDurably()
                .build();

        JobDataMap bdDataMap = new JobDataMap();
        bdDataMap.put(BagDownloadJob.DEPOSITOR, msg.getDepositor());
        bdDataMap.put(BagDownloadJob.LOCATION, msg.getBagLocation());
        bdDataMap.put(BagDownloadJob.PROTOCOL, msg.getProtocol());
        bdDataMap.put(BagDownloadJob.PROPERTIES, props);
        JobDetail bdJobDetail = JobBuilder.newJob(BagDownloadJob.class)
                .setJobData(bdDataMap)
                .withIdentity(msg.getCorrelationId(), "BagDownload")
                .storeDurably()
                .build();

        JobDataMap arDataMap = new JobDataMap();
        arDataMap.put(AceRegisterJob.TOKEN_STORE, msg.getCollection() + "-tokens");
        arDataMap.put(AceRegisterJob.REGISTER, true);
        arDataMap.put(AceRegisterJob.RETURN_KEY, msg.getReturnKey());
        arDataMap.put(AceRegisterJob.ACE_SERVICE, aceService);
        arDataMap.put(AceRegisterJob.AUDIT_PERIOD, msg.getAuditPeriod());
        arDataMap.put(AceRegisterJob.COLLECTION, msg.getCollection());
        arDataMap.put(AceRegisterJob.FIXITY_ALGORITHM, msg.getFixityAlgorithm());
        arDataMap.put(AceRegisterJob.GROUP, msg.getDepositor());
        arDataMap.put(AceRegisterJob.PROPERTIES, props);
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
