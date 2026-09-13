# SeedSight

SeedSight is a client-side Fabric mod for Minecraft 26.2. It keeps the official WorldFinder 0.2.1
map and interface, then adds waypoint navigation and persistent visited checks directly to it.

Press **M**, select a structure or point of interest, and use the original waypoint menu to choose
**Built-in navigation**. A compact arrow and distance appear above the hotbar. The waypoint is
saved per world or server, plays Minecraft experience sounds when set, and clears itself with the
level-up sound after you stay within 12 blocks briefly. Use `/wf waypoint clear` to cancel it.

The same marker menu lets you choose **Check off as visited**. Each POI keeps its own saved state
and gets a small green check on the map; select it again and choose **Uncheck visited** to undo it.
WorldFinder's original multi-select structure filters remain available for choosing which POI
types appear on the map.

## Install

Install Minecraft 26.2 with Fabric Loader 0.19.3 or newer and Fabric API 0.158.0+26.2 or newer.
Remove any separate WorldFinder or older SeedSight JAR, then put the SeedSight JAR from
[`release/`](release/) in the instance's `mods` folder.

## Build

Building requires Java 25 or newer:

```powershell
.\gradlew.bat -p fabric jar
```

The Fabric project uses the official WorldFinder 0.2.1 release in `fabric/libs` as its UI and map
base. The produced JAR is a complete replacement; do not install WorldFinder beside it.

The loader-neutral WorldFinder API and Core modules remain in `api/` and `common/` for addon
compatibility.

## Credits and license

SeedSight modifications are authored by **BurBaGlurbus**. The original WorldFinder project and UI
are by Asashiin. This project is licensed under `LGPL-3.0-or-later`; see [LICENSE](LICENSE) and
[`fabric/NOTICE.md`](fabric/NOTICE.md).
