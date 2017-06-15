package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.BagUpdate;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
public class BagController extends IngestController {
    private final Logger log = LoggerFactory.getLogger(BagController.class);
    private final Integer DEFAULT_PAGE_SIZE = 20;
    private final Integer DEFAULT_PAGE = 0;

    // final BagService bagService;
    final SearchService<Bag, Long, BagRepository> bagService;
    final ReplicationService replicationService;
    final TokenRepository tokenRepository;
    final NodeRepository nodeRepository;
    final IngestSettings settings;

    @Autowired
    public BagController(SearchService<Bag, Long, BagRepository> bagService,
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
        log.info("Getting bags for user {}", principal.getName());

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
    public String getBag(Model model, @PathVariable("id") Long id) {
        log.info("Getting bag {}", id);

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
     * @param model - the viewmodel
     * @param id - id of the bag to update
     * @param update - the updated information
     * @return page showing the individual bag
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.POST)
    public String updateBag(Model model, @PathVariable("id") Long id, BagUpdate update) {
        log.info("Updating bag {}: status = {}", id, update.getStatus());

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
     * Retrieve the page for adding bags
     *
     * @param model - the view model
     * @return page to add a bag
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.GET)
    public String addBag(Model model) {
        model.addAttribute("nodes", nodeRepository.findAll());
        return "addbag";
    }

    /**
     * Handler for adding bags
     *
     * @param model - the view model
     * @param request - the request containing the bag name, depositor, and location
     * @return redirect to the bags page
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.POST)
    public String addBag(Model model, Principal principal, IngestRequest request) {
        log.info("Adding new bag");
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
            bag.setFixityAlgorithm("SHA-256");
            bag.setCreator(principal.getName());
            bag.setLocation(request.getLocation());

            if (request.getRequiredReplications() > 0) {
                bag.setRequiredReplications(request.getRequiredReplications());
            }

            createBagDistributions(bag, request.getReplicatingNodes());
        }

        bagService.save(bag);

        // TODO: Redirect to /bags/{id}
        return "redirect:/bags";
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
        log.info("Getting replications for user {}", principal.getName());

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
    public String getReplication(Model model, @PathVariable("id") Long id) {
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
        model.addAttribute("bags", bagService.findAll(new BagSearchCriteria(), new PageRequest(0, 100)));
        model.addAttribute("nodes", nodeRepository.findAll());
        return "addreplication";
    }

    /**
     * Handler for adding bags
     *
     * @param model - the view model
     * @param request - the request containing the bag name, depositor, and location
     * @return redirect to all replications
     */
    @RequestMapping(value = "/replications/add", method = RequestMethod.POST)
    public String addReplication(Model model, ReplicationRequest request) {
        log.info("Adding new replication from web ui");

        Replication replication = replicationService.create(request, settings);

        // TODO: replicatons/id
        return "redirect:/replications";
    }

}
