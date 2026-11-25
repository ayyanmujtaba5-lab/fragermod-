package com.fragger.mixin;

import com.fragger.FraggerMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true)
    private void fragger$noDisable(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        ItemStack active = self.getActiveItem();
        if (active != null && active.isOf(FraggerMod.SHADOW_SHIELD)) {
            ci.cancel();
        }
    }
}
