package org.chronopolis.ingest.controller;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.DepositorSummary;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.QNode;
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
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;

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
        QBag bag = QBag.bag;
        QDepositor depositor = QDepositor.depositor;
        FileSizeFormatter formatter = new FileSizeFormatter();
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);

        NumberExpression<Long> sumExpr = bag.size.sum();
        NumberExpression<Long> countExpr = bag.countDistinct();

        // retrieve DepositorSummary
        StringPath depositorExpr = bag.depositor.namespace;
        List<DepositorSummary> summaries = factory.selectFrom(QBag.bag)
                .select(Projections.constructor(DepositorSummary.class, sumExpr, countExpr, depositorExpr))
                .orderBy(sumExpr.desc())
                .groupBy(depositorExpr)
                .limit(10)
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

        BigDecimal sizeAvgPerDepositor = new BigDecimal(0);
        Double countAvgPerDepositor = 0d;
        for (Tuple tuple : fetch) {
            Double sizeAvg = tuple.get(bag.size.avg());
            countAvgPerDepositor += tuple.get(countExpr);
            sizeAvgPerDepositor = sizeAvgPerDepositor.add(new BigDecimal(sizeAvg));
        }
        if (numDepositors > 0) {
            sizeAvgPerDepositor = sizeAvgPerDepositor.divide(new BigDecimal(numDepositors));
            countAvgPerDepositor = countAvgPerDepositor / numDepositors;
        }

        model.addAttribute("recent", recent);
        model.addAttribute("summaries", summaries);
        model.addAttribute("formatter", formatter);
        model.addAttribute("numDepositors", numDepositors);
        model.addAttribute("sizeAvg", sizeAvgPerDepositor);
        model.addAttribute("countAvg", countAvgPerDepositor);
        return "depositors/index";
    }

    @GetMapping("/depositors/add")
    public String create(Model model, Principal principal) {
        model.addAttribute("nodes", dao.findAll(QNode.node));
        return "depositors/add";
    }

    @PostMapping("/depositors/")
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
        model.addAllAttributes(addContactAttributes(depositorContactCreate));
        return "depositors/add_contact";
    }

    @PostMapping("/depositors/list/{namespace}/addContact")
    public String addContactAction(Model model,
                                   Principal principal,
                                   @PathVariable("namespace") String namespace,
                                   @Valid DepositorContactCreate depositorContactCreate,
                                   BindingResult result) throws NumberParseException {
        if (result.hasErrors()) {
            log.warn("Invalid contact added: {} errors", result.getErrorCount());
            result.getFieldErrors().forEach(error ->
                    log.error("{}:{}", error.getField(), error.getDefaultMessage()));

            model.addAllAttributes(addContactAttributes(depositorContactCreate));
            return "depositors/add_contact";
        }

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber number = util.parse(
                depositorContactCreate.getPhoneNumber().getNumber(),
                depositorContactCreate.getPhoneNumber().getCountryCode());

        Depositor depositor = dao.findOne(QDepositor.depositor,
                new DepositorFilter().setNamespace(namespace));
        DepositorContact contact = new DepositorContact()
                .setContactEmail(depositorContactCreate.getEmail())
                .setContactName(depositorContactCreate.getName())
                .setContactPhone(util.format(number, INTERNATIONAL));
        depositor.addContact(contact);
        dao.save(depositor);
        return "redirect:/depositors/list/" + namespace;
    }

    private Map<String, ?> addContactAttributes(DepositorContactCreate depositorContactCreate) {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Set<String> regions = util.getSupportedRegions();
        ImmutableSortedSet<String> supportedRegions = ImmutableSortedSet.copyOf(regions);
        return ImmutableMap.of("regions", supportedRegions,
                "depositorContactCreate", depositorContactCreate);
    }
}
