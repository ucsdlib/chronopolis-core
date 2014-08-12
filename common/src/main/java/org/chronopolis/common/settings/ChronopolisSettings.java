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

    @Value("${chron.restore:/scratch1/restore}")
    private String restore;

    /**
     * Get the node name where the chronopolis service is running at
     *
     * i.e.: ncar, sdsc, umiacs
     *
     * @return
     */
    public String getNode() {
        return node;
    }

    public void setNode(final String node) {
        this.node = node;
    }

    /**
     * Get the stage where bags are held
     *
     * @return
     */
    public String getBagStage() {
        return bagStage;
    }

    public void setBagStage(final String bagStage) {
        this.bagStage = bagStage;
    }

    /**
     * Get the stage where tokens are held
     *
     * @return
     */
    public String getTokenStage() {
        return tokenStage;
    }

    public void setTokenStage(final String tokenStage) {
        this.tokenStage = tokenStage;
    }

    /**
     * Get the location where bags are preserved
     *
     * @return
     */
    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(final String preservation) {
        this.preservation = preservation;
    }

    /**
     * Get the stage where restorations will be put
     *
     * @return
     */
    public String getRestore() {
        return restore;
    }

    public void setRestore(final String restore) {
        this.restore = restore;
    }
}
