INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES (1, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'admin', 'test-depositor', 'STAGED', 1, 1, 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES (2, CURRENT_DATE, CURRENT_DATE, 'bag-1', 'admin', 'test-depositor', 'STAGED',  1, 1, 3);

INSERT INTO bag_distribution(id, bag_id, node_id, status) VALUES (DEFAULT, 1, 1, 'REPLICATE');
INSERT INTO bag_distribution(id, bag_id, node_id, status) VALUES (DEFAULT, 1, 2, 'REPLICATE');
