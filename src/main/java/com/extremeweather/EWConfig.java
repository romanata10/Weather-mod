package com.extremeweather;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server/common configuration. Everything that changes gameplay lives here so that
 * pack authors and server owners can tune (or disable) each disaster independently.
 */
public final class EWConfig {

    public static final ForgeConfigSpec SPEC;

    // ------------------------------------------------------------------ general
    public static final ForgeConfigSpec.BooleanValue ENABLE_NATURAL_CELLS;
    public static final ForgeConfigSpec.IntValue MAX_CELLS_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue SYNC_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue FORCE_VANILLA_RAIN;
    public static final ForgeConfigSpec.DoubleValue NATURAL_STORM_CHANCE;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG;

    // ------------------------------------------------------------------ storms
    public static final ForgeConfigSpec.BooleanValue ENABLE_TORNADOES;
    public static final ForgeConfigSpec.DoubleValue TORNADO_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MAX_EF;
    public static final ForgeConfigSpec.DoubleValue MIN_EF;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BLOCK_DESTRUCTION;
    public static final ForgeConfigSpec.IntValue BLOCKS_DESTROYED_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue MAX_BLOCK_HARDNESS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBRIS_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WIND_PUSH;

    // ------------------------------------------------------------------ small vortices
    public static final ForgeConfigSpec.BooleanValue ENABLE_DUST_DEVILS;
    public static final ForgeConfigSpec.DoubleValue DUST_DEVIL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LANDSPOUTS;
    public static final ForgeConfigSpec.DoubleValue LANDSPOUT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WATERSPOUTS;
    public static final ForgeConfigSpec.DoubleValue WATERSPOUT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SNOW_WHIRLS;
    public static final ForgeConfigSpec.DoubleValue SNOW_WHIRL_CHANCE;

    // ------------------------------------------------------------------ storms / lightning
    public static final ForgeConfigSpec.BooleanValue ENABLE_LIGHTNING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HYPER_LIGHTNING;
    public static final ForgeConfigSpec.DoubleValue HYPER_LIGHTNING_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BALL_LIGHTNING;
    public static final ForgeConfigSpec.DoubleValue BALL_LIGHTNING_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPRITES;
    public static final ForgeConfigSpec.DoubleValue SPRITE_CHANCE;

    // ------------------------------------------------------------------ precipitation
    public static final ForgeConfigSpec.BooleanValue ENABLE_HAIL;
    public static final ForgeConfigSpec.DoubleValue HAIL_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RAINBOWS;

    // ------------------------------------------------------------------ ocean
    public static final ForgeConfigSpec.BooleanValue ENABLE_TROPICAL_CYCLONES;
    public static final ForgeConfigSpec.DoubleValue TROPICAL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FLOODING;
    public static final ForgeConfigSpec.IntValue FLOOD_BLOCK_LIMIT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TSUNAMIS;
    public static final ForgeConfigSpec.DoubleValue TSUNAMI_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MAX_WAVE_AMPLITUDE;
    public static final ForgeConfigSpec.BooleanValue WAVES_AFFECT_ENTITIES;

    // ------------------------------------------------------------------ fire
    public static final ForgeConfigSpec.BooleanValue ENABLE_WILDFIRES;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_CHANCE;
    public static final ForgeConfigSpec.IntValue FIRE_SPREAD_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue WILDFIRE_RADIUS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FIRE_WHIRLS;
    public static final ForgeConfigSpec.DoubleValue FIRE_WHIRL_CHANCE;

    // ------------------------------------------------------------------ protection / gadgets
    public static final ForgeConfigSpec.IntValue STORM_ANCHOR_RADIUS;
    public static final ForgeConfigSpec.IntValue SIREN_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SIREN_VOLUME;
    public static final ForgeConfigSpec.BooleanValue ALLOW_CONTROLLER_FOR_NON_OPS;
    public static final ForgeConfigSpec.BooleanValue PROTECT_CREATIVE_PLAYERS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("General simulation settings").push("general");
        ENABLE_NATURAL_CELLS = b.comment("Let storms spawn naturally from the world climate.")
                .define("enableNaturalStorms", true);
        MAX_CELLS_PER_LEVEL = b.comment("Hard cap of simultaneous storm cells per dimension.")
                .defineInRange("maxCellsPerLevel", 3, 1, 32);
        SYNC_INTERVAL = b.comment("How often (in ticks) weather state is sent to clients. Smaller = smoother, more traffic.")
                .defineInRange("syncInterval", 5, 1, 40);
        FORCE_VANILLA_RAIN = b.comment("Storm cells also drive the vanilla rain / thunder state.")
                .define("forceVanillaRain", true);
        NATURAL_STORM_CHANCE = b.comment("Base chance (per spawn attempt) that a new storm cell forms.")
                .defineInRange("naturalStormChance", 0.20D, 0.0D, 1.0D);
        DEBUG_LOG = b.comment("Log weather simulation events to the server console.").define("debugLog", false);
        b.pop();

        b.comment("Thunderstorm / supercell / tornado settings").push("storms");
        ENABLE_TORNADOES = b.define("enableTornadoes", true);
        TORNADO_CHANCE = b.comment("Chance per supercell cycle that a tornado touches down.")
                .defineInRange("tornadoChance", 0.40D, 0.0D, 1.0D);
        MIN_EF = b.comment("Lowest EF rating that can be produced.")
                .defineInRange("minEf", 0.0D, 0.0D, 5.0D);
        MAX_EF = b.comment("Highest EF rating that can be produced.")
                .defineInRange("maxEf", 5.0D, 0.0D, 5.0D);
        ENABLE_BLOCK_DESTRUCTION = b.comment("Tornadoes rip blocks out of the world.")
                .define("enableBlockDestruction", true);
        BLOCKS_DESTROYED_PER_TICK = b.comment("Block destruction budget per vortex and tick (performance guard).")
                .defineInRange("blocksDestroyedPerTick", 6, 0, 64);
        MAX_BLOCK_HARDNESS = b.comment("Blocks with a destroy speed above this value survive a tornado (obsidian etc).")
                .defineInRange("maxBlockHardness", 8.0D, 0.0D, 100.0D);
        ENABLE_DEBRIS_DAMAGE = b.comment("Flying debris hurts entities inside the funnel.").define("enableDebrisDamage", true);
        ENABLE_WIND_PUSH = b.comment("Storms push players and mobs around.").define("enableWindPush", true);
        b.pop();

        b.comment("Small vortices: dust devils, landspouts, waterspouts, snow whirls").push("smallVortices");
        ENABLE_DUST_DEVILS = b.define("enableDustDevils", true);
        DUST_DEVIL_CHANCE = b.comment("Chance per attempt on a hot, dry, clear day.")
                .defineInRange("dustDevilChance", 0.06D, 0.0D, 1.0D);
        ENABLE_LANDSPOUTS = b.define("enableLandspouts", true);
        LANDSPOUT_CHANCE = b.defineInRange("landspoutChance", 0.12D, 0.0D, 1.0D);
        ENABLE_WATERSPOUTS = b.define("enableWaterspouts", true);
        WATERSPOUT_CHANCE = b.defineInRange("waterspoutChance", 0.18D, 0.0D, 1.0D);
        ENABLE_SNOW_WHIRLS = b.define("enableSnowWhirls", true);
        SNOW_WHIRL_CHANCE = b.defineInRange("snowWhirlChance", 0.10D, 0.0D, 1.0D);
        b.pop();

        b.comment("Lightning family: cloud-to-ground, hyper lightning, ball lightning, red sprites").push("lightning");
        ENABLE_LIGHTNING = b.define("enableLightning", true);
        ENABLE_HYPER_LIGHTNING = b.define("enableHyperLightning", true);
        HYPER_LIGHTNING_CHANCE = b.comment("Chance that a supercell discharge becomes a hyper lightning (superbolt).")
                .defineInRange("hyperLightningChance", 0.05D, 0.0D, 1.0D);
        ENABLE_BALL_LIGHTNING = b.define("enableBallLightning", true);
        BALL_LIGHTNING_CHANCE = b.defineInRange("ballLightningChance", 0.04D, 0.0D, 1.0D);
        ENABLE_SPRITES = b.define("enableRedSprites", true);
        SPRITE_CHANCE = b.comment("Chance for a red sprite / gigantic jet above a supercell.")
                .defineInRange("spriteChance", 0.25D, 0.0D, 1.0D);
        b.pop();

        b.comment("Hail and rainbows").push("precipitation");
        ENABLE_HAIL = b.define("enableHail", true);
        HAIL_DAMAGE = b.comment("Damage per hail impact inside a severe storm.")
                .defineInRange("hailDamage", 1.0D, 0.0D, 20.0D);
        ENABLE_RAINBOWS = b.define("enableRainbows", true);
        b.pop();

        b.comment("Oceans: tropical cyclones, storm surge, floods, tsunamis, waves").push("ocean");
        ENABLE_TROPICAL_CYCLONES = b.define("enableTropicalCyclones", true);
        TROPICAL_CHANCE = b.defineInRange("tropicalCycloneChance", 0.35D, 0.0D, 1.0D);
        ENABLE_FLOODING = b.comment("Storm surge and tsunami water may be placed into the world.").define("enableFlooding", true);
        FLOOD_BLOCK_LIMIT = b.comment("Maximum number of water blocks the simulation may place at once.")
                .defineInRange("floodBlockLimit", 2500, 0, 100000);
        ENABLE_TSUNAMIS = b.define("enableTsunamis", true);
        TSUNAMI_CHANCE = b.defineInRange("tsunamiChance", 0.06D, 0.0D, 1.0D);
        MAX_WAVE_AMPLITUDE = b.comment("Storm wave height in blocks (visual + physical).")
                .defineInRange("maxWaveAmplitude", 2.5D, 0.0D, 12.0D);
        WAVES_AFFECT_ENTITIES = b.comment("Waves bob and push entities and boats on the surface.")
                .define("wavesAffectEntities", true);
        b.pop();

        b.comment("Wildfires, fire whirls and smoke").push("fire");
        ENABLE_WILDFIRES = b.define("enableWildfires", true);
        WILDFIRE_CHANCE = b.comment("Chance per strike during a dry thunderstorm to ignite a wildfire.")
                .defineInRange("wildfireChance", 0.35D, 0.0D, 1.0D);
        FIRE_SPREAD_PER_TICK = b.comment("Fire spread budget per fire and tick.")
                .defineInRange("fireSpreadPerTick", 3, 0, 32);
        WILDFIRE_RADIUS = b.defineInRange("wildfireRadius", 60.0D, 8.0D, 256.0D);
        ENABLE_FIRE_WHIRLS = b.define("enableFireWhirls", true);
        FIRE_WHIRL_CHANCE = b.defineInRange("fireWhirlChance", 0.25D, 0.0D, 1.0D);
        b.pop();

        b.comment("Protection blocks and gadgets").push("gadgets");
        STORM_ANCHOR_RADIUS = b.comment("Radius in which a storm anchor suppresses tornadoes.")
                .defineInRange("stormAnchorRadius", 64, 8, 512);
        SIREN_RADIUS = b.comment("Detection radius of a storm siren.").defineInRange("sirenRadius", 128, 16, 1024);
        SIREN_VOLUME = b.defineInRange("sirenVolume", 3.0D, 0.1D, 10.0D);
        ALLOW_CONTROLLER_FOR_NON_OPS = b.comment("Let non operators use the weather controller block.")
                .define("allowControllerForNonOps", false);
        PROTECT_CREATIVE_PLAYERS = b.comment("Creative players are not damaged by weather.")
                .define("protectCreativePlayers", true);
        b.pop();

        SPEC = b.build();
    }

    private EWConfig() {
    }
}
