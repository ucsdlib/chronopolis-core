CREATE OR REPLACE FUNCTION clean_all() RETURNS void AS $BODY$
BEGIN
    -- delete test data only (i.e. not from data.sql)
    DELETE FROM staging_storage;
    DELETE FROM bag_distribution;
    DELETE FROM ace_token;
    DELETE FROM fixity;
    DELETE FROM file;
    DELETE FROM repair_file;
    DELETE FROM strategy;
    DELETE FROM repair;
    DELETE FROM replication;
    DELETE FROM bag;
END;
$BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_file(bag_id bigint, filename text, dtype text) RETURNS bigint AS $BODY$
DECLARE
    file_id bigint;
BEGIN
    -- file
    EXECUTE format('INSERT INTO file (bag_id, filename, size, dtype) VALUES ($1, $2, 1, $3) RETURNING id')
    INTO file_id
    USING bag_id, filename, dtype;

    -- file fixity
    EXECUTE format('INSERT INTO fixity (value, algorithm, file_id) VALUES ($1, $2, $3)')
    USING 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', 'SHA-256', file_id;
    return file_id;
END;
$BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_tokens(bag_id bigint, num_tokens integer) RETURNS void AS $BODY$
BEGIN
    EXECUTE format('INSERT INTO ace_token (create_date, proof, ims_service, ims_host, algorithm, round, bag_id, file_id)
                    SELECT current_timestamp, $4, $5, $6, $4, 1, bag_id, id
                    FROM file WHERE bag_id = $1 AND dtype = $2 LIMIT $3')
    USING bag_id, 'BAG', num_tokens, 'test-proof', 'SHA-256', 'test-host';
END;
$BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_distributions(bag_id bigint, status text, num integer) RETURNS VOID AS $BODY$
BEGIN
    EXECUTE FORMAT('INSERT INTO bag_distribution (bag_id, node_id, status)
                    SELECT $1, id, $2
                    FROM node LIMIT $3')
    USING bag_id, status, num;
END;
$BODY$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION create_bags() RETURNS void AS
$BODY$
DECLARE
    bag_id    bigint;
    bagname   text;
    file_id   bigint;
    region_id bigint;
BEGIN
    -- this is equivalent to running sha-256 on an empty string
    FOR i in 0..9 LOOP
        bagname := concat('bag-', i);
        -- bag
        EXECUTE format('INSERT INTO bag (name, creator, depositor_id, status, size, total_files) VALUES ($1, $2, 1, $3, 1, 3) RETURNING id')
        INTO bag_id
        USING bagname, 'test-admin', 'DEPOSITED';

        -- manifest-sha256
        PERFORM create_file(bag_id, '/manifest-sha256.txt', 'BAG');

        -- tagmanifest + staging
        SELECT id FROM storage_region WHERE data_type = 'BAG' INTO region_id;
        SELECT create_file(bag_id, '/tagmanifest-sha256.txt', 'BAG') INTO file_id;
        EXECUTE format('INSERT INTO staging_storage (bag_id, file_id, region_id, active, path, size, total_files)
                        VALUES ($1, $2, $3, TRUE, $4, 1500, 3)')
        USING bag_id, file_id, region_id, bagname;
    END LOOP;

    -- have one bag TOKENIZED and ready to be replicated
    UPDATE bag SET status = 'TOKENIZED' where id = bag_id;
    SELECT id FROM storage_region WHERE data_type = 'TOKEN' INTO region_id;
    SELECT create_file(bag_id, '/token_store', 'TOKEN_STORE') INTO file_id;
    EXECUTE format('INSERT INTO staging_storage (bag_id, file_id, region_id, active, path, size, total_files)
                    VALUES ($1, $2, $3, TRUE, $4, 1500, 1)')
        USING bag_id, file_id, region_id, 'token-store';
    -- todo: figure out how to set one dist to 'REPLICATE'
    PERFORM create_distributions(bag_id, 'DISTRIBUTE', 4);

    -- create tokens: we want bag-1 (1 token) and bag-3 (3 tokens) to be updated, SO...
    SELECT id from bag where name = 'bag-2' INTO bag_id;
    PERFORM create_tokens(bag_id, 1);
    SELECT id from bag where name = 'bag-3' INTO bag_id;
    PERFORM create_tokens(bag_id, 2);

    -- and create a few more distribution objects
    SELECT id from bag where name = 'bag-0' INTO bag_id;
    PERFORM create_distributions(bag_id, 'DISTRIBUTE', 2);
END;
$BODY$ LANGUAGE plpgsql;
