package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.exception.ForbiddenException;
import org.chronopolis.ingest.models.ReplicationConfigUpdate;
import org.chronopolis.ingest.models.filter.StorageRegionFilter;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.StorageRegionService;
import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.RegionCreate;
import org.chronopolis.rest.models.RegionEdit;
import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;
import org.chronopolis.rest.support.StorageUnit;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.Optional;

/**
 * View controller for the StorageRegion pages
 *
 * @author shake
 */
@Controller
public class StorageRegionUIController extends IngestController {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private final Logger log = LoggerFactory.getLogger(StorageRegionUIController.class);
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final NodeRepository nodes;
    private final StorageRegionService service;

    @Autowired
    public StorageRegionUIController(NodeRepository nodes, StorageRegionService service) {
        this.nodes = nodes;
        this.service = service;
    }

    /**
     * Show a list of all StorageRegions
     *
     * @param model     the model for the controller
     * @param principal the security principal of the user
     * @param filter    the parameters to filter on
     * @return the template for listing StorageRegions
     */
    @GetMapping("/regions")
    public String getRegions(Model model, Principal principal, @ModelAttribute(value = "filter") StorageRegionFilter filter) {
        access.info("[GET /regions] - {}", principal.getName());

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
        model.addAttribute("formatter", new FileSizeFormatter());
        // enum types as well

        return "storage_region/regions";
    }

    /**
     * Return a form for creating a StorageRegion
     *
     * @param model     the model for the controller
     * @param principal the principal of the user
     * @return the template to create a StorageRegion
     */
    @GetMapping("/regions/create")
    public String createRegionForm(Model model, Principal principal, RegionCreate regionCreate) {
        access.info("[GET /regions/create] - {}", principal.getName());
        appendFormAttributes(model, regionCreate);
        return "storage_region/create";
    }

    /**
     * Process a request to create a StorageRegion
     *
     * @param model         the model of the controller
     * @param principal     the principal of the user
     * @param regionCreate  the RegionCreate form
     * @param bindingResult the form validation result
     * @return the newly created StorageRegion
     */
    @PostMapping("/regions")
    public String createRegion(Model model,
                               Principal principal,
                               @Valid RegionCreate regionCreate,
                               BindingResult bindingResult) throws ForbiddenException {
        access.info("[POST /regions] - {}", principal.getName());
        if (bindingResult.hasErrors()) {
            appendFormAttributes(model, regionCreate);
            return "storage_region/create";
        }
        access.info("POST parameters - {};{};{}", regionCreate.getNode(),
                regionCreate.getDataType(),
                regionCreate.getStorageType());

        Node owner;
        if (hasRoleAdmin() || principal.getName().equalsIgnoreCase(regionCreate.getNode())) {
            owner = nodes.findByUsername(regionCreate.getNode());
        } else {
            throw new ForbiddenException("User does not have permissions to create this resource");
        }

        StorageRegion region = new StorageRegion();
        region.setNote(regionCreate.getNote());
        region.setDataType(regionCreate.getDataType());
        region.setStorageType(regionCreate.getStorageType());
        region.setCapacity(regionCreate.getCapacity());
        region.setNode(owner);

        ReplicationConfig config = new ReplicationConfig();
        config.setServer(regionCreate.getReplicationServer());
        config.setPath(regionCreate.getReplicationPath());
        config.setUsername(regionCreate.getReplicationUser());
        config.setRegion(region);

        region.setReplicationConfig(config);
        service.save(region);

        return "redirect:/regions/" + region.getId();
    }

    /**
     * Return the form for updating a StorageRegion
     *
     * @param model      the model for the controller
     * @param principal  the principal of the user
     * @param id         the id of the StorageRegion
     * @param regionEdit the values of the form
     * @return the template for editing
     */
    @GetMapping("/regions/{id}/edit")
    public String editRegionForm(Model model, Principal principal, @PathVariable("id") Long id, RegionEdit regionEdit) {
        access.info("[GET /regions/{}/edit] - {}", id, principal.getName());
        model.addAttribute("dataTypes", DataType.values());
        model.addAttribute("storageTypes", StorageType.values());
        model.addAttribute("storageUnits", StorageUnit.values());
        model.addAttribute("regionEdit", regionEdit);
        return "storage_region/edit";
    }

    /**
     * Update a StorageRegion with the values of the regionEdit form
     *
     * @param model         the model of the controller
     * @param principal     the principal of the user
     * @param id            the id of the StorageRegion
     * @param regionEdit    the form
     * @param bindingResult the result of validation
     * @return the updated StorageRegion
     * @throws ForbiddenException if the user does not have permission to update the resource
     */
    @PostMapping("/regions/{id}/edit")
    public String editRegion(Model model,
                             Principal principal,
                             @PathVariable("id") Long id,
                             @Valid RegionEdit regionEdit,
                             BindingResult bindingResult) throws ForbiddenException {
        access.info("[POST /regions/{}/edit] - {}", id, principal.getName());
        if (bindingResult.hasErrors()) {

            bindingResult.getFieldErrors()
                    .forEach(error -> log.info("{}:{}", error.getField(), error.getDefaultMessage()));

            model.addAttribute("dataTypes", DataType.values());
            model.addAttribute("storageTypes", StorageType.values());
            model.addAttribute("storageUnits", StorageUnit.values());
            model.addAttribute("regionEdit", regionEdit);
            return "storage_region/edit";
        }

        access.info("POST parameters - {};{};{};{}", regionEdit.getCapacity(),
                regionEdit.getStorageUnit(),
                regionEdit.getDataType(),
                regionEdit.getStorageType());

        StorageRegion region = service.find(new StorageRegionSearchCriteria().withId(id));

        if (!hasRoleAdmin() && !principal.getName().equalsIgnoreCase(region.getNode().getUsername())) {
            throw new ForbiddenException("User does not have permissions to create this resource");
        }

        region.setNote(regionEdit.getNote());
        region.setDataType(regionEdit.getDataType());
        region.setStorageType(regionEdit.getStorageType());
        Double capacity = regionEdit.getCapacity() * Math.pow(1000, regionEdit.getStorageUnit().getPower());
        region.setCapacity(capacity.longValue());

        service.save(region);
        return "redirect:/regions/" + id;
    }

    /**
     * Append basic attributes to a model for use as form data
     *
     * @param model        the model of the controller
     * @param regionCreate the previous form data
     */
    private void appendFormAttributes(Model model, RegionCreate regionCreate) {
        model.addAttribute("nodes", nodes.findAll());
        model.addAttribute("dataTypes", DataType.values());
        model.addAttribute("storageUnits", StorageUnit.values());
        model.addAttribute("storageTypes", StorageType.values());
        model.addAttribute("regionCreate", regionCreate);
    }

    /**
     * Retrieve information for a single StorageRegion
     *
     * @param model     the model for the controller
     * @param principal the security principal of the user
     * @param id        the id of the StorageRegion
     * @return the template for displaying a StorageRegion
     */
    @GetMapping("/regions/{id}")
    public String getRegion(Model model, Principal principal, @PathVariable("id") Long id) {
        access.info("[GET /regions/{}] - {}", id, principal.getName());

        StorageRegionSearchCriteria criteria = new StorageRegionSearchCriteria().withId(id);
        StorageRegion region = service.find(criteria);
        BigDecimal bdCapacity = new BigDecimal(region.getCapacity());

        Optional<Long> usedRaw = service.getUsedSpace(region);
        FileSizeFormatter formatter = new FileSizeFormatter();
        String capacity = formatter.format(bdCapacity);
        String used = formatter.format(usedRaw.orElse(0L));
        int percent = usedRaw.map(BigDecimal::new)
                .map(ur -> ur.divide(bdCapacity, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100))
                        .intValue())
                .orElse(0);
        model.addAttribute("region", region);
        model.addAttribute("capacity", capacity);
        model.addAttribute("used", used);
        model.addAttribute("percent", percent);
        return "storage_region/region";
    }

    /**
     * Update the replication configuration information for a StorageRegion
     * <p>
     * Constraints still to do:
     * - if a user is not an admin && not the owning node -> 403
     *
     * @param model     the model for the controller
     * @param principal the security principal of the user
     * @param id        the id of the StorageRegion
     * @param update    the replication configuration information
     * @return the template for displaying a StorageRegion
     */
    @PostMapping("/regions/{id}/config")
    public String updateRegionConfig(Model model, Principal principal, @PathVariable("id") Long id, ReplicationConfigUpdate update) throws ForbiddenException {
        access.info("[POST /regions/{}/config] - {}", id, principal.getName());
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

    /**
     * Placeholder for a Storage Statistics page
     *
     * @return storage template
     */
    @GetMapping("/storage")
    public String storage() {
        access.info("[GET /storage]");
        return "storage/index";
    }
}
