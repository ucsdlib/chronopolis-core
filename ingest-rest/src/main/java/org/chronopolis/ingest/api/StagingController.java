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
import org.chronopolis.rest.models.Node;
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

import static org.chronopolis.ingest.api.Params.DEPOSITOR;
import static org.chronopolis.ingest.api.Params.NAME;
import static org.chronopolis.ingest.api.Params.SORT_BY_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_BY_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.SORT_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.STATUS;
import static org.chronopolis.rest.models.BagDistribution.BagDistributionStatus.DISTRIBUTE;

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

        String fileName = request.getLocation();
        Path stage = Paths.get(ingestSettings.getBagStage());
        Path bagPath = stage.resolve(fileName);
        // Not sure what the point of this is, since the file name should be relative
        Path relPath = stage.relativize(bagPath);

        bag = new Bag(name, depositor);
        bag.setFixityAlgorithm("SHA-256");
        bag.setLocation(relPath.toString());

        if (request.getRequiredReplications() > 0) {
            bag.setRequiredReplications(request.getRequiredReplications());
        }

        createBagDistributions(bag, request.getReplicatingNodes());
        bagRepository.save(bag);

        return bag;
    }

    /**
     * Iterate through a list of node usernames and add them to the BagDistribution table
     * TODO: List<String> -> List<Node> for replicating nodes
     * TODO: Find a home for this
     *
     * @param bag
     * @param replicatingNodes
     */
    private void createBagDistributions(Bag bag, List<String> replicatingNodes) {
        int numDistributions = 0;
        if (replicatingNodes == null) {
            replicatingNodes = new ArrayList<>();
        }

        for (String nodeName : replicatingNodes) {
            Node node = nodeRepository.findByUsername(nodeName);
            if (node != null) {
                log.debug("Creating dist record for {}", nodeName);
                bag.addDistribution(node, DISTRIBUTE);
                numDistributions++;
            }
        }

        if (numDistributions < bag.getRequiredReplications()) {
            for (Node node : nodeRepository.findAll()) {
                log.debug("Creating dist record for {}", node.getUsername());
                bag.addDistribution(node, DISTRIBUTE);
                numDistributions++;
            }
        }

        // if the distributions is still less, set error?
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
