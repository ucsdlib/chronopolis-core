package org.chronopolis.ingest.repository.dao;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @deprecated will be removed by 3.0
 *
 * Generalized service for access database objects. Uses SearchCriteria to build queries, as well
 * as offers a basic save method.
 *
 * Created by shake on 1/24/17.
 */
@Deprecated
@Transactional
public class SearchService<T, I extends Serializable, E extends JpaRepository<T, I> & QueryDslPredicateExecutor<T>> {

    private final E e;

    public SearchService(E e) {
        this.e = e;
    }

    public void delete(T t) {
        e.delete(t);
    }

    public T find(SearchCriteria sc) {
        Predicate predicate = buildPredicate(sc);
        return e.findOne(predicate);
    }

    public List<T> findAll() {
        return e.findAll();
    }

    public Page<T> findAll(SearchCriteria sc, Pageable pageable) {
        Predicate predicate = buildPredicate(sc);
        return e.findAll(predicate, pageable);
    }

    private Predicate buildPredicate(SearchCriteria sc) {
        Map<Object, BooleanExpression> criteria = sc.getCriteria();
        BooleanBuilder builder = new BooleanBuilder();
        for (Object o : criteria.keySet()) {
            builder.and(criteria.get(o));
        }

        return builder.getValue();
    }

    public void save(T t) {
        e.save(t);
    }
}
