package org.chronopolis.common.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Created by shake on 9/30/14.
 */
@Component
public class DPNSettings {
    private final Logger log = LoggerFactory.getLogger(DPNSettings.class);

    @Value("${dpn.api-key:admin}")
    private String apiKey;

    private List<String> dpnEndpoints;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getDPNEndpoints() {
        return dpnEndpoints;
    }

    @Value("${dpn.endpoints:http://localhost}")
    public void setIngestEndpoints(String ingestEndpoints) {
        log.debug("Splitting dpn endpoints");
        String[] endpoints = ingestEndpoints.split(",");
        log.debug("Found {} endpoints: {}", endpoints.length, endpoints);
        if (endpoints.length > 0) {
            this.dpnEndpoints = Arrays.asList(endpoints);
        }
    }

}
