package com.fragger;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.*;

public class FraggerItem extends Item {
    private static final Map<UUID, Integer> hits = new HashMap<>();

    public FraggerItem(Settings settings) { super(settings); }

    public static void resetCombo(UUID id) { hits.remove(id); }

    private static boolean comboActive(Entity e) {
        return hits.getOrDefault(e.getUuid(), 0) >= 3;
    }

    // Every hit during the combo is a crit (+50%)
    @Override
    public float getBonusAttackDamage(Entity target, float baseAttackDamage, DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker != null && comboActive(attacker)) return baseAttackDamage * 0.5f;
        return 0f;
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        UUID id = attacker.getUuid();
        boolean wasActive = comboActive(attacker);
        hits.merge(id, 1, Integer::sum);

        if (attacker.getEntityWorld() instanceof ServerWorld world) {
            if (wasActive) {
                world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
                world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1f);
            } else if (comboActive(attacker)) {
                // Combo just started
                world.playSound(null, attacker.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1f, 1.4f);
            }
        }
    }
}
