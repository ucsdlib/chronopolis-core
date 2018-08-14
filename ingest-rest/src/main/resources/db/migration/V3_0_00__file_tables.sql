CREATE SEQUENCE file_id_seq;
CREATE TABLE file (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_id_seq'),
    size BIGINT DEFAULT 0 CHECK (size >= 0),
    bag_id BIGINT NOT NULL,
    filename TEXT NOT NULL,
    dtype varchar(25) NOT NULL,
    created_at TIMESTAMP DEFAULT current_timestamp,
    UNIQUE (bag_id, filename)
);

CREATE TABLE file_fixity (
    file_id BIGINT NOT NULL,
    fixity_id BIGINT NOT NULL,
    UNIQUE (file_id, fixity_id)
);

ALTER TABLE file
  ADD CONSTRAINT fk_file_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE file_fixity
  ADD CONSTRAINT fk_ff_join_file FOREIGN KEY (file_id) REFERENCES file;

ALTER TABLE file_fixity
  ADD CONSTRAINT fk_ff_join_fixity FOREIGN KEY (fixity_id) REFERENCES fixity;
