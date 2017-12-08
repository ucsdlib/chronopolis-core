INSERT INTO bag_storage(bag_id, staging_id)
    SELECT id, bag_storage_id FROM bag WHERE bag_storage_id IS NOT NULL;

INSERT INTO token_storage(bag_id, staging_id)
    SELECT id, token_storage_id FROM bag WHERE token_storage_id IS NOT NULL;