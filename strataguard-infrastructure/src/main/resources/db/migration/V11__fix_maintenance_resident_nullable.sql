-- V11: Make resident_id nullable in maintenance_requests (admins can create requests without being a resident)
ALTER TABLE maintenance_requests ALTER COLUMN resident_id DROP NOT NULL;
