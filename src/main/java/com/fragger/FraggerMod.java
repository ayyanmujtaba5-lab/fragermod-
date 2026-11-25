package com.fragger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class FraggerMod implements ModInitializer {
    public static final String MOD_ID = "fragger";

    public static final float ATTACK_DAMAGE = 11.0f;
    public static final float ATTACK_SPEED = -2.4f;
    public static final float LASER_DAMAGE = 32.0f;
    public static final double LASER_RANGE = 30.0;
    public static final int LASER_COOLDOWN = 20 * 120;

    public static Item FRAGGER;
    public static Item SHADOW_SHIELD;
    private static final Set<UUID> wasSneaking = new HashSet<>();

    @Override
    public void onInitialize() {
        Identifier id = Identifier.of(MOD_ID, "fragger");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        FRAGGER = Registry.register(Registries.ITEM, key, new FraggerItem(
                new Item.Settings().registryKey(key)
                        .sword(ToolMaterial.NETHERITE, ATTACK_DAMAGE, ATTACK_SPEED)
                        .fireproof()
                        .rarity(Rarity.EPIC)));

        Identifier shieldId = Identifier.of(MOD_ID, "shadow_shield");
        RegistryKey<Item> shieldKey = RegistryKey.of(RegistryKeys.ITEM, shieldId);
        SHADOW_SHIELD = Registry.register(Registries.ITEM, shieldKey, new ShadowShieldItem(
                new Item.Settings().registryKey(shieldKey).maxDamage(-1).fireproof().rarity(Rarity.EPIC)));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> { e.add(FRAGGER); e.add(SHADOW_SHIELD); });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            FraggerItem.resetCombo(entity.getUuid());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) tickPlayer(p);
        });
    }

    private static boolean hasFragger(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).isOf(FRAGGER)) return true;
        return false;
    }

    private static boolean hasShield(ServerPlayerEntity p) {
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).isOf(SHADOW_SHIELD)) return true;
        return false;
    }

    private static void tickPlayer(ServerPlayerEntity p) {
        ServerWorld world = (ServerWorld) p.getEntityWorld();

        if (p.age % 20 == 0 && hasFragger(p))
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 50, 0, true, false, true));

        if (p.age % 20 == 0 && hasShield(p))
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 50, 0, true, false, true));

        ItemStack held = p.getMainHandStack();
        boolean holding = held.isOf(FRAGGER);

        if (holding && p.age % 4 == 0)
            world.spawnParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.4, 0.6, 0.4, 0.01);

        boolean sneaking = p.isSneaking();
        boolean justCrouched = sneaking && !wasSneaking.contains(p.getUuid());
        if (sneaking) wasSneaking.add(p.getUuid()); else wasSneaking.remove(p.getUuid());

        if (holding && justCrouched && !p.getItemCooldownManager().isCoolingDown(held)) {
            fireLaser(p, world);
            p.getItemCooldownManager().set(held, LASER_COOLDOWN);
        }
    }

    private static void fireLaser(ServerPlayerEntity p, ServerWorld world) {
        Vec3d start = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(LASER_RANGE));
        Box box = p.getBoundingBox().stretch(dir.multiply(LASER_RANGE)).expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(p, start, end, box,
                e -> e instanceof LivingEntity && e != p && !e.isSpectator(), LASER_RANGE * LASER_RANGE);
        if (hit != null) end = hit.getPos();

        DustParticleEffect red = new DustParticleEffect(0xFF2200, 1.5f);
        DustParticleEffect orange = new DustParticleEffect(0xFF8800, 1.2f);
        double len = start.distanceTo(end);
        for (double d = 1; d < len; d += 0.4) {
            Vec3d pt = start.add(dir.multiply(d));
            world.spawnParticles(red, pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0);
            world.spawnParticles(orange, pt.x, pt.y, pt.z, 1, 0.1, 0.1, 0.1, 0);
        }
        world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0f, 1.2f);

        if (hit != null) {
            Entity target = hit.getEntity();
            world.spawnParticles(ParticleTypes.SONIC_BOOM, end.x, end.y, end.z, 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 15, 0.3, 0.3, 0.3, 0);
            target.damage(world, world.getDamageSources().playerAttack(p), LASER_DAMAGE);
        }
    }
}
