package com.extremeweather.weather;

import com.extremeweather.EWConfig;
import com.extremeweather.util.EWDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * A single vortex: tornado, landspout, waterspout, dust devil, gustnado, fire whirl,
 * snow whirl or hyper vortex.
 *
 * <p>The vortex is <b>not</b> a Minecraft entity. It is simulated by the server weather engine
 * and replicated to clients as a light weight display record, which allows dozens of them
 * without entity tick cost.</p>
 */
public class Vortex {

    private static final RandomSource RANDOM = RandomSource.create();

    public final UUID id = UUID.randomUUID();
    public VortexType type;
    public VortexShape shape;

    public double x;
    public double y;
    public double z;
    public double vx;
    public double vz;

    /** Continuous EF rating (0 - 5). */
    public float ef;
    public float targetEf;
    public float height;
    public float width;
    public float rotation;
    public float spin;
    public float leanX;
    public float leanZ;

    public int age;
    public int life;
    public int subs;

    public boolean dead;
    /** Optional back reference used by the engine to keep tornadoes attached to their parent cell. */
    public UUID parent;

    public Vortex(VortexType type, double x, double y, double z, float ef) {
        this.type = type;
        this.ef = ef;
        this.targetEf = ef;
        this.x = x;
        this.y = y;
        this.z = z;
        this.shape = VortexShape.pick(RANDOM, ef, type);
        this.spin = (0.12F + ef * 0.06F) * (type.isSmall() ? 1.6F : 1.0F);
        this.life = (int) ((type.isSmall() ? 900 : 2400) + ef * 900);
        this.height = type.isSmall() ? 18.0F + ef * 6.0F : 48.0F + ef * 22.0F;
        this.width = nominalWidth(ef, type);
        this.subs = shape == VortexShape.MULTI_VORTEX ? 2 + RANDOM.nextInt(4) : 0;
        if (shape == VortexShape.EL_RENO) {
            this.width *= 2.6F;
            this.height *= 1.15F;
        }
        this.leanX = (RANDOM.nextFloat() - 0.5F) * 0.2F;
        this.leanZ = (RANDOM.nextFloat() - 0.5F) * 0.2F;
    }

    public static float nominalWidth(float ef, VortexType type) {
        float base = 2.0F + ef * 3.4F;
        if (type == VortexType.WATERSPOUT) {
            base *= 0.7F;
        } else if (type == VortexType.LANDSPOUT || type == VortexType.DUST_DEVIL) {
            base *= 0.45F;
        } else if (type == VortexType.SNOW_WHIRL) {
            base *= 0.7F;
        } else if (type == VortexType.HYPERVORTEX) {
            base *= 1.5F;
        }
        return base;
    }

    public boolean isAlive() {
        return !dead;
    }

    /** Normalised lifetime, 0 = just formed, 1 = about to dissipate. */
    public float lifeProgress() {
        return Mth.clamp(age / (float) Math.max(1, life), 0.0F, 1.0F);
    }

    public float getRadiusAt(float t, float partialTick) {
        float w = width * (1.0F + 0.02F * Mth.sin((age + partialTick) * 0.35F));
        return shape.radiusAt(t, w, ef);
    }

    public void tick(ServerLevel level, WeatherEngine engine) {
        age++;
        rotation += spin;

        updateIntensity();
        updateMotion(level, engine);
        updateGeometry();

        if (!level.hasChunkAt(BlockPos.containing(x, y, z))) {
            return;
        }

        applyWindEffects(level);
        if (EWConfig.ENABLE_BLOCK_DESTRUCTION.get() && type.damaging && ef > 0.6F) {
            destroyBlocks(level);
        }
        spawnAmbientParticles(level);

        if (age > life || ef <= 0.1F) {
            dead = true;
        }
    }

    // ------------------------------------------------------------------ simulation

    private void updateIntensity() {
        float progress = lifeProgress();
        if (progress < 0.12F) {
            targetEf = Math.min(targetEf + 0.02F, EWConfig.MAX_EF.get().floatValue());
        } else if (progress > 0.65F) {
            targetEf = Math.max(0.0F, targetEf - 0.035F);
        }
        ef = Mth.lerp(type.isSmall() ? 0.02F : 0.008F, ef, targetEf);
        ef = Math.max(0.0F, ef + (RANDOM.nextFloat() - 0.5F) * 0.004F);

        // A tornado can change silhouette during its life: rope -> wedge -> rope.
        if (age % 240 == 0 && RANDOM.nextFloat() < 0.25F && !type.isSmall()) {
            shape = VortexShape.pick(RANDOM, ef, type);
            subs = shape == VortexShape.MULTI_VORTEX ? 2 + RANDOM.nextInt(4) : 0;
        }
    }

    private void updateMotion(ServerLevel level, WeatherEngine engine) {
        Vec3 wind = engine.windAt(x, z);
        vx = vx * 0.94D + wind.x * 0.06D;
        vz = vz * 0.94D + wind.z * 0.06D;

        x += vx + (RANDOM.nextDouble() - 0.5D) * 0.05D;
        z += vz + (RANDOM.nextDouble() - 0.5D) * 0.05D;

        BlockPos groundPos = BlockPos.containing(x, y, z);
        if (level.hasChunkAt(groundPos)) {
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x), Mth.floor(z));
            double targetY = top;
            y = Mth.lerp(0.15D, y, targetY);
        }

        if (shape == VortexShape.DEAD_MAN_WALKING) {
            leanX = (float) Math.sin(age * 0.013D) * 0.55F;
            leanZ = (float) Math.cos(age * 0.010D) * 0.45F;
        } else if (shape == VortexShape.EL_RENO) {
            leanX = (float) Math.sin(age * 0.004D) * 0.18F;
            leanZ = (float) Math.cos(age * 0.003D) * 0.18F;
        } else {
            leanX *= 0.98F;
            leanZ *= 0.98F;
        }
    }

    private void updateGeometry() {
        float progress = lifeProgress();
        float fade = progress > 0.8F ? Mth.clamp((1.0F - progress) / 0.2F, 0.15F, 1.0F) : 1.0F;
        width = nominalWidth(ef, type) * fade;
        height = (type.isSmall() ? 18.0F + ef * 6.0F : 48.0F + ef * 22.0F) * (0.6F + 0.4F * fade);
    }

    /** Pushes, lifts and damages everything caught by the circulation. */
    private void applyWindEffects(ServerLevel level) {
        double reach = Math.max(width * 2.2D, 6.0D);
        double lowY = y - 4.0D;
        double highY = y + height;
        AABB box = new AABB(x - reach, lowY, z - reach, x + reach, highY, z + reach);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, box, e -> e.isAlive());
        boolean damagePhase = level.getGameTime() % 8L == 0L;

        for (Entity entity : entities) {
            if (entity instanceof Player player && player.isCreative()
                    && EWConfig.PROTECT_CREATIVE_PLAYERS.get()) {
                continue;
            }
            double dx = entity.getX() - x;
            double dz = entity.getZ() - z;
            double dist = Math.sqrt(dx * dx + dz * dz) + 0.001D;
            double strength = (1.0D - Mth.clamp(dist / reach, 0.0D, 1.0D)) * (0.08D + ef * 0.07D) * type.power;
            if (strength <= 0.0D) {
                continue;
            }
            // tangential + inflow wind, stronger inside the funnel
            double tanX = -dz / dist;
            double tanZ = dx / dist;
            double inX = -dx / dist;
            double inZ = -dz / dist;
            double lift = (0.05D + ef * 0.035D) * (dist < reach * 0.6D ? 1.0D : 0.35D);

            if (EWConfig.ENABLE_WIND_PUSH.get()) {
                entity.push(tanX * strength + inX * strength * 0.35D, lift, tanZ * strength + inZ * strength * 0.35D);
                entity.hurtMarked = true;
                entity.fallDistance = 0.0F;
            }

            if (damagePhase && EWConfig.ENABLE_DEBRIS_DAMAGE.get() && type.damaging
                    && dist < reach * 0.55D && ef > 0.35F) {
                float damage = Math.min(30.0F, 1.5F + ef * 3.0F);
                if (entity instanceof LivingEntity living) {
                    living.hurt(EWDamageTypes.of(level, EWDamageTypes.TORNADO), damage);
                }
            }
            if (type.fire) {
                entity.setSecondsOnFire(4);
            }
        }
    }

    /** Rips blocks out of the ground. Budgeted per tick to keep the server alive. */
    private void destroyBlocks(ServerLevel level) {
        int budget = EWConfig.BLOCKS_DESTROYED_PER_TICK.get();
        if (budget <= 0) {
            return;
        }
        float maxHardness = EWConfig.MAX_BLOCK_HARDNESS.get().floatValue() * Math.max(0.4F, ef / 5.0F);
        for (int i = 0; i < budget; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double t = RANDOM.nextFloat();
            float radius = getRadiusAt(t, 0.0F) * (0.4F + RANDOM.nextFloat() * 0.9F);
            int bx = Mth.floor(x + Math.cos(angle) * radius + leanX * t * 8.0F);
            int bz = Mth.floor(z + Math.sin(angle) * radius + leanZ * t * 8.0F);
            int by = Mth.floor(y + t * height);
            BlockPos pos = new BlockPos(bx, by, bz);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.WATER) || state.is(Blocks.LAVA)) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > maxHardness) {
                continue;
            }
            if (StormAnchor.isProtected(level, pos)) {
                continue;
            }
            level.destroyBlock(pos, false);
        }
    }

    private void spawnAmbientParticles(ServerLevel level) {
        int count = type.isSmall() ? 6 : 14;
        switch (type) {
            case DUST_DEVIL -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    x, y + height * 0.5D, z, count, width, height * 0.4D, width, 0.08D);
            case SNOW_WHIRL -> level.sendParticles(ParticleTypes.SNOWFLAKE,
                    x, y + height * 0.5D, z, count, width, height * 0.4D, width, 0.07D);
            case FIRE_WHIRL -> {
                level.sendParticles(ParticleTypes.FLAME,
                        x, y + height * 0.45D, z, count, width, height * 0.4D, width, 0.12D);
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        x, y + height * 0.7D, z, count / 2, width, height * 0.3D, width, 0.05D);
            }
            case WATERSPOUT -> level.sendParticles(ParticleTypes.SPLASH,
                    x, y + 1.0D, z, count, width, 2.0D, width, 0.25D);
            default -> {
                level.sendParticles(new DustParticleOptions(new Vector3f(0.32F, 0.27F, 0.22F), width * 0.35F + 1.0F),
                        x, y + height * 0.35D, z, count, width, height * 0.35D, width, 0.05D);
                level.sendParticles(ParticleTypes.CLOUD,
                        x, y + height * 0.75D, z, count / 2, width * 1.5D, height * 0.2D, width * 1.5D, 0.04D);
            }
        }
        if (ef > 2.5F && level.random.nextFloat() < 0.2F) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                    x, y + 2.0D, z, 12, width * 1.6D, 6.0D, width * 1.6D, 0.3D);
        }
    }

    // ------------------------------------------------------------------ persistence

    public void save(CompoundTag tag) {
        tag.putUUID("Id", id);
        tag.putString("Type", type.name());
        tag.putString("Shape", shape.name());
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putDouble("VX", vx);
        tag.putDouble("VZ", vz);
        tag.putFloat("Ef", ef);
        tag.putFloat("TargetEf", targetEf);
        tag.putFloat("Height", height);
        tag.putFloat("Width", width);
        tag.putFloat("Rotation", rotation);
        tag.putFloat("Spin", spin);
        tag.putFloat("LeanX", leanX);
        tag.putFloat("LeanZ", leanZ);
        tag.putInt("Age", age);
        tag.putInt("Life", life);
        tag.putInt("Subs", subs);
        if (parent != null) {
            tag.putUUID("Parent", parent);
        }
    }

    public static Vortex load(CompoundTag tag) {
        VortexType type = VortexType.byName(tag.getString("Type"));
        VortexShape shape = VortexShape.byName(tag.getString("Shape"));
        if (type == null) {
            type = VortexType.TORNADO;
        }
        if (shape == null) {
            shape = VortexShape.CONE;
        }
        Vortex vortex = new Vortex(type, tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"), tag.getFloat("Ef"));
        vortex.shape = shape;
        vortex.vx = tag.getDouble("VX");
        vortex.vz = tag.getDouble("VZ");
        vortex.targetEf = tag.getFloat("TargetEf");
        vortex.height = tag.getFloat("Height");
        vortex.width = tag.getFloat("Width");
        vortex.rotation = tag.getFloat("Rotation");
        vortex.spin = tag.getFloat("Spin");
        vortex.leanX = tag.getFloat("LeanX");
        vortex.leanZ = tag.getFloat("LeanZ");
        vortex.age = tag.getInt("Age");
        vortex.life = tag.getInt("Life");
        vortex.subs = tag.getInt("Subs");
        if (tag.hasUUID("Parent")) {
            vortex.parent = tag.getUUID("Parent");
        }
        return vortex;
    }
}
