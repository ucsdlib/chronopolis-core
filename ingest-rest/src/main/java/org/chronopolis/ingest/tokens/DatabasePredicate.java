package org.chronopolis.ingest.tokens;

import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;

import java.util.function.Predicate;

/**
 * Test if a ManifestEntry has not been tokenized
 *
 * @author shake
 */
public class DatabasePredicate implements Predicate<ManifestEntry> {

    private final PagedDAO dao;

    public DatabasePredicate(PagedDAO dao) {
        this.dao = dao;
    }

    @Override
    public boolean test(ManifestEntry entry) {
        Bag bag = entry.getBag();
        // pretty sure this will actually throw an exception if not found
        // should instead just get a boolean value back from the db
        AceToken exists = dao.findOne(QAceToken.aceToken, QAceToken.aceToken.bag.id.eq(bag.getId())
                .and(QAceToken.aceToken.filename.eq(entry.getPath())));
        return exists == null;
    }
}
