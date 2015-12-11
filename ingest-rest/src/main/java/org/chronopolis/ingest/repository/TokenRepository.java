package org.chronopolis.ingest.repository;

import org.chronopolis.rest.models.AceToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.util.List;

/**
 * Repository for interacting with {@link AceToken}s
 *
 *
 * Created by shake on 2/5/15.
 */
public interface TokenRepository extends JpaRepository<AceToken, Long> ,
                                         QueryDslPredicateExecutor<AceToken> {

    List<AceToken> findByBagId(Long bagId);
    Page<AceToken> findByBagId(Long bagId, Pageable pable);

    Long countByBagId(Long bagId);

}
