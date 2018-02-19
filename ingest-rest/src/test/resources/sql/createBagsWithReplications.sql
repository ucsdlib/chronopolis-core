INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'admin', 'test-depositor', 'STAGED', 1, 1, 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-1', 'admin', 'test-depositor', 'STAGED',  1, 1, 3);

-- Kind of janky but... should be ok... want to revisit how we create test data anyways
INSERT INTO bag_distribution(id, bag_id, node_id, status) VALUES (DEFAULT, (SELECT id FROM bag WHERE name = 'bag-0'), 1, 'REPLICATE');
INSERT INTO bag_distribution(id, bag_id, node_id, status) VALUES (DEFAULT, (SELECT id FROM bag WHERE name = 'bag-0'), 2, 'REPLICATE');
