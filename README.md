# Extreme Weather — PM Addon (experimental)

**Minecraft 1.21.1 / NeoForge 21.1 / Java 21.** This is an addon for **ProtoManly's Weather 0.16.4-alpha** and **PMWeatherAPI 0.16.4.1 by nullved**. The old standalone Forge 1.20.1 JAR is **not compatible** with this version. Install all three mods on every client and the server. PMWeather owns cloud formation, storm/tornado rendering, wind and weather sync: this addon must not duplicate its weather renderer. See [PMWeather](https://modrinth.com/mod/protomanlys-weather) and [PMWeatherAPI by nullved](https://modrinth.com/mod/pmweatherapi).

Full 17-point development plan: [docs/ROADMAP-ru.md](docs/ROADMAP-ru.md).

## Added by this addon

* Server-authoritative wildfire spread from lightning with bounded fire updates, destruction of vegetation and replacement of burned grass/dirt with scorched earth.
* Dense wildfire smoke plumes, PMWeather-native fire whirls in large fire clusters, particle embers and ash; when at least eight nearby fires are active, entities beneath the smoke gradually receive Poison. Rain extinguishes fires. Fire effects are shared by all multiplayer clients.
* Weather-derived local effects: hail damage, squall gusts, dust devils, weak waterspouts and landspouts, snow whirls, storm-driven swimmer movement, brief rainbow/sprite particles and rare stronger lightning, branching red sprites, coloured double rainbows and moving ball-lightning orbs. Stormy oceans have crest particles, server-side wind-driven bobbing of floating entities and boats, and bounded temporary water crests saved across restarts and later cleaned up. These are experimental additions, not PMWeather funnel models or a simulated fluid surface.
* Warm oceans can develop PMWeather-native cyclones with short-lived supplemental tropical spiral bands around existing storms, with a calm eye, decaying landfall gusts, and temporary storm-surge water. Hurricane / typhoon / cyclone are regional labels for the same simplified system, not a complete meteorology model.
* A radar and storm siren that use PMWeatherAPI `NearbyStorms`; an anemometer reading PMWeather wind, a simple barometer indicator, plus scorched earth, charred grass and ash blocks. Interact with the instrument blocks to read them. The siren polls nearby PMWeather storms every second, rings on activation and emits redstone power 15 while active. Crafting recipes for all four instruments are included.

**Not yet implemented:** realistic tsunami fluid dynamics (a rare cyclone pressure-wave pulse can briefly push entities and raise bounded water crests), physically simulated tropical-cyclone pressure/wind fields, EF shape overrides, multi-vortex special effects, fully simulated lightning entities, additional PMWeather-native funnel shapes, physical fluid waves, sound design and additional functional blocks. PMWeather itself supplies its own clouds, storms and tornadoes. This is a development preview, **not** a complete version of the 17-feature specification. Do not claim PMWeatherAPI itself provides the missing effects.

## Build and run

Install Java 21 and Gradle 8.8, then run `gradle build`. The GitHub Actions workflow builds and uploads a preview JAR to Releases after each push. Back up worlds before enabling wildfires. MIT license applies to *this addon only*; PMWeather and PMWeatherAPI retain their own licenses. No code or textures from those projects are redistributed here.

## Operator commands (experimental)

* `/ew storm <pm_type> [risk]` — invoke a **native PMWeather** storm via PMWeatherAPI `StormBuilder`; tab completion lists types exposed by the installed PMWeather version, and risk is 0–100. This does not force a tornado or override PMWeather's native rendering.
* `/ew vortex <shape> <ef>` — create a **native PMWeather supercell** and set its public tornado shape, width and stage parameters, synchronized through PMWeather. PMWeather still decides what funnel appears: the 23 names are approximate targets along its continuous shape control, **not 23 independent native mesh types**. Supported names: `wire`, `rope`, `needle`, `v_shaped`, `cigar`, `cylinder`, `cone`, `landspout`, `concave_sided`, `straight_sided`, `convex_sided`, `segmented`, `truncated_cone`, `drill_shaped`, `bowl_shaped`, `wedge`, `plume_base`, `hourglass`, `braided`, `looping`, `multi_vortex`, `dead_man_walking`, `el_reno`.
* `/ew outline <shape> <ef>` — optional older addon particle silhouette, separated from the native command so it does not pretend to be PMWeather.
* `/ew disaster <name>` — `hail`, `rainbow`, `sprite`, `ball_lightning`, `superbolt`, `squall`, `dust_devil`, `waterspout`, `landspout`, `snow_whirl`, `fire_whirl`, `hurricane`, `typhoon`, `cyclone`, `meteotsunami`, `tsunami`, `surge`, `flood`, `wildfire`, `forest_fire`. Commands requiring player position must be run by an operator player.
* `/ew clear` — remove only command-spawned addon funnels; PMWeather's own storms and fire/water still follow their own lifecycles.

**Shape attribution:** The user supplied a tornado-type reference chart. `/ew vortex` now configures an actual PMWeather supercell through public storm fields and PMWeather server sync. Because PMWeather exposes a continuous tornadoShape float, not an enum for every chart label, exact 23 distinct native silhouettes remain unverified. `/ew outline` is the separate procedural preview; no imagery or textures from PMWeather are copied.
