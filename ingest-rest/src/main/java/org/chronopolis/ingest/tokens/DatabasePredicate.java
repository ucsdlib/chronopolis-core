package org.chronopolis.ingest.tokens;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.tokenize.ManifestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * Test if a ManifestEntry has not been tokenized
 *
 * @author shake
 */
public class DatabasePredicate implements Predicate<ManifestEntry> {

    private final Logger log = LoggerFactory.getLogger(DatabasePredicate.class);

    private final PagedDAO dao;

    public DatabasePredicate(PagedDAO dao) {
        this.dao = dao;
    }

    @Override
    public boolean test(ManifestEntry entry) {
        // null checks
        if (entry == null || entry.getBag() == null) {
            return false;
        }

        Long bagId = entry.getBag().getId();
        JPAQueryFactory qf = dao.getJPAQueryFactory();
        boolean exists = qf.selectFrom(QBag.bag)
                .where(QBag.bag.id.eq(bagId))
                .fetchCount() == 1;

        log.trace("[{}:{}] DbFilter: Bag exists {}", entry.getBag().getName(),
                entry.getPath(),
                exists);
        return exists && testToken(qf, entry);
    }

    private boolean testToken(JPAQueryFactory qf, ManifestEntry entry) {
        Long bagId = entry.getBag().getId();
        String filename = entry.getPath();
        boolean notExists = qf.selectFrom(QAceToken.aceToken)
                .where(QAceToken.aceToken.bag.id.eq(bagId)
                        .and(QAceToken.aceToken.filename.eq(filename)))
                .fetchCount() == 0;

        log.trace("[{}:{}] DbFilter: Token !exists {}", entry.getBag().getName(),
            filename,
            notExists);
        return notExists;
    }
}
