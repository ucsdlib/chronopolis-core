package db.migration;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Create storage entries for each bag
 *
 * Created by shake on 7/13/17.
 */
public class V1_6_03__Bag_storage_entries implements JdbcMigration {
    private final Logger log = LoggerFactory.getLogger(V1_6_03__Bag_storage_entries.class);

    @Override
    public void migrate(Connection connection) throws Exception {
        String sqlSelectBags = "SELECT id, location, tag_manifest_digest, token_location, token_digest, size, total_files, fixity_algorithm FROM bag";
        String sqlSelectBagRegion = "SELECT id FROM storage_region WHERE data_type = 'BAG'";
        String sqlSelectTokenRegion = "SELECT id FROM storage_region WHERE data_type = 'TOKEN'";

        try (PreparedStatement bagsStatement = connection.prepareStatement(sqlSelectBags);
             PreparedStatement bagRegionStatement = connection.prepareStatement(sqlSelectBagRegion);
             PreparedStatement tokenRegionStatement = connection.prepareStatement(sqlSelectTokenRegion)) {
            // Setup the ids for our regions
            Long bagRegionId = -1L;
            Long tokenRegionId = -1L;
            ResultSet bagRegionSet = bagRegionStatement.executeQuery();
            ResultSet tokenRegionSet = tokenRegionStatement.executeQuery();

            // What if these are still -1??
            if (bagRegionSet.next() && tokenRegionSet.next()) {
                bagRegionId = bagRegionSet.getLong(1);
                tokenRegionId = tokenRegionSet.getLong(1);

                log.info("Found bag region {} and token region {}", bagRegionId, tokenRegionId);
            }

            ResultSet bags = bagsStatement.executeQuery();
            // It would be nice to batch this but I'm not sure how at this level :/
            while (bags.next()) {
                Long id = bags.getLong(1);
                String location = bags.getString(2);
                String tagDigest = bags.getString(3);
                String tokenLocation = bags.getString(4);
                String tokenDigest = bags.getString(5);
                Long size = bags.getLong(6);
                Long totalFiles = bags.getLong(7);
                String algorithm = bags.getString(8);

                Long bagStorageId = insertStorage(bagRegionId, location, size, totalFiles, connection);
                insertFixity(bagStorageId, algorithm, tagDigest, connection);

                // Maybe we shouldn't push storage for tokens?? I'm not really sure here, but we'll do it for now
                // Can always be changed before the release
                Long tokenStorageId = insertStorage(tokenRegionId, tokenLocation, 1L, 1L, connection);

                // Regardless, a little check in case the tokens have not been written yet. Bag storage id _should_ always be nonnull
                if (tokenStorageId == -1) {
                    updateBag(id, bagStorageId, connection);
                } else {
                    insertFixity(tokenStorageId, algorithm, tokenDigest, connection);
                    updateBagAndTokens(id, bagStorageId, tokenStorageId, connection);
                }
            }
        }
    }

    private void updateBagAndTokens(Long id, Long bagStorageId, Long tokenStorageId, Connection connection) throws SQLException {
        log.trace("Updating bag {}", id);
        String sqlUpdateBag = "UPDATE bag SET bag_storage_id = ?, token_storage_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sqlUpdateBag)) {
            statement.setLong(1, bagStorageId);
            statement.setLong(2, tokenStorageId);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    private void updateBag(Long id, Long bagStorageId, Connection connection) throws SQLException {
        log.trace("Updating bag {}", id);
        String sqlUpdateBag = "UPDATE bag SET bag_storage_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sqlUpdateBag)) {
            statement.setLong(1, bagStorageId);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    private void insertFixity(Long storage, String algorithm, String checksum, Connection connection) throws SQLException {
        String sqlInsertFixity = "INSERT INTO fixity(id, storage_id, algorithm, value, created_at)" +
                " VALUES(DEFAULT, ?, ?, ?, CURRENT_TIMESTAMP)";
        if (checksum != null && !checksum.isEmpty()) {
            try (PreparedStatement statement = connection.prepareStatement(sqlInsertFixity)) {
                log.trace("Pushing fixity: {} {}", algorithm, checksum);
                statement.setLong(1, storage);
                statement.setString(2, algorithm);
                statement.setString(3, checksum);
                statement.executeUpdate();
            }
        }
    }

    private Long insertStorage(Long region, String path, Long size, Long files, Connection connection) throws SQLException {
        Long key = -1L;
        String sqlInsertStorage = "INSERT INTO storage(id, region_id, active, path, size, total_files, updated_at, created_at)" +
                " VALUES(DEFAULT, ?, 'true', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        if (path != null && !path.isEmpty()) {
            ResultSet keys;
            try (PreparedStatement statement = connection.prepareStatement(sqlInsertStorage, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, region);
                statement.setString(2, path);
                statement.setLong(3, size);
                statement.setLong(4, files);
                statement.executeUpdate();
                keys = statement.getGeneratedKeys();
                if (keys.next()) {
                    key = keys.getLong(1);
                }
            }
        }

        log.trace("Created storage region {} for path {}", key, path);
        return key;
    }
}
