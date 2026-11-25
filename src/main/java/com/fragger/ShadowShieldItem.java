package com.fragger;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ShadowShieldItem extends Item {
    public static final int COOLDOWN = 20 * 150; // 2.5 minutes

    public ShadowShieldItem(Settings settings) { super(settings); }

    @Override
    public UseAction getUseAction(ItemStack stack) { return UseAction.BLOCK; }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public net.minecraft.util.ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.isSneaking() && !user.getItemCooldownManager().isCoolingDown(stack)) {
            if (world instanceof ServerWorld sw && user instanceof ServerPlayerEntity sp) {
                shockwave(sw, sp);
                sp.getItemCooldownManager().set(stack, COOLDOWN);
            }
            return net.minecraft.util.ActionResult.SUCCESS;
        }

        user.setCurrentHand(hand);
        return net.minecraft.util.ActionResult.CONSUME;
    }

    private void shockwave(ServerWorld world, ServerPlayerEntity user) {
        Vec3d c = user.getEntityPos();
        Box box = new Box(c.x - 5, c.y - 5, c.z - 5, c.x + 5, c.y + 5, c.z + 5);
        List<PlayerEntity> near = world.getEntitiesByClass(PlayerEntity.class, box,
                pl -> pl != user && pl.isAlive());

        for (PlayerEntity target : near) {
            Vec3d dir = target.getEntityPos().subtract(c);
            if (dir.lengthSquared() < 0.01) dir = new Vec3d(1, 0, 0);
            dir = dir.normalize();
            Vec3d knock = new Vec3d(dir.x * 3.2, 1.0, dir.z * 3.2);
            target.setVelocity(knock);
            target.velocityModified = true;
            if (target instanceof ServerPlayerEntity sp) sp.networkHandler.sendPacket(
                    new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(sp));
        }

        world.spawnParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 8, 2.5, 0.5, 2.5, 0);
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1, c.z, 40, 2.5, 0.5, 2.5, 0.05);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0f, 0.6f);
    }
}
