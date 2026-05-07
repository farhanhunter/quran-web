ALTER TABLE users ADD COLUMN last_read_surah_number INT       DEFAULT NULL;
ALTER TABLE users ADD COLUMN last_read_ayah_number  INT       DEFAULT NULL;
ALTER TABLE users ADD COLUMN last_read_updated_at   TIMESTAMP DEFAULT NULL;
