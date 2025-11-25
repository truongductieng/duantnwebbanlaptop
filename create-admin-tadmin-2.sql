-- Update password for existing Tadmin
UPDATE users
SET password = '$2a$10$slYQmyNdGzin7olVN3/p2OPST9/PgBkqquzi.Ss7KIUgO2t0jKMUm', role='ROLE_ADMIN', enabled=1, locked=0
WHERE username = 'Tadmin' OR username = 'tadmin';

-- Insert if not exists (using INSERT IGNORE)
INSERT IGNORE INTO users (username, email, password, phone, role, full_name, enabled, locked, email_verified, failed_login_attempts, created_at, updated_at)
VALUES ('Tadmin','tadmin@lapshop.com','$2a$10$slYQmyNdGzin7olVN3/p2OPST9/PgBkqquzi.Ss7KIUgO2t0jKMUm','0123456789','ROLE_ADMIN','Tadmin User',1,0,1,0,NOW(),NOW());

-- Verify
SELECT id, username, email, role, enabled FROM users WHERE username = 'Tadmin';
