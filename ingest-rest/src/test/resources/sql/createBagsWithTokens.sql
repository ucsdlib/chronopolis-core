-- create two bags, one which we will request all tokens from and one which we will have partial tokens
INSERT INTO bag VALUES(1, 'new-bag-1', 'test-depositor', 'test-depositor/new-bag-1', 'test-depositor/new-bag-1-tokens', '', '', 'STAGED', 'SHA-256', 1500, 3, 3);
INSERT INTO bag VALUES(2, 'new-bag-2', 'test-depositor', 'test-depositor/new-bag-1', 'test-depositor/new-bag-2-tokens', '', '', 'STAGED', 'SHA-256', 1500, 3, 3);

-- insert a token for the hello_world file
INSERT INTO ace_token VALUES(DEFAULT, CURRENT_DATE, '/data/hello_world', 'test-proof-hw', 'SHA-256', 'SHA-256', 1, 2);
