CREATE SEQUENCE depositor_id_seq;
CREATE TABLE depositor (
    id BIGINT PRIMARY KEY DEFAULT nextval('depositor_id_seq'),
    namespace VARCHAR(255) NOT NULL UNIQUE,
    source_organization TEXT,
    organization_address TEXT,
    created_at TIMESTAMP DEFAULT current_timestamp,
    updated_at TIMESTAMP DEFAULT current_timestamp
);

CREATE SEQUENCE depositor_contact_id_seq;
CREATE TABLE depositor_contact (
    id BIGINT PRIMARY KEY DEFAULT nextval('depositor_contact_id_seq'),
    depositor_id BIGINT NOT NULL,
    contact_name TEXT,
    contact_phone VARCHAR(42), -- the max size could be 21, but some extra space just in case
    contact_email VARCHAR(255)
);

CREATE SEQUENCE depositor_distribution_id_seq;
CREATE TABLE depositor_distribution (
    id BIGINT PRIMARY KEY DEFAULT nextval('depositor_distribution_id_seq'),
    depositor_id BIGINT NOT NULL,
    node_id BIGINT NOT NULL
);

ALTER TABLE depositor_distribution
    ADD CONSTRAINT FK_dd_depositor FOREIGN KEY (depositor_id) REFERENCES depositor;

ALTER TABLE depositor_distribution
    ADD CONSTRAINT FK_dd_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE depositor_contact
    ADD CONSTRAINT FK_dc_depositor FOREIGN KEY (depositor_id) REFERENCES depositor;