-- These will be set not null after data migrations happen
ALTER TABLE fixity ADD COLUMN file_id bigint;
ALTER TABLE ace_token ADD COLUMN file_id bigint;
ALTER TABLE ace_token RENAME COLUMN bag TO bag_id;
ALTER TABLE staging_storage ADD COLUMN bag_id bigint;
ALTER TABLE staging_storage ADD COLUMN file_id bigint;
