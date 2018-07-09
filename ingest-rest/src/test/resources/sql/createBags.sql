    -- INSERT INTO storage_region VALUES(DEFAULT, 1, 'BAG', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);
    INSERT INTO replication_config VALUES (DEFAULT, 1, 'test-server', 'test-replication-user', 'test-replication-path');
    INSERT INTO staging_storage VALUES (DEFAULT, 1, 't', 'bags/test-location', 1500, 3, CURRENT_DATE, CURRENT_DATE);
    INSERT INTO fixity VALUES (DEFAULT, (SELECT currval('staging_storage_id_seq')), 'test-alg', 'test-fixity', CURRENT_DATE);

    -- INSERT INTO storage_region VALUES(DEFAULT, 1, 'TOKEN', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);
    INSERT INTO replication_config VALUES (DEFAULT, 2, 'test-server', 'test-replication-user', 'test-replication-path');
    INSERT INTO staging_storage VALUES (DEFAULT, 2, 't', 'tokens/test-location', 1500, 3, CURRENT_DATE, CURRENT_DATE);
    INSERT INTO fixity VALUES (DEFAULT, (SELECT currval('staging_storage_id_seq')), 'test-alg', 'test-fixity', CURRENT_DATE);

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-1', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-2', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-3', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-4', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-5', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-6', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-7', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-8', 'admin', 1, 'DEPOSITED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    INSERT INTO bag (id, created_at, updated_at, name, creator, depositor_id, status, size, total_files, required_replications)
    VALUES (DEFAULT, CURRENT_DATE, CURRENT_DATE, 'bag-9', 'admin', 1, 'TOKENIZED', 1, 1, 3);
    INSERT INTO bag_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'bags/test-location'));
    INSERT INTO token_storage VALUES ((SELECT currval('bag_id_seq')), (SELECT id FROM staging_storage WHERE path = 'tokens/test-location'));

    -- Create some distribution records too
    INSERT INTO bag_distribution VALUES (1, (SELECT currval('bag_id_seq')), 1, 'DISTRIBUTE');
    INSERT INTO bag_distribution VALUES (2, (SELECT currval('bag_id_seq')), 2, 'DISTRIBUTE');
    INSERT INTO bag_distribution VALUES (3, (SELECT currval('bag_id_seq')), 4, 'DISTRIBUTE');
    INSERT INTO bag_distribution VALUES (4, (SELECT currval('bag_id_seq')), 3, 'REPLICATE');
