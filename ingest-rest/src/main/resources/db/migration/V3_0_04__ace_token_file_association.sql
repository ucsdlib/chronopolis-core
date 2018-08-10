-- ace_token file_id
UPDATE ace_token SET file_id = file.id
  FROM file
  WHERE ace_token.bag_id = file.bag_id AND ace_token.filename = file.filename;
