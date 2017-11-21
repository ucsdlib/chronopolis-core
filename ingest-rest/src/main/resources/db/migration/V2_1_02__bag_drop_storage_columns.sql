-- remove fk constraint + drop columns
ALTER TABLE bag DROP CONSTRAINT IF EXISTS fk_bag_storage;
ALTER TABLE bag DROP CONSTRAINT IF EXISTS fk_token_storage;
ALTER TABLE bag DROP COLUMN bag_storage_id;
ALTER TABLE bag DROP COLUMN token_storage_id;