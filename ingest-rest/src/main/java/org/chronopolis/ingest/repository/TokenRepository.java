package org.chronopolis.ingest.repository;

import org.chronopolis.rest.models.AceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by shake on 2/5/15.
 */
public interface TokenRepository extends JpaRepository<AceToken, Long> {

    List<AceToken> findByBagID(Long bagID);

    Long countByBagID(Long bagID);

}
