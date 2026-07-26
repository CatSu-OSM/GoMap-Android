# Changelog

All notable changes to this experimental Android port are documented here.

The project follows semantic versioning with prerelease identifiers while it is
not ready for production OpenStreetMap editing.

## [0.1.0-alpha.3] - 2026-07-25

### Added

- The bottom-left gear now opens a Go Map-inspired Settings sheet with
  credentials, presets language, miscellaneous, and advanced sections.
- The Settings sheet has a dedicated blue Done control, Android back handling,
  individually scoped row actions, and touch isolation from the map behind it.
- The map control now opens a Go Map-inspired Display sheet with background,
  overlay, filtering, and interaction sections.
- GPX Tracks now has a nested management sheet with foreground recording,
  current/previous track details, retention choices, edit/delete mode, and
  Android GPX sharing.
- GPX tracks are persisted locally and rendered as lines on every map
  background: active recordings are red and completed tracks are `#FE63F9`.

### Changed

- The OpenStreetMap account row accurately reports `Not signed in` until OAuth
  authentication is implemented.
- Settings that are not implemented yet now respond with an explicit
  prerelease notice instead of silently pretending to work.
- Editor with Aerial, Editor only, Aerial only, and Basemap only now switch
  between real MapLibre style and editor-layer combinations.
- The plus button can move between the right and left sides, and map rotation
  can be enabled or disabled from Display settings.
- The GPX Tracks switch now controls map visibility independently from its
  chevron, which opens the track-management sheet.
- Clear Cache, Data Overlays, Quests, Notes and Fixmes, Turn Restrictions, and
  Object Filters are present for interface parity but remain intentionally
  inactive until their backing data and storage systems exist.
- Background GPX collection remains disabled; active recording pauses whenever
  the app leaves the foreground.

### Fixed

- Draft nodes can be selected again after tapping elsewhere to dismiss their
  selection, including when they overlap downloaded OpenStreetMap geometry.
- Compass, location, Display, and plus controls now stay inside Android's
  navigation-bar safe area in landscape instead of being obstructed at the
  right edge.

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
