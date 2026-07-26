#!/usr/bin/env python3
"""Refresh the Android preset catalog and its Temaki/Maki SVG icons.

This is intentionally a manual maintenance command, mirroring Go Map's asset
update workflow. It uses only Python's standard library.
"""

from __future__ import annotations

import argparse
from contextlib import nullcontext
import json
import shutil
import tempfile
import urllib.request
import zipfile
from pathlib import Path


REPOSITORIES = {
    "id-tagging-schema": (
        "https://codeload.github.com/openstreetmap/id-tagging-schema/zip/refs/heads/main",
        "id-tagging-schema-main",
    ),
    "temaki": (
        "https://codeload.github.com/rapideditor/temaki/zip/refs/heads/main",
        "temaki-main",
    ),
    "maki": (
        "https://codeload.github.com/mapbox/maki/zip/refs/heads/main",
        "maki-main",
    ),
    "name-suggestion-index": (
        "https://codeload.github.com/osmlab/name-suggestion-index/zip/refs/heads/main",
        "name-suggestion-index-main",
    ),
}


def download_sources(destination: Path) -> Path:
    for name, (url, directory) in REPOSITORIES.items():
        archive = destination / f"{name}.zip"
        print(f"Downloading {name}...")
        urllib.request.urlretrieve(url, archive)
        with zipfile.ZipFile(archive) as bundle:
            bundle.extractall(destination)
        if not (destination / directory).is_dir():
            raise RuntimeError(f"{name} archive did not contain {directory}")
    return destination


def preset_name(raw_id: str, translated: dict) -> str:
    if name := translated.get("name"):
        return name
    return raw_id.rsplit("/", 1)[-1].replace("_", " ").replace("-", " ").title()


def build_presets(schema_root: Path) -> list[dict]:
    with (schema_root / "dist/presets.min.json").open(encoding="utf-8") as source:
        source_presets = json.load(source)
    with (schema_root / "dist/translations/en.min.json").open(encoding="utf-8") as source:
        translations = json.load(source)["en"]["presets"]["presets"]

    result = []
    for preset_id, preset in source_presets.items():
        if preset_id.startswith("@templates/"):
            continue
        translated = translations.get(preset_id, {})
        tags = preset.get("addTags") or preset.get("tags") or {}
        result.append(
            {
                "id": preset_id,
                "name": preset_name(preset_id, translated),
                "geometry": preset.get("geometry", []),
                "tags": tags,
                "icon": preset.get("icon"),
                "terms": translated.get("terms", []),
            }
        )
    return result


def build_categories(schema_root: Path) -> list[dict]:
    with (schema_root / "dist/preset_categories.min.json").open(
        encoding="utf-8"
    ) as source:
        source_categories = json.load(source)
    with (schema_root / "dist/translations/en.min.json").open(encoding="utf-8") as source:
        translations = json.load(source)["en"]["presets"]["categories"]
    return [
        {
            "id": category_id,
            "name": translations.get(category_id, {}).get(
                "name", category_id.replace("category-", "").replace("_", " ").title()
            ),
            "members": category.get("members", []),
        }
        for category_id, category in source_categories.items()
    ]


def is_available_in(item: dict, country: str) -> bool:
    location = item.get("locationSet") or {}
    includes = location.get("include") or []
    excludes = location.get("exclude") or []
    if country in excludes or "001" in excludes:
        return False
    return not includes or "001" in includes or country in includes


def build_suggestions(nsi_root: Path, country: str) -> list[dict]:
    result = []
    for group in ("brands", "operators", "flags", "transit"):
        group_root = nsi_root / "data" / group
        if not group_root.is_dir():
            continue
        for source_path in group_root.rglob("*.json"):
            with source_path.open(encoding="utf-8") as source:
                source_data = json.load(source)
            for item in source_data.get("items", []):
                if not is_available_in(item, country):
                    continue
                tags = item.get("tags") or {}
                display_name = item.get("displayName")
                if not display_name or not tags:
                    continue
                result.append(
                    {
                        "id": f"nsi/{item['id']}",
                        "name": display_name,
                        "tags": tags,
                        "terms": item.get("matchNames", []),
                    }
                )
    return result


def copy_icons(source_root: Path, output_root: Path) -> None:
    if output_root.exists():
        shutil.rmtree(output_root)
    output_root.mkdir(parents=True)
    icon_sources = (
        ("maki", source_root / "maki-main/icons"),
        ("temaki", source_root / "temaki-main/icons"),
    )
    for prefix, icon_root in icon_sources:
        for source in icon_root.glob("*.svg"):
            shutil.copyfile(source, output_root / f"{prefix}-{source.name}")


def copy_licenses(source_root: Path, output_root: Path) -> None:
    output_root.mkdir(parents=True, exist_ok=True)
    for name, (_, directory) in REPOSITORIES.items():
        candidates = sorted((source_root / directory).glob("LICENSE*"))
        if candidates:
            shutil.copyfile(candidates[0], output_root / f"{name}.txt")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--source-root",
        type=Path,
        help="Use an already-extracted directory containing the four *-main folders.",
    )
    parser.add_argument(
        "--country",
        default="us",
        help="ISO 3166-1 alpha-2 country for bundled NSI suggestions (default: us).",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("androidApp/src/main/assets/presets"),
    )
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    temporary_context = (
        nullcontext(None)
        if args.source_root
        else tempfile.TemporaryDirectory(prefix="gomap-presets-")
    )
    with temporary_context as temporary:
        source_root = args.source_root or download_sources(Path(temporary))
        presets = build_presets(source_root / "id-tagging-schema-main")
        categories = build_categories(source_root / "id-tagging-schema-main")
        suggestions = build_suggestions(
            source_root / "name-suggestion-index-main", args.country.lower()
        )
        catalog = {
            "country": args.country.lower(),
            "sources": {
                name: url for name, (url, _) in REPOSITORIES.items()
            },
            "presets": presets,
            "categories": categories,
            "suggestions": suggestions,
        }
        with (args.output / "catalog.json").open("w", encoding="utf-8") as output:
            json.dump(catalog, output, ensure_ascii=False, separators=(",", ":"))
            output.write("\n")
        copy_icons(source_root, args.output / "icons")
        copy_licenses(source_root, args.output / "licenses")

    print(
        f"Wrote {len(presets)} presets, {len(suggestions)} suggestions, "
        f"and {len(list((args.output / 'icons').glob('*.svg')))} icons."
    )


if __name__ == "__main__":
    main()
