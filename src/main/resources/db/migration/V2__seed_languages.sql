-- V2: Seed master language data

INSERT INTO mst_language (code, name, native_name, is_active, created_at)
VALUES
    ('id', 'Indonesian', 'Bahasa Indonesia', true, NOW()),
    ('en', 'English',    'English',          true, NOW()),
    ('ar', 'Arabic',     'العربية',          true, NOW());
