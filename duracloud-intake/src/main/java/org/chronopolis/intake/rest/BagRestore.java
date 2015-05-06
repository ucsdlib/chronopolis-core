package org.chronopolis.intake.rest;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.db.intake.StatusRepository;
import org.chronopolis.db.intake.model.Status;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.model.DuracloudRestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller to handle restore requests from Duraspace
 *
 * Created by shake on 7/10/14.
 */
@RestController
@RequestMapping("/api/restore")
public class BagRestore {
    private static final Logger log = LoggerFactory.getLogger(BagRestore.class);

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private IntakeSettings settings;

    @Autowired
    private MailUtil mailUtil;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity restore(@RequestBody DuracloudRestore restore) {
        String snapshotId = restore.getSnapshotID();

        // We go from: /export/duraspace/snapshot/storage/...
        // To........: snapshot/storage/...
        Path restoreBase = Paths.get(settings.getDuracloudRestoreStage());
        String location = restoreBase.relativize(Paths.get(restore.getLocation())).toString();

        // grab the depositor and collection name from the bagstatus
        // and forward that through to the ingest service
        Status status = statusRepository.findByBagId(snapshotId);
        ResponseEntity entity;

        if (status == null) {
            log.info("Bag {} not found", snapshotId);
            entity = new ResponseEntity(HttpStatus.NOT_FOUND);
        } else if (status.isReplicated()) {
            log.info("Bag {} found and is replicated", snapshotId);
            entity = new ResponseEntity(HttpStatus.OK);
            /*
            ChronMessage message = messageFactory.collectionRestoreRequestMessage(
                    status.getCollectionName(),
                    status.getDepositor(),
                    location
            );

            producer.send(message, RoutingKey.INGEST_BROADCAST.asRoute());
            */

            SimpleMailMessage smm = new SimpleMailMessage();
            smm.setFrom(mailUtil.getSmtpFrom());
            smm.setTo(mailUtil.getSmtpTo());
            smm.setSubject("Restoration Request");
            smm.setText("Restoration request for snapshot " + snapshotId
                    + "\n Associated depositor: " + status.getDepositor()
                    + "\n Associated collection: " + status.getCollectionName());
            mailUtil.send(smm);
        } else {
            log.info("Bag {} found and is not replicated", snapshotId);
            entity = new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        return entity;
    }

}
