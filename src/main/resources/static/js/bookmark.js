const CHECKPOINT_KEY = 'quranCheckpoint';

// ─── Helpers ────────────────────────────────────────────────────────────────

function saveCheckpoint(data) {
    localStorage.setItem(CHECKPOINT_KEY, JSON.stringify(data));

    if (isLoggedIn()) {
        const csrf = getCsrfToken();
        if (!csrf.token) return;
        fetch('/api/reading-progress', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', [csrf.header]: csrf.token },
            body: JSON.stringify({ surahNumber: data.surahNumber, ayahNumber: data.ayahNumber })
        }).catch(err => console.debug('Reading progress sync failed:', err));
    }
}

function loadCheckpoint() {
    try { return JSON.parse(localStorage.getItem(CHECKPOINT_KEY)); } catch { return null; }
}

function clearCheckpoint() {
    localStorage.removeItem(CHECKPOINT_KEY);
}

function getCsrfToken() {
    return {
        token:  document.querySelector('meta[name="_csrf"]')?.content,
        header: document.querySelector('meta[name="_csrf_header"]')?.content
    };
}

function isLoggedIn() {
    return document.querySelector('main[data-logged-in]')?.dataset?.loggedIn === 'true';
}

// ─── DB bookmark helpers (logged-in only) ───────────────────────────────────

async function fetchBookmarkedAyahsFromDb(surahNumber) {
    const csrf = getCsrfToken();
    if (!csrf.token) return [];
    try {
        const res = await fetch(`/api/bookmarks/${surahNumber}`, {
            headers: { [csrf.header]: csrf.token }
        });
        if (!res.ok) return [];
        const json = await res.json();
        return json.data ?? [];
    } catch {
        return [];
    }
}

async function clearAllBookmarksInDb() {
    const csrf = getCsrfToken();
    if (!csrf.token) return false;
    try {
        const res = await fetch('/api/bookmarks', {
            method: 'DELETE',
            headers: { [csrf.header]: csrf.token }
        });
        return res.ok;
    } catch {
        return false;
    }
}

async function toggleBookmarkInDb(surahNumber, ayahNumber) {
    const csrf = getCsrfToken();
    if (!csrf.token) return null;
    try {
        const res = await fetch('/api/bookmark/toggle', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', [csrf.header]: csrf.token },
            body: JSON.stringify({ surahNumber, ayahNumber })
        });
        if (!res.ok) return null;
        const json = await res.json();
        const data = json.data;
        return (data && 'bookmarked' in data) ? data['bookmarked'] : null;
    } catch {
        return null;
    }
}

// ─── Button state ────────────────────────────────────────────────────────────

function activateButton(btn) {
    btn.classList.add('bookmark-btn--active');
    btn.setAttribute('aria-pressed', 'true');
    const container = document.getElementById('ayah-' + btn.dataset.ayahNumber);
    if (container) container.classList.add('ayah-container--bookmarked');
}

function deactivateButton(btn) {
    btn.classList.remove('bookmark-btn--active');
    btn.setAttribute('aria-pressed', 'false');
    const container = document.getElementById('ayah-' + btn.dataset.ayahNumber);
    if (container) container.classList.remove('ayah-container--bookmarked');
}

// ─── initBookmarkButtons ─────────────────────────────────────────────────────

function initBookmarkButtons() {
    const buttons = document.querySelectorAll('.bookmark-btn');
    if (!buttons.length) return;

    if (isLoggedIn()) {
        // Source of truth: DB. Fetch bookmarked ayahs for this surah.
        const surahNumber = parseInt(buttons[0].dataset.surahNumber, 10);
        fetchBookmarkedAyahsFromDb(surahNumber).then(dbAyahNumbers => {
            buttons.forEach(btn => {
                if (dbAyahNumbers.includes(parseInt(btn.dataset.ayahNumber, 10))) {
                    activateButton(btn);
                }
            });
        });

        // Click: toggle DB (multiple bookmarks allowed)
        buttons.forEach(btn => {
            btn.addEventListener('click', async () => {
                const surah  = parseInt(btn.dataset.surahNumber, 10);
                const ayah   = parseInt(btn.dataset.ayahNumber, 10);
                const result = await toggleBookmarkInDb(surah, ayah);
                if (result === true)  { activateButton(btn);   updateClearAllVisibility(); }
                if (result === false) { deactivateButton(btn); updateClearAllVisibility(); }
            });
        });

        initClearAllButton();

    } else {
        // Guest: localStorage (single active bookmark, existing behavior)
        const cp = loadCheckpoint();
        buttons.forEach(btn => {
            const surahNumber = parseInt(btn.dataset.surahNumber, 10);
            const ayahNumber  = parseInt(btn.dataset.ayahNumber, 10);

            if (cp && cp.surahNumber === surahNumber && cp.ayahNumber === ayahNumber) {
                activateButton(btn);
                if (!window.location.hash) {
                    document.getElementById('ayah-' + ayahNumber)
                        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }

            btn.addEventListener('click', () => {
                if (btn.classList.contains('bookmark-btn--active')) {
                    clearCheckpoint();
                    deactivateButton(btn);
                } else {
                    document.querySelectorAll('.bookmark-btn--active').forEach(deactivateButton);
                    saveCheckpoint({
                        surahNumber: parseInt(btn.dataset.surahNumber, 10),
                        surahName:      btn.dataset.surahName,
                        surahNameLatin: btn.dataset.surahNameLatin,
                        ayahNumber:  parseInt(btn.dataset.ayahNumber, 10)
                    });
                    activateButton(btn);
                }
            });
        });
    }
}

// ─── Clear-all button (logged-in, surah detail only) ─────────────────────────

function updateClearAllVisibility() {
    const btn = document.getElementById('clear-all-bookmarks-btn');
    if (!btn) return;
    const hasActive = document.querySelector('.bookmark-btn--active') !== null;
    btn.hidden = !hasActive;
}

function initClearAllButton() {
    const btn = document.getElementById('clear-all-bookmarks-btn');
    if (!btn) return;

    updateClearAllVisibility();

    btn.addEventListener('click', async () => {
        const ok = await clearAllBookmarksInDb();
        if (ok) {
            document.querySelectorAll('.bookmark-btn--active').forEach(deactivateButton);
            updateClearAllVisibility();
        }
    });
}

// ─── initAutoTracking ────────────────────────────────────────────────────────

function initAutoTracking() {
    const containers = document.querySelectorAll('.ayah-container');
    if (!containers.length) return;

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

// ─── initContinueReading ─────────────────────────────────────────────────────

function initContinueReading() {
    const banner = document.getElementById('continue-reading-banner');
    if (!banner) return;

    if (isLoggedIn()) return;   // server-side banner handles it

    const cp = loadCheckpoint();
    if (!cp) { banner.hidden = true; return; }

    banner.querySelector('[data-cp-name]').textContent  = cp.surahNameLatin || cp.surahName;
    banner.querySelector('[data-cp-ayah]').textContent  = 'Ayah ' + cp.ayahNumber;
    banner.querySelector('[data-cp-link]').href         =
        '/quran/surah/' + cp.surahNumber + '#ayah-' + cp.ayahNumber;
    banner.querySelector('[data-cp-clear]')
        ?.addEventListener('click', () => { clearCheckpoint(); banner.hidden = true; }, { once: true });

    banner.hidden = false;
}

// ─── Logout link (no <form> needed) ──────────────────────────────────────────

function initLogoutLink() {
    document.querySelectorAll('[data-logout]').forEach(el => {
        el.addEventListener('click', e => {
            e.preventDefault();
            const csrf = getCsrfToken();
            fetch('/auth/logout', {
                method: 'POST',
                headers: { [csrf.header]: csrf.token }
            }).then(() => window.location.href = '/');
        });
    });
}

// ─── Boot ────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    initBookmarkButtons();
    initContinueReading();
    initAutoTracking();
    initLogoutLink();
});
