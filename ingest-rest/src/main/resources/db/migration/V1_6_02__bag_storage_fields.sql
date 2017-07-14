ALTER TABLE bag ADD COLUMN bag_storage_id BIGINT;
ALTER TABLE bag ADD COLUMN token_storage_id BIGINT;

ALTER TABLE bag
    ADD CONSTRAINT FK_sr_bag FOREIGN KEY (bag_storage_id) REFERENCES storage;
ALTER TABLE bag
    ADD CONSTRAINT FK_sr_token FOREIGN KEY (token_storage_id) REFERENCES storage;
