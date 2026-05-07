# Quran Web — Development Notes

> Notes & flow agar tidak lupa. Generated from Claude Code sessions.

---

## Feature: Separate & Center Bismillah Display

### Problem

Di DB, ayat pertama surah 2-114 (kecuali 9) menyimpan Bismillah yang merge dengan teks ayat:
```
بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ الٓمٓ
```
Maunya Bismillah tampil di block tersendiri (centered), terpisah dari teks ayat (right-aligned).

### Special Cases

| Surah | Behavior |
|---|---|
| Surah 1 (Al-Fatihah) | Tidak ada block Bismillah terpisah — Bismillah **adalah** ayat 1 |
| Surah 9 (At-Tawbah) | Tidak ada Bismillah sama sekali |
| Surah 2–8, 10–114 | Bismillah centered di atas, di-strip dari teks ayat 1 |

### Files yang Diubah

| File | Perubahan |
|---|---|
| `SurahDetailResponse.java` | Tambah field `private String basmala` |
| `QuranMapper.java` | Constant `BISMILLAH`, logic set basmala + strip dari ayat 1 |
| `surah-detail.html` | Block `th:if="${surah.basmala != null}"` sebelum ayah loop |
| `styles.css` | `.basmala-container` (centered, box-shadow) + `.basmala` (1.8rem, #02978A) |

### Logic di QuranMapper.java

```java
private static final String BISMILLAH = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ";

// Set basmala field
String basmala = (surahNumber == 1 || surahNumber == 9) ? null : BISMILLAH;

// Strip bismillah dari ayat 1 — pakai Normalizer karena combining char order beda
if (basmala != null && ayah.getAyahNumber() == 1) {
String normText = Normalizer.normalize(textArabic, Normalizer.Form.NFC);
String normPrefix = Normalizer.normalize(BISMILLAH + " ", Normalizer.Form.NFC);
    if (normText.startsWith(normPrefix)) {
textArabic = normText.substring(normPrefix.length());
        }
        }
```

**Import:** `java.text.Normalizer`

### Bug Fix Journey: Unicode mismatch saat strip Bismillah

Ini butuh 3 iterasi untuk fix, karena Arabic Unicode combining characters:

**Attempt 1 — `startsWith(BISMILLAH + " ")`:** Gagal. Bytes terlihat sama secara visual tapi gak match.

**Attempt 2 — `indexOf("ٱلرَّحِيمِ")`:** Gagal juga. Kata yang diketik manual punya bytes beda dari yang di DB.

**Attempt 3 — Debug hex bytes:** Akhirnya ketemu root cause:
```
DB text:      ...d984d984 [d991 d98e] d987...  → shadda (U+0651) dulu, baru fatha (U+064E)
Java constant: ...d984d984 [d98e d991] d987...  → fatha (U+064E) dulu, baru shadda (U+0651)
```
Urutan **combining characters** (diacritics/harakat) berbeda antara DB dan Java source file. Secara visual identik, tapi secara bytes beda.

**Final fix — `Normalizer.normalize(text, NFC)`:** Normalize kedua string ke NFC form sebelum compare. NFC menyamakan urutan combining characters → `startsWith` akhirnya return `true`.

**Takeaway:** Untuk Arabic text comparison, **selalu normalize ke NFC dulu**. Jangan pernah compare raw string — combining character order bisa beda meskipun visual identical.

### Verifikasi

1. Surah 78 → Bismillah centered di atas, ayat 1 bersih tanpa Bismillah
2. Surah 9 → Tidak ada block Bismillah
3. Surah 1 → Bismillah muncul sebagai ayat 1:1 di loop (tidak ada block terpisah)

---

## Feature: Bookmark / Checkpoint System

### Overview

User bisa mark posisi bacaan (surah + ayah) dan resume nanti. Karena belum ada auth, checkpoint disimpan di **localStorage** (client-side only, no backend changes).

Dua mode tracking:
1. **Manual bookmark** — klik tombol Bookmark di ayah
2. **Auto-track** — otomatis track ayah yang sedang dibaca via `IntersectionObserver` (50% terlihat di viewport)

### Behavior

- Tiap ayah di surah detail punya tombol **Bookmark** (pill-shaped, SVG icon)
- Klik → simpan ke `localStorage['quranCheckpoint']`, tombol jadi teal, ayah di-highlight
- Klik lagi (toggle off) → hapus checkpoint
- **Auto-track:** Saat scroll di surah detail, ayah yang 50% terlihat otomatis tersimpan sebagai checkpoint (tanpa perlu klik)
- Di halaman **Surah List** dan **Home** → muncul banner **"Continue Reading"** dengan Resume link + Clear button
- Resume → navigate ke surah + scroll ke ayah yang di-bookmark/terakhir dibaca

### localStorage Schema

```json
{
  "surahNumber": 2,
  "surahName": "سُورَةُ البَقَرَةِ",
  "surahNameLatin": "Al-Baqara",
  "ayahNumber": 142
}
```

Key: `quranCheckpoint`

### Files

| File | Perubahan |
|---|---|
| `static/js/bookmark.js` | **Baru** — `initBookmarkButtons()` + `initAutoTracking()` + `initContinueReading()`, dipanggil di `DOMContentLoaded` |
| `styles.css` | Tambah: `.bookmark-btn`, `.bookmark-btn--active`, `.ayah-header` flex, `.ayah-container--bookmarked`, `.continue-reading-banner` + sub-elements |
| `surah-detail.html` | Tambah: `<script>` defer, `id="ayah-N"` di container, `<button class="bookmark-btn">` dengan data attributes |
| `surahs.html` | Tambah: `<script>` defer, `#continue-reading-banner` div sebelum surah list |
| `index.html` | Tambah: `<script>` defer, `#continue-reading-banner` div sebelum navigation hub |

### JS Logic (bookmark.js)

**`initBookmarkButtons()`** — jalan di surah-detail page:
- Query semua `.bookmark-btn`
- Kalau ada checkpoint yang match surah+ayah → set `.bookmark-btn--active` + `.ayah-container--bookmarked` + scroll ke ayah
- On click: toggle save/clear, update classes

**`initAutoTracking()`** — jalan di surah-detail page:
- Pakai `IntersectionObserver` dengan `threshold: 0.5` (50% ayah terlihat di viewport)
- Otomatis simpan posisi bacaan ke `quranCheckpoint` tanpa perlu klik bookmark
- Reuse `saveCheckpoint()` yang sudah ada
- Auto-track dan manual bookmark berdampingan (manual bookmark override auto-track)

**`initContinueReading()`** — jalan di surahs + index pages:
- Cari `#continue-reading-banner`
- Kalau ada checkpoint → isi `[data-cp-name]`, `[data-cp-ayah]`, `[data-cp-link]` href → show banner
- `[data-cp-clear]` click → clear checkpoint, hide banner

**DOMContentLoaded** memanggil ketiganya:
```javascript
document.addEventListener('DOMContentLoaded', () => {
    initBookmarkButtons();
    initContinueReading();
    initAutoTracking();
});
```

### Auto-Track Logic (IntersectionObserver)

```javascript
function initAutoTracking() {
    const containers = document.querySelectorAll('.ayah-container');
    if (!containers.length) return; // only on surah detail page

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (!entry.isIntersecting) return;
            const btn = entry.target.querySelector('.bookmark-btn');
            if (!btn) return;
            saveCheckpoint({
                surahNumber:    parseInt(btn.dataset.surahNumber, 10),
                surahName:      btn.dataset.surahName,
                surahNameLatin: btn.dataset.surahNameLatin,
                ayahNumber:     parseInt(btn.dataset.ayahNumber, 10)
            });
        });
    }, { threshold: 0.5 });

    containers.forEach(c => observer.observe(c));
}
```

**Kenapa IntersectionObserver?**
- Gak perlu ubah template HTML — data attributes sudah ada di `.bookmark-btn`
- Lebih performant dari scroll event listener
- Hanya 1 file berubah (`bookmark.js`), no backend changes

### CSS Classes

| Class | Fungsi |
|---|---|
| `.bookmark-btn` | Pill outline button, gray default |
| `.bookmark-btn:hover` | Teal border + text |
| `.bookmark-btn--active` | Solid teal fill, white text (SVG fill juga berubah) |
| `.ayah-container--bookmarked` | Teal left border + light teal background (#e8f7f5) |
| `.continue-reading-banner` | Teal-bordered card, flex layout, hidden by default |
| `.continue-reading-banner__resume` | Teal pill link |
| `.continue-reading-banner__clear` | Outline pill button |

### Verifikasi

**Manual Bookmark:**
1. Buka surah (e.g. `/quran/surah/2`) → klik Bookmark pada ayah → button teal, card highlight
2. Klik lagi → deactivate, localStorage cleared
3. Bookmark ayah, navigate ke `/quran/surahs` → banner "Continue Reading" muncul
4. Klik "Resume" → navigate ke surah, scroll ke bookmarked ayah
5. Ke `/` (home) → banner juga muncul
6. Klik "Clear" → banner hilang, localStorage cleared
7. Reload surah page dengan bookmark aktif → ayah auto-highlight + scroll

**Auto-Track (IntersectionObserver):**
8. Buka `/quran/surah/1` → scroll pelan-pelan, **jangan klik apa-apa**
9. Buka `/` (home) → banner "Continue Reading" harus muncul dengan ayah terakhir yang terlihat
10. Klik "Resume" → kembali ke posisi ayah tersebut
11. Test dengan surah panjang (e.g. surah 2 Al-Baqarah) → verify update saat scroll

---

## Feature: Auth (Spring Security) + Last Read DB Persistence

### Overview

Sebelumnya checkpoint hanya disimpan di `localStorage` (client-side, hilang kalau ganti browser/device). Sekarang:

- User bisa **register** dan **login**
- Setelah login, auto-track di surah detail otomatis sync posisi bacaan ke **database**
- Halaman home tampilkan "Continue Reading" dari **DB** (bukan localStorage) untuk user yang sudah login
- Guest tetap dapat fitur localStorage seperti sebelumnya

### Arsitektur Auth

**Session-based** (bukan JWT) — tepat untuk Thymeleaf SSR. Spring Security handle session secara native, tidak perlu custom token management.

### Files yang Dibuat / Diubah

**Backend:**

| File | Keterangan |
|---|---|
| `pom.xml` | Tambah `spring-boot-starter-security` + `thymeleaf-extras-springsecurity6` |
| `V22__add_last_read_to_users.sql` | Tambah 3 kolom ke tabel `users`: `last_read_surah_number`, `last_read_ayah_number`, `last_read_updated_at` |
| `model/User.java` | Tambah 3 field sesuai migration |
| `repository/UserRepository.java` | Baru — termasuk `@Modifying updateLastRead()` dan `findByUsernameOrEmail()` |
| `security/CustomUserDetails.java` | Wrapper `UserDetails` — membungkus `User` entity, expose `getUserId()` |
| `security/CustomUserDetailsService.java` | Implements `UserDetailsService` — login via username ATAU email |
| `security/LoginSuccessHandler.java` | Update `last_login` di DB setiap login berhasil |
| `config/SecurityConfig.java` | Form login, logout, route permissions, CSRF, exception handling untuk `/api/**` |
| `service/UserService.java` | Interface: `register()` + `updateLastRead()` |
| `service/impl/UserServiceImpl.java` | Implementasi — bcrypt password, validasi duplikat username/email |
| `dto/request/ReadingProgressRequest.java` | Baru — `surahNumber` (1-114) + `ayahNumber` (min 1) |
| `dto/response/LastReadInfo.java` | Baru — value object untuk data last read di home page |
| `controller/AuthController.java` | `GET/POST /auth/register`, `GET /auth/login` |
| `controller/HomeController.java` | Update — baca last_read dari DB untuk user yang login |
| `controller/ReadingProgressController.java` | `POST /api/reading-progress` — simpan posisi bacaan ke DB |

**Frontend:**

| File | Keterangan |
|---|---|
| `templates/auth/login.html` | Baru — form login (field: `usernameOrEmail`, `password`) |
| `templates/auth/register.html` | Baru — form register dengan field validation |
| `templates/index.html` | Update — CSRF meta tag, auth nav, server-side "Continue Reading" banner |
| `templates/quran/surah-detail.html` | Update — CSRF meta tag + `data-logged-in` agar bookmark.js bisa sync ke backend |
| `templates/quran/surahs.html` | Update — `data-logged-in` agar localStorage banner tidak double saat login |
| `static/js/bookmark.js` | Update — `saveCheckpoint()` sync ke `/api/reading-progress` jika login; `isLoggedIn()` + `getCsrfToken()` helpers; `initContinueReading()` skip jika login |

### Route Map

| Route | Akses | Keterangan |
|---|---|---|
| `/auth/login` | Public | GET: login page; POST: dihandle Spring Security |
| `/auth/register` | Public | GET: form; POST: register akun baru |
| `/auth/logout` | Authenticated | POST only (CSRF protection) |
| `/api/reading-progress` | Authenticated | POST — update last_read di DB |
| `/`, `/quran/**` | Public | Guest + user bisa akses |

### Bookmark.js Flow (setelah login)

```
IntersectionObserver (50% ayah terlihat)
  → saveCheckpoint(data)
      → localStorage.setItem(...)          ← selalu (guest fallback)
      → isLoggedIn() === true?
          → getCsrfToken()                 ← baca meta[name="_csrf"]
          → fetch('/api/reading-progress') ← POST ke backend
              → DB: UPDATE users SET last_read_surah_number=?, last_read_ayah_number=?
```

### "Continue Reading" Banner Logic

```
Home page (/)
├── User login + punya last_read di DB
│   → Server-side banner (Thymeleaf th:if="${dbLastRead != null}")
│   → JS banner di-skip (isLoggedIn() → return early)
│
└── Guest
    → JS banner dari localStorage
    → Server-side banner tidak muncul (dbLastRead = null)
```

### CSRF Handling

Thymeleaf auto-inject CSRF token ke semua `<form th:action="...">`. Untuk `fetch()` dari JS:

```html
<!-- Di head setiap page yang punya bookmark.js -->
<meta name="_csrf" th:content="${_csrf.token}">
<meta name="_csrf_header" th:content="${_csrf.headerName}">
```

```javascript
// bookmark.js membaca dari meta tag
const csrf = getCsrfToken();
fetch('/api/reading-progress', {
    headers: { [csrf.header]: csrf.token }  // X-CSRF-TOKEN: <token>
});
```

### Security Config Highlights

- **Auto-configure DaoAuthenticationProvider** — cukup expose `UserDetailsService` bean + `PasswordEncoder` bean, Spring Security auto-wire keduanya
- **Logout via POST** — mencegah CSRF attack, form logout di template pakai `<form method="post" th:action="@{/auth/logout}">`
- **`/api/**` returns 401 JSON** saat unauthenticated — bukan redirect ke login page

### Fixes: Deprecation Warnings

| Deprecated | Pengganti |
|---|---|
| `new DaoAuthenticationProvider()` + `setUserDetailsService()` | Dihapus — Spring Security auto-configure |
| `new AntPathRequestMatcher(...)` (logout) | `.logoutUrl("/auth/logout")` |
| `new AntPathRequestMatcher("/api/**")` | `PathPatternRequestMatcher.withDefaults().matcher("/api/**")` |
| `frameOptions(lambda)` | `HeadersConfigurer.FrameOptionsConfig::sameOrigin` (method reference) |

### Verifikasi

1. Buka `/auth/register` → daftar akun → redirect ke login
2. Login → home menampilkan username + tombol Logout di nav
3. Buka `/quran/surah/1` → scroll → tidak perlu klik apa-apa
4. Kembali ke `/` → banner "Continue Reading" muncul dari DB (bukan localStorage)
5. Cek DB: `SELECT username, last_read_surah_number, last_read_ayah_number FROM users;`
6. Buka browser incognito → guest mode — banner muncul dari localStorage (jika ada)
7. Login di incognito dengan akun yang sama → banner dari DB muncul

---

## Feature: Manual Bookmark → UserBookmark DB

### Overview

Tombol Bookmark manual di tiap ayah sekarang sync ke tabel `user_bookmark` di DB (untuk user yang login). Guest tetap pakai localStorage seperti sebelumnya.

Perbedaan model:

| | `quranCheckpoint` localStorage | `UserBookmark` DB |
|---|---|---|
| **Tujuan** | Posisi terakhir dibaca ("Continue Reading") | Koleksi bookmark eksplisit |
| **Jumlah** | Satu posisi per device | Banyak per user (multi-ayah) |
| **Trigger** | Auto-track scroll + manual klik | Manual klik tombol Bookmark |
| **Scope** | Guest + Logged-in (speed/offline) | Logged-in only |

### Files yang Dibuat / Diubah

| File | Keterangan |
|---|---|
| `repository/UserBookmarkRepository.java` | Baru — `toggle` via `findByUserIdAndAyahId` + `findBookmarkedAyahNumbersByUserAndSurah` |
| `dto/request/ToggleBookmarkRequest.java` | Baru — `surahNumber` + `ayahNumber` |
| `service/UserBookmarkService.java` | Baru — interface: `toggle()` + `getBookmarkedAyahNumbers()` |
| `service/impl/UserBookmarkServiceImpl.java` | Baru — lookup ayah by surah+ayah, add/remove bookmark |
| `controller/BookmarkController.java` | Baru — `POST /api/bookmark/toggle` + `GET /api/bookmarks/{surahNumber}` |
| `static/js/bookmark.js` | Update — `initBookmarkButtons()` split behavior login vs guest |

### API Endpoints

| Method | Route | Auth | Response |
|---|---|---|---|
| `POST` | `/api/bookmark/toggle` | Required | `{ bookmarked: true/false, ayahNumber: N }` |
| `GET` | `/api/bookmarks/{surahNumber}` | Required | `[1, 5, 12, ...]` — list ayah numbers |

### bookmark.js Flow

**Logged-in user:**
```
Page load
  → GET /api/bookmarks/{surahNumber}
  → Activate semua button yang ada di list (multi-ayah bisa aktif)

Klik Bookmark
  → POST /api/bookmark/toggle { surahNumber, ayahNumber }
  → result === true  → activateButton()
  → result === false → deactivateButton()
  → result === null  → network error, UI tidak berubah
```

**Guest:**
```
Page load
  → Baca quranCheckpoint dari localStorage
  → Activate button yang match (satu saja)

Klik Bookmark (toggle)
  → Active   → clearCheckpoint() + deactivateButton()
  → Inactive → deactivate semua, saveCheckpoint(), activateButton()
```

### Perbedaan UX Login vs Guest

| Aspek | Guest | Logged-in |
|---|---|---|
| Jumlah bookmark aktif | Satu (toggle) | Banyak |
| Sumber data saat load | localStorage | DB via API |
| Klik bookmark | Tulis localStorage | POST ke DB |
| Auto-scroll ke bookmark | Ya (dari checkpoint) | Tidak |

---

## Feature: Halaman /bookmarks + Delete Individual & Clear All

### Overview

Tambah halaman `/bookmarks` khusus untuk user yang sudah login — menampilkan semua bookmark yang tersimpan di DB lintas surah, dengan tombol delete per item dan clear all.

### Files yang Dibuat / Diubah

| File | Keterangan |
|---|---|
| `repository/UserBookmarkRepository.java` | Tambah `findAllByUserIdWithDetails` (JOIN FETCH ayah+surah), `findByIdAndUserId` |
| `service/UserBookmarkService.java` | Tambah `getAll(userId)` + `deleteById(bookmarkId, userId)` |
| `service/impl/UserBookmarkServiceImpl.java` | Implementasi + mapper `toResponse(UserBookmark)` |
| `controller/BookmarkController.java` | Tambah `DELETE /api/bookmark/{id}` + `DELETE /api/bookmarks` |
| `controller/BookmarksPageController.java` | Baru — `GET /bookmarks` → render `bookmarks.html` |
| `templates/bookmarks.html` | Baru — list bookmark, delete per item, clear all, empty state |
| `templates/index.html` | Tambah link "Bookmarks" di nav (hanya saat login) |
| `config/SecurityConfig.java` | Tambah `.requestMatchers("/bookmarks").authenticated()` |

### API Endpoints (lengkap)

| Method | Route | Auth | Keterangan |
|---|---|---|---|
| `POST` | `/api/bookmark/toggle` | Required | Toggle satu bookmark (add/remove) |
| `GET` | `/api/bookmarks/{surahNumber}` | Required | List ayah numbers yang di-bookmark di surah tertentu |
| `DELETE` | `/api/bookmark/{id}` | Required | Hapus satu bookmark by DB ID |
| `DELETE` | `/api/bookmarks` | Required | Hapus semua bookmark user (reset) |

### Halaman /bookmarks

```
/bookmarks
├── Header: "My Bookmarks" + username
├── Nav: Home | Quran | Languages | Health | Logout
├── [Jika kosong] → empty state + link "Start Reading"
└── [Jika ada bookmark]
    ├── Counter: "N bookmark(s)" + tombol "Clear all"
    └── List tiap bookmark:
        ├── Link ke /quran/surah/{N}#ayah-{M}
        ├── Nama surah + nomor ayah
        ├── Teks Arab ayah
        └── Tombol ✕ (delete individual)
```

Semua aksi (delete satu / clear all) dilakukan via `fetch()` ke REST API tanpa reload halaman. Kalau bookmark habis setelah delete, halaman di-reload untuk tampilkan empty state.

### Security

`deleteById()` selalu verifikasi ownership via `findByIdAndUserId` sebelum delete — user tidak bisa hapus bookmark milik user lain meskipun tahu ID-nya.

---

## Fix: Client-side Banner Disembunyikan Saat Login

### Problem

Banner "Continue Reading" dari localStorage (`id="continue-reading-banner"`) tetap ada di DOM meskipun user sudah login — hanya di-hide via JS. Ini tidak efisien dan berisiko flash saat JS belum jalan.

### Fix

Pakai `sec:authorize="isAnonymous()"` agar Thymeleaf tidak render div tersebut sama sekali ke HTML saat user login.

```html
<!-- index.html dan surahs.html -->
<div sec:authorize="isAnonymous()" id="continue-reading-banner" ...>
```

**Efek:**
- Login → div tidak ada di DOM sama sekali → hanya server-side banner yang tampil
- Guest → div dirender, JS mengisi konten dari localStorage

**File yang diubah:** `templates/index.html`, `templates/quran/surahs.html`

---

## Flyway Migration Setup

---

## Kenapa Pindah ke Flyway?

| Sebelum (DataSeeder + data.sql) | Sesudah (Flyway) |
|---|---|
| `count() > 0` guard manual | Flyway auto-track via `flyway_schema_history` |
| Hardcoded Java strings — susah maintain 114 surah | SQL files — mudah di-generate dari API |
| `ddl-auto: create-drop/update` — schema implicit | `ddl-auto: none` — schema eksplisit di SQL |
| `data.sql` partial (cuma Al-Fatihah) | Full 30 juz, grouped per 5 juz |

---

## File yang Berubah

### Modified
- **`pom.xml`** — Tambah `flyway-core` + `flyway-database-postgresql`
- **`application.yaml`** — Semua profile `ddl-auto: none`, H2 URL pakai PostgreSQL mode, tambah `flyway.enabled: true`

### Created
- **`db/migration/V1__create_schema.sql`** — DDL semua tabel
- **`db/migration/V2__seed_languages.sql`** — 3 bahasa (id, en, ar)
- **`db/migration/V3-V21__seed_*.sql`** — Di-generate oleh script
- **`scripts/generate_migrations.py`** — Script generate migration files

### Deleted
- **`DataSeeder.java`**
- **`data.sql`**

---

## Migration File Structure

```
db/migration/
├── V1__create_schema.sql              ← DDL semua tabel
├── V2__seed_languages.sql             ← 3 bahasa
├── V3__seed_surahs.sql                ← 114 surah (generated)
├── V4__seed_ayahs_juz01_05.sql        ← Ayah juz 1-5
├── V5__seed_ayahs_juz06_10.sql
├── V6__seed_ayahs_juz11_15.sql
├── V7__seed_ayahs_juz16_20.sql
├── V8__seed_ayahs_juz21_25.sql
├── V9__seed_ayahs_juz26_30.sql
├── V10__seed_translations_id_juz01_05.sql  ← Terjemahan ID
├── V11__seed_translations_id_juz06_10.sql
├── V12__seed_translations_id_juz11_15.sql
├── V13__seed_translations_id_juz16_20.sql
├── V14__seed_translations_id_juz21_25.sql
├── V15__seed_translations_id_juz26_30.sql
├── V16__seed_translations_en_juz01_05.sql  ← Terjemahan EN
├── V17__seed_translations_en_juz06_10.sql
├── V18__seed_translations_en_juz11_15.sql
├── V19__seed_translations_en_juz16_20.sql
├── V20__seed_translations_en_juz21_25.sql
└── V21__seed_translations_en_juz26_30.sql
```

---

## Flow Eksekusi (Step by Step)

### Phase 1: Setup (sudah done oleh Claude Code)

1. ✅ Tambah Flyway dependency di `pom.xml`
2. ✅ Update `application.yaml` — semua profile `ddl-auto: none`
3. ✅ Buat `V1__create_schema.sql` (DDL)
4. ✅ Buat `V2__seed_languages.sql`
5. ✅ Buat `scripts/generate_migrations.py`
6. ✅ Hapus `DataSeeder.java` + `data.sql`

### Phase 2: Validasi (juz 1-3 dulu) ✅ DONE

```bash
# Generate migration files untuk juz 1-3
python3 scripts/generate_migrations.py --max-juz 3

# Jalankan app (H2 / dev profile)
./mvnw spring-boot:run

# Atau test dengan PostgreSQL
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-postgres
```

**Cek di console** — harus muncul:
```
Migrating schema "public" to version 1 - create schema
Migrating schema "public" to version 2 - seed languages
Migrating schema "public" to version 3 - seed surahs
Migrating schema "public" to version 4 - seed ayahs juz01 05
...
```

**Verify row count:**
```sql
SELECT COUNT(*) FROM mst_surah;       -- harus 114
SELECT COUNT(*) FROM mst_ayah;        -- juz 1-3: ~429
SELECT COUNT(*) FROM mst_translation; -- juz 1-3: ~858 (2 bahasa)
```

### Phase 3: Full Data (setelah validasi OK) ✅ DONE

#### ⚠️ PENTING: Kenapa harus reset DB dulu?

Kalau sudah pernah run `--max-juz 3`, Flyway menyimpan **checksum** tiap migration file di tabel `flyway_schema_history`. Saat `--max-juz 30` dijalankan, script **menimpa** file yang sama (misal `V4__seed_ayahs_juz01_03.sql` isinya berubah jadi `V4__seed_ayahs_juz01_05.sql`). Flyway deteksi checksum beda → **throw error**:

```
FlywayException: Validate failed: Migration checksum mismatch for migration version 4
```

**Solusi:** Reset DB dulu, baru generate ulang. Karena belum prod, ini aman.

#### Langkah yang sudah dijalankan:

```bash
# 1. Hapus volume PostgreSQL (data hilang, fresh start)
docker compose down -v

# 2. Generate ulang semua 30 juz
python3 scripts/generate_migrations.py --max-juz 30

# 3. Start postgres fresh
docker compose up -d postgres

# 4. Jalankan app — Flyway apply V1→V21 dari awal
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-postgres
```

**Note untuk H2 (dev profile):** Gak perlu reset apa-apa, H2 in-memory otomatis clean setiap app restart.

#### Generated files (21 total):

```
V1__create_schema.sql                    ← DDL (manual)
V2__seed_languages.sql                   ← Languages (manual)
V3__seed_surahs.sql                      ← 114 surah
V4__seed_ayahs_juz01_05.sql              ← Ayah juz 1-5
V5__seed_ayahs_juz06_10.sql              ← Ayah juz 6-10
V6__seed_ayahs_juz11_15.sql              ← Ayah juz 11-15
V7__seed_ayahs_juz16_20.sql              ← Ayah juz 16-20
V8__seed_ayahs_juz21_25.sql              ← Ayah juz 21-25
V9__seed_ayahs_juz26_30.sql              ← Ayah juz 26-30
V10-V15__seed_translations_id_*.sql      ← Terjemahan ID per 5 juz
V16-V21__seed_translations_en_*.sql      ← Terjemahan EN per 5 juz
```

**Expected final counts:**
```sql
SELECT COUNT(*) FROM mst_surah;       -- 114
SELECT COUNT(*) FROM mst_ayah;        -- 6236
SELECT COUNT(*) FROM mst_translation; -- 12472 (2 bahasa × 6236 ayat)
```

**Expected Flyway log:**
```
Migrating schema "public" to version 1 - create schema
Migrating schema "public" to version 2 - seed languages
...
Migrating schema "public" to version 21 - seed translations en juz26 30
Successfully applied 21 migrations
```

---

## API Data Source

Script fetch dari **api.alquran.cloud**:

| Endpoint | Data |
|---|---|
| `/v1/quran/quran-uthmani` | Arabic text + metadata (juz, page, hizb, sajda) |
| `/v1/quran/id.indonesian` | Terjemahan Indonesia (Kemenag RI) |
| `/v1/quran/en.sahih` | Terjemahan English (Sahih International) |

---

## H2 Config (Dev Profile)

JDBC URL diubah ke PostgreSQL-compatible mode agar SQL migration compatible:

```
jdbc:h2:mem:qurandb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
```

---

## Troubleshooting

### Flyway "Found more than one migration with version X"

Ini terjadi saat regenerate migration (misal dari `--max-juz 3` ke `--max-juz 30`). Script generate file baru (e.g. `V4__seed_ayahs_juz01_05.sql`) tapi file lama (`V4__seed_ayahs_juz01_03.sql`) masih ada.

```
FlywayException: Found more than one migration with version 4
Offenders:
-> V4__seed_ayahs_juz01_03.sql (SQL)
-> V4__seed_ayahs_juz01_05.sql (SQL)
```

**Fix (2 langkah, keduanya wajib):**

```bash
# 1. Hapus file lama dari src/main/resources/db/migration/
rm src/main/resources/db/migration/V4__seed_ayahs_juz01_03.sql
rm src/main/resources/db/migration/V10__seed_translations_id_juz01_03.sql
rm src/main/resources/db/migration/V16__seed_translations_en_juz01_03.sql

# 2. ⚠️ WAJIB: Clean target/ karena file lama masih ke-cache di situ!
./mvnw clean

# Baru jalankan app
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-postgres
```

**Lesson learned:** Setelah regenerate migration files, SELALU jalankan `./mvnw clean` sebelum run app. Maven copy resources ke `target/classes/` saat build, jadi file lama yang sudah dihapus dari `src/` masih bisa nyangkut di `target/`.

### Flyway checksum mismatch
Kalau edit/regenerate migration file yang sudah pernah jalan:
```
FlywayException: Validate failed: Migration checksum mismatch for migration version X
```

**Fix (dev only):**
```bash
# Option 1: Reset volume PostgreSQL (recommended, paling bersih)
docker compose down -v
docker compose up -d postgres
# lalu restart app — Flyway apply dari V1

# Option 2: Flyway clean (via Maven plugin, kalau sudah setup)
./mvnw flyway:clean flyway:migrate
```

### Mau regenerate semua migration
Karena belum prod, flow-nya:
1. `docker compose down -v` (hapus volume)
2. Hapus semua `V3-V21` files dari `db/migration/`
3. `python3 scripts/generate_migrations.py --max-juz 30`
4. `./mvnw clean` ← **jangan lupa!** bersihkan target/ cache
5. `docker compose up -d postgres`
6. `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-postgres`

### H2 console
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:qurandb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
Username: sa
Password: (kosong)
```

---

## Bug Fixes Log

### 1. `KeyError: 'numberOfAyahs'` di generate_migrations.py

**Problem:** Script pakai `s["numberOfAyahs"]` tapi field itu gak ada di response API `api.alquran.cloud`.

**Actual API surah keys:** `number`, `name`, `englishName`, `englishNameTranslation`, `revelationType`, `ayahs`

**Fix:**
```python
# Sebelum (error)
total = s["numberOfAyahs"]

# Sesudah (fix)
total = len(s["ayahs"])
```

### 2. `revelationType` mapping salah

**Problem:** API return `"Meccan"` / `"Medinan"`, tapi Java enum pakai `MAKKIYAH` / `MADANIYAH`. Sebelumnya cuma di-`.upper()` jadi `"MECCAN"` — gak match enum.

**Fix:**
```python
# Sebelum (salah)
rev_type = s["revelationType"].upper()  # → "MECCAN" ❌

# Sesudah (fix)
rev_raw = s["revelationType"]
rev_type = "MAKKIYAH" if rev_raw == "Meccan" else "MADANIYAH"  # ✅
```

### 3. Generated files setelah fix (--max-juz 3)

| File | Keterangan |
|---|---|
| `V3__seed_surahs.sql` | 114 surah (semua, bukan cuma juz 1-3) |
| `V4__seed_ayahs_juz01_03.sql` | Ayah juz 1-3 |
| `V10__seed_translations_id_juz01_03.sql` | Terjemahan ID juz 1-3 |
| `V16__seed_translations_en_juz01_03.sql` | Terjemahan EN juz 1-3 |

### 4. Duplicate migration version setelah regenerate `--max-juz 30`

**Problem:** Setelah generate ulang dari `--max-juz 3` ke `--max-juz 30`, file lama (e.g. `V4__seed_ayahs_juz01_03.sql`) masih ada di `src/` dan `target/`. Flyway detect 2 file dengan version yang sama → error.

**Fix (2 tahap):**
1. Hapus file lama `*_juz01_03.sql` dari `src/main/resources/db/migration/`
2. Jalankan `./mvnw clean` — karena `target/classes/` masih cache file lama meskipun sudah dihapus dari `src/`

**Takeaway:** Setelah regenerate migration, **selalu `./mvnw clean`** sebelum run app.

### 5. Bismillah gagal di-strip dari ayat 1 (Unicode mismatch)

**Problem:** Bismillah strip gagal — 3 attempt sebelum fix:
1. `startsWith(BISMILLAH + " ")` → gagal, bytes gak match
2. `indexOf("ٱلرَّحِيمِ")` → gagal juga, manually typed string beda bytes
3. Hex debug → ketemu root cause: **combining character order berbeda**

**Root cause:** DB menyimpan shadda (U+0651) sebelum fatha (U+064E), tapi Java source file punya urutan kebalikannya. Visual identik, bytes beda.

```
DB:   d991 d98e  (shadda → fatha)
Java: d98e d991  (fatha → shadda)
```

**Final fix:** `Normalizer.normalize(text, Normalizer.Form.NFC)` sebelum compare:
```java
String normText = Normalizer.normalize(textArabic, Normalizer.Form.NFC);
String normPrefix = Normalizer.normalize(BISMILLAH + " ", Normalizer.Form.NFC);
if (normText.startsWith(normPrefix)) {
textArabic = normText.substring(normPrefix.length());
        }
```

**Takeaway:** Untuk Arabic text comparison, **selalu normalize ke NFC**. Combining character order bisa beda meskipun visual identical. Jangan pernah compare raw Arabic strings.

---

## Docker Troubleshooting

### `error getting credentials` saat `docker compose up`

**Problem:** `docker-credential-desktop` gagal karena Docker Desktop belum login. Error:
```
error getting credentials - err: exit status 1, out: ``
```

**Fix:** Hapus `credsStore` dari `~/.docker/config.json` agar Docker fallback ke file-based credentials (gak perlu login untuk pull public images):

```bash
# Edit ~/.docker/config.json
# Hapus baris: "credsStore": "desktop"
# Atau jalankan:
python3 -c "
import json
with open('$HOME/.docker/config.json') as f:
    cfg = json.load(f)
cfg.pop('credsStore', None)
with open('$HOME/.docker/config.json', 'w') as f:
    json.dump(cfg, f, indent=2)
"
```

Setelah fix, `docker compose up -d postgres` berhasil pull & start.

### compose.yaml warning `version` obsolete
Warning ini aman di-ignore, tapi bisa hapus field `version` dari `compose.yaml` biar bersih.

---

## Catatan Penting

- **V1 dan V2 dibuat manual** (schema + languages) — ini statis, gak perlu generate
- **V3-V21 di-generate script** — jangan edit manual, regenerate aja kalau perlu ubah
- **Flyway TIDAK akan re-run** migration yang sudah sukses — tracked di `flyway_schema_history`
- **`ddl-auto: none`** di semua profile — Hibernate gak bikin/ubah tabel, semua via Flyway
- **Belum prod** — bebas regenerate migration files, tapi setelah deploy prod, migration files jadi immutable (append-only)

---

## Feature: Nav Button Styles (Teal / White / Black Palette)

### Perubahan

Semua tombol di nav sekarang memakai palet teal–putih–hitam tanpa warna lain:

| Class | Style | Hover |
|---|---|---|
| `.btn-login` | Outline abu-abu (#555) | Border + teks teal |
| `.btn-register` | Solid hitam (#1f2937) | Sedikit lebih terang |
| `.btn-logout` | Outline hitam (#1f2937) | Solid hitam, teks putih |

Semua `border-width` menggunakan `2px` (tidak lagi `1.5px` — float px berbeda antar browser).

**File yang diubah:** `static/css/styles.css`

---

## Feature: Logout via JS fetch (tanpa `<form>`)

### Problem

Sebelumnya logout pakai `<form method="post" th:action="@{/auth/logout}">` di dalam nav — ini bekerja tapi verbose dan tidak fleksibel (misalnya di `bookmarks.html`).

### Fix

Hapus tag `<form>`, ganti dengan anchor `data-logout`:

```html
<a href="#" class="btn-logout" data-logout>Logout</a>
```

`bookmark.js` menginisialisasi handler ini via `initLogoutLink()`:

```javascript
function initLogoutLink() {
    document.querySelectorAll('[data-logout]').forEach(el => {
        el.addEventListener('click', e => {
            e.preventDefault();
            fetch('/auth/logout', {
                method: 'POST',
                headers: { [csrf.header]: csrf.token }
            }).then(() => window.location.href = '/');
        });
    });
}
```

CSRF token tetap dikirim via header — tidak perlu form. Ini juga dipakai di `bookmarks.html` via inline `<script>`.

**File yang diubah:** `templates/index.html`, `templates/bookmarks.html`, `static/js/bookmark.js`

---

## Feature: Notes pada Bookmark

### Overview

User yang sudah login bisa menambahkan **catatan (notes)** ke tiap ayah yang di-bookmark. Notes bisa diisi saat pertama kali bookmark dibuat, atau diedit kapan saja dari halaman `/bookmarks`.

### Desain

- Notes **opsional** — tidak ada notes saat bookmark dibuat dari surah-detail adalah valid
- Insert awal via `POST /api/bookmark/toggle` — field `notes` dikirim di body (boleh `null`)
- Edit/update notes via `PUT /api/bookmark/notes` — pakai `ayahId` + `notes` baru
- Halaman `/bookmarks` tampilkan teks notes per item, dengan tombol inline "Edit notes"

### Files yang Dibuat / Diubah

| File | Perubahan |
|---|---|
| `dto/request/ToggleBookmarkRequest.java` | Tambah field `notes` (`@Size(max = 1000)`, opsional) |
| `dto/request/BookmarkRequest.java` | Existing — `ayahId` (Long, `@NotNull`) + `notes` (`@Size(max = 1000)`) |
| `dto/response/BookmarkResponse.java` | Tambah field `ayahId` (Long) |
| `service/UserBookmarkService.java` | Update `toggle()` signature: tambah `notes` param; tambah `updateNotes()` |
| `service/impl/UserBookmarkServiceImpl.java` | `toggle()` set notes saat create; `toResponse()` include `ayahId`; `updateNotes()` baru |
| `controller/BookmarkController.java` | `toggle()` pass `request.getNotes()`; tambah `PUT /api/bookmark/notes` |
| `templates/bookmarks.html` | Tiap item tampilkan notes text + "Edit notes" button + inline editor |

### API Endpoints (lengkap setelah update)

| Method | Route | Auth | Body | Keterangan |
|---|---|---|---|---|
| `POST` | `/api/bookmark/toggle` | Required | `{ surahNumber, ayahNumber, notes? }` | Toggle bookmark, set notes saat add |
| `GET` | `/api/bookmarks/{surahNumber}` | Required | — | List ayah numbers yang di-bookmark |
| `PUT` | `/api/bookmark/notes` | Required | `{ ayahId, notes }` | Insert atau update notes pada bookmark |
| `DELETE` | `/api/bookmark/{id}` | Required | — | Hapus satu bookmark by ID |
| `DELETE` | `/api/bookmarks` | Required | — | Hapus semua bookmark user |

### Alur Notes di Halaman /bookmarks

```
Tiap bookmark item:
├── Teks notes (jika ada) → tampil langsung
├── "No notes" (italic, abu-abu) → jika kosong
├── Tombol "Edit notes" → reveal textarea + Save + Cancel
│
Save clicked:
  → PUT /api/bookmark/notes { ayahId, notes }
  → DOM update in-place (tanpa reload)
  → Teks notes / "No notes" diperbarui
```

### Notes dari Surah-Detail Page

Saat user klik Bookmark di surah-detail, `bookmark.js` kirim:

```javascript
// toggleBookmarkInDb() di bookmark.js
body: JSON.stringify({ surahNumber, ayahNumber })
// notes tidak dikirim → null di backend
```

Notes `null` valid karena field tidak `@NotNull`. User bisa tambahkan notes nanti dari `/bookmarks`.

### Verifikasi

1. Login → buka surah → klik Bookmark pada ayah → bookmark tersimpan tanpa notes
2. Buka `/bookmarks` → ayah tadi muncul dengan label "No notes"
3. Klik "Edit notes" → textarea muncul → ketik catatan → klik "Save"
4. Teks notes muncul langsung di item (tanpa reload)
5. Klik "Edit notes" lagi → ubah teks → Save → update
6. Hapus semua isi textarea → Save → kembali ke "No notes"
7. Cek DB: `SELECT notes FROM user_bookmark WHERE user_id = ?;`