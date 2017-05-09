package org.chronopolis.ingest.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.AceCollections;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.ace.MonitoredItem;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.CollectionInfo;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.RepairSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
import java.util.List;
import java.util.Map;

/**
 * Handle routing and querying for the Repair WebUI stuff
 *
 * todo: check that bags exist before adding
 * todo: move logic out of controller
 *
 * Created by shake on 4/14/17.
 */
@Controller
public class RepairUIController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(RepairUIController.class);

    private final NodeRepository nodes;
    private final SearchService<Bag, Long, BagRepository> bags;
    private final SearchService<Repair, Long, RepairRepository> repairs;

    @Autowired
    public RepairUIController(SearchService<Bag, Long, BagRepository> bService,
                              NodeRepository nodes,
                              SearchService<Repair, Long, RepairRepository> rService) {
        this.bags = bService;
        this.nodes = nodes;
        this.repairs = rService;
    }

    @RequestMapping(path = "/repairs/add", method = RequestMethod.GET)
    public String newRepairRequest() {
        return "repair/add";
    }

    @RequestMapping(path = "/repairs/ace", method = RequestMethod.POST)
    public String getCollections(Model model, HttpSession session, AceCredentials credentials) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class,
                        (JsonDeserializer<Date>)
                        (jsonElement, type, jsonDeserializationContext) -> new Date(jsonElement.getAsLong())
                ).create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(credentials.getEndpoint())
                .client(new OkHttpClient.Builder()
                        .addInterceptor(new OkBasicInterceptor(credentials.getUsername(),
                                credentials.getPassword()))
                        .build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        AceCollections service = retrofit.create(AceCollections.class);
        session.setAttribute("ace", service);
        Call<List<GsonCollection>> call = service.getCollections(null, true, null);
        log.info("{}", call.request().url());
        try {
            Response<List<GsonCollection>> response = call.execute();
            if (response.isSuccessful()) {
                model.addAttribute("collections", response.body());
            }
        } catch (IOException ignored) {
        }

        return "repair/add";
    }

    @RequestMapping(path = "/repairs/collection", method = RequestMethod.POST)
    public String getErrors(Model model, Principal principal, HttpSession session, CollectionInfo infoRequest) {
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

    @RequestMapping(path = "/repairs", method = RequestMethod.POST)
    public String requestRepair(Model model, Principal principal, RepairRequest request) {
        log.info("{} items requested", request.getFiles().size());
        Bag bag = bags.find(new BagSearchCriteria()
                .withDepositor(request.getDepositor())
                .withName(request.getCollection()));
        Node to = nodes.findByUsername(request.getTo().orElse(principal.getName()));
        Repair repair = new Repair();
        repair.setRequester(principal.getName());
        repair.setTo(to);
        repair.setFilesFromRequest(request.getFiles());
        repair.setBag(bag);
        repair.setStatus(RepairStatus.REQUESTED);
        repairs.save(repair);

        return "redirect:/repairs/" + repair.getId();
    }

    @RequestMapping(path = "/repairs", method = RequestMethod.GET)
    public String repairs(Model model) {
        Map<String, String> map = new HashMap<>();
        Page<Repair> repairs = this.repairs.findAll(new RepairSearchCriteria(), createPageRequest(map, map));
        PageWrapper<Repair> pages = new PageWrapper<>(repairs, "/repairs");

        model.addAttribute("repairs", repairs);
        model.addAttribute("pages", pages);
        return "repair/repairs";
    }

    @RequestMapping(path = "/repairs/{id}")
    public String repair(Model model, @PathVariable("id") Long id) {
        Repair repair = repairs.find(new RepairSearchCriteria().withId(id));
        model.addAttribute("repair", repair);

        return "repair/repair";
    }

    /*
    @RequestMapping("/repairs/new")
    public void corruptCollections() {
    }
    */


}
