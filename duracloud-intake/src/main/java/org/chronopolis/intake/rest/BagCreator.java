/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.rest;

import org.chronopolis.intake.duracloud.batch.SnapshotJobManager;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.model.DuracloudRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sooo we want a vault, right?
 * bag-root/
 *    \-- committed/
 *       | [valid bags ready for transport to chron/dpn]
 *    \-- working/
 *       | [bags ready to be validated]
 *    \-- building/ # Is not read by the bag-vault
 *       | [raw items identified by bag-id]
 * 
 * Or maybe the BagModels will serve as the building phase, then the BagVault
 * will serve for it's standard committed/working stuff
 * 
 *
 * @author shake
 */
@RestController
@RequestMapping("/api/bag")
public class BagCreator {
    private static final Logger log = LoggerFactory.getLogger(BagCreator.class);

    @Autowired
    private IntakeSettings intakeSettings;

    @Autowired
    private SnapshotJobManager snapshotJobManager;

    /**
     * New method to initialize a snapshot for duracloud
     *
     * Steps to be taken:
     *   * Create the ChronPackage and BagModel
     *   * Register the manifest
     *   * Finalize the bag
     *
     * @param bag
     * @return HTTP 200 if a new bag, HTTP 400 if a duplicate
     */
    @RequestMapping(value = "snapshot", method = RequestMethod.POST)
    public ResponseEntity<String> snapshot(@RequestBody DuracloudRequest bag) {
        // TODO: Return status from the job manager
        snapshotJobManager.startSnapshotTasklet(bag);
        return new ResponseEntity<>("{\"status\": \"success\"}", HttpStatus.OK);
    }

}
