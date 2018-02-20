-- get all current depositors
INSERT INTO depositor(namespace) SELECT DISTINCT depositor FROM bag;

-- create distributions for each depositor
INSERT INTO depositor_distribution (depositor_id, node_id)
    SELECT DISTINCT d.id as depositor_id, bd.node_id FROM bag
        JOIN bag_distribution bd ON bag.id = bd.bag_id
        JOIN depositor d ON bag.depositor = d.namespace;