-- file entity creation (just pull bag and filename from ace_token)
INSERT INTO file(bag_id, filename, dtype)
    SELECT bag_id, filename, ('BAG') FROM ace_token;

-- token entity creation (pull path from storage)
-- regexp replace up to the first '/' so we only get the token store name
INSERT INTO file(size, bag_id, created_at, filename, dtype)
  SELECT size, ts.bag_id, created_at, regexp_replace(path, '.*/(.*)', '\1'), ('TOKEN_STORE')
  FROM token_storage ts
  JOIN staging_storage ON (ts.staging_id = staging_storage.id);

-- bag file fixity (file_id, fixity_id)
-- retrieve the fixity_id and file_id for only a tagmanifest file
INSERT INTO file_fixity(file_id, fixity_id)
    SELECT file.id, fixity.id FROM bag_storage
      JOIN fixity ON (bag_storage.staging_id = fixity.storage_id)
      JOIN file ON (bag_storage.bag_id = file.bag_id)
      WHERE file.filename = 'tagmanifest-sha256.txt';

-- token file fixity
-- a bit harder to say 'token filename' so just join on a token_store type
INSERT INTO file_fixity(file_id, fixity_id)
    SELECT file.id, fixity.id FROM token_storage
      JOIN fixity ON (token_storage.staging_id = fixity.storage_id)
      JOIN file ON (token_storage.bag_id = file.bag_id)
      WHERE file.dtype = 'TOKEN_STORE';
