package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.models.DepositorContactCreate;
import org.chronopolis.rest.models.DepositorCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Controller to handle Depositor requests and things
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
        return "depositors/depositor";
    }

    @GetMapping("/depositors/list/{namespace}")
    public String list(Model model,
                       Principal principal,
                       @ModelAttribute(value = "filter") DepositorFilter filter) {
        return "depositors/list";
    }

    @GetMapping("/depositors/list/{namespace}/addContact")
    public String addContact(Model model,
                             Principal principal,
                             @PathVariable("namespace") String namespace) {
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
