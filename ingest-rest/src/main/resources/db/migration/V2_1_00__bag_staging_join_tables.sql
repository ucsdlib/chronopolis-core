-- join tables for bag <-> staging relationship
-- bag storage
DROP TABLE IF EXISTS bag_storage;
CREATE TABLE bag_storage (
    bag_id BIGINT NOT NULL,
    staging_id BIGINT NOT NULL
);

-- token storage
DROP TABLE IF EXISTS token_storage;
CREATE TABLE token_storage (
    bag_id BIGINT NOT NULL,
    staging_id BIGINT NOT NULL
);

-- FKs
ALTER TABLE bag_storage
    ADD CONSTRAINT FK_bs_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE token_storage
    ADD CONSTRAINT FK_ts_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE bag_storage
    ADD CONSTRAINT FK_bs_storage FOREIGN KEY (staging_id) REFERENCES staging_storage;

ALTER TABLE token_storage
    ADD CONSTRAINT FK_ts_storage FOREIGN KEY (staging_id) REFERENCES staging_storage;

