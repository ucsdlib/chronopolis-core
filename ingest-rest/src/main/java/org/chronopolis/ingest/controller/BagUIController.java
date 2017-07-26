package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.exception.ForbiddenException;
import org.chronopolis.ingest.models.BagUpdate;
import org.chronopolis.ingest.models.ReplicationCreate;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.models.filter.ReplicationFilter;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.ReplicationSearchCriteria;
import org.chronopolis.ingest.repository.dao.ReplicationService;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.storage.Storage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus.DISTRIBUTE;

/**
 * Controller for handling bag/replication related requests
 *
 * Created by shake on 4/17/15.
 */
@Controller
public class BagUIController extends IngestController {
    private final Logger log = LoggerFactory.getLogger(BagUIController.class);
    private final Integer DEFAULT_PAGE_SIZE = 20;
    private final Integer DEFAULT_PAGE = 0;

    // final BagService bagService;
    final SearchService<Bag, Long, BagRepository> bagService;
    final ReplicationService replicationService;
    final TokenRepository tokenRepository;
    final NodeRepository nodeRepository;
    final IngestSettings settings;

    @Autowired
    public BagUIController(SearchService<Bag, Long, BagRepository> bagService,
                           ReplicationService replicationService,
                           TokenRepository tokenRepository,
                           NodeRepository nodeRepository,
                           IngestSettings settings) {
        this.bagService = bagService;
        this.replicationService = replicationService;
        this.tokenRepository = tokenRepository;
        this.nodeRepository = nodeRepository;
        this.settings = settings;
    }

    /**
     * Retrieve information about all bags
     *
     * @param model - the view model
     * @param principal - authentication information
     * @return page listing all bags
     */
    @RequestMapping(value= "/bags", method = RequestMethod.GET)
    public String getBags(Model model, Principal principal,
                          @ModelAttribute(value = "filter") BagFilter filter) {
        log.info("[GET /bags] - {}", principal.getName());

        BagSearchCriteria criteria = new BagSearchCriteria()
                .nameLike(filter.getName())
                .depositorLike(filter.getDepositor())
                .withStatuses(filter.getStatus());

        Sort.Direction direction = (filter.getDir() == null) ? Sort.Direction.ASC : Sort.Direction.fromStringOrNull(filter.getDir());
        Sort s = new Sort(direction, filter.getOrderBy());
        Page<Bag> bags = bagService.findAll(criteria, new PageRequest(filter.getPage(), DEFAULT_PAGE_SIZE, s));

        PageWrapper<Bag> pages = new PageWrapper<>(bags, "/bags", filter.getParameters());
        model.addAttribute("bags", bags);
        model.addAttribute("pages", pages);
        model.addAttribute("statuses", BagStatus.statusByGroup());

        return "bags";
    }

    /**
     * Get information about a single bag
     *
     * @param model - the view model
     * @param id - the id of the bag
     * @return page showing the individual bag
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.GET)
    public String getBag(Model model, Principal principal, @PathVariable("id") Long id) {
        log.info("[GET /bags/{}] - {}", id, principal.getName());

        BagSearchCriteria bsc = new BagSearchCriteria().withId(id);
        ReplicationSearchCriteria rsc = new ReplicationSearchCriteria().withBagId(id);

        model.addAttribute("bag", bagService.find(bsc));
        model.addAttribute("replications", replicationService.findAll(rsc,
                new PageRequest(DEFAULT_PAGE, DEFAULT_PAGE_SIZE)));
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagId(id));

        return "bag";
    }

    /**
     * Handler for updating a bag
     *
     * todo: constraint on updating the bag as a non-admin
     *
     * @param model - the viewmodel
     * @param id - id of the bag to update
     * @param update - the updated information
     * @return page showing the individual bag
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.POST)
    public String updateBag(Model model, Principal principal, @PathVariable("id") Long id, BagUpdate update) {
        log.info("[POST /bags/{}] - {}", id, principal.getName());
        // should just be a toString
        log.debug("POST parameters - {};{}", update.getLocation(), update.getStatus());

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        bag.setStatus(update.getStatus());
        bagService.save(bag);

        model.addAttribute("bags", bag);
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagId(id));

        return "bag";
    }

    /**
     * Invert the active flag for Storage in a Bag
     *
     * @param principal the principal of the user
     * @param id the id of the bag
     * @param type the type of Storage
     * @return the bag
     */
    @GetMapping(value = "/bags/{id}/storage/activate")
    public String updateBagStorage(Principal principal,
                                   @PathVariable("id") Long id,
                                   @ModelAttribute("type") String type) throws ForbiddenException {
        log.info("[GET /bags/{}/storage/activate] - {}", id, principal.getName());

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);

        Storage storage;
        if ("BAG".equalsIgnoreCase(type)) {
            storage = bag.getBagStorage();
        } else if ("TOKEN".equalsIgnoreCase(type)) {
            storage = bag.getTokenStorage();
        } else {
            // should have a related ExceptionHandler
            throw new RuntimeException("Invalid Type");
        }

        // could do a null check here...
        StorageRegion region = storage.getRegion();
        if (!hasRoleAdmin() && !region.getNode().getUsername().equalsIgnoreCase(principal.getName())) {
            throw new ForbiddenException("User is not allowed to update this resource");
        }

        storage.setActive(!storage.isActive());
        bagService.save(bag);
        return "redirect:/bags/" + bag.getId();
    }

    /**
     * Retrieve the page for adding bags
     *
     * @param model - the view model
     * @return page to add a bag
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.GET)
    public String addBag(Model model, Principal principal) {
        log.info("[GET /bags/add] - {}", principal.getName());
        model.addAttribute("nodes", nodeRepository.findAll());
        return "addbag";
    }

    /**
     * Handler for adding bags
     *
     * @param request - the request containing the bag name, depositor, and location
     * @return redirect to the bags page
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.POST)
    public String addBag(Principal principal, IngestRequest request) {
        log.info("[POST /bags/add] - {}", principal.getName());
        String name = request.getName();
        String depositor = request.getDepositor();
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(depositor)
                .withName(name);
        Bag bag = bagService.find(criteria);

        request.setRequiredReplications(request.getReplicatingNodes().size());

        // only add new bags
        if (bag == null) {
            bag = new Bag(name, depositor);
            // should be a Storage entity
            // bag.setFixityAlgorithm("SHA-256");
            // bag.setLocation(request.getLocation());
            bag.setCreator(principal.getName());

            if (request.getRequiredReplications() > 0) {
                bag.setRequiredReplications(request.getRequiredReplications());
            }

            createBagDistributions(bag, request.getReplicatingNodes());
        }

        bagService.save(bag);
        return "redirect:/bags/" + bag.getId();
    }

    // Copied from StagingController, unify both soon (before next release)
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

    //
    // Replication stuff

    /**
     * Get all replications
     * If admin, return a list of all replications
     * else return a list for the given user
     *
     * @param model - the viewmodel
     * @param principal - authentication information
     * @return the page listing all replications
     */
    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public String getReplications(Model model, Principal principal,
                                  @ModelAttribute(value = "filter") ReplicationFilter filter) {
        log.info("[GET /replications] - {}", principal.getName());

        Page<Replication> replications;
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .bagNameLike(filter.getBag())
                .nodeUsernameLike(filter.getNode())
                .withStatuses(filter.getStatus());

        Sort.Direction direction = (filter.getDir() == null) ? Sort.Direction.ASC : Sort.Direction.fromStringOrNull(filter.getDir());
        Sort s = new Sort(direction, filter.getOrderBy());
        replications = replicationService.findAll(criteria, new PageRequest(filter.getPage(), DEFAULT_PAGE_SIZE, s));

        model.addAttribute("replications", replications);
        model.addAttribute("statuses", ReplicationStatus.statusByGroup());
        model.addAttribute("pages", new PageWrapper<>(replications, "/replications", filter.getParameters()));

        return "replications";
    }

    @RequestMapping(value = "/replications/{id}", method = RequestMethod.GET)
    public String getReplication(Model model, Principal principal, @PathVariable("id") Long id) {
        log.info("[GET /replications/{}] - {}", id, principal.getName());
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withId(id);

        Replication replication = replicationService.find(criteria);
        log.info("Found replication {}::{}", replication.getId(), replication.getNode().getUsername());
        model.addAttribute("replication", replication);

        return "replication";
    }

    /**
     * Get all replications
     * If admin, return a list of all replications
     * else return a list for the given user
     *
     * @param model - the viewmodel
     * @param principal - authentication information
     * @return the addreplication page
     */
    @RequestMapping(value = "/replications/add", method = RequestMethod.GET)
    public String addReplications(Model model, Principal principal) {
        log.info("[GET /replications/add] - {}", principal.getName());
        model.addAttribute("bags", bagService.findAll(new BagSearchCriteria(), new PageRequest(0, 100)));
        model.addAttribute("nodes", nodeRepository.findAll());
        return "addreplication";
    }

    /**
     * Handle a request to create a replication from the Bag.id page
     *
     * @param model the model of the response
     * @param bag the bag id to create replications for
     * @return the create replication form
     */
    @RequestMapping(value = "/replications/create", method = RequestMethod.GET)
    public String createReplicationForm(Model model, Principal principal, @RequestParam("bag") Long bag) {
        log.info("[GET /replications/create] - {}", principal.getName());
        model.addAttribute("bag", bag);
        if (hasRoleAdmin()) {
            model.addAttribute("nodes", nodeRepository.findAll());
        } else {
            List<Node> nodes = new ArrayList<>();
            Node node = nodeRepository.findByUsername(principal.getName());
            if (node != null) {
                nodes.add(node);
            }
            model.addAttribute("nodes", nodes);
        }
        return "replications/create";
    }

    @RequestMapping(value = "/replications/create", method = RequestMethod.POST)
    public String createReplications(Principal principal, @ModelAttribute("form") ReplicationCreate form) {
        log.info("[POST /replications/create] - {}", principal.getName());
        final Long bag = form.getBag();
        form.getNodes().forEach(nodeId -> replicationService.create(bag, nodeId, settings));
        return "redirect:/replications/";
    }

    /**
     * Handler for adding bags
     *
     * @param request - the request containing the bag name, depositor, and location
     * @return redirect to all replications
     */
    @RequestMapping(value = "/replications/add", method = RequestMethod.POST)
    public String addReplication(Principal principal, ReplicationRequest request) {
        log.info("[POST /replications/add] - {}", principal.getName());
        Replication replication = replicationService.create(request, settings);
        return "redirect:/replications/" + replication.getId();
    }

}
