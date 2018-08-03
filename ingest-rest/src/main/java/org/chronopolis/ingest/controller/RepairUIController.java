package org.chronopolis.ingest.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.AceCollections;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.ace.MonitoredItem;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.CollectionInfo;
import org.chronopolis.ingest.models.FulfillmentRequest;
import org.chronopolis.ingest.models.HttpError;
import org.chronopolis.ingest.models.filter.RepairFilter;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.RepairSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.kot.api.OkBasicInterceptor;
import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.entities.Node;
import org.chronopolis.rest.kot.entities.repair.Repair;
import org.chronopolis.rest.kot.models.create.RepairCreate;
import org.chronopolis.rest.kot.models.enums.AuditStatus;
import org.chronopolis.rest.kot.models.enums.RepairStatus;
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
import org.springframework.web.bind.annotation.PostMapping;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handle routing and querying for the Repair WebUI stuff
 *
 * Created by shake on 4/14/17.
 */
@Controller
public class RepairUIController extends IngestController {
    private final Integer DEFAULT_PAGE_SIZE = 20;

    private final Logger log = LoggerFactory.getLogger(RepairUIController.class);
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final NodeRepository nodes;
    private final BagService bags;
    private final SearchService<Repair, Long, RepairRepository> repairs;

    @Autowired
    public RepairUIController(BagService bService,
                              NodeRepository nodes,
                              SearchService<Repair, Long, RepairRepository> rService) {
        this.bags = bService;
        this.nodes = nodes;
        this.repairs = rService;
    }

    /**
     * Start a new repair request
     *
     * @return the add page
     */
    @GetMapping("/repairs/add")
    public String newRepairRequest() {
        access.info("[GET /repairs/add]");
        return "repair/add";
    }

    /**
     * Accept ACE credentials in order to query for corrupt collections
     *
     * @param model The model
     * @param session The http session of the user
     * @param credentials The credentials for ACE
     * @return the corrupted collections to choose from
     */
    @PostMapping("/repairs/ace")
    public String getCollections(Model model, HttpSession session, AceCredentials credentials) {
        access.info("[POST /repairs/ace]");
        // todo: remove gson
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class,
                        (JsonDeserializer<Date>)
                                (jsonElement, type, jsonDeserializationContext) -> new Date(jsonElement.getAsLong())
                ).create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(credentials.getEndpoint())
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new OkBasicInterceptor(
                                credentials.getUsername(),
                                credentials.getPassword()))
                        .build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        AceCollections service = retrofit.create(AceCollections.class);
        session.setAttribute("ace", service);
        Call<List<GsonCollection>> call = service.getCollections(null, true, null);
        log.trace("{}", call.request().url());
        try {
            Response<List<GsonCollection>> response = call.execute();
            if (response.isSuccessful()) {
                // ????? this would be pretty bad performance wise because of the db reads, but w/e
                List<GsonCollection> filtered = response.body().stream()
                        .filter(collection ->
                                bags.find(new BagSearchCriteria()
                                        .withName(collection.getName())
                                        .withDepositor(collection.getGroup())) != null)
                        .collect(Collectors.toList());
                model.addAttribute("collections", filtered);
            } else {
                HttpError error = new HttpError(response.code(), response.message());
                model.addAttribute("error", error);
            }
        } catch (IOException exception) {
            model.addAttribute("error", new HttpError(-1, exception.getMessage()));
        }

        return "repair/add";
    }

    /**
     * Retrieve invalid files defined by a collection
     *
     * @param model The model
     * @param principal The user's principal
     * @param session The user's http session
     * @param infoRequest Information about the ACE collection
     * @return page to select which files to choose
     */
    @PostMapping("/repairs/collection")
    public String getErrors(Model model, Principal principal, HttpSession session, CollectionInfo infoRequest) {
        access.info("[POST /repairs/collection] - {}", principal.getName());
        access.info("POST parameters - {};{}", infoRequest.getCollection(), infoRequest.getDepositor());
        AceCollections collections;
        Object o = session.getAttribute("ace");
        if (o == null || !(o instanceof AceCollections)) {
            return "repair/add";
        }

        List<Node> repairingNodes;
        if (hasRoleAdmin()) {
            repairingNodes = nodes.findAll();
        } else {
            repairingNodes = new ArrayList<>();
            repairingNodes.add(nodes.findByUsername(principal.getName()));
        }

        collections = (AceCollections) o;
        Long collectionId = infoRequest.id();
        // todo: helper method
        Call<List<MonitoredItem>> invalid = collections.getItems(collectionId, "I");
        log.info("{}", invalid.request().url());
        Call<List<MonitoredItem>> missing = collections.getItems(collectionId, "M");
        log.info("{}", missing.request().url());
        Call<List<MonitoredItem>> corrupt = collections.getItems(collectionId, "C");
        log.info("{}", corrupt.request().url());
        try {
            Response<List<MonitoredItem>> execute = invalid.execute();
            model.addAttribute("invalid", execute.body());
            execute = missing.execute();
            model.addAttribute("missing", execute.body());
            execute = corrupt.execute();
            model.addAttribute("corrupt", execute.body());
        } catch (IOException e) {
            log.error("", e);
        }

        model.addAttribute("collection", infoRequest.getCollection());
        model.addAttribute("depositor", infoRequest.getDepositor());
        model.addAttribute("nodes", repairingNodes);

        return "repair/select";
    }

    /**
     * Create a new repair request
     *
     * todo: still want to verify that the bag is nonnull (should be added to sql too)
     *
     * @param principal The user's principal
     * @param request The repair request to process
     * @return The newly created repair
     */
    @PostMapping("/repairs")
    public String requestRepair(Principal principal, RepairCreate request) {
        access.info("[POST /repairs] - {}", principal.getName());
        log.info("{} items requested", request.getFiles().size());
        Bag bag = bags.find(new BagSearchCriteria()
                .withDepositor(request.getDepositor())
                .withName(request.getCollection()));
        Optional<String> of = Optional.ofNullable(request.getTo());
        Node to = nodes.findByUsername(of.orElse(principal.getName()));

        Repair repair = new Repair(bag,
                to,
                null, // from_node -> set once we start to fulfill
                RepairStatus.REQUESTED,
                AuditStatus.PRE,
                null,  // fulfillment type -> determined by from_node
                null,  // fulfillment strategy -> determined by from_node
                principal.getName(),
                false, false, false);
        repair.setFiles(new HashSet<>());
        repair.addFilesFromRequest(request.getFiles());
        repairs.save(repair);

        return "redirect:/repairs/" + repair.getId();
    }

    /**
     * A listing of all repairs
     *
     * @param model the model
     * @param filter Parameters to filter on
     * @return The list of repairs
     */
    @GetMapping("/repairs")
    public String repairs(Model model, @ModelAttribute(value = "filter") RepairFilter filter) {
        access.info("[GET /repairs]");
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withTo(filter.getNode())
                .withFulfillingNode(filter.getFulfillingNode())
                .withStatuses(filter.getStatus())
                .withAuditStatuses(filter.getAuditStatus());

        // might be able to put Sort.Direction in the Paged class
        Sort.Direction direction = (filter.getDir() == null) ? Sort.Direction.DESC : Sort.Direction.fromStringOrNull(filter.getDir());
        Sort s = new Sort(direction, filter.getOrderBy());
        Page<Repair> results = repairs.findAll(criteria, new PageRequest(filter.getPage(), DEFAULT_PAGE_SIZE, s));

        PageWrapper<Repair> pages = new PageWrapper<>(results, "/repairs", filter.getParameters());

        model.addAttribute("repairs", results);
        model.addAttribute("pages", pages);
        model.addAttribute("repairStatus", RepairStatus.Companion.statusByGroup());
        model.addAttribute("auditStatus", AuditStatus.Companion.statusByGroup());
        return "repair/repairs";
    }

    /**
     * Information about a single repair
     *
     * @param model The model
     * @param id The id of the repair
     * @return the repair specified by id
     */
    @GetMapping("/repairs/{id}")
    public String repair(Model model, @PathVariable("id") Long id) {
        access.info("[GET /repairs/{}]", id);
        Repair repair = repairs.find(new RepairSearchCriteria().withId(id));
        model.addAttribute("repair", repair);
        if (repair.getStrategy() != null) {
            model.addAttribute(repair.getType().name().toLowerCase(), repair.getStrategy());
        }

        return "repair/repair";
    }

    /**
     * Return repairs which can be fulfilled
     *
     * @param model The model
     * @param principal The user's principal
     * @return Repairs to fulfill
     */
    @GetMapping("/repairs/fulfill")
    public String fulfillRepair(Model model, Principal principal) {
        access.info("[GET /repairs/fulfill]");
        // todo: either to withFromNot principal.getName
        //       or disable all radios w/ the selected from in the page
        Page<Repair> availableRepairs = repairs.findAll(new RepairSearchCriteria()
                        .withStatus(RepairStatus.REQUESTED),
                createPageRequest(new HashMap<>(), new HashMap<>()));
        List<Node> from;
        if (hasRoleAdmin()) {
            from = nodes.findAll();
        } else {
            from = new ArrayList<>();
            from.add(nodes.findByUsername(principal.getName()));
        }

        model.addAttribute("repairs", availableRepairs);
        model.addAttribute("from", from);
        return "repair/fulfill";
    }

    /**
     * Fulfill a repair
     *
     * @param model the model
     * @param principal the user's principal
     * @param request fulfillment information
     * @return the repair being fulfilled
     */
    @PostMapping("/repairs/fulfill")
    public String createFulfillment(Model model, Principal principal, FulfillmentRequest request) {
        access.info("[POST /repairs/fulfill] - ", principal.getName());
        log.info("{} offering to fulfill {}", request.getFrom(), request.getRepair());
        Repair repair = repairs.find(new RepairSearchCriteria().withId(request.getRepair()));
        String toNode = repair.getTo().getUsername();
        String fromNode = request.getFrom();

        if (repair.getFrom() == null && !Objects.equals(toNode, fromNode)) {
            repair.setFrom(nodes.findByUsername(fromNode));
            repair.setStatus(RepairStatus.STAGING);
            repairs.save(repair);
        }

        return "redirect:/repairs/" + repair.getId();
    }

}
