-- indices
CREATE INDEX CONCURRENTLY idx_bag_storage ON bag_storage (bag_id, staging_id);
CREATE INDEX CONCURRENTLY idx_token_storage ON token_storage (bag_id, staging_id);