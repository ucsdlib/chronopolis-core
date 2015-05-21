package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.QBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import static org.chronopolis.ingest.repository.PredicateUtil.setExpression;

/**
 * Service to build queries for bags from the search criteria
 *
 * Created by shake on 5/20/15.
 */
@Component
@Transactional
public class BagService {
    private final Logger log = LoggerFactory.getLogger(BagService.class);

    private final BagRepository bagRepository;

    @Autowired
    public BagService(BagRepository bagRepository) {
        this.bagRepository = bagRepository;
    }

    // TODO: use search criteria
    public Bag findBag(Long bagId) {
        return bagRepository.findOne(bagId);
    }

    public Page<Bag> findBags(BagSearchCriteria criteria, Pageable pageable) {
        QBag bag = QBag.bag;
        String name = criteria.getName();
        String depositor = criteria.getDepositor();
        BagStatus status = criteria.getStatus();

        BooleanExpression predicate = null;

        if (!name.equals("")) {
            predicate = bag.name.eq(name);
        }

        if (!depositor.equals("")) {
            BooleanExpression depEq = bag.depositor.eq(criteria.getDepositor());
            predicate = setExpression(predicate, depEq);
        }

        if (status != null) {
            BooleanExpression statusEq = bag.status.eq(criteria.getStatus());
            predicate = setExpression(predicate, statusEq);
        }

        // No predicate, return a single page
        if (predicate == null) {
            log.debug("No predicate, returning all bags");
            return bagRepository.findAll(pageable);
        }

        log.debug("Using predicate to query bags");
        // Return a single page of the query asked for
        return bagRepository.findAll(predicate, pageable);
    }

}
