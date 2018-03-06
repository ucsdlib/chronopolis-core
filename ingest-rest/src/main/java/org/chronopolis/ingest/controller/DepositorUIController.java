package org.chronopolis.ingest.controller;

import com.google.common.collect.ImmutableSortedSet;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.models.DepositorContactCreate;
import org.chronopolis.rest.models.DepositorCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.security.Principal;
import java.util.Set;

/**
 * Controller to handle Depositor requests and things
 *
 * todo: Depositor Page
 *       - edit
 *       - add contact
 * todo: Depositor Edit {GET,POST}
 *
 * @author shake
 */
@Controller
public class DepositorUIController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(DepositorUIController.class);

    private final PagedDAO dao;
    private final EntityManager entityManager;

    @Autowired
    public DepositorUIController(PagedDAO dao, EntityManager entityManager) {
        this.dao = dao;
        this.entityManager = entityManager;
    }

    @GetMapping("/depositors")
    public String index(Model model, Principal principal) {
        return "depositors/index";
    }

    @GetMapping("/depositors/create")
    public String create(Model model, Principal principal) {
        // todo: add nodes
        // model.addAttribute("nodes", dao.findPage(QNode.class, new NodeFil))
        return "depositors/create";
    }

    @PostMapping("/depositors/create")
    public String createAction(Model model,
                               Principal principal,
                               @Valid DepositorCreate depositorCreate,
                               BindingResult bindingResult) {
        return "depositors/depositor";
    }

    @GetMapping("/depositors/list/{namespace}")
    public String getDepositor(Model model,
                               Principal principal,
                               @PathVariable("namespace") String namespace) {
        DepositorFilter filter = new DepositorFilter().setNamespace(namespace);
        model.addAttribute("depositor", dao.findOne(QDepositor.depositor, filter));
        return "depositors/depositor";
    }

    @GetMapping("/depositors/list")
    public String list(Model model,
                       Principal principal,
                       @ModelAttribute(value = "filter") DepositorFilter filter) {
        Page<Depositor> depositors = dao.findPage(QDepositor.depositor, filter);
        PageWrapper<Depositor> pages = new PageWrapper<>(depositors, "/depositors/list", filter.getParameters());
        model.addAttribute("depositors", depositors);
        model.addAttribute("pages", pages);
        return "depositors/list";
    }

    @GetMapping("/depositors/list/{namespace}/addContact")
    public String addContact(Model model,
                             Principal principal,
                             @PathVariable("namespace") String namespace,
                             DepositorContactCreate depositorContactCreate) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Set<String> regions = util.getSupportedRegions();
        ImmutableSortedSet<String> supportedRegions = ImmutableSortedSet.copyOf(regions);
        DepositorFilter filter = new DepositorFilter().setNamespace(namespace);
        model.addAttribute("regions", supportedRegions);
        model.addAttribute("depositor", dao.findOne(QDepositor.depositor, filter));
        model.addAttribute("depositorContactCreate", depositorContactCreate);
        return "depositors/add_contact";
    }

    @PostMapping("/depositors/list/{namespace}/addContact")
    public String addContactAction(Model model,
                                   Principal principal,
                                   @PathVariable("namespace") String namespace,
                                   @Valid DepositorContactCreate depositorContactCreate,
                                   BindingResult result) {
        return "depositors/depositor";
    }
}
