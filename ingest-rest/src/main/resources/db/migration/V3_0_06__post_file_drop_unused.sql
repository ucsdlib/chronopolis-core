ALTER TABLE fixity DROP COLUMN storage_id;
ALTER TABLE ace_token DROP COLUMN filename;
ALTER TABLE bag DROP COLUMN required_replications;

DROP TABLE bag_storage;
DROP TABLE token_storage;
