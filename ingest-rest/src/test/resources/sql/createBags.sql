INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (1, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (2, CURRENT_DATE, CURRENT_DATE, 'bag-1', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (3, CURRENT_DATE, CURRENT_DATE, 'bag-2', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (4, CURRENT_DATE, CURRENT_DATE, 'bag-3', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (5, CURRENT_DATE, CURRENT_DATE, 'bag-4', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (6, CURRENT_DATE, CURRENT_DATE, 'bag-5', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (7, CURRENT_DATE, CURRENT_DATE, 'bag-6', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (8, CURRENT_DATE, CURRENT_DATE, 'bag-7', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (9, CURRENT_DATE, CURRENT_DATE, 'bag-8', 'admin', 'test-depositor', 'STAGED', 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, required_replications) VALUES (10, CURRENT_DATE, CURRENT_DATE, 'bag-9', 'admin', 'test-depositor', 'TOKENIZED', 3);

INSERT INTO storage_region VALUES(1, 1, 'BAG', 'LOCAL', 1000000, CURRENT_DATE, CURRENT_DATE);
INSERT INTO storage_region VALUES(2, 1, 'TOKEN', 'LOCAL', 1000000, CURRENT_DATE, CURRENT_DATE);
INSERT INTO replication_config VALUES(1, 1, 'test-server', 'test-replication-user', 'test-replication-path');
INSERT INTO replication_config VALUES(2, 2, 'test-server', 'test-replication-user', 'test-replication-path');
INSERT INTO storage VALUES(1, 1, 1, 'bags/test-location', 1500, 3, CURRENT_DATE, CURRENT_DATE);
INSERT INTO storage VALUES(2, 2, 1, 'tokens/test-location', 1500, 3, CURRENT_DATE, CURRENT_DATE);
UPDATE bag SET bag_storage_id = 1, token_storage_id = 2;

-- Create some distribution records too
INSERT INTO bag_distribution VALUES (1, 10, 1, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES (2, 10, 2, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES (3, 10, 4, 'DISTRIBUTE');
INSERT INTO bag_distribution VALUES (4, 10, 3, 'REPLICATE');
