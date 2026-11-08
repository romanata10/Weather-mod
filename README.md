# Extreme Weather — PM Addon (experimental)

**Minecraft 1.21.1 / NeoForge 21.1 / Java 21.** This is an addon for **ProtoManly's Weather 0.16.4-alpha** and **PMWeatherAPI 0.16.4.1 by nullved**. The old standalone Forge 1.20.1 JAR is **not compatible** with this version. Install all three mods on every client and the server. PMWeather owns cloud formation, storm/tornado rendering, wind and weather sync: this addon must not duplicate its weather renderer. See [PMWeather](https://modrinth.com/mod/protomanlys-weather) and [PMWeatherAPI by nullved](https://modrinth.com/mod/pmweatherapi).

Full 17-point development plan: [docs/ROADMAP-ru.md](docs/ROADMAP-ru.md).

## Added by this addon

* Server-authoritative wildfire spread from lightning with bounded fire updates, destruction of vegetation and replacement of burned grass/dirt with scorched earth.
* Dense wildfire smoke plumes, fire-whirl particle columns and ash; when at least eight nearby fires are active, entities beneath the smoke gradually receive Poison. Rain extinguishes fires. Fire effects are shared by all multiplayer clients.
* Weather-derived local effects: hail damage, squall gusts, dust devils, weak waterspouts and landspouts, snow whirls, storm-driven swimmer movement, brief rainbow/sprite particles and rare stronger lightning, branching red sprites, coloured double rainbows and moving ball-lightning orbs. Stormy oceans have crest particles, server-side wind-driven bobbing of floating entities and boats, and bounded temporary water crests saved across restarts and later cleaned up. These are experimental additions, not PMWeather funnel models or a simulated fluid surface.
* Warm oceans can develop short-lived tropical spiral bands around existing PMWeather storms, with a calm eye, decaying landfall gusts, and temporary storm-surge water. Hurricane / typhoon / cyclone are regional labels for the same simplified system, not a complete meteorology model.
* A radar and storm siren that use PMWeatherAPI `NearbyStorms`; an anemometer reading PMWeather wind, a simple barometer indicator, plus scorched earth, charred grass and ash blocks. Interact with the instrument blocks to read them. The siren polls nearby PMWeather storms every second, rings on activation and emits redstone power 15 while active. Crafting recipes for all four instruments are included.

**Not yet implemented:** realistic tsunami fluid dynamics (a rare cyclone pressure-wave pulse can briefly push entities and raise bounded water crests), physically simulated tropical-cyclone pressure/wind fields, EF shape overrides, multi-vortex special effects, fully simulated lightning entities, PMWeather-integrated fire vortices, physical fluid waves, sound design and additional functional blocks. PMWeather itself supplies its own clouds, storms and tornadoes. This is a development preview, **not** a complete version of the 17-feature specification. Do not claim PMWeatherAPI itself provides the missing effects.

## Build and run

Install Java 21 and Gradle 8.8, then run `gradle build`. The GitHub Actions workflow builds and uploads a preview JAR to Releases after each push. Back up worlds before enabling wildfires. MIT license applies to *this addon only*; PMWeather and PMWeatherAPI retain their own licenses. No code or textures from those projects are redistributed here.

## Operator commands (experimental)

* `/ew storm <pm_type> [risk]` — invoke a **native PMWeather** storm via PMWeatherAPI `StormBuilder`; tab completion lists types exposed by the installed PMWeather version, and risk is 0–100. This does not force a tornado or override PMWeather's native rendering.
* `/ew vortex <shape> <ef>` — explicitly summon an **addon** funnel with EF 0–5, server-side wind and a capped, particle-based shape silhouette. **These are not native PMWeather tornado render shapes.** Supported shapes: `wire`, `rope`, `needle`, `v_shaped`, `cigar`, `cylinder`, `cone`, `landspout`, `concave_sided`, `straight_sided`, `convex_sided`, `segmented`, `truncated_cone`, `drill_shaped`, `bowl_shaped`, `wedge`, `plume_base`, `hourglass`, `braided`, `looping`, `multi_vortex`, `dead_man_walking`, `el_reno`.
* `/ew disaster <name>` — `hail`, `rainbow`, `sprite`, `ball_lightning`, `superbolt`, `squall`, `dust_devil`, `waterspout`, `landspout`, `snow_whirl`, `hurricane`, `typhoon`, `cyclone`, `meteotsunami`, `surge`, `wildfire`. Commands requiring player position must be run by an operator player.
* `/ew clear` — remove only command-spawned addon funnels; PMWeather's own storms and fire/water still follow their own lifecycles.

**Shape attribution:** The user supplied a tornado-type reference chart. Shapes in `/ew vortex` are drawn procedurally and do not copy its image or PMWeather textures. The PMWeather API exposes storm creation and vortex lifecycle events but no documented funnel-renderer shape override. Visual parity with PMWeather is an open task, not an implemented feature.
