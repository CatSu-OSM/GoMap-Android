# Changelog

All notable changes to this experimental Android port are documented here.

The project follows semantic versioning with prerelease identifiers while it is
not ready for production OpenStreetMap editing.

## [0.1.0-alpha.2] - 2026-07-25

### Changed

- The location arrow now toggles the app's live GPS/location component instead
  of only recentering the map.
- Turning location off stops provider updates and hides the location puck and
  accuracy radius.
- Turning location on restarts live updates and recenters on the best available
  fix.
- The location arrow is outline-only while off and filled blue while on, with
  matching accessibility descriptions for both states.

## [0.1.0-alpha.1] - 2026-07-25

### Added

- Initial native Android application using Kotlin, Jetpack Compose, and
  MapLibre Native.
- Esri aerial and OpenStreetMap raster imagery styles.
- OpenStreetMap viewport downloads, XML parsing, and rendering for nodes, ways,
  areas, roads, addresses, and points of interest.
- Go Map-inspired map controls, crosshair, compass, scale bar, selection
  callouts, vertex handles, and action bar.
- Live current-location puck with accuracy radius, last-known-location
  fallback, and compass-oriented bearing.
- Node and way selection with geometry-aware hit testing and expanded touch
  margins.
- Live whole-object movement from the four-direction handle, with topology
  updates for connected ways.
- Common Tags and All Tags editor sheets with visible white text cursors and
  non-interactive blank sheet space.
- Generated iD Tagging Schema preset catalog with Temaki and Maki SVG icons and
  Name Suggestion Index entries.
- Preset search, recent choices, categories, descriptions, icons, and type
  assignment.
- In-memory undo and redo history for draft creation, movement, and tag edits.
- Bottom-right undo, redo, and guarded upload controls after an edit.
- Unit tests covering OSM parsing, topology-aware movement, and edit-history
  behavior.

### Known limitations

- Editing state is not persisted across process restarts.
- OpenStreetMap authentication, changeset review, validation, conflict
  handling, and upload are not implemented.
- Relation editing, complete topology tools, deletion, and many advanced Go
  Map!! features remain incomplete.
- This APK is debug-signed and intended only for prerelease testing.
