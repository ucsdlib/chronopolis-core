package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.create.BagCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.ACTIVE;
import static org.chronopolis.ingest.api.Params.CREATED_AFTER;
import static org.chronopolis.ingest.api.Params.CREATED_BEFORE;
import static org.chronopolis.ingest.api.Params.CREATOR;
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

    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final BagService bagService;

    @Autowired
    public BagController(BagService bagService) {
        this.bagService = bagService;
    }

    /**
     * Retrieve all the bags we know about
     *
     * @param params Query parameters used for searching
     * @return all bags matching the query parameters
     */
    @GetMapping
    public Iterable<Bag> getBags(@RequestParam Map<String, String> params) {
        access.info("[GET /api/bags]");
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withName(params.getOrDefault(NAME, null))
                .withRegion(params.getOrDefault(REGION, null))
                .withCreator(params.getOrDefault(CREATOR, null))
                .withDepositor(params.getOrDefault(DEPOSITOR, null))
                .createdAfter(params.getOrDefault(CREATED_AFTER, null))
                .createdBefore(params.getOrDefault(CREATED_BEFORE, null))
                .updatedAfter(params.getOrDefault(UPDATED_AFTER, null))
                .updatedBefore(params.getOrDefault(UPDATED_BEFORE, null))
                .withActiveStorage(params.getOrDefault(ACTIVE, null))
                .withStatus(params.containsKey(STATUS) ? BagStatus.valueOf(params.get(STATUS)) : null);

        return bagService.findAll(criteria, createPageRequest(params, valid()));
    }

    /**
     * Retrieve information about a single bag
     *
     * @param id the bag id to retrieve
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
     * @param principal authentication information
     * @param request   the request containing the bag name, depositor, and location of the bag
     * @return HTTP 201 with the created Bag
     *         HTTP 400 if the request is not valid (depositor, region)
     *         HTTP 401 if the user is not authenticated
     *         HTTP 403 if the user is not authorized to create
     */
    @PostMapping
    public ResponseEntity<Bag> stageBag(Principal principal, @RequestBody BagCreate request) {
        access.info("[POST /api/bags/] - {}", principal.getName());
        access.info("POST parameters - {}", request.getDepositor(), request.getName(), request.getStorageRegion());
        BagCreateResult result = bagService.processRequest(principal.getName(), request);
        return result.getResponseEntity();
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
