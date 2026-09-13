# SeedSight

SeedSight is a client-side seed map and navigation mod for **Minecraft 26.2 on Fabric**. It retains
the official WorldFinder 0.2.1 map and interface while adding a built-in waypoint system and
persistent visited POI tracking.

Current release: **SeedSight 1.1.0+26.2**

## Features

- Open the original WorldFinder map with **M**.
- Filter multiple structure and POI types from the original side panel.
- Select a structure, POI, spawn marker, or map location and choose **Built-in navigation**.
- Follow a compact direction arrow and distance display above the hotbar.
- Keep the current waypoint after restarting or reconnecting to the same world/server.
- Complete a waypoint automatically after remaining within 12 horizontal blocks briefly.
- Hear Minecraft experience sounds when setting, clearing, or reaching a waypoint.
- See a compact red X on unvisited POIs and a green check on visited POIs.
- Toggle each POI from its marker menu, with a brief badge pulse and Minecraft sound feedback.

## Requirements

- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Fabric API `0.158.0+26.2` or newer
- Java `25` or newer

## Install

1. Download [`seedsight-1.1.0+26.2.jar`](release/seedsight-1.1.0+26.2.jar).
2. Remove any previous WorldFinder or SeedSight JAR from the instance's `mods` folder.
3. Put the SeedSight JAR and Fabric API in the `mods` folder.
4. Start Minecraft and press **M**.

SeedSight is a complete replacement for WorldFinder. Installing both at the same time is blocked
to prevent duplicate classes and interface conflicts.

## Waypoints and visited POIs

Open a map marker's menu and choose **Built-in navigation** to set it as the active waypoint.
Setting another destination replaces the current one. Use `/wf waypoint clear` to cancel it.

Choose **Not visited - check off** for a location you have already explored. SeedSight stores every
visited location independently for the current world or multiplayer server. Unvisited markers show
a red X, while checked markers show a green check. The marker menu changes to **Visited - uncheck**
after checking it off.

Singleplayer seeds are detected automatically. On multiplayer servers, the map requires the known
server seed.

## Build from source

Run from the repository root with Java 25 or newer:

```powershell
.\gradlew.bat -p fabric clean jar installBundle
```

The finished JAR is written to `fabric/build/libs/`, and the install ZIP is written to
`fabric/build/distributions/`. The build uses the official WorldFinder 0.2.1 Fabric release in
`fabric/libs` as the map and UI base.

The loader-neutral WorldFinder API and Core modules remain in `api/` and `common/` for addon
compatibility.

## Credits and license

SeedSight modifications are authored by **BurBaGlurbus**. WorldFinder and its original interface
were created by [Asashiin](https://github.com/Azashiin/WorldFinder).

SeedSight is licensed under `LGPL-3.0-or-later`. See [LICENSE](LICENSE) and
[`fabric/NOTICE.md`](fabric/NOTICE.md).
