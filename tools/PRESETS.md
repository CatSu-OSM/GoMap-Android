# Preset asset updates

The app bundles a generated, offline preset catalog and SVG icon set. Refresh
it manually from the repository root:

```powershell
python tools/update_presets.py
```

The updater downloads the current `main` snapshots of:

- `openstreetmap/id-tagging-schema`
- `rapideditor/temaki`
- `mapbox/maki`
- `osmlab/name-suggestion-index`

It writes `androidApp/src/main/assets/presets/catalog.json`, the Temaki/Maki
icons referenced by that catalog, and copies the upstream license files. NSI
suggestions are filtered to global and United States entries by default; pass
`--country ca`, for example, to build another country catalog.
