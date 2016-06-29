-- Create some bags for the foreign keys
INSERT INTO bag VALUES(1, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'admin', 'test-depositor', 'bags/test-bag-0', 'tokens/test-bag-0', 'token-fixity', 'tag-fixity', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(2, CURRENT_DATE, CURRENT_DATE, 'bag-1', 'admin', 'test-depositor', 'bags/test-bag-1', 'tokens/test-bag-1', 'token-fixity', 'tag-fixity', 'STAGED', 'SHA-256', 1500, 5, 3);

-- With distribution records
-- id, bag_id, node_id, status
INSERT INTO bag_distribution VALUES(DEFAULT, 1, 1, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 1, 2, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 1, 3, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 1, 4, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 2, 1, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 2, 2, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 2, 3, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(DEFAULT, 2, 4, 'DISTRIBUTE');

-- Then create some replications
INSERT INTO replication VALUES (1, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', NULL, NULL, 1, 4);
INSERT INTO replication VALUES (2, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', NULL, NULL, 1, 3);
INSERT INTO replication VALUES (3, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', NULL, NULL, 1, 2);
INSERT INTO replication VALUES (4, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-1', 'rsync@node.org:/bag-1-tokens', 'rsync', NULL, NULL, 1, 1);

INSERT INTO replication VALUES (5, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', NULL, NULL, 2, 4);
INSERT INTO replication VALUES (6, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', NULL, NULL, 2, 3);
INSERT INTO replication VALUES (7, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', NULL, NULL, 2, 2);
INSERT INTO replication VALUES (8, CURRENT_DATE, CURRENT_DATE, 'PENDING', 'rsync@node.org:/bag-2', 'rsync@node.org:/bag-2-tokens', 'rsync', NULL, NULL, 2, 1);
