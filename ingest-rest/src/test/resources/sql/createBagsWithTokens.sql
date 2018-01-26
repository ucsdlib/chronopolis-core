-- create two bags, one which we will request all tokens from and one which we will have partial tokens
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES(1, CURRENT_DATE, CURRENT_DATE, 'new-bag-1', 'admin', 'test-depositor', 'STAGED', 1, 1, 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES(2, CURRENT_DATE, CURRENT_DATE, 'new-bag-2', 'admin', 'test-depositor', 'STAGED', 1, 1, 3);

-- create a third bag which has all tokens made
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES(3, CURRENT_DATE, CURRENT_DATE, 'new-bag-3', 'admin', 'test-depositor', 'STAGED', 1, 1, 3);

INSERT INTO storage_region VALUES(1, 1, 'BAG', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);
INSERT INTO storage_region VALUES(2, 1, 'TOKEN', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);
INSERT INTO staging_storage VALUES(1, 1, 1, 'test-depositor/new-bag-1', 1500, 3, CURRENT_DATE, CURRENT_DATE);
-- INSERT INTO storage VALUES(2, 2, 1, 'tokens/test-location', 1500, 3, CURRENT_DATE, CURRENT_DATE);
INSERT INTO bag_storage VALUES(1, 1);
INSERT INTO bag_storage VALUES(2, 1);
-- UPDATE bag SET bag_storage_id = 1; --, token_storage_id = 2;

-- insert a token for the hello_world file
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, 2);

-- create the three tokens for new-bag-3
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, 3);
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/manifest-sha256.txt', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, 3);
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/tagmanifest-sha256.txt', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, 3);
