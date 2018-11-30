ALTER TABLE fixity
    ADD CONSTRAINT fk_fixity_file FOREIGN KEY (file_id) REFERENCES file;

ALTER TABLE ace_token
    ADD CONSTRAINT fk_token_file FOREIGN KEY (file_id) REFERENCES file;

ALTER TABLE ace_token
    ADD CONSTRAINT fk_token_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE staging_storage
    ADD CONSTRAINT fk_storage_file FOREIGN KEY (file_id) REFERENCES file;

ALTER TABLE staging_storage
    ADD CONSTRAINT fk_storage_bag FOREIGN KEY (bag_id) REFERENCES bag;
