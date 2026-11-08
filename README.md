# Extreme Weather — PM Addon (experimental)

**Minecraft 1.21.1 / NeoForge 21.1 / Java 21.** This is an addon for **ProtoManly's Weather 0.16.4-alpha** and **PMWeatherAPI 0.16.4.1 by nullved**. The old standalone Forge 1.20.1 JAR is **not compatible** with this version. Install all three mods on every client and the server. PMWeather owns cloud formation, storm/tornado rendering, wind and weather sync: this addon must not duplicate its weather renderer. See [PMWeather](https://modrinth.com/mod/protomanlys-weather) and [PMWeatherAPI by nullved](https://modrinth.com/mod/pmweatherapi).

## Added by this addon

* Server-authoritative wildfire spread from lightning with bounded fire updates, destruction of vegetation and replacement of burned grass/dirt with scorched earth.
* Dense wildfire smoke plumes, fire-whirl particle columns and ash; when at least eight nearby fires are active, entities beneath the smoke gradually receive Poison. Rain extinguishes fires. Fire effects are shared by all multiplayer clients.
* Weather-derived local effects: hail damage, squall gusts, dust devils, weak waterspouts and landspouts, snow whirls, storm-driven swimmer movement, brief rainbow/sprite particles and rare stronger lightning, branching red sprites, coloured double rainbows and moving ball-lightning orbs. Stormy oceans have crest particles and server-side wind-driven bobbing of floating entities and boats. These are experimental additions, not PMWeather funnel models or a simulated fluid surface.
* A radar and storm siren that use PMWeatherAPI `NearbyStorms`; an anemometer reading PMWeather wind, a simple barometer indicator, plus scorched earth, charred grass and ash blocks. Interact with the instrument blocks to read them.

**Not yet implemented:** dedicated tsunami physics, tropical-cyclone extensions, EF shape overrides, multi-vortex special effects, fully simulated lightning entities, PMWeather-integrated fire vortices, physical fluid waves, sound design and additional functional blocks. PMWeather itself supplies its own clouds, storms and tornadoes. This is a development preview, **not** a complete version of the 17-feature specification. Do not claim PMWeatherAPI itself provides the missing effects.

## Build and run

Install Java 21 and Gradle 8.8, then run `gradle build`. The GitHub Actions workflow builds and uploads a preview JAR to Releases after each push. Back up worlds before enabling wildfires. MIT license applies to *this addon only*; PMWeather and PMWeatherAPI retain their own licenses. No code or textures from those projects are redistributed here.
