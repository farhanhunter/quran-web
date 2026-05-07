#!/usr/bin/env python3
"""
Generate Flyway SQL migration files for Al-Quran data.

Fetches from api.alquran.cloud and produces:
  V3__seed_surahs.sql
  V4__seed_ayahs_juz01_05.sql  (or partial when --max-juz < 5)
  ...
  V10__seed_translations_id_juz01_05.sql
  ...
  V16__seed_translations_en_juz01_05.sql
  ...

Usage:
  python3 scripts/generate_migrations.py --max-juz 3   # validation
  python3 scripts/generate_migrations.py --max-juz 30  # full
"""

import argparse
import json
import math
import os
import sys
import time
import urllib.request
import urllib.error

# ── Configuration ────────────────────────────────────────────────────────────

BASE_URL = "https://api.alquran.cloud/v1"
OUTPUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "db", "migration",
)

EDITIONS = {
    "arabic":  "quran-uthmani",
    "id":      "id.indonesian",
    "en":      "en.sahih",
}

# Translator names stored in the translation rows
TRANSLATOR_NAMES = {
    "id": "Kementerian Agama RI",
    "en": "Sahih International",
}

GROUP_SIZE = 5  # juz per SQL file


# ── Helpers ───────────────────────────────────────────────────────────────────

def fetch_json(url: str, retries: int = 3) -> dict:
    for attempt in range(1, retries + 1):
        try:
            print(f"  GET {url}", flush=True)
            with urllib.request.urlopen(url, timeout=60) as resp:
                return json.loads(resp.read().decode())
        except urllib.error.URLError as exc:
            print(f"  Attempt {attempt} failed: {exc}", file=sys.stderr)
            if attempt < retries:
                time.sleep(2 ** attempt)
            else:
                raise


def escape_sql(text: str) -> str:
    """Escape single quotes for SQL string literals."""
    if text is None:
        return "NULL"
    return "'" + text.replace("'", "''") + "'"


def juz_range_label(start: int, end: int) -> str:
    return f"juz{start:02d}_{end:02d}"


def file_version_for_group(base_version: int, group_index: int) -> int:
    """Return version number for a file group (0-indexed)."""
    return base_version + group_index


# ── Data fetching ─────────────────────────────────────────────────────────────

def fetch_edition(edition_key: str) -> list:
    """Return list of surah dicts from the given edition."""
    url = f"{BASE_URL}/quran/{EDITIONS[edition_key]}"
    data = fetch_json(url)
    if data.get("code") != 200:
        raise RuntimeError(f"API error for {edition_key}: {data}")
    return data["data"]["surahs"]


# ── SQL generators ────────────────────────────────────────────────────────────

def generate_surahs(surahs_arabic: list, out_dir: str) -> None:
    """V3__seed_surahs.sql — 114 surahs."""
    lines = ["-- V3: Seed all 114 surahs\n",
             "INSERT INTO mst_surah (surah_number, name_arabic, name_latin, name_translation,",
             "                       total_ayahs, revelation_type, revelation_order,",
             "                       description, created_at, updated_at)",
             "VALUES"]
    rows = []
    for s in surahs_arabic:
        num        = s["number"]
        name_ar    = escape_sql(s["name"])
        name_en    = escape_sql(s["englishName"])
        name_trans = escape_sql(s["englishNameTranslation"])
        total      = len(s["ayahs"])
        rev_raw    = s["revelationType"]  # "Meccan" or "Medinan"
        rev_type   = "MAKKIYAH" if rev_raw == "Meccan" else "MADANIYAH"
        rows.append(
            f"({num}, {name_ar}, {name_en}, {name_trans}, "
            f"{total}, '{rev_type}', NULL, NULL, NOW(), NOW())"
        )
    lines.append(",\n".join(rows) + ";")
    _write(out_dir, "V3__seed_surahs.sql", "\n".join(lines))


def generate_ayahs(surahs_arabic: list, max_juz: int, out_dir: str) -> None:
    """V4..V9 — ayah data grouped by 5 juz."""
    # Collect all ayahs up to max_juz
    ayahs = []
    for s in surahs_arabic:
        surah_num = s["number"]
        for a in s["ayahs"]:
            juz = a["juz"]
            if juz > max_juz:
                continue
            ayahs.append({
                "surah_num":   surah_num,
                "ayah_num":    a["numberInSurah"],
                "text_arabic": a["text"],
                "text_simple": None,  # uthmani edition has no simple; filled below
                "juz":         juz,
                "hizb":        a.get("hizbQuarter"),
                "manzil":      a.get("manzil"),
                "page":        a.get("page"),
                "sajda":       "true" if a.get("sajda") else "false",
            })

    if not ayahs:
        print("  No ayahs to generate.", flush=True)
        return

    # group by GROUP_SIZE juz
    max_juz_actual = max(a["juz"] for a in ayahs)
    num_groups = math.ceil(max_juz_actual / GROUP_SIZE)

    for g in range(num_groups):
        juz_start = g * GROUP_SIZE + 1
        juz_end   = min((g + 1) * GROUP_SIZE, max_juz_actual)
        label     = juz_range_label(juz_start, juz_end)
        version   = file_version_for_group(4, g)

        chunk = [a for a in ayahs if juz_start <= a["juz"] <= juz_end]
        if not chunk:
            continue

        lines = [
            f"-- V{version}: Seed ayahs {label}\n",
            "INSERT INTO mst_ayah (surah_id, ayah_number, text_arabic, text_simple,",
            "                      juz_number, hizb_number, manzil_number, page_number,",
            "                      sajda, created_at, updated_at)",
            "VALUES"
        ]
        rows = []
        for a in chunk:
            # surah_id via subquery — works on both H2 and PostgreSQL
            surah_id = f"(SELECT id FROM mst_surah WHERE surah_number = {a['surah_num']})"
            text_ar  = escape_sql(a["text_arabic"])
            text_si  = escape_sql(a["text_simple"])
            hizb     = a["hizb"] if a["hizb"] is not None else "NULL"
            manzil   = a["manzil"] if a["manzil"] is not None else "NULL"
            page     = a["page"] if a["page"] is not None else "NULL"
            rows.append(
                f"({surah_id}, {a['ayah_num']}, {text_ar}, {text_si}, "
                f"{a['juz']}, {hizb}, {manzil}, {page}, {a['sajda']}, NOW(), NOW())"
            )
        lines.append(",\n".join(rows) + ";")
        _write(out_dir, f"V{version}__seed_ayahs_{label}.sql", "\n".join(lines))


def generate_translations(
    surahs_arabic: list,
    surahs_lang: list,
    lang_code: str,
    max_juz: int,
    base_version: int,
    out_dir: str,
) -> None:
    """Generate translation files for one language."""
    translator = TRANSLATOR_NAMES[lang_code]

    # Build lookup: (surah_num, ayah_num) → translation text
    trans_map = {}
    for s in surahs_lang:
        snum = s["number"]
        for a in s["ayahs"]:
            juz = a["juz"]
            if juz > max_juz:
                continue
            trans_map[(snum, a["numberInSurah"])] = a["text"]

    # Build ordered list matching ayahs up to max_juz
    entries = []
    for s in surahs_arabic:
        snum = s["number"]
        for a in s["ayahs"]:
            juz = a["juz"]
            if juz > max_juz:
                continue
            text = trans_map.get((snum, a["numberInSurah"]))
            entries.append({
                "surah_num": snum,
                "ayah_num":  a["numberInSurah"],
                "juz":       juz,
                "text":      text,
            })

    if not entries:
        return

    max_juz_actual = max(e["juz"] for e in entries)
    num_groups = math.ceil(max_juz_actual / GROUP_SIZE)

    for g in range(num_groups):
        juz_start = g * GROUP_SIZE + 1
        juz_end   = min((g + 1) * GROUP_SIZE, max_juz_actual)
        label     = juz_range_label(juz_start, juz_end)
        version   = file_version_for_group(base_version, g)

        chunk = [e for e in entries if juz_start <= e["juz"] <= juz_end]
        if not chunk:
            continue

        lines = [
            f"-- V{version}: Seed {lang_code.upper()} translations {label}\n",
            "INSERT INTO mst_translation (ayah_id, language_id, translator_name, text,",
            "                             created_at, updated_at)",
            "VALUES"
        ]
        rows = []
        lang_id_subq = f"(SELECT id FROM mst_language WHERE code = '{lang_code}')"
        for e in chunk:
            ayah_id_subq = (
                f"(SELECT a.id FROM mst_ayah a "
                f"JOIN mst_surah s ON s.id = a.surah_id "
                f"WHERE s.surah_number = {e['surah_num']} "
                f"AND a.ayah_number = {e['ayah_num']})"
            )
            text = escape_sql(e["text"])
            rows.append(
                f"({ayah_id_subq}, {lang_id_subq}, "
                f"{escape_sql(translator)}, {text}, NOW(), NOW())"
            )
        lines.append(",\n".join(rows) + ";")
        _write(
            out_dir,
            f"V{version}__seed_translations_{lang_code}_{label}.sql",
            "\n".join(lines),
        )


def _write(out_dir: str, filename: str, content: str) -> None:
    path = os.path.join(out_dir, filename)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content + "\n")
    print(f"  Written: {filename}", flush=True)


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Flyway migration SQL files")
    parser.add_argument(
        "--max-juz", type=int, default=3,
        help="Maximum juz number to include (default: 3 for validation)",
    )
    args = parser.parse_args()
    max_juz = args.max_juz

    if max_juz < 1 or max_juz > 30:
        parser.error("--max-juz must be between 1 and 30")

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Output directory: {OUTPUT_DIR}")
    print(f"Generating data for juz 1-{max_juz}\n")

    # ── Fetch data ────────────────────────────────────────────────────────────
    print("Fetching Arabic (uthmani) edition...")
    surahs_arabic = fetch_edition("arabic")

    print("Fetching Indonesian translation...")
    surahs_id = fetch_edition("id")

    print("Fetching English translation...")
    surahs_en = fetch_edition("en")

    print()

    # ── Generate files ────────────────────────────────────────────────────────
    print("Generating V3__seed_surahs.sql ...")
    generate_surahs(surahs_arabic, OUTPUT_DIR)

    print(f"Generating ayah files (juz 1-{max_juz}) ...")
    generate_ayahs(surahs_arabic, max_juz, OUTPUT_DIR)

    # Translation base versions:
    #   ID: V10..V15 (groups 0-5 for juz 1-30)
    #   EN: V16..V21
    print(f"Generating Indonesian translation files ...")
    generate_translations(surahs_arabic, surahs_id, "id", max_juz, 10, OUTPUT_DIR)

    print(f"Generating English translation files ...")
    generate_translations(surahs_arabic, surahs_en, "en", max_juz, 16, OUTPUT_DIR)

    print("\nDone! Migration files written to:")
    print(f"  {OUTPUT_DIR}")
    print()
    print("Next steps:")
    print("  1. Run app with H2: ./mvnw spring-boot:run")
    print("  2. Or with PostgreSQL: ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-postgres")
    if max_juz < 30:
        print(f"\n  After validation, run full data generation:")
        print(f"  python3 scripts/generate_migrations.py --max-juz 30")


if __name__ == "__main__":
    main()
