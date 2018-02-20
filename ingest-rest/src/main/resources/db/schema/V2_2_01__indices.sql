CREATE INDEX CONCURRENTLY idx_filename ON ace_token (bag, filename);
CREATE INDEX CONCURRENTLY idx_bag_storage ON bag_storage (bag_id, staging_id);
CREATE INDEX CONCURRENTLY idx_token_storage ON token_storage (bag_id, staging_id);

-- V2_2_01 - depositor indices
CREATE INDEX idx_dd_dn ON depositor_distribution (depositor_id, node_id);

CREATE INDEX idx_depositor_ns ON depositor (namespace);
CREATE INDEX idx_depositor_contact ON depositor_contact (id, depositor_id);