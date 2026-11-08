# Extreme Weather — Storms & Disasters

Experimental Minecraft **1.20.1 Forge 47.2.0** weather and natural disaster mod. Java 17 required. This repository contains source code, not a prebuilt jar.

## Build / install

1. Install JDK 17 and Gradle 8.1.1 (or generate a Gradle wrapper using `gradle wrapper --gradle-version 8.1.1`).
2. Run `gradle build` in the project root. ForgeGradle will download Minecraft/Forge dependencies.
3. Copy `build/libs/extremeweather-1.0.0+mc1.20.1.jar` (or download the preview JAR from GitHub Releases) to the `mods` directory of **both** a 1.20.1 Forge 47.x client and server.
4. Use `gradle runClient` or `gradle runServer` in development.

**Note:** GitHub Actions successfully compiled the mod on Java 17, but it has **not been play-tested** in a Minecraft client or server. Expect to test and possibly fix integration issues before using it on a server. Back up your world: tornadoes can destroy blocks and floods place water.

## Implemented simulation

- Server-side, per-dimension persistent storm cells: thunderstorms, multi-cell storms, rotating supercells, tropical storms, hurricanes, typhoons, cyclones and extratropical systems. Spawn attempts near loaded players; biomes and temperature influence types.
- Tornadoes EF0–EF5 with rope, cone, stove pipe, barrel, wedge, multi-vortex, dead-man-walking and rare El Reno silhouettes. Wind pushes entities; funnels damage entities and remove soft blocks up to a configurable per-tick budget. Other vortex types: dust devil, landspout, waterspout, snow whirl, gustnado, fire whirl and hypervortex. Some types are command-only.
- Lightning strikes, rare stronger discharges (hyper lightning), storm spark effects, sprites as high-altitude particle effects, visual hail and smoke/flame particles. Wildfires are started by lightning and spread with vanilla fire rules.
- Storm surge/flood water placement from hurricanes, with cleanup when storms pass. Ocean waves apply a small wind-driven bob/push to swimmers; full wave fluid geometry and physical tsunami propagation are **not yet implemented**.
- Client-side cloud and funnel particle visualisation. This is an **early visual prototype**, not a volumetric cloud renderer. A simple particle rainbow appears after nearby storms. Separate ball-lightning entities, dedicated hail projectiles, fully simulated tsunami waves, siren sounds and advanced water deformation are **not yet implemented**. The config exposes some planned options for those systems.
- Functional weather radar, anemometer, barometer, storm siren (interaction warning), weather controller (operator-only), storm anchor (protects nearby blocks) and reinforced glass. Simple generated textures and crafting recipes included.

## Commands (operator-only)

- `/ew status` — active storm/vortex counts
- `/ew clear` — dissipate storms and clean up placed floodwater
- `/ew storm supercell` or `/ew storm hurricane ~ ~ ~` — spawn a storm at your location or coordinates. Supported names: `thunderstorm`, `multi_cell`, `supercell`, `tropical_storm`, `hurricane`, `typhoon`, `cyclone`, `extratropical`.
- `/ew vortex tornado 5` or `/ew vortex waterspout 0.5 ~ ~ ~` — spawn a vortex with EF rating from 0 to 5. Supported names: `tornado`, `landspout`, `waterspout`, `dust_devil`, `gustnado`, `fire_whirl`, `snow_whirl`, `hypervortex`.

## Configuration

Forge generates `config/extremeweather-common.toml` and `config/extremeweather-client.toml`. Disable destructive features with `enableBlockDestruction=false` and `enableFlooding=false`; use `enableNaturalStorms=false` for command-only mode. Particle detail and client effects can be tuned independently. Storm data persists in the dimension's SavedData.

## License

MIT.
