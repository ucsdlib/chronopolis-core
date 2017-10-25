-- POSTGRESQL schema

-- tables used by spring security
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    username varchar(256),
    password varchar(256),
    enabled boolean
);

DROP TABLE IF EXISTS authorities;
create table authorities (
    username varchar(256),
    authority varchar(256)
);

-- our entities
DROP TABLE IF EXISTS bag CASCADE;
DROP SEQUENCE IF EXISTS bag_id_seq;
CREATE SEQUENCE bag_id_seq;
CREATE TABLE bag (
    id bigint PRIMARY KEY DEFAULT nextval('bag_id_seq'),
    bag_storage_id BIGINT,
    token_storage_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    name varchar(255) UNIQUE,
    creator VARCHAR(255),
    depositor varchar(255),
    status varchar(255),
    size bigint NOT NULL,
    total_files bigint NOT NULL,
    required_replications int
);

DROP TABLE IF EXISTS node CASCADE;
DROP SEQUENCE IF EXISTS node_id_seq;
CREATE SEQUENCE node_id_seq;
CREATE TABLE node (
    id bigint PRIMARY KEY DEFAULT nextval('node_id_seq'),
    enabled boolean,
    username varchar(255) UNIQUE,
    password varchar(255)
);

DROP TABLE IF EXISTS replication;
DROP SEQUENCE IF EXISTS replication_replicationid_seq;
CREATE SEQUENCE replication_replicationid_seq;
CREATE TABLE replication (
    id bigint PRIMARY KEY DEFAULT nextval('replication_replicationid_seq'),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    status varchar(255),
    bag_link varchar(255),
    token_link varchar(255),
    protocol varchar(255),
    received_tag_fixity varchar(255),
    received_token_fixity varchar(255),
    bag_id bigint,
    node_id bigint
);

DROP TABLE IF EXISTS restoration;
DROP SEQUENCE IF EXISTS restoration_restoration_id_seq;
CREATE SEQUENCE restoration_restoration_id_seq;
CREATE TABLE restoration (
    restoration_id bigint PRIMARY KEY DEFAULT nextval('restoration_restoration_id_seq'),
    depositor varchar(255),
    link varchar(255),
    name varchar(255),
    protocol varchar(255),
    status varchar(255),
    node_id bigint
);

DROP TABLE IF EXISTS ace_token;
DROP SEQUENCE IF EXISTS ace_token_id_seq;
CREATE SEQUENCE ace_token_id_seq;
CREATE TABLE ace_token (
    id bigint PRIMARY KEY DEFAULT nextval('ace_token_id_seq'),
    create_date timestamp,
    filename text,
    proof text,
    ims_service varchar(255),
    algorithm varchar(255),
    round bigint,
    bag bigint
);

DROP SEQUENCE IF EXISTS bag_distribution_id_seq;
CREATE SEQUENCE bag_distribution_id_seq;
DROP TABLE IF EXISTS bag_distribution;
CREATE TABLE bag_distribution (
    id bigint PRIMARY KEY DEFAULT nextval('bag_distribution_id_seq'),
    bag_id bigint,
    node_id bigint,
    status varchar(255) -- DEFAULT 'DISTRIBUTE'
);

-- repair and associated tables
DROP TABLE IF EXISTS repair;
DROP SEQUENCE IF EXISTS repair_id_seq;
CREATE SEQUENCE repair_id_seq;
CREATE TABLE repair (
    id bigint PRIMARY KEY DEFAULT nextval('repair_id_seq'),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    audit VARCHAR(255),
    status VARCHAR(255),
    requester VARCHAR(255), -- maybe should be a bigint for the user_id instead? (when we update the user table)
    to_node BIGINT NOT NULL,
    from_node BIGINT,
    bag_id BIGINT NOT NULL,
    fulfillment_id BIGINT,
    cleaned BOOLEAN DEFAULT FALSE,
    replaced BOOLEAN DEFAULT FALSE,
    validated BOOLEAN DEFAULT FALSE,
    type VARCHAR(255),
    strategy_id BIGINT
);

DROP TABLE IF EXISTS repair_file;
DROP SEQUENCE IF EXISTS repair_file_id_seq;
CREATE SEQUENCE repair_file_id_seq;
CREATE TABLE repair_file (
    id bigint PRIMARY KEY DEFAULT nextval('repair_file_id_seq'),
    path text,
    repair_id bigint
);

DROP TABLE IF EXISTS strategy;
DROP SEQUENCE IF EXISTS strategy_id_seq;
CREATE SEQUENCE strategy_id_seq;
CREATE TABLE strategy (
    id bigint PRIMARY KEY DEFAULT nextval('strategy_id_seq'),
    api_key VARCHAR(255),
    url VARCHAR(255),
    link VARCHAR(255),
    type VARCHAR(255)
);

ALTER TABLE bag_distribution
    ADD CONSTRAINT FK_bd_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE bag_distribution
    ADD CONSTRAINT FK_bd_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE replication
    ADD CONSTRAINT FK_repl_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE replication
    ADD CONSTRAINT FK_repl_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE restoration
    ADD CONSTRAINT FL_rest_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE ace_token
    ADD CONSTRAINT FK_token_bag FOREIGN KEY (bag) REFERENCES bag;

ALTER TABLE repair
    ADD CONSTRAINT FK_repair_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE repair
    ADD CONSTRAINT FK_repair_to FOREIGN KEY (to_node) REFERENCES node;

ALTER TABLE repair
    ADD CONSTRAINT FK_repair_from FOREIGN KEY (from_node) REFERENCES node;

ALTER TABLE repair_file
    ADD CONSTRAINT FK_rf_repair FOREIGN KEY (repair_id) REFERENCES repair;

ALTER TABLE repair
    ADD CONSTRAINT FK_repair_strat FOREIGN KEY (strategy_id) REFERENCES strategy ON DELETE CASCADE;

-- storage
DROP TABLE IF EXISTS storage_region;
DROP SEQUENCE IF EXISTS storage_region_id_seq;
CREATE SEQUENCE storage_region_id_seq;
CREATE TABLE storage_region (
    id BIGINT PRIMARY KEY DEFAULT nextval('storage_region_id_seq'),
    node_id BIGINT NOT NULL,
    data_type VARCHAR(255) NOT NULL,
    storage_type VARCHAR(255) NOT NULL,
    capacity BIGINT,
    note TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- storage
-- note the size/total_files might still live in the bag, but are here for now as their usage emerges
DROP TABLE IF EXISTS staging_storage;
DROP SEQUENCE IF EXISTS staging_storage_id_seq;
CREATE SEQUENCE staging_storage_id_seq;
CREATE TABLE staging_storage (
    id BIGINT PRIMARY KEY DEFAULT nextval('staging_storage_id_seq'),
    region_id BIGINT NOT NULL,
    active BOOLEAN,
    path VARCHAR(255),
    size BIGINT,
    total_files BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);


DROP TABLE IF EXISTS fixity;
DROP SEQUENCE IF EXISTS fixity_id_seq;
CREATE SEQUENCE fixity_id_seq;
CREATE TABLE fixity (
    id BIGINT PRIMARY KEY DEFAULT nextval('fixity_id_seq'),
    storage_id BIGINT NOT NULL,
    algorithm VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    UNIQUE(storage_id, algorithm)
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

ALTER TABLE staging_storage
    ADD CONSTRAINT FK_storage_sr FOREIGN KEY (region_id) REFERENCES storage_region;

ALTER TABLE fixity
    ADD CONSTRAINT FK_fixity_storage FOREIGN KEY (storage_id) REFERENCES staging_storage;

ALTER TABLE replication_config
    ADD CONSTRAINT FK_rc_sr FOREIGN KEY (region_id) REFERENCES storage_region;

ALTER TABLE bag
    ADD CONSTRAINT FK_bag_storage FOREIGN KEY (bag_storage_id) REFERENCES staging_storage;

ALTER TABLE bag
    ADD CONSTRAINT FK_bag_tokens FOREIGN KEY (bag_storage_id) REFERENCES staging_storage;

-- Indices
CREATE INDEX CONCURRENTLY idx_filename ON ace_token (bag, filename);