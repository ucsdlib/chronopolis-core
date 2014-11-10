package org.chronopolis.ingest.api;

import org.chronopolis.ingest.ChronPackager;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.model.Bag;
import org.chronopolis.ingest.model.IngestRequest;
import org.chronopolis.ingest.repository.BagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
public class StagingController {

    @Autowired
    BagRepository bagRepository;

    @Autowired
    IngestSettings ingestSettings;

    @RequestMapping(value = "bags", method = RequestMethod.GET)
    public Iterable<Bag> getBags() {
        return bagRepository.findAll();
    }

    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(@PathVariable("bag-id") Long bagId) {
        return bagRepository.findOne(bagId);
    }

    @RequestMapping(value = "bag", method = RequestMethod.PUT)
    public String stageBag(@RequestBody IngestRequest request) {
        ChronPackager packager = new ChronPackager(request.getName(),
                request.getFileName(),
                request.getDepositor(),
                ingestSettings);
        bagRepository.save(packager.packageForChronopolis());
        return "ok";
    }

}
