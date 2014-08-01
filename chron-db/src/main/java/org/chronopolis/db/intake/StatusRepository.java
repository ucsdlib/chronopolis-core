package org.chronopolis.db.intake;

import org.chronopolis.db.intake.model.Status;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by shake on 8/1/14.
 */
@Transactional
public interface StatusRepository extends CrudRepository<Status, String> {

    Status findById(String id);

}
