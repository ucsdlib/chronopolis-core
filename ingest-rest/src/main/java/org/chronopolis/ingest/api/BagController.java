package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.chronopolis.ingest.api.Params.CREATED_AFTER;
import static org.chronopolis.ingest.api.Params.CREATED_BEFORE;
import static org.chronopolis.ingest.api.Params.DEPOSITOR;
import static org.chronopolis.ingest.api.Params.NAME;
import static org.chronopolis.ingest.api.Params.REGION;
import static org.chronopolis.ingest.api.Params.SORT_BY_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_BY_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.SORT_SIZE;
import static org.chronopolis.ingest.api.Params.SORT_TOTAL_FILES;
import static org.chronopolis.ingest.api.Params.STATUS;
import static org.chronopolis.ingest.api.Params.UPDATED_AFTER;
import static org.chronopolis.ingest.api.Params.UPDATED_BEFORE;

/**
 * REST Controller for controlling actions associated with bags
 * <p>
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/bags")
public class BagController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(BagController.class);
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final BagService bagService;
    private final NodeRepository nodeRepository;
    private final SearchService<StorageRegion, Long, StorageRegionRepository> regions;

    @Autowired
    public BagController(NodeRepository nodeRepository,
                         BagService bagService,
                         SearchService<StorageRegion, Long, StorageRegionRepository> regions) {
        this.nodeRepository = nodeRepository;
        this.bagService = bagService;
        this.regions = regions;
    }

    /**
     * Retrieve all the bags we know about
     *
     * @param params - Query parameters used for searching
     * @return all bags matching the query parameters
     */
    @GetMapping
    public Iterable<Bag> getBags(@RequestParam Map<String, String> params) {
        access.info("[GET /api/bags]");
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withName(params.getOrDefault(NAME, null))
                .withRegion(params.getOrDefault(REGION, null))
                .withDepositor(params.getOrDefault(DEPOSITOR, null))
                .createdAfter(params.getOrDefault(CREATED_AFTER, null))
                .createdBefore(params.getOrDefault(CREATED_BEFORE, null))
                .updatedAfter(params.getOrDefault(UPDATED_AFTER, null))
                .updatedBefore(params.getOrDefault(UPDATED_BEFORE, null))
                .withActiveStorage(params.getOrDefault(Params.ACTIVE, null))
                .withStatus(params.containsKey(STATUS) ? BagStatus.valueOf(params.get(STATUS)) : null);

        return bagService.findAll(criteria, createPageRequest(params, valid()));
    }

    /**
     * Retrieve information about a single bag
     *
     * @param id - the bag id to retrieve
     * @return the bag specified by the id
     */
    @GetMapping("/{id}")
    public Bag getBag(@PathVariable("id") Long id) {
        access.info("[GET /api/bags/{}]", id);
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withId(id);

        Bag bag = bagService.find(criteria);
        if (bag == null) {
            throw new NotFoundException("bag/" + id);
        }
        return bag;
    }

    /**
     * Notification that a bag exists and is ready to be ingested into Chronopolis
     *
     * @param principal - authentication information
     * @param request   - the request containing the bag name, depositor, and location of the bag
     * @return the bag created from the IngestRequest
     */
    @PostMapping
    public Bag stageBag(Principal principal, @RequestBody IngestRequest request) {
        access.info("[POST /api/bags/] - {}", principal.getName());
        access.info("POST parameters - {}", request.getDepositor(), request.getName(), request.getStorageRegion());
        Long regionId = request.getStorageRegion();
        StorageRegion region = regions.find(new StorageRegionSearchCriteria().withId(regionId));
        if (region == null) {
            throw new BadRequestException("Invalid StorageRegion");
        }

        Set<Node> replicatingNodes = replicatingNodes(request.getReplicatingNodes());
        return bagService.create(principal.getName(), request, region, replicatingNodes);
    }

    /**
     * Helper to get Nodes from a list of node names. Eventually we'll want to do this
     * in the DB through a NodeService.
     *
     * @param nodeNames the node usernames to query
     * @return the set of Node entities found
     */
    private Set<Node> replicatingNodes(List<String> nodeNames) {
        Set<Node> replicatingNodes = new HashSet<>();
        if (nodeNames == null) {
            nodeNames = new ArrayList<>();
        }

        for (String name : nodeNames) {
            Node node = nodeRepository.findByUsername(name);
            if (node != null) {
                replicatingNodes.add(node);
            } else {
                log.debug("Node {} not found for distribution of bag!", name);
            }
        }

        return replicatingNodes;
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
