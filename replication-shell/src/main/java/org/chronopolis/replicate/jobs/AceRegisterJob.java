package org.chronopolis.replicate.jobs;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationProperties;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO: Check if our http calls succeed or not
 *
 * Created by shake on 6/13/14.
 */
public class AceRegisterJob implements Job {
    private final Logger log = LoggerFactory.getLogger(AceRegisterJob.class);

    public static final String COMPLETED = "completed";
    public static final String REGISTER = "register";
    public static final String TOKEN_STORE = "token_store";
    public static final String PROPERTIES = "properties";
    public static final String ACE_SERVICE = "ace_service";
    public static final String MESSAGE = "message";

    private String collection;
    private String group;
    private String fixityAlg;
    private int auditPeriod;
    private boolean register;
    private AtomicBoolean callbackComplete;

    private String tokenStore;
    private String returnKey;
    private ReplicationProperties properties;
    private AceService aceService;

    private CollectionInitMessage message;
    private Map<String, String> completionMap;


    private void initFromDataMap(final JobDataMap jobDataMap) {
        setProperties((ReplicationProperties) jobDataMap.get(PROPERTIES));
        setAceService((AceService) jobDataMap.get(ACE_SERVICE));
        setMessage((CollectionInitMessage) jobDataMap.get(MESSAGE));
        setRegister(jobDataMap.getBoolean(REGISTER));
        setCompletionMap((Map<String, String>) jobDataMap.get(COMPLETED));

        setAuditPeriod(message.getAuditPeriod());
        setCollection(message.getCollection());
        setFixityAlg(message.getFixityAlgorithm());
        setGroup(message.getDepositor());
        setReturnKey(message.getReturnKey());

        setTokenStore(jobDataMap.getString(TOKEN_STORE));
    }

    @Override
    // TODO: Split this up into multiple jobs?
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        initFromDataMap(jobExecutionContext.getJobDetail().getJobDataMap());

        Path collectionPath = Paths.get(properties.getStage(), group, collection);
        Path manifest = Paths.get(properties.getStage(), tokenStore);
        callbackComplete = new AtomicBoolean(false);

        log.trace("Building ACE json");
        GsonCollection aceGson = new GsonCollection.Builder()
                .name(collection)
                .digestAlgorithm(fixityAlg)
                .directory(collectionPath.toString())
                .group(group)
                .storage("local")
                .auditPeriod(String.valueOf(auditPeriod))
                .auditTokens("true")
                .proxyData("false")
                .build();

        log.debug("POSTing {}", aceGson.toJson());
        Map<String, Integer> idMap;
        try {
            idMap = aceService.addCollection(aceGson);
            log.info("Successfully registered collection {}", collection);
        } catch (RetrofitError error) {
            log.error("Error registering ACE collection. Response code {} with reason {}",
                    error.getResponse().getStatus(), error.getResponse().getReason());

            throw new JobExecutionException("HTTP " + error.getResponse().getStatus()
                    + "/" + error.getResponse().getReason()
                    + " from " + error.getUrl());
        }

        // completionMap.put(ACE_REGISTER_COLLECTION, "Successfully registered");

        long id = idMap.get("id");

        Callback tsCallback = new Callback() {
            @Override
            public void success(final Object o, final Response response) {
                log.info("Successfully posted token store");
                // completionMap.put(ACE_REGISTER_TOKENS, "Successfully registered with response "
                //        + response.getStatus());
                callbackComplete.getAndSet(true);
            }

            @Override
            public void failure(final RetrofitError retrofitError) {
                log.error("Error posting token store {} {}",
                        retrofitError.getResponse().getStatus(),
                        retrofitError.getBody());
                // completionMap.put(ACE_REGISTER_TOKENS, "Failed to register tokens:\n"
                //        + retrofitError.getBody());
                callbackComplete.getAndSet(true);
            }
        };

        aceService.loadTokenStore(id, new TypedFile("ASCII Text", manifest.toFile()), tsCallback);

        // Since the callback is asynchronous, we need to wait for it to complete before moving on
        log.trace("Waiting for http call to complete");
        waitForCallback();
        callbackComplete.set(false);

        aceService.startAudit(id, new Callback<Void>() {
            @Override
            public void success(final Void aVoid, final Response response) {
                log.info("Successfully started audit");
                callbackComplete.set(true);
            }

            @Override
            public void failure(final RetrofitError error) {
                log.info("Could not start audit. {} {}",
                        error.getResponse().getStatus(),
                        error.getResponse().getReason());
                callbackComplete.set(true);
            }
        });
        log.trace("Waiting for http call to complete");
        waitForCallback();

   }

    private void waitForCallback() {
        while (!callbackComplete.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
            }
        }
     }

    public void setCollection(final String collection) {
        this.collection = collection;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public void setFixityAlg(final String fixityAlg) {
        this.fixityAlg = fixityAlg;
    }

    public void setAuditPeriod(final int auditPeriod) {
        this.auditPeriod = auditPeriod;
    }

    public void setRegister(final boolean register) {
        this.register = register;
    }

    public void setProperties(final ReplicationProperties replicationProperties) {
        this.properties = replicationProperties;
    }

    public void setAceService(final AceService aceService) {
        this.aceService = aceService;
    }

    public void setTokenStore(final String tokenStore) {
        this.tokenStore = tokenStore;
    }

    public void setReturnKey(final String returnKey) {
        this.returnKey = returnKey;
    }

    public void setMessage(final CollectionInitMessage message) {
        this.message = message;
    }

    public void setCompletionMap(final Map<String,String> completionMap) {
        this.completionMap = completionMap;
    }

}
