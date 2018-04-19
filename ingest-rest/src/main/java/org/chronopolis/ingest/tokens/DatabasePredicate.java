package org.chronopolis.ingest.tokens;

import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
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
        if (entry == null || entry.getBag() == null || entry.getBag().getId() == null) {
            return false;
        }

        Long bagId = entry.getBag().getId();

        // pretty sure this will actually throw an exception if not found
        // should instead just get a boolean value back from the db
        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));
        AceToken token = dao.findOne(QAceToken.aceToken, QAceToken.aceToken.bag.id.eq(bagId)
                .and(QAceToken.aceToken.filename.eq(entry.getPath())));
        log.trace("[{}-{}] Testing: bag not null={};token null={}");
        return bag != null && token == null;
    }
}
