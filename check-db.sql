-- Check DB schema and counts
SHOW DATABASES;
SELECT DATABASE();
SHOW TABLES;
SELECT COUNT(*) AS users_count FROM users;
SELECT COUNT(*) AS orders_count FROM orders;
SELECT COUNT(*) AS products_count FROM products;
