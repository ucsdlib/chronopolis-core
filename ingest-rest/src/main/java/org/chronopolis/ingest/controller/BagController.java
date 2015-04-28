package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.models.BagUpdate;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by shake on 4/17/15.
 */
@Controller
public class BagController {
    private final Logger log = LoggerFactory.getLogger(BagController.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    IngestSettings settings;

    @RequestMapping(value= "/bags", method = RequestMethod.GET)
    public String getBags(Model model, Principal principal) {
        log.info("Getting bags for user {}", principal.getName());

        Collection<Bag> bags = bagRepository.findAll();
        model.addAttribute("bags", bags);

        /*
        log.debug("Adding count for bags");
        for (Bag bag : bags) {
            model.addAttribute(String.valueOf(bag.getID()),
                               tokenRepository.countByBagID(bag.getID()));
        }
        log.debug("Done adding count");
        */

        return "bags";
    }

    @RequestMapping(value = "/bags/{id}", method = RequestMethod.GET)
    public String getBag(Model model, @PathVariable("id") Long id) {
        log.info("Getting bag {}", id);

        // TODO: Could probably use model.addAllAttributes and use that for
        // common pages
        model.addAttribute("bags", bagRepository.findOne(id));
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagID(id));

        return "bag";
    }

    @RequestMapping(value = "/bags/{id}", method = RequestMethod.POST)
    public String updateBag(Model model, @PathVariable("id") Long id, BagUpdate update) {
        log.info("Updating bag {}: status = {}", id, update.getStatus());

        Bag bag = bagRepository.findOne(id);
        bag.setStatus(update.getStatus());
        bagRepository.save(bag);

        model.addAttribute("bags", bag);
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagID(id));

        return "bag";
    }

    @RequestMapping(value = "/bags/add", method = RequestMethod.GET)
    public String addBag(Model model) {
        return "addbag";
    }

    @RequestMapping(value = "/bags/add", method = RequestMethod.POST)
    public String addBag(Model model, IngestRequest request) throws IOException {
        log.info("Adding new bag");
        String name = request.getName();
        String depositor = request.getDepositor();
        Bag bag = bagRepository.findByNameAndDepositor(name, depositor);
        // only add new bags
        if (bag == null) {
            bag = new Bag(name, depositor);
            try {
                initializeBag(bag, request.getLocation());
            } catch (IOException e) {
                log.error("Error creating bag", e);
                throw e;
            }
        }
        bagRepository.save(bag);

        return "redirect:/bags";
    }

    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public String getReplications(Model model, Principal principal) {
        log.info("Getting replications for user {}", principal.getName());
        Collection<Replication> replications;
        if (ControllerUtil.hasRoleAdmin()) {
            replications = replicationRepository.findAll();
        } else {
            replications = replicationRepository.findByNodeUsername(principal.getName());
        }

        model.addAttribute("replications", replications);
        return "replications";
    }

    // I pulled this from the StagingController, it really doesn't need to be duplicated
    // TODO: Either figure out how to get the addbag html page to use the rest api or
    //       find a place so this can be shared code
    public void initializeBag(Bag bag, String filename) throws IOException {
        Path stage = Paths.get(settings.getBagStage());
        Path bagPath = stage.resolve(filename);

        // TODO: Get these passed in the ingest request
        final long[] bagSize = {0};
        final long[] fileCount = {0};
        Files.walkFileTree(bagPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileCount[0]++;
                bagSize[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });

        Path relBag = stage.relativize(bagPath);
        bag.setLocation(relBag.toString());
        bag.setSize(bagSize[0]);
        bag.setTotalFiles(fileCount[0]);
        bag.setFixityAlgorithm("SHA-256");
    }


}
