-- Seed data for JMeter concurrency study
-- Run against database: booking_concurrency_study
-- Assumes empty or compatible tables (Spring ddl-auto=update)

-- CLIENT user (password = "password" bcrypt)
-- If register API works, prefer POST /api/auth/register instead of this insert.
-- BCrypt for "password": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy is a common demo hash;
-- Prefer registering via API so Spring's PasswordEncoder is used.

-- After registering via API, insert working hours for your provider.
-- 2026-08-03 is a Monday => day_of_week = 1

-- Example (replace :provider_id with the real providers.id / users.id):
INSERT INTO working_hours (provider_id, day_of_week, start_time, end_time)
VALUES (1, 1, '09:00:00', '17:00:00')
ON CONFLICT DO NOTHING;

-- If your table has no unique constraint, use a plain insert once:
-- INSERT INTO working_hours (provider_id, day_of_week, start_time, end_time)
-- VALUES (1, 1, '09:00:00', '17:00:00');
