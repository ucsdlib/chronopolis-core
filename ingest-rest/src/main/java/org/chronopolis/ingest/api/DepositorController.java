package org.chronopolis.ingest.api;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.models.DepositorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API implementation for Depositors
 * <p>
 * todo: Pagination
 * todo: Query Parameters
 * todo: Validate DepositorModel
 *
 * @author shake
 */
@RestController
@RequestMapping("/api/depositors")
public class DepositorController {

    private final Logger log = LoggerFactory.getLogger(DepositorController.class);
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private EntityManager entityManager;

    /**
     * Retrieve all Depositors held by the Ingest Server
     *
     * @return HTTP 200 with a list of all Depositors
     *         HTTP 401 if the user is not authenticated
     */
    @GetMapping
    public ResponseEntity<Iterable<Depositor>> depositors() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<Depositor> depositors = queryFactory.selectFrom(QDepositor.depositor)
                .fetch();
        return ResponseEntity.ok(depositors);
    }

    /**
     * Create a Depositor in the Ingest Server
     *
     * @param depositor the Depositor to create
     * @return HTTP 201 if the Depositor was created successfully with the Depositor as the response
     *         HTTP 401 if the user is not authenticated
     *         HTTP 403 if the user requesting the create does not have permission
     *         HTTP 409 if the Depositor already exists
     */
    @PostMapping
    public ResponseEntity<Depositor> createDepositor(@RequestBody DepositorModel depositor) {
        Depositor entity = new Depositor()
                .setNamespace(depositor.getNamespace())
                .setOrganizationAddress(depositor.getOrganizationAddress())
                .setSourceOrganization(depositor.getSourceOrganization())
                .setContacts(depositor.getContacts().stream().map(contact -> new DepositorContact()
                        .setContactName(contact.getContactName())
                        .setContactEmail(contact.getContactEmail())
                        .setContactPhone(contact.getContactPhone()))
                        .collect(Collectors.toSet()));

        // todo: should push this into a separate interface (similar to other controllers) so that
        //       we don't need full EntityManager/JPA instantiation when testing
        entityManager.getTransaction().begin();
        entityManager.persist(entity);
        entityManager.getTransaction().commit();

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

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Depositor depositor = queryFactory.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq(namespace))
                .fetchOne();

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
    public ResponseEntity<Iterable<Bag>> depositorBags(@PathVariable("namespace") String namespace) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<Bag> bags = queryFactory.selectFrom(QBag.bag)
                .where(QBag.bag.depositor.namespace.eq(namespace))
                .limit(10)
                .fetch();

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
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Bag bag = queryFactory.selectFrom(QBag.bag)
                .where(QBag.bag.name.eq(bagName).and(QBag.bag.depositor.namespace.eq(namespace)))
                .fetchOne();
        if (bag != null) {
            response = ResponseEntity.ok(bag);
        }
        return response;
    }

}
