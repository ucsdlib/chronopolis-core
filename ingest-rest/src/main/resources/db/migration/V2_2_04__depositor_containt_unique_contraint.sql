ALTER TABLE depositor_contact  ADD CONSTRAINT unique_depositor_contact_email
    UNIQUE(depositor_id, contact_email);