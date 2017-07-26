package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.exception.ForbiddenException;
import org.chronopolis.ingest.models.RegionCreate;
import org.chronopolis.ingest.models.ReplicationConfigUpdate;
import org.chronopolis.ingest.models.filter.StorageRegionFilter;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.security.Principal;

@Controller
public class StorageRegionUIController extends IngestController {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private final Logger log = LoggerFactory.getLogger(StorageRegionUIController.class);


    private final NodeRepository nodes;
    private final SearchService<StorageRegion, Long, StorageRegionRepository> service;

    @Autowired
    public StorageRegionUIController(NodeRepository nodes, SearchService<StorageRegion, Long, StorageRegionRepository> service) {
        this.nodes = nodes;
        this.service = service;
    }

    /**
     * Show a list of all StorageRegions
     *
     * @param model the model for the controller
     * @param principal the security principal of the user
     * @param filter the parameters to filter on
     * @return the template for listing StorageRegions
     */
    @GetMapping("/regions")
    public String getRegions(Model model, Principal principal, @ModelAttribute(value = "filter") StorageRegionFilter filter) {
        log.info("[GET /regions] - {}", principal.getName());

        StorageRegionSearchCriteria criteria = new StorageRegionSearchCriteria()
                .withCapacityGreaterThan(filter.getCapacityGreater())
                .withCapacityLessThan(filter.getCapacityLess())
                .withStorageType(filter.getType())
                .withNodeName(filter.getName());

        Sort.Direction direction = (filter.getDir() == null) ? Sort.DEFAULT_DIRECTION : Sort.Direction.fromString(filter.getDir());
        Sort sort = new Sort(direction, filter.getOrderBy());
        Page<StorageRegion> regions = service.findAll(criteria, new PageRequest(filter.getPage(), DEFAULT_PAGE_SIZE, sort));
        PageWrapper<StorageRegion> pages = new PageWrapper<>(regions, "/regions", filter.getParameters());

        model.addAttribute("regions", regions);
        model.addAttribute("pages", pages);
        model.addAttribute("storageTypes", StorageType.values());
        // enum types as well

        return "storage_region/regions";
    }

    /**
     * Return a form for creating a StorageRegion
     *
     * @param model the model for the controller
     * @param principal the principal of the user
     * @return the template to create a StorageRegion
     */
    @GetMapping("/regions/create")
    public String createRegionForm(Model model, Principal principal, RegionCreate createForm) {
        log.info("[GET /regions/create] - {}", principal.getName());

        model.addAttribute("nodes", nodes.findAll());
        model.addAttribute("storageTypes", StorageType.values());
        model.addAttribute("dataTypes", DataType.values());

        return "storage_region/create";
    }

    /**
     * Process a request to create a StorageRegion
     *
     * todo: bad requests/conflict?/forbidden
     *
     * @param model the model of the controller
     * @param principal the principal of the user
     * @param create the RegionCreate form
     * @param bindingResult the form validation result
     * @return the newly created StorageRegion
     */
    @PostMapping("/regions")
    public String createRegion(Model model, Principal principal, @Valid RegionCreate create, BindingResult bindingResult) throws ForbiddenException {
        if (bindingResult.hasErrors()) {
            return "redirect:/regions/create";
        }

        Node owner;
        if (hasRoleAdmin() || principal.getName().equalsIgnoreCase(create.getNode())) {
            owner = nodes.findByUsername(create.getNode());
        } else {
            throw new ForbiddenException("User does not have permissions to create this resource");
        }

        StorageRegion region = new StorageRegion();
        region.setDataType(create.getDataType());
        region.setStorageType(create.getStorageType());
        region.setCapacity(create.getCapacity());
        region.setNode(owner);

        ReplicationConfig config = new ReplicationConfig();
        config.setServer(create.getReplicationServer());
        config.setPath(create.getReplicationPath());
        config.setUsername(create.getReplicationUser());
        config.setRegion(region);

        region.setReplicationConfig(config);
        service.save(region);

        return "redirect:/regions/" + region.getId();
    }

    /**
     * Retrieve information for a single StorageRegion
     *
     * @param model the model for the controller
     * @param principal the security principal of the user
     * @param id the id of the StorageRegion
     * @return the template for displaying a StorageRegion
     */
    @GetMapping("/regions/{id}")
    public String getRegion(Model model, Principal principal, @PathVariable("id") Long id) {
        log.info("[GET /regions/{}] - {}", id, principal.getName());

        StorageRegionSearchCriteria criteria = new StorageRegionSearchCriteria().withId(id);
        StorageRegion region = service.find(criteria);
        model.addAttribute("region", region);
        return "storage_region/region";
    }

    /**
     * Update the replication configuration information for a StorageRegion
     *
     * Constraints still to do:
     *   - if a user is not an admin && not the owning node -> 403
     *
     * @param model the model for the controller
     * @param principal the security principal of the user
     * @param id the id of the StorageRegion
     * @param update the replication configuration information
     * @return the template for displaying a StorageRegion
     */
    @PostMapping("/regions/{id}/config")
    public String updateRegionConfig(Model model, Principal principal, @PathVariable("id") Long id, ReplicationConfigUpdate update) throws ForbiddenException {
        log.info("[POST /regions/{}/config] - {}", id, principal.getName());
        StorageRegionSearchCriteria criteria = new StorageRegionSearchCriteria().withId(id);
        StorageRegion region = service.find(criteria);
        Node owner = region.getNode();

        if (!hasRoleAdmin() && !principal.getName().equalsIgnoreCase(owner.getUsername())) {
            throw new ForbiddenException("User does not have permissions to update this resource");
        }

        ReplicationConfig config = region.getReplicationConfig();
        if (config == null) {
            config = new ReplicationConfig();
            // A better way to do this...?
            region.setReplicationConfig(config);
            config.setRegion(region);
        }
        config.setPath(update.getPath());
        config.setServer(update.getServer());
        config.setUsername(update.getUsername());
        service.save(region);

        model.addAttribute("region", region);
        return "storage_region/region";
    }
}
