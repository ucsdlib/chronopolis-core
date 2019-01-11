package org.chronopolis.ingest.controller;

import com.google.common.collect.ImmutableMap;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.ForbiddenException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.models.DepositorSummary;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.DepositorContact;
import org.chronopolis.rest.entities.depositor.DepositorContactKt;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.depositor.QDepositorContact;
import org.chronopolis.rest.models.create.DepositorContactCreate;
import org.chronopolis.rest.models.create.DepositorCreate;
import org.chronopolis.rest.models.update.DepositorUpdate;
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
import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Controller to handle Depositor requests and things
 * <p>
 * todo: Depositor Page
 * - edit
 * - add contact
 * todo: Depositor Edit {GET,POST}
 * todo: Do we want to redirect to 404s on the event depositor namespaces are not found?
 * todo: Do we want to redirect to a 409 page on the event of duplicates?
 *
 * @author shake
 */
@Controller
public class DepositorUIController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(DepositorUIController.class);

    private final PagedDao dao;
    private final EntityManager entityManager;

    @Autowired
    public DepositorUIController(PagedDao dao, EntityManager entityManager) {
        this.dao = dao;
        this.entityManager = entityManager;
    }

    @GetMapping("/depositors")
    public String index(Model model) {
        QBag bag = QBag.bag;
        QDepositor depositor = QDepositor.depositor;
        FileSizeFormatter formatter = new FileSizeFormatter();
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);

        NumberExpression<Long> sumExpr = bag.size.sum();
        NumberExpression<Long> countExpr = bag.countDistinct();

        // retrieve DepositorSummary
        StringPath depositorExpr = bag.depositor.namespace;
        ConstructorExpression<DepositorSummary> constructor =
                Projections.constructor(DepositorSummary.class, sumExpr, countExpr, depositorExpr);
        List<DepositorSummary> bySum = factory.selectFrom(QBag.bag)
                .select(constructor)
                .orderBy(sumExpr.desc())
                .groupBy(depositorExpr)
                .limit(5)
                .fetch();

        List<DepositorSummary> byCount = factory.selectFrom(QBag.bag)
                .select(constructor)
                .orderBy(countExpr.desc())
                .groupBy(depositorExpr)
                .limit(5)
                .fetch();

        long numDepositors = factory.selectFrom(depositor)
                .fetchCount();

        List<Depositor> recent = factory.selectFrom(depositor)
                .orderBy(depositor.createdAt.desc())
                .limit(5)
                .fetch();

        /* The actual query we want to emulate is fairly simple, but I'm not quite sure how to
           map it to a QueryDSL query:
           select avg(count) as count, avg(size) as size from
             (select count(id) as count, avg(size) as size, depositor_id
               from bag group by depositor_id) as q;

            at any rate, we can do the averaging after as it shouldn't be too heavy of an operation
            anyways
         */
        List<Tuple> fetch = factory.selectFrom(bag)
                .select(bag.size.avg(), countExpr, bag.depositor.id)
                .groupBy(bag.depositor.id)
                .fetch();

        Double sizeAvgPerDepositor = 0d;
        Double countAvgPerDepositor = 0d;
        for (Tuple tuple : fetch) {
            countAvgPerDepositor += tuple.get(countExpr);
            sizeAvgPerDepositor += tuple.get(bag.size.avg());
        }
        if (numDepositors > 0) {
            sizeAvgPerDepositor = sizeAvgPerDepositor / numDepositors;
            countAvgPerDepositor = countAvgPerDepositor / numDepositors;
        }

        model.addAttribute("recent", recent);
        model.addAttribute("bySum", bySum);
        model.addAttribute("byCount", byCount);
        model.addAttribute("formatter", formatter);
        model.addAttribute("numDepositors", numDepositors);
        model.addAttribute("sizeAvg", new BigDecimal(sizeAvgPerDepositor));
        model.addAttribute("countAvg", countAvgPerDepositor);
        return "depositors/index";
    }

    @GetMapping("/depositors/add")
    public String create(Model model, DepositorCreate depositorCreate) {
        model.addAttribute("nodes", dao.findAll(QNode.node));
        model.addAttribute("depositorCreate", depositorCreate);
        return "depositors/add";
    }

    @PostMapping("/depositors")
    public String createAction(Model model,
                               Principal principal,
                               @Valid DepositorCreate depositorCreate,
                               BindingResult bindingResult) {
        // Additional constraints
        List<Node> nodes = dao.findAll(QNode.node,
                QNode.node.username.in(depositorCreate.getReplicatingNodes()));
        Depositor depositor = dao.findOne(QDepositor.depositor,
                QDepositor.depositor.namespace.eq(depositorCreate.getNamespace()));

        if (nodes.size() != depositorCreate.getReplicatingNodes().size()) {
            bindingResult.rejectValue("replicatingNode", "node.invalid", "Invalid node detected");
        }

        if (depositor != null) {
            bindingResult.rejectValue("namespace",
                    "namespace.duplicate",
                    "Namespace already in use");
        }

        if (bindingResult.hasErrors()) {
            log.warn("Invalid Depositor added: {} errors", bindingResult.getErrorCount());
            bindingResult.getFieldErrors().forEach(error ->
                    log.error("{}:{}", error.getField(), error.getDefaultMessage()));

            model.addAttribute("nodes", dao.findAll(QNode.node));
            model.addAttribute("depositorCreate", depositorCreate);
            return "depositors/add";
        }

        log.info("Adding depositor");
        depositor = new Depositor();
        // handle lateinits  first
        depositor.setContacts(new HashSet<>());
        depositor.setNodeDistributions(new HashSet<>());
        depositor.setNamespace(depositorCreate.getNamespace());
        depositor.setSourceOrganization(depositorCreate.getSourceOrganization());
        depositor.setOrganizationAddress(depositorCreate.getOrganizationAddress());
        nodes.forEach(depositor::addNodeDistribution);
        dao.save(depositor);

        return "redirect:depositors/list/" + depositor.getNamespace();
    }

    @GetMapping("/depositors/list/{namespace}/edit")
    public String editDepositor(Model model,
                                @PathVariable("namespace") String namespace,
                                DepositorUpdate depositorEdit) {
        Depositor existing = getOrThrowNotFound(namespace);

        BooleanExpression availableNodes = QNode.node.notIn(
                JPAExpressions.select(QNode.node)
                    .from(QDepositor.depositor)
                    .join(QDepositor.depositor.nodeDistributions, QNode.node)
                    .where(QDepositor.depositor.id.eq(existing.getId())));

        model.addAttribute("nodes", dao.findAll(QNode.node, availableNodes));
        model.addAttribute("depositor", existing);
        model.addAttribute("depositorEdit", depositorEdit);
        return "depositors/edit";
    }

    @PostMapping("/depositors/list/{namespace}/edit")
    public String postEditDepositor(Principal principal,
                                    @PathVariable("namespace") String namespace,
                                    DepositorUpdate depositorEdit) {
        Depositor depositor = getOrThrowNotFound(namespace);

        String address = depositorEdit.getOrganizationAddress();
        String organization = depositorEdit.getSourceOrganization();

        if (address != null && !address.isEmpty()) {
            depositor.setOrganizationAddress(address);
        }

        if (organization != null && !organization.isEmpty()) {
            depositor.setSourceOrganization(organization);
        }

        dao.findAll(QNode.node, QNode.node.username.in(depositorEdit.getReplicatingNodes()))
                .forEach(depositor::addNodeDistribution);

        dao.save(depositor);

        return "redirect:/depositors/list/" + namespace;
    }

    @GetMapping("/depositors/list/{namespace}")
    public String getDepositor(Model model,
                               @PathVariable("namespace") String namespace) {
        Depositor depositor = getOrThrowNotFound(namespace);
        model.addAttribute("depositor", depositor);
        return "depositors/depositor";
    }

    @GetMapping("/depositors/list")
    public String list(Model model,
                       @ModelAttribute(value = "filter") DepositorFilter filter) {
        Page<Depositor> depositors = dao.findPage(QDepositor.depositor, filter);
        PageWrapper<Depositor> pages = new PageWrapper<>(depositors,
                "/depositors/list", filter.getParameters());
        model.addAttribute("depositors", depositors);
        model.addAttribute("pages", pages);
        return "depositors/list";
    }

    @GetMapping("/depositors/list/{namespace}/addContact")
    public String addContact(Model model,
                             @PathVariable("namespace") String namespace,
                             DepositorContactCreate depositorContactCreate) {
        Depositor depositor = getOrThrowNotFound(namespace);
        model.addAttribute(depositor);
        model.addAllAttributes(addContactAttributes(depositorContactCreate));
        return "depositors/add_contact";
    }

    @PostMapping("/depositors/list/{namespace}/addContact")
    public String addContactAction(Model model,
                                   Principal principal,
                                   @PathVariable("namespace") String namespace,
                                   @Valid DepositorContactCreate depositorContactCreate,
                                   BindingResult result) throws BadRequestException {
        // Additional constraints
        Depositor depositor = getOrThrowNotFound(namespace);
        DepositorContact contact = dao.findOne(QDepositorContact.depositorContact,
                QDepositorContact.depositorContact.contactEmail
                        .eq(depositorContactCreate.getContactEmail())
                        .and(QDepositorContact.depositorContact.depositor.eq(depositor)));

        if (contact != null) {
            result.rejectValue("email",
                    "email.conflict",
                    "Email already in use for depositor");
        }

        if (result.hasErrors()) {
            log.warn("Invalid contact added: {} errors", result.getErrorCount());
            result.getFieldErrors().forEach(error ->
                    log.error("{}:{}", error.getField(), error.getDefaultMessage()));

            model.addAttribute(depositor);
            model.addAllAttributes(addContactAttributes(depositorContactCreate));
            return "depositors/add_contact";
        }

        return DepositorContactKt.fromRequest(depositorContactCreate)
                .map(fromRequest -> {
                    depositor.addContact(fromRequest);
                    dao.save(fromRequest);
                    return "redirect:/depositors/list/" + namespace;
                }).orElse("exceptions/bad_request");
    }

    @GetMapping("/depositors/list/{namespace}/addNode")
    public String addNode(Model model,
                          @PathVariable("namespace") String namespace,
                          DepositorUpdate depositorEdit) {
        Depositor depositor = getOrThrowNotFound(namespace);

        BooleanExpression availableNodes = QNode.node.notIn(
                JPAExpressions.select(QNode.node)
                    .from(QDepositor.depositor)
                    .join(QDepositor.depositor.nodeDistributions, QNode.node)
                    .where(QDepositor.depositor.id.eq(depositor.getId())));

        model.addAttribute("depositorEdit", depositorEdit);
        model.addAttribute("nodes", dao.findAll(QNode.node, availableNodes));
        model.addAttribute("depositor", depositor);
        return "depositors/add_node";
    }

    @PostMapping("/depositors/list/{namespace}/addNode")
    public String addNodeAction(@PathVariable("namespace") String namespace,
                                DepositorUpdate depositorEdit) {
        Depositor depositor = dao.findOne(QDepositor.depositor,
                QDepositor.depositor.namespace.eq(namespace));
        List<String> nodes = depositorEdit.getReplicatingNodes();
        List<Node> requested = dao.findAll(QNode.node,
                QNode.node.username.in(nodes));
        log.info("Requested nodes: {}", nodes.size());

        if (requested.size() != nodes.size()) {
            log.error("Unable to process request");
        } else {
            log.info("Depositor: {}", depositor.getId());
            requested.forEach(depositor::addNodeDistribution);
            dao.save(depositor);
        }

        return "redirect:/depositors/list/" + namespace;
    }

    @GetMapping("/depositors/list/{namespace}/removeNode")
    public String removeNode(Model model,
                             Principal principal,
                             @PathVariable("namespace") String namespace,
                             @ModelAttribute("name") String name) throws ForbiddenException {
        if (!hasRoleAdmin()) {
            throw new ForbiddenException("User is not allowed to update a Depositor");
        }

        Depositor depositor = getOrThrowNotFound(namespace);

        Node node = dao.findOne(QNode.node, QNode.node.username.eq(name));
        if (node == null) {
            throw new BadRequestException("Invalid node given");
        }

        depositor.removeNodeDistribution(node);
        dao.save(depositor);

        model.addAttribute("depositor", depositor);
        return "redirect:/depositors/list/" + namespace;
    }

    @GetMapping("/depositors/list/{namespace}/removeContact")
    public String removeContact(Model model,
                                Principal principal,
                                @PathVariable("namespace") String namespace,
                                @ModelAttribute("email") String email) throws ForbiddenException {
        if (!hasRoleAdmin()) {
            throw new ForbiddenException("User is not allowed to update a Depositor");
        }


        Depositor depositor = getOrThrowNotFound(namespace);
        DepositorContact contact = dao.findOne(QDepositorContact.depositorContact,
                QDepositorContact.depositorContact.contactEmail.eq(email)
                        .and(QDepositorContact.depositorContact.depositor.eq(depositor)));

        if (contact == null) {
            throw new BadRequestException("Invalid node given");
        }

        depositor.removeContact(contact);
        dao.save(depositor);

        model.addAttribute("depositor", depositor);
        return "redirect:/depositors/list/" + namespace;
    }

    private Map<String, ?> addContactAttributes(DepositorContactCreate depositorContactCreate) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Map<String, Integer> regions = util.getSupportedRegions().stream()
                .collect(Collectors.toMap(region -> region, util::getCountryCodeForRegion,
                        (v1, v2) -> {throw new RuntimeException("Duplicate value " + v2);},
                        TreeMap::new));
        return ImmutableMap.of("regions", regions,
                "depositorContactCreate", depositorContactCreate);
    }

    private Depositor getOrThrowNotFound(String namespace) {
        Depositor depositor = dao.findOne(QDepositor.depositor,
                QDepositor.depositor.namespace.eq(namespace));
        if (depositor == null) {
            throw new NotFoundException(namespace + " does not exist");
        }

        return depositor;
    }
}
