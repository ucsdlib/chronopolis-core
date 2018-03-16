ALTER TABLE depositor_contact  ADD CONSTRAINT unique_depositor_contact_email
    UNIQUE (depositor_id, contact_email);

ALTER TABLE depositor_distribution ADD CONSTRAINT unique_depositor_node_distribution
    UNIQUE (depositor_id, node_id);