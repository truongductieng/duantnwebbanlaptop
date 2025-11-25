-- Update password for existing tadmin with new BCrypt hash
UPDATE users
SET password = '$2a$10$VlPNWe8wqWukf7M8lHQSsusbzN3AdyaZ2CfVjesrMsXr.EfNDsuVG', role='ROLE_ADMIN', enabled=1, locked=0
WHERE LOWER(username) = 'tadmin';

-- Verify the update
SELECT id, username, role, enabled, locked, password FROM users WHERE LOWER(username) = 'tadmin';
