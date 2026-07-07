-- Base prices per seat category (admin-configurable afterwards via /seat-type-prices)
INSERT INTO seat_type_price (seat_type, base_price) VALUES
    ('REGULAR', 150.00),
    ('PREMIUM', 300.00),
    ('RECLINER', 450.00);

-- Single configurable weekend multiplier (admin-configurable via PATCH /pricing-rules/weekend)
INSERT INTO weekend_pricing_rule (multiplier) VALUES (1.20);

-- Dev-only seeded admin account. Password: "AdminPass123!" (BCrypt hash below).
-- Document this credential in README as a development convenience only —
-- admins are provisioned this way, not via self-registration (see spec §7).
INSERT INTO users (name, email, password_hash, role) VALUES
    ('Seed Admin', 'admin@example.com', '$2y$10$KSF5cIYoHxYPiFpzTgBc7.s7./Tab2.TrtmFpIsktB3J0qKStA6f2', 'ADMIN');
