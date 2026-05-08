(function () {
    let surahs = null;

    async function loadSurahs() {
        if (surahs) return;
        const res = await fetch('/quran/api/suggest');
        surahs = await res.json();
    }

    function normalize(str) {
        return (str || '').toLowerCase().replace(/[-\s]/g, '');
    }

    function getResults(q) {
        const nq = normalize(q);
        if (/^\d+[:\s]\d+$/.test(q)) return [];
        if (/^\d+$/.test(q)) {
            const n = parseInt(q, 10);
            return surahs.filter(s => s.surahNumber === n || String(s.surahNumber).startsWith(q));
        }
        return surahs.filter(s =>
            normalize(s.nameLatin).includes(nq) ||
            normalize(s.nameTranslation).includes(nq) ||
            (s.nameArabic || '').includes(q)
        );
    }

    function renderDropdown(ul, q) {
        ul.innerHTML = '';
        const results = getResults(q).slice(0, 8);
        if (results.length === 0) {
            ul.innerHTML = '<li class="s-no-result">No surah found</li>';
        } else {
            results.forEach(s => {
                const li = document.createElement('li');
                li.innerHTML =
                    `<span class="s-num">${s.surahNumber}</span>` +
                    `<span><span class="s-latin">${s.nameLatin}</span><br>` +
                    `<span class="s-translation">${s.nameTranslation || ''}</span></span>` +
                    `<span class="s-arabic">${s.nameArabic}</span>`;
                li.addEventListener('mousedown', e => {
                    e.preventDefault();
                    window.location.href = '/quran/surah/' + s.surahNumber;
                });
                ul.appendChild(li);
            });
        }
        ul.style.display = 'block';
    }

    function init() {
        document.querySelectorAll('.nav-search-input').forEach(input => {
            const wrapper = input.closest('.nav-search');
            const ul = document.createElement('ul');
            ul.className = 'search-dropdown';
            ul.style.display = 'none';
            wrapper.appendChild(ul);

            input.addEventListener('focus', async () => {
                await loadSurahs();
                if (input.value.trim()) renderDropdown(ul, input.value.trim());
            });

            input.addEventListener('input', async () => {
                const q = input.value.trim();
                if (!q) { ul.style.display = 'none'; return; }
                await loadSurahs();
                renderDropdown(ul, q);
            });

            input.addEventListener('blur', () => {
                setTimeout(() => { ul.style.display = 'none'; }, 150);
            });

            input.addEventListener('keydown', e => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const q = input.value.trim();
                    if (q) window.location.href = '/quran/search?q=' + encodeURIComponent(q);
                }
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
