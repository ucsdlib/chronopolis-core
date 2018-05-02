-- create two bags, one which we will request all tokens from and one which we will have partial tokens
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications) VALUES(DEFAULT, CURRENT_DATE, CURRENT_DATE, 'new-bag-1', 'admin', 1, 'DEPOSITED', 1, 1, 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications) VALUES(DEFAULT, CURRENT_DATE, CURRENT_DATE, 'new-bag-2', 'admin', 1, 'DEPOSITED', 1, 1, 3);

-- create a third bag which has all tokens made
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications) VALUES(DEFAULT, CURRENT_DATE, CURRENT_DATE, 'new-bag-3', 'admin', 1, 'DEPOSITED', 1, 1, 3);

INSERT INTO staging_storage VALUES(DEFAULT, 1, 't', 'test-depositor/new-bag-1', 1500, 3, CURRENT_DATE, CURRENT_DATE);
INSERT INTO bag_storage VALUES((SELECT id FROM bag where name = 'new-bag-1'), (SELECT id FROM staging_storage));
INSERT INTO bag_storage VALUES((SELECT id FROM bag where name = 'new-bag-2'), (SELECT id FROM staging_storage));

-- insert a token for the hello_world file
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, (SELECT id FROM bag WHERE name = 'new-bag-2'));

-- create the three tokens for new-bag-3
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, (SELECT id FROM bag WHERE name = 'new-bag-3'));
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/manifest-sha256.txt', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, (SELECT id FROM bag WHERE name = 'new-bag-3'));
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/tagmanifest-sha256.txt', 'test-proof-hw', 'ims-host', 'SHA-256', 'SHA-256', 1, (SELECT id FROM bag WHERE name = 'new-bag-3'));
