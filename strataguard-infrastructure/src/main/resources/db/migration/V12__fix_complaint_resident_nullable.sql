-- V12: Make resident_id nullable in complaints (admins can create complaints without being a resident)
ALTER TABLE complaints ALTER COLUMN resident_id DROP NOT NULL;
