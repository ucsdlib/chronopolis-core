CREATE SEQUENCE file_id_seq;
CREATE TABLE file (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_id_seq'),
    size BIGINT DEFAULT 0 CHECK (size >= 0),
    bag_id BIGINT NOT NULL,
    filename TEXT NOT NULL,
    dtype varchar(25) NOT NULL,
    created_at TIMESTAMP DEFAULT current_timestamp,
    updated_at TIMESTAMP DEFAULT current_timestamp,
    UNIQUE (bag_id, filename)
);

ALTER TABLE file
  ADD CONSTRAINT fk_file_bag FOREIGN KEY (bag_id) REFERENCES bag;
