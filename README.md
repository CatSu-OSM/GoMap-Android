# GoMap Android

An experimental native Android port inspired by
[Go Map!!](https://github.com/bryceco/GoMap), the OpenStreetMap editor for
iPhone and iPad.

> [!WARNING]
> This project is prerelease software. Editing is currently local and
> in-memory, and OpenStreetMap upload is deliberately disabled. Do not rely on
> it for production mapping work or expect edits to survive an app restart.

This is an independent Android port and is not an official Go Map!! release.
The project preserves the upstream ISC license and attribution.

## Current features

- Native Kotlin and Jetpack Compose interface backed by MapLibre Native
- Esri aerial imagery and OpenStreetMap raster styles
- Toggleable live location puck, accuracy radius, and compass-oriented bearing
- OpenStreetMap viewport downloads with styled roads, buildings, addresses,
  and points of interest
- Accurate node and way selection with highlighted geometry and generous touch
  targets
- Live whole-object dragging from the four-direction handle, including updates
  to connected way geometry
- Go Map-inspired selection callouts and action controls
- Go Map-inspired Settings sheet opened from the bottom-left gear
- Go Map-inspired Display sheet with four functional map backgrounds,
  left/right plus-button placement, and a map-rotation toggle
- Common Tags and All Tags editor sheets
- Offline preset catalog generated from iD Tagging Schema, Temaki, Maki, and
  Name Suggestion Index data
- Preset search, categories, descriptions, recent choices, and SVG icons
- In-memory undo and redo history for draft creation, geometry movement, and
  tag changes
- Prerelease upload control with a safety guard until authentication and
  conflict handling are implemented

## Install

Download the APK from the
[latest GitHub prerelease](https://github.com/CatSu-OSM/GoMap-Android/releases).
Android may ask you to allow installation from the app used to open the APK.

## Build

Requirements:

- JDK 17 or newer
- Android SDK with API 36

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is written to
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Project status

See [ANDROID_PORT.md](ANDROID_PORT.md) for the implemented architecture,
current limitations, and roadmap. Release history is maintained in
[CHANGELOG.md](CHANGELOG.md).

## Preset data

The bundled preset database and icons are generated offline from upstream
OpenStreetMap editor projects. See [tools/PRESETS.md](tools/PRESETS.md) for the
sources, licenses, and manual update command.

## License

This project is distributed under the [ISC License](LICENSE.md). Bundled preset
data and icon licenses are included alongside those assets under
`androidApp/src/main/assets/presets/licenses`.
