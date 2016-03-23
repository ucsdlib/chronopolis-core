-- create two bags, one which we will request all tokens from and one which we will have partial tokens
INSERT INTO bag VALUES(1, CURRENT_DATE, CURRENT_DATE, 'new-bag-1', 'admin', 'test-depositor', 'test-depositor/new-bag-1', 'test-depositor/new-bag-1-tokens', '', '', 'STAGED', 'SHA-256', 1500, 3, 3);
INSERT INTO bag VALUES(2, CURRENT_DATE, CURRENT_DATE, 'new-bag-2', 'admin', 'test-depositor', 'test-depositor/new-bag-1', 'test-depositor/new-bag-2-tokens', NULL, NULL, 'STAGED', 'SHA-256', 1500, 3, 3);

-- create a third bag which has all tokens made
INSERT INTO bag VALUES(3, CURRENT_DATE, CURRENT_DATE, 'new-bag-3', 'admin', 'test-depositor', 'test-depositor/new-bag-1', 'test-depositor/new-bag-3-tokens', '', '', 'STAGED', 'SHA-256', 1500, 3, 3);

-- insert a token for the hello_world file
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'SHA-256', 'SHA-256', 1, 2);

-- create the three tokens for new-bag-3
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'SHA-256', 'SHA-256', 1, 3);
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/manifest-sha256.txt', 'test-proof-hw', 'SHA-256', 'SHA-256', 1, 3);
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/tagmanifest-sha256.txt', 'test-proof-hw', 'SHA-256', 'SHA-256', 1, 3);
