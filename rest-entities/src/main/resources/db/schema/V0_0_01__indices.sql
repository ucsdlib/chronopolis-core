CREATE INDEX idx_token_file ON ace_token (bag_id, file_id);

-- V2_2_01 - depositor indices
CREATE INDEX idx_dd_dn ON depositor_distribution (depositor_id, node_id);
CREATE INDEX idx_depositor_ns ON depositor (namespace);
CREATE INDEX idx_depositor_contact ON depositor_contact (id, depositor_id);

-- V3_0_00 - file index
CREATE index file_bag_id_idx ON file (bag_id);
CREATE index file_filename_idx ON file (filename);
CREATE UNIQUE index file_unique_bag_filename ON file (bag_id, filename);
