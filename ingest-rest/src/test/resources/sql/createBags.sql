INSERT INTO bag VALUES(1, 'bag-0', 'test-depositor', 'bags/test-bag-0', 'tokens/test-bag-0', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(2, 'bag-1', 'test-depositor', 'bags/test-bag-1', 'tokens/test-bag-1', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(3, 'bag-2', 'test-depositor', 'bags/test-bag-2', 'tokens/test-bag-2', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(4, 'bag-3', 'test-depositor', 'bags/test-bag-3', 'tokens/test-bag-3', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(5, 'bag-4', 'test-depositor', 'bags/test-bag-4', 'tokens/test-bag-4', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(6, 'bag-5', 'test-depositor', 'bags/test-bag-5', 'tokens/test-bag-5', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(7, 'bag-6', 'test-depositor', 'bags/test-bag-6', 'tokens/test-bag-6', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(8, 'bag-7', 'test-depositor', 'bags/test-bag-7', 'tokens/test-bag-7', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(9, 'bag-8', 'test-depositor', 'bags/test-bag-8', 'tokens/test-bag-8', '', '', 'STAGED', 'SHA-256', 1500, 5, 3);
INSERT INTO bag VALUES(10, 'bag-9', 'test-depositor', 'bags/test-bag-9', 'tokens/test-bag-9', '', '', 'TOKENIZED', 'SHA-256', 1500, 5, 3);

-- Create some distribution records too
INSERT INTO bag_distribution VALUES(1, 10, 1, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(2, 10, 2, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(3, 10, 4, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES(4, 10, 3, 'REPLICATE');
