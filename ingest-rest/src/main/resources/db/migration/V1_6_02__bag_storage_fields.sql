ALTER TABLE bag ADD COLUMN bag_storage_id BIGINT;
ALTER TABLE bag ADD COLUMN token_storage_id BIGINT;

ALTER TABLE bag
    ADD CONSTRAINT FK_bag_storage FOREIGN KEY (bag_storage_id) REFERENCES staging_storage;
ALTER TABLE bag
    ADD CONSTRAINT FK_token_storage FOREIGN KEY (token_storage_id) REFERENCES staging_storage;
