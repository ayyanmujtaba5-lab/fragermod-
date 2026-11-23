package com.fragger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;

public class FraggerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, target, hit) -> {
            if (world.isClient() && player.getStackInHand(hand).isOf(FraggerMod.FRAGGER)) {
                for (int i = 0; i < 20; i++) {
                    double x = target.getX() + (world.random.nextDouble() - 0.5) * target.getWidth() * 1.5;
                    double y = target.getY() + world.random.nextDouble() * target.getHeight();
                    double z = target.getZ() + (world.random.nextDouble() - 0.5) * target.getWidth() * 1.5;
                    world.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.05, 0);
                    if (i % 4 == 0) world.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.1, 0);
                }
            }
            return ActionResult.PASS;
        });
    }
}
