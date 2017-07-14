-- may be 2.0 I'm not sure here
-- Do we need separate id generators? can some be shared? I don't see any reason why not
-- storage_region
DROP TABLE IF EXISTS storage_region;
DROP SEQUENCE IF EXISTS storage_region_id_seq;
CREATE SEQUENCE storage_region_id_seq;
CREATE TABLE storage_region (
    id BIGINT PRIMARY KEY DEFAULT nextval('storage_region_id_seq'),
    node_id BIGINT NOT NULL,
    data_type VARCHAR(255) NOT NULL,
    storage_type VARCHAR(255) NOT NULL,
    capacity BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- storage
-- note the size/total_files might still live in the bag, but are here for now as their usage emerges
DROP TABLE IF EXISTS storage;
DROP SEQUENCE IF EXISTS storage_id_seq;
CREATE SEQUENCE storage_id_seq;
CREATE TABLE storage (
    id BIGINT PRIMARY KEY DEFAULT nextval('storage_id_seq'),
    region_id BIGINT NOT NULL,
    active BOOLEAN,
    path VARCHAR(255),
    size BIGINT,
    total_files BIGINT,
    -- checksum VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- OneToMany makes more sense but... yea.
DROP TABLE IF EXISTS fixity;
DROP SEQUENCE IF EXISTS fixity_id_seq;
CREATE SEQUENCE fixity_id_seq;
CREATE TABLE fixity (
    id BIGINT PRIMARY KEY DEFAULT nextval('fixity_id_seq'),
    storage_id BIGINT NOT NULL,
    algorithm VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);

-- replication_config
DROP TABLE IF EXISTS replication_config;
DROP SEQUENCE IF EXISTS replication_config_id_seq;
CREATE SEQUENCE replication_config_id_seq;
CREATE TABLE replication_config(
    id BIGINT  PRIMARY KEY DEFAULT nextval('replication_config_id_seq'),
    region_id BIGINT NOT NULL,
    server VARCHAR(255) NOT NULL,
    username VARCHAR(255), --nullable
    path VARCHAR(255)
);

-- FKs
ALTER TABLE storage_region
    ADD CONSTRAINT FK_sr_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE storage
    ADD CONSTRAINT FK_storage_sr FOREIGN KEY (region_id) REFERENCES storage_region;

ALTER TABLE fixity
    ADD CONSTRAINT FK_fixity_storage FOREIGN KEY (storage_id) REFERENCES storage;

ALTER TABLE replication_config
    ADD CONSTRAINT FK_rc_sr FOREIGN KEY (region_id) REFERENCES storage_region;
