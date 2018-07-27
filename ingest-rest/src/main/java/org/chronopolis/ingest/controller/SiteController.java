package org.chronopolis.ingest.controller;

import com.google.common.collect.ImmutableSet;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.BagSummary;
import org.chronopolis.ingest.models.DepositorSummary;
import org.chronopolis.ingest.models.UserRequest;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.ingest.repository.dao.UserService;
import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.entities.BagDistributionStatus;
import org.chronopolis.rest.kot.entities.QBag;
import org.chronopolis.rest.kot.entities.QBagDistribution;
import org.chronopolis.rest.kot.entities.QReplication;
import org.chronopolis.rest.kot.models.enums.BagStatus;
import org.chronopolis.rest.kot.models.update.PasswordUpdate;
import org.chronopolis.rest.kot.models.enums.ReplicationStatus;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
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
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

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
        access.info("GET /");
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
        Long active = replications(factory, replication.status.in(ReplicationStatus.Companion.active()));
        Long oneWeek = replications(factory, replication.status.in(ReplicationStatus.Companion.active()),
                replication.updatedAt.before(ZonedDateTime.now().minusWeeks(1)));
        Long twoWeeks = replications(factory, replication.status.in(ReplicationStatus.Companion.active()),
                replication.updatedAt.before(ZonedDateTime.now().minusWeeks(2)));

        model.addAttribute("preserved", preserved);
        model.addAttribute("replicating", replicating);
        model.addAttribute("activeReplications", active);
        model.addAttribute("oneWeekReplications", oneWeek);
        model.addAttribute("twoWeekReplications", twoWeeks);

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
        access.info("[GET /bags/overview] - {}", principal.getName());

        LocalDate before = LocalDate.now().minusWeeks(1);

        QBag bag = QBag.bag;
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);

        // retrieve recent bags
        List<Bag> recentBags = factory.selectFrom(QBag.bag)
                .orderBy(bag.createdAt.desc())
                .limit(5)
                .fetch();

        // retrieve BagStatusSummary
        BagSummary preservedSummary = new BagSummary(0L, 0L, BagStatus.PRESERVED);
        ImmutableSet<BagStatus> statuses = new ImmutableSet.Builder<BagStatus>()
                .addAll(BagStatus.Companion.preservedStates())
                .addAll(BagStatus.Companion.processingStates()).build();

        EnumPath<BagStatus> statusExpr = bag.status;
        NumberExpression<Long> sumExpr = bag.size.sum();
        NumberExpression<Long> countExpr = bag.countDistinct();
        List<BagSummary> summaries = factory.selectFrom(QBag.bag)
                .where(statusExpr.in(statuses))
                .select(Projections.constructor(BagSummary.class, sumExpr, countExpr, statusExpr))
                .groupBy(statusExpr)
                .fetch();

        Long processingBags = 0L;
        Long processingSize = 0L;

        // we only have a few status types so do this here instead of another db call
        for (BagSummary summary : summaries) {
            if (summary.getStatus() != BagStatus.PRESERVED) {
                processingBags += summary.getCount();
                processingSize += summary.getSum();
            } else {
                preservedSummary = summary;
            }
        }
        summaries.remove(preservedSummary);

        // retrieve DepositorSummary
        /*
        StringPath depositorExpr = bag.depositor.namespace;
        List<DepositorSummary> depositorSummaries = factory.selectFrom(QBag.bag)
                .select(Projections.constructor(DepositorSummary.class, sumExpr, countExpr, depositorExpr))
                .groupBy(depositorExpr)
                .limit(10)
                .fetch();
                */

        // retrieve stuck Bags?
        ZonedDateTime beforeDateTime = ZonedDateTime.of(before, LocalTime.of(0, 0), ZoneOffset.UTC);
        Long stuck = factory.selectFrom(bag)
                .select(bag.countDistinct())
                .where(bag.status.in(BagStatus.Companion.processingStates()).and(bag.updatedAt.before(beforeDateTime)))
                .fetchOne();

        // Node totals
        List<DepositorSummary> nodeTotals = factory.from(QBagDistribution.bagDistribution)
                .innerJoin(QBagDistribution.bagDistribution.bag, bag)
                .select(Projections.constructor(DepositorSummary.class, sumExpr, countExpr, QBagDistribution.bagDistribution.node.username))
                .where(QBagDistribution.bagDistribution.status.eq(BagDistributionStatus.REPLICATE))
                .groupBy(QBagDistribution.bagDistribution.node.username)
                .fetch();

        model.addAttribute("before", before);
        model.addAttribute("stuckBags", stuck);
        model.addAttribute("recentBags", recentBags);
        model.addAttribute("processingBags", processingBags);
        model.addAttribute("processingSize", processingSize);
        model.addAttribute("nodeSummaries", nodeTotals);
        model.addAttribute("statusSummaries", summaries);
        model.addAttribute("preservedSummary", preservedSummary);
        // model.addAttribute("depositorSummaries", depositorSummaries);
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
        access.debug("[GET /login]");
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
        access.info("[GET /users] - {}", principal.getName());
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
    public String createUser(UserRequest user, Principal principal) {
        access.info("[POST /users/add] - {}", principal.getName());
        log.debug("Request to create user: {} {} {}", user.getUsername(), user.getRole(), user.isNode());
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
        access.info("[POST /users/update] - {}", principal.getName());
        userService.updatePassword(update, principal);
        return "users";
    }

}
