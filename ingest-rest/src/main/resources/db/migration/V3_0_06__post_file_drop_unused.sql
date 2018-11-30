ALTER TABLE fixity DROP COLUMN storage_id;
ALTER TABLE bag DROP COLUMN required_replications;

DROP TABLE bag_storage;
DROP TABLE token_storage;
