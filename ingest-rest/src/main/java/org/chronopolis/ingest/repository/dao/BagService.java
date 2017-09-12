package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.models.BagStatus;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Service to build queries for bags from the search criteria
 * <p>
 * Created by shake on 5/20/15.
 */
@Transactional
public class BagService extends SearchService<Bag, Long, BagRepository> {

    private final EntityManager entityManager;

    public BagService(BagRepository bagRepository, EntityManager entityManager) {
        super(bagRepository);
        this.entityManager = entityManager;
    }

    public List<Bag> getBagsWithAllTokens() {
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        QBag bag = QBag.bag;
        QAceToken token = QAceToken.aceToken;
        return factory.selectFrom(bag)
                .where(bag.status.eq(BagStatus.DEPOSITED),
                        bag.totalFiles.eq(
                                JPAExpressions.select(token.id.count())
                                        .from(token)
                                        .where(token.bag.id.eq(bag.id))))
                .fetch();
    }

}
