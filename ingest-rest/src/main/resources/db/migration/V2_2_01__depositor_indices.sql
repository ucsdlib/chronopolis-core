CREATE INDEX idx_dd_dn ON depositor_distribution (depositor_id, node_id);

CREATE INDEX idx_depositor_ns ON depositor (namespace);
CREATE INDEX idx_depositor_contact ON depositor_contact (id, depositor_id);
