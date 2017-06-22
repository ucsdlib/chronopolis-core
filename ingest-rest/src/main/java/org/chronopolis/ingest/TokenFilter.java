package org.chronopolis.ingest;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.common.util.Filter;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.QAceToken;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * Created by shake on 8/24/15.
 */
public class TokenFilter implements Filter<Path> {

    private Long id;
    private Set<Path> localFilter;
    private TokenRepository repository;
    private QAceToken token = QAceToken.aceToken;

    public TokenFilter(TokenRepository repository, Long id) {
        this.id = id;
        this.repository = repository;
        this.localFilter= new HashSet<>();
    }

    @Override
    public boolean add(Path path) {
        localFilter.add(path);
        return false;
    }

    @Override
    public boolean contains(Path path) {
        // short circuit to avoid db lookup
        return localFilter.contains(path) || dbContains(path);
    }

    private boolean dbContains(Path path) {
        BooleanExpression predicate = token.bag.id.eq(id)
                .and(token.filename.eq(path.toString()));
        AceToken one = repository.findOne(predicate);
        return one != null;
    }
}
