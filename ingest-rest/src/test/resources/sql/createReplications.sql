-- Create some bags for the foreign keys
INSERT INTO bag VALUES(1, 'bag-0', 'test-depositor', 'bags/test-bag-0', 'tokens/test-bag-0', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(2, 'bag-1', 'test-depositor', 'bags/test-bag-1', 'tokens/test-bag-1', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);

-- Then create some replications
INSERT INTO replication VALUES (1, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', '', '', 1, 4);
INSERT INTO replication VALUES (2, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', '', '', 1, 3);
INSERT INTO replication VALUES (3, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', '', '', 1, 2);
INSERT INTO replication VALUES (4, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', '', '', 1, 1);

INSERT INTO replication VALUES (5, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', '', '', 2, 4);
INSERT INTO replication VALUES (6, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', '', '', 2, 3);
INSERT INTO replication VALUES (7, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', '', '', 2, 2);
INSERT INTO replication VALUES (8, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', '', '', 2, 1);
