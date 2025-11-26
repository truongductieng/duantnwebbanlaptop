-- Update password for existing tadmin (case-insensitive) with new BCrypt hash for '123'
UPDATE users
SET password = '$2a$10$W110KxtTqi9nTOWyKOYG7OmqobtTi3.ZEl83EcYUjjTRSOLq3rtUy', role='ROLE_ADMIN', enabled=1, locked=0
WHERE LOWER(username) = 'tadmin';

-- If account does not exist, insert a new admin user
INSERT INTO users (username, email, password, phone, role, full_name, enabled, locked, email_verified, failed_login_attempts, created_at, updated_at)
SELECT 'tadmin','tadmin@lapshop.com','$2a$10$W110KxtTqi9nTOWyKOYG7OmqobtTi3.ZEl83EcYUjjTRSOLq3rtUy','0123456789','ROLE_ADMIN','Tadmin User',1,0,1,0,NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(username) = 'tadmin');

-- Verify the update or insertion
SELECT id, username, email, role, enabled FROM users WHERE LOWER(username) = 'tadmin';
