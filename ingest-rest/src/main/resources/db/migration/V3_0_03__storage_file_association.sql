-- staging_storage bag_id
UPDATE staging_storage SET bag_id = bag_storage.bag_id
  FROM bag_storage
  WHERE bag_storage.staging_id = staging_storage.id;

UPDATE staging_storage SET bag_id = token_storage.bag_id
  FROM token_storage
  WHERE token_storage.staging_id = staging_storage.id;

-- staging_storage file_id
-- it's easier to use with to handle multiple joins compared to doing them with
-- the UPDATE... FROM t1, t2... syntax
-- by default ONLY set either tagmanifest OR a token_store
WITH tb AS (SELECT ss.id AS rowid, f.id AS file_id FROM staging_storage ss
              JOIN bag_storage bs ON ss.id = bs.staging_id
              JOIN file f ON ss.bag_id = f.bag_id
              WHERE f.filename = 'tagmanifest-sha256.txt')
  UPDATE staging_storage
  SET file_id = tb.file_id FROM tb WHERE id = tb.rowid;

WITH tb AS (SELECT ss.id AS rowid, f.id AS file_id FROM staging_storage ss
              JOIN token_storage ts ON ss.id = ts.staging_id
              JOIN file f ON ss.bag_id = f.bag_id
              WHERE f.dtype = 'TOKEN')
  UPDATE staging_storage
  SET file_id = tb.file_id FROM tb WHERE id = tb.rowid;
