ALTER TABLE ace_token ADD COLUMN ims_host VARCHAR(255);
UPDATE ace_token SET ims_host = 'ims.umiacs.umd.edu';
ALTER TABLE ace_token ALTER COLUMN ims_host SET NOT NULL;