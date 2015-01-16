package org.chronopolis.common.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * TODO: Let's just have a list of endpoints instead of trying to split it up.
 * ex: [http://localhost:8080, https://naradev05.8443:ingest-rest]
 *
 * Created by shake on 12/1/14.
 */
@Component
public class IngestAPISettings {

    @Value("${ingest.api.endpoint:localhost}")
    private String ingestAPIHost;

    @Value("${ingest.api.path:ingest-rest}")
    private String ingestAPIPath;

    @Value("${ingest.api.port:8080}")
    private Integer ingestAPIPort;

    @Value("${ingest.api.username:umiacs}")
    private String ingestAPIUsername;

    @Value("${ingest.api.password:umiacs}")
    private String ingestAPIPassword;

    private List<String> ingestEndpoints;


    public String getIngestAPIHost() {
        return ingestAPIHost;
    }

    public void setIngestAPIHost(final String ingestAPIHost) {
        this.ingestAPIHost = ingestAPIHost;
    }

    public String getIngestAPIPath() {
        return ingestAPIPath;
    }

    public void setIngestAPIPath(final String ingestAPIPath) {
        this.ingestAPIPath = ingestAPIPath;
    }

    public Integer getIngestAPIPort() {
        return ingestAPIPort;
    }

    public void setIngestAPIPort(final Integer ingestAPIPort) {
        this.ingestAPIPort = ingestAPIPort;
    }

    public String getIngestAPIUsername() {
        return ingestAPIUsername;
    }

    public void setIngestAPIUsername(final String ingestAPIUsername) {
        this.ingestAPIUsername = ingestAPIUsername;
    }

    public String getIngestAPIPassword() {
        return ingestAPIPassword;
    }

    public void setIngestAPIPassword(final String ingestAPIPassword) {
        this.ingestAPIPassword = ingestAPIPassword;
    }

    public List<String> getIngestEndpoints() {
        return ingestEndpoints;
    }

    @Value("${ingest.api.endpoints:http://localhost}")
    public void setIngestEndpoints(String ingestEndpoints) {
        System.out.println("Splitting endpoints");
        String[] endpoints = ingestEndpoints.split(",");
        if (endpoints.length > 0) {
            this.ingestEndpoints = Arrays.asList(endpoints);
        }
    }
}
