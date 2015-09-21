package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.BagSearchCriteria;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.BagInitializer.initializeBag;
import static org.chronopolis.ingest.api.Params.DEPOSITOR;
import static org.chronopolis.ingest.api.Params.NAME;
import static org.chronopolis.ingest.api.Params.SORT_BY_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_BY_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.SORT_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.STATUS;

/**
 * REST Controller for controlling actions associated with bags
 *
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api")
public class StagingController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(StagingController.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    BagService bagService;

    @Autowired
    IngestSettings ingestSettings;

    /**
     * Retrieve all the bags we know about
     *
     * @param principal - authentication information
     * @param params - Query parameters used for searching
     * @return
     */
    @RequestMapping(value = "bags", method = RequestMethod.GET)
    public Iterable<Bag> getBags(Principal principal,
                                 @RequestParam Map<String, String> params) {
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(params.containsKey(DEPOSITOR) ? params.get(DEPOSITOR) : null)
                .withName(params.containsKey(NAME) ? params.get(NAME) : null)
                .withStatus(params.containsKey(STATUS) ? BagStatus.valueOf(params.get(STATUS)) : null);

        return bagService.findBags(criteria, createPageRequest(params, valid()));
    }

    /**
     * Retrieve information about a single bag
     *
     * @param principal - authentication information
     * @param bagId - the bag id to retrieve
     * @return
     */
    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(Principal principal, @PathVariable("bag-id") Long bagId) {
        Bag bag = bagRepository.findOne(bagId);
        if (bag == null) {
            throw new NotFoundException("bag/" + bagId);
        }
        return bag;
    }

    /*
    @RequestMapping(value = "bags/{bag-id}/dist/{node-id}", method = RequestMethod.POST)
    public BagDistribution distmag(@PathVariable("bag-id") Long bagId,
                       @PathVariable("node-id") Long nodeId) {
        Bag bag = bagRepository.findOne(bagId);
        Node node = nodeRepository.findOne(nodeId);
        BagDistribution dist = new BagDistribution();
        dist.setBag(bag);
        dist.setNode(node);
        dist.setStatus(BagDistribution.BagDistributionStatus.DISTRIBUTE);
        bag.addDistribution(dist);
        bagRepository.save(bag);
        return dist;
    }
    */

    /**
     * Notification that a bag exists and is ready to be ingested into Chronopolis
     *
     * @param principal - authentication information
     * @param request - the request containing the bag name, depositor, and location of the bag
     * @return
     */
    @RequestMapping(value = "bags", method = RequestMethod.POST)
    public Bag stageBag(Principal principal, @RequestBody IngestRequest request)  {
        String name = request.getName();
        String depositor = request.getDepositor();

        Bag bag = bagRepository.findByNameAndDepositor(name, depositor);
        if (bag != null) {
            log.debug("Bag {} exists from depositor {}, skipping creation", name, depositor);
            return bag;
        }

        bag = new Bag(name, depositor);
        try {
            initializeBag(bag, request);
        } catch (IOException e) {
            log.error("Error initializing bag {}:{}", depositor, name);
            return null;
        }

        bagRepository.save(bag);

        return bag;
    }

    /**
     * Return a map of valid parameters
     *
     * @return
     */
    private Map<String, String> valid() {
        return ImmutableMap.of(
                SORT_BY_TOTAL_FILES, SORT_TOTAL_FILES,
                SORT_BY_SIZE, SORT_SIZE);
    }

}
