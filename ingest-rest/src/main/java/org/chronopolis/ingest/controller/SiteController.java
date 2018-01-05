package org.chronopolis.ingest.controller;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.BagSummary;
import org.chronopolis.ingest.models.DepositorSummary;
import org.chronopolis.ingest.models.UserRequest;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.ingest.repository.dao.UserService;
import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagDistribution;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.PasswordUpdate;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.persistence.EntityManager;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Controller for handling basic site interaction/administration
 * <p>
 * Created by shake on 4/15/15.
 */
@Controller
public class SiteController extends IngestController {

    private final Logger log = LoggerFactory.getLogger(SiteController.class);

    private final EntityManager entityManager;
    private final UserDetailsManager manager;
    private final UserService userService;

    @Autowired
    public SiteController(EntityManager entityManager, UserDetailsManager manager, UserService userService) {
        this.entityManager = entityManager;
        this.manager = manager;
        this.userService = userService;
    }

    /**
     * Get the index page
     *
     * @return the main index
     */
    @GetMapping("/")
    public String getIndex(Model model) {
        log.debug("GET index");
        BagSummary preserved = new BagSummary(0L, 0L, BagStatus.PRESERVED);
        BagSummary replicating = new BagSummary(0L, 0L, BagStatus.REPLICATING);

        // push to separate func
        QBag bag = QBag.bag;
        NumberExpression<Long> sumExpr = bag.size.sum();
        NumberExpression<Long> countExpr = bag.count();
        EnumPath<BagStatus> statusExpr = bag.status;
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        List<Tuple> tuples = factory.selectFrom(QBag.bag)
                .select(sumExpr, countExpr, statusExpr)
                .where(statusExpr.in(BagStatus.PRESERVED, BagStatus.REPLICATING))
                .groupBy(statusExpr)
                .fetch();
        for (Tuple tuple : tuples) {
            Long sum = tuple.get(sumExpr);
            Long count = tuple.get(countExpr);
            BagStatus status = tuple.get(statusExpr);

            // might be a better way to do this
            if (status == BagStatus.PRESERVED) {
                preserved = new BagSummary(sum, count, status);
            } else {
                replicating = new BagSummary(sum, count, status);
            }
        }

        QReplication replication = QReplication.replication;
        Long active = replications(factory, replication.status.in(ReplicationStatus.active()));
        Long oneWeek = replications(factory, replication.status.in(ReplicationStatus.active()),
                replication.updatedAt.before(ZonedDateTime.now().minusWeeks(1)));
        Long twoWeeks = replications(factory, replication.status.in(ReplicationStatus.active()),
                replication.updatedAt.before(ZonedDateTime.now().minusWeeks(2)));

        model.addAttribute("preserved", preserved);
        model.addAttribute("replicating", replicating);
        model.addAttribute("activeReplications", active);
        model.addAttribute("oneWeekReplications", oneWeek);
        model.addAttribute("twoWeeksReplications", twoWeeks);

        return "index";
    }

    /**
     * Return information about all bags in Chronopolis
     * - depositor totals + distribution stats
     * - status totals
     *
     * @param model     the view model
     * @param principal the security principal of the user
     * @return the bags/overview template for display
     */
    @GetMapping("/bags/overview")
    public String getBagsOverview(Model model, Principal principal) {
        // access.info("[GET /bags/overview] - {}", principal.getName());

        QBag bag = QBag.bag;
        QBagDistribution distribution = QBagDistribution.bagDistribution;
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);

        // retrieve recent bags
        List<Bag> recentBags = factory.selectFrom(QBag.bag)
                .orderBy(bag.createdAt.desc())
                .limit(5)
                .fetch();

        // retrieve BagStatusSummary
        NumberExpression<Long> sumExpr = bag.size.sum();
        NumberExpression<Long> countExpr = bag.countDistinct();
        EnumPath<BagStatus> statusExpr = bag.status;
        List<BagSummary> summaries = factory.selectFrom(QBag.bag)
                .select(Projections.constructor(BagSummary.class, sumExpr, countExpr, statusExpr))
                .groupBy(statusExpr)
                .fetch();

        List<String> summaryLabels = new ArrayList<>();
        List<String> summaryData = new ArrayList<>();

        for (BagSummary summary : summaries) {
            summaryLabels.add(summary.getStatus().toString());
            summaryData.add(summary.getCount().toString());
        }

        // retrieve DepositorSummary
        StringPath depositorExpr = bag.depositor;
        List<DepositorSummary> depositorSummaries = factory.selectFrom(QBag.bag)
                .select(Projections.constructor(DepositorSummary.class, sumExpr, countExpr, depositorExpr))
                .groupBy(depositorExpr)
                .limit(10)
                .fetch();

        // retrieve stuck Bags

        // Node totals
        List<DepositorSummary> nodeTotals = factory.from(QBagDistribution.bagDistribution)
                .innerJoin(QBagDistribution.bagDistribution.bag, bag)
                .select(Projections.constructor(DepositorSummary.class, sumExpr, countExpr, QBagDistribution.bagDistribution.node.username))
                .where(QBagDistribution.bagDistribution.status.eq(BagDistribution.BagDistributionStatus.REPLICATE))
                .groupBy(QBagDistribution.bagDistribution.node.username)
                .fetch();

        nodeTotals.forEach(tuple -> {
            log.info("Got a summary: {} {} {}", tuple.getDepositor(), tuple.getSum(), tuple.getCount());
        });


        model.addAttribute("recentBags", recentBags);
        model.addAttribute("nodeSummaries", nodeTotals);
        model.addAttribute("summaryLabels", summaryLabels);
        model.addAttribute("summaryData", summaryData);
        model.addAttribute("statusSummaries", summaries);
        model.addAttribute("depositorSummaries", depositorSummaries);
        model.addAttribute("formatter", new FileSizeFormatter());

        return "bags/index";
    }

    private Long replications(JPAQueryFactory factory, Predicate... predicates) {
        QReplication replication = QReplication.replication;
        return factory.selectFrom(replication)
                .select(replication.count())
                .where(predicates)
                .fetchOne();
    }

    /**
     * Get the login page
     *
     * @return the login page
     */
    @RequestMapping(value = "/login")
    public String login() {
        log.debug("LOGIN");
        return "login";
    }

    /**
     * Return a list of all users if called by an admin, otherwise only add the current
     * user
     *
     * @param model     the model to add attributes to
     * @param principal the security principal of the user
     * @return the users page
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public String getUsers(Model model, Principal principal) {
        Collection<Authority> users = new ArrayList<>();
        String user = principal.getName();

        // Give admins a view into all users
        if (hasRoleAdmin()) {
            users.addAll(userService.listUserAuthorities());
        } else {
            // TODO: userService.getAuthority(name)
            users.add(userService.getUserAuthority(user));
        }

        model.addAttribute("users", users);
        return "users";
    }

    /**
     * Handle creation of a user
     * TODO: Make sure user does not exist before creating
     *
     * @param user The user to create
     * @return redirect to the users page
     */
    @RequestMapping(value = "/users/add", method = RequestMethod.POST)
    public String createUser(UserRequest user) {
        log.debug("Request to create user: {} {} {}", new Object[]{user.getUsername(), user.getRole(), user.isNode()});
        userService.createUser(user);
        return "redirect:/users";
    }

    /**
     * Handler for updating the current users password
     *
     * @param update    The password to update
     * @param principal The security principal of the user
     * @return redirect to the users page
     */
    @RequestMapping(value = "/users/update", method = RequestMethod.POST)
    public String updateUser(PasswordUpdate update, Principal principal) {
        userService.updatePassword(update, principal);
        return "users";
    }

}
