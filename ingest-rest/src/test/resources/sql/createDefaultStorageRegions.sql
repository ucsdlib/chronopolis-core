INSERT INTO storage_region VALUES(1, 1, 'BAG', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);
INSERT INTO storage_region VALUES(2, 1, 'TOKEN', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);

INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES (1, CURRENT_DATE, CURRENT_DATE, 'BAG_MERGE', 'admin', 'TEST', 'STAGED', 1, 1, 3);
INSERT INTO bag(id, created_at, updated_at, name, creator, depositor, status, size, total_files, required_replications) VALUES (2, CURRENT_DATE, CURRENT_DATE, 'TOKEN_MERGE', 'admin', 'TEST', 'STAGED', 1, 1, 3);
