package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.CREATED_AFTER;
import static org.chronopolis.ingest.api.Params.CREATED_BEFORE;
import static org.chronopolis.ingest.api.Params.DEPOSITOR;
import static org.chronopolis.ingest.api.Params.NAME;
import static org.chronopolis.ingest.api.Params.SORT_BY_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_BY_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.SORT_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.STATUS;
import static org.chronopolis.ingest.api.Params.UPDATED_AFTER;
import static org.chronopolis.ingest.api.Params.UPDATED_BEFORE;
import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus.DISTRIBUTE;

/**
 * REST Controller for controlling actions associated with bags
 *
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api")
public class StagingController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(StagingController.class);

    private final NodeRepository nodeRepository;
    // private final BagService bagService;
    private final SearchService<Bag, Long, BagRepository> bagService;
    private final IngestSettings ingestSettings;

    @Autowired
    public StagingController(NodeRepository nodeRepository, SearchService<Bag, Long, BagRepository> bagService, IngestSettings ingestSettings) {
        this.nodeRepository = nodeRepository;
        this.bagService = bagService;
        this.ingestSettings = ingestSettings;
    }

    /**
     * Retrieve all the bags we know about
     *
     * @param params - Query parameters used for searching
     * @return all bags matching the query parameters
     */
    @RequestMapping(value = "bags", method = RequestMethod.GET)
    public Iterable<Bag> getBags(@RequestParam Map<String, String> params) {
        BagSearchCriteria criteria = new BagSearchCriteria()
                .createdAfter(params.getOrDefault(CREATED_AFTER, null))
                .createdBefore(params.getOrDefault(CREATED_BEFORE, null))
                .updatedAfter(params.getOrDefault(UPDATED_AFTER, null))
                .updatedBefore(params.getOrDefault(UPDATED_BEFORE, null))
                .withDepositor(params.containsKey(DEPOSITOR) ? params.get(DEPOSITOR) : null)
                .withName(params.containsKey(NAME) ? params.get(NAME) : null)
                .withStatus(params.containsKey(STATUS) ? BagStatus.valueOf(params.get(STATUS)) : null);

        return bagService.findAll(criteria, createPageRequest(params, valid()));
    }

    /**
     * Retrieve information about a single bag
     *
     * @param bagId - the bag id to retrieve
     * @return the bag specified by the id
     */
    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(@PathVariable("bag-id") Long bagId) {
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withId(bagId);

        Bag bag = bagService.find(criteria);
        if (bag == null) {
            throw new NotFoundException("bag/" + bagId);
        }
        return bag;
    }

    /**
     * Notification that a bag exists and is ready to be ingested into Chronopolis
     *
     * @param principal - authentication information
     * @param request - the request containing the bag name, depositor, and location of the bag
     * @return the bag created from the IngestRequest
     */
    @RequestMapping(value = "bags", method = RequestMethod.POST)
    public Bag stageBag(Principal principal, @RequestBody IngestRequest request)  {
        String name = request.getName();
        String depositor = request.getDepositor();

        BagSearchCriteria criteria = new BagSearchCriteria()
                .withName(name)
                .withDepositor(depositor);

        Bag bag = bagService.find(criteria);
        if (bag != null) {
            log.debug("Bag {} exists from depositor {}, skipping creation", name, depositor);
            return bag;
        }

        log.debug("Received ingest request {}", request);

        String fileName = request.getLocation();
        Path stage = Paths.get(ingestSettings.getBagStage());
        Path bagPath = stage.resolve(fileName);
        // Not sure what the point of this is, since the file name should be relative
        Path relPath = stage.relativize(bagPath);

        bag = new Bag(name, depositor);
        bag.setFixityAlgorithm("SHA-256");
        bag.setCreator(principal.getName());
        bag.setLocation(relPath.toString());

        if (request.getRequiredReplications() > 0) {
            bag.setRequiredReplications(request.getRequiredReplications());
        }

        createBagDistributions(bag, request.getReplicatingNodes());
        bagService.save(bag);

        return bag;
    }

    /**
     * Iterate through a list of node usernames and add them to the BagDistribution table
     * TODO: List<String> -> List<Node> for replicating nodes
     * TODO: Find a home for this
     *
     * @param bag The bag to create distributions for
     * @param replicatingNodes The nodes which the bag will be distributed to
     */
    private void createBagDistributions(Bag bag, List<String> replicatingNodes) {
        int numDistributions = 0;
        if (replicatingNodes == null) {
            replicatingNodes = new ArrayList<>();
        }

        for (String nodeName : replicatingNodes) {
            Node node = nodeRepository.findByUsername(nodeName);
            if (node != null) {
                log.debug("Creating requested dist record for {}", nodeName);
                bag.addDistribution(node, DISTRIBUTE);
                numDistributions++;
            } else {
                log.debug("Unable to make dist record for node {}: Not found", nodeName);
            }
        }

        // if the distributions is still less, set error?
    }


    /**
     * Return a map of valid parameters
     *
     * @return all valid query parameters for sorting
     */
    private Map<String, String> valid() {
        return ImmutableMap.of(
                SORT_BY_TOTAL_FILES, SORT_TOTAL_FILES,
                SORT_BY_SIZE, SORT_SIZE);
    }

}
