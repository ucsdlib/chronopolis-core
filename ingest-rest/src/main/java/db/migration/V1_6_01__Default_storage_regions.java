package db.migration;

import org.chronopolis.rest.models.storage.DataType;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Migration to create a default storage region for bags and tokens
 * in the ingest server
 *
 * Created by shake on 7/12/17.
 */
public class V1_6_01__Default_storage_regions implements JdbcMigration {
    private final Logger log = LoggerFactory.getLogger(V1_6_01__Default_storage_regions.class);

    @Override
    public void migrate(Connection connection) throws Exception {
        long numBags = 0;
        boolean created = false;
        String count = "SELECT count(id) FROM bag";
        String select = "SELECT id FROM node ORDER BY id LIMIT 1";
        String insert = "INSERT INTO storage_region (id, node_id, data_type, storage_type, capacity, created_at, updated_at) VALUES(DEFAULT, ?, ?, 'LOCAL', 1000000000000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (PreparedStatement countBag = connection.prepareStatement(count);
             PreparedStatement selectNode = connection.prepareStatement(select);
             PreparedStatement insertBagSR = connection.prepareStatement(insert);
             PreparedStatement insertTokenSR = connection.prepareStatement(insert)) {
            ResultSet nodeSet = selectNode.executeQuery();
            ResultSet countSet = countBag.executeQuery();

            while (countSet.next()) {
                numBags = countSet.getLong(1);
                log.info("Bag count is {}", numBags);
            }

           // If no nodes exist and no bags exist, this should do nothing
            while (nodeSet.next()) {
                created = true;
                long id = nodeSet.getLong(1);
                log.info("First node is {}", id);
                insertBagSR.setLong(1, id);
                insertBagSR.setString(2, DataType.BAG.name());
                insertTokenSR.setLong(1, id);
                insertTokenSR.setString(2, DataType.TOKEN.name());

                // Let the exception come through so that flyway can rollback the transaction
                log.info("Attempting to execute inserts for storage regions");
                insertBagSR.execute();
                insertTokenSR.execute();
            }

            // If we have multiple bags but couldn't find a node -> fail
            if (numBags > 0 && !created) {
                throw new Exception("Unable to create default StorageRegion without existing node for bags");
            }
        }
    }

}
