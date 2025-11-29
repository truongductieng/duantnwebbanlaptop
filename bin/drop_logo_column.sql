-- Script: Xóa cột logo_url khỏi bảng brand
-- Chạy script này để đồng bộ database với model mới

USE laptopdb;

-- Xóa cột logo_url nếu tồn tại
SET @col_exists = (SELECT COUNT(*) 
                   FROM information_schema.COLUMNS 
                   WHERE TABLE_SCHEMA = 'laptopdb' 
                     AND TABLE_NAME = 'brand' 
                     AND COLUMN_NAME = 'logo_url');

SET @sql = IF(@col_exists > 0, 
              'ALTER TABLE brand DROP COLUMN logo_url', 
              'SELECT "Column logo_url does not exist"');
              
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Kiểm tra cấu trúc bảng sau khi xóa
DESCRIBE brand;
