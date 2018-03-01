package org.chronopolis.ingest.api;

import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.models.DepositorCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.stream.Collectors;

/**
 * API implementation for Depositors
 * <p>
 * todo: Validate DepositorModel
 *
 * @author shake
 */
@RestController
@RequestMapping("/api/depositors")
public class DepositorController {

    private final Logger log = LoggerFactory.getLogger(DepositorController.class);
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final PagedDAO dao;

    @Autowired
    public DepositorController(PagedDAO dao) {
        this.dao = dao;
    }

    /**
     * Retrieve all Depositors held by the Ingest Server
     *
     * @return HTTP 200 with a list of all Depositors
     *         HTTP 401 if the user is not authenticated
     */
    @GetMapping
    public ResponseEntity<Iterable<Depositor>> depositors(@ModelAttribute DepositorFilter filter) {
        return ResponseEntity.ok(dao.findPage(QDepositor.depositor, filter));
    }

    /**
     * Create a Depositor in the Ingest Server
     *
     * @param principal the security principal of the user
     * @param depositor the Depositor to create
     * @return HTTP 201 if the Depositor was created successfully with the Depositor as the response
     *         HTTP 400 if the DepositorCreate is not valid (bad phone number, missing fields, etc)
     *         HTTP 401 if the user is not authenticated
     *         HTTP 403 if the user requesting the create does not have permission
     *         HTTP 409 if the Depositor already exists
     */
    @PostMapping
    public ResponseEntity<Depositor> createDepositor(Principal principal,
                                                     @RequestBody DepositorCreate depositor) {
        access.info("[POST /api/depositors] - {}", principal.getName());
        Depositor entity = new Depositor()
                .setNamespace(depositor.getNamespace())
                .setOrganizationAddress(depositor.getOrganizationAddress())
                .setSourceOrganization(depositor.getSourceOrganization())
                .setContacts(depositor.getContacts().stream().map(contact -> new DepositorContact()
                        .setContactName(contact.getName())
                        .setContactEmail(contact.getEmail() )
                        .setContactPhone(contact.getPhoneNumber().toString()))
                        .collect(Collectors.toSet()));

        dao.save(entity);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieve a Depositor identified by their namespace
     *
     * @param namespace the namespace of the Depositor
     * @return HTTP 200 if the Depositor is found with the Depositor as the response body
     *         HTTP 401 if the user is not authenticated
     *         HTTP 404 if the Depositor is not found
     */
    @GetMapping("/{namespace}")
    public ResponseEntity<Depositor> depositor(@PathVariable("namespace") String namespace) {
        ResponseEntity<Depositor> response = ResponseEntity.notFound().build();

        Depositor depositor = dao.findOne(QDepositor.depositor,
                new DepositorFilter().setNamespace(namespace));

        if (depositor != null) {
            response = ResponseEntity.ok(depositor);
        }

        return response;
    }

    /**
     * Retrieve all Bags which a Depositor has ownership of in Chronopolis
     *
     * @param namespace the namespace of the Depositor
     * @return HTTP 200 with a list of Bags the Depositor owns
     *         HTTP 401 if the user is not authenticated
     *         HTTP 404 if the Depositor does not exist?
     */
    @GetMapping("/{namespace}/bags")
    public ResponseEntity<Iterable<Bag>> depositorBags(@PathVariable("namespace") String namespace,
                                                       @ModelAttribute BagFilter filter) {
        filter.setDepositor(namespace);
        Page<Bag> bags = dao.findPage(QBag.bag, filter);
        return ResponseEntity.ok(bags);
    }

    /**
     * Retrieve a Bag which a Depositor has ownership of in Chronopolis
     *
     * @param namespace the namespace of the Depositor
     * @param bagName   the name of the Bag
     * @return HTTP 200 with the Bag as the response body
     *         HTTP 401 if the user is not authenticated
     *         HTTP 404 if the bag does not exist
     */
    @GetMapping("/{namespace}/bags/{bagName}")
    public ResponseEntity<Bag> depositorBag(@PathVariable("namespace") String namespace,
                                            @PathVariable("bagName") String bagName) {
        ResponseEntity<Bag> response = ResponseEntity.notFound().build();
        BagFilter filter = new BagFilter()
                .setName(bagName)
                .setDepositor(namespace);
        Bag bag = dao.findOne(QBag.bag, filter);
        if (bag != null) {
            response = ResponseEntity.ok(bag);
        }
        return response;
    }

}
