package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.BagUpdate;
import org.chronopolis.ingest.repository.BagSearchCriteria;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.ReplicationSearchCriteria;
import org.chronopolis.ingest.repository.ReplicationService;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
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

    @Autowired
    BagService bagService;

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    ReplicationService replicationService;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    IngestSettings settings;

    // TODO: Param Map
    /**
     * Retrieve information about all bags
     *
     *
     * @param model - the view model
     * @param principal - authentication information
     * @return stringstring
     */
    @RequestMapping(value= "/bags", method = RequestMethod.GET)
    public String getBags(Model model, Principal principal,
                          @RequestParam(defaultValue = "0", required = false) Integer page,
                          @RequestParam(required = false) String depositor,
                          @RequestParam(required = false) BagStatus status) {
        log.info("Getting bags for user {}", principal.getName());

        BagSearchCriteria criteria = new BagSearchCriteria()
                .likeDepositor(depositor)
                // .likeName()
                .withStatus(status);

        Sort s = new Sort(Sort.Direction.ASC, "id");
        Page<Bag> bags = bagService.findBags(criteria, new PageRequest(page, DEFAULT_PAGE_SIZE, s));

        boolean start;
        StringBuilder url = new StringBuilder("/bags");
        // if we don't append anything, let start continue being true
        start = !append(url, "depositor", depositor, true);
        // we only want start to remain true if (start == true && append failed)
        append(url, "status", status, start);


        PageWrapper<Bag> pages = new PageWrapper<>(bags, url.toString());
        model.addAttribute("bags", bags);
        model.addAttribute("pages", pages);
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));

        return "bags";
    }

    private boolean append(StringBuilder url, String name, BagStatus status, boolean start) {
        if (status != null) {
            return append(url, name, status.name(), start);
        }

        return false;
    }

    /**
     * Get information about a single bag
     *
     * @param model - the view model
     * @param id - the id of the bag
     * @return
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.GET)
    public String getBag(Model model, @PathVariable("id") Long id) {
        log.info("Getting bag {}", id);

        // TODO: Could probably use model.addAllAttributes and use that for
        // common pages
        model.addAttribute("bags", bagService.findBag(id));
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
     * @return
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.POST)
    public String updateBag(Model model, @PathVariable("id") Long id, BagUpdate update) {
        log.info("Updating bag {}: status = {}", id, update.getStatus());

        Bag bag = bagService.findBag(id);
        bag.setStatus(update.getStatus());
        bagService.saveBag(bag);

        model.addAttribute("bags", bag);
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagId(id));

        return "bag";
    }

    /**
     * Retrieve the page for adding bags
     *
     * @param model - the view model
     * @return
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
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.POST)
    public String addBag(Model model, Principal principal, IngestRequest request) throws IOException {
        log.info("Adding new bag");
        String name = request.getName();
        String depositor = request.getDepositor();
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(depositor)
                .withName(name);
        Bag bag = bagService.findBag(criteria);

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
            /*
            try {
                initializeBag(bag, request);
            } catch (IOException e) {
                log.error("Error creating bag", e);
                throw e;
            }
            */
        }

        bagService.saveBag(bag);

        // TODO: Redirect to /bags/{id}?
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
     * @return
     */
    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public String getReplications(Model model, Principal principal,
                                  @RequestParam(defaultValue = "0", required = false) Integer page,
                                  @RequestParam(required = false) String node,
                                  @RequestParam(required = false) String bag,
                                  @RequestParam(required = false) ReplicationStatus status) {
        log.info("Getting replications for user {}", principal.getName());
        Page<Replication> replications;
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .likeNodeUsername(node)
                .likeBagName(bag)
                .withStatus(status);

        Sort s = new Sort(Sort.Direction.ASC, "id");
        replications = replicationService.getReplications(criteria, new PageRequest(page, DEFAULT_PAGE_SIZE, s));

        StringBuilder url = new StringBuilder("/replications");

        boolean start;
        // if we don't append anything, let start continue being true
        start = !append(url, "bag", bag, true);
        // we only want start to remain true if (start == true && append failed)
        start = (start && !append(url, "node", node, start));
        append(url, "status", status, start);

        model.addAttribute("replications", replications);
        model.addAttribute("statuses", Arrays.asList(ReplicationStatus.values()));
        model.addAttribute("pages", new PageWrapper<>(replications, url.toString()));

        return "replications";
    }

    private boolean append(StringBuilder builder, String name, ReplicationStatus value, boolean start) {
        if (value != null) {
            return append(builder, name, value.name(), start);
        }

        return false;
    }

    private boolean append(StringBuilder builder, String name, String value, boolean start) {
        if (value != null && !value.isEmpty()) {
            if (start) {
                builder.append("?");
            } else {
                builder.append("&");
            }
            builder.append(name).append("=").append(value);

            return true;
        }

        return false;
    }

    @RequestMapping(value = "/replications/{id}", method = RequestMethod.GET)
    public String getReplication(Model model, @PathVariable("id") Long id) {
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withId(id);

        Replication replication = replicationService.getReplication(criteria);
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
     * @return
     */
    @RequestMapping(value = "/replications/add", method = RequestMethod.GET)
    public String addReplications(Model model, Principal principal) {
        model.addAttribute("bags", bagService.findBags(new BagSearchCriteria(), new PageRequest(0, 100)));
        model.addAttribute("nodes", nodeRepository.findAll());
        return "addreplication";
    }

    /**
     * Handler for adding bags
     *
     * @param model - the view model
     * @param request - the request containing the bag name, depositor, and location
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/replications/add", method = RequestMethod.POST)
    public String addReplication(Model model, ReplicationRequest request) throws IOException {
        log.info("Adding new replication from web ui");

        Replication replication = replicationService.create(request, settings);

        // TODO: replicatons/id
        return "redirect:/replications";
    }

}
