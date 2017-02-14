package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.Map;

/**
 *
 * Created by shake on 1/24/17.
 */
@Transactional
public class SearchService<T, I extends Serializable, E extends JpaRepository<T, I> & QueryDslPredicateExecutor<T>> {
    private final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final E e;

    public SearchService(E e) {
        this.e = e;
    }

    public T find(SearchCriteria sc) {
        BooleanExpression predicate = buildPredicate(sc);
        return e.findOne(predicate);
    }

    public Page<T> findAll(SearchCriteria sc, Pageable pageable) {
        BooleanExpression predicate = buildPredicate(sc);
        return e.findAll(predicate, pageable);
    }

    public BooleanExpression buildPredicate(SearchCriteria sc) {
        BooleanExpression predicate = null;
        Map<Object, BooleanExpression> criteria = sc.getCriteria();
        for (Object o : criteria.keySet()) {
            predicate = PredicateUtil.setExpression(predicate, criteria.get(o));
        }

        return predicate;
    }

    public void save(T t) {
        e.save(t);
    }
}
