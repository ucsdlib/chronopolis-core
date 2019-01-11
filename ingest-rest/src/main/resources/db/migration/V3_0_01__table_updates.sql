-- These will be set not null after data migrations happen
ALTER TABLE fixity ADD COLUMN file_id bigint;
ALTER TABLE staging_storage ADD COLUMN bag_id bigint;
ALTER TABLE staging_storage ADD COLUMN file_id bigint;
