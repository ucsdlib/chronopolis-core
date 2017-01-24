package org.chronopolis.ingest.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import javax.transaction.Transactional;
import java.io.Serializable;

/**
 *
 * Created by shake on 1/24/17.
 */
// @Component
@Transactional
public class RepairService<T, ID extends Serializable, E extends JpaRepository<T, ID> & QueryDslPredicateExecutor<T>> extends SearchService<T, ID, E> {
    private final Logger log = LoggerFactory.getLogger(RepairService.class);

    public RepairService(E jpaRepository) {
        super(jpaRepository);
    }
}
