INSERT INTO bag VALUES(1, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'test-depositor', 'bags/test-bag-0', 'tokens/test-bag-0', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(2, CURRENT_DATE, CURRENT_DATE, 'bag-1', 'test-depositor', 'bags/test-bag-1', 'tokens/test-bag-1', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);

INSERT INTO bag_distribution(id, bag_id, node_id, status) VALUES (DEFAULT, 1, 1, 'REPLICATE');
INSERT INTO bag_distribution(id, bag_id, node_id, status) VALUES (DEFAULT, 1, 2, 'REPLICATE');
