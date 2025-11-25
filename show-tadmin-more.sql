SELECT id, username, LOWER(username) AS username_lower, email, role, enabled, locked, email_verified, failed_login_attempts, password, created_at
FROM users
WHERE LOWER(username) = 'tadmin' OR username LIKE '%Tadmin%';
