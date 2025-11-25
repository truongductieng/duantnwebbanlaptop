-- Show Tadmin record (case-insensitive)
SELECT id, username, LOWER(username) AS username_lower, email, role, enabled, locked, password, created_at
FROM users
WHERE LOWER(username) = 'tadmin' OR username LIKE '%Tadmin%';
