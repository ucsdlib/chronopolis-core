CREATE INDEX CONCURRENTLY idx_file_bag ON file (bag_id);
CREATE INDEX CONCURRENTLY idx_ace_token_bag ON ace_token (bag_id);
CREATE UNIQUE INDEX CONCURRENTLY idx_file_unique_bag_filename ON file (bag_id, filename);
