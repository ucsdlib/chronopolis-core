package org.chronopolis.ingest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Respect my authoritah
 *
 * Created by shake on 3/24/16.
 */
public interface AuthoritiesRepository extends JpaRepository<Authority, String> {
}
