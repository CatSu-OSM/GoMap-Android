# Android port status

This repository contains an experimental native Kotlin/Compose Android port
inspired by Go Map!!.

## Implemented

- MapLibre map surface with OpenStreetMap raster and Esri aerial imagery
- Runtime location permission, live fused-location updates, accuracy display,
  last-known-location fallback, and compass-oriented bearing
- Current-viewport download through the OpenStreetMap map API
- OSM XML parsing into Kotlin node, way, and relation models
- Rendering and selection of downloaded nodes and ways with separate point,
  line, and area hit testing
- Go Map-inspired aerial editor styling with building footprints, road geometry, address labels, POIs, crosshair, compass, and scale bar
- Automatic neighborhood OSM download after location centering, with aerial/street layer switching
- Long-press creation of a local draft node
- Node and whole-way movement with live geometry updates while dragging
- Geometry highlighting, selection vertices, direction arrows, and object
  callouts
- Common Tags and All Tags editor sheets
- iD Tagging Schema presets, Temaki/Maki icons, and US/global Name Suggestion
  Index entries
- Preset search, recent choices, categories, descriptions, and type assignment
- In-memory undo and redo history for draft, geometry, and tag edits
- Guarded upload control that does not send incomplete edits
- Light and dark Android themes and Go Map-inspired glass controls

## Architecture mapping

- `src/Shared/OSMModels` -> `androidApp/.../osm`
- `src/Shared/EditorLayer` and `src/Shared/Tiles` -> MapLibre sources/layers
- iOS view controllers -> Compose screens and Android activities

## Remaining before feature parity

1. Persistent SQLite OSM cache and durable edit history
2. Complete node/way/relation topology editing, creation, deletion, and
   validation
3. Full iD field definitions, conditional fields, localization, and preset
   validation
4. OAuth 2 sign-in, changeset review, conflict detection, conflict resolution,
   and upload
5. OSM notes, quests, imagery alignment controls, GPX, offline support, and
   translations
6. Instrumented UI coverage, accessibility review, performance profiling, and
   production signing

## Safety limitations

- Changes exist only in memory and are lost when the process is restarted.
- Undo and redo are limited to the current session.
- The upload button is intentionally guarded and does not contact the
  OpenStreetMap editing API.
- Viewport downloads use the public OSM map API and are intended only for
  neighborhood-sized areas.

Uploading remains disabled until authentication, validation, persistence, and
conflict handling are implemented.
