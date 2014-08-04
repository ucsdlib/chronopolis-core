package org.chronopolis.common.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component representing chronopolis node settings
 *
 * Created by shake on 8/1/14.
 */
@Component
public class ChronopolisSettings {

    @Value("${chron.node:umiacs}")
    private String node;

    @Value("${chron.stage.bags:/tmp/bags}")
    private String bagStage;

    @Value("${chron.stage.tokens:/tmp/tokens}")
    private String tokenStage;

    @Value("${chron.preservation:/data/chronopolis}")
    private String preservation;

    public String getNode() {
        return node;
    }

    public void setNode(final String node) {
        this.node = node;
    }

    public String getBagStage() {
        return bagStage;
    }

    public void setBagStage(final String bagStage) {
        this.bagStage = bagStage;
    }

    public String getTokenStage() {
        return tokenStage;
    }

    public void setTokenStage(final String tokenStage) {
        this.tokenStage = tokenStage;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(final String preservation) {
        this.preservation = preservation;
    }
}
