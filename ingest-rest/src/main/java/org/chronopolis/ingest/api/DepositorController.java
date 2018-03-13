package org.chronopolis.ingest.api;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.QDepositorContact;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.models.DepositorContactCreate;
import org.chronopolis.rest.models.DepositorContactRemove;
import org.chronopolis.rest.models.DepositorCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;
import static org.chronopolis.ingest.IngestController.hasRoleAdmin;

/**
 * API implementation for Depositors
 * <p>
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
                                                     @Valid @RequestBody
                                                     DepositorCreate depositor) {
        access.info("[POST /api/depositors] - {}", principal.getName());
        QDepositor qDepositor = QDepositor.depositor;

        // Default response of Forbidden if a user is not authorized to create Depositors
        ResponseEntity<Depositor> response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (hasRoleAdmin()) {
            // check collision
            Depositor exists = dao.findOne(qDepositor,
                    qDepositor.namespace.eq(depositor.getNamespace()));

            if (exists == null) {
                Optional<Set<DepositorContact>> parsed = parseContacts(depositor.getContacts());
                Optional<Depositor> depositorOptional = parsed.map(contacts -> new Depositor()
                        .setNamespace(depositor.getNamespace())
                        .setSourceOrganization(depositor.getSourceOrganization())
                        .setOrganizationAddress(depositor.getOrganizationAddress())
                        .setContacts(contacts));
                depositorOptional.ifPresent(dao::save);

                // Even though the BadRequest is handled by Spring, we do an additional check here
                // as a sanity check. This is because a PhoneNumberException can be thrown so it
                // gives a way to handle the exception gracefully.
                response = depositorOptional.map(entity ->
                        ResponseEntity.status(HttpStatus.CREATED).body(entity))
                        .orElse(ResponseEntity.badRequest().build());
            } else {
                response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        return response;
    }

    /**
     * Read through DepositorContactCreate requests and return a Set of DepositorContacts if
     * the requests are valid. If a Contact is invalid, return an empty Optional. This way if
     * no Contacts were requested to be made, we can still return an empty Set.
     * <p>
     * Note: We might want to move this somewhere else as this will likely be something we
     * need to do in multiple places
     *
     * @param creates the DepositorContactCreate requests
     * @return The DepositorContacts
     */
    private Optional<Set<DepositorContact>> parseContacts(List<DepositorContactCreate> creates) {
        PhoneNumberUtil numberUtil = PhoneNumberUtil.getInstance();
        Set<DepositorContact> contacts = new HashSet<>(creates.size());
        Optional<Set<DepositorContact>> safe = Optional.of(contacts);
        try {
            // todo: DepositorContact.fromCreateRequest()
            for (DepositorContactCreate create : creates) {
                DepositorContactCreate.PhoneNumber proto = create.getPhoneNumber();
                Phonenumber.PhoneNumber phoneNumber =
                        numberUtil.parse(proto.getNumber(), proto.getCountryCode());
                contacts.add(new DepositorContact().setContactName(create.getName())
                        .setContactEmail(create.getEmail())
                        .setContactPhone(numberUtil.format(phoneNumber, INTERNATIONAL)));
            }
        } catch (NumberParseException exception) {
            // This shouldn't happen, but in case we're trying an unvalidated DCC
            log.error("Unable to parse number", exception);
            safe = Optional.empty();
        }

        return safe;
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
        // Default response if the namespace does not match a known Depositor
        ResponseEntity<Iterable<Bag>> entity = ResponseEntity.notFound().build();

        QDepositor qDepositor = QDepositor.depositor;
        Depositor depositor = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
        if (depositor != null) {
            filter.setDepositor(namespace);
            Page<Bag> bags = dao.findPage(QBag.bag, filter);
            entity = ResponseEntity.ok(bags);
        }

        return entity;
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

    /**
     * Create a DepositorContact for a Depositor
     *
     * @param principal the principal of the user making the request
     * @param namespace the namespace of the Depositor
     * @param create    the RequestBody, a {@link DepositorContactCreate}
     * @return HTTP 201 with the DepositorContact if successful
     *         HTTP 400 if the DepositorContactModel is not valid
     *         HTTP 403 if the user is not authorized
     *         HTTP 404 if a Depositor does not exist with the given namespace
     *         TBD: HTTP 409 if the DepositorContact already exists
     */
    @PostMapping("/{namespace}/contacts")
    public ResponseEntity<DepositorContact> addContact(Principal principal,
                                                       @PathVariable("namespace") String namespace,
                                                       @Valid @RequestBody
                                                       DepositorContactCreate create) {
        access.info("[POST /api/depositors/{}/contacts] - {}", namespace, principal.getName());
        ResponseEntity<DepositorContact> response = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();

        if (hasRoleAdmin()) {
            QDepositor qDepositor = QDepositor.depositor;
            QDepositorContact qDepositorContact = QDepositorContact.depositorContact;
            Depositor depositor = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
            DepositorContact depositorContact = dao.findOne(qDepositorContact,
                    qDepositorContact.depositor.namespace.eq(namespace)
                            .and(qDepositorContact.contactEmail.eq(create.getEmail())));
            if (depositor != null && depositorContact == null) {
                Optional<DepositorContact> contact = DepositorContact.fromCreateRequest(create);
                // if valid...
                response = contact.map(entity -> {
                    depositor.addContact(entity);
                    dao.save(depositor);
                    return ResponseEntity.status(HttpStatus.CREATED).body(entity);
                }).orElse(ResponseEntity.badRequest().build());
            } else if (depositorContact != null) {
                response = ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else {
                response = ResponseEntity.notFound().build();
            }
        }

        return response;
    }

    /**
     * Remove a DepositorContact from a Depositor
     *
     * @param principal the principal of the user making the request
     * @param namespace the namespace identifying the Depositor
     * @param body      the RequestBody identifying the DepositorContact to Remove
     * @return HTTP 200 if the DepositorContact is removed
     *         HTTP 400 if the DepositorContact can not be found
     *         HTTP 403 if the user is not authorized to remove content
     *         HTTP 404 if the Depositor can not be found
     */
    @DeleteMapping("/{namespace}/contacts")
    public ResponseEntity<Depositor> removeContact(Principal principal,
                                                   @PathVariable("namespace") String namespace,
                                                   @RequestBody DepositorContactRemove body) {
        access.info("[DELETE /api/depositors/{}/contacts] - {}", namespace, principal.getName());
        ResponseEntity<Depositor> response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (hasRoleAdmin()) {
            QDepositor qDepositor = QDepositor.depositor;
            QDepositorContact qContact = QDepositorContact.depositorContact;
            Depositor depositor = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));
            DepositorContact contact = dao.findOne(qContact,
                    qContact.depositor.namespace.eq(namespace)
                            .and(qContact.contactEmail.eq(body.getEmail())));

            // I wonder if this is the best way to handle these...
            if (depositor == null) {
                response = ResponseEntity.notFound().build();
            } else if (contact == null) {
                response = ResponseEntity.badRequest().build();
            } else {
                depositor.removeContact(contact);
                dao.save(depositor);
                response = ResponseEntity.ok(depositor);
            }
        }

        return response;
    }

    /**
     * Add a node to the set of DepositorNode for a Depositor, reflecting that a Node will receive
     * Replications for Bags ingested by the Depositor.
     *
     * @param principal the principal of the user making the request
     * @param namespace the namespace of the Depositor
     * @param nodeName  the name of the Node
     * @return HTTP 200 - the node was added as a replicating node
     *         HTTP 400 if the nodeName does not map to any known Node
     *         HTTP 403 if the user does not have authorization to update this resource
     *         HTTP 404 if the namespace does not map to any known Depositor
     */
    @PostMapping("/{namespace}/nodes/{nodeName}")
    public ResponseEntity<Depositor> addNode(Principal principal,
                                             @PathVariable("namespace") String namespace,
                                             @PathVariable("nodeName") String nodeName) {
        access.info("[POST /api/depositors/{}/nodes/{}] - {}",
                namespace, nodeName, principal.getName());
        ResponseEntity<Depositor> response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (hasRoleAdmin()) {
            QNode qNode = QNode.node;
            QDepositor qDepositor = QDepositor.depositor;
            Node node = dao.findOne(qNode, qNode.username.eq(nodeName));
            Depositor depositor = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));

            if (depositor == null) {
                response = ResponseEntity.notFound().build();
            } else if (node == null) {
                response = ResponseEntity.badRequest().build();
            } else {
                depositor.addNodeDistribution(node);
                dao.save(depositor);
                response = ResponseEntity.ok(depositor);
            }
        }

        return response;
    }

    /**
     * Remove a node, identified by its name, from the Set of DepositorNodes of a Depositor. This
     * restricts the Replications for a Depositor to no longer be sent to the identified Node.
     *
     * @param principal the principal of the user making the request
     * @param namespace the namespace identifying the Depositor
     * @param nodeName  the name identifying the Node
     * @return HTTP 200 if the request completed successfully
     *         HTTP 400 if the Node does not exist
     *         HTTP 403 if the user does not have authorization to perform the action
     *         HTTP 404 if the Depositor does not exist
     */
    @DeleteMapping("/{namespace}/nodes/{nodeName}")
    public ResponseEntity<Depositor> removeNode(Principal principal,
                                                @PathVariable("namespace") String namespace,
                                                @PathVariable("nodeName") String nodeName) {
        access.info("[DELETE /api/depositors/{}/nodes/{}] - {}",
                namespace, nodeName, principal.getName());
        ResponseEntity<Depositor> response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (hasRoleAdmin()) {
            QNode qNode = QNode.node;
            QDepositor qDepositor = QDepositor.depositor;
            Node node = dao.findOne(qNode, qNode.username.eq(nodeName));
            Depositor depositor = dao.findOne(qDepositor, qDepositor.namespace.eq(namespace));

            if (depositor == null) {
                response = ResponseEntity.notFound().build();
            } else if (node == null) {
                response = ResponseEntity.badRequest().build();
            } else {
                depositor.removeNodeDistribution(node);
                dao.save(depositor);
                response = ResponseEntity.ok(depositor);
            }
        }

        return response;
    }

}
