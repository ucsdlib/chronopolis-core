-- file entity creation (just pull bag and filename from ace_token)
INSERT INTO file(bag_id, filename, dtype)
    SELECT bag_id, filename, ('BAG') FROM ace_token;

-- token entity creation (pull path from storage)
-- regexp replace up to the first '/' so we only get the token store name
INSERT INTO file(size, bag_id, created_at, filename, dtype)
  SELECT size, ts.bag_id, created_at, regexp_replace(path, '.*/(.*)', '\1'), ('TOKEN_STORE')
  FROM token_storage ts
  JOIN staging_storage ON (ts.staging_id = staging_storage.id);

-- fixity for bag files (that we have)
-- similar to storage association
WITH tb AS (SELECT fixity.id AS rowid, file.id AS file_id FROM bag_storage bs
              JOIN fixity ON (bs.staging_id = fixity.storage_id)
              JOIN file ON (bs.bag_id = file.bag_id)
              WHERE file.dtype = 'BAG')
    UPDATE fixity
    SET file_id = tb.file_id FROM tb WHERE id = rowid;

-- fixity for token stores
WITH tb AS (SELECT fixity.id AS rowid, file.id AS file_id FROM token_storage ts
              JOIN fixity ON (ts.staging_id = fixity.storage_id)
              JOIN file ON (ts.bag_id = file.bag_id)
              WHERE file.dtype = 'TOKEN_STORE')
  UPDATE fixity
  SET file_id = tb.file_id FROM tb WHERE id = rowid;

