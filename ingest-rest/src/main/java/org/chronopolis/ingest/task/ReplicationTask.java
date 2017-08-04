package org.chronopolis.ingest.task;

import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.dao.ReplicationService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple task to create replications for bags which have finished tokenizing
 *
 * Created by shake on 2/13/15.
 */
@Component
@EnableScheduling
public class ReplicationTask {
    private final Logger log = LoggerFactory.getLogger(ReplicationTask.class);

    private final BagRepository bagRepository;
    private final ReplicationService service;

    @Autowired
    public ReplicationTask(BagRepository bagRepository, ReplicationService service) {
        this.bagRepository = bagRepository;
        this.service = service;
    }

    @Scheduled(cron = "${ingest.cron.request:0 */10 * * * *}")
    public void createReplications() {
        Collection<Bag> bags = bagRepository.findByStatus(BagStatus.TOKENIZED);

        for (Bag bag : bags) {
            List<Replication> replications = bag.getDistributions().stream()
                    .map(dist -> service.create(dist.getBag(), dist.getNode()))
                    .collect(Collectors.toList());

            if (replications.size() == bag.getDistributions().size() && replications.size() != 0) {
                log.debug("{} updating status to replicating", bag.getName());
                bag.setStatus(BagStatus.REPLICATING);
                bagRepository.save(bag);
            }
        }

    }

}
