package org.chronopolis.replicate.service;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.mail.SmtpProperties;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.chronopolis.rest.models.ReplicationStatus.STARTED;

/**
 * Service for running the replication-shell in development mode. Give a prompt for interacting
 * with the user, allowing them to decide when to query the ingest-server
 *
 *
 * Created by shake on 2/23/15.
 */
@Component
@Profile("development")
public class CommandLineService implements ReplicationService {
    private final Logger log = LoggerFactory.getLogger(CommandLineService.class);

    private final ApplicationContext context;
    private final AceConfiguration aceConfiguration;
    private final SmtpProperties smtpProperties;
    private final ReplicationProperties replicationProperties;
    private final IngestAPIProperties properties;
    private final IngestAPI ingestAPI;
    private final Submitter submitter;

    @Autowired
    public CommandLineService(ApplicationContext context,
                              AceConfiguration aceConfiguration,
                              SmtpProperties smtpProperties,
                              ReplicationProperties replicationProperties,
                              IngestAPIProperties properties,
                              IngestAPI ingestAPI,
                              Submitter submitter) {
        this.context = context;
        this.aceConfiguration = aceConfiguration;
        this.smtpProperties = smtpProperties;
        this.replicationProperties = replicationProperties;
        this.properties = properties;
        this.ingestAPI = ingestAPI;
        this.submitter = submitter;

        printStart();
    }

    private void printStart() {
        log.info("Replication-Shell started in development mode with properties:");
        log.info("  ACE:");
        log.info("   am-endpoint : {}", aceConfiguration.getAm());
        log.info("   am-username : {}", aceConfiguration.getUsername());
        // log.info("   am-password : {}", aceConfiguration.getPassword());
        log.info("   am-audit-period : {}", aceConfiguration.getAuditPeriod());
        log.info("  SMTP:");
        log.info("   to : {}", smtpProperties.getTo());
        log.info("   send : {}", smtpProperties.getSend());
        log.info("  IngestAPI:");
        log.info("   api-endpoint : {}", properties.getEndpoint());
        log.info("   api-username : {}", properties.getUsername());
        // log.info("   api-password : {}", properties.getPassword());
        log.info("  Replication:");
        log.info("   node-name : {}", replicationProperties.getNode());
        log.info("   preservation-dir : {}", replicationProperties.getStorage().getPreservation());
        log.info("   smtp-send-on-success : {}", replicationProperties.getSmtp().getSendOnSuccess());
    }

    /**
     * Main entry point for the class, display the prompt and when we receive
     * the QUIT option, tell Spring to shutdown
     *
     */
    @Override
    public void replicate() {
        boolean done = false;
        while (!done) {
            OPTION option = inputOption();
            if (option.equals(OPTION.RESTFUL_QUERY)) {
                log.info("Query for active replications");
                query(STARTED, false);

                log.info("Query for new replications");
                query(ReplicationStatus.PENDING, true);
            } else if (option.equals(OPTION.QUIT)) {
                log.info("Quitting");
                done = true;
            }

        }
    }

    /**
     * Create a prompt and read the input for the next option
     *
     * @return the input given
     */
    private OPTION inputOption() {
        OPTION option = OPTION.UNKNOWN;
        while (option.equals(OPTION.UNKNOWN)) {
            StringBuilder sb = new StringBuilder("Enter Option: ");
            String sep = " | ";
            for (OPTION value : OPTION.values()) {
                if (!value.equals(OPTION.UNKNOWN)) {
                    sb.append(value.name());
                    sb.append(" [");
                    sb.append(value.name().charAt(0));
                    sb.append("]");
                    sb.append(sep);
                }
            }

            //The one difference, mwahhaha
            sb.replace(sb.length() - sep.length(), sb.length(), " -> ");
            System.out.println(sb.toString());
            option = OPTION.fromString(readLine().trim());
        }
        return option;
    }

    /**
     * Send queries to the ingest-server to receive ongoing replications
     *
     * @param status - the status of replications to query for
     * @param update - whether or not to update the requests while replicating
     */
    private void query(ReplicationStatus status, boolean update) {
        Map<String, Object> params = new HashMap<>();
        Page<Replication> replications;
        params.put("status", status);
        params.put("node", properties.getUsername());
        Call<PageImpl<Replication>> call = ingestAPI.getReplications(params);
        try {
            Response<PageImpl<Replication>> execute = call.execute();
            replications = execute.body();
        } catch (IOException e) {
            log.error("Error getting replications from server", e);
            return;
        }

        log.debug("Found {} replications", replications.getNumberOfElements());

        for (Replication replication : replications) {
            log.info("Starting job for replication id {}", replication.getId());
            submitter.submit(replication);
        }
    }

    /**
     * Read in from stdin
     *
     * @return the read input
     */
    private String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    private enum OPTION {
        RESTFUL_QUERY, QUIT, UNKNOWN;

        private static OPTION fromString(String text) {
            switch (text) {
                case "R":
                case "r":
                    return RESTFUL_QUERY;
                case "Q":
                case "q":
                    return QUIT;
                default:
                    return UNKNOWN;
            }
        }
    }

}
